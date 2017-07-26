package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;

/**
 * Task for Sky-type plots.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class SkyPlot2Task extends TypedPlot2Task {

    private static final String viewsysName_ =
        SkySurfaceFactory.VIEWSYS_KEY.getMeta().getShortName();

    /**
     * Constructor.
     */
    public SkyPlot2Task() {
        super( SkyPlotType.getInstance(), null, new SkyPlotContext() );
        Parameter<SkySys> viewsysParam = null;
        for ( Parameter param : super.getParameters() ) {
            if ( viewsysName_.equals( param.getName() ) ) {
                viewsysParam = param;
            }
        }
        viewsysParam.setNullPermitted( true );
        viewsysParam.setStringDefault( null );

        /* Initialise the context with this parameter,
         * since it's not possible to do it at construction time. */
        ((SkyPlotContext) getPlotContext())
                         .setViewsysParameter( viewsysParam );
    }

    /**
     * PlotContext implementation for use with sky plots.
     * It has custom logic for determining per-layer DataGeoms,
     * based on a per-plot SkySys view selection and a per-layer
     * SkySys data selection.
     */
    private static class SkyPlotContext extends PlotContext {
        private Parameter<SkySys> viewsysParam_;

        /**
         * Constructor.
         */
        SkyPlotContext() {
            super( SkyPlotType.getInstance(),
                   new DataGeom[] { SkyDataGeom.GENERIC },
                   SingleGanger.FACTORY );
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

        public Parameter[] getGeomParameters( String suffix ) {
            return new Parameter[] {
                createDataSysParameter( suffix ),
            };
        }

        public DataGeom getGeom( Environment env, String suffix )
                throws TaskException {
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
            return SkyDataGeom.createGeom( datasys, viewsys );
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
