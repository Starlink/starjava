package uk.ac.starlink.topcat;

/**
 * This class describes columns in a table of metadata.  Such columns
 * may be editable, have names, etc.
 */
public abstract class MetaColumn {

    private boolean isEditable;
    private String name;
    private Class clazz;

    public MetaColumn( String name, Class clazz, boolean isEditable ) {
        this.name = name;
        this.clazz = clazz;
        this.isEditable = isEditable;
    }

    abstract public Object getValue( int irow );

    public void setValue( int irow, Object value ) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return name;
    }

    public Class getContentClass() {
        return clazz;
    }

    public boolean isEditable() {
        return isEditable;
    }

}
