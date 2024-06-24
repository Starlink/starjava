package uk.ac.starlink.ndx;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;

/**
 * Interface for objects which can construct an Ndx from a URL.
 * {@link #makeNdx} constructs an Ndx from an existing resource
 * and {@link #outputNdx} constructs a new resource containing a copy
 * of an existing Ndx.
 * If the URL fed to <code>outputNdx</code> is subsequently fed to
 * <code>makeNdx</code> the factory should understand it to reference the
 * resource which was created by the earlier call (given that it still
 * exists).
 *
 * @author   Mark Taylor (Starlink)
 */
public interface NdxHandler {

    /**
     * Constructs a readable Ndx based on the existing resource at a given URL.
     * If the resource is not recognised or it is not known how to
     * construct such an Ndx, then <code>null</code> should be returned.
     * If the resource exists but some error occurs in processing it,
     * or if this factory knows that it understands the URL but is
     * unable to locate such a resource then an IOException should be
     * thrown; however, if it is possible that a different factory could
     * correctly construct an Ndx from this URL then a null return
     * is preferred.
     *
     * @param   url   the URL of the resource from which an Ndx  is to
     *                be constructed
     * @param   mode read/write/update access mode for component arrays
     * @throws  IOException  if the URL is understood but an Ndx cannot
     *                       be made
     */
    Ndx makeNdx( URL url, AccessMode mode ) throws IOException;

    /**
     * Constructs a new Ndx containing writable and uninitialised 
     * array components at the given URL with characteristics 
     * matching those of a given template Ndx.
     * The scalar components will be copied directly from the template,
     * and the new Ndx will have array components matching the array 
     * components of the template in shape and type (and whether 
     * they exist or not).  They may, but are not required to, match 
     * in point of bad values and pixel ordering scheme.
     * The initial values of the created array components are undefined, 
     * but they will be writable when subsequently opened.
     * <p>
     * If the URL is not recognised by this handler or it is not known
     * how to construct such an Ndx then <code>false</code> should be returned.
     * If the handler recognises the URL but some error occurs in 
     * creating the new Ndx, then an IOException should be thrown; 
     * however, if it is possible that a different handler could
     * correctly construct a writable Ndx as requested then a <code>false</code>
     * return is preferred.
     *
     * @param  url  a URL at which the new NDX should be written
     * @param  template   a template Ndx object from which non-array data
     *                    should be initialised - all scalar components
     *                    will be copied from it, and new blank writable
     *                    array components matching the ones in it will be
     *                    created
     * @return  true if the handler could create the Ndx at <code>url</code>,
     *          false if it doesn't understand this URL
     * @throws  IOException  if the URL is understood but an NDArray cannot
     *                       be made
     */
    boolean makeBlankNdx( URL url, Ndx template ) throws IOException;

    /**
     * Constructs a new Ndx which is a copy of the given Ndx at a
     * location determined by a given URL.
     * Exactly what 'copy' means is dependent on the implementation - 
     * it may be effectively a reference to the original data or it
     * may be an independent copy.
     * <p>
     * The return status indicates whether such a resource was successfully
     * created.
     * If the URL is not recognised or this factory does not feel qualified
     * to construct an Ndx with the given URL then it must return false.
     * If some error occurs during construction
     * then an IOException should in general be thrown; however if
     * this factory thinks that another factory might have more luck
     * then a false return is preferred.
     *
     * @param  url    the URL at which the resource backing the Ndx  is
     *                to be written
     * @param  original  an Ndx whose data is to be copied to the resource
     *                   given by <code>url</code>
     * @return  true if the copy can be made, false if this handler does 
     *               not feel qualified to create a resource with the given
     *               URL.
     * @throws  IOException  if an error occurred during the resource 
     *                       creation or data copying
     */
    boolean outputNdx( URL url, Ndx original ) throws IOException;
}
