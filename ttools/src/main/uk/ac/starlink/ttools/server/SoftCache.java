package uk.ac.starlink.ttools.server;

import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map implementation that stores values using
 * {@link java.lang.ref.SoftReference} objects.
 * This is therefore supposed to act like a memory-limited LRU cache.
 * However, it's at the mercy of the JVM SoftReference implementation,
 * so it's not guaranteed that it's going to do a good job of it.
 *
 * <p>Note: I read a comment somewhere that the Sun/Oracle JVM will
 * drop these cache entries less aggressively if it's running in
 * <code>-server</code> rather than <code>-client</code> mode.
 *
 * @author   Mark Taylor
 * @since    14 Jan 2020
 */
public class SoftCache<K,V> extends AbstractMap<K,V> {

    private final Map<K,SoftReference<V>> map_;
    private final Set<Map.Entry<K,V>> entrySet_;

    /**
     * Constructor.
     */
    public SoftCache() {
        map_ = new ConcurrentHashMap<K,SoftReference<V>>();
        entrySet_ = new AbstractSet<Map.Entry<K,V>>() {
            public int size() {
                return map_.size();
            }
            public void clear() {
                map_.clear();
            }
            public Iterator<Entry<K,V>> iterator() {
                final Iterator<Entry<K,SoftReference<V>>> baseIt =
                    map_.entrySet().iterator();
                return new Iterator<Entry<K,V>>() {
                    public boolean hasNext() {
                        return baseIt.hasNext();
                    }
                    public Map.Entry<K,V> next() {
                        final Entry<K,SoftReference<V>> baseEntry =
                            baseIt.next();
                        return new Entry<K,V>() {
                            public K getKey() {
                                return baseEntry.getKey();
                            }
                            public V getValue() {
                                return baseEntry.getValue().get();
                            }
                            public V setValue( V value ) {
                                SoftReference<V> ref =
                                    baseEntry
                                   .setValue( new SoftReference<V>( value ) );
                                return ref == null ? null : ref.get();
                            }
                        };
                    }
                    public void remove() {
                        baseIt.remove();
                    }
                };
            }
        };
    }

    /**
     * Deletes any entries that have been garbage collected.
     */
    public void purge() {
        for ( Iterator<Entry<K,SoftReference<V>>> it =
                  map_.entrySet().iterator();
              it.hasNext(); ) {
            if ( it.next().getValue().get() == null ) {
                it.remove();
            }
        }
    }

    public Set<Entry<K,V>> entrySet() {
        return entrySet_;
    }

    @Override
    public boolean containsKey( Object key ) {
        return map_.containsKey( key );
    }

    @Override
    public V get( Object key ) {
        SoftReference<V> ref = map_.get( key );
        if ( ref != null ) {
            V value = ref.get();
            if ( value == null ) {
                map_.remove( key );
            }
            return value;
        }
        else {
            return null;
        }
    }

    @Override
    public V put( K key, V value ) {
        SoftReference<V> ref = map_.put( key, new SoftReference<V>( value ) );
        return ref == null ? null : ref.get();
    }

    @Override
    public V remove( Object key ) {
        SoftReference<V> ref = map_.remove( key );
        return ref == null ? null : ref.get();
    }
}
