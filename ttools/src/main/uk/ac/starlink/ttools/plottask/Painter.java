package uk.ac.starlink.ttools.plottask;

import java.io.IOException;
import uk.ac.starlink.ttools.plot.Picture;

/**
 * Interface for plot output.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public interface Painter {

    /**
     * Export the graphics contained in a Picture.
     *
     * @param   picture  graphics to export
     */
    void paintPicture( Picture picture ) throws IOException;
}
