package uk.ac.starlink.treeview;

/**
 * Contains the state used to create a DataNode.
 * The node in question was created using the method 
 * {@link DataNodeFactory#makeDataNode} or similar.
 */
public class CreationState {
    private final DataNodeFactory fact;
    private final DataNodeBuilder builder;
    private final DataNode parent;
    private final Object obj;

    /**
     * Constructs a CreationState object with all the relevant information.
     */
    public CreationState( DataNodeFactory fact, DataNodeBuilder builder,
                          DataNode parent, Object obj ) {
        this.fact = fact;
        this.builder = builder;
        this.parent = parent;
        this.obj = obj;
    }

    /**
     * Constructs a CreationState object from only its parent. 
     * This will provide a limited amount of functionality - the alterego
     * menu won't work.
     */
    public CreationState( DataNode parent ) {
        this( null, null, parent, null );
    }
    public DataNodeBuilder getBuilder() {
        return builder;
    }
    public DataNodeFactory getFactory() {
        return fact;
    }
    public DataNode getParent() {
        return parent;
    }
    public Object getObject() {
        return obj;
    }
    public String toString() {
        return "Factory: " + fact + ";  " 
             + "Builder: " + builder + ";  "
             + "Parent: " + parent + "; "
             + "Object: " + obj;
    }
}
