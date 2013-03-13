package uk.ac.starlink.ttools.plot2.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Map containing typed configuration keys.
 *
 * <p>Missing entries for a key are for most purposes treated as if
 * the key is present with its default value.  Null values may however
 * be placed explicitly into the map.
 *
 * <p>Null keys are not supported.
 *
 * <p>Concurrency requirements: this map will get written in one thread
 * (perhaps the Event Dispatch Thread), and after writing is complete
 * may be concurrently in the same and different threads.
 *
 * @author   Mark Taylor
 * @since    22 Feb 2013
 * @see    ConfigKey
 */
@Equality
public class ConfigMap {

    private final Map<ConfigKey<?>,Object> map_;
    private final static Object NULL_VALUE = new Object() {
        public String toString() {
            return "<null>";
        }
    };

    /**
     * Constructs an empty map.
     */
    public ConfigMap() {

        /* Use a concurrent hash map since access may be from different threads.
         * However, this disallows null keys and values, so we have to 
         * handle null values explicitly. */
        map_ = new ConcurrentHashMap<ConfigKey<?>,Object>( 8, 0.75f, 1 );
    }

    /**
     * Copy constructor.
     *
     * @param  copy  map to copy
     */
    public ConfigMap( ConfigMap copy ) {
        this();
        map_.putAll( copy.map_ );
    }

    /**
     * Puts an entry into this map.
     *
     * @param  key   key 
     * @param  value   value to associate with key
     */
    public <T> void put( ConfigKey<T> key, T value ) {
        map_.put( key, value == null ? NULL_VALUE : value );
    }

    /**
     * Copies all the entries from a given map into this map.
     *
     * @param   config   map to copy
     */
    public void putAll( ConfigMap config ) {
        map_.putAll( config.map_ );
    }

    /**
     * Reads the value associated with a given key.
     * If the key is not present in the map, the default value for that
     * key is returned.
     *
     * @param  key  key
     * @return  value earlier written to map, or default value
     */
    public <T> T get( ConfigKey<T> key ) {
        Object value = map_.get( key );

        /* If the key is not present, return the key's default value. */
        if ( value == null ) {
            return key.getDefaultValue();
        }

        /* If the value is the special null marker, return null.
         * This measure is necessary since the concurrent map implementation
         * does not support null values. */
        else if ( value == NULL_VALUE ) {
            return null;
        }

        /* Otherwise return the actual stored value. */
        else {
            return key.cast( value );
        }
    }

    /**
     * Returns a set view of the keys explicitly written into the map.
     * Note that non-null (default) values may be read from the map
     * even for keys absent from this set.
     *
     * @return  set of keys backed by map; supports removal operations
     * @see   java.util.Map#keySet
     */
    public Set<ConfigKey<?>> keySet() {
        return map_.keySet();
    }

    @Override
    public boolean equals( Object o ) {
        return o instanceof ConfigMap
            && this.map_.equals( ((ConfigMap) o).map_ );
    }

    @Override
    public int hashCode() {
        return map_.hashCode();
    }

    @Override
    public String toString() {
        return map_.toString();
    }
}
