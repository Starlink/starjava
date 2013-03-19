package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;

/**
 * GUI component for obtaining data position coordinates.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public interface PositionCoordPanel {

    /**
     * Returns the graphical component for user interaction.
     *
     * @return  component
     */
    JComponent getComponent();

    /**
     * Sets the table with reference to which this panel will resolve
     * coordinate descriptions.
     *
     * @param  tcModel   table from which coordinate values will be drawn
     */
    void setTable( TopcatModel tcModel );

    /**
     * Returns the coordinate values currently selected in this panel.
     * If there is insufficient information to contribute to a plot
     * (not all of the
     * {@link uk.ac.starlink.ttools.plot2.data.Coord#isRequired required}
     * coord values are filled in)
     * then null will be returned.
     *
     * @return   nCoord-element array of coord contents, or null
     */
    GuiCoordContent[] getContents();

    /**
     * Returns the position geometry that defines the mapping of input
     * to data coordinates.
     *
     * @return  data geom
     */
    DataGeom getDataGeom();

    /**
     * Adds a listener which will be notified when the coordinate selection
     * changes.
     *
     * @param  listener  listener
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener which was added previously.
     *
     * @param  listener  listener
     */
    void removeActionListener( ActionListener listener );
}
