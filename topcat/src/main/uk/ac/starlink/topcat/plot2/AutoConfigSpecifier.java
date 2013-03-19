package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Extends ConfigSpecifier to decorate each compoent specifier as an
 * AutoSpecifier.  This adds an "Auto" checkbox to each entry which can
 * be used to override the default settings.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class AutoConfigSpecifier extends ConfigSpecifier {

    private static final ComponentGui AUTO_GUI = new ComponentGui() {
        public <T> Specifier<T> createSpecifier( ConfigKey<T> key ) {
            return new AutoSpecifier<T>( key.createSpecifier() );
        }
    };

    /**
     * Constructor.
     *
     * @param  keys  config keys
     */
    public AutoConfigSpecifier( ConfigKey[] keys ) {
        super( keys, AUTO_GUI );
    }

    /**
     * Returns the auto specifier associated with one of this object's keys.
     *
     * @param   key  config key
     * @return  auto specifier for key
     */
    public <T> AutoSpecifier<T> getAutoSpecifier( ConfigKey<T> key ) {
        return (AutoSpecifier<T>) super.getSpecifier( key );
    }
}
