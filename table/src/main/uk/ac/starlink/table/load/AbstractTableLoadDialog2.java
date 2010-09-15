package uk.ac.starlink.table.load;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Partial implementation of TableLoadDialog2 interface.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public abstract class AbstractTableLoadDialog2 implements TableLoadDialog2 {

    private final String name_;
    private final String description_;
    private final FormatComboBoxModel formatSelectorModel_;
    private Component queryComponent_;
    private JMenu[] menus_;
    private Action[] toolbarActions_;
    private Icon icon_;
    private String[] formats_;
    private Action submitAct_;
    private boolean configured_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.load" );

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  description  dialogue description
     */
    protected AbstractTableLoadDialog2( String name, String description ) {
        name_ = name;
        description_ = description;
        formats_ = new String[ 0 ];
        formatSelectorModel_ = new FormatComboBoxModel();
        menus_ = new JMenu[ 0 ];
        toolbarActions_ = new Action[ 0 ];
    }

    /**
     * Constructs the query component used by this dialogue.
     * Called only once (lazily).
     *
     * @return  query component
     */
    protected abstract Component createQueryComponent();

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public Icon getIcon() {
        return icon_;
    }

    public Component getQueryComponent() {
        if ( queryComponent_ == null ) {
            if ( ! configured_ ) {
                logger_.warning( "getQueryComponent called before configure" );
            }
            queryComponent_ = createQueryComponent();
        }
        return queryComponent_;
    }

    public JMenu[] getMenus() {
        getQueryComponent();
        return menus_;
    }

    /**
     * Sets the menus for this dialogue.
     *
     * @param  menus  menu array
     */
    protected void setMenus( JMenu[] menus ) {
        menus_ = menus;
    }

    public Action[] getToolbarActions() {
        getQueryComponent();
        return toolbarActions_;
    }

    /**
     * Sets the toolbar actions for this dialogue.
     *
     * @param  acts  toolbar actions
     */
    protected void setToolbarActions( Action[] acts ) {
        toolbarActions_ = acts;
    }

    /**
     * The default implementation returns true.
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Sets the icon to associate with this dialogue.
     *
     * @param   icon   icon
     */
    public void setIcon( Icon icon ) {
        icon_ = icon;
    }

    /**
     * Sets the icon to associate with this dialogue by specifying its URL.
     * If a null URL is given, the icon is set null.
     *
     * @param  iconUrl  URL of gif, png or jpeg icon
     */
    public void setIconUrl( URL iconUrl ) {
        setIcon( iconUrl == null ? null : new ImageIcon( iconUrl ) );
    }

    public void configure( StarTableFactory tfact, Action submitAct ) {
        configured_ = true;

        /* Set formats. */
        FormatComboBoxModel fcm = formatSelectorModel_;
        fcm.fireAllRemoved();
        formats_ =
            (String[]) tfact.getKnownFormats().toArray( new String[ 0 ] );
        fcm.fireAllAdded();
        if ( fcm.getSelectedItem() == null && fcm.getSize() > 0 ) {
            fcm.setSelectedItem( fcm.getElementAt( 0 ) );
        }

        /* Set submission action. */
        submitAct_ = submitAct;
    }

    /**
     * Returns a new combo box which can be used to select table formats
     * from the ones known by this dialogue.
     * This method may be called multiple times, but the same model is
     * used in each case.
     *
     * @return  table format combo box
     */
    public JComboBox createFormatSelector() {
        return new JComboBox( formatSelectorModel_ );
    }

    /**
     * Returns the table format currently selected by any of the format
     * selectors.
     *
     * @return  selected table format
     * @see     #createFormatSelector
     */
    public String getSelectedFormat() {
        return (String) formatSelectorModel_.getSelectedItem();
    }

    public Action getSubmitAction() {
        return submitAct_;
    }

    /**
     * Invokes this dialogue's Submit Action with a non-descript ActionEvent.
     */
    protected void submit() {
        getSubmitAction()
       .actionPerformed( new ActionEvent( AbstractTableLoadDialog2.this,
                                          0, "Submit" ) );
    }

    /**
     * Sets this dialogue's Submit Action's enabledness state.
     *
     * @param  enabled   true iff the submit action can be invoked
     */
    protected void setEnabled( boolean enabled ) {
        getSubmitAction().setEnabled( enabled );
    }

    /**
     * ComboBoxModel for selecting table formats from the list of those known
     * by this dialogue.
     */
    private class FormatComboBoxModel extends AbstractListModel
                                      implements ComboBoxModel {
        private Object selected_;
        public Object getElementAt( int ix ) {
            return ix == 0 ? StarTableFactory.AUTO_HANDLER
                           : formats_[ ix - 1 ];
        }
        public int getSize() {
            return formats_.length + 1;
        }
        public Object getSelectedItem() {
            return selected_;
        }
        public void setSelectedItem( Object item ) {
            selected_ = item;
        }
        void fireAllRemoved() {
            fireIntervalRemoved( this, 0, getSize() );
        }
        void fireAllAdded() {
            fireIntervalAdded( this, 0, getSize() );
        }
    }
}
