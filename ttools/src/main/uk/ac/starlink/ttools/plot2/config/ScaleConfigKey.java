package uk.ac.starlink.ttools.plot2.config;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.ttools.plot2.ParsedFunctionCall;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.ScaleType;
import uk.ac.starlink.util.gui.ComboBoxBumper;

/**
 * Config key for axis scales.
 *
 * <p>Serialization to string uses
 * {@link uk.ac.starlink.ttools.plot2.ParsedFunctionCall},
 * so it's a scale name with optionally comma-separated parameters
 * enclosed in parentheses, e.g. "<code>linear</code>" or
 * "<code>symlog(2,.5)</code>".  Missing parameters are permitted
 * and set to the relevant parameter default.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2025
 */
public class ScaleConfigKey extends ConfigKey<Scale> {

    /**
     * Constructor.
     *
     * @param  meta  metadata
     */
    public ScaleConfigKey( ConfigMeta meta ) {
        super( meta, Scale.class, Scale.LINEAR );
    }

    public String valueToString( Scale scale ) {
        ScaleType type = scale.getScaleType();
        ScaleType.Param[] params = type.getParams();
        String typeName = type.getName();
        double[] paramValues = scale.getParamValues();
        int np = paramValues.length;
        while ( np > 0 &&
                paramValues[ np - 1 ] == params[ np - 1 ].getDefault() ) {
            np--;
        }
        if ( np > 0 ) {
            double[] values = new double[ np ];
            System.arraycopy( paramValues, 0, values, 0, np );
            return new ParsedFunctionCall( typeName, values ).toString();
        }
        else {
            return typeName;
        }
    }

    public Scale stringToValue( String txt ) throws ConfigException {
        ParsedFunctionCall pfc = ParsedFunctionCall.fromString( txt );
        if ( pfc != null ) {
            String typeName = pfc.getFunctionName();
            double[] args = pfc.getArguments();
            ScaleType type = ScaleType.fromName( typeName );
            if ( type != null ) {
                if ( args != null && args.length > type.getParams().length ) {
                    throw new ConfigException( this,
                                               "Too many arguments for "
                                             + "scale type " + type.getName() );
                }
                try {
                    return type.createScale( args == null ? new double[ 0 ]
                                                          : args );
                }
                catch ( IllegalArgumentException e ) {
                    throw new ConfigException( this,
                                               "Bad scale arguments: " + txt,
                                               e );
                }
            }
            else {
                throw new ConfigException( this,
                                           "Unknown scale type: " + typeName );
            }
        }
        else {
            throw new ConfigException( this, "Unparseable scale: " + txt );
        }
    }

    public Specifier<Scale> createSpecifier() {
        return new ScaleSpecifier();
    }

    /**
     * Specifier implementation for this key.
     */
    private static class ScaleSpecifier extends SpecifierPanel<Scale> {
        private final JComboBox<ScaleType> typeBox_;
        private final JComponent fieldContainer_;
        private final Map<ScaleType,ArgField[]> fieldsMap_;

        ScaleSpecifier() {
            super( false );
            typeBox_ = new JComboBox<ScaleType>( ScaleType.getInstances() );
            fieldContainer_ = Box.createHorizontalBox();
            fieldsMap_ = new HashMap<ScaleType,ArgField[]>();
            ActionListener forwarder = getActionForwarder();
            typeBox_.addActionListener( forwarder );
        }

        protected JComponent createComponent() {
            Box box = Box.createHorizontalBox();
            box.add( typeBox_ );
            box.add( Box.createHorizontalStrut( 2 ) );
            box.add( new ComboBoxBumper( typeBox_ ) );
            box.add( fieldContainer_ );
            typeBox_.addActionListener( evt -> {
                Object typeObj = typeBox_.getSelectedItem();
                if ( typeObj instanceof ScaleType ) {
                    updateSelector( (ScaleType) typeObj );
                }
            } );
            return box;
        }

        public Scale getSpecifiedValue() {
            ScaleType type = typeBox_.getItemAt( typeBox_.getSelectedIndex() );
            double[] args = Arrays.stream( fieldsMap_.get( type ) )
                           .mapToDouble( ArgField::readValue )
                           .toArray();
            return type.createScale( args );
        }

        public void setSpecifiedValue( Scale scale ) {
            ScaleType type = scale.getScaleType();
            double[] args = scale.getParamValues();
            typeBox_.setSelectedItem( type );
            updateSelector( type );
            ArgField[] afields = fieldsMap_.get( type );
            for ( int ia = 0; ia < afields.length; ia++ ) {
                afields[ ia ].writeValue( args[ ia ] );
            }
        }

        public void submitReport( ReportMap report ) {
        }

