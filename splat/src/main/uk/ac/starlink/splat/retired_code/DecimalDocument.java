package uk.ac.starlink.splat.iface;

import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 *
 * DecimalDocument extends PlainDocument to so that any associated
 * components will only accept valid floating or integer words. The
 * actual format of the representation (locale specific) is defined by
 * a DecimalFormat object.
 *
 * @since $Date$
 * @since 26-OCT-2000
 * @author Peter W. Draper
 * @version $Id$
 */
public class DecimalDocument extends PlainDocument 
{
    private DecimalFormat format;

    public DecimalDocument( DecimalFormat format ) 
    {
        this.format = format;
    }

    public DecimalFormat getFormat() 
    {
        return format;
    }

    public void insertString(int offs, String str, AttributeSet a) 
        throws BadLocationException 
    {
        //  Add a trailing zero so that "+", "-" and "." may be
        //  entered as the first character. This is in fact the only
        //  floating point specific part of this class. The real work
        //  is done by the NumberFormat object.
        String currentText = getText( 0, getLength() ) + "0";

        String beforeOffset = currentText.substring(0, offs);
        String afterOffset = currentText.substring(offs, currentText.length());
        String proposedResult = beforeOffset + str + afterOffset;
        try {
            format.parseObject( proposedResult );
            super.insertString( offs, str, a );
        } catch (ParseException e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void remove( int offs, int len ) 
        throws BadLocationException 
    {
        String currentText = getText( 0, getLength() );
        String beforeOffset = currentText.substring( 0, offs );
        String afterOffset = currentText.substring(
                               len + offs, currentText.length() );
        String proposedResult = beforeOffset + afterOffset;
        try {
            if ( proposedResult.length() > 1 ) { // any single value OK.
                format.parseObject( proposedResult );
            }
            super.remove( offs, len );
        } catch ( ParseException e ) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
