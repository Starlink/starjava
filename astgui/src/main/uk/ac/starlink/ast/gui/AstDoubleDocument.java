/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 * History:
 *    24-JUL-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Toolkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import uk.ac.starlink.ast.Frame;

/**
 * AstDoubleDocument extends PlainDocument to so that any associated
 * components will only accept valid AstDouble formatted strings. This
 * requires a {@link PlotController} and a valid axis.
 * <p>
 * Use this with AstDoubleFields (a JTextField) that should only take
 * appropriately formatted AST coordinates (like RA and Dec).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstDoubleDocument extends PlainDocument
{
    /** Used to access the {@link Plot}, needed as Plots are generally
     *  re-created and a direct reference would go stale. */
    private PlotController controller;
    private int axis;

    /**
     * Constructor for the AstDoubleDocument object
     */
    public AstDoubleDocument( PlotController controller, int axis )
    {
        this.controller = controller;
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

        //  The special field <bad> is used to mean that the default
        //  should be used. This is shown as an empty string.
        if ( str.equals( "<bad>" ) ) {
            super.remove( 0, getLength() );
        }
        else if ( proposedResult.equals( "" ) ) {
                //  An Empty field is acceptable, since it means <bad>
                //  or undefined too.
            super.insertString( offs, str, a );
        }
        else {
            
            //  Try to unformat this value into a double.
            double value =
                controller.getPlotCurrentFrame().unformat( axis, 
                                                           proposedResult );
            if ( ! AstDouble.isBad( value ) ) {
                //  Conversion succeeded so insert new string.
                super.insertString( offs, str, a );
            }
            else {
                // Squawk!
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    //  Remove characters.
    public void remove( int offs, int len )
        throws BadLocationException
    {
        String currentText = getText( 0, getLength() );
        String beforeOffset = currentText.substring( 0, offs );
        String afterOffset = currentText.substring( len + offs,
                                                    currentText.length() );
        String proposedResult = beforeOffset + afterOffset;

        //  An Empty field is always accepted. This is same as <bad>
        //  (except no-one should ever see that string, but do that as
        //  well anyway).
        if ( currentText.equals( "<bad>" ) ) {
            super.remove( 0, getLength() );
        } 
        else if ( proposedResult.equals( "" ) ) {
            super.remove( offs, len );
        }
        else {
            //  Try to unformat this value into a double.
            double value =
                controller.getPlotCurrentFrame().unformat(axis,proposedResult);
            if ( ! AstDouble.isBad( value ) ) {

                //  Conversion succeeded so remove characters.
                super.remove( offs, len );
            }
            else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
