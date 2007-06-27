package uk.ac.starlink.topcat.plot;

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
import uk.ac.starlink.table.DefaultValueInfo;
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
    private final ToggleButtonModel weightModel_;
    private final JCheckBox offsetSelector_;
    private final RoundingSpinner binSizer_;
    private final RoundingSpinner.RoundingSpinnerModel linearBinModel_;
    private final RoundingSpinner.RoundingSpinnerModel logBinModel_;

    private double autoYMax_;
    private double autoYMaxCumulative_;
    private double autoLinearBinWidth_;
    private double autoLogBinWidth_;

    private static final int DEFAULT_BINS = 20;

    /** Description of vertical plot axis. */
    private final static ValueInfo COUNT_INFO = 
        new DefaultValueInfo( "Count", Integer.class,
                              "Number of values in bin" );
    private final static ValueInfo WEIGHT_INFO =
        new DefaultValueInfo( "Weighted count", Double.class,
                              "Weighted sum of values in bin" );

    /**
     * Constructs a new histogram window.
     *
     * @param  parent  parent component (may be used for positioning)
     */
    public HistogramWindow( Component parent ) {
        super( "Histogram", new String[] { "X" }, 0,
               new ErrorModeSelectionModel[ 0 ], parent );

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
            new ToggleButtonModel( "Weighted Counts", ResourceIcon.WEIGHT,
                                   "Allow weighting of histogram counts" );
        weightModel_.addActionListener( getReplotListener() );

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
        plotMenu.add( weightModel_.createMenuItem() );
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
        getToolBar().add( weightModel_.createToolbarButton() );
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
                            ValueInfo axinfo = HistogramWindow.this.hasWeights()
                                             ? WEIGHT_INFO
                                             : COUNT_INFO;
                            yaxed.setAxis( axinfo );
                        }
                    } );
                }
            }
        };
        weightModel_.addActionListener( weightAction );

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
                ValueInfo axinfo = HistogramWindow.this.hasWeights()
                                 ? WEIGHT_INFO
                                 : COUNT_INFO;
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

            /* See if there is any weighting on the Y axis. */
            state.setWeighted( hasWeights() );
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

    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        Range xRange = super.calculateRanges( pointSelection, points )[ 0 ];
        boolean xlog = getLogModels()[ 0 ].isSelected();
        double[] xBounds = xRange.getFiniteBounds( xlog );
        calculateMaxCount( pointSelection, points, xBounds, true );
        return new Range[] { xRange };
    }

    public Rectangle getPlotBounds() {
        Rectangle bounds =
            new Rectangle( plot_.getSurface().getClip().getBounds() );
        bounds.y--;
        bounds.height += 2;
        return bounds;
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
            Class clazz = getPointSelectors().getMainSelector()
                         .getAxesSelector().getData().getColumnInfo( 0 )
                         .getContentClass();
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
            double w = coords[ 1 ];
            if ( x >= xlo && x <= xhi ) {
                for ( int is = 0; is < nset; is++ ) {
                    setFlags[ is ] = rsets[ is ].isIncluded( lp );
                }
                binned.submitDatum( x, w, setFlags );
            }
        }

        /* Find the highest bin count. */
        double[] ymaxes = new double[ nset ];
        double[] ytots = new double[ nset ];
        for ( Iterator binIt = binned.getBinIterator( false );
              binIt.hasNext(); ) {
            BinnedData.Bin bin = (BinnedData.Bin) binIt.next();
            for ( int is = 0; is < nset; is++ ) {
                double s = bin.getWeightedCount( is );
                ytots[ is ] += s;
                ymaxes[ is ] = Math.max( ymaxes[ is ], s );
            }
        }
        double yMax = 0;
        double yMaxTot = 0;
        for ( int is = 0; is < nset; is++ ) {
            yMax = Math.max( yMax, ymaxes[ is ] );
            yMaxTot = Math.max( yMaxTot, ytots[ is ] );
        }

        /* Store results for later calculations. */
        autoYMax_ = yMax * ( 1 + getPadRatio() );
        autoLinearBinWidth_ = bwLinear;
        autoLogBinWidth_ = bwLog;
        autoYMaxCumulative_ = yMaxTot * ( 1 + getPadRatio() );
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
     * Returns the binned data in the form of a StarTable suitable for
     * saving or otherwise exporting.
     *
     * @return   table representing the current histogram
     */
    private StarTable getBinDataTable() {

        /* Get the binned data object from the last plotted histogram. */
        final BinnedData binData = plot_.getBinnedData();

        /* Is it a weighted sum or a simple count? */
        HistogramPlotState state = (HistogramPlotState) plot_.getState();
        final boolean weighted = state.getWeighted();

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
            StringBuffer descrip = new StringBuffer();
            descrip.append( "Count " );
            if ( weightInfo != null ) {
                descrip.append( "weighted by " )
                       .append( weightInfo.getName() )
                       .append( " " );
            }
            RowSubset rset =
                (RowSubset) tcModel.getSubsets().get( setId.getSetIndex() );
            if ( rset != RowSubset.ALL ) {
                descrip.append( "for row subset " )
                       .append( rset.getName() )
                       .append( ' ' );
            }
            descrip.append( "in table " )
                   .append( tcModel.getLabel() );
            ColumnInfo colInfo =
                new ColumnInfo( namer.getName( setId ), 
                                weighted ? Double.class : Integer.class,
                                descrip.toString() );
            if ( weightInfo != null ) {
                colInfo.setUnitString( weightInfo.getUnitString() );
            }
            infos[ icol++ ] = colInfo;
        }
        assert icol == infos.length;

        /* Construct and return a non-random table which gets its data
         * from the BinnedData object. */
        AbstractStarTable binsTable = new AbstractStarTable() {
            public int getColumnCount() {
                return infos.length;
            }
            public long getRowCount() {
                return -1L;
            }
            public ColumnInfo getColumnInfo( int icol ) {
                return (ColumnInfo) infos[ icol ];
            }
            public RowSequence getRowSequence() {
                final Iterator binIt = binData.getBinIterator( true );
                Iterator rowIt = new Iterator() {
                    public boolean hasNext() {
                        return binIt.hasNext();
                    }
                    public Object next() {
                        BinnedData.Bin bin = (BinnedData.Bin) binIt.next();
                        Object[] row = new Object[ ipre + nset ];
                        int icol = 0;
                        row[ icol++ ] = new Double( bin.getLowBound() );
                        row[ icol++ ] = new Double( bin.getHighBound() );
                        for ( int iset = 0; iset < nset; iset++ ) {
                            double sum = bin.getWeightedCount( iset );
                            assert weighted || ( sum == (int) sum ) : sum;
                            row[ icol++ ] =
                                weighted ? (Number) new Double( sum )
                                         : (Number) new Integer( (int) sum );
                        }
                        return row;
                    }
                    public void remove() {
                        binIt.remove();
                    }
                };
                return new IteratorRowSequence( rowIt );
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
                        RowSubset rset = (RowSubset)
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
