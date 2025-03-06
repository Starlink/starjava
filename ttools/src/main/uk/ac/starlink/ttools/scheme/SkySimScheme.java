package uk.ac.starlink.ttools.scheme;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.AccessRowSequence;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableScheme;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.filter.AddColumnFilter;
import uk.ac.starlink.ttools.filter.AddSkyCoordsFilter;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.ttools.filter.BasicFilter;
import uk.ac.starlink.ttools.filter.ColumnMetadataFilter;
import uk.ac.starlink.ttools.filter.KeepColumnFilter;
import uk.ac.starlink.ttools.filter.ReplaceColumnFilter;
import uk.ac.starlink.util.URLDataSource;

/**
 * TableScheme that can provide a simulated view of the sky.
 * The current implementation uses data sampled from Gaia EDR3.
 * Of course the real sky is not represented, but at first glance
 * the rough distribution of stars across the sky and some basic
 * photometry is plausible.  The output table can be of arbitrary size.
 * This can therefore be used for certain kinds of test data.
 *
 * @author   Mark Taylor
 * @since    6 Aug 2020
 */
public class SkySimScheme implements TableScheme, Documented {

    private final String tableName_ = "skysimdata.fits";
    private final String cnameWeight_ = "count";
    private final String stdevSuffix_ = "_stdev";
    private SkySimData simData_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.scheme" );

    public String getSchemeName() {
        return "skysim";
    }

