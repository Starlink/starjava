package uk.ac.starlink.topcat.plot2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Extends ConfigSpecifier to decorate some of its component specifiers as
 * AutoSpecifiers.  This adds an "Auto" checkbox to each entry which can
 * be used to override the default settings.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 * @see   OptionalConfigSpecifier
 */
public class AutoConfigSpecifier extends ConfigSpecifier {

    private final Collection<ConfigKey<?>> autoKeys_;

    /**
     * Constructs a specifier with some of its keys decorated as AutoSpecifiers.
     *
     * @param  allKeys   all the keys for configuration
     * @param  autoKeys  subset of allKeys which should be presented as
     *                   AutoSpecifiers; any entries not contained in
     *                   allKeys are ignored
     */
    public AutoConfigSpecifier( ConfigKey<?>[] allKeys,
                                ConfigKey<?>[] autoKeys ) {
        super( allKeys, new AutoComponentGui( autoKeys ) );
        autoKeys_ = new HashSet<ConfigKey<?>>( Arrays.asList( autoKeys ) );
    }

    /**
     * Constructs a specifier with all of its keys decorated as AutoSpecifiers.
     *
     * @param  keys  config keys
     */
    public AutoConfigSpecifier( ConfigKey<?>[] keys ) {
        this( keys, keys );
    }

    /**
     * Returns the auto specifier associated with one of this object's keys.
     * If the key is not one that has an auto specifier, null is returned.
     *
     * @param   key  config key
     * @return  auto specifier for key, or null
     */
    public <T> AutoSpecifier<T> getAutoSpecifier( ConfigKey<T> key ) {
        return autoKeys_.contains( key )
             ? (AutoSpecifier<T>) super.getSpecifier( key )
             : null;
    }

    /**
     * ComponentGui implementation that decorates specifiers as
     * AutoSpecifiers if they correspond to one of the keys in a given list.
     * Otherwise falls back to default behaviour.
     */
    private static class AutoComponentGui implements ComponentGui {
        private final Collection<ConfigKey<?>> autoKeys_;

        /**
         * Constructor.
         *
         * @param  autoKeys  list of config keys which should be auto-ised
         */
        AutoComponentGui( ConfigKey<?>[] autoKeys ) {
            autoKeys_ = new HashSet<ConfigKey<?>>( Arrays.asList( autoKeys ) );
        }

        public <T> Specifier<T> createSpecifier( ConfigKey<T> key ) {
            Specifier<T> baseSpecifier = key.createSpecifier();
            return autoKeys_.contains( key )
                 ? new AutoSpecifier<T>( baseSpecifier )
                 : baseSpecifier;
        }
    }
}
