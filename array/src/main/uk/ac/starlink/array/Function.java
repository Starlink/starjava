package uk.ac.starlink.array;

/**
 * Defines a real function of a real variable <i>y=f(x)</i>
 * and its inverse.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface Function {

    /**
     * The forward function.  If only the inverse function will be required,
     * it is permissible to throw an UnsupportedOperationException.
     *
     * @param  x  the argument <i>x</i> of the function
     * @return    the return value <i>y</i> of the function.
     *            May be Double.NaN.
     */
    double forward( double x );

    /**
     * The inverse function.  If only the forward function will be required,
     * it is permissible to throw an UnsupportedOperationException.
     *
     * @param  y  the argument <i>y</i> of the inverse function
     * @return    the return value <i>x</i> of the inverse function
     *            May be Double.NaN.
     */
    double inverse( double y );
}
