package uk.ac.starlink.treeview;

import java.util.Iterator;
import javax.swing.Icon;

/**
 * DataNode object which acts as a clone of an existing node.
 * It copies its attributes from a given node, but to some extent 
 * maintains its own state.
 */
public class DuplicateDataNode implements DataNode {

    private DataNode base;

    /**
     * Initialises this node from the state of a given base node.
     *
     * @param  base  the base data node
     */
    public DuplicateDataNode( DataNode base ) {
        this.base = base;
    }

    public boolean allowsChildren() {
        return base.allowsChildren();
    }

    public void setLabel( String label ) {
        base.setLabel( label );
    }

    public Iterator getChildIterator() {
        return base.getChildIterator();
    }

    public Object getParentObject() {
        return base.getParentObject();
    }

    public void setParentObject( Object parent ) {
        base.setParentObject( parent );
    }

    public String getLabel() {
        return base.getLabel();
    }

    public String getName() {
        return base.getName();
    }

    public String getNodeTLA() {
        return base.getNodeTLA();
    }

    public String getNodeType() {
        return base.getNodeType();
    }

    public String getDescription() {
        return base.getDescription();
    }

    public Icon getIcon() {
        return base.getIcon();
    }

    public String getPathElement() {
        return base.getPathElement();
    }

    public String getPathSeparator() {
        return base.getPathSeparator();
    }

    public void configureDetail( DetailViewer dv ) {
        base.configureDetail( dv );
    }

    public void setChildMaker( DataNodeFactory fact ) {
        base.setChildMaker( fact );
    }

    public DataNodeFactory getChildMaker() {
        return base.getChildMaker();
    }

    public void setCreator( CreationState state ) {
        base.setCreator( state );
    }

    public CreationState getCreator() {
        return base.getCreator();
    }

    public String toString() {
        return base.toString();
    }
}
