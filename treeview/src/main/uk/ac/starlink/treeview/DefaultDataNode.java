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
    private short iconID = IconFactory.NO_ICON;

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

    public Iterator getChildIterator() {
        return null;
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

    /**
     * This may be called by subclasses to set the icon returned by 
     * this node to one of the ones defined in the IconFactory class.
     *
     * @param   code  one of the icon identifiers defined as static
     *          final members of the {@link IconFactory} class
     */
    protected void setIconID( short id ) {
        this.iconID = id;
    }

    /**
     * Returns a default icon, unless setIconID has been called, in which
     * case it returns the one indicated by that call.
     *
     * @return   an icon representing this node
     */
    public Icon getIcon() {
        if ( iconID == IconFactory.NO_ICON ) {
            return IconFactory.getIcon( allowsChildren() ? IconFactory.PARENT
                                                         : IconFactory.LEAF );
        }
        else {
            return IconFactory.getIcon( iconID );
        }
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

    /**
     * Uses the node's childMaker to turn objects into data nodes.
     * Nodes should if possible construct their children using this method
     * or one of the other <tt>makeChild</tt> methods.
     * invoking it in their getChildIterator implementation.  It may not
     * be possible to do so if the children cannot be constructed by
     * a DataNodeFactory, for instance if they do not have one-argument
     * constructors.
     *
     * @param  childObj  the object which forms the basis for a child
     *         data node
     */
    public DataNode makeChild( Object childObj ) {
        return makeChild( childObj, this, getChildMaker() );
    }

    /**
     * Uses a custom node factory and given parent to turn objects 
     * into data nodes.
     * Nodes may construct their children using this method if they
     * need to use a node factory other than the inherited one or
     * a parent other than themselves for the purpose.
     *
     * @param  childObj  the object which forms the basis for a child
     *         data node
     * @param  factory  the custom node factory
     */
    public DataNode makeChild( Object childObj, DataNode parent, 
                               DataNodeFactory factory ) {
        try {
            return factory.makeDataNode( parent, childObj );
        }
        catch ( NoSuchDataException e ) {
            return getChildMaker().makeErrorDataNode( parent, e );
        }
    }

    /**
     * Constructs an error data node from a throwable.  This method can
     * be used to create a error which is the child of this node.
     *
     * @param  th  the throwable on which the data node will be based
     */
    public DataNode makeErrorChild( Throwable th ) {
        return getChildMaker().makeErrorDataNode( this, th );
    }

    /**
     * Constructs an error data node from a throwable with given parentage.
     *
     * @param  th  the throwable on which the data node will be based
     * @param  parent  the parent of the new error data node
     */
    public DataNode makeErrorChild( Throwable th, DataNode parent ) {
        return getChildMaker().makeErrorDataNode( parent, th );
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
