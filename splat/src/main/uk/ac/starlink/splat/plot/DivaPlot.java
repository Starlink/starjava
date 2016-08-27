/*
 * Copyright (C) 2002-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *    12-SEP-1999 (Peter W. Draper):
 *       Original version.
 *    06-JUN-2002 (Peter W. Draper):
 *       Changed to use JNIAST instead of local JNI version.
 *    24-FEB-2005 (Peter W. Draper):
 *       Changed meaning of scaling a plot. This now applies to the graphics
 *       size, not the world coordinates.
 *    01-MAR-2005 (Peter W. Draper):
 *       Added an option to only display the grid within the bounds of
 *       the component visible region. Major changes.
 */
package uk.ac.starlink.splat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.gui.AstAxes;
import uk.ac.starlink.ast.gui.AstPlotSource;
import uk.ac.starlink.ast.gui.AstTicks;
import uk.ac.starlink.ast.gui.ColourStore;
import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.diva.Draw;
import uk.ac.starlink.diva.DrawActions;
import uk.ac.starlink.diva.DrawGraphicsPane;
import uk.ac.starlink.diva.FigureStore;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.data.ObjectTypeEnum;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.plot.behavior.MagnitudeAxisInvertingBehavior;
import uk.ac.starlink.splat.plot.behavior.PlotBehavior;
import uk.ac.starlink.splat.util.SplatException;
import diva.canvas.JCanvas;

/**
 * Plots an astronomical spectra using a Swing component with Diva graphics
 * overlay support. <p>
 *
 * Spectra are defined as SpecData objects encapulsulated in a SpecDataComp
 * object. <p>
 *
 * Requires ASTJ and DefaultGrf objects to actually draw the graphics and
 * perform any world coordinate transformations. TODO: remove most
 * uses of ASTJ that have been deprecated.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see ASTJ
 */
