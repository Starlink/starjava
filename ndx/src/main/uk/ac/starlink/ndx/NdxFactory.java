package uk.ac.starlink.ndx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.array.AccessMode;

/**
 * Manufactures Ndx objects from URLs.
 * Methods are available to construct a readable Ndx from a URL
 * pointing to an existing resource, or to construct a new Ndx located 
 * at a given URL and populate it from the content of an existing Ndx.
 * <p>
 * This factory delegates the actual Ndx creation to external
 * NdxBuilder objects; the URL is passed to each one in turn until
 * one can make an Ndx out of it, which object is returned to the caller.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxFactory {

    private static List defaultBuilders;
    private final List builders;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    /**
     * Constructs an NdxFactory with a default list of builders.
     */
    public NdxFactory() {
        if ( defaultBuilders == null ) {
            defaultBuilders = new ArrayList( 3 );
            Class[] noParams = new Class[ 0 ];
            Object[] noArgs = new Object[ 0 ];
            String className;

            /* Attempt to add an NDFNdxBuilder if the class is available. */
            className = "uk.ac.starlink.hds.NDFNdxBuilder";
            try {
                Class clazz = Class.forName( className );
                Method meth = clazz.getMethod( "getInstance", noParams );
                NdxBuilder builder = (NdxBuilder) meth.invoke( null, noArgs );
                defaultBuilders.add( builder );
                // logger.info( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.warning( className +
                                " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.warning( "Failed to register " + className +
                                ": - " + e );
            }

            /* Attempt to add a FITSNdxBuilder if the class is available. */
            className = "uk.ac.starlink.fits.FITSNdxBuilder";
            try {
                Class clazz = Class.forName( className );
                Method meth = clazz.getMethod( "getInstance", noParams );
                NdxBuilder builder = (NdxBuilder) meth.invoke( null, noArgs );
                defaultBuilders.add( builder );
                // logger.info( className + " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.warning( className +
                                " class not found - can't register" );
            }
            catch ( Exception e ) {
                logger.warning( "Failed to register " + className +
                                ": - " + e );
            }

            /* Add the XMLNdxBuilder. */
            defaultBuilders.add( XMLNdxBuilder.getInstance() );
        }
        builders = new ArrayList( defaultBuilders );
    }

    /**
     * Gets the list of builders which actually do the URL->Ndx
     * construction.  Builders earlier in the list are given a
     * chance to make an Ndx before ones later in the list.
     * This list may be modified to change the behaviour of the
     * NdxFactory.
     *
     * @return   a mutable List of {@link NdxBuilder} objects used
     *           for turning URLs into Ndxs
     */
    public List getBuilders() {
        return builders;
    }

    /**
     * Constructs a readable Ndx from a URL representing an exisiting
     * resource.
     * A null result will be returned if none of the available builders
     * understands the URL; an IOException will result if one of the
     * builders is willing to handle the URL but fails to find an
     * Ndx resource at it.
     *
     * @param   url  a URL pointing to a resource representing an Ndx
     * @param   mode the mode with which it should be accessed
     * @return   a readable Ndx object view of the resource at url, or
     *           null if one could not be found
     * @throws   IOException  if there is any I/O error
     */
    public Ndx makeNdx( URL url, AccessMode mode )
            throws IOException {
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            NdxBuilder builder = (NdxBuilder) it.next();
            Ndx nda = builder.makeNdx( url, mode );
            if ( nda != null ) {
                return nda;
            }
        }
        return null;
    }

    /**
     * Writes a new resource at a given URL containing an Ndx with data
     * copied from an existing Ndx.
     * An UnsupportedOperationException will result if none  
     * of the available builders understands the URL; 
     * an IOException will result if one of the
     * builders is willing to handle the URL but fails to make an Ndx
     * out of it or some error is encountered during the copy.
     *
     * @param   url  a URL pointing to a writable resource.  This may be
     *               a suitable <code>file:</code>-protocol URL or one
     *               with some other protocol which can provide an
     *               output-capable connection
     * @param   original  an existing Ndx object whose data will be copied
     *             to <tt>url</tt>
     * @throws IOException  if an I/O error occurs
     * @throws UnsupportedOperationException  no handler exists for this URL
     */
    public void createNewNdx( URL url, Ndx original )
            throws IOException {
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            NdxBuilder builder = (NdxBuilder) it.next();
            if ( builder.createNewNdx( url, original ) ) {
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
     * calls {@link #makeNdx(URL,mode)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *                   parsed as a URL it will be treated as a filename
     * @param   mode the mode with which it should be accessed
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
     * Writes a new resource at a given location containing an Ndx with
     * data copied from an existing Ndx.
     * This convenience method just turns
     * the location into a URL and calls {@link #createNewNdx(URL,Ndx)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *                   parsed as a URL it will be treated as a filename
     * @param  original  an existing Ndx object whose data will be copied
     * @throws IOException  if there is any I/O error
     * @throws FileNotFoundException  if the location doesn't look like a 
     *                     file or URL
     */
    public void createNewNdx( String location, Ndx original )
            throws IOException{
        createNewNdx( getUrl( location ), original );
    }

    private static URL getUrl( String location ) throws FileNotFoundException {
        URL url;
        try {
            url = new URL( location );
        }
        catch ( MalformedURLException e ) {
            try { 
                URL context = new URL( "file:." );
                url = new URL( context, location );
            }
            catch ( MalformedURLException e1 ) {
                String msg = "'" + location + "' doesn't look like a filename";
                throw (FileNotFoundException) new FileNotFoundException( msg )
                                             .initCause( e1 );
            }
        }
        return url;
    }
}
