package uk.ac.starlink.treeview;

import java.awt.Component;
import java.io.IOException;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ErrorDialog;

/**
 * TreeNodeChooser subclass designed to return StarTable objects.
 */
public class TableNodeChooser extends TreeNodeChooser {

    /**
     * Constructs a new chooser.
     *
     * @param  initial root of the tree to browse
     */
    public TableNodeChooser( DataNode root ) {
        super( root );
    }

    /**
     * Allows selection of nodes of type 
     *    {@link TableHDUDataNode},
     *    {@link VOTableTableDataNode},
     *    {@link StarTableDataNode},
     * and any node which implements {@link StarTableChoosable} and
     * has <tt>isStarTable()==true</tt>.
     */
    protected boolean isChoosable( DataNode node ) {
        return node instanceof TableHDUDataNode 
            || node instanceof VOTableTableDataNode
            || node instanceof StarTableDataNode 
            || node instanceof StarTableChoosable &&
               ((StarTableChoosable) node).isStarTable();
    }

    /**
     * Pops up a modal dialog to choose a table from this chooser.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <tt>null</tt> will be returned.
     *
     * @param  buttonText  the text to appear on the 'choose' button
     *         (or <tt>null</tt> for default)
     * @param  title  the title of the dialog window
     *         (or <tt>null</tt> for default)
     * @return a table corresponding to the selected DataNode, 
     *         or <tt>null</tt> if none was selected or there was an error
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
            ErrorDialog.showError( e, "Failed to make StarTable from " + node,
                                   parent );
            return null;
        }
    }

    /**
     * Turns a DataNode into a StarTable.
     *
     * @param   node
     * @return  StarTable made from <tt>node</tt>
     * @throws  IOException  if there's trouble
     */
    public StarTable makeStarTable( DataNode node ) throws IOException {
        if ( node instanceof TableHDUDataNode ) {
            return ((TableHDUDataNode) node).getStarTable();
        }
        else if ( node instanceof VOTableTableDataNode ) {
            return ((VOTableTableDataNode) node).getStarTable();
        }
        else if ( node instanceof StarTableDataNode ) {
            return ((StarTableDataNode) node).getStarTable();
        }
        else if ( node instanceof StarTableChoosable ) {
            return ((StarTableChoosable) node).getStarTable();
        }
        else if ( node == null ) {
            return null;
        }
        else if ( ! isChoosable( node ) ) {
            throw new IllegalArgumentException( node + " is not choosable" );
        }
        else {
            throw new AssertionError( "How did " + node + " (" + 
                                      node.getClass().getName() +
                                      " get here?" );
        }
    }

    /**
     * DataNodes may implement this interface to declare themselves
     * choosable by instances of TableNodeChooser.
     */
    public interface StarTableChoosable {

        /**
         * Indicates whether a StarTable can be obtained from this DataNode.
         * 
         * @return  <tt>true</tt> iff {@link #getStarTable} can be expected to
         *          return a StarTable
         */
        boolean isStarTable();

        /**
         * Returns the StarTable object associated with this DataNode.
         * This method should only be called if {@link #isStarTable} 
         * returns <tt>true</tt>.
         *
         * @return  the table
         */
        StarTable getStarTable() throws IOException;
    }
            
}
