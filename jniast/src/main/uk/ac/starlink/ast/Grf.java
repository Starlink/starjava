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

    /** Symbolic capability type indicating escape sequence recognition. */
    public final static int GRF__ESC =
        AstObject.getAstConstantI( "GRF__ESC" );

    /** Symbolic capability type indicating M-type justification. */
    public final static int GRF__MJUST =
        AstObject.getAstConstantI( "GRF__MJUST" );

    /** Symbolic capability type indicating a working {@link #scales} method. */
    public final static int GRF__SCALES =
        AstObject.getAstConstantI( "GRF__SCALES" );

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
     * Draws a polyline (a set of connected lines).
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

    /**
     * Gets the axis scales.
     * This function returns an array containing two values (one for each
     * axis) which scale increments on the corresponding axis into a
     * "normal" coordinate system in which:
     * <ol>
     * <li>The axes have equal scale in terms of (for instance) 
     *     millimetres per unit distance.
     * <li>X values increase from left to right.
     * <li>Y values increase from bottom to top.
     * </ol>
     *
     * @return  a 2-element array;
     *          the first element is the scale for the X axis
     *          (Xnorm = scales[0]*Xworld) and
     *          the second element is the scale for the Y axis
     *          (Ynorm = scales[1]*Yworld)
     * @since  V3.2
     */
    public float[] scales() throws Exception;

    /**
     * Indicates which abilities this <code>Grf</code> implementation has.
     * This method is used by a <code>Plot</code> to determine if
     * this object has given capability.  Which capability is being
     * enquired about is indicated by the value of the <code>cap</code>
     * argument.
     * According to the values of <code>cap</code> and <code>val</code>, 
     * the method should return a value indicating
     * capability as described below:
     * <dl>
     * <dt>GRF__SCALES
     * <dd>Return a non-zero value if it provides a working implementation
     *     of the {@link #scales} method, and zero otherwise.
     *     The <code>value</code> argument should be ignored.
     * <dt>GRF__MJUST
     * <dd>Return a non-zero value if the {@link #text} and {@link #txExt}
     *     methods recognise "M" as a
     *     character in the justification string. If the first character of
     *     a justification string is "M", then the text should be justified
     *     with the given reference point at the bottom of the bounding box.
     *     This is different to "B" justification, which requests that the
     *     reference point be put on the baseline of the text, since some
     *     characters hang down below the baseline. If <code>text</code>
     *     or <code>txExt</code> cannot differentiate between "M" and "B",
     *     then this method should return zero, in which case "M"
     *     justification will never be requested by <code>Plot</code>
     *     The <code>value</code> argument should be ignored.
     * <dt>GRF__ESC
     * <dd>Return a non-zero value if the {@link #text} and {@link #txExt}
     *     methods can recognise and interpret graphics escape sequences
     *     within the supplied string.  Zero should be returned if 
     *     escape sequences cannot be interpreted (in which case the
     *     <code>Plot</code> will interpret them itself if needed).
     *     The <code>value</code> argument should be ignored only if
     *     escape cannot be interpreted by <code>text</code> and 
     *     <code>txExt</code>.  Otherwise <code>value</code> indicates
     *     whether <code>text</code> and <code>txExt</code> should 
     *     interpret escape sequences in subsequent calls.
     *     If <code>value</code> is non-zero then escape sequences 
     *     should be interpreted by <code>text</code> and <code>txExt</code>.
     *     Otherwise they should be drawn as literal text.
     *     See {@link uk.ac.starlink.ast.grf.GrfEscape} for more information
     *     about escape sequences.
     * </dl>
     *
     * @param   cap  graphics capability type - one of 
     *          GRF__SCALES, GRF__MJUST, GRF__ESC as described above
     * @param   value  flag whose meaning is dependent on <code>cap</code>
     *          as described above
     * @see     uk.ac.starlink.ast.grf.GrfEscape
     * @since   V3.2
     */
    public int cap( int cap, int value );
}
