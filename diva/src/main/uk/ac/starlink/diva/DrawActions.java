/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-DEC-2003 (Peter W. Draper):
 *       Conversion of jsky.image.graphics.gui.CanvasDraw for SPLAT uses.
 */
package uk.ac.starlink.diva;

import diva.canvas.Figure;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.CircleManipulator;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.PathManipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.PathFigure;
import diva.canvas.toolbox.TypedDecorator;

import diva.util.java2d.Polygon2D;
import diva.util.java2d.Polyline2D;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputListener;

import uk.ac.starlink.diva.geom.InterpolatedCurve2D;
import uk.ac.starlink.diva.interp.AkimaSplineInterp;
import uk.ac.starlink.diva.interp.CubicSplineInterp;
import uk.ac.starlink.diva.interp.HermiteSplineInterp;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.LinearInterp;
import uk.ac.starlink.diva.interp.PolynomialInterp;
import uk.ac.starlink.util.gui.SelectStringDialog;
import uk.ac.starlink.diva.images.ImageHolder;

/**
 * This class defines a set of objects (created as AbstractActions)
 * for drawing on a JCanvas that implements the Draw
 * interface. These can be used for menu items or buttons in the user
 * interface.
 *
 * @author Allan Brighton
 * @author Peter W. Draper
 * @version $Id$
 */
