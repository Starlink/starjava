package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
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
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * GraphicsWindow which presents one-dimensional data as a histogram.
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public class HistogramWindow extends GraphicsWindow {

    private final Histogram plot_;
    private final Action[] validityActions_;

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

        /* Create and the histogram plot itself.  Being a histogram, there's
         * no point zooming in such a way that the Y axis goes below zero,
         * so block that. */
        plot_ = new Histogram( new PtPlotSurface( this ) {
            void _setYRange( double min, double max ) {
                super._setYRange( Math.max( 0.0, min ), max );
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

    protected void doReplot( PlotState state, Points points ) {
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( state );
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {
            plot_.rescale();
        }
        plot_.repaint();
    }

    public StyleSet getDefaultStyles( int npoint ) {
        return BarStyles.sideFilled( "Filled Adjacent" );
    }

    public StyleSet[] getStyleSets() {
        return new StyleSet[] {
            BarStyles.sideFilled( "Filled Adjacent" ),
            BarStyles.sideOpen( "Open Adjacent" ),
            BarStyles.filled( "Filled Overplot" ),
            BarStyles.open( "Open Overplot" ),
            BarStyles.spikes( "Spikes" ),
            BarStyles.tops( "Tops" ),
        };
    }

    public PlotState getPlotState() {
        PlotState state = super.getPlotState();
        boolean valid = state != null && state.getValid();
        if ( valid ) {
            state.setAxes( new ValueInfo[] { state.getAxes()[ 0 ],
                                             COUNT_INFO } );
            state.setLogFlags( new boolean[] { state.getLogFlags()[ 0 ],
                                               false } );
            state.setFlipFlags( new boolean[] { state.getFlipFlags()[ 0 ],
                                                false } );
        }
        for ( int i = 0; i < validityActions_.length; i++ ) {
            validityActions_[ i ].setEnabled( valid );
        }
        return state;
    }

    private void subsetFromVisible() {
        addNewSubsets( plot_.getVisiblePoints() );
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
