package uk.ac.starlink.vo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides some way of converting a string value into a numeric.
 * A number of useful subclasses are provided.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public abstract class ValueConverter {

    private final String name_;
    private final static Pattern SEX_PATTERN = Pattern.compile(
        " *([+\\-]?)" +
        " *([0-9]+) *[: ]" +
        " *([0-9]+) *[: ]" +
        " *([0-9]+\\.?[0-9]*) *"
    );

    /**
     * Constructor.
     *
     * @param  name   format name (suitable for display in a combo box)
     */
    public ValueConverter( String name ) {
        name_ = name;
    }

    /**
     * Converts a string value to a numeric for this format
     *
     * @param   sval  string value
     * @return   numeric equivalent of <tt>sval</tt>
     * @throws   IllegalArugmentException if <tt>sval</tt> doesn't make
     *           sense to this converter
     */
    public abstract double convertValue( String sval );

    /**
     * Returns format name.
     *
     * @return   name
     */
    public String getName() {
        return name_;
    }

    public String toString() {
        return getName();
    }

    /** 
     * ValueConverter class which scales by a given factor
     * (represents a particular unit).
     */
    public static class UnitValueConverter extends ValueConverter {
        private final double factor_;

        /**
         * Constructor.
         *
         * @param  name  unit name
         * @param  factor unit conversion factor
         */
        public UnitValueConverter( String name, double factor ) {
            super( name );
            factor_ = factor;
        }
        public double convertValue( String sval ) {
            return Double.parseDouble( sval ) * factor_;
        }
    }

    /**
     * Converter for degrees:minutes:seconds format.
     */
    public static class DMSDegreesValueConverter extends ValueConverter {
        public DMSDegreesValueConverter() {
            super( "dd:mm:ss" );
        }
        public double convertValue( String sval ) {
            return dmsToDegrees( sval );
        }
    }

    /**
     * Converter for hours:minute:seconds format.
     */
    public static class HMSDegreesValueConverter extends ValueConverter {
        public HMSDegreesValueConverter() {
            super( "hh:mm:ss" );
        }
        public double convertValue( String sval ) {
            return 15.0 * dmsToDegrees( sval );
        }
    }

    /**
     * Converts DD:MM:SS type string into degrees.
     *
     * @param  dms  dms string
     */
    private static double dmsToDegrees( String dms ) {
        Matcher matcher = SEX_PATTERN.matcher( dms );
        if ( ! matcher.matches() ) {
            throw new NumberFormatException( "\"" + dms +
                                             "\" not sexagesimal" );
        }
        try {
            double sign = "-".equals( matcher.group( 1 ) ) ? -1.0 : +1.0;
            int deg = Integer.parseInt( matcher.group( 2 ) );
            int min = Integer.parseInt( matcher.group( 3 ) );
            double sec = Double.parseDouble( matcher.group( 4 ) );
            return sign * ( deg + ( min + sec / 60.0 ) / 60.0 );
        }
        catch ( IllegalArgumentException e ) {
            throw new NumberFormatException( "\"" + dms +
                                             "\" not sexagesimal" );
        }
    }

}
