package uk.ac.starlink.topcat.join;

import uk.ac.starlink.table.ValueInfo;

/**
 * Encodes and decodes values for a ValueInfo between different 
 * string representations.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Sep 2004
 */
public abstract class ValueCodec {

    /**
     * Decodes a string to give a value in the context of this codec.
     *
     * @param  text  string representation of the value 
     * @return  value object as read from <code>text</code>
     */
    public abstract Object unformatString( String text );

    /**
     * Returns a string representation of a given value in the context of
     * this codec.
     * The returned string should be no longer than a given maximum length.
     *
     * @param  value  value object
     * @param  maxLength  maximum number of characters in the returned string
     * @return  string representation of <code>value</code>
     */
    public abstract String formatValue( Object value, int maxLength );

    /**
     * Returns an array of ValueCodec objects suitable for representing
     * the values pertaining to a given <code>ValueInfo</code> object.
     * The returned array will contain at least one element, which 
     * just gets its implementation from the behaviour of the submitted
     * ValueInfo itself, but it may contain more than this, for instance
     * ones relating to different units.
     *
     * @param  info  description of the value to represent
     * @return  array of value codecs for <code>info</code>, containing
     *          at least one element
     */
    public static ValueCodec[] getCodecs( ValueInfo info ) {
        String units = info.getUnitString();
        Class<?> clazz = info.getContentClass();
        if ( Number.class.isAssignableFrom( clazz ) &&
             ( units != null && ( units.equalsIgnoreCase( "radian" ) ||
                                  units.equalsIgnoreCase( "radians" ) ) ) ) {
            return new ValueCodec[] {
                new FactorCodec( info, "arcsec", Math.PI / 180 / 60 / 60 ),
                new FactorCodec( info, "arcmin", Math.PI / 180 / 60 ),
                new FactorCodec( info, "degree", Math.PI / 180 ),
                new FactorCodec( info, "radian", 1. ),
            };
        }
        else {
            return new ValueCodec[] { new UnitCodec( info ) };
        }
    }

    /**
     * Degenerate ValueCodec implementation which just defers 
     * all the work to a ValueInfo.
     */
    private static class UnitCodec extends ValueCodec {
        private final ValueInfo info_;
        public UnitCodec( ValueInfo info ) {
            info_ = info;
        }
        public Object unformatString( String text ) {
            return info_.unformatString( text );
        }
        public String formatValue( Object value, int maxLength ) {
            return info_.formatValue( value, maxLength );
        }
    }

    /**
     * ValueCodec implementation which can represent a value modified
     * by a given multiplicative factor.
     */
    private static class FactorCodec extends ValueCodec {
        private final ValueInfo info_;
        private final String name_;
        private final double factor_;
        private final double rotcaf_;

        /**
         * Constructs a new FactorCodec.
         * 
         * @param  info  value info on which it's based
         * @param  name  name of this codec (typically a unit name)
         * @param  factor   multiplicative factor
         */
        public FactorCodec( ValueInfo info, String name, double factor ) {
            info_ = info;
            name_ = name;
            factor_ = factor;
            rotcaf_ = 1.0 / factor;
        }
        public Object unformatString( String text ) {
            return multiplyObject( info_.unformatString( text ), factor_ );
        }
        public String formatValue( Object value, int maxLength ) {
            return info_.formatValue( multiplyObject( value, rotcaf_ ), 
                                      maxLength );
        }
        public String toString() {
            return name_;
        }

        /**
         * Multiplies a Number object by a constant, returning a Number object,
         * probably of the same kind.  No guarantees are made about what
         * happens if <code>value</code> is not a number.
         *
         * @param  value  value to multiply, hopefully a number
         * @param  factor constant to multiply <code>value</code> by
         * @return  object which tries to represent <code>value * factor</code>
         */
        private static Object multiplyObject( Object value, double factor ) {
            if ( value instanceof Number ) {
                double dval = ((Number) value).doubleValue() * factor;
                if ( value instanceof Float ) {
                    return new Float( (float) dval );
                }
                else {
                    return new Double( dval );
                }
            }
            else {
                return value;
            }
        }
    }
    
}
