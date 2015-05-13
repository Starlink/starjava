package uk.ac.starlink.vo;

/**
 * Represents table metadata from a TableSet document.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2011
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class TableMeta {
    String type_;
    String name_;
    String title_;
    String description_;
    String utype_;
    ColumnMeta[] columns_;
    ForeignMeta[] foreignKeys_;

    public String getType() {
        return type_;
    }

    /**
     * Returns this table's name.
     * This is a string suitable for unadorned insertion into an ADQL query,
     * so syntactically it must match ADQL's <code>&lt;table_name&gt;</code>
     * production.
     * It should not be quoted, or have a schema name prepended,
     * or be otherwise adjusted, for use in an ADQL query.
     *
     * @return  name suitable for use in ADQL
     */
    public String getName() {
        return name_;
    }

    public String getTitle() {
        return title_;
    }

    public String getDescription() {
        return description_;
    }

    public String getUtype() {
        return utype_;
    }

    public ColumnMeta[] getColumns() {
        return columns_;
    }

    public ForeignMeta[] getForeignKeys() {
        return foreignKeys_;
    }

    /**
     * Returns this table's name.
     *
     * @return  name
     */
    @Override
    public String toString() {
        return getName();
    }
}
