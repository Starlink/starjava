package uk.ac.starlink.table.join;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match engine which considers two rows matched if they contain objects
 * which are non-blank and equal.  The objects will typically
 * be strings, but could equally be something else.
 * Match scores are always either 0.0 (equal) or -1.0 (unequal).
 *
 * <p>The equality is roughly in the sense of {@link java.lang.Object#equals},
 * but some additional work is done, so that for instance (multi-dimensional)
 * arrays are compared (recursively) on their contents, and blank objects
 * are compared in the sense used in the rest of STIL.  A blank value is
 * not considered equal to anything, including another blank value.
 * Scalar numeric values are, as far as possible, compared on numeric
 * value rather than object equality, though this numeric value comparison
 * does not currently apply to arrays.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Mar 2004
 */
public class EqualsMatchEngine implements MatchEngine {

    /** Stateless MatchKit instance for use with this class. */
    private static final MatchKit KIT = new MatchKit() {
        public Object[] getBins( Object[] tuple ) {
            Object obj = tuple[ 0 ];
            return Tables.isBlank( obj )
                 ? NO_BINS
                 : new Object[] { new Integer( getHash( obj ) ) };
        }
        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            return isEqual( tuple1[ 0 ], tuple2[ 0 ] ) ? 0.0 : -1.0;
        }
    };

    public Supplier<MatchKit> createMatchKitFactory() {
        return () -> KIT;
    };

    /**
     * Returns null.  You could check bounds on hashcodes, but it's not
     * likely to be very revealing.
     */
    public Supplier<Coverage> createCoverageFactory() {
        return null;
    }

    public double getScoreScale() {
        return 1.0;
    }

    /**
     * The match score is uninteresting, since it's either -1 or 0.
     * We flag this by returning <code>null</code> here.
     *
     * @return  null
     */
    public ValueInfo getMatchScoreInfo() {
        return null;
    }

    public ValueInfo[] getTupleInfos() {
        DefaultValueInfo vinfo = 
            new DefaultValueInfo( "Matched Value", Object.class,
                                  "Value for exact match" );
        vinfo.setNullable( false );
        return new ValueInfo[] { vinfo };
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[ 0 ];
    }

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[ 0 ];
    }

    public String toString() {
        return "Exact Value";
    }

    /**
     * Determines whether two objects are equal or not.
     *
     * @param   o1   object 1
     * @param   o2   object 2
     * @return  true iff <code>o1</code> is equals to <code>o2</code>
     */
    private static boolean isEqual( Object o1, Object o2 ) {
        if ( Tables.isBlank( o1 ) || Tables.isBlank( o2 ) ) {
            return false;
        }
        else if ( o1.equals( o2 ) ) {
            return true;
        }
        Class<?> c1 = o1.getClass();
        Class<?> c2 = o2.getClass();
        if ( c1.isArray() && c2.equals( c1 ) ) {
            Class<?> clazz = c1.getComponentType();
            assert clazz == c2.getComponentType();
            if ( clazz == byte.class ) {
                return Arrays.equals( (byte[]) o1, (byte[]) o2 );
            }
            else if ( clazz == short.class ) {
                return Arrays.equals( (short[]) o1, (short[]) o2 );
            }
            else if ( clazz == int.class ) {
                return Arrays.equals( (int[]) o1, (int[]) o2 );
            }
            else if ( clazz == long.class ) {
                return Arrays.equals( (long[]) o1, (long[]) o2 );
            }
            else if ( clazz == float.class ) {
                return Arrays.equals( (float[]) o1, (float[]) o2 );
            }
            else if ( clazz == double.class ) {
                return Arrays.equals( (double[]) o1, (double[]) o2 );
            }
            else if ( clazz == boolean.class ) {
                return Arrays.equals( (boolean[]) o1, (boolean[]) o2 );
            }
            else if ( clazz == char.class ) {
                return Arrays.equals( (char[]) o1, (char[]) o2 );
            }
            else {
                assert Object.class.isAssignableFrom( clazz );
                Object[] a1 = (Object[]) o1;
                Object[] a2 = (Object[]) o2;
                int n1 = a1.length;
                int n2 = a2.length;
                if ( n1 != n2 ) {
                    return false;
                }
                else {
                    for ( int i = 0; i < n1; i++ ) {
                        if ( ! isEqual( a1[ i ], a2[ i ] ) ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        else if ( isNumber( o1 ) && isNumber( o2 ) && ! c1.equals( c2 ) ) {
            Number n1 = (Number) o1;
            Number n2 = (Number) o2;
            return isInteger( n1 ) && isInteger( n2 )
                 ? n1.longValue() == n2.longValue()
                 : n1.doubleValue() == n2.doubleValue();
        }
        else {
            return false;
        }
    }

    /**
     * Returns a hash code for an object.  This matches the sense of 
     * equality used by {@link #isEquals}.
     *
     * @param   obj  object; null is permitted
     * @return  hash code
     */
    private static int getHash( Object obj ) {
        if ( Tables.isBlank( obj ) ) {
            return 0;
        }
        else if ( obj.getClass().isArray() ) {
            int leng = Array.getLength( obj );
            int hash = 17;
            for ( int i = 0; i < leng; i++ ) {
                hash = 23 * hash + getHash( Array.get( obj, i ) );
            }
            return hash;
        }
        else if ( isNumber( obj ) ) {
            long bits = Double.doubleToLongBits( ((Number) obj).doubleValue() );

            /* Implementation copied from Double.hashCode. */
            return (int) ( bits ^ ( bits >>> 32 ) );
        }
        else {
            return obj.hashCode();
        }
    }

    /**
     * Indicates whether a value is one of the normal numeric types.
     * Weird Number subclasses like BigInteger are excluded.
     * 
     * @param  obj  test object
     * @return   true iff obj is a normal number object
     */
    private static boolean isNumber( Object obj ) {
        return obj instanceof Number
            && ( isInteger( (Number) obj )
              || obj instanceof Float
              || obj instanceof Double );
    }

    /**
     * Indicates whether a numeric value has an integer type.
     *
     * @param  num  number object
     * @return  true iff object is considered integer
     */
    private static boolean isInteger( Number num ) {
        return num instanceof Byte
            || num instanceof Short
            || num instanceof Integer
            || num instanceof Long;
    }
}
