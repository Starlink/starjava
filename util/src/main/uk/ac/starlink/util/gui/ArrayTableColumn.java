package uk.ac.starlink.util.gui;

/**
 * Provides the specification for a table column.
 * This is to be used in conjunction with {@link ArrayTableModel}.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2009
 */
public abstract class ArrayTableColumn<R,C> {

    private final String name_;
    private final Class<C> clazz_;

    /**
     * Constructor.
     *
     * @param   name   column name
     * @param   clazz  class which all objects returned by the
     *                 {@link #getValue} method will be instances of (or null)
     */
    public ArrayTableColumn( String name, Class<C> clazz ) {
        name_ = name;
        clazz_ = clazz;
    }

    /**
     * Returns the value in this column for the row represented by the
     * given row data object.
     *
     * @param  item  row data object
     * @return   cell value in this column
     */
    public abstract C getValue( R item );

    /**
     * Returns the name of this column.
     *
     * @return   column name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the class which all values of this column will belong to.
     *
     * @return   content class
     */
    public Class<C> getContentClass() {
        return clazz_;
    }
}
