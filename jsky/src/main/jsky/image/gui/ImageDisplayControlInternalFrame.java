/*
 * ESO Archive
 *
 * $Id: ImageDisplayControlInternalFrame.java,v 1.14 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/29  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;

/**
 * Provides a top level window (internal frame version) for an ImageDisplayControl panel.
 *
 * @version $Revision: 1.14 $
 * @author Allan Brighton
 */
public class ImageDisplayControlInternalFrame extends JInternalFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageDisplayControlInternalFrame.class);

    /** The frame's toolbar */
    protected ImageDisplayToolBar toolBar;

    /** Panel containing image display and controls */
    protected ImageDisplayControl imageDisplayControl;

    // These are used make new frames visible by putting them in different locations
    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public ImageDisplayControlInternalFrame(JDesktopPane desktop, int size) {
        super(_I18N.getString("imageDisplay"), true, false, true, true);

        imageDisplayControl = makeImageDisplayControl(size);
        imageDisplayControl.getImageDisplay().setDesktop(desktop);
        DivaMainImageDisplay mainImageDisplay = imageDisplayControl.getImageDisplay();
        ImageDisplayToolBar toolBar = makeToolBar(mainImageDisplay);
        Container contentPane = getContentPane();
        setJMenuBar(makeMenuBar(mainImageDisplay, toolBar));
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(imageDisplayControl, BorderLayout.CENTER);

        // set default window size and remember changes between sessions
        Preferences.manageLocation(this, xOffset * openFrameCount, yOffset * openFrameCount);
        Preferences.manageSize(imageDisplayControl, new Dimension(600, 500));
        openFrameCount++;

        pack();
        setResizable(true);
        setIconifiable(true);
        setClosable(true);
        setMaximizable(true);

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        //setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     *
     * @param desktop The JDesktopPane to add the frame to.
     */
    public ImageDisplayControlInternalFrame(JDesktopPane desktop) {
        this(desktop, ImagePanner.DEFAULT_SIZE);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param filename An image file to display.
     */
    public ImageDisplayControlInternalFrame(JDesktopPane desktop, int size, String filename) {
        this(desktop, size);

        if (filename != null) {
            imageDisplayControl.getImageDisplay().setFilename(filename);
        }
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel with the
     * default settings.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param filename An image file to display.
     */
    public ImageDisplayControlInternalFrame(JDesktopPane desktop, String filename) {
        this(desktop, ImagePanner.DEFAULT_SIZE, filename);
    }

    /** Return the ImageDisplayControl object (the main widget) */
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
}

