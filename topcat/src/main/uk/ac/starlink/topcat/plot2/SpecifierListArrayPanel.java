package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Displays and manages a variable-length array of Specifiers for each of a
 * given list of ConfigKeys.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2023
 */
public class SpecifierListArrayPanel {

    private final List<KSpec<?>> specList_;
    private final IntFunction<String> labelFunc_;
    private final JComponent panel_;
    private final ActionForwarder actionForwarder_;
    private int count_;

    /**
     * Constructor.
     *
     * @param  keys   defines what specifiers to show for each index
     * @param  labelFunc  provides a generic label (applied to all keys)
     *                    for each index
     */
    public SpecifierListArrayPanel( ConfigKey<?>[] keys,
                                    IntFunction<String> labelFunc ) {
        specList_ = Arrays.stream( keys )
                          .map( KSpec::new )
                          .collect( Collectors.toList() );
        labelFunc_ = labelFunc;
        actionForwarder_ = new ActionForwarder();
        panel_ = new JPanel();
    }

    /**
     * Returns the configuration specified by this panel for a given index.
     *
     * @param  index  array index
     * @return   config map
     */
    public ConfigMap getConfig( int index ) {
        ConfigMap config = new ConfigMap();
        for ( KSpec<?> kspec : specList_ ) {
            kspec.putValue( index, config );
        }
        return config;
    }

    /**
     * Returns the GUI component containing this panel.
     *
     * @return  panel
     */
    public JComponent getComponent() {
        return panel_;
    }

    /**
     * Adds a listener for changes to the specifiers.
     *
     * @param  l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        actionForwarder_.addActionListener( l );
    }

    /**
     * Removes a listener for changes to the specifiers.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        actionForwarder_.removeActionListener( l );
    }

    /**
     * Updates the display to show a given number of specifier sets.
     * Ones not seen before are lazily created as required.
     *
     * @param  count  number of specifiers to display
     */
    public void showElements( int count ) {
        if ( count == count_ ) {
            return;
        }
        count_ = count;

        /* Clear panel and prepare to repopulate it. */
        panel_.removeAll();
        GridBagLayout layout = new GridBagLayout();
        panel_.setLayout( layout );
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 0;

        /* Add each specifier label once in first column. */
        GridBagConstraints cons1 = (GridBagConstraints) cons.clone();
        for ( KSpec<?> kspec : specList_ ) {
            cons1.gridy++;
            ConfigKey<?> key = kspec.key_;
            JLabel label = new JLabel( key.getMeta().getLongName() + ": " );
            cons1.anchor = GridBagConstraints.EAST;
            layout.setConstraints( label, cons1 );
            panel_.add( label );
        }

        /* For each visible array index, add index label and one of each
         * specifier. */
        cons1 = (GridBagConstraints) cons.clone();
        for ( int i = 0; i < count; i++ ) {
            cons1.gridy = 0;
            cons1.gridx++;
            GridBagConstraints cons2 = (GridBagConstraints) cons1.clone();
            JLabel label = new JLabel( labelFunc_.apply( i ) );
            cons2.anchor = GridBagConstraints.CENTER;
            cons2.insets = new Insets( 2, 8, 2, 8 );
            layout.setConstraints( label, cons2 );
            panel_.add( label );
            for ( KSpec<?> kspec : specList_ ) {
                cons1.gridy++;
                JComponent component = kspec.getSpecifier( i ).getComponent();
                cons1.anchor = GridBagConstraints.CENTER;
                layout.setConstraints( component, cons1 );
                panel_.add( component );
            }
        }

        /* Pad and refresh. */
        cons1 = (GridBagConstraints) cons.clone();
        cons1.weightx = 1;
        cons1.gridy = 0;
        cons1.gridx = count + 1;
        Component pad = Box.createHorizontalStrut( 4 );
        layout.setConstraints( pad, cons1 );
        panel_.add( pad, cons1 );
        panel_.revalidate();
        panel_.repaint();
    }

    /**
     * Aggregates a typed ConfigKey and an array of specifiers for that key.
     */
    private class KSpec<T> {

        final ConfigKey<T> key_;
        final List<Specifier<T>> specifiers_;

        /**
         * Constructor.
         *
         * @param  key  config key
         */
        KSpec( ConfigKey<T> key ) {
            key_ = key;
            specifiers_ = new ArrayList<Specifier<T>>();
        }

        /**
         * Writes an indexed config value into a given map.
         *
         * @param  index  array index
         * @param  map   config map to receive value
         */
        void putValue( int index, ConfigMap map ) {
            map.put( key_, getSpecifier( index ).getSpecifiedValue() );
        }

        /**
         * Returns the specifier for a given index.
         *
         * @param  index  array index
         * @return   specifier for index
         */
        Specifier<T> getSpecifier( int index ) {
            while ( specifiers_.size() <= index ) {
                Specifier<T> specifier = key_.createSpecifier();
                specifier.setSpecifiedValue( key_.getDefaultValue() );
                specifier.addActionListener( actionForwarder_ );
                specifiers_.add( specifier );
            }
            return specifiers_.get( index );
        }
    }
}
