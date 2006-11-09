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
import java.awt.Cursor;
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
import uk.ac.starlink.diva.images.ImageHolder;
import uk.ac.starlink.diva.interp.BasicInterpolatorFactory;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.diva.interp.InterpolatorFactory;
import uk.ac.starlink.util.gui.SelectStringDialog;

/**
 * This class defines a set of objects (created as AbstractActions)
 * for drawing on a JCanvas that implements the Draw interface. These
 * can be used for menu items or buttons in the user interface.
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

    /** List of listeners for change events */
    protected EventListenerList listenerList = new EventListenerList();

    /** Event fired for changes */
    protected ChangeEvent changeEvent = new ChangeEvent( this );

    /** List of listeners for figure creation events */
    protected EventListenerList figureListenerList = new EventListenerList();

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
    protected Font font = (Font) fonts.get( 0 );

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
    protected AbstractAction[] lineWidthActions = new AbstractAction[NUM_LINE_WIDTHS];

    // Interpolated curves.

    /** The default InterpolatorFactory */
    protected InterpolatorFactory interpolatorFactory = null;

    /** Actions used to create a curve */
    protected AbstractAction[] curveActions = null;

    /** Current curve interpolator. */
    protected int interpolator = InterpolatorFactory.HERMITE;

    /** The DrawFigureFactory */
    protected DrawFigureFactory figureFactory = 
        DrawFigureFactory.getReference();


    /** FigureStore instance */
    protected FigureStore store = null;

    /**
     * Create an instance for use with a specified Draw
     *
     * @param canvas on which graphics will be drawn.
     * @param store used to save and restore graphics, null for none.
     */
    public DrawActions( Draw canvas, FigureStore store )
    {
        this( canvas, store, BasicInterpolatorFactory.getInstance() );
    }

    /**
     * Create an instance for use with a specified Draw and
     * InterpolatorFactory.
     *
     * @param canvas on which graphics will be drawn.
     * @param store used to save and restore graphics, null for none.
     * @param factory an {@link InterpolatorFactory}.
     */
    public DrawActions( Draw canvas, FigureStore store, 
                        InterpolatorFactory factory )
    {
        this.canvas = canvas;
        graphics = canvas.getGraphicsPane();
        if ( factory != null ) {
            setInterpolatorFactory( factory );
        }

        canvas.addMouseListener( this );
        canvas.addMouseMotionListener( this );

        drawingModeActions.add( new DrawingModeAction( "select", SELECT ) );
        drawingModeActions.add( new DrawingModeAction( "edit", EDIT ) );
        for ( int i = 2; i < NUM_DRAWING_MODES; i++ ) {
            drawingModeActions
                .add(new DrawingModeAction(DrawFigureFactory.SHORTNAMES[i-2], i));
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

        n = fonts.size();
        for ( int i = 0; i < n; i++ ) {
            fontActions.add( new FontAction( (Font) fonts.get( i ) ) );
        }

        setFigureStore( store );
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
        canvas.getComponent()
            .setCursor( Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) );
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
        ListIterator it = getListIterator( true );
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
        ListIterator it = getListIterator( true );
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
        ListIterator it = getListIterator( true );
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
        ListIterator it = getListIterator( true );
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
        ListIterator it = getListIterator( true );
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
     * Set the InterpolatorFactory.
     */
    public void setInterpolatorFactory( InterpolatorFactory factory )
    {
        this.interpolatorFactory = factory;

        //  Update the Actions for this factory.
        int n = interpolatorFactory.getInterpolatorCount();
        curveActions = new AbstractAction[n];
        for ( int i = 0; i < n; i++ ) {
            curveActions[i] = new CurveAction( i );
        }
    }

    /**
     * Get the InterpolatorFactory.
     */
    public InterpolatorFactory getInterpolatorFactory()
    {
        return interpolatorFactory;
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
        ListIterator it = getListIterator( true );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                if ( fig instanceof InterpolatedCurveFigure ) {

                    //  Modify the type of interpolator to the new
                    //  one, if needed.
                    try {
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
                    catch (Exception e) {
                        //  Conversion failed. Hopefully leave figure as it
                        //  was. Should be true until call to setShape above.
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
        return interpolatorFactory.makeInterpolator( interpolator );
    }

    //
    // Listeners for setting changes.
    //
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
    // Listeners for figure creation (finished) events.
    //

    /**
     * Register to receive figure change events from this object whenever
     * a figure has been created.
     */
    public void addFigureListener( FigureListener l )
    {
        figureListenerList.add( FigureListener.class, l );
    }

    /**
     * Stop receiving figure change events from this object.
     */
    public void removeFigureListener( FigureListener l )
    {
        figureListenerList.remove( FigureListener.class, l );
    }

    /**
     * Notify any figure listeners of a figure created event.
     */
    protected void fireFigureEvent( DrawFigure figure, int type )
    {
        FigureChangedEvent e = new FigureChangedEvent( figure, type, null );
        Object[] listeners = figureListenerList.getListenerList();
        if ( type == FigureChangedEvent.CREATED ) {
            for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
                if ( listeners[i] == FigureListener.class ) {
                    ((FigureListener) listeners[i + 1]).figureCreated( e );
                }
            }
        }
        else if ( type == FigureChangedEvent.REMOVED ) {
            for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
                if ( listeners[i] == FigureListener.class ) {
                    ((FigureListener) listeners[i + 1]).figureRemoved( e );
                }
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

        //  If already clicked then drawing has begun. When count is 1
        //  click just update any shapes that are potentially not
        //  finished (lines, polylines) to include the new position
        //  this click adds. Double click finishes these figures. Other
        //  figures are finished on the second click, regardless.
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
                    // Check if figure is complete. Interpolated lines may
                    // have points limits and we should stop when they are
                    // full. 
                    curve = (InterpolatedCurve2D) figure.getShape();
                    if ( ! curve.isFull() ) {
                        return;
                    }
                    figure.setInteractor( si );
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
            //  First press.
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
        // look for the first of these that can be editted.
        if ( drawingMode == EDIT ) {

            boolean found = false;
            SelectionModel sm =
                graphics.getSelectionInteractor().getSelectionModel();
            ListIterator it = getListIterator( false );

            while ( ( ! found ) && it.hasPrevious() ) {
                DrawFigure fig = (DrawFigure) it.previous();
                if ( sm.containsSelection( fig ) ) {

                    //  When a figure that can be edited is
                    //  encountered the state is changed to as if it
                    //  has just been created.
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
                }
            }
            return;
        }

        // If already creating a figure that is extended until a
        // double click, just return.
        if ( ( drawingMode == POLYLINE && polyline != null )
             || ( drawingMode == POLYGON && polygon != null )
             || ( drawingMode == FREEHAND && freehand != null )
             || ( drawingMode == CURVE && curve != null )
             || ( drawingMode == LINE && line != null ) ) {
            return;
        }

        // Start of figure creation.
        startX = e.getX();
        startY = e.getY();

        // Create a snapshot of the current figure configurations. The
        // geometry of figures initially is just a dummy.
        FigureProps props = new FigureProps();
        props.setFill( fill );
        props.setOutline( outline );
        props.setThickness( lineWidth );
        props.setComposite( composite );

        props.setX1( startX );
        props.setY1( startY );
        props.setX2( startX + 1 );
        props.setY2( startY + 1 );
        props.setWidth( 1 );
        props.setHeight( 1 );

        //  Create the figure. Figures which are completed by double
        //  clicking have their shapes recorded for modification.
        figure = null;
        switch ( drawingMode ) {
           case LINE:
               figure = figureFactory.create( DrawFigureFactory.LINE, 
                                              props );
               line = (Line2D.Double) figure.getShape();
               break;

           case RECTANGLE:
               figure = figureFactory.create( DrawFigureFactory.RECTANGLE, 
                                              props);
               break;

           case XRANGE:
               figure = figureFactory.create( DrawFigureFactory.XRANGE, 
                                              props );
               break;

           case ELLIPSE:
               figure = figureFactory.create( DrawFigureFactory.ELLIPSE, 
                                              props );
               break;

           case POLYLINE:
               figure = figureFactory.create( DrawFigureFactory.POLYLINE, 
                                              props );
               polyline = (Polyline2D.Double) figure.getShape();
               break;

           case POLYGON:
               figure = figureFactory.create( DrawFigureFactory.POLYGON, 
                                              props );
               polygon = (Polygon2D.Double) figure.getShape();
               break;

           case FREEHAND:
               figure = figureFactory.create( DrawFigureFactory.FREEHAND, 
                                              props );
               freehand = (Polyline2D.Double) figure.getShape();
               break;

           case CURVE:
               props.setInterpolator( makeInterpolator() );
               figure = figureFactory.create( DrawFigureFactory.CURVE, props );
               curve = (InterpolatedCurve2D) figure.getShape();
               break;

           case TEXT:
               //  Created later at startX, startY.
               return;
        }

        //  Add the figure to the DrawGraphicsPane and keep a record.
        if ( figure != null ) {
            addDrawFigure( (DrawFigure) figure );
        }
    }

    public void mouseReleased( MouseEvent e )
    {
        //  If the figure is still being drawn, then just update the
        //  shape. Otherwise finish the figure, unless it has no size.
        //  TEXT is a special case, it is actually created, or edited
        //  on release.
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
               
               //  Prompt for the text.
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

            //  Update figure being created to follow a drag. Most figures are
            //  updated with a new shape to show the new position replacing an
            //  old one (previous x1,y1 for new x1,y1), but the freehand has a
            //  new point added for each drag event. Rectangular shaped
            //  objects are corrected to have positive widths and heights.

            int x0 = startX;
            int y0 = startY;
            int x1 = e.getX();
            int y1 = e.getY();
            int n;

            Shape shape = null;
            switch ( drawingMode ) {
               case LINE:
                   shape = new Line2D.Double( x0, y0, x1, y1 );
                   break;

               case RECTANGLE:
               case XRANGE:
                   if ( x1 < x0 ) {
                       int tmp = x1;
                       x1 = x0;
                       x0 = tmp;
                   }
                   if ( y1 < y0 ) {
                       int tmp = y1;
                       y1 = y0;
                       y0 = tmp;
                   }
                   shape = new Rectangle2D.Double( x0, y0, x1 - x0, y1 - y0 );
                   break;

               case ELLIPSE:
                   if ( x1 < x0 ) {
                       int tmp = x1;
                       x1 = x0;
                       x0 = tmp;
                   }
                   if ( y1 < y0 ) {
                       int tmp = y1;
                       y1 = y0;
                       y0 = tmp;
                   }
                   shape = new Ellipse2D.Double( x0, y0, x1 - x0, y1 - y0 );
                   break;

               case POLYLINE:
                   n = polyline.getVertexCount();
                   Polyline2D.Double pl = new Polyline2D.Double();
                   pl.moveTo( polyline.getX( 0 ), polyline.getY(0) );
                   for ( int i = 1; i < n; i++ ) {
                       pl.lineTo( polyline.getX( i ), polyline.getY(i ) );
                   }
                   pl.lineTo( x1, y1 );
                   shape = pl;
                   break;

               case POLYGON:
                   n = polygon.getVertexCount();
                   Polygon2D.Double pg = new Polygon2D.Double();
                   pg.moveTo( polygon.getX( 0 ), polygon.getY( 0 ) );
                   for ( int i = 1; i < n; i++ ) {
                       pg.lineTo( polygon.getX( i ), polygon.getY( i ) );
                   }
                   pg.lineTo( x1, y1 );
                   shape = pg;
                   break;

               case FREEHAND:
                   freehand.lineTo( x1, y1 );
                   shape = freehand;
                   break;

               case CURVE:
                   n = curve.getVertexCount();
                   InterpolatedCurve2D c =
                       new InterpolatedCurve2D( makeInterpolator() );
                   Interpolator i = c.getInterpolator();
                   i.setCoords( curve.getXVertices(), curve.getYVertices(),
                                true );
                   c.lineTo( x1, y1 );
                   c.orderVertices(); // Curves must be monotonic.
                   shape = c;
                   break;

               case TEXT:
                   return;
            }

            //  Cause an update of the shape as drawn.
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
            
            // Inform any listeners that the Figure is completed.
            fireFigureEvent( (DrawFigure) figure, FigureChangedEvent.CREATED );
            figure = null;
        }
        polyline = null;
        polygon = null;
        freehand = null;
        curve = null;
        line = null;
        setDrawingMode( SELECT );
        mouseClicked = false;

        canvas.getComponent().setCursor( Cursor.getDefaultCursor() );
    }

    /**
     * Remove all figures created by this instance.
     */
    public synchronized void clear()
    {
        int size = figureList.size();
        for ( int i = size - 1; i >= 0; i-- ) {
            DrawFigure fig = (DrawFigure) figureList.get( i );
            deleteFigure( fig );
        }
    }

    /**
     * Delete the selected figures.
     */
    public void deleteSelected()
    {
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();

        //  Don't use ListIterator when deleting as this causes a
        //  concurrent modification of figureList (.remove is called
        //  when this should only be performed on the ListIterator).
        int size = figureList.size();
        for ( int i = size - 1; i >= 0; i-- ) {
            DrawFigure fig = (DrawFigure) figureList.get( i );
            if ( sm.containsSelection( fig ) ) {
                deleteFigure( fig );
            }
        }
    }

    /**
     * Delete a given figure, if displayed.
     */
    public void deleteFigure( DrawFigure figure )
    {
        if ( figureList.remove( figure ) ) {
            graphics.removeFigure( figure );
            fireFigureEvent( (DrawFigure) figure, FigureChangedEvent.REMOVED );
        }
    }

    /**
     * Toggle the visibility all figures created by this instance.
     */
    public void hideGraphics()
    {
        visible = !visible;
        ListIterator it = getListIterator( true );
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
        ListIterator it = getListIterator( true );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                graphics.raiseFigure( fig );
            }
        }
    }

    /**
     * Raise the given figure.
     */
    public void raiseFigure( DrawFigure figure )
    {
        if ( figureList.contains( figure ) ) {
            graphics.raiseFigure( figure );
        }
    }

    /**
     * Lower the selected figures.
     */
    public void lowerSelected()
    {
        SelectionModel sm =
            graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = getListIterator( true );
        while ( it.hasNext() ) {
            DrawFigure fig = (DrawFigure) it.next();
            if ( sm.containsSelection( fig ) ) {
                graphics.lowerFigure( fig );
            }
        }
    }

    /**
     * Lower the given figure.
     */
    public void lowerFigure( DrawFigure figure )
    {
        if ( figureList.contains( figure ) ) {
            graphics.lowerFigure( figure );
        }
    }

    /**
     * Return a ListIterator over the figures. If forward is set false
     * the Iterator will be set to the end of the list so that it is
     * ready to be traversed backwards.
     */
    public ListIterator getListIterator( boolean forward )
    {
        if ( forward ) {
            return figureList.listIterator( 0 );
        }
        else {
            return figureList.listIterator( figureList.size() );
        }
    }

    /**
     * Create a DrawFigure using preset properties.
     */
    public DrawFigure createDrawFigure( int type, FigureProps props )
    {
        DrawFigure figure = figureFactory.create( type, props );
        addDrawFigure( figure );
        return figure;
    }

    /**
     * Add a DrawFigure created to the managed list.
     */
    public void addDrawFigure( DrawFigure figure )
    {
        graphics.addFigure( figure );
        figureList.add( figure );
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

        return decorator;
    }

    /**
     * Interact with the object for saving and restoring figures from
     * an XML store of some kind. The simple action here communicates
     * that the user wants to interact, we just supply a reference so
     * that this system may interact with various components.
     */
    protected void saveRestore()
    {
        if ( store != null ) {
            store.setDrawActions( this );
            store.activate();
        }
    }

    /**
     * Set the {@link FigureStore}.
     */
    public void setFigureStore( FigureStore store )
    {
        this.store = store;
        saveRestoreAction.setEnabled( store != null );
    }

    /**
     * Get the {@link FigureStore}.
     */
    public FigureStore getFigureStore()
    {
        return store;
    }

    // -- Local Action classes --

    /** Action to use to delete the selected figure. */
    protected AbstractAction deleteSelectedAction =
        new AbstractAction( "Delete selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                deleteSelected();
            }
        };
    public Action getDeleteSelectedAction()
    {
        return deleteSelectedAction;
    }


    /** Action to use to remove all figures. */
    protected AbstractAction clearAction =
        new AbstractAction( "Remove all" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                clear();
            }
        };
    public Action getClearAction()
    {
        return clearAction;
    }

    /** Action to use to toggle the visibility of all figures. */
    protected AbstractAction hideGraphicsAction =
        new AbstractAction( "Hide figures" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                hideGraphics();
            }
        };
    public Action getHideAction()
    {
        return hideGraphicsAction;
    }

    /** Action to raise selected Figures  */
    protected AbstractAction raiseSelectedAction =
        new AbstractAction( "Raise selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                raiseSelected();
            }
        };
    public Action getRaiseSelectedAction()
    {
        return raiseSelectedAction;
    }

    /** Action to lower selected Figures  */
    protected AbstractAction lowerSelectedAction =
        new AbstractAction( "Lower selected" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                lowerSelected();
            }
        };
    public Action getLowerSelectedAction()
    {
        return lowerSelectedAction;
    }

    /** Action to save or restore the figures */
    protected AbstractAction saveRestoreAction =
        new AbstractAction( "Save/restore figures" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                saveRestore();
            }
        };

    /** Local base class for creating menu/toolbar actions. */
    protected abstract class GraphicsAction
        extends AbstractAction
    {
        String name;

        // Each action may have an icon associated with its name, try
        // looking for these in a standard place. This is fails the
        // string is just used.
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
                //  Do nothing.
            }
            this.name = name;
        }
    }

    /**
     * Local class used to set the drawing mode.
     */
    protected class DrawingModeAction
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
    protected class LineWidthAction
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
    protected class OutlineAction
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
    protected class FillAction
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
    protected class CompositeAction
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
    protected class FontAction
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
    protected class CurveAction
        extends AbstractAction
    {
        int interpolator = InterpolatorFactory.HERMITE;

        public CurveAction( int interpolator )
        {
            super( interpolatorFactory.getShortName( interpolator ) );
            this.interpolator = interpolator;
        }

        public void actionPerformed( ActionEvent evt )
        {
            setCurve( interpolator );
        }
    }
}
