/*
 * ESO Archive
 *
 * $Id: FITSHDUChooserFrame.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an FITSHDUChooser panel.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class FITSHDUChooserFrame extends JFrame {

    private FITSHDUChooser fitsHDUChooser;


    /**
     * Create a top level window containing an FITSHDUChooser panel.
     */
    public FITSHDUChooserFrame(MainImageDisplay imageDisplay, FITSImage fitsImage) {
        super("Image Extensions");
        fitsHDUChooser = new FITSHDUChooser(this, imageDisplay, fitsImage);
        getContentPane().add(fitsHDUChooser, BorderLayout.CENTER);
        Preferences.manageLocation(this);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        pack();
        setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /** Return the internal panel object */
    public FITSHDUChooser getFitsHDUChooser() {
        return fitsHDUChooser;
    }
}

