package uk.ac.starlink.array;

/**
 * Access mode identifier.  Objects in this class are used to
 * indicate what kind of access is required for a requested array object.
 *
 * This class exemplifies the <i>typesafe enum</i> pattern -- the only
 * possible instances are supplied as static final fields of the class, and
 * these instances are immutable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AccessMode {

    /** Object representing read-only access. */
    public static final AccessMode READ = new AccessMode( "read", 
                                                          true, false );

    /** Object representing update (read and write) access. */
    public static final AccessMode UPDATE = new AccessMode( "update", 
                                                            true, true );

    /** Object representing write-only access. */
    public static final AccessMode WRITE = new AccessMode( "write",
                                                           false, true );

    private final String name;
    private final boolean isReadable;
    private final boolean isWritable;

    private AccessMode( String name, boolean isReadable, boolean isWritable ) {
        this.name = name;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
    }

    /**
     * Indicates whether this mode includes read access.
     *
     * @return   true for READ and UPDATE, false for WRITE
     */
    public boolean isReadable() {
        return isReadable;
    }

    /**
     * Indicates whether this mode includes write access.
     *
     * @return   true for WRITE and UPDATE, false for READ
     */
    public boolean isWritable() {
        return isWritable;
    }

    public String toString() {
        return name;
    }
}
