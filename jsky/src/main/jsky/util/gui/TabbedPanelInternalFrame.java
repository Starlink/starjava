/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TabbedPanelInternalFrame.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JInternalFrame;

import jsky.util.Preferences;


/**
 * Provides an internal frame for a TabbedPanel and some dialog buttons.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class TabbedPanelInternalFrame extends JInternalFrame {

    private TabbedPanel _tabbedPanel;

    /**
     * Create a top level window containing a TabbedPanel.
     */
    public TabbedPanelInternalFrame(String title) {
        super(title);
        _tabbedPanel = new TabbedPanel(this);
        getContentPane().add(_tabbedPanel, BorderLayout.CENTER);
        pack();
        setClosable(true);
        setResizable(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        Preferences.manageLocation(this);
        //Preferences.manageSize(_tabbedPanel, new Dimension(400, 450));
        setVisible(true);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    public TabbedPanel getTabbedPanel() {
        return _tabbedPanel;
    }
}

