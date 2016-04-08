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
     * Constructs a map with the same content as a given template.
     *
     * @param  copy  map whose contents are to be copied
     */
    public ReportMap( ReportMap copy ) {
        this();
        map_.putAll( copy.map_ );
    }

    /**
     * Sets an entry.
     *
     * @param  key  key
     * @param  value   value
     */
    public <T> void put( ReportKey<T> key, T value ) {
        map_.put( key, value );
    }

    /**
     * Copies all the entries from a given map into this map.
     *
     * @param   report   map to copy
     */
    public void putAll( ReportMap report ) {
        map_.putAll( report.map_ );
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

    /**
     * Returns a string representation of this map, with an option
     * to include or exclude the non-general-interest entries.
     *
     * @param  includeAll  true to include all entries,
     *                     false to include only general-interest entries
     * @return   string representation of this map,
     *           zero-length if there are no items of interest
     */
    public String toString( boolean includeAll ) {
        StringBuffer sbuf = new StringBuffer();
        for ( ReportKey<?> key : keySet() ) {
            if ( includeAll || key.isGeneralInterest() ) {
                if ( sbuf.length() > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( key.getMeta().getShortName() )
                    .append( "=" )
                    .append( (Object) get( key ) );
            }
        }
        return sbuf.toString();
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

    @Override
    public String toString() {
        return toString( false );
    }
}
