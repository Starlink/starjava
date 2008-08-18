package uk.ac.starlink.ttools.plottask;

import java.io.IOException;
import javax.swing.JComponent;

/**
 * Interface for plot output.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public interface Painter {

    /**
     * Export the graphics displayed by a Swing component in some way.
     *
     * @param   plot  component to export
     */
    void paintPlot( JComponent plot ) throws IOException;
}
