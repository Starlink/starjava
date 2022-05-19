package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Positioning constraint for plotted text.
 * An anchor takes care of text alignment and positioning given a
 * reference point.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public abstract class Anchor {

    /** North - reference point is above centre of horizontal text. */
    public static Anchor N = new HorizontalAnchor() {
        protected int[] getOffset( Rectangle box, int pad ) {
            return new int[] { - box.width / 2, box.height + pad };
        }
        public String toString() {
            return "North";
        }
    };

    /** East - reference point is to right of horizontal text. */
    public static Anchor E = new HorizontalAnchor() {
        protected int[] getOffset( Rectangle box, int pad ) {
            return new int[] { - box.width - pad, box.height / 2 };
        }
        public String toString() {
            return "East";
        }
    };

    /** West - reference point is to left of horizontal text. */
    public static Anchor W = new HorizontalAnchor() {
        protected int[] getOffset( Rectangle box, int pad ) {
            return new int[] { pad, box.height / 2 };
        }
        public String toString() {
            return "West";
        }
    };

    /** South - reference point is below center of horizontal text. */
    public static Anchor S = new HorizontalAnchor() {
        protected int[] getOffset( Rectangle box, int pad ) {
            return new int[] { - box.width / 2, -pad };
        }
        public String toString() {
            return "South";
        }
    };

    /** Center - reference point is the center of the text. */
    public static Anchor C = new HorizontalAnchor() {
        protected int[] getOffset( Rectangle box, int pad ) {
            return new int[] { - box.width / 2, box.height / 2 };
        }
        public String toString() {
            return "Center";
        }
    };

    /**
     * Returns a rectangle within which all of the given label will fall.
     *
     * @param   label   text
     * @param   px    reference point X position
     * @param   py    reference point Y position
     * @param   captioner  object that can turn text into graphics
     * @return   plotted text bounding box
     */
    public abstract Rectangle getCaptionBounds( Caption label, int px, int py,
                                                Captioner captioner );

    /**
     * Draws a text label onto a given graphics context.
     *
     * @param   label   text
     * @param   px    reference point X position
     * @param   py    reference point Y position
     * @param   captioner  object that can turn text into graphics
     * @param   g  graphics context
     */
    public abstract void drawCaption( Caption label, int px, int py,
                                      Captioner captioner, Graphics g );

    /**
     * Returns a new anchor for drawing text at a non-horizontal angle.
     *
     * @param   theta  rotation angle anticlockwise from X axis in radians
     * @param   baseAnchor  anchor supplying positioning constraints for
     *                      horizontal text
     * @return  new anchor
     */
    public static Anchor createAngledAnchor( double theta, Anchor baseAnchor ) {
        return new AngledAnchor( theta, baseAnchor );
    }

    /**
     * Anchor for drawing horizontal text.
     */
    public static abstract class HorizontalAnchor extends Anchor {

        public Rectangle getCaptionBounds( Caption label, int px, int py,
                                           Captioner captioner ) {
            Rectangle cbox = captioner.getCaptionBounds( label );
            int[] offset = getOffset( cbox, captioner.getPad() );
            cbox.translate( px + offset[ 0 ], py + offset[ 1 ] );
            return cbox;
        }

        public void drawCaption( Caption label, int px, int py,
                                 Captioner captioner, Graphics g ) {
            Rectangle cbox = captioner.getCaptionBounds( label );
            int[] offset = getOffset( cbox, captioner.getPad() );
            int xoff = px + offset[ 0 ];
            int yoff = py + offset[ 1 ];
            g.translate( xoff, yoff );
            captioner.drawCaption( label, g );
            g.translate( -xoff, -yoff );
        }

        /**
         * Returns the positional offset from the reference point to
         * position the graphics context at before calling drawCaption.
         *
         * @param   bounds  caption bounds
         * @param   pad   captioner-specific pad value
         */
        protected abstract int[] getOffset( Rectangle bounds, int pad );
    }

    /**
     * Anchor for drawing non-horizontal text.
     */
    private static class AngledAnchor extends Anchor {

        private final double theta_;
        private final Anchor baseAnchor_;

        /**
         * Constructor.
         *
         * @param   theta  rotation angle anticlockwise from X axis in radians
         * @param   baseAnchor  anchor supplying positioning constraints for
         *                      horizontal text
         */
        public AngledAnchor( double theta, Anchor baseAnchor ) {
            theta_ = theta;
            baseAnchor_ = baseAnchor;
        }

        public Rectangle getCaptionBounds( Caption label, int px, int py,
                                           Captioner captioner ) {
            Rectangle baseBounds =
                baseAnchor_.getCaptionBounds( label, px, py, captioner );
	    Rectangle2D.Double b0 = new Rectangle2D.Double();
            b0.setRect( baseBounds.getBounds2D() );
            b0.x -= px;
            b0.y -= py;
            Rectangle2D.Double b1 = new Rectangle2D.Double();
            b1.add( rotatePoint( b0.x, b0.y ) );
            b1.add( rotatePoint( b0.x + b0.width, b0.y ) );
            b1.add( rotatePoint( b0.x + b0.width, b0.y + b0.height ) );
            b1.add( rotatePoint( b0.x, b0.y + b0.height ) );
            b1.x += px;
            b1.y += py;
            return b1.getBounds();
        }

        public void drawCaption( Caption label, int px, int py,
                                 Captioner captioner, Graphics g ) {
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform trans0 = g2.getTransform();
            g2.translate( px, py );
            g2.rotate( theta_ );
            baseAnchor_.drawCaption( label, 0, 0, captioner, g );
            g2.setTransform( trans0 );
        }

        /**
         * Rotates a point around the origin by this anchor's rotation angle.
         *
         * @param  px  input x coordinate
         * @param  py  input y coordinate
         * @retrun   rotated position
         */
        private Point2D rotatePoint( double px, double py ) {
            double cos = Math.cos( theta_ );
            double sin = Math.sin( theta_ );
            return new Point2D.Double( cos * px - sin * py,
                                       sin * px + cos * py );
        }
    }
}
