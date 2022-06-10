package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Paints the shape of per-point markers.
 * When painting, this object will not manipulate the colours;
 * any colouring is done externally.
 *
 * <p>This interface provides two ways of drawing the same thing:
 * with a {@link ShapePainter}, which does it a point at a time, 
 * and with a BinPlan, which accumulates all the pixel values for the
 * whole grid ready to paint in one go.  They should represent the same
 * data, it's up to the caller which it uses.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
@Equality
public interface Outliner {

    /**
     * Returns an icon suitable for identifying points painted by this
     * object in a plot legend.  The returned icon does not manipulate
     * the colour of the graphics context it operates on.
     *
     * @return  legend icon for this outliner
     */
    Icon getLegendIcon();

    /**
     * Defines what non-axis ranging information is required by this outliner.
     * The return value is a map in which the keys define the ranges
     * that must be supplied to the painting methods, and the values
     * are reader objects that can acquire these ranges from a dataset.
     *
     * @param  geom  data geometry
     * @return  map of required scale keys to scale reader objects
     */
    Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom );

    /**
     * Indicates whether this outliner is willing to provide painters
     * for a given DataSpec.
     * The return value may be unconditionally true, but this method
     * provides a hook for outliners to reject plotting based only
     * on characteristics of the DataSpec.
     *
     * @param  dataSpec  data specification for plot
     * @return   true if dataSpec can be used to obtain a non-null painter
     */
    boolean canPaint( DataSpec dataSpec );

    /**
     * Creates a ShapePainter object for plotting onto 2-dimensional surfaces.
     * This method should only be called if {@link #canPaint}
     * returns true for the supplied DataSpec.
     *
     * @param   surface   plot surface
     * @param   geom   coordinate geometry
     * @param   dataSpec  data specification
     * @param   auxSpans  map of scale information required for plot
     * @param   paperType  2-d paper type
     * @return  new 2-d painter
     */
    ShapePainter create2DPainter( Surface surface, DataGeom geom,
                                  DataSpec dataSpec,
                                  Map<AuxScale,Span> auxSpans,
                                  PaperType2D paperType );

    /**
     * Creates a ShapePainter object for plotting onto 3-dimensional surfaces.
     * This method should only be called if {@link #canPaint}
     * returns true for the supplied DataSpec.
     *
     * @param   surface   3-d plot surface
     * @param   geom   coordinate geometry
     * @param   dataSpec  data specification
     * @param   auxSpans  map of scale information required for plot
     * @param   paperType  3-d paper type
     * @return  new 3-d painter
     */
    ShapePainter create3DPainter( CubeSurface surface, DataGeom geom,
                                  DataSpec dataSpec,
                                  Map<AuxScale,Span> auxSpans,
                                  PaperType3D paperType );

    /**
     * Calculates an opaque object which contains the drawing of this
     * outliner represented as an array of bins, one per pixel.
     * To make sense of the returned object, use the
     * {@link #getBinCounts} and {@link #getPointCount} methods.
     *
     * <p>If one of the supplied knowPlans fits the bill, it will be
     * returned without further calculation.
     *
     * @param   surface   plot surface
     * @param   geom   coordinate geometry
     * @param   auxSpans  map of scale information required for plot
     * @param   dataStore  data storage
     * @param   dataSpec   coordinate specification
     * @param   knownPlans  list of existing plans
     * @return   bin plan, either newly calculated or taken from
     *           <code>knownPlans</code>
     */
    Object calculateBinPlan( Surface surface, DataGeom geom,
                             Map<AuxScale,Span> auxSpans,
                             DataStore dataStore, DataSpec dataSpec,
                             Object[] knownPlans );

    /**
     * Returns the bin contents for a given bin plan produced by
     * this object.
     *
     * @param  binPlan  bin plan returned from <code>calculateBinPlan</code>
     * @return   array of counts, one element per bin
     */
    int[] getBinCounts( Object binPlan );

    /**
     * Returns the number of data positions which contributed to 
     * a BinPlan generated by this object.
     * This may or may not be the same as the sum of the pixel counts
     * in the binCounts array; if each data position might contribute
     * multiple pixel hits, the numbers will not be the same.
     *
     * @param  binPlan  bin plan returned from <code>calculateBinPlan</code>
     * @return   number of data positions in plan
     */
    long getPointCount( Object binPlan );
}
