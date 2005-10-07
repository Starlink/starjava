/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 *
 *  History:
 *    30-JUN-2000 (Peter W. Draper):
 *       Original version.
 *    01-MAR-2005 (Peter W. Draper):
 *       Added methods for establishing contexts to group drawn elements.
 */
package uk.ac.starlink.ast.grf;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.swing.JComponent;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.AstObject;

/**
 * This class implements the Grf interface required to draw graphics
 * using JNIAST. It provides the graphics functions for drawing lines,
 * curves, arbitrary text and markers onto a Swing JComponent (using
 * Graphics2D).
 * <p>
 * The state of any graphics drawn is persistent and can be re-drawn
 * onto a Graphics2D object using the paint method. This should be
 * invoked from the paintComponent method of the JComponent.
 * <pre>
 *  public void paintComponent( Graphics g )
 *  {
 *        super.paintComponent( g );
 *        defaultGrf.paint( (Graphics2D)g ):
 *  }
 * </pre>
 * Although if you're using a Plot, then you should use its paint
 * method (which will eventually call this paint method).
 * <p>
 * Standard {@link Color} objects are encoded to an integer using
 * {@link #encodeColor} method,
 * so that all colours can be represented by a unique integer
 * for passing through the Grf interface. To reconstruct a Color use the
 * {@link #decodeColor} method.
 * <p>
 * In addition to the standard Grf interface this implementation also
 * offers a set of additional public methods that can be used
 * directly. For instance a set of double precision methods are
 * available for extra efficiency (the standard Grf interface only
 * works with floats) and methods are provided that control the fonts
 * that are used (see the addFont, deleteFont methods).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DefaultGrf
     implements Grf
{
    //  ============
    //  Constructors
    //  ============

    /**
     * Default constructor
     */
    public DefaultGrf()
    {
        establishContext( DEFAULT );
    }

    /**
     * Constructor Initialise the graphics component being used.
     *
     * @param component normally the component that will be drawn
     *                  onto. This is actually only used for correctly
     *                  examining how fonts will be drawn, so may not
     *                  match the Graphics2D object that is eventually
     *                  drawn into.
     */
    public DefaultGrf( JComponent component )
    {
        establishContext( DEFAULT );
        this.component = component;
    }


    //  ================
    //  Public constants
    //  ================

    /**
     * Constants defining different attributes. Only one that isn't present in
     * the Grf interface.
     */
    public final static int GRF__ALPHA = 5; // Alpha blending fraction

    /**
     * Constant defining a BAD value (used to break polylines), this is the
     * same as AST__BAD by definition.
     */
    public final static double BAD = AstObject.AST__BAD;

    /**
     * Use a plain line drawing style.
     */
    public final static int PLAIN = 1;

    /**
     * Use a dashed line drawing style.
     */
    public final static int DASH = 2;

    /**
     * Use a dotted line drawing style.
     */
    public final static int DOT = 3;

    /**
     * Use a short dash line drawing style.
     */
    public final static int SHORTDASH = 4;

    /**
     * Use a long dash line drawing style.
     */
    public final static int LONGDASH = 5;

    /**
     * Use a dot dash line drawing style.
     */
    public final static int DOTDASH = 6;

    /**
     * Name of the default context list.
     */
    public static final String DEFAULT = "DEFAULT";

    //  ===================
    //  Protected variables
    //  ===================

    /**
     * JComponent (or sub-class) that is to be drawn into. This is
     * needed only for checking out text rendering.
     */
    private JComponent component = null;

    /**
     * Current graphic state
     */
    private DefaultGrfState gstate = new DefaultGrfState();

    /**
     * The current list of graphics context information.
     */
    private ArrayList currentContext = null;

    /**
     * List of all graphics context information.
     */
    private ArrayList masterContext = new ArrayList();

    /**
     * Map of keys for graphics context lists.
     */
    private Map masterMap = new HashMap();

    /**
     * Object for managing the global list of graphics fonts.
     */
    private DefaultGrfFontManager grfFontManager = DefaultGrfFontManager.getReference();

    //  =======
    //  Methods
    //  =======

    /**
     * Set the JComponent used to check drawing of fonts.
     *
     * @param component The new graphic value
     */
    public void setGraphic( JComponent component )
    {
        this.component = component;
    }

    /**
     * Get the JComponent used to check the drawing of fonts.
     *
     * @return The graphic value
     */
    public JComponent getGraphic()
    {
        return component;
    }

    /**
     * Establish a graphics context for grouping a set of related drawing
     * operations. The context is associated with the given String, which you
     * must ensure is unique within your application.
     * The key can be used to reference this context when clearing grahics.
     * A special context {@link #DEFAULT} is recognised as the default
     * context. This is the initial state and can be reused (although
     * switching back will produce a confusion in the Z-ordering of the
     * graphics, all elements drawn to the default context will be rendered
     * first).
     *
     * @param key a key to associate with this context. If null then the
     *            DEFAULT context will be assumed.
     */
    public void establishContext( String key )
    {
        if ( key == null || key.equals( DEFAULT ) ) {

            //  Default context. Retrieve this if available.
            if ( masterContext.size() != 0 ) {
                currentContext = (ArrayList) masterMap.get( key );
                return;
            }
        }
        currentContext = new ArrayList();

        masterMap.put( key, currentContext );
        masterContext.add( currentContext );
    }

    /**
     * Draw the complete current graphics context onto a Graphics2D
     * object. This method should be called whenever the JComponent
     * that we are drawing to needs repainting.
     *
     * @param g2 the Graphics2D object to repaint.
     */
    public void paint( Graphics2D g2 )
    {
        update( g2 );
    }

    /**
     * Clear all graphics buffers. Note this  does not erase drawing, that
     * happens after next paint.
     */
    public void reset()
    {
        masterContext.clear();
        gstate = new DefaultGrfState();
        establishContext( DEFAULT );
    }

    /**
     * Remove and return all graphics in a given context. Returns the complete
     * context, if found, otherwise a null is returned. The returned context
     * maybe re-established using the {@link #reAdd} method (although the
     * Z-order will now be different).
     */
    public Object remove( String key )
    {
        Object removedList = masterMap.remove( key );
        if ( removedList != null ) {
            masterContext.remove( removedList );
            masterMap.put( key, null );
        }
        gstate = new DefaultGrfState();
        return removedList;
    }

    /**
     * Re-add a removed context. The context must have been returned by the
     * {@link #remove} method.
     */
    public void reAdd( String key, Object context )
    {
        if ( key == null || key.equals( DEFAULT ) ) {

            //  Default context. Not allowed, so ignore.
            return;
        }
        if ( context instanceof ArrayList ) {
            currentContext = (ArrayList) context;
            masterMap.put( key, currentContext );
            masterContext.add( currentContext );
        }
    }


    /**
     * Update current graphics context to include a polyline.
     *
     * @param x array of polyline X vertices
     * @param y array of polyline Y vertices
     */
    public void polyline( double[] x, double[] y )
    {
        if ( component != null ) {
            DefaultGrfContainer g =
                new DefaultGrfContainer( DefaultGrfContainer.LINE, x, y,
                                         gstate );
            currentContext.add( g );
        }
    }


    /**
     * Update current graphics context to include a set of markers.
     *
     * @param x array of X positions
     * @param y array of Y positions
     * @param type an integer indicating the type of symbol
     *             required. The possible values are defined as
     *             constants in the DefaultGrfMarker class.
     */
    public void marker( double[] x, double[] y, int type )
    {
        if ( component != null ) {
            DefaultGrfContainer g =
                new DefaultGrfContainer( DefaultGrfContainer.MARK,
                                         x, y, type, gstate );
            currentContext.add( g );
        }
    }


    /**
     * Update current graphic context with a character string.
     *
     * @param text Pointer to the string to be displayed.
     * @param x The reference x coordinate.
     * @param y The reference y coordinate.
     * @param just A character string which specifies the location within the
     *             text string which is to be placed at the reference
     *             position given by x and y. The first character may
     *             be 'T' for "top", 'C' for "centre", or 'B' for
     *             "bottom", and specifies the vertical location of
     *             the reference position. Note, "bottom" corresponds
     *             to the base-line of normal text. Some characters
     *             (eg "y", "g", "p", etc) descend below the
     *             base-line. The second character may be 'L' for
     *             "left", 'C' for "centre", or 'R' for "right", and
     *             specifies the horizontal location of the reference
     *             position. If the string has less than 2 characters
     *             then 'C' is used for the missing characters.
     * @param upx The x component of the up-vector for the text, in graphics
     *            world coordinates. If necessary the supplied value
     *            should be negated to ensure that positive values
     *            always refer to displacements from left to right on
     *            the screen.
     * @param upy The y component of the up-vector for the text, in graphics
     *            world coordinates. If necessary the supplied value
     *            should be negated to ensure that positive values
     *            always refer to displacements from bottom to top on
     *            the screen.
     */
    public void text( String text, double x, double y, String just,
                      double upx, double upy )
    {
        if ( component != null ) {
            DefaultGrfContainer g =
                textProperties( text, x, y, just, upx, upy );
            currentContext.add( g );
        }
    }


    /**
     * Calculate the properties of a text object.
     */
    protected DefaultGrfContainer textProperties( String text, double x,
                                                  double y, String just,
                                                  double upx, double upy )
    {
        //  Get the Graphics2D object from the intended target.
        Graphics2D g2 = (Graphics2D) component.getGraphics();

        //  Get the current font.
        Font f2 = grfFontManager.getFont( (int) gstate.getFont() );

        //  Get the current scale factor.
        double scale = gstate.getSize();

        //  Get a FontRenderContext and the bounds of the string.
        //  Note bounds run from top-left to bottom-right, so the
        //  Y origin needs flipping. XXX only actual use of component.
        FontRenderContext frc = g2.getFontRenderContext();
        Rectangle2D rect = f2.getStringBounds( text, frc );

        //  Convert rectangle into required bounds.
        double[] bbox = new double[8];
        bbox[0] = rect.getX();
        bbox[1] = rect.getY();
        bbox[2] = bbox[0] + rect.getWidth();
        bbox[3] = bbox[1];
        bbox[4] = bbox[2];
        bbox[5] = bbox[1] + rect.getHeight();
        bbox[6] = bbox[0];
        bbox[7] = bbox[5];
        for ( int i = 0; i < 7; i++ ) {
            bbox[i] *= scale;
        }

        //  Work out the rotation in radians. Negative to get angles
        //  running anti-clockwise from X through -Y. AST also seems
        //  to use upx and upy in an inverted sense.
        double angle = -Math.atan2( -upx, upy );

        //  Add in the translation to the correct x,y anchor
        //  position.
        double[] anchor = getAnchor( just, angle, x, y, bbox );

        //  Create transformation of text by this angle.
        AffineTransform trans =
            AffineTransform.getTranslateInstance( anchor[0], anchor[1] );
        trans.rotate( angle );
        trans.scale( scale, scale );

        //  Transform the text bounds to this position.
        double[] bounds = new double[8];
        trans.transform( bbox, 0, bounds, 0, 4 );

        //  Record the text properties and return them.
        return new DefaultGrfContainer( DefaultGrfContainer.TEXT,
                                        text, anchor[0], anchor[1],
                                        angle, bounds, gstate );
    }


    /**
     * Flush all graphics, thereby redrawing them. Required for AST interface,
     * normally do this by (re)paint methods.
     */
    public void flush()
    {
        if ( component != null ) {
            component.repaint();
        }
    }


    /**
     * Return the extent of a character string.
     *
     * <p><b>Note:</b>
     * The text rotation and position is determined by creating an
     * affine transformation and applying this directly to the unrotated
     * bounding box. I hope this saves time and is faster than adding a
     * transform to the Graphics2D object.
     *
     * @param text Pointer to the string to be measured.
     * @param x The reference x coordinate.
     * @param y The reference y coordinate.
     * @param just A character string which specifies the location within the
     *             text string which is to be placed at the reference
     *             position given by x and y. The first character may
     *             be 'T' for "top", 'C' for "centre", or 'B' for
     *             "bottom", and specifies the vertical location of
     *             the reference position. Note, "bottom" corresponds
     *             to the base-line of normal text. Some characters
     *             (eg "y", "g", "p", etc) descend below the
     *             base-line. The second character may be 'L' for
     *             "left", 'C' for "centre", or 'R' for "right", and
     *             specifies the horizontal location of the reference
     *             position. If the string has less than 2 characters
     *             then 'C' is used for the missing characters.
     * @param upx The x component of the up-vector for the text, in graphics
     *            world coordinates. If necessary the supplied value should be
     *            negated to ensure that positive values always refer to
     *            displacements from left to right on the screen.
     * @param upy The y component of the up-vector for the text, in graphics
     *            world coordinates. If necessary the supplied value should be
     *            negated to ensure that positive values always refer to
     *            displacements from bottom to top on the screen.
     * @return double[] the X and Y bounds
     */
    public double[] textExtent( String text, double x, double y, String just,
                                double upx, double upy )
    {
        if ( component != null ) {
            DefaultGrfContainer cont =
                textProperties( text, x, y, just, upx, upy );
            return cont.getBBox();
        }
        return null;
    }


    /**
     * Return the character height in component coordinates.
     *
     * @return double[2] X and Y size of characters.
     */
    public double[] charHeight()
    {
        double[] sizes = new double[2];
        double[] bounds;

        //  Just use textExtent on a simple string.
        bounds = textExtent( "ABCD", 0.0, 0.0, "CC", 1.0, 0.0 );
        sizes[0] = bounds[4] - bounds[0];
        bounds = textExtent( "ABCD", 0.0, 0.0, "CC", 0.0, 1.0 );
        sizes[1] = bounds[5] - bounds[1];
        return sizes;
    }


    /**
     * Enquire or set a graphics attribute value This function returns the
     * current value of a specified graphics attribute, and optionally
     * establishes a new value. The supplied value is converted to an integer
     * value if necessary.
     *
     * @param attr An integer value identifying the required attribute. The
     *             following symbolic values are defined:
     *             <ul>
     *                <li> GRF__STYLE - Line style.
     *                <li> GRF__WIDTH - Line width.
     *                <li> GRF__SIZE - Character and marker size scale factor.
     *                <li> GRF__FONT - Character font.
     *                <li> GRF__COLOUR - Colour index.
     *                <li> GRF__ALPHA - Alpha blending fraction.
     *             </ul>
     *
     * @param value A new value to store for the attribute. If this is BAD no
     *              value is stored.
     * @param prim The sort of graphics primitive to be drawn with the new
     *             attribute. Identified by the following values:
     *             <ul>
     *                <li> GRF__LINE
     *                <li> GRF__MARK
     *                <li> GRF__TEXT
     *             </ul>
     *
     * @return The current attribute value (before change).
     */
    public double attribute( int attr, double value, int prim )
    {
        int ival;
        double dval;
        double oldValue;

        //  If required retrieve the current line style, and set a new line
        //  style.
        if ( attr == GRF__STYLE ) {
            oldValue = gstate.getStyle();
            if ( value != BAD ) {
                gstate.setStyle( value );
            }
        }
        else if ( attr == GRF__WIDTH ) {

            //  Line widths are really integers starting at 1.
            oldValue = gstate.getWidth();
            if ( value != BAD ) {
                gstate.setWidth( Math.max( 1, value ) );
            }
        }
        else if ( attr == GRF__SIZE ) {

            //  If required retrieve the current character size, and
            //  set a new size.  The attribute value should be a
            //  factor by which to multiply the default character
            //  size.
            oldValue = gstate.getSize();
            if ( value != BAD ) {
                gstate.setSize( value );
            }
        }
        else if ( attr == GRF__FONT ) {

            // If required retrieve the current character font, and
            // set a new font.
            oldValue = gstate.getFont();
            if ( value != BAD ) {
                gstate.setFont( value );
            }
        }
        else if ( attr == GRF__COLOUR ) {

            //  If required retrieve the current colour index, and set
            //  a new colour index.
            oldValue = gstate.getColour();
            if ( value != BAD ) {
                gstate.setColour( value );
            }
        }
        else if ( attr == GRF__ALPHA ) {
            oldValue = gstate.getAlpha();
            if ( value != BAD ) {
                gstate.setAlpha( Math.min( 1.0, Math.max( 0.0, value ) ) );
            }
        }
        else {
            //  Invalid attribute.
            return BAD;
        }
        return oldValue;
    }


    /**
     * Get the axis scales. Java assumes an ideal pixel size of 72dpi.
     */
    public double[] axisScale()
    {
        //  Java assumes ideal pixel size of 72dpi. Y runs from the
        //  top to the bottom.

        double[] scales = new double[2];
        scales[0] = 1.0 / 72.0;
        scales[1] = -scales[0];
        return scales;
    }


    /**
     * Add a new font.
     *
     * @param font the new Font.
     * @return the graphics index of the Font.
     */
    public int addFont( Font font )
    {
        return grfFontManager.add( font );
    }


    /**
     * Disable a font by removing it from the known list. Do this when a font
     * is no longer required (or can no longer be referenced).
     *
     * @param font the font to remove from the known list
     * @return the graphics index of the Font.
     */
    public int deleteFont( Font font )
    {
        return grfFontManager.remove( font );
    }


    /**
     * Get the graphics index of a known font.
     *
     * @param font the font to locate.
     * @return index of the font or the default font if not known.
     */
    public int getFontIndex( Font font )
    {
        return grfFontManager.getIndex( font );
    }


    /**
     * Get a known font by graphics index.
     *
     * @param index graphics index of the required font.
     * @return the known font or, if unknown the default font.
     */
    public Font getFont( int index )
    {
        return grfFontManager.getFont( index );
    }


    /**
     * Set the clipping region (null to reset).
     *
     * @param region a region specifying the outer limits of the region to
     *               draw in.
     */
    public void setClipRegion( Rectangle region )
    {
        gstate.setClip( region );
    }


    /**
     * Return the X and Y AST anchor position of a string.
     *
     * @param just the AST justification string.
     * @param angle rotation of text when plotted.
     * @param x the x coordinate of the bounding box lower corner.
     * @param y the y coordinate of the bounding box lower corner.
     * @param bbox the bounding box of the text that is to be plotted (from
     *             Font.getStringBounds()).
     * @return double[2] the x,y positions of the anchor point, this is
     *         adjusted so that the actual rotation about the lower lefthand
     *         corner of the text string gives the correct positioning.
     */
    protected double[] getAnchor( String just, double angle,
                                  double x, double y, double[] bbox )
    {
        //  Local variables
        int lenJust = just.length();
        char[] localJust = new char[2];
        double[] anchor = new double[2];

        //  Convert anchor into fully expressed version (missing
        //  fields are 'C').
        if ( lenJust == 0 ) {
            localJust[0] = 'C';
            localJust[1] = 'C';
        }
        else if ( lenJust == 1 ) {
            localJust[0] = just.charAt( 0 );
            localJust[1] = 'C';
        }
        else {
            localJust[0] = just.charAt( 0 );
            localJust[1] = just.charAt( 1 );
        }

        //  Get width and height of text bounding box.
        double w = bbox[2] - bbox[0];
        double h = bbox[5] - bbox[1];

        //  Now get the relative anchor position. Note these return the
        //  position where a BL anchored string should be rotation from
        //  to simulate the correct anchor and rotation.
        switch ( localJust[0] ) {
            case 'C':
                switch ( localJust[1] ) {
                    case 'C':
                        anchor[0] = x - w * 0.5 * Math.cos( angle ) -
                                        h * 0.5 * Math.sin( angle );
                        anchor[1] = y - w * 0.5 * Math.sin( angle ) +
                                        h * 0.5 * Math.cos( angle );
                        break;
                    case 'L':
                        anchor[0] = x - h * 0.5 * Math.sin( angle );
                        anchor[1] = y + h * 0.5 * Math.cos( angle );
                        break;
                    case 'R':
                        anchor[0] = x - w * Math.cos( angle ) -
                                        h * 0.5 * Math.sin( angle );
                        anchor[1] = y - w * Math.sin( angle ) +
                                        h * 0.5 * Math.cos( angle );
                        break;
                }
                break;
            case 'T':
                switch ( localJust[1] ) {
                    case 'C':
                        anchor[0] = x - w * 0.5 * Math.cos( angle ) -
                                        h * Math.sin( angle );
                        anchor[1] = y - w * 0.5 * Math.sin( angle ) +
                                        h * Math.cos( angle );
                        break;
                    case 'L':
                        anchor[0] = x - h * Math.sin( angle );
                        anchor[1] = y + h * Math.cos( angle );
                        break;
                    case 'R':
                        anchor[0] = x - w * Math.cos( angle ) -
                                        h * Math.sin( angle );
                        anchor[1] = y - w * Math.sin( angle ) +
                                        h * Math.cos( angle );
                        break;
                }
                break;
            case 'B':
                switch ( localJust[1] ) {
                    case 'C':
                        anchor[0] = x - w * 0.5 * Math.cos( angle );
                        anchor[1] = y - w * 0.5 * Math.sin( angle );
                        break;
                    case 'L':
                        anchor[0] = x;
                        anchor[1] = y;
                        break;
                    case 'R':
                        anchor[0] = x - w * Math.cos( angle );
                        anchor[1] = y - w * Math.sin( angle );
                        break;
                }
                break;
        }
        return anchor;
    }


    /**
     * Draw the all known graphics to a component. Does the real work.
     */
    protected void update( Graphics2D g2 )
    {
        DefaultGrfContainer cont;
        ArrayList context = null;
        int size = 0;

        //  Recover the context lists in the order they were added.
        for ( int j = 0; j < masterContext.size(); j++ ) {
            context = (ArrayList) masterContext.get( j );

            //  Render each context container in the order they were added.
            size = context.size();
            for ( int i = 0; i < size; i++ ) {
                cont = (DefaultGrfContainer) context.get( i );
                if ( cont.getType() == DefaultGrfContainer.LINE ) {
                    drawLine( g2, cont );
                }
                else if ( cont.getType() == DefaultGrfContainer.MARK ) {
                    drawMark( g2, cont );
                }
                else if ( cont.getType() == DefaultGrfContainer.TEXT ) {
                    drawText( g2, cont );
                }
            }
        }
    }


    /**
     * Draw a polyline onto a given Graphics2D object, using the state in a
     * given GrfContainer.
     *
     * @param g2 the Graphics2D object that we are drawing into.
     * @param cont container with the graphics state set.
     */
    public static void drawLine( Graphics2D g2, DefaultGrfContainer cont )
    {
        //  Extract line specific information and restore graphics
        //  state
        double[] xPos = cont.getXPositions();
        double[] yPos = cont.getYPositions();
        DefaultGrfState gstate = (DefaultGrfState) cont.getGrfState();

        //  Set the colour.
        Color oldcolour = g2.getColor();
        g2.setColor( new Color( (int) gstate.getColour() ) );

        //  Set line thickness and dashes.
        Stroke oldstroke = g2.getStroke();
        g2.setStroke( lineStroke( (int) gstate.getStyle(),
                                  (float) gstate.getWidth() ) );

        //  Set the clipping region. If an explicit one is given then
        //  use its union with the graphics clip.
        Rectangle localClip = gstate.getClip();
        Shape oldclip = g2.getClip();
        if ( localClip != null ) {
            g2.clipRect( localClip.x, localClip.y, localClip.width,
                         localClip.height );
        }
        Rectangle globalClip = g2.getClipBounds();

        //  Set the alpha blending fraction.
        Composite oldAlpha = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   (float)gstate.getAlpha()) );

        //  Define an extent of values along the X axis that need to be
        //  drawn. This is a guess which we use to cut down on the number of
        //  points re-drawn when zoomed. It speeds up rendered lines a lot
        //  (these are drawn completely to see if they intersect the graphics
        //  clip, which can be very slow, when thick, stroked and
        //  anti-aliased). Note some very zoomed plots could have missing
        //  lines at the edges with this strategy because the axis
        //  border-lines can extend from one side of the component to the
        //  other they can be completely rejected, so lines with a few points
        //  are always drawn.
        int xlower = Integer.MIN_VALUE;
        int xupper = Integer.MAX_VALUE;
        if ( globalClip != null ) {
            xlower = globalClip.x - globalClip.width;
            xupper = globalClip.x + globalClip.width * 2;
        }

        //  Draw the line. If we encounter any breaks (that is occurences of
        //  BAD), then need to do this in segments.
        int x[] = new int[xPos.length];
        int y[] = new int[xPos.length];
        int j = 0;
        int t = 0;
        if ( xPos.length > 20 ) {

            //  Clip long lines.
            for ( int i = 0; i < xPos.length; i++ ) {
                if ( xPos[i] != BAD && yPos[i] != BAD ) {
                    x[j] = (int) xPos[i];
                    if ( x[j] > xlower && x[j] < xupper ) {
                        y[j] = (int) yPos[i];
                        //  Skip "same" positions in graphics coords. We've
                        //  already drawn that dot! This speeds up graphics
                        //  that are very small.
                        if ( j > 0 ) {
                            if ( y[j] != y[j - 1] || x[j] != x[j - 1] ) {
                                j++;
                                t++;
                            }
                        }
                        else {
                            j = 1;
                        }
                    }
                }
                else {
                    if ( j > 0 ) {
                        g2.drawPolyline( x, y, j );
                        j = 0;
                    }
                }
            }
            if ( j > 0 ) {
                g2.drawPolyline( x, y, j );
            }

            //  We must draw a "few" points for the sense of the polyline to
            //  be clear (the 4 comes from watching the SPLAT vertical hair
            //  with histogram spectrum, this just redraws the line, plus some
            //  "damage", since the damage region is small not all local lines
            //  are re-drawn).
            if ( t < 4 ) {
                int[] bounds = bound( (double) xlower, xPos );
                // Pick a few extra points...
                int lower = Math.max( 0, bounds[0] - 5 );
                int upper = Math.min( xPos.length, bounds[1] + 5 );
                j = 0;
                for ( int i = lower; i < upper; i++ ) {
                    if ( xPos[i] != BAD && yPos[i] != BAD ) {
                        x[j] = (int) xPos[i];
                        y[j] = (int) yPos[i];
                        j++;
                    }
                    else {
                        if ( j > 0 ) {
                            g2.drawPolyline( x, y, j );
                            j = 0;
                        }
                    }
                }
                if ( j > 0 ) {
                    g2.drawPolyline( x, y, j );
                }
            }
        }
        else {

            //  Short-lines are not clipped.
            for ( int i = 0; i < xPos.length; i++ ) {
                if ( xPos[i] != BAD && yPos[i] != BAD ) {
                    x[j] = (int) xPos[i];
                    y[j] = (int) yPos[i];
                    j++;
                }
                else {
                    if ( j > 0 ) {
                        g2.drawPolyline( x, y, j );
                        j = 0;
                    }
                }
            }
            if ( j > 0 ) {
                g2.drawPolyline( x, y, j );
            }
        }

        //  Restore stroke, colour and clip.
        g2.setStroke( oldstroke );
        g2.setColor( oldcolour );
        g2.setClip( oldclip );
        g2.setComposite( oldAlpha );
    }


    /**
     * Create a Stroke for drawing a line using one of the possible line
     * styles.
     *
     * @param type one of the possible types: PLAIN, DASH, DOT, SHORTDASH,
     *             LONGDASH or DOTDASH.
     * @param width the width of the line that will be drawn with this Stroke.
     * @return the Stroke
     */
    public static Stroke lineStroke( int type, float width )
    {
        switch ( type ) {
           case PLAIN:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND );
           case DASH:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND, 10.0f,
                                       new float[]{6, 6}, 0 );
           case SHORTDASH:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND, 10.0f,
                                       new float[]{3, 3}, 0 );
           case DOT:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND, 10.0f,
                                       new float[]{1, 1}, 0 );
           case LONGDASH:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND, 10.0f,
                                       new float[]{12, 12}, 0 );
           case DOTDASH:
               return new BasicStroke( width, BasicStroke.CAP_BUTT,
                                       BasicStroke.JOIN_ROUND, 10.0f,
                                       new float[]{3, 3, 6, 3}, 0 );
        }

        //  If not a known type, just return a default Stroke.
        return new BasicStroke( width );
    }


    /**
     * Draw a text string.
     */
    protected void drawText( Graphics2D g2, DefaultGrfContainer cont )
    {
        //  Need a valid position to draw text.
        if ( cont.getX() == BAD || cont.getY() == BAD ) {
            return;
        }
        float x = (float) cont.getX();
        float y = (float) cont.getY();
        DefaultGrfState gstate = (DefaultGrfState) cont.getGrfState();

        //  Set the clipping region. If an explicit one if given then
        //  use it's union with the graphics clip.
        Rectangle localClip = gstate.getClip();
        Shape oldclip = g2.getClip();
        if ( localClip != null ) {
            g2.clipRect( localClip.x, localClip.y, localClip.width,
                         localClip.height );
        }

        //  If text doesn't lie in the current clipping region then
        //  don't draw it (Graphics2D is more careful and inspects off
        //  image text too, which can be quite slow as it checks for
        //  the effects of any strokes etc., which have a width). Note
        //  this isn't accurate, we should work with the intersection
        //  of the rotated and scaled Shape. -100, +200 pixels is just a
        //  longish string (vertical or horizontal). When a local clip is
        //  being used, try to preserve that as much as possible, so just
        //  do the obvious test.
        if ( localClip == null ) {
            if ( ! g2.hitClip( (int) x - 100, (int) y - 100, 200, 200 ) ) {
                return;
            }
        }
        else {
            if ( ! g2.hitClip( (int) x, (int) y, 1, 1 ) ) {
                return;
            }
        }

        //  Get the state associated with this request.
        String text = cont.getText();
        double angle = cont.getAngle();
        double size = gstate.getSize();

        //  Set the text colour.
        g2.setColor( new Color( (int) gstate.getColour() ) );

        //  Establish text font
        g2.setFont( grfFontManager.getFont( (int) gstate.getFont() ) );

        //  Record current transformation.
        AffineTransform oldtrans = g2.getTransform();

        //  Translate to centre of text.
        g2.translate( x, y );

        //  Add in the scale.
        if ( size != 1.0 ) {
            g2.transform( AffineTransform.getScaleInstance( size, size ) );
        }

        //  Add in the rotation, if needed.
        if ( angle != 0.0 ) {
            g2.transform( AffineTransform.getRotateInstance( angle ) );
        }

        //  Draw the text.
        g2.drawString( text, 0.0f, 0.0f );

        //  Restore transform.
        g2.setTransform( oldtrans );

        //  Restore clip.
        g2.setClip( oldclip );
    }


    /**
     * Draw a graphics marker.
     */
    protected void drawMark( Graphics2D g2, DefaultGrfContainer cont )
    {
        //  Extract line specific information and restore graphics
        //  state
        double[] x = cont.getXPositions();
        double[] y = cont.getYPositions();
        int type = cont.getInt();
        DefaultGrfState gstate = (DefaultGrfState) cont.getGrfState();
        double size = gstate.getSize();

        //  Set the marker colour.
        g2.setColor( new Color( (int) gstate.getColour() ) );

        //  Store current stroke so that it can be restored.
        Stroke oldstroke = g2.getStroke();

        //  Set line thickness and dashes.
        g2.setStroke( lineStroke( (int) gstate.getStyle(),
            (float) gstate.getWidth() ) );

        //  Set the clipping region. If an explicit one if given then
        //  use it's union with the graphics clip.
        Rectangle localClip = gstate.getClip();
        Shape oldclip = g2.getClip();
        if ( localClip != null ) {
            g2.clipRect( localClip.x, localClip.y, localClip.width,
                         localClip.height );
        }

        //  Draw the marker at each position. Skip those that don't
        //  lie within the clip region.
        for ( int i = 0; i < x.length; i++ ) {
            if ( x[i] != BAD && y[i] != BAD ) {
                if ( g2.hitClip( (int) x[i], (int) y[i], 1, 1 ) ) {
                    DefaultGrfMarker.draw( g2, type, x[i], y[i], size );
                }
            }
        }

        //  Restore Stroke.
        g2.setStroke( oldstroke );

        //  Restore clip.
        g2.setClip( oldclip );
    }

    /**
     * Locate the indices of the two coordinates that lie closest to a
     * given coordinate.
     *
     * @param xcoord the coordinate value to bound.
     * @param values the coordinates to check.
     *
     * @return array of two integers, the lower and upper indices.
     */
    public static int[] bound( double xcoord, double[] values )
    {
        //  Use a binary search.
        int low = 0;
        int high = values.length - 1;
        int mid = 0;
        boolean increases = ( values[low] < values[high] );

        if ( increases ) { 
            while ( low < high - 1 ) {
                mid = ( low + high ) / 2;
                if ( xcoord < values[mid] ) {
                    high = mid;
                }
                else {
                    low = mid;
                }
            }
        }
        else {
            while ( low < high - 1 ) {
                mid = ( low + high ) / 2;
                if ( xcoord > values[mid] ) {
                    high = mid;
                }
                else {
                    low = mid;
                }
            }

        }
        int bounds[] = new int[2];
        bounds[0] = low;
        bounds[1] = high;
        return bounds;
    }

    /**
     * Encode a Color to a unique integer value that can be used in
     * the Grf interface.
     *
     * @param colour the Color to encode.
     * @return the integer representation.
     */
    public static int encodeColor( Color colour )
    {
        int result = colour.getRGB();
        if ( result == -1 ) {
            //  White is a special case. The AST Grf internals see a
            //  -1 value as a flag to not use a colour. This may give
            //  us problems with alpha-blending...
            result = 0;
        }
        return result;
    }

    /**
     *  Decode a colour encoded as an integer using encodeColor.
     *
     *  @param value the integer that represents a colour.
     *  @return a Color object.
     */
    public static Color decodeColor( int value )
    {
        if ( value == 0 ) {
            value = -1; // White is a special case.
        }
        return new Color( value );
    }

    //
    // Grf implementation. Mostly pinched from the old JNIAST SplatGrf
    // by Mark Taylor.
    //
    public void paint( Graphics g )
    {
        update( (Graphics2D) g );
    }

    public void clear()
    {
        reset();
    }

    public void line( int n, float[] x, float[] y )
    {
        double[] dx = new double[n];
        double[] dy = new double[n];
        for ( int i = 0; i < n; i++ ) {
            dx[i] = (double) x[i];
            dy[i] = (double) y[i];
        }
        polyline( dx, dy );
    }

    public void mark( int n, float[] x, float[] y, int type )
    {
        double[] dx = new double[n];
        double[] dy = new double[n];
        for ( int i = 0; i < n; i++ ) {
            dx[i] = (double) x[i];
            dy[i] = (double) y[i];
        }
        marker( dx, dy, type );
    }

    public void text( String text, float x, float y, String just,
                      float upx, float upy )
    {
        text( text, (double) x, (double) y, just, (double) upx, (double) upy );

    }

    /**
     * Enquires or sets a graphics attribute value.  The current value is
     * returned, and optionally a new value may be established.  The
     * attribute is in all cases represented by a double precision value,
     * which is interpreted according to the attribute as described
     * below.
     *
     * <dl>
     * <dt>attr=GRF__STYLE
     * <dd>line style - one of
     *    <ul>
     *    <li>PLAIN
     *    <li>DASH
     *    <li>DOT
     *    <li>SHORTDASH
     *    <li>LONGDASH
     *    <li>DOTDASH
     *    </ul>
     * <dt>attr=GRF__WIDTH
     * <dd>line width - an value giving the width of drawn lines
     * <dt>attr=GRF__SIZE
     * <dd>character/marker size scale factor - 1.0 is normal size
     * <dt>attr=GRF__FONT
     * <dd>font index for drawn text.  This is an integer mapped to a font
     *     by the {@link DefaultGrfFontManager} class.
     * <dt>attr=GRF__COLOUR
     * <dd>integer interpreted as a 32-bit alpha-red-green-blue value,
     *     as per the result of a {@link #encodeColor} call.
     *     <p>
     *     Normally bits 0-23 of the integer represent the red green
     *     and blue intensities in the usual way, and bits 24-31
     *     represent the alpha value (<code>0xff</code> means opaque and
     *     <code>0x00</code> means transparent), the exception being
     *     the value opaque white which is 0x00000000.
     *     <p>
     *     Note this means that a 24-bit colour value along the lines
     *     <code>0xc0c0c0</code> would give you a completely transparent
     *     colour - probably not what you want.  To specify normal
     *     (opaque) grey, you should use <code>0xffc0c0c0</code> or
     *     equivalantly <code>encodeColor(Color.LIGHT_GRAY)</code>.
     *     </ul>
     * </dl>
     * <p>
     */
    public double attr( int attr, double value, int prim )
    {
        if ( attr == GRF__COLOUR ) {

            // Treat the colour attribute specially - instead of just
            // forwarding the call to the attribute method, we split the
            // value into an RGB part (bits 0-23) which gets forwarded to
            // atribute(GRF__COLOUR,,) and an alpha part (bits 24-31) which
            // get forwarded to attribute(GRF__ALPHA,,). Remember to
            // deal with opaque white special case too.
            double retrgb;
            double retalpha;
            if ( value != AstObject.AST__BAD ) {
                int ivalue = (int) value;
                if ( ivalue == 0 ) ivalue = -1;  // Opaque white.
                double rgb = (double) ( ivalue & 0x00ffffff );
                int opaque = ( ivalue & 0xff000000 ) >>> 24;
                double alpha = ( opaque / 255.0 );
                retrgb = attribute( GRF__COLOUR, rgb, prim );
                retalpha = attribute( GRF__ALPHA, alpha, prim );
            }
            else {
                retrgb = attribute( GRF__COLOUR, AstObject.AST__BAD, prim );
                retalpha = attribute( GRF__ALPHA, AstObject.AST__BAD, prim );
            }
            int retopaque = (int) ( ( retalpha ) * 255.9 );
            double result = (double)( ( retopaque << 24 ) | ( (int) retrgb ) );
            if ( result == -1.0 ) {
                result = 0.0;                      // Opaque white
            }
            return result;
        }
        else {
            return attribute( attr, value, prim );
        }
    }

    public float[] qch()
    {
        double[] dch = charHeight();
        return new float[]{(float) dch[0], (float) dch[1]};
    }

    public float[][] txExt( String text, float x, float y, String just,
                            float upx, float upy )
    {
        double[] dte = textExtent( text, (double) x, (double) y,
                                   just, (double) upx,
                                   (double) upy );
        float[] xte = new float[]{(float) dte[6], (float) dte[4],
                                  (float) dte[2], (float) dte[0]};
        float[] yte = new float[]{(float) dte[7], (float) dte[5],
                                  (float) dte[3], (float) dte[1]};
        return new float[][]{xte, yte};
    }

    public int cap( int cap, int value )
    {
        //  Just support scales and let AST control the interpretation of
        //  all escape sequences.
        if ( cap == GRF__SCALES ) {
            return 1;
        }
        return 0;
    }

    public float[] scales()
    {
        //  For a JComponent we assume the scales are required to be square
        //  and fixed, so we cannot work out better values than 1, -1.
        return new float[] { 1.0f, -1.0f };
    }
}
