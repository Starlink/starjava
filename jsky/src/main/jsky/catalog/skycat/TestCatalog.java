/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TestCatalog.java,v 1.4 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog.skycat;

import java.io.IOException;
import java.net.URL;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TestTableQueryResult;
import jsky.coords.CoordinateRadius;


/**
 * Used for testing. This class provides a dummy catalog class that can be used for testing.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 *
 */
public class TestCatalog implements Catalog {

    /**
     * Implementation of the clone method (makes a shallow copy).
     */
    public Object clone() {
	try {
	    return super.clone();
	} 
	catch (CloneNotSupportedException ex) {
	    throw new InternalError(); // won't happen
	}
    }

    /** Set the name of the catalog  */
    public void setName(String name) {
    }

    /** Return the name of the catalog */
    public String getName() {
        return "Test Catalog";
    }

    /** Return the Id or short name of the catalog */
    public String getId() {
        return "Test";
    }

    /** Return a string to display as a title for the catalog in a user interface */
    public String getTitle() {
        return "Test Catalog";
    }

    /** Return a description of the catalog, or null if not available */
    public String getDescription() {
        return null;
    }

    /** Return a URL pointing to documentation for the catalog, or null if not available */
    public URL getDocURL() {
        return null;
    }

    /** If this catalog can be querried, return the number of query parameters that it accepts */
    public int getNumParams() {
        return 4;
    }

    /** Return a description of the ith query parameter */
    public FieldDesc getParamDesc(int i) {
        return new FieldDescAdapter("Param" + i);
    }

    /** Return a description of the named query parameter */
    public FieldDesc getParamDesc(String name) {
        return new FieldDescAdapter(name);
    }

    /**
     * Given a description of a region of the sky (center point and radius range),
     * and the current query argument settings, set the values of the corresponding
     * query parameters.
     *
     * @param queryArgs (in/out) describes the query arguments
     * @param region (in) describes the query region (center and radius range)
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region) {
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return true;
    }

    /** Return the catalog type (normally one of the Catalog constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER) */
    public String getType() {
        return Catalog.CATALOG;
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return false;
    }

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
    public QueryResult query(QueryArgs queryArgs) throws IOException {
        System.out.println("XXX queryArgs = " + queryArgs);

        // show how to access the URL from the catalog config file
        SkycatCatalog cat = (SkycatCatalog) SkycatConfigFile.getConfigFile().getCatalog(getName());
        if (cat != null) {
            SkycatConfigEntry entry = cat.getConfigEntry();
            if (entry != null)
                System.out.println("XXX URL = " + entry.getURL(0));
            else
                System.out.println("XXX couldn't find the catalog entry");
        }
        else {
            System.out.println("XXX couldn't find catalog named " + getName());
        }
        return new TestTableQueryResult();
    }

    /** Set the parent catalog directory */
    public void setParent(CatalogDirectory catDir) {
    }

    /** Return a reference to the parent catalog directory, or null if not known. */
    public CatalogDirectory getParent() {
	return null;
    }

    /** 
     * Return an array of Catalog or CatalogDirectory objects representing the 
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
	return null; 
    }
}


