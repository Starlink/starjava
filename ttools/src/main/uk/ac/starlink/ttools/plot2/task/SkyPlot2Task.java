package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;

/**
 * Task for Sky-type plots.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class SkyPlot2Task extends TypedPlot2Task {

    private final SysParameter viewsysParam_;

    /**
     * Constructor.
     */
    public SkyPlot2Task() {
        super( SkyPlotType.getInstance(), new SkyPlotContext() );

        /* Parameter to define the sky system in which the plot is viewed. */
        viewsysParam_ = new SysParameter( "viewsys" );
        viewsysParam_.setNullPermitted( true );
        viewsysParam_.setStringDefault( null );

        /* Initialise the context with this parameter,
         * since it's not possible to do it at construction time. */
        ((SkyPlotContext) getPlotContext())
                         .setViewsysParameter( viewsysParam_ );
    }

    public Parameter[] getParameters() {
        List<Parameter> params = new ArrayList<Parameter>();
        params.addAll( Arrays.asList( super.getParameters() ) );
        params.add( viewsysParam_ );
        return params.toArray( new Parameter[ 0 ] );
    }

    /**
     * PlotContext implementation for use with sky plots.
     * It has custom logic for determining per-layer DataGeoms,
     * based on a per-plot SkySys view selection and a per-layer
     * SkySys data selection.
     */
    private static class SkyPlotContext extends PlotContext {
        private SysParameter viewsysParam_;

        /**
         * Constructor.
         */
        SkyPlotContext() {
            super( SkyPlotType.getInstance(),
                   new DataGeom[] { SkyDataGeom.GENERIC } );
        }

        /**
         * Sets the per-plot view system parameter.
         * Must be invoked with a non-null value before this context
         * is used for geom determination.
         *
         * @param   viewsysParam  parameter for selecting per-plot view system
         */
        public void setViewsysParameter( SysParameter viewsysParam ) {
            viewsysParam_ = viewsysParam;
        }

        public Parameter[] getGeomParameters( String suffix ) {
            return new Parameter[] {
                new SysParameter( datasysParamName( suffix ) ),
            };
        }

        public DataGeom getGeom( Environment env, String suffix )
                throws TaskException {
            SkySys viewsys = viewsysParam_.objectValue( env );
            SysParameter datasysParam = new ParameterFinder<SysParameter>() {
                protected SysParameter createParameter( String sfix ) {
                    return new SysParameter( datasysParamName( sfix ) );
                }
            }.getParameter( env, suffix );
            datasysParam.setNullPermitted( viewsys == null );
            SkySys datasys = datasysParam.objectValue( env );
            if ( viewsys == null && datasys != null ) {
                String msg = new StringBuffer()
                   .append( "Must be null if " )
                   .append( viewsysParam_.getName() )
                   .append( " is null" )
                   .toString();
                throw new ParameterValueException( datasysParam, msg );
            }
            return SkyDataGeom.createGeom( datasys, viewsys );
        }

        /**
         * Returns the parameter name to use for the data sys parameter.
         *
         * @param  suffix  layer-specific suffix
         * @return  parameter name
         */
        private String datasysParamName( String suffix ) {
            return "datasys" + suffix;
        }
    }

    /**
     * Parameter type for selecting sky coordinate systems.
     */
    private static class SysParameter extends ChoiceParameter<SkySys> {

        /**
         * Constructor.
         *
         * @param  name  parameter name
         */
        SysParameter( String name ) {
            super( name, SkySys.getKnownSystems( false ) );
        }
    }
}
