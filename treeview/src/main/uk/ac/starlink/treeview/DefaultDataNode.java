package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * A basic implementation of the {@link DataNode} interface.
 * It may be used directly for simple nodes, or it may be subclassed 
 * for convenience in writing more specific <code>DataNode</code>
 * implementors.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class DefaultDataNode implements DataNode {
    static IconFactory iconMaker = IconFactory.getInstance();

    private static TreeCellRenderer cellRenderer;
    private static DataNodeFactory defaultChildMaker;

    private String name;
    private String label;
    private String nodetype = "Data node";
    private DataNodeFactory childMaker;
    private JComponent fullView;
    private CreationState creator;

    /**
     * Constructs a blank <code>DefaultDataNode</code>.
     */
    public DefaultDataNode() {
        this( "" );
    }

    /**
     * Constructs a <code>DefaultDataNode</code> with a given name.
     *
     * @param  name  the name to use for this object.
     */
    public DefaultDataNode( String name ) {
        this.name = name;
        setLabel( ( name == null ) ? "null" : name );
    }

    public boolean allowsChildren() {
        return false;
    }

    /**
     * Returns an array of the node's children; this method is called by
     * this class's implementation of <code>getChildIterator</code>.
     * To provide <code>DataNode</code>'s <code>getChildIterator</code>
     * public method, either this method or <code>getChildIterator</code>
     * itself should be overridden, but not both.
     *
     * @return  a list of the child nodes as iterated over by the 
     *          <code>getChildIterator</code> method - <code>null</code>
     *          in the default implementation
     */
    protected DataNode[] getChildren() {
        return null;
    }

    public Iterator getChildIterator() {
        return Arrays.asList( getChildren() ).iterator();
    }

    public boolean hasParentObject() {
        return false;
    }

    public Object getParentObject() {
        throw new UnsupportedOperationException();
    }

    public void setLabel( String label ) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return null;
    }

    /**
     * Returns the string "...".
     *
     * @return  "..."
     */
    public String getNodeTLA() {
        return "...";
    }

    public String getNodeType() {
        return nodetype;
    }

    public String toString() {
        if ( getLabel() == null ) {
            System.err.println( super.toString() + " has null label ?!?" );
        }
        String result = getLabel().trim();
        String desc = getDescription();
        if ( desc != null ) {
            desc = desc.trim();
            if ( desc.length() > 0 ) {
                result += "  " + desc;
            }
        }
        return result;
    }

    public Icon getIcon() {
        return iconMaker.getIcon( allowsChildren() ? IconFactory.PARENT
                                                   : IconFactory.LEAF );
    }

    public String getPathSeparator() {
        return ".";
    }

    public String getPathElement() {
        return getName();
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
        }
        return fullView;
    }

    public void setChildMaker( DataNodeFactory factory ) {
        childMaker = factory;
    }

    public DataNodeFactory getChildMaker() {
        if ( defaultChildMaker == null ) {
            defaultChildMaker = new DataNodeFactory();
        }
        if ( childMaker == null ) {
            childMaker = defaultChildMaker;
        }
        return childMaker;
    }

    public TreeCellRenderer getTreeCellRenderer() {
        /*
         * Note that the TreeCellRenderer is not constructed until it is 
         * asked for, so that if it never gets asked for we can execute 
         * without any Swing loader overheads.
         */
        if ( cellRenderer == null ) {
            cellRenderer = new DataNodeTreeCellRenderer();
        }
        return cellRenderer;
    }

    public void setCreator( CreationState state ) {
        this.creator = state;
    }

    public CreationState getCreator() {
        return creator;
    }

    /**
     * Returns the path from the top of the tree of DataNodes to a given
     * node.  This may be overridden by subclasses which know how to
     * determine their absolute pathname, but for those that don't this
     * implementation will probably give a sensible result.
     *
     * @return  the path to this node
     */
    public String getPath() {
        StringBuffer path = new StringBuffer();
        boolean ok = accumulatePath( this, path );
        return ok ? path.toString() : null;
    }

    private static boolean accumulatePath( DataNode dnode, StringBuffer path ) {
        CreationState creator = dnode.getCreator();
        String sep = dnode.getPathSeparator();
        if ( sep == null ) {
            return false;
        }
        if ( path.length() > 0 ) {
            path.insert( 0, sep );
        }
        String pathel = dnode.getPathElement();
        if ( pathel == null ) {
            return false;
        }
        path.insert( 0, pathel );
        DataNode parent = ( creator == null ) ? null : creator.getParent();
        if ( parent == null ) {
            return false;
        }
        else if ( parent == DataNode.ROOT ) {
            return true;
        }
        else {
            return accumulatePath( parent, path );
        }
    }

    /*
     * Class to do tree cell rendering.  It inherits most behaviour from
     * DefaultTreeCellRenderer, but modifies the icon and text in accordance
     * with the DataNode's getIcon and toString methods.
     */
    protected class DataNodeTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent( JTree tree, Object value,
                                                       boolean selected,
                                                       boolean expanded,
                                                       boolean leaf, int row,
                                                       boolean hasFocus ) {

            /* Get the data node from the tree node. */
            DefaultMutableTreeNode tNode = (DefaultMutableTreeNode) value;
            DataNode dNode = (DataNode) tNode.getUserObject();

            /* Use the superclass to construct a default component. */
            super.getTreeCellRendererComponent( tree, value, selected, 
                                                expanded, leaf, row, hasFocus );

            /* Customise it according to the data. */
            setIcon( dNode.getIcon() );
            String text = dNode.toString();
            if ( text.trim().equals( "" ) ) text = "...";
            setText( text );

            /* Return the constructed JLabel. */
            return this;
        }
    }

    /**
     * It beeps.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Gets the first few bytes of a file.
     * This utility function is suitable for magic number checks.
     *
     * @param  file  the file in question
     * @param  nbytes  the number of bytes to retrieve
     * @return   an nbytes-element array of the starting bytes in the file.
     *           If it's not long enough, non-existent bytes will appear
     *           as zeros.
     * @throws  IOException if there is some problem reading
     */
    public static byte[] startBytes( File file, int nbytes )
            throws IOException {
        byte[] buf = new byte[ nbytes ];
        InputStream strm = null;
        try {
            strm = new FileInputStream( file );
            strm.read( buf );
            return buf;
        }
        finally {
            if ( strm != null ) {
                strm.close();
            }
        }
    }
}
