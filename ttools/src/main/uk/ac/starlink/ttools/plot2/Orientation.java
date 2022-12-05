package uk.ac.starlink.ttools.plot2;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Defines text orientation for axis labelling.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public abstract class Orientation {

    /** Orientation suitable for X axis labelling. */
    public static final Orientation X = new Orientation() {
        public AffineTransform captionTransform( Rectangle bounds, int pad ) {
            return AffineTransform
                  .getTranslateInstance( -bounds.width / 2, -bounds.y + pad );
        }
        public boolean isDown() {
            return true;
        }
    };

    /** Orientation suitable for Y axis labelling. */
    public static final Orientation Y = new Orientation() {
        public AffineTransform captionTransform( Rectangle bounds, int pad ) {
            AffineTransform trans = new AffineTransform();
            trans.rotate( +0.5 * Math.PI );
            trans.translate( -bounds.width - pad, -bounds.y / 2 );
            return trans;
        }
        public boolean isDown() {
            return false;
        }
    };

    /** Orientation suitable for labelling top-edge X axis. */
    public static final Orientation ANTI_X = new Orientation() {
        public AffineTransform captionTransform( Rectangle bounds, int pad ) {
            return AffineTransform
                  .getTranslateInstance( -bounds.width / 2, -pad );
        }
        public boolean isDown() {
            return false;
        }
    };

    /** Orientation suitable for labelling right-hand Y axis. */
    public static final Orientation ANTI_Y = new Orientation() {
        public AffineTransform captionTransform( Rectangle bounds, int pad ) {
            AffineTransform trans = new AffineTransform();
            trans.rotate( +0.5 * Math.PI );
            trans.translate( pad, -bounds.y / 2 );
            return trans;
        }
        public boolean isDown() {
            return true;
        }
    };

    /**
     * Returns a transformation suitable for writing axis captions.
     * If a graphics context is positioned with the point to be
     * annotated at the origin, applying the returned transformation gives a
     * graphics context on which a caption with the given bounding box
     * can be painted.  The origin of the bounds should be the baseline
     * at the start of the line, its height should reflect the maximum
     * font height, and the width should be the actual width.
     */
    public abstract AffineTransform captionTransform( Rectangle bounds,
                                                      int pad );

    /** 
     * Indicates whether the positive Y direction points towards the axis.
     *
     * @return   true for axis below text, false for axis above text
     */
    public abstract boolean isDown();
}
