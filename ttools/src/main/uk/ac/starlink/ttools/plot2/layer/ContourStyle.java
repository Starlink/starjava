package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
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

    /**
     * Constructor.
     *
     * @param  color  contour line colour
     * @param  nLevel  number of contours
     * @param  offset  offset from zero of first contour
     * @param  nSmooth  smoothing kernel width
     * @param  levelMode  level determination algorithm
     * @param  combiner   combination mode
     */
    public ContourStyle( Color color, int nLevel, double offset, int nSmooth,
                         LevelMode levelMode, Combiner combiner ) {
        color_ = color;
        nLevel_ = nLevel;
        offset_ = offset;
        nSmooth_ = nSmooth;
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
            private static final int width_ = 12;
            private static final int height_ = 12;
            public int getIconWidth() {
                return 12;
            }
            public int getIconHeight() {
                return 12;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color color = g.getColor();
                g.setColor( color_ );
                g.drawOval( x, y, width_, height_ );
                g.drawOval( x + 2, y + 3, width_ - 4, height_ - 6 );
                g.setColor( color );
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
                && this.levelMode_.equals( other.levelMode_ )
                && this.combiner_.equals( other.combiner_ );
        }
        else {
            return false;
        }
    }
}