        /**
         * Called if the type selection may have changed, to update
         * related parts of the GUI.
         *
         * @param  type  new selected scale type
         */
        private void updateSelector( ScaleType type ) {
            assert typeBox_.getSelectedItem() == type;
            assert SwingUtilities.isEventDispatchThread();
            fieldContainer_.removeAll();
            if ( ! fieldsMap_.containsKey( type ) ) {
                ScaleType.Param[] params = type.getParams();
                ArgField[] afields = new ArgField[ params.length ];
                ChangeListener listener =
                    evt -> getActionForwarder()
                          .actionPerformed( new ActionEvent( this, 0, "x" ) );
                for ( int ip = 0; ip < params.length; ip++ ) {
                    ArgField afield = new ArgField( params[ ip ] );
                    afield.addChangeListener( listener );
                    afields[ ip ] = afield;
                }
                fieldsMap_.put( type, afields );
            }
            for ( ArgField afield : fieldsMap_.get( type ) ) {
                fieldContainer_.add( Box.createHorizontalStrut( 5 ) );
                fieldContainer_.add( new JLabel( afield.param_.getName()
                                               + ":" ) );
                fieldContainer_.add( afield );
            }
            fieldContainer_.revalidate();
        }
    }

    /**
     * Field for displaying a named numeric parameter associated with
     * a scale specification.
     */
    private static class ArgField extends JSpinner {

        private final ScaleType.Param param_;

        /**
         * Constructor.
         *
         * @param   param  scale parameter definition
         */
        ArgField( ScaleType.Param param ) {
            super( new ParamSpinnerModel( param ) );
            param_ = param;
            setToolTipText( param.getDescription() );
            setValue( param.getDefault() );

            // This is a hack; for all existing ScaleType implementations,
            // all their arguments must be strictly positive.
            // If future implementations have more nuanced requirements on
            // their arguments, something cleverer will be needed here.
            boolean requirePositive = true;
            setEditor( new ArgNumberEditor( this, param.getDefault(),
                                            requirePositive ) );
        }

        /**
         * Returns the currently displayed value.
         *
         * @return  value
         */
        public double readValue() { 
            Object obj = getValue();
            return obj instanceof Number ? ((Number) obj).doubleValue()
                                         : param_.getDefault();
        }

        /**
         * Sets the displayed value.
         *
         * @param  d value
         */
        public void writeValue( double d ) {
            setValue( Double.valueOf( d ) );
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension( Math.max( size.width, 64 ), size.height );
        }
    }

    /**
     * Custom editor class for the JSpinner displaying Arg values.
     */
    private static class ArgNumberEditor extends JSpinner.NumberEditor {
        private final double dflt_;
        private final boolean requirePositive_;

        /**
         * Constructor.
         *
         * @param  spinner  spinner to which it will be attached
         * @param  dflt    parameter default value
         * @parm   requirePositive  true if values must be strictly &gt;0
         */
        public ArgNumberEditor( JSpinner spinner, double dflt,
                                boolean requirePositive ) {
            super( spinner );
            dflt_ = dflt;
            requirePositive_ = requirePositive;

            /* Field validation seems like a real mess - there appears to be
             * functionality in JFormattedTextField dedicated to this but
             * it doesn't work like I'd expect.  Hack it till it behaves. */
            JFormattedTextField field = getTextField();
            JFormattedTextField.AbstractFormatter formatter =
                    new JFormattedTextField.AbstractFormatter() {
                public Object stringToValue( String txt )
                        throws ParseException {
                    try {
                        return Double.valueOf( txt );
                    }
                    catch ( NumberFormatException e ) {
                        throw (ParseException)
                              new ParseException( "Not a number", 0 )
                             .initCause( e );
                    }
                }
                public String valueToString( Object value )
                        throws ParseException {
                    if ( value instanceof Number ) {
                        double d = ((Number) value).doubleValue();
                        if ( requirePositive_ && d <= 0 ) {
                            throw new ParseException( "Non-positive",
                                                      0 );
                        }
                        return d == (int) d
                             ? Integer.toString( (int) d )
                             : Double.toString( d );
                    }
                    else {
                        throw new ParseException( "Not a number", 0 );
                    }
                }
            };
            field.setFormatterFactory( new JFormattedTextField
                                          .AbstractFormatterFactory() {
                public JFormattedTextField.AbstractFormatter
                        getFormatter( JFormattedTextField tf ) {
                    return formatter;
                }
            } );
            field.addFocusListener( new FocusListener() {
                public void focusGained( FocusEvent evt ) {
                    checkValue();
                }
                public void focusLost( FocusEvent evt ) {
                    checkValue();
                }
                private void checkValue() {
                    Object val = field.getValue();
                    if ( ! (val instanceof Number) ||
                         ( requirePositive_ &&
                           ! (((Number) val).doubleValue() > 0) ) ) {
                        field.setValue( Double.valueOf( dflt_ ) );
                    }
                }
            } );
        }
    }

    /**
     * SpinnerModel based on a ScaleType.Param.
     * Values are numeric.
     */
    private static class ParamSpinnerModel extends SpinnerNumberModel {
        private final ScaleType.Param param_;
        private double value_;

        /**
         * Constructor.
         *
         * @param  param  param
         */
        ParamSpinnerModel( ScaleType.Param param ) {
            param_ = param;
            value_ = param.getDefault();
        }

        @Override
        public void setValue( Object value ) {
            if ( value instanceof Number ) {
                value_ = ((Number) value).doubleValue();
                fireStateChanged();
            }
        }

        @Override
        public Object getValue() {
            return Double.valueOf( value_ );
        }

        @Override
        public Object getNextValue() {
            return Double.valueOf( param_.nextUp( value_ ) );
        }

        @Override
        public Object getPreviousValue() {
            return Double.valueOf( param_.nextDown( value_ ) );
        }
    }
}
