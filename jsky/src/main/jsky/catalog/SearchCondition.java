/*
 * ESO Archive
 *
 * $Id: SearchCondition.java,v 1.7 2002/08/20 09:57:57 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/17  Created
 */

package jsky.catalog;

import java.io.Serializable;

import jsky.util.StringUtil;


/**
 * Represents a search condition for values in a given table column,
 * or parameters to a query or request.  A searchCondition consists of
 * a name (column or parameter name), a min and a max value. If there
 * can be only one value, the min and max values will be equal.
 */
public class SearchCondition implements Serializable {

    /** Describes the column or parameter whose values are given here */
    private FieldDesc _fieldDesc;

    /** The minimum value */
    private Comparable _minVal;

    /** The maximum value */
    private Comparable _maxVal;

    /** True if the condition includes the min value */
    private boolean _minInclusive = true;

    /** True if the condition includes the max value */
    private boolean _maxInclusive = true;


    /**
     * Create a new SearchCondition where minVal <= x <= maxVal
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, Comparable minVal, Comparable maxVal) {
        _fieldDesc = fieldDesc;
        _minVal = minVal;
        _maxVal = maxVal;
    }

    /**
     * Create a new SearchCondition for the given column or parameter description,
     * where the "inclusive" parameters specify whether
     * the min and/or max values are included in the range.
     */
    public SearchCondition(FieldDesc fieldDesc, Comparable minVal, boolean minInclusive,
                           Comparable maxVal, boolean maxInclusive) {
        _fieldDesc = fieldDesc;
        _minVal = minVal;
        _minInclusive = minInclusive;
        _maxVal = maxVal;
        _maxInclusive = maxInclusive;
    }

    /**
     * Create a new SearchCondition where x == val
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, Comparable val) {
        _fieldDesc = fieldDesc;
        _minVal = _maxVal = val;
    }

    /**
     * Create a new numerical SearchCondition where (minVal <= x <= maxVal)
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, double minVal, double maxVal) {
        this(fieldDesc, new Double(minVal), new Double(maxVal));
    }

    /**
     * Create a new numerical SearchCondition where (x == val)
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, double val) {
        this(fieldDesc, val, val);
    }

    /**
     * Create a new String SearchCondition where (minVal <= x <= maxVal)
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, String minVal, String maxVal) {
        this(fieldDesc, (Comparable) minVal.trim(), (Comparable) maxVal.trim());
    }

    /**
     * Create a new String SearchCondition where (x == val)
     * for the given column or parameter description.
     */
    public SearchCondition(FieldDesc fieldDesc, String val) {
        this(fieldDesc, (Comparable) val.trim(), (Comparable) val.trim());
    }

    /** Return the value (actually a Double or String) */
    public Comparable getVal() {
        return _minVal;
    }

    /** Return the minimum value (actually a Double or String) */
    public Comparable getMinVal() {
        return _minVal;
    }

    /** Return the maximum value (actually a Double or String) */
    public Comparable getMaxVal() {
        return _maxVal;
    }

    /** Return True if the condition includes the min value. */
    public boolean isMinInclusive() {
        return _minInclusive;
    }

    /** Return True if the condition includes the max value. */
    public boolean isMaxInclusive() {
        return _maxInclusive;
    }

    /**
     * Return true if the condition is true for the given value.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(Comparable val) {
        if (_minVal == _maxVal) {
            if (_minVal instanceof String && val instanceof String) {
                // for strings, only check the start of the string
                //return ((String)val).startsWith((String)_minVal);
                return StringUtil.match((String) _minVal, (String) val);
            }
            return (_minVal.compareTo(val) == 0);
        }

        if (_minInclusive && _maxInclusive)
            return (_minVal.compareTo(val) <= 0 && _maxVal.compareTo(val) >= 0);
        if (_minInclusive && !_maxInclusive)
            return (_minVal.compareTo(val) <= 0 && _maxVal.compareTo(val) > 0);
        if (!_minInclusive && _maxInclusive)
            return (_minVal.compareTo(val) < 0 && _maxVal.compareTo(val) >= 0);
        return (_minVal.compareTo(val) < 0 && _maxVal.compareTo(val) > 0);
    }


    /**
     * Return true if the condition is true for the given numeric value.
     * If the condition was specified as a String, the return value is false.
     *
     * @param val The value to be checked against the condition.
     * @return true if the value satisfies the condition.
     */
    public boolean isTrueFor(double val) {
        return isTrueFor(new Double(val));
    }


    /** Return the column or parameter description. */
    public FieldDesc getFieldDesc() {
        return _fieldDesc;
    }

    /** Return the column or parameter name. */
    public String getName() {
        return _fieldDesc.getName();
    }

    /** Return the column or parameter id. */
    public String getId() {
        return _fieldDesc.getId();
    }

    /** Return true if this object represents a range. */
    public boolean isRange() {
        return (_minVal != _maxVal);
    }


    /**
     * Return a string representation of this class in the form "name=minVal[,maxVal]"
     */
    public String toString() {
        String name = _fieldDesc.getName();
        if (_minVal == _maxVal)
            return name + "=" + _minVal;
        return name + "=" + _minVal + "," + _maxVal;
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {
        SearchCondition s = new SearchCondition(new FieldDescAdapter("X"), 0, 1);

        if (!s.isTrueFor(0.5))
            throw new RuntimeException("test failed for 0.5: " + s);
        if (s.isTrueFor(1.5))
            throw new RuntimeException("test failed for 1.5: " + s);

        s = new SearchCondition(new FieldDescAdapter("S"), "aaa", "kkk");
        if (!s.isTrueFor("bbb"))
            throw new RuntimeException("test failed for \"bbb\": " + s);
        if (s.isTrueFor("mmm"))
            throw new RuntimeException("test failed for \"mmm\": " + s);

        System.out.println("All tests passed");
    }

}





