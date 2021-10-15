package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Knows how to turn a number of offset points to a drawable glyph.
 *
 * <p>Note that this is <em>not</em> a {@link java.lang.FunctionalInterface},
 * since instances need in general to implement
 * {@link uk.ac.starlink.ttools.plot2.Equality}.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2021
 */
@Equality
public interface MultiPointScribe {

    /**
     * Returns a glyph representing the this object's rendering of
     * a given offset array.  The glyph is considered to be centered
     * at the origin, so the offsets will usually surround (0,0).
     * The two input arrays must be of the same size.
     *
     * <p>A common usage is for error bars; in this case there are
     * typically (2*N) offsets, representing errors in N dimensions.
     * Error bars come in consecutive pairs which describe
     * error bars along the same axis in different directions.
     * Missing error bars are represented as (0,0).  The values must come
     * in axis order where that makes sense, but note in some contexts
     * (e.g. 3D) these may be data axes rather than graphics plane axes.
     *
     * <p>This method is quite likely to get called from time to time with
     * ridiculously large offset arrays.  Implementations should try to
     * ensure that they don't attempt graphics operations which may
     * cause the graphics system undue grief, such as filling an ellipse
     * the size of a village.
     *
     * @param  xoffs  X coordinates of point offsets
     * @param  yoffs  Y coordinates of point offsets
     * @return  glyph displaying shape
     */
    Glyph createGlyph( int[] xoffs, int[] yoffs );
}
