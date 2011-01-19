package uk.ac.starlink.datanode.tree;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.Action;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;

/**
 * Table load dialogue which can select tables from a datanode tree.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2010
 */
public class TreeTableLoadDialog extends AbstractTableLoadDialog {

    private TableNodeChooser chooser_;

    /**
     * Constructor.
     */
    public TreeTableLoadDialog() {
        super( "Hierarchy Browser", "Load table using treeview-type browser" );
        setIcon( IconFactory.getIcon( IconFactory.HIERARCH ) );
    }

    public Component createQueryComponent() {
        NodeUtil.setGUI( true );
        chooser_ = createNodeChooser();
        chooser_.setControlsVisible( false );
        chooser_.getChooseAction()
                .addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( "enabled".equals( evt.getPropertyName() ) ) {
                    updateReady();
                }
            }
        } );
        updateReady();
        return chooser_;
    }

    public boolean isReady() {
        return chooser_.getChooseAction().isEnabled();
    }

    public TableLoader createTableLoader() {
        final DataNode node = chooser_.getSelectedNode();
        if ( node == null || ! node.hasDataObject( DataType.TABLE ) ) {
            return null;
        }
        else {
            return new TableLoader() {
                public String getLabel() {
                    return node.getLabel();
                }
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    try {
                        StarTable table =
                            (StarTable) node.getDataObject( DataType.TABLE );
                        table = tfact.randomTable( table );
                        return Tables.singleTableSequence( table );
                    }
                    catch ( DataObjectException e ) {
                        throw (IOException)
                              new IOException( e.getMessage() ).initCause( e );
                    }
                }
            };
        }
    }

    /**
     * Constructs a node chooser for use with this dialogue.
     *
     * @return   new node chooser
     */
    protected TableNodeChooser createNodeChooser() {
        return new TableNodeChooser();
    }
}
