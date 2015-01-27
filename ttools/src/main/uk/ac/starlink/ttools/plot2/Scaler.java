package uk.ac.starlink.ttools.plot2;

/**
 * Defines the mapping of an input value to the range 0..1.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2015
 */
public interface Scaler {

    /**
     * Scales an input value to the interval 0..1.
     * NaN values stay NaN.
     *
     * @param  val  input data value
     * @return  value in range 0..1 (inclusive), or NaN for NaN input
     */
    double scaleValue( double val );
}
