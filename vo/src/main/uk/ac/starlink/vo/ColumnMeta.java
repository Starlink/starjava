package uk.ac.starlink.vo;

import java.util.Arrays;

/**
 * Represents column metadata from a TableSet document.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2011
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class ColumnMeta {
    String name_;
    String description_;
    String unit_;
    String ucd_;
    String utype_;
    String dataType_;  // has attributes, but content is a token
    String[] flags_;

    /**
     * Returns this column's name.
     * This is a string suitable for unadorned insertion into an ADQL query,
     * so syntactically it must match ADQL's <code>&lt;column_name&gt;</code>,
     * hence <code>&lt;identifier&gt;</code> production
     * (a <code>&lt;regular_identifier&gt;</code> without quotes
     * or a <code>&lt;delimited_identifer&gt;</code> including quotes).
     * It should not be quoted or otherwise adjusted for use in an ADQL query.
     *
     * @return  name suitable for use in ADQL
     */
    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public String getUnit() {
        return unit_;
    }

    public String getUcd() {
        return ucd_;
    }

    public String getUtype() {
        return utype_;
    }

    public String getDataType() {
        return dataType_;
    }

    public String[] getFlags() {
        return flags_;
    }

    /**
     * Indicates whether this column is declared indexed.
     *
     * @return  true iff one of the flag values is "indexed"
     */
    public boolean isIndexed() {
        return hasFlag( "indexed" );
    }

    /**
     * Indicates whether this column is declared primary.
     *
     * @return  true iff one of the flag values is "primary"
     */
    public boolean isPrimary() {
        return hasFlag( "primary" );
    }

    /**
     * Indicates whether this column is declared nullable.
     *
     * @return  true iff one of the flag values is "nullable"
     */
    public boolean isNullable() {
        return hasFlag( "nullable" );
    }

    /**
     * Convenience function to find out if a given flag value is present.
     *
     * @param  flagTxt  flag value to query
     * @return   true iff one of the flag values is equal to
     *           <code>flagTxt</code>
     */
    public boolean hasFlag( String flagTxt ) {
        return flags_ != null && Arrays.asList( flags_ ).contains( flagTxt );
    }

    /**
     * Returns this column's name.
     *
     * @return  name
     */
    @Override
    public String toString() {
        return getName();
    }
}
