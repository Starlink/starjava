package uk.ac.starlink.ttools.plot2.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.layer.CartesianErrorCoordSet;
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
import uk.ac.starlink.ttools.plot2.layer.MultiPointForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.Stats1Plotter;
import uk.ac.starlink.ttools.plot2.layer.Unit;
import uk.ac.starlink.ttools.plot2.layer.TracePlotter;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Defines the characteristics of a 2-d plot with a horizontal time axis.
 *
 * <p>This is a singleton class, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class TimePlotType
        implements PlotType<TimeSurfaceFactory.Profile,TimeAspect> {

    private static final TimePlotType INSTANCE = new TimePlotType();
    private static final TimeSurfaceFactory SURFACE_FACTORY =
        new TimeSurfaceFactory();

    /**
     * Private constructor for singleton.
     */
    private TimePlotType() {
    }

    public DataGeom[] getPointDataGeoms() {
        return new DataGeom[] { TimeDataGeom.INSTANCE };
    }

    public Plotter<?>[] getPlotters() {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots symmetric or asymmetric error bars in the Y direction.",
            "</p>",
        } );
        MultiPointForm errorForm =
            MultiPointForm
           .createDefaultForm( "YError", ResourceIcon.FORM_ERROR1, descrip,
                               CartesianErrorCoordSet
                              .createSingleAxisErrorCoordSet( 2, 1, "Y" ),
                               StyleKeys.ERROR_SHAPE_1D, false );
        ShapeForm[] modeForms = new ShapeForm[] { MarkForm.SINGLE };
        List<Plotter<?>> plotters = new ArrayList<Plotter<?>>();
        PerUnitConfigKey<Unit> unitKey = TimeUnit.createHistogramConfigKey();
        plotters.addAll( Arrays.asList( new Plotter<?>[] {
            new LinePlotter( LinePlotter.TIME_SORTAXIS_KEY ),
            new LinearFitPlotter( true ),
        } ) );
        plotters.addAll( Arrays
                        .asList( ShapePlotter
                                .createShapePlotters( modeForms,
                                                      ShapeMode.MODES_2D ) ) );
        plotters.addAll( Arrays.asList( new Plotter<?>[] {
            new FillPlotter( false ),
            TracePlotter.createPointsTracePlotter( false ),
            new GridPlotter( true ),
            new HistogramPlotter( TimeDataGeom.T_COORD, true, unitKey ),
            new FixedKernelDensityPlotter( TimeDataGeom.T_COORD, true,
                                           unitKey ),
            new KnnKernelDensityPlotter( TimeDataGeom.T_COORD, true, unitKey ),
            new DensogramPlotter( TimeDataGeom.T_COORD, true ),
            new Stats1Plotter( TimeDataGeom.T_COORD, true, unitKey ),
            ShapePlotter.createFlat2dPlotter( errorForm ),
            new SpectrogramPlotter( TimeDataGeom.T_COORD ),
            LabelPlotter.POINT_INSTANCE,
            FunctionPlotter.PLANE,
        } ) );
        return plotters.toArray( new Plotter<?>[ 0 ] );
    }

    public SurfaceFactory<TimeSurfaceFactory.Profile,TimeAspect>
                          getSurfaceFactory() {
        return SURFACE_FACTORY;
    }

    public PaperTypeSelector getPaperTypeSelector() {
        return PaperTypeSelector.SELECTOR_2D;
    }

    public String toString() {
        return "time";
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static TimePlotType getInstance() {
        return INSTANCE;
    }
}
