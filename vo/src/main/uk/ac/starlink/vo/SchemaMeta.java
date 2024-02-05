package uk.ac.starlink.vo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents schema metadata from a TableSet document.
 * The scalar members are intended to be set by classes in this package
 * near construction time.
 * The tables member may or may not be populated, depending on the
 * source of the instance;
 * check the documentation for the relevant factory class.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2015
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class SchemaMeta {

    String name_;
    String title_;
    Integer index_;
    String description_;
    String utype_;
    Map<String,Object> extras_;
    private TableMeta[] tables_;

    /**
     * Constructor.
     */
    protected SchemaMeta() {
        extras_ = new LinkedHashMap<String,Object>();
    }

    /**
     * Returns this schema's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns this schema's human-readable title.
     * Note, this is supplied by VODataService, but not by TAP_SCHEMA.
     *
     * @return  human-readable title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns this schema's description.
     *
     * @return  text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns this schema's schema index.
     *
     * @return  schema index, or null if not available
     */
    public Integer getIndex() {
        return index_;
    }

    /**
     * Returns this schema's utype.
     *
     * @return  utype string
     */
    public String getUtype() {
        return utype_;
    }

    /**
     * Returns a map of additional non-standard metadata items for this schema.
     *
     * @return  extras map
     */
    public Map<String,Object> getExtras() {
        return extras_;
    }

    /**
     * Returns a list of the tables contained in this schema.
     * If the result is null, nothing is known about the tables,
     * and the list may need to be explicitly set.
     *
     * @return  tables contained in this schema, or null
     */
    public TableMeta[] getTables() {
        return tables_;
    }

    /**
     * Sets the tables contained in this schema.
     *
     * @param  tables  table list
     */
    public void setTables( TableMeta[] tables ) {
        tables_ = tables;
    }

    /**
     * Returns this schema's name.
     *
     * @return  name
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns a new schema with no tables and the given name.
     *
     * @param  name   name of new schema
     * @return  new empty schema
     */
    public static SchemaMeta createDummySchema( String name ) {
        SchemaMeta schema = new SchemaMeta();
        schema.name_ = name;
        return schema;
    }
}
