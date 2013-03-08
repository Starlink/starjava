package uk.ac.starlink.ttools.plot2.layer;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Performs colouring of an outline.
 * This interface defines how to turn an outline shape into a legend icon,
 * but subclasses may provide more specific functionality for use during
 * the actual plot (cf {@link uk.ac.starlink.ttools.plot.Style}).
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
@Equality
public interface Stamper {

    /**
     * Returns an icon for use in a legend given an outline shape.
     *
     * @param  outliner  outline shape
     * @return  icon
     */
    Icon createLegendIcon( Outliner outliner );
}
