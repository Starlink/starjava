/*
 * ESO Archive
 *
 * $Id: CatalogDirectory.java,v 1.3 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/05  Created
 */

package jsky.catalog;

import java.net.URL;
import java.util.List;

import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeModel;


/**
 * This defines the interface for a catalog directory, allowing catalogs
 * to be organized in a hierarchy.
 */
public abstract interface CatalogDirectory extends Catalog, TreeModel {

    /** Return the number of catalogs in this directory */
    public int getNumCatalogs();

    /** Return the ith catalog in the directory */
    public Catalog getCatalog(int i);

    /** Return the named catalog, if found in this directory */
    public Catalog getCatalog(String catalogName);

    /** Return the index of the given catalog in the directory */
    public int indexOf(Catalog cat);

    /** Return a memory catalog describing the list of catalogs in the directory */
    public TableQueryResult getCatalogList();

    /** Return an array of catalogs describing the path to the given catalog or catalog directory. */
    public Catalog[] getPath(Catalog cat);

    /**
     * Add the given catalog to the catalog list. An error message is displayed if the
     * catalog is already in the list. If a separate catalog with the same name is in the list,
     * the user is asked if it should be removed.
     */
    public void addCatalog(Catalog cat);

    /** Remove the given catalog from the catalog list. */
    public void removeCatalog(Catalog cat);

    /** Replace the given old catalog with the given new catalog in the catalog list. */
    public void replaceCatalog(Catalog oldCat, Catalog newCat);

    /** Move the the given catalog up or down in the list. */
    public void moveCatalog(Catalog cat, boolean up);

    /** Move the the given catalog all the way up or down in the list, as far as possible. */
    public void moveCatalogToEnd(Catalog cat, boolean up);

    /** 
     * Save the contents of this catalog directory to make it permanent
     * (for example, in a config file under ~/.jsky/...).
     */
    public void save();

    /** Reload the catalog directory and return the new (or existing) object for it. */
    public CatalogDirectory reload();

    /** 
     * Attempt to read a catalog subdirectory from the given URL and insert
     * the object for it in the catalog tree.
     *
     * @return the new CatalogDirectory
     * @throws RuntimeException if the catalog directory could not be created
     */
    public CatalogDirectory loadSubDir(URL url);

    /**
     * Return a list of name servers (Catalogs with type
     * equal to "namesvr") to use to resolve astronomical object names.
     */
    public List getNameServers();
}


