/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PickObjectInternalFrame.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import javax.swing.JInternalFrame;

import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for a PickObject panel.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class PickObjectInternalFrame extends JInternalFrame {

    /** The main panel */
    private PickObject pickObject;

    /**
     * Create a top level window containing an PickObject panel.
     */
    public PickObjectInternalFrame(MainImageDisplay imageDisplay) {
        super("Pick Object");
        pickObject = new PickObject(this, imageDisplay);
        getContentPane().add(pickObject, BorderLayout.CENTER);
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
    public PickObject getPickObject() {
        return pickObject;
    }
}

