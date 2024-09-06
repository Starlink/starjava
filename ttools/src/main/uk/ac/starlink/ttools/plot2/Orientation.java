package uk.ac.starlink.ttools.plot2;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Defines text orientation for axis labelling.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
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
     *
     * @param  bounds  rectangle enclosing caption text
     * @param  pad    number of pixels gap between caption and axis
     * @return  transform applied to graphics context for writing caption text
     */
    public abstract AffineTransform captionTransform( Rectangle bounds,
                                                      int pad );

    /** 
     * Indicates whether the positive Y direction points towards the axis.
     *
     * @return   true for axis below text, false for axis above text
     */
    public abstract boolean isDown();

    /**
     * Returns an orientation suitable for X axis labelling, in which
     * labels are rotated by a given angle.
     *
     * @param  thetaDeg  rotation angle clockwise in degrees,
     *                   usually between 0 and 90
     * @param  isAnti   true for top-edge X axis, false for bottom-edge
     * @return  new orientation
     */
    public static Orientation createAngledX( double thetaDeg, boolean isAnti ) {
        return new AngledXOrientation( -Math.toRadians( thetaDeg ), isAnti );
    }

    /**
     * Orientation suitable for X axis labelling in which labels are
     * rotated by a given angle.
     */
    private static class AngledXOrientation extends Orientation {

        private final double thetaRad_;
        private final boolean isAnti_;
        private final double cosTheta_;
        private final AffineTransform rotateTransform_;

        /**
         * Constructor.
         *
         * @param  thetaRad  rotation angle anticlockwise in radians
         * @param  isAnti   true for top-edge X axis, false for bottom-edge
         */
        AngledXOrientation( double thetaRad, boolean isAnti ) {
            thetaRad_ = thetaRad;
            isAnti_ = isAnti;
            cosTheta_ = Math.cos( thetaRad_ );
            rotateTransform_ = AffineTransform.getRotateInstance( thetaRad_ );
        }

        public AffineTransform captionTransform( Rectangle bounds, int pad ) {
            if ( isAnti_ ) {
                AffineTransform trans = new AffineTransform();
                trans.translate( -0.5 * bounds.y * cosTheta_, -pad );
                trans.rotate( thetaRad_ );
                return trans;
            }
            else {
                AffineTransform trans = new AffineTransform( rotateTransform_ );
                trans.translate( -bounds.width, -bounds.y / 2 );
                trans.rotate( -thetaRad_ );
                trans.translate( 0, pad - 0.5 * cosTheta_ * bounds.y );
                trans.rotate( thetaRad_ );
                return trans;
            }
        }

        public boolean isDown() {
            return ! isAnti_;
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits( (float) thetaRad_ )
                 * ( isAnti_ ? -1 : +1 );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof AngledXOrientation ) {
                AngledXOrientation other = (AngledXOrientation) o;
                return this.thetaRad_ == other.thetaRad_
                    && this.isAnti_ == other.isAnti_;
            }
            else {
                return false;
            }
        }
    }
}
