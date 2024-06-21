package uk.ac.starlink.table;

import java.util.BitSet;

/**
 * A column which provides data storage in java arrays.
 * This abstract class has separate implementations for primitive and
 * object arrays.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ArrayColumn extends ColumnData {

    private final Object data;

    /**
     * Constructs a new column based on an existing <code>ColumnInfo</code>
     * object and with a given number of rows.
     *
     * @param   base   the template <code>ColumnInfo</code>
     * @param   data   the array used to hold the data
     */
    ArrayColumn( final ColumnInfo base, Object data ) {
        super( new ColumnInfo( base ) {
            public void setContentClass( Class<?> clazz ) {
                if ( ! clazz.equals( base.getContentClass() ) ) {
                    throw new IllegalArgumentException( "Can't change class" );
                }
                super.setContentClass( clazz );
            }
        } );
        this.data = data;
    }

    /**
     * Returns true, since this class can store cell values.
     *
     * @return  true
     */
    public boolean isWritable() {
        return true;
    }

    public void storeValue( long lrow, Object val ) {
        storeValue( (int) lrow, val );
    }

    public Object readValue( long lrow ) {
        return readValue( (int) lrow );
    }

    abstract void storeValue( int irow, Object val );
    abstract Object readValue( int irow );

    /**
     * Returns the array object which holds the array data for this
     * column.
     *
     * @return   data array
     */
    public Object getArray() {
        return data;
    }

    /**
     * Ensures that this column's info is compatible with the given class.
     * If this column's info has a null class, it will be set from the
     * given one.  In case of incompatibility a runtime exception will
     * be thrown.
     *
     * @param  clazz  the class to check for compatibility
     * @throws  IllegalArgumentException  if not compatible
     */
    void checkContentClass( Class<?> clazz ) {
        ColumnInfo colinfo = getColumnInfo();
        if ( colinfo.getContentClass() == null ) {
             colinfo.setContentClass( clazz );
        }
        else if ( ! colinfo.getContentClass().isAssignableFrom( clazz ) ) {
            throw new IllegalArgumentException( 
                "Incompatible content class in column info: " +
                colinfo.getContentClass() + " not assignable from " + clazz );
        }
    }

    /**
     * Obtains an <code>ArrayColumn</code> object based on a template 
     * object with a given number of rows.  A new ColumnInfo object
     * will be constructed based on the given one.  It will return a 
     * PrimitiveArrayColumn if <code>info</code> describes a primitive type.
     *
     * @param   base  the template <code>ColumnInfo</code> - note this is
     *          not the actual ColumnInfo object which will be returned
     *          by the <code>getColumnInfo</code> method of the returned 
     *          <code>ArrayColumn</code>
     * @param   rowCount  the number of rows it is to hold
     * @return  a new <code>ArrayColumn</code> based on <code>base</code> with
     *          storage for <code>rowCount</code> elements
     */
    public static ArrayColumn makeColumn( ColumnInfo base, long rowCount ) {

        /* Validate the number of rows. */
        if ( rowCount > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Too many rows requested: " 
                                              + rowCount
                                              + " > Integer.MAX_VALUE" );
        }
        int nrow = (int) rowCount;
        assert (long) nrow == rowCount;

        /* Get the base class. */
        Class<?> clazz = base.getContentClass();

        /* Bail out if there isn't one. */
        if ( clazz == null ) {
            throw new IllegalArgumentException( "No class defined" );
        }

        /* Return a primitive column if possible. */
        else if ( clazz == Boolean.class ||
                  clazz == Character.class ||
                  clazz == Byte.class ||
                  clazz == Short.class ||
                  clazz == Integer.class ||
                  clazz == Long.class ||
                  clazz == Float.class ||
                  clazz == Double.class ) {
            return PrimitiveArrayColumn.makePrimitiveColumn( base, nrow );
        }
 
        /* Otherwise an object one. */
        else {
            return new ObjectArrayColumn( base, new Object[ nrow ] );
        }
    }

    /**
     * Constructs a new ArrayColumn based on a given data array.
     * The <code>contentClass</code> of the given base column info must
     * be compatible with the supplied data array; in the case of
     * a primitive <code>data</code> array it should be that of the 
     * corresponding wrapper class, and for non-primitive classes
     * it should be the class of what the array is an array of.
     * Alternatively, the <code>base</code> column info may have a 
     * <code>null</code> content class, in which case the column info for
     * the new column will be set appropriately from the data array.
     *
     * @param  base  the column info on which to base this column's info
     * @param  data  an array of primitives or objects which will form
     *         the storage for this column
     * @throws  IllegalArgumentException if <code>data</code> isn't an array or 
     *          <code>base.getContentClass()</code> is incompatible with 
     *          <code>data</code>
     */
    public static ArrayColumn makeColumn( ColumnInfo base, Object data ) {

        /* Find out what it's an array of. */
        Class<?> eclazz = data.getClass().getComponentType();

        /* Bail out if it's not an array. */
        if ( eclazz == null ) {
            throw new IllegalArgumentException( "Data object is not an array "
                                              + " (it's a " + data.getClass() );
        }

        /* Return a primitive or object array column. */
        if ( eclazz.isPrimitive() ) {
            return PrimitiveArrayColumn.makePrimitiveColumn( base, data );
        }
        else {
            return new ObjectArrayColumn( base, (Object[]) data );
        }
    }

    /**
     * Constructs a new ArrayColumn based on a given data array.
     * This convenience method results in a column with no metadata other
     * than the column name and its data type.  The data type is inferred
     * from the type of the array object supplied.
     *
     * @param  name  the name of the new column
     * @param  data  an array of primitives or objects which will form
     *         the storage for this column
     */
    public static ArrayColumn makeColumn( String name, Object data ) {
        Class<?> contentClass = data.getClass().getComponentType();
        if ( contentClass.isPrimitive() ) {
            if ( contentClass == byte.class ) {
                contentClass = Byte.class;
            }
            else if ( contentClass == short.class ) {
                contentClass = Short.class;
            }
            else if ( contentClass == int.class ) {
                contentClass = Integer.class;
            }
            else if ( contentClass == long.class ) {
                contentClass = Long.class;
            }
            else if ( contentClass == float.class ) {
                contentClass = Float.class;
            }
            else if ( contentClass == double.class ) {
                contentClass = Double.class;
            }
            else if ( contentClass == char.class ) {
                contentClass = Character.class;
            }
            else if ( contentClass == boolean.class ) {
                contentClass = Boolean.class;
            }
            else {
                throw new AssertionError( "Unknonwn primitive type?? " +
                                          contentClass );
            }
        }
        return makeColumn( new ColumnInfo( name, contentClass, null ), data );
    }
}
