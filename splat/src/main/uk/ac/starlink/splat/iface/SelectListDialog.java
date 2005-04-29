/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
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
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Dialog to offer the option to choose from a list of items.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SelectListDialog
    extends JDialog
    implements ActionListener
{
    private static SelectListDialog dialog = null;
    private static Object[] selection = null;
    private JList list = null;
    private JButton acceptButton = null;
    private JButton cancelButton = null;

    /**
     * Set up and show the dialog.
     *
     * @param comp a component that is associated with dialog.
     * @param labelText descriptive text for the list.
     * @param title title for the dialog window.
     * @param values the values to be selected. These objects should be
     *               Strings or have a toString method that shows a
     *               descriptive value.
     */
    public static Object[] showDialog( Component comp, String label,
                                       String title, Object[] values )
    {
        Frame frame = JOptionPane.getFrameForComponent( comp );
        selection = null;
        dialog = new SelectListDialog( frame, label, title, values );
        dialog.setVisible( true );
        return selection;
    }

    private SelectListDialog( Frame frame, String label, String title,
                              Object[] values )
    {
        super( frame, title, true );

        //  Buttons.
        cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( this );

        acceptButton = new JButton( "Accept" );
        acceptButton.setActionCommand( "Accept" );
        acceptButton.addActionListener( this );
        getRootPane().setDefaultButton( acceptButton );

        //  JList. Shows the values and allows multiple selections.
        list = new JList( values );
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //  JList goes in scroll pane.
        JScrollPane scroller = new JScrollPane( list );
        scroller.setPreferredSize( new Dimension( 250, 200 ) );

        //  Need panel for scroller so we can add the label.
        JPanel listPanel = new JPanel();
        listPanel.setLayout( new BoxLayout( listPanel, BoxLayout.PAGE_AXIS ) );
        JLabel listLabel = new JLabel( label );
        listLabel.setLabelFor( list );
        listPanel.add( listLabel );
        listPanel.add( Box.createRigidArea( new Dimension( 0, 5 ) ) );
        listPanel.add( scroller );
        listPanel.setBorder
            ( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

        // Lay out action buttons.
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout( new BoxLayout( actionPanel,
                                              BoxLayout.LINE_AXIS ) );
        actionPanel.setBorder
            ( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
        actionPanel.add( Box.createHorizontalGlue() );
        actionPanel.add( cancelButton );
        actionPanel.add( Box.createHorizontalGlue() );
        actionPanel.add( acceptButton );
        actionPanel.add( Box.createHorizontalGlue() );

        Container contentPane = getContentPane();
        contentPane.add( listPanel, BorderLayout.CENTER );
        contentPane.add( actionPanel, BorderLayout.SOUTH );
        pack();
    }

    //
    // Implement ActionListeners for buttons.
    //
    public void actionPerformed( ActionEvent e )
    {
        if ( e.getSource().equals( acceptButton  ) ) {
            selection = list.getSelectedValues();
        }
        dialog.setVisible( false );
    }
}
