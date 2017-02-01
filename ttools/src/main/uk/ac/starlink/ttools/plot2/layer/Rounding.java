package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot.Rounder;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines a policy for coming up with round numbers.
 *
 * @author   Mark Taylor
 * @since    1 Feb 2017
 */
@Equality
public abstract class Rounding {

    /** Policy suitable for normal numeric axes. */
    public static Rounding DECIMAL = new Rounding() {
        public Rounder getRounder( boolean isLog ) {
            return isLog ? Rounder.LOG : Rounder.LINEAR;
        }
    };

    /** Policy suitable for time axes with data units of seconds. */
    public static Rounding SECONDS = new Rounding() {
        public Rounder getRounder( boolean isLog ) {
            return isLog ? Rounder.LOG : Rounder.TIME_SECOND;
        }
    };

    /**
     * Returns a rounder object for linear/logarithmic rounding.
     *
     * @param  isLog  true for logarithmic, false for linear
     * @return  rounder
     */
    public abstract Rounder getRounder( boolean isLog );

    /**
     * Returns a suitable implementation for an axis that either is or is
     * not a time axis.
     *
     * @param   isTime  true for time axis in seconds, false for normal numeric
     * @return   rounding instance
     */
    public static Rounding getRounding( boolean isTime ) {
        return isTime ? SECONDS : DECIMAL;
    }
}
