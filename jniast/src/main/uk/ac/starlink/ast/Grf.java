package uk.ac.starlink.ast;

import java.awt.Graphics;

/**
 * Interface for specifying graphics implementations used by 
 * <code>Plot</code>s.
 * Each Plot owns an object which implements this interface and 
 * uses it to do the actual graphical output.
 * Implementations of Grf are expected to keep track of all the 
 * graphics written to them (i.e. <code>line</code>, <code>mark</code>
 * and <code>text</code> calls) and be able actually to paint all
 * resulting graphics so far requested onto a given graphics context 
 * at any time in response to a call of the <code>paint</code> method.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public interface Grf {

    /** Symbolic attribute type indicating line style. */
    public final static int GRF__STYLE = 
        AstObject.getAstConstantI( "GRF__STYLE" );

    /** Symbolic attribute type indicating line width. */
    public final static int GRF__WIDTH = 
        AstObject.getAstConstantI( "GRF__WIDTH" );

    /** Symbolic attribute type indicating character/marker size 
      * scale factor. */
    public final static int GRF__SIZE = 
        AstObject.getAstConstantI( "GRF__SIZE" );

    /** Symbolic attribute type indicating character font. */
    public final static int GRF__FONT = 
        AstObject.getAstConstantI( "GRF__FONT" );

    /** Symbolic attribute type indicating colour index. */
    public final static int GRF__COLOUR = 
        AstObject.getAstConstantI( "GRF__COLOUR" );

    /** Symbolic primitive type indicating lines. */
    public final static int GRF__LINE = 
        AstObject.getAstConstantI( "GRF__LINE" );

    /** Symbolic primitive type indicating markers. */
    public final static int GRF__MARK = 
        AstObject.getAstConstantI( "GRF__MARK" );

    /** Symbolic primitive type indicating text. */
    public final static int GRF__TEXT =
        AstObject.getAstConstantI( "GRF__TEXT" );

    /**
     * Paints the graphics written to this Grf object since its 
     * construction or the last call of <code>clear</code> into the
     * graphics context given.
     *
     * @param   g2  the graphics context.  It will be cast to a 
     *              {@link java.awt.Graphics2D}, but hopefully it will be
     *              one of those anyway.
     */
    public void paint( Graphics g2 );

    /**
     * Clears all known graphics.  It cannot actually erase graphics
     * drawn to an existing graphics context, but immediately following
     * this call, a subsequent call to <code>paint</code> will draw 
     * nothing to its graphics context.
     */
    public void clear();

    /**
     * Flushes all pending graphics to the output device.
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public void flush() throws Exception;

    /**
     * Draws a polyline (i.e. a set of connected lines).
     * Nothing should be done if <code>n</code> is less than 2, or if 
     * <code>x</code> or <code>y</code> are null.
     *
     * @param   n  the number of points to plot
     * @param   x  an array of <code>n</code> x-coordinates
     * @param   y  an array of <code>n</code> y-coordinates
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public void line( int n, float[] x, float[] y ) throws Exception;

    /**
     * Displays markers at the given positions.
     * Nothing should be done if <code>n</code> is less than 2, or if 
     * <code>x</code> or <code>y</code> are null.
     * 
     * @param   n     the number of points to plot
     * @param   x     an array of <code>n</code> x-coordinates
     * @param   y     an array of <code>n</code> y-coordinates
     * @param   type  an integer indicating the type of marker to plot
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public void mark( int n, float[] x, float[] y, int type ) throws Exception;

    /**
     * Displays a character string at a given position using a specified
     * justification and up-vector.  A null value for <code>just</code>
     * is equivalent to supplying "CC".  <code>upx=upy=0</code> should
     * throw an Exception.
     *
     * @param   text  the string to be displayed
     * @param   x     the reference x coordinate
     * @param   y     the reference y coordinate
     * @param   just  a character string which specifies the location within
     *                the text string which is to be placed at the reference
     *                position given by x and y. The first character
     *                may be 'T' for "top", 'C' for "centre", or 'B' for
     *                "bottom", and specifies the vertical location of the
     *                reference position. Note, "bottom" corresponds to the
     *                base-line of normal text. Some characters (eg "y",
     *                "g", "p", etc) descend below the base-line. The
     *                second character may be 'L' for "left", 'C' for
     *                "centre", or 'R' for "right", and specifies the
     *                horizontal location of the reference position. If
     *                the string has less than 2 characters then 'C'
     *                is used for the missing characters.
     * @param   upx   The x component of the up-vector for the text,
     *                in graphics world coordinates. If necessary the 
     *                supplied value should be negated to ensure that 
     *                positive values always refer to displacements from
     *                left to right on the screen.
     * @param   upy   The y component of the up-vector for the text,
     *                in graphics world coordinates. If necessary the 
     *                supplied value should be negated to ensure that
     *                positive values always refer to displacements from
     *                bottom to top on the screen.
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public void text( String text, float x, float y, String just, 
                      float upx, float upy ) throws Exception;

    /**
     * Enquires or sets a graphics attribute value.  The current value is
     * returned, and optionally a new value may be established.  The 
     * attribute is in all cases represented by a double precision value;
     * the meaning of this is determined by, and should be documented by,
     * the Grf implementation.
     *
     * @param  attr  an integer value identifying the required attribute.
     *               One of the following:
     *               <ul><li>GRF__STYLE - line style
     *                   <li>GRF__WIDTH - line width
     *                   <li>GRF__SIZE - character/marker size scale factor
     *                   <li>GRF__FONT - character font
     *                   <li>GRF__COLOUR - colour index
     *               </ul>
     * @param  value a new value to store for the attribute.  If this is 
     *               AstObject.AST__BAD no value is stored.
     * @param  prim  the sort of graphics primitive to be drawn with the
     *               new attribute.   One of the following:
     *               <ul><li>GRF__LINE
     *                   <li>GRF__MARK
     *                   <li>GRF__TEXT
     *               </ul>
     * @return  the old value of the attribute indicated
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public double attr( int attr, double value, int prim ) throws Exception;

    /**
     * Returns the character height in world coordinates.
     * 
     * @return  a two-element array: 
     *          the first element gives the height of characters drawn 
     *          with a vertical baseline (an increment in the X axis) and 
     *          the second element gives the height of characters drawn
     *          with a horizontal baseline (an increment in the Y axis)
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public float[] qch() throws Exception;

    /**
     * Gets the extent of a character string.  This method returns the
     * corners of a box which would enclose the supplied character string
     * if it were displayed using the <code>text</code> method.
     * The returned box <i>includes</i> any leading or trailing spaces.
     * The order of the corners is anti-clockwise (in world coordinates)
     * starting at the bottom left.  A null value for <code>just</code>
     * is equivalent to supplying "CC".  <code>upx=upy=0</code> should
     * throw an Exception.
     *
     * @param   text  the string to be displayed
     * @param   x     the reference x coordinate
     * @param   y     the reference y coordinate
     * @param   just  a character string which specifies the location within
     *                the text string which is to be placed at the reference
     *                position given by x and y. The first character
     *                may be 'T' for "top", 'C' for "centre", or 'B' for
     *                "bottom", and specifies the vertical location of the
     *                reference position. Note, "bottom" corresponds to the
     *                base-line of normal text. Some characters (eg "y",
     *                "g", "p", etc) descend below the base-line. The
     *                second character may be 'L' for "left", 'C' for
     *                "centre", or 'R' for "right", and specifies the
     *                horizontal location of the reference position. If
     *                the string has less than 2 characters then 'C'
     *                is used for the missing characters.
     * @param   upx   The x component of the up-vector for the text,
     *                in graphics world coordinates. If necessary the 
     *                supplied value should be negated to ensure that 
     *                positive values always refer to displacements from
     *                left to right on the screen.
     * @param   upy   The y component of the up-vector for the text,
     *                in graphics world coordinates. If necessary the 
     *                supplied value should be negated to ensure that
     *                positive values always refer to displacements from
     *                bottom to top on the screen.
     *
     * @return  a two-element array of arrays of doubles.  The first element
     *          is the x-coordinates of the corners, and the second element is
     *          the y-coordinates
     *
     * @throws  Exception  an Exception may be thrown if there is an error
     */
    public float[][] txExt( String text, float x, float y, String just,
                            float upx, float upy ) throws Exception;
}
