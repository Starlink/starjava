/*
 * Copyright (C) 2001-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-JUN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.lang.*;

import uk.ac.starlink.ast.*;

/**
 * AstDouble implements a double precision Number that represents a
 * value that should be formatted and unformatted using the
 * characteristics of an axis of an AST Frame. This allows it to be
 * encoded and decoded as say RA and Dec, as well as more mundane types
 * such as simple double precision.
 * <p>
 * This transformation is achieved using the "format" and "unformat"
 * AST routines. Failures to decode a string are signalled by the
 * value of this object being equivalent to BAD (i.e. test isBad()).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see <a href='http://www.starlink.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_'>Starlink User Note 211</a>
 */
public class AstDouble extends Number implements Comparable
{
    /**
     * The value.
     */
    private double value = 0.0;

    /**
     * The AST frame.
     */
    private Frame frame = null;

    /**
     * The axis of the frame to reference (start at 1).
     */
    private int axis = 0;

    /**
     * The AST bad value.
     */
    public static final double BAD = -Double.MAX_VALUE;

    /**
     * Construct an AstDouble.
     *
     * @param value the value to be stored.
     * @param frame a reference to an AST frame whose properties
     *              define the axis formatting and unformatting
     *              capabilities. This may be a frameset or plot, in
     *              which case it applies to the current frame.
     * @param axis the axis in the frameset that should be used.
     */
    public AstDouble( double value, Frame frame, int axis )
    {
        this.value = value;
        this.frame = frame;
        this.axis = axis;
    }

    /**
     * Construct an AstDouble from a formatted value.
     *
     * @param value the value to be unformatted and stored.
     * @param frame a reference to an AST frame whose properties
     *              define the axis formatting and unformatting
     *              capabilities. This may be a frameset or plot, in
     *              which case it applies to the current frame.
     * @param axis the axis in the frameset that should be used.
     */
    public AstDouble( String value, Frame frame, int axis )
        throws NumberFormatException
    {
        this( valueOf( value, frame, axis ).doubleValue(), frame, axis );
    }

    /**
     * Check if this object is BAD (i.e. AST__BAD).
     */
    public boolean isBad()
    {
        return ( doubleValue() == BAD );
    }

    /**
     * Check if a value is BAD (i.e. AST__BAD).
     *
     * @param value the value to compare.
     */
    public static boolean isBad( double value )
    {
        return ( value == BAD );
    }

    /**
     * Returns a new double initialized to the value represented by
     * the specified String.
     *
     * @param value the string to be parsed.
     * @param frame a reference to an AST frame whose properties
     *              define the axis formatting and unformatting
     *              capabilities. This may be a frameset or plot, in
     *              which case it applies to the current frame.
     * @param axis the axis in the frameset that should be used.
     *
     * @return double representation of the string. BAD if fails.
     */
    public static double parseDouble( String value, Frame frame,
                                      int axis )
    {
        return frame.unformat( axis, value );
    }

    /**
     * Return a String representation of this object.
     *
     * @return the AST formatted representation of this object.
     */
    public String toString()
    {
        return frame.format( axis, value );
    }

    /**
     * Return a formatted String representation of a double.
     *
     * @param value the value to be formatted.
     * @param frame a reference to an AST frame whose properties
     *                 define the axis formatting and unformatting
     *                 capabilities. This may be a frameset or plot, in
     *                 which case it applies to the current frame.
     * @param axis the axis in the frameset that should be used.
     */
    public static String toString( double value, Frame frame, int axis )
    {
        return frame.format( axis, value );
    }

    /**
     * Creates a new AstDouble object by initializing to the value
     * represented by the specified string. This is useful as it
     * keeps the existing frame set and axis.
     *
     * @param value the string to be parsed.
     *
     * @return the new AstDouble object.
     */
    public AstDouble valueOf( String value )
    {
        double dvalue = parseDouble( value, frame, axis );
        return new AstDouble( dvalue, frame, axis );
    }

    /**
     * Returns a new AstDouble object initialized to the value
     * represented by the specified string.
     *
     * @param value the string to be parsed.
     * @param frame a reference to an AST frame whose properties
     *              define the axis formatting and unformatting
     *              capabilities. This may be a frameset or plot, in
     *              which case it applies to the current frame.
     * @param axis the axis in the frameset that should be used.
     *
     * @return the new AstDouble object.
     */
    public static AstDouble valueOf( String value, Frame frame, int axis )
    {
        double dvalue = parseDouble( value, frame, axis );
        return new AstDouble( dvalue, frame, axis );
    }

//
// Number interface.
//
    public byte byteValue()
    {
        return (byte) value;
    }
    public double doubleValue()
    {
        return value;
    }
    public float floatValue()
    {
        return (float) value;
    }
    public int intValue()
    {
        return (int) value;
    }
    public long longValue()
    {
        return (long) value;
    }
    public short shortValue()
    {
        return (short) value;
    }

//
// Comparible interface.
//
   /**
    * Compares this AstDouble to another Object. If the Object is an
    * AstDouble, this method behaves like compareTo(AstDouble).
    * Otherwise, it throws a ClassCastException (as AstDoubles are
    * comparable only to other AstDoubles).
    *
    * @param o the Object to be compared.

    * @return the value 0 if the argument is an AstDouble numerically
    *         equal to this AstDouble; a value less than 0 if the
    *         argument is a Double numerically greater than this
    *         AstDouble; and a value greater than 0 if the argument is
    *         an AstDouble numerically less than this Double.
    *
    * @throws ClassCastException if the argument is not an AstDouble.
    * @see java.lang.Double#compareTo
    */
    public int compareTo( Object o )
    {
        // Not optimal solution, use Double.compareTo.
        AstDouble d =  (AstDouble) o;
        Double newO = Double.valueOf( d.doubleValue() );
        return Double.valueOf( value ).compareTo( newO );
    }
}
