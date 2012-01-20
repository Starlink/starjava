package uk.ac.starlink.ttools.plot;

import java.awt.Graphics2D;
import java.io.IOException;

/**
 * Interface for a self-contained object which can paint itself on a
 * graphics context.
 * It's very like an {@link javax.swing.Icon}, but intended for
 * use in contexts which may be headless.
 *
 * @author   Mark Taylor
 * @since    20 Jan 2012
 */
public interface Picture {

    /**
     * Get horizontal extent.
     *
     * @return   width in pixels
     */
    int getPictureWidth();

    /**
     * Get vertical extent.
     *
     * @return  height in pixels
     */
    int getPictureHeight();

    /**
     * Paint the content of this painting on the given graphics context.
     * The intended graphics content ought only to extend between
     * 0 and width on the X axis and 0 and height on the Y axis.
     *
     * @param   g2  graphics context
     */
    void paintPicture( Graphics2D g2 ) throws IOException;
}
