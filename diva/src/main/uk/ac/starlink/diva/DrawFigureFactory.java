/*
 * Copyright (C) 2000-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-DEC-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import diva.canvas.CanvasUtilities;
import diva.util.java2d.Polyline2D;
import diva.util.java2d.Polygon2D;
import diva.canvas.Figure;

import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.geom.InterpolatedCurve2D;

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
    public static final String[] SHORTNAMES =
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
    public static final int NUM_FIGURES = SHORTNAMES.length;

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
     *  Create a {@link Figure} of the given type using the specified
     *  properties to initialise it.
     */
    public DrawFigure create( int type, FigureProps props )
    {
        DrawFigure newFigure = null;

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

    /**
     *  Create a {@link DrawFigure} using the specified properties to
     *  initialise it.
     */
    public DrawFigure create( FigureProps props )
    {
        return create( props.getType(), props );
    }

    /**
     * Create a line {@link DrawFigure} using the given properties
     */
    public DrawFigure createLine( FigureProps props )
    {
        return createLine( props.getX1(),
                           props.getY1(),
                           props.getX2(),
                           props.getY2(),
                           props.getOutline(),
                           props.getThickness(),
                           props.getComposite() );
    }

    /**
     * Create a line {@link DrawFigure} using the given parameters
     */
    public DrawFigure createLine( double x1, double y1, double x2, double y2,
                                  Paint outline, double thickness,
                                  AlphaComposite composite )
    {
        return new DrawLineFigure( x1, y1, x2, y2, outline, (float)thickness,
                                   composite );
    }

    /**
     * Create a line {@link DrawFigure} using the given parameters
     */
    public DrawFigure createLine( double x1, double y1, double x2, double y2 )
    {
        return new DrawLineFigure( x1, y1, x2, y2 );
    }

    /**
     * Create a rectangle {@link DrawFigure} using the given properties
     */
    public DrawFigure createRectangle( FigureProps props )
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

    /**
     * Create a line {@link DrawFigure} using the given parameters
     */
    public DrawFigure createRectangle( double x, double y, double width,
                                       double height, Paint outline,
                                       Paint fill, double thickness,
                                       AlphaComposite composite )
    {
        return new DrawRectangleFigure( x, y, width, height, fill,
                                        outline, (float) thickness,
                                        composite );
    }

    /**
     * Create an ellipse {@link DrawFigure} using the given properties
     */
    public DrawFigure createEllipse( FigureProps props )
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

    /**
     * Create an ellipse {@link DrawFigure} using the given parameters
     */
    public DrawFigure createEllipse( double x, double y, double width,
                                     double height, Paint outline,
                                     Paint fill, double thickness,
                                     AlphaComposite composite )
    {
        return new DrawEllipseFigure( x, y, width, height, fill,
                                      outline, (float) thickness,
                                      composite );
    }

    /**
     * Create a polyline {@link DrawFigure} using the given properties
     */
    public DrawFigure createPolyline( FigureProps props )
    {
        if ( props.getXArray() != null ) {
            return createPolyline( props.getXArray(),
                                   props.getYArray(),
                                   props.getOutline(),
                                   props.getThickness(),
                                   props.getComposite() );
        }
        else {
            return createPolyline( props.getX1(),
                                   props.getY1(),
                                   props.getOutline(),
                                   props.getThickness(),
                                   props.getComposite() );
        }
    }

    /**
     * Create a polyline {@link DrawFigure} using the given parameters
     */
    public DrawFigure createPolyline( double x, double y, Paint outline,
                                      double thickness,
                                      AlphaComposite composite )
    {
        return new DrawPolylineFigure( x, y, outline, (float) thickness,
                                       composite );
    }

    /**
     * Create a polyline {@link DrawFigure} using the given parameters
     */
    public DrawFigure createPolyline( double[] x, double[] y, Paint outline,
                                      double thickness,
                                      AlphaComposite composite )
    {
        Polyline2D.Double pl = new Polyline2D.Double();
        pl.moveTo( x[0], y[0] );
        for ( int i = 1; i < x.length; i++ ) {
            pl.lineTo( x[i], y[i] );
        }
        return new DrawPolylineFigure( pl, outline, (float) thickness,
                                       composite );
    }

    /**
     * Create a polygon {@link DrawFigure} using the given properties
     */
    public DrawFigure createPolygon( FigureProps props )
    {
        if ( props.getXArray() != null ) {
            return createPolygon( props.getXArray(),
                                  props.getYArray(),
                                  props.getFill(),
                                  props.getOutline(),
                                  props.getThickness(),
                                  props.getComposite() );

        }
        else {
            return createPolygon( props.getX1(),
                                  props.getY1(),
                                  props.getFill(),
                                  props.getOutline(),
                                  props.getThickness(),
                                  props.getComposite() );
        }

    }

    /**
     * Create a polygon {@link DrawFigure} using the given parameters
     */
    public DrawFigure createPolygon( double x, double y, Paint fill,
                                     Paint outline, double thickness,
                                     AlphaComposite composite )
    {
        return new DrawPolygonFigure( x, y, fill, outline, (float) thickness,
                                      composite );
    }

    /**
     * Create a polygon {@link DrawFigure} using the given parameters
     */
    public DrawFigure createPolygon( double[] x, double[] y, Paint fill,
                                     Paint outline, double thickness,
                                     AlphaComposite composite )
    {
        Polygon2D.Double pg = new Polygon2D.Double();
        pg.moveTo( x[0], y[0] );
        for ( int i = 1; i < x.length; i++ ) {
            pg.lineTo( x[i], y[i] );
        }
        return new DrawPolygonFigure( pg, fill, outline, (float) thickness,
                                      composite );
    }


    /**
     * Create a freehand {@link DrawFigure} using the given properties
     */
    public DrawFigure createFreehand( FigureProps props )
    {
        if ( props.getXArray() != null ) {
            return createFreehand( props.getXArray(),
                                   props.getYArray(),
                                   props.getOutline(),
                                   props.getThickness(),
                                   props.getComposite() );
        }
        else {
            return createFreehand( props.getX1(),
                                   props.getY1(),
                                   props.getOutline(),
                                   props.getThickness(),
                                   props.getComposite() );
        }
    }

    /**
     * Create a freehand {@link DrawFigure} using the given parameters
     */
    public DrawFigure createFreehand( double x, double y, Paint outline,
                                      double thickness,
                                      AlphaComposite composite )
    {
        return new DrawFreehandFigure( x, y, outline, (float) thickness,
                                       composite );
    }

    /**
     * Create a freehand {@link DrawFigure} using the given parameters
     */
    public DrawFigure createFreehand( double[] x, double[] y, Paint outline,
                                      double thickness,
                                      AlphaComposite composite )
    {
        Polyline2D.Double pl = new Polyline2D.Double();
        pl.moveTo( x[0], y[0] );
        for ( int i = 1; i < x.length; i++ ) {
            pl.lineTo( x[i], y[i] );
        }
        return new DrawFreehandFigure( pl, outline, (float) thickness,
                                       composite );
    }

    /**
     * Create a text {@link DrawFigure} using the given properties
     */
    public DrawFigure createText( FigureProps props )
    {
        return createText( props.getX1(),
                           props.getY1(),
                           props.getWidth(),
                           props.getHeight(),
                           props.getText(),
                           props.getFill(),
                           props.getFont(),
                           props.getComposite() );
    }

    /**
     * Create a text {@link DrawFigure} using the given parameters,
     * width and height indicate that some "size" is required.
     */
    public DrawFigure createText( double x, double y,
                                  double width, double height,
                                  String text, Paint fill, Font font,
                                  AlphaComposite composite )
    {
        DrawLabelFigure label = new DrawLabelFigure( text, font );
        label.setComposite( composite );
        label.setFillPaint( fill );

        if ( width != 1.0 || height != 1.0 ) {
            //  Text has scales to apply before rendering. Usually this is
            //  a consequence of restoring a DrawLabelFigure.
            Rectangle2D.Double sr = new Rectangle2D.Double( x, y, width,
                                                            height );
            AffineTransform tr =
                CanvasUtilities.computeTransform( label.getBounds(), sr );
            label.transform( tr );
        }
        else {
            label.translateTo( x, y );
        }
        return label;
    }

    /**
     * Create a curve {@link DrawFigure} using the given properties
     */
    public DrawFigure createCurve( FigureProps props )
    {
        return createCurve( props.getX1(),
                            props.getY1(),
                            props.getInterpolator(),
                            props.getOutline(),
                            props.getThickness(),
                            props.getComposite() );


    }

    /**
     * Create a curve {@link DrawFigure} using the given parameters
     */
    public DrawFigure createCurve( double x1, double y1,
                                   Interpolator interpolator,
                                   Paint outline, double thickness,
                                   AlphaComposite composite )
    {
        // If the interpolator has coordinates, these are used.
        if ( interpolator.getCount() > 0 ) {
            InterpolatedCurve2D c2d = new InterpolatedCurve2D( interpolator );
            return new InterpolatedCurveFigure( c2d, outline,
                                                (float) thickness, composite );

        }
        else {
            return new InterpolatedCurveFigure( interpolator, x1, y1,
                                                outline, (float) thickness,
                                                composite );
        }
    }

    /**
     * Create an XRangeFigure {@link DrawFigure} using the given properties
     */
    public DrawFigure createXRange( FigureProps props )
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

    /**
     * Create an XRangeFigure {@link DrawFigure} using the given parameters
     */
    public DrawFigure createXRange( double x, double y, double width,
                                    double height, Paint fill, Paint outline,
                                    double thickness,
                                    AlphaComposite composite )
    {
        XRangeFigure fig = new XRangeFigure( x, y, width, height,
                                             fill, outline, (float) thickness,
                                             composite );
        return fig;
    }

    /**
     * Create a {@link FigureProps} instance that describes the given
     * {@link DrawFigure}.
     */
    public FigureProps getFigureProps( DrawFigure figure )
    {
        FigureProps props = new FigureProps();
        props.setFill( figure.getFillPaint() );
        props.setOutline( figure.getStrokePaint() );
        props.setThickness( figure.getLineWidth() );
        props.setComposite( (AlphaComposite) figure.getComposite() );
        
        // Make sure we use unstroked bounds when storing.
        Rectangle2D bounds = figure.getShape().getBounds2D();
        props.setX1( bounds.getX() );
        props.setY1( bounds.getY() );
        props.setX2( bounds.getX() + bounds.getWidth() );
        props.setY2( bounds.getY() + bounds.getHeight() );
        props.setWidth( bounds.getWidth() );
        props.setHeight( bounds.getHeight() );

        if ( figure instanceof DrawLineFigure ) {
            props.setType( LINE );
        }
        else if ( figure instanceof DrawRectangleFigure ) {
            props.setType( RECTANGLE );
        }
        else if ( figure instanceof DrawEllipseFigure ) {
            props.setType( ELLIPSE );
        }
        else if ( figure instanceof DrawPolylineFigure ) {
            props.setType( POLYLINE );

            Polyline2D.Double pl = (Polyline2D.Double) figure.getShape();
            int n = pl.getVertexCount();
            double[] xa = new double[n];
            double[] ya = new double[n];
            for ( int i = 0; i < n; i++ ) {
                xa[i] = pl.getX( i );
                ya[i] = pl.getY( i );
            }
            props.setXArray( xa );
            props.setYArray( ya );
        }
        else if ( figure instanceof DrawPolygonFigure ) {
            props.setType( POLYGON );

            Polygon2D.Double pg = (Polygon2D.Double) figure.getShape();
            int n = pg.getVertexCount();
            double[] xa = new double[n];
            double[] ya = new double[n];
            for ( int i = 0; i < n; i++ ) {
                xa[i] = pg.getX( i );
                ya[i] = pg.getY( i );
            }
            props.setXArray( xa );
            props.setYArray( ya );
        }
        else if ( figure instanceof DrawFreehandFigure ) {
            props.setType( FREEHAND );

            Polyline2D.Double pl = (Polyline2D.Double) figure.getShape();
            int n = pl.getVertexCount();
            double[] xa = new double[n];
            double[] ya = new double[n];
            for ( int i = 0; i < n; i++ ) {
                xa[i] = pl.getX( i );
                ya[i] = pl.getY( i );
            }
            props.setXArray( xa );
            props.setYArray( ya );
        }
        else if ( figure instanceof DrawLabelFigure ) {
            props.setType( TEXT );
            props.setText( ((DrawLabelFigure)figure).getString() );
            props.setFont( ((DrawLabelFigure)figure).getFont() );
        }
        else if ( figure instanceof InterpolatedCurveFigure ) {
            props.setType( CURVE );
            InterpolatedCurve2D curve = (InterpolatedCurve2D)figure.getShape();
            props.setInterpolator( curve.getInterpolator() );
        }
        else if ( figure instanceof XRangeFigure ) {
            props.setType( XRANGE );
        }
        return props;
    }
}
