package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Sub-interface of Surface for surface types that are
 * basically two-dimensional and Cartesian.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2016
 */
public interface PlanarSurface extends Surface {

    /**
     * Indicates the scaling along the two axes.
     *
     * @return  2-element array giving horizontal, vertical axis scales
     */
    public Scale[] getScales();

    /**
     * Indicates which axes are reversed.
     *
     * @return  2-element array giving horizontal, vertical flip flags;
     *          true to invert normal plot direction
     */
    public boolean[] getFlipFlags();

    /**
     * Indicates which axes represent time values.
     *
     * @return  2-element array giving horizontal, vertical time flags;
     *          true for time axis with data units of seconds,
     *          false for normal numeric axis
     */
    public boolean[] getTimeFlags();

    /**
     * Returns the axis objects used by this surface.
     *
     * @return  2-element array giving horizontal, vertical axis instances
     */
    public Axis[] getAxes();

    /**
     * Returns the limits in data coordinates of the plot region.
     *
     * @return  2x2 array <code>{{xlo, xhi}, {ylo, yhi}}</code>
     */
    public double[][] getDataLimits();
}
