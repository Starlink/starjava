/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PickObjectFrame.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for a PickObject panel.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class PickObjectFrame extends JFrame {

    /** The main panel */
    private PickObject pickObject;


    /**
     * Create a top level window containing an PickObject panel.
     */
    public PickObjectFrame(MainImageDisplay imageDisplay) {
        super("Pick Objects");
        pickObject = new PickObject(this, imageDisplay);
        getContentPane().add(pickObject, BorderLayout.CENTER);
        pack();
        Preferences.manageLocation(this);
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /** Return the internal panel object */
    public PickObject getPickObject() {
        return pickObject;
    }
}

