/*
 * ESO Archive
 *
 * $Id: ImageCutLevelsInternalFrame.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;

import jsky.util.I18N;
import jsky.util.Preferences;


/**
 * Provides a top level window for an ImageCutLevels panel.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class ImageCutLevelsInternalFrame extends JInternalFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageCutLevelsInternalFrame.class);

    // The GUI panel
    private ImageCutLevels imageCutLevels;


    /**
     * Create a top level window containing an ImageCutLevels panel.
     */
    public ImageCutLevelsInternalFrame(BasicImageDisplay imageDisplay) {
        super(_I18N.getString("imageCutLevels"), true, false, true, true);
        imageCutLevels = new ImageCutLevels(this, imageDisplay);
        getContentPane().add(imageCutLevels, BorderLayout.CENTER);
        pack();

        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        Preferences.manageSize(imageCutLevels, new Dimension(100, 100));
        setVisible(true);
    }

    /**
     * Update the display from the current image
     */
    public void updateDisplay() {
        imageCutLevels.updateDisplay();
    }
}

