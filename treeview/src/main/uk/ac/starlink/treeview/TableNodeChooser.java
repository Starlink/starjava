package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ErrorDialog;

/**
 * TreeNodeChooser subclass designed to return 
 * {@link uk.ac.starlink.table.StarTable} objects.
 * DataNode implementations which wish to declare themselves (potentially)
 * choosable by this chooser, because they can provide an
 * associated <tt>StarTable</tt> object,
 * must implement the {@link TableNodeChooser.Choosable} interface.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TableNodeChooser extends TreeNodeChooser {

    private DataNodeFactory nodeFact;

    private static List shunnedClassList;

    /**
     * Constructs a new chooser.  If <tt>root</tt> is <tt>null</tt>,
     * a default root is used (the user's current directory).
     *
     * @param  root initial root of the tree to browse, or <tt>null</tt>
     */
    public TableNodeChooser( DataNode root ) {
        super( root );
        if ( root == null ) {
            File dir = new File( System.getProperty( "user.dir" ) );
            setRoot( getNodeMaker().makeChildNode( null, dir ) );
        }

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
     * Allows selection of any node which implements 
     * {@link Choosable} and has <tt>isStarTable()==true</tt>.
     *
     * @param  node  the node to test for choosability
     * @return  true iff the node is suitable for turning into a table
     */
    protected boolean isChoosable( DataNode node ) {
        return node instanceof Choosable 
            && ((Choosable) node).isStarTable();
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
     * @param   node the data node
     * @return  StarTable made from <tt>node</tt>
     * @throws  IOException  if there's trouble
     */
    public StarTable makeStarTable( DataNode node ) throws IOException {
        if ( isChoosable( node ) ) {
            assert node instanceof Choosable;
            return ((Choosable) node).getStarTable();
        }
        else {
            throw new IllegalArgumentException( node + " is not choosable" );
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
                "uk.ac.starlink.treeview.CompressedDataNode",
                "uk.ac.starlink.treeview.NdxDataNode",
                "uk.ac.starlink.treeview.NDFDataNode",
                "uk.ac.starlink.treeview.WCSDataNode",
                "uk.ac.starlink.treeview.HDSDataNode",
                "uk.ac.starlink.treeview.NDArrayDataNode",
            };
            List classes = new ArrayList();
            for ( int i = 0; i < shunned.length; i++ ) {
                try {
                    Class clazz = Class.forName( shunned[ i ] );
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

    /**
     * DataNodes may implement this interface to declare themselves
     * choosable by instances of TableNodeChooser.  This indicates that
     * the implementing class is capable of supplying a StarTable.
     */
    public interface Choosable {

        /**
         * Indicates whether a StarTable can be obtained from this DataNode.
         * 
         * @return  <tt>true</tt> iff {@link #getStarTable} can be expected to
         *          return a StarTable
         */
        boolean isStarTable();

        /**
         * Returns the StarTable object associated with this DataNode.
         * Behaviour is undefined unless <tt>isStarTable()==true</tt>.
         *
         * @return  the table
         * @throws  IOException if an error obtaining the table occurs
         */
        StarTable getStarTable() throws IOException;
    }
            
}
