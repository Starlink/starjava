/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Creates a window that display the SPLAT HelpSet. This is used in
 * preference to the default HelpBrowser as we want to offer the
 * ability to select a font and to define a proxy server for
 * hyperlinks that leave the local system.
 * <p>
 * This class also offers an Action implementation for adding Help
 * menus and toolbar actions.
 * <p>
 * When closed this window will be hidden, not disposed. If this
 * therefore necessary that the user disposes of it when it is really
 * no longer required.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class HelpFrame
    extends JFrame
{
    /**
     * The one instance of this class.
     */
    private static HelpFrame helpFrame = null;

    /**
     * The HelpSet that contains the help system descriptions (TOC,
     * views etc.).
     */
    protected HelpSet helpSet = null;

    /**
     * Component view of the HelpSet.
     */
    protected JHelp helpComponent = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu optionsMenu = new JMenu();
    protected JMenuItem fontSelectMenu = new JMenuItem();

    /**
     * The font chooser.
     */
    protected JFontChooser fontChooser = null;

    /**
     * The proxy server dialog.
     */
    protected ProxySetupFrame proxyWindow = null;

    /**
     * Create an instance.
     */
    private HelpFrame() throws Exception
    {
        initMenus();
        initHelp();
        initFrame();
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Create the File menu.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Add an action to close the window.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        closeButton.setToolTipText( "Close window" );

        //  Create the options menu.
        optionsMenu.setText( "Options" );
        menuBar.add( optionsMenu );

        FontAction fontAction = new FontAction( "Choose font..." );
        optionsMenu.add( fontAction );

        ProxyAction proxyAction = new ProxyAction( "Configure proxy..." );
        optionsMenu.add( proxyAction );
    }

    /**
     * Initialise the HelpSet and display components.
     */
    protected void initHelp() throws Exception
    {
        //  Initialise the HelpSet.
        URL url = HelpFrame.class.getResource( "/HelpSet.hs" );
        if ( url == null ) {
            throw new SplatException( "Failed to locate help" );
        }
        else {
            helpSet = new HelpSet( null, url );
            helpComponent = new JHelp( helpSet );
            getContentPane().add( helpComponent, BorderLayout.CENTER );
        }
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Help" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 800, 500 ) );
        setVisible( true );
    }

    /**
     * Create the default help window and show a page. There is only
     * one instance of this class for the whole application.
     *
     * @param id the page to initially show, null defaults to default
     *           top page.
     */
    public static void showHelpWindow( String id )
    {
        //  Create the HelpFrame instance if needed.
        if ( helpFrame == null || ! helpFrame.isDisplayable() ) {
            try {
                helpFrame = new HelpFrame();
            }
            catch (Exception e ) {
                JOptionPane.showMessageDialog
                    ( null, e.getMessage(), "Error initialising help system",
                      JOptionPane.ERROR_MESSAGE );
            }

            //  We'd potentially like to know if the window is closed.
            helpFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        helpClosed();
                    }
                });
        }

        //  Show the given page.
        helpFrame.showID( id );
    }

    /**
     * Show a specific page in the help window. If null then the
     * default page is shown.
     */
    protected void showID( String id )
    {
        helpComponent.setCurrentID( id );

        // Default font looks ugly (poor space proportions for some
        // reason), so always set at least once for now.
        if ( ! fontSet ) {
            Font f = UIManager.getFont( "TextField.font" );
            if ( f != null ) {
                helpComponent.setFont( f );
            }
            fontSet = true;
        }
        //helpComponent.setFont( new Font( "SansSerif", Font.PLAIN, 12 ) );

        helpFrame.setVisible( true );
    }
    private boolean fontSet = false;

    /**
     * Choose the text font.
     */
    protected void chooseFont()
    {
        if ( fontChooser == null ) {
            fontChooser = new JFontChooser( helpFrame, "Select Font", true );
        }
        fontChooser.show();
        if ( fontChooser.accepted() ) {
            Font newFont = fontChooser.getSelectedFont();
            helpComponent.setFont( newFont );
            //  TODO: refresh to display new font.
        }
    }

    /**
     * Set the proxy server and port.
     */
    protected void showProxy()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.show();
    }

    /**
     *  The window is closed.
     */
    protected static void helpClosed()
    {
        // Nullify if method for closing switches to dispose, from
        // withdraw.
        // helpFrame = null;
    }

    /**
     *  Close the window.
     */
    protected static void closeWindowEvent()
    {
        if ( helpFrame != null ) {
            helpFrame.dispose();
        }
    }

    /**
     * Inner class defining Action for choosing a new font.
     */
    protected class FontAction extends AbstractAction
    {
        public FontAction( String name ) {
            super( name );
        }
        public void actionPerformed( ActionEvent ae ) {
            chooseFont();
        }
    }

    /**
     * Inner class defining Action for setting the proxy server.
     */
    protected class ProxyAction extends AbstractAction
    {
        public ProxyAction( String name ) {
            super( name );
        }
        public void actionPerformed( ActionEvent ae ) {
            showProxy();
        }
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent();
        }
    }

    /**
     * Add a Help menu to a given menu bar and populate it with the
     * standard items and a named topic id. If a toolbar is given then
     * the topic item is added to it.
     *
     * @param topic the help system identifier for the named topic.
     * @param description description for the menu item
     * @param menuBar menubar to add the Help menu
     * @param toolBar toolbar to add topic to (null for don't)
     *
     * @return the "Help" JMenu created.
     */
    public static JMenu createHelpMenu( String topic,
                                        String description,
                                        JMenuBar menuBar,
                                        JToolBar toolBar )
    {
        //  Create the help menu and add it to the menubar.
        JMenu helpMenu = new JMenu( "Help" );
        menuBar.add( helpMenu );

        //  Add an action to get help for the given topic.
        if ( topic != null ) {
            Action helpAction = HelpFrame.getAction( description, topic );
            helpMenu.add( helpAction );
            if ( toolBar != null ) {
                toolBar.add( helpAction );
            }
        }

        //  Add an action to the top of the help system.
        Action topAction = HelpFrame.getAction( "On SPLAT",
                                                "splat-help" );
        helpMenu.add( topAction );

        return helpMenu;
    }

//
// HelpAction implementation.
//
    protected static ImageIcon helpImage =
        new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );

    /**
     *  Inner class defining HelpAction.
     */
    protected static class HelpAction extends AbstractAction
    {
        protected String id = null;
        public HelpAction( String menuDescription, String id )
        {
            super( menuDescription, helpImage );
            this.id = id;
        }
        public void actionPerformed( ActionEvent ae )
        {
            showHelpEvent( id );
        }
    }

    /**
     *  Requested to show the action help topic.
     */
    protected static void showHelpEvent( String id )
    {
        HelpFrame.showHelpWindow( id );
    }

    /**
     * Return an action for displaying the help window with the given
     * topic. Use this method to get HelpActions.
     *
     * @param description the description to the topic
     * @param id the help system identifier for the topic
     *
     * @return an Action to display the help topic.
     */
    public static Action getAction( String description, String id )
    {
        return new HelpAction( description, id );
    }
}
