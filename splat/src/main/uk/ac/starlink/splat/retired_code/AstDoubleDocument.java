// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    24-JUL-2001 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.iface;

import java.awt.Toolkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import uk.ac.starlink.splat.ast.AstDouble;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * AstDoubleDocument extends PlainDocument to so that any associated
 * components will only accept valid AstDouble formatted strings. This
 * requires a Plot and a valid axis. <p>
 *
 * Use this with AstDoubleFields (a JTextField) that should only take
 * appropriately formatted AST coordinates (like RA and Dec).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstDoubleDocument extends PlainDocument
{
    private DivaPlot plot;
    private int axis;

    /**
     * Constructor for the AstDoubleDocument object
     */
    public AstDoubleDocument( DivaPlot plot, int axis )
    {
        this.plot = plot;
        this.axis = axis;
    }

    //  Insert characters.
    public void insertString( int offs, String str, AttributeSet a )
        throws BadLocationException
    {
        //  Store current value.
        String currentText = getText( 0, getLength() );

        //  Construct the text string with the new characters inserted.
        String beforeOffset = currentText.substring( 0, offs );
        String afterOffset = currentText.substring( offs,
                                                    currentText.length() );
        String proposedResult = beforeOffset + str + afterOffset;

        //  Try to unformat this value into a double.
        double value = plot.unFormat( axis, proposedResult );
        if ( ! AstDouble.isBad( value ) ) {

            //  Conversion succeeded so insert new string.
            super.insertString( offs, str, a );
        }
        else {
            // Squawk!
            Toolkit.getDefaultToolkit().beep();
        }
    }

    //  Remove characters.
    public void remove( int offs, int len )
        throws BadLocationException
    {
        String currentText = getText( 0, getLength() );
        String beforeOffset = currentText.substring( 0, offs );
        String afterOffset =
            currentText.substring( len + offs, currentText.length() );
        String proposedResult = beforeOffset + afterOffset;

        //  An Empty field is always accepted.
        if ( proposedResult.equals( "" ) ) {
            super.remove( offs, len );
        }
        else {
            //  Try to unformat this value into a double.
            double value = plot.unFormat( axis, proposedResult );
            if ( ! AstDouble.isBad( value ) ) {

                //  Conversion succeeded so remove characters.
                super.remove( offs, len );
            }
            else {
                // Squawk!
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
