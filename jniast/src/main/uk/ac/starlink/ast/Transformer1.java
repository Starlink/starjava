package uk.ac.starlink.ast;

/**
 * Defines a custom one-dimensional coordinate transformation function.
 * This abstract subclass of <code>Transformer</code> is provided for
 * convenience when only a 1-d transformation is required.  Subclasses
 * need to implement the <code>tran1</code> method, which is somewhat more
 * convenient than <code>Transformer</code>'s <code>tranP</code> method.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public abstract class Transformer1 extends Transformer {
    /**
     * Indicates whether this <code>Transformer</code>
     * is able to transform between coordinate spaces with the given
     * dimensionalities.
     *
     * @param  nin   the number of coordinates of an input point
     * @param  nout  the number of coordinates of an output point
     * @return   true only if both <code>nin</code> and <code>nout</code> 
     *           are equal to 1
     */
    public final boolean canTransformCoords( int nin, int nout ) {
        return ( nin == 1 && nout == 1 );
    }

    /* 
     * Implements Transformer's tranP method in terms of this class's
     * tran1 method.
     */
    public final double[][] tranP( int npoint, int ncoord_in, double[][] in,
                                  boolean forward, int ncoord_out )
        throws Exception {
        return new double[][] { tran1( npoint, in[ 0 ], forward ) };
    }

    /**
     * Transforms points from 1-d input coordinates to 1-d output coordinates.
     * This method does the work of the <code>Transformer1</code>,
     * taking an input coordinate array and returning an output 
     * coordinate array.
     * <p>
     * This method will only be called with <code>forward</code> set true
     * if a prior call of the <code>hasForward</code> method has returned
     * true, and will only be called with <code>forward</code> set false
     * if a prior call of the <code>hasInverse</code> has returned true.
     *
     * @param  npoint      the number of points to be transformed
     * @param  xin         an array of <code>npoint</code> elements 
     *                     representing the coordinates to be transformed
     * @param forward      <code>true</code> if the forward transformation
     *                     is to be used, <code>false</code> if its inverse
     *                     transformation is to be used
     * @return   an array of <code>npoint</code> elements giving the 
     *           transformed coordinates.
     * throws  Exception   If an error occurs during the transformation,
     *                     this method may throw an Exception.  In this
     *                     case the transformation will be aborted.
     */
    public abstract double[] tran1( int npoint, double[] xin, boolean forward ) 
        throws Exception;
}
