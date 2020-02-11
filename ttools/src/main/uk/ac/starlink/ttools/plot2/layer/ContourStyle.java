package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Style for contour plots.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2013
 */
@Equality
public class ContourStyle implements Style {

    private final Color color_;
    private final int nLevel_;
    private final double offset_;
    private final int nSmooth_;
    private final LevelMode levelMode_;
    private final Combiner combiner_;
    private final int thickness_;

    /**
     * Constructor.
     *
     * @param  color  contour line colour
     * @param  nLevel  number of contours
     * @param  offset  offset from zero of first contour
     * @param  nSmooth  smoothing kernel width
     * @param  thickness  line thickness
     * @param  levelMode  level determination algorithm
     * @param  combiner   combination mode
     */
    public ContourStyle( Color color, int nLevel, double offset, int nSmooth,
                         int thickness, LevelMode levelMode,
                         Combiner combiner ) {
        color_ = color;
        nLevel_ = nLevel;
        offset_ = offset;
        nSmooth_ = nSmooth;
        thickness_ = thickness;
        levelMode_ = levelMode;
        combiner_ = combiner;
    }

    /**
     * Returns contour colour.
     *
     * @return  colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Returns requested number of contours.
     *
     * @return   level count
     */
    public int getLevelCount() {
        return nLevel_;
    }

    /**
     * Returns the offset of the first contour from zero.
     *
     * @return  zero offset
     */
    public double getOffset() {
        return offset_;
    }

    /**
     * Returns smoothing kernel width.
     *
     * @return  smoothing amount; 1 means no smooth
     */
    public int getSmoothing() {
        return nSmooth_;
    }

    /**
     * Returns line thickness.
     *
     * @return   thickness of plotted contour lines in pixels
     */
    public int getThickness() {
        return thickness_;
    }

    /**
     * Returns level determination algorithm.
     *
     * @return   level mode
     */
    public LevelMode getLevelMode() {
        return levelMode_;
    }

    /**
     * Returns the combination mode.
     *
     * @return  combiner
     */
    public Combiner getCombiner() {
        return combiner_;
    }

    public Icon getLegendIcon() {
        return new Icon() {
            private static final int width_ = 14;
            private static final int height_ = 12;
            public int getIconWidth() {
                return width_;
            }
            public int getIconHeight() {
                return height_;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor( color_ );
                g2.setStroke( new BasicStroke( thickness_ ) );
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                g2.drawOval( x, y, width_, height_ );
                if ( thickness_ < 3 ) {
                    g2.drawOval( x + 2, y + 3, width_ - 6, height_ - 6 );
                }
            }
        };
    }

    @Override
    public int hashCode() {
        int code = 23037;
        code = code * 23 + color_.hashCode();
        code = code * 23 + nLevel_;
        code = code * 23 + Float.floatToIntBits( (float) offset_ );
        code = code * 23 + nSmooth_;
        code = code * 23 + thickness_;
        code = code * 23 + levelMode_.hashCode();
        code = code * 23 + combiner_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ContourStyle ) {
            ContourStyle other = (ContourStyle) o;
            return this.color_.equals( other.color_ )
                && this.nLevel_ == other.nLevel_
                && this.offset_ == other.offset_
                && this.nSmooth_ == other.nSmooth_
                && this.thickness_ == other.thickness_
                && this.levelMode_.equals( other.levelMode_ )
                && this.combiner_.equals( other.combiner_ );
        }
        else {
            return false;
        }
    }
}
