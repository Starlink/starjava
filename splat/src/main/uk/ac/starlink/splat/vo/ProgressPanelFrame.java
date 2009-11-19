/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     12-JAN-2007 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Frame for displaying a number of ProgressPanels, so that a series of 
 * concurrent downloads can be monitored.
 */
public class ProgressPanelFrame
    extends JFrame
{
    private ArrayList progressPanels = new ArrayList();
    private JPanel mainPanel = null;

    /**
     * Create a top level window. This is "empty" initially, use the
     * {@link addProgressPanel} method to start adding panels. The
     * overall progress will be halted when the close button is
     * pressed (each of the panels will be halted in turn).
     *
     */
    public ProgressPanelFrame( String title ) 
    {
        initUI();
        initMenus();
        initFrame( title );
    }

    protected void initUI()
    {
        getContentPane().setLayout( new BorderLayout() );

        mainPanel = new JPanel();
        mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS ) );

        JScrollPane scroller = new JScrollPane( mainPanel );
        getContentPane().add( scroller, BorderLayout.CENTER );
    }
    
    protected void initMenus()
    {
        //  Add the menuBar.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Action bar uses BoxLayout.
        JPanel actionBar = new JPanel();
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Get icons.
        Icon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add an action to close the window and stop all downloads.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        actionBar.add( Box.createGlue() );
        closeButton.setToolTipText( "Close window and halt downloads" );

        getContentPane().add( actionBar, BorderLayout.SOUTH );
    }


    protected void initFrame( String title )
    {
        setTitle( Utilities.getTitle( title ) );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        setSize( new Dimension( 600, 400 ) );
        setVisible( true );
    }

    /**
     * Add a ProgressPanel.
     */
    public void addProgressPanel( ProgressPanel panel )
    {
        progressPanels.add( panel );
        mainPanel.add( panel );
    }


    /**
     * Stop all progress panels.
     */
    protected void stopAll()
    {
        Iterator i = progressPanels.iterator();
        while ( i.hasNext() ) {
            ((ProgressPanel) i.next()).stop();
        }
    }

    /**
     * Close the window, stop all downloads and close.
     */
    protected void closeWindowEvent()
    {
        stopAll();
        this.dispose();
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }
}


