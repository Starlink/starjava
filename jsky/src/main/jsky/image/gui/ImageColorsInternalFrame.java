/*
 * ESO Archive
 *
 * $Id: ImageColorsInternalFrame.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;


import java.awt.BorderLayout;

import javax.swing.JInternalFrame;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an ImageColors panel.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class ImageColorsInternalFrame extends JInternalFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageColorsInternalFrame.class);

    // The GUI panel
    private ImageColors imageColors;


    /**
     * Create a top level window containing an ImageColors panel.
     */
    public ImageColorsInternalFrame(BasicImageDisplay imageDisplay) {
        super(_I18N.getString("imageColors"), true, false, true, true);
        imageColors = new ImageColors(this, imageDisplay);
        getContentPane().add(imageColors, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }


    /** Return the internal panel object */
    public ImageColors getImageColors() {
        return imageColors;
    }
}

