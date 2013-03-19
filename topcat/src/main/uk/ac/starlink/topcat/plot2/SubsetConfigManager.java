package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Maintains one configuration component each for a group of RowSubsets.
 * The configuration items are split into two types; normal ones and
 * ones that are centrally managed so that each time a new one is requested
 * it has a different default.  A typical usage is to allow a different
 * default colour to be associated with each subset.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class SubsetConfigManager {

    private final NextSupplier nextSupplier_;
    private final ConfigKey[] nextKeys_;
    private final ConfigKey[] otherKeys_;
    private final ConfigKey[] allKeys_;
    private final Map<RowSubset,SubsetConfigger> configgers_;
    private final ActionForwarder forwarder_;

    /**
     * Constructor.
     *
     * @param  nextSupplier  manages dispensing different objects of the
     *                       same type
     * @param  otherKeys   keys for config items not handled by the
     *                     nextSupplier that should be handled by this
     *                     managers configgers
     */
    public SubsetConfigManager( NextSupplier nextSupplier,
                                ConfigKey[] otherKeys ) {
        nextSupplier_ = nextSupplier;
        nextKeys_ = nextSupplier_.getKeys();
        otherKeys_ = otherKeys;
        allKeys_ = PlotUtil.arrayConcat( nextKeys_, otherKeys_ );
        configgers_ = new HashMap<RowSubset,SubsetConfigger>();
        forwarder_ = new ActionForwarder();
    }

    /**
     * Returns the config keys managed by this manager.
     */
    public ConfigKey[] getConfigKeys() {
        return allKeys_;
    }

    /**
     * Lazily constructs and returns a SubsetConfigger for a given subset.
     *
     * @param   subset   subset for which the configger is required
     * @return  configger
     */
    public Configger getConfigger( RowSubset subset ) {
        return getSubsetConfigger( subset );
    }

    /**
     * Lazily constructs and returns a SubsetConfigger for a given subset.
     *
     * @param   subset   subset for which the configger is required
     * @return  configger
     */
    private SubsetConfigger getSubsetConfigger( RowSubset subset ) {
        if ( ! configgers_.containsKey( subset ) ) {
            SubsetConfigger configger = new SubsetConfigger();
            configger.specifier_.addActionListener( forwarder_ );
            configgers_.put( subset, configger );
        }
        return configgers_.get( subset );
    }

    /**
     * Returns the GUI configuration component for a given row subset.
     *
     * @param  subset  row subset
     * @return  configuration component
     */
    public JComponent getConfiggerComponent( RowSubset subset ) {
        return getSubsetConfigger( subset ).getComponent();
    }

    /**
     * Adds a listener to be notified whenever the state of one of this
     * manager's configuration components changes.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a previously added listener.
     *
     * @param  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    /**
     * Configures state for a single subset.
     */
    private class SubsetConfigger implements Configger {
        private final ConfigSpecifier specifier_;
        private JComponent panel_;
        private boolean init_;

        /**
         * Constructor.
         */
        SubsetConfigger() {
            specifier_ = new ConfigSpecifier( allKeys_ );
        }

        /**
         * Returns the configuration.
         * This has the effect of pulling a value from the nextSupplier
         * for next-type keys if they have not already been assigned a value
         * in some other way.
         *
         * @return  configuration
         */
        public ConfigMap getConfig() {
            if ( ! init_ ) {
                init_ = true;
                for ( int ik = 0; ik < nextKeys_.length; ik++ ) {
                    initNextValue( (ConfigKey<?>) nextKeys_[ ik ] );
                }
            }
            return specifier_.getSpecifiedValue();
        }

        /**
         * Returns the user interaction component for this configger.
         *
         * @return  GUI component
         */
        public JComponent getComponent() {
            if ( panel_ == null ) {
                panel_ = specifier_.getComponent();
                ConfigMap map = new ConfigMap();
                for ( int ik = 0; ik < nextKeys_.length; ik++ ) {
                    ConfigKey<?> key = nextKeys_[ ik ];
                    map.put( key, null );
                }
                specifier_.setSpecifiedValue( map );
            }
            return panel_;
        }

        /**
         * Initialiases the value for a given key in the config specifier
         * by pulling it from the NextSupplier if it has not already
         * been assigned one explicitly by the user.
         *
         * @param  key  config key
         */
        private <T> void initNextValue( ConfigKey<T> key ) {
            Specifier<T> speccer = specifier_.getSpecifier( key );

            /* Check if the user has already interacted with the component
             * and explicitly set its value.  If so, don't auto-init it.
             * The check against null isn't exactly this test, but it
             * works OK for this purpose at the moment. */
            if ( speccer.getSpecifiedValue() == null ) {
                speccer.setSpecifiedValue( nextSupplier_.getNextValue( key ) );
            }
        }
    }
}
