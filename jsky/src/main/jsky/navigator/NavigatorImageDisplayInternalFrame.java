/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplayInternalFrame.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import javax.swing.JDesktopPane;

import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayControlInternalFrame;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.image.gui.ImageDisplayToolBar;

/**
 * Extends ImageDisplayControlInternalFrame to add catalog support.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayInternalFrame extends ImageDisplayControlInternalFrame {

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public NavigatorImageDisplayInternalFrame(JDesktopPane desktop, int size) {
        super(desktop, size);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public NavigatorImageDisplayInternalFrame(JDesktopPane desktop) {
        super(desktop);
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public NavigatorImageDisplayInternalFrame(JDesktopPane desktop, int size, String fileOrUrl) {
        super(desktop, size, fileOrUrl);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public NavigatorImageDisplayInternalFrame(JDesktopPane desktop, String fileOrUrl) {
        super(desktop, fileOrUrl);
    }

    /**
     * Set the instance of the catalog navigator to use with this image.
     */
    public void setNavigator(Navigator navigator) {
        NavigatorImageDisplay imageDisplay = (NavigatorImageDisplay) imageDisplayControl.getImageDisplay();
        imageDisplay.setNavigator(navigator);
    }

    /**
     * Return the instance of the catalog navigator used with this image.
     */
    public Navigator getNavigator() {
        NavigatorImageDisplay imageDisplay = (NavigatorImageDisplay) imageDisplayControl.getImageDisplay();
        return imageDisplay.getNavigator();
    }

    /** Make and return the menubar */
    protected ImageDisplayMenuBar makeMenuBar(DivaMainImageDisplay mainImageDisplay, ImageDisplayToolBar toolBar) {
        return new NavigatorImageDisplayMenuBar((NavigatorImageDisplay) mainImageDisplay,
                (NavigatorImageDisplayToolBar) toolBar);
    }

    /** Make and return the toolbar */
    protected ImageDisplayToolBar makeToolBar(DivaMainImageDisplay mainImageDisplay) {
        return new NavigatorImageDisplayToolBar((NavigatorImageDisplay) mainImageDisplay);
    }

    /**
     * Make and return the image display control frame.
     *
     * @param size the size (width, height) to use for the pan and zoom windows.
     */
    protected ImageDisplayControl makeImageDisplayControl(int size) {
        return new NavigatorImageDisplayControl(this, size);
    }
}

