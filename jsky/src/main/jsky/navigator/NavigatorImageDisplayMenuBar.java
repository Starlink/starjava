/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorImageDisplayMenuBar.java,v 1.13 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.navigator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jsky.catalog.Catalog;
import jsky.image.gui.ImageDisplayMenuBar;
import jsky.util.I18N;
import jsky.util.Resources;

/**
 * Extends the image display menubar by adding a catalog menu.
 *
 * @version $Revision: 1.13 $
 * @author Allan Brighton
 */
public class NavigatorImageDisplayMenuBar extends ImageDisplayMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorImageDisplayMenuBar.class);

    /** Handle for the Image menu */
    private JMenu _catalogMenu;

    /** Handle for the Help menu */
    private JMenu _helpMenu;

    /**
     * Create the menubar for the given main image display.
     *
     * @param imageDisplay the target image display
     * @param toolBar the toolbar associated with this menubar (shares some actions)
     */
    public NavigatorImageDisplayMenuBar(NavigatorImageDisplay imageDisplay,
                                        NavigatorImageDisplayToolBar toolBar) {
        super(imageDisplay, toolBar);

        add(_catalogMenu = new NavigatorCatalogMenu(imageDisplay, true));

        // move the Pick Object menu item to the Catalog menu
        JMenuItem pickObjectMenuItem = getPickObjectMenuItem();
        getViewMenu().remove(pickObjectMenuItem);
        _catalogMenu.add(pickObjectMenuItem);

        _catalogMenu.addSeparator();
        _catalogMenu.add(createSaveCatalogOverlaysWithImageMenuItem());
    }


    /**
     * Create a menu item for saving the current catalog overlays as a FITS table in the image file.
     */
    protected JMenuItem createSaveCatalogOverlaysWithImageMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("saveCatalogWithImage"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                ((NavigatorImageDisplay) getImageDisplay()).saveCatalogOverlaysWithImage();
            }
        });
        return menuItem;
    }

    /**
     * Create the Help menu.
     */
    protected JMenu createHelpMenu() {
        JMenu menu = new JMenu(_I18N.getString("help"));
        // XXX to be done...
        return menu;
    }

    /** Return the handle for the Catalog menu */
    public JMenu getCatalogMenu() {
        return _catalogMenu;
    }

    /** Return the handle for the Help menu */
    public JMenu getHelpMenu() {
        return _helpMenu;
    }
}
