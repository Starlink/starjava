package uk.ac.starlink.ttools.plot2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map containing typed entries generated as a result of plotting.
 * The sequence in which entries are added is significant;
 * general interest entries should be added in the order in which
 * it makes sense for a UI to present them to the user.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2014
 */
public class ReportMap {

    private final Map<ReportKey<?>,Object> map_;
    private String[] summary_;

    /**
     * Constructs an empty map.
     */
    public ReportMap() {
        map_ = new LinkedHashMap<ReportKey<?>,Object>();
    }

    /**
     * Sets an entry.
     *
     * @param  key  key
     * @param  value   value
     */
    public <T> void set( ReportKey<T> key, T value ) {
        map_.put( key, value );
    }

    /**
     * Retrieves an entry.
     *
     * @param  key   key
     * @return   associated value; if no entry present, null is returned
     */
    public <T> T get( ReportKey<T> key ) {
        return key.getValueClass().cast( map_.get( key ) );
    }

    /**
     * Returns an ordered set of the keys actually present in this map.
     * The sequence is the order in which entries were set.
     *
     * @return   list of keys
     */
    public Set<ReportKey<?>> keySet() {
        return map_.keySet();
    }

    @Override
    public int hashCode() {
        int code = 77612;
        code = 23 * code + map_.hashCode();
        code = 23 * code + PlotUtil.hashCode( summary_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ReportMap ) {
            ReportMap other = (ReportMap) o;
            return this.map_.equals( other.map_ )
                && PlotUtil.equals( this.summary_, other.summary_ );
        }
        else {
            return false;
        }
    }
}
