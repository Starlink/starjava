package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * GraphicsWindow which presents one-dimensional data as a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public class HistogramWindow extends GraphicsWindow {

    private final Histogram plot_;
    private final Action[] validityActions_;
    private final ToggleButtonModel yLogModel_;
    private final ToggleButtonModel cumulativeModel_;
    private final JCheckBox offsetSelector_;
    private final RoundingSpinner binSizer_;
    private final RoundingSpinner.RoundingSpinnerModel linearBinModel_;
    private final RoundingSpinner.RoundingSpinnerModel logBinModel_;

    private int autoYMax_;
    private int autoYMaxCumulative_;
    private double autoLinearBinWidth_;
    private double autoLogBinWidth_;

    private static final int DEFAULT_BINS = 20;

    /** Description of vertical plot axis. */
    private final static ValueInfo COUNT_INFO = 
        new DefaultValueInfo( "Count", Integer.class,
                              "Number of values in bin" );

    /**
     * Constructs a new histogram window.
     *
     * @param  parent  parent component (may be used for positioning)
     */
    public HistogramWindow( Component parent ) {
        super( "Histogram", new String[] { "X" }, 0, parent );

        /* Create the histogram plot itself.  Being a histogram, there's
         * no point zooming in such a way that the Y axis goes below zero,
         * so block that. */
        PlotSurface surf = new PtPlotSurface() {
            void _setYRange( double min, double max ) {
                super._setYRange( Math.max( getMinYValue(), min ), max );
            }
        };
        plot_ = new Histogram( surf ) {
            protected void requestZoom( double[][] bounds ) {
                double[] xbounds = bounds[ 0 ];
                if ( xbounds != null ) {
                    getAxisWindow().getEditors()[ 0 ].clearBounds();
                    getViewRanges()[ 0 ].setBounds( xbounds );
                }
                double[] ybounds = bounds[ 1 ];
                if ( ybounds != null && ybounds[ 1 ] > 0.0 ) {
                    getAxisWindow().getEditors()[ 1 ].clearBounds();
                    getViewRanges()[ 1 ]
                       .setBounds( Math.max( 0.0, ybounds[ 0 ] ),
                                   ybounds[ 1 ] );
                }
                replot();
            }
        };

        /* Place the histogram plot. */
        getMainArea().add( plot_, BorderLayout.CENTER );

        /* Actions for rescaling the axes. */
        Action rescaleActionXY =
            new RescaleAction( "Rescale", ResourceIcon.RESIZE,
                               "Rescale the plot to fit all data",
                               true, true );
        Action rescaleActionX = 
            new RescaleAction( "Rescale X", ResourceIcon.RESIZE_X,
                               "Rescale the X axis to fit all data",
                               true, false );
        Action rescaleActionY =
            new RescaleAction( "Rescale Y", ResourceIcon.RESIZE_Y,
                               "Rescale the Y axis to fit all data",
                               false, true );

        /* Model for Y log axis selection.  Possibly questionable for a 
         * histogram?  But someone might want it. */
        yLogModel_ = new ToggleButtonModel( "Log Y Axis", ResourceIcon.YLOG,
                                           "Logarithmic scale for the Y axis" );
        yLogModel_.addActionListener( getReplotListener() );

        /* Model for selecting a cumulative rather than conventional plot. */
        cumulativeModel_ =
            new ToggleButtonModel( "Cumulative Plot", ResourceIcon.CUMULATIVE,
                                   "Plot cumulative bars rather than counts" );
        cumulativeModel_.addActionListener( getReplotListener() );

        /* Bin size and offset selector box. */
        binSizer_ = new RoundingSpinner();
        linearBinModel_ = new RoundingSpinner.RoundingSpinnerModel( binSizer_ );
        logBinModel_ = new RoundingSpinner.RoundingSpinnerModel( binSizer_ );
        binSizer_.setModel( linearBinModel_ );
        getLogModels()[ 0 ].addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                boolean isLog = getLogModels()[ 0 ].isSelected();
                binSizer_.setLogarithmic( isLog );
                binSizer_.setModel( isLog ? logBinModel_ : linearBinModel_ );
                offsetSelector_.setEnabled( ! isLog );
            }
        } );
        binSizer_.addChangeListener( getReplotListener() );
        offsetSelector_ = new JCheckBox( "Offset:" );
        offsetSelector_.setHorizontalTextPosition( SwingConstants.LEFT );
        offsetSelector_.addChangeListener( getReplotListener() );
        JComponent binBox = Box.createHorizontalBox();
        binBox.add( offsetSelector_ );
        binBox.add( Box.createHorizontalStrut( 15 ) );
        binBox.add( new JLabel( "Width: " ) );
        binBox.add( binSizer_ );
        binBox.setBorder( makeTitledBorder( "Bin Placement" ) );
        getMainArea().add( binBox, java.awt.BorderLayout.SOUTH );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( cumulativeModel_.createMenuItem() );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for axis operations. */
        JMenu axisMenu = new JMenu( "Axes" );
        axisMenu.setMnemonic( KeyEvent.VK_A );
        axisMenu.add( getFlipModels()[ 0 ].createMenuItem() );
        axisMenu.addSeparator();
        axisMenu.add( getLogModels()[ 0 ].createMenuItem() );
        axisMenu.add( yLogModel_.createMenuItem() );
        getJMenuBar().add( axisMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        Action fromVisibleAction = new BasicAction( "New subset from visible",
                                                    ResourceIcon.RANGE_SUBSET,
                                                    "Define a new row subset "
                                                  + "containing only currently "
                                                  + "visible range" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubsets( plot_.getVisiblePoints() );
            }
        };
        subsetMenu.add( fromVisibleAction );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for plot style selection. */
        JMenu styleMenu = new JMenu( "Bar Style" );
        styleMenu.setMnemonic( KeyEvent.VK_B );
        StyleSet[] styleSets = getStyleSets();
        for ( int i = 0; i < styleSets.length; i++ ) {
            final StyleSet styleSet = styleSets[ i ];
            String name = styleSet.getName();
            Icon icon = BarStyles.getIcon( styleSet );
            Action stylesAct = new BasicAction( name, icon,
                                                "Set bar plotting style to "
                                              + name ) {
                public void actionPerformed( ActionEvent evt ) {
                    setStyles( styleSet );
                    replot();
                }
            };
            styleMenu.add( stylesAct );
        }
        getJMenuBar().add( styleMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( rescaleActionXY );
        getToolBar().add( rescaleActionX );
        getToolBar().add( rescaleActionY );
        getToolBar().add( getAxisEditAction() );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( cumulativeModel_.createToolbarButton() );
        getToolBar().add( yLogModel_.createToolbarButton() );
        getToolBar().add( getReplotAction() );
        getToolBar().add( fromVisibleAction );
        getToolBar().addSeparator();

        /* Prepare a list of actions which are enabled/disabled according
         * to whether the plot state is valid. */
        List actList = new ArrayList();
        actList.add( rescaleActionXY );
        actList.add( rescaleActionX );
        actList.add( rescaleActionY );
        actList.add( fromVisibleAction );
        actList.add( getReplotAction() );
        validityActions_ = (Action[]) actList.toArray( new Action[ 0 ] );

        /* Add standard help actions. */
        addHelp( "HistogramWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected PointSelector createPointSelector() {

        /* This bit just copied from the superclass. */
        DefaultPointSelector.ToggleSet[] toggleSets =
            new DefaultPointSelector.ToggleSet[] {
                new DefaultPointSelector.ToggleSet( "Log", getLogModels() ),
                new DefaultPointSelector.ToggleSet( "Flip", getFlipModels() ),
            };

        /* The superclass implementation assumes that the number of
         * axes on the screen is the same as the number of dimensions of
         * the data being plotted - not true for a histogram (2 vs. 1,
         * respectively).  So we need to make sure the AxisEditor array
         * supplied by the PointSelector is 2 long - one for the data
         * axis and one for the counts axis (screen Y axis). */
        return new DefaultPointSelector( getStyles(), new String[] { "X" },
                                         toggleSets ) {
            public AxisEditor[] createAxisEditors() {
                AxisEditor countEd = new AxisEditor( "Count" );
                countEd.setAxis( COUNT_INFO );
                return new AxisEditor[] {
                    super.createAxisEditors()[ 0 ],
                    countEd,
                };
            }
        };
    }

    protected void doReplot( PlotState pstate, Points points ) {

        /* Send the plot component the most up to date plotting state. */
        HistogramPlotState state = (HistogramPlotState) pstate;
        plot_.setPoints( points );
        plot_.setState( state );

        /* Schedule a repaint. */
        plot_.repaint();
    }

    public StyleSet getDefaultStyles( int npoint ) {
        return BarStyles.sideFilled( "Filled Adjacent" );
    }

    public StyleSet[] getStyleSets() {
        return new StyleSet[] {
            BarStyles.sideFilled( "Filled Adjacent" ),
            // BarStyles.sideFilled3d( "Bevelled Adjacent" ),
            BarStyles.sideOpen( "Open Adjacent", true, false ),
            BarStyles.tops( "Outlines", true, false ),
            BarStyles.filled( "Filled Overplot" ),
            BarStyles.filled3d( "Bevelled Overplot" ),
            BarStyles.open( "Open Overplot", true, false ),
            BarStyles.spikes( "Spikes", true, false ),
            BarStyles.sideOpen( "Black Open Adjacent", false, true ),
            BarStyles.tops( "Black Outlines", false, true ),
            BarStyles.open( "Black Open Overplot", false, true ),
            BarStyles.spikes( "Black Spikes", false, true ),
        };
    }

    protected StyleEditor createStyleEditor() {
        return new BarStyleEditor();
    }

    protected PlotState createPlotState() {
        HistogramPlotState state = new HistogramPlotState();
        return state;
    }

    public PlotState getPlotState() {
        HistogramPlotState state = (HistogramPlotState) super.getPlotState();
        boolean valid = state != null && state.getValid();
        if ( valid ) {

            /* Fill in histogram-specific items. */
            double bw = binSizer_.getNumericValue();
            if ( bw > 0 ) {
                state.setBinWidth( bw );
            }
            state.setZeroMid( offsetSelector_.isSelected() );
            state.setCumulative( cumulativeModel_.isSelected() );

            /* The state obtained from the superclass implementation has
             * purely 1-d attributes, since the histogram data model 1-d plot.
             * However the plot itself is on a 2-d plotting surface, 
             * so modify some of the state to contain axis information here. */
            state.setAxes( new ValueInfo[] {
                state.getAxes()[ 0 ],
                COUNT_INFO
            } );
            state.setLogFlags( new boolean[] {
                state.getLogFlags()[ 0 ],
                yLogModel_.isSelected(),
            } );
            state.setFlipFlags( new boolean[] {
                state.getFlipFlags()[ 0 ],
                false
            } );

            /* Calculate the Y data range based on the autosized range
             * calculated last time calculateMaxCount was called and on
             * the current state (bin width and cumuluative flag). */
            double yMax = cumulativeModel_.isSelected()
                ? autoYMaxCumulative_
                : ( state.getLogFlags()[ 0 ]
                        ? autoYMax_ * Math.log( bw )
                                    / Math.log( autoLogBinWidth_ )
                        : autoYMax_ * bw / autoLinearBinWidth_ );
            Range yRange = new Range( 0.0, yMax );
            yRange.limit( getViewRanges()[ 1 ] );
            state.setRanges( new double[][] {
                state.getRanges()[ 0 ],
                yRange.getFiniteBounds( yLogModel_.isSelected() )
            } );
        }

        /* Configure some actions to be enabled/disabled according to 
         * whether the plot state is valid. */
        for ( int i = 0; i < validityActions_.length; i++ ) {
            validityActions_[ i ].setEnabled( valid );
        }

        /* Return the state. */
        return state;
    }

    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        Range xRange = super.calculateRanges( pointSelection, points )[ 0 ];
        boolean xlog = getLogModels()[ 0 ].isSelected();
        double[] xBounds = xRange.getFiniteBounds( xlog );
        calculateMaxCount( pointSelection, points, xBounds, true );
        return new Range[] { xRange };
    }

    /**
     * Calculates the data range on the Y axis.  Being a histogram, this
     * isn't determined by the data alone it's also a function of certain
     * plot state values such as bin size.
     * This method stores its results in instance variables for later use.
     *
     * @param  pointSelection  point selection
     * @param  points  points data
     * @param  xBounds   bounds on the X axis for assessment
     * @param  autoWidth  true iff you want the bin width to be (re)calculated
     *         automatically by this routine
     */
    private void calculateMaxCount( PointSelection pointSelection,
                                    Points points, double[] xBounds,
                                    boolean autoWidth ) {

        /* Get and possibly store the bin size. */
        if ( autoWidth ||
             ! ( linearBinModel_.getValue() instanceof Number ) ||
             ! ( logBinModel_.getValue() instanceof Number ) ) {
            double[] multBounds = new Range( xBounds ).getFiniteBounds( true );
            double factor =
                Math.exp( Math.log( multBounds[ 1 ] / multBounds[ 0 ] )
                          / DEFAULT_BINS );
            double bwLog = Rounder.LOG.round( factor );
            logBinModel_.setValue( new Double( bwLog ) );

            double gap = ( xBounds[ 1 ] - xBounds[ 0 ] ) / DEFAULT_BINS;
            assert gap > 0.0;
            Class clazz = getPointSelectors().getMainSelector().getData()
                         .getColumnInfo( 0 ).getContentClass();
            double bwLinear = Rounder.LINEAR.round( gap );
            if ( clazz == Byte.class ||
                 clazz == Short.class ||
                 clazz == Integer.class ||
                 clazz == Long.class ) {
                gap = Math.ceil( gap );
            }
            linearBinModel_.setValue( new Double( bwLinear ) );
        }
        double bwLog = ((Number) logBinModel_.getValue()).doubleValue();
        double bwLinear = ((Number) linearBinModel_.getValue()).doubleValue();

        /* Acquire an empty binned data object. */
        RowSubset[] rsets = pointSelection.getSubsets();
        int nset = rsets.length;
        boolean zeromid = offsetSelector_.isSelected();
        MapBinnedData binned = getLogModels()[ 0 ].isSelected()
            ? MapBinnedData.createLogBinnedData( nset, bwLog )
            : MapBinnedData.createLinearBinnedData( nset, bwLinear, zeromid );
        MapBinnedData.BinMapper mapper = binned.getMapper();
        double xlo = mapper.getBounds( mapper.getKey( xBounds[ 0 ] ) )[ 0 ];
        double xhi = mapper.getBounds( mapper.getKey( xBounds[ 1 ] ) )[ 1 ];

        /* Populate it. */
        int np = points.getCount();
        boolean[] setFlags = new boolean[ nset ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            double[] coords = points.getPoint( ip );
            double x = coords[ 0 ];
            if ( x >= xlo && x <= xhi ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = rsets[ is ].isIncluded( lp );
                }
                binned.submitDatum( x, setFlags );
            }
        }

        /* Find the highest bin count. */
        int[] ymaxes = new int[ nset ];
        int[] ytots = new int[ nset ];
        for ( Iterator binIt = binned.getBinIterator( false );
              binIt.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) binIt.next();
            for ( int is = 0; is < nset; is++ ) {
                int n = bin.getCount( is );
                ytots[ is ] += n;
                ymaxes[ is ] = Math.max( ymaxes[ is ], n );
            }
        }
        int yMax = 0;
        int yMaxTot = 0;
        for ( int is = 0; is < nset; is++ ) {
            yMax = Math.max( yMax, ymaxes[ is ] );
            yMaxTot = Math.max( yMaxTot, ytots[ is ] );
        }

        /* Store results for later calculations. */
        autoYMax_ = yMax + (int) ( yMax * getPadRatio() );
        autoLinearBinWidth_ = bwLinear;
        autoLogBinWidth_ = bwLog;
        autoYMaxCumulative_ = yMaxTot + (int) ( yMaxTot * getPadRatio() );
    }

    /**
     * Returns the minimum sensible value for the Y axis.
     * Being a histogram, there's no point in going below the value 
     * which represents zero counts.
     *
     * @return   minimum Y value
     */
    private double getMinYValue() {

        /* The value used for logarithmic axes here is a bit arbitrary,
         * but something a bit below zero makes sense. */
        return yLogModel_.isSelected() ? -1. : 0.;
    }

    /**
     * Action class for rescaling one or both axes.
     */
    private class RescaleAction extends BasicAction {
        final boolean scaleX_;
        final boolean scaleY_;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  shortdesc  action short description (tooltips)
         * @param  scaleX  true for rescaling on X axis
         * @param  scaleY  true for rescaling on Y axis
         */
        RescaleAction( String name, Icon icon, String shortdesc, 
                       boolean scaleX, boolean scaleY ) {
            super( name, icon, shortdesc );
            scaleX_ = scaleX;
            scaleY_ = scaleY;
        }

        public void actionPerformed( ActionEvent evt ) {
            PlotState state = getPlotState();
            PointSelection pointSelection = state.getPointSelection();
            Points points = getPoints();
            if ( state.getValid() && points != null ) {

                /* Do X scaling if required. */
                double[] xBounds;
                if ( scaleX_ ) {
                    Range xRange = HistogramWindow.super
                        .calculateRanges( pointSelection, points )[ 0 ];
                    getDataRanges()[ 0 ] = xRange;
                    getViewRanges()[ 0 ].clear();
                    getAxisWindow().getEditors()[ 0 ].clearBounds();
                    xBounds =
                        xRange.getFiniteBounds( state.getLogFlags()[ 0 ] );
                }
                else {
                    xBounds = state.getRanges()[ 0 ];
                }

                /* Do Y scaling if required. */
                if ( scaleY_ ) {
                    calculateMaxCount( pointSelection, points, xBounds,
                                       scaleX_ );
                    getViewRanges()[ 1 ].clear();
                    getAxisWindow().getEditors()[ 1 ].clearBounds();
                }
                replot();
            }
        }
    }
}
