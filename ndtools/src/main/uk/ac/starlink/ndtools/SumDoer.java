package uk.ac.starlink.ndtools;

/**
 * Defines a two-parameter arithmetic operation.
 *
 * @see  ConstArithmetic
 */
interface SumDoer {

    /**
     * Performs arithmetic on two <tt>double</tt> values and returns a result.
     *
     * @param  var   the first value (the variable one)
     * @param  konst  the second value (the constant one)
     * @return  the result of combining <tt>var</tt> and <tt>konst</tt>
     */
    double doSum( double var, double konst );
}
