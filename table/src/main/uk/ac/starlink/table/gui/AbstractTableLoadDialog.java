package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Skeleton implementation of a {@link TableLoadDialog}.
 * Concrete subclasses should implement the abstract methods
 * {@link #submitLoad} and {@link #cancelLoad}.
 *
 * <p>Subclasses are encouraged to override the
 * {@link javax.swing.JComponent#setEnabled} method to en/disable
 * child components which ought not to be active while a load is actually
 * taking place.  The overriding implementation ought to call
 * <tt>super.setEnabled</tt>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Oct 2009
 */
public abstract class AbstractTableLoadDialog extends JPanel
                                              implements TableLoadDialog {

    private final String name_;
    private final String description_;
    private final Action okAction_;
    private final Action cancelAction_;
    private final JProgressBar progBar_;
    private final ComboBoxModel emptyModel_;
    private Icon icon_;
    private JDialog dialog_;
    private StarTableFactory factory_;
    private ComboBoxModel formatModel_;
    private TableConsumer consumer_;

    /**
     * Constructor.
     * 
     * @param  name  dialogue name (typically used as text of a button)
     * @param  description  dialogue description (typically used as 
     *         tooltip text)
     */
    public AbstractTableLoadDialog( String name, String description ) {
        super( new BorderLayout() );
        name_ = name;
        description_ = description;
        okAction_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                ok();
            }
        };
        cancelAction_ = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancel();
            }
        };
        progBar_ = new JProgressBar();
        emptyModel_ = new DefaultComboBoxModel();
    }

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public Icon getIcon() {
        return icon_;
    }

    /**
     * Sets the icon to associate with this dialogue.
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

    /**
     * Should feed a table or tables to the given consuemer based on the
     * current state of this component.  This method is invoked when the
     * OK action is selected by the user.
     *
     * @param  dialog  dialogue currently containing this component
     * @param  tfact   table factory to use for generating tables
     * @param  format  selected table format;
     *                 it may or may not be appropriate to ignore this hint
     * @param  consumer  destination for loaded table or tables
     */
    protected abstract void submitLoad( JDialog dialog, StarTableFactory tfact,
                                        String format, TableConsumer consumer )
            throws Exception;

    /**
     * Should interrupt any current load action, so that any pending load
     * which is not complete should avoid passing tables to the consumer
     * in the future, and preferably any work in progress should be 
     * stopped.
     */
    protected abstract void cancelLoad();

    public boolean showLoadDialog( Component parent,
                                   StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer consumer ) {

        /* Check state. */
        if ( dialog_ != null ) {
            throw new IllegalStateException( "Dialogue already active" );
        }

        /* Construct a modal dialogue containing this component. */
        JDialog dia = createDialog( parent );

        /* Set up state used only when the dialogue is active. */
        dialog_ = dia;
        factory_ = factory;
        formatModel_ = formatModel;
        consumer_ = consumer;
        setFormatModel( formatModel );

        /* Pop up the modal dialogue. */
        dia.show();
        setBusy( false );

        /* Clear members used only when the dialogue is active. */
        boolean ok = dia == dialog_;
        dialog_ = null;
        factory_ = null;
        formatModel_ = null;
        consumer_ = null;
        setFormatModel( emptyModel_ );

        /* Return status. */
        return ok;
    }

    /**
     * Constructs a dialogue based on this component; this component forms
     * the main part of the dialogue window, with an OK and Cancel button
     * shown as well.  This method may be overridden by subclasses to
     * customise the dialogue's appearance.
     *
     * @param  parent component
     * @return  modal dialogue
     */
    protected JDialog createDialog( Component parent ) {

        /* Work out the new dialogue's master. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }

        /* Create a panel containing OK and Cancel buttons. */
        JComponent controlPanel = Box.createHorizontalBox();
        controlPanel.add( Box.createHorizontalGlue() );
        controlPanel.add( new JButton( cancelAction_ ) );
        controlPanel.add( Box.createHorizontalStrut( 5 ) );
        controlPanel.add( new JButton( okAction_ ) );
        controlPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

        /* Create the dialogue containing this component. */
        JDialog dialog = new JDialog( frame, getName(), true );
        JComponent main = new JPanel( new BorderLayout() );
        main.add( this, BorderLayout.CENTER );
        main.add( controlPanel, BorderLayout.SOUTH );
        dialog.getContentPane().setLayout( new BorderLayout() );
        dialog.getContentPane().add( main, BorderLayout.CENTER );
        dialog.getContentPane().add( progBar_, BorderLayout.SOUTH );

        /* Prepare and return dialogue. */
        dialog.setLocationRelativeTo( parent );
        dialog.pack();
        return dialog;
    }

    /**
     * Installs a table format selector intot this dialogue.
     * If it makes sense for a concrete dialogue implementation to
     * display format selection, it should override this method in
     * such a way as to present the format model to the user for
     * selection (presumably by setting it as the model of a
     * visible {@link javax.swing.JComboBox}).
     *
     * <p>The default implementation does nothing (suitable for classes
     * which can't make sense of varying table formats).
     *
     * @param   formatModel  selector model to install
     */
    protected void setFormatModel( ComboBoxModel formatModel ) {
    }

    /**
     * Returns the action associated with hitting the OK dialogue button.
     *
     * @return  OK action
     */
    protected Action getOkAction() {
        return okAction_;
    }

    /**
     * Returns the action associated with hitting the Cancel dialogue button.
     *
     * @return  Cancel action
     */
    protected Action getCancelAction() {
        return cancelAction_;
    }

    /**
     * Returns the progress bar at the bottom of the dialogue window.
     */
    protected JProgressBar getProgessBar() {
        return progBar_;
    }

    /**
     * Gives visible indication (including disabling components) that this
     * component is active or not.
     *
     * @param  busy  whether we're busy
     */
    protected void setBusy( boolean busy ) {
        setEnabled( ! busy );
        okAction_.setEnabled( ! busy );
        progBar_.setIndeterminate( busy );
    }

    /**
     * Indicates whether the given dialogue is currently being displayed
     * to the user containing this component.
     *
     * @param  dialog  dialog window to test
     * @return   true  iff <code>dialog</code> is being displayed
     */
    protected boolean isActive( JDialog dialog ) {
        return dialog == dialog_;
    }

    /**
     * Invoked when the OK button is pressed.
     */
    private void ok() {
        if ( dialog_ == null ) {
            return;
        }
        Object formatItem = formatModel_.getSelectedItem();
        String format = formatItem == null ? null : formatItem.toString();
        try {
            submitLoad( dialog_, factory_, format, consumer_ );
        }
        catch ( Exception e ) {
            ErrorDialog.showError( dialog_, "Dialogue Error", e );
            return;
        }
    }

    /**
     * Invoked when the Cancel button is pressed.
     */
    private void cancel() {
        if ( dialog_ != null ) {
            dialog_.dispose();
            dialog_ = null;
        }
        cancelLoad();
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
}
