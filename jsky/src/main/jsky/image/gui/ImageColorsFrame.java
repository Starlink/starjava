/*
 * ESO Archive
 *
 * $Id: ImageColorsFrame.java,v 1.7 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;


import java.awt.BorderLayout;

import javax.swing.JFrame;

import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an ImageColors panel.
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class ImageColorsFrame extends JFrame {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(ImageColorsFrame.class);

    // The GUI panel
    private ImageColors imageColors;


    /**
     * Create a top level window containing an ImageColors panel.
     */
    public ImageColorsFrame(BasicImageDisplay imageDisplay) {
        super(_I18N.getString("imageColors"));
        imageColors = new ImageColors(this, imageDisplay);
        getContentPane().add(imageColors, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /** Return the internal panel object */
    public ImageColors getImageColors() {
        return imageColors;
    }
}

