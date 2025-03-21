package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimeSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;

/**
 * Axis control for plot with a horizontal time axis.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2013
 */
public class TimeAxisController
        extends CartesianAxisController<TimeSurfaceFactory.Profile,TimeAspect> {

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public TimeAxisController() {
        super( new TimeSurfaceFactory(), createAxisLabelKeys() );
        SurfaceFactory<TimeSurfaceFactory.Profile,TimeAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip tab. */
        mainControl.addSpecifierTab( "Coords",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            TimeSurfaceFactory.YSCALE_KEY,
            TimeSurfaceFactory.YFLIP_KEY,
        } ) );

        /* Navigator tab. */
        addNavigatorTab();

        /* Range tab. */
        addAspectConfigTab( "Range",
                            new ConfigSpecifier( surfFact.getAspectKeys() ) {
            @Override
            protected void checkConfig( ConfigMap config )
                    throws ConfigException {
                checkRangeSense( config, "Time",
                                 TimeSurfaceFactory.TMIN_KEY,
                                 TimeSurfaceFactory.TMAX_KEY );
                checkRangeSense( config, "Y",
                                 TimeSurfaceFactory.YMIN_KEY,
                                 TimeSurfaceFactory.YMAX_KEY );
            }
        } );

        /* Grid tab. */
        List<ConfigKey<?>> gridKeyList = new ArrayList<>();
        Collections.addAll( gridKeyList,
                            TimeSurfaceFactory.TFORMAT_KEY,
                            TimeSurfaceFactory.GRID_KEY,
                            StyleKeys.MINOR_TICKS,
                            StyleKeys.SHADOW_TICKS,
                            TimeSurfaceFactory.TCROWD_KEY,
                            TimeSurfaceFactory.YCROWD_KEY,
                            TimeSurfaceFactory.ORIENTATIONS_KEY );
        Collections.addAll( gridKeyList,
                            StyleKeys.GRIDCOLOR_KEYSET.getKeys() );
        ConfigKey<?>[] gridKeys = gridKeyList.toArray( new ConfigKey<?>[ 0 ] );
        mainControl.addSpecifierTab( "Grid", new ConfigSpecifier( gridKeys ) );

        /* Labels tab. */
        addLabelsTab();

        /* Secondary axes tab. */
        mainControl.addSpecifierTab( "Secondary",
                                     new ConfigSpecifier( new ConfigKey<?>[] {
            TimeSurfaceFactory.T2FUNC_KEY,
            TimeSurfaceFactory.T2LABEL_KEY,
            TimeSurfaceFactory.Y2FUNC_KEY,
            TimeSurfaceFactory.Y2LABEL_KEY,
        } ) );

        /* Font tab. */
        mainControl.addSpecifierTab( "Font",
                                     new ConfigSpecifier( StyleKeys.CAPTIONER
                                                         .getKeys() ) );

        /* Check we have the keys specified by the surface factory,
         * but exclude redundant/deprecated ones used for CLI
         * backward compatibility. */
        List<ConfigKey<?>> reqKeys =
            new ArrayList<ConfigKey<?>>( Arrays.asList( surfFact
                                                       .getProfileKeys() ) );
        reqKeys.remove( TimeSurfaceFactory.YLOG_KEY );
        assert assertHasKeys( reqKeys.toArray( new ConfigKey<?>[ 0 ] ) );
    }

    @Override
    protected boolean logChanged( TimeSurfaceFactory.Profile prof1,
                                  TimeSurfaceFactory.Profile prof2 ) {
        return prof1.getYScale().isPositiveDefinite()
            != prof2.getYScale().isPositiveDefinite();
    }

    @Override
    protected boolean clearRange( TimeSurfaceFactory.Profile oldProfile,
                                  TimeSurfaceFactory.Profile newProfile,
                                  PlotLayer[] oldLayers, PlotLayer[] newLayers,
                                  boolean lock ) {
        if ( super.clearRange( oldProfile, newProfile, oldLayers, newLayers,
                               lock ) ) {
            return true;
        }
        else if ( lock ) {
            return false;
        }
        else {

            /* Special handling for spectrograms.
             * The vertical axis range will (at least may) depend on whether 
             * the spectral axis is channel bins or spectral coordinate.
             * So if that characteristic has changed for any of the visible
             * spectrograms, signal that a re-range is required. */
            SpecState oldState = getSpecState( oldLayers );
            SpecState newState = getSpecState( newLayers );
            if ( newState.nspec_ == oldState.nspec_ &&
                 newState.nscale_ != oldState.nscale_ ) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Returns the config keys for axis labelling.
     *
     * @return  T, Y axis label config keys
     */
    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( TimeSurfaceFactory.TLABEL_KEY );
        list.add( TimeSurfaceFactory.YLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys =
            (ConfigKey<String>[]) list.toArray( new ConfigKey<?>[ 0 ] );
        return keys;
    }

    /**
     * Returns information about the spectrogram plots
     * in a given set of layers.
     *
     * @param  layers
     * @return  spectrogram group state
     */
    private static SpecState getSpecState( PlotLayer[] layers ) {
        int nspec = 0;
        int nscale = 0;
        for ( PlotLayer layer : layers ) {
            Style style = layer.getStyle();
            if ( style instanceof SpectrogramPlotter.SpectroStyle ) {
                nspec++;
                if ( ((SpectrogramPlotter.SpectroStyle) style)
                    .getScaleSpectra() ) {
                    nscale++;
                }
            }
        }
        return new SpecState( nspec, nscale );
    }

    /**
     * Characterises some aspects of the content of visible spectrogram layers.
     */
    private static class SpecState {
        final int nspec_;
        final int nscale_;

        /**
         * Constructor.
         *
         * @param  nspec  number of spectrograms in group
         * @param  nscale  number of those spectrograms with scaleSpectra
         *                 flag set true
         */
        SpecState( int nspec, int nscale ) {
            nspec_ = nspec;
            nscale_ = nscale;
        }
        @Override
        public String toString() {
            return nscale_ + "/" + nspec_;
        }
    }
}
