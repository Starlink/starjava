/*
 * ESO Archive
 *
 * $Id: ImageDisplayControlFrame.java,v 1.14 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an ImageDisplayControl panel.
 *
 * @version $Revision: 1.14 $
 * @author Allan Brighton
 */
public class ImageDisplayControlFrame extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayControlFrame.class);

    /** The frame's toolbar */
    protected ImageDisplayToolBar toolBar;

    /** Panel containing image display and controls */
    protected ImageDisplayControl imageDisplayControl;

    /** Count of instances of thiss class */
    private static int openFrameCount = 0;

    /** Used to make new frames visible by putting them in different locations */
    private static final int xOffset = 30, yOffset = 30;


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public ImageDisplayControlFrame(int size) {
        super(_I18N.getString("imageDisplay"));

        imageDisplayControl = makeImageDisplayControl(size);
        final DivaMainImageDisplay mainImageDisplay = imageDisplayControl.getImageDisplay();
        toolBar = makeToolBar(mainImageDisplay);
        setJMenuBar(makeMenuBar(mainImageDisplay, toolBar));

        Container contentPane = getContentPane();
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(imageDisplayControl, BorderLayout.CENTER);

        imageDisplayControl.setBorder(BorderFactory.createEtchedBorder());

        // set default window size and remember changes between sessions
        Preferences.manageLocation(this, xOffset * openFrameCount, yOffset * openFrameCount);
        Preferences.manageSize(imageDisplayControl, new Dimension(600, 500));
        openFrameCount++;

        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                mainImageDisplay.close();
            }

            public void windowClosed(WindowEvent e) {
                if (--openFrameCount == 0 && mainImageDisplay.isMainWindow())
                    mainImageDisplay.exit();
            }
        });
        //setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public ImageDisplayControlFrame() {
        this(ImagePanner.DEFAULT_SIZE);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public ImageDisplayControlFrame(int size, String fileOrUrl) {
        this(size);

        if (fileOrUrl != null) {
            imageDisplayControl.getImageDisplay().setFilename(fileOrUrl);
        }
        else {
            imageDisplayControl.getImageDisplay().blankImage(0., 0.);
        }
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public ImageDisplayControlFrame(String fileOrUrl) {
        this(ImagePanner.DEFAULT_SIZE, fileOrUrl);
    }


    /** Return the internal ImageDisplayControl panel */
    public ImageDisplayControl getImageDisplayControl() {
        return imageDisplayControl;
    }


    /** Make and return the toolbar */
    protected ImageDisplayToolBar makeToolBar(DivaMainImageDisplay mainImageDisplay) {
        return new ImageDisplayToolBar(mainImageDisplay);
    }

    /** Make and return the menubar */
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new ImageDisplayMenuBar(mainImageDisplay, toolBar);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    protected ImageDisplayControl makeImageDisplayControl(int size) {
        return new ImageDisplayControl(this, size);
    }

    /**
     * usage: java ImageDisplayControlFrame [fileOrUrl]
     */
    public static void main(String[] args) {
        String fileOrUrl = null;
        int size = ImagePanner.DEFAULT_SIZE;

        if (args.length >= 1)
            fileOrUrl = args[0];

        ImageDisplayControlFrame frame = new ImageDisplayControlFrame(size, fileOrUrl);
        frame.setVisible(true);
    }
}

