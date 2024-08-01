package uk.ac.starlink.ttools.plot2.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.CustomComboBoxRenderer;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Typed specifier for selecting options from a combo box.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2013
 */
public class ComboBoxSpecifier<V> extends SpecifierPanel<V> {

    private final Class<V> clazz_;
    private final JComboBox<V> comboBox_;
    private final boolean allowAny_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.config" );

    /**
     * Constructs a specifier with a given combo box, and optional custom
     * labelling and setting restrictions.
     *
     * @param  clazz     value type for this specifier
     * @param  comboBox  combo box instance with appropriate options
     * @param  customStringify  if true, this object's <code>stringify</code>
     *                          method is used to provide combo box text
     * @param  allowAny   if true, then the <code>setSpecifiedValue</code>
     *                    method is allowed to set any value; otherwise,
     *                    it is restricted to the options in the combo box
     */
    @SuppressWarnings("this-escape")
    public ComboBoxSpecifier( Class<V> clazz, JComboBox<V> comboBox,
                              boolean customStringify, boolean allowAny ) {
        super( comboBox.isEditable() );
        clazz_ = clazz;
        comboBox_ = comboBox;
        allowAny_ = allowAny;
        if ( customStringify ) {
            comboBox_.setRenderer(
                    new CustomComboBoxRenderer<V>( stringify( null ) ) {
                @Override
                protected String mapValue( V value ) {
                    return stringify( value );
                }
            } );
        }
    }

    /**
     * Constructs a specifier with a given combo box and default options.
     *
     * @param  clazz     value type for this specifier
     * @param  comboBox  combo box instance with appropriate options
     */
    public ComboBoxSpecifier( Class<V> clazz, JComboBox<V> comboBox ) {
        this( clazz, comboBox, false, true );
    }

    /**
     * Constructs a specifier selecting from a given collection of options.
     *
     * @param  clazz     value type for this specifier
     * @param  options   options
     */
    public ComboBoxSpecifier( Class<V> clazz, Collection<V> options ) {
        this( clazz, new JComboBox<V>( new Vector<V>( options ) ), true, true );
        comboBox_.setSelectedIndex( 0 );
    }

    /**
     * Constructs a specifier selecting from a given array of options.
     *
     * @param  options   options
     */
    @SuppressWarnings("unchecked")
    public ComboBoxSpecifier( V[] options ) {
        this( (Class<V>) options.getClass().getComponentType(),
              Arrays.asList( options ) );
    }

    /**
     * May be used to turn typed values into text labels for the
     * combo box.
     * The default implementation uses toString; subclasses may
     * override it.
     * 
     * @param  value  typed value
     * @return  string representation
     */
    public String stringify( V value ) {
        return value == null ? null : value.toString();
    }

    protected JComponent createComponent() {
        final Box line = Box.createHorizontalBox();
        line.add( new ShrinkWrapper( comboBox_ ) );
        line.add( Box.createHorizontalStrut( 5 ) );
        line.add( new ComboBoxBumper( comboBox_ ) );
        comboBox_.addActionListener( getActionForwarder() );
        line.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( "enabled".equals( evt.getPropertyName() ) ) {
                    comboBox_.setEnabled( line.isEnabled() );
                }
            }
        } );
        return line;
    }

    public V getSpecifiedValue() {

        /* Note that the selected item is not typed and may or, conceivably,
         * may not be the appropriate type for this value, so we have to
         * test and cast it explicitly. */
        Object selObj = comboBox_.getSelectedItem();
        @SuppressWarnings("unchecked")
        V selValue = clazz_.isInstance( selObj ) ? clazz_.cast( selObj ) : null;
        return selValue;
    }

    public void setSpecifiedValue( V value ) {
        boolean tmpEditable = allowAny_ && ! comboBox_.isEditable();
        if ( tmpEditable ) {
            comboBox_.setEditable( true );
        }
        comboBox_.setSelectedItem( value );
        if ( tmpEditable ) {
            comboBox_.setEditable( false );
        }
        if ( ! PlotUtil.equals( comboBox_.getSelectedItem(), value ) ) {
            assert ! allowAny_;
            logger_.warning( "Attempt to set unlisted value " + value
                           + " failed" );
        }
    }

    public void submitReport( ReportMap report ) {
    }

    /**
     * Returns this specifier's combo box.
     *
     * @return   combo box doing the work
     */
    public JComboBox<V> getComboBox() {
        return comboBox_;
    }
}
