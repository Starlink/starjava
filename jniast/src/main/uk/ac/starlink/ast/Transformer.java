package uk.ac.starlink.ast;

/**
 * Defines a custom general coordinate transformation function. 
 * Objects subclassing this abstract class can be used to construct a 
 * <code>Mapping</code> (an {@link IntraMap}) 
 * with a user-defined transformation function.
 * <p>
 * If the <code>IntraMap</code>s containing this <code>Transformer</code>
 * is to be read or written via a {@link Channel}, the class
 * must be declared {@link java.io.Serializable}.
 * <p>
 * This is implemented as an abstract class (rather than an interface)
 * so that additional `flags' along the lines of the <code>simpFI</code>
 * method can be introduced in the future without breaking existing code.
 * 
 * @author   Mark Taylor (Starlink) 
 * @version  $Id$
 */
public abstract class Transformer {

    /**
     * Indicates whether this <code>Transformer</code>
     * is able to transform between coordinate spaces with the given 
     * dimensionalities.
     *
     * @param  nin   the number of coordinates of an input point
     * @param  nout  the number of coordinates of an output point
     * @return       the method should return <code>true</code> if
     *               this <code>Transformer</code>'s
     *               <code>tranP</code> method will be able to transform
     *               points with <code>nin</code> coordinates to
     *               points with <code>nout</code> coordinates, 
     *               and <code>false</code> otherwise
     */
    public abstract boolean canTransformCoords( int nin, int nout );

    /**
     * Indicates whether this <code>Transformer</code> can perform a 
     * forward transformation (whether the transformation method may
     * be invoked with <code>forward</code> set true).
     * <p>
     * The default
     * implementation returns <code>true</code> - subclasses should
     * override this if the forward transformation is not available.
     *
     * @return   whether the forward transformation is available
     */
    public boolean hasForward() {
        return true;
    }

    /**
     * Indicates whether this <code>Transformer</code> can perform an
     * inverse transformation (whether the transformation method may
     * be invoked with <code>forward</code> set false).
     * <p>
     * The default
     * implementation returns <code>true</code> - subclasses should
     * override this if the inverse transformation is not available.
     *
     * @return   whether the inverse transformation is available
     */
    public boolean hasInverse() {
        return true;
    }

    /**
     * Indicates whether a forward transformation followed by an inverse
     * transformation may always be considered to restore the original 
     * coordinates.  It is not necessary that both forward and inverse
     * transformations have been implemented.
     * <p>
     * The default implementation returns <code>false</code> - subclasses
     * should override this if such simplifications are always valid.
     *
     * @return   whether forward followed by inverse is a unit transformation
     */
    public boolean simpFI() {
        return false;
    }

    /**
     * Indicates whether an inverse transformation followed by a forward
     * transformation may always be considered to restore the original 
     * coordinates.  It is not necessary that both forward and inverse
     * transformations have been implemented.
     * <p>
     * The default implementation returns <code>false</code> - subclasses
     * should override this if such simplifications are always valid.
     *
     * @return   whether inverse followed by forward is a unit transformation
     */
    public boolean simpIF() {
        return false;
    }

    /**
     * Transforms points from input coordinates to output coordinates.
     * This method does the work of the <code>Transformer</code>,
     * taking an array of input coordinate arrays and returning
     * an array of output coordinate arrays.
     * <p>
     * This method will only be called with <code>forward</code> set true
     * if a prior call of the <code>hasForward</code> method has returned
     * true, and will only be called with <code>forward</code> set false
     * if a prior call of the <code>hasInverse</code> has returned true.
     * <p>
     * If <code>forward</code> is true, then this method will only be called
     * with values of <code>ncoord_in</code>, <code>ncoord_out</code>
     * for which a prior call of <code>canTransform(ncoord_in,ncoord_out)</code>
     * has returned true.<br>
     * If <code>forward</code> is false, then this method will only be called
     * with values of <code>ncoord_in</code>, <code>ncoord_out</code>
     * for which a prior call of <code>canTransform(ncoord_out,ncoord_in)</code>
     * has returned true.
     *
     * @param  npoint      the number of points to be transformed
     * @param  ncoord_in   the number of coordinates being supplied
     *                     for each input point (the dimensionality of
     *                     the input space)
     * @param  in          an array of <code>ncoord_in</code> arrays,
     *                     each containing <code>npoint</code> elements.
     *                     These give the coordinates of each point to
     *                     transform.
     * @param forward      <code>true</code> if the forward transformation
     *                     is to be used, <code>false</code> if its inverse
     *                     transformation is to be used
     * @param ncoord_out   the number of coordinates being generated 
     *                     by the transformation for each output point
     *                     (the dimensionality of the output space).
     *                     This need not be equal to <code>ncoord_in</code>.
     * @return             an array of <code>ncoord_out</code> arrays,
     *                     each containing <code>npoint</code> elements.
     *                     These give the coordinates of the transformed
     *                     points.
     * throws  Exception   If an error occurs during the transformation,
     *                     this method may throw an Exception.  In this
     *                     case the transformation will be aborted.
     */
    public abstract double[][] tranP( int npoint, int ncoord_in, double[][] in,
                                      boolean forward, int ncoord_out )
        throws Exception;

    /**
     * Gives a brief indication of the purpose of this <code>Transformer</code>.
     *
     * @return a short (one-line) string describing the purpose of this
     *           transformation function
     */
    public abstract String getPurpose();

    /**
     * Names the author of this <code>Transformer</code>.
     *
     * @return a string giving the name of the author of this transformation
     *         function
     */
    public abstract String getAuthor();

    /**
     * Gives contact details for the author of this <code>Transformer</code>.
     *
     * @return a string giving contact details, for instance an e-mail 
     *         or WWW address, for the author.
     */
    public abstract String getContact();
}
