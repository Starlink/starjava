package uk.ac.starlink.astrogrid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.astrogrid.store.tree.Node;
import org.astrogrid.store.tree.TreeClientFactory;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.gui.LoadWorker;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.util.DataSource;

/**
 * Table load dialogue which allows browsing through MySpace for files.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Nov 2005
 */
public class MyspaceTableLoadDialog extends MyspaceSelector 
                                    implements TableLoadDialog {

    final private JComboBox formatComboBox_;
    private JDialog dialog_;
    private StarTableFactory factory_;
    private TableConsumer eater_;
    private ComboBoxModel dummyModel_;
    private Boolean success_;
    private Boolean isEnabled_;

    private static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    /**
     * Constructs a new loader.
     */
    public MyspaceTableLoadDialog() {
        setAllowContainerSelection( false );
        formatComboBox_ = new JComboBox();
        dummyModel_ = formatComboBox_.getModel();
        Box formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Format: " ) );
        formatBox.add( formatComboBox_ );
        formatBox.add( Box.createHorizontalGlue() );
        getExtraPanel().add( formatBox, BorderLayout.WEST );
        getExtraPanel().add( Box.createVerticalStrut( 5 ), BorderLayout.SOUTH );
    }

    public String getName() {
        return "Browse MySpace";
    }

    public String getDescription() {
        return "Load table from files in MySpace";
    }

    public boolean isEnabled() {
        if ( isEnabled_ == null ) {
            String msg;
            String endpointProperty = "org.astrogrid.registry.query.endpoint";
            try {
                String endpoint = System.getProperty( endpointProperty );
                if ( endpoint == null ) {
                    msg = "Property " + endpointProperty + " undefined";
                }
                else {
                    new TreeClientFactory().createClient();
                    msg = null;
                }
            }
            catch ( Throwable th ) {
                msg = th.toString();
            }
            if ( msg != null ) {
                logger_.info( msg + " - no MySpace" );
            }
            isEnabled_ = Boolean.valueOf( msg == null );
        }
        return isEnabled_.booleanValue();
    }

    public boolean showLoadDialog( Component parent, 
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {

        /* Install the format model. */
        formatComboBox_.setModel( formatModel );
        factory_ = factory;
        eater_ = eater;

        /* Create a dialogue containing this component. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }
        dialog_ = new JDialog( frame, "MySpace Table Browser", true );
        dialog_.getContentPane().setLayout( new BorderLayout() );
        dialog_.getContentPane().add( this, BorderLayout.CENTER );
        dialog_.setLocationRelativeTo( parent );
        dialog_.pack();
        success_ = null;
        dialog_.show();

        /* Tidy up and return. */
        boolean success = success_.booleanValue();
        factory_ = null;
        eater_ = null;
        success_ = null;
        formatComboBox_.setModel( dummyModel_ );
        return success;
    }

    /**
     * Invoked when load button is hit; this attempts to turn the currently
     * selected file into a table and stores it.
     * It then disposes the dialogue.
     */
    protected void ok() {
        Node node = getSelectedNode();

        /* Check we've got the right kind of object (it should be). */
        if ( ! ( node instanceof org.astrogrid.store.tree.File ) ) {
            return;
        }

        /* Create a data source. */
        final DataSource datsrc = 
            new MyspaceDataSource( (org.astrogrid.store.tree.File) node );

        /* Initiate load sequence. */
        final String format = getHandler();
        final StarTableFactory factory = factory_;
        new LoadWorker( eater_, node.getName() ) {
            protected StarTable attemptLoad() throws IOException {
                return factory.makeStarTable( datsrc, format );
            }
        }.invoke();
        success_ = Boolean.TRUE;
        dialog_.dispose();
    }

    /**
     * Disposes the dialogue.
     */
    protected void cancel() {
        success_ = Boolean.FALSE;
        dialog_.dispose();
    }

    /**
     * Returns the selected handler string.
     * 
     * @return  table load format
     */
    private String getHandler() {
        return (String) formatComboBox_.getSelectedItem();
    }
}
