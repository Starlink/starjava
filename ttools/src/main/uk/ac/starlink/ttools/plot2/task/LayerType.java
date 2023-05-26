package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;

/**
 * Represents the type of a plot layer as selected by the user.
 * This object does not correspond to anything very well-defined in
 * the plot2 API: it may represent either a single Plotter object or
 * a collection of Plotter objects from which one may be selected
 * by use of associated parameters.
 * However, it's a useful abstraction, to associate with a user-visible
 * task parameter, since the number of Plotter objects that may be
 * associated with a PlotType is typically too large for a comfortable
 * selection from a single choice parameter.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2014
 */
public interface LayerType {

    /**
     * Returns the name of this layer type.
     *
     * @return  name as selected by user
     */
    String getName();

    /**
     * Returns an XML description of this layer type.
     *
     * @return   one or more &lt;p&gt; elements
     */
    String getXmlDescription();

    /**
     * Returns a list of zero or more additional parameters associated
     * with this layer type that may be required to turn it into the
     * specification of an actual Plotter object.
     *
     * @param  suffix  layer suffix string for use in the execution environment
     * @return  zero or more associated parameters, for documentation purposes
     */
    Parameter<?>[] getAssociatedParameters( String suffix );

    /**
     * Returns a CoordGroup characteristic of this layer type.
     * It is not guaranteed that the returned value will be identical
     * to the CoordGroup of all the plotters that this type can return.
     *
     * @return  best-efforts CoordGroup
     */
    CoordGroup getCoordGroup();

    /**
     * Returns a list of any non-positional coordinates associated
     * with this layer.
     *
     * @return  zero or more non-positional coordinates
     */
    Coord[] getExtraCoords();

    /**
     * Returns the style keys associated with this layer type.
     *
     * @return  zero or more style keys associated with every layer produced
     *          by this type
     */
    ConfigKey<?>[] getStyleKeys();

    /**
     * Acquires a Plotter for this layer type.
     *
     * @param  env  execution environment
     * @param  suffix  layer suffix string
     *
     * @return  plotter
     */
    Plotter<?> getPlotter( Environment env, String suffix )
            throws TaskException;
}
