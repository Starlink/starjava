/*
 * ESO Archive
 *
 * $Id: FITSHDUChooserInternalFrame.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import javax.swing.JInternalFrame;

import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**

 * Provides a top level window for an  FITSHDUChooser panel.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class FITSHDUChooserInternalFrame extends JInternalFrame {

    private FITSHDUChooser fitsHDUChooser;

    /**
     * Create a top level window containing an FITSHDUChooser panel.
     */
    public FITSHDUChooserInternalFrame(MainImageDisplay imageDisplay, FITSImage fitsImage) {
        super("Image Extensions", true, false, true, true);
        fitsHDUChooser = new FITSHDUChooser(this, imageDisplay, fitsImage);
        Preferences.manageLocation(this);
        getContentPane().add(fitsHDUChooser, BorderLayout.CENTER);
        pack();

        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }


    /** Return the internal panel object */
    public FITSHDUChooser getFitsHDUChooser() {
        return fitsHDUChooser;
    }
}

