package uk.ac.starlink.tplot;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
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
public class TablePlot extends JComponent {

    private final List plotListeners_;

    /**
     * Constructor.
     */
    public TablePlot() {
        plotListeners_ = new ArrayList();
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
     * Determines whether the given graphics context represents a
     * vector graphics type environment (such as PostScript).
     * 
     * @param  g  graphics context to test
     * @return  true iff <code>g</code> is PostScript-like
     */
    public static boolean isVectorContext( Graphics g ) {
        return ( g instanceof EpsGraphics2D )
            || ( g instanceof Graphics2D
                 && ((Graphics2D) g).getDeviceConfiguration().getDevice()
                                    .getType() == GraphicsDevice.TYPE_PRINTER );
    }
}
