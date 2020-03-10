package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Can paint a text caption onto a graphics context in horizontal orientation.
 *
 * <p>It might be better to rework this interface so that the reference
 * position is the origin of the bounding box rather than the start of
 * the baseline which may have a descender.  That would make the calculations
 * easier - it's easy to get confused about the origins and height.
 * It would also mean this interface could just return an
 * {@link javax.swing.Icon}.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public interface Captioner {

    /**
     * Draws a caption such that the left end of the text baseline is at
     * the origin of the supplied graphics context.
     *
     * @param  label   caption text
     * @param  g  graphics context
     */
    void drawCaption( Caption label, Graphics g );

    /**
     * Returns a bounding box for the caption drawn by a corresponding call
     * to {@link #drawCaption}.
     *
     * @param  label  caption text
     * @return  bounding box for caption drawn at the origin
     */
    Rectangle getCaptionBounds( Caption label );

    /**
     * Returns a suitable padding value for separating captions from
     * the reference position or other graphical elements.
     *
     * @return   pad value in pixels
     */
    int getPad();
}
