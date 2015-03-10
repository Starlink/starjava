package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * ConfigSpecifier subclass that adds checkboxes for some of its
 * component specifiers, indicating whether those specifiers should
 * be used for the result or not.  Where they are not used, the relevant
 * GUI controls are disabled.
 *
 * <p>This is quite like {@link AutoConfigSpecifier}, but the override value is
 * not reflected in the GUI.  Perhaps the two classes should be combined?
 *
 * @author   Mark Taylor
 * @since    18 Mar 2013
 */
public class OptionalConfigSpecifier extends ConfigSpecifier {

    private final Collection<ConfigKey<?>> optKeys_;

    /**
     * Constructor.
     *
     * @param   allKeys  all of the keys
     * @param   optKeys  subset of allKeys which should be annotated with
     *                   activation checkboxes; any entries not contained
     *                   in allKeys are ignored
     * @param   optionText  text to annotate the checkboxes 
     */
    public OptionalConfigSpecifier( ConfigKey<?>[] allKeys,
                                    ConfigKey<?>[] optKeys,
                                    String optionText ) {
        super( allKeys, new OptComponentGui( optKeys, optionText ) );
        optKeys_ = new HashSet<ConfigKey<?>>( Arrays.asList( optKeys ) );
    }

    @Override
    public ConfigMap getSpecifiedValue() {
        ConfigMap config = super.getSpecifiedValue();
        for ( Iterator<ConfigKey<?>> it = config.keySet().iterator();
              it.hasNext(); ) {
            ConfigKey<?> key = it.next();
            if ( optKeys_.contains( key ) &&
                 ! ((OptSpecifier) getSpecifier( key )).isActive() ) {
                it.remove();
            }
        }
        return config;
    }

    /**
     * Configures this specifier with the current state of a supplied template.
     *
     * @param  template  specifier supplying required configuration
     */
    public void configureFrom( OptionalConfigSpecifier template ) {
        Set<ConfigKey<?>> allKeys =
            new HashSet<ConfigKey<?>>( Arrays.asList( getConfigKeys() ) );
        allKeys.retainAll( Arrays.asList( template.getConfigKeys() ) );
        Set<ConfigKey<?>> optKeys = new HashSet<ConfigKey<?>>( optKeys_ );
        optKeys.retainAll( template.optKeys_ );
        for ( ConfigKey<?> key : allKeys ) {
            copyConfig( template, key, optKeys.contains( key ) );
        }
    }

    /**
     * Copies the configuration for a given key from a template config
     * specifier to this one.
     *
     * @param   template  template specifier
     * @param   key   key whose value is to be updated
     * @param   isOpt  true iff the specifier is known to be an OptSpecifier
     *                 for both template and target
     */
    private <T> void copyConfig( OptionalConfigSpecifier template,
                                 ConfigKey<T> key, boolean isOpt ) {
        Specifier<T> targetKeySpecifier = getSpecifier( key );
        Specifier<T> templateKeySpecifier = template.getSpecifier( key );
        targetKeySpecifier
            .setSpecifiedValue( templateKeySpecifier.getSpecifiedValue() );
        if ( isOpt ) {
            ((OptSpecifier) targetKeySpecifier)
                .setActive( ((OptSpecifier) templateKeySpecifier).isActive() );
        }
    }

    /**
     * Specifier implementation with an associated override checkbox.
     */
    private static class OptSpecifier<T> extends SpecifierPanel<T> {

        final Specifier<T> baseSpecifier_;
        final JCheckBox checkBox_;

        /**
         * Constructor.
         *
         * @param   key  config key
         * @param   optText   text to annotate override button
         */
        OptSpecifier( ConfigKey<T> key, String optText ) {

            /* Xfill false is not necessarily the right choice here, but it
             * is for the specifiers that we are currently using this on. */
            super( false );
            baseSpecifier_ = key.createSpecifier();
            checkBox_ = new JCheckBox( optText, true );
        }

        protected JComponent createComponent() {
            updateStatus();
            final ActionListener forwarder = getActionForwarder();
            baseSpecifier_.addActionListener( forwarder );
            checkBox_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateStatus();
                    forwarder.actionPerformed( evt );
                }
            } );
            JComponent line = Box.createHorizontalBox();
            line.add( baseSpecifier_.getComponent() );
            line.add( checkBox_ );
            return line;
        }

        /**
         * Indicates whether this specifier is yielding its own value.
         * If false, the override value should be used.
         *
         * @return  true if this specifier's own value is to be used
         */
        public boolean isActive() {
            return ! checkBox_.isSelected();
        }

        /**
         * Sets whether this specifier will yield its own value,
         * as opposed to the override value.
         *
         * @param  isActive  true if this specifier's own value is to be used
         */
        public void setActive( boolean isActive ) {
            checkBox_.setSelected( isActive );
            updateStatus();
        }

        /**
         * Ensures the GUI is correctly reflecting the active state of
         * the OptSpecifier.
         */
        private void updateStatus() {
            baseSpecifier_.getComponent().setEnabled( isActive() );
        }

        public T getSpecifiedValue() {
            return baseSpecifier_.getSpecifiedValue();
        }

        public void setSpecifiedValue( T value ) {
            baseSpecifier_.setSpecifiedValue( value );
        }

        public void submitReport( ReportMap report ) {
            baseSpecifier_.submitReport( report );
        }
    }

    /**
     * ComponentGui implementation that decorates specifiers with
     * a checkbox if they correspond to one of the keys in a given list.
     * Otherwise falls back to default behaviour.
     */
    private static class OptComponentGui implements ComponentGui {
        final Collection optKeys_;
        final String optText_;

        /**
         * Constructor.
         *
         * @param   optKeys  list of config keys which should have override
         *                   checkboxes
         * @param   optText  string with which to label override checkboxes
         */
        OptComponentGui( ConfigKey<?>[] optKeys, String optText ) {
            optKeys_ = new HashSet<ConfigKey<?>>( Arrays.asList( optKeys ) );
            optText_ = optText;
        }

        public <T> Specifier<T> createSpecifier( ConfigKey<T> key ) {
            return optKeys_.contains( key )
                 ? new OptSpecifier<T>( key, optText_ )
                 : key.createSpecifier();
        }
    }
}
