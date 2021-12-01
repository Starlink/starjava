package uk.ac.starlink.cdf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.ac.bristol.star.cdf.DataType;
import uk.ac.bristol.star.cdf.EpochFormatter;
import uk.ac.bristol.star.cdf.TtScaler;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;

/**
 * Utility class for identifying domain mappers for CDF datatypes.
 * Currently the epoch-like columns are identified as in the Time domain.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class CdfDomains {

    private static final double AD0_UNIX_SECONDS =
        EpochFormatter.AD0_UNIX_MILLIS * 1e-3;
    private static final Map<DataType,DomainMapper> mapperMap_ =
        createMapperMap();

    /**
     * Returns an appropriate DomainMapper for a given CDF data type.
     *
     * @param   dtype   data type
     * @return   appropriate domain mapper, or null
     */
    public static DomainMapper getMapper( DataType dtype ) {

        /* TIME_TT2000 has rather strange equality semantics, so map any
         * TIME_TT2000 data type to the canonical one.
         * It's a leap second thing. */
        if ( dtype != null &&
             dtype.getName().equals( DataType.TIME_TT2000.getName() ) ) {
            dtype = DataType.TIME_TT2000;
        }
        return mapperMap_.get( dtype );
    }

    /**
     * Constructs a map from data type to value domain.
     *
     * @return  new type->mapper map
     */
    private static Map<DataType,DomainMapper> createMapperMap() {
        Map<DataType,DomainMapper> map = new HashMap<DataType,DomainMapper>();
        map.put( DataType.EPOCH, new EpochTimeMapper() );
        map.put( DataType.EPOCH16, new Epoch16TimeMapper() );
        map.put( DataType.TIME_TT2000, new Tt2000TimeMapper() );
        return Collections.unmodifiableMap( map );
    }

    /**
     * TimeMapper implementation for CDF_EPOCH data type.
     */
    private static class EpochTimeMapper extends TimeMapper {
        EpochTimeMapper() {
            super( Double.class, "CDF_EPOCH", "Milliseconds since AD 0" );
        }
        public double toUnixSeconds( Object value ) {
            if ( value instanceof Double ) {
                double ad0Millis = ((Double) value).doubleValue();
                double ad0Seconds = ad0Millis * 1e-3;
                return AD0_UNIX_SECONDS + ad0Seconds;
            }
            else {
                return Double.NaN;
            }
        }
    }

    /**
     * TimeMapper implementation for CDF_EPOCH16 data type.
     */
    private static class Epoch16TimeMapper extends TimeMapper {
        Epoch16TimeMapper() {
            super( double[].class, "CDF_EPOCH16",
                   "[seconds, picoseconds] since AD 0" );
        }
        public double toUnixSeconds( Object value ) {
            if ( value instanceof double[] ) {
                double[] array = (double[]) value;
                if ( array.length == 2 ) {
                    double ad0Seconds = array[ 0 ];
                    double plusPicos = array[ 1 ];
                    return AD0_UNIX_SECONDS + ad0Seconds + plusPicos * 1e-12;
                }
                else {
                    return Double.NaN;
                }
            }
            else {
                return Double.NaN;
            }
        }
    }

    /**
     * TimeMapper implementation for CDF_TIME_TT2000 data type.
     */
    private static class Tt2000TimeMapper extends TimeMapper {
        private int iLastScaler_;
        private final TtScaler[] scalers_;
        Tt2000TimeMapper() {
            super( Long.class, "CDF_TIME_TT2000",
                   "TT milliseconds since J2000" );
            scalers_ = TtScaler.getTtScalers();
        }
        public double toUnixSeconds( Object value ) {
            if ( value instanceof Long ) {
                long timeTt2k = ((Long) value).longValue();
                long tt2kMillis = timeTt2k / 1000000;
                int plusNanos = (int) ( timeTt2k % 1000000 );
                if ( plusNanos < 0 ) {
                    tt2kMillis--;
                    plusNanos += 1000000;
                }
                int index = TtScaler.getScalerIndex( tt2kMillis, scalers_,
                                                     iLastScaler_ );
                iLastScaler_ = index;
                return scalers_[ index ].tt2kToUnixMillis( tt2kMillis ) * 1e-3
                     + plusNanos * 1e-9;
            }
            else {
                return Double.NaN;
            }
        }
    }
}
