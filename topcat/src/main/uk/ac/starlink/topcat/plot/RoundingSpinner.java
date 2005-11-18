package uk.ac.starlink.topcat.plot;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;

/**
 * JSpinner subclass which goes up or down using round numbers.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2005
 */
public class RoundingSpinner extends JSpinner {

    private boolean isLog_;

    /**
     * Constructs a new spinner with default (linear) rounding.
     */
    public RoundingSpinner() {
        setModel( new RoundingSpinnerModel( this ) );
        setEditor( new NumberEditor( this ) );
    }

    /**
     * Returns this spinner's selected value.
     *
     * @return  value
     */
    public double getNumericValue() {
        return ((Number) getValue()).doubleValue();
    }

    /**
     * Sets this spinner's selected value.
     *
     * @param  value  value
     */
    public void setNumericValue( double value ) {
        setValue( new Double( value ) );
    }

    /**
     * Configures this spinner for logarithmic or linear values.
     *
     * @param  isLog  true for logarithmic rounding, false for linear
     */
    public void setLogarithmic( boolean isLog ) {
        isLog_ = isLog;
    }

    /**
     * Determines whether this spinner is configured for logarithmic values.
     *
     * @return   true for logarithmic rounding, false for linear
     */
    public boolean isLogarithmic() {
        return isLog_;
    }

    /**
     * Returns the rounder used by this spinner.
     *
     * @return  rounder
     */
    private Rounder getRounder() {
        return isLog_ ? Rounder.LOG : Rounder.LINEAR;
    }

    /**
     * Spinner model used for a rounding spinner.
     */
    private static class RoundingSpinnerModel extends AbstractSpinnerModel {

        private final RoundingSpinner spinner_;
        private double value_;

        RoundingSpinnerModel( RoundingSpinner spinner ) {
            spinner_ = spinner;
        }

        public Object getNextValue() {
            return new Double( spinner_.getRounder().nextUp( value_ ) );
        }

        public Object getPreviousValue() {
            return new Double( spinner_.getRounder().nextDown( value_ ) );
        }

        public Object getValue() {
            return new Double( value_ );
        }

        public void setValue( Object val ) {
            double value;
            if ( val instanceof Number ) {
                value = ((Number) val).doubleValue();
            }
            else if ( val instanceof String ) {
                value = Double.parseDouble( (String) val );
            }
            else {
                throw new IllegalArgumentException( "Wrong class " + val );
            }
            value_ = value;
            fireStateChanged();
        }
    }

    /**
     * Editor component used for a rounding spinner.
     */
    private static class NumberEditor extends JSpinner.DefaultEditor {
        NumberEditor( JSpinner spinner ) {
            super( spinner );
            getTextField().setEditable( true );
            getTextField().setHorizontalAlignment( JTextField.RIGHT );

            /* Good grief.  This is just so that the number is rendered
             * using Double.toString(), not a custom renderer. */
            getTextField().setFormatterFactory( new JFormattedTextField
                                                   .AbstractFormatterFactory() {
                public JFormattedTextField.AbstractFormatter 
                       getFormatter( JFormattedTextField tf ) {
                    return new JFormattedTextField.AbstractFormatter() {
                        public Object stringToValue( String text ) {
                            return Double.valueOf( text );
                        }
                        public String valueToString( Object value ) {
                            return String.valueOf( value );
                        }
                    };
                }
            } );
        }
    }
}
