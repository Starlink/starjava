package uk.ac.starlink.datanode.tree.select;

import java.io.File;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import uk.ac.starlink.connect.ConnectorAction;
import uk.ac.starlink.connect.ConnectorManager;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;

/**
 * Combo box from which can be selected a DataNode to serve as the 
 * root of the visible tree.  From an application's point of view,
 * the idea is that this can be used to obtain any starting point 
 * the user wants to get to.
 *
 * <p>The choices it presents to the user are the root(s) of the 
 * local filesystem, plus any roots of remote filesystems which are
 * available.  You can set the current value to any of these roots,
 * or any descendants of them.  As a convenience to the user, it 
 * also permits the selection of any of the ancestors of the selected
 * node.  This is basically what the combo box in the JFileChooser
 * does, but this one allows remote filesystems as well.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2005
 */
public class NodeRootComboBox extends JComboBox {

    private NodeRootModel model_;
    private final DataNodeFactory factory_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.datanode.tree.select" );

    /**
     * Constructor.
     *
     * @param   factory used for creating new nodes within this component
     */
    public NodeRootComboBox( DataNodeFactory factory ) {
        super( new NodeRootModel( factory ) );
        factory_ = factory;
        assert model_ instanceof NodeRootModel;
        setRenderer( new NodeListCellRenderer( getRenderer() ) );
    }

    /**
     * Sets this combo box's model.  Will only accept a suitable model
     * (one acquired from another <code>NodeRootComboBox</code>).
     *
     * @param  model  model
     * @throws  ClassCastException  if it's the wrong type
     */
    public void setModel( ComboBoxModel model ) {
        model_ = (NodeRootModel) model;
        super.setModel( model );
    }

    /**
     * Returns a {@link NodeRootModel}.
     *
     * @return   model
     */
    public ComboBoxModel getModel() {
        assert model_ == super.getModel();
        return model_;
    }

    /**
     * Returns the selected DataNode.
     *
     * @return  current selection
     */
    public DataNode getSelectedNode() {
        return model_.getSelectedNode();
    }

    /**
     * Returns any ConnectorAction associatd with the currently selected node.
     * It will be null unless the selection represents a remote filesystem.
     *
     * @return  connector action, or null
     */
    public ConnectorAction getConnectorAction() {
        return model_.getConnectorAction();
    }

    /**
     * Adds root nodes to the selector representing all the known 
     * filestores.  This includes any roots of the local filesytem(s)
     * and any remote filestores as supplied by 
     * {@link uk.ac.starlink.connect.ConnectorManager}.
     * The selection is also set to a sensible initial value
     * (probably the current directory).
     */
    public void addDefaultRoots() {

        /* Note: there is a problem with listRoots on Windows 2000 - it
         * pops up a dialogue about empty removable drives (floppy, cd-rom).
         * See Java bug id #4711632.  There may be workarounds but I tried
         * for a bit and didn't manage (hard without a local win2000 machine),
         * so I've given up for now since there's probably not that many
         * win2000 users. */
        File[] fileRoots = File.listRoots();

        /* Add nodes for local filesystems. */
        for ( int i = 0; i < fileRoots.length; i++ ) {
            File dir = fileRoots[ i ];
            if ( dir.isDirectory() && dir.canRead() ) {
                try {
                    DataNode dirNode = factory_.makeDataNode( null, dir );
                    model_.addChain( new NodeChain( dirNode ) );
                }
                catch ( NoSuchDataException e ) {
                    logger_.warning( "Can't read directory " + dir + 
                                     ": " + e );
                }
            }

            /* Unreadable root is quite common on MS Windows for, e.g.,
             * empty removable drives. */
            else {
                logger_.info( "Local filesystem root " + dir +
                              " is not a readable directory" );
            }
        }

        /* Add nodes for remote filestores. */
        ConnectorAction[] actions = ConnectorManager.getConnectorActions();
        for ( int i = 0; i < actions.length; i++ ) {
            DataNode node = new ConnectorDataNode( actions[ i ] );
            factory_.configureDataNode( node, null, actions[ i ] );
            model_.addChain( new NodeChain( node ) );
        }

        /* Try to set the current selection to something sensible. */
        File dir = new File( "." );
        try {
            dir = new File( System.getProperty( "user.dir" ) );
        }
        catch ( SecurityException e ) {
            logger_.warning( "Can't get current directory" );
        }
        if ( dir.isDirectory() ) {
            try {
                DataNode node = factory_.makeDataNode( null, dir );
                factory_.fillInAncestors( node );
                model_.setSelectedItem( node );
            }
            catch ( NoSuchDataException e ) {
                logger_.warning( "Can't create node from current directory: " 
                               + e );
            }
        }
        else {
            logger_.warning( "Can't read current directory" );
        }

    }
}
