/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewerInternalFrame.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.html;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JInternalFrame;

import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;
import jsky.util.gui.LookAndFeelMenu;

/**
 * Provides a top level window for an HTMLViewer panel.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class HTMLViewerInternalFrame extends JInternalFrame {

    private HTMLViewer viewer;


    /**
     * Create a top level window containing an HTMLViewer panel.
     */
    public HTMLViewerInternalFrame() {
        super("HTML Viewer", true, false, true, true);
        viewer = new HTMLViewer(this);
        GenericToolBar toolbar = new GenericToolBar(viewer);
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(viewer, BorderLayout.CENTER);
        setJMenuBar(new HTMLViewerMenuBar(viewer, toolbar));

        Preferences.manageLocation(this);
        Preferences.manageSize(viewer, new Dimension(600, 500));

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
    public HTMLViewer getHTMLViewer() {
        return viewer;
    }
}

