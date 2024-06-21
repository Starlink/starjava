package uk.ac.starlink.datanode.nodes;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
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

    private String name;
    private String label;
    private String desc;
    private String nodetype = "Data node";
    private DataNodeFactory childMaker;
    private CreationState creator;
    private Object parentObject;
    private short iconID = IconFactory.NO_ICON;
    private Map dataMap = new HashMap();

    /**
     * Constructs a blank <code>DefaultDataNode</code>.
     */
    public DefaultDataNode() {
    }

    /**
     * Constructs a <code>DefaultDataNode</code> with a given name.
     *
     * @param  name  the name to use for this object.
     */
    public DefaultDataNode( String name ) {
        this();
        setName( name );
    }

    /**
     * The <code>DefaultDataNode</code> implementation of this method returns 
     * <code>false</code>.
     */
    public boolean allowsChildren() {
        return false;
    }

    /**
     * The <code>DefaultDataNode</code> implementation of this method throws
     * <code>UnsupportedOperationException</code> 
     * ({@link #allowsChildren} is false).
     */
    public Iterator getChildIterator() {
        throw new UnsupportedOperationException();
    }

    public Object getParentObject() {
        return parentObject;
    }

    public void setParentObject( Object parent ) {
        this.parentObject = parent;
    }

    public void setLabel( String label ) {
        this.label = label;
    }

    public String getLabel() {
        if ( label != null ) {
            return label;
        }
        else if ( name != null ) {
            return name;
        }
        else {
            return "<unnamed>";
        }
    }

    /**
     * Sets the name of this node.  Since the name of a node should not 
     * change over its lifetime (though a label can), this is only 
     * intended for use during construction by subclasses.
     *
     * @param  name  the node's name
     */
    protected void setName( String name ) {
        this.name = name;
        if ( label == null && name != null ) {
            setLabel( label = name );
        }
    }

    public String getName() {
        return name == null ? "..." : name;
    }

    /**
     * Sets the value which will be returned by {@link #getDescription}.
     *
     * @param  desc  the description string
     */
    public void setDescription( String desc ) {
        this.desc = desc;
    }

    public String getDescription() {
        return desc;
    }

    /**
     * The <code>DefaultDataNode</code> implementation returns the string "...".
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
        return NodeUtil.toString( this );
    }

    /**
     * This may be called by subclasses to set the icon returned by 
     * this node to one of the ones defined in the IconFactory class.
     *
     * @param   id  one of the icon identifiers defined as static
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

    /**
     * Returns a default separator string.
     *
     * @return "."
     */
    public String getPathSeparator() {
        return ".";
    }

    /**
     * The <code>DefaultDataNode</code> implementation 
     * returns the label as a default path element.
     *
     * @return  the node's label
     */
    public String getPathElement() {
        return getLabel();
    }

    /**
     * Provides a straightforward method of storing typed data objects
     * for this node, as accessed by the 
     * {@link #hasDataObject}/{@link #getDataObject}
     * methods.  You can invoke this method to register a data object
     * for a particular data type.  Note that this is only suitable if
     * the data object is free or cheap to come by - in the case
     * that its construction is expensive then the data object ought to
     * be constructed on demand by <code>getDataObject</code> rather than being
     * registered as a matter of course (since it may never be needed).
     *
     * @param   type  data type of object to register
     * @param   data  data object of type <code>type</code> for this node - 
     *          must be non-null and of class <code>type.getDataClass()</code>
     */
    public void registerDataObject( DataType type, Object data ) {
        if ( type != null ) {
            if ( type.getDataClass().isAssignableFrom( data.getClass() ) ) {
                dataMap.put( type, data );
            }
            else {
                throw new ClassCastException( "Data object is not a " + 
                                              type.getDataClass() );
            }
        }
    }

    /**
     * The <code>DefaultDataNode</code> implementation returns true for 
     * only those data objects which have been registered using 
     * {@link #registerDataObject}.
     */
    public boolean hasDataObject( DataType type ) {
        return dataMap.containsKey( type );
    }

    /**
     * The <code>DefaultDataNode</code> implementation returns any data object
     * which has been registered using {@link #registerDataObject}.
     */
    public Object getDataObject( DataType type ) throws DataObjectException {

        /* Make sure we check hasDataObject here, since it may be overridden. */
        if ( hasDataObject( type ) && dataMap.containsKey( type ) ) {
            return dataMap.get( type );
        }
        else {
            throw new IllegalArgumentException( "Type " + type + " not known" );
        }
    }

    /**
     * No custom configuration is performed.
     */
    public void configureDetail( DetailViewer dv ) {
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
     * This convenience method just calls 
     * <code>getChildMaker().makeChildNode(this,childObj)</code>.
     * In general, nodes should use this method to construct their
     * children.
     *
     * @param  childObj  the object which forms the basis for a child
     *         data node
     * @see    DataNodeFactory#makeDataNode
     */
    public DataNode makeChild( Object childObj ) {
        return getChildMaker().makeChildNode( this, childObj );
    }

    /**
     * Constructs an error data node from a throwable.  This method can
     * be used to create a error which is the child of this node.
     * This convenience method just calls 
     * <code>getChildMaker().makeErrorDataNode(this,th)</code>
     *
     * @param  th  the throwable on which the data node will be based
     * @see   DataNodeFactory#makeErrorDataNode
     */
    public DataNode makeErrorChild( Throwable th ) {
        return getChildMaker().makeErrorDataNode( this, th );
    }

    public void setCreator( CreationState state ) {
        this.creator = state;
    }

    public CreationState getCreator() {
        return creator;
    }

    /**
     * It beeps.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

}
