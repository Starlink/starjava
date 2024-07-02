package uk.ac.starlink.topcat;

import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.func.CoordsRadians;

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
        Class<?> clazz = info.getContentClass();

        /* Does the column represent a right ascension? */
        if ( matches( info, Tables.RA_INFO ) &&
             Number.class.isAssignableFrom( clazz ) &&
             ( "radians".equalsIgnoreCase( units ) ||
               "rad".equalsIgnoreCase( units ) ) ) {
            return new ColumnConverter[] {
                new FactorConverter( "degrees", CoordsRadians.DEGREE_RADIANS ),
                new FactorConverter( "hours", CoordsRadians.HOUR_RADIANS ),
                new UnitConverter( "radians" ),
            };
        }

        /* Does the column represent a declination? */
        else if ( matches( info, Tables.DEC_INFO ) &&
                  Number.class.isAssignableFrom( clazz ) &&
                  ( "radians".equalsIgnoreCase( units ) ||
                    "rad".equalsIgnoreCase( units ) ) ) {
            return new ColumnConverter[] {
                new FactorConverter( "degrees", CoordsRadians.DEGREE_RADIANS ),
                new UnitConverter( "radians" ),
            };
        }

        /* Does the column represent some other kind of angle?  If so, 
         * it's likely an error of some kind, so make small units available. */
        else if ( Number.class.isAssignableFrom( clazz ) &&
                  ( "radians".equalsIgnoreCase( units ) ||
                    "rad".equalsIgnoreCase( units ) ) ) {
            return new ColumnConverter[] {
                new FactorConverter( "arcsec",
                                     CoordsRadians.ARC_SECOND_RADIANS ),
                new FactorConverter( "arcmin",
                                     CoordsRadians.ARC_MINUTE_RADIANS ),
                new FactorConverter( "degrees",
                                     CoordsRadians.DEGREE_RADIANS ),
                new UnitConverter( "radians" ),
            };
        }

        /* Currently, there are no other converters in use.  Return a 
         * single degenerate converter. */
        else {
            return new ColumnConverter[] { new UnitConverter( units ) };
        }
    }

    /** 
     * Utility method to determine whether an info resembles another one.
     * Currently matches name and content class.
     *
     * @param  info1  first info
     * @param  info2  second info
     * @return  true iff <code>info1</code> matches <code>info2</code>
     */
    private static boolean matches( ValueInfo info1, ValueInfo info2 ) {
        return info1 != null
            && info2 != null
            && info1.getName().equals( info2.getName() )
            && info1.getContentClass().equals( info2.getContentClass() );
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
                 ? Double.valueOf( ((Number) value).doubleValue() * factor_ )
                 : null;
        }
        public String toString() {
            return name_;
        }
    }
}
