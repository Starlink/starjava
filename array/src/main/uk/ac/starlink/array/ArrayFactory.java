package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;

/**
 * Interface for objects which can construct an NDArray from a URL.
 * {@link #makeNDArray} constructs an NDArray from an existing resource
 * and {@link #makeNewNDArray} constructs an NDArray backed by a newly
 * created resource.
 * If the URL fed to <tt>makeNewNDArray</tt> is subsequently fed to
 * <tt>makeNDArray</tt> the factory should understand it to reference the 
 * resource which was created by the earlier call (given that 
 * it still exists).
 *
 * @author   Mark Taylor (Starlink)
 */
public interface ArrayFactory {

    /**
     * Constructs an NDArray based on the existing resource at a given URL.
     * If the resource is not recognised or it is not known how to 
     * construct such an NDArray, then <tt>null</tt> should be returned.
     * If the resource exists but some error occurs in processing it,
     * or if this factory knows that it understands the URL but is 
     * unable to locate such a resource then an IOException should be
     * thrown; however, if it is possible that a different factory could
     * correctly construct an NDArray from this URL then a null return
     * is preferred.
     *
     * @param   url   the URL of the resource from which an NDArray is to
     *                be constructed
     * @param   mode  the read/update/write mode with which to create the array
     * @throws  IOException  if the URL is understood but an NDArray cannot
     *                       be made
     */
    NDArray makeNDArray( URL url, AccessMode mode ) throws IOException;

    /**
     * Constructs a new NDArray with the given characteristics in a 
     * location determined by a given URL.
     * If the URL is not recognised or this factory does not feel qualified
     * to construct an NDArray with the given URL then <tt>null</tt> 
     * should be returned.  If some error occurs during construction
     * then an IOException should in general be thrown; however if
     * this factory thinks that another factory might have more luck
     * then a null return is preferred.
     *
     * @param  url    the URL at which the resource backing the NDArray is 
     *                to be written
     * @param  shape  the shape of the new NDArray to construct.  If this 
     *                object is an {@link OrderedNDShape}, its {@link Order}
     *                <i>may</i> be used as a hint about the pixel ordering
     *                scheme of the NDArray to be created, but no guarantee
     *                is made that the orderings will match
     * @param  type   the primitive data type of the new NDArray to construct
     * @throws   IOException  if the URL is understood but the requested
     *                        NDArray cannot be constructed there
     */
    NDArray makeNewNDArray( URL url, NDShape shape, Type type ) 
        throws IOException;
}
