package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;

/**
 * Plotting style for continuous lines.
 *
 * @author   Mark Taylor
 * @since    13 Jun 2013
 */
public class LineStyle implements Style {

    private final Color color_;
    private final Stroke stroke_;
    private final boolean antialias_;
    private final Icon legendIcon_;

    /**
     * Constructor.
     *
     * @param  color  line colour
     * @param  stroke  line stroke
     * @param  antialias  whether line is to be antialiased
     *                    (only likely to make a difference on bitmapped paper)
     */
    public LineStyle( Color color, Stroke stroke, boolean antialias ) {
        color_ = color;
        stroke_ = stroke;
        antialias_ = antialias;
        legendIcon_ = new Icon() {
            final int width = MarkerStyle.LEGEND_ICON_WIDTH;
            final int height = MarkerStyle.LEGEND_ICON_HEIGHT;
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g;
                Color color0 = g2.getColor();
                Stroke stroke0 = g2.getStroke();
                g2.setColor( color_ );
                g2.setStroke( stroke_ );
                int y1 = y + height / 2;
                g2.drawLine( x, y1, x + width, y1 );
                g2.setColor( color0 );
                g2.setStroke( stroke0 );
            }
        };
    }

    /**
     * Returns the line colour.
     *
     * @return   colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Returns the object used to stroke the line.
     *
     * @return  stroke
     */
    public Stroke getStroke() {
        return stroke_;
    }

    /**
     * Indicates whether the line will be antialiased in suitable
     * (bitmapped) contexts.
     *
     * @return  true for antialiasing
     */
    public boolean getAntialias() {
        return antialias_;
    }

    public Icon getLegendIcon() {
        return legendIcon_;
    }

    /**
     * Convenience method to return a line tracer that will use this style.
     *
     * @param   g  graphics context
     * @param   bounds   clip bounds
     * @param   nwork   workspace array size
     * @param   isPixel  if true graphics context is considered pixellised
     * @return   new line tracer
     */
    public LineTracer createLineTracer( Graphics g, Rectangle bounds,
                                        int nwork, boolean isPixel ) {
        return new LineTracer( g, bounds, stroke_, antialias_, nwork, isPixel );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LineStyle ) {
            LineStyle other = (LineStyle) o;
            return this.color_.equals( other.color_ )
                && this.stroke_.equals( other.stroke_ )
                && this.antialias_ == other.antialias_;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 90125;
        code = 23 * code + color_.hashCode();
        code = 23 * code + stroke_.hashCode();
        code = 23 * code + ( antialias_ ? 11 : 13 );
        return code;
    }
}
