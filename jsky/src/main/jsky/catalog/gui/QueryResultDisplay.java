/*
 * ESO Archive
 *
 * $Id: QueryResultDisplay.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/05  Created
 */

package jsky.catalog.gui;

import jsky.catalog.*;

/**
 * This defines the common interface for classes that can display a QueryResult.
 * Classes defining this interface should know how to display the contents of
 * classes implementing the QueryResult interface.
 */
public abstract interface QueryResultDisplay {

    /**
     * Display the given query result.
     */
    public void setQueryResult(QueryResult queryResult);
}
