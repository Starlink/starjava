/*
 * ESO Archive
 *
 * $Id: CatalogFactory.java,v 1.3 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;

import java.util.*;
import java.io.*;


/**
 * Used to manage access to a known list of catalogs.
 * Catalogs may be registered by name and later searched for by name.
 */
public class CatalogFactory {

    // Sorted list of known catalogs
    private static TreeSet _catalogSet = new TreeSet(
            new Comparator() {
                public int compare(Object o1, Object o2) {
                    return (((Catalog) o1).getName().compareTo(((Catalog) o2).getName()));
                }
            }
    );

    // The list of catalogs, unsorted, in the order they were registered
    private static List _catalogList = new ArrayList();


    /**
     * Register the given catalog. The argument may be any object that
     * implements the Catalog interface and will be used for any access
     * to that catalog. Since the catalog may not actually be used, the
     * constructor should not open any connections until needed.
     *
     * @param catalog An object to use to query the catalog.
     * @param overwrite if true, the given catalog object replaces any 
     *                previously defined catalog with the same name,
     *                otherwise only the first catalog registered with a 
     *                given name is actually registered.
     */
    public static void registerCatalog(Catalog catalog, boolean overwrite) {
        if (getCatalogByName(catalog.getName()) != null) {
	    if (overwrite) {
		_catalogSet.remove(catalog);
		_catalogList.remove(catalog);
	    }
	    else {
		return;
	    }
        }
        _catalogSet.add(catalog);
        _catalogList.add(catalog);
    }


    /**
     * This method returns a Catalog object that can be used to query
     * the given catalog, or null if no such object was found.
     *
     * @param catalogName The name of a registered catalog
     *
     * @return The object to use to query the catalog, or null if not found.  
     */
    public static Catalog getCatalogByName(String catalogName) {
        Iterator it = _catalogSet.iterator();
        while (it.hasNext()) {
            Catalog catalog = (Catalog) (it.next());
            String s = catalog.getName();
            if (s != null && s.equals(catalogName)) {
                return catalog;
            }
        }

        return null;
    }

    /**
     * This method returns a list of Catalog objects that have the given type,
     * in the order in which they were registered.
     *
     * @param type The catalog type (as returned by <code>Catalog.getType()</code>)
     * @return the list of Catalog objects found
     */
    public static List getCatalogsByType(String type) {
	List l = new ArrayList();
        Iterator it = _catalogList.iterator();
        while (it.hasNext()) {
            Catalog catalog = (Catalog) (it.next());
            String s = catalog.getType();
            if (s != null && s.equals(type)) {
                l.add(catalog);;
            }
        }

        return l;
    }



    /**
     * Unregister the given catalog, removing it from the list of known
     * catalogs.
     *
     * @param catalog The catalog to be removed from the list.
     */
    public static void unregisterCatalog(Catalog catalog) {
        if (_catalogSet.contains(catalog)) {
            _catalogSet.remove(catalog);
            _catalogList.remove(catalog);
	}
    }


    /**
     * Returns an Iterator to visit each registered catalog in sorted order.
     * @return The Iterator object for a sorted list of Catalogs.
     */
    public static Iterator iterator() {
        return _catalogSet.iterator();
    }
}
