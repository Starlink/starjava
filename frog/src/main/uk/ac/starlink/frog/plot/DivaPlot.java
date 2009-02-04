package uk.ac.starlink.frog.plot;

import diva.canvas.JCanvas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.gui.AstPlotSource;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.gui.ColourStore;
import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.ast.gui.AstTicks;
import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.frog.data.DataLimits;
import uk.ac.starlink.frog.data.TimeSeries;
import uk.ac.starlink.frog.data.TimeSeriesComp;
import uk.ac.starlink.frog.data.TimeSeriesFactory;
import uk.ac.starlink.frog.util.FrogException;

import uk.ac.starlink.diva.*;

/**
 * Plots an atimes series using a Swing component with Diva graphics
 * overlay support. <p>
 *
 * Series are defined as TimeSeries objects encapulsulated in a TimeSeriesComp
 * object. <p>
 *
 * Requires AstUtilities and DefaultGrf objects to actually draw the graphics
 * and perform any world coordinate transformations. 
 
 * TODO: remove most uses of AstUtilities that have been deprecated.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see #Diva,#AstUtilities
 */
public class DivaPlot
     extends JCanvas
     implements AstPlotSource, Draw, Printable, MouseListener
{
    /**
     * X scale factor for displaying data.
     */
    protected float xScale = 1.0F;

    /**
     * Y scale factor for displaying data.
     */
    protected float yScale = 1.0F;

    /**
     * Have either scales changed? If so need complete redraw.
     */
    protected boolean xyScaled = true;

    /**
     * X and Y scales used last call to setScale.
     */
    protected float lastXScale = 0.0F;
    /**
     * Description of the Field
     */
    protected float lastYScale = 0.0F;

    /**
     * X scale that maps data range into initial graphic size.
     */
    protected float baseXScale = 1.0F;

    /**
     * Y scale that maps data range into initial graphic size.
     */
    protected float baseYScale = 1.0F;

    /**
     * Number of pixels reserved at edges.
     */
    protected int xSaved = 70;

    /**
     * Number of pixels reserved at edges.
     */
    protected int ySaved = 30;

    /**
     * Smallest X data value.
     */
    protected double xMin;

    /**
     * Largest X data value.
     */
    protected double xMax;

    /**
     * Smallest Y data value.
     */
    protected double yMin;

    /**
     * Largest Y data value.
     */
    protected double yMax;

    /**
     * Bounding box for creating astPlot from physical coordinates
     */
    protected double[] baseBox = new double[4];

    /**
     * Reference to series composite object. This plots the actual series
     * and mediates requests about composite properties.
     */
    protected TimeSeriesComp series = null;

    /**
     * AstUtilities object that mediates AST graphics drawing requests and
     * transforms coordinates.
     */
    protected AstUtilities astJ = null;

    /**
     * DefaultGrf object that manages all graphics.
     */
    protected DefaultGrf javaGrf = null;

    /**
     * Object that contains a description of the non-series AST properties
     * of the plot (i.e. labels etc.).
     */
    protected PlotConfiguration config = new PlotConfiguration();

    /**
     * Object that contains a description of the data limits to be
     * applied.
     */
    protected DataLimits dataLimits = new DataLimits();

     /**
     * Object that contains a description of how much of the edge
     * regions are to be retained.
     */
    protected GraphicsEdges graphicsEdges = new GraphicsEdges();

    /**
     * Object that contains a description of what hints should be used
     * when drawing the graphics.
     */
    protected GraphicsHints graphicsHints = new GraphicsHints();

    /**
     * Object that contains the colour used for the component
     * background.
     */
    protected ColourStore backgroundColourStore = 
        new ColourStore( "background" );

     /**
     * Special pane for displaying interactive graphics.
     */
    protected DivaPlotGraphicsPane graphicsPane = null;

    /**
     * The instance of DrawActions to be used.
     */
    protected DrawActions drawActions = null;
    
    /**
     * Whether we should show the vertical hair.
     */
    protected boolean showVHair = false;

    /**
     * Plot a series of series.
     *
     * @param series reference to series.
     *
     * @exception FrogException thrown if problems reading series.
     */
    public DivaPlot( TimeSeriesComp series )
        throws FrogException
    {
        super();
        initConfig();
        initSeries( series );
    }

    /**
     * Plot a series stored in a file.
     *
     * @param file name of file containing series.
     *
     * @exception FrogException thrown if problems reading series.
     */
    public DivaPlot( String file ) throws FrogException
    {
        super();
        initConfig();
        TimeSeriesFactory factory = TimeSeriesFactory.getReference();
        TimeSeriesComp series;
        TimeSeries specData;
        specData = factory.get( file );
        series = new TimeSeriesComp( specData );
        initSeries( series );
    }

    /**
     * Initialize the PlotConfiguration object to one that provides
     * all the facilities that can be used to configure this.
     * <p>
     * This adds a GraphicsEdges, GraphicsHints and ColourStore
     * objects. The ColourStore is setup to control the Plot
     * background.
     */
    protected void initConfig()
    {
        config.add( graphicsHints );
        config.add( graphicsEdges );
        config.add( backgroundColourStore );
        config.add( dataLimits );
    }

    /**
     * Initialise series information.
     *
     * @param series Description of the Parameter
     *
     * @exception FrogException thrown if problems reading series.
     */
    protected void initSeries( TimeSeriesComp series )
        throws FrogException
    {
        //  Autoscrolls when in a scrollable component.
        setAutoscrolls( true );

        //  Add a DivaPlotGraphicsPane to use for displaying
        //  interactive graphics elements.
        graphicsPane = new DivaPlotGraphicsPane(
                            DrawActions.getTypedDecorator() );
        setCanvasPane( graphicsPane );
        getCanvasPane().setAntialiasing( false );

        //  Overlay layer needs stronger colour.
        graphicsPane.getOverlayLayer().setPaint( Color.darkGray );

        //  Retain reference to series data and properties.
        this.series = series;

        //  Initialize preferred initial size.
        setPreferredSize( new Dimension( 600,400 ) );

        //  Create the required DefaultGrf object to draw graphics.
        javaGrf = new DefaultGrf( this );

        //  First time initialisation of series properties.
        updateProps( true );

        //  Add any keyboard interactions that we want.
        addKeyBoardActions();

        // Listen for mouse events so we can control the keyboard
        // focus.
        addMouseListener( this );
    }

    /**
     * Get a reference to the TimeSeriesComp that we're using.
     *
     * @return The specDataComp value
     */
    public TimeSeriesComp getTimeSeriesComp()
    {
        return series;
    }

    /**
     * Change the TimeSeriesComp that we should use.
     *
     * @param series The new specDataComp value
     *
     * @exception FrogException thrown if problems reading series.
     */
    public void setTimeSeriesComp( TimeSeriesComp series ) throws FrogException
    {
        this.series = series;
        updateProps( true );
    }

    /**
     * Gets the focusTraversable attribute of the DivaPlot object
     *
     * @return The focusTraversable value
     */
    public boolean isFocusTraversable()
    {
        //  This component would like to receive the focus.
        //  Javadoc comments from JComponent.
        return isEnabled();
    }

    /**
     * Add any keyboard interactions that we want to provide.
     */
    protected void addKeyBoardActions()
    {
        //  Add key shift-left and shift-right bindings for fine
        //  positioning of the vertical hair.
        Action leftAction =
            new AbstractAction( "moveLeft" )
            {
                public void actionPerformed( ActionEvent e )
                {
                    scrollVHair( -1 );
                }
            };
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT,
            KeyEvent.SHIFT_MASK ),
            leftAction );

        Action rightAction =
            new AbstractAction( "moveRight" )
            {
                public void actionPerformed( ActionEvent e )
                {
                    scrollVHair( 1 );
                }
            };
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT,
            KeyEvent.SHIFT_MASK ),
            rightAction );

        //  The space-bar (with control, alt and shift modifiers)
        //  makes the interactive readouts show the interpolated
        //  vertical hair coordinates (this can be true if the hair is
        //  moved by the arrow keys above).
        Action spaceAction =
            new AbstractAction( "showVHairCoords" )
            {
                public void actionPerformed( ActionEvent e )
                {
                    showVHairCoords();
                }
            };
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 ),
            spaceAction );
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE,
            KeyEvent.CTRL_MASK ),
            spaceAction );
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE,
            KeyEvent.META_MASK ),
            spaceAction );
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE,
            KeyEvent.SHIFT_MASK ),
            spaceAction );
    }

    /**
     * Add an action to associate with a KeyStroke.
     *
     * @param keyStroke the KeyStroke (e.g. shift-left would be obtained
     *      using: KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT,
     *      KeyEvent.SHIFT_MASK ).
     * @param action the Action (i.e. implementation of AbstractAction) to
     *      associate with the KeyStroke.
     */
    protected void addKeyBoardAction( KeyStroke keyStroke, Action action )
    {
        getInputMap().put( keyStroke, action );
        getActionMap().put( action, action );
    }

    /**
     * Get the AST graphics configuration object.
     *
     * @return The PlotConfiguration reference.
     */
    public PlotConfiguration getPlotConfiguration()
    {
        return config;
    }

    /**
     * Get the DataLimits configuration object.
     *
     * @return The DataLimits reference.
     */
    public DataLimits getDataLimits()
    {
        return dataLimits;
    }

    /**
     * Get the GraphicsEdges configuration object.
     *
     * @return The GraphicsEdges reference.
     */
    public GraphicsEdges getGraphicsEdges()
    {
        return graphicsEdges;
    }

    /**
     * Get the GraphicsHints configuration object.
     *
     * @return The GraphicsHints reference.
     */
    public GraphicsHints getGraphicsHints()
    {
        return graphicsHints;
    }

    /**
     * Get the ColourStore that can be used to control the
     * background.
     *
     * @return The background ColourStore.
     */
    public ColourStore getBackgroundColourStore()
    {
        return backgroundColourStore;
    }

    /**
     * Update the composite properties of the series and setup for display.
     *
     * @param init Description of the Parameter
     *
     * @exception FrogException thrown if problems reading series.
     */
    public void updateProps( boolean init )
        throws FrogException
    {
        //  No series, so maybe nothing to do.
        if ( series.count() == 0 ) {
            return;
        }

        //  Get the series AST frameset, that describes the
        //  coordinates and set the DefaultGrf object is should use to draw
        //  graphics.
        astJ = series.getAst();
        astJ.setGraphic( javaGrf );

        //  Set the data limits for plotting.
        setDataLimits();

        //  Define the size of the box used to draw the series
        //  (physical coordinates, i.e. the current frame).
        if ( dataLimits.isXFlipped() ) {
            baseBox[0] = xMax;
            baseBox[2] = xMin;
        } 
        else {
            baseBox[0] = xMin;
            baseBox[2] = xMax;
        }
        if ( dataLimits.isYFlipped() ) {
            baseBox[1] = yMax;
            baseBox[3] = yMin;
        }
        else {
            baseBox[1] = yMin;
            baseBox[3] = yMax;
        }
     

        //  The base scales are defined to fit the data content into
        //  the plotting area. Do this once so that scales remain same
        //  to user.
        if ( init ) {
            setBaseScale();
        }

        //  Set the preferred size of the component -- big enough for
        //  pixel resolution times the scale factor.
        setScale( xScale, yScale );

        //  Establish the border for labelling.
        setBorder( xSaved, ySaved );
    }

    /**
     * Set the range of any plotted axes. These can be overridden by
     * values in the DataLimits object, or set to the minimum/maximum
     * of the data values.
     */
    public void setDataLimits()
    {
        if ( dataLimits == null ) {
            setAutoLimits();
        }
        else {
            if ( dataLimits.isYAutoscaled() || dataLimits.isXAutoscaled() ) {
                setAutoLimits();
            }
            if ( ! dataLimits.isXAutoscaled() ) {
                xMin = dataLimits.getXLower();
                xMax = dataLimits.getXUpper();
            }
            if ( ! dataLimits.isYAutoscaled() ) {
                yMin = dataLimits.getYLower();
                yMax = dataLimits.getYUpper();
            }
        }
    }

    /**
     * Set the plot limits to those of the data.
     */
    protected void setAutoLimits()
    {
        double[] range = series.getAutoRange();
        xMin = range[0];
        xMax = range[1];
        yMin = range[2];
        yMax = range[3];
    }

    /**
     * Set the base scale. This matches the plot size to the viewable area
     * (this is what scale equals 1 implies).
     */
    public void setBaseScale()
    {
        Dimension size = getPreferredSize();
        baseXScale = (float) size.width / (float) ( xMax - xMin );
        baseYScale = (float) size.height / (float) ( yMax - yMin );
    }

    /**
     * Get the current X dimension scale factor.
     *
     * @return The xScale value
     */
    public float getXScale()
    {
        return xScale;
    }

    /**
     * Get the current Y dimension scale factor
     *
     * @return The yScale value
     */
    public float getYScale()
    {
        return yScale;
    }

    /**
     * Fit series to the displayed width. Follow this with a
     * setScale( 1, x ), to update the display.
     */
    public void fitToWidth()
    {
        Dimension size = getSize();
        baseXScale = (float) size.width / (float) ( xMax - xMin );
        lastXScale = 0;
    }

    /**
     * Fit series to the displayed height. Follow this with a setScale( x, 1
     * ), to update the display.
     */
    public void fitToHeight()
    {
        Dimension size = getSize();
        baseYScale = (float) size.height / (float) ( yMax - yMin );
        lastYScale = 0;
    }

    /**
     * Reset the preferred size of the whole component to match the current
     * scaling configuration.
     */
    protected void resetPreferredSize()
    {
        //  Set the requested size of the plotting component. This
        //  needs to be honoured by the user of this class somehow
        //  (use center of a BorderLayout, probably with a
        //  JScrollPane, if expecting to resize after creation).
        setPreferredSize
            ( new Dimension( (int) ( baseXScale * xScale * ( xMax - xMin ) ),
            (int) ( baseYScale * yScale * ( yMax - yMin ) ) ) );
    }

    /**
     * Update the plot to reflect any changes in the series being plotted
     * (i.e. when a new series is added).
     *
     * @exception FrogException thrown if problems reading series.
     */
    public void update()
        throws FrogException
    {
        updateProps( false );
        updateComponent();
    }

    /**
     * Update the plot to reflect any changes in the component size etc. Do
     * not perform complete reinitialisation.
     */
    protected void updateComponent()
    {
        resetPreferredSize();
        xyScaled = true;
        revalidate();
        repaint();
    }

    /**
     * Set the display scale factor. This is actually implemented by resizing
     * our component size.
     *
     * @param xs The new X scale value
     * @param ys The new Y scale value
     */
    public void setScale( float xs, float ys )
    {
        if ( xs != lastXScale || ys != lastYScale ) {

            //  Record scales to avoid unnecessary updates.
            lastXScale = xs;
            lastYScale = ys;

            //  Positive values are direct scales, negative are inverse.
            if ( xs == 0.0F ) {
                xScale = 1.0F;
            }
            else if ( xs < 0.0F ) {
                xScale = Math.abs( 1.0F / xs );
            }
            else {
                xScale = xs;
            }
            if ( ys == 0.0F ) {
                yScale = 1.0F;
            }
            else if ( ys < 0.0F ) {
                yScale = Math.abs( 1.0F / ys );
            }
            else {
                yScale = ys;
            }
            updateComponent();
        }
    }

    /**
     * Set the number of pixels reserved for axis labelling.
     *
     * @param x The new X border value
     * @param y The new Y border value
     */
    public void setBorder( int x, int y )
    {
        Insets curBorder = getInsets();
        EmptyBorder newBorder = new EmptyBorder( y, x, y, x );
        super.setBorder( newBorder );
        curBorder = getInsets();
        xSaved = x;
        ySaved = y;
        repaint();
    }

    /**
     * Draw the series. This should use the current plotting attributes and
     * style.
     */
    protected void drawSeries()
    {
        double[] limits = null;
        if ( graphicsEdges != null ) {
            if ( graphicsEdges.isClipped() ) {
                limits = new double[4];
                if ( dataLimits.isXFlipped() ) {
                   limits[0] = xMax;
                   limits[2] = xMin;
                } 
                else {
                   limits[0] = xMin;
                   limits[2] = xMax;
                }
                if ( dataLimits.isYFlipped() ) {
                   limits[1] = yMax;
                   limits[3] = yMin;
                }
                else {
                   limits[1] = yMin;
                   limits[3] = yMax;
                }
            }
        }
        series.drawSeries( javaGrf, astJ.getPlot(), limits );
    }

    private Component parent = null;

    /**
     * Sets the parent attribute of the DivaPlot object
     *
     * @param parent The new parent value
     */
    public void setParent( Component parent )
    {
        this.parent = parent;
    }

    /**
     * PaintComponent method for drawing/redrawing graphics when interface
     * requires it.
     *
     * @param g Graphics object
     */
    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        if ( series.count() == 0 ) {
            return;
        }

        //  XXX restore the correct DefaultGrf object to the AST interface.
        astJ.setGraphic( javaGrf );

        if ( xyScaled ) {

            //  Scale of plot has changed or been set for the first
            //  time. So we need to redraw everything.
            xyScaled = false;

            //  Keep reference to existing AstPlot so we know how
            //  graphics coordinates are already drawn.
            Plot oldAstPlot = astJ.getPlot();

            //  Create an astPlot for the graphics, this is matched to
            //  the component size and is how we get an apparent
            //  rescale of drawing. So that we get linear axis 1
            //  coordinates we must choose the current frame as the
            //  base frame (so that the mapping from graphics to
            //  physical is linear). We also add any AST plotting
            //  configuration options. TODO: stop using AstUtilities class.
            FrameSet astref = astJ.getRef();
            String current = astref.getC( "Current" );
            String base = astref.getC( "Base" );
            astref.set( "Base=" + current );

            if ( config != null ) {
                if ( graphicsEdges != null ) {
                    astJ.astPlot( this, baseBox,
                                  graphicsEdges.getXLeft(),
                                  graphicsEdges.getXRight(),
                                  graphicsEdges.getYTop(),
                                  graphicsEdges.getYBottom(),
                                  config.getAst() );
                }
                else {
                    astJ.astPlot( this, baseBox, 0.05, 0.00, 0.03, 0.03, 
                                  config.getAst() );
                }
            }
            else {
                astJ.astPlot( this, baseBox, 0.05, 0.00, 0.03, 0.03, "" );
            }

            // The plot must use our Grf implementation.
            astJ.getPlot().setGrf( javaGrf );

            //  Restore the base plot as promised.
            astref.set( "Base=" + base );

            //  If requested (i.e. the default gap is chosen) then
            //  decrease the gap between X major ticks so we see more
            //  labels when zoomed.
            double xGap = 0.0;
            if ( config != null ) {
                AstTicks astTicks = 
                    (AstTicks) config.getControlsModel( AstTicks.class );
                xGap = astTicks.getXGap();
            }
            if ( xGap == 0.0 || xGap == DefaultGrf.BAD ) {

                //  TODO: make this work again.
                xGap = astJ.getPlot().getD( "gap(1)" );
                xGap = xGap / Math.max( 1.0, xScale * 0.5 );
                astJ.astSetPlot( "gap(1)=" + xGap );
            }

            //  Clear all existing AST graphics
            javaGrf.reset();

            //  Draw the coordinate grid/axes.
            astJ.astGrid();

            //  Draw the series.
            drawSeries();

            //  Resize overlay graphics.
            if ( oldAstPlot != null ) {
                redrawOverlay( oldAstPlot );
            }

            //  Inform any listeners that the Plot has been scaled.
            fireScaled();

            //  Drawn at least once, so OK to track mouse events. XXX
            //  still a few seen AST errors about null pointer..
            readyToTrack = true;
        }

        //  Use antialiasing for either the text, lines and text, or
        //  nothing.
        if ( graphicsHints != null ) {
            graphicsHints.applyRenderingHints( (Graphics2D) g );
        }

        //  Repaint all graphics.
        astJ.getPlot().paint( g );
    }

    /**
     * Redraw any overlay graphics to match a change in size.
     *
     * @param oldAstPlot reference to the AstPlot used to draw the graphics
     *      (this has a base coordinate system xin the old graphics
     *      coordinates).
     */
    protected void redrawOverlay( Plot oldAstPlot )
    {
        //  Pass old and new AstPlots to the overlay pane. This will
        //  use them to transform the graphics positions.
        graphicsPane.astTransform( oldAstPlot, astJ.getPlot() );

        //  Force a reset of the vertical hair, if vertical scale is
        //  changed.
        resetVHair();
    }

    /**
     * If shown, remove the vertical hair.
     */
    public void removeVHair()
    {
        if ( barShape != null ) {
            graphicsPane.getOverlayLayer().remove( barShape );
            graphicsPane.getOverlayLayer().repaint( barShape );
            barShape = null;
        }
    }

    /**
     * Reset the vertical hair to match the current zoom.
     */
    public void resetVHair()
    {
        float x = getVHairPosition();
        removeVHair();
        setVHairPosition( x );
    }

    /**
     * Get the position of the vertical hair (graphics coordinates).
     *
     * @return The position
     */
    public float getVHairPosition()
    {
        if ( barShape == null ) {
            return 0;
        }
        else {
            return barShape.x1;
        }
    }

    /**
     * Move the vertical hair to a new position.
     *
     * @param x The new position
     */
    public void setVHairPosition( float x )
    {
        if ( showVHair ) {
            if ( barShape == null ) {
                float[] limits = astJ.getGraphicsLimits();
                barShape = new Line2D.Float( (float) x, limits[1],
                    (float) x, limits[3] );
                graphicsPane.getOverlayLayer().add( barShape );
            }

            //  Repaint the current figure region, move figure and
            //  then repaint in new position (otherwise get trails).
            graphicsPane.getOverlayLayer().repaint( barShape );
            barShape.x1 = x;
            barShape.x2 = x;
            graphicsPane.getOverlayLayer().repaint( barShape );
        }
    }

    /**
     * Line used as the vertical hair.
     */
    protected Line2D.Float barShape = null;

    /**
     * Scroll the vertical hair from its current position.
     *
     * @param increment the change in x position.
     */
    public void scrollVHair( float increment )
    {
        float xnow = getVHairPosition();
        setVHairPosition( xnow + increment );
    }

    /**
     * Display the series coordinate and interpolated value at the position
     * of the vertical hair. Requires that a MouseMotionTracker has been
     * established by the trackMouseMotion method.
     *
     * @see trackMouseMotion
     */
    protected void showVHairCoords()
    {
        if ( series.count() == 0 || ! showVHair ) {
            return;
        }
        String[] xypos =
            series.formatInterpolatedLookup( (int) getVHairPosition(),
                                              astJ.getPlot() );
        tracker.updateCoords( xypos[0], xypos[1] );
    }

    /**
     * Transform a series of positions between the graphics and current
     * coordinate systems (of the AstPlot).
     *
     * @param positions position in x,y pairs
     * @param forward whether to use forward transformation
     * @return transformed positions, x[0] is [0][0], y[0] is [1][0] etc.
     */
    public double[][] transform( double[] positions, boolean forward )
    {
        return AstUtilities.astTran2( astJ.getPlot(), positions, forward );
    }

    /**
     * Access the limits of the graphics coordinates used to draw the plot.
     *
     * @return coordinate limits (graphbox of Plot)
     */
    public float[] getGraphicsLimits()
    {
        return astJ.getGraphicsLimits();
    }

    /**
     * Register object to receive mouse motion events.
     *
     * @param tracker name of an object whose class implements the
     *      MouseMotionTracker interface. This will have its updateCoords
     *      method called when a mouseMoved event is trapped.
     */
    public void trackMouseMotion( final MouseMotionTracker tracker )
    {
        this.tracker = tracker;
        addMouseMotionListener(
            new MouseMotionListener()
            {
                public void mouseMoved( MouseEvent e )
                {
                    if ( series.count() == 0 || ! readyToTrack ) {
                        return;
                    }
                    String[] xypos = series.formatLookup( e.getX(),
                                                           astJ.getPlot() );
                    tracker.updateCoords( xypos[0], xypos[1] );
                    setVHairPosition( e.getX() );
                }

                public void mouseDragged( MouseEvent e )
                {
                    if ( series.count() == 0 ) {
                        return;
                    }
                }
            } );

        //  TODO: re-implement this using a listener scheme.
    }
    private MouseMotionTracker tracker = null;
    private boolean readyToTrack = false;

    /**
     * Convert a formatted coordinate into a floating point value.
     *
     * @param axis the axis of the Plot (1 or 2) to use for when determining
     *      the current formatting rules.
     * @param value the formatted value.
     * @return the floating point representation.
     */
    public double unFormat( int axis, String value )
    {
        if ( series.count() == 0 ) {
            return 0.0;
        }
        return series.unFormat( axis, astJ.getPlot(), value );
    }

    /**
     * Convert a floating point coordinate into a formatted value.
     *
     * @param axis the axis of the Plot (1 or 2) to use for when determining
     *      the current formatting rules.
     * @param value the value for format.
     * @return the formatted value, returns null for failure.
     */
    public String format( int axis, double value )
    {
        if ( series.count() == 0 ) {
            return null;
        }
        return series.format( axis, astJ.getPlot(), value );
    }

    /**
     * Return a label that can be used to identify the units of the given
     * axis. Note the AST description will be used if the plot hasn't been
     * drawn yet (these are generally the same).
     *
     * @param axis the axis
     * @return The axis label
     */
    public String getLabel( int axis )
    {
        if ( series.count() == 0 ) {
            return "";
        }

        Plot plot = astJ.getPlot();
        if ( plot != null ) {
            return plot.getC( "label(" + axis + ")" );
        }

        //  Try WCS description.
        FrameSet astref = astJ.getRef();
        if ( astref != null ) {
            astref.getC( "label(" + axis + ")" );
        }
        return new String( "Axis" + axis );
    }

    //  Make a postscript copy of the series.
    public int print( Graphics g, PageFormat pf, int pageIndex )
    {
        if ( pageIndex > 0 ) {
            return NO_SUCH_PAGE;
        }

        //  Set background to white to match paper.
        Color oldback = getBackground();
        setBackground( Color.white );

        //  Make graphics shift and scale up/down to fit the page.
        Graphics2D g2 = (Graphics2D) g;
        fitToPage( g2, pf );

        //  Print the series.
        print( g2 );

        //  Restore background colour.
        setBackground( oldback );

        return PAGE_EXISTS;
    }

    /**
     * Shift and scale the current graphic so that it fits within a printed
     * page.
     *
     * @param g2 Graphics2D object that mediates printing
     * @param pf the current page format
     */
    protected void fitToPage( Graphics2D g2, PageFormat pf )
    {
        //  Get size of viewable page area and derive scale to make
        //  the content fit.
        double pageWidth = pf.getImageableWidth() - 2.0 * xSaved;
        double pageHeight = pf.getImageableHeight() - 2.0 * ySaved;
        double xinset = pf.getImageableX() + xSaved;
        double yinset = pf.getImageableY() + ySaved;
        double compWidth = (double) getWidth();
        double compHeight = (double) getHeight();
        double xscale = pageWidth / compWidth;
        double yscale = pageHeight / compHeight;
        if ( xscale < yscale ) {
            yscale = xscale;
        }
        else {
            xscale = yscale;
        }

        //  Shift and scale.
        g2.translate( xinset, yinset );
        g2.scale( xscale, yscale );
    }

    /**
     * Return reference to the DivaPlotGraphicsPane object. Use this to create
     * any interactive graphics objects.
     *
     * @return The graphicsPane value
     * @see DrawGraphicsPane, Diva, AbstractFigure, GraphicsPane
     */
    public DrawGraphicsPane getGraphicsPane()
    {
        return graphicsPane;
    }
  
    /**
     * Reference to the thing thats doing the drawing
     */
    public Component getComponent()
    {
        return this;
    }
    /**
     * Return reference to the FrameSet used to transform from
     * graphics to world coordinates. This is really a direct
     * reference to the AstPlot, so do not modify it (any changes
     * would be lost on the next resize).
     *
     * @return The Plot FrameSet
     */
    public FrameSet getMapping()
    {
        return astJ.getPlot();
    }

    /**
     * Set whether the plot is showing the vertical hair.
     *
     * @param show Whether to show the vertical hair
     */
    public void setShowVHair( boolean show )
    {
        if ( showVHair ) {
            if ( ! show ) {
                removeVHair();
            }
        }
        this.showVHair = show;
    }

    /**
     * Say if the plot is showing the vertical hair.
     *
     * @return Whether vertical hair is showing or not
     */
    public boolean isShowVHair()
    {
        return this.showVHair;
    }

