package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Skeleton implementation of a {@link TableLoadDialog}.
 * Concrete subclasses need to populate this panel with components forming
 * the specific part of the query dialogue (presumably text fields, 
 * combo boxes and so on) and then implement the 
 * {@link #getTableSupplier} method which returns an object capable of
 * trying to load a table based on the current state of the component.
 * All the issues about threading are taken care of by the implementation
 * of this class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public abstract class BasicTableLoadDialog extends JPanel 
                                           implements TableLoadDialog {

    private final String name_;
    private final String description_;
    private final Action okAction_;
    private final Action cancelAction_;
    private final JProgressBar progBar_;
    private JDialog dialog_;
    private StarTableFactory factory_;
    private ComboBoxModel formatModel_;
    private TableConsumer consumer_;
    private TableSupplier supplier_;
    
    /**
     * Constructor.
     *
     * @param  name  dialogue name (typically used as text of a button)
     * @param  description  dialogue description (typically used as
     *         tooltip text)
     */
    public BasicTableLoadDialog( String name, String description ) {
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
    }

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

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
        supplier_ = null;

        /* Pop up the modal dialogue. */
        setBusy( false );
        dia.show();
        setBusy( false );

        /* Clear members used only when the dialogue is active. */
        boolean ok = dia == dialog_;
        dialog_ = null;
        factory_ = null;
        formatModel_ = null;
        consumer_ = null;
        supplier_ = null;

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
        controlPanel.add( new JButton( okAction_ ) );

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
     * Concrete subclasses should implement this method to supply a
     * TableSupplier object which can attempt to load a table based on
     * the current state (as filled in by the user) of this component.
     * If the state is not suitable for an attempt at loading a table
     * (e.g. some components are filled in in an obviously wrong way)
     * then an exception should be thrown.
     *
     * @return  table supplier corresponding to current state of this component
     * @throws  IllegalStateException  if validation fails
     */
    protected abstract TableSupplier getTableSupplier()
            throws IllegalStateException;

    /**
     * Defines an object which can attempt to load a particular table.
     */
    public interface TableSupplier {

        /**
         * Attempts to load a table.
         * This synchronous method is not to be called on the event
         * dispatch thread.
         *
         * @param  factory  factory used for loading if necessary
         * @param  format   format string
         */
        StarTable getTable( StarTableFactory factory, String format )
                throws IOException;

        /**
         * Returns a string representation (location maybe) of the table
         * which this object will load.
         *
         * @return  table id
         */
        String getTableID();
    }

    /**
     * Gives visible indication (including disabling components) that this
     * component is active or not.
     *
     * @param  busy  whether we're busy
     */
    private void setBusy( boolean busy ) {
        setEnabled( ! busy );
        okAction_.setEnabled( ! busy );
        progBar_.setIndeterminate( busy );
    }


    private class DialogConsumer implements TableConsumer {
        final JDialog dia_;
        String id_;
        DialogConsumer( JDialog dia ) {
            dia_ = dia;
        }
        public void loadStarted( String id ) {
            id_ = id;
        }
        public void loadSucceeded( StarTable table ) {
            if ( dialog_ == dia_ ) {
                consumer_.loadStarted( id_ );
                consumer_.loadSucceeded( table );
                setBusy( false );
                dialog_.dispose();
            }
        }
        public void loadFailed( Throwable th ) {
            if ( dialog_ == dia_ ) {
                consumer_.loadStarted( id_ );
                consumer_.loadFailed( th );
                setBusy( false );
            }
        }
    }

    /**
     * Invoked when the OK button is pressed.
     */
    private void ok() {
        if ( dialog_ == null ) {
            return;
        }
        final TableSupplier supplier = getTableSupplier();
        final StarTableFactory factory = factory_;
        final String format = formatModel_.getSelectedItem().toString();
        setBusy( true );
        new LoadWorker( new DialogConsumer( dialog_ ),
                        supplier.getTableID() ) {
            protected StarTable attemptLoad() throws IOException {
                return supplier.getTable( factory, format ); 
            }
        }.invoke();
    }

    /**
     * Invoked when the Cancel button is pressed. 
     */
    private void cancel() {
        if ( dialog_ != null ) {
            dialog_.dispose();
        }
    }
}
