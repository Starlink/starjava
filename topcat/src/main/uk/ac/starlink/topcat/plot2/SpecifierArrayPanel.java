package uk.ac.starlink.topcat.plot2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;

/**
 * Displays a variable number of Specifiers for a given ConfigKey.
 *
 * @author   Mark Taylor
 * @since    21 Sep 2023
 */
public class SpecifierArrayPanel<T> {

    private final ConfigKey<T> key_;
    private final IntFunction<String> labelFunc_;
    private final IntFunction<Specifier<T>> specifierFunc_;
    private final List<Specifier<T>> specifiers_;
    private final JPanel panel_;
    private final ActionForwarder forwarder_;
    private int count_;

    /**
     * Constructs a panel with default specifiers.
     *
     * @param   key  config key to use for all specifiers
     */
    public SpecifierArrayPanel( ConfigKey<T> key ) {
        this( key,
              i -> key.getMeta().getLongName() + " " + ( i + 1 ),
              i -> key.createSpecifier() );
    }

    /**
     * Constructs a panel with custom labels and specifiers.
     *
     * @param   key  config key by which configured values will be identified
     * @param   labelFunc  generates a specifier label for a given index
     * @param   specifierFunc  generates a specifier for a given index
     */
    public SpecifierArrayPanel( ConfigKey<T> key,
                                IntFunction<String> labelFunc,
                                IntFunction<Specifier<T>> specifierFunc ) {
        key_ = key;
        labelFunc_ = labelFunc;
        specifierFunc_ = specifierFunc;
        forwarder_ = new ActionForwarder();
        specifiers_ = new ArrayList<Specifier<T>>();
        panel_ = new JPanel();
    }

    /**
     * Returns the specifier for a given index.
     *
     * @param  index  array index
     * @return  specifier
     */
    public Specifier<T> getSpecifier( int index ) {
        while ( specifiers_.size() <= index ) {
            Specifier<T> specifier = specifierFunc_.apply( index );
            specifier.setSpecifiedValue( key_.getDefaultValue() );
            specifier.addActionListener( forwarder_ );
            specifiers_.add( specifier );
        }
        return specifiers_.get( index );
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
        forwarder_.addActionListener( l );
    }

    /**
     * Removes a listener for changes to the specifiers.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }

    /**
     * Updates the display to show a given number of specifiers.
     * Ones not seen before are lazily created as required.
     *
     * @param  count  number of specifiers to display
     */
    public void showElements( int count ) {
        if ( count == count_ ) {
            return;
        }
        count_ = count;
        panel_.removeAll();
        GridBagLayout layout = new GridBagLayout();
        panel_.setLayout( layout );
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 0;
        cons.insets = new Insets( 0, 0, 4, 0 );
        for ( int index = 0; index < count; index++ ) {

            /* Add the label. */
            JLabel label = new JLabel( labelFunc_.apply( index ) + ": " );
            GridBagConstraints cons1 = (GridBagConstraints) cons.clone();
            cons1.anchor = GridBagConstraints.EAST;
            layout.setConstraints( label, cons1 );
            panel_.add( label );

            /* Add the specifier, which is lazily created as required. */
            Specifier<T> specifier = getSpecifier( index );
            boolean xfill = specifier.isXFill();
            JComponent comp = specifier.getComponent();
            if ( xfill ) {
                comp.setPreferredSize( comp.getMinimumSize() );
            }
            GridBagConstraints cons2 = (GridBagConstraints) cons.clone();
            cons2.gridx = 1;
            cons2.anchor = GridBagConstraints.WEST;
            cons2.weightx = 1.0;
            cons2.fill = xfill ? GridBagConstraints.HORIZONTAL
                               : GridBagConstraints.NONE;
            cons2.gridwidth = GridBagConstraints.REMAINDER;
            layout.setConstraints( comp, cons2 );
            panel_.add( comp );
            cons.gridy++;
        }
        panel_.revalidate();
        panel_.repaint();
    }
}
