package uk.ac.starlink.splat.iface;

import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JTextField;

/**
 * DecimalField extends JTextField to force the entry of valid decimal
 * (i.e. floating point and integer) numbers. The representation and
 * exact format of the numbers is locale specific and is defined by a
 * DecimalFormat object.
 *
 * @since $Date$
 * @since 26-OCT-2000
 * @author Peter W. Draper
 * @version $Id$
 */
public class DecimalField extends JTextField
{
    /**
     * Reference to object that describes the locale specific number
     * format.
     */
    protected DecimalFormat decimalFormat;

    /**
     * Create an instance, requires the initial value (as a double),
     * the number of columns to show and a DecimalFormat object to use
     * when checking and formatting the accepted values. Uses a
     * DecimalDocument to check that any typed values are valid for
     * this format.
     */
    public DecimalField( double value, int columns,
                         DecimalFormat format )
    {
        super( columns );
        setDocument( new DecimalDocument( format ) );
        decimalFormat = format;
        setDoubleValue( value );
    }

    /**
     * Create an instance, requires the initial value (as an int),
     * the number of columns to show and a DecimalFormat object to use
     * when checking and formatting the accepted values. Uses a
     * DecimalDocument to check that any typed values are valid for
     * this format.
     */
    public DecimalField( int value, int columns, DecimalFormat format )
    {
        super( columns );
        setDocument( new DecimalDocument( format ) );
        decimalFormat = format;
        setIntValue( value );
    }

    /**
     * Get the current value as double precision. If this fails then
     * 0.0 is returned.
     */
    public double getDoubleValue()
    {
        double retVal = 0.0;
        try {
            retVal = decimalFormat.parse( getText() ).doubleValue();
        } catch ( ParseException e ) {
        }
        return retVal;
    }

    /**
     * Get the current value as an integer. If this fails then 0 is
     * returned.
     */
    public int getIntValue()
    {
        int retVal = 0;
        try {
            retVal = decimalFormat.parse( getText() ).intValue();
        } catch ( ParseException e ) {
        }
        return retVal;
    }

    /**
     * Set the current value.
     */
    public void setDoubleValue( double value )
    {
        setText( decimalFormat.format( value ) );
    }

    /**
     * Set the current value.
     */
    public void setIntValue( int value )
    {
        setText( decimalFormat.format( value ) );
    }
}
