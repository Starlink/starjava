package uk.ac.starlink.table;

import java.util.Iterator;

/**
 * Iterates over a shape array, as got from {@link ValueInfo#getShape}.
 * Will only work for a finite-valued array, not for one with a
 * variable last dimension (negative dimension value).
 * The object returned by the <code>next</code> method is an array 
 * with the same number of dimensions as the shape itself, giving
 * the current position (the first returned value is an N-element 
 * array of zeros).
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class ShapeIterator implements Iterator<int[]> {
    private final int[] shape_;
    private final int ndim_;
    private int[] pos_;

    /**
     * Constructs a new ShapeIterator.
     *
     * @param  shape  array of dimensions
     * @throws IllegalArgumentException  if <code>shape</code> has negative or
     *         zero elements
     */
    public ShapeIterator( int[] shape ) {
        if ( shape.length == 0 ) {
            throw new IllegalArgumentException( "Empty shape" );
        }
        for ( int i = 0; i < shape.length; i++ ) {
            if ( shape[ i ] <= 0 ) {
                throw new IllegalArgumentException( "Not a finite shape" );
            }
        }
        shape_ = shape;
        ndim_ = shape_.length;
        pos_ = new int[ ndim_ ];
    }

    public boolean hasNext() {
        return pos_ != null;
    }

    public int[] next() { 
        int[] next = pos_.clone();
        for ( int j = 0; j < ndim_; j++ ) {
            if ( ++pos_[ j ] < shape_[ j ] ) {
                    return next;         
            }
            else {
                pos_[ j ] = 0;
            }
        }
        pos_ = null;
        return next;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
