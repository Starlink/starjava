package uk.ac.starlink.ast;

/**
 * Defines a custom two-dimensional coordinate transformation function.
 * This abstract subclass of <code>Transformer</code> is provided for
 * convenience when only a 2-d transformation is required.  Subclasses
 * need to implement the <code>tran2</code> method, which is somewhat more
 * convenient than <code>Transformer</code>'s <code>tranP</code> method.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public abstract class Transformer2 extends Transformer {
    /**
     * Indicates whether this <code>Transformer</code>
     * is able to transform between coordinate spaces with the given
     * dimensionalities.
     *
     * @param  nin   the number of coordinates of an input point
     * @param  nout  the number of coordinates of an output point
     * @return   true only if both <code>nin</code> and <code>nout</code> 
     *           are equal to 2
     */
    public final boolean canTransformCoords( int nin, int nout ) {
        return ( nin == 2 && nout == 2 );
    }

    /* 
     * Implements Transformer's tranP method in terms of this class's
     * tran2 method.
     */
    public final double[][] tranP( int npoint, int ncoord_in, double[][] in,
                                   boolean forward, int ncoord_out )
                            throws Exception {
        return tran2( npoint, in[ 0 ], in[ 1 ], forward );
    }

    /**
     * Transforms points from 2-d input coordinates to 2-d output coordinates.
     * This method does the work of the <code>Transformer2</code>,
     * taking input x and y coordinate arrays and returning a two-element
     * array consisting of the x and y output coordinate arrays.
     * <p>
     * This method will only be called with <code>forward</code> set true
     * if a prior call of the <code>hasForward</code> method has returned
     * true, and will only be called with <code>forward</code> set false
     * if a prior call of the <code>hasInverse</code> has returned true.
     *
     * @param  npoint      the number of points to be transformed
     * @param  xin         an array of <code>npoint</code> elements 
     *                     representing the X coordinates to be transformed
     * @param  yin         an array of <code>npoint</code> elements
     *                     representing the Y coordinates to be transformed
     * @param forward      <code>true</code> if the forward transformation
     *                     is to be used, <code>false</code> if its inverse
     *                     transformation is to be used
     * @return   a two-element array; the first element is an 
     *           <code>npoint</code>-element array of the output X coordinates,
     *           and the second element is an
     *           <code>npoint</code>-element array of the output Y coordinates
     * @throws  Exception  If an error occurs during the transformation,
     *                     this method may throw an Exception.  In this
     *                     case the transformation will be aborted.
     */
    public abstract double[][] tran2( int npoint, double[] xin, double yin[],
                                      boolean forward ) 
                               throws Exception;
}
