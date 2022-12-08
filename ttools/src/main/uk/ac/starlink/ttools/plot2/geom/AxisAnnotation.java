package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics;
import uk.ac.starlink.ttools.plot2.Surround;

/**
 * Defines text labelling to decorate a plot.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public interface AxisAnnotation {

    /**
     * Returns the padding space around the edge of a plot bounds rectangle
     * required to accommodate the labels painted by this object.
     *
     * @param  withScroll  true if the padding should be large enough to
     *                     accommodate labelling requirements if the
     *                     surface is scrolled
     * @return  padding surround
     */
    Surround getSurround( boolean withScroll );

    /**
     * Paints the annotations.
     * They should fit in the padding region defined by the
     * result of the {link #getPadding} method.
     *
     * @param  g  graphics context
     */
    void drawLabels( Graphics g );
}
