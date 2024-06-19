package uk.ac.starlink.topcat;

import uk.ac.starlink.table.ColumnInfo;

/**
 * This class describes columns in a table of metadata.  Such columns
 * may be editable, have names, etc.
 *
 * @see  MetaColumnTableModel
 */
public abstract class MetaColumn {

    private final ColumnInfo info_;

    /**
     * Constructs a new MetaColumn with a given name and content class.
     *
     * @param  name  the name of the column
     * @param  clazz  the Class of which every entry in this column will
     *         be a member
     */
    public MetaColumn( String name, Class<?> clazz ) {
        this( name, clazz, null );
    }

    /**
     * Constructs a new MetaColumn with a given name, content class and
     * description.
     *
     * @param  name  the name of the column
     * @param  clazz  the Class of which every entry in this column will
     *         be a member
     * @param  description  short textual description of column
     */
    public MetaColumn( String name, Class<?> clazz, String description ) {
        this( new ColumnInfo( name.replaceAll( " ", "_" ),
                              clazz, description ) );
    }

    /**
     * Constructs a new MetaColumn with a given metadata object.
     *
     * @param   info  column metadata
     */
    public MetaColumn( ColumnInfo info ) {
        info_ = info;
    }

    /**
     * Returns the entry at a given row in this column.
     *
     * @param  irow  the row for which this column is being queried
     */
    abstract public Object getValue( int irow );

    /**
     * Indicates whether the entry at a given row in this column can
     * be edited (whether {@link #setValue} may be called on <code>irow</code>).
     * This class's implementation returns <code>false</code>, but it may
     * be overridden by subclasses which permit cell modification.
     *
     * @param  irow  the row for which this column is being queried
     * @return  whether the entry at <code>irow</code> may be written to
     */
    public boolean isEditable( int irow ) {
        return false;
    }

    /**
     * Sets the value of the entry in this column at a given row.
     * This should only be called if {@link #isEditable}(irow) returns true.
     *
     * @param   irow  the row whose value in this column is to be set
     * @param   value  the new value of the entry at <code>irow</code>
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
        return info_.getName().replaceAll( "_", " " );
    }

    /**
     * Returns the class of which all entries in this column are members.
     *
     * @return  content class
     */
    public Class<?> getContentClass() {
        return info_.getContentClass();
    }

    /**
     * Returns the metadata object describing this column.
     *
     * @return  column metadata
     */
    public ColumnInfo getInfo() {
        return info_;
    }
}
