package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.UCDSelector;

/**
 * A dialogue window which queries the user for the characteristics of
 * a new table parameter and then appends it to the parameter list.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Aug 2004
 */
public class ParameterQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private final JTextField nameField_;
    private final JComboBox<Class<?>> typeBox_;
    private final JTextField valueField_;
    private final JTextField unitsField_;
    private final JTextField descField_;
    private final UCDSelector ucdSelector_;
    private final JTextField utypeField_;

    /**
     * Constructs a new ParameterQueryWindow.
     *
     * @param  tcModel  topcat model describing the table this relates to
     * @param  parent   parent window, may be used for positioning
     */
    @SuppressWarnings("this-escape")
    public ParameterQueryWindow( TopcatModel tcModel, Component parent ) {
        super( "Add New Parameter", parent );
        tcModel_ = tcModel;
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField_ = new JTextField();
        stack.addLine( "Parameter Name", nameField_ );

        /* Type field. */
        typeBox_ = new JComboBox<Class<?>>();
        typeBox_.addItem( String.class );
        typeBox_.addItem( Byte.class );
        typeBox_.addItem( Short.class );
        typeBox_.addItem( Integer.class );
        typeBox_.addItem( Long.class );
        typeBox_.addItem( Float.class );
        typeBox_.addItem( Double.class );
        typeBox_.addItem( Boolean.class );
        typeBox_.setRenderer( new ClassComboBoxRenderer( "?" ) );
        typeBox_.setSelectedItem( String.class );
        stack.addLine( "Type", typeBox_ );

        /* Value field. */
        valueField_ = new JTextField();
        stack.addLine( "Value", valueField_ );

        /* Units field. */
        unitsField_ = new JTextField();
        stack.addLine( "Units", unitsField_ );

        /* Description field. */
        descField_ = new JTextField();
        stack.addLine( "Description", descField_ );

        /* UCD field. */
        ucdSelector_ = new UCDSelector();
        stack.addLine( "UCD", ucdSelector_ );

        /* Utype field. */
        utypeField_ = new JTextField();
        stack.addLine( "Utype", utypeField_ );

        /* Add help information. */
        addHelp( "ParameterQueryWindow" );
    }

    /**
     * Invoked when the user tries to OK the transaction.
     */
    protected boolean perform() {
        String name = normalize( nameField_.getText() );
        Class<?> clazz = (Class<?>) typeBox_.getSelectedItem();
        String valueString = normalize( valueField_.getText() );
        String units = normalize( unitsField_.getText() );
        String desc = normalize( descField_.getText() );
        String ucd = ucdSelector_.getID();
        String utype = normalize( utypeField_.getText() );

        if ( name == null ) {
            return false;
        }
        else {
            DefaultValueInfo vinfo = new DefaultValueInfo( name, clazz, desc );
            vinfo.setUnitString( units );
            vinfo.setUCD( ucd );
            vinfo.setUtype( utype );
            Object value;
            if ( valueString == null ) {
                value = null;
            }
            else {
                try {
                    value = vinfo.unformatString( valueString );
                }
                catch ( RuntimeException e ) {
                    String[] msg = new String[] {
                        "Invalid value \"" + valueString + "\"",
                        "for type " + clazz.getName(),
                        "(" + e.toString() + ")"
                    };
                    JOptionPane.showMessageDialog( this, msg, 
                                                   "Invalid Parameter Value",
                                                   JOptionPane.ERROR_MESSAGE );
                    return false;
                }
            }
               
            DescribedValue dval = new DescribedValue( vinfo, value );
            tcModel_.addParameter( dval );
            return true;
        }
    }

    /**
     * Utility function that gives you the string you put in,
     * or <code>null</code> if it's a blank string.
     *
     * @param   base  input string
     * @return  <code>base</code> or <code>null</code>
     */
    private static String normalize( String base ) {
        return ( base == null || base.trim().length() == 0 ) ? null : base;
    }

}
