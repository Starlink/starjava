package uk.ac.starlink.ttools.plot;

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
    private boolean xLog_;
    private boolean yLog_;
    private boolean xFlip_;
    private boolean yFlip_;

    /**
     * Constructor.
     *
     * @param   component  the component on which this surface will draw
     * @param   xLog  true iff X axis is logarithmically scaled
     * @param   yLog  true iff Y axis is logarithmically scaled
     * @param   xFlip true iff X axis is inverted
     * @param   yFlip true iff Y axis is inverted
     */
    public GraphSurface( JComponent component, boolean xLog, boolean yLog,
                         boolean xFlip, boolean yFlip ) {
        component_ = component;
        xLog_ = xLog;
        yLog_ = yLog;
        xFlip_ = xFlip;
        yFlip_ = yFlip;
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
            double rx;
            if ( xLog_ ) {
                rx = x > 0.0 ? Math.log( x / xlo_ ) / Math.log( xhi_ / xlo_ )
                             : Double.NEGATIVE_INFINITY;
            }
            else {
                rx = ( x - xlo_ ) / ( xhi_ - xlo_ );
            }
            double ry;
            if ( yLog_ ) {
                ry = y > 0.0 ? Math.log( y / ylo_ ) / Math.log( yhi_ / ylo_ )
                             : Double.NEGATIVE_INFINITY;
            }
            else {
                ry = ( y - ylo_ ) / ( yhi_ - ylo_ );
            }
            if ( xFlip_ ) {
                rx = 1.0 - rx;
            }
            if ( ! yFlip_ ) {
                ry = 1.0 - ry;
            }
            double px = bounds_.x + rx * bounds_.width;
            double py = bounds_.y + ry * bounds_.height;
            int ipx = px > MAX_COORD ? MAX_COORD
                                     : ( px < - MAX_COORD ? - MAX_COORD
                                                          : (int) px );
            int ipy = py > MAX_COORD ? MAX_COORD
                                     : ( py < - MAX_COORD ? - MAX_COORD
                                                          : (int) py );
            return new Point( ipx, ipy );
        }
    }

    public double[] graphicsToData( int px, int py, boolean insideOnly ) {
        if ( insideOnly &&
             ( px < bounds_.x || px > bounds_.x + bounds_.width ||
               py < bounds_.y || py > bounds_.y + bounds_.height ) ) {
            return null;
        }
        else {
            double rx = ( px - bounds_.x ) / (double) bounds_.width;
            double ry = ( py - bounds_.y ) / (double) bounds_.height;
            if ( xFlip_ ) {
                rx = 1.0 - rx;
            }
            if ( ! yFlip_ ) {
                ry = 1.0 - ry;
            }
            return new double[] {
                xLog_ ? xlo_ * Math.exp( rx * Math.log( xhi_ / xlo_ ) )
                      : xlo_ + rx * ( xhi_ - xlo_ ),
                yLog_ ? ylo_ * Math.exp( ry * Math.log( yhi_ / ylo_ ) )
                      : ylo_ + ry * ( yhi_ - ylo_ ), 
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
