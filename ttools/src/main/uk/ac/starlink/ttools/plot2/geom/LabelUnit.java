package uk.ac.starlink.ttools.plot2.geom;

import java.util.Arrays;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Utility class for working with units for labelling extents.
 * It is not currently intended for use with absolute values.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class LabelUnit implements Comparable<LabelUnit> {

    private final String name_;
    private final double factor_;

    /**
     * Constructor.
     *
     * @param  name  human-readable unit name for annotating values
     * @param  factor   size of this unit in terms of some standard unit
     */
    public LabelUnit( String name, double factor ) {
        name_ = name;
        factor_ = factor;
    }

    /**
     * Returns this unit's name as used for annotating values.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the size of this unit in terms of some standard unit.
     *
     * @return   factor
     */
    public double getFactor() {
        return factor_;
    }

    public int compareTo( LabelUnit other ) {
        return Double.compare( this.getFactor(), other.getFactor() );
    }

    /**
     * Returns an appropriate unit for annotating the given value.
     *
     * @param  value  value in standard units
     * @param  units  list of available units
     * @return  most suitable entry from supplied units list
     */
    public static LabelUnit getUnit( double value, LabelUnit[] units ) {
        LabelUnit[] sunits = units.clone();
        Arrays.sort( units );
        for ( int i = 1; i < sunits.length; i++ ) {
            if ( value < units[ i ].factor_ ) {
                return units[ i - 1 ];
            }
        }
        return units[ units.length - 1 ];
    }

    /**
     * Formats a given number giving its value in a sensible precision
     * using a suitable unit.
     *
     * @param   value  numeric value in standard units
     * @param   eps    approximate precision of value in standard units
     * @param   units  list of available units
     * @return  string giving numeric value, with unit name appended 
     */
    public static String formatValue( double value, double eps,
                                      LabelUnit[] units ) {
        LabelUnit unit = getUnit( value, units );
        double factor = unit.getFactor();
        return PlotUtil.formatNumber( value / factor, eps / factor )
             + unit.getName();
    }
}
