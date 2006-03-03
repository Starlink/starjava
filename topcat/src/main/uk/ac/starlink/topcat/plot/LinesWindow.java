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
import uk.ac.starlink.topcat.TopcatModel;

/**
 * GraphicsWindow which draws a stack of line graphs.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesWindow extends GraphicsWindow {

    private final LinesPlot plot_;

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
        double[][] yRanges = new double[ nsel ][];
        List pselList = new ArrayList( nsel );
        for ( int isel = 0; isel < nsel; isel++ ) {
            PointSelector psel = pointSelectors.getSelector( isel );
            pselList.add( psel );
            yRanges[ isel ] = new double[] { Double.NaN, Double.NaN };
            if ( psel.isValid() ) {
                yAxes[ isel ] = psel.getData().getColumnInfo( 1 );
            }
        }
        state.setYAxes( yAxes );
        state.setYRanges( yRanges );

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

        /* Return state. */
        return state;
    }

    protected PointSelector createPointSelector() {

        /* Create the new point selector. */
        DefaultPointSelector newSelector =
            new DefaultPointSelector( new String[] { "X", "Y" }, null );
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
        return new MarkStyleEditor( true, false );
    }

    public StyleSet getDefaultStyles( int npoint ) {
        final MarkStyle lineStyle = 
            (MarkStyle) MarkStyles.points( null ).getStyle( 0 );
        lineStyle.setColor( Color.BLACK );
        boolean many = npoint > 10000;
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
}
