package uk.ac.starlink.datanode.factory;

import uk.ac.starlink.datanode.nodes.DataNode;

/**
 * Contains the state used to create a DataNode.
 * The node in question was created using the method 
 * {@link DataNodeFactory#makeDataNode} or similar.
 */
public class CreationState {

    private final DataNode parent;
    private final Object obj;
    private DataNodeFactory fact;
    private DataNodeBuilder builder;
    private String trace;

    /**
     * Constructs a CreationState object.
     */
    public CreationState( DataNode parent, Object obj ) {
        this.parent = parent;
        this.obj = obj;
    }

    public DataNode getParent() {
        return parent;
    }

    public Object getObject() {
        return obj;
    }

    public void setBuilder( DataNodeBuilder builder ) {
        this.builder = builder;
    }
    public DataNodeBuilder getBuilder() {
        return builder;
    }

    public void setFactory( DataNodeFactory fact ) { 
        this.fact = fact;
    }
    public DataNodeFactory getFactory() {
        return fact;
    }

    public void setFactoryTrace( String trace ) {
        this.trace = trace;
    }
    public String getFactoryTrace() {
        return trace;
    }

    public String toString() {
        return "Factory: " + fact + ";  " 
             + "Builder: " + builder + ";  "
             + "Parent: " + parent + "; "
             + "Object: " + obj;
    }
}
