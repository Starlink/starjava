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
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;

/**
 * Generic plot2 task for STILTS.
 * The plot type is determined from the environment using a Parameter.
 * The resulting task is very flexible, but the details of the parameters
 * that will actually be used cannot be determined in absence of the
 * plot type (that is, before the Environment is available),
 * so the task is not very good at describing its required parameters.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2014
 */
public class GenericPlot2Task extends AbstractPlot2Task {

    private final ChoiceParameter<PlotType<?,?>> typeParam_;

    /**
     * Constructor.
     *
     * @param  allowAnimate  true iff animation options should be provided
     * @param  hasZoneSuffixes  true iff zone content can be controlled
     *                          explicitly by use of parameter suffixes
     */
    public GenericPlot2Task( boolean allowAnimate, boolean hasZoneSuffixes ) {
        super( allowAnimate, hasZoneSuffixes );

        /* Plot type parameter. */
        typeParam_ = new ChoiceParameter<PlotType<?,?>>
                                        ( "type", new PlotType<?,?>[]{
            PlanePlotType.getInstance(),
            SkyPlotType.getInstance(),
            CubePlotType.getInstance(),
            SpherePlotType.getInstance(),
            TimePlotType.getInstance(),
        } );
    }

    public String getPurpose() {
        return "Draws a generic plot";
    }

    public Parameter<?>[] getParameters() {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        paramList.add( typeParam_ );
        paramList.addAll( Arrays.asList( getBasicParameters() ) );
        return paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public PlotContext<?,?> getPlotContext( Environment env )
            throws TaskException {
        return PlotContext
              .createStandardContext( typeParam_.objectValue( env ) );
    }

    protected <T> String getConfigParamDefault( Environment env,
                                                ConfigKey<T> key,
                                                String[] suffixes ) {
        return null;
    }
}
