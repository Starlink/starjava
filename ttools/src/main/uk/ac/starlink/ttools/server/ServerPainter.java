package uk.ac.starlink.ttools.server;

import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot2.task.PlotConfiguration;
import uk.ac.starlink.ttools.plottask.Painter;

/**
 * Custom painter implementation for use with the PlotServlet.
 * It doesn't plot anything, but it is able to store a
 * PlotConfiguration object for later retrieval.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2019
 */
public class ServerPainter implements Painter {

    private PlotConfiguration<?,?> plotConfig_;

    /** No-op. */
    public void paintPicture( Picture picture ) {
    }

    /**
     * Stores plot config.
     *
     * @param  plotConfig  config
     */
    public void setPlotConfiguration( PlotConfiguration<?,?> plotConfig ) {
        plotConfig_ = plotConfig;
    }

    /**
     * Retrieves plot config.
     *
     * @return  config
     */
    public PlotConfiguration<?,?> getPlotConfiguration() {
        return plotConfig_;
    }
}
