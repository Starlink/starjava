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
    private final Class clazz;
    public CreationState( DataNodeFactory fact, Class clazz, Object obj ) {
        this.fact = fact;
        this.clazz = clazz;
        this.obj = obj;
    }
    public Class getObjectClass() {
        return clazz;
    }
    public DataNodeFactory getFactory() {
        return fact;
    }
    public Object getObject() {
        return obj;
    }
    public String toString() {
        return "Factory: " + fact + ";  " 
             + "Class: " + clazz + ";  "
             + "Object: " + obj;
    }
}
