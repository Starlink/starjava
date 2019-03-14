package uk.ac.starlink.ttools.plot2;

/**
 * Defines how to get a ShadeAxis for a shader range.
 *
 * @since   25 Sep 2014
 */
public interface ShadeAxisFactory {

    /**
     * Indicates whether the axis this factory will produce will have a
     * logarithmic scale.
     *
     * @return   true for logarithmic, false for linear
     */
    public boolean isLog();

    /**
     * Returns a shade axis for a given span.
     *
     * @param   span  data range
     * @return   shader axis
     */
    ShadeAxis createShadeAxis( Span span );
}
