package uk.ac.starlink.topcat.plot2;

import java.io.File;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatTableNamer;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.task.CoordSpec;
import uk.ac.starlink.ttools.plot2.task.LayerSpec;
import uk.ac.starlink.ttools.task.CredibleString;

/**
 * Aggregates information about gang of PlotLayers and some additional
 * information about how it was configured.
 * The plot layer array has one entry per plot zone, but some entries
 * may be null.
 *
 * <p>The resulting object is able to come up with a suitable LayerSpec.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2017
 */
public class TopcatLayer {

    private final PlotLayer[] plotLayers_;
    private final ConfigMap config_;
    private final String leglabel_;
    private final TopcatModel tcModel_;
    private final GuiCoordContent[] contents_;
    private final RowSubset rset_;
    private final Plotter<?> plotter_;
    private final DataGeom dataGeom_;
    private final int izone_;

    /**
     * Constructs a layer based on a table.
     *
     * @param  plotLayers  per-zone array of plot layers,
     *                     at least one non-null member
     * @param  config   configuration used to set up the plot layers
     *                  (superset is permitted)
     * @param  leglabel  label used in the legend;
     *                   if null, excluded from the legend
     * @param  tcModel   TopcatModel containing the table
     * @param  contents  information about data columns used to construct plot
     *                   (superset is not permitted)
     * @param  rset    row subset for which layer is plotted
     */
    public TopcatLayer( PlotLayer[] plotLayers, ConfigMap config,
                        String leglabel, TopcatModel tcModel,
                        GuiCoordContent[] contents, RowSubset rset ) {
        plotLayers_ = plotLayers;
        config_ = config;
        leglabel_ = leglabel;
        tcModel_ = tcModel;
        contents_ = contents == null ? new GuiCoordContent[ 0 ] : contents;
        rset_ = rset;
 
        /* Plotter and DataGeom should be the same for all non-null layers.
         * If there's exactly one zone populated, assign that one as
         * the zone index, otherwise, record no zone index (izone=-1). */
        Plotter<?> plotter = null;
        DataGeom dataGeom = null;
        int izone = -1;
        int nl = 0;
        for ( int iz = 0; iz < plotLayers.length; iz++ ) {
            PlotLayer layer = plotLayers[ iz ];
            if ( layer != null ) {
                nl++;
                izone = iz;
                Plotter<?> p = layer.getPlotter();
                DataGeom dg = layer.getDataGeom();
                assert p == null || plotter == null || p == plotter;
                assert dg == null || dataGeom == null || dg.equals( dataGeom );
                if ( p != null ) {
                    plotter = p;
                }
                if ( dg != null ) {
                    dataGeom = dg;
                }
            }
        }
        assert plotter != null;
        plotter_ = plotter;
        dataGeom_ = dataGeom;
        izone_ = nl == 1 ? izone : -1;
    }

    /**
     * Constructs a layer with no table data.
     *
     * @param  plotLayers  per-zone array of plot layers,
     *                     at least one non-null member
     * @param  config   configuration used to set up the plot layer
     *                  (superset is permitted)
     * @param  leglabel  label used in the legend;
     *                   if null, excluded from the legend
     */
    public TopcatLayer( PlotLayer[] plotLayers, ConfigMap config,
                        String leglabel ) {
        this( plotLayers, config, leglabel, null, null, null );
    }

    /**
     * Returns the plotter used by this layer.
     *
     * @return  plotter
     */
    public Plotter<?> getPlotter() {
        return plotter_;
    }

    /**
     * Returns the DataGeom used by this layer.
     *
     * @return  dataGeom, may be null
     */
    public DataGeom getDataGeom() {
        return dataGeom_;
    }

    /**
     * Returns the plot layers stored by this object.
     *
     * @return  per-zone array of plot layers, at least one non-null member
     */
    public PlotLayer[] getPlotLayers() {
        return plotLayers_;
    }

    /**
     * Returns a layer specification for this layer placed within
     * a given zone.
     *
     * <p>It shouldn't be null, unless it was impossible to write the
     * specification for some reason??
     *
     * @return  layer specification, hopefully not null??
     */
    public LayerSpec getLayerSpec() {
        if ( tcModel_ == null ) {
            return new LayerSpec( plotter_, config_, leglabel_, izone_ );
        }
        else {
            CoordSpec[] coordSpecs =
                GuiCoordContent.getCoordSpecs( contents_ );
            CredibleString selectExpr =
                TopcatTableNamer.getSelectExpression( rset_ );
            StarTable table = TopcatTableNamer.getTable( tcModel_ );
            return new LayerSpec( plotter_, config_, leglabel_, izone_,
                                  table, coordSpecs, dataGeom_, selectExpr );
        }
    }
}
