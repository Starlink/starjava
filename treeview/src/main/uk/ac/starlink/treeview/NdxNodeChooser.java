package uk.ac.starlink.treeview;

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
import uk.ac.starlink.util.ErrorDialog;

/**
 * TreeNodeChooser subclass designed to return {@link uk.ac.starlink.ndx.Ndx}
 * objects.
 * DataNode implementations which wish to declare themselves (potentially)
 * choosable by this chooser, because they can provide an associated
 * <tt>Ndx</tt> object,
 * must implement the {@link NdxNodeChooser.Choosable} interface.
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
     * Constructs a new chooser.  If <tt>root</tt> is <tt>null</tt>,
     * a default root is used (the user's current directory).
     *
     * @param   root  initial root of the tree to browse, or <tt>null</tt>
     */
    public NdxNodeChooser( DataNode root ) {
        super( root );
        if ( root == null ) {
            File dir = new File( System.getProperty( "user.dir" ) );
            setRoot( getNodeMaker().makeChildNode( null, dir ) );
        }

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

    /**
     * Constructs a new chooser with a default root.
     */
    public NdxNodeChooser() {
        this( null );
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
     * Allows selection of any node which implements {@link Choosable}
     * and has <tt>isStarTable()==true</tt> and fits the dimensionality
     * constraints of ths chooser.
     *
     * @param  node  the node to test for choosability
     * @return  true iff the node is suitable for turning into a table
     */
    protected boolean isChoosable( DataNode node ) {
        if ( node instanceof Choosable ) {
            Choosable ndxNode = (Choosable) node;
            if ( ndxNode.isNdx() ) {
                int ndim = ndxNode.getShape().getNumDims();
                if ( ndim <= maxDims && ndim >= minDims ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Pops up a modal dialog to choose an NDX from this chooser.
     * If an error occurs in turning the selection into an NDX,
     * the user will be informed, and <tt>null</tt> will be returned.
     * 
     * @param  parent  the parent component for the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     * @param  title  the title of the dialog window
     * @return  an NDX corresponding to the selected DataNode, 
     *          or <tt>null</tt> if none was selected or there was
     *          an error converting it to an NDX
     */
    public Ndx chooseNdx( Component parent, String buttonText, String title ) {
        Ndx ndx;
        DataNode node = chooseDataNode( parent, buttonText, title );
        try {
            return node == null ? null : makeNdx( node );
        }
        catch ( IOException e ) {
            ErrorDialog.showError( e, "Failed to make NDX from " + node,
                                   parent );
            return null;
        }
    }

    /**
     * Pops upa modal dialog to choose an NDX from this chooser, with 
     * default characteristics.
     * If an error occurs in turning the selection into an NDX,
     * the user will be informed, and <tt>null</tt> will be returned.
     *
     * @param  parent  the parent component for the dialog
     * @return  an NDX corresponding to the selected DataNode,
     *          or <tt>null</tt> if none was selected or there was
     *          an error converting it to an NDX
     */
    public Ndx chooseNdx( Component parent ) {
        return chooseNdx( parent, "Open NDX", "NDX browser" );
    }

    /**
     * Turns a DataNode into an NDX.
     *
     * @param  node  the data node
     * @return  NDX made from <tt>node</tt>
     * @throws  IOException if there's trouble
     */
    public Ndx makeNdx( DataNode node ) throws IOException {
        if ( isChoosable( node ) ) {
            assert node instanceof Choosable;
            return ((Choosable) node).getNdx();
        }
        else {
            throw new IllegalArgumentException( node + " is not choosable" );
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
                "uk.ac.starlink.treeview.CompressedDataNode",
                "uk.ac.starlink.treeview.JDBCDataNode",
                "uk.ac.starlink.treeview.StarTableDataNode",
                "uk.ac.starlink.treeview.TableHDUDataNode",
                "uk.ac.starlink.treeview.VOComponentDataNode",
                "uk.ac.starlink.treeview.VOTableDataNode",
                "uk.ac.starlink.treeview.VOTableTableDataNode",
                "uk.ac.starlink.treeview.NDFDataNode",
                "uk.ac.starlink.treeview.HistoryDataNode",
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

    /**
     * DataNodes may implement this interface to declare themselves 
     * choosable by instances of NdxNodeChooser.  This indicates that
     * the implemenating class is capable of supplying an NDX.
     */
    public interface Choosable {

        /**
         * Indicates whether an NDX is in fact available from this DataNode.
         *
         * @return  <tt>true</tt> iff {@link #getNdx} can be expected
         *          to return an NDX
         */
        boolean isNdx();

        /**
         * Gives the shape of the NDX which will be returned by {@link #getNdx}.
         * Behaviour is undefined unless <tt>isNdx()==true</tt>.
         *
         * @return  the shape of the NDX
         */
        NDShape getShape();

        /**
         * Returns the NDX object associated with this DataNode.
         * Behaviour is undefined unless <tt>isNdx()==true</tt>.
         *
         * @return  the NDX
         * @throws  IOException if an error obtaining the NDX occurs
         */
        Ndx getNdx() throws IOException;
    }
}
