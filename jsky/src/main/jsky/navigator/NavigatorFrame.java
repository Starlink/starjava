/*
 * ESO Archive
 *
 * $Id: NavigatorFrame.java,v 1.18 2002/08/04 21:48:51 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/02  Created
 */

package jsky.navigator;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.BasicTablePlotter;
import jsky.catalog.gui.CatalogTree;
import jsky.catalog.gui.TablePlotter;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;
import jsky.util.gui.LookAndFeelMenu;


/**
 * Provides a top level window and menubar for the Navigator class.
 */
public class NavigatorFrame extends JFrame {

    /** Main panel */
    protected Navigator navigator;

    /** Set to true until setVisible is called */
    private boolean firstTime = true;


    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the given catalog directory in it.
     *
     * @param catDir The top level catalog directory to display
     *
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     */
    public NavigatorFrame(CatalogDirectory catDir, MainImageDisplay imageDisplay) {
        super("Catalog Navigator");

        CatalogTree catalogTree = new CatalogTree(catDir);
        TablePlotter plotter = new BasicTablePlotter();

        navigator = new Navigator(this, catalogTree, plotter, imageDisplay);
        catalogTree.setQueryResult(catDir);

        NavigatorToolBar toolbar = new NavigatorToolBar(navigator);
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(navigator, BorderLayout.CENTER);
        setJMenuBar(new NavigatorMenuBar(navigator, toolbar));

        // set default window size and remember changes between sessions
        Preferences.manageLocation(this);
        Preferences.manageSize(navigator, new Dimension(650, 550));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // include this top level window in any future look and feel changes
        LookAndFeelMenu.addWindow(this);
    }

    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the given catalog directory in it.
     *
     * @param catDir The top level catalog directory to display
     */
    public NavigatorFrame(CatalogDirectory catDir) {
        this(catDir, null);
    }

    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the default catalog directory in it.
     */
    public NavigatorFrame() {
        this(Navigator.getCatalogDirectory());
    }

    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the default catalog directory in it.
     *
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     */
    public NavigatorFrame(MainImageDisplay imageDisplay) {
        this(Navigator.getCatalogDirectory(), imageDisplay);
    }


    /** Return the navigator panel. */
    public Navigator getNavigator() {
        return navigator;
    }


    /** Delay pack until first show of window to avoid linux display bug */
    public void setVisible(boolean b) {
        if (b && firstTime) {
            firstTime = false;
            pack();
        }
        super.setVisible(b);
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        NavigatorFrame f = new NavigatorFrame();
        f.setVisible(true);
    }
}

