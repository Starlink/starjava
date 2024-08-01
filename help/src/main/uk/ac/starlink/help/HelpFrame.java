/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     13-FEB-2004 (Peter W. Draper):
 *        Refactored into help package from SPLAT..
 */
package uk.ac.starlink.help;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.help.HelpSet;
import javax.help.HelpSetException;
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
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import uk.ac.starlink.help.images.ImageHolder;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.util.gui.BasicFontChooser;

/**
 * Creates a windows that displays a JavaHelp HelpSet. This is used in
 * preference to the default HelpBrowser in JavaHelp as we want to offer the
 * ability to select a font and to define a proxy server for hyperlinks that
 * leave the local system.
 * <p>
 * This class also offers an Action implementation for adding Help menus and
 * toolbar actions.
 * <p>
 * When closed this window will be hidden, not disposed. It is therefore
 * necessary that the user disposes of it when it is really no longer
 * required.
 * <p>
 * Example usage:
 * <pre>
 *    HelpFrame.addHelpSet( helpSetURL );
 *    HelpFrame.setHelpTitle( "Online help" );
 *    HelpFrame.createHelpMenu( "window-help", "Help on window",
 *                              "main-help", "Help on application",
 *                               menuBar, toolBar );
 * </pre>
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
    protected static HelpSet helpSet = null;

    /**
     * The window title.
     */
    protected static String title = "Help";

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
    protected BasicFontChooser fontChooser = null;

    /**
     * The proxy server dialog.
     */
    protected ProxySetupFrame proxyWindow = null;

    /**
     * Create an instance. Protected as there is usually only one 
     * instance of this class per application.
     *
     * @throws  HelpSetException  in case of error
     */
    @SuppressWarnings("this-escape")
    protected HelpFrame()
        throws HelpSetException
    {
        initMenus();
        initHelp();
        initFrame();
    }
    
    /**
     * Add a HelpSet, must do this at least once before a request to display
     * help is made.
     *
     * @param  helpSetURL  url of help set
     */
    public static void addHelpSet( URL helpSetURL )
    {
        try {
            if ( helpSet == null ) {
                helpSet = new HelpSet( null, helpSetURL );
            }
            else {
                helpSet.add( new HelpSet( null, helpSetURL ) );
            }
        }
        catch (HelpSetException e) {
            JOptionPane.showMessageDialog( null, e.getMessage(),
                                           "Error adding HelpSet",
                                           JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Set the title.
     *
     * @param  title  title
     */
    public static void setHelpTitle( String title )
    {
        HelpFrame.title = title;
        if ( helpFrame != null ) {
            helpFrame.setTitle( title );
        }
    }

    /**
     * Add a Help menu to a given menu bar and populate it with the
     * a window specific topic and an optional application-wide topic.
     * If a toolbar is given then the window specific topic item is added to
     * it.
     * <p>
     * Typically the application-wide topic would be to the head of the
     * complete documentation set.
     *
     * @param windowTopic the help system identifier for the window specific
     *                    help (null for none).
     * @param windowDescription description for the window specific menu item.
     * @param appTopic the help system identifier for application help (null
     *                 for none).
     * @param appDescription description for the application help.
     * @param menuBar menubar to add the Help menu
     * @param toolBar toolbar to add topic to (null for don't)
     *
     * @return the "Help" JMenu created.
     */
    public static JMenu createHelpMenu( String windowTopic,
                                        String windowDescription,
                                        String appTopic,
                                        String appDescription,
                                        JMenuBar menuBar,
                                        JToolBar toolBar )
    {
        //  Compatibility implementation, bindKeys is false.
        return createHelpMenu( windowTopic, windowDescription,
                               appTopic, appDescription,
                               menuBar, toolBar, false );
    }

    /**
     * Add a Help menu to a given menu bar and populate it with the
     * a window specific topic and an optional application-wide topic.
     * If a toolbar is given then the window specific topic item is added to
     * it.
     * <p>
     * Typically the application-wide topic would be to the head of the
     * complete documentation set.
     *
     * @param windowTopic the help system identifier for the window specific
     *                    help (null for none).
     * @param windowDescription description for the window specific menu item.
     * @param appTopic the help system identifier for application help (null
     *                 for none).
     * @param appDescription description for the application help.
     * @param menuBar menubar to add the Help menu
     * @param toolBar toolbar to add topic to (null for don't)
     * @param bindKeys if true add standard key bindings to accelerate help
     *                 (f1 and shift-f1) and standard alt-h to access Help
     *                 menu. 
     *
     * @return the "Help" JMenu created.
     */
    public static JMenu createHelpMenu( String windowTopic,
                                        String windowDescription,
                                        String appTopic,
                                        String appDescription,
                                        JMenuBar menuBar,
                                        JToolBar toolBar,
                                        boolean bindKeys )
    {
        //  Create the help menu and add it to the menubar.
        JMenu helpMenu = new JMenu( "Help" );
        if ( bindKeys ) {
            helpMenu.setMnemonic( KeyEvent.VK_H );
        }
        menuBar.add( helpMenu );

        //  Add an action to get help for the given topic.
        if ( windowTopic != null ) {
            Action helpAction = HelpFrame.getAction( windowDescription,
                                                     windowTopic );
            if ( bindKeys ) {
                helpAction.putValue( Action.ACCELERATOR_KEY, 
                                     KeyStroke.getKeyStroke( "shift F1" ) );
            }
            helpMenu.add( helpAction );

            // If a toolbar is given add the window specific help to that.
            if ( toolBar != null ) {
                toolBar.add( helpAction );
            }
        }

        //  Add an action for the application-wide help.
        if ( appDescription != null ) {
            Action topAction = HelpFrame.getAction( appDescription, appTopic );
            if ( bindKeys ) {
                topAction.putValue( Action.ACCELERATOR_KEY, 
                                    KeyStroke.getKeyStroke( "F1" ) );
            }
            helpMenu.add( topAction );
        }
        return helpMenu;
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
            catch ( HelpSetException e ) {
                JOptionPane.showMessageDialog(null, e.getMessage(),
                                              "Error initialising help system",
                                              JOptionPane.ERROR_MESSAGE);
            }

            //  We'd potentially like to know if the window is closed.
            helpFrame.addWindowListener( new WindowAdapter()
                {
                    public void windowClosed( WindowEvent evt ) {
                        helpClosed();
                    }
                });
        }

        //  Show the given page.
        helpFrame.showID( id );
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
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add an action to close the window.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        closeButton.setToolTipText( "Close window" );

        //  Create the options menu.
        optionsMenu.setText( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        FontAction fontAction = new FontAction( "Choose font..." );
        optionsMenu.add( fontAction );

        ProxyAction proxyAction = new ProxyAction( "Configure proxy..." );
        optionsMenu.add( proxyAction );
    }

    /**
     * Initialise the Help display components.
     *
     * @throws   HelpSetException   in case of error
     */
    protected void initHelp()
        throws HelpSetException
    {
        //  Initialise the HelpSet.
        helpComponent = new JHelp( helpSet );
        getContentPane().add( helpComponent, BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        super.setTitle( title );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 800, 500 ) );
        setVisible( true );
    }

    /**
     * Show a specific page in the help window. If null then the
     * default page is shown.
     *
     * @param  id  identifier of help entry to display
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
        helpFrame.setVisible( true );
    }
    private boolean fontSet = false;

    /**
     * Choose the text font.
     */
    protected void chooseFont()
    {
        if ( fontChooser == null ) {
            fontChooser = new BasicFontChooser( helpFrame, "Select Font",
                                                true );
        }
        fontChooser.setVisible( true );
        if ( fontChooser.accepted() ) {
            Font newFont = fontChooser.getSelectedFont();
            helpComponent.setFont( newFont );
            //  TODO: refresh to display new font everywhere.
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
        proxyWindow.setVisible( true );
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
    protected class FontAction
        extends AbstractAction
    {
        public FontAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            chooseFont();
        }
    }

    /**
     * Inner class defining Action for setting the proxy server.
     */
    protected class ProxyAction
        extends AbstractAction
    {
        public ProxyAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showProxy();
        }
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

    //
    // HelpAction implementation.
    //
    protected static ImageIcon helpImage =
        new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );

    /**
     *  Inner class defining HelpAction.
     */
    protected static class HelpAction
        extends AbstractAction
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
     *
     * @param   id   identifier of help item
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
