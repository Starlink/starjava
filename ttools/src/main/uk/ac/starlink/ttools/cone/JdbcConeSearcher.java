package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.SelectorStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.jdbc.SequentialResultSetStarTable;
import uk.ac.starlink.ttools.filter.AddColumnsTable;
import uk.ac.starlink.ttools.filter.CalculatorColumnSupplement;
import uk.ac.starlink.ttools.filter.ColumnSupplement;
import uk.ac.starlink.ttools.filter.PermutedColumnSupplement;
import uk.ac.starlink.ttools.func.CoordsDegrees;

/**
 * ConeSearcher implementation using JDBC access to an SQL database.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2007
 */
public class JdbcConeSearcher implements ConeSearcher {

    private final ConeSelector selector_;
    private final String raCol_;
    private final String decCol_;
    private final AngleUnits units_;
    private final String tileCol_;
    private final SkyTiling tiling_;
    private final Connection connectionToClose_;
    private boolean first_ = true;
    private int raIndex_ = -1;
    private int decIndex_ = -1;
    private int raRsetIndex_ = -1;
    private int decRsetIndex_ = -1;
    private int tileRsetIndex_ = -1;

    private static final ValueInfo RADEG_INFO =
        new DefaultValueInfo( "RA_DEGREES", Double.class,
                              "Right ascension in degrees" );
    private static final ValueInfo DECDEG_INFO =
        new DefaultValueInfo( "DEC_DEGREES", Double.class,
                              "Declination in degrees" );
    static {
        ((DefaultValueInfo) RADEG_INFO).setUnitString( "deg" );
        ((DefaultValueInfo) DECDEG_INFO).setUnitString( "deg" );
    }
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param  connection   live connection to database
     * @param  tableName  name of a table in the database to search
     * @param  raCol  name of table column containing right ascension
     * @param  decCol name of table column containing declination
     * @param  units  angular units used by ra and dec columns
     * @param  tileCol column containing a sky tiling index value, or null
     * @param  tiling tiling scheme used by tileCol column
     * @param  cols   list of column names for the SELECT statement
     * @param  where  additional WHERE clause constraints
     * @param  bestOnly  true iff only the closest match is required (hint)
     * @param  prepareSql  whether to use PreparedStatements or not
     * @param  closeConnection  whether to close the connection when this
     *         object is closed
     */
    public JdbcConeSearcher( Connection connection, String tableName,
                             String raCol, String decCol, AngleUnits units,
                             String tileCol, SkyTiling tiling,
                             String cols, String where, boolean bestOnly,
                             boolean prepareSql, boolean closeConnection )
            throws SQLException {
        raCol_ = raCol;
        decCol_ = decCol;
        units_ = units;
        tileCol_ = tileCol;
        tiling_ = tiling;
        connectionToClose_ = closeConnection ? connection : null;
        selector_ = ( tiling != null && tileCol != null )
               ? ConeSelector.createTiledSelector( connection, tableName, raCol,
                                                   decCol, units, cols, where,
                                                   tileCol, tiling, prepareSql )
               : ConeSelector.createSelector( connection, tableName, raCol,
                                              decCol, units, cols, where,
                                              prepareSql );
    }

