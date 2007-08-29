/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     27-AUG-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.MutableComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.util.gui.GridBagLayouter;

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
    implements ActionListener, KeyListener
{
    /** The JComboBox used to get value */
    protected JComboBox comboBox = new JComboBox( new UniqueModel() );

    /** The accepted value, null if not accepted */
    protected String value = null;

    /** Buttons */
    protected JButton okButton = new JButton( "OK" );
    protected JButton clearButton = new JButton( "Clear" );
    protected JButton cancelButton = new JButton( "Cancel" );

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
        okButton.addActionListener( this );
        cancelButton.addActionListener( this );
        clearButton.addActionListener( this );
        getRootPane().setDefaultButton( okButton );

        //  JComboBox editable and <return> accepts.
        comboBox.setEditable( true );

        comboBox.getEditor().getEditorComponent().addKeyListener( this );

        //  Make typical size longer.
        comboBox.setPrototypeDisplayValue
            ("                                                ");

        //  Label and JComboBox
        JPanel actionPane = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( actionPane, GridBagLayouter.SCHEME3 );
        layouter.add( labelText + ": " );
        layouter.add( comboBox, true );

        //  Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.X_AXIS ) );
        buttonPane.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        buttonPane.add( Box.createHorizontalGlue() );
        buttonPane.add( clearButton );
        buttonPane.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
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
        if ( value != null && ! value.equals( "" ) ) {
            return value;
        }
        return null;
    }

    /**
     * Accept the value.
     */
    protected void acceptValue()
    {
        String newValue = (String) comboBox.getEditor().getItem();
        if ( newValue != null ) {
            if ( ! newValue.equals( value ) ) {
                comboBox.addItem( value );
            }
        }
        value = newValue;
    }

    /**
     * Clear the value.
     */
    protected void clearValue()
    {
        value = null;
    }

    /**
     * Clear the field and value.
     */
    protected void clearField()
    {
        comboBox.getEditor().setItem( null );
    }

    /**
     * Close the window.
     */
    protected void closeWindow()
    {
        setVisible( false );
    }

    // ActionListener interface.
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        if ( source.equals( okButton ) ) {
            acceptValue();
            closeWindow();
        }
        else if ( source.equals( cancelButton ) ) {
            clearValue();
            closeWindow();
        }
        else if ( source.equals( clearButton ) ) {
            clearField();
        }
        return;
    }

    // KeyListener interface.

    public void keyPressed( KeyEvent e )
    {
        if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            acceptValue();
            closeWindow();
        }
    }
    public void keyReleased( KeyEvent e )
    {
        //  Do nothing.
    }
    public void keyTyped( KeyEvent e )
    {
        // Do nothing.
    }

    /**
     * ComboBoxModel that maintains a unique set of history values.
     */
    protected class UniqueModel
        extends AbstractListModel
        implements MutableComboBoxModel
    {
        private Vector vector = new Vector();
        private int selected = -1;

        public UniqueModel()
        {
            // Do nothing.
        }

        public Object getSelectedItem()
        {
            if ( selected != -1 ) {
                return vector.get( selected );
            }
            return null;
        }

        public void setSelectedItem( Object anItem )
        {
            selected = vector.indexOf( anItem );
        }

        public Object getElementAt( int index )
        {
            return vector.get( index );
        }

        public int getSize()
        {
            return vector.size();
        }

        //  Do not add Objects that are already stored (repeated Strings)
        //  or blank Strings.
        public void addElement( Object anItem )
        {
            if ( anItem == null ) {
                return;
            }
            if ( anItem instanceof String ) {
                if ( ( (String) anItem).equals( "" ) ) {
                    return;
                }
            }

            int i = vector.indexOf( anItem );
            if ( i == -1 ) {
                vector.add( anItem );
                int pos = vector.size() - 1;
                fireIntervalAdded( anItem, pos, pos );
            }
        }

        public void insertElementAt( Object anItem, int index )
        {
            int i = vector.indexOf( anItem );
            if ( i == -1 ) {
                vector.add( index, anItem );
                fireIntervalAdded( anItem, index, index );
            }
        }

        public void removeElement( Object anItem )
        {
            int pos = vector.indexOf( anItem );
            vector.remove( anItem );
            fireIntervalRemoved( anItem, pos, pos );
        }

        public void removeElementAt( int index )
        {
            Object anItem = vector.get( index );
            vector.remove( index );
            fireIntervalRemoved( anItem, index, index );
        }

    }
}
