package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;

/**
 * Defines a style for marking a set of data.
 * This interface currently defines only a method for drawing an
 * example marker for use in a legend, but plot-type-specific classes
 * will probably have to define additional methods.
 *
 * <p>Note it is essential that Style implementations provide implementations
 * of <code>equals()</code> (and hence also of <code>hashCode()</code>)
 * for which equality means that styles look the same as each other.
 * An <code>equals</code> implementation based on identity (inherited
 * from the behaviour of <code>Object</code>) will lead to poor performance
 * of the plotting classes.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2005
 */
public interface Style {

    /**
     * Draws a legend for this marker centered at a given position.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    public void drawLegend( Graphics g, int x, int y );
}
