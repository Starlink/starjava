/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplayControl.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import java.awt.*;
import java.net.*;

import jsky.image.gui.ImageDisplayControl;
import jsky.image.gui.DivaMainImageDisplay;

/**
 * Extends the ImageDisplayControl class by adding support for
 * browsing catalogs and plotting catalog symbols on the image.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayControl extends ImageDisplayControl {

    /**
     * Construct a NavigatorImageDisplayControl widget.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     * @param size   the size (width, height) to use for the pan and zoom windows.
     */
    public NavigatorImageDisplayControl(Component parent, int size) {
        super(parent, size);
    }

    /**
     * Make a NavigatorImageDisplayControl widget with the default settings.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     */
    public NavigatorImageDisplayControl(Component parent) {
        super(parent);
    }


    /**
     * Make a NavigatorImageDisplayControl widget with the default settings and display the contents
     * of the image file pointed to by the URL.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param url The URL for the image to load
     */
    public NavigatorImageDisplayControl(Component parent, URL url) {
        super(parent, url);
    }


    /**
     * Make a NavigatorImageDisplayControl widget with the default settings and display the contents
     * of the image file.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param filename The image file to load
     */
    public NavigatorImageDisplayControl(Component parent, String filename) {
        super(parent, filename);
    }

    /** Make and return the image display window */
    protected DivaMainImageDisplay makeImageDisplay() {
        return new NavigatorImageDisplay(parent);
    }
}

