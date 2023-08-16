package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SingleGangerFactory;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.AreaForm;
import uk.ac.starlink.ttools.plot2.layer.CentralForm;
import uk.ac.starlink.ttools.plot2.layer.ContourPlotter;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.PairLinkForm;
import uk.ac.starlink.ttools.plot2.layer.PolygonForms;
import uk.ac.starlink.ttools.plot2.layer.SizeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SkyCorrelationCoordSet;
import uk.ac.starlink.ttools.plot2.layer.SkyDensityPlotter;
import uk.ac.starlink.ttools.plot2.layer.SkyGridPlotter;
import uk.ac.starlink.ttools.plot2.layer.SkyVectorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.SkyEllipseCoordSet;
import uk.ac.starlink.ttools.plot2.layer.SizeXyForm;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Defines the characteristics of a plot on the surface of the
 * celestial sphere.
 *
 * <p>This is a singleton class, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2013
 */
public class SkyPlotType
        implements PlotType<SkySurfaceFactory.Profile,SkyAspect> {

    private final DataGeom[] dataGeoms_;
    private static final SkyPlotType INSTANCE = new SkyPlotType();

    private static final SkySurfaceFactory SURFACE_FACTORY =
        new SkySurfaceFactory();

    /**
     * Private singleton constructor.
     */
    private SkyPlotType() {
        dataGeoms_ = new DataGeom[] {
            SkyDataGeom.GENERIC,
        };
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
            SkyVectorCoordSet.createForm(),
            SkyEllipseCoordSet.createForm(),
            SkyCorrelationCoordSet.createForm(),
            PairLinkForm.getInstance(),
            MarkForm.PAIR,
            PolygonForms.QUAD,
            MarkForm.QUAD,
            PolygonForms.ARRAY,
            AreaForm.SKY_INSTANCE,
            CentralForm.SKY_INSTANCE,
        };
        Plotter<?>[] shapePlotters =
            ShapePlotter.createShapePlotters( forms, ShapeMode.MODES_2D );
        list.addAll( Arrays.asList( shapePlotters ) );
        list.addAll( Arrays.asList( new Plotter<?>[] {
            LabelPlotter.POINT_INSTANCE,
            LabelPlotter.AREA_SKY_INSTANCE,
            new ContourPlotter( true ),
            new SkyDensityPlotter( true, true ),
            new HealpixPlotter( true ),
            new SkyGridPlotter(),
        } ) );
        return list.toArray( new Plotter<?>[ 0 ] );
    }

    public SurfaceFactory<SkySurfaceFactory.Profile,SkyAspect>
            getSurfaceFactory() {
        return SURFACE_FACTORY;
    }

    public GangerFactory<SkySurfaceFactory.Profile,SkyAspect>
            getGangerFactory() {
        return SingleGangerFactory.instance();
    }

    public PaperTypeSelector getPaperTypeSelector() {
        return PaperTypeSelector.SELECTOR_2D;
    }

    public String toString() {
        return "sky";
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static SkyPlotType getInstance() {
        return INSTANCE;
    }
}
