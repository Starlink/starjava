package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Interface describing the action of a dialogue with which the user
 * can interact to specify a new table to load.
 *
 * @author    Mark Taylor (Starlink)
 * @since     25 Nov 2004
 */
public interface TableLoadDialog {

    /**
     * Presents the user with a dialogue which may be used to specify 
     * a table to load.
     * A null return indicates that the user did not successfully
     * choose a table.  Error conditions during the course of the user
     * interaction should in general be dealt with by informing the user
     * (e.g. with a popup) and permitting another attempt.
     * Some sort of cancel button should be provided which should trigger
     * a return with no table.
     *
     * <p>The <tt>formatModel</tt> argument may be used to determine or 
     * set the format to be used for interpreting tables.
     * Its entries are <tt>String</tt>s, and in general the selected one
     * should be passed as the <tt>handler</tt> argument to one of
     * <tt>factory</tt>'s <tt>makeStarTable</tt> methods.
     * The dialogue may wish to allow the user to modify the selection
     * by presenting a {@link javax.swing.JComboBox} based on this model.
     * An suitable model can be obtained using 
     * {@link StarTableChooser#makeFormatBoxModel}.
     * 
     * @param  parent   parent window
     * @param  factory  factory which may be used for table creation
     * @param  formatModel  comboBoxModel 
     * @return   array of 0 or more loaded tables, or null
     */
    StarTable loadTableDialog( Component parent, StarTableFactory factory,
                               ComboBoxModel formatModel );

    /**
     * Name of this dialogue.  This will typically be used as the text of
     * a button ({@link javax.swing.Action#NAME}) 
     * which invokes <tt>loadTableDialog</tt>
     *
     * @return  name
     */
    String getName();

    /**
     * Description of this dialogue.  This will typically be used as the
     * tooltip text of a button ({@link javax.swing.Action#SHORT_DESCRIPTION})
     * which invokes <tt>loadTableDialog</tt>
     *
     * @return   short description
     */
    String getDescription();

    /**
     * Indicates whether this dialog can be invoked.  This allows the
     * implementation to check that it has enough resources (e.g. required
     * classes) for it to be worth trying it.  This method should be 
     * invoked before the first invocation of {@link #loadTableDialog},
     * but is not guaranteed to be invoked again.
     *
     * @return  true iff this dialog can be used
     */
    boolean isEnabled();
}
