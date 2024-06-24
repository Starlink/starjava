package uk.ac.starlink.hds;

import uk.ac.starlink.array.Type;

/**
 * Enumeration of the numeric data types available within the HDS system.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HDSType {

    private final String name;
    private final Number badValue;
    private final Type javaType;

    private HDSType( String name, Type javaType, Number badValue ) {
        this.name = name;
        this.javaType = javaType;
        this.badValue = badValue;
    }

    /** Signed byte type. */
    public static HDSType _BYTE = new HDSType(
        "_BYTE",
        Type.BYTE,
        new Byte( (byte) 0x80 ) );

    /** Unsigned byte type. */
    public static HDSType _UBYTE = new HDSType(
        "_UBYTE",
        Type.BYTE,
        new Byte( (byte) 0xff ) );

    /** Signed word (2-byte) type. */
    public static HDSType _WORD = new HDSType(
        "_WORD",
        Type.SHORT,
        new Short( (short) 0x8000 ) );

    /** Unsigned word (2-byte) type. */
    public static HDSType _UWORD = new HDSType(
        "_UWORD",
        Type.INT,
        new Short( (short) 0xffff ) );

    /** Signed integer (4-byte) type. */
    public static HDSType _INTEGER = new HDSType(
        "_INTEGER",
        Type.INT,
        new Integer( 0x80000000 ) );

    /** Real (4-byte floating point) type. */
    public static HDSType _REAL = new HDSType(
        "_REAL",
        Type.FLOAT,
        new Float( Float.intBitsToFloat( 0xff7fffff ) ) );

    /** Double precision (8-byte floating point) type. */
    public static HDSType _DOUBLE = new HDSType(
        "_DOUBLE",
        Type.DOUBLE,
        new Double( Double.longBitsToDouble( 0xffefffffffffffffL ) ) );

    /**
     * Returns the name of this type.
     *
     * @return  type name (upper case, including prepended underscore)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the magic bad value used within the HDS/ARY/NDF system
     * for this data type.
     *
     * @return   bad value representation
     */
    public Number getBadValue() {
        return badValue;
    }

    /**
     * Returns the java type most naturally associated with this HDS type.
     *
     * @return   primitive data type representation
     */
    public Type getJavaType() {
        return javaType;
    }

    public String toString() {
        return name;
    }

    /**
     * Gets the HDSType object most naturally associated with a given 
     * java primitive numeric type.
     *
     * @param  javaType object representing a java primitive type
     * @return  an appropriate HDSType object
     */
    public static HDSType fromJavaType( Type javaType ) {
        if ( javaType == Type.BYTE ) {
            return _UBYTE;
        }
        else if ( javaType == Type.SHORT ) {
            return _WORD;
        }
        else if ( javaType == Type.INT ) {
            return _INTEGER;
        }
        else if ( javaType == Type.FLOAT ) {
            return _REAL;
        }
        else if ( javaType == Type.DOUBLE ) {
            return _DOUBLE;
        }
        else {
            throw new AssertionError();
        }
    }

    /**
     * Gets an HDSType from its HDS name.  This does the opposite of
     * the {@link #getName} method.  Returns <code>null</code> if 
     * <code>name</code> does not refer to a supported HDS numeric type;
     * note this will apply to _LOGICAL, _CHAR and structure types
     * (ones not starting with an underscore).
     *
     * @param   name  name of the HDS type, including prepended underscore.
     *                Not case sensitive.
     * @return  the numeric HDS type corresponding to <code>name</code>,
     *          or <code>null</code> if it is not one
     */
    public static HDSType fromName( String name ) {
        if ( name.equalsIgnoreCase( "_BYTE" ) ) {
            return _BYTE;
        }
        else if ( name.equalsIgnoreCase( "_UBYTE" ) ) {
            return _UBYTE;
        }
        else if ( name.equalsIgnoreCase( "_WORD" ) ) {
            return _WORD;
        }
        else if ( name.equalsIgnoreCase( "_UWORD" ) ) {
            return _UWORD;
        }
        else if ( name.equalsIgnoreCase( "_INTEGER" ) ) {
            return _INTEGER;
        }
        else if ( name.equalsIgnoreCase( "_REAL" ) ) {
            return _REAL;
        }
        else if ( name.equalsIgnoreCase( "_DOUBLE" ) ) {
            return _DOUBLE;
        }
        else {
            return null;
        }
    }

}
