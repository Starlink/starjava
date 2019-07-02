package uk.ac.starlink.ttools.plot2.config;

/**
 * Aggregates a set of config keys which are used together to specify
 * an object.
 *
 * @author   Mark Taylor
 * @since    6 Mar 2014
 */
public interface KeySet<T> {

    /**
     * The config keys used to specify an object.
     *
     * @return  fixed list of config keys
     */
    ConfigKey<?>[] getKeys();

    /**
     * Creates a typed value based on the values in a map corresponding
     * to this object's keys.
     *
     * @param   map  map for which the values corresponding to
     *           <code>getKeys</code> will be examined
     * @return  specified typed value
     */
    T createValue( ConfigMap map );
}
