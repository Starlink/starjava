// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: QueryArgs.java,v 1.7 2002/08/04 21:48:50 brighton Exp $

package jsky.catalog;

import java.util.Vector;

import jsky.coords.CoordinateRadius;


/**
 * An interface representing the values of the arguments to a catalog query.
 * The values correspond one to one with a given catalog's parameters, as 
 * returned by the <code>Catalog.getParamDesc(index)</code> method.
 *
 * @see Catalog#getNumParams
 * @see Catalog#getParamDesc
 */
public abstract interface QueryArgs {

    /** Set the value for the ith parameter */
    public void setParamValue(int i, Object value);

    /** Set the value for the parameter with the given label */
    public void setParamValue(String label, Object value);

    /** Set the int value for the parameter with the given label */
    public void setParamValue(String label, int value);

    /** Set the double value for the parameter with the given label */
    public void setParamValue(String label, double value);


    /** Set the array of parameter values directly. */
    public void setParamValues(Object[] values);

    /** Get the value of the ith parameter */
    public Object getParamValue(int i);

    /** Get the value of the named parameter
     *
     * @param label the parameter name or id
     * @return the value of the parameter, or null if not specified
     */
    public Object getParamValue(String label);

    /**
     * Get the value of the named parameter as an integer.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public int getParamValueAsInt(String label, int defaultValue);

    /**
     * Get the value of the named parameter as a double.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public double getParamValueAsDouble(String label, double defaultValue);

    /**
     * Get the value of the named parameter as a String.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public String getParamValueAsString(String label, String defaultValue);


    /**
     * Return the object id being searched for, or null if none was defined.
     */
    public String getId();

    /**
     * Set the object id to search for.
     */
    public void setId(String id);


    /**
     * Return an object describing the query region (center position and
     * radius range), or null if none was defined.
     */
    public CoordinateRadius getRegion();

    /**
     * Set the query region (center position and radius range) for
     * the search.
     */
    public void setRegion(CoordinateRadius region);


    /** Return the catalog we are accesing. */
    public Catalog getCatalog();


    /**
     * Return an array of SearchCondition objects indicating the
     * values or range of values to search for.
     */
    public SearchCondition[] getConditions();

    /** Returns the max number of rows to be returned from a table query */
    public int getMaxRows();

    /** Set the max number of rows to be returned from a table query */
    public void setMaxRows(int maxRows);


    /** Returns the query type (an optional string, which may be interpreted by some catalogs) */
    public String getQueryType();

    /** Set the query type (an optional string, which may be interpreted by some catalogs) */
    public void setQueryType(String queryType);
}
