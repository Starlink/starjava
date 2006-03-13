package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;

/**
 * Plotting surface for drawing graphs on.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class GraphSurface implements PlotSurface {

    private final JComponent component_;
    private Rectangle bounds_;
    private double xlo_;
    private double xhi_;
    private double ylo_;
    private double yhi_;
    private boolean labelX_;
    private boolean labelY_;

    /**
     * Constructor.
     *
     * @param   component  the component on which this surface will draw
     */
    public GraphSurface( JComponent component ) {
        component_ = component;
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

    /**
     * Sets the rectangle within which data points may be plotted.
     * Additional annotations (such as axis labels) may be drawn outside
     * this region.
     *
     * @param   bounds   the region of the component which represents the
     *          target for data points; annotations may be drawn outside 
     *          this region
     */
    public void setBounds( Rectangle bounds ) {
        bounds_ = new Rectangle( bounds );
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

    public void paintSurface( Graphics g ) {
    }

    public String toString() {
        return bounds_ + " -> " +
               new Rectangle2D.Double( xlo_, ylo_, xhi_ - xlo_, yhi_ - ylo_ );
    }
}
