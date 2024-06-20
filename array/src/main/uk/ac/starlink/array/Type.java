package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Primitive numeric data type identifier.  Objects in this class are used to
 * identify the type of the bulk data held in an {@link NDArray}.
 * <p>
 * This class exemplifies the <i>typesafe enum</i> pattern -- the only
 * possible instances are supplied as static final fields of the class, and
 * these instances are immutable.
 * 
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class Type {

    private static List allTypes = new ArrayList( 5 );

    /** Object representing primitive data of <code>byte</code> type. */
    public static final Type BYTE = new Type( "byte" );

    /** Object representing primitive data of <code>short</code> type. */
    public static final Type SHORT = new Type( "short" );

    /** Object representing primitive data of <code>int</code> type. */
    public static final Type INT = new Type( "int" );

    /** Object representing primitive data of <code>float</code> type. */
    public static final Type FLOAT = new Type( "float" );

    /** Object representing primitive data of <code>double</code> type. */
    public static final Type DOUBLE = new Type( "double" );

    private final String name;
    private final Class clazz;
    private final int nBytes;
    private final Number defaultBadValue;
    private final boolean isFloating;
    private final double minval;
    private final double maxval;

    /*
     * Private sole constructor.
     */
    private Type( String name ) {
        this.name = name;
        if ( name.equals( "byte" ) ) {
            clazz = byte.class;
            nBytes = 1;
            defaultBadValue = Byte.valueOf( (byte) 0x80 );
            isFloating = false;
            minval = (double) Byte.MIN_VALUE;
            maxval = (double) Byte.MAX_VALUE;
        }
        else if ( name.equals( "short" ) ) {
            clazz = short.class;
            nBytes = 2;
            defaultBadValue = Short.valueOf( (short) 0x8000 );
            isFloating = false;
            minval = (double) Short.MIN_VALUE;
            maxval = (double) Short.MAX_VALUE;
        }
        else if ( name.equals( "int" ) ) {
            clazz = int.class;
            nBytes = 4;
            defaultBadValue = Integer.valueOf( 0x80000000 );
            isFloating = false;
            minval = (double) Integer.MIN_VALUE;
            maxval = (double) Integer.MAX_VALUE;
        }
        else if ( name.equals( "float" ) ) {
            clazz = float.class;
            nBytes = 4;
            defaultBadValue = Float.valueOf( Float.NaN );
            isFloating = true;
            minval = (double) - Float.MAX_VALUE;
            maxval = (double) Float.MAX_VALUE;
        }
        else if ( name.equals( "double" ) ) {
            clazz = double.class;
            nBytes = 8;
            defaultBadValue = Double.valueOf( Double.NaN );
            isFloating = true;
            minval = - Double.MAX_VALUE;
            maxval = Double.MAX_VALUE;
        }
        else {
            assert false;
            throw new Error();
        }

        /* Append this object to the list of known types. */
        allTypes.add( this );
    }

    public String toString() {
        return name;
    }

    /**
     * Returns the number of bytes occupied by this primitive type.
     *
     * @return  the size of this type in bytes
     */
    public int getNumBytes() {
        return nBytes;
    }

    /**
     * Returns the java primitive class associated with this type.
     *
     * @return  the class corresponding to this object
     */
    public Class javaClass() {
        return clazz;
    }

    /**
     * Returns the default bad value used for this type.
     * This is never null; it is the <code>NaN</code> value for floating point 
     * types, and the lowest (negatative) value for the integer types.
     *
     * @return   the bad value used for this Type by default
     */
    public Number defaultBadValue() {
        return defaultBadValue;
    }

    /**
     * Returns a default bad value handler for this type.
     * For type BYTE, this is a null handler (no values considered bad),
     * and for the other types it is a handler using the value returned
     * by the {@link #defaultBadValue} method.
     *
     * @return   a BadHandler object implementing the default bad value 
     *           policy for this type
     */
    public BadHandler defaultBadHandler() {
        return ( this == BYTE ) 
                    ? BadHandler.getHandler( this, null )
                    : BadHandler.getHandler( this, defaultBadValue );
    }

    /**
     * Indicates whether this type represents floating point values.
     *
     * @return   true for floating point types, false for integral types
     */
    public boolean isFloating() {
        return isFloating;
    }

    /**
     * Returns the lowest (= most negative) value which 
     * can be represented by this type.
     *
     * @return  lowest value this type can represent, as a <code>double</code>
     */
    public double minimumValue() {
        return minval;
    }

    /**
     * Returns the highest value which can be represented by this type.
     *
     * @return  highest value this type can represetn, as a <code>double</code>
     */
    public double maximumValue() {
        return maxval;
    }

    /**
     * Constructs a new primitive array of a requested size and the
     * appropriate type for this object.  This utility method is useful
     * for array allocation in type-generic programming.
     * The current implementation just invokes
     * {@link java.lang.reflect.Array#newInstance}.
     *
     * @param  size  the number of elements required
     * @return    a new primitive array 
     */
    public Object newArray( int size ) {
        return Array.newInstance( clazz, size );
    }

    /**
     * Checks that a given Object is in fact an array of the primitive
     * type corresponding to this Type, and contains at least a given
     * number of elements; throws an exception if not.  
     * If the given Object satisfies these 
     * requirements no action is taken; if it does not then
     * an <code>IllegalArgumentException</code> is thrown.
     * This utility method is useful for parameter checking in 
     * type-generic programming.
     * 
     * @param   array   an Object purporting to be a java array of 
     *                  primitives of the right primitive type for this
     *                  Type and of at least minsize elements
     * @param   minsize the minimum satisfactory size of array
     * @throws  IllegalArgumentException  if array is not an array or
     *          has fewer than minSize elements
     */
    public void checkArray( Object array, int minsize ) {
        if ( array.getClass().getComponentType() != clazz ) {
            throw new IllegalArgumentException(
                "Object " + array + " is not of type " + clazz + "[]" );
        }
        else if ( Array.getLength( array ) < minsize ) {
            throw new IllegalArgumentException(
                "Primitive array too short: " + 
                Array.getLength( array ) + " < " + minsize );
        }
    }

    /**
     * Returns a list of all the known Types.
     *
     * @return  an unmodifiable List containing all the existing type objects.
     */
    public static List allTypes() {
        return Collections.unmodifiableList( allTypes );
    }

    /**
     * Returns the Type object corresponding to a given java class.
     * If no corresponding type exists (<code>cls</code> is not one of the
     * supported primitive numeric types) then <code>null</code> is returned.
     *
     * @param   cls  a (presumably numeric primitive) class.  
     *               May be <code>null</code>
     * @return  the Type object corresponding to <code>cls</code>,
     *          or <code>null</code>
     */
    public static Type getType( Class cls ) {
        if ( byte.class.equals( cls ) ) {
            return BYTE;
        }
        else if ( short.class.equals( cls ) ) {
            return SHORT;
        }
        else if ( int.class.equals( cls ) ) {
            return INT;
        }
        else if ( float.class.equals( cls ) ) {
            return FLOAT;
        }
        else if ( double.class.equals( cls ) ) {
            return DOUBLE;
        }
        else {
            return null;
        }
    }

}
