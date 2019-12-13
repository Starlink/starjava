package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import java.io.IOException;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Object capable of executing a static or interactive plot.
 * All configuration options are contained.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2019
 */
public interface PlotConfiguration<P,A> {

    /**
     * Creates a data store suitable for use with this object.
     * 
     * @param     prevStore  previously obtained data store, may be null
     * @return    object containing plot data
     */
    DataStore createDataStore( DataStore prevStore )
            throws IOException, InterruptedException;

    /**
     * Returns the requested external size of the plot.
     *
     * @return  external bounds size
     */
    Dimension getPlotSize();

    /**
     * Returns a navigator suitable for the plot.
     *
     * @return  navigator
     */
    Navigator<A> createNavigator();

    /**
     * Creates a PlotScene that can paint the plot
     * 
     * @param  dataStore  object containing plot data
     * @param  caching   plot caching policy
     * @return  scene
     */
    PlotScene<P,A> createPlotScene( DataStore dataStore,
                                    PlotCaching caching );

    /**
     * Generates an icon which will draw the plot.
     * This may be slow to paint.
     *
     * @param  dataStore  object containing plot data
     */
    Icon createPlotIcon( DataStore dataStore );
}
