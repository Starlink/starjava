package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;

/**
 * Simple implementation of a PositionCoordPanel.
 * It only deals with a single, fixed DataGeom.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class SimplePositionCoordPanel implements PositionCoordPanel {

    private final CoordPanel coordPanel_;
    private final DataGeom geom_;

    /**
     * Constructor.
     *
     * @param  coordPanel  panel providing actual coordinate entry
     * @param  geom  fixed data geom
     */
    public SimplePositionCoordPanel( CoordPanel coordPanel, DataGeom geom ) {
        coordPanel_ = coordPanel;
        geom_ = geom;
    }

    public DataGeom getDataGeom() {
        return geom_;
    }

    public JComponent getComponent() {
        return coordPanel_;
    }

    public void setTable( TopcatModel tcModel ) {
        coordPanel_.setTable( tcModel );
    }

    public GuiCoordContent[] getContents() {
        return coordPanel_.getContents();
    }

    public void addActionListener( ActionListener listener ) {
        coordPanel_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        coordPanel_.removeActionListener( listener );
    }

    /**
     * Constructs a position coord panel based on a given DataGeom.
     * Each positional coordinate from the DataGeom is represented once.
     *
     * @param  geom   provides description of positional coordinates
     * @param  autoPopulate  if true, some attempt will be made to
     *                       fill in the fields with non-blank values
     *                       when a table is selected
     */
    public static SimplePositionCoordPanel createPanel( DataGeom geom,
                                                        boolean autoPopulate ) {
        CoordPanel cpanel = new CoordPanel( geom.getPosCoords(), autoPopulate );
        return new SimplePositionCoordPanel( cpanel, geom );
    }
}
