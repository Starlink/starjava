package uk.ac.starlink.treeview;

/**
 * Contains the state used to create a DataNode.
 * Three items of information are held: the DataNodeFactory fact,
 * and the creation object's class and reference clazz and obj.
 * The node in question was created using the call
 * <pre>
 *    fact.makeDataNode( clazz, obj );
 * </pre>
 */
class CreationState {
    private final DataNodeFactory fact;
    private final Object obj;
    private final DataNodeBuilder builder;
    public CreationState( DataNodeFactory fact, DataNodeBuilder builder,
                          Object obj ) {
        this.fact = fact;
        this.builder = builder;
        this.obj = obj;
    }
    public DataNodeBuilder getBuilder() {
        return builder;
    }
    public DataNodeFactory getFactory() {
        return fact;
    }
    public Object getObject() {
        return obj;
    }
    public String toString() {
        return "Factory: " + fact + ";  " 
             + "Builder: " + builder + ";  "
             + "Object: " + obj;
    }
}
