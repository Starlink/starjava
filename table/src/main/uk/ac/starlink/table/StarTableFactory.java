package uk.ac.starlink.table;

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
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableFactory {

    private List builders;
    private JDBCHandler jdbcHandler;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );
    private static String[] defaultBuilderClasses = { 
        "uk.ac.starlink.fits.FitsTableBuilder",
        "uk.ac.starlink.votable.VOTableBuilder",
        "uk.ac.starlink.table.formats.WDCTableBuilder" };

    /**
     * Constructs a StarTableFactory with a default list of builders.
     */
    public StarTableFactory() {
        builders = new ArrayList( 2 );

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultBuilderClasses.length; i++ ) {
            String className = defaultBuilderClasses[ i ];
            try {
                Class clazz = Class.forName( className );
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
            StarTable startab = builder.makeStarTable( datsrc );
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
            return getJDBCHandler().makeStarTable( location );
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
