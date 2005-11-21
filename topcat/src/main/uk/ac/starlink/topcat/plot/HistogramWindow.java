package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.Action;
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
    private final RoundingSpinner binSizer_;

    private double binWidth_;
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
        super( "Histogram", new String[] { "X" }, parent );

        /* Create the histogram plot itself.  Being a histogram, there's
         * no point zooming in such a way that the Y axis goes below zero,
         * so block that. */
        plot_ = new Histogram( new PtPlotSurface( this ) {
            void _setYRange( double min, double max ) {
                super._setYRange( Math.max( getMinYValue(), min ), max );
            }
        } );

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
        yLogModel_.addActionListener( getReplotAction() );

        /* Bin size selector widget. */
        binSizer_ = new RoundingSpinner();
        getLogModels()[ 0 ].addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                binSizer_.setLogarithmic( getLogModels()[ 0 ].isSelected() );
            }
        } );
        binSizer_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                getReplotAction()
               .actionPerformed( new ActionEvent( evt.getSource(), 0, null ) ); 
            }
        } );
        binSizer_.setBorder( makeTitledBorder( "Bin Widths" ) );
        getMainArea().add( binSizer_, java.awt.BorderLayout.SOUTH );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( rescaleActionXY );
        plotMenu.add( rescaleActionX );
        plotMenu.add( rescaleActionY );
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
                subsetFromVisible();
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
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getFlipModels()[ 0 ].createToolbarButton() );
        getToolBar().add( getLogModels()[ 0 ].createToolbarButton() );
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

        /* Render component visible. */
        pack();
        setVisible( true );
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected void doReplot( PlotState pstate, Points points ) {
        HistogramPlotState state = (HistogramPlotState) pstate;
        HistogramPlotState lastState = (HistogramPlotState) plot_.getState();
        plot_.setPoints( points );
        plot_.setState( state );
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {

            /* Calculate bin width here if it needs to be done.  We can't
             * do it when the plot state is initialised, since at that
             * point we don't have the Points data, and that is needed
             * to work out the range of the data and hence how wid the 
             * bins are going to be. */
            if ( state.getValid() ) {
                double bw = autoBinWidth( state, points );
                state.setBinWidth( bw );
                setBinWidth( bw );
            }

            /* Rescale if axes have changed. */
            plot_.rescale();
        }

        /* If the bin width has changed, modify the Y axis by a multiplier.
         * This isn't essential, but it means that the Y axis range covers
         * approximately the same data points rather than creeping up/down
         * with a change in the bin width. */
        else if ( state.getBinWidth() != lastState.getBinWidth() ) {
            plot_.scaleYFactor( state.getBinWidth() / lastState.getBinWidth() );
        }

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

    protected PlotState createPlotState() {
        HistogramPlotState state = new HistogramPlotState();
        double bw = getBinWidth();
        if ( bw > 0 ) {
            state.setBinWidth( bw );
        }
        return state;
    }

    public PlotState getPlotState() {
        PlotState state = super.getPlotState();
        boolean valid = state != null && state.getValid();

        /* The state obtained from the superclass implementation has
         * purely 1-d attributes, since the histogram data model 1-d plot.
         * However the plot itself is on a 2-d plotting surface, 
         * so modify some of the state to contain axis information here. */
        if ( valid ) {
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
        }

        /* Configure some actions to be enabled/disabled according to 
         * whether the plot state is valid. */
        for ( int i = 0; i < validityActions_.length; i++ ) {
            validityActions_[ i ].setEnabled( valid );
        }

        /* Return the state. */
        return state;
    }

    private void subsetFromVisible() {
        addNewSubsets( plot_.getVisiblePoints() );
    }

    /**
     * Sets the bin width explicitly.
     *
     * @param  bw  new bin width value
     */
    private void setBinWidth( double bw ) {
        binSizer_.setNumericValue( bw );
    }

    /**
     * Returns the currently set bin width.
     *
     * @return  bin width
     */
    private double getBinWidth() {
        return binSizer_.getNumericValue();
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
     * Calculates a sensible bin width for a given plot state and point set.
     * The returned value is additive for linear X axis and multiplicative
     * for logarithmic.
     *
     * @param  state   plot state
     * @param  points  point set
     * @return   reasonable bin width
     */
    private static double autoBinWidth( PlotState state, Points points ) {
        double[] range = getXRange( state, points );
        if ( state.getLogFlags()[ 0 ] ) {
            double factor = Math.exp( Math.log( range[ 1 ] / range[ 0 ] ) 
                                      / DEFAULT_BINS );
            return Rounder.LOG.round( factor );
        }
        else {
            double gap = ( range[ 1 ] - range[ 0 ] ) / DEFAULT_BINS;
            if ( gap < 1.0 ) {
                Class clazz = state.getAxes()[ 0 ].getContentClass();
                if ( clazz == Byte.class ||
                     clazz == Short.class ||
                     clazz == Integer.class ||
                     clazz == Long.class ) {
                    return 1.0;
                }
            }
            return Rounder.LINEAR.round( gap );
        }
    }

    public static double[] getXRange( PlotState state, Points points ) {
        boolean xlog = state.getLogFlags()[ 0 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        int nok = 0;
        int np = points.getCount();
        RowSubset[] rsets = state.getPointSelection().getSubsets();
        int nset = rsets.length;
        double[] coords = new double[ 1 ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset; is++ ) {
                if ( rsets[ is ].isIncluded( lp ) ) {
                    use = true;
                    break;
                }
            }
            if ( use ) {
                points.getCoords( ip, coords );
                double xp = coords[ 0 ];
                if ( ! Double.isNaN( xp ) && ! Double.isInfinite( xp ) &&
                     ( ! xlog || xp > 0.0 ) ) {
                    nok++;
                    if ( xp < xlo ) {
                        xlo = xp;
                    }
                    if ( xp > xhi ) {
                        xhi = xp;
                    }
                } 
            }
        }
        return nok > 0 ? new double[] { xlo, xhi }
                       : ( xlog ? new double[] { 0., 1. }
                                : new double[] { 1., 2. } );
    }

    /**
     * Actions for rescaling the plot in X and/or Y directions to fit the
     * data.
     */
    private class RescaleAction extends BasicAction {
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
            plot_.rescale( scaleX_, scaleY_ );
            forceReplot();
        }
    }

}
