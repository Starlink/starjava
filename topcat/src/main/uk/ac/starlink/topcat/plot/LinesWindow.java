package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
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
import uk.ac.starlink.ttools.convert.ValueConverter;

/**
 * GraphicsWindow which draws a stack of line graphs.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesWindow extends GraphicsWindow implements TopcatListener {

    private final LinesPlot plot_;
    private final ToggleButtonModel antialiasModel_;
    private final ToggleButtonModel vlineModel_;
    private final ToggleButtonModel zeroLineModel_;
    private final Map yViewRangeMap_;

    private Range[] yDataRanges_;
    private StyleSet styles_;
    private int[] activePoints_;

    private static final Color[] COLORS;
    static {
        COLORS = new Color[ Styles.COLORS.length + 1 ];
        COLORS[ 0 ] = Color.BLACK;
        System.arraycopy( Styles.COLORS, 0, COLORS, 1, Styles.COLORS.length );
    }
    private static final StyleSet LINES;
    private static final StyleSet POINTS;
    private static final StyleSet[] STYLE_SETS = new StyleSet[] {
        LINES = MarkStyles.lines( "Black/Coloured Lines", COLORS ),
        MarkStyles.lines( "Lines" ),
        MarkStyles.dashedLines( "Dashed Lines" ),
        POINTS = MarkStyles.points( "Black/Coloured Points", COLORS ),
        MarkStyles.points( "Points" ),
        MarkStyles.spots( "Dots", 1 ),
        MarkStyles.spots( "Spots", 2 ),
        MarkStyles.openShapes( "Small Coloured Outlines", 3, null ),
        MarkStyles.openShapes( "Medium Coloured Outlines", 4, null ),
        MarkStyles.openShapes( "Small Black Outlines", 3, Color.black ),
        MarkStyles.openShapes( "Medium Black Outlines", 4, Color.black ),
    };

    /**
     * Constructor.
     *
     * @param   parent  parent component
     */
    public LinesWindow( Component parent ) {
        super( "Line Plot", new String[] { "X", "Y" }, parent );

        /* Set some initial values. */
        activePoints_ = new int[ 0 ];
        yViewRangeMap_ = new HashMap();

        /* Construct a plot component to hold the plotted graphs. */
        plot_ = new LinesPlot() {
            protected void requestZoomX( double x0, double x1 ) {
                getAxisWindow().getEditors()[ 0 ].clearBounds();
                getViewRanges()[ 0 ].setBounds( x0, x1 );
                replot();
            }
            protected void requestZoomY( int igraph, double y0, double y1 ) {
                getAxisWindow().getEditors()[ 1 + igraph ].clearBounds();
                PointSelector psel = getPointSelectors().getSelector( igraph );
                if ( yViewRangeMap_.containsKey( psel ) ) {
                    ((Range) yViewRangeMap_.get( psel )).setBounds( y0, y1 );
                }
                replot();
            }
        };
        plot_.setPreferredSize( new Dimension( 400, 400 ) );

        /* Add it to the display. */
        getMainArea().add( plot_, BorderLayout.CENTER );
        
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
        JComponent posLabel = plot_.createPositionLabel();
        posLabel.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                posLabel.getMaximumSize()
                                                        .height ) );
        getStatusBox().add( posLabel );
        getStatusBox().add( Box.createHorizontalGlue() );

        /* Arrange for clicking on a given point to cause row activation. */
        plot_.addMouseListener( new PointClickListener() );
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
                addNewSubsets( plot_.getPointsInRange() );
            }
        };

        /* Vertical crosshair toggle action. */
        vlineModel_ = new ToggleButtonModel( "Show vertical crosshair",
                                             ResourceIcon.Y_CURSOR,
                                             "Display a vertical line " +
                                             "which follows the mouse" );
        new CrosshairListener( plot_, vlineModel_ );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
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

        /* Populate toolbar. */
        getToolBar().add( rescaleActionXY );
        getToolBar().add( rescaleActionX );
        getToolBar().add( rescaleActionY );
        getToolBar().add( getAxisEditAction() );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( zeroLineModel_.createToolbarButton() );
        getToolBar().add( getReplotAction() );
        getToolBar().add( antialiasModel_.createToolbarButton() );
        getToolBar().add( fromXRangeAction );
        getToolBar().add( vlineModel_.createToolbarButton() );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "LinesWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected void doReplot( PlotState state, Points points ) {
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( (LinesPlotState) state );
        plot_.repaint();
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
        List pselList = new ArrayList( nsel );
        for ( int isel = 0; isel < nsel; isel++ ) {
            LinesPointSelector psel =
                (LinesPointSelector) pointSelectors.getSelector( isel );
            pselList.add( psel );
            Range yRange;
            if ( psel.isValid() ) {
                AxisEditor yAxEd = axEds[ 1 + isel ];
                ColumnInfo cinfo = psel.getData().getColumnInfo( 1 );
                yAxes[ isel ] = cinfo;
                yConverters[ isel ] =
                    (ValueConverter)
                    cinfo.getAuxDatumValue( TopcatUtils.NUMERIC_CONVERTER_INFO,
                                            ValueConverter.class );
                yAxisLabels[ isel ] = yAxEd.getLabel();
                yLogFlags[ isel ] = psel.getYLogModel().isSelected();
                yFlipFlags[ isel ] = psel.getYFlipModel().isSelected();
                yRange = new Range( yDataRanges_[ isel ] );
                if ( yViewRangeMap_.containsKey( psel ) ) {
                    yRange.limit( (Range) yViewRangeMap_.get( psel ) );
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
        SetId[] setIds = state.getPointSelection().getSetIds();
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

        /* Active points. */
        state.setActivePoints( activePoints_ );

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
        LinesPointSelector newSelector =
            new LinesPointSelector( getStyles(), logModel, flipModel );

        /* Work out if there is a default X axis we should initialise the
         * new selector with.  We'll do this if all the existing valid
         * point selectors are using the same X axis. */
        PointSelectorSet pointSelectors = getPointSelectors();
        DefaultPointSelector mainSel =
            (DefaultPointSelector) pointSelectors.getMainSelector();
        TopcatModel mainTable = null;
        Object mainXAxis = null;
        if ( mainSel != null && mainSel.isValid() ) {
            mainTable = mainSel.getTable();
            mainXAxis = mainSel.getColumnSelector( 0 ).getSelectedItem();
            for ( int i = 0; 
                  mainTable != null && mainXAxis != null
                                    && i < pointSelectors.getSelectorCount();
                  i++ ) {
                DefaultPointSelector psel =
                    (DefaultPointSelector) pointSelectors.getSelector( i );
                if ( psel.isValid() ) {
                    TopcatModel table = psel.getTable();
                    Object xAxis =
                        psel.getColumnSelector( 0 ).getSelectedItem();
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
                newSelector.getColumnSelector( 0 ).setSelectedItem( mainXAxis );
            }
        }

        /* Return the initialised selector. */
        return newSelector;
    }

    protected StyleEditor createStyleEditor() {
        return new LinesStyleEditor();
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
    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        Range xRange = getViewRanges()[ 0 ];
        double[] xBounds = xRange.isClear() ? null
                                            : xRange.getFiniteBounds( false );
        Range[] xyRanges = calculateRanges( pointSelection, points, xBounds );
        yDataRanges_ = new Range[ xyRanges.length - 1 ];
        System.arraycopy( xyRanges, 1, yDataRanges_, 0, xyRanges.length - 1 );
        return new Range[] { xyRanges[ 0 ] };
    }

    /**
     * Calculates data ranges along the X and Y axes for a given 
     * point selection and data object.
     *
     * @param  pointSelection  point selection for calculations
     * @param  ponints  points data
     * @param  xBounds  (lower,upper) bounds array giving region for range
     *         determination; may be null for the whole X region
     */
    private Range[] calculateRanges( PointSelection pointSelection,
                                     Points points, double[] xBounds ) {

        /* Set X and Y bounds. */
        double xlo = xBounds == null ? - Double.MAX_VALUE : xBounds[ 0 ];
        double xhi = xBounds == null ? + Double.MAX_VALUE : xBounds[ 1 ];
        double ylo = - Double.MAX_VALUE;
        double yhi = + Double.MAX_VALUE;

        /* Work out which graph each set belongs to. */
        PointSelectorSet pointSelectors = getPointSelectors();
        int ngraph = pointSelectors.getSelectorCount();
        List pselList = new ArrayList( ngraph );
        for ( int isel = 0; isel < ngraph; isel++ ) {
            pselList.add( pointSelectors.getSelector( isel ) );
        }
        SetId[] setIds = pointSelection.getSetIds();
        int nset = setIds.length;
        int[] graphIndices = new int[ nset ];
        for ( int iset = 0; iset < nset; iset++ ) {
            int igraph = pselList.indexOf( setIds[ iset ].getPointSelector() );
            graphIndices[ iset ] = igraph;
        }

        /* Set up initial values for extrema. */
        Range xRange = new Range();
        Range[] yRanges = new Range[ ngraph ];
        for ( int i = 0; i < ngraph; i++ ) {
            yRanges[ i ] = new Range();
        }

        /* Go through all the data finding extrema. */
        RowSubset[] sets = pointSelection.getSubsets();
        int npoint = points.getCount();
        double[] coords = new double[ 2 ];
        for ( int ip = 0; ip < npoint; ip++ ) {
            points.getCoords( ip, coords );
            double x = coords[ 0 ];
            double y = coords[ 1 ];
            if ( x >= xlo && x <= xhi && y >= ylo && y <= yhi ) {
                boolean isUsed = false;
                for ( int iset = 0; iset < nset; iset++ ) {
                    if ( sets[ iset ].isIncluded( ip ) ) {
                        int igraph = graphIndices[ iset ];
                        isUsed = true;
                        yRanges[ igraph ].submit( y );
                    }
                }
                if ( isUsed ) {
                    xRange.submit( x );
                }
            }
        }

        /* Package and return calculated ranges. */
        Range[] ranges = new Range[ ngraph + 1 ];
        ranges[ 0 ] = xRange;
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            yRanges[ igraph ].pad( 0.025 );
            ranges[ igraph + 1 ] = yRanges[ igraph ];
        }
        return ranges;
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                TopcatModel tcModel = evt.getModel();
                PointSelection psel = plot_.getState().getPointSelection();
                long lrow = ((Long) datum).longValue();
                long[] lps = psel.getPointsForRow( tcModel, lrow );
                int[] ips = new int[ lps.length ];
                for ( int i = 0; i < lps.length; i++ ) {
                    ips[ i ] = Tables.checkedLongToInt( lps[ i ] );
                }
                activePoints_ = ips;
                replot();
            }
        }
    }

    /**
     * Listener which can keep the AxisWindow up to date with the current
     * state of the plot.  It needs to add and remove AxisEditor components
     * in tandem with the state of the PointSelectorSet component.
     */
    private class AxisWindowUpdater implements ActionListener {
        int nsel_;
        AxisEditor xAxEd_;
        Map yAxEdMap_;

        /**
         * Check that this object has run through initialisation sequence.
         */
        private void ensureInitialised() {
            if ( xAxEd_ == null ) {
                assert yAxEdMap_ == null;

                /* Set up the X axis editor. */
                PointSelector mainSel = getPointSelectors().getMainSelector();
                AxisEditor[] mainEds = mainSel.createAxisEditors();
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
                yAxEdMap_ = new HashMap();
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
                    AxisEditor yAxEd = psel.createAxisEditors()[ 1 ];
                    yAxEd.setTitle( "Y Axis (" + psel.getLabel() + ")" ); 
                    yAxEd.addActionListener( getReplotListener() );
                    yAxEdMap_.put( psel, yAxEd );
                    Range yRange = new Range();
                    yAxEd.addMaintainedRange( yRange );
                    yViewRangeMap_.put( psel, yRange );
                }
                axEds[ 1 + isel ] = (AxisEditor) yAxEdMap_.get( psel );
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
            if ( evt.getButton() == MouseEvent.BUTTON1 ) {
                PointIterator pit = plot_.getPlottedPointIterator();
                if ( pit != null ) {
                    int ip = pit.getClosestPoint( evt.getPoint(), 4 );
                    if ( ip >= 0 ) {
                        PointSelection psel = 
                            plot_.getState().getPointSelection();
                        psel.getPointTable( ip )
                            .highlightRow( psel.getPointRow( ip ) );
                    }
                    else {
                        activePoints_ = new int[ 0 ];
                        replot();
                    }
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
                Rectangle zone = linesPlot_.getPlotRegion();
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
            Points points = getPoints();
            if ( state.getValid() && points != null ) {
                double[] xLimits = scaleY_ && ! scaleX_
                                 ? state.getRanges()[ 0 ]
                                 : null;
                Range[] ranges = calculateRanges( state.getPointSelection(),
                                                  points, xLimits );
                getAxisWindow().clearRanges();
                AxisEditor[] axEds = getAxisWindow().getEditors();
                if ( scaleX_ ) {
                    getDataRanges()[ 0 ] = ranges[ 0 ];
                    getViewRanges()[ 0 ].clear();
                }
                if ( scaleY_ ) {
                    System.arraycopy( ranges, 1,
                                      yDataRanges_, 0, ranges.length - 1 );
                    for ( Iterator it = yViewRangeMap_.entrySet().iterator();
                          it.hasNext(); ) {
                        ((Range) ((Map.Entry) it.next()).getValue()).clear();
                    }
                }
                replot();
            }
        }
    }

    /**
     * Custom point selector for LinesWindow.
     * It features individual log and flip checkboxes for the Y axis;
     * these are supplied externally and may be different for each 
     * selector, unlike those in plot windows which share the same
     * log/flip flag arrays for each axis.
     */
    private static class LinesPointSelector extends DefaultPointSelector {
        private final ToggleButtonModel yLogModel_;
        private final ToggleButtonModel yFlipModel_;

        /**
         * Constructor.
         *
         * @param  styles      initial style set
         * @param  yLogModel   toggler for Y axis log scaling
         * @param  yFlipModel  toggler for Y axis inverted sense
         */
        LinesPointSelector( MutableStyleSet styles,
                            ToggleButtonModel yLogModel,
                            ToggleButtonModel yFlipModel ) {
            super( styles, new String[] { "X", "Y" },
                   new DefaultPointSelector.ToggleSet[] {
                       new DefaultPointSelector.ToggleSet(
                           "Log", new ToggleButtonModel[] { null,
                                                            yLogModel } ),
                       new DefaultPointSelector.ToggleSet(
                           "Flip", new ToggleButtonModel[] { null,
                                                             yFlipModel } ),
                   } );
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

        protected void configureSelectors( TopcatModel tcModel ) {
            super.configureSelectors( tcModel );

            /* Ensure that the magic 'index' column is included in the
             * X axis column selector. */
            if ( tcModel != null ) {
                getColumnSelector( 0 )
               .setModel( new ColumnDataComboBoxModel( tcModel, Number.class,
                                                       true, true ) );
            }
        }

        protected void initialiseSelectors() {

            /* If we can find an epoch-type column, use that for the X axis,
             * otherwise just use the magic 'index' column. */
            ComboBoxModel xModel = getColumnSelector( 0 ).getModel();
            ColumnData timeCol = xModel instanceof ColumnDataComboBoxModel
                ? ((ColumnDataComboBoxModel) xModel)
                 .getColumnData( TopcatUtils.TIME_INFO )
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
}
