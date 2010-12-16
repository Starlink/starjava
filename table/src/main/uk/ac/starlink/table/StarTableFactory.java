package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.table.formats.IpacTableBuilder;
import uk.ac.starlink.table.formats.TstTableBuilder;
import uk.ac.starlink.table.formats.WDCTableBuilder;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.URLDataSource;

/**
 * Manufactures {@link StarTable} objects from generic inputs.
 * This factory delegates the actual table creation to external 
 * {@link TableBuilder} objects, each of which knows how to read a 
 * particular table format.  Various <tt>makeStarTable</tt> methods
 * are offered, which construct <tt>StarTable</tt>s from different
 * types of object, such as {@link java.net.URL} and
 * {@link uk.ac.starlink.util.DataSource}.  Each of these comes in
 * two types: automatic format detection and named format.
 *
 * <p>In the case of a named format, a specifier must be given for the
 * format in which the table to be read is held.  This may be one of
 * the following:
 * <ul>
 * <li>The format name - this is a short mnemonic string like "fits"
 *     which is returned by the TableBuilder's <tt>getFormatName</tt> method - 
 *     it is matched case insensitively.  This must be one of the 
 *     builders known to the factory.
 * <li>The classname of a suitable TableBuilder (the class must 
 *     implement <tt>TableBuilder</tt> and have no-arg constructor).
 *     Such a class must be on the classpath, but need not have been 
 *     specified previously to the factory.
 * <li>The empty string or <tt>null</tt> or {@link #AUTO_HANDLER} - 
 *     in this case automatic format detection is used.
 * </ul>
 *
 * <p>In the case of automatic format detection (no format specified),
 * the factory hands the table location to each of the handlers in the
 * default handler list in turn, and if any of them can make a table out
 * of it, it is returned.
 *
 * <p>In either case, failure to make a table will usually result in a
 * <tt>TableFormatException</tt>, though if an error in actual I/O is
 * encountered an <tt>IOException</tt> may be thrown instead.
 * <p>
 * By default, if the corresponding classes are present, the following
 * TableBuilders are installed in the <em>default handler list</em>
 * (used by default in automatic format detection):
 * <ul>
 * <li> {@link uk.ac.starlink.votable.FitsPlusTableBuilder}
 *      (format name="fits-plus")
 * <li> {@link uk.ac.starlink.votable.ColFitsPlusTableBuilder}
 *      (format name="colfits-plus")
 * <li> {@link uk.ac.starlink.fits.ColFitsTableBuilder}
 *      (format name="colfits-basic")
 * <li> {@link uk.ac.starlink.fits.FitsTableBuilder}
 *      (format name="fits")
 * <li> {@link uk.ac.starlink.votable.VOTableBuilder}
 *      (format name="votable")
 * </ul>
 *
 * <p>The following additional handlers are installed in the
 * <em>known handler list</em>
 * (not used by default but available by specifying the format name):
 * <ul>
 * <li> {@link uk.ac.starlink.table.formats.AsciiTableBuilder}
 *      (format name="ascii")
 * <li> {@link uk.ac.starlink.table.formats.CsvTableBuilder}
 *      (format name="csv")
 * <li> {@link uk.ac.starlink.table.formats.TstTableBuilder}
 *      (format name="tst")
 * <li> {@link uk.ac.starlink.table.formats.IpacTableBuilder}
 *      (format name="ipac")
 * <li> {@link uk.ac.starlink.table.formats.WDCTableBuilder}
 *      (format name="wdc")
 * </ul>
 * Additionally, any classes named in the 
 * <tt>startable.readers</tt> system property (as a colon-separated list)
 * which implement the {@link TableBuilder} interface and have a no-arg
 * constructor will be instantiated and added to the known handler list.
 *
 * <p>The factory has a flag <tt>requireRandom</tt> which determines 
 * whether the <tt>makeStarTable</tt> methods are guaranteed to return
 * tables which provide random access (<tt>StarTable.isRandom()==true</tt>).
 * <strong>NOTE</strong> the meaning (and name) of this flag has changed
 * as of STIL version 2.1.  Previously it was only a hint that random
 * tables were preferred.  Now setting it true guarantees that all 
 * tables returned by the factory are random.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableFactory {

    private List defaultBuilders_;
    private List knownBuilders_;
    private JDBCHandler jdbcHandler_;
    private boolean requireRandom_;
    private StoragePolicy storagePolicy_;

    /**
     * System property which can contain a list of {@link TableBuilder} classes
     * for addition to the known (non-automatically detected) handler list.
     */
    public static final String KNOWN_BUILDERS_PROPERTY =
        "startable.readers";

    /**
     * Special handler identifier which signifies automatic format detection. 
     */
    public static final String AUTO_HANDLER = "(auto)";

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );
    private static String[] defaultBuilderClasses = { 
        "uk.ac.starlink.votable.FitsPlusTableBuilder",
        "uk.ac.starlink.votable.ColFitsPlusTableBuilder",
        "uk.ac.starlink.fits.ColFitsTableBuilder",
        "uk.ac.starlink.fits.FitsTableBuilder",
        "uk.ac.starlink.votable.VOTableBuilder",
    };
    private static String[] knownBuilderClasses = {
        AsciiTableBuilder.class.getName(),
        CsvTableBuilder.class.getName(),
        TstTableBuilder.class.getName(),
        IpacTableBuilder.class.getName(),
        WDCTableBuilder.class.getName(),
    };

    /**
     * Constructs a StarTableFactory with a default list of builders
     * which is not guaranteed to construct random-access tables.
     */
    public StarTableFactory() {
        this( false );
    }

    /**
     * Constructs a StarTableFactory with a default list of builders
     * specifying whether it will return random-access tables.
     *
     * @param   requireRandom  whether random-access tables will be constructed
     */
    public StarTableFactory( boolean requireRandom ) {
        requireRandom_ = requireRandom;
        defaultBuilders_ = new ArrayList();

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultBuilderClasses.length; i++ ) {
            String className = defaultBuilderClasses[ i ];
            try {
                Class clazz = this.getClass().forName( className );
                TableBuilder builder = (TableBuilder) clazz.newInstance();
                defaultBuilders_.add( builder );
                logger.config( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( "Failed to register " + className + " - " + e );
            }
        }

        /* Assemble list of all known builders - this includes the default
         * list plus perhaps some others. */
        knownBuilders_ = new ArrayList( defaultBuilders_ );
        for ( int i = 0; i < knownBuilderClasses.length; i++ ) {
            String className = knownBuilderClasses[ i ];
            try {
                Class clazz = this.getClass().forName( className );
                TableBuilder builder = (TableBuilder) clazz.newInstance();
                knownBuilders_.add( builder );
                logger.config( className + " registered as known" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( "Failed to register " + className + " - " + e );
            }
        }

        /* Attempt to add known handlers listed in system property. */
        knownBuilders_.addAll( Loader
                              .getClassInstances( KNOWN_BUILDERS_PROPERTY,
                                                  TableBuilder.class ) );
    }

    /**
     * Constructs a StarTableFactory which is a copy of an existing one.
     *
     * @param   fact   instance to copy
     */
    public StarTableFactory( StarTableFactory fact ) {
        this( fact.requireRandom() );
        defaultBuilders_ = new ArrayList( fact.defaultBuilders_ );
        knownBuilders_ = new ArrayList( fact.knownBuilders_ );
        jdbcHandler_ = fact.jdbcHandler_;
        storagePolicy_ = fact.storagePolicy_;
    }

    /**
     * Gets the list of builders which are used for automatic format detection.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     * This list can be modified to change the behaviour of the factory.
     *
     * @return  a mutable list of {@link TableBuilder} objects used to
     *          construct <tt>StarTable</tt>s
     */
    public List getDefaultBuilders() {
        return defaultBuilders_;
    }

    /**
     * Sets the list of builders which actually do the table construction.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     *
     * @param  builders  an array of TableBuilder objects used to 
     *         construct <tt>StarTable</tt>s
     */
    public void setDefaultBuilders( TableBuilder[] builders ) {
        defaultBuilders_ = new ArrayList( Arrays.asList( builders ) );
    }

    /**
     * Gets the list of builders which are available for selection by
     * format name. 
     * This is initially set to the list of default builders
     * plus a few others.
     * This list can be modified to change the behaviour of the factory.
     *
     * @return  a mutable list of {@link TableBuilder} objects which may be 
     *          specified for table building
     */
    public List getKnownBuilders() {
        return knownBuilders_;
    }

    /**
     * Sets the list of builders which are available for selection by
     * format name. 
     * This is initially set to the list of default builders
     * plus a few others.
     * 
     * @param  builders  an array of TableBuilder objects used to 
     *         construct <tt>StarTable</tt>s
     */
    public void setKnownBuilders( TableBuilder[] builders ) {
        knownBuilders_ = new ArrayList( Arrays.asList( builders ) );
    }

    /**
     * Returns the list of format names, one for each of the handlers returned
     * by {@link #getKnownBuilders}.
     * 
     * @return   list of format name strings
     */
    public List getKnownFormats() {
        List formats = new ArrayList();
        for ( Iterator it = getKnownBuilders().iterator(); it.hasNext(); ) {
            Object b = it.next();
            if ( b instanceof TableBuilder ) {
                formats.add( ((TableBuilder) b).getFormatName() );
            }
        }
        return formats;
    }

    /**
     * Sets whether random-access tables will be constructed by this factory.
     * If this flag is set <tt>true</tt> then any table returned by
     * the various <tt>makeStarTable</tt> methods is guaranteed to 
     * provide random access (its {@link StarTable#isRandom} method will 
     * return <tt>true</tt>).  If the flag is false, then returned 
     * tables may or may not be random-access.
     *
     * @param  requireRandom  whether this factory will create 
     *         random-access tables
     */
    public void setRequireRandom( boolean requireRandom ) {
        requireRandom_ = requireRandom;
    }

    /**
     * Returns the <tt>requireRandom</tt> flag.
     * If this flag is set <tt>true</tt> then any table returned by
     * the various <tt>makeStarTable</tt> methods is guaranteed to 
     * provide random access (its {@link StarTable#isRandom} method will 
     * return <tt>true</tt>).  If the flag is false, then returned 
     * tables may or may not be random-access.
     *
     * @return  whether this factory will create random-access tables
     */
    public boolean requireRandom() {
        return requireRandom_;
    }

    /**
     * Sets the storage policy.  This may be used to determine what kind
     * of scratch storage is used when constructing tables.
     *
     * @param  policy  the new storage policy object
     */
    public void setStoragePolicy( StoragePolicy policy ) {
        storagePolicy_ = policy;
    }

    /**
     * Returns the current storage policy.  This may be used to determine
     * what kind of scratch storage is used when constructing tables.
     * If it has not been set explicitly, the default policy is used 
     * ({@link StoragePolicy#getDefaultPolicy}).
     *
     * @return  storage policy object
     */
    public StoragePolicy getStoragePolicy() {
        if ( storagePolicy_ == null ) {
            storagePolicy_ = StoragePolicy.getDefaultPolicy();
        }
        return storagePolicy_;
    }

    /**
     * Returns a table based on a given table and guaranteed to have
     * random access.  If the original table <tt>table</tt> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * <p>This convenience method is equivalent to 
     * <tt>getStoragePolicy().randomTable(table)</tt>.
     *
     * @param  table  original table
     * @return  a table with the same data as <tt>table</tt> and with
     *          <tt>isRandom()==true</tt>
     */
    public StarTable randomTable( StarTable table ) throws IOException {
        return getStoragePolicy().randomTable( table );
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a <tt>DataSource</tt> 
     * object using automatic format detection.
     *
     * @param  datsrc  the data source containing the table data
     * @return a new StarTable view of the resource <tt>datsrc</tt>
     * @throws TableFormatException if none of the default handlers
     *         could turn <tt>datsrc</tt> into a table
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        for ( Iterator it = defaultBuilders_.iterator(); it.hasNext(); ) {
            TableBuilder builder = (TableBuilder) it.next();
            try {
                StarTable startab = 
                    builder.makeStarTable( datsrc, requireRandom(),
                                           getStoragePolicy() );
                startab = prepareTable( startab );
                startab.setURL( datsrc.getURL() );
                if ( startab.getName() == null ) {
                    startab.setName( datsrc.getName() );
                }
                return startab;
            }
            catch ( TableFormatException e ) {
                logger.info( "Table not " + builder.getFormatName() + " - " +
                             e.getMessage() );
            }
        }

        /* None of the handlers could make a table. */
        StringBuffer msg = new StringBuffer();
        msg.append( "Can't make StarTable from \"" )
           .append( datsrc.getName() )
           .append( "\"" );
        Iterator it = defaultBuilders_.iterator();
        if ( it.hasNext() ) {
            msg.append( " (tried" );
            while ( it.hasNext() ) {
                msg.append( " " )
                   .append( ((TableBuilder) it.next()).getFormatName() );
                if ( it.hasNext() ) {
                    msg.append( ',' );
                }
            }
            msg.append( ')' );
        }
        else {
            msg.append( " - no table handlers available" );
        }
        throw new TableFormatException( msg.toString() );
    }

    /**
     * Constructs a sequence of StarTables from a DataSource using automatic
     * format detection.  Only certain formats (those whose handlers 
     * implement {@link MultiTableBuilder} will be capable of returning
     * a sequence having more than one element.
     *
     * @param  datsrc  the data source containing the table data
     * @return   a sequence of tables loaded from <code>datsrc</code>
     * @throws TableFormatException if none of the default handlers
     *         could turn <tt>datsrc</tt> into a table
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( DataSource datsrc ) 
            throws TableFormatException, IOException {
        for ( Iterator it = defaultBuilders_.iterator(); it.hasNext(); ) {
            TableBuilder builder = (TableBuilder) it.next();
            try {
                if ( builder instanceof MultiTableBuilder ) {
                    TableSequence tseq =
                        ((MultiTableBuilder) builder)
                       .makeStarTables( datsrc, getStoragePolicy() );
                    String nameBase = datsrc.getName() + "-";
                    return prepareTableSequence( tseq, nameBase );
                }
                else {
                    StarTable startab =
                        builder.makeStarTable( datsrc, requireRandom(),
                                               getStoragePolicy() );
                    startab = prepareTable( startab );
                    startab.setURL( datsrc.getURL() );
                    if ( startab.getName() == null ) {
                        startab.setName( datsrc.getName() );
                    }
                    return Tables.singleTableSequence( startab );
                }
            }
            catch ( TableFormatException e ) {
                logger.info( "Table not " + builder.getFormatName() + " - "
                           + e.getMessage() );
            }
        }

        /* None of the handlers could make tables. */
        StringBuffer msg = new StringBuffer();
        msg.append( "Can't make StarTables from \"" )
           .append( datsrc.getName() )
           .append( "\"" );
        Iterator it = defaultBuilders_.iterator();
        if ( it.hasNext() ) {
            msg.append( " (tried" );
            while ( it.hasNext() ) {
                msg.append( " " )
                   .append( ((TableBuilder) it.next()).getFormatName() );
                if ( it.hasNext() ) {
                    msg.append( ',' );
                }
            }
            msg.append( ')' );
        }
        else {
            msg.append( " - no table handlers available" );
        }
        throw new TableFormatException( msg.toString() );
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a location string
     * using automatic format detection.  The location string
     * can represent a filename or URL, including a <tt>jdbc:</tt>
     * protocol URL if an appropriate JDBC driver is installed.
     *
     * @param  location  the name of the table resource
     * @return a new StarTable view of the resource at <tt>location</tt>
     * @throws TableFormatException if no handler capable of turning
     *        <tt>location</tt> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     */
    public StarTable makeStarTable( String location )
            throws TableFormatException, IOException {
        if ( location.startsWith( "jdbc:" ) ) {
            StarTable table = prepareTable( getJDBCHandler()
                                           .makeStarTable( location, false ) );
            return requireRandom() ? randomTable( table )
                                   : table;
        }
        else {
            return makeStarTable( DataSource.makeDataSource( location ) );
        }
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a URL using
     * automatic format detection.
     *
     * @param  url  the URL where the table lives
     * @return a new StarTable view of the resource at <tt>url</tt>
     * @throws TableFormatException if no handler capable of turning
     *        <tt>datsrc</tt> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     * @deprecated  Use <code>makeStarTable(new URLDataSource(url))</code>
     */
    public StarTable makeStarTable( URL url ) throws IOException {
        return makeStarTable( new URLDataSource( url ) );
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a <tt>DataSource</tt>
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <tt>null</tt> or the empty string or
     * the special value {@link #AUTO_HANDLER} is 
     * supplied for <tt>handler</tt>, it will fall back on automatic 
     * format detection.
     *
     * @param  datsrc  the data source containing the table data
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource <tt>datsrc</tt>
     * @throws TableFormatException  if <tt>datsrc</tt> does not contain
     *         a table in the format named by <tt>handler</tt>
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( DataSource datsrc, String handler )
            throws TableFormatException, IOException {
        if ( handler == null || handler.trim().length() == 0 ||
             handler.equals( AUTO_HANDLER ) ) {
            return makeStarTable( datsrc );
        }
        TableBuilder builder = getTableBuilder( handler );
        StarTable startab;
        try {
            startab = builder.makeStarTable( datsrc, requireRandom(),
                                             getStoragePolicy() );
            startab = prepareTable( startab );
        }

        /* If the table handler fails to load the table, rethrow the exception
         * with additional information about the handler that failed. */
        catch ( TableFormatException e ) {
            String msg = "Can't open " + datsrc.getName() + " as " + 
                         builder.getFormatName();
            String emsg = e.getMessage();
            if ( emsg != null && emsg.trim().length() > 0 ) {
                msg += " (" + emsg + ")";
            }
            else {
                msg += " (" + e.toString() + ")";
            }
            throw new TableFormatException( msg, e );
        }

        /* Doctor the table's URL and name. */
        startab.setURL( datsrc.getURL() );
        if ( startab.getName() == null ) {
            startab.setName( datsrc.getName() );
        }

        /* Return the table. */
        return startab;
    }

    /**
     * Constructs a sequence of StarTables from a DataSource using a named
     * table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <tt>null</tt> or the empty string or
     * the special value {@link #AUTO_HANDLER} is 
     * supplied for <tt>handler</tt>, it will fall back on automatic 
     * format detection.
     *
     * <p>If the handler does not implement the {@link MultiTableBuilder}
     * interface, then the returned sequence will contain a single table.
     *
     * @param  datsrc  the data source containing the table data
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a sequence of StarTables loaded from <tt>datsrc</tt>
     * @throws TableFormatException  if <tt>datsrc</tt> does not contain
     *         a table in the format named by <tt>handler</tt>
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( DataSource datsrc, String handler )
            throws TableFormatException, IOException {
        if ( handler == null || handler.trim().length() == 0 ||
             handler.equals( AUTO_HANDLER ) ) {
            return makeStarTables( datsrc );
        }
        TableBuilder builder = getTableBuilder( handler );
        StarTable[] startabs;
        try {
            if ( builder instanceof MultiTableBuilder ) {
                TableSequence tseq = 
                    ((MultiTableBuilder) builder)
                   .makeStarTables( datsrc, getStoragePolicy() );
                String nameBase = datsrc.getName() + "-";
                return prepareTableSequence( tseq, nameBase );
            }
            else {
                StarTable startab =
                    builder.makeStarTable( datsrc, requireRandom(),
                                           getStoragePolicy() );
                startab = prepareTable( startab );
                startab.setURL( datsrc.getURL() );
                if ( startab.getName() == null ) {
                    startab.setName( datsrc.getName() );
                }
                return Tables.singleTableSequence( startab );
            }
        }

        /* If the table handler fails to read the table, rethrow the exception
         * with additional information about the handler that failed. */
        catch ( TableFormatException e ) {
            String msg = "Can't open " + datsrc.getName() + " as " +
                         builder.getFormatName();
            String emsg = e.getMessage();
            if ( emsg != null && emsg.trim().length() > 0 ) {
                msg += " (" + emsg + ")";
            }
            else {
                msg += " (" + e.toString() + ")";
            }
            throw new TableFormatException( msg, e );
        }
    }

    /**
     * Constructs a sequence of <tt>StarTable</tt>s from a location string
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <tt>null</tt> or the empty string or
     * the special value {@link #AUTO_HANDLER} is 
     * supplied for <tt>handler</tt>, it will fall back on automatic 
     * format detection.
     *
     * @param  location  the name of the table resource
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <tt>location</tt>
     * @throws TableFormatException  if <tt>location</tt> does not point to
     *         a table in the format named by <tt>handler</tt>
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( String location, String handler )
            throws TableFormatException, IOException {
        if ( location.startsWith( "jdbc:" ) ) {
            StarTable table = makeStarTable( location, handler );
            return Tables.singleTableSequence( table );
        }                       
        else {
            return makeStarTables( DataSource.makeDataSource( location ),
                                   handler );
        }
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a location string
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <tt>null</tt> or the empty string or
     * the special value {@link #AUTO_HANDLER} is 
     * supplied for <tt>handler</tt>, it will fall back on automatic 
     * format detection.  A location of "-" means standard input - in 
     * this case the handler must be specified.
     *
     * @param  location  the name of the table resource
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <tt>location</tt>
     * @throws TableFormatException  if <tt>location</tt> does not point to
     *         a table in the format named by <tt>handler</tt>
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( String location, String handler )
            throws TableFormatException, IOException {
        if ( location.startsWith( "jdbc:" ) ) {
            StarTable table = prepareTable( getJDBCHandler()
                                           .makeStarTable( location, false ) );
            return requireRandom() ? randomTable( table )
                                   : table;
        }
        else if ( location.equals( "-" ) ) {
            return makeStarTable( System.in, getTableBuilder( handler ) );
        }
        else {
            return makeStarTable( DataSource.makeDataSource( location ),
                                  handler );
        }
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a URL 
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <tt>null</tt> or the empty string or
     * the special value {@link #AUTO_HANDLER} is 
     * supplied for <tt>handler</tt>, it will fall back on automatic 
     * format detection.
     *
     * @param  url  the URL where the table lives
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <tt>url</tt>
     * @throws TableFormatException  if the resource at <tt>url</tt> cannot
     *         be turned into a table by <tt>handler</tt>
     * @throws IOException  if an I/O error is encountered
     * @deprecated  Use 
     *         <code>makeStarTable(new URLDataSource(url),handler)</code>
     */
    public StarTable makeStarTable( URL url, String handler )
            throws TableFormatException, IOException {
        return makeStarTable( new URLDataSource( url ), handler );
    }

    /**
     * Attempts to read and return a StarTable from an input stream.
     * This is not always possible, since certain table handlers
     * may required more than one pass through the input data.
     * The handler must be specified (automatic format detection cannot
     * be used on a stream).
     * The input stream will be decompressed and buffered if necessary.
     *
     * @param  in  input stream 
     * @param  builder   handler which understands the data in <tt>in</tt>
     * @return  a table read from the stream if it could be done
     * @see    TableBuilder#streamStarTable
     * @throws  TableFormatException   if <code>builder</code> needs more
     *          than one pass of the data, or the stream is in some way
     *          malformed
     * @throws  IOException for other I/O errors
     */
    public StarTable makeStarTable( InputStream in, TableBuilder builder )
            throws TableFormatException, IOException {
        in = Compression.decompressStatic( in );
        in = new BufferedInputStream( in );
        RowStore store = getStoragePolicy().makeRowStore();
        builder.streamStarTable( in, store, null );
        return prepareTable( store.getStarTable() );
    }

    /**
     * Constructs a StarTable from a 
     * {@link java.awt.datatransfer.Transferable} object 
     * using automatic format detection. 
     * In conjunction with a suitable {@link javax.swing.TransferHandler}
     * this makes it easy to accept drop of an object representing a table
     * which has been dragged from another application.
     * <p>
     * The implementation of this method currently tries the following 
     * on a given transferable to turn it into a table:
     * <ul>
     * <li>If it finds a {@link java.net.URL} object, passes that to the
     *     URL factory method
     * <li>If it finds a transferable that will supply an
     *     {@link java.io.InputStream}, turns it into a 
     *     {@link uk.ac.starlink.util.DataSource} and passes that to the 
     *     <tt>DataSource</tt> constructor
     * </ul>
     *
     * @param  trans  the Transferable object to construct a table from
     * @return  a new StarTable constructed from the Transferable
     * @throws  TableFormatException  if no table can be constructed
     * @see  #canImport
     */
    public StarTable makeStarTable( final Transferable trans )
            throws IOException {

        /* Go through all the available flavours offered by the transferable. */
        DataFlavor[] flavors = trans.getTransferDataFlavors();
        StringBuffer msg = new StringBuffer();
        for ( int i = 0; i < flavors.length; i++ ) {
            final DataFlavor flavor = flavors[ i ];
            String mimeType = flavor.getMimeType();
            Class clazz = flavor.getRepresentationClass();

            /* If it represents a URL, get the URL and offer it to the
             * URL factory method. */
            if ( clazz.equals( URL.class ) ) {
                try {
                    Object data = trans.getTransferData( flavor );
                    if ( data instanceof URL ) {
                        URL url = (URL) data;
                        return makeStarTable( url );
                    }
                }
                catch ( UnsupportedFlavorException e ) {
                    throw new RuntimeException( "DataFlavor " + flavor 
                                              + " support withdrawn?" );
                }
                catch ( TableFormatException e ) {
                    msg.append( e.getMessage() );
                }
            }

            /* If we can get a stream, see if any of the builders will
             * take it. */
            if ( InputStream.class.isAssignableFrom( clazz ) && 
                 ! flavor.isFlavorSerializedObjectType() ) {
                for ( Iterator it = defaultBuilders_.iterator(); 
                      it.hasNext(); ) {
                    TableBuilder builder = (TableBuilder) it.next();
                    if ( builder.canImport( flavor ) ) {
                        InputStream in;
                        try {
                            in = (InputStream) trans.getTransferData( flavor );
                        }
                        catch ( UnsupportedFlavorException e ) {
                            throw new RuntimeException(
                                "DataFlavor " + flavor + 
                                " support withdrawn?" );
                        }
                        return makeStarTable( in, builder );
                    }
                }
            }
        }

        /* No luck. */
        throw new TableFormatException( msg.toString() );
    }

    /**
     * Indicates whether a particular set of <tt>DataFlavor</tt> ojects
     * offered by a {@link java.awt.datatransfer.Transferable}
     * is suitable for attempting to turn the <tt>Transferable</tt>
     * into a StarTable.
     * <p>
     * Each of the builder objects is queried about whether it can 
     * import the given flavour, and if one says it can, a true value
     * is returned.  A true value is also returned if one of the flavours
     * has a representation class of {@link java.net.URL}.
     *
     * @param  flavors  the data flavours offered
     */
    public boolean canImport( DataFlavor[] flavors ) {
        for ( int i = 0; i < flavors.length; i++ ) {
            DataFlavor flavor = flavors[ i ];
            String mimeType = flavor.getMimeType();
            Class clazz = flavor.getRepresentationClass();
            if ( clazz.equals( URL.class ) ) {
                return true;
            }
            else {
                for ( Iterator it = defaultBuilders_.iterator();
                      it.hasNext(); ) {
                    TableBuilder builder = (TableBuilder) it.next();
                    if ( builder.canImport( flavor ) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the JDBC handler object used by this factory.
     *
     * @return   the JDBC handler
     */
    public JDBCHandler getJDBCHandler() {
        if ( jdbcHandler_ == null ) {
            jdbcHandler_ = new JDBCHandler();
        }
        return jdbcHandler_;
    }

    /**
     * Sets the JDBC handler object used by this factory.
     *
     * @param  handler  the JDBC handler
     */
    public void setJDBCHandler( JDBCHandler handler ) {
        jdbcHandler_ = handler;
    }

    /**
     * Returns a table handler with a given name.
     * This name may be either its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.
     *
     * @param   name  specification of the handler required
     * @return  TableBuilder specified by <tt>name</tt>
     * @throws  TableFormatException  if <tt>name</tt> doesn't name any 
     *          available handler
     */
    public TableBuilder getTableBuilder( String name )
            throws TableFormatException {
        if ( name == null ) {
            throw new TableFormatException( "No table handler with null name" );
        }

        /* Try all the known handlers, matching against format name. */
        List builders = new ArrayList( knownBuilders_ );
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            TableBuilder builder = (TableBuilder) it.next();
            if ( builder.getFormatName().equalsIgnoreCase( name ) ) {
                return builder;
            }
        }

        /* See if it's a classname */
        try {
            Class clazz = this.getClass().forName( name );
            if ( TableBuilder.class.isAssignableFrom( clazz ) ) {
                return (TableBuilder) clazz.newInstance();
            }
            else {
                throw new TableFormatException( 
                    "Class " + clazz + " does not implement TableBuilder" );
            }
        }
        catch ( InstantiationException e ) {
            throw new TableFormatException( e.toString(), e );
        }
        catch ( IllegalAccessException e ) {
            throw new TableFormatException( e.toString(), e );
        }
        catch ( ClassNotFoundException e ) {
            // No, it's not a class name.
        }

        /* Failed to find any handler for name. */
        throw new TableFormatException( "No table handler available for " 
                                      + name );
    }

    /**
     * Prepares a table for return from one of the makeStarTable methods.
     * Currently what this does is to randomise it if it needs randomising.
     *
     * @param  startab  table to prepare
     * @return  prepared table - may be <tt>startab</tt> or a new one
     */
    private StarTable prepareTable( StarTable startab ) throws IOException {
        return requireRandom() ? randomTable( startab )
                               : startab;
    }

    /**
     * Prepares a sequence of tables for return from one of the makeStarTables
     * methods.  As well as calling {@link #prepareTable}, it adjusts the
     * tables names if appropriate.
     *
     * @param  tseq  input sequence
     * @param  nameBase  stem of table name
     * @return  output sequence
     */
    private TableSequence prepareTableSequence( final TableSequence tseq,
                                                final String nameBase ) {
        return new TableSequence() {
            private int index;
            public StarTable nextTable() throws IOException {
                StarTable table = tseq.nextTable();
                if ( table == null ) {
                    return null;
                }
                else {
                    index++;
                    table = prepareTable( table );
                    if ( table.getName() == null ) {
                        table.setName( nameBase + index );
                    }
                    return table;
                }
            }
        };
    }
}
