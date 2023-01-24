package uk.ac.starlink.ttools.build;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class containing API-independent helper functions for
 * writing doclets.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2023
 */
public class DocletUtil {

    private static final Map<String,String> TYPE_NAMES = createTypeNameMap();

    /**
     * Private sole constructor prevents instantiation.
     */
    private DocletUtil() {
    }

    /**
     * Returns a user-friendly type name for certain class names.
     * It works for most primitive types, wrapper types, and String.
     *
     * @param  fully qualified class name
     * @return   user-friendly type name, or null
     */
    public static String getScalarTypeName( String className ) {
        return TYPE_NAMES.get( className );
    }

    /**
     * Returns a map of some type names to user-friendly names.
     *
     * @return  unmodificable type name map
     */
    private static Map<String,String> createTypeNameMap() {
        Map<String,String> map = new HashMap<>();
        map.put( byte.class.getName(), "byte" );
        map.put( short.class.getName(), "short integer" );
        map.put( int.class.getName(), "integer" );
        map.put( long.class.getName(), "long integer" );
        map.put( float.class.getName(), "floating point" );
        map.put( double.class.getName(), "floating point" );
        map.put( Byte.class.getName(), "byte" );
        map.put( Short.class.getName(), "short integer" );
        map.put( Integer.class.getName(), "integer" );
        map.put( Long.class.getName(), "long integer" );
        map.put( Float.class.getName(), "floating point" );
        map.put( Double.class.getName(), "floating point" );
        map.put( String.class.getName(), "String" );
        return Collections.unmodifiableMap( map );
    }
}
