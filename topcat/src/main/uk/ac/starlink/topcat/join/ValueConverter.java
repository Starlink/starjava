package uk.ac.starlink.topcat.join;

import uk.ac.starlink.table.ValueInfo;

/**
 * Converts values for a ValueInfo between different string representations.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Sep 2004
 */
public abstract class ValueConverter {

    /**
     * Decodes a string to give a value in the context of this converter.
     *
     * @param  text  string representation of the value 
     * @return  value object as read from <tt>text</tt>
     */
    public abstract Object unformatString( String text );

    /**
     * Returns a string representation of a given value in the context of
     * this converter.
     * The returned string should be no longer than a given maximum length.
     *
     * @param  value  value object
     * @param  maxLength  maximum number of characters in the returned string
     * @return  string representation of <tt>value</tt>
     */
    public abstract String formatValue( Object value, int maxLength );


    /**
     * Returns an array of ValueConverter objects suitable for representing
     * the values pertaining to a given <tt>ValueInfo</tt> object.
     * The returned array will contain at least one element, which 
     * just gets its implementation from the behaviour of the submitted
     * ValueInfo itself, but it may contain more than this, for instance
     * ones relating to different units.
     *
     * @param  info  description of the value to represent
     * @return  array of value converters for <tt>info</tt>, containing
     *          at least one element
     */
    public static ValueConverter[] getConverters( ValueInfo info ) {
        String units = info.getUnitString();
        Class clazz = info.getContentClass();
        if ( Number.class.isAssignableFrom( clazz ) &&
             ( units != null && ( units.equalsIgnoreCase( "radian" ) ||
                                  units.equalsIgnoreCase( "radians" ) ) ) ) {
            return new ValueConverter[] {
                new FactorConverter( info, "arcsec", Math.PI / 180 / 60 / 60 ),
                new FactorConverter( info, "arcmin", Math.PI / 180 / 60 ),
                new FactorConverter( info, "degree", Math.PI / 180 ),
                new FactorConverter( info, "radian", 1. ),
            };
        }
        else {
            return new ValueConverter[] { new UnitConverter( info ) };
        }
    }

    /**
     * Degenerate ValueConverter implementation which just defers 
     * all the work to a ValueInfo.
     */
    private static class UnitConverter extends ValueConverter {
        private final ValueInfo info_;
        public UnitConverter( ValueInfo info ) {
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
     * ValueConverter implementation which can represent a value modified
     * by a given multiplicative factor.
     */
    private static class FactorConverter extends ValueConverter {
        private final ValueInfo info_;
        private final String name_;
        private final double factor_;
        private final double rotcaf_;

        /**
         * Constructs a new FactorConverter.
         * 
         * @param  info  value info on which it's based
         * @param  name  name of this converter (typically a unit name)
         * @param  factor   multiplicative factor
         */
        public FactorConverter( ValueInfo info, String name, double factor ) {
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
         * happens if <tt>value</tt> is not a number.
         *
         * @param  value  value to multiply, hopefully a number
         * @param  factor constant to multiply <tt>value</tt> by
         * @return  object which tries to represent <tt>value * factor</tt>
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
