/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-MAY-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.grf;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Java class to draw a marker into a Graphics2D object. The range of markers
 * provided are defined as constants of this class. The default is a
 * circle. Markers are defined using a position and a typical size,
 * rather than precise geometries.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DefaultGrfMarker
{
    /**
     * Use a dot marker.
     */
    public final static int DOT = 0;

    /**
     * Use a cross marker.
     */
    public final static int CROSS = 1;

    /**
     * Use a plus marker.
     */
    public final static int PLUS = 2;

    /**
     * Use a square marker.
     */
    public final static int SQUARE = 3;

    /**
     * Use a circle marker.
     */
    public final static int CIRCLE = 4;

    /**
     * Use a diamond marker.
     */
    public final static int DIAMOND = 5;

    /**
     * Use a pointing up triangle marker.
     */
    public final static int UPTRIANGLE = 6;

    /**
     * Use a pointing down triangle marker.
     */
    public final static int DOWNTRIANGLE = 7;

    /**
     * Use a filled square marker.
     */
    public final static int FILLEDSQUARE = 8;

    /**
     * Use a filled circle marker.
     */
    public final static int FILLEDCIRCLE = 9;

    /**
     * Use a filled diamond marker.
     */
    public final static int FILLEDDIAMOND = 10;

    /**
     * Use a filled pointing up triangle marker.
     */
    public final static int FILLEDUPTRIANGLE = 11;

    /**
     * Use a filled pointing down triangle marker.
     */
    public final static int FILLEDDOWNTRIANGLE = 12;;

    /**
     * Descriptions of the various types of markers. This is indexed by the
     * public variables.
     */
    private final static String[] descriptions = {
        "dot",
        "cross",
        "plus",
        "square",
        "circle",
        "diamond",
        "triangle",
        "filled square",
        "filled circle",
        "filled diamond",
        "filled triangle"
    };


    /**
     * Draw a marker at a given position.
     *
     * @param g2 the Graphics2D to draw onto
     * @param type the type of marker to draw. Must be one of the
     *             defined constants, CIRCLE, SQUARE etc.
     * @param x the X centre of the marker (graphics coordinates)
     * @param y the Y centre of the marker (graphics coordinates)
     * @param size the size of the marker (graphics coordinates)
     */
    public static void draw( Graphics2D g2, int type, double x, double y,
                             double size )
    {
        switch ( type ) {
            case DOT:
                drawDot( g2, x, y, size );
                break;
            case CROSS:
                drawCross( g2, x, y, size );
                break;
            case PLUS:
                drawPlus( g2, x, y, size );
                break;
            case SQUARE:
                drawSquare( g2, x, y, size, false );
                break;
            case CIRCLE:
                drawCircle( g2, x, y, size, false );
                break;
            case DIAMOND:
                drawDiamond( g2, x, y, size, false );
                break;
            case UPTRIANGLE:
                drawTriangle( g2, x, y, size, true, false );
                break;
            case DOWNTRIANGLE:
                drawTriangle( g2, x, y, size, false, false );
                break;
            case FILLEDSQUARE:
                drawSquare( g2, x, y, size, true );
                break;
            case FILLEDCIRCLE:
                drawCircle( g2, x, y, size, true );
                break;
            case FILLEDDIAMOND:
                drawDiamond( g2, x, y, size, true );
                break;
            case FILLEDUPTRIANGLE:
                drawTriangle( g2, x, y, size, true, true );
                break;
            case FILLEDDOWNTRIANGLE:
                drawTriangle( g2, x, y, size, false, true );
                break;
            default:
                drawCircle( g2, x, y, size, true );
                break;
        }
    }


    /**
     * Get a user presentable description of a marker.
     *
     * @param type the type of marker (POINT etc.).
     * @return the marker description.
     */
    public static String getDescription( int type )
    {
        return descriptions[type];
    }


    /**
     * Draw a dot, should be a point really, but is a filled square.
     */
    protected static void drawDot( Graphics2D g2, double x, double y,
                                   double size )
    {
        if ( size > 0.0 ) {
            drawSquare( g2, x, y, size, true );
        }
        else {
            drawSquare( g2, x, y, 0.001, true );
        }
    }


    /**
     * Draw a cross.
     */
    protected static void drawCross( Graphics2D g2, double x, double y,
                                     double size )
    {
        double half = size * 0.5;
        g2.draw( new Line2D.Double( x - half, y + half, x + half, y - half ) );
        g2.draw( new Line2D.Double( x - half, y - half, x + half, y + half ) );
    }


    /**
     * Draw a plus.
     */
    protected static void drawPlus( Graphics2D g2, double x, double y,
                                    double size )
    {
        double half = size * 0.5;
        g2.draw( new Line2D.Double( x - half, y, x + half, y ) );
        g2.draw( new Line2D.Double( x, y - half, x, y + half ) );
    }


    /**
     * Draw a square.
     */
    protected static void drawSquare( Graphics2D g2, double x, double y,
                                      double size, boolean filled )
    {
        double half = size * 0.5;
        Rectangle2D.Double square = new Rectangle2D.Double( x - half,
                                                            y - half,
                                                            size, size );
        g2.draw( square );
        if ( filled ) {
            g2.fill( square );
        }
    }


    /**
     * Draw a circle.
     */
    protected static void drawCircle( Graphics2D g2, double x, double y,
                                      double size, boolean filled )
    {
        double half = size * 0.5;
        Ellipse2D.Double circle = new Ellipse2D.Double( x - half,
                                                        y - half,
                                                        size, size );
        g2.draw( circle );
        if ( filled ) {
            g2.fill( circle );
        }
    }


    /**
     * Draw a diamond.
     */
    protected static void drawDiamond( Graphics2D g2, double x, double y,
                                       double size, boolean filled )
    {
        double half = size * 0.5;
        GeneralPath path = new GeneralPath();
        path.moveTo( (float) ( x - half ), (float) y );
        path.lineTo( (float) x, (float) ( y - half ) );
        path.lineTo( (float) ( x + half ), (float) y );
        path.lineTo( (float) x, (float) ( y + half ) );
        path.lineTo( (float) ( x - half ), (float) y );
        g2.draw( path );
        if ( filled ) {
            g2.fill( path );
        }
    }


    /**
     * Draw a triangle
     */
    protected static void drawTriangle( Graphics2D g2, double x, double y,
                                        double size, boolean up, 
                                        boolean filled )
    {
        double half = size * 0.5;
        GeneralPath path = new GeneralPath();
        if ( up ) {
            path.moveTo( (float) ( x - half ), (float) ( y - half ) );
            path.lineTo( (float) ( x + half ), (float) ( y - half ) );
            path.lineTo( (float) x, (float) ( y + half ) );
            path.lineTo( (float) ( x - half ), (float) ( y - half ) );
        }
        else {
            path.moveTo( (float) ( x - half ), (float) ( y + half ) );
            path.lineTo( (float) ( x + half ), (float) ( y + half ) );
            path.lineTo( (float) x, (float) ( y - half ) );
            path.lineTo( (float) ( x - half ), (float) ( y + half ) );
        }
        g2.draw( path );
        if ( filled ) {
            g2.fill( path );
        }
    }
}
