/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSKeywordsFrame.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */


package jsky.image.fits.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an FITSKeywords panel.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class FITSKeywordsFrame extends JFrame {

    private FITSKeywords fitsKeywords;


    /**
     * Create a top level window containing an FITSKeywords panel.
     */
    public FITSKeywordsFrame(MainImageDisplay imageDisplay) {
        super("FITS Keywords");
        fitsKeywords = new FITSKeywords(this, imageDisplay);
        getContentPane().add(fitsKeywords, BorderLayout.CENTER);
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
        fitsKeywords.updateDisplay();
    }

    /** Return the internal panel object */
    public FITSKeywords getFITSKeywords() {
        return fitsKeywords;
    }
}

