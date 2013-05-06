package uk.ac.starlink.topcat.plot2;

import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.DataGeom;

/**
 * Simple implementation of a PositionCoordPanel.
 * It only deals with a single, fixed DataGeom.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class SimplePositionCoordPanel extends CoordPanel
                                      implements PositionCoordPanel {
    private final DataGeom geom_;

    /**
     * Constructor.
     *
     * @param  geom  fixed data geom
     * @param  autoPopulate  whether to attempt to fill in non-blank
     *                       data values automatically
     */
    public SimplePositionCoordPanel( DataGeom geom, boolean autoPopulate ) {
        super( geom.getPosCoords(), autoPopulate );
        geom_ = geom;
    }

    public DataGeom getDataGeom() {
        return geom_;
    }

    public JComponent getComponent() {
        return this;
    }
}
