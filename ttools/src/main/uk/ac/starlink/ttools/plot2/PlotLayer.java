package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Represents a layered element of the plot.
 * When combined with certain other information it can draw data or
 * other graphical elements onto a Surface, and report on the surface
 * region covered by such graphics.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public interface PlotLayer {

    /**
     * Returns the plotter that generated this layer.
     * Used to help determine whether this layer is the same as another one.
     *
     * @return   parent plotter
     */
    Plotter<?> getPlotter();

    /**
     * Returns the plot style used by this layer.
     *
     * @return  plot style
     */
    Style getStyle();

    /**
     * Returns the data geometry used by this layer.
     * This can be used in conjunction with the DataSpec to determine the
     * base positions in data space of what has been plotted.
     * Depending on the nature of the returned object, these positions may
     * be actual points in the data space, or some higher-dimensional object.
     * If null is returned, no such information is available.
     *
     * @return  data geom, or null
     */
    DataGeom getDataGeom();

    /**
     * Gives this layer a chance to adjust the coordinate ranges
     * assembled during data ranging.  Supplied is an array of range
     * objects, each corresponding to one of the data position dimensions
     * (it has <code>surface.getDataDimCount</code> elements).
     * If this layer needs to adjust these ranges beyond what is implied
     * by the result of <code>getDataGeom</code>, it may be done here.
     * The implementation may or may not need to acquire
     * a tuple sequence from the supplied <code>dataStore</code>.
     *
     * <p>An array of flags indicating whether each range corresponds to
     * a logarithmic axis is also supplied (same number of eements as
     * <code>ranges</code>).  This may or may not make physical sense
     * for a given case - if in doubt, false elements are given.
     *
     * <p>In many cases (especially for point-plotting type layers)
     * the implementation of this method will be a no-operation.
     *
     * @param   ranges   array of data space dimension ranges, may be adjusted
     * @param   scales      array of axis scalings
     *                      corresponding to <code>ranges</code> array
     * @param   dataStore   data storage object
     */
    void extendCoordinateRanges( Range[] ranges, Scale[] scales,
                                 DataStore dataStore );

    /**
     * Returns the data spec that defines the data used by this layer.
     * May be null if no tabular data is required.
     *
     * @return  data spec, or null
     */
    DataSpec getDataSpec();

    /**
     * Returns a map indicating what additional ranging needs to be done on
     * the input data before this layer can be drawn.
     * Each key of the returned map represents a range that needs to be
     * determined; such keys may be shared between layers in the same plot.
     * The corresponding value is an object that can be used to (help)
     * determine the range from the data.
     *
     * <p>Note that ranging of the plot surface axes themselves is
     * handled elsewhere.
     *
     * @return  range scales required for plot
     */
    Map<AuxScale,AuxReader> getAuxRangers();

    /**
     * Returns an object that describes some facts about how this layer
     * draws itself used for rendering.
     *
     * @return   layer option flags
     */
    LayerOpt getOpt();

    /**
     * Creates a drawing from this layer that can contribute to
     * a user-visible plot.
     * The <code>auxSpans</code> parameter is a map that must contain
     * a Span object for (at least) every scale returned as a
     * key of the map returned by {@link #getAuxRangers}.
     *
     * <p>If this layer is unable to draw to the given paper type,
     * an unchecked exception may be thrown.  In general it's up
     * to the plotting system to ensure that layers are only painted
     * on suitable paper types.  This logic is in {@link PlotType}.
     *
     * @param   surface  plot surface
     * @param   auxSpans   range information
     * @param   paperType  rendering object
     */
    Drawing createDrawing( Surface surface, Map<AuxScale,Span> auxSpans,
                           PaperType paperType );
}
