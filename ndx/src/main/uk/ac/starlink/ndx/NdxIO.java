package uk.ac.starlink.ndx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.URLUtils;

/**
 * Performs I/O between Ndx objects and resources named by URLs.
 * Methods are available to construct a readable Ndx from a URL
 * pointing to an existing resource, or to construct a new Ndx located 
 * at a given URL and populate it from the content of an existing Ndx.
 * <p>
 * This factory delegates the actual Ndx I/O to external
 * NdxHandler objects; the URL is passed to each one in turn until
 * one recognises the URL as one it can deal with, at which point the
 * rest of the work is handed off to that object.
 * <p>
 * By default, if the corresponding classes are present, the following
 * NdxHandlers are installed:
 * <ul>
 * <li>{@link uk.ac.starlink.hds.NDFNdxHandler}
 * <li>{@link uk.ac.starlink.oldfits.FitsNdxHandler}
 * <li>{@link uk.ac.starlink.ndx.XMLNdxHandler}
 * </ul>
 * Consult the documentation of these classes to find out about the format 
 * of URLs understood by each.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxIO {

    private static List defaultHandlers;
    private List handlers;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    static {
        /*
         * Load system properties from global config file.
         * Specifically, we might need to set http.proxyHost and http.proxyPort.
         */
        Loader.loadProperties();
    }

    /**
     * Constructs an NdxIO with a default list of handlers.
     */
    public NdxIO() {

        /* Construct the default list of handlers if it has not yet been done.
         * (only required first time around). */
        if ( defaultHandlers == null ) {
            defaultHandlers = new ArrayList( 3 );
            Class[] noParams = new Class[ 0 ];
            Object[] noArgs = new Object[ 0 ];
            String className;

            /* Attempt to add an NDFNdxHandler if the class is available. */
            className = "uk.ac.starlink.hds.NDFNdxHandler";
            try {
                Class clazz = this.getClass().forName( className );
                Method meth = clazz.getMethod( "getInstance", noParams );
                NdxHandler handler = (NdxHandler) meth.invoke( null, noArgs );
                defaultHandlers.add( handler );
                logger.config( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            
            /* A LinkageError probably means that the native libraries are
             * not available.  In this case, try to invoke the
             * HDSPackage.isAvailable method to do logging in a standard way. */
            catch ( LinkageError e ) {
                try {
                    this.getClass().forName( "uk.ac.starlink.hds.HDSPackage" )
                        .getMethod( "isAvailable", noParams )
                        .invoke( null, noArgs );
                }
                catch ( Exception e2 ) {
                    logger.config( className + " " + e2 +
                                   " - can't register" );
                }
            }
            
            /* Any other error, just log it directly. */
            catch ( InvocationTargetException e ) {
                logger.config( className + " " + e.getTargetException() 
                               + " - can't register" );
            }
            catch ( Exception e ) {
                logger.config( className + " " + e + " - can't register" );
            }

            /* Attempt to add a FitsNdxHandler if the class is available. */
            className = "uk.ac.starlink.oldfits.FitsNdxHandler";
            try {
                Class clazz = this.getClass().forName( className );
                Method meth = clazz.getMethod( "getInstance", noParams );
                NdxHandler handler = (NdxHandler) meth.invoke( null, noArgs );
                defaultHandlers.add( handler );
                logger.config( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( className + e + " - can't register" );
            }

            /* Add the XMLNdxHandler. */
            defaultHandlers.add( XMLNdxHandler.getInstance() );
        }

        /* Set the handler list for this object from the default list. */
        handlers = new ArrayList( defaultHandlers );
    }

    /**
     * Gets the list of handlers which actually do the URL-&gt;Ndx
     * construction.  Handlers earlier in the list are given a
     * chance to handle a URL before ones later in the list.
     * This list is mutable and may be modified to change the behaviour 
     * of the <tt>NdxIO</tt>.
     *
     * @return   a List of {@link NdxHandler} objects used
     *           for turning URLs into Ndxs
     */
    public List getHandlers() {
        return handlers;
    }

    /**
     * Sets the list of handlers which actually do the URL-&gt;Ndx
     * construction.  Handlers earlier in the list are given a 
     * chance to handle a URL before ones later in the list.
     *
     * @param  handlers  an array of NdxHandler objects used for
     *                   turning URLs into Ndxs
     */
    public void setHandlers( NdxHandler[] handlers ) {
        this.handlers = new ArrayList( Arrays.asList( handlers ) );
    }

    /**
     * Constructs a readable Ndx from a URL representing an exisiting
     * resource.
     * A null result will be returned if none of the available handlers
     * understands the URL; an IOException will result if one of the
     * handlers is willing to handle the URL but fails to find an
     * Ndx resource at it.
     *
     * @param   url  a URL pointing to a resource representing an Ndx
     * @param   mode read/write/update access mode for component arrays
     * @return   a readable Ndx object view of the resource at url, or
     *           null if one could not be found
     * @throws   IOException  if there is any I/O error
     */
    public Ndx makeNdx( URL url, AccessMode mode ) throws IOException {
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            NdxHandler handler = (NdxHandler) it.next();
            Ndx ndx = handler.makeNdx( url, mode );
            if ( ndx != null ) {
                return ndx;
            }
        }
        return null;
    }

    /**
     * Constructs a new Ndx containing writable and uninitialised  
     * array components at the given URL with characteristics 
     * matching those of a given template Ndx.
     * The scalar components will be copied directly from the template,
     * and the new Ndx
     * will have array components matching the array components of
     * the template in shape and type (and whether they exist or not).
     * They may match in point of bad values and ordering scheme, but
     * this is dependent on the details of the output format.
     * The initial values of the created array components are undefined,
     * but they will be writable.  Applications which call this method
     * should in general then open the new NDX using {@link #makeNdx} 
     * and write values to the array components so that it has valid
     * array components.
     * <p>
     * The classes {@link DefaultMutableNdx} and 
     * {@link uk.ac.starlink.array.DummyNDArray} may be useful in 
     * constructing a suitable template Ndx object.
     * <p>
     * The method will return true if the Ndx was created successfully,
     * and false if none of the available handlers is able to handle
     * the URL.  An IOException will result if one of the handlers
     * is willing to handle the URL but encounters some problem when
     * attempting to write the new resource.
     *
     * @param  url  a URL at which the new NDX should be written
     * @param  template   a template Ndx object from which non-array data
     *                    should be initialised - all scalar components
     *                    will be copied from it, and new blank writable
     *                    array components matching the ones in it will be
     *                    created
     * @return  true iff a new blank Ndx was written at <tt>url</tt>
     * @throws  IOException  if there is any I/O error
     */
    public boolean makeBlankNdx( URL url, Ndx template ) throws IOException {
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            NdxHandler handler = (NdxHandler) it.next();
            if ( handler.makeBlankNdx( url, template ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a new resource at a given URL containing an Ndx with data
     * copied from an existing Ndx.
     * An UnsupportedOperationException will result if none  
     * of the available handlers understands the URL; 
     * an IOException will result if one of the
     * handlers is willing to handle the URL but fails to make an Ndx
     * out of it or some error is encountered during the copy.
     *
     * @param   url  a URL pointing to a writable resource.  This may be
     *               a suitable <code>file:</code>-protocol URL or one
     *               with some other protocol which can provide an
     *               output-capable connection
     * @param   ndx  an existing Ndx object whose data will be copied
     *             to <tt>url</tt>
     * @throws IOException  if an I/O error occurs
     * @throws UnsupportedOperationException  no handler exists for this URL
     */
    public void outputNdx( URL url, Ndx ndx )
            throws IOException {
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            NdxHandler handler = (NdxHandler) it.next();
            if ( handler.outputNdx( url, ndx ) ) {
                return;
            }
        }
        throw new UnsupportedOperationException( 
            "No Ndx handler found for URL " + url );
    }


    /**
     * Constructs a readable Ndx from a location representing an existing
     * resource.
     * This convenience method just turns the location into a URL and
     * calls {@link #makeNdx(URL,AccessMode)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *                   parsed as a URL it will be treated as a filename
     * @param   mode read/write/update access mode for component arrays
     * @return   a readable Ndx object view of the resource at url, or
     *           null if one could not be found
     * @throws   IOException  if there is any I/O error
     * @throws FileNotFoundException  if the location doesn't look like a 
     *                     file or URL
     */
    public Ndx makeNdx( String location, AccessMode mode ) throws IOException {
        return makeNdx( getUrl( location ), mode );
    }

    /**
     * Constructs a new Ndx containing writable and uninitialised 
     * data arrays at a given location.
     * This convenience method just turns the location into a URL and
     * calls {@link #makeBlankNdx(URL,Ndx)}.
     *
     * @param  location  the location of the new resource.  If it cannot
     *                   be parsed as a URL it will be treated as a filename
     * @param  template   a template Ndx object on which to base the new one
     * @return  true if the Ndx was written successfully
     * @throws  IOException  if there is any I/O error
     */
    public boolean makeBlankNdx( String location, Ndx template )
            throws IOException {
         return makeBlankNdx( getUrl( location ), template );
    }

    /**
     * Writes a new resource at a given location containing an Ndx with
     * data copied from an existing Ndx.
     * This convenience method just turns
     * the location into a URL and calls {@link #outputNdx(URL,Ndx)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *                   parsed as a URL it will be treated as a filename
     * @param  ndx  an existing Ndx object whose data will be copied
     * @throws IOException  if there is any I/O error
     * @throws FileNotFoundException  if the location doesn't look like a 
     *                     file or URL
     */
    public void outputNdx( String location, Ndx ndx ) throws IOException {
        outputNdx( getUrl( location ), ndx );
    }

    private static URL getUrl( String location ) {
        return URLUtils.makeURL( location );
    }
}