public class DivaPlot
    extends JCanvas
    implements Draw, Printable, MouseListener, AstPlotSource
{
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.plot.DivaPlot" );

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
     * X scale that defines a scale factor of one.
     */
    protected float baseXScale = 1.0F;

    /**
     * Y scale that defines a scale factor of one.
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
     * Bounding box for creating full-sized Plot from physical coordinates.
     */
    protected double[] baseBox = new double[4];

    /**
     * Bounding box of visible region in physical coordinates.
     */
    protected double[] visibleBaseBox = new double[4];

    /**
     * Reference to spectra composite object. This plots the actual spectra
     * and mediates requests about composite properties.
     */
    protected SpecDataComp spectra = null;

    /**
     * ASTJ object that mediates AST graphics drawing requests and transforms
     * coordinates.
     */
    protected ASTJ astJ = null;

    /**
     * DefaultGrf object that manages all AST graphics.
     */
    protected DefaultGrf mainGrf = null;

    /**
     * The current Plot used for all drawing.
     */
    protected Plot mainPlot = null;

    /**
     *  Graphics limits of the plot region.
     */
    protected float[] graphbox = new float[4];

    /**
     * Object that contains a description of the non-spectral AST properties
     * of the plot.
     */
    protected PlotConfiguration plotConfig = new PlotConfiguration();

    /**
     * The AstTicks instance of the PlotConfiguration object.
     */
    protected AstTicks astTicks = null;

    /**
     * The AstAxes instance of the PlotConfiguration object.
     */
    protected AstAxes astAxes = null;

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
     * Whether we should show the vertical hair.
     */
    protected boolean showVHair = false;

    /**
     * The instance of DrawActions to be used.
     */
    protected DrawActions drawActions = null;

    /**
     * Whether to just draw in the visible region of the component.
     */
    protected boolean visibleOnly = false;

    /**
     * Whether coordinate tracking moves freely around display reporting plot
     * coordinates or is attached to the current spectrum and reports spectral
     * coordinates and data values.
     */
    protected boolean trackFreely = false;

    /**
     * Whether to draw line identifiers aligned to both systems of a DSB
     * spectrum or not.
     */
    private boolean doubleDSBLineIds = true;

    /**
     * Stacker to control the stacking of spectra when a shift is to be
     * applied so that they can be viewed.
     */
    protected PlotStacker stacker = new PlotStacker( this, null, true, 0.0 );

    /**
     * Whether the PlotStacker facility should be used.
     */
    private boolean usePlotStacker = false;

    /**
     * Whether to show the plot legend.
     */
    private boolean showLegendFigure = false;

    /**
     * The legend figure.
     */
    protected SpecLegendFigure legendFigure = null;
    
    protected List<PlotBehavior> behaviors = new LinkedList<PlotBehavior>();

    /**
     * Plot a series of spectra.
     *
     * @param spectra reference to spectra.
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    public DivaPlot( SpecDataComp spectra )
        throws SplatException
    {
        super();
        initConfig();
        initSpec( spectra );
    }

    /**
     * Plot a spectrum stored in a file.
     *
     * @param file name of file containing spectrum.
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    public DivaPlot( String file )
        throws SplatException
    {
        super();
        initConfig();
        SpecDataFactory factory = SpecDataFactory.getInstance();
        SpecDataComp spectrum;
        SpecData specData;
        specData = factory.get( file );
        spectrum = new SpecDataComp( specData );
        initSpec( spectrum );
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
        plotConfig.add( graphicsHints );
        plotConfig.add( graphicsEdges );
        plotConfig.add( backgroundColourStore );
        plotConfig.add( dataLimits );

        //  The AstTicks object is used for scaling the tick separation when
        //  zoomed.
        astTicks = (AstTicks) plotConfig.getControlsModel( AstTicks.class );

        //  The AstAxes object is used for testing if logarithmic drawing is
        //  enabled.
        astAxes = (AstAxes) plotConfig.getControlsModel( AstAxes.class );
        
        // add some extra behaviors
        addBehaviors();
    }

    /**
     * Initialise spectra information.
     *
     * @param spectra Description of the Parameter
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    protected void initSpec( SpecDataComp spectra )
        throws SplatException
    {
        //  Autoscrolls when in a scrollable component.
        setAutoscrolls( true );

        //  Add a DivaPlotGraphicsPane to use for displaying
        //  interactive graphics elements.
        graphicsPane =
            new DivaPlotGraphicsPane( DrawActions.getTypedDecorator() );
        setCanvasPane( graphicsPane );
        getCanvasPane().setAntialiasing( false );

        //  Overlay layer needs stronger colour.
        graphicsPane.getOverlayLayer().setPaint( Color.darkGray );

        //  Retain reference to spectra data and properties.
        this.spectra = spectra;

        //  Initialize preferred initial size.
        setPreferredSize( new Dimension( 700, 350 ) );

        //  Create Figure that contains the spectra.
        GrfFigure grfFigure = new GrfFigure( this );
        graphicsPane.addFigure( grfFigure );

        //  Create Figure for the legend.
        setShowingLegendFigure( showLegendFigure );

        //  Create the required DefaultGrf object to draw AST graphics.
        mainGrf = new DefaultGrf( this );

        //  First time initialisation of spectra properties.
        updateProps( true );

        //  Add any keyboard interactions that we want.
        addKeyBoardActions();

        // Listen for mouse events so we can control the keyboard
        // focus.
        addMouseListener( this );
    }

    /**
     *  Whether to show the legend figure or not.
     */
    public void setShowingLegendFigure( boolean showLegendFigure )
    {
        this.showLegendFigure = showLegendFigure;
        if ( showLegendFigure ) {
            if ( legendFigure == null ) {
                legendFigure = new SpecLegendFigure( this );
            }
            graphicsPane.addFigure( legendFigure );
            legendFigure.update();
        }
        else {
            if ( legendFigure != null ) {
                graphicsPane.removeFigure( legendFigure );
            }
        }
    }

    /**
     *  Whether we're showing the legend figure or not.
     */
    public boolean isShowingLegendFigure()
    {
        return showLegendFigure;
    }

    /**
     * Get a reference to the Grf instance.
     */
    public Grf getGrf()
    {
        return mainGrf;
    }

    /**
     * Get a reference to the SpecDataComp that we're using.
     *
     * @return The specDataComp value
     */
    public SpecDataComp getSpecDataComp()
    {
        return spectra;
    }

    /**
     * Change the SpecDataComp that we should use.
     *
     * @param spectra The new specDataComp value
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    public void setSpecDataComp( SpecDataComp spectra )
        throws SplatException
    {
        this.spectra = spectra;
        updateProps( true );
    }

    /**
     * Set whether we are drawing the grid in the visible region or not.
     * This does not cause a change until the next redraw.
     */
    public void setVisibleOnly( boolean visibleOnly )
    {
        this.visibleOnly = visibleOnly;
    }

    /**
     * Get whether we are only drawing the grid in the visible region.
     */
    public boolean isVisibleOnly()
    {
        return visibleOnly;
    }

    /**
     * Set whether or not the MouseMotionTracker reports the current Plot
     * position, or the nearest position of the current spectrum.
     */
    public void setTrackFreely( boolean trackFreely )
    {
        this.trackFreely = trackFreely;
    }

    /**
     * Get whether or not the MouseMotionTracker is reporting the current
     * graphics position, or the nearest position of the current spectrum.
     */
    public boolean isTrackFreely()
    {
        return trackFreely;
    }

    /**
     * Set whether to draw line identifiers for both DSB axes.
     */
    public void setDoubleDSBLineIds( boolean doubleDSBLineIds )
    {
        this.doubleDSBLineIds = doubleDSBLineIds;
    }

    /**
     * Get whether we are drawing line identifiers for both axes.
     */
    public boolean isDoubleDSBLineIds()
    {
        return doubleDSBLineIds;
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
        Action leftAction = new AbstractAction( "moveLeft" )
        {
            public void actionPerformed( ActionEvent e )
            {
                scrollVHair( -1 );
            }
        };
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT,
                                                   KeyEvent.SHIFT_MASK ),
                           leftAction );

        Action rightAction = new AbstractAction( "moveRight" )
        {
            public void actionPerformed( ActionEvent e )
            {
                scrollVHair( 1 );
            }
        };
        addKeyBoardAction( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT,
                                                   KeyEvent.SHIFT_MASK ),
                           rightAction );

        //  The space-bar (with control, alt and shift modifiers) makes the
        //  interactive readouts show the interpolated vertical hair
        //  coordinates (this can be true if the hair is moved by the arrow
        //  keys above).
        Action spaceAction = new AbstractAction( "showVHairCoords" )
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
     * Add an action to associate with a KeyStroke. Probably best not to
     * over-write existing associations.
     *
     * @param keyStroke the KeyStroke (e.g. shift-left would be obtained
     *      using: KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT,
     *      KeyEvent.SHIFT_MASK ).
     * @param action the Action (i.e. implementation of AbstractAction) to
     *      associate with the KeyStroke.
     */
    public void addKeyBoardAction( KeyStroke keyStroke, Action action )
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
        return plotConfig;
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

     * Get the ColourStore that can be used to control the background.
     *
     * @return The background ColourStore.
     */
    public ColourStore getBackgroundColourStore()
    {
        return backgroundColourStore;
    }

    /**
     * Convenience method for obtaining reference to the AstAxes instance.
     * This is the one used by the PlotConfiguration.
     *
     * @return The AstAxes instance.
     */
    public AstAxes getAstAxes()
    {
        return astAxes;
    }

    /**
     * Update the composite properties of the spectra and setup for display.
     *
     * @param init Description of the Parameter
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    public void updateProps( boolean init )
        throws SplatException
    {
        //  No spectra, so maybe nothing to do.
        if ( spectra.count() == 0 ) {
            return;
        }

        //  Get the spectra AST frameset, this describes the coordinates.
        astJ = spectra.getAst();

        //  Set the data limits for plotting. Throws a SplatException.
        setDataLimits();

        //  If stacking then we need to make room for the offsets.
        if ( usePlotStacker ) {
            yMin = yMin + stacker.getMinLimit();
            yMax = yMax + stacker.getMaxLimit();
        }

        //  Define the size of the box used to draw the spectra
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

        //  Set the values that define what a unit scale in either axes means.
        if ( init ) {
            setBaseScale();
        }

        //  Set the preferred size of the component. This is the current
        //  scales times the basescale.
        setScale( xScale, yScale );

        //  Establish the border for labelling.
        if ( init ) {
            setBorder( xSaved, ySaved );
        }
    }

    /**
     * Set the range of any plotted axes. These can be overridden by
     * values in the DataLimits object, or set to the minimum/maximum
     * of the data values.
     */
    public void setDataLimits()
        throws SplatException
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
            
            // apply extra behaviors
            for (PlotBehavior behavior : behaviors) {
            	behavior.setDataLimits(this);
            }
            
        }
    }

    /**
     * Set the plot limits to those of the data.
     */
    protected void setAutoLimits()
        throws SplatException
    {
        double[] range = spectra.getAutoRange();
        xMin = range[0];
        xMax = range[1];
        yMin = range[2];
        yMax = range[3];

        if ( xMin == SpecData.BAD || xMax == SpecData.BAD ||
             yMin == SpecData.BAD || yMax == SpecData.BAD ) {

            //  At least one set of ranges is bad.
            throw new SplatException( "Cannot determine automatic " +
                                      "limits for the spectrum: \"" +
                                      spectra.getShortName() + "\"" );
        }
    }

    /**
     * Set the base scale. This matches the plot size to the viewable area
     * (this is what scale equals 1 implies).
     */
    public void setBaseScale()
    {
        Dimension size = getPreferredSize();
        baseXScale = (float) size.width;
        baseYScale = (float) size.height;
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
     * Fit spectrum to the displayed width. Follow this with a
     * setScale( 1, x ), to update the display.
     */
    public void fitToWidth()
    {
        baseXScale = (float) getWidth();
        lastXScale = 0.0F;
    }

    /**
     * Fit spectrum to the displayed height. Follow this with a
     * setScale( x, 1 ), to update the display.
     */
    public void fitToHeight()
    {
        baseYScale = (float) getHeight();
        lastYScale = 0.0F;
    }

    /**
     * Reset the preferred size of the whole component to match the current
     * scaling configuration.
     */
    protected void resetPreferredSize()
    {
        //  Set the requested size of the plotting component. This needs to be
        //  honoured by the user of this class somehow (use center of a
        //  BorderLayout, probably with a JScrollPane, if expecting to resize
        //  after creation).
        setPreferredSize( new Dimension( (int)( baseXScale * xScale ),
                                         (int)( baseYScale * yScale ) ) );
    }

    /**
     * Update the plot to reflect any changes in the spectra being plotted
     * (when a new spectrum is added).
     *
     * @exception SplatException thrown if problems reading spectra.
     */
    public void update( boolean scaled )
        throws SplatException
    {
        updateProps( false );
        updateComponent( scaled );
    }

    /**
     * Update the plot to reflect any changes in the component size etc. Do
     * not perform complete reinitialisation.
     */
    protected void updateComponent( boolean scaled )
    {
        resetPreferredSize();
        if ( scaled ) {
            xyScaled = true; // Else leave at current value.
        }
        revalidate();
        repaint();
    }

    /**
     * Update the plot to reflect any changes in the spectra that have been
     * drawn, but do not change the component size.
     */
    public void staticUpdate()
        throws SplatException
    {
        float xs = xScale;
        float ys = yScale;
        updateProps( false );
        xScale = xs;
        yScale = ys;
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
            updateComponent( true );
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
     * Draw the line identifiers aligned to the other sideband when
     * spectrum is dualside.
     *
     * @param postfix some string to add to the labels so that these are
     *                clearly aligned to the current sideband, which shouldn't
     *                be the default sideband ("(USB)" or "(LSB)" as
     *                appropriate).
     */
    protected void drawDoubleDSBLineIdentifiers( String postfix )
        throws SplatException
    {
        //  Note no clipping limits apply for ids.
        if ( spectra.count() > 0 && doubleDSBLineIds ) {

            //  Regenerate the mappings to match the current sideband of the
            //  current spectrum.
            spectra.regenerate();

            //  Draw these in red. Usual default is blue.
            spectra.drawLineIdentifiers( mainGrf, mainPlot, null, baseBox,
                                         postfix, Color.red.getRGB() );
            spectra.regenerate();
        }
    }

    /**
     * Draw the spectra. This should use the current plotting attributes and
     * style.
     */
    protected void drawSpectra()
        throws SplatException
    {
        double[] limits = null;
        if ( graphicsEdges.isClipped() ) {
            limits = new double[4];

            //  Check for when we are just displaying the visible region.
            if ( visibleOnly ) {
                if ( dataLimits.isXFlipped() ) {
                    limits[0] = Math.max(visibleBaseBox[2], visibleBaseBox[0]);
                    limits[2] = Math.min(visibleBaseBox[2], visibleBaseBox[0]);
                }
                else {
                    limits[0] = Math.min(visibleBaseBox[2], visibleBaseBox[0]);
                    limits[2] = Math.max(visibleBaseBox[2], visibleBaseBox[0]);
                }
                if ( dataLimits.isYFlipped() ) {
                    limits[1] = Math.max(visibleBaseBox[3], visibleBaseBox[1]);
                    limits[3] = Math.min(visibleBaseBox[3], visibleBaseBox[1]);
                }
                else {
                    limits[1] = Math.min(visibleBaseBox[3], visibleBaseBox[1]);
                    limits[3] = Math.max(visibleBaseBox[3], visibleBaseBox[1]);
                }
            }
            else {
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
        if ( spectra.count() > 0 ) {
            if ( usePlotStacker ) {
                try {
                    stacker.updateOffsets();
                }
                catch (SplatException e) {
                    logger.info("Failed to stack spectra: " + e.getMessage());
                }
            }
            spectra.drawSpec( mainGrf, mainPlot, limits, baseBox );
        }
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
     * Get the PlotStacker instance in use.
     */
    public PlotStacker getPlotStacker()
    {
        return stacker;
    }

    /**
     * Set if the PlotStacker facility should be used.
     */
    public void setUsePlotStacker( boolean usePlotStacker )
    {
        this.usePlotStacker = usePlotStacker;
        spectra.setApplyYOffsets( usePlotStacker );
    }

    /**
     * Return if the PlotStacker facility is being used.
     */
    public boolean isUsePlotStacker()
    {
        return usePlotStacker;
    }

    /**
     *  Redraw all AST graphics, includes grid (axes) and spectra.
     *
     * @param g Graphics object
     */
    public boolean redrawAll( Graphics2D g )
    {
        //  No spectra or not realized then nothing to do yet.
        if ( spectra.count() == 0 || getPreferredSize().width == 0
             || ! isVisible() || astJ == null ) {
            return true;
        }

        //  If we fail in the next section, then we should be ready to redraw
        //  it all next time, so remember if this was requested.
        boolean wasScaled = xyScaled;
        boolean ok = true;

        //  When the scale of plot has changed or been set for the first time
        //  we need to draw everything, but when we're tracking the grid to
        //  just the visible area (visibleOnly mode) we also need to redraw
        //  the grid, regardless of whether a scale has occurred or not, but
        //  under that circumstance we do not draw the more expensive spectra,
        //  unless the graphics are clipped, in which case we have no choice.
        //  When clipped we avoid drawing the graphics overlay, unless the
        //  plot has been scaled (not doing so causes a re-draw runaway as the
        //  figure is permanently dirty).
        try {
            if ( xyScaled || visibleOnly ) {
                boolean scaleFigures = true;
                if ( visibleOnly && graphicsEdges.isClipped() ) {
                    if ( ! xyScaled ) {
                        scaleFigures = false;
                    }
                    xyScaled = true;
                }

                //  Keep reference to existing AstPlot so we know how graphics
                //  coordinates are already drawn and can rescale any overlay
                //  graphics to the correct size.
                Plot oldAstPlot = mainPlot;

                //  Get the AST FrameSet to use and check if this contains a
                //  DSBSpecFrame, we handle those differently, unless the
                //  sideband is set to "LO".
                FrameSet astref = astJ.getRef();
                boolean isDSB = astJ.isFirstAxisDSBSpecFrame();
                String sideband = null;
                if ( isDSB ) {
                    sideband = astref.getC( "SideBand" );
                    if ( "LO".equals( sideband ) ) {
                        isDSB = false;
                    }
                }

                //  Create a Plot for the graphics, this is matched to the
                //  component size or it's visible area and is how we get an
                //  apparent rescale of drawing. So that we get linear axis 1
                //  coordinates we must choose the current frame as the base
                //  frame (so that the mapping from graphics to physical is
                //  linear). We also add any AST plotting configuration
                //  options.
                int current = astref.getCurrent();
                int base = astref.getBase();
                astref.setBase( current );
                String options = isDSB ? plotConfig.getAst( true ) :
                                         plotConfig.getAst();
                createPlot( astref, graphicsEdges.getXLeft(),
                            graphicsEdges.getXRight(), graphicsEdges.getYTop(),
                            graphicsEdges.getYBottom(), options );
                astref.setBase( base );

                //  The Plot must use our Grf implementation.
                mainPlot.setGrf( mainGrf );

                if ( !visibleOnly ) {
                    //  When displaying a grid that covers the whole drawing
                    //  area then we decrease the gap between X major ticks so
                    //  we see more labels when zoomed. We do not do this if
                    //  the user has specific a gap.
                    double xGap = 0.0;
                    xGap = astTicks.getXGap();
                    if ( ( xGap == 0.0 || xGap == DefaultGrf.BAD ) &&
                         xScale > 2.0 )
                    {
                        xGap = mainPlot.getD( "gap(1)" );
                        xGap = xGap / Math.max( 1.0, xScale * 0.5 );
                        mainPlot.set( "gap(1)=" + xGap );
                    }
                }

                if ( xyScaled ) {
                    //  Clear all existing AST graphics
                    mainGrf.reset();
                }
                else {
                    //  Just clear grid overlay.
                    mainGrf.remove( "GRID" );
                }

                //  Draw the coordinate grid/axes.
                mainGrf.establishContext( "GRID" );
                mainPlot.grid();

                //  If DSBSpecFrame, want to also draw the other side band
                //  coordinates along the unused X axis (usually top).
                Plot dsbPlot = null;
                if ( isDSB ) {

                    //  Create a new mainPlot that uses the world coordinates
                    //  of the other band. Note we get the second pass Plot
                    //  configuration from the plotConfig (the grid drawn
                    //  already should have been drawn using the first pass
                    //  state).
                    Plot mainMainPlot = mainPlot;
                    setSideBandBaseBox( astref, sideband );
                    astref.setBase( current );
                    createPlot( astref, graphicsEdges.getXLeft(),
                                graphicsEdges.getXRight(),
                                graphicsEdges.getYTop(),
                                graphicsEdges.getYBottom(),
                                plotConfig.getAst( false ) );
                    dsbPlot = mainPlot;
                    astref.setBase( base );
                    unsetSideBandBaseBox();

                    //  Switch the sideband and draw the grid.
                    String currentSideBand = " (USB)";
                    if ( "USB".equals( sideband ) ) {
                        astref.setC( "SideBand", "LSB" );
                        currentSideBand = " (LSB)";
                    }
                    else {
                        astref.setC( "SideBand", "USB" );
                    }

                    //  This Plot shares the mainGrf with the real mainPlot.
                    dsbPlot.setGrf( mainGrf );
                    dsbPlot.grid();

                    //  Restore the original mainPlot and sideband and
                    //  continue.
                    mainPlot = mainMainPlot;
                    astref.setC( "SideBand", sideband );
                }

                //  Draw the spectra, if required.
                if ( xyScaled ) {
                    mainGrf.establishContext( "SPECTRA" );
                    drawSpectra();

                    //  Resize overlay graphics.
                    if ( scaleFigures && oldAstPlot != null ) {
                        redrawOverlay( oldAstPlot );
                    }

                    //  If any line identifiers are to be redrawn matched
                    //  against the other sideband, then do that now (this
                    //  avoids problems with overwrites of the labels by
                    //  spectra).
                    if ( isDSB ) {

                        //  Switch limits to sideband for clipping etc.
                        setSideBandBaseBox( astref, sideband );

                        //  Switch current spectral coordinates to other
                        //  sideband.
                        String currentSideBand = " (USB)";
                        if ( "USB".equals( sideband ) ) {
                            astref.setC( "SideBand", "LSB" );
                            currentSideBand = " (LSB)";
                        }
                        else {
                            astref.setC( "SideBand", "USB" );
                        }

                        //  Draw the line identifiers.
                        Plot mainMainPlot = mainPlot;
                        mainPlot = dsbPlot;
                        drawDoubleDSBLineIdentifiers( currentSideBand );

                        //  Restore limits.
                        unsetSideBandBaseBox();

                        //  Restore the sideband and plot.
                        mainPlot = mainMainPlot;
                        astref.setC( "SideBand", sideband );
                    }

                    //  Inform any listeners that the Plot has been scaled.
                    if ( xyScaled ) {
                        fireScaled();

                        //  Drawn at least once, so OK to track mouse events.
                        readyToTrack = true;
                    }
                }
                mainGrf.establishContext( mainGrf.DEFAULT );

                //  Only do full redraw once per scale change.
                xyScaled = false;
            }

            //  Use antialiasing for either the text, lines and text, or
            //  nothing.
            graphicsHints.applyRenderingHints( (Graphics2D) g );

            //  Repaint all graphics.
            mainPlot.paint( g );
        }
        catch (Exception e) {
            // Trap all Exceptions and continue so we can recover
            // when we try to repaint.
            logger.info( "Failed to draw spectra" );
            if ( e.getMessage() != null ) {
                logger.info( e.getMessage() );
            }
            else {
                e.printStackTrace();
            }
            if ( wasScaled && !xyScaled ) {
                xyScaled = true;
                ok = false;
            }
        }
        catch (Throwable t) {
            // Trap all errors too (like OutOfMemory).
            logger.info( "Failed to draw spectra" );
            logger.info( t.getMessage() );
            if ( wasScaled && !xyScaled ) {
                xyScaled = true;
                ok = false;
            }
        }
        return ok;
    }


    /** Variable to save existing baseBox values when switching side bands */
    private double[] sbandBaseBox = new double[4];
    private double[] sbandVisibleBaseBox = new double[4];

    /**
     * Transform the existing baseBox to the coordinates of the other band
     * when using a DSBSpecFrame.
     */
    private void setSideBandBaseBox( FrameSet astref, String sideband )
    {
        //  Save existing baseBoxes.
        double[] tmp = sbandBaseBox;
        sbandBaseBox = baseBox;
        baseBox = tmp;

        tmp = sbandVisibleBaseBox;
        sbandVisibleBaseBox = visibleBaseBox;
        visibleBaseBox = tmp;

        //  Extract the Frame containing the DSBSpecFrame and make two
        //  copies. Then switch a copy to other sideband, and to make sure we
        //  take sidebands into account when getting a mapping set both to use
        //  AlignSideBand when aligning.
        Frame f1 = (Frame) astref.getFrame( FrameSet.AST__CURRENT ).copy();
        Frame f2 = (Frame) f1.copy();
        if ( "USB".equals( sideband ) ) {
            f2.setC( "SideBand", "LSB" );
        }
        else {
            f2.setC( "SideBand", "USB" );
        }
        f2.setB( "AlignSideBand", true );
        f1.setB( "AlignSideBand", true );
        Mapping map = f1.convert( f2, "" );

        //  Transform existing baseBox values into new sideband.
        double[] xin = new double[2];
        double[] yin = new double[2];
        xin[0] = sbandBaseBox[0];
        xin[1] = sbandBaseBox[2];
        yin[0] = sbandBaseBox[1];
        yin[1] = sbandBaseBox[3];

        double[][] trn = map.tran2( 2, xin, yin, true );

        baseBox[0] = trn[0][0];
        baseBox[1] = sbandBaseBox[1];
        baseBox[2] = trn[0][1];
        baseBox[3] = sbandBaseBox[3];
    }

    /**
     * Restore saved baseBoxes from the last call to setSideBandBaseBox.
     */
    private void unsetSideBandBaseBox()
    {
        double[] tmp = baseBox;
        baseBox = sbandBaseBox;
        sbandBaseBox = tmp;

        tmp = visibleBaseBox;
        visibleBaseBox = sbandVisibleBaseBox;
        sbandVisibleBaseBox = tmp;
    }

    /**
     *  Create a Plot matched to this component.
     *
     *  @param astref  a FrameSet that describes the mapping from physical to
     *                 graphics coordinates.
     *  @param xleft   Fraction of component display surface to be
     *                 reserved on left for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param xright  Fraction of component display surface to be
     *                 reserved on right for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param ytop    Fraction of component display surface to be
     *                 reserved on top for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param ybottom Fraction of component display surface to be
     *                 reserved on bottom for axes labels etc (can be zero,
     *                 in which case use control of the Insets to provide
     *                 required space).
     *  @param options a string of AST options to use when creating plot.
     */
    protected void createPlot( FrameSet astref,
                               double xleft, double xright,
                               double ytop, double ybottom,
                               String options )
    {
        //  Do nothing if no AST frameset available.
        if ( astref == null ) {
            return;
        }

        //  Find out the size of the graphics component and how much of it is
        //  kept for the graphics border.
        Dimension size = getPreferredSize();
        Insets inset = getInsets();

        //  Rectangle that define the graphics extent.
        Rectangle graphrect = null;

        //  If visibleOnly is set then we only draw a grid in the visible part
        //  of the component. In this case the component will usually be a
        //  JViewport of a JScrollPane, but that doesn't have to be true.
        if ( visibleOnly ) {

            //  Get the visible region.
            Rectangle visrect = getVisibleRect();

            //  Work out the reserve, based on this size, not the full one.
            int lgap = inset.left + (int)( visrect.width * xleft );
            int rgap = inset.right + (int)( visrect.width * xright );
            int bgap = inset.bottom + (int)( visrect.height * ybottom );
            int tgap = inset.top + (int)( visrect.height * ytop );

            //  Create a full sized mainPlot which uses our gaps. This is used
            //  to transform the graphics visible area into world coordinates.
            graphrect = new Rectangle( lgap, tgap,
                                       size.width - rgap - lgap,
                                       size.height - bgap - tgap );
            mainPlot = new Plot( astref, graphrect, baseBox );

            //  Apply options as these may include log-scaling which effects
            //  the range.
            if ( options != null ) {
                mainPlot.set( options );
            }

            //  Now determine equivalent rectangle for the visible region.
            graphrect = new Rectangle( visrect.x + lgap,
                                       visrect.y + tgap,
                                       visrect.width - rgap - lgap,
                                       visrect.height - bgap - tgap );

            //  Transform visual area into world coordinates to get minimum
            //  and maximums.
            double[] pos = new double[4];
            pos[0] = graphrect.x;
            pos[1] = graphrect.y + graphrect.height;
            pos[2] = graphrect.x + graphrect.width;
            pos[3] = graphrect.y;
            double tmp[][] = transform( pos, true );
            if ( tmp != null ) {
                visibleBaseBox[0] = tmp[0][0]; // xMin
                visibleBaseBox[1] = tmp[1][0]; // yMin
                visibleBaseBox[2] = tmp[0][1]; // xMax
                visibleBaseBox[3] = tmp[1][1]; // yMax
            }

            //  Now create the astPlot that only covers the visible part.
            mainPlot = new Plot( astref, graphrect, visibleBaseBox );
        }
        else {
            //  Graphics fills all of component except for reserve.
            int lgap = inset.left + (int)( size.width * xleft );
            int rgap = inset.right + (int)( size.width * xright );
            int bgap = inset.bottom + (int)( size.height * ybottom );
            int tgap = inset.top + (int)( size.height * ytop );
            graphrect = new Rectangle( lgap, tgap,
                                       size.width - rgap - lgap,
                                       size.height - bgap - tgap );
            mainPlot = new Plot( astref, graphrect, baseBox );
        }

        //  Set any AST options for the Plot.
        if ( options != null ) {
            mainPlot.set( options );
        }

        //  Keep the graphics limits these are used for drawing overlay
        //  figures that should respect the bounds of the grid.
        graphbox[0] = (float) ( graphrect.x );
        graphbox[1] = (float) ( graphrect.y + graphrect.height );
        graphbox[2] = (float) ( graphrect.x + graphrect.width );
        graphbox[3] = (float) ( graphrect.y );
    }
    private double count = 0;

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
        graphicsPane.astTransform( oldAstPlot, mainPlot );

        //  Force a reset of the vertical hair, if vertical scale is changed.
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
                barShape = new Line2D.Float( (float) x, graphbox[1],
                                             (float) x, graphbox[3] );
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
     * Display the spectral coordinate and interpolated value at the position
     * of the vertical hair. Requires that a MouseMotionTracker has been
     * established by the trackMouseMotion method.
     *
     * @see #trackMouseMotion
     */
    protected void showVHairCoords()
    {
        if ( spectra.count() == 0 || ! showVHair ) {
            return;
        }
        String[] xypos = spectra.formatInterpolatedLookup
            ( (int) getVHairPosition(), mainPlot );
        tracker.updateCoords( xypos[0], xypos[1] );
    }

    /**
     * Transform a series of positions between the graphics and current
     * coordinate systems (of the Plot).
     *
     * @param positions position in x,y pairs
     * @param forward whether to use forward transformation
     * @return transformed positions, x[0] is [0][0], y[0] is [1][0] etc.
     */
    public double[][] transform( double[] positions, boolean forward )
    {
        if ( mainPlot != null ) {
            return ASTJ.astTran2( mainPlot, positions, forward );
        }
        return null;
    }

    /**
     * Access the limits of the graphics coordinates used to draw the Plot.
     *
     * @return coordinate limits (graphbox of Plot)
     */
    public float[] getGraphicsLimits()
    {
        return graphbox;
    }

    /**
     * Access the limits of the physical coordinates used to draw the Plot.
     *
     * @return physical limits (basebox of Plot)
     */
    public double[] getPhysicalLimits()
    {
        return baseBox;
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
        addMouseMotionListener( new MouseMotionListener()
            {
                public void mouseMoved( MouseEvent e )
                {
                    if ( spectra.count() == 0 || ! readyToTrack ||
                         mainPlot == null ) {
                        return;
                    }
                    String[] xypos = null;
                    if ( trackFreely ) {
                        xypos = formatPos( e.getX(), e.getY() );
                    }
                    else {
                        xypos = spectra.formatLookup( e.getX(), mainPlot );
                    }
                    tracker.updateCoords( xypos[0], xypos[1] );
                    setVHairPosition( e.getX() );
                }

                public void mouseDragged( MouseEvent e )
                {
                    if ( spectra.count() == 0 ) {
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
        if ( spectra.count() == 0 ) {
            return 0.0;
        }
        return spectra.unFormat( axis, mainPlot, value );
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
        if ( spectra.count() == 0 ) {
            return null;
        }
        return spectra.format( axis, mainPlot, value );
    }

    /**
     * Convert graphics coordinates into formatted values for the current
     * Plot axes.
     *
     * @param x the axis 1 graphics coordinate.
     * @param y the axis 2 graphics coordinate.
     * @return the formatted values, null when none can be determined.
     */
    public String[] formatPos( double x, double y )
    {
        if ( mainPlot == null ) {
            return null;
        }

        //  Transform graphics to physical coordinates.
        double[] gtmp = new double[2];
        gtmp[0] = x;
        gtmp[1] = y;
        double[][] ptmp = transform( gtmp, true );

        //  Format.
        String result[] = new String[2];
        result[0] = mainPlot.format( 1, ptmp[0][0] );
        result[1] = mainPlot.format( 2, ptmp[1][0] );
        return result;
    }

    /**
     * Return a label that can be used to identity the values of an
     * axis. Note the AST description will be used if the Plot hasn't
     * been drawn yet (these are generally the same).
     *
     * @param axis the axis
     * @return The axis label
     */
    public String getLabel( int axis )
    {
        if ( spectra.count() == 0 ) {
            return "";
        }

        if ( mainPlot != null ) {
            return mainPlot.getC( "label(" + axis + ")" );
        }

        //  Try WCS description.
        FrameSet astref = astJ.getRef();
        if ( astref != null ) {
            return astref.getC( "label(" + axis + ")" );
        }

        // Default value.
        return "Axis" + axis;
    }

    //  Make a postscript copy of the spectra. Implements Printable interface.
    //  If pf is null we just pass the Graphics object on.
    public int print( Graphics g, PageFormat pf, int pageIndex )
    {
        if ( pageIndex > 0 ) {
            return NO_SUCH_PAGE;
        }

        Color oldback = null;
        if ( pf != null ) {

            //  Set background to white to match paper.
            oldback = getBackground();
            setBackground( Color.white );

            //  Make graphics shift and scale up/down to fit the page.
            fitToPage( (Graphics2D) g, pf );
        }

        //  Print the spectra and AST graphics.
        print( (Graphics2D) g );

        if ( pf != null ) {
            //  Restore background colour.
            setBackground( oldback );
        }

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
        double pageWidth = pf.getImageableWidth();
        double pageHeight = pf.getImageableHeight();
        double xinset = pf.getImageableX();
        double yinset = pf.getImageableY();
        double compWidth;
        double compHeight;
        Rectangle visrect = null;
        if ( visibleOnly ) {
            //  Only print visible part.
            visrect = getVisibleRect();
            compWidth = (double) visrect.width;
            compHeight = (double) visrect.height;
        }
        else {
            compWidth = (double) getWidth();
            compHeight = (double) getHeight();
        }
        double xscale = pageWidth / compWidth;
        double yscale = pageHeight / compHeight;
        if ( xscale < yscale ) {
            yscale = xscale;
        }
        else {
            xscale = yscale;
        }

        if ( visibleOnly ) {
            xinset = xinset - ( visrect.x * xscale );
            yinset = yinset - ( visrect.y * yscale );
        }

        //  Shift and scale.
        g2.translate( xinset, yinset );
        g2.scale( xscale, yscale );
    }

    /**
     * Return reference to the FrameSet used to transform from graphics to
     * world coordinates. This is really a direct reference to the Plot, so do
     * not modify it (any changes would be lost on the next resize).
     *
     * @return The Plot FrameSet
     */
    public FrameSet getMapping()
    {
        return mainPlot;
    }

    /**
     * Set whether the Plot is showing the vertical hair.
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
    // PlotClicked event interface
    //
    protected EventListenerList plotClickedListeners = new EventListenerList();

    /**
     * Registers a listener for to be informed when the Plot recieves a click..
     *
     * @param l the PlotClickedListener
     */
    public void addPlotClickedListener( PlotClickedListener l )
    {
        plotClickedListeners.add( PlotClickedListener.class, l );
    }

    /**
     * Remove a listener for Plot clicks.
     *
     * @param l the PlotClickedListener
     */
    public void removePlotClickedListener( PlotClickedListener l )
    {
        plotClickedListeners.remove( PlotClickedListener.class, l );
    }

    /**
     * Send an event to all PlotClickedListeners when the plot recieves
     * a mouse click.
     */
    protected void fireClicked( MouseEvent e )
    {
        Object[] list = plotClickedListeners.getListenerList();
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == PlotClickedListener.class ) {
                ( (PlotClickedListener) list[i + 1] ).plotClicked( e );
            }
        }
    }

    //
    //  Implement the MouseListener interface. The purpose of this is to
    //  make sure that we receive the keyboard focus when anyone
    //  clicks on this component (TODO: understand why this is necessary)
    //  and to send a PlotClickedEvent to all PlotClickedListeners.
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
        fireClicked( e );
    }
    public void mouseReleased( MouseEvent e )
    {
        //  Do nothing.
    }

    //
    // Draw interface. Also requires addMouseListener and
    // addMouseMotionListener (part of JComponent).
    //

    //  Return our version of DrawGraphicsPane (DivaPlotGraphicsPane).
    public DrawGraphicsPane getGraphicsPane()
    {
        return graphicsPane;
    }

    //  Return a reference to this.
    public Component getComponent()
    {
        return this;
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
            drawActions =
                new DrawActions( this, null,
                                 PlotInterpolatorFactory.getInstance() );
        }
        return drawActions;
    }

    // Implementation of AstPlotSource interface.
    public Plot getPlot()
    {
        return mainPlot;
    }
    
    protected void addBehaviors() {
    	behaviors.add(new MagnitudeAxisInvertingBehavior());
    }
}