//
// PlotScaleChanged event interface
//
    protected EventListenerList plotScaledListeners = new EventListenerList();

    /**
     * Registers a listener for to be informed when Plot changes scale.
     *
     * @param l the PlotScaledListener
     */
    public void addPlotScaledListener( PlotScaledListener l )
    {
        plotScaledListeners.add( PlotScaledListener.class, l );
    }

    /**
     * Remove a listener for Plot scale changes.
     *
     * @param l the PlotScaledListener
     */
    public void removePlotScaledListener( PlotScaledListener l )
    {
        plotScaledListeners.remove( PlotScaledListener.class, l );
    }

    /**
     * Send an event to all PlotScaledListener when the plot drawing scale is
     * changed.
     */
    protected void fireScaled()
    {
        Object[] list = plotScaledListeners.getListenerList();
        PlotScaleChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == PlotScaledListener.class ) {
                if ( e == null ) {
                    e = new PlotScaleChangedEvent( this );
                }
                ( (PlotScaledListener) list[i + 1] ).plotScaleChanged( e );
            }
        }
    }

    //
    // DrawActions. Used when creating interactive figures on this.
    //
    /**
     * Return the instance of {@link DrawActions} to use with this DivaPlot.
     * Note this is setup to not provide save/restore. A user of this class
     * should add an implementation of {@link FigureStore} to enable this.
     */
    public DrawActions getDrawActions()
    {
        if ( drawActions == null ) {
            drawActions = new DrawActions( this, null );
        }
        return drawActions;
    }

    // Implementation of AstPlotSource interface.
    public Plot getPlot()
    {
        return astJ.getPlot();
    }    
    
//
//  Implement the MouseListener interface. The purpose of this is to
//  simply make sure that we receive the keyboard focus when anyone
//  clicks on this component. TODO: understand why this is necessary.
//
    public void mouseClicked( MouseEvent e )
    {
        requestFocus();
    }
    public void mouseEntered( MouseEvent e )
    {
        // Do nothing.
    }
    public void mouseExited( MouseEvent e )
    {
        //  Do nothing.
    }
    public void mousePressed( MouseEvent e )
    {
        requestFocus();
    }
    public void mouseReleased( MouseEvent e )
    {
        //  Do nothing.
    }
}
