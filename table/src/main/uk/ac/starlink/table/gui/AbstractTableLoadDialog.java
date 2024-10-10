package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Partial implementation of TableLoadDialog interface.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public abstract class AbstractTableLoadDialog implements TableLoadDialog {

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
        Logger.getLogger( "uk.ac.starlink.table.gui" );

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  description  dialogue description
     */
    protected AbstractTableLoadDialog( String name, String description ) {
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
            updateReady();
        }
        return queryComponent_;
    }

    public JMenu[] getMenus() {
        return menus_;
    }

    /**
     * Sets the menus for this dialogue.
     *
     * @param  menus  menu array
     */
    public void setMenus( JMenu[] menus ) {
        menus_ = menus;
    }

    public Action[] getToolbarActions() {
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
     * Adds an action to the toolbar for this dialogue.
     * Utility function.
     *
     * @param act  action to add
     */
    protected void addToolbarAction( Action act ) {
        List<Action> toolActs =
            new ArrayList<>( Arrays.asList( getToolbarActions() ) );
        toolActs.add( act );
        setToolbarActions( toolActs.toArray( new Action[ 0 ] ) );
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
        formats_ = tfact.getKnownFormats().toArray( new String[ 0 ] );
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
    public JComboBox<String> createFormatSelector() {
        JComboBox<String> formatSelector =
            new JComboBox<>( formatSelectorModel_ );
        formatSelector.setEditable( true );
        return formatSelector;
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
     * Indicates whether the query component of this dialogue is currently
     * contained in a visible window.
     *
     * @return  true  iff this dialog's query component exists and is showing
     */
    public boolean isComponentShowing() {
        Window container =
            queryComponent_ == null
                ? null
                : (Window) SwingUtilities
                          .getAncestorOfClass( Window.class, queryComponent_ );
        return container != null && container.isShowing();
    }

    /**
     * Invokes this dialogue's Submit Action with a non-descript ActionEvent.
     */
    protected void submit() {
        getSubmitAction()
       .actionPerformed( new ActionEvent( AbstractTableLoadDialog.this,
                                          0, "Submit" ) );
    }

    /**
     * Updates the enabledness state of this dialogue's Submit Action
     * according to the current value of {@link #isReady}.
     * Subclasses should call this method when the return value of
     * <code>isReady</code> might have changed.
     */
    protected void updateReady() {
        getSubmitAction().setEnabled( isReady() );
    }

    /**
     * Indicates whether the submit action should be enabled.
     * The implementation should return true if the user should be
     * allowed to submit the query, or false if the internal state
     * of this dialogue is known to be incomplete in some way.
     * Evaluation should be fast; the return does not need to provide a
     * guarantee that a submitted query will suceed.
     *
     * <p>The default implementation returns true.
     *
     * @return   false iff dialogue state is known to be incomplete
     */
    public boolean isReady() {
        return true;
    }

    /**
     * Converts an exception to an IOException, probably by wrapping it
     * in one.  This utility method can be used for wrapping up an
     * exception of some other kind if it needs to be thrown in
     * <code>TableSupplier.getTable</code>.
     *
     * @param  th  base throwable
     * @return   IOException based on <code>th</code>
     */
    public static IOException asIOException( Throwable th ) {
        if ( th instanceof IOException ) {
            return (IOException) th;
        }
        String msg = th.getMessage();
        if ( msg != null ) {
            msg = th.getClass().getName();
        }
        return (IOException) new IOException( msg ).initCause( th );
    }

    /**
     * ComboBoxModel for selecting table formats from the list of those known
     * by this dialogue.
     */
    private class FormatComboBoxModel extends AbstractListModel<String>
                                      implements ComboBoxModel<String> {
        private Object selected_;
        public String getElementAt( int ix ) {
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
