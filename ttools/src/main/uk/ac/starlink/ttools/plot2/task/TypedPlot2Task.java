package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Input;

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
    private final Map<ConfigKey<String>,Input> axlabelMap_;
    private final Parameter[] params_;

    /**
     * Constructs a plot task with a supplied PlotContext.
     *
     * <p>The <code>axlabelMap</code> parameter gives the chance to set
     * up a correspondence between axis label config keys and the coordinates
     * to which they correspond.  If this is done, then the names of the
     * data values actually supplied to the task can be used as defaults
     * for the axis labels.
     *
     * @param  plotType  fixed plot type
     * @param  axlabelMap  mapping from axis label keys to corresponding
     *                     common data input coordinates, or null
     * @param  context   fixed plot context
     */
    public TypedPlot2Task( PlotType plotType,
                           Map<ConfigKey<String>,Input> axlabelMap,
                           PlotContext context ) {
        super();
        plotType_ = plotType;
        context_ = context;
        axlabelMap_ = axlabelMap == null
                    ? new HashMap<ConfigKey<String>,Input>()
                    : axlabelMap;
        List<Parameter> paramList = new ArrayList<Parameter>();

        /* Standard parameters applicable to all plot tasks. */
        paramList.addAll( Arrays.asList( getBasicParameters() ) );

        /* Parameters specific to the plotting surface type. */
        SurfaceFactory surfFact = plotType.getSurfaceFactory();
        paramList.addAll( getKeyParams( surfFact.getProfileKeys() ) );
        paramList.addAll( getKeyParams( surfFact.getAspectKeys() ) );
        paramList.addAll( getKeyParams( surfFact.getNavigatorKeys() ) );

        /* Layer parameter, which defines what plotters are available. */
        paramList.add( createLabelParameter( EXAMPLE_LAYER_SUFFIX ) );
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
     * <p>The <code>axlabelMap</code> parameter gives the chance to set
     * up a correspondence between axis label config keys and the coordinates
     * to which they correspond.  If this is done, then the names of the
     * data values actually supplied to the task can be used as defaults
     * for the axis labels.
     *
     * @param  plotType  fixed plot type
     * @param  axlabelMap  mapping from axis label keys to corresponding
     *                     common data input coordinates, or null
     */
    public TypedPlot2Task( PlotType plotType,
                           Map<ConfigKey<String>,Input> axlabelMap ) {
        this( plotType, axlabelMap, createDefaultPlotContext( plotType ) );
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

    protected <T> ConfigParameter<T>
            createConfigParameter( Environment env, ConfigKey<T> key,
                                   String[] suffixes )
            throws TaskException {
        ConfigParameter<T> param = new ConfigParameter<T>( key );
        final Input dataInput = axlabelMap_.get( key );
        if ( dataInput != null ) {
            String dataName = getAxisDataName( env, dataInput, suffixes );
            if ( dataName != null ) {
                param.setStringDefault( dataName );
            }
        }
        return param;
    }

    /**
     * Attempts to locate the value of a parameter corresponding to a
     * particular input data coordinate.  This value may provide
     * suitable text for labelling a corresponding axis. 
     * Supplied suffixes are tried in turn until one comes up with the goods.
     *
     * @param  env  execution environment
     * @param  input  input data coordinate
     * @param  suffixes  suffixes of layers being plotted
     * @return  suitable name for input data axis, or null if none found
     */
    private static String getAxisDataName( Environment env, final Input input,
                                           String[] suffixes )
            throws TaskException {
        for ( String suffix : suffixes ) {
            Parameter<String> dataParam =
                    new ParameterFinder<Parameter<String>>() {
                public Parameter<String> createParameter( String sfix ) {
                    return createDataParameter( input, sfix, false );
                }
            }.findParameter( env, suffix );
            if ( dataParam != null ) {
                return dataParam.stringValue( env );
            }
        }
        return null;
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
