package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.layer.AreaForm;
import uk.ac.starlink.ttools.plot2.layer.ArrayShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.CartesianErrorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.CartesianMultiPointForm;
import uk.ac.starlink.ttools.plot2.layer.CartesianVectorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.CentralForm;
import uk.ac.starlink.ttools.plot2.layer.ContourPlotter;
import uk.ac.starlink.ttools.plot2.layer.DensogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.ErrorArrayForm;
import uk.ac.starlink.ttools.plot2.layer.FillPlotter;
import uk.ac.starlink.ttools.plot2.layer.FixedKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.GridPlotter;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.KnnKernelDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.HandleArrayForm;
import uk.ac.starlink.ttools.plot2.layer.LineArrayForm;
import uk.ac.starlink.ttools.plot2.layer.LineCombineArrayPlotter;
import uk.ac.starlink.ttools.plot2.layer.LinePlotter;
import uk.ac.starlink.ttools.plot2.layer.LinearFitPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkArrayForm;
import uk.ac.starlink.ttools.plot2.layer.MarkCombineArrayPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
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
public class PlanePlotType
        implements PlotType<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private static PlaneDataGeom DATAGEOM = PlaneDataGeom.INSTANCE;
    private static final PlaneSurfaceFactory.Config PLANE_CONFIG =
        new PlaneSurfaceFactory.Config() {
            public boolean has2dMetric() {
                return true;
            }
            public boolean hasSecondaryAxes() {
                return true;
            }
        };
    private static final PlanePlotType INSTANCE =
        new PlanePlotType( new PlaneSurfaceFactory( PLANE_CONFIG ),
                           createDefaultPlotters() );

    private final PlaneSurfaceFactory surfFact_;
    private final Plotter<?>[] plotters_;

    /**
     * Constructor.
     *
     * @param  surfFact  surface factory
     * @param  plotters  available plotters for use with this plot type
     */
    public PlanePlotType( PlaneSurfaceFactory surfFact,
                          Plotter<?>[] plotters ) {
        surfFact_ = surfFact;
        plotters_ = plotters;
    }

    public DataGeom[] getPointDataGeoms() {
        return new DataGeom[] { DATAGEOM };
    }

    public Plotter<?>[] getPlotters() {
        return plotters_.clone();
    }

    public PlaneSurfaceFactory getSurfaceFactory() {
        return surfFact_;
    }

    public GangerFactory<PlaneSurfaceFactory.Profile,PlaneAspect>
                         getGangerFactory() {
        return SingleGangerFactory.instance();
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
    private static Plotter<?>[] createDefaultPlotters() {
        Coord[] coords = DATAGEOM.getPosCoords();
        String[] axisNames = new String[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            axisNames[ i ] = ((FloatingCoord) coords[ i ])
                            .getInput().getMeta().getLongName();
        };
        List<Plotter<?>> list = new ArrayList<Plotter<?>>();
        ShapeForm[] forms = new ShapeForm[] {
            MarkForm.SINGLE,
            SizeForm.getInstance(),
            SizeXyForm.getInstance(),
            CartesianMultiPointForm
           .createVectorForm( "XYVector",
                              new CartesianVectorCoordSet( axisNames ), true ),
            CartesianMultiPointForm
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
            AreaForm.PLANE_INSTANCE,
            CentralForm.PLANE_INSTANCE,
        };
        Plotter<?>[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( shapePlotters ) );
        ShapeForm[] arrayForms = new ShapeForm[] {
            LineArrayForm.getInstance(),
            MarkArrayForm.getInstance(),
            HandleArrayForm.getInstance(),
            ErrorArrayForm.Y,
            ErrorArrayForm.XY,
        };
        Plotter<?>[] arrayShapePlotters =
            ArrayShapePlotter
           .createArrayShapePlotters( arrayForms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( arrayShapePlotters ) );
        list.add( LineCombineArrayPlotter.INSTANCE );
        list.add( MarkCombineArrayPlotter.INSTANCE );
        list.add( TracePlotter.createArraysTracePlotter( true ) );
        PerUnitConfigKey<Unit> unitKey = null;
        list.addAll( Arrays.asList( new Plotter<?>[] {
            new LinePlotter( LinePlotter.PLANE_SORTAXIS_KEY ),
            new LinearFitPlotter( true ),
            LabelPlotter.POINT_INSTANCE,
            LabelPlotter.AREA_PLANE_INSTANCE,
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
