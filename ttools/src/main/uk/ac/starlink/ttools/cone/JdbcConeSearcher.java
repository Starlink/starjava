package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.jdbc.SequentialResultSetStarTable;
import uk.ac.starlink.ttools.filter.AddColumnsTable;

/**
 * ConeSearcher implementation using SQL.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2007
 */
public class JdbcConeSearcher implements ConeSearcher {

    private final String raCol_;
    private final String decCol_;
    private final AngleUnits units_;
    private final String tileCol_;
    private final SkyTiling tiling_;
    private final PreparedStatement allRaStatement_;
    private final PreparedStatement middleRaStatement_;
    private final PreparedStatement equinoxRaStatement_;
    private boolean first_ = true;
    private int raIndex_ = -1;
    private int decIndex_ = -1;
    private int raRsetIndex_ = -1;
    private int decRsetIndex_ = -1;

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
     *                in degrees
     * @param  decCol name of table column containing declination
     *                in degrees
     * @param  units  angular units used by ra and dec columns
     * @param  tileCol column containing a sky tiling index value, or null
     * @param  tiling tiling scheme used by tileCol column
     * @param  cols   list of column names for the SELECT statement
     * @param  where  additional WHERE clause constraints
     * @param  bestOnly  true iff only the closest match is required (hint)
     */
    JdbcConeSearcher( Connection connection, String tableName,
                      String raCol, String decCol, AngleUnits units,
                      String tileCol, SkyTiling tiling,
                      String cols, String where, boolean bestOnly )
            throws SQLException {
        raCol_ = raCol;
        decCol_ = decCol;
        units_ = units;
        tileCol_ = tileCol;
        tiling_ = tiling;

        /* Write the common parts of the SQL SELECT statement. */
        String preRa = new StringBuffer() 
            .append( "SELECT" )
            .append( ' ' )
            .append( cols )
            .append( ' ' )
            .append( "FROM" )
            .append( ' ' )
            .append( tableName )
            .append( ' ' )
            .append( "WHERE" )
            .append( ' ' )
            .append( "( " )
            .append( decCol )
            .append( " BETWEEN ? AND ? )" ) // minDec,maxDec
            .toString();
        if ( tiling_ != null && tileCol != null ) {
            preRa = new StringBuffer( preRa )
            .append( "AND" )
            .append( ' ' )
            .append( "( " )
            .append( tileCol )
            .append( " BETWEEN ? AND ?" ) // minTile,maxTile
            .append( " )" )
            .toString();
        }
        if ( where != null && where.trim().length() > 0 ) {
            preRa = new StringBuffer( preRa )
                .append( ' ' )
                .append( "AND ( " )
                .append( where )
                .append( " )" )
                .toString();
        }

        /* Prepare three different variants of the SELECT statement. 
         * In most cases a simple BETWEEN will work fine for 
         * filtering on right ascension.  However, in the case that
         * the region straddles the vernal equinox (RA=0/RA=360 line)
         * we need to exclude the range between the values instead.
         * In the case that the search region includes the pole, no
         * filtering on RA can be done. */
        String allRaSelectSql =
            preRa;
        String middleRaSelectSql =
            preRa + " AND ( " + raCol + " BETWEEN ? AND ? )";
        String equinoxRaSelectSql =
            preRa + " AND ( " + raCol + " < ? OR " + raCol + " > ? )";

        /* Pre-compile the SQL for later use. */
        logger_.info( allRaSelectSql );
        allRaStatement_ = connection.prepareStatement( allRaSelectSql );
        logger_.info( middleRaSelectSql );
        middleRaStatement_ = connection.prepareStatement( middleRaSelectSql );
        logger_.info( equinoxRaSelectSql );
        equinoxRaStatement_ = connection.prepareStatement( equinoxRaSelectSql );
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {

        /* Work out the bounds of a rectangular RA, Dec box within which
         * any records in the requested cone must fall.  Note that this
         * is a superset of the desired result (it makes the SELECT
         * statement much simpler and, more importantly, optimisable) -
         * this is permitted by the performSearch contract. */
        double deltaDec = sr;
        double minDec = Math.max( dec - deltaDec, -90 );
        double maxDec = Math.min( dec + deltaDec, +90 );
        double deltaRa =
            Math.toDegrees( calculateDeltaRa( Math.toRadians( ra ),
                                              Math.toRadians( dec ),
                                              Math.toRadians( sr ) ) );
        double minRa = ra - deltaRa;
        double maxRa = ra + deltaRa;

        /* Configure a precompiled statement accordingly.  Note that
         * this will be done differently according to whether the
         * RA range straddles the vernal equinox (RA=0 line) or not. */
        PreparedStatement stmt;
        if ( deltaRa >= 180 ) {
            stmt = allRaStatement_;
            logger_.info( "no ra restriction" );
        }
        else if ( minRa > 0 && maxRa < 360 ) {
            stmt = middleRaStatement_;
            logger_.info( "ra BETWEEN " + minRa + " AND " + maxRa );
        }
        else {
            stmt = equinoxRaStatement_;
            double min = maxRa % 360.;
            double max = ( minRa + 360 ) % 360.;
            minRa = min;
            maxRa = max;
            logger_.info( "ra NOT BETWEEN " + minRa + " AND " + maxRa );
        }
        logger_.info( "dec BETWEEN " + minDec + " AND " + maxDec );
        try {
            double factor = units_.getCircle() / AngleUnits.DEGREES.getCircle();
            stmt.clearParameters();
            int ipar = 0;
            stmt.setDouble( ++ipar, minDec * factor );
            stmt.setDouble( ++ipar, maxDec * factor );
            if ( tiling_ != null ) {
                long[] range = tiling_.getTileRange( ra, dec, sr );
                long lotile = range == null ? Long.MIN_VALUE : range[ 0 ];
                long hitile = range == null ? Long.MAX_VALUE : range[ 1 ];
                if ( range != null ) {
                    logger_.info( tileCol_ + " BETWEEN " + lotile + " AND "
                                + hitile );
                }
                stmt.setLong( ++ipar, lotile );
                stmt.setLong( ++ipar, hitile );
            }
            if ( stmt != allRaStatement_ ) {
                stmt.setDouble( ++ipar, minRa * factor );
                stmt.setDouble( ++ipar, maxRa * factor );
            }
        }
        catch ( SQLException e ) {
            throw (IOException)
                  new IOException( "Error configuring SQL statement: "
                                 + e.getMessage() )
                 .initCause( e );
        }

        /* Execute the statement and turn it into a StarTable. */
        ResultSet rset;
        try {
            rset = stmt.executeQuery();
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
        }

        /* Doctor the output table: if the angles are not in degrees as
         * supplied, append columns which are in degrees, since they are
         * required by the interface. */
        StarTable result;
        if ( convertAngles && raRsetIndex_ >= 0 && decRsetIndex_ >= 0 ) {
            int[] inColIndices = new int[ ncolRset ];
            for ( int i = 0; i < ncolRset; i++ ) {
                inColIndices[ i ] = i;
            }
            final double factor =
                AngleUnits.DEGREES.getCircle() / units_.getCircle();
            ColumnInfo[] outInfos = new ColumnInfo[] {
                new ColumnInfo( RADEG_INFO ),
                new ColumnInfo( DECDEG_INFO ),
            };
            result = new AddColumnsTable( rsetTable, inColIndices, outInfos,
                                          ncolRset ) {
                protected Object[] calculateValues( Object[] inValues ) {
                    Object ra = (Number) inValues[ raRsetIndex_ ];
                    Object dec = (Number) inValues[ decRsetIndex_ ];
                    return ( ra instanceof Number && dec instanceof Number )
                      ? new Object[] {
                            new Double( ((Number) ra).doubleValue() * factor ),
                            new Double( ((Number) dec).doubleValue() * factor ),
                        }
                      : new Object[ 2 ];
                }
            };
        }
        else {
            result = rsetTable;
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
     * Works out the minimum change in Right Ascension which will encompass
     * all points within a given search radius at a given central ra, dec.
     *
     * @param   ra   right ascension of the centre of the search region
     *               in radians
     * @param   dec  declination of the centre of the search region 
     *               in radians
     * @param   sr   radius of the search region in radians
     * @return  minimum change in radians of RA from the central value
     *          which will contain the entire search region
     */
    public static double calculateDeltaRa( double ra, double dec, double sr ) {
 
        /* Get the arc angle between the pole and the cone centre. */
        double hypArc = Math.PI / 2 - Math.abs( dec );

        /* If the search radius is greater than this, then all right 
         * ascensions must be included. */
        if ( sr >= hypArc ) {
            return Math.PI;
        }

        /* In the more general case, we need a bit of spherical trigonometry.
         * Consider a right spherical triangle with one vertex at the pole,
         * one vertex at the centre of the search circle, and the right angle
         * vertex at the tangent between the search circle and a line of
         * longitude; then apply Napier's Pentagon.  The vertex angle at the
         * pole is the desired change in RA. */
        return Math.asin( Math.cos( Math.PI / 2 - sr ) / Math.sin( hypArc ) );
    }
}
