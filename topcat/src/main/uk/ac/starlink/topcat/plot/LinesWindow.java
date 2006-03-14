package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * GraphicsWindow which draws a stack of line graphs.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesWindow extends GraphicsWindow {

    private final LinesPlot plot_;
    private final ToggleButtonModel antialiasModel_;

    /**
     * Constructor.
     *
     * @param   parent  parent component
     */
    public LinesWindow( Component parent ) {
        super( "Line Plot", new String[] { "X", "Y" }, parent );

        plot_ = new LinesPlot();
        plot_.setPreferredSize( new Dimension( 400, 400 ) );

        getMainArea().add( plot_, BorderLayout.CENTER );

        antialiasModel_ = new ToggleButtonModel( "Antialias",
                                                 ResourceIcon.ANTIALIAS,
                                                 "Select whether lines are " +
                                                 "drawn with antialiasing" );
        antialiasModel_.setSelected( false );
        antialiasModel_.addActionListener( getReplotListener() );

        getToolBar().add( antialiasModel_.createToolbarButton() );
        getToolBar().add( getReplotAction() );

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
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {
            plot_.rescale();
        }
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

        /* Configure range and axis information for each plot. */
        ValueInfo[] yAxes = new ValueInfo[ nsel ];
        String[] yAxisLabels = new String[ nsel ];
        double[][] yRanges = new double[ nsel ][];
        boolean[] yLogFlags = new boolean[ nsel ];
        boolean[] yFlipFlags = new boolean[ nsel ];
        List pselList = new ArrayList( nsel );
        for ( int isel = 0; isel < nsel; isel++ ) {
            LinesPointSelector psel =
                (LinesPointSelector) pointSelectors.getSelector( isel );
            pselList.add( psel );
            yRanges[ isel ] = new double[] { Double.NaN, Double.NaN };
            if ( psel.isValid() ) {
                yAxes[ isel ] = psel.getData().getColumnInfo( 1 );
                yAxisLabels[ isel ] = yAxes[ isel ].getName();
                yLogFlags[ isel ] = psel.getYLogModel().isSelected();
                yFlipFlags[ isel ] = psel.getYFlipModel().isSelected();
            }
        }
        state.setYAxes( yAxes );
        state.setYAxisLabels( yAxisLabels );
        state.setYRanges( yRanges );
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
            new LinesPointSelector( logModel, flipModel );
        newSelector.setStyles( new PoolStyleSet( getDefaultStyles( 0 ),
                                                 new BitSet() ) );

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

    public StyleSet getDefaultStyles( int npoint ) {
        final MarkStyle lineStyle = 
            (MarkStyle) MarkStyles.points( null ).getStyle( 0 );
        lineStyle.setColor( Color.BLACK );
        boolean many = npoint > 2000;
        lineStyle.setLine( many ? null : MarkStyle.DOT_TO_DOT );
        lineStyle.setHidePoints( ! many );
        return new StyleSet() {
            public String getName() {
                return "Lines";
            }
            public Style getStyle( int index ) {
                return lineStyle;
            }
        };
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
         * @param  yLogModel   toggler for Y axis log scaling
         * @param  yFlipModel  toggler for Y axis inverted sense
         */
        LinesPointSelector( ToggleButtonModel yLogModel,
                            ToggleButtonModel yFlipModel ) {
            super( new String[] { "X", "Y" },
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
    }
}
