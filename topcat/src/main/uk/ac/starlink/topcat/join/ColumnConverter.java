package uk.ac.starlink.topcat.join;

import uk.ac.starlink.table.ValueInfo;

/**
 * Performs unit conversions on data values based on a given ValueInfo.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Sep 2004
 */
public abstract class ColumnConverter {

    /**
     * Converts a value from its raw value to the value required for
     * a particular purpose.
     *
     * @param  value  raw value
     * @return   converted value
     */
    public abstract Object convertValue( Object value );

    /**
     * Provides a user-understandable description of what this converter
     * does.  Must be enough to distinguish it from distinct converters
     * which might do the same sort of thing.
     */
    public abstract String toString();

    /**
     * Returns a set of converters appropriate for a given ValueInfo.
     * If only one converter is returned, it's a unit converter
     * (equivalent to a no-op).
     *
     * @param  info  object describing the data which is required
     * @return  a set of alternative converters which could be used to 
     *          modify the values in a given column
     */
    public static ColumnConverter[] getConverters( ValueInfo info ) {
        String units = info.getUnitString();
        Class clazz = info.getContentClass();

        /* If the base info has units of radians, return converters for
         * raw values of radians or degrees. */
        if ( Number.class.isAssignableFrom( clazz ) &&
             ( units != null && ( units.equalsIgnoreCase( "radian" ) ||
                                  units.equalsIgnoreCase( "radians" ) ) ) ) {
            return new ColumnConverter[] {
                new FactorConverter( "degree", Math.PI / 180 ),
                new UnitConverter( "radian" ),
            };
        }

        /* Currently, there are no other converters in use.  Return a 
         * single degenerate converter. */
        else {
            return new ColumnConverter[] { new UnitConverter( units ) };
        }
    }

    /**
     * Degenerate converter implementation.
     */
    private static class UnitConverter extends ColumnConverter {
        final String name_;
        public UnitConverter( String name ) {
            name_ = name;
        }
        public Object convertValue( Object value ) {
            return value;
        }
        public String toString() {
            return name_;
        }
    }

    /**
     * Converter implementation that multiplies by a factor.
     */
    private static class FactorConverter extends ColumnConverter {
        final String name_;
        final double factor_;
        public FactorConverter( String name, double factor ) {
            name_ = name;
            factor_ = factor;
        }
        public Object convertValue( Object value ) {
            return value instanceof Number 
                 ? new Double( ((Number) value).doubleValue() * factor_ )
                 : null;
        }
        public String toString() {
            return name_;
        }
    }
}
