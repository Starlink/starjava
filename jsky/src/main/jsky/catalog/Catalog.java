// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: Catalog.java,v 1.11 2002/08/04 21:48:50 brighton Exp $


package jsky.catalog;


import jsky.coords.CoordinateRadius;

import java.net.URL;
import java.io.IOException;


/**
 * This interface defines the common interface to all catalogs.
 *
 * @version $Revision: 1.11 $
 * @author Allan Brighton
 */
public abstract interface Catalog extends QueryResult, Cloneable {

    /** Value returned by getType() for servers that return a table */
    public static final String CATALOG = "catalog";

    /** Value returned by getType() for servers that return a table containing pointers to 
	images and other data */
    public static final String ARCHIVE = "archive";

    /** Value returned by getType() for servers that return an image */
    public static final String IMAGE_SERVER = "imagesvr";

    /** Value returned by getType() for servers that return the RA,Dec coordinates for an object name */
    public static final String NAME_SERVER = "namesvr";

    /** Value returned by getType() for catalogs that return a list of other catalogs */
    public static final String DIRECTORY = "directory";

    /** Value returned by getType() for local catalog files. */
    public static final String LOCAL = "local";


    /** Implementation of the clone method (makes a shallow copy). */
    public Object clone();


    /** Return the name of the catalog */
    public String getName();

    /** Set the catalog's name */
    public void setName(String name);


    /** Return the Id or short name of the catalog */
    public String getId();

    /** Return a string to display as a title for the catalog in a user interface */
    public String getTitle();

    /** Return a description of the catalog, or null if not available */
    public String getDescription();

    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL();

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams();

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i);

    /** Return a description of the named query parameter */
    public FieldDesc getParamDesc(String name);


    /**
     * Given a description of a region of the sky (center point and radius range),
     * and the current query argument settings, set the values of the corresponding
     * query parameters.
     *
     * @param queryArgs (in/out) describes the query arguments
     * @param region (in) describes the query region (center and radius range)
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region);

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal();

    /** Return true if this object represents an image server. */
    public boolean isImageServer();

    /** Return the catalog type (one of the constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType();

    /** Set the parent catalog directory */
    public void setParent(CatalogDirectory catDir);

    /** Return a reference to the parent catalog directory, or null if not known. */
    public CatalogDirectory getParent();

    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath();

    /**
     * Query the catalog using the given arguments and return the result.
     * The result of a query may be any class that implements the QueryResult
     * interface. It is up to the calling class to interpret and display the
     * result. In the general case where the result is downloaded via HTTP,
     * The URLQueryResult class may be used.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException;
}


