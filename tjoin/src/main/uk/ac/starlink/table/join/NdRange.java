package uk.ac.starlink.table.join;

import java.util.Arrays;

/**
 * Describes a range in an N-dimensional space.
 * Each dimension may have a minimum and maximum; each of these bounds is a
 * {@link java.lang.Comparable} object.
 * Any or all of the bounds may be missing (null); this indicates that no
 * bounds are in operation in that dimension, so that all values are 
 * effectively inside it.
 *
 * @author   Mark Taylor
 * @since    21 Nov 2007
 */
public class NdRange {

    private final int ndim_;
    private final Comparable<?>[] mins_;
    private final Comparable<?>[] maxs_;
    private final boolean isBounded_;

    /**
     * Constructs a range giving its bounds.
     * The arrays are copied (cloned) by the constructor, so that subsequent
     * changes to them will not be reflected in the state of this object.
     * Any of the bounds may be null.
     *
     * @param   mins   minimum bounds
     * @param   maxs   maximum bounds
     */
    public NdRange( Comparable<?>[] mins, Comparable<?>[] maxs ) {
        mins_ = mins.clone();
        maxs_ = maxs.clone();
        ndim_ = mins.length;
        if ( ndim_ != maxs.length ) {
            throw new IllegalArgumentException( "Array length mismatch" );
        }
        boolean isBounded = false;
        for ( int i = 0; i < ndim_; i++ ) {
            Comparable<?> min = mins_[ i ];
            Comparable<?> max = maxs_[ i ];
            boolean hasMin = min != null;
            boolean hasMax = max != null;
            isBounded = isBounded || hasMin || hasMax;
            if ( hasMin && hasMax && compare( min, max ) > 0 ) {
                throw new IllegalArgumentException( "Boundary limits error: " 
                                                  + min + " > " + max );
            }
        }
        isBounded_ = isBounded;
    }

    /**
     * Constructs a range with no bounds.  Nothing is excluded from it.
     *
     * @param  ndim  dimensionality
     */
    public NdRange( int ndim ) {
        this( new Comparable<?>[ ndim ], new Comparable<?>[ ndim ] );
        assert ! isBounded();
    }

    /**
     * Indicates whether this range has any restrictions on inclusion at all.
     *
     * @return   true  iff {@link #isInside} can ever return false
     */
    public boolean isBounded() {
        return isBounded_;
    }

    /**
     * Returns the array of minimum values.
     * Unknown elements may be nulls.
     *
     * @return  <code>ndim</code>-element array of minima, some may be null
     */
    public Comparable<?>[] getMins() {
        return mins_.clone();
    }

    /**
     * Returns the array of maximum values.
     * Unknown elements may be nulls.
     *
     * @return  <code>ndim</code>-element array of maxima, some may be null
     */
    public Comparable<?>[] getMaxs() {
        return maxs_.clone();
    }

