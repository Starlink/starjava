package uk.ac.starlink.vo;

/**
 * Represents schema metadata from a TableSet document.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2015
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class SchemaMeta {

    String name_;
    String title_;
    String description_;
    String utype_;
    TableMeta[] tables_;

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

    public TableMeta[] getTables() {
        return tables_;
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
}
