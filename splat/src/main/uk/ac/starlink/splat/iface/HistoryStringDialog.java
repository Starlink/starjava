/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-AUG-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
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
import javax.swing.JComboBox;

/**
 * Create a dialog for obtaining a single {@link String} value.
 * A history of previously used values is retained in a drop-down menu.
 * Use the {@link #showDialog}" method to activate an instance of this
 * class, but unusually for a dialog you must create an instance, this is so
 * that the history element works.
 *
 * @author Peter W. Draper
 * @version $Id$
 */

public class HistoryStringDialog
    extends JDialog
{
    /** The JComboBox used to get value */
    protected JComboBox comboBox = new JComboBox();

    /** The accepted value, null if not accepted */
    protected String value = null;

    /**
     * Show a dialog window. Returning the user value.
     *
     * @param dialog an instance of this class
     *
     * @return the accepted value, or null if cancelled or the value
     *         is invalid.
     */
    public static String showDialog( HistoryStringDialog dialog )
    {
        dialog.setLocationRelativeTo( dialog.getOwner() );
        dialog.setVisible( true );
        return dialog.getValue();
    }

    /**
     * Constructor use the {@link showDialog} method to control.
     *
     * @param component a parent component for the dialog, may be null
     * @param title the title for the dialog window
     * @param labelText description of the value that is to be obtained
     */
    public HistoryStringDialog( Component component, String title, 
                                String labelText )
    {
        super( JOptionPane.getFrameForComponent( component ), title, true );

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

        //  Need to edit the JComboBox.
        comboBox.setEditable( true );

        //  Label and JComboBox
        JPanel actionPane = new JPanel();
        JLabel label = new JLabel( labelText + ": " );
        actionPane.setLayout( new BoxLayout( actionPane, BoxLayout.X_AXIS ) );
        actionPane.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        //actionPane.add( Box.createHorizontalGlue() );
        actionPane.add( label );
        actionPane.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        actionPane.add( comboBox );

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
     * Get the value.
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
        value = (String) comboBox.getSelectedItem();
    }

    /**
     * Close the window.
     */
    protected void closeWindow()
    {
        setVisible( false );
    }
}
