package uk.ac.starlink.topcat;

/**
 * This class describes columns in a table of metadata.  Such columns
 * may be editable, have names, etc.
 *
 * @see  MetaColumnTableModel
 */
public abstract class MetaColumn {

    private String name;
    private Class clazz;

    /**
     * Constructs a new MetaColumn with a given name and content class.
     *
     * @param  name  the name of the column
     * @param  clazz  the Class of which every entry in this column will
     *         be a member
     */
    public MetaColumn( String name, Class clazz ) {
        this.name = name;
        this.clazz = clazz;
    }

    /**
     * Returns the entry at a given row in this column.
     *
     * @param  irow  the row for which this column is being queried
     */
    abstract public Object getValue( int irow );

    /**
     * Indicates whether the entry at a given row in this column can
     * be edited (whether {@link #setValue} may be called on <tt>irow</tt>).
     * This class's implementation returns <tt>false</tt>, but it may
     * be overridden by subclasses which permit cell modification.
     *
     * @param  irow  the row for which this column is being queried
     * @return  whether the entry at <tt>irow</tt> may be written to
     */
    public boolean isEditable( int irow ) {
        return false;
    }

    /**
     * Sets the value of the entry in this column at a given row.
     * This should only be called if {@link #isEditable}(irow) returns true.
     *
     * @param   irow  the row whose value in this column is to be set
     * @param   value  the new value of the entry at <tt>irow</tt>
     */
    public void setValue( int irow, Object value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the name of this column.
     *
     * @return  name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the class of which all entries in this column are members.
     *
     * @return  content class
     */
    public Class getContentClass() {
        return clazz;
    }

}
