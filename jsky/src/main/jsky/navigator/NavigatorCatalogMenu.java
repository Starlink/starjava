/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorCatalogMenu.java,v 1.10 2002/08/04 21:48:51 brighton Exp $
 */

package jsky.navigator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.CatalogNavigatorOpener;
import jsky.util.I18N;
import jsky.util.ProxyServerUtil;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProxyServerDialog;

/**
 * Implements a standard catalog menu with separate submenus for different
 * catalog types.
 *
 * @version $Revision: 1.10 $
 * @author Allan Brighton
 */
public class NavigatorCatalogMenu extends JMenu implements TreeModelListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(NavigatorCatalogMenu.class);

    /** Object responsible for creating and/or displaying the catalog window. */
    private CatalogNavigatorOpener _opener;

    /** Set to true if this menu is in the Catalog window menubar (doesn't have a Browse item) */
    private boolean _isInCatalogWindow = false;

    /** Catalog submenu */
    private JMenu _catalogMenu;

    /** Archive submenu */
    private JMenu _archiveMenu;

    /** Image server submenu */
    private JMenu _imageServerMenu;

    /** Local Catalog submenu */
    private JMenu _localCatalogMenu;

    /** The "Browse" menu item */
    private JMenuItem _browseMenuItem;

    /** The "Proxy Settings" menu item */
    private JMenuItem _proxyMenuItem;

    // Dialog window for proxy server settings
    private ProxyServerDialog _proxyDialog;

    // This restores any proxy settings from a previous session
    static {
        ProxyServerUtil.init();
    }

    /**
     * Create the menubar for the given main image display.
     *
     * @param opener the object responsible for creating and displaying the catalog window
     * @param navigator used to access the catalog tree for the "Reload" item
     * @param addBrowseItem if true, add the "Browse" menu item
     */
    public NavigatorCatalogMenu(CatalogNavigatorOpener opener, boolean addBrowseItem) {
        super(_I18N.getString("catalog"));
        _opener = opener;
        _isInCatalogWindow = !addBrowseItem;

        addMenuItems();
    }


    /** Add the catalog menu items. */
    public void addMenuItems() {
        CatalogDirectory dir = null;
        try {
	    dir = Navigator.getCatalogDirectory();
        }
        catch (Exception e) {
            DialogUtil.error(e);
            return;
        }

        // update menu when the config file changes
        dir.removeTreeModelListener(this);
        dir.addTreeModelListener(this);

        _catalogMenu = _createCatalogSubMenu(this, _catalogMenu, true, dir, Catalog.CATALOG, _I18N.getString("catalogs"));

        _archiveMenu = _createCatalogSubMenu(this, _archiveMenu, true, dir, Catalog.ARCHIVE, _I18N.getString("archives"));

        _imageServerMenu = _createCatalogSubMenu(this, _imageServerMenu, true, dir, Catalog.IMAGE_SERVER, _I18N.getString("imageServers"));

        _localCatalogMenu = _createCatalogSubMenu(this, _localCatalogMenu, true, dir, Catalog.LOCAL, _I18N.getString("localCats"));
        _localCatalogMenu.addSeparator();
        _localCatalogMenu.add(_createCatalogLocalOpenMenuItem());

        if (!_isInCatalogWindow && _browseMenuItem == null) {
            addSeparator();
            add(_browseMenuItem = _createCatalogBrowseMenuItem());
        }

	if (_proxyMenuItem == null) {
	    addSeparator();
	    add(_proxyMenuItem = _createProxySettingsMenuItem());
	}
    }


    /**
     * Create and return a submenu listing catalogs of the given type.
     *
     * @param parentMenu the menu to add the new menu to
     * @param oldMenu if not null, update this menu, otherwise create a new one
     * @param clearMenu if true and oldMenu is not null, clear out the old menu, otherwise add to it
     * @param dir the catalog directory (config file) reference
     * @param servType the server type string for the catalogs that should be in the menu
     * @param label the label for the submenu
     * @return the ne or updated menu
     */
    private JMenu _createCatalogSubMenu(JMenu parentMenu, JMenu oldMenu, boolean clearMenu, 
					CatalogDirectory dir, String servType, 
					String label) {
        JMenu menu = oldMenu;
        if (menu == null) {
            menu = new JMenu(label);
            parentMenu.add(menu);
        }
        else if (clearMenu) {
            menu.removeAll();
        }

        if (dir == null) {
            System.out.println("XXX null config file");
            return menu;
        }
        int n = dir.getNumCatalogs();
        for (int i = 0; i < n; i++) {
            Catalog cat = dir.getCatalog(i);
	    if (cat.getType().equals(servType)) {
		menu.add(_createCatalogMenuItem(cat));
	    }
	    /*
	    else if (cat instanceof CatalogDirectory) {
		//_createCatalogSubMenu(parentMenu, menu, false, (CatalogDirectory)cat, servType, label); 
		CatalogDirectory catDir = (CatalogDirectory)cat;
		JMenu m = _createCatalogSubMenu(menu, null, false, catDir, servType, catDir.getName());
		if (m.getItemCount() == 0)
		    menu.remove(m); 
	    }
	    */
        }
        return menu;
    }


    /**
     * Create a menu item for accessing a specific catalog.
     */
    private JMenuItem _createCatalogMenuItem(final Catalog cat) {
        JMenuItem menuItem = new JMenuItem(cat.getName());
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _opener.openCatalogWindow(cat);
            }
        });
        return menuItem;
    }


    /**
     * Create the Catalog => "Local Catalogs" => "Open..." menu item
     */
    private JMenuItem _createCatalogLocalOpenMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("open") + "...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _opener.openLocalCatalog();
            }
        });
        return menuItem;
    }


    /**
     * Create the Catalog => "Browse..." menu item
     */
    private JMenuItem _createCatalogBrowseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("browse") + "...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _opener.openCatalogWindow();
            }
        });
        return menuItem;
    }

    /**
     * Create the Catalog => "Proxy Settings..." menu item
     */
    private JMenuItem _createProxySettingsMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("proxySettings"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (_proxyDialog == null)
                    _proxyDialog = new ProxyServerDialog();
                _proxyDialog.setVisible(true);
            }
        });
        return menuItem;
    }


    // -- implement the TreeModelListener interface
    // (so we can update the menus whenever the catalog tree is changed)


    public void treeNodesChanged(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeNodesInserted(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        addMenuItems();
    }

    public void treeStructureChanged(TreeModelEvent e) {
        addMenuItems();
    }
}
