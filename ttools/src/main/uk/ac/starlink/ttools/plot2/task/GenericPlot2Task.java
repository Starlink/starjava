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

    private final ChoiceParameter<PlotType> typeParam_;
    private final ChoiceParameter<DataGeom> geomParam_;
    private final Parameter[] params_;

    /**
     * Constructor.
     */
    public GenericPlot2Task() {
        super( true );

        List<Parameter> paramList = new ArrayList<Parameter>();

        /* Plot type parameter. */
        typeParam_ = new ChoiceParameter<PlotType>( "type", new PlotType[] {
            PlanePlotType.getInstance(),
            SkyPlotType.getInstance(),
            CubePlotType.getInstance(),
            SpherePlotType.getInstance(),
            TimePlotType.getInstance(),
        } );
        paramList.add( typeParam_ );

        /* DataGeom parameter. */
        geomParam_ = new ChoiceParameter<DataGeom>( "geom", DataGeom.class );
        paramList.add( geomParam_ );

        /* Other parameters from superclass. */
        paramList.addAll( Arrays.asList( getBasicParameters() ) );
        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    public String getPurpose() {
        return "Draws a generic plot";
    }

    public PlotContext getPlotContext( Environment env ) throws TaskException {

        /* Get plot type. */
        final PlotType plotType = typeParam_.objectValue( env );

        /* Determine the data position coordinate geometry. */
        DataGeom[] geoms = plotType.getPointDataGeoms();
        geomParam_.clearOptions();
        for ( int ig = 0; ig < geoms.length; ig++ ) {
            geomParam_.addOption( geoms[ ig ], geoms[ ig ].getVariantName() );
        }
        geomParam_.setDefaultOption( geoms[ 0 ] );
        final DataGeom dataGeom = geomParam_.objectValue( env );
        return new PlotContext() {
            public PlotType getPlotType() {
                return plotType;
            }
            public DataGeom getDataGeom() {
                return dataGeom;
            }
        };
    }

    public Parameter[] getParameters() {
        return params_;
    }
}
