/*
 * ESO Archive
 *
 * $Id: JSkyCat.java,v 1.24 2002/08/20 09:57:57 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.app.jskycat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;

import jsky.image.gui.MainImageDisplay;
import jsky.navigator.NavigatorFrame;
import jsky.navigator.NavigatorImageDisplayFrame;
import jsky.navigator.NavigatorImageDisplayInternalFrame;
import jsky.navigator.NavigatorInternalFrame;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.BasicWindowMonitor;
import jsky.util.gui.DesktopUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ExampleFileFilter;
import jsky.util.gui.LookAndFeelMenu;
import jsky.util.gui.SwingUtil;

/**
 * Main class for the JSkyCat application.
 */
public class JSkyCat extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(JSkyCat.class);

    /** File selection dialog, when using internal frames */
    protected JFileChooser fileChooser;

    /** Main window, when using internal frames */
    protected static JDesktopPane desktop;

    /** The main image frame (or internal frame) */
    protected Component imageFrame;


    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param internalFrames if true, use internal frames
     * @param showNavigator if true, display the catalog navigator on startup
     * @param portNum if not zero, listen on this port for remote control commnds
     *
     * @see JSkyCatRemoteControl
     */
    public JSkyCat(String imageFileOrUrl, boolean internalFrames, boolean showNavigator,
                   int portNum) {
        super("JSky");

        if (internalFrames || desktop != null) {
            makeInternalFrameLayout(showNavigator, imageFileOrUrl);
        }
        else {
            makeFrameLayout(showNavigator, imageFileOrUrl);
        }

        // Clean up on exit
        addWindowListener(new BasicWindowMonitor());

        if (portNum > 0) {
            try {
                new JSkyCatRemoteControl(portNum, this).start();
            }
            catch (IOException e) {
                DialogUtil.error(e);
            }
        }
    }

    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     * @param internalFrames if true, use internal frames
     * @param showNavigator if true, display the catalog navigator on startup
     */
    public JSkyCat(String imageFileOrUrl, boolean internalFrames, boolean showNavigator) {
        this(imageFileOrUrl, internalFrames, showNavigator, 0);
    }


    /**
     * Create the JSkyCat application class and display the contents of the
     * given image file or URL, if not null.
     *
     * @param imageFileOrUrl an image file or URL to display
     */
    public JSkyCat(String imageFileOrUrl) {
        this(imageFileOrUrl, false, false, 0);
    }

    /** Return the JDesktopPane, if using internal frames, otherwise null */
    public static JDesktopPane getDesktop() {
        return desktop;
    }

    /** Set the JDesktopPane to use for top level windows, if using internal frames */
    public static void setDesktop(JDesktopPane dt) {
        desktop = dt;
    }

    /**
     * Do the window layout using internal frames
     *
     * @param showNavigator if true, display the catalog navigator on startup
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeInternalFrameLayout(boolean showNavigator, String imageFileOrUrl) {
        boolean ownDesktop = false;   // true if this class owns the desktop
        if (desktop == null) {
            setJMenuBar(makeMenuBar());

            desktop = new JDesktopPane();
            desktop.setBorder(BorderFactory.createEtchedBorder());
            DialogUtil.setDesktop(desktop);
            ownDesktop = true;

            //Make dragging faster:
            desktop.putClientProperty("JDesktopPane.dragMode", "outline");

            // fill the whole screen
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int w = (int) (screen.width - 10),
                    h = (int) (screen.height - 10);
            Preferences.manageSize(desktop, new Dimension(w, h), getClass().getName() + ".size");
            Preferences.manageLocation(this, 0, 0);

            setDesktopBackground();
            setContentPane(desktop);
        }

        NavigatorInternalFrame navigatorFrame = null;
        NavigatorImageDisplayInternalFrame imageFrame = null;
        if (imageFileOrUrl != null || !showNavigator) {
            imageFrame = makeNavigatorImageDisplayInternalFrame(desktop, imageFileOrUrl);
            this.imageFrame = imageFrame;
            desktop.add(imageFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(imageFrame);
            imageFrame.setVisible(true);
        }

        if (showNavigator) {
            if (imageFrame != null) {
                MainImageDisplay imageDisplay = imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame = makeNavigatorInternalFrame(desktop, imageDisplay);
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min(screen.width - imageFrame.getWidth(), navigatorFrame.getWidth());
                imageFrame.setLocation(x, 0);
                imageFrame.setNavigator(navigatorFrame.getNavigator());
            }
            else {
                navigatorFrame = makeNavigatorInternalFrame(desktop, null);
            }
            desktop.add(navigatorFrame, JLayeredPane.DEFAULT_LAYER);
            desktop.moveToFront(navigatorFrame);
            navigatorFrame.setLocation(0, 0);
            navigatorFrame.setVisible(true);
        }

        if (ownDesktop) {
            // include this top level window in any future look and feel changes
            LookAndFeelMenu.addWindow(this);

            pack();
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    exit();
                }
            });
            setVisible(true);
        }
        setTitle(getAppName() + " - version " + getAppVersion());
    }

    /**
     * Do the window layout using normal frames
     *
     * @param showNavigator if true, display the catalog navigator on startup
     * @param imageFileOrUrl an image file or URL to display
     */
    protected void makeFrameLayout(boolean showNavigator, String imageFileOrUrl) {
        NavigatorFrame navigatorFrame = null;
        NavigatorImageDisplayFrame imageFrame = null;

        if (imageFileOrUrl != null || !showNavigator) {
            imageFrame = makeNavigatorImageDisplayFrame(imageFileOrUrl);
            this.imageFrame = imageFrame;
        }

        if (showNavigator) {
            if (imageFrame != null) {
                MainImageDisplay imageDisplay = imageFrame.getImageDisplayControl().getImageDisplay();
                navigatorFrame = makeNavigatorFrame(imageDisplay);
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int x = Math.min(screen.width - imageFrame.getWidth(), navigatorFrame.getWidth());
                imageFrame.setLocation(x, 0);
                imageFrame.setNavigator(navigatorFrame.getNavigator());
            }
            else {
                navigatorFrame = makeNavigatorFrame(null);
            }
            navigatorFrame.setLocation(0, 0);
            navigatorFrame.setVisible(true);
        }
    }


    /** Make and return the application menubar (used when internal frames are in use) */
    protected JMenuBar makeMenuBar() {
        return new JSkyCatMenuBar(this);
    }


    /**
     * Make and return an internal frame for displaying the given image (may be null).
     *
     * @param desktop used to display the internal frame
     * @param imageFileOrUrl specifies the iamge file or URL to display
     */
    protected NavigatorImageDisplayInternalFrame makeNavigatorImageDisplayInternalFrame(JDesktopPane desktop, String imageFileOrUrl) {
        NavigatorImageDisplayInternalFrame f = new NavigatorImageDisplayInternalFrame(desktop, imageFileOrUrl);
        f.getImageDisplayControl().getImageDisplay().setTitle(getAppName());
        return f;
    }

    /** Return the name of this application. */
    protected String getAppName() {
        return "JSkyCat";
    }

    /** Return the version number of this application as a String. */
    protected String getAppVersion() {
        return JSkyCatVersion.JSKYCAT_VERSION.substring(5);
    }

    /**
     * Make and return a frame for displaying the given image (may be null).
     *
     * @param imageFileOrUrl specifies the iamge file or URL to display
     */
    protected NavigatorImageDisplayFrame makeNavigatorImageDisplayFrame(String imageFileOrUrl) {
        NavigatorImageDisplayFrame f = new NavigatorImageDisplayFrame(imageFileOrUrl);
        f.getImageDisplayControl().getImageDisplay().setTitle(getAppName() + " - version " + getAppVersion());
        f.setVisible(true);
        return f;
    }

    /**
     * Make and return an internal frame for displaying catalog information.
     *
     * @param desktop used to display the internal frame
     * @param imageDisplay used to display images from image servers
     */
    protected NavigatorInternalFrame makeNavigatorInternalFrame(JDesktopPane desktop, MainImageDisplay imageDisplay) {
        NavigatorInternalFrame f = new NavigatorInternalFrame(desktop, imageDisplay);
        f.setVisible(true);
        return f;
    }

    /**
     * Make and return a frame for displaying catalog information.
     *
     * @param imageDisplay used to display images from image servers
     */
    protected NavigatorFrame makeNavigatorFrame(MainImageDisplay imageDisplay) {
        return new NavigatorFrame(imageDisplay);
    }


    /** Set the desktop background pattern */
    protected void setDesktopBackground() {
        new DesktopUtil(desktop, "stars.gif");
    }


    /**
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    public JFileChooser makeFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));

        ExampleFileFilter configFileFilter = new ExampleFileFilter(new String[]{"cfg"},
                _I18N.getString("catalogConfigFilesSkycat"));
        fileChooser.addChoosableFileFilter(configFileFilter);

        ExampleFileFilter skycatLocalCatalogFilter = new ExampleFileFilter(new String[]{"table", "tbl", "cat"},
                _I18N.getString("localCatalogFilesSkycat"));
        fileChooser.addChoosableFileFilter(skycatLocalCatalogFilter);

        ExampleFileFilter fitsFilter = new ExampleFileFilter(new String[]{"fit", "fits", "fts"},
                _I18N.getString("fitsFileWithTableExt"));
        fileChooser.addChoosableFileFilter(fitsFilter);

        fileChooser.setFileFilter(fitsFilter);

        return fileChooser;
    }


    /**
     * Display a file chooser to select a filename to display in a new internal frame.
     */
    public void open() {
        if (fileChooser == null) {
            fileChooser = makeFileChooser();
        }
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
            open(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


    /**
     * Display the given file or URL in a new internal frame.
     */
    public void open(String fileOrUrl) {
        if (desktop != null) {
            if (fileOrUrl.endsWith(".fits") || fileOrUrl.endsWith(".fts")) {
                NavigatorImageDisplayInternalFrame frame = new NavigatorImageDisplayInternalFrame(desktop);
                desktop.add(frame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(frame);
                frame.setVisible(true);
                frame.getImageDisplayControl().getImageDisplay().setFilename(fileOrUrl);
            }
            else {
                NavigatorInternalFrame frame = new NavigatorInternalFrame(desktop);
                frame.getNavigator().open(fileOrUrl);
                desktop.add(frame, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(frame);
                frame.setVisible(true);
            }
        }
    }


    /**
     * Exit the application with the given status.
     */
    public void exit() {
        System.exit(0);
    }


    /** Return the main image frame (JFrame or JInternalFrame) */
    public Component getImageFrame() {
        return imageFrame;
    }

    /** Return the main image display */
    protected MainImageDisplay getImageDisplay() {
        if (imageFrame instanceof NavigatorImageDisplayFrame)
            return ((NavigatorImageDisplayFrame) imageFrame).getImageDisplayControl().getImageDisplay();
        else if (imageFrame instanceof NavigatorImageDisplayInternalFrame)
            return ((NavigatorImageDisplayInternalFrame) imageFrame).getImageDisplayControl().getImageDisplay();
        return null;
    }

    /**
     * Convenience method to set the visibility of the image JFrame (or JInternalFrame).
     */
    public void setImageFrameVisible(boolean visible) {
        if (imageFrame != null) {
            imageFrame.setVisible(visible);

            if (visible)
                SwingUtil.showFrame(imageFrame);
        }
    }


    /**
     * The main class of the JSkyCat application.
     * <p>
     * Usage: java [-Djsky.catalog.skycat.config=$SKYCAT_CONFIG]
     *             [-Djsky.util.logger.config=$LOG_CONFIG]
     *             JSkyCat [-[no]internalframes]
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
     * (The default is to use internal frames under Windows only).
     * <p>
     * If -shownavigator is specified, the catalog navigator window is displayed on startup.
     * <p>
     * The -port option causes the main image window to listen on a socket for client connections.
     * This can be used to remote control the application.
     * <p>
     * The imageFileOrUrl argument may be an image file or URL to load.
     */
    public static void main(String args[]) {
        String imageFileOrUrl = null;
        boolean internalFrames = (File.separatorChar == '\\');
        boolean showNavigator = false;
        int portNum = 0;
        boolean ok = true;
	int tilecache = 64;

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                String opt = args[i];
                if (opt.equals("-internalframes")) {
                    internalFrames = true;
                }
                else if (opt.equals("-nointernalframes")) {
                    internalFrames = false;
                }
                else if (opt.equals("-shownavigator")) {
                    showNavigator = true;
                }
                else if (opt.equals("-port")) {
                    String arg = args[++i];
                    portNum = Integer.parseInt(arg);
                }
		else if (opt.equals("-tilecache")) {
		    try {
			tilecache = Integer.parseInt(args[++i]);
		    }
		    catch(NumberFormatException e) {
			System.out.println("Warning: bad value for -tilecache option: " + args[i]);
		    }
		}
                else {
                    System.out.println(_I18N.getString("unknownOption") + ": " + opt);
                    ok = false;
                    break;
                }
            }
            else {
                if (imageFileOrUrl != null) {
                    System.out.println(_I18N.getString("specifyOneImageOrURL") + ": " + imageFileOrUrl);
                    ok = false;
                    break;
                }
                imageFileOrUrl = args[i];
            }
        }

        if (!ok) {
            System.out.println("Usage: java [-Djsky.catalog.skycat.config=$SKYCAT_CONFIG] JSkyCat [-[no]internalframes] [-shownavigator] [-port portNum] [imageFileOrUrl]");
            System.exit(1);
        }
	
	TileCache cache = JAI.getDefaultInstance().getTileCache();
	cache.setMemoryCapacity(tilecache * 1024 * 1024);
	//new tilecachetool.TCTool();

        new JSkyCat(imageFileOrUrl, internalFrames, showNavigator, portNum);
    }
}

