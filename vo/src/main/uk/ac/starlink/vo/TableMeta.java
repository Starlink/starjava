package uk.ac.starlink.vo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents table metadata from a TableSet document.
 * The scalar members are intended to be set by classes in this package
 * near construction time.
 * The columns and foreignKeys members may or may not be populated,
 * depending on the source of the instance;
 * check the documentation for the relevant factory class.
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
    Integer index_;
    String description_;
    String utype_;
    String nrows_;
    Map<String,Object> extras_;
    private ColumnMeta[] columns_;
    private ForeignMeta[] foreignKeys_;

    /**
     * Constructor.
     */
    protected TableMeta() {
        extras_ = new LinkedHashMap<String,Object>();
    }

    /**
     * Returns this table's type.
     * TAP 1.0 TAP_SCHEMA.tables says this should be one of "table" or "view";
     * VODataService allows "output", "base_table", "view" or other values.
     *
     * @return  table type label
     */
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

    /**
     * Returns this table's human-readable title.
     * Note, this is supplied by VODataService, but not by TAP_SCHEMA.
     *
     * @return  human-readable title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns this table's description.
     *
     * @return  text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns this table's table index.
     *
     * @return table index, or null if not available
     */
    public Integer getIndex() {
        return index_;
    }

    /**
     * Returns the (approximate?) row count declared for this table.
     *
     * @return  string indicating row count; may or may not be strictly numeric
     */
    public String getNrows() {
        return nrows_;
    }

    /**
     * Returns this table's Utype.
     *
     * @return  utype string
     */
    public String getUtype() {
        return utype_;
    }

    /**
     * Returns a map of additional non-standard metadata items for this table.
     *
     * @return  extras map
     */
    public Map<String,Object> getExtras() {
        return extras_;
    }

    /**
     * Returns a list of the columns contained in this table.
     * If the result is null, nothing is known about the columns,
     * and the list may need to be explicitly set.
     *
     * @return  columns contained in this table, or null
     */
    public ColumnMeta[] getColumns() {
        return columns_;
    }

    /**
     * Sets the columns contained in this table.
     *
     * @param  columns  column list
     */
    public void setColumns( ColumnMeta[] columns ) {
        columns_ = columns;
    }

    /**
     * Returns a list of the foreign keys associated with this table.
     * If the result is null, nothing is known about the foreign keys,
     * and the list may need to be explicitly set.
     *
     * @return  foreign keys associated with this table, or null
     */
    public ForeignMeta[] getForeignKeys() {
        return foreignKeys_;
    }

    /**
     * Sets the foreign keys associated with this table.
     *
     * @param  foreignKeys  foreign key list
     */
    public void setForeignKeys( ForeignMeta[] foreignKeys ) {
        foreignKeys_ = foreignKeys;
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
