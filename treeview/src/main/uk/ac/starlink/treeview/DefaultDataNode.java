package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import uk.ac.starlink.util.DataSource;

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

    private static DataNodeFactory defaultChildMaker;
    private static final String PATH_UNSET = new String( "path_not_set" );

    private String name;
    private String label;
    private String path = PATH_UNSET;
    private String nodetype = "Data node";
    private DataNodeFactory childMaker;
    private JComponent fullView;
    private CreationState creator;
    private Object parentObject;

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
        return parentObject != null;
    }

    public Object getParentObject() {
        if ( hasParentObject() ) {
            return parentObject;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public void setParentObject( Object parent ) {
        this.parentObject = parent;
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
        return IconFactory.getIcon( allowsChildren() ? IconFactory.PARENT
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
        if ( path == PATH_UNSET ) {
            StringBuffer pbuf = new StringBuffer();
            boolean ok = accumulatePath( this, pbuf );
            path = ok ? pbuf.toString() : null;
        }
        return path;
    }

    public void setPath( String path ) {
        this.path = path;
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
        if ( dnode instanceof DefaultDataNode ) { // uuurrrggh
            DefaultDataNode ddnode = (DefaultDataNode) dnode;
            if ( ddnode.path != null && ddnode.path != PATH_UNSET ) {
                path.insert( 0, ddnode.path );
                return true;
            }
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

    /**
     * It beeps.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

}
