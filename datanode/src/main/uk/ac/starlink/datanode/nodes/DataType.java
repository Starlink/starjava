package uk.ac.starlink.datanode.nodes;

import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;

/**
 * Enumeration-like class describing types of data object which can be 
 * supplied by a DataNode.  This is intended to cover the sort of 
 * object - like a table or array - which can have useful things done
 * to it by external applications.  It's not designed to enumerate a
 * fine-grained list of all possible objects which might relate to a 
 * node.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Jan 2005
 */
public class DataType {

    private final String name_;
    private final Class clazz_;

    /**
     * DataType representing a {@link uk.ac.starlink.table.StarTable} object.
     */
    public static final DataType TABLE = 
        new DataType( "TABLE", StarTable.class );

    /**
     * DataType representing a {@link uk.ac.starlink.ndx.Ndx} object.
     */
    public static final DataType NDX =
        new DataType( "NDX", Ndx.class );

    /**
     * DataType representing a {@link uk.ac.starlink.util.DataSource} object.
     */
    public static final DataType DATA_SOURCE =
        new DataType( "DATA_SOURCE", DataSource.class );

    /**
     * Constructs a new DataType.
     *
     * @param  name  type name
     * @param  clazz   class of data object corresponding to this type
     */
    protected DataType( String name, Class clazz ) {
        name_ = name;
        clazz_ = clazz;
    }

    /**
     * Returns the name of this type.
     * 
     * @return   type name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the class of data object corresponding to this type.
     *
     * @return   data object class
     */
    public Class getDataClass() {
        return clazz_;
    }

    public String toString() {
        return getName();
    }
}
