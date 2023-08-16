package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Generates Ganger instances for a particular purpose,
 * for instance a particular type of plot,
 * taking account of supplied user preferences.
 *
 * <p>You can find a basic single-zone implementation in
 * {@link SingleGangerFactory}.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2016
 */
public interface GangerFactory<P,A> {

    /**
     * Returns the configuration keys that can be used to configure
     * the gangers produced by this factory.
     *
     * @return  ganger configuration keys
     */
    ConfigKey<?>[] getGangerKeys();

    /**
     * Indicates whether this ganger factory has zones that can be created
     * according to the details of the plot and controlled independently
     * by user request.
     *
     * @return   true iff zones can be manipulated explicitly by the user
     */
    boolean hasIndependentZones();

    /**
     * Creates a ganger.
     *
     * @param   padding  required padding around plot area
     * @param   config   configuration map that may contain keys from
     *                   getGangerKeys
     * @param   context  additional context required for ganger creation
     * @return  new ganger
     */
    Ganger<P,A> createGanger( Padding padding, ConfigMap config,
                              GangContext context );
}
