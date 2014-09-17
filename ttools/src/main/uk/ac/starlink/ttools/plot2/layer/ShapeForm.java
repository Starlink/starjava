package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * Plotter Form sub-interface for use with ShapeMode.
 * This defines the shape of data points plotted, which may be influenced
 * by data other than the actual point position(s), for instance
 * error bar sizes.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public interface ShapeForm extends ModePlotter.Form {

    /**
     * Returns a description of this mode as an XML string.
     * The return value should be one or more &lt;p&gt; elements.
     *
     * @return  XML description of form
     */
    String getFormDescription();

    /**
     * Returns the number of data positions per tuple used by this form.
     *
     * @return   number of sets of positional coordinates
     */
    int getPositionCount();

    /**
     * Returns data coordinates additional to the basic position which
     * are required to plot a point.
     *
     * @return   additional plot coordinates
     */
    Coord[] getExtraCoords();

    /**
     * Returns style configuration keys specific to this form.
     * These keys will be used in the config map supplied to
     * {@link #createOutliner}.
     *
     * @return   config keys
     */
    ConfigKey[] getConfigKeys();

    /**
     * Returns an object which will do the work of drawing shapes
     * when supplied with the appropriate style information and data.
     * The significant keys in the supplied config map are those
     * given by {@link #getConfigKeys}.
     *
     * @param  config  configuration map from which values for this
     *                 form's config keys will be extracted
     * @return  new outliner object
     */
    Outliner createOutliner( ConfigMap config );
}
