/*
 * ESO Archive
 *
 * $Id: ImagePropertiesFrame.java,v 1.6 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;


import java.awt.*;
import javax.swing.*;

import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an ImageProperties panel.
 *
 * @version $Revision: 1.6 $
 * @author Allan Brighton
 */
public class ImagePropertiesFrame extends JFrame {

    private ImageProperties imageProperties;


    /**
     * Create a top level window containing an ImageProperties panel.
     */
    public ImagePropertiesFrame(MainImageDisplay imageDisplay) {
        super("Image Properties");
        imageProperties = new ImageProperties(this, imageDisplay);
        getContentPane().add(imageProperties, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /**
     * Update the display from the current image
     */
    public void updateDisplay() {
        imageProperties.updateDisplay();
    }

    /** Return the internal panel object */
    public ImageProperties getImageProperties() {
        return imageProperties;
    }
}

