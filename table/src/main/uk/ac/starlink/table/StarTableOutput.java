package uk.ac.starlink.table;

import java.awt.datatransfer.Transferable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.formats.AsciiTableWriter;
import uk.ac.starlink.table.formats.CsvTableWriter;
import uk.ac.starlink.table.formats.HTMLTableWriter;
import uk.ac.starlink.table.formats.LatexTableWriter;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.util.Loader;

/**
 * Outputs StarTable objects.
 * This object delegates the actual writing to one of a list of 
 * format-specific writer objects whose content can be configured
 * externally.
 *
 * By default, if the corresponding classes are present, the following
 * handlers are installed:
 * <ul>
 * <li> {@link uk.ac.starlink.votable.FitsPlusTableWriter}
 * <li> {@link uk.ac.starlink.fits.FitsTableWriter}
 * <li> {@link uk.ac.starlink.votable.VOTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.TextTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.AsciiTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.CsvTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.HTMLTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.LatexTableWriter}
 * <li> {@link uk.ac.starlink.mirage.MirageTableWriter}
 * </ul>
 * Additionally, any classes named in the <tt>startable.writers</tt>
 * system property (as a colon-separated list) which implement the
 * {@link StarTableWriter} interface and have a no-arg constructor will be
 * instantiated and added to this list of handlers.
 *
 * <p>It can additionally write to JDBC tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableOutput {

    private List handlers;
    private JDBCHandler jdbcHandler;
    private static String[] defaultHandlerClasses = {
        "uk.ac.starlink.votable.FitsPlusTableWriter",
        "uk.ac.starlink.fits.FitsTableWriter",
        "uk.ac.starlink.votable.VOTableWriter",
        TextTableWriter.class.getName(),
        AsciiTableWriter.class.getName(),
        CsvTableWriter.class.getName(),
        HTMLTableWriter.class.getName(),
        LatexTableWriter.class.getName(),
        "uk.ac.starlink.mirage.MirageTableWriter",
    };
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );

    private StarTableWriter voWriter;
    private Method voWriteMethod;

    /**
     * System property which can contain a list of {@link StarTableWriter}
     * classes for addition to the list of known output handlers.
     */
    public static final String EXTRA_WRITERS_PROPERTY = "startable.writers";

    /**
     * Constructs a StarTableOutput with a default list of handlers.
     */
    public StarTableOutput() {
        handlers = new ArrayList();

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultHandlerClasses.length; i++ ) {
            String className = defaultHandlerClasses[ i ];
            try {
                Class clazz = this.getClass().forName( className );

                /* See if the class provides a method which can return
                 * a list of handlers. */
                StarTableWriter[] writers = null;
                try {
                    Method getList = clazz.getMethod( "getStarTableWriters", 
                                                       new Class[ 0 ] );
                    int mods = getList.getModifiers();
                    if ( Modifier.isStatic( mods ) && 
                         Modifier.isPublic( mods ) ) {
                        Class retClass = getList.getReturnType();
                        if ( retClass.isArray() && 
                             StarTableWriter.class
                            .isAssignableFrom( retClass.getComponentType() ) ) {
                            writers = (StarTableWriter[]) 
                                      getList.invoke( null, new Object[ 0 ] );
                        }
                    }
                }
                catch ( NoSuchMethodException e ) {
                    // no problem
                }

                /* If we got a list, use that. */
                if ( writers != null ) {
                    for ( int j = 0; j < writers.length; j++ ) {
                        StarTableWriter handler = writers[ j ];
                        handlers.add( handler );
                        logger.config( "Handler " + handler.getFormatName() +
                                       " registered" );
                    }
                }

                /* Otherwise, just instantiate the class with a no-arg
                 * constructor. */
                else {
                    StarTableWriter handler = 
                        (StarTableWriter) clazz.newInstance();
                    handlers.add( handler );
                    logger.config( "Handler " + handler.getFormatName() +
                                   " registered" );
                }
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( "Failed to register " + className + " - " + e );
            }
        }

        /* Add any further handlers specified by system property. */
        handlers.addAll( Loader.getClassInstances( EXTRA_WRITERS_PROPERTY,
                                                   StarTableWriter.class ) );

        /* Further initialization. */
        initializeForTransferables();
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
     *         a string which matches the format name of one of the registered 
     *         <tt>StarTableWriter</tt>s (first match is used, 
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> to indicate that a handler should be 
     *         selected based on the value of <tt>location</tt>.
     *         Ignored for <tt>jdbc:</tt>-protocol locations
     * @throws TableFormatException  if no suitable handler is known
     */
    public void writeStarTable( StarTable startab, String location,
                                String format )
            throws TableFormatException, IOException {

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

        /* Otherwise dispatch the job to a suitable handler. */
        else {
            getHandler( format, location )
           .writeStarTable( startab, location, this );
        }
    }

    /**
     * Writes a StarTable to an output stream.
     * This convenience method wraps the stream in a BufferedOutputStream
     * for efficiency and uses the submitted <tt>handler</tt> to perform
     * the write, closing the stream afterwards.
     *
     * @param  startab   table to write
     * @param  out       raw output stream
     * @param  handler   output handler
     * @see  #getHandler
     */
    public void writeStarTable( StarTable startab, OutputStream out,
                                StarTableWriter handler ) throws IOException {
        try {
            if ( ! ( out instanceof BufferedOutputStream ) ) {
                out = new BufferedOutputStream( out );
            }
            handler.writeStarTable( startab, out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    /**
     * Returns an output stream which points to a given location.
     * Typically <tt>location</tt> is a filename and a corresponding
     * <tt>FileOutputStream</tt> is returned, but there may be other
     * possibilities.  The stream returned by this method will not
     * in general be buffered; for high performance writes, wrapping it
     * in a {@link java.io.BufferedOutputStream} may be a good idea.
     *
     * @param   location  name of destination
     * @return   output stream which writes to <tt>location</tt>
     * @throws  IOException  if no stream pointing to <tt>location</tt>
     *          can be opened
     */
    public OutputStream getOutputStream( String location ) throws IOException {
        if ( location.equals( "-" ) ) {
            return System.out;
        }
        else {
            File file = new File( location );

            /* If the file exists, attempt to delete it before attempting
             * to write to it.  This is potentially important since we 
             * don't want to scribble all over a file which may already 
             * be mapped - quite likely if we're overwriting mapped a 
             * FITS file.
             * On POSIX deleting (unlinking) a mapped file will keep its data
             * safe until it's unmapped.  On Windows, deleting it will 
             * fail - in this case we throw an exception. */
            if ( file.exists() ) {
                if ( file.delete() ) {
                    logger.info( "Deleting file \"" + location + 
                                 "\" prior to overwriting" );
                }
                else {
                    throw new IOException( "Can't delete \"" + location + 
                                           "\" prior to overwriting" );
                }
            }

            /* Return a new stream which will write to the file. */
            return new FileOutputStream( file );
        }
    }

    /**
     * Returns a StarTableWriter object given a format to write and a location
     * to write to.  Returns null if none can be found.
     *
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered
     *         <tt>StarTableWriter</tt>s (first match is used,
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> to indicate that a handler should be
     *         selected based on the value of <tt>location</tt>.
     * @param  location  destination of the table to be written.
     *         If <tt>format</tt> is null, the value of this will be used
     *         to try to determine which handler to use, typically on the
     *         basis of filename extension
     * @throws TableFormatException  if no handler suitable for the arguments
     *         can be found
     * @return a suitable output handler
     */
    public StarTableWriter getHandler( String format, String location ) 
            throws TableFormatException {

        /* Do we have a format string? */
        if ( format != null && format.length() > 0 ) {
        
            /* See if the format is the class name of a StarTableWriter. */
            try {
                Class fcls = this.getClass().forName( format );
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
                if ( handler.getFormatName().toLowerCase()
                            .startsWith( format.toLowerCase() ) ) { 
                    return handler;
                }
            }

            /* No luck - throw an exception. */
            throw new TableFormatException( "No handler for table format \"" +
                                            format + "\"" );
        }

        /* If no format has been specified, offer it to the first handler 
         * which likes the look of its filename. */
        else {
            for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
                StarTableWriter handler = (StarTableWriter) it.next();
                if ( handler.looksLikeFile( location ) ) {
                    return handler;
                }
            }

            /* None of them do - failure. */
            StringBuffer msg = new StringBuffer();
            msg.append( "No handler specified for writing table.\n" )
               .append( "Known formats: " );
            for ( Iterator it = getKnownFormats().iterator(); it.hasNext(); ) {
                msg.append( it.next() );
                if ( it.hasNext() ) {
                    msg.append( ", " );
                }
            }
            throw new TableFormatException( msg.toString() );
        }
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

    /**
     * Returns a <tt>Transferable</tt> object associated with a given
     * StarTable, for use at the drag end of a drag and drop operation.
     *
     * @param  startab  the table which is to be dragged
     * @see  StarTableFactory#makeStarTable(java.awt.datatransfer.Transferable)
     */
    public Transferable transferStarTable( final StarTable startab ) {
        if ( voWriteMethod != null ) {
            return new StarTableTransferable( this, startab );
        }
        else {
            return null;
        }
    }

    /**
     * Sets up one serializer suitable for streaming objects during
     * drag and drop.  Has to use reflection, since it uses VOTable classes,
     * which are probably not around at compile time.
     */
    private void initializeForTransferables() {

        /* Identify one which is suitable for serializing for transferables. */
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            StarTableWriter handler = (StarTableWriter) it.next();
            if ( handler.getFormatName().equals( "votable-binary-inline" ) ) {
                try {
                    Class[] args = new Class[] { StarTable.class,
                                                 OutputStream.class };
                    voWriteMethod = handler.getClass()
                                   .getMethod( "writeStarTable", args );
                    voWriter = handler;
                }
                catch ( NoSuchMethodException e ) {
                    voWriter = null;
                }
            }
        }
        if ( voWriteMethod == null ) {
            logger.warning( "No transferable serializer found" );
        }
    }

    void transferTable( StarTable table, OutputStream ostrm ) 
            throws IOException {
        try {
            voWriteMethod.invoke( voWriter, new Object[] { table, ostrm } );
        }
        catch ( InvocationTargetException e ) {
            Throwable target = e.getTargetException();
            if ( target instanceof IOException ) {
                throw (IOException) target;
            }
            else {
                System.err.println( "Reflection trouble!" );
                target.printStackTrace( System.err );
            }
        }
        catch ( Exception e ) {
            e.printStackTrace( System.err );
        }
    }
}
