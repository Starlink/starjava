/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CatalogNavigatorOpener.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.catalog.gui;

import jsky.catalog.Catalog;

/**
 * This interface is implemented by classes that can create and/or
 * open the catalog navigator window to display the contents of a
 * given catalog.
 */
public abstract interface CatalogNavigatorOpener {

    /** Open the catalog window. */
    public void openCatalogWindow();

    /** Open the catalog window and display the interface for given catalog, if not null. */
    public void openCatalogWindow(Catalog cat);

    /** Open a catalog window for the named catalog, if found. */
    public void openCatalogWindow(String name);

    /** Pop up a file browser to select a local catalog file to open. */
    public void openLocalCatalog();
}


