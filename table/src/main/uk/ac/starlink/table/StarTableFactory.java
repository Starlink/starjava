package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

/**
 * Manufactures {@link StarTable} objects from generic inputs.
 * This factory delegates the actual table creation to external 
 * {@link TableBuilder} objects; the generic input is passed to each one
 * in turn until one can make a <tt>StarTable</tt> from it, which object
 * is returned to the caller.
 * JDBC is also used to create tables under appropriate circumstances.
 * <p>
 * By default, if the corresponding classes are present, the following
 * TableBuilders are installed:
 * <ul>
 * <li> {@link uk.ac.starlink.fits.FitsTableBuilder}
 * <li> {@link uk.ac.starlink.votable.VOTableBuilder}
 * <li> {@link uk.ac.starlink.table.formats.WDCTableBuilder}
 * <li> {@link uk.ac.starlink.table.formats.TextTableBuilder}
 * </ul>
 * <p>
 * The factory has a flag <tt>wantRandom</tt> which determines 
 * whether random-access tables are
 * preferred results of the <tt>makeStarTable</tt> methods.
 * Setting this flag to <tt>true</tt> does <em>not</em> guarantee
 * that returned tables will have random access 
 * (the {@link Tables#randomTable} method should be used for that),
 * but this flag is passed to builders as a hint in case they know
 * how to make either random or non-random tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableFactory {

    private List builders;
    private JDBCHandler jdbcHandler;
    private boolean wantRandom;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );
    private static String[] defaultBuilderClasses = { 
        "uk.ac.starlink.fits.FitsTableBuilder",
        "uk.ac.starlink.votable.VOTableBuilder",
        "uk.ac.starlink.table.formats.WDCTableBuilder",
        "uk.ac.starlink.table.formats.TextTableBuilder",
    };

    /**
     * Constructs a StarTableFactory with a default list of builders
     * which will not preferentially construct random-access tables.
     */
    public StarTableFactory() {
        this( false );
    }

    /**
     * Constructs a StarTableFactory with a default list of builders
     * specifying whether it should preferentially construct random-access
     * tables.
     *
     * @param   wantRandom  whether random-access tables are preferred
     */
    public StarTableFactory( boolean wantRandom ) {
        this.wantRandom = wantRandom;
        builders = new ArrayList( 2 );

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultBuilderClasses.length; i++ ) {
            String className = defaultBuilderClasses[ i ];
            try {
                Class clazz = this.getClass().forName( className );
                TableBuilder builder = (TableBuilder) clazz.newInstance();
                builders.add( builder );
                logger.config( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( "Failed to register " + className + " - " + e );
            }
        }
    }

    /**
     * Gets the list of builders which actually do the table construction.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     * This list may be modified to change the behaviour of the factory.
     *
     * @return  a mutable list of {@link TableBuilder} objects used to
     *          construct <tt>StarTable</tt>s
     */
    public List getBuilders() {
        return builders;
    }

    /**
     * Sets the list of builders which actually do the table construction.
     * Builders earlier in the list are given a chance to make the
     * table before ones later in the list.
     *
     * @param  builders  an array of TableBuilder objects used to 
     *         construct <tt>StarTable</tt>s
     */
    public void setBuilders( TableBuilder[] builders ) {
        this.builders = new ArrayList( Arrays.asList( builders ) );
    }

    /**
     * Sets whether random-access tables are by preference 
     * created by this factory.
     *
     * @param  wantRandom  whether, preferentially, this factory should
     *         create random-access tables
     */
    public void setWantRandom( boolean wantRandom ) {
        this.wantRandom = wantRandom;
    }

    /**
     * Returns the <tt>wantRandom</tt> flag.
     *
     * @return  whether, preferentially, this factory should create 
     *          random-access tables
     */
    public boolean wantRandom() {
        return wantRandom;
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a <tt>DataSource</tt> 
     * object.  
     *
     * @param  datsrc  the data source containing the table data
     * @return a new StarTable view of the resource <tt>datsrc</tt>
     * @throws UnknownTableFormatException if no handler capable of turning
     *        <tt>datsrc</tt> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     */
    public StarTable makeStarTable( DataSource datsrc ) throws IOException {
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            TableBuilder builder = (TableBuilder) it.next();
            StarTable startab = builder.makeStarTable( datsrc, wantRandom );
            if ( startab != null ) {
                if ( startab instanceof AbstractStarTable ) {
                    AbstractStarTable abst = (AbstractStarTable) startab;
                    if ( abst.getName() == null ) {
                        abst.setName( datsrc.getName() );
                    }
                    if ( abst.getURL() == null ) {
                        abst.setURL( datsrc.getURL() );
                    }
                }
                return startab;
            }
        }

        /* None of the handlers was prepared to have a go. */
        StringBuffer msg = new StringBuffer();
        msg.append( "Can't make StarTable from \"" )
           .append( datsrc.getName() )
           .append( "\"" );
        Iterator it = builders.iterator();
        if ( it.hasNext() ) {
            msg.append( "\nTried handlers:\n" );
            while ( it.hasNext() ) {
                msg.append( "    " )
                   .append( it.next() )
                   .append( "\n" );
            }
        }
        else {
            msg.append( " - no table handlers available" );
        }
        throw new UnknownTableFormatException( msg.toString() );
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a location string,
     * which can represent a filename or URL, including a <tt>jdbc:</tt>
     * protocol URL if an appropriate JDBC driver is installed.
     *
     * @param  location  the name of the table resource
     * @return a new StarTable view of the resource at <tt>location</tt>
     * @throws UnknownTableFormatException if no handler capable of turning
     *        <tt>location</tt> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     */
    public StarTable makeStarTable( String location ) throws IOException {
        if ( location.startsWith( "jdbc:" ) ) {
            return getJDBCHandler().makeStarTable( location, wantRandom );
        }
        else {
            return makeStarTable( DataSource.makeDataSource( location ) );
        }
    }

    /**
     * Constructs a readable <tt>StarTable</tt> from a URL.
     *
     * @param  url  the URL where the table lives
     * @return a new StarTable view of the resource at <tt>url</tt>
     * @throws UnknownTableFormatException if no handler capable of turning
     *        <tt>datsrc</tt> into a table is available
     * @throws IOException  if one of the handlers encounters an error
     *         constructing a table
     */
    public StarTable makeStarTable( URL url ) throws IOException {
        return makeStarTable( new URLDataSource( url ) );
    }

    /**
     * Constructs a StarTable from a 
     * {@link java.awt.datatransfer.Transferable} object.  
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
     *     {@link java.util.DataSource} and passes that to the 
     *     <tt>DataSource</tt> constructor
     * </ul>
     * <p>
     * This method doesn't throw an exception if it fails to come up
     * with a StarTable, it merely returns <tt>null</tt>.  This is because
     * with many flavours to choose from, it's not clear which exception
     * ought to get thrown.
     *
     * @param  trans  the Transferable object to construct a table from
     * @return  a new StarTable constructed from the Transferable, or
     *          <tt>null</tt> if it can't be done
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
                catch ( UnknownTableFormatException e ) {
                    msg.append( e.getMessage() );
                }
            }

            /* If we can get a stream, see if any of the builders will
             * take it. */
            if ( InputStream.class.isAssignableFrom( clazz ) && 
                 ! flavor.isFlavorSerializedObjectType() ) {
                for ( Iterator it = builders.iterator(); it.hasNext(); ) {
                    TableBuilder builder = (TableBuilder) it.next();
                    if ( builder.canImport( flavor ) ) {
                        DataSource datsrc = new DataSource() {
                            protected InputStream getRawInputStream()
                                    throws IOException {
                                try {
                                    return (InputStream) 
                                           trans.getTransferData( flavor );
                                }
                                catch ( UnsupportedFlavorException e ) {
                                    throw new RuntimeException(
                                        "DataFlavor " + flavor + 
                                        " support withdrawn?" );
                                }
                            }
                            public URL getURL() {
                                return null;
                            }
                        };
                        StarTable startab =
                            builder.makeStarTable( datsrc, wantRandom );
                        if ( startab != null ) {
                            return startab;
                        }
                        else {
                            msg.append( "Tried: " )
                               .append( builder )
                               .append( " on type " ) 
                               .append( flavor.getPrimaryType() )
                               .append( '/' )
                               .append( flavor.getSubType() )
                               .append( '\n' );
                        }
                    }
                }
            }
        }

        /* No luck. */
        throw new UnknownTableFormatException( msg.toString() );
    }

    /**
     * Indicates whether a particular set of <tt>DataFlavor</tt> ojects
     * offered by a <tt>{@link java.awt.datatransfer.Transferable}
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
                for ( Iterator it = builders.iterator(); it.hasNext(); ) {
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
        if ( jdbcHandler == null ) {
            jdbcHandler = new JDBCHandler();
        }
        return jdbcHandler;
    }
  
}
