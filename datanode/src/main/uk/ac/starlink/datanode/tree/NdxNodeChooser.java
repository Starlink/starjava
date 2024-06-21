package uk.ac.starlink.datanode.tree;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.CompressedDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.DataObjectException;
import uk.ac.starlink.datanode.nodes.DataType;
import uk.ac.starlink.datanode.nodes.HistoryDataNode;
import uk.ac.starlink.datanode.nodes.JDBCDataNode;
import uk.ac.starlink.datanode.nodes.NDFDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.StarTableDataNode;
import uk.ac.starlink.datanode.nodes.TableHDUDataNode;
import uk.ac.starlink.datanode.nodes.VOComponentDataNode;
import uk.ac.starlink.datanode.nodes.VOTableDataNode;
import uk.ac.starlink.datanode.nodes.XMLDataNode;

/**
 * TreeNodeChooser subclass designed to return {@link uk.ac.starlink.ndx.Ndx}
 * objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdxNodeChooser extends TreeNodeChooser {

    private DataNodeFactory nodeFact;
    private int minDims = 1;
    private int maxDims = Integer.MAX_VALUE;

    private static List shunnedClassList;
    private static List deprecatedClassList;
    private static List preferredClassList;

    /**
     * Constructs a new chooser.
     */
    public NdxNodeChooser() {

        /* Add some buttons for recursive searches. */
        Action findSelectedAction = getSearchSelectedAction();
        Action findAllAction = getSearchAllAction();
        findSelectedAction.putValue( Action.SHORT_DESCRIPTION,
                                     "Locate all NDXs under selected node" );
        findAllAction.putValue( Action.SHORT_DESCRIPTION,
                                "Locate all NDXs in the tree" );
        getButtonPanel().add( new JButton( findSelectedAction ) );
        getButtonPanel().add( Box.createHorizontalStrut( 10 ) );
        getButtonPanel().add( new JButton( findAllAction ) );
    }

    public void setRoot( DataNode root ) {
        customiseFactory( root.getChildMaker() );
        super.setRoot( root );
    }

    public synchronized DataNodeFactory getNodeMaker() { 
        if ( nodeFact == null ) {
            nodeFact = new DataNodeFactory();
            customiseFactory( nodeFact );
        }
        return nodeFact;
    }

    /**
     * Sets the minimum dimensionality that an NDX must have to qualify
     * for choosability.
     *
     * @param  minDims   minimum acceptable dimensionality
     */
    public void setMinDims( int minDims ) {
        this.minDims = minDims;
    }

    /**
     * Sets the maximum dimensionality that an NDX must have to qualify
     * for choosability.
     *
     * @param   maxDims  maximum acceptable dimensionality
     */
    public void setMaxDims( int maxDims ) {
        this.maxDims = maxDims;
    }

    /**
     * Allows selection of any node which can supply an NDX which fits the
     * dimensionality constraints of this chooser.
     *
     * @param  node  the node to test for choosability
     * @return  true iff the node is suitable for turning into a table
     */
    protected boolean isChoosable( DataNode node ) {
        return node.hasDataObject( DataType.NDX );
    }

    /**
     * Pops up a modal dialog to choose an NDX from this chooser.
     * If an error occurs in turning the selection into an NDX,
     * the user will be informed, and <code>null</code> will be returned.
     * 
     * @param  parent  the parent component for the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     * @param  title  the title of the dialog window
     * @return  an NDX corresponding to the selected DataNode, 
     *          or <code>null</code> if none was selected or there was
     *          an error converting it to an NDX
     */
    public Ndx chooseNdx( Component parent, String buttonText, String title ) {
        Ndx ndx;
        DataNode node = chooseDataNode( parent, buttonText, title );
        try {
            return node == null ? null : makeNdx( node );
        }
        catch ( IOException e ) {
            ErrorDialog.showError( parent, "Bad NDX", e,
                                   "Failed to make NDX from " + node );
            return null;
        }
    }

    /**
     * Pops up a modal dialog to choose an NDX from this chooser, with 
     * default characteristics.
     * If an error occurs in turning the selection into an NDX,
     * the user will be informed, and <code>null</code> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return  an NDX corresponding to the selected DataNode,
     *          or <code>null</code> if none was selected or there was
     *          an error converting it to an NDX
     */
    public Ndx chooseNdx( Component parent ) {
        return chooseNdx( parent, "Open NDX", "NDX browser" );
    }

    /**
     * Turns a DataNode into an NDX.
     *
     * @param  node  the data node
     * @return  NDX made from <code>node</code>
     * @throws  IOException if there's trouble
     */
    public Ndx makeNdx( DataNode node ) throws IOException {
        if ( isChoosable( node ) ) {
            assert node.hasDataObject( DataType.NDX );
            try {
                return (Ndx) node.getDataObject( DataType.NDX );
            }
            catch ( DataObjectException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
        else {
            throw new IllegalArgumentException( node + " is not an NDX" );
        }
    }

    /**
     * Does some customisation of a DataNodeFactory to make it suitable
     * for use in an NDXNodeChooser.  Its builder list is modified so
     * it doesn't investigate any nodes which can't contain an NDX.
     */
    private static void customiseFactory( DataNodeFactory fact ) {

        /* Make sure we have the list of DataNode classes we do not wish
         * to see. */
        if ( shunnedClassList == null ) {
            String[] shunned = new String[] {
                CompressedDataNode.class.getName(),
                JDBCDataNode.class.getName(),
                StarTableDataNode.class.getName(),
                TableHDUDataNode.class.getName(),
                VOComponentDataNode.class.getName(),
                VOTableDataNode.class.getName(),
                NDFDataNode.class.getName(),
                HistoryDataNode.class.getName(),
            };
            List classes = new ArrayList();
            for ( int i = 0; i < shunned.length; i++ ) {
                try {
                    Class clazz = 
                        Class.forName( shunned[ i ], true,
                              Thread.currentThread().getContextClassLoader() );
                    classes.add( clazz );
                }
                catch ( ClassNotFoundException e ) {
                    // not known, so won't be used in any case
                }
            }
            shunnedClassList = classes;

            deprecatedClassList = Arrays.asList( new Class[] {
                XMLDataNode.class,
            } );

            preferredClassList = Arrays.asList( new Class[] {
                NdxDataNode.class,
            } );
        }

        /* Remove each of the shunned classes from the factory. */
        for ( Iterator it = shunnedClassList.iterator(); it.hasNext(); ) {
            fact.removeNodeClass( (Class) it.next() );
        }

        /* Do some additional reordering. */
        for ( Iterator it = deprecatedClassList.iterator(); it.hasNext(); ) {
            fact.setDeprecatedClass( (Class) it.next() );
        }
        for ( Iterator it = preferredClassList.iterator(); it.hasNext(); ) {
            fact.setPreferredClass( (Class) it.next() );
        }
    }
}
