package uk.ac.starlink.array;

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
import uk.ac.starlink.util.URLUtils;

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
 * <p>
 * By default, if the corresponding classes are present, the following
 * ArrayBuilders are installed:
 * <ul>
 * <li> {@link uk.ac.starlink.hds.HDSArrayBuilder}
 * <li> {@link uk.ac.starlink.oldfits.FitsArrayBuilder}
 * </ul>
 * Consult the documentation of these classes to find out about the format
 * of URLs understood by each.
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
            Class clazz = this.getClass().forName( className );
            Method meth = clazz.getMethod( "getInstance", noParams );
            ArrayBuilder builder = 
                (ArrayBuilder) meth.invoke( null, noArgs );
            builders.add( builder );
            logger.config( className + " registered" );
        }
        catch ( ClassNotFoundException e ) {
            logger.config( className + " not found - can't register" );
        }

        catch ( InvocationTargetException e ) {

            /* A LinkageError probably means that the native libraries are
             * not available.  In this case, try to invoke the 
             * HDSPackage.isAvailable method to do logging in a standard way.
             * If that doesn't work though, log it directly. */
            if ( e.getTargetException() instanceof LinkageError ) {
                try {
                    this.getClass().forName( "uk.ac.starlink.hds.HDSPackage" )
                        .getMethod( "isAvailable", noParams )
                        .invoke( null, noArgs );
                }
                catch ( Exception e2 ) {
                    logger.config( className + " " + e2 + " - can't register" );
                }
            }
            else {
                logger.config( className + " " + e + " - can't register" );
            }
        }
        catch ( Exception e ) {
            logger.config( className + e + " - can't register" );
        }

        /* Attempt to add a FitsArrayBuilder if the class is available. */
        className = "uk.ac.starlink.oldfits.FitsArrayBuilder";
        try {
            Class clazz = this.getClass().forName( className );
            Method meth = clazz.getMethod( "getInstance", noParams );
            ArrayBuilder builder = (ArrayBuilder) meth.invoke( null, noArgs );
            builders.add( builder );
            logger.config( className + " registered" );
        }
        catch ( ClassNotFoundException e ) {
            logger.config( className + " class not found - can't register" );
        }
        catch ( Exception e ) {
            logger.config( "Failed to register " + className + ": - " + e );
        }
    }

    /**
     * Gets the list of builders which actually do the URL-&gt;NDArray
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
     * Sets the list of builders which actually do the URL-&gt;NDArray
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
     * <p>
     * The <tt>bh</tt> parameter indicates a requested bad value handling
     * scheme, but does not guarantee that it will be used, since not all
     * storage formats are capable of storing bad values in arbitrary
     * ways.  According to the resource type, bad value handling will
     * be provided on a best-efforts basis.
     * If <tt>bh</tt> is null, the implementation will choose a bad value
     * handling policy of its own.
     *
     * @param   url  a URL pointing to a writable resource.  This may be
     *               a suitable <code>file:</code>-protocol URL or one
     *               with some other protocol which can provide an
     *               output-capable connection
     * @param  shape   the shape of the new array
     * @param  type    a Type object indicating the type of data in the array
     * @param  bh      the requested bad value handling policy - see above.
     * @return   a new writable NDArray object with the given URL, 
     *           or null if none could be constructed because none of
     *           the handlers recognised the URL
     * @throws IOException  if an I/O error occurs
     * @throws IllegalArgumentException if the type of <tt>bh</tt> does not 
     *         match <tt>type</tt> 
     */
    public NDArray makeNewNDArray( URL url, NDShape shape, Type type,
                                   BadHandler bh )
            throws IOException {
        if ( bh != null && bh.getType() != type ) {
            throw new IllegalArgumentException( 
                "Bad handler type " + bh.getType() + 
                " does not match specified type " + type );
        }
        for ( Iterator it = builders.iterator(); it.hasNext(); ) {
            ArrayBuilder builder = (ArrayBuilder) it.next();
            NDArray nda = builder.makeNewNDArray( url, shape, type, bh );
            if ( nda != null ) {
                return nda;
            }
        }
        return null;
    }

    /**
     * Constructs a new NDArray to which data can be written given a URL
     * and another template NDArray.  This convenience method simply
     * calls {@link #makeNewNDArray(URL,NDShape,Type,BadHandler)}
     * with the <tt>shape</tt>, <tt>type</tt> and <tt>bh</tt> 
     * parameters copied from the <tt>template</tt> NDArray.
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
        return makeNewNDArray( url, template.getShape(), template.getType(),
                               template.getBadHandler() );
    }

    /**
     * Constructs a readable NDArray from a location representing an 
     * existing resource.
     * This convenience method just turns the location into a URL and calls 
     * {@link #makeNDArray(URL,AccessMode)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *         parsed as a URL, it will be treated as a filename
     * @param   mode the mode with which it should be accessed
     * @return   a readable NDArray object view of the data at
     *           <tt>location</tt>, or null if one could not be found
     * @throws   IOException  if there is any I/O error
     */
    public NDArray makeNDArray( String location, AccessMode mode ) 
            throws IOException {
        return makeNDArray( getUrl( location ), mode );
    }

    /**
     * Constructs a new NDArray to which data can be written given a location
     * and the array characteristics.
     * This convenience method just turns the location into a URL and calls
     * {@link #makeNewNDArray(URL,NDShape,Type,BadHandler)}
     *
     * @param  location  the location of the resource.  If it cannot be
     *         parsed as a URL, it will be treated as a filename
     * @param  shape   the shape of the new array
     * @param  type    a Type object indicating the type of data in the array
     * @param  bh      requested bad value handling policy
     * @return   a new writable NDArray object at the given location,
     *           or null if none could be constructed because none of
     *           the handlers recognised the URL
     * @throws IOException  if an I/O error occurs
     */
    public NDArray makeNewNDArray( String location, NDShape shape, Type type,
                                   BadHandler bh )
            throws IOException {
        return makeNewNDArray( getUrl( location ), shape, type, bh );
    }

    /**
     * Constructs a new NDArray to which data can be written given a location
     * and another template NDArray.
     * This convenience method just turns the location into a URL and calls
     * {@link #makeNewNDArray(URL,NDArray)}.
     *
     * @param  location  the location of the resource.  If it cannot be
     *         parsed as a URL, it will be treated as a filename
     * @param  template  an NDArray whose characteristics are to be
     *                   copied when the new one is created
     * @return   a new writable NDArray object at the given location
     * @throws IOException  if an I/O error occurs
     */
    public NDArray makeNewNDArray( String location, NDArray template )
            throws IOException {
        return makeNewNDArray( getUrl( location ), template );
    }
 
    private static URL getUrl( String location ) {
        return URLUtils.makeURL( location );
    }

 
}
