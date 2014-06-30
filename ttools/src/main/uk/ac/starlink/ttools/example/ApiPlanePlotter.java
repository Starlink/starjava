package uk.ac.starlink.ttools.example;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.SimpleDataStoreFactory;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlaneNavigator;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.Outliner;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapeStyle;
import uk.ac.starlink.ttools.plot2.layer.Stamper;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plot2.task.ColumnDataSpec;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;

/**
 * PlanePlotter implementation that sets up a plot explicitly.
 * There's a lot to do, it's quite complicated, even for a simple plot.
 * If you don't like this approach, see the much more straightforward
 * {@link EnvPlanePlotter} implementation instead.
 * This approach however gives compile-time type-checking of the
 * plot parameters. 
 *
 * @author   Mark Taylor
 * @since    12 Jun 2014
 */
public class ApiPlanePlotter implements SinePlot.PlanePlotter {

    public JComponent createPlotComponent( StarTable table,
                                           boolean dataMayChange )
            throws InterruptedException, IOException {

        /* It's a 2d plot. */
        PlanePlotType plotType = PlanePlotType.getInstance();
        DataGeom geom = plotType.getPointDataGeoms()[ 0 ];

        /* Create the Profile for the plot surface.  This encapsulates
         * those things about the geometry and appearance of the plot
         * axes which will not change with window resizing, zooming etc. */
        PlaneSurfaceFactory surfFact = new PlaneSurfaceFactory();
        boolean xlog = false;
        boolean ylog = false;
        boolean xflip = false;
        boolean yflip = false;
        String xlabel = "X axis";
        String ylabel = "Y axis";
        Captioner captioner = new BasicCaptioner();
        double xyfactor = Double.NaN;
        boolean grid = false;
        double xcrowd = 1;
        double ycrowd = 1;
        boolean minor = true;
        Color gridColor = Color.BLACK;
        Color axlabelColor = Color.BLACK;
        PlaneSurfaceFactory.Profile profile =
            new PlaneSurfaceFactory.Profile( xlog, ylog, xflip, yflip,
                                             xlabel, ylabel, captioner,
                                             xyfactor, grid, xcrowd, ycrowd,
                                             minor, gridColor, axlabelColor );

        /* Set up a plot Aspect.  This is the initial data range,
         * and is subject to change by user navigation. */
        double[] xlimits = new double[] { 0, 1 };
        double[] ylimits = new double[] { -1.2, 1.2 };
        PlaneAspect aspect = new PlaneAspect( xlimits, ylimits );

        /* Set up a Navigator which determines what mouse gestures are
         * available to the user for plot pan/zoom etc.  Note that
         * anisotropic pan/zoom are available with wheel/drag gestures
         * outside the plot axes. */
        double zoomFactor = StyleKeys.ZOOM_FACTOR.getDefaultValue();
        boolean xZoom = true;
        boolean yZoom = true;
        boolean xPan = true;
        boolean yPan = true;
        double xAnchor = Double.NaN;
        double yAnchor = Double.NaN;
        Navigator<PlaneAspect> navigator =
            new PlaneNavigator( zoomFactor,
                                xZoom, yZoom, xPan, yPan, xAnchor, yAnchor );

        /* We will not use optional decorations for this plot. */
        Icon legend = null;
        float[] legPos = null;
        ShadeAxis shadeAxis = null;
        Range shadeFixRange = null;
        boolean surfaceAuxRange = false;

        /* Prepare the list of plot layers; in this case there is only one. */
        PlotLayer[] layers = { createScatterLayer( geom, table), };

        /* Prepare the data cache. */
        int nl = layers.length;
        DataSpec[] dataSpecs = new DataSpec[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            dataSpecs[ il ] = layers[ il ].getDataSpec();
        }
        DataStoreFactory storeFact = new SimpleDataStoreFactory();
        DataStore dataStore = storeFact.readDataStore( dataSpecs, null );
        boolean caching = ! dataMayChange;

        /* Finally construct, size and return the plot component. */
        Compositor compositor = Compositor.SATURATION;
        PaperTypeSelector ptSel = plotType.getPaperTypeSelector();
        JComponent comp =
            new PlotDisplay<PlaneSurfaceFactory.Profile,PlaneAspect>
                           ( layers, surfFact, profile, aspect, legend, legPos,
                             shadeAxis, shadeFixRange, ptSel, compositor,
                             dataStore, surfaceAuxRange, navigator, caching );
        comp.setPreferredSize( new Dimension( 500, 400 ) );
        return comp;
    }

    /**
     * Returns a plot layer plotting the first two columns of a given table
     * against each other.
     *
     * @param  geom  data geom
     * @param  table  data table
     * @return  new layer
     */
    private PlotLayer createScatterLayer( DataGeom geom, StarTable table ) {

        /* Prepare the data for the scatter plot layer: use the first
         * two columns of the supplied table as X and Y.*/
        DataSpec dataSpec =
            new ColumnDataSpec( table, geom.getPosCoords(),
                                new int[][] { { 0 }, { 1 } } );

        /* Prepare the graphical style of the scatter plot layer:
         * it's a scatter plot with single-position markers, plotted
         * in a single fixed colour. */
        MarkShape shape = MarkShape.OPEN_CIRCLE;
        int size = 2;
        Outliner outliner = MarkForm.createMarkOutliner( shape, size );
        Stamper stamper = new ShapeMode.FlatStamper( Color.RED );
        ShapeStyle style = new ShapeStyle( outliner, stamper );

        /* Combine the data and style to generate a scatter plot layer. */
        Plotter plotter = ShapePlotter.createFlat2dPlotter( MarkForm.SINGLE );
        return plotter.createLayer( geom, dataSpec, style );
    }

    /**
     * Returns a plot layer plotting the first column of a given table
     * as a histogram.
     *
     * @param  geom  data geom
     * @param  table  data table
     * @return  new layer
     */
    private PlotLayer createHistogramLayer( DataGeom geom, StarTable table ) {

        /* Prepare the data. */
        DataSpec dataSpec =
            new ColumnDataSpec( table, new Coord[] { PlaneDataGeom.X_COORD },
                                new int[][] { { 1 } } );

        /* Prepare the style. */
        Color color = Color.BLUE;
        BarStyle.Form barForm = BarStyle.FORM_OPEN;
        BarStyle.Placement placement = BarStyle.PLACE_ADJACENT;
        boolean cumulative = true;
        boolean norm = true;
        int thick = 1;
        float[] dash = null;
        BinSizer sizer = BinSizer.createCountBinSizer( 16, true );
        double phase = 0;
        HistogramPlotter.HistoStyle style =
            new HistogramPlotter.HistoStyle( color, barForm, placement,
                                             cumulative, norm, thick, dash,
                                             sizer, phase );

        /* Combine data and style to generate a histogram plot layer. */
        Plotter plotter = new HistogramPlotter( PlaneDataGeom.X_COORD, false );
        return plotter.createLayer( geom, dataSpec, style );
    }
}
