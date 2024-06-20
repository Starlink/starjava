package uk.ac.starlink.topcat.plot;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSource;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.BarStyles;
import uk.ac.starlink.ttools.plot.BinnedData;
import uk.ac.starlink.ttools.plot.Histogram;
import uk.ac.starlink.ttools.plot.HistogramPlotState;
import uk.ac.starlink.ttools.plot.MapBinnedData;
import uk.ac.starlink.ttools.plot.NormalisedBinnedData;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotListener;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Rounder;
import uk.ac.starlink.ttools.plot.StyleSet;

/**
 * GraphicsWindow which presents one-dimensional data as a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public class HistogramWindow extends GraphicsWindow {

    private final SurfaceZoomRegionList zoomRegionList_;
    private final Action[] validityActions_;
    private final ToggleButtonModel yLogModel_;
    private final ToggleButtonModel cumulativeModel_;
    private final ToggleButtonModel weightModel_;
    private final ToggleButtonModel normaliseModel_;
    private final JCheckBox offsetSelector_;
    private final RoundingSpinner binSizer_;
    private final RoundingSpinner.RoundingSpinnerModel linearBinModel_;
    private final RoundingSpinner.RoundingSpinnerModel logBinModel_;

    private Range autoYRange_;
    private Range autoYRangeCum_;
    private Range autoYRangeNorm_;
    private Range autoYRangeCumNorm_;
    private double autoLinearBinWidth_;
    private double autoLogBinWidth_;

    private static final int DEFAULT_BINS = 20;

    /**
     * Constructs a new histogram window.
     *
     * @param  parent  parent component (may be used for positioning)
     */
    public HistogramWindow( Component parent ) {
        super( "Histogram (old)", new Histogram( new PtPlotSurface() ), 
               new String[] { "X" }, 0, false, new ErrorModeSelectionModel[ 0 ],
               parent );
        final Histogram plot = (Histogram) getPlot();
        plot.setPreferredSize( new Dimension( 500, 300 ) );

        /* Zooming. */
        plot.addPlotListener( new PlotListener() {
            public void plotChanged( PlotEvent evt ) {
                zoomRegionList_.reconfigure();
            }
        } );
        zoomRegionList_ = new SurfaceZoomRegionList( plot ) {
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
        Zoomer zoomer = new Zoomer();
        zoomer.setRegions( zoomRegionList_ );
        zoomer.setCursorComponent( plot );
        Component scomp = plot.getSurface().getComponent();
        scomp.addMouseListener( zoomer );
        scomp.addMouseMotionListener( zoomer );

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

        /* Model for allowing weighting of the histogram. */
        weightModel_ =
            new ToggleButtonModel( "Weight Counts", ResourceIcon.WEIGHT,
                                   "Allow weighting of histogram counts" );
        weightModel_.addActionListener( getReplotListener() );

        /* Model to allow normalisation of the histogram. */
        normaliseModel_ =
            new ToggleButtonModel( "Normalisation", ResourceIcon.NORMALISE,
                                   "Normalise histogram counts to unity" );
        normaliseModel_.addActionListener( getReplotListener() );

        /* Actions for saving or exporting the binned data as a table. */
        TableSource binSrc = new TableSource() {
            public StarTable getStarTable() {
                return getBinDataTable();
            }
        };
        Action saveAct = createSaveTableAction( "binned data", binSrc );
        Action importAct = createImportTableAction( "binned data", binSrc,
                                                    "histogram" );
        getExportMenu().addSeparator();
        getExportMenu().add( saveAct );
        getExportMenu().add( importAct );

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
        getExtrasPanel().add( binBox );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( cumulativeModel_.createMenuItem() );
        plotMenu.add( weightModel_.createMenuItem() );
        plotMenu.add( normaliseModel_.createMenuItem() );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
        plotMenu.add( getLegendModel().createMenuItem() );
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
                addNewSubsets( plot.getVisiblePoints() );
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
        getPointSelectorToolBar().addSeparator();
        getPointSelectorToolBar().add( weightModel_.createToolbarButton() );
        getToolBar().add( rescaleActionXY );
        getToolBar().add( rescaleActionX );
        getToolBar().add( rescaleActionY );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getLegendModel().createToolbarButton() );
        getToolBar().add( cumulativeModel_.createToolbarButton() );
        getToolBar().add( normaliseModel_.createToolbarButton() );
        getToolBar().add( yLogModel_.createToolbarButton() );
        getToolBar().add( fromVisibleAction );
        getToolBar().addSeparator();

        /* Prepare a list of actions which are enabled/disabled according
         * to whether the plot state is valid. */
        List<Action> actList = new ArrayList<Action>();
        actList.add( rescaleActionXY );
        actList.add( rescaleActionX );
        actList.add( rescaleActionY );
        actList.add( fromVisibleAction );
        actList.add( getReplotAction() );
        validityActions_ = actList.toArray( new Action[ 0 ] );

        /* Add standard help actions. */
        addHelp( "HistogramWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlotPanel() {
        return getPlot();
    }

    protected PointSelector createPointSelector() {

        /* Action which resets the axis window Y axis editor when weighting
         * status may have changed. */
        final ActionListener weightAction = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                AxisWindow axwin = getAxisWindow();
                final AxisEditor yaxed = axwin == null
                                       ? null
                                       : axwin.getEditors()[ 1 ];
                if ( yaxed != null ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            ValueInfo axinfo =
                                Histogram
                               .getYInfo( HistogramWindow.this.hasWeights(),
                                          HistogramWindow.this.isNormalised() );
                            yaxed.setAxis( axinfo );
                        }
                    } );
                }
            }
        };
        weightModel_.addActionListener( weightAction );
        normaliseModel_.addActionListener( weightAction );

        /* Basic selector for X axis. */
        AxesSelector axsel =
            new CartesianAxesSelector( new String[] { "X" }, getLogModels(),
                                       getFlipModels(),
                                       new ErrorModeSelectionModel[ 0 ] );

        /* Add the possibility of weighting it. */
        final WeightedAxesSelector waxsel = new WeightedAxesSelector( axsel ) {

            /* The superclass implementation assumes that the number of
             * axes on the screen is the same as the number of dimensions of
             * the data being plotted - not true for a histogram (2 vs. 1,
             * respectively).  So we need to make sure the AxisEditor array
             * supplied by the PointSelector is 2 long - one for the data
             * axis and one for the counts axis (screen Y axis). */
            public AxisEditor[] createAxisEditors() {
                AxisEditor yaxed = new AxisEditor( "Y" );
                ValueInfo axinfo =
                    Histogram
                   .getYInfo( HistogramWindow.this.hasWeights(),
                              HistogramWindow.this.isNormalised() );
                yaxed.setAxis( axinfo );
                return new AxisEditor[] {
                    super.createAxisEditors()[ 0 ],
                    yaxed,
                };
            }
        };
        waxsel.enableWeights( weightModel_.isSelected() );
        waxsel.getWeightSelector().addActionListener( weightAction );
        weightModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                waxsel.enableWeights( weightModel_.isSelected() );
            }
        } );
        weightAction.actionPerformed( null );

        /* Construct and return a suitable point selector. */
        return new PointSelector( waxsel, getStyles() );
    }

    public int getMainRangeCount() {
        return 1;
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
            double binBase;
            if ( getLogModels()[ 0 ].isSelected() ) {
                binBase = offsetSelector_.isSelected() ? 1.0
                                                       : Math.sqrt( bw );
            }
            else {
                binBase = offsetSelector_.isSelected() ? - bw / 2.0
                                                       : 0.0;
            }
            state.setBinBase( binBase );
            state.setCumulative( cumulativeModel_.isSelected() );

            /* The state obtained from the superclass implementation has
             * purely 1-d attributes, since the histogram data model 1-d plot.
             * However the plot itself is on a 2-d plotting surface, 
             * so modify some of the state to contain axis information here. */
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
            boolean isNorm = isNormalised();
            boolean ylog = yLogModel_.isSelected();
            Range yRange;
            if ( cumulativeModel_.isSelected() ) {
                yRange = new Range( isNorm ? autoYRangeCumNorm_
                                           : autoYRangeCum_ );
            }
            else {
                yRange = new Range( isNorm ? autoYRangeNorm_
                                           : autoYRange_ );
                double factor = state.getLogFlags()[ 0 ]
                              ? Math.log( bw ) / Math.log( autoLogBinWidth_ )
                              : bw / autoLinearBinWidth_;
                double[] bnds = yRange.getFiniteBounds( ylog );
                yRange = new Range( bnds[ 0 ] * factor, bnds[ 1 ] * factor );
            }
            yRange.pad( getPadRatio() );
            yRange.limit( getViewRanges()[ 1 ] );
            double[] yBounds = yRange.getFiniteBounds( ylog );
            if ( ! ylog ) {
                yBounds[ 0 ] = 0.0;
            }
            state.setRanges( new double[][] {
                state.getRanges()[ 0 ],
                yBounds,
            } );

            /* Configure weighting and normalisation. */
            state.setWeighted( hasWeights() );
            state.setNormalised( isNorm );
        }

        /* Configure some actions to be enabled/disabled according to 
         * whether the plot state is valid. */
        for ( int i = 0; i < validityActions_.length; i++ ) {
            validityActions_[ i ].setEnabled( valid );
        }

        /* Return the state. */
        return state;
    }

    /**
     * Indicates whether any of the active point selectors have non-trivial
     * weighting axes.
     *
     * @return  true  if some weighting may be in force
     */
    private boolean hasWeights() {
        PointSelectorSet psels = getPointSelectors();
        for ( int ip = 0; ip < psels.getSelectorCount(); ip++ ) {
            if ( ((WeightedAxesSelector)
                  psels.getSelector( ip ).getAxesSelector()).hasWeights() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether histogram normalisation is in operation.
     *
     * @return  true iff histogram is normalised
     */
    private boolean isNormalised() {
        return normaliseModel_.isSelected();
    }

    public Range[] calculateRanges( PlotData data, PlotState state ) {
        Range xRange = super.calculateRanges( data, state )[ 0 ];
        boolean xlog = getLogModels()[ 0 ].isSelected();
        double[] xBounds = xRange.getFiniteBounds( xlog );
        calculateMaxCount( data, xBounds, true );
        return new Range[] { xRange };
    }

    /**
     * Calculates the data range on the Y axis.  Being a histogram, this
     * isn't determined by the data alone it's also a function of certain
     * plot state values such as bin size.
     * This method stores its results in instance variables for later use.
     *
     * @param  data  plot data
     * @param  xBounds   bounds on the X axis for assessment
     * @param  autoWidth  true iff you want the bin width to be (re)calculated
     *         automatically by this routine
     */
    private void calculateMaxCount( PlotData data, double[] xBounds,
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
            logBinModel_.setValue( Double.valueOf( bwLog ) );

            double gap = ( xBounds[ 1 ] - xBounds[ 0 ] ) / DEFAULT_BINS;
            assert gap > 0.0;
            Class<?> clazz = getPointSelectors().getMainSelector()
                            .getAxesSelector().getData().getColumnInfo( 0 )
                            .getContentClass();
            double bwLinear = Rounder.LINEAR.round( gap );
            if ( clazz == Byte.class ||
                 clazz == Short.class ||
                 clazz == Integer.class ||
                 clazz == Long.class ) {
                gap = Math.ceil( gap );
            }
            linearBinModel_.setValue( Double.valueOf( bwLinear ) );
        }
        double bwLog = ((Number) logBinModel_.getValue()).doubleValue();
        double bwLinear = ((Number) linearBinModel_.getValue()).doubleValue();

        /* Acquire empty binned data objects, one normalised one not. */
        int nset = data.getSetCount();
        boolean xlog = getLogModels()[ 0 ].isSelected();
        boolean offset = offsetSelector_.isSelected();
        final double binBase;
        final double binWidth;
        if ( xlog ) {
            binBase = offset ? 1.0 : Math.sqrt( bwLog );
            binWidth = bwLog;
        }
        else {
            binBase = offset ? bwLinear / 2.0 : 0.0;
            binWidth = bwLinear;
        }
        MapBinnedData.BinMapper<Long> mapper =
            MapBinnedData.createBinMapper( xlog, binWidth, binBase );
        BinnedData binned = mapper.createBinnedData( nset );
        BinnedData binnedNorm =
            new NormalisedBinnedData( mapper.createBinnedData( nset ) );

        /* Work out the X bounds. */
        double xlo = mapper.getBounds( mapper.getKey( xBounds[ 0 ] ) )[ 0 ];
        double xhi = mapper.getBounds( mapper.getKey( xBounds[ 1 ] ) )[ 1 ];

        /* Populate the binned data objects. */
        boolean[] setFlags = new boolean[ nset ];
        PointSequence pseq = data.getPointSequence();
        while ( pseq.next() ) {
            double[] coords = pseq.getPoint();
            double x = coords[ 0 ];
            double w = coords[ 1 ];
            if ( x >= xlo && x <= xhi && ! Double.isNaN( w ) ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = pseq.isIncluded( is );
                }
                binned.submitDatum( x, w, setFlags );
                binnedNorm.submitDatum( x, w, setFlags );
            }
        }
        pseq.close();

        /* Find the bin count ranges. */
        Range yRange = new Range();
        Range yRangeCum = new Range();
        {
            double[] ytots = new double[ nset ];
            for ( Iterator<BinnedData.Bin> binIt =
                      binned.getBinIterator( false );
                  binIt.hasNext(); ) {
                BinnedData.Bin bin = binIt.next();
                for ( int is = 0; is < nset; is++ ) {
                    double s = bin.getWeightedCount( is );
                    ytots[ is ] += s;
                    yRange.submit( s );
                    yRangeCum.submit( ytots[ is ] );
                }
            }
        }
        Range yRangeNorm = new Range();
        Range yRangeCumNorm = new Range();
        {
            double[] yntots = new double[ nset ];
            for ( Iterator<BinnedData.Bin> binIt =
                      binnedNorm.getBinIterator( false );
                  binIt.hasNext(); ) {
                BinnedData.Bin bin = binIt.next();
                for ( int is = 0; is < nset; is++ ) {
                    double s = bin.getWeightedCount( is );
                    yntots[ is ] += s;
                    yRangeNorm.submit( s );
                    yRangeCumNorm.submit( yntots[ is ] );
                }
            }
        }

        /* Store results for later calculations. */
        autoLinearBinWidth_ = bwLinear;
        autoLogBinWidth_ = bwLog;
        autoYRange_ = yRange;
        autoYRangeNorm_ = yRangeNorm;
        autoYRangeCum_ = yRangeCum;
        autoYRangeCumNorm_ = yRangeCumNorm;
    }

    /**
     * Returns the binned data in the form of a StarTable suitable for
     * saving or otherwise exporting.
     *
     * @return   table representing the current histogram
     */
    private StarTable getBinDataTable() {
        Histogram plot = (Histogram) getPlot();

        /* Get the binned data object from the last plotted histogram. */
        final BinnedData binData = plot.getBinnedData();

        /* Is it a weighted sum or a simple count? */
        HistogramPlotState state = (HistogramPlotState) plot.getState();
        final boolean isCumulative = state.getCumulative();
        final boolean isInt = binData.isInteger();

        /* Get the list of set IDs which describes which table/subset pairs
         * each of the sets in the binned data represents. */
        SetId[] setIds = getPointSelectors().getPointSelection().getSetIds();
        final int nset = setIds.length;

        /* Set up the first two columns of the output table, which are
         * lower and upper bounds for each bin. */
        final int ipre = 2;
        final ColumnInfo[] infos = new ColumnInfo[ ipre + nset ];
        int icol = 0;
        infos[ icol++ ] =
            new ColumnInfo( "LOW", Double.class, "Bin lower bound" );
        infos[ icol++ ] =
            new ColumnInfo( "HIGH", Double.class, "Bin upper bound" );
        assert icol == ipre;

        /* Add a new column for each of the different subsets represented
         * in the histogram. */
        SetNamer namer = createSetNamer( setIds );
        for ( int is = 0; is < nset; is++ ) {
            SetId setId = setIds[ is ];
            ColumnInfo weightInfo = getWeightInfo( setId );
            TopcatModel tcModel = setId.getPointSelector().getTable();
            List<String> descripWords = new ArrayList<String>();
            if ( state.getNormalised() ) {
                descripWords.add( "normalised" );
            }
            if ( isCumulative ) {
                descripWords.add( "cumulative" );
            }
            descripWords.add( "count" );
            String word1 = descripWords.remove( 0 );
            StringBuffer descrip = new StringBuffer();
            descrip.append( Character.toUpperCase( word1.charAt( 0 ) ) )
                   .append( word1.substring( 1 ) )
                   .append( ' ' );
            for ( String word : descripWords ) {
                descrip.append( word )
                       .append( ' ' );
            }
            if ( weightInfo != null ) {
                descrip.append( "weighted by " )
                       .append( weightInfo.getName() )
                       .append( ' ' );
            }
            RowSubset rset = tcModel.getSubsets().get( setId.getSetIndex() );
            if ( rset != RowSubset.ALL ) {
                descrip.append( "for row subset " )
                       .append( rset.getName() );
            }
            descrip.append( "in table " + tcModel.getLabel() );
            ColumnInfo colInfo =
                new ColumnInfo( namer.getName( setId ), 
                                isInt ? Integer.class : Double.class,
                                descrip.toString() );
            if ( weightInfo != null ) {
                colInfo.setUnitString( weightInfo.getUnitString() );
            }
            infos[ icol++ ] = colInfo;
        }
        assert icol == infos.length;

        /* Construct and return a non-random table which gets its data
         * from the BinnedData object. */
        double[] xrange = state.getRanges()[ 0 ];
        final double xlo = xrange[ 0 ];
        final double xhi = xrange[ 1 ];
        AbstractStarTable binsTable = new AbstractStarTable() {
            public int getColumnCount() {
                return infos.length;
            }
            public long getRowCount() {
                return -1L;
            }
            public ColumnInfo getColumnInfo( int icol ) {
                return infos[ icol ];
            }
            public RowSequence getRowSequence() {
                final Iterator<BinnedData.Bin> binIt =
                    binData.getBinIterator( true );
                final double[] sums = new double[ nset ];
                return new RowSequence() {
                    private Object[] currentRow_;

                    public boolean next() {
                        while ( binIt.hasNext() ) {
                            BinnedData.Bin bin = binIt.next();
                            if ( bin.getHighBound() > xlo &&
                                 bin.getLowBound() < xhi ) {
                                currentRow_ = getRow( bin );
                                return true;
                            }
                        }
                        currentRow_ = null;
                        return false;
                    }
                    public Object[] getRow() {
                        return currentRow_;
                    }
                    public Object getCell( int icol ) {
                        return currentRow_[ icol ];
                    }
                    public void close() {
                    }

                    /**
                     * Turns a bin into a row of the exported table.
                     *
                     * @param  bin
                     * @return  table row
                     */
                    private Object[] getRow( BinnedData.Bin bin ) {
                        Object[] row = new Object[ ipre + nset ];
                        int icol = 0;
                        row[ icol++ ] = Double.valueOf( bin.getLowBound() );
                        row[ icol++ ] = Double.valueOf( bin.getHighBound() );
                        for ( int iset = 0; iset < nset; iset++ ) {
                            double sum = bin.getWeightedCount( iset );
                            if ( isCumulative ) {
                                sums[ iset ] += sum;
                                sum = sums[ iset ];
                            }
                            assert ( ! isInt ) || ( sum == (int) sum ) : sum;
                            row[ icol++ ] =
                                isInt ? Integer.valueOf( (int) sum )
                                      : Double.valueOf( sum );
                        }
                        return row;
                    }

                };
            }
        };
        binsTable.setName( "Histogram" );
        return binsTable;
    }

    /**
     * Returns the column metadata associated with the weighting axis,
     * if there is weighting.  If no weighting is used, null is returned.
     *
     * @param  setId  set identifier
     * @return  non-trivial weighting column metadata, or null
     */
    private static final ColumnInfo getWeightInfo( SetId setId ) {
        AxesSelector axsel = setId.getPointSelector().getAxesSelector();
        if ( axsel instanceof WeightedAxesSelector ) {
            WeightedAxesSelector waxsel = (WeightedAxesSelector) axsel;
            return waxsel.hasWeights()
                ?  ((ColumnData) waxsel.getWeightSelector().getSelectedItem())
                                       .getColumnInfo()
                : null;
        }
        else {
            assert false;
            return null;
        }
    }

    /**
     * Returns an object which can provide sensible labels for elements of
     * a set of SetId objects.  Depending on what similarities/differences
     * these have, the choice about how to label them best (compactly)
     * will be different.
     *
     * @param  setIds  array of objects which needs to be distinguished
     * @param  base   stem of name
     * @return  suitable setId namer object
     */
    private SetNamer createSetNamer( SetId[] setIds ) {
        int nset = setIds.length;

        /* Only one set - call it anything. */
        if ( nset == 1 ) {
            return new SetNamer() {
                String getName( SetId setId ) {
                    return getBaseName( setId );
                }
            };
        }

        /* Multiple sets: need some distinguishing names. */
        else {

            /* Work out whether they all have the same table and/or all 
             * represent the ALL subset. */
            boolean multiTable = false;
            boolean multiSet = false;
            TopcatModel table0 = setIds[ 0 ].getPointSelector().getTable();
            for ( int is = 0; is < nset; is++ ) {
                multiTable = multiTable
                    || setIds[ is ].getPointSelector().getTable() != table0;
                multiSet = multiSet || setIds[ is ].getSetIndex() != 0;
            }
            final boolean useTable = multiTable;
            final boolean useSet = multiSet;

            /* Return a namer which names according to table or subset or both,
             * as appropriate. */
            return new SetNamer() {
                String getName( SetId setId ) {
                    TopcatModel tcModel = setId.getPointSelector().getTable();
                    StringBuffer sbuf = new StringBuffer();
                    if ( useTable ) {
                        sbuf.append( "t" );
                        sbuf.append( tcModel.getID() );
                    }
                    if ( useTable && useSet ) {
                        sbuf.append( '_' );
                    }
                    if ( useSet ) {
                        RowSubset rset =
                            tcModel.getSubsets().get( setId.getSetIndex() );
                        sbuf.append( rset.getName() );
                    }
                    ColumnInfo weightInfo = getWeightInfo( setId );
                    sbuf.append( '_' )
                        .append( getBaseName( setId ) );
                    return sbuf.toString();
                }
            };
        }
    }

    /**
     * Interface describing an object which can give names to SetId objects.
     */
    private abstract class SetNamer {

        /**
         * Returns a good name for a SetId.
         *
         * @param  setId   object to label
         * @param  compact human-readable name
         */
        abstract String getName( SetId setId );

        /**
         * Returns the basic name (unmodified by set/table identifier) for
         * the counts column of a given dataset.
         *
         * @param   setId  dataset identifier
         * @return   basic count column name
         */
        String getBaseName( SetId setId ) {
            ColumnInfo weightInfo = getWeightInfo( setId );
            return weightInfo == null ? "COUNT"
                                      : "SUM_" + weightInfo.getName();
        }
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
            Points points = getPoints();
            if ( state.getValid() && points != null ) {
                PlotData data = ((PointSelection) state.getPlotData())
                               .createPlotData( points );

                /* Do X scaling if required. */
                double[] xBounds;
                if ( scaleX_ ) {
                    Range xRange = HistogramWindow.super
                                  .calculateRanges( data, state )[ 0 ];
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
                    calculateMaxCount( data, xBounds, scaleX_ );
                    getViewRanges()[ 1 ].clear();
                    getAxisWindow().getEditors()[ 1 ].clearBounds();
                }
                replot();
            }
        }
    }
}
