package uk.ac.starlink.array;

/**
 * Defines a real function of two real variables.
 */
public interface Combiner {

    /**
     * The function of two variables.
     *
     * @param  val1  the first variable
     * @param  val2  the second variable
     * @return  the result of the function.  May be Double.NaN.
     */
    double combination( double val1, double val2 );
}
