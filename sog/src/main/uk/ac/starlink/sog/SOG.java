/*
 * ESO Archive
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 * $Id$
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 * Peter W. Draper 2002/05/10  Converted for testing in HDX (SOG not JSky)
 */
package uk.ac.starlink.sog;

import com.sun.media.jai.codec.ImageCodec;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import jsky.app.jskycat.JSkyCat;
import jsky.app.jskycat.JSkyCatVersion;

import jsky.image.gui.MainImageDisplay;
import jsky.image.gui.DivaMainImageDisplay;

import jsky.navigator.NavigatorFrame;
import jsky.navigator.NavigatorInternalFrame;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.BasicWindowMonitor;
import jsky.util.gui.DesktopUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.LookAndFeelMenu;

import uk.ac.starlink.jaiutil.HDXCodec;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;

/**
 * Main class for the SOG application.
 *
 * @author Peter W. Draper
 * @created June 11, 2002
 */
public class SOG
    extends JFrame
{
    // Used to access internationalized strings (see i18n/gui*.proprties)
    private final static I18N _I18N = I18N.getInstance( JSkyCat.class );

    /** Top level catalog directory */
    public final static String DEFAULT_URL =
        "http://archive.eso.org/skycat/skycat2.0.cfg";

    /** File selection dialog, when using internal frames */
    protected JFileChooser fileChooser;

    /** Main window, when using internal frames */
    protected static JDesktopPane desktop;

    /** The main image frame (or internal frame) */
    protected Component imageFrame;

    /** The menu bar */
    protected JMenuBar menuBar;

    // Add the Codec for reading HDX/NDX files.
    static {
        ImageCodec.registerCodec( new HDXCodec() );
    }

    /**
     * The last instance of SOG created (usually only one would be
     * present per-application
     */
    private static SOG instance = null;

    /**
     * Whether closing final window causes System.exit(). Switch off
     * for testing and when embedded.
     */
    private boolean doExit = true;

    /**
     * Default constructor for internal tests. Do not use.
     */
    public SOG()
    {
        this( null, false, false, 0, false, false );
    }

    /**
     * Create the SOG application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param internalFrames if true, use internal frames
     * @param showNavigator if true, display the catalog navigator on startup
     */
    public SOG( String imageFileOrUrl, boolean internalFrames,
                boolean showNavigator )
    {
        this( imageFileOrUrl, internalFrames, showNavigator, 0, true, false );
    }

    /**
     * Create the SOG application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    public SOG( String imageFileOrUrl )
    {
        this( imageFileOrUrl, false, false, 0, true, false );
    }

    /**
     * Create the SOG application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param internalFrames if true, use internal frames
     * @param showNavigator if true, display the catalog navigator on startup
     * @param portNum if not zero, listen on this port for remote control commnds
     * @param doExit whether application should exit on close
     * @param showPhotom whether to show the photometry button
     * @see SOGRemoteControl
     */
    public SOG( String imageFileOrUrl, boolean internalFrames,
                boolean showNavigator, final int portNum, boolean doExit,
                boolean showPhotom )
    {
        super( "SOG::JSky" );

        //  This is the last instance created.
        instance = this;
        this.doExit = doExit;

        //  Set whether to show any photometry buttons (needs webservices so
        //  generally not).
        SOGNavigatorImageDisplay.setPhotomEnabled( showPhotom );

        if ( internalFrames || desktop != null ) {
            makeInternalFrameLayout( showNavigator, imageFileOrUrl );
        }
        else {
            makeFrameLayout( showNavigator, imageFileOrUrl );
        }

        // XXX Replace Graphics menu.

        // Clean up on exit
        addWindowListener( new BasicWindowMonitor() );

        //  Startup remote control services.
        if ( portNum > 0 ) {
            try {
               SOGRemoteControl control = SOGRemoteControl.getInstance();
               control.setPortNumber( portNum );
               control.start();
            }
            catch (Exception e) {
               System.err.println( e.getMessage() );
            }
        }
    }

    /**
     * Get the last instance of SOG.
     *
     * @return The instance value
     */
    public static SOG getInstance()
    {
        return instance;
    }

    /**
     * Set whether the application will exit.
     */
    public void setDoExit( boolean exit )
    {
        doExit = exit;
        ((SOGNavigatorImageDisplay) getImageDisplay()).setDoExit( doExit );
    }

    /**
     * Return the JDesktopPane, if using internal frames, otherwise null
     *
     * @return The desktop value
     */
    public static JDesktopPane getDesktop()
    {
        return desktop;
    }

    /**
     * Set the JDesktopPane to use for top level windows, if using internal
     * frames
     *
     * @param dt The new desktop value
     */
    public static void setDesktop( JDesktopPane dt )
    {
        desktop = dt;
    }

    /**
     * Do the window layout using internal frames
     *
     * @param showNavigator if true, display the catalog navigator on startup
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeInternalFrameLayout( boolean showNavigator,
                                            String imageFileOrUrl )
    {
        boolean ownDesktop = false;

        // true if this class owns the desktop
        if ( desktop == null ) {
            setJMenuBar( makeMenuBar() );

            desktop = new JDesktopPane();
            desktop.setBorder( BorderFactory.createEtchedBorder() );
            DialogUtil.setDesktop( desktop );
            ownDesktop = true;

            //Make dragging faster:
            desktop.putClientProperty( "JDesktopPane.dragMode", "outline" );

            // fill the whole screen
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int w = (int) ( screen.width - 10 );
            int
                h = (int) ( screen.height - 10 );
            Preferences.manageSize( desktop, new Dimension( w, h ),
                                    getClass().getName() + ".size" );
            Preferences.manageLocation( this, 0, 0 );

            setDesktopBackground();
            setContentPane( desktop );
        }

        NavigatorInternalFrame navigatorFrame = null;
        SOGNavigatorImageDisplayInternalFrame imageFrame = null;
        if ( imageFileOrUrl != null || ! showNavigator ) {
            imageFrame =
                makeNavigatorImageDisplayInternalFrame( desktop,
                                                        imageFileOrUrl );
            this.imageFrame = imageFrame;
            desktop.add( imageFrame, JLayeredPane.DEFAULT_LAYER );
            desktop.moveToFront( imageFrame );
            imageFrame.setVisible( true );
            menuBar = imageFrame.getJMenuBar();
        }

        if ( showNavigator ) {
            if ( imageFrame != null ) {
                MainImageDisplay imageDisplay =
                    imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame =
                    makeNavigatorInternalFrame( desktop, imageDisplay );
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min( screen.width - imageFrame.getWidth(),
                                  navigatorFrame.getWidth() );
                imageFrame.setLocation( x, 0 );
                imageFrame.setNavigator( navigatorFrame.getNavigator() );
            }
            else {
                navigatorFrame = makeNavigatorInternalFrame( desktop, null );
            }
            desktop.add( navigatorFrame, JLayeredPane.DEFAULT_LAYER );
            desktop.moveToFront( navigatorFrame );
            navigatorFrame.setLocation( 0, 0 );
            navigatorFrame.setVisible( true );
        }

        if ( ownDesktop ) {
            // include this top level window in any future look and
            // feel changes
            LookAndFeelMenu.addWindow( this );

            pack();
            setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
            addWindowListener(
                new WindowAdapter()
                {
                    public void windowClosing( WindowEvent e )
                    {
                        exit();
                    }
                } );
            setVisible( true );
        }
        setTitle( getAppName() + " - version " + getAppVersion() );
    }

    /**
     * Do the window layout using normal frames
     *
     * @param showNavigator if true, display the catalog navigator on startup
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeFrameLayout( boolean showNavigator,
                                    String imageFileOrUrl )
    {
        NavigatorFrame navigatorFrame = null;
        SOGNavigatorImageDisplayFrame imageFrame = null;

        if ( imageFileOrUrl != null || ! showNavigator ) {
            imageFrame = makeNavigatorImageDisplayFrame( imageFileOrUrl );
            this.imageFrame = imageFrame;
            menuBar = imageFrame.getJMenuBar();
        }

        if ( showNavigator ) {
            if ( imageFrame != null ) {
                MainImageDisplay imageDisplay =
                    imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame = makeNavigatorFrame( imageDisplay );
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min( screen.width - imageFrame.getWidth(),
                                  navigatorFrame.getWidth() );
                imageFrame.setLocation( x, 0 );
                imageFrame.setNavigator( navigatorFrame.getNavigator() );
            }
            else {
                navigatorFrame = makeNavigatorFrame( null );
            }
            navigatorFrame.setLocation( 0, 0 );
            navigatorFrame.setVisible( true );
        }
    }


    /**
     * Make and return the application menubar (only used with internal
     * frames).
     */
    protected JMenuBar makeMenuBar()
    {
        return new SOGMenuBar( this );
    }

    /**
     * Make and return an internal frame for displaying the given image (may
     * be null).
     *
     * @param desktop used to display the internal frame
     * @param imageFileOrUrl specifies the iamge file or URL to display
     */
    protected SOGNavigatorImageDisplayInternalFrame
        makeNavigatorImageDisplayInternalFrame( JDesktopPane desktop,
                                                String imageFileOrUrl )
    {
        SOGNavigatorImageDisplayInternalFrame f =
            new SOGNavigatorImageDisplayInternalFrame( desktop,
                                                       imageFileOrUrl );
        DivaMainImageDisplay d = f.getImageDisplayControl().getImageDisplay();
        if ( d instanceof SOGNavigatorImageDisplay ) {
            ( (SOGNavigatorImageDisplay) d ).setDoExit( doExit );
        }
        d.setTitle( getAppName() );
        return f;
    }

    /**
     * Return the name of this application.
     *
     * @return The appName value
     */
    protected String getAppName()
    {
        return "SOG::JSky";
    }

    /**
     * Return the version number of this application as a String.
     *
     * @return The appVersion value
     */
    protected String getAppVersion()
    {
        return JSkyCatVersion.JSKYCAT_VERSION.substring( 5 );
    }

    /**
     * Make and return a frame for displaying the given image (may be null).
     *
     * @param imageFileOrUrl specifies the iamge file or URL to display
     * @return Description of the Return Value
     */
    protected SOGNavigatorImageDisplayFrame
        makeNavigatorImageDisplayFrame( String imageFileOrUrl )
    {
        SOGNavigatorImageDisplayFrame f =
            new SOGNavigatorImageDisplayFrame( imageFileOrUrl );
        DivaMainImageDisplay d = f.getImageDisplayControl().getImageDisplay();
        if ( d instanceof SOGNavigatorImageDisplay ) {
            ( (SOGNavigatorImageDisplay) d ).setDoExit( doExit );
        }
        d.setTitle( getAppName() + " - version " + getAppVersion() );
        f.setVisible( true );
        return f;
    }

    /**
     * Make and return an internal frame for displaying catalog information.
     *
     * @param desktop used to display the internal frame
     * @param imageDisplay used to display images from image servers
     * @return Description of the Return Value
     */
    protected NavigatorInternalFrame
        makeNavigatorInternalFrame( JDesktopPane desktop,
                                    MainImageDisplay imageDisplay )
    {
        NavigatorInternalFrame f =
            new NavigatorInternalFrame( desktop, imageDisplay );
        f.setVisible( true );
        return f;
    }

    /**
     * Make and return a frame for displaying catalog information.
     *
     * @param imageDisplay used to display images from image servers
     * @return Description of the Return Value
     */
    protected NavigatorFrame
        makeNavigatorFrame( MainImageDisplay imageDisplay )
    {
        return new NavigatorFrame( imageDisplay );
    }


    /** Set the desktop background pattern */
    protected void setDesktopBackground()
    {
        new DesktopUtil( desktop, "stars.gif" );
    }


    /**
     * Create and return a new file chooser to be used to select a
     * local catalog file to open.
     *
     * @return Description of the Return Value
     */
    public JFileChooser makeFileChooser()
    {
        BasicFileChooser fileChooser = new BasicFileChooser( false );

        BasicFileFilter configFileFilter =
            new BasicFileFilter( new String[]{"cfg"},
                                 _I18N.getString("catalogConfigFilesSkycat"));
        fileChooser.addChoosableFileFilter( configFileFilter );

        BasicFileFilter skycatLocalCatalogFilter =
            new BasicFileFilter( new String[]{"table", "tbl", "cat"},
                                 _I18N.getString("localCatalogFilesSkycat"));
        fileChooser.addChoosableFileFilter( skycatLocalCatalogFilter );

        BasicFileFilter fitsFilter =
            new BasicFileFilter( new String[]{"fit", "fits", "fts"},
                                 _I18N.getString( "fitsFileWithTableExt" ) );
        fileChooser.addChoosableFileFilter( fitsFilter );

        fileChooser.setFileFilter( fitsFilter );

        return fileChooser;
    }


    /**
     * Display a file chooser to select a filename to display in a new
     * internal frame.
     */
    public void open()
    {
        if ( fileChooser == null ) {
            fileChooser = makeFileChooser();
        }
        int option = fileChooser.showOpenDialog( this );
        if ( option == BasicFileChooser.APPROVE_OPTION &&
             fileChooser.getSelectedFile() != null ) {
            open( fileChooser.getSelectedFile().getAbsolutePath() );
        }
    }


    /**
     * Display the given file or URL in a new internal frame.
     *
     * @param fileOrUrl Description of the Parameter
     */
    public void open( String fileOrUrl )
    {
        if ( desktop != null ) {
            if ( fileOrUrl.endsWith( ".fits" ) ||
                 fileOrUrl.endsWith( ".fts" ) ) {
                SOGNavigatorImageDisplayInternalFrame frame =
                    new SOGNavigatorImageDisplayInternalFrame( desktop );
                desktop.add( frame, JLayeredPane.DEFAULT_LAYER );
                desktop.moveToFront( frame );
                frame.setVisible( true );
                frame.getImageDisplayControl().getImageDisplay().
                    setFilename( fileOrUrl );
            }
            else {
                NavigatorInternalFrame frame =
                    new NavigatorInternalFrame( desktop );
                frame.getNavigator().open( fileOrUrl );
                desktop.add( frame, JLayeredPane.DEFAULT_LAYER );
                desktop.moveToFront( frame );
                frame.setVisible( true );
            }
        }
    }


    /** Exit the application */
    public void exit()
    {
        if ( doExit ) {
            System.exit( 0 );
        }
    }


    /**
     * Return the main image frame (JFrame or JInternalFrame)
     *
     * @return The imageFrame value
     */
    public Component getImageFrame()
    {
        return imageFrame;
    }

    /**
     * Return the main image display
     *
     * @return The imageDisplay value
     */
    public MainImageDisplay getImageDisplay()
    {
        if ( imageFrame instanceof SOGNavigatorImageDisplayFrame ) {
            return ((SOGNavigatorImageDisplayFrame)imageFrame).
                getImageDisplayControl().getImageDisplay();
        }
        else if ( imageFrame instanceof 
                  SOGNavigatorImageDisplayInternalFrame ) {
            return ((SOGNavigatorImageDisplayInternalFrame)imageFrame).
                getImageDisplayControl().getImageDisplay();
        }
        return null;
    }

    /**
     * Convenience method to set the visibility of the image JFrame (or
     * JInternalFrame).
     *
     * @param visible The new imageFrameVisible value
     */
    public void setImageFrameVisible( boolean visible )
    {
        if ( imageFrame != null ) {
            imageFrame.setVisible( visible );

            if ( imageFrame instanceof JFrame ) {
                if ( visible ) {
                    ( (JFrame) imageFrame ).setState( Frame.NORMAL );
                }
            }
            else if ( imageFrame instanceof JInternalFrame ) {
                if ( visible ) {
                    JInternalFrame f = (JInternalFrame) imageFrame;
                    try {
                        f.setIcon( false );
                    }
                    catch ( PropertyVetoException e ) {
                    }
                    try {
                        f.setClosed( false );
                    }
                    catch ( PropertyVetoException e ) {
                    }
                }
            }
        }
    }


    /**
     * Usage: java [-Djsky.catalog.skycat.config=$SKYCAT_CONFIG]
     *             [-Djsky.util.logger.config=$LOG_CONFIG]
     *             SOG [-[no]internalframes]
     *             [-shownavigator]
     *             [-port portNumber]
     *             [imageFileOrUrl]
     * <p>
     * The <em>jsky.catalog.skycat.config</em> property defines the Skycat style catalog config file to use.
     * (The default uses the ESO Skycat config file).
     * <p>
     * The <em>jsky.util.logger.config</em> property defines the log4j config file to use for logging output.
     * (The default file is jsky/util/locConfig.prop).
     * <p>
     * If -internalframes is specified, internal frames are used.
     * The -nointernalframes option has the opposite effect.
     * <p>
     * If -shownavigator is specified, the catalog navigator window is displayed on startup.
     * <p>
     * The -port option causes the main image window to listen on a socket for client connections.
     * This can be used to remote control the application.
     * <p>
     * The imageFileOrUrl argument may be an image file or URL to load.
     *
     * @param args Description of the Parameter
     */
    public static void main( String args[] )
    {
        String imageFileOrUrl = null;
        boolean internalFrames = false;
        boolean showNavigator = false;
        boolean showPhotom = false;
        int portNum = 0;
        boolean ok = true;

        for ( int i = 0; i < args.length; i++ ) {
            if ( args[i].charAt( 0 ) == '-' ) {
                String opt = args[i];
                if ( opt.equals( "-internalframes" ) ) {
                    internalFrames = true;
                }
                else if ( opt.equals( "-nointernalframes" ) ) {
                    internalFrames = false;
                }
                else if ( opt.equals( "-shownavigator" ) ) {
                    showNavigator = true;
                }
                else if ( opt.equals( "-showphotom" ) ) {
                    showPhotom = true;
                }
                else if ( opt.equals( "-port" ) ) {
                    String arg = args[++i];
                    portNum = Integer.parseInt( arg );
                }
                else {
                    System.out.println( _I18N.getString( "unknownOption" ) + ": " + opt );
                    ok = false;
                    break;
                }
            }
            else {
                if ( imageFileOrUrl != null ) {
                    System.out.println( _I18N.getString( "specifyOneImageOrURL" ) + ": " + imageFileOrUrl );
                    ok = false;
                    break;
                }
                imageFileOrUrl = args[i];
            }
        }

        if ( ! ok ) {
            System.out.println
                ( "Usage: java [-Djsky.catalog.skycat.config=$SKYCAT_CONFIG]"+
                  " SOG [-[no]internalframes] [-shownavigator] "+
                  "[-port portNum] [-showphotom] [imageFileOrUrl]" );
            System.exit( 1 );
        }

        new SOG( imageFileOrUrl, internalFrames, showNavigator,
                 portNum, true, showPhotom );
    }
}
