// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: CatalogUIHandler.java,v 1.3 2002/08/04 21:48:50 brighton Exp $

package jsky.catalog.gui;

import javax.swing.JComponent;

import jsky.catalog.Catalog;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;


/**
 * Defines the interface for classes wishing to define thier own catalog
 * query or query result components.
 */
public abstract interface CatalogUIHandler {

    /**
     * This interface may be implemented by Catalog and QueryResult objects that 
     * wish to define custom user interfaces.
     *
     * @param display can be used to display the results of a catalog query
     *
     * @return a user interface component for the catalog or queryResult object, or null,
     *         in which case a default component will be used, based on the object type
     */
    public JComponent makeComponent(QueryResultDisplay display);
}
