package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.convert.ValueConverter;

/**
 * GraphicsWindow which draws a stack of line graphs.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesWindow extends GraphicsWindow {

    private final LinesPlot plot_;
    private final ToggleButtonModel antialiasModel_;
    private Range[] yDataRanges_;
    private final Map yViewRangeMap_;

    private StyleSet styles_;

    private final static Color[] COLORS;
    static {
        COLORS = new Color[ Styles.COLORS.length + 1 ];
        COLORS[ 0 ] = Color.BLACK;
        System.arraycopy( Styles.COLORS, 0, COLORS, 1, Styles.COLORS.length );
    }

    /**
     * Constructor.
     *
     * @param   parent  parent component
     */
    public LinesWindow( Component parent ) {
        super( "Line Plot", new String[] { "X", "Y" }, parent );

        yViewRangeMap_ = new HashMap();
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

        getMainArea().add( plot_, BorderLayout.CENTER );
        
        getPointSelectors().addActionListener( new AxisWindowUpdater() );

        /* Add a status line reporting on cursor position. */
        JComponent posLabel = plot_.createPositionLabel();
        posLabel.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                posLabel.getMaximumSize()
                                                        .height ) );
        getStatusBox().add( posLabel );
        getStatusBox().add( Box.createHorizontalGlue() );

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

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Populate toolbar. */
        getToolBar().add( rescaleActionXY );
        getToolBar().add( rescaleActionX );
        getToolBar().add( rescaleActionY );
        getToolBar().add( getAxisEditAction() );
        getToolBar().add( getReplotAction() );
        getToolBar().add( antialiasModel_.createToolbarButton() );
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
        return npoint < 2000 ? MarkStyles.lines( "lines", COLORS )
                             : MarkStyles.points( "points", COLORS );
    }

    /**
     * Returns a 1-element array giving only the X axis range.
     */
    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        Range[] xyRanges = calculateRanges( pointSelection, points, null );
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
            ranges[ igraph + 1 ] = yRanges[ igraph ];
        }
        return ranges;
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
               .setModel( new ColumnDataComboBoxModel( tcModel, true, true ) );
            }
        }

        protected void initialiseSelectors() {

            /* X axis gets magic 'index' column. */
            getColumnSelector( 0 ).setSelectedIndex( 1 );

            /* Y axis gets first non-index column. */
            getColumnSelector( 1 ).setSelectedIndex( 1 );
        }
    }
}
