package uk.ac.starlink.ttools.plot2.data;

/**
 * Enumerates the possibilities for internal storage of coordinate data
 * for presentation to the plotting classes.
 * In particular defines the type of the object returned by
 * {@link Coord#inputToStorage}.
 *
 * <p>At time of writing, this data is stored in memory, but in principle
 * they could be cached in some disk-based file.
 * For that reason, they should as far as possible be restricted to
 * objects that are easily serialized to a byte array
 * (primitives, ideally scalars or fixed-length arrays).
 *
 * @see   CachedColumnFactory
 * @see   Coord
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public enum StorageType {

    /** Boolean type.  Output type is {@link java.lang.Boolean}.  */
    BOOLEAN,

    /** Double precision type.  Output type is {@link java.lang.Double}. */
    DOUBLE,

    /** Single precision type.  Output type is {@link java.lang.Float}. */
    FLOAT,

    /** Integer type.  Output type is {@link java.lang.Integer}. */
    INT,

    /** Short integer type.  Output type is {@link java.lang.Short}. */
    SHORT,

    /** Byte type.  Output type is {@link java.lang.Byte}. */
    BYTE,

    /** String type.  Output type is {@link java.lang.String}. */
    STRING,

    /** Integer triple type.  Output type is <code>int[3]</code>. */
    INT3,

    /** Double precision triple type.  Output type is <code>double[3]</code>. */
    DOUBLE3,

    /** Single precision triple type.  Output type is <code>float[3]</code>. */
    FLOAT3,

    /** Double precision variable length array type.
     *  Output type is <code>double[]</code>. */
    DOUBLE_ARRAY,

    /** Single precision variable length array type.
     *  Output type is <code>float[]</code>. */
    FLOAT_ARRAY;
}
