package uk.ac.starlink.topcat.plot2;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BooleanColumnRowSubset;
import uk.ac.starlink.topcat.InverseRowSubset;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SingleRowSubset;
import uk.ac.starlink.topcat.SyntheticRowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.task.CoordSpec;
import uk.ac.starlink.ttools.plot2.task.LayerSpec;
import uk.ac.starlink.ttools.task.Credibility;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.util.URLUtils;

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
    public static final TopcatNamer PATHNAME_NAMER;
    public static final TopcatNamer FILENAME_NAMER;
    public static final TopcatNamer LABEL_NAMER;
    public static final TopcatNamer TNUM_NAMER;
    private static final TableNamer[] TABLENAMERS = new TableNamer[] {
        PATHNAME_NAMER = new TopcatNamer( "Pathname", true ),
        FILENAME_NAMER = new TopcatNamer( "Filename", true ),
        LABEL_NAMER = new TopcatNamer( "Label", true ),
        TNUM_NAMER = new TopcatNamer( "TNum", false ),
    };
    private static final ValueInfo FORMAT_INFO =
        new DefaultValueInfo( TopcatLayer.class.getName() + "TableBuilder",
                              TableBuilder.class );

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
            CredibleString selectExpr = getSelectExpression( rset_ );
            StarTable table = getLayerTable( tcModel_ );
            return new LayerSpec( plotter_, config_, leglabel_, izone_,
                                  table, coordSpecs, dataGeom_, selectExpr );
        }
    }

    /**
     * Returns a list of TableNamer objects that give the user options for
     * referencing TopcatModels by a text string in generated stilts commands.
     * The stilts commands are assumed to have been specified using
     * methods in this class.
     *
     * @return  table namer user options
     */
    public static TableNamer[] getLayerTableNamers() {
        return TABLENAMERS;
    }

    /**
     * Returns a best effort at an expression indicating row selection
     * corresponding to a given RowSubset.
     * In some cases, for instance a subset defined by a bitmap,
     * there's no way to do this that will result in an evaluatable
     * expression, so in those cases just return the subset name or something.
     *
     * @param  rset  row subset
     * @return   attempt at expression giving row inclusion, not null
     */
    private static CredibleString getSelectExpression( RowSubset rset ) {
        if ( rset == null ) {
            return null;
        }
        else if ( rset.equals( RowSubset.ALL ) ) {
            return null;
        }
        else if ( rset.equals( RowSubset.NONE ) ) {
            return new CredibleString( "false", Credibility.YES );
        }
        else if ( rset instanceof SyntheticRowSubset ) {
            return new CredibleString( ((SyntheticRowSubset) rset)
                                       .getExpression(), Credibility.MAYBE );
        }
        else if ( rset instanceof BooleanColumnRowSubset ) {
            BooleanColumnRowSubset cset = (BooleanColumnRowSubset) rset;
            String expr = cset.getTable()
                         .getColumnInfo( cset.getColumnIndex() ).getName();
            return new CredibleString( expr, Credibility.YES );
        }
        else if ( rset instanceof SingleRowSubset ) {
            SingleRowSubset sset = (SingleRowSubset) rset;
            return new CredibleString( "$0==" + sset.getRowIndex(),
                                       Credibility.YES );
        }
        else if ( rset instanceof InverseRowSubset ) {
            CredibleString invResult =
                getSelectExpression( ((InverseRowSubset) rset)
                                    .getInvertedSubset() );
            return new CredibleString( "!(" + invResult.getValue() + ")",
                                       invResult.getCredibility() );
        }
        else {
            return new CredibleString( "<" + rset.getName() + ">",
                                       Credibility.NO );
        }
    }

    /**
     * Returns a table corresponding to the current apparent table of
     * a topcat model.  Its parameter list also contains parameters
     * giving various naming options corresponding to the FileNamer
     * instances defined by this class.
     *
     * @param   tcModel  topcat model
     * @return   table view
     */
    private static StarTable getLayerTable( TopcatModel tcModel ) {
        List<DescribedValue> params = new ArrayList<DescribedValue>();
        params.add( new DescribedValue( FORMAT_INFO,
                                        tcModel.getTableFormat() ) );
        params.add( TNUM_NAMER
                   .createNameParam( "T" + tcModel.getID(), Credibility.NO ) );
        params.add( LABEL_NAMER
                   .createNameParam( tcModel.getLabel(), Credibility.MAYBE ) );
        String loc = tcModel.getLocation();
        URL url;
        try {
            url = new URL( loc );
        }
        catch ( MalformedURLException e ) {
            url = null;
        }
        File file = url == null ? null
                                : URLUtils.urlToFile( url.toString() );
        if ( file == null ) {
            file = new File( loc );
        }
        final CredibleString filename;
        final CredibleString pathname;
        if ( url != null ) {
            filename = new CredibleString( file.getName(), Credibility.NO );
            pathname = new CredibleString( loc, Credibility.YES );
        }
        else if ( file.exists() ) {
            filename = new CredibleString( file.getName(), Credibility.MAYBE );
            pathname = new CredibleString( file.getAbsolutePath(),
                                           Credibility.YES );
        }
        else {
            filename = new CredibleString( loc, Credibility.NO );
            pathname = filename;
        }
        params.add( FILENAME_NAMER.createNameParam( filename ) );
        params.add( PATHNAME_NAMER.createNameParam( pathname ) );
        StarTable table =
            new MetaCopyStarTable( tcModel.getViewModel().getSnapshot() );
        table.getParameters().addAll( params );
        return table;
    }

    /**
     * TableNamer implementation for use by this class.
     * An instance of this class can be used to prepare a DescribedValue
     * to be stashed in the Parameter list of a StarTable, where the
     * value is the name to be used for that table.
     */
    private static class TopcatNamer implements TableNamer {
        final String name_;
        final ValueInfo nameInfo_;
        final boolean hasFormat_;

        /**
         * Constructor.
         *
         * @param  name   TableNamer user name
         * @param  hasFormat   whether to report table format when available
         */
        TopcatNamer( String name, boolean hasFormat ) {
            name_ = name;
            hasFormat_ = hasFormat;
            String paramName = TopcatLayer.class.getName() + "_" + name;
            nameInfo_ = new DefaultValueInfo( paramName, CredibleString.class );
        }

        /**
         * Returns an object to be stashed in a table's parameter list
         * giving the table name.
         *
         * @param  credStr  value
         * @return  described value
         */
        DescribedValue createNameParam( CredibleString credStr ) {
            return new DescribedValue( nameInfo_, credStr );
        }

        /**
         * Returns an object to be stashed in a table's parameter list
         * giving the table name.
         *
         * @param  str   table name string
         * @param  cred  table name credibility
         * @return  described value
         */
        DescribedValue createNameParam( String str, Credibility cred ) {
            return createNameParam( new CredibleString( str, cred ) );
        }

        public CredibleString nameTable( StarTable table ) {
            Object value = Tables.getValue( table.getParameters(), nameInfo_ );
            return value instanceof CredibleString
                 ? (CredibleString) value
                 : new CredibleString( "???", Credibility.NO );
        }

        public TableBuilder getTableFormat( StarTable table ) {
            if ( hasFormat_ ) {
                Object fmt = Tables.getValue( table.getParameters(),
                                              FORMAT_INFO );
                return fmt instanceof TableBuilder
                     ? (TableBuilder) fmt
                     : null;
            }
            else {
                return null;
            }
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}
