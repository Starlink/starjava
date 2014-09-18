package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.layer.CartesianErrorCoordSet;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.LabelPlotter;
import uk.ac.starlink.ttools.plot2.layer.LinePlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.MultiPointForm;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Defines the characteristics of a 2-d plot with a horizontal time axis.
 *
 * <p>This is a singleton class, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class TimePlotType implements PlotType {

    private static final TimePlotType INSTANCE = new TimePlotType();
    private static final SurfaceFactory SURFACE_FACTORY =
        new TimeSurfaceFactory();

    /**
     * Private constructor for singleton.
     */
    private TimePlotType() {
    }

    public DataGeom[] getPointDataGeoms() {
        return new DataGeom[] { TimeDataGeom.INSTANCE };
    }

    public Plotter[] getPlotters() {
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots symmetric or asymmetric error bars in the Y direction.",
            "</p>",
        } );
        MultiPointForm errorForm =
            new MultiPointForm( "YError", ResourceIcon.FORM_ERROR1, descrip,
                                CartesianErrorCoordSet
                               .createSingleAxisErrorCoordSet( 2, 1, "Y" ),
                                false, StyleKeys.ERROR_SHAPE_1D );
        return new Plotter[] {
            new LinePlotter(),
            ShapePlotter.createFlat2dPlotter( MarkForm.SINGLE ),
            ShapePlotter.createFlat2dPlotter( errorForm ),
            new SpectrogramPlotter( TimeDataGeom.T_COORD ),
            new LabelPlotter(),
            FunctionPlotter.PLANE,
        };
    }

    public SurfaceFactory getSurfaceFactory() {
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
