package uk.ac.starlink.array;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manufactures NDArray objects from URLs. 
 * Methods are available to construct a readable NDArray from a URL
 * pointing to an existing array resource, or to construct a 
 * new NDArray with a location given by a URL.
 * <p>
 * This factory delegates the actual NDArray creation to external
 * ArrayBuilder objects; the URL is passed to each one in turn 
 * until one can make an NDArray object from it, which object 
 * is returned to the caller.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NDArrayFactory {

    private List builders;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.array" );

    /**
     * Constructs an NDArrayFactory with a default list of builders.
     */
    public NDArrayFactory() {
        builders = new ArrayList( 2 );
        Class[] noParams = new Class[ 0 ];
        Object[] noArgs = new Object[ 0 ];
        String className;

        /* Attempt to add an HDSArrayBuilder if the class is available. */
        className = "uk.ac.starlink.hds.HDSArrayBuilder";
        try {
            Class clazz = Class.forName( className );
            Method meth = clazz.getMethod( "getInstance", noParams );
            ArrayBuilder builder = (ArrayBuilder) meth.invoke( null, noArgs );
            builders.add( builder );
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

        /* Attempt to add a FitsArrayBuilder if the class is available. */
        className = "uk.ac.starlink.fits.FitsArrayBuilder";
        try {
            Class clazz = Class.forName( className );
            Method meth = clazz.getMethod( "getInstance", noParams );
            ArrayBuilder builder = (ArrayBuilder) meth.invoke( null, noArgs );
            builders.add( builder );
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
    }

    /**
     * Gets the list of builders which actually do the URL->NDArray
     * construction.  Builders earlier in the list are given a 
     * chance to handle a URL before ones later in the list.
     * This list may be modified to change the behaviour of the 
     * NDArrayFactory.
     *
     * @return   a mutable List of {@link ArrayBuilder} objects used
     *           for turning URLs into NDArrays
     */
    public List getBuilders() {
        return builders;
    }

    /**
     * Sets the list of builders which actually do the URL->NDArray
     * construction  Builders earlier in the list are given a chance
     * to handle a URL before ones later in the list.
     *
     * @param  builders  an array of ArrayBuilder objects used for 
     *                   turning URLs into NDArrays
     */
    public void setBuilders( ArrayBuilder[] builders ) {
        this.builders = new ArrayList( Arrays.asList( builders ) );
    }

    /**
     * Constructs a readable NDArray from a URL representing an exisiting
     * resource.
     * A null result will be returned if none of the available builders
     * understands the URL; an IOException will result if one of the
     * builders is willing to handle the URL but fails to find an
     * array resource at it.
     *
     * @param   url  a URL pointing to a resource holding array data
     * @param   mode the mode with which it should be accessed
     * @return   a readable NDArray object view of the data at url, or
     *           null if one could not be found
     * @throws   IOException  if there is any I/O error
     */
    public NDArray makeNDArray( URL url, AccessMode mode )
            throws IOException {
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            ArrayBuilder builder = (ArrayBuilder) it.next();
            NDArray nda = builder.makeNDArray( url, mode );
            if ( nda != null ) {
                return nda;
            }
        }
        return null;
    }

    /**
     * Constructs a new NDArray to which data can be written given a URL
     * and the array characteristics.
     * A null result will be returned if none of the available builders
     * understands the URL; an IOException will result if one of the 
     * builders is willing to handle the URL but fails to make an array
     * resource at it.
     *
     * @param   url  a URL pointing to a writable resource.  This may be
     *               a suitable <code>file:</code>-protocol URL or one
     *               with some other protocol which can provide an
     *               output-capable connection
     * @param  shape   the shape of the new array
     * @param  type    a Type object indicating the type of data in the array
     * @return   a new writable NDArray object with the given URL, 
     *           or null if none could be constructed because none of
     *           the handlers recognised the URL
     * @throws IOException  if an I/O error occurs
     */
    public NDArray makeNewNDArray( URL url, NDShape shape, Type type )
            throws IOException {
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            ArrayBuilder builder = (ArrayBuilder) it.next();
            NDArray nda = builder.makeNewNDArray( url, shape, type );
            if ( nda != null ) {
                return nda;
            }
        }
        return null;
    }

    /**
     * Constructs a new NDArray to which data can be written given a URL
     * and another template NDArray.  This convenience method simply
     * calls {@link #makeNewNDArray(URL,NDShape,Type)}
     * with the shape and type parameters copied from
     * the template NDArray.
     *
     * @param   url  a URL pointing to a writable resource.  This may be
     *               a suitable <code>file:</code>-protocol URL or one
     *               with some other protocol which can provide an
     *               output-capable connection
     * @param  template  an NDArray whose characteristics are to be
     *                   copied when the new one is created
     * @return   a new writable NDArray object with the given URL
     * @throws IOException  if an I/O error occurs
     */
    public NDArray makeNewNDArray( URL url, NDArray template )
            throws IOException {
        return makeNewNDArray( url, template.getShape(), template.getType() );
    }
}
