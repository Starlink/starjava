/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSKeywordsInternalFrame.java,v 1.5 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JInternalFrame;

import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an FITSKeywords panel.
 *
 * @version $Revision: 1.5 $
 * @author Allan Brighton
 */
public class FITSKeywordsInternalFrame extends JInternalFrame {

    private FITSKeywords fitsKeywords;


    /**
     * Create a top level window containing an FITSKeywords panel.
     */
    public FITSKeywordsInternalFrame(MainImageDisplay imageDisplay) {
        super("FITS Keywords", true, false, true, true);
        fitsKeywords = new FITSKeywords(this, imageDisplay);
        fitsKeywords.setPreferredSize(new Dimension(500, 400));
        getContentPane().add(fitsKeywords, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setClosable(true);
        setIconifiable(false);
        setMaximizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);

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

