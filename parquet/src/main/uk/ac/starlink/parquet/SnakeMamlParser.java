package uk.ac.starlink.parquet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import uk.ac.starlink.table.TableFormatException;

/**
 * MamlParser based on the SnakeYaml library.
 *
 * <p>In principle, this implementation is coded from the
 * MAML 1.0-1.2 format description at
 * <a href="https://github.com/asgr/MAML-Format">
 *          https://github.com/asgr/MAML-Format</a>.
 * In practice it was done partly by looking at example files in the
 * same place.
 *
 * <p>MAML support in STIL is currently on a best-efforts basis.
 * This implementation picks up some of the most useful metadata,
 * but does not make great efforts to do the best thing in all cases.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2026
 */
public class SnakeMamlParser implements MamlParser {

    public MamlMetadata parseMaml( String txt ) throws TableFormatException {
        final Object yaml;
        try {
            yaml = new Yaml().load( txt );
        }
        catch ( YAMLException e ) {
            throw new TableFormatException( "YAML parse error: "
                                          + e.getMessage(), e );
        }
        if ( yaml == null ) {
            throw new TableFormatException( "No YAML content" );
        }
        else if ( ! ( yaml instanceof Map ) ) {
            throw new TableFormatException( "Unexpected YAML content "
                                          + yaml.getClass().getName() );
        }
        Map<?,?> map = (Map) yaml;
        String name = getStringValue( map, "table" );
        Map<String,String> paramMap = new LinkedHashMap<>();
        for ( String key :
              new String[] {
                  "survey", "dataset", "table", "version", "date", "author",
                  "description", "license", "MAML_version",
              } ) {
            String sval = getStringValue( map, key );
            if ( sval != null ) {
                paramMap.put( key, sval );
            }
        }
        Map<String,MamlMetadata.Field> fieldMap = new LinkedHashMap<>();
        List<?> fieldsList = getTypedValue( map, "fields", List.class );
        if ( fieldsList != null ) {
            for ( Object fieldObj : fieldsList ) {
                if ( fieldObj instanceof Map ) {
                    Map<?,?> fmap = (Map) fieldObj;
                    String fname = getStringValue( fmap, "name" );
                    if ( fname != null ) {
                        String funit = getStringValue( fmap, "unit" );
                        String finfo = getStringValue( fmap, "info" );
                        Number colsizeObj =
                            getTypedValue( fmap, "col_size", Number.class );
                        int fcolsize = colsizeObj == null
                                     ? -1
                                     : colsizeObj.intValue();
                        Object ucdObj = fmap.get( "ucd" );
                        final String fucd;
                        if ( ucdObj instanceof String ) {
                            fucd = (String) ucdObj;
                        }
                        else if ( ucdObj instanceof List ) {
                            fucd = ((List<?>) ucdObj).stream()
                                  .filter( o -> o instanceof String )
                                  .map( o -> (String) o )
                                  .collect( Collectors.joining( ";" ) );
                        }
                        else {
                            fucd = null;
                        }
                        fieldMap.put( fname, new MamlMetadata.Field() {
                            public String getUnit() {
                                return funit;
                            }
                            public String getInfo() {
                                return finfo;
                            }
                            public String getUcd() {
                                return fucd == null || fucd.trim().length() == 0
                                     ? null
                                     : fucd;
                            }
                            public int getColSize() {
                                return fcolsize;
                            }
                        } );
                    }
                }
            }
        }
        return new MamlMetadata() {
            public String getName() {
                return name;
            }
            public Map<String,String> getParameters() {
                return paramMap;
            }
            public Map<String,Field> getFields() {
                return fieldMap;
            }
        };
    }

    /**
     * Extracts a value with a required type from a map.
     *
     * @param  map  map
     * @param  key  key
     * @param  clazz   required type of result
     * @return   value of key if it exists and is of the required type,
     *           otherwise null
     */
    private static <T> T getTypedValue( Map<?,?> map, String key,
                                        Class<T> clazz ) {
        Object value = map.get( key );
        return clazz.isInstance( value ) ? clazz.cast( value ) : null;
    }

    /**
     * Extracts a string-typed value from a map.
     *
     * @param  map  map
     * @param  key  key
     * @return   value of key if it exists and is a non-empty string,
     *           otherwise null
     */
    private static String getStringValue( Map<?,?> map, String key ) {
        String sval = getTypedValue( map, key, String.class );
        return sval == null || sval.trim().length() == 0 ? null : sval;
    }
}
