package uk.ac.starlink.ttools.plot;

import com.lowagie.text.pdf.PdfGraphics2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Superclass for all components which draw plots from table data.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2008
 */
public abstract class TablePlot extends JComponent {

    private final List plotListeners_;
    private PlotState state_;

    /**
     * Constructor.
     */
    protected TablePlot() {
        plotListeners_ = new ArrayList();
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( PlotState state ) {
        state_ = state;
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
     * Adds a listener which will be notified when this plot has been painted.
     *
     * @param   listener   listener to add
     */
    public void addPlotListener( PlotListener listener ) {
        plotListeners_.add( listener );
    }

    /**
     * Removes a listener previously added by <code>addPlotListener</code>.
     *
     * @param   listener  listener to remove
     */
    public void removePlotListener( PlotListener listener ) {
        plotListeners_.remove( listener );
    }

    /**
     * Sends a plot event to all registered listeners.
     *
     * <p>This method currently declared private because I think it that 
     * in current usage it should be called deferred using
     * {@link #firePlotChangedLater}.  Could be publicised if that turns
     * out not to be true in the future.
     *
     * @param   evt   event to dispatch
     */
    private void firePlotChanged( PlotEvent evt ) {
        for ( Iterator it = plotListeners_.iterator(); it.hasNext(); ) {
            ((PlotListener) it.next()).plotChanged( evt );
        }
    }

    /**
     * Sends a plot event to all registered listeners, deferring the send
     * by submitting it for future execution on the AWT event dispatch thread.
     * Although this will normally be called from the event dispatch thread,
     * it will normally be called from within a 
     * {@link javax.swing.JComponent#paintComponent} invocation.
     * I'm not certain it's a bad idea to call other swing-type methods
     * from within a paint, but it sounds like a good thing to avoid.
     *
     * @param  evt  event to dispatch
     */
    protected void firePlotChangedLater( final PlotEvent evt ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                firePlotChanged( evt );
            }
        } );
    }

    /**
     * Calculates data bounds for a given data set as appropriate for this
     * plot.
     *
     * @param   data  plot data
     * @param   state  plot state
     * @return  data bounds object
     */
    public DataBounds calculateBounds( PlotData data, PlotState state ) {
        boolean hasErrors = data.getNerror() > 0;
        int ndim = data.getNdim();
        int mainNdim = state.getMainNdim();
        int nset = data.getSetCount();

        /* Set up blank range objects. */
        Range[] ranges = new Range[ ndim ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            ranges[ idim ] = new Range();
        }
        int[] npoints = new int[ nset ];

        /* Submit each data point which will be plotted to the ranges. */
        PointSequence pseq = data.getPointSequence();
        int ip = 0;
        while ( pseq.next() ) {
            double[] coords = pseq.getPoint();
            boolean isValid = true;

            /* Check that the point has valid (non-blank) coordinates.
             * Note that blank auxiliary axis coordinates are OK. */
            for ( int idim = 0; idim < mainNdim && isValid; idim++ ) {
                isValid = isValid && ( ! Double.isNaN( coords[ idim ] ) &&
                                       ! Double.isInfinite( coords[ idim ] ) );
            }
            if ( isValid ) {
                boolean isUsed = false;
                for ( int iset = 0; iset < nset; iset++ ) {
                    if ( pseq.isIncluded( iset ) ) {
                        npoints[ iset ]++;
                        isUsed = true;
                    }
                }
                if ( isUsed ) {
                    for ( int idim = 0; idim < ndim; idim++ ) {
                        ranges[ idim ].submit( coords[ idim ] );
                    }
                    if ( hasErrors ) {
                        double[][] errs = pseq.getErrors();
                        for ( int ierr = 0; ierr < errs.length; ierr++ ) {
                            double[] err = errs[ ierr ];
                            if ( err != null ) {

                                /* Note when adjusting ranges for errors we
                                 * only examine the non-auxiliary dimensions
                                 * (mainNdim not ndim) since aux axes don't
                                 * have errors. */
                                for ( int idim = 0; idim < mainNdim; idim++ ) {
                                    ranges[ idim ].submit( err[ idim ] );
                                }
                            }
                        }
                    }
                    ip++;
                }
            }
        }
        pseq.close();

        /* Return a DataBounds object containing the results. */
        return new DataBounds( ranges, ip, npoints );
    }

    /**
     * Returns the bounds of the actual plot.  This may be used for visual
     * alignment of some items in the window.  It is permissible to return
     * just the bounds of the plot component itself, but alignment of other
     * components (legends) may look better if the bounds of the actual
     * plotting region (for instance, excluding external axis labels) is
     * returned.
     * 
     * @return  plot region bounds
     */
    public abstract Rectangle getPlotBounds();
   

    /**
     * Determines whether the given graphics context represents a
     * vector graphics type environment (such as PostScript).
     * 
     * @param  g  graphics context to test
     * @return  true iff <code>g</code> is PostScript-like
     */
    public static boolean isVectorContext( Graphics g ) {
        return ( g instanceof EpsGraphics2D )
            || ( g instanceof PdfGraphics2D )
            || ( g instanceof Graphics2D
                 && ((Graphics2D) g).getDeviceConfiguration().getDevice()
                                    .getType() == GraphicsDevice.TYPE_PRINTER );
    }
}
