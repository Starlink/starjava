package uk.ac.starlink.topcat.vizier;

import java.awt.Component;
import javax.swing.JTable;

/**
 * Defines one of the GUI options for selecting a Vizier catalogue to search.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public interface VizierMode {

    /**
     * Returns a name for this mode.
     *
     * @return  name  mode name
     */
    String getName();

    /**
     * Returns the graphical component containing user controls for this
     * mode.
     *
     * @return   component
     */
    Component getComponent();

    /**
     * Returns a table whose rows represent VizieR catalogues.
     * The table's model must be a 
     * {@link uk.ac.starlink.util.gui.ArrayTableModel} with items 
     * that are {@link Queryable}s.
     *
     * @return  table of queryable objects representing Vizier catalogues
     */
    JTable getQueryableTable();

    /**
     * Sets the object which can query a VizieR server and store metadata.
     * This method must be called before the mode is called upon to 
     * contact the server.
     *
     * @param  vizinfo  vizier search object
     */
    void setVizierInfo( VizierInfo vizinfo );

    /**
     * Perform whatever updating from the remote server is necessary
     * to initialise the state of this component from the currently
     * installed VizierInfo.  This method is called from the event 
     * dispatch thread, but should work asynchronously so as not to
     * block the GUI.
     */
    void readData();
}
