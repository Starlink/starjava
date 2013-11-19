package uk.ac.starlink.topcat.plot2;

import java.awt.event.MouseEvent;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;

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
                                final MouseEvent evt,
                                final Iterable<double[]> dposIt ) {

        /* The click operation *may* take time, if it is necessary to
         * iterate over the data positions.  To cover that possibility,
         * calculate the new aspect asynchronously and update the GUI
         * later on the EDT.  Also make sure that progress is logged. */
        plotPanel_.submitPlotAnnotator( new Runnable() {
            public void run() {
                final A aspect = navigator.click( surface, evt, dposIt );
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

        /* Get a cloud to iterate over for providing data positions. */
        final PointCloud cloud =
            new PointCloud( plotPanel_.getPlotLayers(), true );
        final DataStore dataStore = plotPanel_.getDataStore();

        /* Work out how many positions there are in the cloud. */
        long nr = 0;
        DataSpec[] dataSpecs = cloud.getDataSpecs();
        for ( int i = 0; i < dataSpecs.length; i++ ) {
            nr += ((GuiDataSpec) dataSpecs[ i ]).getRowCount();
        }
        final long nrow = nr;

        /* Return an iterable which iterates over those positions, updating
         * a progresser as it goes.  It also checks for interruptions. */
        return new Iterable<double[]>() {
            public Iterator<double[]> iterator() {
                final Iterator<double[]> baseIt =
                    cloud.createDataPosIterator( dataStore );
                final Progresser prog = plotPanel_.createProgresser( nrow );
                return new Iterator<double[]>() {
                    public double[] next() {
                        prog.increment();
                        return baseIt.next();
                    }
                    public boolean hasNext() {
                        if ( Thread.interrupted() ) {
                            Thread.currentThread().interrupt();
                            prog.reset();
                            return false;
                        }
                        else {
                            return baseIt.hasNext();
                        }
                    }
                    public void remove() {
                        baseIt.remove();
                    }
                };
            }
        };
    }
}
