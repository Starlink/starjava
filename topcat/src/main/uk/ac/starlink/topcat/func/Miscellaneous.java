package uk.ac.starlink.topcat.func;

/**
 * Miscellaneous mathematical functions for use in JEL expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Jul 2004
 */
public class Miscellaneous {

    private static double LOG10_FACTOR = 1.0 / Math.log( 10.0 );

    /**
     * Logarithm to base 10.
     *
     * @param  x  argument
     * @return   log<sub>10</sub>(x)
     */
    public static double log10( double x ) {
        return LOG10_FACTOR * Math.log( x );
    }

    /**
     * Natural logarithm.
     *
     * @param  x  argument
     * @return   log<sub>e</sub>(x)
     */
    public static double ln( double x ) {
        return Math.log( x );
    }
}
