package uk.ac.starlink.treeview;

import java.util.*;

/**
 * Describes a position or shape of a rectangular data array.
 * This class has more or less the functionality of a <code>long[]</code>
 * array.
 * <p>
 * Note that unlike the Fortran HDS library, dimension indices used
 * by the methods in this class run from 0 .. (dimensionality - 1).
 *
 * @author   Mark Taylor (STARLINK)
 * @version  $Id$
 */
public class Cartesian implements Cloneable {

    // The values of the coords represented by this object.
    private long[] coordinates;

    /**
     * Initialises a <code>Cartesian</code> from an array of 
     * <code>long</code>s.
     *
     * @param  coords  an array of <code>long</code> giving the coords
     */
    public Cartesian( long[] coords ) {
        coordinates = (long[]) coords.clone();
    }

    /**
     * Initialises a zero-valued <code>Cartesian</code> of a given 
     * dimensionality.
     *
     * @param  ndim  dimensionality of the object to be created
     */
    public Cartesian( int ndim ) {
        this( new long[ ndim ] );
    }

    /**
     * Returns an array containing the coordinates.
     *
     * @return       an array of <code>long</code>s whose <code>length</code>
     *               matches the dimensionality of this 
     *               <code>Cartesian</code>. 
     *               Thus <code>o.getCoords().length == o.getNdim()</code>).
     */
    public long[] getCoords() {
        return coordinates;
    }

    /** 
     * Returns a single coord value.
     *
     * @param   dim    the index of the dimension to be retrieved
     *                 (<code>0..ndim-1</code>)
     * @return         the value of the coord specified by <code>index</code>
     */
    public long getCoord( int dim ) {
        return coordinates[ dim ];
    }

    /**
     * Sets a single coord value.
     *
     * @param  dim                  the index of the dimension to be set 
     *                              (<code>0..ndim-1</code>)
     * @param  coord                the value the coord is to be set to (>0)
     */
    public void setCoord( int dim, long coord ) {
        coordinates[ dim ] = coord;
    }

    /**
     * Gets the dimensionality of this <code>Cartesian</code>.
     * 
     * @return    an <code>int</code> giving the dimensionality of this 
     *            Cartesian.
     */
    public int getNdim() {
        return coordinates.length;
    }

    /**
     * Says whether two <code>Cartesian</code> objects represent the same
     * shape/position.
     *
     * @param   car2  another <code>Caretesian</code> for comparison with this.
     * @return        <code>true</code> if the this object the same as the
     *                <code>car2</code> argument, <code>false</code> otherwise.
     */
    public boolean equals( Cartesian car2 ) {
        return Arrays.equals( coordinates, car2.coordinates );
    }


    /**
     * Gets the number of cells represented by this Cartesian shape.
     * This is normally the product of the coords, but if any of the
     * coords is negative a value of -1 is returned.
     *
     * @return  the number of cells in this shape
     */
    public long numCells() {
        long num = 1;
        for ( int i = 0; i < coordinates.length; i++ ) {
            if ( coordinates[ i ] < 0 ) {
                return -1;
            }
            num *= coordinates[ i ];
        }
        return num;
    }

    /**
     * Gets an iterator over all the positions in this Cartesian shape.
     * For instance, if the Cartesian is (2,2), the iterator will iterate 
     * over the values (1,1), (2,1), (1,2), (2,2).
     *
     * @return  an <code>Iterator</code> over positions in a cuboid
     *          represented by this Cartesian
     */
    public Iterator cellIterator() {
        return new Iterator() {
            private int ndim = coordinates.length;
            private Cartesian nextPos = new Cartesian( ndim );
            {
                for ( int i = 0; i < ndim; i++ ) {
                    nextPos.setCoord( i, 1 );
                }
            }
            private boolean more = ( numCells() >= 0 );
            public boolean hasNext() {
                return more;
            }
            public Object next() throws NoSuchElementException {
                if ( more ) {
                    Cartesian result = (Cartesian) nextPos.clone();
                    for ( int i = 0; i < ndim; i++ ) {
                        if ( ++nextPos.coordinates[ i ] <= coordinates[ i ] ) {
                            return result;
                        }
                        else {
                            nextPos.coordinates[ i ] = 1;
                        }
                    }
                    more = false;
                    return result;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a coordinate array giving the position of a cell a certain
     * offset away from the start of the array in cellIterator order.
     *
     * @param   off  the offset from the start of the array
     * @return  a coordinate array
     */
    public long[] offsetToPos( long off ) {
        if ( off < 0 || off >= numCells() ) {
            throw new IndexOutOfBoundsException( 
                "Offset " + off + " out of bounds" );
        }
        int ndim = getNdim();
        long[] p = new long[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            p[ i ] = off % coordinates[ i ];
            off /= coordinates[ i ];
        }
        return p;
    }

    /**
     * Returns a deep copy of this object.
     *
     * @return  a <code>Cartesian</code> with coords of the same value as
     *          this one.
     */
    public Object clone() {
        return new Cartesian( (long[]) coordinates.clone() );
    }

    /**
     * Gets a <code>String</code> representation of this
     * <code>Cartesian</code> object.  It will look something like 
     * "(1,2,3)".
     *
     * @return  a <code>String</code> representation of this object
     */
    public String toString() {
        return toString( coordinates );
    }

    /**
     * Gets a <code>String</code> representation of a coordinate array.
     * It will look something like "(1,2,3)".
     *
     * @param   coords  an array giving the coordinates
     * @return  a <code>String</code> representation of this object
     */
    public static String toString( long[] coords ) {
        int ndim = coords.length;
        if ( ndim > 0 ) {
            StringBuffer buf = new StringBuffer( "(" );
            for ( int i = 0; i < ndim; i++ ) {
                buf.append( ' ' ) 
                   .append( Long.toString( coords[ i ] ) )
                   .append( ( i + 1 < ndim ) ? "," : "" );
            }
            buf.append( " )" );
            return buf.toString();
        }
        else {
            return "";
        }
    }

    /**
     * Returns a string representation of an array this shape with a given
     * non-zero origin.  The result will look something like 
     * <code>(a:b,c:d)</code>.
     *
     * @param  origin  a <code>Cartesian</code> giving the origin of the
     *                 array to be described.  May be <code>null</code>
     *                 indicating a default origin.
     * @return         a string describing concisely the interval covered by
     *                 an array with the shape of this object but starting
     *                 at the position given by the <code>origin</code> 
     *                 argument.
     */
    public String shapeDescriptionWithOrigin( Cartesian origin ) {
        String result;
        int ndim = coordinates.length;
        if ( origin != null && origin.coordinates.length != ndim ) {
            throw new RuntimeException( "Objects have different shapes" );
        }
        if ( ndim > 0 ) {

            /* Set default origin if we do not have an explicit one. */
            if ( origin == null ) {
               origin = new Cartesian( coordinates.length );
               for ( int i = 0; i < coordinates.length; i++ ) {
                   origin.coordinates[ i ] = 1;
               }
            }

            /* Construct the shape string. */
            StringBuffer buf = new StringBuffer( "(" );
            for ( int i = 0; i < ndim; i++ ) {
                buf.append( ' ' )
                   .append( origin.coordinates[ i ] )
                   .append( ':' )
                   .append( coordinates[ i ] + origin.coordinates[ i ] - 1 )
                   .append( ( i + 1 < ndim ) ? "," : "" );
            }
            buf.append( " )" );
            return buf.toString();
        }
        else {
            return "";
        }
    }
}
