/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TabbedPanelFrame.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jsky.util.Preferences;


/**
 * Provides a frame with a TabbedPanel and some dialog buttons.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class TabbedPanelFrame extends JFrame {

    private TabbedPanel _tabbedPanel;


    /**
     * Create a top level window containing a TabbedPanel.
     */
    public TabbedPanelFrame(String title) {
        super(title);
        _tabbedPanel = new TabbedPanel(this);
        getContentPane().add(_tabbedPanel, BorderLayout.CENTER);
        Preferences.manageLocation(this);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    public TabbedPanel getTabbedPanel() {
        return _tabbedPanel;
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        TabbedPanelFrame tpf = new TabbedPanelFrame("Test");
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        TabbedPanel tp = tpf.getTabbedPanel();
        JTabbedPane jtp = tp.getTabbedPane();
        jtp.add(new JPanel(), "Test1");
        jtp.add(new JPanel(), "Test2");
        tp.getOKButton().addActionListener(al);
        tp.getCancelButton().addActionListener(al);
        tpf.pack();
        tpf.setVisible(true);
    }
}

