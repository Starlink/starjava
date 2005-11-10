package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Shape;
import java.util.BitSet;
import javax.swing.BorderFactory;
import javax.swing.JComponent;

/**
 * Experimental implementation of PlotSurface - debugging purposes only.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Jun 2004
 */
class DumbPlotSurface extends JComponent implements PlotSurface {

    private double xlo_ = -1.0;
    private double ylo_ = -1.0;
    private double xhi_ = +1.0;
    private double yhi_ = +1.0;

    public DumbPlotSurface() {
        setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
    }

    public void setState( PlotState state ) {
    }

    public void setDataRange( double xlo, double ylo, double xhi, double yhi ) {
        xlo_ = xlo;
        ylo_ = ylo;
        xhi_ = xhi;
        yhi_ = yhi;
    }

    public Point dataToGraphics( double dx, double dy, boolean insideOnly ) {
        if ( Double.isNaN( dx ) || Double.isInfinite( dx ) ||
             Double.isNaN( dy ) || Double.isInfinite( dy ) ) {
            return null;
        }
        if ( ! insideOnly || ( dx >= xlo_ && dx <= xhi_ &&
                               dy >= ylo_ && dy <= yhi_ ) ) {
            int px = (int) Math.round( getWidth() * 
                                       ( dx - xlo_ ) / ( xhi_ - xlo_ ) );
            int py = (int) Math.round( getHeight() *
                                       ( dy - ylo_ ) / ( yhi_ - ylo_ ) );
            return new Point( px, py );
        }
        else {
            return null;
        }
    }

    public double[] graphicsToData( int px, int py, boolean insideOnly ) {
        if ( insideOnly && ( px < 0 || px > getWidth() ||
                             py < 0 || py > getHeight() ) ) {
            return null;
        }
        else {
            double dx = xlo_ + px * ( xhi_ - xlo_ ) / getWidth();
            double dy = ylo_ + py * ( yhi_ - ylo_ ) / getHeight();
            return new double[] { dx, dy };
        }
    }

    public Shape getClip() {
        return null;
    }

    public JComponent getComponent() {
        return this;
    }

    public void paintSurface( Graphics g ) {
        paintComponent( g );
    }

}
