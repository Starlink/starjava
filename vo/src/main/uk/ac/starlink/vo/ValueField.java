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

/**
 * Generalised data entry field.
 * As well as a text entry field, this also contains an option for choosing
 * the format in which the data will be entered.
 * The format options are defined by an array of {@link ValueConverter}
 * objects supplied at construction time.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ValueField {

    private final JLabel label_;
    private final JTextField entryField_;
    private final JComboBox convSelector_;

    /**
     * Constructor.
     *
     * @param  name  field name
     * @param  convs  list of converter objects
     */
    public ValueField( String name, ValueConverter[] convs ) {
        label_ = new JLabel( name + ": " );
        entryField_ = new JTextField( 12 );
        convSelector_ = new JComboBox( convs );
        convSelector_.setSelectedIndex( 0 );
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
    public JComboBox getConverterSelector() {
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
        ValueConverter vc = (ValueConverter) convSelector_.getSelectedItem();
        return vc.convertValue( getEntryField().getText() );
    }

    /**
     * Returns an instance suitable for entering Right Ascension,
     * for which {@link #getValue} returns degrees.
     *
     * @param  ra field
     */
    public static ValueField makeRADegreesField() {
        return new ValueField( "RA", new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.HMSDegreesValueConverter(),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }

    /**
     * Returns an instance suitable for entering Declination
     * for which {@link #getValue} returns degrees.
     *
     * @param  dec field
     */
    public static ValueField makeDecDegreesField() {
        return new ValueField( "Dec", new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.DMSDegreesValueConverter(),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }

    /**
     * Returns an instance suitable for entering an angular radius,
     * for which {@link #getValue} returns degrees.
     *
     * @param  radius field
     */
    public static ValueField makeRadiusDegreesField() {
        return new ValueField( "Radius", new ValueConverter[] {
            new ValueConverter.UnitValueConverter( "degrees", 1. ),
            new ValueConverter.UnitValueConverter( "arcmin", 1. / 60. ),
            new ValueConverter.UnitValueConverter( "arcsec", 1. / 60. / 60. ),
            new ValueConverter.UnitValueConverter( "radians", 180. / Math.PI ),
        } );
    }

}
