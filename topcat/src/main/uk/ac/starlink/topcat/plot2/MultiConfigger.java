package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Configger implementation that aggregates config entries from a number
 * of subordinate configgers.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public class MultiConfigger implements Configger {

    private final List<Configger> configgerList_;

    /**
     * Constructs a MultiConfigger with an initial list.
     *
     * @param  configgers   inital list of subordinate configgers
     */
    public MultiConfigger( Configger[] configgers ) {
        configgerList_ =
            new ArrayList<Configger>( Arrays.asList( configgers ) );
    }

    /**
     * Constructs an empty MultiConfigger.
     */
    public MultiConfigger() {
        this( new Configger[ 0 ] );
    }

    /**
     * Adds a subordinate configger to the list.
     *
     * @param  configger   item whose config values will be gathered for the
     *                     result of this one
     */
    public void addConfigger( Configger configger ) {
        configgerList_.add( configger );
    }

    public ConfigMap getConfig() {
        ConfigMap map = new ConfigMap();
        for ( Configger c : configgerList_ ) {
            map.putAll( c.getConfig() );
        }
        return map;
    }
}
