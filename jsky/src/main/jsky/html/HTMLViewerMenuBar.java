/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewerMenuBar.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.html;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GenericToolBar;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Implements a menubar for an HTMLViewer.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class HTMLViewerMenuBar extends JMenuBar {

    /** Target panel */
    protected HTMLViewer htmlViewer;

    /** The toolbar associated with the target panel */
    protected GenericToolBar toolBar;

    /** Handle for the File menu */
    protected JMenu fileMenu;

    /** Handle for the View menu */
    protected JMenu viewMenu;

    /** Handle for the Go menu */
    protected JMenu goMenu;

    /**
     * The current HTML viewer window (for the Go/history menu, which may be shared by
     * multiple windows)
     */
    protected static HTMLViewer currentHTMLViewer;


    /**
     * Create the menubar for the given HTMLViewer panel
     */
    public HTMLViewerMenuBar(final HTMLViewer htmlViewer, GenericToolBar toolBar) {
        super();
        this.htmlViewer = htmlViewer;
        this.toolBar = toolBar;
        add(fileMenu = createFileMenu());
        add(viewMenu = createViewMenu());
        add(goMenu = createGoMenu(null));

        // Arrange to always set the current window to use,
        // since the same items may be in the menus of multiple windows
        goMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                currentHTMLViewer = htmlViewer;
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

        // keep the Go history menu up to date
        htmlViewer.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                goMenu.removeAll();
                createGoMenu(goMenu);
            }
        });
    }

    /**
     * Return the current viewer window (for the Go/history menu, which may be shared by
     * multiple windows);
     */
    public static HTMLViewer getCurrentHTMLViewer() {
        return currentHTMLViewer;
    }

    /**
     * Set the current viewer window (for the Go/history menu, which may be shared by
     * multiple windows);
     */
    public static void setCurrentHTMLViewer(HTMLViewer htmlViewer) {
        currentHTMLViewer = htmlViewer;
    }

    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(htmlViewer.getOpenAction());
        menu.addSeparator();
        menu.add(createFileOpenURLMenuItem());
        //menu.addSeparator();
        //menu.add(htmlViewer.getSaveAsAction());
        //menu.add(htmlViewer.getPrintAction());
        menu.addSeparator();
        menu.add(createFileCloseMenuItem());

        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        JMenuItem menuItem = new JMenuItem("Open URL...");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                htmlViewer.openURL();
            }
        });
        return menuItem;
    }

    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        JMenuItem menuItem = new JMenuItem("Close");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                htmlViewer.close();
            }
        });
        return menuItem;
    }


    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.add(createViewToolBarMenuItem());
        menu.add(createViewShowToolBarAsMenu());

        // Only add Look and Feel item if not using internal frames
        // (otherwise its in the main Image menu)
        //if (htmlViewer.getRootComponent() instanceof JFrame) {
        //    menu.addSeparator();
        //    menu.add(new LookAndFeelMenu());
        //}

        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Toolbar");

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowToolBar";

        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                toolBar.setVisible(rb.getState());
                if (rb.getState())
                    Preferences.set(prefName, "true");
                else
                    Preferences.set(prefName, "false");
            }
        });

        // check for a previous preference setting
        String pref = Preferences.get(prefName);
        if (pref != null)
            menuItem.setState(pref.equals("true"));
        else
            menuItem.setState(true);

        return menuItem;
    }

    /**
     * Create the View => "Show Toolbar As" menu
     */
    protected JMenu createViewShowToolBarAsMenu() {
        JMenu menu = new JMenu("Show Toolbar As");

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("Pictures and Text");
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("Pictures Only");
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem("Text Only");

        b1.setSelected(true);
        toolBar.setShowPictures(true);
        toolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowToolBarAs";

        ItemListener itemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    if (rb.getText().equals("Pictures and Text")) {
                        toolBar.setShowPictures(true);
                        toolBar.setShowText(true);
                        Preferences.set(prefName, "1");
                    }
                    else if (rb.getText().equals("Pictures Only")) {
                        toolBar.setShowPictures(true);
                        toolBar.setShowText(false);
                        Preferences.set(prefName, "2");
                    }
                    else if (rb.getText().equals("Text Only")) {
                        toolBar.setShowPictures(false);
                        toolBar.setShowText(true);
                        Preferences.set(prefName, "3");
                    }
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        String pref = Preferences.get(prefName);
        if (pref != null) {
            JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[]{null, b1, b2, b3};
            try {
                ar[Integer.parseInt(pref)].setSelected(true);
            }
            catch (Exception e) {
            }
        }

        return menu;
    }


    /**
     * Create the Go menu.
     */
    protected JMenu createGoMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu("Go");

        menu.add(htmlViewer.getBackAction());
        menu.add(htmlViewer.getForwAction());
        menu.addSeparator();
        htmlViewer.addHistoryMenuItems(menu);
        menu.addSeparator();
        menu.add(createGoClearHistoryMenuItem());

        return menu;
    }

    /**
     * Create the Go => "Clear History" menu item.
     */
    protected JMenuItem createGoClearHistoryMenuItem() {
        JMenuItem menuItem = new JMenuItem("Clear History");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                htmlViewer.clearHistory();
                goMenu.removeAll();
                createGoMenu(goMenu);
            }
        });
        return menuItem;
    }


    /** Return the handle for the File menu */
    public JMenu getFileMenu() {
        return fileMenu;
    }

    /** Return the handle for the View menu */
    public JMenu getViewMenu() {
        return viewMenu;
    }

    /** Return the handle for the Go menu */
    public JMenu getGoMenu() {
        return goMenu;
    }
}
