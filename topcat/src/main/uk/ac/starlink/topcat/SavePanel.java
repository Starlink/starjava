package uk.ac.starlink.topcat;

import javax.swing.ComboBoxModel;
import javax.swing.JPanel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
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
    private final ComboBoxModel formatBoxModel_;

    /**
     * Constructor.
     *
     * @param   title   short component name for use in tabber
     * @param   saveChooser  controlling component
     * @param   formatBoxModel  selector model for table output format;
     *          the model contents are Strings
     */
    protected SavePanel( String title, TableSaveChooser saveChooser,
                         ComboBoxModel formatBoxModel ) {
        title_ = title;
        saveChooser_ = saveChooser;
        formatBoxModel_ = formatBoxModel;
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
     * Returns a selector for table output formats.
     * The contents of the model are Strings.
     *
     * @return   format selector model
     */
    public ComboBoxModel getFormatBoxModel() {
        return formatBoxModel_;
    }

    /**
     * Returns the tables chosen by the user from this panel to save.
     *
     * @return  tables to save
     */
    public abstract StarTable[] getTables();
}
