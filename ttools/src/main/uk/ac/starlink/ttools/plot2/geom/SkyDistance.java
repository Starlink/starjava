package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.Caption;
import uk.ac.starlink.ttools.plot2.BasicTicker;

/**
 * Encapsulates a distance on the sky and a label for its magnitude.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2019
 */
public class SkyDistance {

    private final double radians_;
    private final Caption caption_;
    private static final int[] SEX_NUMBERS =
        { 1, 2, 3, 4, 5, 10, 15, 20, 30, 40, 45, 50, 60, 90, 120, 180 };
    private static final int[] DEC_NUMBERS =
        { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private static final Caption DEG_UNIT =
        Caption.createCaption( "\u00b0", "^\\circ" );
    private static final Caption MIN_UNIT =
        Caption.createCaption( "'", "^\\prime" );
    private static final Caption SEC_UNIT =
        Caption.createCaption( "\"", "^{\\prime\\prime}" );
    private static final Caption MAS_UNIT =
        Caption.createCaption( "mas" );
    private static final SkyDistance DEG_DISTANCE =
        new SkyDistance( Math.PI / 180.,
                         unitCaption( 1, DEG_UNIT ) );
    private static final SkyDistance MIN_DISTANCE =
        new SkyDistance( DEG_DISTANCE.radians_ / 60.,
                         unitCaption( 1, MIN_UNIT ) );
    private static final SkyDistance SEC_DISTANCE =
        new SkyDistance( MIN_DISTANCE.radians_ / 60.,
                         unitCaption( 1, SEC_UNIT ) );

    /**
     * Constructor.
     *
     * @param  radians  distance in radians
     * @param  caption  annotation giving distance as a human-readable string
     */
    public SkyDistance( double radians, Caption caption ) {
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
     * @return   human-readable label indicating distance
     */
    public Caption getCaption() {
        return caption_;
    }

    @Override
    public String toString() {
        return "\"" + caption_.toText() + "\""
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
                                                   Caption unit,
                                                   int[] numbers,
                                                   SkyDistance maxDist ) {
        for ( int num : numbers ) {
            if ( quant < num ) {
                double d = num / quant * rad;
                return maxDist != null && d / maxDist.radians_ > 0.999
                     ? maxDist
                     : new SkyDistance( d, unitCaption( num, unit ) );
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
                                                   Caption unit,
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
                                       .append( unit ) );
            }
        }
        return null;
    }

    /**
     * Concatenates a number and a unit caption to provide a caption
     * giving the result.
     *
     * @param   quantity  numeric quantity
     * @param   unit  unit representation
     * @return   labelled representation of the quantity
     */
    private static Caption unitCaption( int quantity, Caption unit ) {
        return Caption.createCaption( Integer.toString( quantity ) )
              .append( unit );
    }
}
