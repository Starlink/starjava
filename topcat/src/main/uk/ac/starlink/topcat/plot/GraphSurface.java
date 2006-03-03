package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;

/**
 * Plotting surface for drawing graphs on.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class GraphSurface implements PlotSurface {

    private final JComponent component_;
    private final Rectangle bounds_;
    private double xlo_;
    private double xhi_;
    private double ylo_;
    private double yhi_;
    private boolean labelX_ = true;
    private boolean labelY_ = true;

    /**
     * Constructor.
     *
     * @param   component  the component on which this surface will draw
     * @param   bounds   the region of the component which represents the
     *          target for data points; annotations may be drawn outside 
     *          this region
     */
    public GraphSurface( JComponent component, Rectangle bounds ) {
        component_ = component;
        bounds_ = new Rectangle( bounds );
    }

    public Shape getClip() {
        return bounds_;
    }

    public JComponent getComponent() {
        return component_;
    }

    public void setDataRange( double xlo, double ylo, double xhi, double yhi ) {
        xlo_ = xlo;
        ylo_ = ylo;
        xhi_ = xhi;
        yhi_ = yhi;
    }

    public Point dataToGraphics( double x, double y, boolean insideOnly ) {
        if ( insideOnly && ( x < xlo_ || x > xhi_ || y < ylo_ || y > yhi_ ) ) {
            return null;
        }
        else if ( Double.isNaN( x ) || Double.isNaN( y ) ) {
            return null;
        }
        else {
            double rx = ( x - xlo_ ) / ( xhi_ - xlo_ );
            double ry = ( y - ylo_ ) / ( yhi_ - ylo_ );
            return new Point(
                bounds_.x + (int) ( rx * bounds_.width ),
                bounds_.y + (int) ( ( 1.0 - ry ) * bounds_.height ) );
        }
    }

    public double[] graphicsToData( int px, int py, boolean insideOnly ) {
        if ( insideOnly &&
             ( px < bounds_.x || px > bounds_.x + bounds_.width ||
               py < bounds_.y || py > bounds_.y + bounds_.height ) ) {
            return null;
        }
        else {
            double rx = px - bounds_.x / bounds_.width;
            double ry = py - bounds_.y / bounds_.height;
            return new double[] {
                xlo_ + rx * ( xhi_ - xlo_ ),
                ylo_ + ( 1.0 - ry ) * ( yhi_ - ylo_ ), 
            };
        }
    }

    public void setState( PlotState state ) {
    }

    /**
     * Determines whether labels are drawn for each axis.
     *
     * @param   labelX  true iff you want the horizontal axis labelled
     * @param   labelY  true iff you want the vertical axis labelled
     */
    public void setLabelAxes( boolean labelX, boolean labelY ) {
        labelX_ = labelX;
        labelY_ = labelY;
    }

    public void paintSurface( Graphics g ) {
        g = g.create();
        g.setColor( Color.WHITE );
        g.fillRect( bounds_.x, bounds_.y, bounds_.width, bounds_.height );
        g.setColor( Color.BLACK );
        g.drawRect( bounds_.x, bounds_.y, bounds_.width, bounds_.height );
    }

    /**
     * Returns the additional space required outside the clip of a 
     * GraphSurface which is be required for label annotation on
     * axes for which labels will be drawn.
     *
     * @param   comp  component on which the drawing is done
     */
    public static int getLabelGap( JComponent comp ) {
        return 1;
    }

    public String toString() {
        return bounds_ + " -> " +
               new Rectangle2D.Double( xlo_, ylo_, xhi_ - xlo_, yhi_ - ylo_ );
    }
}
