/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplayFrame.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import jsky.image.gui.DivaMainImageDisplay;
import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.ImageDisplayControlFrame;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.image.gui.ImageDisplayToolBar;

/**
 * Extends ImageDisplayControlFrame to add catalog support.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayFrame extends ImageDisplayControlFrame {

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public NavigatorImageDisplayFrame(int size) {
        super(size);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel
     * with the default settings.
     */
    public NavigatorImageDisplayFrame() {
        super();
    }


    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param size   the size (width, height) to use for the pan and zoom windows.
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public NavigatorImageDisplayFrame(int size, String fileOrUrl) {
        super(size, fileOrUrl);
    }

    /**
     * Create a top level window containing an ImageDisplayControl panel.
     *
     * @param fileOrUrl The file name or URL of an image to display.
     */
    public NavigatorImageDisplayFrame(String fileOrUrl) {
        super(fileOrUrl);
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

