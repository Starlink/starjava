/*
 * ESO Archive
 *
 * $Id: FieldDesc.java,v 1.6 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/28  Created
 */

package jsky.catalog;

import java.net.*;

/**
 * This interface describes a catalog field, which may be an input field,
 * such as a query parameter, or an output field, such as a table column
 * description. Any of the methods may return null, if the information
 * is not known, although normally, at least a field name should be
 * provided. To save work, classes that implement this interface may be
 * derived from FieldDescAdatper and override as many of the methods as
 * needed.
 */
public abstract interface FieldDesc {

    /** Return the Id of this field. */
    public String getId();

    /** Return the name of this field. */
    public String getName();

    /** 
     * Return a string describing the semantic type of the field (for example: "ra", "dec", "radius"). 
     * @see #getFieldClass
     */
    public String getType();

    /** Return a string describing the units of the field values, if known (for example: "arcmin", "arcsec", "deg") */
    public String getUnits();

    /** Return a string describing the format of the field, if known, otherwise null */
    public String getFormat();

    /** Return a more detailed description of this field. */
    public String getDescription();

    /** Return a URL pointing to documentation for this field, or null if not available */
    public URL getDocURL();

    /** Return true if the field has a link pointing to more data. */
    public boolean hasLink();

    /**
     * Return the text to display for the link, if there is one, otherwise null.
     *
     * @param tableQueryResult object representing the table data
     * @param value the value in the table cell
     * @param row the row in the table
     * @param column the column in the table
     *
     * @throws RuntimeException if the field is not a link
     */
    public String getLinkText(TableQueryResult tableQueryResult, Object value, int row, int column);

    /**
     * If this field has a link, follow it and return the value it points to as a QueryResult.
     *
     * @param tableQueryResult object representing the table data
     * @param value the value in the table cell
     * @param row the row in the table
     *
     * @throws MalformedURLException if the value is not valid URL string
     * @throws RuntimeException if the value is not a string
     */
    public QueryResult getLinkValue(TableQueryResult tableQueryResult, Object value, int row) 
	throws MalformedURLException;

    /** Return the class to use to store values in this field. */
    public Class getFieldClass();

    /** Return the field's default value, if there is one (may be null) */
    //public Object getValue();

    /** Parse the given string into the correct class type for this field and return the value. */
    public Object getValue(String s);

    /** Return the default value for this field, or null if there is no default. */
    public Object getDefaultValue();

    /** If a list of options was defined for the field, return the number of options, otherwise 0. */
    public int getNumOptions();

    /** Return the index of the default option, or -1 if there is no default. */
    public int getDefaultOptionIndex();

    /** Return the name of the ith option for this field. */
    public String getOptionName(int i);

    /** Return the value of the ith option for this field. */
    public Object getOptionValue(int i);

    /** Return true if the given value is valid for this field, otherwise false. */
    public boolean isValid(Object value);

    /** Return true if this field is the unique id. */
    public boolean isId();

    /** Return true if this field contains a world coordinates RA value. */
    public boolean isRA();

    /** Return true if this field contains a world coordinates Dec value. */
    public boolean isDec();

    /** Return true if this field represents the min value of a range. */
    public boolean isMin();

    /** Return true if this field represents the max value of a range. */
    public boolean isMax();
}


