/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-DEC-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.Figure;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import uk.ac.starlink.diva.interp.Interpolator;;

/**
 * This class creates and enumerates the possible instance of Figures
 * that can be created on an instance of {@link Draw} for use with a
 * {@link DrawActions} instance.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DrawFigureFactory
{
    //
    // Enumeration of the "types" of Figure that can be created.
    //
    public static final int LINE = 0;
    public static final int RECTANGLE = 1;
    public static final int ELLIPSE = 2;
    public static final int POLYLINE = 3;
    public static final int POLYGON = 4;
    public static final int FREEHAND = 5;
    public static final int TEXT = 6;
    public static final int CURVE = 7;
    public static final int XRANGE = 8;

    /** Simple names for the various figures */
    public static final String[] shortNames =
    {
        "line",
        "rectangle",
        "ellipse",
        "polyline",
        "polygon",
        "freehand",
        "text",
        "curve",
        "xrange"
    };

    /** Number of figure types supported */
    public static final int NUM_FIGURES = shortNames.length;

    /**
     *  Create the single class instance.
     */
    private static final DrawFigureFactory instance = new DrawFigureFactory();

    /**
     *  Hide the constructor from use.
     */
    private DrawFigureFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static DrawFigureFactory getReference()
    {
        return instance;
    }

    /**
     *  Create a Figure of the given type using the specified
     *  properties to initialise it.
     */
    public Figure create( int type, FigureProps props )
    {
        Figure newFigure = null;

        switch ( type ) 
        {
           case LINE:
               newFigure = createLine( props );
               break;
           case RECTANGLE:
               newFigure = createRectangle( props );
               break;
           case ELLIPSE:
               newFigure = createEllipse( props );
               break;
           case POLYLINE:
               newFigure = createPolyline( props );
               break;
           case POLYGON:
               newFigure = createPolygon( props );
               break;
           case FREEHAND:
               newFigure = createFreehand( props );
               break;
           case TEXT:
               newFigure = createText( props );
               break;
           case CURVE:
               newFigure = createCurve( props );
               break;
           case XRANGE:
               newFigure = createXRange( props );
               break;
        }
        return newFigure;
    }

    /** Create a line Figure using the given properties */
    public Figure createLine( FigureProps props )
    {
        return createLine( props.getX1(), 
                           props.getY1(),
                           props.getX2(), 
                           props.getY2(),
                           props.getOutline(),
                           props.getThickness(),
                           props.getComposite() );
    }

    /** Create a line Figure using the given parameters */
    public Figure createLine( double x1, double y1, double x2, double y2,
                              Paint outline, double thickness,
                              AlphaComposite composite )
    {
        return new DrawLineFigure( x1, y1, x2, y2, outline, (float)thickness, 
                                   composite );
    }

    /** Create a line Figure using the given parameters */
    public Figure createLine( double x1, double y1, double x2, double y2 )
    {
        return new DrawLineFigure( x1, y1, x2, y2 );
    }

    /** Create a rectangle Figure using the given properties */
    public Figure createRectangle( FigureProps props )
    {
        return createRectangle( props.getX1(),
                                props.getY1(),
                                props.getWidth(),
                                props.getHeight(),
                                props.getOutline(),
                                props.getFill(),
                                props.getThickness(),
                                props.getComposite() );
    }

    /** Create a line Figure using the given parameters */
    public Figure createRectangle( double x, double y, double width,
                                   double height, Paint outline,
                                   Paint fill, double thickness,
                                   AlphaComposite composite )
    {
        return new DrawRectangleFigure( x, y, width, height, fill,
                                        outline, (float) thickness,
                                        composite );
    }

    /** Create an ellipse Figure using the given properties */
    public Figure createEllipse( FigureProps props )
    {
        return createEllipse( props.getX1(),
                              props.getY1(),
                              props.getWidth(),
                              props.getHeight(),
                              props.getOutline(),
                              props.getFill(),
                              props.getThickness(),
                              props.getComposite() );
    }

    /** Create an ellipse Figure using the given parameters */
    public Figure createEllipse( double x, double y, double width,
                                 double height, Paint outline,
                                 Paint fill, double thickness,
                                 AlphaComposite composite )
    {
        return new DrawEllipseFigure( x, y, width, height, fill,
                                      outline, (float) thickness,
                                      composite );
    }

    /** Create a polyline Figure using the given properties */
    public Figure createPolyline( FigureProps props )
    {
        return createPolyline( props.getX1(),
                               props.getY1(),
                               props.getOutline(),
                               props.getThickness(),
                               props.getComposite() );
    }

    /** Create a polyline Figure using the given parameters */
    public Figure createPolyline( double x, double y, Paint outline,
                                  double thickness, AlphaComposite composite )
    {
        return new DrawPolylineFigure( x, y, outline, (float) thickness,
                                       composite );
    }

    /** Create a polygon Figure using the given properties */
    public Figure createPolygon( FigureProps props )
    {
        return createPolygon( props.getX1(),
                              props.getY1(),
                              props.getFill(),
                              props.getOutline(),
                              props.getThickness(),
                              props.getComposite() );
    }

    /** Create a polygon Figure using the given parameters */
    public Figure createPolygon( double x, double y, Paint fill, Paint outline,
                                 double thickness, AlphaComposite composite )
    {
        return new DrawPolygonFigure( x, y, fill, outline, (float) thickness,
                                      composite );
    }

    /** Create a freehand Figure using the given properties */
    public Figure createFreehand( FigureProps props )
    {
        return createFreehand( props.getX1(),
                               props.getY1(),
                               props.getOutline(),
                               props.getThickness(),
                               props.getComposite() );
    }

    /** Create a freehand Figure using the given parameters */
    public Figure createFreehand( double x, double y, Paint outline,
                                  double thickness, AlphaComposite composite )
    {
        return new DrawFreehandFigure( x, y, outline, (float) thickness,
                                       composite );
    }

    /** Create a text Figure using the given properties */
    public Figure createText( FigureProps props )
    {
        return createText( props.getX1(),
                           props.getY1(),
                           props.getText(),
                           props.getOutline(),
                           props.getFont(),
                           props.getComposite() );

    }

    /** Create a text Figure using the given parameters */
    public Figure createText( double x, double y, String text, Paint outline,
                              Font font, AlphaComposite composite )
    {
        DrawLabelFigure label = new DrawLabelFigure( text, font );
        label.setComposite( composite );
        label.setFillPaint( outline );
        label.translateTo( x, y );
        return label;
    }

    /** Create a curve Figure using the given properties */
    public Figure createCurve( FigureProps props )
    {
        return createCurve( props.getX1(), 
                            props.getY1(),
                            props.getInterpolator(), 
                            props.getOutline(),
                            props.getThickness(),
                            props.getComposite() );


    }

    /** Create a curve Figure using the given parameters */
    public Figure createCurve( double x1, double y1,
                               Interpolator interpolator,
                               Paint outline, double thickness,
                               AlphaComposite composite )
    {
        return new InterpolatedCurveFigure( interpolator, x1, y1,
                                            outline, (float) thickness,
                                            composite );
    }

    /** Create a xrange Figure using the given properties */
    public Figure createXRange( FigureProps props )
    {
        return createXRange( props.getX1(),
                             props.getY1(),
                             props.getWidth(),
                             props.getHeight(),
                             props.getFill(),
                             props.getOutline(),
                             props.getThickness(),
                             props.getComposite() );

    }

    /** Create a xrange Figure using the given parameters */
    public Figure createXRange( double x, double y, double width,
                                double height, Paint fill, Paint outline,
                                double thickness, AlphaComposite composite )
    {
        XRangeFigure fig = new XRangeFigure( x, y, width, height,
                                             fill, outline, (float) thickness,
                                             composite );
        return fig;
    }
}
