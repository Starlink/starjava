package uk.ac.starlink.vo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Generalised data entry field which can hold a double precision number.
 * As well as a text entry field, this also contains an option for choosing
 * the format in which the data will be entered.
 * The format options are defined by an array of {@link ValueConverter}
 * objects supplied at construction time.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class DoubleValueField {

    private final DefaultValueInfo info_;
    private final JLabel label_;
    private final JTextField entryField_;
    private final JComboBox<ValueConverter> convSelector_;

    /**
     * Constructs a value field given its name.
     *
     * @param  name  field name
     * @param  convs  list of converter objects
     */
    public DoubleValueField( String name, ValueConverter[] convs ) {
        this( new DefaultValueInfo( name, Double.class, "" ), convs );
    }

    /**
     * Constructs a value field given a ValueInfo object.
     *
     * @param  info  field metadata
     * @param  convs  list of converter objects
     */
    public DoubleValueField( ValueInfo info, ValueConverter[] convs ) {
        info_ = new DefaultValueInfo( info );
        label_ = new JLabel( info_.getName() + ": " );
        entryField_ = new JTextField( 12 ) {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            @Override
            public Dimension getMinimumSize() {
                return new Dimension( 32, super.getPreferredSize().height );
            }
        };
        convSelector_ = new JComboBox<>( convs );
        convSelector_.setSelectedIndex( 0 );
        String description = info.getDescription();
        if ( description != null ) {
            setDescription( description );
        }
    }

    /**
     * Returns the ValueInfo object which describes the data in this field.
     *
     * @return   metadata object for this field
     */
    public DefaultValueInfo getValueInfo() {
        return info_;
    }

    /**
     * Sets the description of this field.  This may be presented as a tool
     * tip and stored in the metadata associated with this field.
     *
     * @param  description   description of field
     */
    public void setDescription( String description ) {
        info_.setDescription( description );
        label_.setToolTipText( description );
        entryField_.setToolTipText( description );
    }

    /**
     * Returns a described value object (metadata+data) which describes 
     * the value currently held by this field.
     *
     * @return   content of this field
     */
    public DescribedValue getDescribedValue() {
        Double val;
        try {
            val = Double.valueOf( getValue() );
        }
        catch ( Exception e ) {
            val = null;
        }
        return new DescribedValue( info_, val );
    }

    /**
     * Returns this field's label componnent.
     *
     * @return  label
     */
    public JLabel getLabel() {
        return label_;
    }

    /** 
     * Returns this field's text entry component.
     *
     * @return   entry field
     */
    public JTextField getEntryField() {
        return entryField_;
    }

    /**
     * Returns the combo box used to select the entry format used by
     * this field.
     *
     * @return   format selector
     */
    public JComboBox<ValueConverter> getConverterSelector() {
        return convSelector_;
    }

    /**
     * Returns the numeric value which the user has entered.
     * This may be modified according to which format convertor is currently
     * selected.
     *
     * @return  field value
     * @throws  IllegalArgumentException  if the current contents of the
     *          entry field don't make sense to the current format selector
     */
    public double getValue() {
        ValueConverter vc =
            convSelector_.getItemAt( convSelector_.getSelectedIndex() );
        try {
            return vc.convertValue( getEntryField().getText() );
        }
        catch ( RuntimeException e ) {
            String msg = "Invalid value for " + info_.getName() + " field";
            throw (IllegalArgumentException)
                  new IllegalArgumentException( msg ).initCause( e );
        }
    }

    /**
     * Sets the state of the GUI component controlled by this field.
     *
     * @param  value  value to display
     */
    public void setValue( double value ) {
        ValueConverter vc =
            convSelector_.getItemAt( convSelector_.getSelectedIndex() );
        try {
            getEntryField().setText( vc.unconvertValue( Double
                                                       .valueOf( value ) ) );
        }
        catch ( RuntimeException e ) {
            String msg = "Invalid value for " + info_.getName() + " field";
            throw (IllegalArgumentException)
                  new IllegalArgumentException( msg ).initCause( e );
        }
    }

    /**
     * Sets the enabled status of the user-interacting components of this
     * field.
     *
     * @param   enabled  whether this component is to be enabled or not
     */
    public void setEnabled( boolean enabled ) {
        entryField_.setEnabled( enabled );
        convSelector_.setEnabled( enabled );
        label_.setEnabled( enabled );
    }

    /**
     * Returns an instance suitable for entering Right Ascension,
     * for which {@link #getValue} returns degrees.
     *
     * @return  ra field
     */
    public static DoubleValueField makeRADegreesField() {
        DefaultValueInfo info = new DefaultValueInfo( "RA", Double.class, 
                                                      "Right Ascension" );
        info.setUnitString( "deg" );
        return new DoubleValueField( info, new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.HMSDegreesValueConverter(),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }

    /**
     * Returns an instance suitable for entering Declination
     * for which {@link #getValue} returns degrees.
     *
     * @return  dec field
     */
    public static DoubleValueField makeDecDegreesField() {
        DefaultValueInfo info = new DefaultValueInfo( "Dec", Double.class,
                                                      "Declination" );
        info.setUnitString( "deg" );
        return new DoubleValueField( info, new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.DMSDegreesValueConverter(),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }

    /**
     * Returns an instance suitable for entering an angular size,
     * for which {@link #getValue} returns degrees.
     *
     * @param   info   description of field content
     * @return  angular size field
     */
    public static DoubleValueField makeSizeDegreesField( ValueInfo info ) {
        if ( info instanceof DefaultValueInfo ) {
            ((DefaultValueInfo) info).setUnitString( "deg" );
        }
        return new DoubleValueField( info, new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.UnitValueConverter( "arcmin", 1. / 60. ),
            new ValueConverter.UnitValueConverter( "arcsec", 1. / 60. / 60. ),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }
}
