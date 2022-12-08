package uk.ac.starlink.ttools.example;

import java.awt.Color;
import java.io.IOException;
import java.util.function.DoubleUnaryOperator;
import javax.swing.Icon;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.SimpleDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlaneNavigator;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.layer.BinSizer;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.plot2.layer.Cumulation;
import uk.ac.starlink.ttools.plot2.layer.HistogramPlotter;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.MarkerShape;
import uk.ac.starlink.ttools.plot2.layer.Normalisation;
import uk.ac.starlink.ttools.plot2.layer.Outliner;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapeStyle;
import uk.ac.starlink.ttools.plot2.layer.Stamper;
import uk.ac.starlink.ttools.plot2.layer.Unit;
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

    public PlotDisplay<?,?> createPlotComponent( StarTable table,
                                                 boolean dataMayChange )
            throws InterruptedException, IOException {

        /* Prepare an object which knows how to draw the plot from the table. */
        PlotGenerator<PlaneSurfaceFactory.Profile,PlaneAspect>
                      plotGen = createPlotGenerator( table );

        /* Set up a Navigator which determines what mouse gestures are
         * available to the user for plot pan/zoom etc. */
        Navigator<PlaneAspect> navigator = createPlaneNavigator();

        /* Configure data access. */
        boolean surfaceAuxRange = false;
        boolean caching = ! dataMayChange;

        /* Finally construct and return the component. */
        return plotGen.createPlotDisplay( navigator, surfaceAuxRange, caching );
    }

    /**
     * Constructs a PlotGenerator that contains the details of the plot
     * to be done.  This method does the work for specifying the plot.
     *
     * @param  table  input data
     * @return   plot based on the first two columns of the table
     */
    private PlotGenerator<PlaneSurfaceFactory.Profile,PlaneAspect>
                          createPlotGenerator( StarTable table )
            throws IOException, InterruptedException {

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
        DoubleUnaryOperator x2func = null;
        DoubleUnaryOperator y2func = null;
        String x2label = null;
        String y2label = null;
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
                                             xlabel, ylabel, x2func, y2func,
                                             x2label, y2label, captioner,
                                             xyfactor, grid, xcrowd, ycrowd,
                                             minor, gridColor, axlabelColor );

        /* Set up a plot Aspect.  This is the initial data range,
         * and is subject to change by user navigation. */
        double[] xlimits = new double[] { 0, 1 };
        double[] ylimits = new double[] { -1.2, 1.2 };
        PlaneAspect aspect = new PlaneAspect( xlimits, ylimits );

        /* We will not use optional decorations for this plot. */
        Icon legend = null;
        float[] legPos = null;
        String title = null;
        ShadeAxisFactory shadeFact = null;
        Span shadeFixSpan = null;

        /* Prepare the list of plot layers; in this case there is only one. */
        PlotLayer[] layers = { createScatterLayer( geom, table), };

        /* Prepare the data cache. */
        int nl = layers.length;
        DataSpec[] dataSpecs = new DataSpec[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            dataSpecs[ il ] = layers[ il ].getDataSpec();
        }
        TupleRunner tupleRunner = TupleRunner.DEFAULT;
        DataStoreFactory storeFact = new SimpleDataStoreFactory( tupleRunner );
        DataStore dataStore = storeFact.readDataStore( dataSpecs, null );

        /* Rendering details. */
        Compositor compositor = Compositor.SATURATION;
        PaperTypeSelector ptSel = plotType.getPaperTypeSelector();

        /* Dimensions. */
        int xpix = 500;
        int ypix = 400;
        Padding padding = new Padding();

        /* Construct and return the plot generator. */
        return new PlotGenerator<PlaneSurfaceFactory.Profile,PlaneAspect>
                                ( layers, surfFact, profile, aspect,
                                  legend, legPos, title, shadeFact,
                                  shadeFixSpan, ptSel, compositor,
                                  dataStore, xpix, ypix, padding );
    }

    /**
     * Constructs and returns a navigator for interpreting user mouse
     * gestures to move around the plot.  Note that anisotropic pan/zoom
     * are available with wheel/drag gestures outside the plot axes.
     *
     * @return  navigator object suitable for a 2d plot
     */
    private Navigator<PlaneAspect> createPlaneNavigator() {

        /* This sets the zoom factor to its standard default value,
         * as retrieved from the ZOOM_FACTOR configuration option
         * (currently equal to 1.2).  You could just write a literal
         * value in here instead, but this idiom is sometimes useful
         * (not just for Navigator construction) if you want to use the
         * standard library value for one of the configuration options. */
        double zoomFactor = StyleKeys.ZOOM_FACTOR.getDefaultValue();

        /* Set the other options explicitly. */
        boolean xZoom = true;
        boolean yZoom = true;
        boolean xPan = true;
        boolean yPan = true;
        double xAnchor = Double.NaN;
        double yAnchor = Double.NaN;
        return new PlaneNavigator( zoomFactor,
                                   xZoom, yZoom, xPan, yPan, xAnchor, yAnchor );
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
        MarkerShape shape = MarkerShape.OPEN_CIRCLE;
        int size = 2;
        Outliner outliner = MarkForm.createMarkOutliner( shape, size );
        Stamper stamper = new ShapeMode.FlatStamper( Color.RED );
        ShapeStyle style = new ShapeStyle( outliner, stamper );

        /* Combine the data and style to generate a scatter plot layer. */
        Plotter<ShapeStyle> plotter =
            ShapePlotter.createFlat2dPlotter( MarkForm.SINGLE );
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
        Cumulation cumulative = Cumulation.NONE;
        Normalisation norm = Normalisation.NONE;
        Unit unit = Unit.UNIT;
        int thick = 1;
        float[] dash = null;
        BinSizer sizer = BinSizer.createCountBinSizer( 16 );
        double phase = 0;
        Combiner combiner = Combiner.SUM;
        HistogramPlotter.HistoStyle style =
            new HistogramPlotter.HistoStyle( color, barForm, placement,
                                             cumulative, norm, unit, thick,
                                             dash, sizer, phase, combiner );

        /* Combine data and style to generate a histogram plot layer. */
        Plotter<HistogramPlotter.HistoStyle> plotter =
            new HistogramPlotter( PlaneDataGeom.X_COORD, false, null );
        return plotter.createLayer( geom, dataSpec, style );
    }
}
