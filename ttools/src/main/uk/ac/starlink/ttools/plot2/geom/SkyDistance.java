package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.BasicTicker;

/**
 * Encapsulates a distance on the sky and a label for its magnitude.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2019
 */
public class SkyDistance {

    private final double radians_;
    private final String caption_;
    private static final int[] SEX_NUMBERS =
        { 1, 2, 3, 4, 5, 10, 15, 20, 30, 40, 45, 50, 60, 90, 120, 180 };
    private static final int[] DEC_NUMBERS =
        { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final String DEG_UNIT = "\u00b0";
    private static final String MIN_UNIT = "'";
    private static final String SEC_UNIT = "\"";
    private static final String MAS_UNIT = "mas";
    private static final SkyDistance DEG_DISTANCE =
        new SkyDistance( Math.PI / 180., "1" + DEG_UNIT );
    private static final SkyDistance MIN_DISTANCE =
        new SkyDistance( DEG_DISTANCE.radians_ / 60., "1" + MIN_UNIT );
    private static final SkyDistance SEC_DISTANCE =
        new SkyDistance( MIN_DISTANCE.radians_ / 60., "1" + SEC_UNIT );

    /**
     * Constructor.
     *
     * @param  radians  distance in radians
     * @param  caption  annotation giving distance as a human-readable string
     */
    public SkyDistance( double radians, String caption ) {
        radians_ = radians;
        caption_ = caption;
    }

    /**
     * Returns the distance in radians.
     *
     * @return   distance in radians
     */
    public double getRadians() {
        return radians_;
    }

    /**
     * Returns the description of this distance.
     *
     * @return   human-readable string indicating distance
     */
    public String getCaption() {
        return caption_;
    }

    @Override
    public String toString() {
        return "\"" + caption_ + "\""
             + "(" + Float.toString( (float) Math.toDegrees( radians_ ) )
             + "deg)";
    }

    /**
     * Returns a distance with a round value in the region of the
     * supplied angle.
     *
     * @param  rad  approximate distance in radians
     * @return  SkyDistance instance with a round number caption
     */
    public static SkyDistance getRoundDistance( double rad ) {
        double deg = rad * 180 / Math.PI;
        if ( deg >= 1 ) {
            return fixedRoundDistance( rad, deg, DEG_UNIT, SEX_NUMBERS, null );
        }
        double min = deg * 60;
        if ( min >= 1 ) {
            return fixedRoundDistance( rad, min, MIN_UNIT, SEX_NUMBERS,
                                       DEG_DISTANCE );
        }
        double sec = min * 60;
        if ( sec >= 1 ) {
            return fixedRoundDistance( rad, sec, SEC_UNIT, SEX_NUMBERS,
                                       MIN_DISTANCE );
        }
        double mas = sec * 1000;
        return floatRoundDistance( rad, mas, MAS_UNIT, DEC_NUMBERS,
                                   SEC_DISTANCE );
    }

    /**
     * Returns a round number distance using a fixed-point annotation.
     *
     * @param   rad   approximate angle in radians
     * @param   quant  value of <code>rad</code> scaled to target unit
     * @param   unit   unit representation
     * @param   numbers  ascending list of acceptable multiples of unit
     * @param   maxDist  value to return if the result is greater
     *                   than or nearly equal to it
     * @return   round number distance near the guide distance
     */
    private static SkyDistance fixedRoundDistance( double rad, double quant,
                                                   String unit,
                                                   int[] numbers,
                                                   SkyDistance maxDist ) {
        for ( int num : numbers ) {
            if ( quant < num ) {
                double d = num / quant * rad;
                return maxDist != null && d / maxDist.radians_ > 0.999
                     ? maxDist
                     : new SkyDistance( d, Integer.toString( num ) + unit );
            }
        }
        return null;
    }

    /**
     * Returns a round number distance using floating-point annotation.
     *
     * @param   rad   approximate angle in radians
     * @param   quant  value of <code>rad</code> scaled to target unit
     * @param   unit   unit representation
     * @param   mantissas  ascending list of acceptable mantissas,
     *                     between 0 and 10
     * @param   maxDist  value to return if the result is greater
     *                   than or nearly equal to it
     * @return   round number distance near the guide distance
     */
    private static SkyDistance floatRoundDistance( double rad, double quant,
                                                   String unit,
                                                   int[] mantissas,
                                                   SkyDistance maxDist ) {
        int exp = (int) Math.floor( Math.log10( quant ) );
        double mult = Math.pow( 10, exp );
        for ( int mantissa : mantissas ) {
            if ( mantissa * mult >= quant ) {
                double d = mantissa * mult / quant * rad;
                return maxDist != null && d / maxDist.radians_ >= 0.999
                     ? maxDist
                     : new SkyDistance( d,
                                        BasicTicker.linearLabel( mantissa, exp )
                                        + unit );
            }
        }
        return null;
    }
}
