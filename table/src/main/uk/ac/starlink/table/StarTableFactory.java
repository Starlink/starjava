package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.table.formats.IpacTableBuilder;
import uk.ac.starlink.table.formats.MrtTableBuilder;
import uk.ac.starlink.table.formats.TstTableBuilder;
import uk.ac.starlink.table.formats.WDCTableBuilder;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.JDBCTableScheme;
import uk.ac.starlink.util.BeanConfig;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.URLDataSource;

/**
 * Manufactures {@link StarTable} objects from generic inputs.
 * This factory delegates the actual table creation to external
 * {@link TableBuilder} objects, each of which knows how to read a
 * particular table format from an input data stream.
 * Various <code>makeStarTable</code> methods
 * are offered, which construct <code>StarTable</code>s from different
 * types of object, such as {@link java.net.URL} and
 * {@link uk.ac.starlink.util.DataSource}.  Each of these comes in
 * two types: automatic format detection and named format.
 * Additionally, a list of {@link TableScheme} objects is maintained,
 * each of which can produce a table from an opaque specification string
 * of the form <code>:&lt;scheme-name&gt;:&lt;spec&gt;</code>.
 *
 * <p>In the case of a named format, a specifier must be given for the
 * format in which the table to be read is held.  This may be one of
 * the following:
 * <ul>
 * <li>The format name - this is a short mnemonic string like "fits"
 *     which is returned by the TableBuilder's <code>getFormatName</code>
 *     method - it is matched case insensitively.  This must be one of the
 *     builders known to the factory.
 * <li>The classname of a suitable TableBuilder (the class must
 *     implement <code>TableBuilder</code> and have no-arg constructor).
 *     Such a class must be on the classpath, but need not have been
 *     specified previously to the factory.
 * <li>The empty string or <code>null</code> or {@link #AUTO_HANDLER} -
 *     in this case automatic format detection is used.
 * </ul>
 *
 * <p>In the case of automatic format detection (no format specified),
 * the factory hands the table location to each of the handlers in the
 * default handler list in turn, and if any of them can make a table out
 * of it, it is returned.
 *
 * <p>In either case, failure to make a table will usually result in a
 * <code>TableFormatException</code>, though if an error in actual I/O is
 * encountered an <code>IOException</code> may be thrown instead.
 *
 * <p>By default, if the corresponding classes are present, the following
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
 * <li> {@link uk.ac.starlink.cdf.CdfTableBuilder}
 *      (format name="cdf")
 * <li> {@link uk.ac.starlink.ecsv.EcsvTableBuilder}
 *      (format name="ecsv")
 * <li> {@link uk.ac.starlink.pds4.Pds4TableBuilder}
 *      (format name="pds4")
 * <li> {@link uk.ac.starlink.table.formats.MrtTableBuilder}
 *      (format name="mrt")
 * <li> {@link uk.ac.starlink.parquet.ParquetTableBuilder}
 *      (format name="parquet")
 * <li> {@link uk.ac.starlink.feather.FeatherTableBuilder}
 *      (format name="feather")
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
 * <li> {@link uk.ac.starlink.hapi.HapiTableBuilder}
 *      (format name="hapi")
 * <li> {@link uk.ac.starlink.gbin.GbinTableBuilder}
 *      (format name="gbin")
 * <li> {@link uk.ac.starlink.table.formats.WDCTableBuilder}
 *      (format name="wdc")
 * </ul>
 *
 * <p>Additionally, any classes named in the
 * <code>startable.readers</code> system property (as a colon-separated list)
 * which implement the {@link TableBuilder} interface and have a no-arg
 * constructor will be instantiated and added to the known handler list.
 *
 * <p>Some {@link #makeStarTable(java.lang.String) makeStarTable} methods
 * take a location String rather than an input stream or DataSource;
 * these may either give a URL or filename, or a
 * <em>scheme-based location</em> of the form
 * <code>:&lt;scheme-name&gt;:&lt;scheme-specification&gt;</code>,
 * for instance "<code>jdbc://localhost/db1#SELECT id FROM gsc</code>".
 * There is a theoretical risk of a namespace clash between
 * input-yielding URLs, or even filenames, and scheme-based locations,
 * but if scheme names avoid obvious values like "http" and "C"
 * this is not likely to cause problems in practice.
 * <p>The following TableSchemes are installed by default:
 * <ul>
 * <li>{@link uk.ac.starlink.table.jdbc.JDBCTableScheme} (scheme name="jdbc")
 * <li>{@link LoopTableScheme} (scheme name="loop")
 * <li>{@link TestTableScheme} (scheme name="test")
 * <li>{@link ClassTableScheme} (scheme name="class")
 * <li>{@link uk.ac.starlink.hapi.HapiTableScheme} (scheme name="hapi")
 * </ul>
 * <p>Additionally, any classes named in the <code>startable.schemes</code>
 * system property (as a colon-separated list) which implement the
 * {@link TableScheme} interface and have a no-arg constructor
 * will be instantiated and added to the known scheme list.
 *
 * <p>The factory has a flag <code>requireRandom</code> which determines 
 * whether the <code>makeStarTable</code> methods are guaranteed to return
 * tables which provide random access (<code>StarTable.isRandom()==true</code>).
 * <strong>NOTE</strong> the meaning (and name) of this flag has changed
 * as of STIL version 2.1.  Previously it was only a hint that random
 * tables were preferred.  Now setting it true guarantees that all
 * tables returned by the factory are random.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableFactory {

    private List<TableBuilder> defaultBuilders_;
    private List<TableBuilder> knownBuilders_;
    private Map<String,TableScheme> schemes_;
    private JDBCHandler jdbcHandler_;
    private boolean requireRandom_;
    private StoragePolicy storagePolicy_;
    private TablePreparation tablePrep_;
    private Predicate<DataSource> inputRestriction_;

    /**
     * System property which can contain a list of {@link TableBuilder}
     * classnames for addition to the known (non-automatically detected)
     * handler list.
     */
    public static final String KNOWN_BUILDERS_PROPERTY =
        "startable.readers";

    /**
     * System property which can contain a list of {@link TableScheme}
     * classnames for addition to the default list.
     */
    public static final String SCHEMES_PROPERTY = "startable.schemes";

    /**
     * Special handler identifier which signifies automatic format detection.
     */
    public static final String AUTO_HANDLER = "(auto)";

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.table" );
    private static final Pattern SCHEME_REGEX =
        Pattern.compile( ":([a-zA-Z0-9_-]+):(.*)" );
    private static String[] defaultBuilderClasses = {
        "uk.ac.starlink.votable.FitsPlusTableBuilder",
        "uk.ac.starlink.votable.ColFitsPlusTableBuilder",
        "uk.ac.starlink.fits.ColFitsTableBuilder",
        "uk.ac.starlink.fits.FitsTableBuilder",
        "uk.ac.starlink.votable.VOTableBuilder",
        "uk.ac.starlink.cdf.CdfTableBuilder",
        "uk.ac.starlink.ecsv.EcsvTableBuilder",
        "uk.ac.starlink.pds4.Pds4TableBuilder",
        "uk.ac.starlink.table.formats.MrtTableBuilder",
        "uk.ac.starlink.parquet.ParquetTableBuilder",
        "uk.ac.starlink.feather.FeatherTableBuilder",
        "uk.ac.starlink.gbin.GbinTableBuilder",
    };
    private static String[] knownBuilderClasses = {
        AsciiTableBuilder.class.getName(),
        CsvTableBuilder.class.getName(),
        TstTableBuilder.class.getName(),
        IpacTableBuilder.class.getName(),
        "uk.ac.starlink.hapi.HapiTableBuilder",
        WDCTableBuilder.class.getName(),
    };
    private static String[] dfltSchemeClasses = {
        LoopTableScheme.class.getName(),
        TestTableScheme.class.getName(),
        ClassTableScheme.class.getName(),
        "uk.ac.starlink.hapi.HapiTableScheme",
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

        /* List of default builders. */
        defaultBuilders_ =
            listFromClassNames( defaultBuilderClasses, TableBuilder.class );

        /* List of all known builders - this includes the default list,
         * some others, and any listed in a system property. */
        knownBuilders_ = new ArrayList<TableBuilder>();
        knownBuilders_.addAll( defaultBuilders_ );
        knownBuilders_.addAll( listFromClassNames( knownBuilderClasses,
                                                   TableBuilder.class ) );
        knownBuilders_.addAll( Loader
                              .getClassInstances( KNOWN_BUILDERS_PROPERTY,
                                                  TableBuilder.class ) );

        /* Prepare a list of TableSchemes, including one for JDBC
         * (handled internally by default for historical reasons),
         * other default instances, and any supplied by system property. */
        List<TableScheme> schemeList = new ArrayList<TableScheme>();
        schemeList.add( new JDBCTableScheme( this ) );
        schemeList.addAll( listFromClassNames( dfltSchemeClasses,
                                               TableScheme.class ) );
        schemeList.addAll( Loader.getClassInstances( SCHEMES_PROPERTY,
                                                     TableScheme.class ) );
        schemes_ = new LinkedHashMap<String,TableScheme>();
        for ( TableScheme scheme : schemeList ) {
            addScheme( scheme );
        }
    }

    /**
     * Constructs a StarTableFactory which is a copy of an existing one.
     *
     * @param   fact   instance to copy
     */
    public StarTableFactory( StarTableFactory fact ) {
        this( fact.requireRandom() );
        defaultBuilders_ = new ArrayList<TableBuilder>( fact.defaultBuilders_ );
        knownBuilders_ = new ArrayList<TableBuilder>( fact.knownBuilders_ );
        schemes_ = new LinkedHashMap<String,TableScheme>( fact.schemes_ );
        storagePolicy_ = fact.storagePolicy_;
        tablePrep_ = fact.tablePrep_;
    }

    /**
     * Gets the list of builders which are used for automatic format detection.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     * This list can be modified to change the behaviour of the factory.
     *
     * @return  a mutable list of {@link TableBuilder} objects used to
     *          construct <code>StarTable</code>s
     */
    public List<TableBuilder> getDefaultBuilders() {
        return defaultBuilders_;
    }

    /**
     * Sets the list of builders which actually do the table construction.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     *
     * @param  builders  an array of TableBuilder objects used to
     *         construct <code>StarTable</code>s
     */
    public void setDefaultBuilders( TableBuilder[] builders ) {
        defaultBuilders_ =
            new ArrayList<TableBuilder>( Arrays.asList( builders ) );
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
    public List<TableBuilder> getKnownBuilders() {
        return knownBuilders_;
    }

    /**
     * Sets the list of builders which are available for selection by
     * format name.
     * This is initially set to the list of default builders
     * plus a few others.
     *
     * @param  builders  an array of TableBuilder objects used to
     *         construct <code>StarTable</code>s
     */
    public void setKnownBuilders( TableBuilder[] builders ) {
        knownBuilders_ =
            new ArrayList<TableBuilder>( Arrays.asList( builders ) );
    }

    /**
     * Returns the list of format names, one for each of the handlers returned
     * by {@link #getKnownBuilders}.
     *
     * @return   list of format name strings
     */
    public List<String> getKnownFormats() {
        List<String> formats = new ArrayList<String>();
        for ( TableBuilder b : getKnownBuilders() ) {
            formats.add( b.getFormatName() );
        }
        return formats;
    }

    /**
     * Returns a schemeName-&gt;scheme map indicating the TableSchemes
     * in use by this factory.
     * This map is mutable, so entries may be added or removed,
     * but <strong>NOTE</strong> that the map keys are assumed to be
     * equivalent to the getSchemeName return value for their value,
     * so modify it with care.
       Consider using the {@link #addScheme} method for adding entries.
     *
     * @return   map of scheme names to schemes
     */
    public Map<String,TableScheme> getSchemes() {
        return schemes_;
    }

    /**
     * Safely adds a table scheme for use by this factory.
     * It is equivalent to
     * <code>getSchemes().put(scheme.getSchemeName(),scheme)</code>.
     *
     * @param  scheme   new scheme for use
     */
    public void addScheme( TableScheme scheme ) {
        schemes_.put( scheme.getSchemeName(), scheme );
    }

    /**
     * Sets whether random-access tables will be constructed by this factory.
     * If this flag is set <code>true</code> then any table returned by
     * the various <code>makeStarTable</code> methods is guaranteed to
     * provide random access (its {@link StarTable#isRandom} method will
     * return <code>true</code>).  If the flag is false, then returned
     * tables may or may not be random-access.
     *
     * @param  requireRandom  whether this factory will create
     *         random-access tables
     */
    public void setRequireRandom( boolean requireRandom ) {
        requireRandom_ = requireRandom;
    }

    /**
     * Returns the <code>requireRandom</code> flag.
     * If this flag is set <code>true</code> then any table returned by
     * the various <code>makeStarTable</code> methods is guaranteed to
     * provide random access (its {@link StarTable#isRandom} method will
     * return <code>true</code>).  If the flag is false, then returned
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
     * Sets a table preparation object that is invoked on each table
     * created by this factory.  Any previous value is overwritten.
     * Null is allowed.
     *
     * @param  tablePrep  new table preparation, or null
     */
    public void setPreparation( TablePreparation tablePrep ) {
        tablePrep_ = tablePrep;
    }

    /**
     * Returns the current table preparation object, if any.
     * By default, null is returned.
     *
     * @return   table preparation, or null
     */
    public TablePreparation getPreparation() {
        return tablePrep_;
    }

    /**
     * Sets an object that can control access to input data.
     * If a non-null value is set, then any attempt to read a table
     * from a resource such as a file or URL will first test it
     * using the supplied predicate.
     * If its <code>test</code> method returns false,
     * the table read attempt will fail with an IOException.
     *
     * @param  restriction  policy for restricting DataSource access,
     *                      or null for no restrictions
     */
    public void setInputRestriction( Predicate<DataSource> restriction ) {
        inputRestriction_ = restriction;
    }

    /**
     * Returns the object controlling access to input data.
     * By default this returns null, meaning no access controls.
     *
     * @return   policy for restricting DataSource access, or null
     * @see   #setInputRestriction
     */
    public Predicate<DataSource> getInputRestriction() {
        return inputRestriction_;
    }

    /**
     * Returns a table based on a given table and guaranteed to have
     * random access.  If the original table <code>table</code> has random
     * access then it is returned, otherwise a new random access table
     * is built using its data.
     *
     * <p>This convenience method is equivalent to
     * <code>getStoragePolicy().randomTable(table)</code>.
     *
     * @param  table  original table
     * @return  a table with the same data as <code>table</code> and with
     *          <code>isRandom()==true</code>
     */
    public StarTable randomTable( StarTable table ) throws IOException {
        return getStoragePolicy().randomTable( table );
    }

    /**
     * Constructs a <code>StarTable</code> from a <code>DataSource</code>
     * object using automatic format detection.
     *
     * @param  datsrc  the data source containing the table data
     * @return a new StarTable view of the resource <code>datsrc</code>
     * @throws TableFormatException if none of the default handlers
     *         could turn <code>datsrc</code> into a table
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( DataSource datsrc )
            throws TableFormatException, IOException {
        checkDataSource( datsrc );
        List<TableBuilder> builders = getTableBuilders( datsrc );
        for ( TableBuilder builder : builders ) {
            try {
                StarTable startab =
                    builder.makeStarTable( datsrc, requireRandom(),
                                           getStoragePolicy() );
                startab = prepareTable( startab, builder );
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
        Iterator<TableBuilder> it = builders.iterator();
        if ( it.hasNext() ) {
            msg.append( " (tried" );
            while ( it.hasNext() ) {
                msg.append( " " )
                   .append( it.next().getFormatName() );
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
     * implement {@link MultiTableBuilder}) will be capable of returning
     * a sequence having more than one element.
     *
     * @param  datsrc  the data source containing the table data
     * @return   a sequence of tables loaded from <code>datsrc</code>
     * @throws TableFormatException if none of the default handlers
     *         could turn <code>datsrc</code> into a table
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( DataSource datsrc )
            throws TableFormatException, IOException {
        checkDataSource( datsrc );
        List<TableBuilder> builders = getTableBuilders( datsrc );
        for ( TableBuilder builder : builders ) {
            try {
                if ( builder instanceof MultiTableBuilder ) {
                    MultiTableBuilder mbuilder = (MultiTableBuilder) builder;
                    TableSequence tseq =
                        mbuilder
                       .makeStarTables( datsrc, getStoragePolicy() );
                    String nameBase = datsrc.getName() + "-";
                    return prepareTableSequence( tseq, nameBase, mbuilder );
                }
                else {
                    StarTable startab =
                        builder.makeStarTable( datsrc, requireRandom(),
                                               getStoragePolicy() );
                    startab = prepareTable( startab, builder );
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
        Iterator<TableBuilder> it = builders.iterator();
        if ( it.hasNext() ) {
            msg.append( " (tried" );
            while ( it.hasNext() ) {
                msg.append( " " )
                   .append( it.next().getFormatName() );
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
     * Constructs a <code>StarTable</code> from a location string
     * without format specification.
     * The location string can represent a filename or URL,
     * or a scheme-based specification of the form
     * <code>&lt;scheme&gt;:&lt;scheme-spec&gt;</code>
     * corresponding to one of the installed {@link #getSchemes schemes}.
     *
     * @param  location  the name of the table resource
     * @return a new StarTable view of the resource at <code>location</code>
     * @throws TableFormatException if no handler capable of turning
     *        <code>location</code> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     */
    public StarTable makeStarTable( String location )
            throws TableFormatException, IOException {
        String[] schemeLoc = parseSchemeLocation( location );
        if ( schemeLoc != null ) {
            TableScheme scheme = schemes_.get( schemeLoc[ 0 ] );
            if ( scheme != null ) {
                return createSchemeTable( location, scheme );
            }
            else {
                return makeStarTable( schemeSyntaxDataSource( location ) );
            }
        }
        else {
            return makeStarTable( DataSource.makeDataSource( location ) );
        }
    }

    /**
     * Constructs a <code>StarTable</code> from a URL using
     * automatic format detection.
     *
     * @param  url  the URL where the table lives
     * @return a new StarTable view of the resource at <code>url</code>
     * @throws TableFormatException if no handler capable of turning
     *        <code>datsrc</code> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     * @deprecated  Use <code>makeStarTable(new URLDataSource(url))</code>
     */
    @Deprecated
    public StarTable makeStarTable( URL url ) throws IOException {
        return makeStarTable( new URLDataSource( url ) );
    }

    /**
     * Constructs a <code>StarTable</code> from a <code>DataSource</code>
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <code>null</code> or the empty string or
     * the special value {@link #AUTO_HANDLER} is
     * supplied for <code>handler</code>, it will fall back on automatic
     * format detection.
     *
     * @param  datsrc  the data source containing the table data
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource <code>datsrc</code>
     * @throws TableFormatException  if <code>datsrc</code> does not contain
     *         a table in the format named by <code>handler</code>
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( DataSource datsrc, String handler )
            throws TableFormatException, IOException {
        checkDataSource( datsrc );
        if ( handler == null || handler.trim().length() == 0 ||
             handler.equals( AUTO_HANDLER ) ) {
            return makeStarTable( datsrc );
        }
        TableBuilder builder = getTableBuilder( handler );
        StarTable startab;
        try {
            startab = builder.makeStarTable( datsrc, requireRandom(),
                                             getStoragePolicy() );
            startab = prepareTable( startab, builder );
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
     * handler previously.  If <code>null</code> or the empty string or
     * the special value {@link #AUTO_HANDLER} is
     * supplied for <code>handler</code>, it will fall back on automatic
     * format detection.
     *
     * <p>If the handler does not implement the {@link MultiTableBuilder}
     * interface, then the returned sequence will contain a single table.
     *
     * @param  datsrc  the data source containing the table data
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a sequence of StarTables loaded from <code>datsrc</code>
     * @throws TableFormatException  if <code>datsrc</code> does not contain
     *         a table in the format named by <code>handler</code>
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( DataSource datsrc, String handler )
            throws TableFormatException, IOException {
        checkDataSource( datsrc );
        if ( handler == null || handler.trim().length() == 0 ||
             handler.equals( AUTO_HANDLER ) ) {
            return makeStarTables( datsrc );
        }
        TableBuilder builder = getTableBuilder( handler );
        StarTable[] startabs;
        try {
            if ( builder instanceof MultiTableBuilder ) {
                MultiTableBuilder mbuilder = (MultiTableBuilder) builder;
                TableSequence tseq =
                    mbuilder
                   .makeStarTables( datsrc, getStoragePolicy() );
                String nameBase = datsrc.getName() + "-";
                return prepareTableSequence( tseq, nameBase, mbuilder );
            }
            else {
                StarTable startab =
                    builder.makeStarTable( datsrc, requireRandom(),
                                           getStoragePolicy() );
                startab = prepareTable( startab, builder );
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
     * Constructs a sequence of <code>StarTable</code>s from a location string
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <code>null</code> or the empty string or
     * the special value {@link #AUTO_HANDLER} is
     * supplied for <code>handler</code>, it will fall back on automatic
     * format detection.
     *
     * <p>Alternatively, the location string can be a
     * scheme-based specification, in which case the <code>handler</code>
     * is ignored.
     *
     * @param  location  the name of the table resource
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <code>location</code>
     * @throws TableFormatException  if <code>location</code> does not point to
     *         a table in the format named by <code>handler</code>
     * @throws IOException  if an I/O error is encountered
     */
    public TableSequence makeStarTables( String location, String handler )
            throws TableFormatException, IOException {
        String[] schemeLoc = parseSchemeLocation( location );
        if ( schemeLoc != null ) {
            TableScheme scheme = schemes_.get( schemeLoc[ 0 ] );
            if ( scheme != null ) {
                StarTable table = createSchemeTable( location, scheme );
                return Tables.singleTableSequence( table );
            }
            else {
                return makeStarTables( schemeSyntaxDataSource( location ),
                                       handler );
            }
        }
        else {
            return makeStarTables( DataSource.makeDataSource( location ),
                                   handler );
        }
    }

    /**
     * Constructs a <code>StarTable</code> from a location string
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <code>null</code> or the empty string or
     * the special value {@link #AUTO_HANDLER} is
     * supplied for <code>handler</code>, it will fall back on automatic
     * format detection.
     *
     * <p>A location of "-" means standard input - in this case
     * the handler must be specified.
     *
     * <p>Alternatively, the location string can be a
     * scheme-based specification, in which case the <code>handler</code>
     * is ignored.
     *
     * @param  location  the name of the table resource
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <code>location</code>
     * @throws TableFormatException  if <code>location</code> does not point to
     *         a table in the format named by <code>handler</code>
     * @throws IOException  if an I/O error is encountered
     */
    public StarTable makeStarTable( String location, String handler )
            throws TableFormatException, IOException {
        if ( "-".equals( location ) ) {
            return makeStarTable( System.in, getTableBuilder( handler ) );
        }
        else {
            String[] schemeLoc = parseSchemeLocation( location );
            if ( schemeLoc != null ) {
                TableScheme scheme = schemes_.get( schemeLoc[ 0 ] );
                if ( scheme != null ) {
                    return createSchemeTable( location, scheme );
                }
                else {
                    return makeStarTable( schemeSyntaxDataSource( location ),
                                          handler );
                }
            }
            else {
                return makeStarTable( DataSource.makeDataSource( location ),
                                      handler );
            }
        }
    }

    /**
     * Constructs a <code>StarTable</code> from a URL
     * using a named table input handler.
     * The input handler may be named either using its format name
     * (as returned from the {@link TableBuilder#getFormatName} method)
     * or by giving the full class name of the handler.  In the latter
     * case this factory does not need to have been informed about the
     * handler previously.  If <code>null</code> or the empty string or
     * the special value {@link #AUTO_HANDLER} is
     * supplied for <code>handler</code>, it will fall back on automatic
     * format detection.
     *
     * @param  url  the URL where the table lives
     * @param  handler  specifier for the handler which can handle tables
     *         of the right format
     * @return a new StarTable view of the resource at <code>url</code>
     * @throws TableFormatException  if the resource at <code>url</code> cannot
     *         be turned into a table by <code>handler</code>
     * @throws IOException  if an I/O error is encountered
     * @deprecated  Use
     *         <code>makeStarTable(new URLDataSource(url),handler)</code>
     */
    @Deprecated
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
     * @param  builder   handler which understands the data in <code>in</code>
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
        return prepareTable( store.getStarTable(), builder );
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
     *     <code>DataSource</code> constructor
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
            Class<?> clazz = flavor.getRepresentationClass();

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
                for ( TableBuilder builder : defaultBuilders_ ) {
                    if ( builder.canImport( flavor ) ) {
                        Object data;
                        try {
                            data = trans.getTransferData( flavor );
                        }
                        catch ( UnsupportedFlavorException e ) {
                            throw new RuntimeException(
                                "DataFlavor " + flavor +
                                " support withdrawn?" );
                        }
                        if ( data instanceof InputStream ) {
                            return makeStarTable( (InputStream) data, builder );
                        }
                        else {
                            throw new RuntimeException( "Flavour lies?" );
                        }
                    }
                }
            }
        }

        /* No luck. */
        throw new TableFormatException( msg.toString() );
    }

    /**
     * Indicates whether a particular set of <code>DataFlavor</code> ojects
     * offered by a {@link java.awt.datatransfer.Transferable}
     * is suitable for attempting to turn the <code>Transferable</code>
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
            Class<?> clazz = flavor.getRepresentationClass();
            if ( clazz.equals( URL.class ) ) {
                return true;
            }
            else {
                for ( TableBuilder builder : defaultBuilders_ ) {
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
     * @return  TableBuilder specified by <code>name</code>
     * @throws  TableFormatException  if <code>name</code> doesn't name any
     *          available handler
     */
    public TableBuilder getTableBuilder( String name )
            throws TableFormatException {
        if ( name == null ) {
            throw new TableFormatException( "No table handler with null name" );
        }

        /* Try all the known handlers, matching against format name. */
        for ( TableBuilder builder : knownBuilders_ ) {
            if ( builder.getFormatName().equalsIgnoreCase( name ) ) {
                return builder;
            }
        }

        /* See if it's a dynamically created builder; the basic name
         * may be either a builder name or a TableBuilder classname,
         * and an optional configuration parenthesis may be appended. */
        BeanConfig config = BeanConfig.parseSpec( name );
        String cname = config.getBaseText();
        Class<? extends TableBuilder> clazz = getBuilderClass( cname );
        if ( clazz != null ) {
            TableBuilder tbuilder;
            try {
                tbuilder = clazz.getDeclaredConstructor().newInstance();
            }
            catch ( ReflectiveOperationException e ) {
                throw new TableFormatException( "Can't instantiate class "
                                              + clazz.getName(), e );
            }
            try {
                config.configBean( tbuilder );
            }
            catch ( LoadException e ) {
                throw new TableFormatException( "Handler configuration failed: "
                                              + e, e );
            }
            return tbuilder;
        }

        /* Failed to find any handler for name. */
        throw new TableFormatException( "No table handler available for "
                                      + name );
    }

    /**
     * Returns the TableBuilder subclass corresponding to a given
     * specified name.  This may be a classname or the name of one of
     * the handlers known to this factory.
     *
     * @param  name  class name or label
     * @return  class, or null if nothing suitable is found
     */
    private Class<? extends TableBuilder> getBuilderClass( String name )
            throws TableFormatException {
        for ( TableBuilder builder : knownBuilders_ ) {
            if ( builder.getFormatName().equalsIgnoreCase( name ) ) {
                return builder.getClass();
            }
        }
        Class<?> clazz;
        try {
            clazz = Class.forName( name );
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }
        if ( TableBuilder.class.isAssignableFrom( clazz ) ) {
            return clazz.asSubclass( TableBuilder.class ); 
        }
        else {
            throw new TableFormatException( "Class " + clazz.getName()
                                          + " does not implement TableBuilder");
        }
    }

    /**
     * Returns a list of TableBuilders that are worth trying to read
     * data from a given DataSource.
     *
     * @param  datsrc   data source
     * @return  list of candidate table builders
     */
    private List<TableBuilder> getTableBuilders( DataSource datsrc ) {

        /* Include first of all the default builder list; these can
         * identify tables by magic number, so will succeed if the
         * table can be interpreted in their format.
         * You might think it was a good idea to restrict or reorder this
         * list on the basis of source name, but it's not, since the
         * original order of that list means that e.g. a colfits is
         * interpreted as a colfits even though it's also a FITS.
         * Hence the looksLikeFile method is never used for builders
         * in the default list. */ 
        List<TableBuilder> list =
            new ArrayList<TableBuilder>( defaultBuilders_ );

        /* Then look at the filename/location indicated by the datasource;
         * if one of the known handlers recognises the name, try that one too.*/
        TableBuilder locBuilder =
            getBuilderByLocation( knownBuilders_, datsrc.getName() );
        if ( locBuilder == null ) {
            URL url = datsrc.getURL();
            if ( url != null ) {
                locBuilder =
                    getBuilderByLocation( knownBuilders_, url.toString() );
            }
        }
        if ( locBuilder != null ) {
            list.add( locBuilder );
        }
        return list;
    }

    /**
     * Tries to identify a TableBuilder that recognises the given location.
     * Compression suffixes are stripped from the given location string.
     *
     * @param   builders   list of candidate TableBuilders
     * @param   loc      table location/filename; compression suffixes etc
     *                   may be included, but will be ignored
     * @return   a TableBuilder that declares itself (probably) suitable
     *           for use with the given location, or null if none do
     */
    private static TableBuilder
            getBuilderByLocation( List<TableBuilder> builders, String loc ) {
        if ( loc != null ) {
            loc = loc.replaceFirst( "[.](gz|Z|bz2|bzip2|gzip)$", "" );
            for ( TableBuilder builder : builders ) {
                if ( builder.looksLikeFile( loc ) ) {
                    return builder;
                }
            }
        }
        return null;
    }

    /**
     * Prepares a table for return from one of the makeStarTable methods.
     * Currently what this does is to randomise it if it needs randomising.
     *
     * @param  startab  table to prepare
     * @param  builder   table builder
     * @return  prepared table - may be <code>startab</code> or a new one
     */
    private StarTable prepareTable( StarTable startab, TableBuilder builder )
            throws IOException {
        if ( requireRandom() ) {
            startab = randomTable( startab );
        }
        if ( tablePrep_ != null ) {
            startab = tablePrep_.prepareLoadedTable( startab, builder );
        }
        return startab;
    }

    /**
     * Prepares a sequence of tables for return from one of the makeStarTables
     * methods.  As well as calling {@link #prepareTable}, it adjusts the
     * tables names if appropriate.
     *
     * @param  tseq  input sequence
     * @param  nameBase  stem of table name
     * @param  builder   table builder
     * @return  output sequence
     */
    private TableSequence
            prepareTableSequence( final TableSequence tseq,
                                  final String nameBase,
                                  final MultiTableBuilder builder ) {
        return new TableSequence() {
            private int index;
            public StarTable nextTable() throws IOException {
                StarTable table = tseq.nextTable();
                if ( table == null ) {
                    return null;
                }
                else {
                    index++;
                    table = prepareTable( table, builder );
                    if ( table.getName() == null ) {
                        table.setName( nameBase + index );
                    }
                    return table;
                }
            }
        };
    }

    /**
     * Ensures that access is permitted to the given data source.
     * If access has been blocked, an exception will be thrown.
     *
     * @param   datsrc  data source to check
     * @throws  IOException  if access is blocked to <code>datsrc</code>
     */
    private void checkDataSource( DataSource datsrc ) throws IOException {
        if ( inputRestriction_ != null &&
             ! inputRestriction_.test( datsrc ) ) {
            throw new IOException( "Access blocked to data at " + datsrc );
        }
    }

    /**
     * Attempts to turn a location which apparently refers to a table Scheme
     * into a DataSource.  This will usually fail, since a location with
     * scheme syntax is presumably supposed to define a scheme-based table,
     * but this method is provided for those cases where that does not
     * seem to be the case, just in case the location string instead
     * refers to a strangely-named file.  This method essensially just
     * calls <code>DataSource.makeDataSource(location)</code>,
     * however in (the expected) case of failure it throws an exception with
     * an informative error message about schemes rather than about files.
     *
     * @param  location  full location string, assumed to have scheme syntax
     * @return  data source referencing actual data
     * @throws  FileNotFoundException  if no such file/URL
     */
    private DataSource schemeSyntaxDataSource( String location )
            throws IOException {
        try {
            return DataSource.makeDataSource( location );
        }
        catch ( FileNotFoundException e ) {
            String msg = new StringBuffer()
                .append( "No such scheme \"" )
                .append( parseSchemeLocation( location )[ 0 ] )
                .append( "\" - known schemes: " )
                .append( schemes_.keySet() )
                .toString();
            throw new FileNotFoundException( msg );
        }
    }

    /**
     * Constructs and prepares a table from a location string using its scheme.
     *
     * @param  location  full location string
     * @param  scheme  non-null scheme which must correspond to the
     *                 scheme named in the location
     * @return  table ready for return from this factory
     */
    private StarTable createSchemeTable( String location, TableScheme scheme )
            throws IOException {
        String schemeName = scheme.getSchemeName();
        String[] parts = parseSchemeLocation( location );
        if ( parts == null ) {
            throw new IllegalArgumentException( "Location \"" + location + "\""
                                              + " is not of form "
                                              + ":<scheme-name>:<spec>" );
        }
        else if ( ! schemeName.equals( parts[ 0 ] ) ) {
            throw new IllegalArgumentException( "Location \"" + location + "\""
                                              + " is not of form "
                                              + ":" + schemeName + ":<spec>" );
        }
        String spec = parts[ 1 ];
        final StarTable table;
        try {
            table = scheme.createTable( spec );
        }
        catch ( TableFormatException e ) {
            String msg = new StringBuffer()
                .append( "Bad format for " )
                .append( ":" )
                .append( schemeName )
                .append( ":" )
                .append( scheme.getSchemeUsage() )
                .append( " (was \"" )
                .append( location )
                .append( "\"" )
                .toString();
            throw new TableFormatException( msg, e );
        }
        return prepareTable( table, null );
    }

    /**
     * Parses a scheme-format table specification as a scheme name
     * and a scheme-specific part.
     * Normally schemes are of the form
     * ":&lt;scheme-name&gt;:&lt;scheme-specific-part&gt;",
     * but as a special case the initial colon may be omitted for JDBC
     * (backward compatibility).
     *
     * @param   location  table specification
     * @return   if the location is syntactically a scheme,
     *           a 2-element array giving [scheme-name,scheme-specific-part];
     *           otherwise null
     */
    public static String[] parseSchemeLocation( String location ) {
        if ( location.startsWith( "jdbc:" ) ) {
            return new String[] { "jdbc", location.substring( 5 ) };
        }
        else {
            Matcher matcher = SCHEME_REGEX.matcher( location );
            return matcher.matches()
                 ? new String[] { matcher.group( 1 ), matcher.group( 2 ) }
                 : null;
        }
    }

    /**
     * Turns an array of classnames into an array of instances of those
     * classes.  A no-arg constructor is required.
     * Behaviour is logged.
     *
     * @param  classNames  names of classes to instantiate
     * @param  type    required type of instances
     * @return  mutable list of instances
     */
    private static <T> List<T> listFromClassNames( String[] classNames,
                                                   Class<T> type ) {
        List<T> list = new ArrayList<>();
        for ( String cname : classNames ) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends T> clazz =
                    (Class<? extends T>) Class.forName( cname );
                T instance = clazz.getDeclaredConstructor().newInstance();
                list.add( instance );
                logger.config( cname + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.info( cname + " not found - can't register" );
            }
            catch ( Throwable e ) {
                logger.log( Level.WARNING,
                            "Failed to register " + cname + " - " + e, e );
            }
        }
        return list;
    }
}
