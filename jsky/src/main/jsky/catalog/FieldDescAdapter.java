/*
 * ESO Archive
 *
 * $Id: FieldDescAdapter.java,v 1.7 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/01  Created
 */

package jsky.catalog;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import jsky.util.NameValue;


/**
 * This class provides a default implementation of the FieldDesc interface,
 * used to describes a catalog field (column or query parameter).
 */
public class FieldDescAdapter implements FieldDesc, Serializable {

    // The id of the field
    private String _id;

    // The name of the field
    private String _name;

    // The default value for this field, or null if there is no default
    private Object _defaultValue;

    // A description of the field, if available, otherwise null
    private String _description;

    // A Class object representing the data type of the field values
    private Class _fieldClass = String.class;

    // True if this field is the min value of a range
    private boolean _minFlag = false;

    // True if this field is the max value of a range
    private boolean _maxFlag = false;

    // An array containing the field's allowed values
    private NameValue[] _options;

    // The default value for this field
    private String _value;

    // A string describing the field's semantic type (for example: "ra", "dec", "radius")
    private String _type;

    // The units for this field (For example: "arcmin", "arcsec", "deg", "hours")
    private String _units;

    // The format of this field (or the format that the server expects the parameter to be in).
    // For example: "h:m:s", "d:m:s", "h m s", "d m s", "hours", "deg"
    private String _format;

    // True if this field contains the unique id
    private boolean _idFlag = false;

    // True if this field contains a world coordinates RA value
    private boolean _raFlag = false;

    // True if this field contains a world coordinates Dec value
    private boolean _decFlag = false;



    /** Create an empty field description */
    public FieldDescAdapter() {
    }

    /** Create a field description with the given name */
    public FieldDescAdapter(String name) {
        _name = name;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    /** Set a string describing the semantic type of the field (for example: "ra", "dec", "radius") */
    public void setType(String type) {
        _type = type;
    }

    /** 
     * Return a string describing the semantic type of the field (for example: "ra", "dec", "radius"). 
     */
    public String getType() {
        return _type;
    }

    /** Set the units of the field */
    public void setUnits(String units) {
        _units = units;
    }

    /** Return a string describing the units of the field values, if known (for example: "arcmin", "arcsec", "deg") */
    public String getUnits() {
        return _units;
    }

    /** Set a string describing the format of the field, if known */
    public void setFormat(String format) {
        _format = format;
    }

    /** Return a string describing the format of the field, if known, otherwise null */
    public String getFormat() {
        return _format;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public String getDescription() {
        return _description;
    }

    public void setFieldClass(Class fieldClass) {
        _fieldClass = fieldClass;
    }

    public Class getFieldClass() {
        return _fieldClass;
    }

    /** Return the field's default value, if there is one (may be null) */
    //public Object getValue() {
    //	return _value;
    //}

    /** Set the field's default value */
    //public void setValue(String value) {
    //    _value = value;
    //}

    /** Parse the given string into the correct class type for this field and return the value. */
    public Object getValue(String s) {
        return FieldFormat.getValue(this, s);
    }

    /** Return the default value for this field, or null if there is no default. */
    public Object getDefaultValue() {
        return _defaultValue;
    }

    /** Set the default value for this field. */
    public void setDefaultValue(Object v) {
        _defaultValue = v;
    }



    /**
     * Set the field options to a list of NameValue objects.
     */
    public void setOptions(NameValue[] options) {
        _options = options;
    }

    /**
     * If the field may only have a limited number of values, return the number
     * of values, otherwise 0.
     */
    public int getNumOptions() {
        if (_options == null)
            return 0;
        return _options.length;
    }

    /** Return the index of the default option, or -1 if there is no default. */
    public int getDefaultOptionIndex() {
        // make the first option the default, if there is one
        return 0;
    }

    /** Return the name of the ith option for this field. */
    public String getOptionName(int i) {
        if (_options == null)
            return null;
        return _options[i].getName();
    }

    /** Return the value of the ith option for this field. */
    public Object getOptionValue(int i) {
        if (_options == null)
            return null;
        return _options[i].getValue();
    }


    /** Return true if the given value is valid for this field, otherwise false. */
    public boolean isValid(Object value) {
        return true;
    }

    /** Return a URL pointing to documentation for this field, or null if not available */
    public URL getDocURL() {
        return null;
    }



    /** Return true if the field has a link pointing to more data. */
    public boolean hasLink() {
        String name = getName();
        return (name.equals("PREVIEW") || name.equals("MORE"));
    }

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
    public String getLinkText(TableQueryResult tableQueryResult, Object value, int row, int column) {
        String name = getName();
        if (name.equals("PREVIEW"))
            return "Preview";
        if (name.equals("MORE"))
            return "More Info";
        throw new RuntimeException("Bad table link: " + name);
    }


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
	throws MalformedURLException {

        if (value instanceof String) {
            String s = (String)value;
            if (s.startsWith("M=") || s.startsWith("P="))
                s = s.substring(2);
	    return new URLQueryResult(new URL(s));
        }
        throw new RuntimeException("Invalid table link: " + value);
    }


    /** Return true if this field is the unique id. */
    public boolean isId() {
        return _idFlag;
    }

    /** Set to true if this field contains the unique id */
    public void setIsId(boolean flag) {
        _idFlag = flag;
    }

    /** Return true if this field contains a world coordinates RA value. */
    public boolean isRA() {
        return _raFlag;
    }

    /** Set to true if this field contains a world coordinates RA value. */
    public void setIsRA(boolean flag) {
        _raFlag = flag;
    }

    /** Return true if this field contains a world coordinates Dec value. */
    public boolean isDec() {
        return _decFlag;
    }

    /** Set to true if this field contains a world coordinates RA value. */
    public void setIsDec(boolean flag) {
        _decFlag = flag;
    }


    /** Return true if this field is the min value of a range. */
    public boolean isMin() {
        return _minFlag;
    }

    /** Set to true if this field is the min value of a range. */
    public void setIsMin(boolean b) {
        _minFlag = b;
    }

    /** Return true if this field is the max value of a range. */
    public boolean isMax() {
        return _maxFlag;
    }

    /** Set to true if this field is the max value of a range. */
    public void setIsMax(boolean b) {
        _maxFlag = b;
    }
}


