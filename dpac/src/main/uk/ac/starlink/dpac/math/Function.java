package uk.ac.starlink.dpac.math;

/**
 * Defines a univariate function.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public interface Function {

    /**
     * Evaluates the function at a given point.
     *
     * @param   x  independent variable
     * @return   value of function at x
     */
    double f( double x );
}
