package uk.ac.starlink.datanode.tree;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.CompressedDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.HDSDataNode;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.NDArrayDataNode;
import uk.ac.starlink.datanode.nodes.NDFDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.WCSDataNode;

/**
 * TreeNodeChooser subclass designed to return 
 * {@link uk.ac.starlink.table.StarTable} objects.
 * DataNode implementations which wish to declare themselves (potentially)
 * choosable by this chooser, because they can provide an
 * associated <code>StarTable</code> object,
 * must return true from <code>DataNode.hasDataObject(DataType.TABLE)</code>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TableNodeChooser extends TreeNodeChooser {

    private DataNodeFactory nodeFact;

    private static List shunnedClassList;

    /**
     * Constructs a new chooser.
     */
    public TableNodeChooser() {
        super();

        /* Add some buttons for recursive searches. */
        Action findSelectedAction = getSearchSelectedAction();
        Action findAllAction = getSearchAllAction();
        findSelectedAction.putValue( Action.SHORT_DESCRIPTION,
                                     "Locate all tables under selected node" );
        findAllAction.putValue( Action.SHORT_DESCRIPTION,
                                "Locate all tables in the tree" );
        getButtonPanel().add( new JButton( findSelectedAction ) );
        getButtonPanel().add( Box.createHorizontalStrut( 10 ) );
        getButtonPanel().add( new JButton( findAllAction ) );
    }

    public void setRoot( DataNode root ) {
        customiseFactory( root.getChildMaker() );
        super.setRoot( root );
    }

    /**
     * Returns a lazily created node factory suitable for making nodes 
     * in a table-browsing environment.
     */
    public synchronized DataNodeFactory getNodeMaker() {
        if ( nodeFact == null ) {
            nodeFact = new DataNodeFactory();
            customiseFactory( nodeFact );
        }
        return nodeFact;
    }

    /**
     * Allows selection of any node which has a data object of type 
     * {@link uk.ac.starlink.datanode.nodes.DataType#TABLE}.
     *
     * @param  node  the node to test for choosability
     * @return  true iff the node is suitable for turning into a table
     */
    protected boolean isChoosable( DataNode node ) {
        return node.hasDataObject( DataType.TABLE );
    }

    /**
     * Pops up a modal dialog to choose a table from this chooser.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <code>null</code> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     *         (or <code>null</code> for default)
     * @param  title  the title of the dialog window
     *         (or <code>null</code> for default)
     * @return a table corresponding to the selected DataNode, 
     *         or <code>null</code> if none was selected or there was an error
     *         in converting it to a table
     */
    public StarTable chooseStarTable( Component parent, String buttonText,
                                      String title ) {
        StarTable starTable;
        DataNode node = chooseDataNode( parent, buttonText, title );
        try {
            return node == null ? null : makeStarTable( node );
        }
        catch ( IOException e ) {
            ErrorDialog.showError( parent, "Bad Table", e,
                                   "Failed to make StarTable from " + node );
            return null;
        }
    }

    /**
     * Pops up a modal dialog to choose a table from this chooser, with
     * default characteristics.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <code>null</code> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return a table corresponding to the selected DataNode, 
     *         or <code>null</code> if none was selected or there was an error
     *         in converting it to a table
     */
    public StarTable chooseStarTable( Component parent ) {
        return chooseStarTable( parent, "Open Table", "Table browser" );
    }

    /**
     * Turns a DataNode into a StarTable.
     *
     * @param   node the data node
     * @return  StarTable made from <code>node</code>
     * @throws  IOException  if there's trouble
     */
    public StarTable makeStarTable( DataNode node ) throws IOException {
        if ( isChoosable( node ) ) {
            assert node.hasDataObject( DataType.TABLE );
            try {
                return (StarTable) node.getDataObject( DataType.TABLE );
            }
            catch ( DataObjectException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
        else {
            throw new IllegalArgumentException( node + " is not a table" );
        }
    }
    
    /**
     * Does some customisation of a DataNodeFactory to make it suitable
     * for use in a TableNodeChooser.  Its builder list is modified so
     * that it doesn't make any nodes which can't contain a table.
     * <p>
     * You might think that it would also be a good idea to promote the
     * StarTableDataNode builder to the top of the builder list, but
     * it wouldn't be, since it is too eager; for instance it would turn
     * a FITS file into a StarTable if any of its HDUs were tables,
     * and we want to be offered the opportunity of expanding it and
     * picking the HDU of our choice.
     *
     * @param   fact  the factory to customise
     */
    private static void customiseFactory( DataNodeFactory fact ) {

        /* Make sure we have the list of DataNode classes we do not
         * wish to see. */
        if ( shunnedClassList == null ) {
            String[] shunned = new String[] {
                CompressedDataNode.class.getName(),
                NdxDataNode.class.getName(),
                NDFDataNode.class.getName(),
                WCSDataNode.class.getName(),
                HDSDataNode.class.getName(),
                NDArrayDataNode.class.getName(),
            };
            List classes = new ArrayList();
            for ( int i = 0; i < shunned.length; i++ ) {
                try {
                    Class clazz = 
                        Class.forName( shunned[ i ], true,
                              Thread.currentThread().getContextClassLoader());
                    classes.add( clazz );
                }
                catch ( ClassNotFoundException e ) {
                    // not known, so won't be used in any case
                }
            }
            shunnedClassList = classes;   
        }

        /* Remove each of the shunned classes from the factory. */
        for ( Iterator it = shunnedClassList.iterator(); it.hasNext(); ) {
            fact.removeNodeClass( (Class) it.next() );
        }
    }
}
