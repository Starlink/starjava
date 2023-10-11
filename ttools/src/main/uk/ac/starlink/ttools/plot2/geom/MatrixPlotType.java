package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.layer.ContourPlotter;
import uk.ac.starlink.ttools.plot2.layer.DensogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.FillPlotter;
import uk.ac.starlink.ttools.plot2.layer.FixedKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.GridPlotter;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.KnnKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.LinePlotter;
import uk.ac.starlink.ttools.plot2.layer.LinearFitPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.Stats1Plotter;
import uk.ac.starlink.ttools.plot2.layer.TracePlotter;
import uk.ac.starlink.ttools.plot2.layer.Unit;

/**
 * PlotType for a grid of plots.
 * This is sometimes known as a "corner plot" or SPLOM (Scatter PLOt Matrix).
 *
 * @author   Mark Taylor
 * @since    16 Aug 2023
 */
public class MatrixPlotType extends PlanePlotType {

    /** SurfaceFactory configuration for matrix plot. */
    public static final PlaneSurfaceFactory.Config MATRIX_CONFIG =
        new PlaneSurfaceFactory.Config() {
            public boolean has2dMetric() {
                return true;
            }
            public boolean hasSecondaryAxes() {
                return false;
            }
        };

    /** Default instance. */
    private static final MatrixPlotType INSTANCE =
        new MatrixPlotType( new PlaneSurfaceFactory( MATRIX_CONFIG ),
                            createMatrixPlotters() );

    /**
     * Constructor.
     *
     * @param   plotters  plotters for use with this plot type
     */
    public MatrixPlotType( PlaneSurfaceFactory surfFact,
                           Plotter<?>[] plotters ) {
        super( surfFact, plotters );
    }

    @Override
    public GangerFactory<PlaneSurfaceFactory.Profile,PlaneAspect>
                         getGangerFactory() {
        return MatrixGangerFactory.instance();
    }

    @Override
    public String toString() {
        return "matrix";
    }

    /**
     * Returns the default instance of this plot type.
     *
     * @return   instance
     */
    public static MatrixPlotType getInstance() {
        return INSTANCE;
    }

    /**
     * Returns coordinate metadata for one of the spatial coordinates
     * used by the matrix plot.
     *
     * @param  ic  coordinate index (0-based)
     * @return  user-facing metadata for coordinate
     */
    public static InputMeta getCoordMeta( int ic ) {
        String suffix = Integer.toString( ic + 1 );
        InputMeta meta = new InputMeta( "x" + suffix, "X" + suffix );
        meta.setShortDescription( "Coordinate for spatial vector element #"
                                + suffix );
        return meta;
    }

    /**
     * Returns the human-readable coordinate name for one of the
     * spatial coordinates used by the matrix plot.
     *
     * @param  ic  coordinate index (0-based)
     * @return   coordinate name
     */
    public static String getCoordName( int ic ) {
        return getCoordMeta( ic ).getLongName();
    }

    /**
     * Constructs the list of plotters suitable for use with matrix plot.
     *
     * @return   plotter array
     */
    private static Plotter<?>[] createMatrixPlotters() {
        List<Plotter<?>> list = new ArrayList<>();
        ShapeForm[] forms = new ShapeForm[] {
            MarkForm.SINGLE,
        };
        Plotter<?>[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( shapePlotters ) );
        PerUnitConfigKey<Unit> unitKey = null;
        list.addAll( Arrays.asList( new Plotter<?>[] {
            new LinePlotter( LinePlotter.PLANE_SORTAXIS_KEY ),
            new LinearFitPlotter( true ),
            LabelPlotter.POINT_INSTANCE,
            new ContourPlotter( true ),
            new GridPlotter( true ),
            new FillPlotter( true ),
            TracePlotter.createPointsTracePlotter( true ),
            new HistogramPlotter( PlaneDataGeom.X_COORD, true, unitKey ),
            new FixedKernelDensityPlotter( PlaneDataGeom.X_COORD, true,
                                           unitKey ),
            new KnnKernelDensityPlotter( PlaneDataGeom.X_COORD, true, unitKey ),
            new DensogramPlotter( PlaneDataGeom.X_COORD, true ),
            new Stats1Plotter( PlaneDataGeom.X_COORD, true, unitKey ),
            FunctionPlotter.PLANE,
        } ) );
        return list.toArray( new Plotter<?>[ 0 ] );
    }
}
