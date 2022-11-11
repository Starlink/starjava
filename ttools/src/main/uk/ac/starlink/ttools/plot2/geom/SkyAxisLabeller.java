package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Graphics2D;
import java.awt.Insets;
import uk.ac.starlink.ttools.plot2.Captioner;

/**
 * Performs axis labelling for a sky plot.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2013
 */
public interface SkyAxisLabeller {

    /**
     * Returns a name for this axis labelling mode.
     *
     * @return  one-word name
     */
    String getLabellerName();

    /**
     * Returns a description for this mode.
     *
     * @return  description
     */
    String getLabellerDescription();

    /**
     * Returns an axis annotation object for a given grid painter and
     * captioner.
     *
     * @param  gridLiner  grid lines for a sky plot
     * @param  captioner  text renderer
     * @param  skySys   sky coordinate system
     * @return  axis annotation
     */
    AxisAnnotation createAxisAnnotation( GridLiner gridLiner,
                                         Captioner captioner,
                                         SkySys skySys );
}
