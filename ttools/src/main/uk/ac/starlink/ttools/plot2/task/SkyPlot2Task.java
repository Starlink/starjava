package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.geom.HealpixDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyAspect;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;

/**
 * Task for Sky-type plots.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class SkyPlot2Task
        extends TypedPlot2Task<SkySurfaceFactory.Profile,SkyAspect> {

    private final SkyPlotContext skyContext_;

    private static final String viewsysName_ =
        SkySurfaceFactory.VIEWSYS_KEY.getMeta().getShortName();

    /**
     * Constructor.
     */
    public SkyPlot2Task() {
        super( SkyPlotType.getInstance(), null, new SkyPlotContext() );
        skyContext_ = (SkyPlotContext) getPlotContext();
        Parameter<SkySys> viewsysParam = null;
        for ( Parameter<?> param : super.getParameters() ) {
            if ( viewsysName_.equals( param.getName() ) ) {
                @SuppressWarnings("unchecked")
                Parameter<SkySys> vp = (Parameter<SkySys>) param;
                viewsysParam = vp;
            }
        }
        viewsysParam.setNullPermitted( true );
        viewsysParam.setStringDefault( null );

        /* Initialise the context with this parameter,
         * since it's not possible to do it at construction time. */
        skyContext_.setViewsysParameter( viewsysParam );
    }

    @Override
    public ConfigMap createCustomConfigMap( Environment env )
            throws TaskException {
        ConfigMap config = super.createCustomConfigMap( env );
        config.put( SkySurfaceFactory.VIEWSYS_KEY,
                    skyContext_.viewsysParam_.objectValue( env ) );
        return config;
    }

    /**
     * PlotContext implementation for use with sky plots.
     * It has custom logic for determining per-layer DataGeoms,
     * based on a per-plot SkySys view selection and a per-layer
     * SkySys data selection.
     */
    private static class SkyPlotContext
            extends PlotContext<SkySurfaceFactory.Profile,SkyAspect> {
        private static final SkyPlotType SKY_TYPE = SkyPlotType.getInstance();
        private Parameter<SkySys> viewsysParam_;

        /**
         * Constructor.
         */
        SkyPlotContext() {
            super( SKY_TYPE, new DataGeom[] { SkyDataGeom.GENERIC } );
        }

        /**
         * Sets the per-plot view system parameter.
         * Must be invoked with a non-null value before this context
         * is used for geom determination.
         *
         * @param   viewsysParam  parameter for selecting per-plot view system
         */
        public void setViewsysParameter( Parameter<SkySys> viewsysParam ) {
            viewsysParam_ = viewsysParam;
        }

        public Parameter<?> getGeomParameter( String suffix ) {
            return createDataSysParameter( suffix );
        }

        public DataGeom getGeom( Environment env, String suffix )
                throws TaskException {

            /* Get data and view sky coordinate systems.  The DataGeom
             * has the job of taking account of any transformation implied
             * by the difference between these two, so we need to know
             * what they are to construct it. */
            SkySys viewsys = viewsysParam_.objectValue( env );
            ConfigParameter<SkySys> datasysParam =
                    new ParameterFinder<ConfigParameter<SkySys>>() {
                public ConfigParameter<SkySys> createParameter( String sfix ) {
                    return createDataSysParameter( sfix );
                }
            }.getParameter( env, suffix );
            datasysParam.setNullPermitted( viewsys == null );
            datasysParam.setDefaultOption( viewsys );
            SkySys datasys = datasysParam.objectValue( env );
            if ( viewsys == null && datasys != null ) {
                String msg = new StringBuffer()
                   .append( datasysParam.getName() )
                   .append( " must be null if " )
                   .append( viewsysParam_.getName() )
                   .append( " is null" )
                   .toString();
                throw new TaskException( msg );
            }

            /* Do special case handling for the HealpixPlotter.
             * This is necessary because the HealpixPlotter has positional
             * coordinates, but they are not the same as the positional
             * coordinates assumed by the rest of the sky plot invocation
             * (it's a tile index rather than a lon/lat pair).
             * If you don't do this, the task will ask the user for lon/lat.
             * Switching on a particular layer type like this is not very
             * nice, and really this should be abstracted out to make more
             * generalised enquiries about the layer to see if this
             * kind of handling is necessary, and enquire for layer-specific
             * coordinate types.  However, for now the Healpix plotter is
             * the only example of this sort of thing, so it's not clear
             * what would be the best form for such generalisation.
             * If other instances of this kind of requirement come up,
             * consider this more carefully and generalise the handling
             * as appropriate. */
            LayerType layer = createLayerTypeParameter( suffix, this )
                             .objectValue( env );
            if ( layer.getPlotter( env, suffix ) instanceof HealpixPlotter ) {
                boolean isNest = HealpixPlotter.IS_NEST;
                int level = new ParameterFinder<ConfigParameter<Integer>>() {
                    public ConfigParameter<Integer>
                            createParameter( String sfix ) {
                        return ConfigParameter
                              .createLayerSuffixedParameter( HealpixPlotter
                                                            .DATALEVEL_KEY,
                                                             sfix, false );
                    }
                }.getParameter( env, suffix )
                 .objectValue( env )
                 .intValue();
                return HealpixDataGeom
                      .createGeom( level, isNest, datasys, viewsys );
            }

            /* Otherwise, it's a normal SkyDataGeom. */
            else {
                return SkyDataGeom.createGeom( datasys, viewsys );
            }
        }

        /**
         * Returns the parameter name to use for the data sys parameter.
         *
         * @param  suffix  layer-specific suffix
         * @return  parameter name
         */
        private ConfigParameter<SkySys>
                createDataSysParameter( String suffix ) {
            return ConfigParameter
                  .createLayerSuffixedParameter( SkySurfaceFactory.DATASYS_KEY,
                                                 suffix, false );
        }
    }
}
