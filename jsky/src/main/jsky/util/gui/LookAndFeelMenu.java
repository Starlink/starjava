/*
 * ESO Archive
 *
 * $Id: LookAndFeelMenu.java,v 1.5 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/12/10  Created (based on JDK demo)
 */

package jsky.util.gui;

import jsky.image.gui.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Defines a standard "Look and Feel" menu item that applications
 * can add to a menu to allow the user to change the look and feel of the
 * application.
 */
public class LookAndFeelMenu extends JMenu {

    // Current ui
    private String currentUI = "Metal";

    // L&F radio buttons
    private JRadioButtonMenuItem macMenuItem;
    private JRadioButtonMenuItem metalMenuItem;
    private JRadioButtonMenuItem motifMenuItem;
    private JRadioButtonMenuItem windowsMenuItem;

    private static final String macClassName = "com.sun.java.swing.plaf.mac.MacLookAndFeel";
    private static final String metalClassName = "javax.swing.plaf.metal.MetalLookAndFeel";
    private static final String motifClassName = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
    private static final String windowsClassName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

    /**
     * Static list of top level windows (JFrames, JInternalFrames, etc.).
     * The application must call LookAndFeelMenu.addWindow() to add a frame to the list.
     */
    private static LinkedList windows = new LinkedList();


    /**
     * Create a Look and Feel menu with the given title.
     */
    public LookAndFeelMenu(String title) {
        super(title);

        ButtonGroup group = new ButtonGroup();
        ToggleUIListener toggleUIListener = new ToggleUIListener();

        metalMenuItem = (JRadioButtonMenuItem) add(new JRadioButtonMenuItem("Java Look and Feel"));
        metalMenuItem.setSelected(UIManager.getLookAndFeel().getName().equals("Metal"));
        //metalMenuItem.setSelected(true);
        metalMenuItem.setEnabled(isAvailableLookAndFeel(metalClassName));
        group.add(metalMenuItem);
        metalMenuItem.addItemListener(toggleUIListener);
        metalMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));

        motifMenuItem = (JRadioButtonMenuItem) add(new JRadioButtonMenuItem("Motif Look and Feel"));
        motifMenuItem.setSelected(UIManager.getLookAndFeel().getName().equals("CDE/Motif"));
        motifMenuItem.setEnabled(isAvailableLookAndFeel(motifClassName));
        group.add(motifMenuItem);
        motifMenuItem.addItemListener(toggleUIListener);
        motifMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));

        windowsMenuItem = (JRadioButtonMenuItem) add(new JRadioButtonMenuItem("Windows Style Look and Feel"));
        windowsMenuItem.setSelected(UIManager.getLookAndFeel().getName().equals("Windows"));
        windowsMenuItem.setEnabled(isAvailableLookAndFeel(windowsClassName));
        group.add(windowsMenuItem);
        windowsMenuItem.addItemListener(toggleUIListener);
        windowsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));

        macMenuItem = (JRadioButtonMenuItem) add(new JRadioButtonMenuItem("Macintosh Look and Feel"));
        macMenuItem.setSelected(UIManager.getLookAndFeel().getName().equals("Macintosh"));
        macMenuItem.setEnabled(isAvailableLookAndFeel(macClassName));
        group.add(macMenuItem);
        macMenuItem.addItemListener(toggleUIListener);
        macMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
    }


    /**
     * Create a "Look and Feel"  menu.
     */
    public LookAndFeelMenu() {
        this("Look and Feel");
    }


    /**
     * Create a "Look and Feel"  menu for the given frame
     */
    public LookAndFeelMenu(Component frame) {
        this("Look and Feel");
        addWindow(frame);
    }


    /**
     * Create a Look and Feel  menu with the given title for the given frame
     */
    public LookAndFeelMenu(String title, Component frame) {
        this(title);
        addWindow(frame);
    }


    /**
     * Add a top level window to the list of windows that need to be updated when the
     * look and feel is changed.
     */
    public static void addWindow(Component c) {
        windows.add(c);
    }


    /**
     * Return a list of top level windows (JFrames or JInternalFrames).
     */
    public static LinkedList getWindows() {
        return windows;
    }


    /**
     * A utility function that layers on top of the LookAndFeel's
     * isSupportedLookAndFeel() method. Returns true if the LookAndFeel
     * is supported. Returns false if the LookAndFeel is not supported
     * and/or if there is any kind of error checking if the LookAndFeel
     * is supported.
     * The L&F menu will use this method to detemine whether the various
     * L&F options should be active or inactive.
     */
    protected static boolean isAvailableLookAndFeel(String classname) {
        try { // Try to create a L&F given a String
            Class lnfClass = Class.forName(classname,true,
                                           Thread.currentThread().getContextClassLoader());
            LookAndFeel newLAF = (LookAndFeel) (lnfClass.newInstance());
            return newLAF.isSupportedLookAndFeel();
        }
        catch (Exception e) { // If ANYTHING weird happens, return false
            return false;
        }
    }


    /**
     * Switch the between the Windows, Motif, Mac, and the Java Look and Feel
     */
    class ToggleUIListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            ListIterator it = windows.listIterator(0);
            while (it.hasNext()) {
                Component root = (Component) it.next();
                root.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                try {
                    if (rb.isSelected()) {
                        if (rb.getText().equals("Windows Style Look and Feel")) {
                            currentUI = "Windows";
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                            SwingUtilities.updateComponentTreeUI(root);
                        }
                        else if (rb.getText().equals("Macintosh Look and Feel")) {
                            currentUI = "Macintosh";
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.mac.MacLookAndFeel");
                            SwingUtilities.updateComponentTreeUI(root);
                        }
                        else if (rb.getText().equals("Motif Look and Feel")) {
                            currentUI = "Motif";
                            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                            SwingUtilities.updateComponentTreeUI(root);
                        }
                        else if (rb.getText().equals("Java Look and Feel")) {
                            currentUI = "Metal";
                            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                            SwingUtilities.updateComponentTreeUI(root);
                        }
                    }
                }
                catch (UnsupportedLookAndFeelException exc) {
                    // Error - unsupported L&F
                    rb.setEnabled(false);
                    System.err.println("Unsupported LookAndFeel: " + rb.getText());

                    // Set L&F to JLF
                    try {
                        currentUI = "Metal";
                        metalMenuItem.setSelected(true);
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                        SwingUtilities.updateComponentTreeUI(root);
                    }
                    catch (Exception exc2) {
                        exc2.printStackTrace();
                        System.err.println("Could not load LookAndFeel: " + exc2);
                        exc2.printStackTrace();
                    }
                }
                catch (Exception exc) {
                    rb.setEnabled(false);
                    exc.printStackTrace();
                    System.err.println("Could not load LookAndFeel: " + rb.getText());
                    exc.printStackTrace();
                }

                root.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

        }
    }
}



