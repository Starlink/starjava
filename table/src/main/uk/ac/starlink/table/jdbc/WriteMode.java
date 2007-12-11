package uk.ac.starlink.table.jdbc;

/**
 * Defines how records are written to a database table.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2007
 */
public class WriteMode {

    private final String name_;
    private final boolean attemptDrop_;
    private final boolean create_;
    private final String description_;

    /**
     * WriteMode which creates a new database table before writing.
     * It is an error if a table of the same name already exists.
     */
    public static final WriteMode CREATE =
        new WriteMode( "create", false, true, new StringBuffer()
            .append( "Creates a new table before writing. " )
            .append( "It is an error if a table of the same name " )
            .append( "already exists." ).toString() );

    /**
     * WriteMode which creates a new database table before writing.
     * If a table of the same name already exists, it is dropped first.
     */
    public static final WriteMode DROP_CREATE =
        new WriteMode( "dropcreate", true, true, new StringBuffer()
            .append( "Creates a new database table before writing. " )
            .append( "If a table of the same name already exists, " )
            .append( "it is dropped first." )
            .toString() );

    /**
     * WriteMode which appends to an existing table.  An error results if
     * the named table has the wrong structure for the data being written.
     */
    public static final WriteMode APPEND =
        new WriteMode( "append", false, false, new StringBuffer()
            .append( "Appends to an existing table. " )
            .append( "An error results if the named table has the wrong " )
            .append( "structure (number or types of columns) " )
            .append( "for the data being written." )
            .toString() );

    /** Array of all known write modes. */
    private static final WriteMode[] ALL_MODES = new WriteMode[] {
        CREATE, DROP_CREATE, APPEND,
    };

    /**
     * Constructor.
     * 
     * @param   name  mode name
     * @param   attemptDrop  whether an attempt is made to drop the table
     *          before creating it (attempt may fail silently)
     * @param   create  whether the table must be created before writing
     *          (must succeed)
     * @param   description  textual description of mode operation
     */
    private WriteMode( String name, boolean attemptDrop, boolean create,
                       String description ) {
        name_ = name;
        attemptDrop_ = attemptDrop;
        create_ = create;
        description_ = description;
    }

    /**
     * Indicates whether an attempt should be made to drop the table
     * before creating it.
     * 
     * @return  attempt drop flag
     */
    boolean getAttemptDrop() {
        return attemptDrop_;
    }
        
    /**
     * Indicates whether the table should be created before writing.
     * 
     * @return  create flag
     */
    boolean getCreate() {
        return create_; 
    }

    /**
     * Returns a short description of this mode's operation.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns this mode's name.
     */
    public String toString() {
        return name_;
    }

    /**
     * Returns an array of all known write modes.
     *
     * @return  write mode array
     */
    public static WriteMode[] getAllModes() {
        return (WriteMode[]) ALL_MODES.clone();
    }
}
