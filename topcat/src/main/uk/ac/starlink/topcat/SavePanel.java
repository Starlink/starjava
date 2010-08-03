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
    private final ComboBoxModel formatBoxModel_;

    /**
     * Constructor.
     *
     * @param   title   short component name for use in tabber
     * @param   formatBoxModel  selector model for table output format;
     *          the model contents are Strings
     */
    protected SavePanel( String title, ComboBoxModel formatBoxModel ) {
        title_ = title;
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
     * Configures the chooser currently controlling this panel.
     * The supplied <code>chooser</code> will be null if this panel is
     * not active.
     * This panel should take the responsibility for setting the chooser's
     * enabledness for as long as it is active.
     *
     * @param   chooser  controlling save chooser, or null
     */
    public abstract void setActiveChooser( TableSaveChooser chooser );

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
