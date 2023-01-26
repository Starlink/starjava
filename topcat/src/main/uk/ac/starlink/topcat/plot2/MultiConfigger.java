package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Configger implementation that aggregates config entries from a number
 * of subordinate configgers.
 * The subordinate configgers can be either global (applying to all zones)
 * or per-zone (applying to only a requested zone id).
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public class MultiConfigger {

    private final List<Configger> globalConfiggers_;
    private final Map<ZoneId,List<Configger>> zoneConfiggers_;

    /**
     * Constructor.
     */
    public MultiConfigger() {
        globalConfiggers_ = new ArrayList<Configger>();
        zoneConfiggers_ = new HashMap<ZoneId,List<Configger>>();
    }

    /**
     * Adds a global subordinate configger to the list.
     *
     * @param  configger   item whose config values will be gathered for the
     *                     result of this one
     */
    public void addGlobalConfigger( Configger configger ) {
        globalConfiggers_.add( configger );
    }

    /**
     * Adds a subordinate configger that contributes configuration
     * specific to a given zone.
     *
     * @param  zid  zone id
     * @param  configger   per-zone configger
     */
    public void addZoneConfigger( ZoneId zid, Configger configger ) {
        if ( ! zoneConfiggers_.containsKey( zid ) ) {
            zoneConfiggers_.put( zid, new ArrayList<Configger>() );
        }
        zoneConfiggers_.get( zid ).add( configger );
    }

    /**
     * Acquires that part of the configuration from subordinate configgers
     * that applies to all zones.
     *
     * @return  global config
     */
    public ConfigMap getGlobalConfig() {
        ConfigMap map = new ConfigMap();
        for ( Configger c : globalConfiggers_ ) {
            map.putAll( c.getConfig() );
        }
        return map;
    }

    /**
     * Acquires all the configuration from subordinate configgers that
     * applies to a given zone.  This includes the global config.
     *
     * @param  zid  zone of interest
     * @return   configuration for zone
     */
    public ConfigMap getZoneConfig( ZoneId zid ) {
        ConfigMap map = new ConfigMap();
        map.putAll( getGlobalConfig() );
        List<Configger> zlist = zoneConfiggers_.get( zid );
        if ( zlist != null ) {
            for ( Configger c : zlist ) {
                map.putAll( c.getConfig() );
            }
        }
        return map;
    }

    /**
     * Convenience method to return a dynamic configger whose configuration
     * returns depend on the state at getConfig-time of a given zone selector.
     *
     * @param  zsel   zone specifier
     * @return   configger
     */
    public Configger layerConfigger( final Specifier<ZoneId> zsel ) {
        return new Configger() {
            public ConfigMap getConfig() {
                assert SwingUtilities.isEventDispatchThread();
                return getZoneConfig( zsel.getSpecifiedValue() );
            }
        };
    }
}
