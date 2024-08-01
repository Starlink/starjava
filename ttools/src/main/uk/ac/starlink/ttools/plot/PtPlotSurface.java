package uk.ac.starlink.ttools.plot;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;

/**
 * PlotSurface implementation which uses Ptplot classes for axis plotting
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class PtPlotSurface extends PlotBox implements PlotSurface {

    private PlotState state_;
    private static final int padPixels_ = 0;

    /**
     * Constructs a new surface.
     */
    @SuppressWarnings("this-escape")
    public PtPlotSurface() {
        setColor( false );
        _setPadding( 0.0 );
        _expThreshold = 3;
        _topPadding = 0;

        /* Set _uly here - the superclass sets it during plotting, which 
         * messes up alignments done prior to the plot. */
        _uly = _topPadding + 5;
    }

    public void setFont( Font font ) {
        super.setFont( font );
        _labelFont = font;
        _labelFontMetrics = getFontMetrics( _labelFont );
        double superScale = 0.75;
        _superscriptFont =
             font.deriveFont( AffineTransform
                             .getScaleInstance( superScale, superScale ) );
        _superscriptFontMetrics = getFontMetrics( _superscriptFont );
    }

    public void setState( PlotState state ) {
        state_ = state;
        if ( state_ != null && state.getValid() ) {
            configure( state_ );
        }
        else {
            setXLabel( "" );
            setYLabel( "" );
            setXLog( false );
            setYLog( false );
            setXFlip( false );
            setYFlip( false );
            setGrid( false );
            checkInvariants();
        }
    }

    /*
     * The coordinates in a PlotBox confused me for some time.
     * What you have to remember is that PlotBox stores its state
     * (protected instance variables such as _xscale, _yscale, 
     * _xMax, _xMin, _yMax, _yMin etc) referring to the linear
     * coordinates which map onto the plotting surface.
     * So if you're using these variables, convert to the linear
     * space first (i.e. take logarithms if necessary).
     * This strikes me as a bit of a strange way to work, since it's
     * neither data coordinates (not logarithmic) but it's not graphics
     * coordinates either (double precision, has offset and scaling),
     * it's somewhere in between.
     */

    public void setDataRange( double xlo, double ylo, double xhi, double yhi ) {
        if ( ! Double.isNaN( xlo ) && ! Double.isNaN( xhi ) ) {
            if ( _xlog ) {
                xlo = xlo > 0.0 ? Math.log( xlo ) * _LOG10SCALE : 0.0;
                xhi = xhi > 0.0 ? Math.log( xhi ) * _LOG10SCALE : 1.0;
            }
            if ( _xflip ) {
                double xl = -xhi;
                double xh = -xlo;
                xlo = xl;
                xhi = xh;
            }
            int width = _lrx - _ulx;
            double xpad = ( xhi - xlo ) * padPixels_ / width;
            setXRange( xlo - xpad, xhi + xpad );
        }
        if ( ! Double.isNaN( ylo ) && ! Double.isNaN( yhi ) ) {
            if ( _ylog ) {
                ylo = ylo > 0.0 ? Math.log( ylo ) * _LOG10SCALE : 0.0;
                yhi = yhi > 0.0 ? Math.log( yhi ) * _LOG10SCALE : 1.0;
            }
            if ( _yflip ) {
                double yl = -yhi;
                double yh = -ylo;
                ylo = yl;
                yhi = yh;
            }
            int height = _lry - _uly;
            double ypad = ( yhi - ylo ) * padPixels_ / height;
            setYRange( ylo - ypad, yhi + ypad );
        }
        checkInvariants();
    }

