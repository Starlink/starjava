package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;

/**
 * Interface for subdialogues which know how to save a table to some 
 * destination or other.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
public interface TableSaveDialog {

    /**
     * Returns this dialogue's name 
     * (may be used as an Action's NAME property).
     *
     * @return  name
     */
    String getName();

    /**
     * Returns this dialogue's description
     * (may be used as an Action's SHORT_DESCRIPTION property).
     *
     * @return description
     */
    String getDescription();

    /**
     * Returns an icon for use in identifying this dialogue.
     *
     * @return  icon
     */
    Icon getIcon();

    /**
     * Indicates whether there is a reasonable chance of this dialogue
     * working.
     *
     * @return   false iff there's no point offering use of this dialogue
     */
    boolean isAvailable();

    /**
     * Pops up a modal dialogue which allows the user to save the given
     * tables to a single destination.  The dialogue should allow the
     * user to select an output destination and return only when he
     * has done so or indicated that he does not wish to.
     * Having selected a destination the tables should be saved to it.
     * If the save fails the user should be notified with a popup.
     *
     * @param   parent  parent component
     * @param   sto   object determining how tables are saved
     * @param   formatModel  combo box model containing names of table
     *          save formats which can be selected
     * @param   tables  the tables to save
     * @return   true iff the save completed successfully
     */
    boolean showSaveDialog( Component parent, StarTableOutput sto,
                            ComboBoxModel formatModel, StarTable[] tables );
}
