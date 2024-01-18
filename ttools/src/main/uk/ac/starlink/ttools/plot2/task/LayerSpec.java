package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.task.CredibleString;

/**
 * Specifies a plot layer in sufficient detail to recreate it as
 * part of a STILTS plotting command.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2017
 * @see  PlotSpec
 */
public class LayerSpec {

    private final Plotter<?> plotter_;
    private final ConfigMap config_;
    private final String leglabel_;
    private final int izone_;
    private final StarTable table_;
    private final CoordSpec[] coordSpecs_;
    private final DataGeom dataGeom_;
    private final CredibleString selectExpr_;

    /**
     * Constructs a layer specification for a layer with no table data.
     *
     * @param   plotter  plotter
     * @param   config   per-layer configuration; superset is permitted
     * @param   leglabel legend label, or null to exclude from legend
     * @param   izone    zone index
     */
    public LayerSpec( Plotter<?> plotter, ConfigMap config, String leglabel,
                      int izone ) {
        this( plotter, config, leglabel, izone, null, null, null, null );
    }

    /**
     * Constructor.
     *
     * @param   plotter  plotter
     * @param   config   per-layer configuration; superset is permitted
     * @param   leglabel legend label, or null to exclude from legend
     * @param   izone    zone index
     * @param   table    table supplying data points;
     *                   where a string representation of the table is required,
     *                   its <code>getName</code> method will generally be used
     * @param   coordSpecs  list of coordinate specifications
     * @param   dataGeom  data geom
     * @param   selectExpr  boolean expression evaluated in the context of
     *                      the supplied table; if non-null, only true rows
     *                      are included
     */
    public LayerSpec( Plotter<?> plotter, ConfigMap config, String leglabel,
                      int izone, StarTable table, CoordSpec[] coordSpecs,
                      DataGeom dataGeom, CredibleString selectExpr ) {
        plotter_ = plotter;
        config_ = config;
        leglabel_ = leglabel;
        izone_ = izone;
        table_ = table;
        coordSpecs_ = coordSpecs == null ? new CoordSpec[ 0 ] : coordSpecs;
        dataGeom_ = dataGeom;
        selectExpr_ = selectExpr;
    }

    /**
     * Returns this layer's plotter.
     *
     * @return  plotter
     */
    public Plotter<?> getPlotter() {
        return plotter_;
    }

    /**
     * Returns this layer's configuration options.
     *
     * @return   config
     */
    public ConfigMap getConfig() {
        return config_;
    }

    /**
     * Returns the legend label associated with this layer.
     * If the return value is null, then this layer should not be
     * represented in a legend, even if the legend is displayed.
     *
     * @return  legend label
     */
    public String getLegendLabel() {
        return leglabel_;
    }

    /**
     * Returns the index of the zone in which this layer is placed.
     *
     * @return  zone index
     */
    public int getZoneIndex() {
        return izone_;
    }

    /**
     * Returns the table supplying this layer's data.
     *
     * @return  table, may be null
     */
    public StarTable getTable() {
        return table_;
    }

    /**
     * Coordinate specifications used by this layer.
     *
     * @return  coordinate specification list, not null
     */
    public CoordSpec[] getCoordSpecs() {
        return coordSpecs_;
    }

    /**
     * Returns the data geom used by this layer.
     *
     * @return  data geom
     */
    public DataGeom getDataGeom() {
        return dataGeom_;
    }

    /**
     * Returns an expression that indicates row inclusion for the table.
     * This is an expression to be evaluated in the context of the
     * supplied data table.  If null, all rows are considered to be
     * included.
     *
     * @return   row selection expression
     */
    public CredibleString getSelectExpr() {
        return selectExpr_;
    }
}
