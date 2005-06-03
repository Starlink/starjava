/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-MAR-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Create a dialog for obtaining a single decimal number. Use the
 * "showDialog()" method to activate.
 *
 * @author Peter W. Draper
 * @version $Id$
 */

public class DecimalDialog
    extends JDialog
{
    /** The DecimalField */
    protected DecimalField decimalField = null;

    /** The ScientificFormat */
    protected ScientificFormat decimalFormat = null;

    /** The accepted value, null if not accepted */
    protected String value = null;

    /**
     * Show a dialog window. Returning the user value as a Number.
     *
     * @param component a parent component for the dialog, may be null
     * @param title the title for the dialog window
     * @param labelText description of the value that is to be obtained
     * @param format ScientificFormat instance for formatting numbers
     * @param initialValue the initial value to show in the text area
     *
     * @return the accepted value, or null if cancelled or the value
     *         is invalid.
     */
    public static Number showDialog( Component component,
                                     String title,
                                     String labelText,
                                     ScientificFormat format,
                                     Number initialValue )
    {
        //  Create a dialog window
        Frame frame = JOptionPane.getFrameForComponent( component );
        DecimalDialog dialog = new DecimalDialog( frame, title, labelText,
                                                  initialValue, format );
        dialog.setLocationRelativeTo( frame );
        dialog.setVisible( true );
        return dialog.getNumber();
    }

    /**
     * Constructor, not public as all instances should be create using
     * the "showDialog" method.
     */
    private DecimalDialog( Frame frame, String title, String labelText,
                           Number initialValue, ScientificFormat format )
    {
        super( frame, title, true );

        //  Buttons.
        JButton okButton = new JButton( "OK" );
        okButton.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    acceptValue();
                    closeWindow();
                }
            });

        JButton cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    closeWindow();
                }
            });
        getRootPane().setDefaultButton( okButton );

        //  Label and DecimalField.
        JPanel actionPane = new JPanel();
        JLabel label = new JLabel( labelText + ": " );
        decimalFormat = format;
        decimalField = new DecimalField( initialValue.doubleValue(), 10, decimalFormat );
        actionPane.setLayout( new BoxLayout( actionPane, BoxLayout.X_AXIS ) );
        actionPane.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        actionPane.add( Box.createHorizontalGlue() );
        actionPane.add( label );
        actionPane.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        actionPane.add( decimalField );

        //  Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.X_AXIS ) );
        buttonPane.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        buttonPane.add( Box.createHorizontalGlue() );
        buttonPane.add( cancelButton );
        buttonPane.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        buttonPane.add( okButton );

        // Put everything together, using the content pane's BorderLayout.
        getContentPane().add( actionPane, BorderLayout.NORTH );
        getContentPane().add( buttonPane, BorderLayout.SOUTH );
        pack();
    }

    /**
     * Get the Number representing users value.
     */
    protected Number getNumber()
    {
        if ( value != null ) {
            try {
                return decimalFormat.parse( value );
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Accept the value.
     */
    protected void acceptValue()
    {
        value = decimalField.getText();
    }

    /**
     * Close the window.
     */
    protected void closeWindow()
    {
        setVisible( false );
    }
}
