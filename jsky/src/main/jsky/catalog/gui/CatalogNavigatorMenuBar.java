/*
 * ESO Archive
 *
 * $Id: CatalogNavigatorMenuBar.java,v 1.22 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.catalog.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jsky.catalog.MemoryCatalog;
import jsky.catalog.TableQueryResult;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.gui.GenericToolBar;
import jsky.util.gui.LookAndFeelMenu;
import javax.swing.SwingUtilities;


/**
 * Implements a menubar for a CatalogNavigator.
 */
public class CatalogNavigatorMenuBar extends JMenuBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogNavigatorMenuBar.class);

    // names used to store settings in user preferences
    private final String _className = getClass().getName();
    private final String _viewToolBarPrefName = _className + ".ShowToolBar";
    private final String _showToolBarAsPrefName = _className + ".ShowToolBarAs";
    private final String _showCatalogTreePrefName = _className + ".CatalogTree";

    // Target panel 
    private CatalogNavigator _navigator;

    // The toolbar associated with the image display 
    private GenericToolBar _toolBar;

    // Handle for the File menu 
    private JMenu _fileMenu;

    // Handle for the View menu 
    private JMenu _viewMenu;

    // Handle for the Go menu 
    private JMenu _goMenu;

    // Handle for the catalog menu 
    private JMenu _catalogMenu;

    // Handle for the Table menu 
    private JMenu _tableMenu;

    // Toggle menu item needs to be updated based on current display 
    private JCheckBoxMenuItem _tableCellsEditableMenuItem;

    // The current catalog window (for the Go/history menu, which may be shared by
    // multiple windows)
    private static CatalogNavigator _currentCatalogNavigator;

    // Controls the visibility of the catalog tree
    private JCheckBoxMenuItem _showCatalogTreeMenuItem;
    
    // Used to show/hide the catalog tree for certain query components
    private static Hashtable _catalogTreeIsVisibleMap = new Hashtable();
    

    /**
     * Create the menubar for the given CatalogNavigator panel
     */
    public CatalogNavigatorMenuBar(CatalogNavigator navigator, GenericToolBar toolBar) {
        super();
        _navigator = navigator;
        _toolBar = toolBar;
        add(_fileMenu = createFileMenu());
        add(_viewMenu = createViewMenu());
        add(_goMenu = createGoMenu(null));
        add(_catalogMenu = createCatalogMenu());
        add(_tableMenu = createTableMenu());

        // Arrange to always set the current window for use by the CatalogHistoryItem class,
        // since the same items may be in the menus of multiple catalog windows
        _goMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                _currentCatalogNavigator = _navigator;
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });

        // Receive notification whenever a new catalog is displayed
        _navigator.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
		// keep the Go history menu up to date
		updateGoMenu();

		// Check if the catalog tree should be visible with this catalog
		updateCatalogTree();
            } 
        });
    }

    
    /** Update the Go menu after a change in the component displayed in the catalog navigator */
    protected void updateGoMenu() {
	_goMenu.removeAll();
	createGoMenu(_goMenu);
    }


    /** Update the catalog tree after a change in the component displayed in the catalog navigator */
    protected void updateCatalogTree() {
	// Check if the catalog tree should be visible with this catalog
	JComponent component = _navigator.getQueryComponent();
	if (component == null)
	    return;
	Boolean showTreeObj = (Boolean)_catalogTreeIsVisibleMap.get(component.getClass());
	boolean showTree = true;
	if (showTreeObj != null) {
	    showTree = showTreeObj.booleanValue();
	}
	else {
	    showTree = Preferences.get(_showCatalogTreePrefName, true);
	}
		
	if (showTree != _showCatalogTreeMenuItem.isSelected()) {
	    _showCatalogTreeMenuItem.setSelected(showTree);
	    _showCatalogTree(showTree, false);
	}
    }


    /**
     * Return the current catalog window (for the Go/history menu, which may be shared by
     * multiple catalog windows);
     */
    public static CatalogNavigator getCurrentCatalogNavigator() {
        return _currentCatalogNavigator;
    }

    /**
     * Set the current catalog window (for the Go/history menu, which may be shared by
     * multiple catalog windows);
     */
    public static void setCurrentCatalogNavigator(CatalogNavigator navigator) {
        _currentCatalogNavigator = navigator;
    }


    /**
     * Control the visibility of the catalog tree component, based on the given component class type.
     * Whenever a query component with the given class type is displayed in the 
     * catalog navigator window, the tree visibility will be set to the given argument 
     * (and later restored when a different catalog or result is displayed).
     * <p>
     * This method is included in this class, so that the state of the associated checkbox 
     * menu item can be kept up to date.
     */
    public static void setCatalogTreeIsVisible(Class c, boolean visible) {
	_catalogTreeIsVisibleMap.put(c, new Boolean(visible));
    }

    
    /**
     * Create the File menu.
     */
    protected JMenu createFileMenu() {
        JMenu menu = new JMenu(_I18N.getString("file"));
        menu.add(_navigator.getOpenAction());
        menu.addSeparator();
        menu.add(createFileOpenURLMenuItem());
        menu.add(createFileClearMenuItem());
        menu.addSeparator();
        menu.add(_navigator.getSaveAsAction());
        menu.add(_navigator.getSaveWithImageAction());
        menu.add(_navigator.getSaveAsHTMLAction());
        menu.add(_navigator.getPrintAction());
        menu.addSeparator();

        menu.add(createFileCloseMenuItem());

        // check if using internal frames before adding exit item
        JDesktopPane desktop = _navigator.getDesktop();
        if (desktop == null && _navigator.isMainWindow()) {
            menu.add(createFileExitMenuItem());
        }

        return menu;
    }


    /**
     * Create the File => "Open URL" menu item
     */
    protected JMenuItem createFileOpenURLMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("openURL") + "...");
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _navigator.openURL();
            }
        });
        return menuItem;
    }

    /**
     * Create the File => Clear menu item
     */
    protected JMenuItem createFileClearMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clear"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _navigator.clear();
            }
        });
        return menuItem;
    }


    /**
     * Create the File => Exit menu item
     */
    protected JMenuItem createFileExitMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("exit"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _navigator.exit();
            }
        });
        return menuItem;
    }

    /**
     * Create the File => Close menu item
     */
    protected JMenuItem createFileCloseMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("close"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _navigator.close();
            }
        });
        return menuItem;
    }


    /**
     * Create the View menu.
     */
    protected JMenu createViewMenu() {
        JMenu menu = new JMenu(_I18N.getString("view"));
        menu.add(createViewToolBarMenuItem());
        menu.add(createViewShowToolBarAsMenu());
        menu.add(createViewCatalogTreeMenu());

        // Only add Look and Feel item if not using internal frames
        // (otherwise its in the main Image menu)
        //if (_navigator.getRootComponent() instanceof JFrame) {
        //    menu.addSeparator();
        //    menu.add(new LookAndFeelMenu());
        //}

        return menu;
    }

    /**
     * Create the View => "Toolbar" menu item
     */
    protected JCheckBoxMenuItem createViewToolBarMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("toolbar"));

        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _toolBar.setVisible(rb.isSelected());
                if (rb.isSelected())
                    Preferences.set(_viewToolBarPrefName, "true");
                else
                    Preferences.set(_viewToolBarPrefName, "false");
            }
        });

        // check for a previous preference setting
        String pref = Preferences.get(_viewToolBarPrefName);
        if (pref != null)
            menuItem.setSelected(pref.equals("true"));
        else
            menuItem.setSelected(true);

        return menuItem;
    }

    /**
     * Create the View => "Show Toolbar As" menu
     */
    protected JMenu createViewShowToolBarAsMenu() {
        JMenu menu = new JMenu(_I18N.getString("showToolBarAs"));

        JRadioButtonMenuItem b1 = new JRadioButtonMenuItem(_I18N.getString("picAndText"));
        JRadioButtonMenuItem b2 = new JRadioButtonMenuItem(_I18N.getString("picOnly"));
        JRadioButtonMenuItem b3 = new JRadioButtonMenuItem(_I18N.getString("textOnly"));

        b1.setSelected(true);
        _toolBar.setShowPictures(true);
        _toolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        ItemListener itemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
                if (rb.isSelected()) {
                    if (rb.getText().equals(_I18N.getString("picAndText"))) {
                        _toolBar.setShowPictures(true);
                        _toolBar.setShowText(true);
                        Preferences.set(_showToolBarAsPrefName, "1");
                    }
                    else if (rb.getText().equals(_I18N.getString("picOnly"))) {
                        _toolBar.setShowPictures(true);
                        _toolBar.setShowText(false);
                        Preferences.set(_showToolBarAsPrefName, "2");
                    }
                    else if (rb.getText().equals(_I18N.getString("textOnly"))) {
                        _toolBar.setShowPictures(false);
                        _toolBar.setShowText(true);
                        Preferences.set(_showToolBarAsPrefName, "3");
                    }
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        String pref = Preferences.get(_showToolBarAsPrefName);
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

    /** Create the View => "Catalog Tree" menu item. */
    protected JCheckBoxMenuItem createViewCatalogTreeMenu() {
        _showCatalogTreeMenuItem = new JCheckBoxMenuItem(_I18N.getString("catalogTree"));

        _showCatalogTreeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _showCatalogTree(_showCatalogTreeMenuItem.isSelected(), true);
            }
        });

        // check for a previous preference setting
        boolean showTree = Preferences.get(_showCatalogTreePrefName, true);
        _showCatalogTreeMenuItem.setSelected(showTree);
        _showCatalogTree(showTree, false);

        return _showCatalogTreeMenuItem;
    }

    // Set the visible state of the catalog tree subwindow
    private void _showCatalogTree(boolean show, boolean setPrefs) {
        if (show) {
            _navigator.getCatalogTree().setVisible(true);
            _navigator.getQuerySplitPane().resetToPreferredSizes();
            _navigator.getQuerySplitPane().setDividerSize(10);
        }
        else {
            _navigator.getCatalogTree().setVisible(false);
            _navigator.getQuerySplitPane().setDividerSize(0);
        }

	if (setPrefs) {
	    Preferences.set(_showCatalogTreePrefName, show);
	}
	else {
	    _showCatalogTreeMenuItem.setSelected(show);
	}
	
    }


    /**
     * Create the Go menu.
     */
    protected JMenu createGoMenu(JMenu menu) {
        if (menu == null)
            menu = new JMenu(_I18N.getString("go"));

        menu.add(_navigator.getBackAction());
        menu.add(_navigator.getForwAction());
        menu.addSeparator();
        _navigator.addHistoryMenuItems(menu);
        menu.addSeparator();
        menu.add(createGoClearHistoryMenuItem());

        return menu;
    }

    /**
     * Create the Go => "Clear History" menu item.
     */
    protected JMenuItem createGoClearHistoryMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("clearHistory"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                _navigator.clearHistory();
                _goMenu.removeAll();
                createGoMenu(_goMenu);
            }
        });
        return menuItem;
    }

    /** Add a catalog menu to the catalog navigator frame */
    protected JMenu createCatalogMenu() {
        // to be defined in a derived class
        return new JMenu(_I18N.getString("catalog"));
    }

    /**
     * Create the Table menu.
     */
    protected JMenu createTableMenu() {
        JMenu menu = new JMenu(_I18N.getString("table"));
        menu.add(_navigator.getAddRowAction());
        menu.add(_navigator.getDeleteSelectedRowsAction());

        menu.addSeparator();
        menu.add(_tableCellsEditableMenuItem = createTableCellsEditableMenuItem());

        // Enable/disable/update the previous checkbutton
        _navigator.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JComponent c = _navigator.getResultComponent();
                if (c instanceof TableDisplayTool) {
                    _tableCellsEditableMenuItem.setEnabled(true);
                    TableQueryResult queryResult = ((TableDisplayTool) c).getTable();
                    if (queryResult instanceof MemoryCatalog) {
                        MemoryCatalog cat = (MemoryCatalog) queryResult;
                        _tableCellsEditableMenuItem.setSelected(!cat.isReadOnly());
                    }
                }
                else {
                    _tableCellsEditableMenuItem.setSelected(false);
                    _tableCellsEditableMenuItem.setEnabled(false);
                }
            }
        });

        return menu;
    }

    /**
     * Create the Table => "Editable Table Cells" menu item
     */
    protected JCheckBoxMenuItem createTableCellsEditableMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(_I18N.getString("editableTableCells"));

        menuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
                _navigator.setTableCellsEditable(rb.isSelected());
            }
        });

        return menuItem;
    }

    /** Return the catalog navigator panel */
    public CatalogNavigator getNavigator() {
	return _navigator;
    }

    /** Return the toolbar associated with the image display */
    public GenericToolBar getToolBar() {
	return _toolBar;
    }

    /** Return the handle for the File menu */
    public JMenu getFileMenu() {
        return _fileMenu;
    }

    /** Return the handle for the View menu */
    public JMenu getViewMenu() {
        return _viewMenu;
    }

    /** Return the handle for the Go menu */
    public JMenu getGoMenu() {
        return _goMenu;
    }

    /** Return the handle for the Catalog menu */
    public JMenu getCatalogMenu() {
        return _catalogMenu;
    }

    /** Return the handle for the Table menu */
    public JMenu getTableMenu() {
        return _tableMenu;
    }
}
