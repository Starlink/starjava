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
    private final int igraph_;
    private Rectangle bounds_;
    private double xlo_;
    private double xhi_;
    private double ylo_;
    private double yhi_;
    private LinesPlotState state_;
    private boolean labelX_;
    private boolean labelY_;

    /**
     * Constructor.
     *
     * @param   component  the component on which this surface will draw
     * @param   igraph  index of the graph in the plot state which this
     *          surface will be used for
     */
    public GraphSurface( JComponent component, int igraph ) {
        component_ = component;
        igraph_ = igraph;
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
        state_ = (LinesPlotState) state;
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
        int sy = component_.getGraphics().getFontMetrics().getHeight();
        if ( labelX_ ) {
            ValueInfo xInfo = state_.getAxes()[ 0 ];
            Graphics gx = g.create();
            gx.translate( bounds_.x, bounds_.y + bounds_.height );
            Plot3D.annotateAxis( gx, xInfo.getName(), bounds_.width, sy,
                                 xlo_, xhi_, state_.getLogFlags()[ 0 ],
                                 state_.getFlipFlags()[ 1 ] );
        }
        if ( labelY_ ) {
            ValueInfo yInfo = state_.getYAxes()[ igraph_ ];
            if ( yInfo != null ) {
                Graphics2D gy = (Graphics2D) g.create();
                gy.translate( bounds_.x, bounds_.y + bounds_.height );
                gy.rotate( - Math.PI * 0.5 );
                Plot3D.annotateAxis( gy, yInfo.getName(), bounds_.height, sy,
                                     ylo_, yhi_, false, false );
            }
        }
    }

    /**
     * Returns the space required around the edge of the plot region for
     * axis annotation and so on.
     *
     * @return   annotation insets
     */
    public Insets getAnnotationInsets() {
        FontMetrics fm = component_.getGraphics().getFontMetrics();
        int fh = fm.getHeight();
        return new Insets( fh / 2, fh * 2, fh * 2, 1 );
    }

    public String toString() {
        return bounds_ + " -> " +
               new Rectangle2D.Double( xlo_, ylo_, xhi_ - xlo_, yhi_ - ylo_ );
    }
}
