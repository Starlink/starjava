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
import uk.ac.starlink.table.load.AbstractTableLoadDialog2;
import uk.ac.starlink.table.load.TableLoader;

/**
 * Table load dialogue which can select tables from a datanode tree.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2010
 */
public class TreeTableLoadDialog2 extends AbstractTableLoadDialog2 {

    private TableNodeChooser chooser_;

    /**
     * Constructor.
     */
    public TreeTableLoadDialog2() {
        super( "Hierarchy Browser", "Load table using treeview-type browser" );
        setIcon( IconFactory.getIcon( IconFactory.HIERARCH ) );
    }

    public Component createQueryComponent() {
        NodeUtil.setGUI( true );
        chooser_ = createNodeChooser();
        chooser_.setControlsVisible( false );
        final Action chooseAct = chooser_.getChooseAction();
        chooseAct.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( "enabled".equals( evt.getPropertyName() ) ) {
                    TreeTableLoadDialog2.this
                                        .setEnabled( chooseAct.isEnabled() );
                }
            }
        } );
        setEnabled( chooseAct.isEnabled() );
        return chooser_;
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
                public StarTable[] loadTables( StarTableFactory tfact )
                        throws IOException {
                    try {
                        StarTable table =
                            (StarTable) node.getDataObject( DataType.TABLE );
                        return new StarTable[] { tfact.randomTable( table ) };
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
