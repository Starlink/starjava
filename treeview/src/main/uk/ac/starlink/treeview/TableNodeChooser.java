package uk.ac.starlink.treeview;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ErrorDialog;

/**
 * TreeNodeChooser subclass designed to return StarTable objects.
 */
public class TableNodeChooser extends TreeNodeChooser {

    private DataNodeFactory nodeFact;

    /**
     * Constructs a new chooser.  If <tt>root</tt> is <tt>null</tt>,
     * a default root is used (the user's current directory).
     *
     * @param  initial root of the tree to browse, or <tt>null</tt>
     */
    public TableNodeChooser( DataNode root ) {
        super( root );
        if ( root == null ) {
            try {
                File dir = new File( System.getProperty( "user.dir" ) );
                setRoot( new FileDataNode( dir ) );
            }
            catch ( NoSuchDataException e ) {
                // never mind
            }
        }
    }

    /**
     * Constructs a new chooser with the current directory as the root.
     */
    public TableNodeChooser() {
        this( null );
    }

    /**
     * As well as setting the root, this modifies the root node's 
     * childMaker object as appropriate for a tree which is interested 
     * in tables.
     */
    public void setRoot( DataNode root ) {
        DataNodeFactory maker = root.getChildMaker();
        maker.removeNodeClass( NDFDataNode.class );
        maker.removeNodeClass( HDSDataNode.class );
        maker.removeNodeClass( NDArrayDataNode.class );
        maker.removeNodeClass( NdxDataNode.class );
        maker.removeNodeClass( HDXDataNode.class );
        super.setRoot( root );
    }

    /**
     * Returns a lazily created node factory suitable for making nodes 
     * in a table-browsing environment.
     */
    public DataNodeFactory getNodeMaker() {
        if ( nodeFact == null ) {
            nodeFact = new DataNodeFactory();

            /* You might think that it would be sensible to put the
             * StarTable node at the head of the list, but this isn't
             * the case, since it's too eager: for instance it would turn 
             * a FITS file into a StarTable if any of its HDUs were tables,
             * and we want to be offered the opportunity of expanding it
             * and picking an HDU of our choice.  So we just remove some
             * of the node types which are not going to have tables 
             * inside. */
            String[] eschewed = new String[] {
                "uk.ac.starlink.treeview.NdxDataNode",
                "uk.ac.starlink.treeview.NDFDataNode",
                "uk.ac.starlink.treeview.WCSDataNode",
                "uk.ac.starlink.treeview.HDSDataNode",
                "uk.ac.starlink.treeview.NDArrayDataNode",
            };
            for ( int i = 0; i < eschewed.length; i++ ) {
                try {
                    Class clazz = Class.forName( eschewed[ i ] );
                    nodeFact.removeNodeClass( clazz );
                }
                catch ( ClassNotFoundException e ) {
                    // class not known, will not be created
                    // logger.warn( "Class " + echewed[ i ] + " not known" );
                }
            }
        }
        return nodeFact;
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
     * @param  parent  the parent component for the dialog
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
     * Pops up a modal dialog to choose a table from this chooser, with
     * default characteristics.
     * If an error occurs in turning the selection into a table,
     * the user will be informed, and <tt>null</tt> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return a table corresponding to the selected DataNode, 
     *         or <tt>null</tt> if none was selected or there was an error
     *         in converting it to a table
     */
    public StarTable chooseStarTable( Component parent ) {
        return chooseStarTable( parent, "Open Table", "Table browser" );
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
