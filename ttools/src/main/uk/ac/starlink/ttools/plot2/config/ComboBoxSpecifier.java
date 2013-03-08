package uk.ac.starlink.ttools.plot2.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

    private final JComboBox comboBox_;

    /**
     * Constructs a specifier with a given combo box, and optional custom
     * laelling.
     *
     * @param  comboBox  combo box instance with appropriate options
     *                   (must all be assignable from V)
     * @param  customStringify  if true, this object's <code>stringify</code>
     *                          method is used to provide combo box text
     */
    public ComboBoxSpecifier( JComboBox comboBox, boolean customStringify ) {
        super( comboBox.isEditable() );
        comboBox_ = comboBox;
        if ( customStringify ) {
            comboBox_.setRenderer( new CustomComboBoxRenderer() {
                public Object mapValue( Object value ) {
                    @SuppressWarnings("unchecked")
                    V val = (V) value;
                    return stringify( val );
                }
            } );
        }
    }

    /**
     * Constructs a specifier with a given combo box and default labelling.
     *
     * @param  comboBox  combo box instance with appropriate options
     *                   (must all be assignable from V)
     */
    public ComboBoxSpecifier( JComboBox comboBox ) {
        this( comboBox, false );
    }

    /**
     * Constructs a specifier selecting from a given collection of options.
     *
     * @param  options   options
     */
    public ComboBoxSpecifier( Collection<V> options ) {
        this( new JComboBox( options.toArray() ), true );
        comboBox_.setSelectedIndex( 0 );
    }

    /**
     * Constructs a specifier selecting from a given array of options.
     *
     * @param  options   options
     */
    public ComboBoxSpecifier( V[] options ) {
        this( Arrays.asList( options ) );
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
        @SuppressWarnings("unchecked")
        V value = (V) comboBox_.getSelectedItem();
        return value;
    }

    public void setSpecifiedValue( V value ) {
        comboBox_.setSelectedItem( value );
    }

    /**
     * Returns this specifier's combo box.
     *
     * @return   combo box doing the work
     */
    public JComboBox getComboBox() {
        return comboBox_;
    }
}
