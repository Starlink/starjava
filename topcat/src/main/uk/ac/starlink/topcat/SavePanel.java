package uk.ac.starlink.topcat;

import javax.swing.JPanel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.TableSaveChooser;

/**
 * Abstract superclass for component which interrogates the user
 * about which tables to save.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2010
 */
public abstract class SavePanel extends JPanel {

    private final String title_;
    private final TableSaveChooser saveChooser_;

    /**
     * Constructor.
     *
     * @param   title   short component name for use in tabber
     * @param   saveChooser  controlling component
     */
    protected SavePanel( String title, TableSaveChooser saveChooser ) {
        title_ = title;
        saveChooser_ = saveChooser;
    }

    /**
     * Returns component name.
     *
     * @return  title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns chooser controlling this panel.
     *
     * @return  chooser
     */
    public TableSaveChooser getSaveChooser() {
        return saveChooser_;
    }

    /**
     * Returns the tables chosen by the user from this panel to save.
     *
     * @return  tables to save
     */
    public abstract StarTable[] getTables();
}
