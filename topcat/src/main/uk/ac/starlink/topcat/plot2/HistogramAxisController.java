package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneNavigator;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.AbstractKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.Cumulation;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.Normalisation;
import uk.ac.starlink.ttools.plot2.layer.Pixel1dPlotter;

/**
 * Axis control for histogram window.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2014
 */
public class HistogramAxisController
        extends CartesianAxisController<PlaneSurfaceFactory.Profile,
                                        PlaneAspect> {

    private final BinSizer.BinSizerSpecifier binWidthSpecifier_;
    private final BinSizer.BinSizerSpecifier smoothWidthSpecifier_;
    private final JLabel histoCountLabel_;
    private final JLabel kdeCountLabel_;

    /**
     * Constructor.
     */
    public HistogramAxisController() {
        super( new HistogramSurfaceFactory(),
               PlaneAxisController.createAxisLabelKeys() );
        SurfaceFactory<PlaneSurfaceFactory.Profile,PlaneAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
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
                            new ConfigSpecifier( surfFact.getAspectKeys() ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                checkRangeSense( config, "X",
                                 PlaneSurfaceFactory.XMIN_KEY,
                                 PlaneSurfaceFactory.XMAX_KEY );
                checkRangeSense( config, "Y",
                                 PlaneSurfaceFactory.YMIN_KEY,
                                 PlaneSurfaceFactory.YMAX_KEY );
            }
        } );

        /* Grid tab. */
        List<ConfigKey<?>> gridKeyList = new ArrayList<>();
        gridKeyList.add( PlaneSurfaceFactory.GRID_KEY );
        gridKeyList.addAll( Arrays
                           .asList( StyleKeys.GRIDCOLOR_KEYSET.getKeys() ) );
        gridKeyList.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.MINOR_TICKS,
            StyleKeys.SHADOW_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
        } ) );
        ConfigKey<?>[] gridKeys = gridKeyList.toArray( new ConfigKey<?>[ 0 ] );
        mainControl.addSpecifierTab( "Grid", new ConfigSpecifier( gridKeys ) );

        /* Labels tab. */
        addLabelsTab();
        AutoSpecifier<String> ySpecifier = 
            getLabelSpecifier()
           .getAutoSpecifier( PlaneSurfaceFactory.YLABEL_KEY );
        ySpecifier.setAuto( false );
        ySpecifier.setSpecifiedValue( null );

        /* Secondary axes tab. */
        mainControl.addSpecifierTab( "Secondary",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            PlaneSurfaceFactory.X2FUNC_KEY,
            PlaneSurfaceFactory.X2LABEL_KEY,
            PlaneSurfaceFactory.Y2FUNC_KEY,
            PlaneSurfaceFactory.Y2LABEL_KEY,
        } ) );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        /* Bars control. */
        ConfigSpecifier hbarSpecifier =
                new ConfigSpecifier( new ConfigKey<?>[] {
            HistogramPlotter.BINSIZER_KEY,
            HistogramPlotter.PHASE_KEY,
        } );
        binWidthSpecifier_ =
            getSliderSpecifier( hbarSpecifier, HistogramPlotter.BINSIZER_KEY );
        assert binWidthSpecifier_ != null;
        histoCountLabel_ = new JLabel();
        addCountLabel( hbarSpecifier.getComponent(),
                       "Visible Histograms", histoCountLabel_ );
        ConfigSpecifier kbinSpecifier =
                new ConfigSpecifier( new ConfigKey<?>[] {
            Pixel1dPlotter.SMOOTHSIZER_KEY,
            Pixel1dPlotter.KERNEL_KEY,
        } );
        smoothWidthSpecifier_ =
            getSliderSpecifier( kbinSpecifier, Pixel1dPlotter.SMOOTHSIZER_KEY );
        assert smoothWidthSpecifier_ != null;
        kdeCountLabel_ = new JLabel();
        addCountLabel( kbinSpecifier.getComponent(),
                       "Visible KDEs", kdeCountLabel_ );
        ConfigSpecifier genSpecifier = new ConfigSpecifier( new ConfigKey<?>[] {
            StyleKeys.CUMULATIVE,
            StyleKeys.NORMALISE,
        } );
        ConfigControl barControl =
            new ConfigControl( "Bins", ResourceIcon.HISTOBARS );
        barControl.addSpecifierTab( "Histogram", hbarSpecifier );
        barControl.addSpecifierTab( "KDE", kbinSpecifier );
        barControl.addSpecifierTab( "General", genSpecifier );
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
                if ( state0.cumulative_ != state1.cumulative_ ||
                     state0.norm_ != state1.norm_ ) {
                    return true;
                }
                else if ( ! PlotUtil.equals( state0.sizer_, state1.sizer_ ) &&
                          ! state0.cumulative_.isCumulative() ) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
    }

    @Override
    public void submitReports( Map<LayerId,ReportMap> reports ) {
        updateBinSizerText( HistogramPlotter.BINWIDTH_KEY,
                            binWidthSpecifier_, histoCountLabel_, reports );
        updateBinSizerText( Pixel1dPlotter.SMOOTHWIDTH_KEY,
                            smoothWidthSpecifier_, kdeCountLabel_, reports );
    }

    /**
     * Updates the state of the GUI to reflect the current actual
     * bin/smoothing widths.  This is is useful visual feedback for
     * the user, but can only be known as a result of performing
     * the plot.
     *
     * @param   key  report key giving the actual width in data coordinates
     *               extracted from a BinSizer config item
     * @param   specifier   specifier from which the BinSizer value is acquired
     * @param   countLabel   label to be updated with number of layers with
     *                       the relevant type
     * @param   reports   reports obtained from doing the plot
     */
    private static void
            updateBinSizerText( ReportKey<Double> key,
                                BinSizer.BinSizerSpecifier specifier,
                                JLabel countLabel,
                                Map<LayerId,ReportMap> reports ) {

        /* See if we have a value for the actual width for the relevant
         * config item. */
        double dval = Double.NaN;
        int count = 0;
        for ( ReportMap report : reports.values() ) {
            Double value = report == null ? null : report.get( key );
            double dval0 = value == null ? Double.NaN : value.doubleValue();
            if ( ! Double.isNaN( dval0 ) ) {
                assert Double.isNaN( dval ) || dval0 == dval
                     : key + " not unique?";
                dval = dval0;
                count++;
            }
        }

        /* Report number of layers of this type. */
        countLabel.setText( Integer.toString( count ) );

        /* If so, pass it to the relevant specifier for display. */
        if ( specifier != null ) {
            ReportMap report = new ReportMap();
            report.put( key, new Double( dval ) );
            specifier.submitReport( report );
        }
    }

    /**
     * Returns a typed specifier used to acquire the value for a BinSizer
     * config item.  Null is returned if the specifier can't be found or
     * is of the wrong type.
     *
     * @param  cs  config specifier expected to hold the relevant item
     * @param  key   config key for the relevant item 
     * @return    bin sizer specifier, or null
     */
    private static BinSizer.BinSizerSpecifier
            getSliderSpecifier( ConfigSpecifier cs, ConfigKey<BinSizer> key ) {
        Specifier<BinSizer> spec = cs.getSpecifier( key );
        assert spec instanceof BinSizer.BinSizerSpecifier;
        return spec instanceof BinSizer.BinSizerSpecifier
             ? (BinSizer.BinSizerSpecifier) spec
             : null;
    }

    /**
     * Returns the common state of the histogram layers concerning
     * how the bars are laid out.
     *
     * @param  layers   plot layers
     * @return  bar state if one can be determined, null if no histogram layers
     */
    private static BarState getBarState( PlotLayer[] layers ) {

        /* Get the state from one representative layer, since at present
         * configuration of these items is per-window not per-layer.
         * There is code here to assert that this is still the case,
         * i.e. that the BarState is consistent across all layers;
         * subsequent changes to the GUI could invalidate those assertions
         * in which case we have to rethink the role of the bar state. */
        BinSizer sizer = null;
        Cumulation cumul = null;
        Normalisation norm = null;
        boolean hasBars = false;
        for ( PlotLayer layer : layers ) {
            BinSizer sizer1 = null;
            Cumulation cumul1 = null;
            Normalisation norm1 = null;
            Style style = layer.getStyle();
            final boolean layerHasBars;
            if ( style instanceof HistogramPlotter.HistoStyle ) {
                layerHasBars = true;
                HistogramPlotter.HistoStyle hstyle =
                    (HistogramPlotter.HistoStyle) style;
                sizer1 = hstyle.getBinSizer();
                cumul1 = hstyle.getCumulative();
                norm1 = hstyle.getNormalisation();
            }
            else if ( style instanceof
                      AbstractKernelDensityPlotter.KDenseStyle ) {
                layerHasBars = true;
                AbstractKernelDensityPlotter.KDenseStyle dstyle =
                    (AbstractKernelDensityPlotter.KDenseStyle) style;
                cumul1 = dstyle.getCumulative();
                norm1 = dstyle.getNormalisation();
            }
            else {
                layerHasBars = false;
            }
            if ( layerHasBars ) {
                assert sizer == null || sizer1 == null
                             || sizer.equals( sizer1 );
                assert cumul == null || cumul.equals( cumul1 );
                assert norm == null || norm.equals( norm1 );
                sizer = sizer1;
                cumul = cumul1;
                norm = norm1;
            }
            hasBars = hasBars || layerHasBars;
        }
        return hasBars ? new BarState( sizer, cumul, norm ) : null;
    }

    /**
     * Adds a heading/JLabel pair to the bottom of a given component.
     *
     * @param  panel  panel to add to
     * @param  labelHead   text of heading
     * @param  labelComp   component to add under heading
     */
    private static void addCountLabel( JComponent panel, String labelHead,
                                       JLabel labelComp ) {
        JComponent holder = new JPanel( new BorderLayout() );
        holder.add( new LineBox( labelHead, labelComp ), BorderLayout.NORTH );
        holder.setBorder( BorderFactory.createEmptyBorder( 10, 0, 5, 0 ) );
        panel.add( holder );
    }

    /**
     * Surface factory for histogram.
     */
    private static class HistogramSurfaceFactory extends PlaneSurfaceFactory {
        
        private static final ConfigKey<Boolean> HIST_XANCHOR_KEY =
            createAxisAnchorKey( "X", true );
        private static final ConfigKey<Boolean> HIST_YANCHOR_KEY =
            createAxisAnchorKey( "Y", false );

        HistogramSurfaceFactory() {
            super( false );
        }

        @Override
        public ConfigKey<?>[] getNavigatorKeys() {
            return new ConfigKey<?>[] {
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
        final Cumulation cumulative_;
        final Normalisation norm_;
        BarState( BinSizer sizer, Cumulation cumulative, Normalisation norm ) {
            sizer_ = sizer;
            cumulative_ = cumulative;
            norm_ = norm;
        }
    }
}
