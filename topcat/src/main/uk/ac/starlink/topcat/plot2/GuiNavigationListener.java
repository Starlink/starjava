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

    public int getSurfaceIndex( Point pos ) {
        return plotPanel_.getGang().getNavigationZoneIndex( pos );
    }

    public Surface getSurface( int isurf ) {
        return plotPanel_.getLatestSurface( isurf );
    }

    @Override
    protected void handleClick( final Navigator<A> navigator, final int isurf,
                                final Point pos, final int ibutt,
                                final Iterable<double[]> dposIt ) {
        final Surface surface = getSurface( isurf );
        if ( surface != null ) {

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
                                setAspect( isurf, aspect );
                            }
                        } );
                    }
                }
            } );
        }
    }

    public Iterable<double[]> createDataPosIterable( Point pos ) {

        /* Handles progress reporting and thread interruption. */
        int iz = getSurfaceIndex( pos );
        Surface surf = plotPanel_.getSurface( iz );
        if ( surf != null && surf.getPlotBounds().contains( pos ) ) {
            GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud( iz );
            return pointCloud
                  .createDataPosIterable( pointCloud.createGuiDataStore() );
        }
        else {
            return null;
        }
    }
}
