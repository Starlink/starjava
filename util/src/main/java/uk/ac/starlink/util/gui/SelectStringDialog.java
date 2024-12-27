/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-DEC-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Create a dialog for obtaining a text string with the option of
 * selecting and including special characters. Use the "showDialog()"
 * method to activate.
 *
 * @author Peter W. Draper
 * @version $Id$
 */

public class SelectStringDialog
    extends JDialog
{
    /** The SelectText component */
    protected SelectTextField textField = null;

    /** The accepted value, null if not accepted */
    protected String value = null;

    /**
     * Show a dialog window. Returning the user value as a String.
     *
     * @param component a parent component for the dialog, may be null
     * @param title the title for the dialog window
     * @param labelText description of the value that is to be obtained
     * @param initialValue the initial value to show in the text area
     *
     * @return the accepted value, or null if cancelled or the value
     *         is invalid.
     */
    public static String showDialog( Component component, String title,
                                     String labelText, String initialValue )
    {
        //  Create a dialog window
        Frame frame = JOptionPane.getFrameForComponent( component );
        SelectStringDialog dialog = new SelectStringDialog( frame, title,
                                                            labelText,
                                                            initialValue );
        dialog.setLocationRelativeTo( frame );
        dialog.setVisible( true );
        return dialog.getValue();
    }

    /**
     * Constructor, not public as all instances should be create using
     * the "showDialog" method.
     */
    private SelectStringDialog( Frame frame, String title, String labelText,
                                String initialValue )
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

        //  SelectText.
        JPanel actionPane = new JPanel();

        SelectCharacters selectCharacters =
            new SelectCharacters( frame, "Select Characters", true,
                                  getFont() );
        textField = new SelectTextField( initialValue, selectCharacters );
        textField.setColumns( 25 );
        JLabel label = new JLabel( labelText + ": " );
        actionPane.setLayout( new BoxLayout( actionPane, BoxLayout.X_AXIS ) );
        actionPane.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        actionPane.add( Box.createHorizontalGlue() );
        actionPane.add( label );
        actionPane.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        actionPane.add( textField );

        textField.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                acceptValue();
                closeWindow();
            }
        });

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
     * Get the users value.
     */
    protected String getValue()
    {
        return value;
    }

    /**
     * Accept the value.
     */
    protected void acceptValue()
    {
        value = textField.getText();
    }

    /**
     * Close the window.
     */
    protected void closeWindow()
    {
        setVisible( false );
    }
}
