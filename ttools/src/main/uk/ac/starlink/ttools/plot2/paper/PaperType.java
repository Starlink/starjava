package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Abstracts the graphic substrate that datasets can be plotted on.
 * Painting operations are performed on a supplied {@link Paper} object
 * that must be of the type appropriate to this PaperType.
 * Geometry-specific sub-interfaces provide more specific painting operations.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public interface PaperType {

    /**
     * Indicates whether this represents a bitmap or vector type of
     * graphics context.
     *
     * @return  true for pixellated surface, false for vector
     */
    boolean isBitmap();

    /**
     * Paints the content of a list of drawing objects onto a given 
     * plot surface, and returns the result as an Icon.
     *
     * <p>The <code>requireCached</code> argument provides a hint about
     * whether the output icon will cache computations.
     * Set this true if you might want to paint the returned icon 
     * multiple times, false if it is one-shot only, or if keeping
     * the memory footprint small is more important than speed.
     *
     * <p>In general it's OK to call the <code>paintIcon</code> method of
     * the returned object with a null <code>Component</code>.
     * The returned icon is the size of the <code>plotBounds</code> rectangle,
     * and will be painted at plotBounds.x, plotBounds.y.
     * It contains everything in that region except perhaps for decorations,
     * and it is opaque.  It does not (cannot) contain external axis labels,
     * but must contain any internal markings which appear underneath the
     * data points.
     *
     * <p>An implementation will usually create a Paper object and pass it
     * in turn to the supplied <code>drawings</code> so that the returned
     * icon can be based on the drawn-on paper.
     *
     * @param  surface  plot surface
     * @param  drawings  array of drawing objects to be painted in sequence
     * @param  plans   array of plan objects corresponding to the
     *                 <code>drawings</code> array argument
     * @param  dataStore  data storage object
     * @param  requireCached  hint about whether to cache the calculation data
     * @return  plotBounds-sized icon
     */
    @Slow
    Icon createDataIcon( Surface surface, Drawing[] drawings, Object[] plans,
                         DataStore dataStore, boolean requireCached );

    /**
     * Paints a Decal onto a given paper object.
     *
     * @param  paper  graphics destination, of appropriate type for this object
     * @param  decal  graphic to paint
     */
    void placeDecal( Paper paper, Decal decal );
}