//  public double[] getDataRange() {
//      return new double[] { _xMin, _yMin, _xMax, _yMax };
//  }

    public Point dataToGraphics( double dx, double dy, boolean insideOnly ) {
        if ( Double.isNaN( dx ) || Double.isNaN( dy ) ) {
            return null;
        }
        if ( _xlog ) {
            if ( dx > 0.0 ) {
                dx = Math.log( dx ) * _LOG10SCALE;
            }
            else {
                if ( insideOnly ) {
                    return null;
                }
                else {
                    dx = Double.NEGATIVE_INFINITY;
                }
            }
        }
        if ( _ylog ) {
            if ( dy > 0.0 ) {
                dy = Math.log( dy ) * _LOG10SCALE;
            }
            else {
                if ( insideOnly ) {
                    return null;
                }
                else {
                    dy = Double.NEGATIVE_INFINITY;
                }
            }
        }
        if ( _xflip ) {
            dx = -dx;
        }
        if ( _yflip ) {
            dy = -dy;
        }
        if ( ! insideOnly || ( dx >= _xMin && dx <= _xMax &&
                               dy >= _yMin && dy <= _yMax ) ) {
            double px = _ulx + ( ( dx - _xMin ) * _xscale );
            double py = _lry - ( ( dy - _yMin ) * _yscale );
            int ipx = px > MAX_COORD ? MAX_COORD
                                     : ( px < - MAX_COORD ? - MAX_COORD
                                                          : (int) px );
            int ipy = py > MAX_COORD ? MAX_COORD
                                     : ( py < - MAX_COORD ? - MAX_COORD
                                                          : (int) py );
            return new Point( ipx, ipy );
        }
        else {
            return null;
        }
    }

    public double[] graphicsToData( int px, int py, boolean insideOnly ) {
        if ( insideOnly &&
             ( px < _ulx || px > _lrx || py < _uly || py > _lry ) ) {
            return null;
        }
        double dx = _xMin + ( ( px - _ulx ) / _xscale );
        double dy = _yMin - ( ( py - _lry ) / _yscale );
        if ( _xflip ) {
            dx = -dx;
        }
        if ( _yflip ) {
            dy = -dy;
        }
        if ( _xlog ) {
            dx = Math.pow( 10., dx );
        }
        if ( _ylog ) {
            dy = Math.pow( 10., dy );
        }
        return new double[] { dx, dy };
    }

    public Shape getClip() {
        int width = _lrx - _ulx - 1;
        int height = _lry - _uly - 1;
        return new Rectangle( _ulx + 1, _uly + 1, width, height );
    }

    public JComponent getComponent() {
        return this;
    }

    public void paintSurface( Graphics g ) {
        paintComponent( g );
    }

    protected void _zoom( int x, int y ) {
        double oldXMin = _xMin;
        double oldXMax = _xMax;
        double oldYMin = _yMin;
        double oldYMax = _yMax;
        super._zoom( x, y );
        if ( _xMin != oldXMin || _xMax != oldXMax ||
             _yMin != oldYMin || _yMax != oldYMax ) {
            checkInvariants();
        }
    }

    /**
     * Hack around the fact that PlotBox does a lot of its updating of
     * protected variables (which we use for coordinate conversion) in
     * its paintComponent method.  This is bad, because it means the
     * results we get are dependent on whether the redraw has actually
     * happened yet, which it might or might not have.
     * So here we effectively do a dry call of paintComponent, which
     * does the calculations and updates the state, without actually
     * writing any graphics.
     */
    private void checkInvariants() {
        _drawPlot( (Graphics) null, true );
    }

    /**
     * Configures this plot box from a PlotState object.
     *
     * @param   state  state for configuration
     */
    private void configure( PlotState state ) {

        /* Axes. */
        setXLabel( state.getAxisLabels()[ 0 ] );
        setYLabel( state.getAxisLabels()[ 1 ] );

        /* Logarithmic plot flags. */
        setXLog( state.getLogFlags()[ 0 ] );
        setYLog( state.getLogFlags()[ 1 ] );

        /* Axis flip flags. */
        setXFlip( state.getFlipFlags()[ 0 ] );
        setYFlip( state.getFlipFlags()[ 1 ] );

        /* Grid flag. */
        setGrid( state.getGrid() );

        checkInvariants();
    }
}