    public StarTable performSearch( final double ra, final double dec,
                                    final double sr )
            throws IOException {

        /* Execute the statement and turn it into a StarTable. */
        ResultSet rset;
        try {
            rset = selector_.executeQuery( ra, dec, sr );
        }
        catch ( SQLException e ) {
            throw (IOException)
                  new IOException( "Error executing SQL statement: "
                                 + e.getMessage() )
                 .initCause( e );
        }
        StarTable rsetTable;
        try {
            rsetTable = new SequentialResultSetStarTable( rset );
        }
        catch ( SQLException e ) {
            throw (IOException)
                  new IOException( "Error retrieving data from SQL "
                                 + "statement: " + e.getMessage() )
                 .initCause( e );
        }
        int ncolRset = rsetTable.getColumnCount();

        /* Identify the columns containing RA and Dec first time around
         * (it should be the same for every query, so we only do it once). */
        boolean convertAngles = ! AngleUnits.DEGREES.equals( units_ );
        final double angleFactor =
            AngleUnits.DEGREES.getCircle() / units_.getCircle();
        if ( first_ ) {
            first_ = false;
            try {
                raRsetIndex_ = rset.findColumn( raCol_ ) - 1;
                decRsetIndex_ = rset.findColumn( decCol_ ) - 1;
                if ( convertAngles ) {
                    raIndex_ = ncolRset;
                    decIndex_ = ncolRset + 1;
                }
                else {
                    raIndex_ = raRsetIndex_;
                    decIndex_ = decRsetIndex_;
                }
            }
            catch ( SQLException e ) {
                logger_.warning( "Cannot identify ra/dec columns" );
                raRsetIndex_ = -1;
                decRsetIndex_ = -1;
                raIndex_ = -1;
                decIndex_ = -1;
            }
            if ( tileCol_ != null ) {
                try {
                    tileRsetIndex_ = rset.findColumn( tileCol_ ) - 1;
                }
                catch ( SQLException e ) {
                    logger_.warning( "Cannot identify tile column" );
                    tileRsetIndex_ = -1;
                }
            }
        }

        /* Filter the output table so that it contains only results 
         * inside the requested cone.  The result of the SQL query may 
         * contain some additional ones, since it queries a box-like shape. */
        StarTable coneTable = new SelectorStarTable( rsetTable ) {
            public boolean isIncluded( RowSequence rseq ) throws IOException {
                Object raCell = rseq.getCell( raRsetIndex_ );
                Object decCell = rseq.getCell( decRsetIndex_ );
                double rowRa = ( raCell instanceof Number )
                             ? ((Number) raCell).doubleValue() * angleFactor
                             : Double.NaN;
                double rowDec = ( decCell instanceof Number )
                              ? ((Number) decCell).doubleValue() * angleFactor
                              : Double.NaN;
                double dist =
                    CoordsDegrees.skyDistanceDegrees( ra, dec, rowRa, rowDec );
                return ! ( dist > sr );
            }
        };

        /* Consider doctoring the output table: if the angles are not
         * in degrees as supplied, we need to append columns which
         * are in degrees, since they are required by the interface. */
        List<ColumnInfo> outInfoList = new ArrayList<ColumnInfo>();
        final boolean addDegCols =
            convertAngles && raRsetIndex_ >= 0 && decRsetIndex_ >= 0;
        if ( addDegCols ) {
            outInfoList.add( new ColumnInfo( RADEG_INFO ) );
            outInfoList.add( new ColumnInfo( DECDEG_INFO ) );
        }
        final int nAddCol = outInfoList.size();
        final boolean hasTiles = tileRsetIndex_ >= 0;
        StarTable result = coneTable;

        /* Adjust the table if we need to add degrees, or for assertions
         * about tiles. */
        if ( nAddCol > 0 || hasTiles ) {
            int[] colMap =
                  hasTiles
                ? new int[] { raRsetIndex_, decRsetIndex_, tileRsetIndex_ }
                : new int[] { raRsetIndex_, decRsetIndex_ };
            ColumnSupplement radecSup =
                new PermutedColumnSupplement( result, colMap );
            ColumnInfo[] addInfos =
                outInfoList.toArray( new ColumnInfo[ 0 ] );
            ColumnSupplement addSup =
                    new CalculatorColumnSupplement( radecSup, addInfos ) {
                protected Object[] calculate( Object[] inValues ) {

                    /* Get input row values. */
                    Object raObj = inValues[ 0 ];
                    Object decObj = inValues[ 1 ];
                    Object tileObj = hasTiles ? inValues[ 2 ] : null;

                    /* Prepare to populate output row. */
                    Object[] calcValues = new Object[ nAddCol ];
                    int icol = 0;

                    /* Work out position in degrees. */
                    double raDeg = getDouble( raObj ) * angleFactor;
                    double decDeg = getDouble( decObj ) * angleFactor;

                    /* If necessary, prepare additional column values
                     * containing position in degrees. */
                    if ( addDegCols ) {
                        calcValues[ icol++ ] = Double.valueOf( raDeg );
                        calcValues[ icol++ ] = Double.valueOf( decDeg );
                    }

                    /* If using tiles, do an assertion test on the value of
                     * this one. */
                    if ( hasTiles ) {
                        long gotTile = ((Number) tileObj).longValue();
                        long calcTile =
                            tiling_.getPositionTile( raDeg, decDeg );
                        if ( gotTile != calcTile ) {
                            logger_.warning( "Tiling equivalence fails: "
                                           + calcTile + " != " + gotTile );
                        }
                    }

                    /* Return additional column values. */
                    assert icol == calcValues.length;
                    return calcValues;
                }
            };
            result = new AddColumnsTable( result, addSup );
        }

        /* Return the result table. */
        return result;
    }

    public int getRaIndex( StarTable result ) {
        return raIndex_;
    }

    public int getDecIndex( StarTable result ) {
        return decIndex_;
    }

    /**
     * Closes the connection if requested to.
     */
    public void close() {
        if ( connectionToClose_ != null ) {
            try {
                connectionToClose_.close();
            }
            catch ( SQLException e ) {
                logger_.warning( "Error closing connection: " + e );
            }
        }
    }
}
