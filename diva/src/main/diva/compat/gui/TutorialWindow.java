/*
 * $Id: TutorialWindow.java,v 1.7 2002/05/16 21:20:05 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * TutorialWindow is a JFrame that is used to display tutorial examples.
 * It contains a menubar with a quit entry and a few other
 * useful bits.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 * @deprecated Use diva.gui.BasicFrame instead.
 */
public class TutorialWindow extends JFrame {
    // My menubar
    transient JMenuBar menubar = null;

    // Constructor -- create the Frame and give it a title.
    public TutorialWindow(String title) {
        super(title);

        // Create the menubar and set it
        setJMenuBar(createMenuBar());

        // Close the window on any window event
        addWindowListener(windowListener);
    }

    /** Create the menubar
     */
    public JMenuBar createMenuBar () {
        JMenuBar menubar;
        JMenu menuFile;
        JMenuItem itemClose;

        // Create the menubar and menus
        menubar = new JMenuBar();

        menuFile = new JMenu("File");
        menuFile.setMnemonic('F');

        // Create the menu items
        itemClose = menuFile.add(actionClose);
        itemClose.setMnemonic('C');
        itemClose.setToolTipText("Close this window");

        // Build the menus
        menubar.add(menuFile);

        return menubar;
    }

    /////////////////////////////////////////////////////////////////
    // The action classes
    //
    private transient Action actionClose = new AbstractAction ("Close") {
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    };

    /////////////////////////////////////////////////////////////////
    // The window listener
    //
    transient WindowListener windowListener = new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            actionClose.actionPerformed(null);
        }
        public void windowIconified (WindowEvent e) {
            System.out.println(e);
        }
        public void windowDeiconified (WindowEvent e) {
            System.out.println(e);
        }
    };
}


