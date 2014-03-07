package uk.ac.starlink.topcat.plot2;

import java.util.Arrays;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneNavigator;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;

/**
 * Axis control for histogram window.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2014
 */
public class HistogramAxisController
        extends CartesianAxisController<PlaneSurfaceFactory.Profile,
                                        PlaneAspect> {

    /** Keys to control common bar configuration for all histogram layers. */
    private static final ConfigKey[] BAR_KEYS = new ConfigKey[] {
        BinSizer.BINSIZER_KEY,
        HistogramPlotter.PHASE_KEY,
        HistogramPlotter.CUMULATIVE_KEY,
        HistogramPlotter.NORM_KEY,
    };

    /**
     * Constructor.
     *
     * @param  stack  control stack
     */
    public HistogramAxisController( ControlStack stack ) {
        super( new HistogramSurfaceFactory(),
               PlaneAxisController.createAxisLabelKeys(), stack );
        SurfaceFactory surfFact = getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.XLOG_KEY,
            PlaneSurfaceFactory.YLOG_KEY,
            PlaneSurfaceFactory.XFLIP_KEY,
            PlaneSurfaceFactory.YFLIP_KEY,
            PlaneSurfaceFactory.XYFACTOR_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) );

        /* Grid tab. */
        mainControl.addSpecifierTab( "Grid",
                                     new ConfigSpecifier( new ConfigKey[] {
            PlaneSurfaceFactory.GRID_KEY,
            StyleKeys.GRID_COLOR,
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.MINOR_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
            StyleKeys.GRID_ANTIALIAS,
        } ) );

        /* Labels tab. */
        addLabelsTab();
        AutoSpecifier<String> ySpecifier = 
            getLabelSpecifier()
           .getAutoSpecifier( PlaneSurfaceFactory.YLABEL_KEY );
        ySpecifier.setAuto( false );
        ySpecifier.setSpecifiedValue( null );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );


        /* Bars control. */
        ConfigControl barControl =
            new ConfigControl( "Bars", ResourceIcon.HISTOBARS );
        barControl.addSpecifierTab( "Bars", new ConfigSpecifier( BAR_KEYS ) );
        addControl( barControl );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    protected boolean logChanged( PlaneSurfaceFactory.Profile prof1,
                                  PlaneSurfaceFactory.Profile prof2 ) {
        return ! Arrays.equals( prof1.getLogFlags(), prof2.getLogFlags() );
    }

    @Override
    protected boolean clearRange( PlaneSurfaceFactory.Profile oldProfile,
                                  PlaneSurfaceFactory.Profile newProfile,
                                  PlotLayer[] oldLayers, PlotLayer[] newLayers,
                                  boolean lock ) {
        if ( super.clearRange( oldProfile, newProfile, oldLayers, newLayers,
                               lock ) ) {
            return true;
        }
        else if ( lock ) {
            return false;
        }

        /* Look at the bar style changes which ought to cause a re-range.
         * If any of them have changed between all the old and new layers,
         * it's a global change which is worth a re-range. */
        else {
            BarState state0 = getBarState( oldLayers );
            BarState state1 = getBarState( newLayers );
            if ( state0 == null && state1 == null ) {
                return false;
            }
            else if ( state0 == null || state1 == null ) {
                return true;
            }
            else {
                if ( state0.isCumulative_ != state1.isCumulative_ ||
                     state0.isNorm_ != state1.isNorm_ ) {
                    return true;
                }
                else if ( ! state0.sizer_.equals( state1.sizer_ ) &&
                          ! state0.isCumulative_ ) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
    }

    /**
     * Returns the common state of the histogram layers concerning
     * how the bars are laid out.
     *
     * @param  layers   plot layers
     * @return  bar state if one can be determined, null if no histogram layers
     */
    private static BarState getBarState( PlotLayer[] layers ) {

        /* Just find one histogram layer and use the state from that.
         * For now that is sufficient, since there is no mechanism to
         * modify these config items per layer in this plot. */
        for ( int il = 0; il < layers.length; il++ ) {
            Style style = layers[ il ].getStyle();
            if ( style instanceof HistogramPlotter.HistoStyle ) {
                HistogramPlotter.HistoStyle hstyle =
                    (HistogramPlotter.HistoStyle) style;
                BinSizer sizer = hstyle.getBinSizer();
                boolean cumul = hstyle.isCumulative();
                boolean norm = hstyle.isNormalised();
                return new BarState( sizer, cumul, norm );
            }
        }
        return null;
    }

    /**
     * Surface factory for histogram.
     */
    private static class HistogramSurfaceFactory extends PlaneSurfaceFactory {
        
        private static final ConfigKey<Boolean> HIST_XANCHOR_KEY =
            createAxisAnchorKey( "X", true );
        private static final ConfigKey<Boolean> HIST_YANCHOR_KEY =
            createAxisAnchorKey( "Y", false );

        @Override
        public ConfigKey[] getNavigatorKeys() {
            return new ConfigKey[] {
                NAVAXES_KEY,
                HIST_XANCHOR_KEY,
                HIST_YANCHOR_KEY,
                StyleKeys.ZOOM_FACTOR,
            };
        }

        @Override
        public Navigator<PlaneAspect> createNavigator( ConfigMap navConfig ) {
            double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
            boolean[] navFlags = navConfig.get( NAVAXES_KEY );
            boolean xnav = navFlags[ 0 ];
            boolean ynav = navFlags[ 1 ];
            double xAnchor = navConfig.get( HIST_YANCHOR_KEY ) ? 0.0
                                                               : Double.NaN;
            double yAnchor = navConfig.get( HIST_XANCHOR_KEY ) ? 0.0
                                                               : Double.NaN;
            return new PlaneNavigator( zoom, xnav, ynav, xnav, ynav,
                                       xAnchor, yAnchor );
        }
    }

    /**
     * Characterises how histogram bars are laid out on the plot surface.
     */
    private static class BarState {
        final BinSizer sizer_;
        final boolean isCumulative_;
        final boolean isNorm_;
        BarState( BinSizer sizer, boolean isCumulative, boolean isNorm ) {
            sizer_ = sizer;
            isCumulative_ = isCumulative;
            isNorm_ = isNorm;
        }
    }
}
