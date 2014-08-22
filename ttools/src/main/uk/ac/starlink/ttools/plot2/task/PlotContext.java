package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;

/**
 * Aggregates some miscellaneous information required for a plot task
 * that may not be available until execution time.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2014
 */
public interface PlotContext {

    /**
     * Returns the plot type.
     *
     * @return  plot type
     */
    PlotType getPlotType();

    /**
     * Returns the DataGeom.
     *
     * @return  geom
     */
    DataGeom getDataGeom();
}
