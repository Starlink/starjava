package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Decoration;

/**
 * Decoration subclass that also provides a target rectangle.
 * This target indicates a graphics surface region to which zooming
 * is intended.
 *
 * <p>Note that the target rectangle is not assessed as part of the
 * equality conditions for this object; it is considered to be
 * an annotation of the icon, completely determined by its existing
 * characteristics.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2014
 */
public class BandDecoration extends Decoration {

    private final Rectangle targetRect_;

    /**
     * Constructor.
     *
     * @param  icon   decoration content; this icon must have equality semantics
     * @param  gx   x position for icon
     * @param  gy   y position for icon
     * @param  targetRect  target rectangle
     */
    public BandDecoration( Icon icon, int gx, int gy, Rectangle targetRect ) {
        super( icon, gx, gy );
        targetRect_ = targetRect;
    }

    /**
     * Returns the target rectangle for this object.
     *
     * @return  target rectangle
     */
    public Rectangle getTargetRectangle() {
        return targetRect_;
    }
}
