// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IRSACatalogQueryTool.java,v 1.2 2002/08/05 10:57:20 brighton Exp $

package jsky.catalog.irsa;

import jsky.catalog.Catalog;
import jsky.catalog.gui.CatalogQueryPanel;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.navigator.NavigatorQueryTool;


/**
 * Defines the user interface for querying a {@link IRSACatalog}. This replaces the default
 * {@link NavigatorQueryTool} with once specialized for the IRSACatalog class.
 *
 * @author Allan Brighton 
 */
public final class IRSACatalogQueryTool extends NavigatorQueryTool {

    /**
     * Initialize a query panel for the given catalog.
     * 
     * @param catalog the catalog, for which a user interface component is being generated
     *
     * @param display object used to display the results of a query
     */
    public IRSACatalogQueryTool(Catalog catalog, QueryResultDisplay display) {
	super(catalog, display);
    }
    
    /** Make and return the catalog query panel */
    protected CatalogQueryPanel makeCatalogQueryPanel(Catalog catalog) {
	return new IRSACatalogQueryPanel(catalog);
    }
}
