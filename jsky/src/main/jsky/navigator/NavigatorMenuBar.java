/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorMenuBar.java,v 1.8 2002/08/04 21:48:51 brighton Exp $
 */

package jsky.navigator;

import javax.swing.JMenu;

import jsky.catalog.gui.CatalogNavigatorMenuBar;
import jsky.catalog.gui.CatalogTree;
import jsky.util.I18N;

/**
 * Extends the CatalogNavigatorMenuBar class by adding a "Catalog"
 * menu containing implementation specific items for selecting the catalogs
 * to display in the tree.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class NavigatorMenuBar extends CatalogNavigatorMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorMenuBar.class);

    /** Handle for the Help menu */
    private JMenu _helpMenu;


    /**
     * Create the menubar for the given Navigator panel.
     */
    public NavigatorMenuBar(Navigator navigator, NavigatorToolBar toolbar) {
        super(navigator, toolbar);
        //add(_helpMenu = createHelpMenu());
    }

    /** Add a catalog menu to the catalog navigator frame */
    protected JMenu createCatalogMenu() {
	Navigator navigator = (Navigator)getNavigator();
        JMenu menu = new NavigatorCatalogMenu(navigator, false);

	CatalogTree catalogTree = navigator.getCatalogTree();
	menu.addSeparator();
	menu.add(catalogTree.makeReloadMenuItem());

	menu.addSeparator();
	menu.add(catalogTree.getCutAction());
	menu.add(catalogTree.getCopyAction());
	menu.add(catalogTree.getPasteAction());

	menu.addSeparator();
	menu.add(catalogTree.getToTopAction());
	menu.add(catalogTree.getMoveUpAction());
	menu.add(catalogTree.getMoveDownAction());
	menu.add(catalogTree.getToBottomAction());

        return menu;
    }


    /**
     * Create the Help menu.
     */
    protected JMenu createHelpMenu() {
        JMenu menu = new JMenu(_I18N.getString("help"));
        // XXX to be done...
        return menu;
    }

    /** Return the handle for the Help menu */
    public JMenu getHelpMenu() {
        return _helpMenu;
    }
}

