package uk.ac.starlink.vo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents column metadata from a TableSet document.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2011
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 * @see  <a href="http://www.ivoa.net/Documents/TAP/"
 *          >IVOA TAP Recommendation</a>
 */
public class ColumnMeta {

    String name_;
    String description_;
    String unit_;
    String ucd_;
    String utype_;
    String dataType_;
    String arraysize_;
    String xtype_;
    String[] flags_;
    Map<String,Object> extras_;

    /**
     * Constructor.
     */
    protected ColumnMeta() {
        extras_ = new LinkedHashMap<String,Object>();
    }

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

    /**
     * Returns this column's description.
     *
     * @return  text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns this column's unit string.
     *
     * @return  unit
     */
    public String getUnit() {
        return unit_;
    }

    /**
     * Returns a UCD associated with this column.
     *
     * @return  ucd
     */
    public String getUcd() {
        return ucd_;
    }

    /**
     * Returns a Utype associated with this column.
     *
     * @return  utype
     */
    public String getUtype() {
        return utype_;
    }

    /**
     * Returns the datatype for this column.
     * This may be an ADQL data type (TAP 1.0 sec 2.5) or a vs:TAPType or
     * vs:VOTableType (VODataService 1.1 sec 3.5.3).
     *
     * @return  datatype
     */
    public String getDataType() {
        return dataType_;
    }

    /**
     * Array size, normally in the form of a VOTable arraysize attribute.
     *
     * @return   arraysize string
     */
    public String getArraysize() {
        return arraysize_;
    }

    /**
     * Extended type, normally as for a VOTable xtype attribute.
     *
     * @return  xtype
     */
    public String getXtype() {
        return xtype_;
    }

    /**
     * Returns a list of strings corresponding to flags that are set on
     * this column.
     * This list is in principle open (according to VODataService)
     * but VODataService mentions the values "indexed", "primary", "nullable",
     * while TAP_SCHEMA.columns defines "principal", "indexed" and "std".
     *
     * @return   array of flag strings set
     */
    public String[] getFlags() {
        return flags_;
    }

    /**
     * Indicates whether this column is declared indexed.
     * Available from both TAP_SCHEMA and VODataService metadata.
     *
     * @return  true iff one of the flag values is "indexed"
     */
    public boolean isIndexed() {
        return hasFlag( "indexed" );
    }

    /**
     * Indicates whether this column is declared primary.
     * Only available from VODataService metadata.
     *
     * @return  true iff one of the flag values is "primary"
     */
    public boolean isPrimary() {
        return hasFlag( "primary" );
    }

    /**
     * Indicates whether this column is declared nullable.
     * Only available from VODataService metadata.
     *
     * @return  true iff one of the flag values is "nullable"
     */
    public boolean isNullable() {
        return hasFlag( "nullable" );
    }

    /**
     * Indicates whether this column is declared principal.
     * Only available from TAP_SCHEMA metadata.
     *
     * @return  true iff one of the flag values is "principal"
     */
    public boolean isPrincipal() {
        return hasFlag( "principal" );
    }

    /**
     * Indicates whether this column is declared standard.
     * Only available from TAP_SCHEMA metadata.
     *
     * @return  true iff one of the flag values is "std"
     */
    public boolean isStd() {
        return hasFlag( "std" );
    }

    /**
     * Returns a map of additional non-standard metadata items for this column.
     *
     * @return  extras map
     */
    public Map<String,Object> getExtras() {
        return extras_;
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