    public String getSchemeUsage() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<nrow>" );
        return sbuf.toString();
    }

    public String getXmlDescription() {
        return DocUtils.join( new String[] {
            "<p>Generates a simulated all-sky star catalogue",
            "with a specified number of rows.",
            "This is intended to provide crude test catalogues",
            "when no suitable real dataset of the required size",
            "is available.",
            "In the current implementation the row count,",
            "which may be given in integer or exponential notation,",
            "is the only parameter,",
            "so the specification",
            "\"<code>:" + getSchemeName() + ":5e6</code>\"",
            "would give a 5 million-row simulated catalogue.",
            "</p>",
            "<p>The current implementation provides somewhat realistic",
            "position-dependent star densities and",
            "distributions of magnitudes and colours",
            "based on positionally averaged values from",
            "<a href='https://www.cosmos.esa.int/web/gaia/"
                    + "early-data-release-3'>Gaia EDR3</a>.",
            "The source positions do not correspond to actual stars.",
            "The columns and the statistics on which the output is based",
            "may change in future releases.",
            "</p>"
        } );
    }

    public String getExampleSpecification() {
        return "6";
    }

    public StarTable createTable( String argtxt ) throws IOException {
        final long nrow;
        try {
            nrow = argtxt.length() == 0
                 ? 10000
                 : Tables.parseCount( argtxt );
        }
        catch ( NumberFormatException e ) {
            throw new TableFormatException( "Not numeric: " + argtxt, e );
        }
        StarTable table = createBasicTable( getSimData(), nrow );
        try {
            return filterTable( table );
        }
        catch ( ArgException | IOException e ) {
            logger_.log( Level.WARNING, "SkySim filter failed: " + e, e );
            return table;
        }
    }

    /**
     * Lazily constructs a SkySimData instance holding the data for this scheme.
     *
     * @return  data object
     */
    private synchronized SkySimData getSimData() throws IOException {
        if ( simData_ == null ) {
            simData_ = readSimData();
        }
        return simData_;
    }

    /**
     * Reads a SkySimData instance holding the data for this scheme.
     *
     * @return  data object
     */
    SkySimData readSimData() throws IOException {
        StarTableFactory tfact = new StarTableFactory( true );
        tfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
        URL url = getClass().getResource( tableName_ );
        StarTable table = tfact.makeStarTable( new URLDataSource( url ) );
        assert table.isRandom();
        HealpixTableInfo hpxInfo =
            HealpixTableInfo.fromParams( table.getParameters() );
        return SkySimData
              .readData( table, hpxInfo, cnameWeight_, stdevSuffix_ );
    }

    /**
     * Constructs the basic table containing sampled simulation data.
     *
     * @param  simData  statistics on which to base the simulation
     * @param  nrow    number of rows in table
     * @return   simulation table
     */
    static StarTable createBasicTable( SkySimData simData, long nrow )
            throws IOException {

        /* We convert everything from double to float for compactness,
         * in case the table is output (it doesn't make much difference
         * as long as it's virtual).  Since the values are simulated,
         * the extra sig figs are not relevant.  The positional
         * quantisation is no worse than a few tens of mas, which
         * corresponds to a HEALPix level of around 22. */
        StarTable table =
            new SkySimTable<Float>( nrow, simData, Float.class,
                                    d -> Float.valueOf( (float) d ) );
        table.setName( "SimulatedSky-" + nrow );
        DescribedValue[] params = {
            new DescribedValue(
                new DefaultValueInfo( "Description", String.class, null ),
                "Simulated star catalogue, based on Gaia EDR3" ),
            new DescribedValue(
                new DefaultValueInfo( "HealpixLevel", Integer.class,
                                      "Healpix level of aggregated statistics"),
                Integer.valueOf( simData.getHealpixInfo().getLevel() ) ),
        };
        table.getParameters().addAll( Arrays.asList( params ) );
        return table;
    }

    /**
     * Applies STILTS filter operations to make the table more usable or
     * useful.  The manipulations are not typesafe, so this method may fail.
     *
     * @param   table  input table
     * @return   enhanced table
     */
    static StarTable filterTable( StarTable table )
            throws ArgException, IOException {
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            String ucd = cinfo.getUCD();
            int istat = ucd == null ? -1 : ucd.indexOf( ";stat" );
            if ( istat >= 0 ) {
                cinfo.setUCD( ucd.substring( 0, istat ) );
            }
        }
        table = filter( table, new AddSkyCoordsFilter(),
                        "galactic", "icrs", "l", "b", "ra", "dec" );
        table = filter( table, new ReplaceColumnFilter(),
                        "ra", "(float)ra" );
        table = filter( table, new ReplaceColumnFilter(),
                        "dec", "(float)dec" );
        table = filter( table, new ColumnMetadataFilter(),
                        "-desc", "G magnitude", "gmag" );
        table = filter( table, new ColumnMetadataFilter(),
                        "-desc", "R magnitude", "rmag" );
        table = filter( table, new ColumnMetadataFilter(),
                        "-desc", "B - R colour", "b_r" );
        table = filter( table, new KeepColumnFilter(),
                        "ra dec l b gmag rmag b_r" );
        return table;
    }

    /**
     * Apply a stilts filter operation.
     *
     * @param  table  input table
     * @param  filter   filter instance
     * @param  args   string arguments to filter command
     * @return   output table
     */
    private static StarTable filter( StarTable table, BasicFilter filter,
                                     String... args )
            throws ArgException, IOException {
        Iterator<String> argIt =
            new ArrayList<>( Arrays.asList( args ) ).iterator();
        StarTable out;
        try {
            out = filter.createStep( argIt ).wrap( table );
        }
        catch ( IOException | ArgException e ) {
            logger_.warning( "SkySim filter failed: " + filter.getName()
                           + Arrays.toString( args ) );
            return table;
        }
        if ( argIt.hasNext() ) {
            throw new ArgException( "Unused args from "
                                  + Arrays.toString( args ) );
        }
        return out;
    }

    /**
     * StarTable implementation based on SkySimData.
     *
     * <p>Although the cell values are randomly sampled,
     * they are deterministic for a given table, so that
     * a table with a given number of rows N will always return
     * the same value for a particular cell.
     *
     * @param  <F>  data type of all output columns
     */
    private static class SkySimTable<F extends Number>
            extends AbstractStarTable {

        private final long nrow_;
        private final SkySimData simData_;
        private final DoubleFunction<F> toCell_;
        private final SkySimData.Col[] cols_;
        private final int ncol_;
        private final ColumnInfo[] cinfos_;

        /**
         * Constructor.
         *
         * @param  nrow   row count
         * @param  simData   data defining content
         * @param  toCell   converts a double to the output cell value
         */
        SkySimTable( long nrow, SkySimData simData, Class<F> fclazz,
                     DoubleFunction<F> toCell ) {
            nrow_ = nrow;
            simData_ = simData;
            toCell_ = toCell;
            List<SkySimData.Col> colList = new ArrayList<>();
            colList.add( simData.createCoordColumn( false ) );
            colList.add( simData.createCoordColumn( true ) );
            colList.addAll( Arrays.asList( simData.createQuantityColumns() ) );
            cols_ = colList.toArray( new SkySimData.Col[ 0 ] );
            ncol_ = cols_.length;
            cinfos_ = new ColumnInfo[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                ColumnInfo cinfo = new ColumnInfo( cols_[ icol ].getInfo() );
                Class<?> clazz = cinfo.getContentClass();
                if ( Float.class.equals( clazz ) ||
                     Double.class.equals( clazz ) ) {
                   cinfo.setContentClass( fclazz );
                }
                cinfos_[ icol ] = cinfo;
            }
        }

        public boolean isRandom() {
            return true;
        }

        public long getRowCount() {
            return nrow_;
        }

        public int getColumnCount() {
            return cols_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return cinfos_[ icol ];
        }

        public RowAccess getRowAccess() throws IOException {
            return new RowAccess() {
                long rowSeed_;
                int simdataIrow_ = -1;
                public void setRowIndex( long irow ) throws IOException {
                    rowSeed_ = getRowSeed( irow );
                    simdataIrow_ = simData_.getRandomRowIndex( rowSeed_ );
                }
                public F getCell( int icol ) throws IOException {
                    long cellSeed = getCellSeed( rowSeed_, icol );
                    double dval = cols_[ icol ]
                                 .getValue( simdataIrow_, cellSeed );
                    return toCell_.apply( dval );
                }
                public Object[] getRow() throws IOException {
                    Object[] row = new Object[ ncol_ ];
                    for ( int icol = 0; icol < ncol_; icol++ ) {
                        row[ icol ] = getCell( icol );
                    }
                    return row;
                }
                public void close() throws IOException {
                }
            };
        }

        public RowSequence getRowSequence() throws IOException {
            return AccessRowSequence.createInstance( this );
        }

        public F getCell( long irow, int icol ) throws IOException {
            long rowSeed = getRowSeed( irow );
            long seed = getCellSeed( rowSeed, icol );
            int simdataIrow = simData_.getRandomRowIndex( rowSeed );
            double dval = cols_[ icol ].getValue( simdataIrow, seed );
            return toCell_.apply( dval );
        }

        public Object[] getRow( long irow ) throws IOException {
            long rowSeed = getRowSeed( irow );
            int simdataIrow = simData_.getRandomRowIndex( rowSeed );
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                long seed = getCellSeed( rowSeed, icol );
                double dval = cols_[ icol ].getValue( simdataIrow, seed );
                row[ icol ] = toCell_.apply( dval );
            }
            return row;
        }

        /**
         * Returns a deterministic random seed associated with a given
         * row of this table.
         *
         * @param  irow  row index
         * @return  hash of irow
         */
        private long getRowSeed( long irow ) {
            return ( nrow_ * 1000 + irow ) * -9234789;
        }

        /**
         * Returns a deterministic random seed associated with a given
         * cell of this table.
         *
         * @param  rowSeed   seed for row, acquired from getRowSeed method
         * @param  icol  column index
         * @return   hash of irow,icol
         */
        private long getCellSeed( long rowSeed, int icol ) {
            return ( rowSeed + icol ) * 21192442;
        }
    }
}
