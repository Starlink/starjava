/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     05-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

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
import javax.swing.KeyStroke;

import org.astrogrid.samp.gui.GuiHubConnector;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Frame displaying SAMP status and controls.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public class SampFrame
    extends JFrame
{
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SampFrame.class );

    /** Initial window size and location. */
    private static final Rectangle defaultWindowLocation =
        new Rectangle( 0, 0, 550, 600 );

    /** Content pane of frame */
    private JPanel contentPane = null;

    /** Hub connector. */
    private GuiHubConnector hubConnector;

    /** Action buttons container. */
    protected JPanel actionBar = new JPanel();

    /**
     * Constructor.
     */
    public SampFrame( GuiHubConnector hubConnector )
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        this.hubConnector = hubConnector;
        initUI();
        initFrame();
    }

    /**
     * Initialise the main part of the user interface.
     */
    private void initUI()
    {
        //  Menubar and toolbars.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Help menu.
        HelpFrame.createHelpMenu( "samp-window", "Help on window",
                                  menuBar, null );

        //  Main part is a monitor panel derived from the connector.
        contentPane.add( hubConnector.createMonitorPanel(),
                         BorderLayout.CENTER );

        //  Action bar uses a BoxLayout and is placed at the south.
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Add an action to connect and/or start hub.
        Icon connectIcon =
            new ImageIcon( ImageHolder.class.getResource( "connect.gif" ) );
        Action connectAction =
            hubConnector.createRegisterOrHubAction( this, null );
        connectAction.putValue( Action.SMALL_ICON, connectIcon );
        actionBar.add( new JButton( connectAction ) );

        //  Add an action to close the window (appears in File menu
        //  and action bar).
        Icon closeIcon =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        Action closeAction = new CloseAction( "Close", closeIcon );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( closeButton );
    }

    /**
     * Inititalise frame properties.
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "SAMP Status" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, defaultWindowLocation, prefs,
                                    "SampFrame" );
        setVisible( true );
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "SampFrame" );
        this.dispose();
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
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
