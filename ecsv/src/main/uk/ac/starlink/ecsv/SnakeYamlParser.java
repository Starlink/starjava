package uk.ac.starlink.ecsv;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * YamlParser implementation based on the SnakeYAML library
 *
 * <p>The current implementation is based on SnakeYAML 1.25,
 * which in turn claims to understand YAML 1.1.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 * @see   <a href="https://bitbucket.org/asomov/snakeyaml/src/"
 *                >https://bitbucket.org/asomov/snakeyaml/src/</a>
 */
public class SnakeYamlParser implements YamlParser {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ecsv" );

    public EcsvMeta parseMeta( String[] lines ) throws EcsvFormatException {

        /* Get SnakeYAML to perform the parse. */
        StringBuffer sbuf = new StringBuffer();
        for ( String line : lines ) {
            sbuf.append( line )
                .append( '\n' );
        }
        final Object yaml;
        try {
            yaml = new Yaml().load( sbuf.toString() );
        }
        catch ( MarkedYAMLException e ) {
            throw new EcsvFormatException( "YAML parse error: "
                                         + e.getProblem(), e );
        }
        catch ( YAMLException e ) {
            throw new EcsvFormatException( "YAML parse error: "
                                         + e.getMessage(), e );
        }
        if ( yaml == null ) {
            throw new EcsvFormatException( "No YAML content" );
        }
        else if ( ! ( yaml instanceof Map ) ) {
            throw new EcsvFormatException( "Unexpected YAML content "
                                         + yaml.getClass().getName() );
        }
        Map<?,?> map = (Map) yaml;

        /* Extract the required information from the parsed structure
         * and turn it into an EcsvMeta object. */
        final char delimiter = getDelimiter( map.get( "delimiter" ) );
        final Map<?,?> tableMeta = getMeta( map.get( "meta" ) );
        final EcsvColumn<?>[] columns = getColumns( map.get( "datatype" ) );
        final String schema = getStringValue( map, "schema" );
        return new EcsvMeta() {
            public char getDelimiter() {
                return delimiter;
            }
            public EcsvColumn<?>[] getColumns() {
                return columns;
            }
            public Map<?,?> getTableMeta() {
                return tableMeta;
            }
            public String getSchema() {
                return schema;
            }
        };
    }

    /**
     * Converts a suitable object to an ECSV delimiter character.
     *
     * @param  delimObj  item that should contain delimiter
     * @return   best guess at delimiter character, hopefully comma or space
     */
    private char getDelimiter( Object delimObj ) {
        if ( delimObj instanceof String && ((String) delimObj).length() == 1 ) {
            return ((String) delimObj).charAt( 0 );
        }
        else if ( delimObj instanceof Character ) {
            return ((Character) delimObj).charValue();
        }
        if ( delimObj != null ) {
            logger_.warning( "Unexpected ECSV delimiter declaration"
                           + "\"" + delimObj + "\"" );
        }
        return ' ';
    }

    /**
     * Converts a suitable object to a generic table metadata map.
     *
     * @param   metaObj  item that should contain a metadata map
     * @param   metadata map, or null if absent or unsuitable
     */
    private Map<?,?> getMeta( Object metaObj ) {
        if ( metaObj == null ) {
            return null;
        }
        else if ( metaObj instanceof Map ) {
            return (Map<?,?>) metaObj;
        }
        else {
            logger_.warning( "Ignoring malformed table metadata of type "
                           + metaObj.getClass().getName() );
            return null;
        }
    }

    /**
     * Converts a suitable object to a list of column specifiers.
     *
     * @param   colsObj  item that should contain a list of datatype specifiers
     * @return   array of column metadata objects
     */
    private EcsvColumn<?>[] getColumns( Object colsObj )
            throws EcsvFormatException {
        if ( colsObj instanceof List ) {
            return getColumnsArray( ((List) colsObj).toArray() );
        }
        else if ( colsObj instanceof Object[] ) {
            return getColumnsArray( (Object[]) colsObj );
        }
        else if ( colsObj == null ) {
            throw new EcsvFormatException( "No datatype array in metadata" );
        }
        else {
            throw new EcsvFormatException( "No comprehensible datatype array "
                                         + "in metadata" );
        }
    }

    /**
     * Converts an array of suitable objects to a list of column specifiers.
     *
     * @param  colObjs  array of items that should be datatype specifiers
     * @return   array of column metadata objects
     */
    private EcsvColumn<?>[] getColumnsArray( Object[] colObjs )
            throws EcsvFormatException {
        int ncol = colObjs.length;
        EcsvColumn<?>[] cols = new EcsvColumn<?>[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            Object colObj = colObjs[ ic ];
            if ( colObj instanceof Map ) {
                cols[ ic ] = createColumn( (Map<?,?>) colObj );
            }
            else if ( colObj == null ) {
                throw new EcsvFormatException( "Null element "
                                             + "in column metadata array");
            }
            else {
                throw new EcsvFormatException( "Unexpected element type "
                                             + colObj.getClass().getName()
                                             + " in column metadata array" );
            }
        }
        return cols;
    }

    /**
     * Converts a suitable object to a column specifier.
     *
     * @param  colMap    map that should contain metadata specification
     * @return   corresponding column metadata object
     */
    private EcsvColumn<?> createColumn( Map<?,?> colMap )
            throws EcsvFormatException {
        final String name = getStringValue( colMap, "name" );
        String datatype = getStringValue( colMap, "datatype" );
        if ( name == null ) {
            throw new EcsvFormatException( "Column has no name" );
        }
        if ( datatype == null ) {
            throw new EcsvFormatException( "Column " + name
                                         + " has no datatype" );
        }
        final EcsvDecoder<?> decoder = EcsvDecoder.createDecoder( datatype );
        if ( decoder == null ) {
            throw new EcsvFormatException( "Unknown/unsupported datatype "
                                         + datatype );
        }
        return createColumn( name, decoder, colMap );
    }

    /**
     * Creates a typed column metadata object.
     *
     * @param  name  column name
     * @param  decoder   value decoder
     * @param  colMap   map containing all available parsed column metadata
     * @return   column metadata object
     */
    private static <T> EcsvColumn<T> createColumn( final String name,
                                                   final EcsvDecoder<T> decoder,
                                                   Map<?,?> colMap ) {
        final String unit = getStringValue( colMap, "unit" );
        final String format = getStringValue( colMap, "format" );
        final String description = getStringValue( colMap, "description" );
        final String datatype = getStringValue( colMap, "datatype" );
        Object mapObj = colMap.get( "meta" );
        final Map<?,?> meta = mapObj instanceof Map ? (Map<?,?>) mapObj : null;
        return new EcsvColumn<T>() {
            public String getName() {
                return name;
            }
            public EcsvDecoder<T> getDecoder() {
                return decoder;
            }
            public String getDatatype() {
                return datatype;
            }
            public String getUnit() {
                return unit;
            }
            public String getFormat() {
                return format;
            }
            public String getDescription() {
                return description;
            }
            public Map<?,?> getMeta() {
                return meta;
            }
        };
    }

    /**
     * Attempts to acquire a string value from a map.
     *
     * @param   map   map
     * @param  key  map key
     * @return  string value corresponding to <code>key</code>, or null
     */
    private static String getStringValue( Map<?,?> map, String key ) {
        Object valueObj = map.get( key );
        if ( valueObj instanceof String ) {
            return (String) valueObj;
        }
        else if ( valueObj == null ) {
            return null;
        }
        else {
            logger_.warning( "Non-string value for key \"" + key + "\"" );
            return null;
        }
    }
}
