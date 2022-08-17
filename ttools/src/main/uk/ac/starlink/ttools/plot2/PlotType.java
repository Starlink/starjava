package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * High-level description of a type of plot that can be drawn.
 * All the information about plotting options and implementation is
 * available from this interface, so that generic plot presentation
 * components (like user plot windows) can be written with no hard-coded
 * knowledge about whether they are plotting an all-sky plot, 3D scatter,
 * histogram or whatever.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public interface PlotType<P,A> {

    /**
     * Returns a list of one or more geometry variants which describe
     * how user-supplied point coordinates map to the data space.
     * If multiple values are returned, the first one may be used as some kind
     * of default.
     *
     * @return   data geom option list
     */
    DataGeom[] getPointDataGeoms();

    /**
     * Returns an object that can construct the plot surface including
     * axis painting and geometry information.
     *
     * @return   surface factory
     */
    SurfaceFactory<P,A> getSurfaceFactory();

    /**
     * Returns a list of plotters that can be used to paint
     * data on the surface.
     *
     * @return   plotter list
     */
    Plotter<?>[] getPlotters();

    /**
     * Returns an object which can provide graphics rendering functionality
     * based on the required plot layers for this plot type.
     *
     * @return  paper type selector
     */
    PaperTypeSelector getPaperTypeSelector();
}
