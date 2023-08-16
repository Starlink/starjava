package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.layer.CartesianErrorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.CartesianMultiPointForm;
import uk.ac.starlink.ttools.plot2.layer.CartesianVectorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.ContourPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.Line3dPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.PairLinkForm;
import uk.ac.starlink.ttools.plot2.layer.PolygonForms;
import uk.ac.starlink.ttools.plot2.layer.SizeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SizeXyForm;
import uk.ac.starlink.ttools.plot2.layer.SphereGridPlotter;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Defines the characteristics of a plot in 3-dimensional
 * anisotropic space.
 *
 * <p>This is a singleton class, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class CubePlotType
        implements PlotType<CubeSurfaceFactory.Profile,CubeAspect> {

    private static final CubeSurfaceFactory SURFACE_FACTORY =
        new CubeSurfaceFactory( false );
    private final DataGeom[] dataGeoms_;
    private final String[] axisNames_;
    private static final CubePlotType INSTANCE = new CubePlotType();

    /**
     * Private singleton constructor.
     */
    private CubePlotType() {
        dataGeoms_ = new DataGeom[] {
            CubeDataGeom.INSTANCE,
            SphereDataGeom.INSTANCE,
        };
        Coord[] coords = dataGeoms_[ 0 ].getPosCoords();
        axisNames_ = new String[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            axisNames_[ i ] = ((FloatingCoord) coords[ i ])
                             .getInput().getMeta().getLongName();
        }
    }

    public DataGeom[] getPointDataGeoms() {
        return dataGeoms_;
    }

    public Plotter<?>[] getPlotters() {
        List<Plotter<?>> list = new ArrayList<Plotter<?>>();
        ShapeForm[] forms = new ShapeForm[] {
            MarkForm.SINGLE,
            SizeForm.getInstance(),
            SizeXyForm.getInstance(),
            CartesianMultiPointForm
           .createVectorForm( "XYZVector",
                              new CartesianVectorCoordSet( axisNames_ ),
                              true ),
            CartesianMultiPointForm
           .createErrorForm( "XYZError",
                             CartesianErrorCoordSet
                            .createAllAxesErrorCoordSet( axisNames_ ),
                             StyleKeys.ERROR_SHAPE_3D ),
            PairLinkForm.getInstance(),
            MarkForm.PAIR,
            PolygonForms.QUAD,
            MarkForm.QUAD,
            PolygonForms.ARRAY,
        };
        Plotter<?>[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_3D );
        list.addAll( Arrays.asList( shapePlotters ) );
        list.addAll( Arrays.asList( new Plotter<?>[] {
            LabelPlotter.POINT_INSTANCE,
            new Line3dPlotter(),
            new ContourPlotter( true ),
            new SphereGridPlotter(),
        } ) );
        return list.toArray( new Plotter<?>[ 0 ] );
    }

    public SurfaceFactory<CubeSurfaceFactory.Profile,CubeAspect>
                          getSurfaceFactory() {
        return SURFACE_FACTORY;
    }

    public GangerFactory<CubeSurfaceFactory.Profile,CubeAspect>
                         getGangerFactory() {
        return SingleGangerFactory.instance();
    }

    public PaperTypeSelector getPaperTypeSelector() {
        return PaperTypeSelector.SELECTOR_3D;
    }

    public String toString() {
        return "cube";
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static CubePlotType getInstance() {
        return INSTANCE;
    }
}
