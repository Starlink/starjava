package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
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
import uk.ac.starlink.ttools.plot2.layer.PairLinkForm;
import uk.ac.starlink.ttools.plot2.layer.PlaneCorrelationCoordSet;
import uk.ac.starlink.ttools.plot2.layer.PlaneEllipseCoordSet;
import uk.ac.starlink.ttools.plot2.layer.PolygonForms;
import uk.ac.starlink.ttools.plot2.layer.SizeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SizeXyForm;
import uk.ac.starlink.ttools.plot2.layer.Stats1Plotter;
import uk.ac.starlink.ttools.plot2.layer.TracePlotter;
import uk.ac.starlink.ttools.plot2.layer.Unit;
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

    private static PlaneDataGeom DATAGEOM = PlaneDataGeom.INSTANCE;
    private static final PlanePlotType INSTANCE =
        new PlanePlotType( createDefaultPlotters(), true );
    private final SurfaceFactory surfFact_;
    private final Plotter[] plotters_;

    /**
     * Constructor.
     *
     * @param  plotters  available plotters for use with this plot type
     * @param  has2dMetric  true if it may make sense to measure distances
     *                      that are not parallel to either axis
     */
    public PlanePlotType( Plotter[] plotters, boolean has2dMetric ) {
        plotters_ = plotters;
        surfFact_ = new PlaneSurfaceFactory( has2dMetric );
    }

    public DataGeom[] getPointDataGeoms() {
        return new DataGeom[] { DATAGEOM };
    }

    public Plotter[] getPlotters() {
        return plotters_.clone();
    }

    public SurfaceFactory getSurfaceFactory() {
        return surfFact_;
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

    /**
     * Assembles the list of plotters available to a normal plane plot window.
     *
     * @return  plane plotter list
     */
    private static Plotter[] createDefaultPlotters() {
        Coord[] coords = DATAGEOM.getPosCoords();
        String[] axisNames = new String[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            axisNames[ i ] = ((FloatingCoord) coords[ i ])
                            .getInput().getMeta().getLongName();
        };
        List<Plotter> list = new ArrayList<Plotter>();
        ShapeForm[] forms = new ShapeForm[] {
            MarkForm.SINGLE,
            SizeForm.getInstance(),
            SizeXyForm.getInstance(),
            MultiPointForm
           .createVectorForm( "XYVector",
                              new CartesianVectorCoordSet( axisNames ), true ),
            MultiPointForm
           .createErrorForm( "XYError",
                             CartesianErrorCoordSet
                            .createAllAxesErrorCoordSet( axisNames ),
                             StyleKeys.ERROR_SHAPE_2D ),
            PlaneEllipseCoordSet.createForm(),
            PlaneCorrelationCoordSet.createForm(),
            PairLinkForm.getInstance(),
            MarkForm.PAIR,
            PolygonForms.QUAD,
            MarkForm.QUAD,
            PolygonForms.ARRAY,
        };
        Plotter[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( shapePlotters ) );
        PerUnitConfigKey<Unit> unitKey = null;
        list.addAll( Arrays.asList( new Plotter[] {
            new LinePlotter(),
            new LinearFitPlotter( true ),
            new LabelPlotter(),
            new ContourPlotter( true ),
            new GridPlotter( true ),
            new FillPlotter( true ),
            new TracePlotter( true ),
            new HistogramPlotter( PlaneDataGeom.X_COORD, true, unitKey ),
            new FixedKernelDensityPlotter( PlaneDataGeom.X_COORD, true,
                                           unitKey ),
            new KnnKernelDensityPlotter( PlaneDataGeom.X_COORD, true, unitKey ),
            new DensogramPlotter( PlaneDataGeom.X_COORD, true ),
            new Stats1Plotter( PlaneDataGeom.X_COORD, true, unitKey ),
            FunctionPlotter.PLANE,
        } ) );
        return list.toArray( new Plotter[ 0 ] );
    }
}
