/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewerFrame.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.html;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JFrame;

import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window for an HTMLViewer panel.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class HTMLViewerFrame extends JFrame {

    private HTMLViewer viewer;


    /**
     * Create a top level window containing an HTMLViewer panel.
     */
    public HTMLViewerFrame() {
        super("HTML Viewer");
        viewer = new HTMLViewer(this);
        GenericToolBar toolbar = new GenericToolBar(viewer);
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(viewer, BorderLayout.CENTER);
        setJMenuBar(new HTMLViewerMenuBar(viewer, toolbar));

        Preferences.manageLocation(this);
        Preferences.manageSize(viewer, new Dimension(600, 500));

        pack();
        setVisible(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /** Return the internal panel object */
    public HTMLViewer getHTMLViewer() {
        return viewer;
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java HTMLViewerFrame URL");
            System.exit(1);
        }

        try {
            URL url = new URL(args[0]);
            URLConnection connection = url.openConnection();
            String contentType = connection.getContentType();
            if (contentType == null || contentType.startsWith("text/html")) {
                HTMLViewerFrame f = new HTMLViewerFrame();
                f.getHTMLViewer().setPage(url);
            }
            else {
                System.out.println("error: URL content type is not text/html");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}



