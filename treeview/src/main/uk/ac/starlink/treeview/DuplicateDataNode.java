package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Iterator;
import javax.swing.Icon;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.table.StarTable;

/**
 * DataNode object which acts as a clone of an existing node.
 * It copies its attributes from a given node, but to some extent 
 * maintains its own state.
 */
public class DuplicateDataNode implements DataNode,
                                          Draggable,
                                          TableNodeChooser.Choosable,
                                          NdxNodeChooser.Choosable {

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

    public boolean isStarTable() {
        return ( base instanceof TableNodeChooser.Choosable )
                    ?  ((TableNodeChooser.Choosable) base).isStarTable()
                    : false;
    }

    public StarTable getStarTable() throws IOException {
        return ((TableNodeChooser.Choosable) base).getStarTable();
    }

    public boolean isNdx() {
        return ( base instanceof NdxNodeChooser.Choosable )
                    ? ((NdxNodeChooser.Choosable) base).isNdx()
                    : false;
    }

    public NDShape getShape() {
        return ((NdxNodeChooser.Choosable) base).getShape();
    }

    public Ndx getNdx() throws IOException {
        return ((NdxNodeChooser.Choosable) base).getNdx();
    }

    public void customiseTransferable( DataNodeTransferable trans ) 
            throws IOException {
        if ( base instanceof Draggable ) {
            ((Draggable) base).customiseTransferable( trans );
        }
    }
}
