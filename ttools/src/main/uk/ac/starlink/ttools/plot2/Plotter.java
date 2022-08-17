package uk.ac.starlink.ttools.plot2;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * A Plotter can produce PlotLayers given data and appropriate configuration.
 * It can also report what data coordinates and style configuration
 * information are needed for the plot.  This self-describing nature
 * means that a plotting framework can largely build a user interface
 * automatically from a Plotter instance.
 *
 * <p>A Plotter also acts as part of an identifier for the type of
 * plot being performed, which is necessary for determining PlotLayer
 * equality; two PlotLayers are equivalent if they match in point of
 * DataSpec, Style and Plotter.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public interface Plotter<S extends Style> {

    /**
     * Returns the name of this plotter for use in user interface.
     *
     * @return  user-directed plotter name
     */
    String getPlotterName();

    /**
     * Returns an icon for this plotter for use in user interface.
     *
     * @return  plotter icon
     */
    Icon getPlotterIcon();

    /**
     * Returns an XML description of this plotter.
     *
     * <p>Note: really this should appear at the LayerType level.
     *
     * @return   one or more &lt;p&gt; elements
     */
    String getPlotterDescription();

    /**
     * Returns an object describing which data coordinates are used for
     * this plotter and how they are arranged in supplied DataSpec objects.
     *
     * @return  coordinate group
     */
    CoordGroup getCoordGroup();

    /**
     * Returns the configuration keys used to configure style for this plotter.
     * The keys in the return value are used in the map supplied to
     * the {@link #createStyle} method.
     *
     * @return    keys used when creating a style for this plotter.
     */
    ConfigKey<?>[] getStyleKeys();

    /**
     * Creates a style that can be used when creating a plot layer.
     * The keys that are significant in the supplied config map
     * are those returned by {@link #getStyleKeys}.
     * The return value can be used as input to {@link #createLayer}.
     *
     * @param   config  map of style configuration items
     * @return   plotter-specific plot style
     */
    S createStyle( ConfigMap config ) throws ConfigException;

    /**
     * Indicates whether the drawings produced by this plotter will
     * return general interest report information to upstream plotting code.
     *
     * @return   true if the plot report may return interesting information
     * @see   Drawing#getReport
     */
    boolean hasReports();

    /**
     * Returns an opaque object characterising the region of the plot surface
     * covered when using a given plotter style.
     * If this object changes between layers produced by this plotter,
     * it provides a hint that it may be necessary to redetermine the
     * axis ranges (using
     * {@link PlotLayer#extendCoordinateRanges extendCoordinateRanges}).
     *
     * <p>In many cases, such as scatter-plot-like plotters, the range
     * is determined only by the coordinate data
     * (managed by {@link uk.ac.starlink.ttools.plot2.data.DataSpec} inputs)
     * so a null value may be returned.
     * This method is chiefly required by histogram-like plotters for which
     * the region on the plot surface is not the union of the input positions.
     *
     * @param  style   plot style to assess
     * @return  opaque object with equality semantics,
     *          or null if axis range is not a function of style
     */
    @Equality
    Object getRangeStyleKey( S style );

    /**
     * Creates a PlotLayer based on the given geometry, data and style.
     *
     * <p>The <code>style</code> parameter is the result of a call to
     * {@link #createStyle}.
     *
     * <p>The <code>dataSpec</code> parameter must contain the coordinates
     * defined by this plotter's CoordGroup.
     *
     * <p>The <code>pointDataGeom</code>
     * parameter is only used if
     * <code>getCoordGroup()</code>.
     * {@link uk.ac.starlink.ttools.plot2.data.CoordGroup#getPositionCount
     *                                                    getPositionCount}
     * returns a non-zero value,
     * otherwise the plot does not have point positions.
     *
     * <p>It is legal to supply null for any of the parameters;
     * if insufficient data is supplied to generate a plot, then
     * the method should return null.
     *
     * <p>Creating a layer should be cheap; layers may be created and not used.
     *
     * @param   pointDataGeom  indicates base position coordinates and their
     *                    mapping to points in the data space
     * @param   dataSpec  specifies the data required for the plot
     * @param   style   data style as obtained from <code>createStyle</code>
     * @return   new plot layer, or null if no drawing will take place
     */
    PlotLayer createLayer( DataGeom pointDataGeom, DataSpec dataSpec, S style );
}
