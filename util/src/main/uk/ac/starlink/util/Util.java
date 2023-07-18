package uk.ac.starlink.util;

import java.util.Map;
import java.util.Objects;

/**
 * General class containing utility methods.
 * Several of these relate to the java.util classes.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2023
 */
public class Util {

    /**
     * Private sole constructor prevents instantiation.
     */
    private Util() {
    }

    /**
     * Typed map get operation.
     * This simply calls {@link java.util.Map#get(Object)},
     * but it provides compile-time assurance that the supplied key
     * has the right type.
     *
     * @param  map  map
     * @param  key  key
     * @return   result of map.get(key)
     */
    public static <K,V> V get( Map<K,V> map, K key ) {
        return map.get( key );
    }

    /**
     * Typed map key test operation.
     * This simply calls {@link java.util.Map#containsKey(Object)},
     * but it provides compile-time assurance that the supplied key
     * has the right type.
     *
     * @param  map  map
     * @param  key  key
     * @return   result of map.containsKey(key)
     */
    public static <K,V> boolean containsKey( Map<K,V> map, K key ) {
        return map.containsKey( key );
    }

    /**
     * Typed map remove operation.
     * This simply calls {@link java.util.Map#remove(Object)},
     * but it provides compile-time assurance that the supplied key
     * has the right type.
     *
     * @param  map  map
     * @param  key  key
     * @return  result of map.remove(key)
     */
    public static <K,V> V remove( Map<K,V> map, K key ) {
        return map.remove( key );
    }

    /**
     * Typed equality operation.
     * This simply calls {@link Objects#equals(Object,Object)},
     * but provides compile-time assurance that the two supplied parameters
     * have the same type.
     *
     * @param  t1  first object
     * @param  t2  second object
     * @return  result of Objects.equals(t1, t1)
     */
    public static <T> boolean equals( T t1, T t2 ) {
        return Objects.equals( t1, t2 );
    }
}
