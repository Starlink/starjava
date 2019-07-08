package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;

/**
 * Manages centrally-dispensed value sets for one or more ConfigKeys.
 * Normally, a ConfigKey has a single default.
 * However, in some cases (subset colours) it is desirable for the
 * default to be one of a list, so that the first time the default is
 * acquired it has one value, the second time it has a different value, etc.
 * This object manages groups of default values to achieve that.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class NextSupplier {

    private final Map<ConfigKey<?>,KVals<?>> kvalMap_;

    /**
     * Constructor.
     */
    public NextSupplier() {
        kvalMap_ = new HashMap<ConfigKey<?>,KVals<?>>();
    }

    /**
     * Adds a key to be managed by this supplier, and provides a set of
     * values to be used for it.  The nextValues will be taken from this
     * list cyclically.
     *
     * @param  key  key to be managed by this supplier
     * @param  values  list of distinct values to provide successive defaults
     *                 for <code>key</code>
     */
    public <T> void putValues( ConfigKey<T> key, T[] values ) {
        kvalMap_.put( key, new KVals<T>( values ) );
    }

    /**
     * Returns the keys managed by this supplier.
     *
     * @return   managed key list
     */
    public ConfigKey<?>[] getKeys() {
        return kvalMap_.keySet().toArray( new ConfigKey<?>[ 0 ] );
    }

    /**
     * Returns the next unused value to use for a given key.
     * The value dispensed cycles through the items provided when the
     * key was initialised.
     *
     * @param  key  one of the keys managed by this supplier
     * @return   next value for key
     */
    public <T> T getNextValue( ConfigKey<T> key ) {
        @SuppressWarnings("unchecked")
        KVals<T> kv = (KVals<T>) kvalMap_.get( key );
        return kv == null ? null : kv.nextValue();
    }

    /** 
     * Associates a list of possible values with a config key.
     */
    private static class KVals<T> {
        final T[] values_;
        private int iseq_;

        /**
         * Constructor.
         *
         * @param  values   sequence of values to be dispensed
         */
        KVals( T[] values ) {
            values_ = values;
        }

        /**
         * Returns the next value in the sequence.
         *
         * @return  next value
         */
        T nextValue() {
            return values_[ iseq_++ % values_.length ];
        }
    }
}
