/*
 * ESO Archive
 *
 * $Id: QueryResult.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;


/**
 * Represents the result of a catalog query. We don't
 * make any assumptions here about the type of the query result. It
 * could be tabular data, an image, or HTML file, etc. It is up to
 * the implementing classes to define the rest.
 */
public abstract interface QueryResult {

}
