package uk.ac.starlink.ndx;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;

/**
 * Interface for objects which can construct an Ndx from a URL.
 * {@link #makeNdx} constructs an Ndx from an existing resource
 * and {@link #createNewNdx} constructs a new resource containing a copy
 * of an existing Ndx.
 * If the URL fed to <tt>createNewNdx</tt> is subsequently fed to
 * <tt>makeNdx</tt> the factory should understand it to reference the
 * resource which was created by the earlier call (given that it still
 * exists).
 *
 * @author   Mark Taylor (Starlink)
 */
public interface NdxBuilder {

    /**
     * Constructs an Ndx based on the existing resource at a given URL.
     * If the resource is not recognised or it is not known how to
     * construct such an NDArray, then <tt>null</tt> should be returned.
     * If the resource exists but some error occurs in processing it,
     * or if this factory knows that it understands the URL but is
     * unable to locate such a resource then an IOException should be
     * thrown; however, if it is possible that a different factory could
     * correctly construct an Ndx from this URL then a null return
     * is preferred.
     *
     * @param   url   the URL of the resource from which an Ndx  is to
     *                be constructed
     * @param   mode  the read/update/write mode with which to create the Ndx
     * @throws  IOException  if the URL is understood but an NDArray cannot
     *                       be made
     */
    Ndx makeNdx( URL url, AccessMode mode ) throws IOException;

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
     *                   given by <tt>url</tt>
     * @return  true if the copy can be made, false if this builder does 
     *               not feel qualified to create a resource with the given
     *               URL.
     * @throws  IOException  if an error occurred during the resource 
     *                       creation or data copying
     */
    boolean createNewNdx( URL url, Ndx original ) throws IOException;
}
