package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;

/**
 * Plot2 task specialised for a fixed PlotType.
 * Knowing the PlotType up front doesn't make it more capable,
 * but it allows much more parameter auto-documentation to be done
 * than if the PlotType is determined only from the Environment.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class TypedPlot2Task extends AbstractPlot2Task {

    private final PlotType plotType_;
    private final PlotContext context_;
    private final Parameter[] params_;

    /**
     * Constructs a plot task with a supplied PlotContext.
     *
     * @param  plotType  fixed plot type
     * @param  context   fixed plot context
     */
    public TypedPlot2Task( PlotType plotType, PlotContext context ) {
        super();
        plotType_ = plotType;
        context_ = context;
        List<Parameter> paramList = new ArrayList<Parameter>();

        /* Standard parameters applicable to all plot tasks. */
        paramList.addAll( Arrays.asList( getBasicParameters() ) );

        /* Parameters specific to the plotting surface type. */
        SurfaceFactory surfFact = plotType.getSurfaceFactory();
        paramList.addAll( getKeyParams( surfFact.getProfileKeys() ) );
        paramList.addAll( getKeyParams( surfFact.getAspectKeys() ) );
        paramList.addAll( getKeyParams( surfFact.getNavigatorKeys() ) );

        /* Layer parameter, which defines what plotters are available. */
        paramList.add( createLayerTypeParameter( EXAMPLE_LAYER_SUFFIX,
                                                 context ) );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    /**
     * Constructs a plot task with a default plot context.
     * If the plot type has only a single DataGeom no geom selection
     * is allowed, otherwise there is a per-layer geom selection
     * parameter.
     *
     * @param  plotType  fixed plot type
     */
    public TypedPlot2Task( PlotType plotType ) {
        this( plotType, createDefaultPlotContext( plotType ) );
    }

    public String getPurpose() {
        return "Draws a " + plotType_.toString() + " plot";
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public PlotContext getPlotContext( Environment env ) {
        return context_;
    }

    /**
     * Returns the fixed plot context for this task.
     *
     * @return  plot context
     */
    public PlotContext getPlotContext() {
        return context_;
    }

    /**
     * Returns a list of non-suffixed parameters based on a list of
     * ConfigKeys.
     *
     * @param  keys  config keys
     * @return  parameters for acquiring config key values
     */
    private static List<Parameter> getKeyParams( ConfigKey[] keys ) {
        List<Parameter> plist = new ArrayList<Parameter>();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            plist.add( new ConfigParameter( keys[ ik ] ) );
        }
        return plist;
    }

    /**
     * Returns a default plot context for a given PlotType.
     * If the plot type has only a single DataGeom no geom selection
     * is allowed, otherwise there is a per-layer geom selection
     * parameter.
     *
     * @param  plotType  plot type
     * @return  context
     */
    private static PlotContext createDefaultPlotContext( PlotType plotType ) {
        final DataGeom[] geoms = plotType.getPointDataGeoms();
        return geoms.length == 1
             ? PlotContext.createFixedContext( plotType, geoms[ 0 ] )
             : PlotContext.createStandardContext( plotType );
    }
}
