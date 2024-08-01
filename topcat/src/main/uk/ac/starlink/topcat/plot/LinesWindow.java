package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.DataBounds;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.LinesPlot;
import uk.ac.starlink.ttools.plot.LinesPlotState;
import uk.ac.starlink.ttools.plot.MarkStyles;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotListener;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PlotSurface;
import uk.ac.starlink.ttools.plot.PointIterator;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.StyleSet;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.WrapUtils;

/**
 * GraphicsWindow which draws a stack of line graphs.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesWindow extends GraphicsWindow implements TopcatListener {

    private final JComponent plotPanel_;
    private final ToggleButtonModel antialiasModel_;
    private final ToggleButtonModel vlineModel_;
    private final ToggleButtonModel zeroLineModel_;
    private final Map<PointSelector,Range> yViewRangeMap_;

    private Range[] yDataRanges_;
    private StyleSet styles_;
    private Annotator annotator_;

    private static final Color[] COLORS;
    static {
        COLORS = new Color[ Styles.COLORS.length + 1 ];
        COLORS[ 0 ] = Color.BLACK;
        System.arraycopy( Styles.COLORS, 0, COLORS, 1, Styles.COLORS.length );
    }
    private static final ErrorRenderer DEFAULT_ERROR_RENDERER =
        ErrorRenderer.EXAMPLE;
    private static final StyleSet[] STYLE_SETS =
            fixDefaultErrorRenderers( DEFAULT_ERROR_RENDERER, new StyleSet[] {
        MarkStyles.lines( "Black/Coloured Lines", COLORS ),
        MarkStyles.lines( "Lines" ),
        MarkStyles.dashedLines( "Dashed Lines" ),
        MarkStyles.points( "Black/Coloured Points", COLORS ),
        MarkStyles.points( "Points" ),
        MarkStyles.spots( "Dots", 1 ),
        MarkStyles.spots( "Spots", 2 ),
        MarkStyles.openShapes( "Small Coloured Outlines", 3, null ),
        MarkStyles.openShapes( "Medium Coloured Outlines", 4, null ),
        MarkStyles.openShapes( "Small Black Outlines", 3, Color.black ),
        MarkStyles.openShapes( "Medium Black Outlines", 4, Color.black ),
    } );
    private static final StyleSet LINES = STYLE_SETS[ 0 ];
    private static final StyleSet POINTS = STYLE_SETS[ 3 ];
    static {
        assert LINES.getName().equals( "Black/Coloured Lines" );
        assert POINTS.getName().equals( "Black/Coloured Points" );
    }
    private static final ErrorRenderer[] ERROR_RENDERERS =
        ErrorRenderer.getOptions2d();
    private static final String[] AXIS_NAMES = new String[] { "X", "Y", };

    /**
     * Constructor.
     *
     * @param   parent  parent component
     */
    @SuppressWarnings("this-escape")
    public LinesWindow( Component parent ) {
        super( "Stacked Line Plot (old)", new LinesPlot(), AXIS_NAMES, 0, true,
               createErrorModeModels( AXIS_NAMES ), parent );

        /* Set some initial values. */
        yViewRangeMap_ = new HashMap<PointSelector,Range>();
        annotator_ = new Annotator();

        /* Construct a plot component to hold the plotted graphs. */
        final LinesPlot plot = (LinesPlot) getPlot();
        plot.setPreferredSize( new Dimension( 400, 400 ) );
        plot.setBorder( BorderFactory.createEmptyBorder( 10, 0, 0, 10 ) );

        /* Overlay components of display. */
        plotPanel_ = new JPanel();
        plotPanel_.setOpaque( false );
        plotPanel_.setLayout( new OverlayLayout( plotPanel_ ) );
        plotPanel_.add( annotator_ );
        plotPanel_.add( plot );

        /* Zooming. */
        final Zoomer zoomer = new Zoomer();
        zoomer.setCursorComponent( plot );
        plot.addPlotListener( new PlotListener() {
            public void plotChanged( PlotEvent evt ) {
                zoomer.setRegions( Arrays
                                  .asList( createZoomRegions( plot ) ) );
            }
        } );
        plot.addMouseListener( zoomer );
        plot.addMouseMotionListener( zoomer );

        /* The axis window has to be kept up to date with the point selectors,
         * since the number of axis editor components it contains is the
         * same as the number of point selectors. */
        getPointSelectors().addActionListener( new AxisWindowUpdater() );

        /* Add a new grid option - display only a single grid line at y=0
         * (requested by Silvia Dalla).  Ensure that this is exclusive with
         * the normal grid toggle. */
        final ToggleButtonModel gridModel = getGridModel();
        gridModel.setSelected( false );
        gridModel.setText( "Full Grid" );
        gridModel.setDescription( "Draw all X and Y grid lines" );
        zeroLineModel_ = new ToggleButtonModel( "y=0 Grid Lines",
                                                ResourceIcon.Y0_LINE,
                                                "Draw grid line only at y=0" );
        zeroLineModel_.addActionListener( getReplotListener() );
        ActionListener gridListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( zeroLineModel_.isSelected() && gridModel.isSelected() ) {
                    if ( evt.getSource() == zeroLineModel_ ) {
                        gridModel.setSelected( false );
                    }
                    else {
                        assert evt.getSource() == gridModel;
                        zeroLineModel_.setSelected( false );
                    }
                }
            }
        };
        gridModel.addActionListener( gridListener );
        zeroLineModel_.addActionListener( gridListener );

        /* Add a status line reporting on cursor position. */
        JComponent posLabel = new LinesPositionLabel( plot );
        posLabel.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                posLabel.getMaximumSize()
                                                        .height ) );
        getStatusBox().add( posLabel );
        getStatusBox().add( Box.createHorizontalGlue() );

        /* Arrange for clicking on a given point to cause row activation. */
        plot.addMouseListener( new PointClickListener() );
        getPointSelectors().addTopcatListener( this );

        /* Rescaling actions. */
        Action rescaleActionXY =
            new RescaleAction( "Rescale", ResourceIcon.RESIZE,
                               "Rescale all plots to fit all data",
                               true, true );
        Action rescaleActionX =
            new RescaleAction( "Rescale", ResourceIcon.RESIZE_X,
                               "Rescale the X axis to fit all data",
                               true, false );
        Action rescaleActionY =
            new RescaleAction( "Rescale", ResourceIcon.RESIZE_Y,
                               "Rescale Y axis on all plots to fit all data",
                               false, true );

        /* Antialias action. */
        antialiasModel_ = new ToggleButtonModel( "Antialias",
                                                 ResourceIcon.ANTIALIAS,
                                                 "Select whether lines are " +
                                                 "drawn with antialiasing" );
        antialiasModel_.setSelected( true );
        antialiasModel_.addActionListener( getReplotListener() );

        /* Point selection action. */
        Action fromXRangeAction = new BasicAction( "New subset from X range",
                                                   ResourceIcon.XRANGE_SUBSET,
                                                   "Define a new subset " +
                                                   "for each plotted table " +
                                                   "containing only points " +
                                                   "in the visible X range" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubsets( plot.getPointsInRange() );
            }
        };

        /* Vertical crosshair toggle action. */
        vlineModel_ = new ToggleButtonModel( "Show vertical crosshair",
                                             ResourceIcon.Y_CURSOR,
                                             "Display a vertical line " +
                                             "which follows the mouse" );
        new CrosshairListener( plot, vlineModel_ );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
        plotMenu.add( getLegendModel().createMenuItem() );
        plotMenu.add( zeroLineModel_.createMenuItem() );
        plotMenu.add( getReplotAction() );
        plotMenu.add( vlineModel_.createMenuItem() );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for rendering operations. */
        JMenu renderMenu = new JMenu( "Rendering" );
        renderMenu.setMnemonic( KeyEvent.VK_R );
        renderMenu.add( antialiasModel_.createMenuItem() );
        getJMenuBar().add( renderMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        subsetMenu.add( fromXRangeAction );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for error modes. */
        getJMenuBar().add( createErrorModeMenu() );

        /* Construct a menu for line style set selection. */
        JMenu styleMenu = new JMenu( "Line Style" );
        styleMenu.setMnemonic( KeyEvent.VK_L );
        for ( int i = 0; i < STYLE_SETS.length; i++ ) {
            final StyleSet styleSet = STYLE_SETS[ i ];
            String name = styleSet.getName();
            Icon icon = MarkStyles.getIcon( styleSet );
            Action stylesAct = new BasicAction( name, icon,
                                                "Set line plotting style to " 
                                                + name ) {
                public void actionPerformed( ActionEvent evt ) {
                    setStyles( styleSet );
                    replot();
                }
            };
            styleMenu.add( stylesAct );
        }
        getJMenuBar().add( styleMenu );

        /* Add a new menu for error bar style selection. */
        getJMenuBar().add( createErrorRendererMenu( ERROR_RENDERERS ) );

        /* Populate toolbar. */
        getPointSelectorToolBar().addSeparator();
        getPointSelectorToolBar().add( getErrorModeModels()[ 0 ]
                                      .createOnOffToolbarButton() );
        getPointSelectorToolBar().add( getErrorModeModels()[ 1 ]
                                      .createOnOffToolbarButton() );
        getToolBar().add( rescaleActionXY );
        getToolBar().add( rescaleActionX );
        getToolBar().add( rescaleActionY );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getLegendModel().createToolbarButton() );
        getToolBar().add( zeroLineModel_.createToolbarButton() );
        getToolBar().add( vlineModel_.createToolbarButton() );
        getToolBar().add( antialiasModel_.createToolbarButton() );
        getToolBar().add( fromXRangeAction );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "LinesWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlotPanel() {
        return plotPanel_;
    }

    protected void doReplot( PlotState state ) {
        annotator_.setState( state );
        super.doReplot( state );
    }

    protected PlotState createPlotState() {
        return new LinesPlotState( getPointSelectors().getSelectorCount() );
    }

    public PlotState getPlotState() {
        LinesPlotState state = (LinesPlotState) super.getPlotState();
        if ( ! state.getValid() ) {
            return state;
        }

        /* Acquire some values. */
        PointSelectorSet pointSelectors = getPointSelectors();
        int nsel = pointSelectors.getSelectorCount();
        AxisEditor[] axEds = getAxisWindow().getEditors();

        /* Configure Y axis and range information for each plot. */
        ValueInfo[] yAxes = new ValueInfo[ nsel ];
        ValueConverter[] yConverters = new ValueConverter[ nsel ];
        String[] yAxisLabels = new String[ nsel ];
        double[][] yBounds = new double[ nsel ][];
        boolean[] yLogFlags = new boolean[ nsel ];
        boolean[] yFlipFlags = new boolean[ nsel ];
        List<PointSelector> pselList = new ArrayList<PointSelector>( nsel );
        for ( int isel = 0; isel < nsel; isel++ ) {
            PointSelector psel = pointSelectors.getSelector( isel );
            pselList.add( psel );
            LinesAxesSelector axsel =
                findLinesAxesSelector( psel.getAxesSelector() );
            Range yRange;
            if ( psel.isReady() ) {
                AxisEditor yAxEd = axEds[ 1 + isel ];
                ColumnInfo cinfo =
                    psel.getAxesSelector().getData().getColumnInfo( 1 );
                yAxes[ isel ] = cinfo;
                yConverters[ isel ] =
                    cinfo.getAuxDatumValue( TopcatUtils.NUMERIC_CONVERTER_INFO,
                                            ValueConverter.class );
                yAxisLabels[ isel ] = yAxEd.getLabel();
                yLogFlags[ isel ] = axsel.getYLogModel().isSelected();
                yFlipFlags[ isel ] = axsel.getYFlipModel().isSelected();
                yRange = new Range( yDataRanges_[ isel ] );
                if ( yViewRangeMap_.containsKey( psel ) ) {
                    yRange.limit( yViewRangeMap_.get( psel ) );
                }
            }
            else {
                yRange = new Range();
            }
            yBounds[ isel ] = yRange.getFiniteBounds( yLogFlags[ isel ] );
        }
        state.setYAxes( yAxes );
        state.setYConverters( yConverters );
        state.setYAxisLabels( yAxisLabels );
        state.setYRanges( yBounds );
        state.setYLogFlags( yLogFlags );
        state.setYFlipFlags( yFlipFlags );

        /* Configure information for each subset about which graph it will
         * be plotted on. */
        SetId[] setIds = ((PointSelection) state.getPlotData()).getSetIds();
        int nset = setIds.length;
        int[] graphIndices = new int[ nset ];
        for ( int iset = 0; iset < nset; iset++ ) {
            graphIndices[ iset ] =
                pselList.indexOf( setIds[ iset ].getPointSelector() );
        }
        state.setGraphIndices( graphIndices );

        /* Antialiasing. */
        state.setAntialias( antialiasModel_.isSelected() );

        /* Y=0 lines. */
        state.setYZeroFlag( zeroLineModel_.isSelected() );

        /* Return state. */
        return state;
    }

    protected PointSelector createPointSelector() {

        /* Create the new point selector. */
        ToggleButtonModel logModel = 
            new ToggleButtonModel( getLogModels()[ 1 ].getText(),
                                   getLogModels()[ 1 ].getIcon(),
                                   getLogModels()[ 1 ].getDescription() );
        ToggleButtonModel flipModel =
            new ToggleButtonModel( getFlipModels()[ 1 ].getText(),
                                   getFlipModels()[ 1 ].getIcon(),
                                   getFlipModels()[ 1 ].getDescription() );
        logModel.addActionListener( getReplotListener() );
        flipModel.addActionListener( getReplotListener() );
        AxesSelector axsel =
            new LinesAxesSelector( logModel, flipModel, getErrorModeModels() );
        axsel = addExtraAxes( axsel );
        PointSelector newSelector = new PointSelector( axsel, getStyles() );

        /* Work out if there is a default X axis we should initialise the
         * new selector with.  We'll do this if all the existing valid
         * point selectors are using the same X axis. */
        PointSelectorSet pointSelectors = getPointSelectors();
        PointSelector mainSel = pointSelectors.getMainSelector();
        TopcatModel mainTable = null;
        Object mainXAxis = null;
        if ( mainSel != null && mainSel.isReady() ) {
            mainTable = mainSel.getTable();
            mainXAxis = findLinesAxesSelector( mainSel.getAxesSelector() )
                       .getColumnSelector( 0 ).getSelectedItem();
            for ( int i = 0; 
                  mainTable != null && mainXAxis != null
                                    && i < pointSelectors.getSelectorCount();
                  i++ ) {
                PointSelector psel = pointSelectors.getSelector( i );
                if ( psel.isReady() ) {
                    TopcatModel table = psel.getTable();
                    Object xAxis =
                        findLinesAxesSelector( psel.getAxesSelector() )
                       .getColumnSelector( 0 ).getSelectedItem();
                    if ( ( table != null && ! table.equals( mainTable ) ) ||
                         ( xAxis != null && ! xAxis.equals( mainXAxis ) ) ) {
                        mainTable = null;
                        mainXAxis = null;
                        break;
                    }
                }
            }
        }

        /* Initialise with the default X axis and table if there is one. */
        if ( mainTable != null ) {
            newSelector.setTable( mainTable, false );
            if ( mainXAxis != null ) {
                findLinesAxesSelector( axsel ).getColumnSelector( 0 )
                                              .setSelectedItem( mainXAxis );
            }
        }

        /* Return the initialised selector. */
        return newSelector;
    }

    protected StyleEditor createStyleEditor() {
        return new LinesStyleEditor( ERROR_RENDERERS, DEFAULT_ERROR_RENDERER,
                                     getErrorModeModels() );
    }

    public void setStyles( StyleSet styles ) {
        styles_ = styles;
        super.setStyles( styles );
    }

    public MutableStyleSet getStyles() {
        if ( styles_ == null ) {
            styles_ = getDefaultStyles( 0 );
        }

        /* Note that, unlike the default GraphicsWindow behaviour, we 
         * return a styleset which is independent each time here rather
         * than one which keeps track of which styles have been dispensed.
         * That's because in a lines plot, it makes sense to use the
         * same style for different data sets (as they're not plotted
         * over each other). */
        return new PoolStyleSet( styles_, new BitSet() );
    }

    public StyleSet getDefaultStyles( int npoint ) {
        return npoint < 20000 ? LINES : POINTS;
    }

    /**
     * Returns a 1-element array giving only the X axis range.
     */
    public Range[] calculateRanges( PlotData data, PlotState state ) {
        Range xRange = getViewRanges()[ 0 ];
        double[] xLimits = xRange.isClear() ? null
                                            : xRange.getFiniteBounds( false );
        DataBounds xyBounds = ((LinesPlot) getPlot())
                             .calculateBounds( data, state, xLimits );
        Range[] xyRanges = xyBounds.getRanges();
        yDataRanges_ = new Range[ xyRanges.length - 1 ];
        System.arraycopy( xyRanges, 1, yDataRanges_, 0, xyRanges.length - 1 );
        return new Range[] { xyRanges[ 0 ] };
    }

    protected boolean isLegendInteresting( PlotState state ) {

        /* Determine whether any of the plotted graphs contain more than one
         * dataset. */
        boolean hasMultiples = false;
        SetId[] setIds = ((PointSelection) state.getPlotData()).getSetIds();
        Set<PointSelector> pselSet = new HashSet<PointSelector>();
        for ( int i = 0; ! hasMultiples && i < setIds.length; i++ ) {
            PointSelector psel = setIds[ i ].getPointSelector();
            if ( pselSet.contains( psel ) ) {
                hasMultiples = true;
            }
            else {
                pselSet.add( psel );
            }
        }
        return hasMultiples;
    }

    /**
     * Convenience method to return the LinesAxesSelector associated 
     * with a given AxesSelector.
     * This may be the supplied <code>axsel</code> itself, or some object
     * wrapped by it.
     *
     * @param   axsel   axes selector associated with this window
     * @return  associated LinesAxesSelector
     */
    private static LinesAxesSelector
                   findLinesAxesSelector( AxesSelector axsel ) {
        return (LinesAxesSelector)
               WrapUtils.getWrapped( axsel, LinesAxesSelector.class );
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                TopcatModel tcModel = evt.getModel();
                PointSelection psel = 
                    (PointSelection) getPlot().getState().getPlotData();
                long lrow = ((Long) datum).longValue();
                long[] lps = psel.getPointsForRow( tcModel, lrow );
                int[] ips = new int[ lps.length ];
                for ( int i = 0; i < lps.length; i++ ) {
                    ips[ i ] = Tables.checkedLongToInt( lps[ i ] );
                }
                annotator_.setActivePoints( ips );
                replot();
            }
        }
    }

    /**
     * Called when the plot geometry changes to ensure that the zoomer has
     * the right associated regions.
     *
     * @param   plot   lines plot for which zoom regions are required
     * @return  array of appropriate X and Y axis zoom region objects
     */
    private ZoomRegion[] createZoomRegions( LinesPlot plot ) {
        LinesPlotState state = (LinesPlotState) plot.getState();
        int ngraph = state.getGraphCount();
        PlotSurface[] surfaces = plot.getSurfaces();
        ZoomRegion[] regions = new ZoomRegion[ ngraph + 1 ];

        /* Add Y axis zoom regions for each graph. */
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            final int ig = igraph;
            final PlotSurface surface = surfaces[ igraph ];
            final boolean flip = state.getYFlipFlags()[ igraph ];
            Rectangle displayBox = surface.getClip().getBounds();
            final int xPos = displayBox.x;
            final int yPos = displayBox.y;
            final int yInc = displayBox.height;
            Rectangle yAxisBox =
                new Rectangle( 0, displayBox.y, displayBox.x, yInc );
            ZoomRegion yZoom = new AxisZoomRegion( false, yAxisBox,
                                                   displayBox ) {
                public void zoomed( double[][] bounds ) {
                    double v0 = bounds[ 0 ][ 0 ];
                    double v1 = bounds[ 0 ][ 1 ];
                    int y0 = (int) Math.round( yPos + v0 * yInc );
                    int y1 = (int) Math.round( yPos + v1 * yInc );
                    requestZoomY( ig,
                                  surface.graphicsToData( xPos, flip ? y0 : y1,
                                                          false )[ 1 ],
                                  surface.graphicsToData( xPos, flip ? y1 : y0,
                                                          false )[ 1 ] );
                }
            };
            regions[ igraph ] = yZoom;
        }

        /* Add an X axis zoom region applying to all the graphs. */
        final PlotSurface surface0 = surfaces[ ngraph - 1 ];
        Rectangle displayBox = plot.getPlotBounds();
        final int xPos = displayBox.x;
        final int xInc = displayBox.width;
        Rectangle xAxisBox =
            new Rectangle( xPos, displayBox.y + displayBox.height,
                           xInc, plot.getHeight() );
        ZoomRegion xZoom = new AxisZoomRegion( true, xAxisBox, displayBox ) {
            public void zoomed( double[][] bounds ) {
                double v0 = bounds[ 0 ][ 0 ];
                double v1 = bounds[ 0 ][ 1 ];
                int x0 = (int) Math.round( xPos + v0 * xInc );
                int x1 = (int) Math.round( xPos + v1 * xInc );
                requestZoomX( surface0.graphicsToData( x0, 0, false )[ 0 ],
                              surface0.graphicsToData( x1, 0, false )[ 0 ] );
            }
        };
        regions[ ngraph ] = xZoom;

        /* Return combined set of zoom regions. */
        return regions;
    }

    /**
     * Indicates that the user has asked to zoom to a particular region
     * in the X direction.
     *
     * @param   x0  lower bound of new view region in data coordinates
     * @param   x1  upper bound of new view region in data coordinates
     */
    private void requestZoomX( double x0, double x1 ) {
        getAxisWindow().getEditors()[ 0 ].clearBounds();
        getViewRanges()[ 0 ].setBounds( x0, x1 );
        replot();
    }

    /**
     * Indicates that the user has asked to zoom to a particular region
     * in the Y direction for one of the graphs.
     *
     * @param  igraph  index of graph to zoom on
     * @param  y0    lower bound of new view region in data coordinates
     * @param  y1    upper bound of new view region in data coordinates
     */
    private void requestZoomY( int igraph, double y0, double y1 ) {
        getAxisWindow().getEditors()[ 1 + igraph ].clearBounds();
        PointSelector psel = getPointSelectors().getSelector( igraph );
        if ( yViewRangeMap_.containsKey( psel ) ) {
            yViewRangeMap_.get( psel ).setBounds( y0, y1 );
        }
        replot();
    }

    /**
     * Listener which can keep the AxisWindow up to date with the current
     * state of the plot.  It needs to add and remove AxisEditor components
     * in tandem with the state of the PointSelectorSet component.
     */
    private class AxisWindowUpdater implements ActionListener {
        int nsel_;
        AxisEditor xAxEd_;
        Map<PointSelector,AxisEditor> yAxEdMap_;

        /**
         * Check that this object has run through initialisation sequence.
         */
        private void ensureInitialised() {
            if ( xAxEd_ == null ) {
                assert yAxEdMap_ == null;

                /* Set up the X axis editor. */
                PointSelector mainSel = getPointSelectors().getMainSelector();
                AxisEditor[] mainEds =
                    mainSel.getAxesSelector().createAxisEditors();
                xAxEd_ = mainEds[ 0 ];
                xAxEd_.addMaintainedRange( getViewRanges()[ 0 ] );
                xAxEd_.addActionListener( getReplotListener() );

                /* Set up the main Y axis editor (can't be removed). */
                AxisEditor yAxEd = mainEds[ 1 ];
                yAxEd.setTitle( "Y Axis (" +
                                PointSelectorSet.MAIN_TAB_NAME + ")" );
                yAxEd.addActionListener( getReplotListener() );
                Range yRange = new Range();
                yAxEd.addMaintainedRange( yRange );
                yViewRangeMap_.put( mainSel, yRange );

                /* Set up a map to keep track of which axis editor corresponds
                 * to which selector. */
                yAxEdMap_ = new HashMap<PointSelector,AxisEditor>();
                yAxEdMap_.put( mainSel, yAxEd );
            }
            assert xAxEd_ != null;
            assert yAxEdMap_ != null;
        }

        public void actionPerformed( ActionEvent evt ) {
            PointSelectorSet pointSelectors = getPointSelectors();
            int nsel = pointSelectors.getSelectorCount();

            /* Rebuild the axis window if the number of selectors has changed
             * since last time. */
            if ( nsel != nsel_ ) {
                nsel_ = nsel;
                updateAxisWindow();
            }
        }

        /**
         * Ensures that the axis window's state matches the current state
         * of the point selector set.
         */
        private void updateAxisWindow() {
            ensureInitialised();
            PointSelectorSet pointSelectors = getPointSelectors();
            int nsel = pointSelectors.getSelectorCount();

            /* Build a list of axis editors matching current state. */
            AxisEditor[] axEds = new AxisEditor[ 1 + nsel ];
            axEds[ 0 ] = xAxEd_;
            for ( int isel = 0; isel < nsel; isel++ ) {
                PointSelector psel = pointSelectors.getSelector( isel );
                if ( ! yAxEdMap_.containsKey( psel ) ) {
                    AxisEditor yAxEd =
                        psel.getAxesSelector().createAxisEditors()[ 1 ];
                    yAxEd.setTitle( "Y Axis (" + psel.getLabel() + ")" ); 
                    yAxEd.addActionListener( getReplotListener() );
                    yAxEdMap_.put( psel, yAxEd );
                    Range yRange = new Range();
                    yAxEd.addMaintainedRange( yRange );
                    yViewRangeMap_.put( psel, yRange );
                }
                axEds[ 1 + isel ] = yAxEdMap_.get( psel );
            }

            /* Install it in the axis window. */
            getAxisWindow().setEditors( axEds );
        }
    }

    /**
     * Handles mouse clicks to select points.
     */
    private class PointClickListener extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            LinesPlot plot = (LinesPlot) getPlot();
            if ( evt.getButton() == MouseEvent.BUTTON1 ) {
                PointIterator pit = plot.getPlottedPointIterator();
                int ip = pit.getClosestPoint( evt.getPoint(), 4 );
                if ( ip >= 0 ) {
                    PointSelection psel =
                        (PointSelection) plot.getState().getPlotData();
                    psel.getPointTable( ip )
                        .highlightRow( psel.getPointRow( ip ) );
                }
                else {
                    annotator_.setActivePoints( new int[ 0 ] );
                    replot();
                }
            }
        }
    }

    /**
     * Panel which can write annotations.
     */
    private class Annotator extends AnnotationPanel {
        private LinesPlotState state_;

        /**
         * Sets the plot state.
         *
         * @param  state plot state
         */
        public void setState( PlotState state ) {
            state_ = (LinesPlotState) state;
        }

        protected void paintComponent( Graphics g ) {
            int[] activePoints = getActivePoints();
            if ( activePoints.length == 0 || state_ == null ) {
                return;
            }

            /* Assemble a per-graph array of active point lists and 
             * corresponding point placers. */
            PointPlacer[] placers = ((LinesPlot) getPlot()).getPointPlacers();
            int ngraph = placers.length;
            BitSet activeMask = new BitSet();
            for ( int i = 0; i < activePoints.length; i++ ) {
                activeMask.set( activePoints[ i ] );
            }
            PlotData data = state_.getPlotData();
            int nset = data.getSetCount();
            int[] graphIndices = state_.getGraphIndices();
            IntList[] activeLists = new IntList[ ngraph ];
            for ( int ig = 0; ig < ngraph; ig++ ) {
                activeLists[ ig ] = new IntList();
            }
            PointSequence pseq = data.getPointSequence();
            for ( int ip = 0; pseq.next(); ip++ ) {
                if ( activeMask.get( ip ) ) {
                    for ( int is = 0; is < nset; is++ ) {
                        if ( pseq.isIncluded( is ) ) {
                            activeLists[ graphIndices[ is ] ].add( ip );
                        }
                    }
                }
            }

            /* Now for each constituent graph plot the active points associated
             * with it.  Do this by creating an AnnotationPanel which would
             * paint the right thing, and then rather than placing it just
             * invoke its paintComponent method directly. */
            for ( int ig = 0; ig < ngraph; ig++ ) {
                int[] ips = activeLists[ ig ].toIntArray();
                if ( ips.length > 0 ) {
                    AnnotationPanel ann = new AnnotationPanel();
                    ann.setPlotData( data );
                    ann.setPlacer( placers[ ig ] );
                    ann.setActivePoints( ips );
                    ann.paintComponent( g );
                }
            }
        }
    }

    /**
     * Mouse listener which moves a vertical crosshair around on the
     * plotting surface(s).
     */
    private static class CrosshairListener extends MouseInputAdapter
                                           implements ChangeListener {
        final LinesPlot linesPlot_;
        final ButtonModel switchModel_;
        final Rectangle vLine_;
        final Color lineColor_;
        Graphics g_;
        boolean visible_;
        boolean on_;

        /**
         * Constructs a new CrosshairListener and starts it listening to
         * a supplied plot component and switch model in the appropriate ways.
         * No other installation into other components is required.
         *
         * @param  linesPlot  plot component
         * @param  switchModel   button model which determines when this
         *         listener is working and when it's inactive
         */
        CrosshairListener( LinesPlot linesPlot, ButtonModel switchModel ) {
            linesPlot_ = linesPlot;
            switchModel_ = switchModel;
            vLine_ = new Rectangle();
            vLine_.width = 1;
            switchModel.addChangeListener( this );
            lineColor_ = new Color( ~ ( new Color( 0x008000 ).getRGB() ) );
        }

        public void mouseEntered( MouseEvent evt ) {
            if ( on_ ) {
                g_ = linesPlot_.getGraphics();
                g_.setXORMode( lineColor_ );
            }
            redraw( evt.getPoint() );
        }

        public void mouseExited( MouseEvent evt ) {
            redraw( null );
        }

        public void mouseMoved( MouseEvent evt ) {
            redraw( evt.getPoint() );
        }

        public void stateChanged( ChangeEvent evt ) {
            updateState();
        }

        public void updateState() {
            boolean on = switchModel_.isSelected();
            if ( on != on_ ) {
                on_ = on;
                if ( on_ ) {
                    g_ = linesPlot_.getGraphics();
                    g_.setXORMode( lineColor_ );
                    linesPlot_.addMouseListener( this );
                    linesPlot_.addMouseMotionListener( this );
                }
                else {
                    redraw( null );
                    g_.dispose();
                    linesPlot_.removeMouseListener( this );
                    linesPlot_.removeMouseListener( this );
                }
            }
        }

        /**
         * Possibly undraws the crosshair at its last position, 
         * and then possibly draws it at the supplied one.
         *
         * @param  p  position of the current crosshair, or null to undraw
         */
        private void redraw( Point p ) {
            if ( visible_ ) {
                g_.fillRect( vLine_.x, vLine_.y, vLine_.width, vLine_.height );
                visible_ = false;
            }
            if ( p != null && on_ ) {
                Rectangle zone = linesPlot_.getPlotBounds();
                if ( zone.contains( p ) ) {
                    vLine_.x = p.x;
                    vLine_.y = zone.y;
                    vLine_.height = zone.height;
                    g_.fillRect( vLine_.x, vLine_.y,
                                 vLine_.width, vLine_.height );
                    visible_ = true;
                }
            }
        }
    }

    /**
     * Action for performing rescaling actions.
     */
    public class RescaleAction extends BasicAction {
        final boolean scaleX_;
        final boolean scaleY_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  shortdesc  short description (tooltips)
         * @param  scaleX   whether to rescale in X direction
         * @param  scaleY   whether to rescale in Y direction
         */
        RescaleAction( String name, Icon icon, String shortdesc,
                       boolean scaleX, boolean scaleY ) {
            super( name, icon, shortdesc );
            scaleX_ = scaleX;
            scaleY_ = scaleY;
        }

        public void actionPerformed( ActionEvent evt ) {
            PlotState state = getPlotState();
            PointSelection psel = (PointSelection) state.getPlotData();
            Points points = getPoints();
            if ( state.getValid() && points != null ) {
                double[] xLimits = scaleY_ && ! scaleX_
                                 ? state.getRanges()[ 0 ]
                                 : null;
                Range[] ranges = ((LinesPlot) getPlot())
                                .calculateBounds( psel.createPlotData( points ),
                                                  state, xLimits )
                                .getRanges();
                getAxisWindow().clearRanges();
                if ( scaleX_ ) {
                    getDataRanges()[ 0 ] = ranges[ 0 ];
                    getViewRanges()[ 0 ].clear();
                }
                if ( scaleY_ ) {
                    System.arraycopy( ranges, 1,
                                      yDataRanges_, 0, ranges.length - 1 );
                    for ( Range range : yViewRangeMap_.values() ) {
                        range.clear();
                    }
                }
                replot();
            }
        }
    }

    /**
     * Custom axes selector for LinesWindow.
     * It features individual log and flip checkboxes for the Y axis;
     * these are supplied externally and may be different for each 
     * selector, unlike those in plot windows which share the same
     * log/flip flag arrays for each axis.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static class LinesAxesSelector extends CartesianAxesSelector {
        private final ToggleButtonModel yLogModel_;
        private final ToggleButtonModel yFlipModel_;

        /**
         * Constructor.
         *
         * @param  yLogModel   toggler for Y axis log scaling
         * @param  yFlipModel  toggler for Y axis inverted sense
         * @param  errorModeModels  selection models for error modes,
         *                          one per axis
         */
        LinesAxesSelector( ToggleButtonModel yLogModel,
                           ToggleButtonModel yFlipModel,
                           ErrorModeSelectionModel[] errorModeModels ) {
            super( AXIS_NAMES,
                   new ToggleButtonModel[] { null, yLogModel, },
                   new ToggleButtonModel[] { null, yFlipModel, },
                   errorModeModels );
            yLogModel_ = yLogModel;
            yFlipModel_ = yFlipModel;
        }

        /**
         * Returns the model for Y axis logarithmic scaling.
         *
         * @return  Y axis log toggler
         */
        public ToggleButtonModel getYLogModel() {
            return yLogModel_;
        }

        /**
         * Returns the model for Y axis inverted sense.
         *
         * @return  Y axis flip toggler
         */
        public ToggleButtonModel getYFlipModel() {
            return yFlipModel_;
        }

        public void setTable( TopcatModel tcModel ) {
            super.setTable( tcModel );

            /* Ensure that the magic 'index' column is included in the
             * X axis column selector. */
            if ( tcModel != null ) {
                getColumnSelector( 0 )
               .setModel( new ColumnDataComboBoxModel( tcModel, Number.class,
                                                       true, true ) );
            }
        }

        public void initialiseSelectors() {

            /* If we can find an epoch-type column, use that for the X axis,
             * otherwise just use the magic 'index' column. */
            ComboBoxModel xModel = getColumnSelector( 0 ).getModel();
            ColumnData timeCol = xModel instanceof ColumnDataComboBoxModel
                ? ((ColumnDataComboBoxModel) xModel)
                 .getBestMatchColumnData( TopcatUtils.TIME_INFO )
                : null;
            xModel.setSelectedItem( timeCol == null ? xModel.getElementAt( 1 )
                                                    : timeCol );

            /* Y axis gets first non-null column which isn't the one we've
             * used for X. */
            ComboBoxModel yModel = getColumnSelector( 1 ).getModel();
            for ( int i = 0; i < yModel.getSize(); i++ ) {
                Object item = yModel.getElementAt( i );
                if ( item != null &&
                     ! item.equals( xModel.getSelectedItem() ) ) {
                    yModel.setSelectedItem( item );
                    break;
                }
            }
        }
    }

    /**
     * Component which can display the current coordinates of the cursor
     * within a LinesPlot component.
     */
    private static class LinesPositionLabel extends JLabel
                                            implements MouseMotionListener {
        private final LinesPlot plot_;
        private int isurf_;
        private PlotSurface surface_;
        private PositionReporter reporter_;

        /**
         * Constructor.
         *
         * @param  plot  plot for which cursor position should be reported
         */
        LinesPositionLabel( LinesPlot plot ) {
            plot_ = plot;
            Font font = getFont();
            setFont( new Font( "Monospaced",
                               font.getStyle(), font.getSize() ) );
            setBorder( BorderFactory.createCompoundBorder(
                           BorderFactory.createEtchedBorder(),
                           BorderFactory.createEmptyBorder( 0, 5, 0, 5 ) ) );
            plot.addMouseMotionListener( this );
            reportPosition( null );
        }

        public void mouseMoved( MouseEvent evt ) {
            reportPosition( evt.getPoint() );
        }

        public void mouseDragged( MouseEvent evt ) {
            reportPosition( evt.getPoint() );
        }

        /**
         * Reports the position at a given point by drawing it as the text
         * content of this label.
         *
         * @param  p   position
         */
        private void reportPosition( Point p ) {
            PositionReporter reporter = getReporter( p );
            StringBuffer sbuf = new StringBuffer( "Position: " );
            if ( reporter != null ) {
                String[] fc = reporter.formatPosition( p.x, p.y );
                if ( fc != null ) {
                    sbuf.append( '(' )
                        .append( fc[ 0 ] )
                        .append( ", " )
                        .append( fc[ 1 ] )
                        .append( ')' );
                }
            }
            setText( sbuf.toString() );
        }

        /**
         * Returns a reporter object which corresponds to the given position.
         *
         * @param   p  point at which reporting is required
         * @return  position reporter which knows about coordinates
         *          at <code>p</code> (may be null if invalid position)
         */
        private PositionReporter getReporter( Point p ) {
            LinesPlotState state = (LinesPlotState) plot_.getState();
            PlotSurface[] surfaces = plot_.getSurfaces();

            /* No point, no reporter. */
            if ( p == null || state == null || ! state.getValid() ) {
                return null;
            }

            /* If the reporter required is the same as for the last call
             * to this method, and if that reporter is still valid for
             * this plot, return the same one. */
            if ( isurf_ < surfaces.length &&
                 surface_ == surfaces[ isurf_ ] &&
                 surface_.getClip().contains( p ) ) {
                return reporter_;
            }

            else {

                /* Search through the plot surfaces corresponding to the
                 * most recent graphs plotted. */
                for ( int is = 0; is < surfaces.length; is++ ) {
                    PlotSurface surf = surfaces[ is ];
                    if ( surf.getClip().contains( p ) ) {

                        /* If the point is within one, construct a new
                         * suitable reporter, save it and enough information
                         * to be able to tell whether it's still valid later,
                         * and return it. */
                        ValueConverter xConv = state.getConverters()[ 0 ];
                        ValueConverter yConv = state.getYConverters()[ is ];
                        isurf_ = is;
                        surface_ = surf;
                        reporter_ = new PositionReporter( surf, xConv, yConv ) {
                            protected void reportPosition( String[] coords ) {
                            }
                        };
                        return reporter_;
                    }
                }
            }

            /* Point is not in any of the known graphs - return null. */
            return null;
        }
    }
}
