package uk.ac.starlink.vo.datalink;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Defines the mapping of columns named by the DataLink standard
 * to a given table.
 *
 * <p>The usual way to obtain an instance of this class is using the
 * static {@link #getMap getMap} method.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2017
 * @see   <a href="http://www.ivoa.net/documents/DataLink/"
 *           >DataLink-1.0, sec 3.2</a>
 */
public class LinkColMap {

    private final Map<ColDef,Integer> icolMap_;

    /** ID column definition. */
    public static final ColDef<String> COL_ID;

    /** access_url column definition. */
    public static final ColDef<String> COL_ACCESSURL;

    /** service_def column definition. */
    public static final ColDef<String> COL_SERVICEDEF;

    /** error_message column definition. */
    public static final ColDef<String> COL_ERRORMESSAGE;

    /** description column definition. */
    public static final ColDef<String> COL_DESCRIPTION;

    /** semantics column definition. */
    public static final ColDef<String> COL_SEMANTICS;

    /** content_type column definition. */
    public static final ColDef<String> COL_CONTENTTYPE;

    /** content_length column definition. */
    public static final ColDef<Number> COL_CONTENTLENGTH;

    /** Map by column name of all columns required in a DataLink table. */
    public static final Map<String,ColDef> COLDEF_MAP;
    static {
        Map<String,ColDef> map = new LinkedHashMap<String,ColDef>();
        ColDef[] coldefs = {
            COL_ID =
                new ColDef<String>( "ID", "meta.id;meta.main", String.class ),
            COL_ACCESSURL =
                new ColDef<String>( "access_url", "meta.ref.url",
                                    String.class ),
            COL_SERVICEDEF =
                new ColDef<String>( "service_def", "meta.ref", String.class ),
            COL_ERRORMESSAGE =
                new ColDef<String>( "error_message", "meta.code.error",
                                    String.class ),
            COL_DESCRIPTION =
                new ColDef<String>( "description", "meta.note", String.class ),
            COL_SEMANTICS =
                new ColDef<String>( "semantics", "meta.code", String.class ),
            COL_CONTENTTYPE =
                new ColDef<String>( "content_type", "meta.code.mime",
                                    String.class ),
            COL_CONTENTLENGTH =
                new ColDef<Number>( "content_length", "phys.size;meta.file",
                                    Number.class ),
        };
        for ( ColDef coldef : coldefs ) {
            map.put( coldef.getName(), coldef );
        }
        COLDEF_MAP = Collections.unmodifiableMap( map );
    }

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo.datalink" );

    /**
     * Constructor.
     *
     * @param  icolMap  map from column definition to column index,
     *                  providing the state of this object
     */
    protected LinkColMap( Map<ColDef,Integer> icolMap ) {
        icolMap_ = icolMap;
    }

    /**
     * Returns the value of the DataLink id column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>id</code> column
     */
    public String getId( Object[] row ) {
        return getValue( COL_ID, row );
    }

    /**
     * Returns the value of the DataLink access_url column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>access_url</code> column
     */
    public String getAccessUrl( Object[] row ) {
        return getValue( COL_ACCESSURL, row );
    }

    /**
     * Returns the value of the DataLink service_def column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>service_def</code> column
     */
    public String getServiceDef( Object[] row ) {
        return getValue( COL_SERVICEDEF, row );
    }

    /**
     * Returns the value of the DataLink error_message column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>error_message</code> column
     */
    public String getErrorMessage( Object[] row ) {
        return getValue( COL_ERRORMESSAGE, row );
    }

    /**
     * Returns the value of the DataLink description column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>description</code> column
     */
    public String getDescription( Object[] row ) {
        return getValue( COL_DESCRIPTION, row );
    }

    /**
     * Returns the value of the DataLink semantics column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>semantics</code> column
     */
    public String getSemantics( Object[] row ) {
        return getValue( COL_SEMANTICS, row );
    }

    /**
     * Returns the value of the DataLink content_type column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>content_type</code> column
     */
    public String getContentType( Object[] row ) {
        return getValue( COL_CONTENTTYPE, row );
    }

    /**
     * Returns the value of the DataLink content_length column in a given row.
     *
     * @param  row  row from the table for which this map was prepared
     * @return   cell value for the <code>content_length</code> column,
     *           may be null
     */
    public Long getContentLength( Object[] row ) {
        Number cleng = getValue( COL_CONTENTLENGTH, row );
        if ( cleng == null ) {
            return null;
        }
        else if ( cleng instanceof Long ) {
            return (Long) cleng;
        }
        else if ( Double.isNaN( cleng.doubleValue() ) ) {
            return null;
        }
        else {
            return new Long( cleng.longValue() );
        }
    }

    /**
     * Returns the typed corresponding to a given column definition
     * in a given row.
     *
     * @param  col  column value extractor object
     * @param  row  row from the table for which this map was prepared
     * @return   typed cell value for <code>col</code>
     */
    public <C> C getValue( ColDef<C> col, Object[] row ) {
        Integer icol = icolMap_.get( col );
        if ( icol == null ) {
            return null;
        }
        else {
            C value = col.clazz_.cast( row[ icol.intValue() ] );
            return Tables.isBlank( value ) ? null : value;
        }
    }

    /**
     * Constructs a LinkColMap that knows where the DataLink columns are
     * in a supplied table.
     * Columns are identified by name and content type.
     * Incorrect UCDs etc lead to warnings emitted through the logging system.
     * No check is made that all columns are present;
     * attempts to retrieve column values for unidentified columns just
     * return null.
     *
     * @param  table   table (assumed DataLink) to interpret
     * @return   column map object
     */
    public static LinkColMap getMap( StarTable table ) {
        if ( table == null ) {
            return new LinkColMap( new HashMap<ColDef,Integer>() );
        }
        int ncol = table.getColumnCount();
        Map<ColDef,Integer> icolMap = new LinkedHashMap<ColDef,Integer>();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo info = table.getColumnInfo( ic );
            String name = info.getName();
            ColDef coldef = COLDEF_MAP.get( name );
            if ( coldef != null ) {
                String ucd = info.getUCD();
                Class<?> clazz = info.getContentClass();
                boolean isCorrectUcd = ucd != null
                                   && coldef.getUcd().equals( coldef.getUcd() );
                boolean isCorrectClazz = ((Class<?>) coldef.getContentClass())
                                        .isAssignableFrom( clazz );
                boolean hasValue = icolMap.containsKey( coldef );
                if ( hasValue ) {
                    logger_.warning( "Duplicate column \"" + name + "\""
                                   + " in DataLink table" );
                }
                else if ( ! isCorrectClazz ) {
                    logger_.warning( "Wrong type for DataLink column " + name
                                   + " (" + clazz.getSimpleName() + ")" );
                }
                else {
                    icolMap.put( coldef, new Integer( ic ) );
                    if ( ! isCorrectUcd ) {
                        if ( ucd == null ) {
                            logger_.warning( "Missing UCD for DataLink column "
                                           + name );
                        }
                        else {
                            logger_.warning( "Wrong UCD for DataLink column "
                                           + name + ": " + ucd + " != "
                                           + coldef.getUcd() );
                        }
                    }
                }
            }
        }
        return new LinkColMap( icolMap );
    }

    /**
     * Utility class that encapsulates the characteristics of a given
     * column from the DataLink standard.
     */
    public static class ColDef<C> {
        private final String name_;
        private final String ucd_;
        private final Class<C> clazz_;

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  ucd   column UCD
         * @param  clazz   required content class
         */
        private ColDef( String name, String ucd, Class<C> clazz ) {
            name_ = name;
            ucd_ = ucd;
            clazz_ = clazz;
        }

        /**
         * Returns the column's name.
         *
         * @return name
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns the column's UCD.
         *
         * @return  ucd
         */
        public String getUcd() {
            return ucd_;
        }

        /**
         * Returns the required content class for the column.
         *
         * @return  class
         */
        public Class<C> getContentClass() {
            return clazz_;
        }
    }
}