    /**
     * Determines whether a set of coordinates is within this range.
     * Objects on the bounds count as inside.
     * This method will always return true if {@link #isBounded} returns
     * false.
     *
     * @param  coords  point to assess
     * @return  false if the point is definitely outside this range
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    public boolean isInside( Object[] coords ) {
        if ( isBounded_ ) {
            for ( int i = 0; i < ndim_; i++ ) {
                if ( coords[ i ] instanceof Comparable ) {
                    Comparable<?> coord = (Comparable<?>) coords[ i ];
                    Comparable<?> min = mins_[ i ];
                    if ( min != null && compare( coord, min ) < 0 ) {
                        return false;
                    }
                    Comparable<?> max = maxs_[ i ];
                    if ( max != null && compare( coord, max ) > 0 ) {
                        return false;
                    }
                }
            }
            return true;
        }
        else {
            return true;
        }
    }

    public boolean equals( Object o ) {
        if ( o instanceof NdRange ) {
            NdRange other = (NdRange) o;
            return Arrays.equals( this.mins_, other.mins_ )
                && Arrays.equals( this.maxs_, other.maxs_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = 23;
        code = 37 * code + Arrays.hashCode( mins_ );
        code = 37 * code + Arrays.hashCode( maxs_ );
        return code;
    }

    /**
     * Returns a human-readable description of this range.
     */
    public String toString() {
        if ( isBounded() ) {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < ndim_; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( mins_[ i ] == null ? ""
                                                : formatObject( mins_[ i ] ) );
                sbuf.append( " .. " );
                sbuf.append( maxs_[ i ] == null ? ""
                                                : formatObject( maxs_[ i ] ) );
            }
            return sbuf.toString();
        }
        else {
            return "(unbounded)";
        }
    }

    /**
     * Returns a new range which is the intersection of two given ones.
     * If the intersection is empty (regions are disjoint) then null will
     * be returned.
     *
     * @param  r1  first range
     * @param  r2  second range
     * @return   non-empty intersection, or null
     */
    public static NdRange intersection( NdRange r1, NdRange r2 ) {
        if ( r1.ndim_ != r2.ndim_ ) {
            throw new IllegalArgumentException( "Dimensionality mismatch" );
        }
        else {
            int ndim = r1.ndim_;
            Comparable<?>[] mins = new Comparable<?>[ ndim ];
            Comparable<?>[] maxs = new Comparable<?>[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                mins[ i ] = max( r1.mins_[ i ], r2.mins_[ i ], false );
                maxs[ i ] = min( r1.maxs_[ i ], r2.maxs_[ i ], false );
            }
            for ( int i = 0; i < ndim; i++ ) {
                if ( mins[ i ] != null && maxs[ i ] != null &&
                     compare( mins[ i ], maxs[ i ] ) > 0 ) {
                    return null;
                }
            }
            return new NdRange( mins, maxs );
        }
    }

    /**
     * Returns a new range which is the union of two given ones.
     *
     * @param  r1  first range
     * @param  r2  second range
     * @return  union
     */
    public static NdRange union( NdRange r1, NdRange r2 ) {
        if ( r1.ndim_ != r2.ndim_ ) {
            throw new IllegalArgumentException( "Dimensionality mismatch" );
        }
        else {
            int ndim = r1.ndim_;
            Comparable<?>[] mins = new Comparable<?>[ ndim ];
            Comparable<?>[] maxs = new Comparable<?>[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                mins[ i ] = min( r1.mins_[ i ], r2.mins_[ i ], true );
                maxs[ i ] = max( r1.maxs_[ i ], r2.maxs_[ i ], true );
            }
            return new NdRange( mins, maxs );
        }
    }

    /**
     * Returns the lesser of two objects, with explicit null handling.
     *
     * @param  c1   first object
     * @param  c2   second object
     * @param  failNull  what happens if c1 or c2 is null; if true null is 
     *                   returned, if false the non-null value is returned
     * @return  minimum
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    public static Comparable<?> min( Comparable<?> c1, Comparable<?> c2,
                                     boolean failNull ) {
        if ( c1 == null ) {
            return failNull ? null : c2;
        }
        else if ( c2 == null ) {
            return failNull ? null : c1;
        }
        else {
            return compare( c1, c2 ) < 0 ? c1 : c2;
        }
    }

    /**
     * Returns the greater of two objects, with explicit null handling.
     *
     * @param   c1  first object
     * @param   c2  second object
     * @param  failNull  what happens if c1 or c2 is null; if true null is 
     *                   returned, if false the non-null value is returned
     * @return   maximum
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    public static Comparable<?> max( Comparable<?> c1, Comparable<?> c2,
                                     boolean failNull ) {
        if ( c1 == null ) {
            return failNull ? null : c2;
        }
        else if ( c2 == null ) {
            return failNull ? null : c1;
        }
        else {
            return compare( c1, c2 ) > 0 ? c1 : c2;
        }
    }

    /**
     * Compares <code>Comparable</code>s.  Slightly smarter than
     * {@link java.lang.Comparable#compareTo}, since it can compare
     * different number types (such as Float and Double) with each other
     * without throwing a ClassCastException.
     *
     * @param   o1  first object
     * @param   o2  second object
     * @return   <code>o1.compareTo(o2)</code> or similar
     * @throws  ClassCastException  if <code>o1</code> and <code>o2</code>
     *                              are not mutually comparable
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static int compare( Comparable o1, Comparable o2 ) {
        try {
            return o1.compareTo( o2 );
        }       
        catch ( ClassCastException e ) {
            if ( o1 instanceof Number && o2 instanceof Number ) {
                return Double.compare( ((Number) o1).doubleValue(),
                                       ((Number) o2).doubleValue() );
            }                    
            else {               
                throw e;
            }
        }
    }

    /**
     * Returns a string version of a comparable object.
     *
     * @param   c  object
     * @return  string
     */
    private static String formatObject( Comparable<?> c ) {
        if ( c == null ) {
            return "null";
        }
        else if ( c instanceof Double ) {
            return Float.toString( ((Number) c).floatValue() );
        }
        else {
            return c.toString();
        }
    }
}
