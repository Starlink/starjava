/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProgressPanelDialog.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */


package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * Provides a top level window for an ProgressPanel panel.
 */
public class ProgressPanelDialog extends JDialog {

    private ProgressPanel progressPanel;


    /**
     * Create a top level window containing an ProgressPanel panel.
     *
     * @param parent the parent frame (may be null)
     * @param title the title to display in the dialog
     */
    public ProgressPanelDialog(String title, Frame parent) {
        super(parent, "Progress");

        // center dialog in screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - 150, screenSize.height / 2 - 100);

        progressPanel = new ProgressPanel(this, title);
        getContentPane().add(progressPanel, BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }


    /** Return the internal panel object */
    public ProgressPanel getProgressPanel() {
        return progressPanel;
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Test ProgressPanelDialog");
        JPanel panel = new JPanel();
        JButton start = new JButton("Show");
        JButton exit = new JButton("Exit");
        f.getContentPane().add(panel);
        panel.add(start);
        panel.add(exit);
        f.pack();
        f.setVisible(true);
        LookAndFeelMenu.addWindow(f);

        final ProgressPanel pp = ProgressPanel.makeProgressPanel("Download in progress...");

        start.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pp.start();
            }
        });
        exit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }
}


