package uk.ac.starlink.topcat.plot2;

import java.awt.Point;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot2.NavAction;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Abstract NavigationListener subclass that works with a PlotPanel.
 * The main thing it does is arrange for progress logging where required.
 * The only time this is required is when a data pos iterator
 * is used in handling (re-center) click events.  That is generally only
 * for 3d plots.  But it's harmless to use this wrapper in any case.
 *
 * @author   Mark Taylor
 * @since    19 Nov 2013
 */
public abstract class GuiNavigationListener<A> extends NavigationListener<A> {

    private final PlotPanel<?,A> plotPanel_;

    /**
     * Constructor.
     *
     * @param  plotPanel    plot panel
     */
    public GuiNavigationListener( PlotPanel<?,A> plotPanel ) {
        plotPanel_ = plotPanel;
    }

    public Surface getSurface() {
        return plotPanel_.getLatestSurface();
    }

    @Override
    protected void handleClick( final Navigator<A> navigator,
                                final Surface surface,
                                final Point pos, final int ibutt,
                                final Iterable<double[]> dposIt ) {

        /* The click operation *may* take time, if it is necessary to
         * iterate over the data positions.  To cover that possibility,
         * calculate the new aspect asynchronously and update the GUI
         * later on the EDT.  Also make sure that progress is logged. */
        plotPanel_.submitPlotAnnotator( new Runnable() {
            public void run() {
                NavAction<A> navact =
                    navigator.click( surface, pos, ibutt, dposIt );
                updateDecoration( navact.getDecoration(), true );
                final A aspect = navact == null ? null
                                                : navact.getAspect();
                if ( aspect != null &&
                     ! Thread.currentThread().isInterrupted() ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            setAspect( aspect );
                        }
                    } );
                }
            }
        } );
    }

    public Iterable<double[]> createDataPosIterable() {

        /* Handles progress reporting and thread interruption. */
        GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud();
        return pointCloud
              .createDataPosIterable( pointCloud.createGuiDataStore() );
    }
}