public class DrawActions
    implements MouseInputListener
{
    /** The target drawble canvas */
    protected Draw canvas;

    /** Object managing image graphics */
    protected DrawGraphicsPane graphics;

    /** list of listeners for change events */
    protected EventListenerList listenerList = new EventListenerList();

    /** Event fired for changes */
    protected ChangeEvent changeEvent = new ChangeEvent( this );

    /** True if mouse was clicked */
    protected boolean mouseClicked = false;

    /** Starting point of drag */
    protected int startX;

    /** Starting point of drag */
    protected int startY;

    /** Used while drawing a polyline */
    protected Polyline2D.Double polyline;

    /** Used while drawing a polygon */
    protected Polygon2D.Double polygon;

    /** Used while drawing freehand */
    protected Polyline2D.Double freehand;

    /** Used while drawing an interpolated curve */
    protected InterpolatedCurve2D curve;

    /** Used while drawing a line */
    protected Line2D.Double line;

    /** Current figure (during figure creation) */
    protected Figure figure;

    /** List of figures created by this instance */
    protected LinkedList figureList = new LinkedList();

    // Selection modes

    // Want to add some actions "before" the figures themselves, so we
    // need to reassign the basic enumerations from the FigureFactory.
    // XX could force use of an Iterator to keep the correct order for
    // looping over selection and figure modes.

    /** Mode to select an object */
    public static final int SELECT = 0;

    /** Mode to edit an object */
    public static final int EDIT = 1;

    // FigureFactory renames...
    public static final int LINE = DrawFigureFactory.LINE + 2;
    public static final int RECTANGLE = DrawFigureFactory.RECTANGLE + 2;
    public static final int ELLIPSE = DrawFigureFactory.ELLIPSE + 2;
    public static final int POLYLINE = DrawFigureFactory.POLYLINE + 2;
    public static final int POLYGON = DrawFigureFactory.POLYGON + 2;
    public static final int FREEHAND = DrawFigureFactory.FREEHAND + 2;
    public static final int TEXT = DrawFigureFactory.TEXT + 2;
    public static final int CURVE = DrawFigureFactory.CURVE + 2;
    public static final int XRANGE = DrawFigureFactory.XRANGE + 2;

    /** The number of "drawing mode", that's type of Figures plus
     *  selection modes. */
    public static final int NUM_DRAWING_MODES =
        DrawFigureFactory.NUM_FIGURES + 2;

    /** Drawing mode actions */
    protected ArrayList drawingModeActions = new ArrayList(NUM_DRAWING_MODES);

    /** Current drawing mode */
    protected int drawingMode = SELECT;

    /** Used to toggle the visibility of all figures */
    protected boolean visible = true;

    // Colors

    /** Colors for color change actions */
    public static ArrayList colors = new ArrayList();
    static {
        colors.add( Color.black );
        colors.add( Color.blue );
        colors.add( Color.cyan );
        colors.add( Color.darkGray );
        colors.add( Color.gray );
        colors.add( Color.green );
        colors.add( Color.lightGray );
        colors.add( Color.magenta );
        colors.add( Color.orange );
        colors.add( Color.pink );
        colors.add( Color.red );
        colors.add( Color.white );
        colors.add( Color.yellow );
        colors.add( null );           //  Indicates none if appropriate.
    };

    /** Current fill paint */
    protected Paint fill = null;

    /** Current outline paint */
    protected Paint outline = Color.black;

    /** Actions to use to set the outline color */
    protected ArrayList outlineActions = new ArrayList();

    /** Actions to use to set the fill color */
    protected ArrayList fillActions = new ArrayList();

    // Composites

    /** Composites */
    public static final AlphaComposite[] COMPOSITES =
        new AlphaComposite[]
        {
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.0F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.1F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.2F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.3F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.4F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.6F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.7F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.8F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.9F ),
            AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1.0F )
        };

    /** Display names for Composites */
    public static final String[] COMPOSITE_NAMES =
        new String[]{
            "0%", "10%", "20%", "30%", "40%", "50%",
            "60%", "70%", "80%", "90%", "100%"
        };

    /** The number of composites defined above */
    public static final int NUM_COMPOSITES = COMPOSITES.length;

    /** Actions to use to set the composite */
    protected AbstractAction[] compositeActions =
        new AbstractAction[NUM_COMPOSITES];

    /** Current composite */
    protected AlphaComposite composite = null;


    // Fonts

    /** Fixed fonts for font change actions */
    public static ArrayList fonts = new ArrayList();
    static {
        fonts.add( new Font( "Serif", Font.PLAIN, 14 ) );
        fonts.add( new Font( "Serif", Font.ITALIC, 14 ) );
        fonts.add( new Font( "Serif", Font.BOLD, 14 ) );
        fonts.add( new Font( "SansSerif", Font.PLAIN, 14 ) );
        fonts.add( new Font( "SansSerif", Font.ITALIC, 14 ) );
        fonts.add( new Font( "SansSerif", Font.BOLD, 14 ) );
        fonts.add( new Font( "Monospaced", Font.PLAIN, 14 ) );
        fonts.add( new Font( "Monospaced", Font.ITALIC, 14 ) );
        fonts.add( new Font( "Monospaced", Font.BOLD, 14 ) );
    };

    /** Actions for choosing a preset font */
    protected ArrayList fontActions = new ArrayList();

    /** Default font for text items */
    protected Font font = new Font( "Serif", Font.BOLD, 14 );

    /** Figure to be modified during an edit */
    protected DrawLabelFigure editLabelFigure = null;


    // Line widths

    /** Supported line widths */
    public static final int[] LINE_WIDTHS = { 1, 2, 3, 4, 5, 6 };

    /** Number of Supported line widths */
    public static final int NUM_LINE_WIDTHS = LINE_WIDTHS.length;

    /** Current line width */
    protected int lineWidth = 1;

    /** Actions to use to set the line width */
    protected AbstractAction[] lineWidthActions =
        new AbstractAction[NUM_LINE_WIDTHS];


    // Interpolated curves.

    /** Hermite splines */
    public static final int HERMITE = 0;

    /** Akima splines */
    public static final int AKIMA = 1;

    /** Cubic splines */
    public static final int CUBIC = 2;

    /** Single polynomial though all points. */
    public static final int POLYNOMIAL = 3;

    /** Straight lines between points (same as polyline) */
    public static final int LINEAR = 4;

    /** Display names for Curves */
    public static final String[] CURVE_NAMES =
        {
            "Hermite",
            "Akima",
            "Cubic",
            "Polynomial",
            "Linear",
        };

    /** The number of curve types for which actions are defined. */
    public static final int NUM_CURVES = CURVE_NAMES.length;

    /** Actions used to create a curve */
    protected AbstractAction[] curveActions = new AbstractAction[NUM_CURVES];

    /** Current curve interpolator. */
    protected int interpolator = HERMITE;

    /** Guide for maximum polynomial degree */
    public static final int MAX_POLYDEGREE = 20;

    /** The DrawFigureFactory */
    protected DrawFigureFactory figureFactory = 
        DrawFigureFactory.getReference();

    /** Action to use to delete the selected figure. */
    protected AbstractAction deleteSelectedAction =
        new AbstractAction( "Delete selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                deleteSelected();
            }
        };

    /** Action to use to remove all figures. */
    protected AbstractAction clearAction =
        new AbstractAction( "Remove all" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                clear();
            }
        };

    /** Action to use to toggle the visibility of all figures. */
    protected AbstractAction hideGraphicsAction =
        new AbstractAction( "Hide figures" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                hideGraphics();
            }
        };

    /** Action to raise selected Figures  */
    protected AbstractAction raiseSelectedAction =
        new AbstractAction( "Raise selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                raiseSelected();
            }
        };

    /** Action to lower selected Figures  */
    protected AbstractAction lowerSelectedAction =
        new AbstractAction( "Lower selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                lowerSelected();
            }
        };

    /**
     * Create an instance for use with a specified DivaDraw
     *
     * @param canvas on which graphics will be drawn.
     */
    public DrawActions( Draw canvas )
    {
        this.canvas = canvas;
        graphics = canvas.getGraphicsPane();

        canvas.addMouseListener( this );
        canvas.addMouseMotionListener( this );

        drawingModeActions.add( new DrawingModeAction( "select", SELECT ) );
        drawingModeActions.add( new DrawingModeAction( "edit", EDIT ) );
        for ( int i = 2; i < NUM_DRAWING_MODES; i++ ) {
            drawingModeActions
                .add(new DrawingModeAction(DrawFigureFactory.shortNames[i-2], i));
        }

        for ( int i = 0; i < NUM_LINE_WIDTHS; i++ ) {
            lineWidthActions[i] = new LineWidthAction( i + 1 );
        }

        int n = colors.size();
        for ( int i = 0; i < n; i++ ) {
            outlineActions.add( new OutlineAction((Color) colors.get( i ) ) );
        }

        for ( int i = 0; i < n; i++ ) {
            fillActions.add( new FillAction((Color) colors.get( i ) ) );
        }

        for ( int i = 0; i < NUM_COMPOSITES; i++ ) {
            compositeActions[i] = new CompositeAction( COMPOSITE_NAMES[i],
                                                       COMPOSITES[i] );
        }

        for ( int i = 0; i < NUM_CURVES; i++ ) {
            curveActions[i] = new CurveAction( i );
        }

        n = fonts.size();
        for ( int i = 0; i < n; i++ ) {
            fontActions.add( new FontAction( (Font) fonts.get( i ) ) );
        }
    }

    /**
     * Return the Draw instance.
     */
    public Draw getDraw()
    {
        return canvas;
    }

    /**
     * Set the drawing mode.
     *
     * @param mode one of the mode constants defined in this class
     */
    public void setDrawingMode( int drawingMode )
    {
        this.drawingMode = drawingMode;
        fireChange();
    }

    /**
     * Return the current drawing mode.
     */
    public int getDrawingMode()
    {
        return drawingMode;
    }

    /**
     * Return the action for the given mode.
     */
    public AbstractAction getDrawingModeAction( int drawingMode )
    {
        return (AbstractAction) drawingModeActions.get( drawingMode );
    }

    /**
     * Set the line width.
     */
    public void setLineWidth( int lineWidth )
    {
        this.lineWidth = lineWidth;

        // apply change to any selected figures
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                fig.setLineWidth( lineWidth );
            }
        }
        fireChange();
    }

    /**
     * Return the current line width for drawing.
     */
    public int getLineWidth()
    {
        return lineWidth;
    }

    /**
     * Return the action for the given line width
     */
    public AbstractAction getLineWidthAction( int i )
    {
        return lineWidthActions[i];
    }

    /**
     * Set the outline color.
     */
    public void setOutline( Paint outline )
    {
        this.outline = outline;

        // apply change to any selected figures
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                fig.setStrokePaint( outline );
            }
        }
        fireChange();
    }

    /**
     * Return the current outline color for drawing.
     */
    public Paint getOutline()
    {
        return outline;
    }

    /**
     * Return the action for the given outline color
     */
    public AbstractAction getOutlineAction( int i )
    {
        return (AbstractAction) outlineActions.get( i );
    }

    /**
     * Set the fill color.
     */
    public void setFill( Paint fill )
    {
        this.fill = fill;

        // apply change to any selected figures
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                fig.setFillPaint( fill );
            }
        }
        fireChange();
    }


    /**
     * Return the current fill color for drawing.
     */
    public Paint getFill()
    {
        return fill;
    }

    /**
     * Return the action for the given fill color.
     */
    public AbstractAction getFillAction( int i )
    {
        return (AbstractAction) fillActions.get( i );
    }

    /**
     * Return the number of colours.
     */
    public int getColorCount()
    {
        return colors.size();
    }

    /**
     * Get a Color by index.
     */
    public Color getColor( int i )
    {
        return (Color) colors.get( i );
    }

    /**
     * Add a new color.
     */
    public void addColor( Color color )
    {
        colors.add( color );
        outlineActions.add( new OutlineAction( color ) );
        fillActions.add( new FillAction( color ) );
    }


    /**
     * Set the composite (transparency).
     */
    public void setComposite( AlphaComposite composite )
    {
        this.composite = composite;

        // apply change to any selected figures
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                fig.setComposite( composite );
            }
        }
        fireChange();
    }

    /**
     * Return the current composite composite for drawing.
     */
    public AlphaComposite getComposite()
    {
        return composite;
    }

    /**
     * Return the action for the given composite composite.
     */
    public AbstractAction getCompositeAction( int i )
    {
        return compositeActions[i];
    }

    /**
     * Set the font to use for labels.
     */
    public void setFont( Font font )
    {
        this.font = font;

        // apply change to any selected figures
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                if ( fig instanceof DrawLabelFigure ) {
                    ((DrawLabelFigure) fig).setFont( font );
                }
            }
        }
        fireChange();
    }

    /**
     * Add a new font.
     */
    public void addFont( Font font )
    {
        fonts.add( font );
        fontActions.add( new FontAction( font ) );
    }

    /**
     * Return the current font color for drawing.
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Return a given font.
     */
    public Font getFont( int i )
    {
        return (Font) fonts.get( i );
    }

    /**
     * Return the action for the given font.
     */
    public AbstractAction getFontAction( int i )
    {
        return (AbstractAction) fontActions.get( i );
    }

    /**
     * Return the number of fonts
     */
    public int fontCount()
    {
        return fonts.size();
    }

    /**
     * Set the interpolated curve type.
     */
    public void setCurve( int interpolator )
    {
        this.interpolator = interpolator;

        //  Apply change to any selected curves.
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                if ( fig instanceof InterpolatedCurveFigure ) {

                    //  Modify the type of interpolator to the new
                    //  one, if needed.
                    InterpolatedCurveFigure ifig =
                        (InterpolatedCurveFigure) fig;
                    InterpolatedCurve2D icurve =
                        (InterpolatedCurve2D) ifig.getShape();
                    Interpolator cinterp = icurve.getInterpolator();
                    Interpolator ninterp = makeInterpolator();
                    if ( cinterp.getClass() != ninterp.getClass() ) {
                        ninterp.setCoords( cinterp.getXCoords(),
                                           cinterp.getYCoords(), true );
                        icurve.setInterpolator( ninterp );
                        fig.setShape( icurve );
                    }
                }
            }
        }
        fireChange();
    }

    /**
     * Return the current curve type.
     */
    public int getCurve()
    {
        return interpolator;
    }

    /**
     * Return the action for a given curve interpolation type.
     */
    public AbstractAction getCurveAction( int i )
    {
        return curveActions[i];
    }

    /**
     * Make an instance of the current interpolator.
     */
    public Interpolator makeInterpolator()
    {
        switch (interpolator)
        {
           case HERMITE: {
               return new HermiteSplineInterp();
           }
           case AKIMA: {
               return new AkimaSplineInterp();
           }
           case CUBIC: {
               return new CubicSplineInterp();
           }
           case POLYNOMIAL: {
               return new PolynomialInterp();
           }
           case LINEAR: {
               return new LinearInterp();
           }
        }
        return null;
    }

    /**
     * Register to receive change events from this object whenever the
     * drawing settings are changed.
     */
    public void addChangeListener( ChangeListener l )
    {
        listenerList.add( ChangeListener.class, l );
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener( ChangeListener l )
    {
        listenerList.remove( ChangeListener.class, l );
    }

    /**
     * Notify any listeners of a change.
     */
    protected void fireChange()
    {
        Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ChangeListener.class ) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    //
    // For the MouseInputListener interface
    //
    public void mouseClicked( MouseEvent e )
    {
        int count = e.getClickCount();
        SelectionInteractor si = graphics.getSelectionInteractor();

        if ( mouseClicked ) {  // clicked previously?
            if ( drawingMode == POLYLINE ) {
                if ( count == 1 ) {
                    polyline = (Polyline2D.Double) figure.getShape();
                    return;
                }
                else if ( count > 1 ) {
                    figure.setInteractor( si );
                }
            }
            else if ( drawingMode == POLYGON ) {
                if ( count == 1 ) {
                    polygon = (Polygon2D.Double) figure.getShape();
                    return;
                }
                else if ( count > 1 ) {
                    figure.setInteractor( si );
                }
            }
            else if ( drawingMode == FREEHAND ) {
                if ( count == 1 ) {
                    freehand = (Polyline2D.Double) figure.getShape();
                    return;
                }
                else if ( count > 1 ) {
                    figure.setInteractor( si );
                }
            }
            else if ( drawingMode == CURVE ) {
                if ( count == 1 ) {
                    curve = (InterpolatedCurve2D) figure.getShape();
                    return;
                }
                else if ( count > 1 ) {
                    figure.setInteractor( si );
                }
            }
            else if ( drawingMode == LINE ) {
                if ( count == 1 ) {
                    line = (Line2D.Double) figure.getShape();
                    return;
                }
                else if ( count > 1 ) {
                    figure.setInteractor( si );
                }
            }

            // finish drawing figure
            finishFigure();
        }
        else {
            mouseClicked = true;
        }
    }

    public void mouseEntered( MouseEvent e )
    {
        // Do nothing.
    }

    public void mouseExited( MouseEvent e )
    {
        // Do nothing.
    }

    public void mousePressed( MouseEvent e  )
    {
        // If editing choose a figure to modify. If any are selected,
        // look for the first that we can support.
        if ( drawingMode == EDIT ) {
            // Make selected figure current.

            boolean found = false;
            SelectionModel sm =
                graphics.getSelectionInteractor().getSelectionModel();
            ListIterator it = figureList.listIterator( figureList.size() );
            while ( ( ! found ) && it.hasPrevious() ) {
                DrawFigure fig = (DrawFigure) it.previous();
                if ( sm.containsSelection( fig ) ) {
                    // Use first.
                    if ( fig instanceof InterpolatedCurveFigure ) {
                        drawingMode = CURVE;
                        curve = (InterpolatedCurve2D) fig.getShape();
                        figure = fig;
                        found = true;
                    }
                    else if ( fig instanceof DrawLabelFigure ) {
                        // Edit text. Save reference to the Figure to
                        // be changed and switch drawingMode.
                        editLabelFigure = (DrawLabelFigure) fig;
                        drawingMode = TEXT;
                        figure = fig;
                        found = true;
                    }
                    else if ( fig instanceof DrawPolylineFigure ) {
                        drawingMode = POLYLINE;
                        polyline = (Polyline2D.Double) fig.getShape();
                        figure = fig;
                        found = true;
                    }
                    else if ( fig instanceof DrawPolygonFigure ) {
                        drawingMode = POLYGON;
                        polygon = (Polygon2D.Double) fig.getShape();
                        figure = fig;
                        found = true;
                    }
                    else if ( fig instanceof DrawFreehandFigure ) {
                        drawingMode = FREEHAND;
                        freehand = (Polyline2D.Double) fig.getShape();
                        figure = fig;
                        found = true;
                    }
                    break;
                }
            }
            return;
        }

        if ( ( drawingMode == POLYLINE && polyline != null )
             || ( drawingMode == POLYGON && polygon != null )
             || ( drawingMode == FREEHAND && freehand != null )
             || ( drawingMode == CURVE && curve != null )
             || ( drawingMode == LINE && line != null ) ) {
            return;
        }

        startX = e.getX();
        startY = e.getY();

        figure = null;
        FigureProps props = new FigureProps();
        props.setFill( fill );
        props.setOutline( outline );
        props.setThickness( lineWidth );
        props.setX1( startX );
        props.setY1( startY );
        props.setComposite( composite );

        switch ( drawingMode ) {
           case LINE:
               props.setX2( startX + 1 );
               props.setY2( startY + 1 );
               figure = figureFactory.create( DrawFigureFactory.LINE, props );
               line = (Line2D.Double) figure.getShape();
               break;

           case RECTANGLE:
               props.setWidth( 1 );
               props.setHeight( 1 );
               figure = figureFactory.create( DrawFigureFactory.RECTANGLE, props );
               break;

           case XRANGE:
               props.setWidth( 1 );
               props.setHeight( 1 );
               figure = figureFactory.create( DrawFigureFactory.XRANGE, props );
               break;

           case ELLIPSE:
               props.setWidth( 1 );
               props.setHeight( 1 );
               figure = figureFactory.create( DrawFigureFactory.ELLIPSE, props );
               break;

           case POLYLINE:
               figure = figureFactory.create( DrawFigureFactory.POLYLINE, props );
               polyline = (Polyline2D.Double) figure.getShape();
               break;

           case POLYGON:
               figure = figureFactory.create( DrawFigureFactory.POLYGON, props );
               polygon = (Polygon2D.Double) figure.getShape();
               break;

           case FREEHAND:
               figure = figureFactory.create( DrawFigureFactory.FREEHAND, props );
               freehand = (Polyline2D.Double) figure.getShape();
               break;

           case CURVE:
               props.setInterpolator( makeInterpolator() );
               figure = figureFactory.create( DrawFigureFactory.CURVE, props );
               curve = (InterpolatedCurve2D) figure.getShape();
               break;

           case TEXT:
               //  Created later.
               return;
        }

        if ( figure != null ) {
            graphics.addFigure( figure );
            figureList.add( figure );
        }
    }

    public void mouseReleased( MouseEvent e )
    {
        switch ( drawingMode ) {
           case POLYLINE:
               if ( polyline != null ) {
                   polyline = (Polyline2D.Double) figure.getShape();
               }
               else if ( startX != e.getX() || startY != e.getY() ) {
                   finishFigure();
               }
               break;

           case POLYGON:
               if ( polygon != null ) {
                   polygon = (Polygon2D.Double) figure.getShape();
               }
               else if ( startX != e.getX() || startY != e.getY() ) {
                   finishFigure();
               }
               break;

           case CURVE:
               if ( curve != null ) {
                   curve = (InterpolatedCurve2D) figure.getShape();
               }
               else if ( startX != e.getX() || startY != e.getY() ) {
                   finishFigure();
               }
               break;

           case TEXT:
               //  If the text is being edited, rather than created
               //  just modify the existing figure (keeps properties).
               Point2D.Double pos = new Point2D.Double( startX, startY );

               String defaultText = "";
               if ( editLabelFigure != null ) {
                   defaultText = editLabelFigure.getString();
               }
               String s =
                   SelectStringDialog.showDialog( canvas.getComponent(), 
                                                  "Text label:",
                                                  "Label", defaultText );
               if ( s != null && s.length() != 0 ) {
                   if ( editLabelFigure != null ) {
                       editLabelFigure.setString( s );
                       editLabelFigure = null;
                   }
                   else {
                       FigureProps props =
                           new FigureProps( startX, startY, s, font, fill );
                       props.setComposite( composite );
                       figure = figureFactory.create( DrawFigureFactory.TEXT,
                                                      props );
                       graphics.addFigure( figure );
                       figureList.add( figure );
                   }
                   finishFigure();
               }
               break;

           default:
               if ( startX != e.getX() || startY != e.getY() ) {
                   finishFigure();
               }
        }
    }

    public void mouseDragged( MouseEvent e )
    {
        if ( figure != null ) {
            int endX = e.getX();
            int endY = e.getY();
            int n;

            Shape shape = null;
            switch ( drawingMode ) {
               case LINE:
                   shape = new Line2D.Double( startX, startY, endX, endY );
                   break;

               case RECTANGLE:
                   shape = new Rectangle2D.Double( startX, startY,
                                                   endX - startX,
                                                   endY - startY );
                   break;

               case XRANGE:
                   shape = new Rectangle2D.Double( startX, startY,
                                                   endX - startX,
                                                   endY - startY );
                   break;

               case ELLIPSE:
                   shape = new Ellipse2D.Double( startX, startY,
                                                 endX - startX,
                                                 endY - startY );
                   break;

               case POLYLINE:
                   n = polyline.getVertexCount();
                   Polyline2D.Double pl = new Polyline2D.Double();
                   pl.moveTo( polyline.getX( 0 ), polyline.getY(0) );
                   for ( int i = 1; i < n; i++ ) {
                       pl.lineTo( polyline.getX( i ), polyline.getY(i ) );
                   }
                   pl.lineTo( endX, endY );
                   shape = pl;
                   break;

               case POLYGON:
                   n = polygon.getVertexCount();
                   Polygon2D.Double pg = new Polygon2D.Double();
                   pg.moveTo( polygon.getX( 0 ), polygon.getY( 0 ) );
                   for ( int i = 1; i < n; i++ ) {
                       pg.lineTo( polygon.getX( i ), polygon.getY( i ) );
                   }
                   pg.lineTo( endX, endY );
                   shape = pg;
                   break;

               case FREEHAND:
                   freehand.lineTo( endX, endY );
                   shape = freehand;
                   break;

               case CURVE:
                   n = curve.getVertexCount();
                   InterpolatedCurve2D c =
                       new InterpolatedCurve2D( makeInterpolator() );
                   c.moveTo( curve.getXVertex( 0 ), curve.getYVertex( 0 ) );
                   for ( int i = 1; i < n; i++ ) {
                       c.lineTo( curve.getXVertex( i ), curve.getYVertex(i ) );
                   }
                   c.lineTo( endX, endY );
                   c.orderVertices();
                   shape = c;
                   break;

               case TEXT:
                   return;
            }

            if ( shape != null ) {
                ( (DrawFigure) figure ).setShape( shape );
            }
        }
    }

    public void mouseMoved( MouseEvent e )
    {
        mouseDragged( e );
    }

    /**
     * Finish off the current figure and select it.
     */
    protected void finishFigure()
    {
        if ( figure != null ) {
            graphics.clearSelection();
            graphics.select( figure );
            figure = null;
        }
        polyline = null;
        polygon = null;
        freehand = null;
        curve = null;
        line = null;
        setDrawingMode( SELECT );
        mouseClicked = false;
    }

    /**
     * Remove all figures created by this instance.
     */
    public void clear()
    {
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            graphics.removeFigure( fig );
            it.remove();
        }
    }

    /**
     * Delete the selected figures.
     */
    public void deleteSelected()
    {
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                graphics.removeFigure( fig );
                it.remove();
            }
        }
    }

    /**
     * Toggle the visibility all figures created by this instance.
     */
    public void hideGraphics()
    {
        visible = !visible;
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            fig.setVisible( visible );
            fig.repaint();
        }
    }

    /**
     * Raise the selected figures.
     */
    public void raiseSelected()
    {
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                graphics.raiseFigure( fig );
            }
        }
    }

    /**
     * Lower the selected figures.
     */
    public void lowerSelected()
    {
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator( 0 );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                graphics.lowerFigure( fig );
            }
        }
    }

    /**
     * Return a list of figures managed by this instance.
     */
    public LinkedList getFigureList()
    {
        return figureList;
    }

    /**
     * Create and return a TypedDecorator suitable for manipulating
     * the various types of figures in natural ways.
     */
    public static TypedDecorator getTypedDecorator()
    {
        // Use a generic decorator that can be tuned to use
        // different actual manipulators according to the type of
        // figure. The default manipulator is a BoundsManipulator.
        Manipulator man = new BoundsManipulator();
        TypedDecorator decorator = new TypedDecorator( man );

        //  Set manipulators for each figure type, if required.
        man = new RangeManipulator();
        decorator.addDecorator( XRangeFigure.class, man );

        man = new PathManipulator();
        decorator.addDecorator( DrawPathFigure.class, man );
        decorator.addDecorator( DrawLineFigure.class, man );

        man = new InterpolatedCurveManipulator();
        decorator.addDecorator( InterpolatedCurveFigure.class, man );

        //man = new CircleManipulator();
        //decorator.addDecorator( DrawCircleFigure.class, man );

        return decorator;
    }

    // -- Local Action classes --

    /** Local base class for creating menu/toolbar actions. */
    abstract class GraphicsAction
        extends AbstractAction
    {
        String name;

        public GraphicsAction( String name )
        {
            super();
            try {
                ImageIcon icon =
                    new ImageIcon( ImageHolder.class.getResource( name +
                                                                  ".gif" ) );
                putValue( Action.SMALL_ICON, icon );
            }
            catch (Exception e) {
                System.out.println( "No icon: " + name + ".gif" );
            }
            this.name = name;
        }
    }

    /**
     * Local class used to set the drawing mode.
     */
    class DrawingModeAction
        extends GraphicsAction
    {
        int drawingMode;

        public DrawingModeAction( String name, int drawingMode )
        {
            super( name );
            this.drawingMode = drawingMode;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setDrawingMode( drawingMode );
        }
    }

    /**
     * Local class used to set the line width.
     */
    class LineWidthAction
        extends GraphicsAction
    {
        int width;

        public LineWidthAction( int i )
        {
            super( "width" + i );
            width = i;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setLineWidth( width );
        }
    }

    /**
     * Local class used to set the outline color for figures.
     */
    class OutlineAction
        extends AbstractAction
    {
        Paint color;

        public OutlineAction( Paint color )
        {
            super( color != null ? "    " : "None" );
            this.color = color;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setOutline( color );
        }
    }

    /**
     * Local class used to set the fill color for figures.
     */
    class FillAction
        extends AbstractAction
    {
        Paint color;

        public FillAction( Paint color )
        {
            super( color != null ? "    " : "None" );
            this.color = color;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setFill( color );
        }
    }

    /**
     * Local class used to set the transparency for figures.
     */
    class CompositeAction
        extends AbstractAction
    {
        AlphaComposite composite;

        public CompositeAction( String label, AlphaComposite composite )
        {
            super( label );
            this.composite = composite;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setComposite( composite );
        }
    }

    /**
     * Local class used to set the label font.
     */
    class FontAction
        extends AbstractAction
    {
        Font font;

        public FontAction( Font font )
        {
            super( font.getFontName() );
            this.font = font;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setFont( font );
        }
    }

    /**
     * Local class used to set the interpolated curve type.
     */
    class CurveAction
        extends AbstractAction
    {
        int interpolator = HERMITE;

        public CurveAction( int interpolator )
        {
            super( CURVE_NAMES[interpolator] );
            this.interpolator = interpolator;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setCurve( interpolator );
        }
    }
}
