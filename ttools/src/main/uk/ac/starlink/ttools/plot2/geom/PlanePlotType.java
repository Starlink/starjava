package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.layer.CartesianErrorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.CartesianVectorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.ContourPlotter;
import uk.ac.starlink.ttools.plot2.layer.DensogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.FillPlotter;
import uk.ac.starlink.ttools.plot2.layer.FixedKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.GridPlotter;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.KnnKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.LinePlotter;
import uk.ac.starlink.ttools.plot2.layer.LinearFitPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.MultiPointForm;
import uk.ac.starlink.ttools.plot2.layer.Normalisation;
import uk.ac.starlink.ttools.plot2.layer.PairLinkForm;
import uk.ac.starlink.ttools.plot2.layer.PlaneEllipseCoordSet;
import uk.ac.starlink.ttools.plot2.layer.SizeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SizeXyForm;
import uk.ac.starlink.ttools.plot2.layer.Stats1Plotter;
import uk.ac.starlink.ttools.plot2.layer.TracePlotter;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Defines the characteristics of a plot on a 2-dimensional plane.
 *
 * <p>This is a singleton class, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlanePlotType implements PlotType {

    private static final SurfaceFactory SURFACE_FACTORY =
        new PlaneSurfaceFactory();
    private static final PlanePlotType INSTANCE = new PlanePlotType();
    private final DataGeom[] dataGeoms_;
    private final String[] axisNames_;

    /**
     * Private constructor for singleton.
     */
    private PlanePlotType() {
        dataGeoms_ = new DataGeom[] { PlaneDataGeom.INSTANCE };
        Coord[] coords = dataGeoms_[ 0 ].getPosCoords();
        axisNames_ = new String[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            axisNames_[ i ] = ((FloatingCoord) coords[ i ])
                             .getInput().getMeta().getLongName();
        };
    }

    public DataGeom[] getPointDataGeoms() {
        return dataGeoms_;
    }

    public Plotter[] getPlotters() {
        List<Plotter> list = new ArrayList<Plotter>();
        ShapeForm[] forms = new ShapeForm[] {
            MarkForm.SINGLE,
            SizeForm.getInstance(),
            SizeXyForm.getInstance(),
            MultiPointForm
           .createVectorForm( "XYVector",
                              new CartesianVectorCoordSet( axisNames_ ), true ),
            MultiPointForm
           .createErrorForm( "XYError",
                             CartesianErrorCoordSet
                            .createAllAxesErrorCoordSet( axisNames_ ),
                             StyleKeys.ERROR_SHAPE_2D ),
            MultiPointForm
           .createEllipseForm( "XYEllipse", new PlaneEllipseCoordSet(), true ),
            PairLinkForm.getInstance(),
            MarkForm.PAIR,
        };
        Plotter[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( shapePlotters ) );
        ConfigKey<Normalisation> normKey = StyleKeys.NORMALISE;
        list.addAll( Arrays.asList( new Plotter[] {
            new LinePlotter(),
            new LinearFitPlotter( true ),
            new LabelPlotter(),
            new ContourPlotter(),
            new GridPlotter( true ),
            new FillPlotter( true ),
            new TracePlotter( true ),
            new HistogramPlotter( PlaneDataGeom.X_COORD, true, normKey ),
            new FixedKernelDensityPlotter( PlaneDataGeom.X_COORD, true,
                                           normKey ),
            new KnnKernelDensityPlotter( PlaneDataGeom.X_COORD, true, normKey ),
            new DensogramPlotter( PlaneDataGeom.X_COORD, true ),
            new Stats1Plotter( PlaneDataGeom.X_COORD, true, normKey ),
            FunctionPlotter.PLANE,
        } ) );
        return list.toArray( new Plotter[ 0 ] );
    }

    public SurfaceFactory getSurfaceFactory() {
        return SURFACE_FACTORY;
    }

    public PaperTypeSelector getPaperTypeSelector() {
        return PaperTypeSelector.SELECTOR_2D;
    }

    public String toString() {
        return "plane";
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static PlanePlotType getInstance() {
        return INSTANCE;
    }
}
