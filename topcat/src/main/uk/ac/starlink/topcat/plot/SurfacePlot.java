package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JComponent;
import javax.swing.OverlayLayout;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Abstract superclass for plot components which display on 
 * {@link PlotSurface}.  Note that the PlotSurface is not specified in
 * the constructor but must be set before any plotting is done.
 * This class mainly handles keeping track of the current state
 * (<code>PlotState</code>, <code>Points</code> and <code>PlotSurface</code>
 * members).  The actual work is done by the <code>paintComponent</code>
 * method of concrete subclasses.
 *
 * <p>The details of the plot are determined by a {@link PlotState} object
 * which indicates what the plot will look like and a {@link Points}
 * object which provides the data to plot.  Setting these values does
 * not itself trigger a change in the component, they only take effect
 * when {@link #paintComponent} is called (e.g. following a {@link #repaint}
 * call).  The X and Y ranges of the displayed plot are not specified
 * programatically; they may be changed by user interaction.
 * The drawing of axes and other decorations is done by a decoupled
 * {@link PlotSurface} object (bridge pattern).
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public abstract class SurfacePlot extends JComponent implements Printable {

    private Points points_;
    private PlotState state_;
    private PlotSurface surface_;

    /**
     * Constructor.
     */
    protected SurfacePlot() {
        setLayout( new OverlayLayout( this ) );
        setOpaque( false );
    }

    /**
     * Sets the plotting surface which draws axes and other decorations
     * that form the background to the actual plotted points.
     * This must be called before the component is drawn.
     *
     * @param  surface  plotting surface implementation
     */
    public void setSurface( PlotSurface surface ) {
        if ( surface_ != null ) {
            remove( surface_.getComponent() );
        }
        surface_ = surface;
        surface_.setState( state_ );
        surface_.getComponent().setOpaque( false );
        add( surface_.getComponent() );
    }

    /**
     * Returns the plotting surface on which this component displays.
     *
     * @return   plotting surface
     */
    public PlotSurface getSurface() {
        return surface_;
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     *
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
    }

    /**
     * Returns the data set for this point.
     *
     * @return  data points
     */
    public Points getPoints() {
        return points_;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( PlotState state ) {
        state_ = state;
        if ( surface_ != null ) {
            surface_.setState( state_ );
        }
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public PlotState getState() {
        return state_;
    }

    /**
     * Returns the current point selection.
     * This convenience method just retrieves it from the current plot state.
     *
     * @return   point selection
     */
    public PointSelection getPointSelection() {
        return state_.getPointSelection();
    }

    /**
     * Updates the X and Y ranges of the plotting surface so that all the
     * data points which are currently selected for plotting will fit in
     * nicely.
     */
    public void rescale() {
        double[] range = getState().getValid() ? getFullDataRange()
                                               : null;
        double xlo, ylo, xhi, yhi;
        if ( range != null ) {
            xlo = range[ 0 ];
            ylo = range[ 1 ];
            xhi = range[ 2 ];
            yhi = range[ 3 ];
        }
        else {
            boolean xlog = false;
            boolean ylog = false;
            if ( state_ != null && state_.getLogFlags() != null ) {
                xlog = state_.getLogFlags()[ 0 ];
                ylog = state_.getNdim() > 1 && state_.getLogFlags()[ 1 ];
            }
            xlo = xlog ? 0.1 : 0.;
            xhi = 1.;
            ylo = ylog ? 0.1 : 0.;
            yhi = 1.;
        }
        surface_.setDataRange( xlo, ylo, xhi, yhi );
    }

    /**
     * Works out the range of coordinates to accommodate all the data
     * points owned by this plot.  If no range can be determined, 
     * null may be returned.
     *
     * @return   4-element array (xlo,ylo,xhi,yhi)
     */
    public abstract double[] getFullDataRange();

    /**
     * Implements the {@link java.awt.print.Printable} interface.
     * At time of writing, this method is not used by TOPCAT, though it
     * could be; in particular it is not used to implement the
     * export to EPS functionality.
     * The code is mostly pinched from SPLAT.
     */
    public int print( Graphics g, PageFormat pf, int pageIndex ) {
        if ( pageIndex == 0 ) {

            /* Get a graphics object scaled for this component to print on. */
            Graphics2D g2 = (Graphics2D) g.create();
            int gap = 70;  // points
            double pageWidth = pf.getImageableWidth() - 2.0 * gap;
            double pageHeight = pf.getImageableHeight() - 2.0 * gap;
            double xinset = pf.getImageableX() + gap;
            double yinset = pf.getImageableY() + gap;
            double compWidth = (double) getWidth();
            double compHeight = (double) getHeight();
            double xscale = pageWidth / compWidth;
            double yscale = pageHeight / compHeight;
            if ( xscale < yscale ) {
                yscale = xscale;
            }
            else {
                xscale = yscale;
            }
            g2.translate( xinset, yinset );
            g2.scale( xscale, yscale );

            /* Draw the plot. */
            print( g2 );
            return PAGE_EXISTS;
        }
        else {
            return NO_SUCH_PAGE;
        }
    }

    /**
     * Determines whether a point with a given index is included in the
     * current plot.  This doesn't necessarily mean it's visible, since
     * it might fall outside the bounds of the current display area,
     * but it means the point does conceptually form part of what is
     * being plotted.
     *
     * @param  ip  index of point to check
     * @return  true  iff point <tt>ip</tt> is included in this plot
     */
    public boolean isIncluded( int ip ) {
        RowSubset[] sets = getPointSelection().getSubsets();
        int nset = sets.length;
        for ( int is = 0; is < nset; is++ ) {
            if ( sets[ is ].isIncluded( (long) ip ) ) {
                return true;
            }
        }
        return false;
    }
}
