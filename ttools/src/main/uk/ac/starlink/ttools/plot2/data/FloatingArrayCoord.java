package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Coord implementation for a variable-length array of floating point values.
 * This covers both single and double precision.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2013
 */
public abstract class FloatingArrayCoord extends SingleCoord {

    /* Implementations could be improved: in many cases the number of elements
     * will be the same for every row.  The implementation could be smart
     * enough to spot this and optimise storage accordingly. */

    /**
     * Constructor.
     *
     * @param  meta  input value metadata
     * @param  isRequired   true if this coordinate is required for plotting
     * @param  isDouble  true for double precision, false for single
     */
    private FloatingArrayCoord( InputMeta meta, boolean isRequired,
                                boolean isDouble ) {
        super( meta, isRequired, Object.class,
               isDouble ? StorageType.DOUBLE_ARRAY : StorageType.FLOAT_ARRAY,
               null );
    }

    /**
     * Reads an array value from an appropriate column in the current row
     * of a given TupleSequence.
     *
     * @param  tseq  sequence positioned at a row
     * @param  icol  index of column in sequence corresponding to this Coord
     * @return  value of floating array colun at the current sequence row
     */
    public abstract double[] readArrayCoord( TupleSequence tseq, int icol );

    /**
     * Returns the length of an array value at an appropriate column in the
     * current row of a given TupleSequence.
     *
     * @param  tseq  sequence positioned at a row
     * @param  icol  index of column in sequence corresponding to this Coord
     * @return   array length
     */
    public abstract int getArrayCoordLength( TupleSequence tseq, int icol );

    /**
     * Returns a list of the classes which can be used as single user
     * coordinate values for floating array coordinates.
     * It's all the primitive numeric array types.
     */
    public static Class[] getAcceptableClasses() {
        return new Class[] {
            double[].class,
            float[].class,
            long[].class,
            int[].class,
            short[].class,
            byte[].class,
        };
    }

    /**
     * Constructs a new FloatingArrayCoord.
     *
     * @param  meta   input value metadata
     * @param  isRequired   true if this coordinate is required for plotting
     * @return   new coord
     */
    public static FloatingArrayCoord createCoord( InputMeta meta,
                                                  boolean isRequired ) {
        if ( PlotUtil.storeFullPrecision() ) {

            /* Double precision implementation. */
            return new FloatingArrayCoord( meta, isRequired, true ) {

                public Object inputToStorage( Object[] values,
                                              DomainMapper[] mappers ) {
                    Object a = values[ 0 ];
                    final double[] da;
                    if ( a instanceof double[] ) {
                        da = (double[]) a;
                    }
                    else if ( a instanceof float[] ) {
                        float[] fa = (float[]) a;
                        int n = fa.length;
                        da = new double[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            da[ i ] = fa[ i ];
                        }
                    }
                    else if ( a instanceof long[] ) {
                        long[] la = (long[]) a;
                        int n = la.length;
                        da = new double[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            da[ i ] = la[ i ];
                        }
                    }
                    else if ( a instanceof int[] ) {
                        int[] ia = (int[]) a;
                        int n = ia.length;
                        da = new double[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            da[ i ] = ia[ i ];
                        }
                    }
                    else if ( a instanceof short[] ) {
                        short[] sa = (short[]) a;
                        int n = sa.length;
                        da = new double[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            da[ i ] = sa[ i ];
                        }
                    }
                    else if ( a instanceof byte[] ) {
                        byte[] ba = (byte[]) a;
                        int n = ba.length;
                        da = new double[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            da[ i ] = ba[ i ];
                        }
                    }
                    else {
                        da = new double[ 0 ];
                    }
                    return da;
                }

                public int getArrayCoordLength( TupleSequence tseq, int icol ) {
                    return ((double[]) tseq.getObjectValue( icol )).length;
                }
 
                public double[] readArrayCoord( TupleSequence tseq, int icol ) {
                    double[] dval = (double[]) tseq.getObjectValue( icol );
                    return dval;
                }
            };
        }
        else {

            /* Single precision implementation. */
            return new FloatingArrayCoord( meta, isRequired, true ) {
                public Object inputToStorage( Object[] values,
                                              DomainMapper[] mappers ) {
                    Object a = values[ 0 ];
                    final float[] fa;
                    if ( a instanceof float[] ) {
                        fa = (float[]) a;
                    }
                    else if ( a instanceof double[] ) {
                        double[] da = (double[]) a;
                        int n = da.length;
                        fa = new float[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            fa[ i ] = (float) da[ i ];
                        }
                    }
                    else if ( a instanceof long[] ) {
                        long[] la = (long[]) a;
                        int n = la.length;
                        fa = new float[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            fa[ i ] = (float) la[ i ];
                        }
                    }
                    else if ( a instanceof int[] ) {
                        int[] ia = (int[]) a;
                        int n = ia.length;
                        fa = new float[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            fa[ i ] = (float) ia[ i ];
                        }
                    }
                    else if ( a instanceof short[] ) {
                        short[] sa = (short[]) a;
                        int n = sa.length;
                        fa = new float[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            fa[ i ] = (float) sa[ i ];
                        }
                    }
                    else if ( a instanceof byte[] ) {
                        byte[] ba = (byte[]) a;
                        int n = ba.length;
                        fa = new float[ n ];
                        for ( int i = 0; i < n; i++ ) {
                            fa[ i ] = (float) ba[ i ];
                        }
                    }
                    else {
                        fa = new float[ 0 ];
                    }
                    return fa;
                }

                public int getArrayCoordLength( TupleSequence tseq, int icol ) {
                    return ((float[]) tseq.getObjectValue( icol )).length;
                }

                public double[] readArrayCoord( TupleSequence tseq, int icol ) {
                    float[] fval = (float[]) tseq.getObjectValue( icol );
                    int n = fval.length;
                    double[] dval = new double[ n ];
                    for ( int i = 0; i < n; i++ ) {
                        dval[ i ] = (double) fval[ i ];
                    }
                    return dval;
                }
            };
        }
    }
}
