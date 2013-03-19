package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Control implementation which uses tabs and contains one or more
 * ConfigSpecifiers as the user interaction component.
 * There's nothing to stop you adding non-ConfigSpecifier tabs too.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class ConfigControl extends TabberControl implements Configger {

    private final List<Specifier<ConfigMap>> specifierList_;

    /**
     * Constructor.
     *
     * @param  label  control label
     * @param  icon   control icon
     */
    public ConfigControl( String label, Icon icon ) {
        super( label, icon );
        specifierList_ = new ArrayList<Specifier<ConfigMap>>();
    }

    /**
     * Adds a config specifier as one of the tabs.
     *
     * @param   name  tab label
     * @param  specifier  config specifier
     */
    protected void addSpecifierTab( String name,
                                    Specifier<ConfigMap> specifier ) {
        addControlTab( name, specifier.getComponent(), true );
        specifier.addActionListener( getActionForwarder() );
        specifierList_.add( specifier );
    }

    /**
     * Returns a single config map containing all of the config
     * information gathered by this control.
     * The returned map is the union of all the configs gathered from
     * the specifiers added by the {@link #addSpecifierTab addSpecifierTab}
     * method.
     *
     * @return  all configuration information gathered by this control
     */
    public ConfigMap getConfig() {
        ConfigMap config = new ConfigMap();
        for ( Specifier<ConfigMap> specifier : specifierList_ ) {
            config.putAll( specifier.getSpecifiedValue() );
        }
        return config;
    }

    /**
     * Utility method to assert that all of a given set of keys
     * are actually being obtained by this component.
     *
     * @param  requiredKeys   list of keys this control should obtain
     * @return  true iff the <code>getConfig</code> method contains entries
     *          for all the required keys
     * @throws  AssertionError if the result would be false and assertions
     *                         are enabled
     */
    public boolean assertHasKeys( ConfigKey[] requiredKeys ) {
        Set<ConfigKey> reqSet =
            new HashSet<ConfigKey>( Arrays.asList( requiredKeys ) );
        Set<ConfigKey<?>> gotSet = getConfig().keySet();
        reqSet.removeAll( gotSet );
        assert reqSet.isEmpty() : "Missing required keys " + reqSet;
        return reqSet.isEmpty();
    }
}
