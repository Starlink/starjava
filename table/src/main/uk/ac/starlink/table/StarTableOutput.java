package uk.ac.starlink.table;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.jdbc.JDBCHandler;

/**
 * Outputs StarTable objects.
 * This object delegates the actual writing to one of a list of 
 * format-specific writer objects whose content can be configured
 * externally.
 *
 * By default, if the corresponding classes are present, the following
 * handlers are installed:
 * <ul>
 * <li> {@link uk.ac.starlink.fits.FitsTableWriter}
 * <li> {@link uk.ac.starlink.votable.VOTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.TextTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.LatexTableWriter}
 * <li> {@link uk.ac.starlink.mirage.MirageTableWriter}
 * </ul>
 * It can additionally write to JDBC tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableOutput {

    private List handlers;
    private JDBCHandler jdbcHandler;
    private static String[] defaultHandlerClasses = {
        "uk.ac.starlink.fits.FitsTableWriter",
        "uk.ac.starlink.votable.VOTableWriter",
        "uk.ac.starlink.table.formats.TextTableWriter",
        "uk.ac.starlink.table.formats.LatexTableWriter",
        "uk.ac.starlink.mirage.MirageTableWriter",
    };
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );


    /**
     * Constructs a StarTableOutput with a default list of handlers.
     */
    public StarTableOutput() {
        handlers = new ArrayList( 4 );

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultHandlerClasses.length; i++ ) {
            String className = defaultHandlerClasses[ i ];
            try {
                Class clazz = Class.forName( className );
                StarTableWriter handler = (StarTableWriter) clazz.newInstance();
                handlers.add( handler );
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
     * Gets the list of handlers which can actually do table output.
     * Handlers earlier in the list are given a chance to write the
     * table before ones later in the list.
     *
     * @return  handlers  an array of <tt>StarTableWriter</tt> objects 
     */
    public List getHandlers() {
        return handlers;
    }

    /**
     * Sets the list of handlers which can actually do table output.
     * Handlers earlier in the list are given a chance to write the
     * table before ones later in the list.
     *
     * @param  an array of <tt>StarTableWriter</tt> objects
     */
    public void setHandlers( StarTableWriter[] handlers ) {
        this.handlers = new ArrayList( Arrays.asList( handlers ) );
    }

    /**
     * Writes a <tt>StarTable</tt> object out to some external storage.
     * The format in which it is written is determined by some
     * combination of the given output location and a format indicator.
     *
     * @param  startab  the table to output
     * @param  location the location at which to write the new table.
     *         This may be a filename or URL, including a <tt>jdbc:</tt>
     *         protocol if suitable JDBC drivers are installed
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be 
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches one of the registered 
     *         <tt>StarTableWriter</tt>s (first match is used),
     *         or <tt>null</tt> to indicate that a handler should be 
     *         selected based on the value of <tt>location</tt>.
     *         Ignored for <tt>jdbc:</tt>-protocol locations
     * @throws UnknownTableFormatException  if no suitable handler is known
     */
    public void writeStarTable( StarTable startab, String location,
                                String format )
            throws UnknownTableFormatException, IOException {

        /* Handle the JDBC case. */
        if ( location.startsWith( "jdbc:" ) ) {
            try {
                getJDBCHandler().createJDBCTable( startab, location );
                return;
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        else {

            /* See if the specified format is suitable for any known handler. */
            StarTableWriter handler = getHandler( location, format );
            if ( handler != null ) {
                handler.writeStarTable( startab, location );
            }

            /* If not, throw an UnknownTableFormatException. */
            else {
                if ( format != null ) {
                    throw new UnknownTableFormatException(
                        "No handler for table format " + format );
                }
                else {
                    StringBuffer msg = new StringBuffer();
                    msg.append( "No handler specified for writing table.\n" )
                       .append( "Known formats: " );
                    for ( Iterator it = getKnownFormats().iterator();
                          it.hasNext(); ) {
                        msg.append( it.next() );
                        if ( it.hasNext() ) {
                            msg.append( ", " );
                        }
                    }
                    throw new UnknownTableFormatException( msg.toString() );
                }
            }
        }
    }
 
    /**
     * Returns a StarTableWriter object given a format to write and a location
     * to write to.  Returns null if none can be found.
     */
    private StarTableWriter getHandler( String location, String format ) {

        /* Do we have a format string? */
        if ( format != null ) {
        
            /* See if the format is the class name of a StarTableWriter. */
            try {
                Class fcls = Class.forName( format );
                if ( StarTableWriter.class.isAssignableFrom( fcls ) ) {

                    /* Is it one of the registered ones? */
                    for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
                        StarTableWriter handler = (StarTableWriter) it.next();
                        if ( fcls.isInstance( handler ) ) {
                            return handler;
                        }
                    }

                    /* Otherwise, can we instantiate it with a no-arg
                     * constructor? */
                    try {
                        StarTableWriter handler = 
                            (StarTableWriter) fcls.newInstance();
                        return handler;
                    }
                    catch ( IllegalAccessException e ) {
                    }
                    catch ( InstantiationException e ) {
                    }
                }
            }
            catch ( ClassNotFoundException e ) {
                // it's not a class name
            }

            /* Otherwise, see if it names an output format. */
            for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
                StarTableWriter handler = (StarTableWriter) it.next();
                if ( handler.getFormatName().equalsIgnoreCase( format ) ) {
                    return handler;
                }
            }
        }

        /* Otherwise, offer it to the first handler which likes the look
         * of its filename. */
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            StarTableWriter handler = (StarTableWriter) it.next();
            if ( handler.looksLikeFile( location ) ) {
                return handler;
            }
        }

        /* No luck. */
        return null;
    }


    /**
     * Returns a list of the format strings which are defined by the
     * handlers registered with this object.  The elements of the returned
     * list can be passed as the <tt>format</tt> argument to the 
     * {@link #writeStarTable} method.
     */
    public List getKnownFormats() {
        List kf = new ArrayList();
        kf.add( "jdbc" );
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            kf.add( ((StarTableWriter) it.next()).getFormatName() );
        }
        return kf;
    }

    /**
     * Returns the JDBCHandler object used for writing tables to JDBC
     * connections.
     *
     * @return  the JDBC handler
     */
    public JDBCHandler getJDBCHandler() {
        if ( jdbcHandler == null ) {
            jdbcHandler = new JDBCHandler();
        }
        return jdbcHandler;
    }

    /**
     * Sets the JDBCHandler object used for writing tables to JDBC 
     * connections.
     *
     * @param  handler  the handler to use
     */
    public void setJDBCHandler( JDBCHandler handler ) {
        this.jdbcHandler = handler;
    }

}
