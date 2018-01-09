package uk.ac.starlink.ttools.plot2.layer;

import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ValueInfo;

/**
 * ColumnData implementation that presents the values from a
 * BinList.Result object, assuming bin index is the row number.
 *
 * @param  <T>  type of ValueInfo content class, should be numeric
 */
public abstract class BinResultColumnData<T> extends ColumnData {

    private final BinList.Result binResult_;
    private final double binFactor_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Constructor.
     *
     * @param  info   metadata object describing column data;
     *                the content class must match &lt;T&gt;
     * @param  binResult  object supplying the data
     * @param  binFactor  pre-multiplier for all bin result values
     */
    BinResultColumnData( ValueInfo info, BinList.Result binResult,
                         double binFactor ) {
        super( info );
        binResult_ = binResult;
        binFactor_ = binFactor;
    }

    public Object readValue( long irow ) {
        double dval = binResult_.getBinValue( irow ) * binFactor_;
        return Double.isNaN( dval ) ? null : convert( dval );
    }

    /**
     * Converts a raw (combined) data value to the content class
     * of this column.
     *
     * @param  numeric data value for a row
     * @param  object representation of <code>dval</code> as ValueInfo type
     */
    abstract T convert( double dval );

    /**
     * Returns a ColumnData instance for a given metadata object and
     * bin data set.  The content class of <code>info</code> must be
     * one of the numeric wrapper types.
     *
     * @param  info  required metadata for returned column,
     *               with some numeric content class
     * @param  binResult  supplies data
     * @param  binFactor  multiplier for all bin values;
     *                    typically obtained using Combiner.Type.getBinFactor
     * @return   new column data
     */
    public static ColumnData createInstance( ValueInfo info,
                                             BinList.Result binResult,
                                             double binFactor ) {
        Class clazz = info.getContentClass();
        if ( Byte.class.equals( clazz ) ) {
            return new BinResultColumnData<Byte>( info, binResult,
                                                  binFactor ) {
                Byte convert( double dval ) {
                    return new Byte( (byte) dval );
                }
            };
        }
        else if ( Short.class.equals( clazz ) ) {
            return new BinResultColumnData<Short>( info, binResult,
                                                   binFactor ) {
                Short convert( double dval ) {
                    return new Short( (short) dval );
                }
            };
        }
        else if ( Integer.class.equals( clazz ) ) {
            return new BinResultColumnData<Integer>( info, binResult,
                                                     binFactor ) {
                Integer convert( double dval ) {
                    return new Integer( (int) dval );
                }
            };
        }
        else if ( Long.class.equals( clazz ) ) {
            return new BinResultColumnData<Long>( info, binResult,
                                                  binFactor ) {
                Long convert( double dval ) {
                    return new Long( (long) dval );
                }
            };
        }
        else if ( Float.class.equals( clazz ) ) {
            return new BinResultColumnData<Float>( info, binResult,
                                                   binFactor ) {
                Float convert( double dval ) {
                    return new Float( (float) dval );
                }
            };
        }
        else if ( Double.class.equals( clazz ) ) {
            return new BinResultColumnData<Double>( info, binResult,
                                                    binFactor ) {
                public Double convert( double dval ) {
                    return new Double( dval );
                }
            };
        }
        else {
            logger_.warning( "Surprising data type: " + clazz + "; "
                           + "can't create ColumnData" );
            return null;
        }
    }
}
