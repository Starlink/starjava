package uk.ac.starlink.ttools.cone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Abstract class for an object which can make a JDBC SELECT query 
 * corresponding to cone searches.
 * 
 * @author   Mark Taylor
 * @since    9 Jan 2007
 */
public abstract class ConeSelector {

    private final double unitFactor_;
    final String preamble_;
    final String postamble_;
    final RangeClause decClause_;
    final RangeClause middleRaClause_;
    final RangeClause equinoxRaClause_;

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
     * @param  cols   list of column names for the SELECT statement
     * @param  where  additional WHERE clause constraints
     */
    protected ConeSelector( Connection connection, String tableName,
                            String raCol, String decCol, AngleUnits units,
                            String cols, String where ) 
            throws SQLException {
        unitFactor_ = units.getCircle() / AngleUnits.DEGREES.getCircle();
        String quote = connection.getMetaData().getIdentifierQuoteString();
        preamble_ = new StringBuffer()
            .append( "SELECT" )
            .append( ' ' )
            .append( cols )
            .append( ' ' )
            .append( "FROM" )
            .append( ' ' )
            .append( quote )
            .append( tableName )
            .append( quote )
            .append( ' ' )
            .append( "WHERE" )
            .append( ' ' )
            .toString();
        postamble_ = ( where != null && where.trim().length() > 0 )
                   ? new StringBuffer()
                         .append( ' ' )
                         .append( "AND ( " )
                         .append( where )
                         .append( " )" )
                         .toString()
                   : "";
        decClause_ = new BetweenClause( quote + decCol + quote );
        middleRaClause_ = new BetweenClause( quote + raCol + quote );
        equinoxRaClause_ = new OutsideClause( quote + raCol + quote );
    }

    /**
     * Returns an SQL ResultSet containing the records corresponding to
     * a cone search with the given parameters.  
     * This may return a superset of the records in the given cone - it
     * is essential to use SQL which is simple (that is portable and
     * hopefully optimisable).
     *
     * @param  ra  right ascension of cone centre in degrees
     * @param  dec declination of cone centre in degrees
     * @param  sr  search radius of cone in degrees
     * @return  ResultSet containing records in cone (and possible some more)
     */
    public abstract ResultSet executeQuery( double ra, double dec, double sr )
            throws SQLException;

    /**
     * Converts an angle in degrees to the units appropriate for the RA and
     * Dec columns used by this object.
     *
     * @param   angle   angle in degrees
     * @return   angle in native units
     */
    double fromDegrees( double angle ) {
        return unitFactor_ * angle;
    }

    /**
     * Returns a new selector object which just queries on RA and Dec values.
     * You can choose an implementation which uses 
     * <code>PreparedStatement</code>s or not - there may be (big) 
     * performance implications.
     *
     * @param  connection   live connection to database
     * @param  tableName  name of a table in the database to search
     * @param  raCol  name of table column containing right ascension
     * @param  decCol name of table column containing declination
     * @param  units  angular units used by ra and dec columns
     * @param  cols   list of column names for the SELECT statement
     * @param  where  additional WHERE clause constraints
     * @param  usePrepared true to use JDBC {@link java.sql.PreparedStatement}s,
     *                     false for normal {@link java.sql.Statement}s
     */
    public static ConeSelector createSelector( Connection connection,
                                               String tableName, String raCol,
                                               String decCol, AngleUnits units,
                                               String cols, String where,
                                               boolean usePrepared )
            throws SQLException {
        if ( usePrepared ) {
            return new PreparedSelector( connection, tableName, raCol, decCol,
                                          units, cols, where, 0, null ) {
                protected void setExtraParameters( PreparedStatement stmt,
                                                   int ipar, double ra,
                                                   double dec, double sr ) {
                }
            };
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns a new selector object which queries using RA, Dec and a 
     * spatial tiling value.
     * You can choose an implementation which uses 
     * <code>PreparedStatement</code>s or not - there may be (big) 
     * performance implications.
     *
     * @param  connection   live connection to database
     * @param  tableName  name of a table in the database to search
     * @param  raCol  name of table column containing right ascension
     * @param  decCol name of table column containing declination
     * @param  units  angular units used by ra and dec columns
     * @param  cols   list of column names for the SELECT statement
     * @param  where  additional WHERE clause constraints
     * @param  tileCol column containing a sky tiling index value
     * @param  tiling tiling scheme used by tileCol column
     * @param  usePrepared true to use JDBC {@link java.sql.PreparedStatement}s,
     *                     false for normal {@link java.sql.Statement}s
     */
    public static ConeSelector createTiledSelector( Connection connection,
                                                    String tableName,
                                                    String raCol, String decCol,
                                                    AngleUnits units,
                                                    String cols, String where,
                                                    String tileCol,
                                                    final SkyTiling tiling,
                                                    boolean usePrepared )
            throws SQLException {
        if ( usePrepared ) {
            String quote = connection.getMetaData().getIdentifierQuoteString();
            final RangeClause tileClause =
                new BetweenClause( quote + tileCol + quote );
            return new PreparedSelector( connection, tableName, raCol, decCol,
                                         units, cols, where, 2,
                                         tileClause.toSql( "?", "?" ) ) {
                protected void setExtraParameters( PreparedStatement stmt,
                                                   int ipar, double ra,
                                                   double dec, double sr )
                        throws SQLException {
                    long[] range = tiling.getTileRange( ra, dec, sr );
                    long lotile = range == null ? Long.MIN_VALUE : range[ 0 ];
                    long hitile = range == null ? Long.MAX_VALUE : range[ 1 ];
                    if ( range != null ) {
                        logger_.info( tileClause
                                     .toSql( Long.toString( lotile ),
                                             Long.toString( hitile ) ) );
                    }
                    stmt.setLong( ++ipar, lotile );
                    stmt.setLong( ++ipar, hitile );
                }
            };
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Convenience method to return a SkyBox corresponding to a cone search
     * where the inputs and outputs are in degrees.
     *
     * @param   ra  cone centre RA in degrees
     * @param   dec  cone centre declination in degrees
     * @param   sr   cone radius in degrees
     * @return  sky box containing cone box cut with values in degrees
     */
    private static SkyBox getConeBoxDegrees( double ra, double dec,
                                             double sr ) {
        return SkyBox.getConeBox( Math.toRadians( ra ), Math.toRadians( dec ),
                                  Math.toRadians( sr ) )
              .toDegrees();
    }

    /**
     * ConeSelector subclass which uses {@link java.sql.PreparedStatement}s.
     * Concrete subclasses must supply zero or more additional parameters
     * to the prepared statement.
     */
    private static abstract class PreparedSelector extends ConeSelector {
        private PreparedStatement allRaStatement_;
        private PreparedStatement middleRaStatement_;
        private PreparedStatement equinoxRaStatement_;
        private final int nExtraParams_;

        /**
         * Constructor.
         *
         * @param  connection   live connection to database
         * @param  tableName  name of a table in the database to search
         * @param  raCol  name of table column containing right ascension
         * @param  decCol name of table column containing declination
         * @param  units  angular units used by ra and dec columns
         * @param  cols   list of column names for the SELECT statement
         * @param  where  additional WHERE clause constraints
         * @param  nExtraParams  number of parameters in prepared statement
         *                       in addition to RA and Dec ones
         * @param  extraTemplate PreparedStatement-type template for 
         *                       part of WHERE clause containing 
         *                       <code>nExtraParams</code> additional 
         *                       parameters ("?" characters)
         */
        PreparedSelector( Connection connection, String tableName,
                          String raCol, String decCol, AngleUnits units,
                          String cols, String where, int nExtraParams,
                          String extraTemplate ) throws SQLException {
            super( connection, tableName, raCol, decCol, units, cols, where );
            nExtraParams_ = nExtraParams;

            /* Construct SQL for the three different cases. */
            StringBuffer allRaBuf = new StringBuffer( preamble_ );
            if ( extraTemplate != null && extraTemplate.length() > 0 ) {
                allRaBuf.append( "( " )
                        .append( extraTemplate )
                        .append( " )" )
                        .append( " AND " );
            }
            allRaBuf.append( decClause_.toSql( "?", "?" ) );
            String allRaSql = allRaBuf.toString();
            String middleRaSql = new StringBuffer()
                .append( allRaSql )
                .append( " AND " )
                .append( middleRaClause_.toSql( "?", "?" ) )
                .toString();
            String equinoxRaSql = new StringBuffer()
                .append( allRaSql )
                .append( " AND " )
                .append( equinoxRaClause_.toSql( "?", "?" ) )
                .toString();

            /* Compile the SQL for later use. */
            logger_.info( allRaSql );
            allRaStatement_ = connection.prepareStatement( allRaSql );
            logger_.info( middleRaSql );
            middleRaStatement_ = connection.prepareStatement( middleRaSql );
            logger_.info( equinoxRaSql );
            equinoxRaStatement_ = connection.prepareStatement( equinoxRaSql );
        }

        /**
         * Set the value of all extra parameters in the PreparedStatement
         * specific to this object.
         *
         * @param  stmt  prepared statement
         * @param  ipar  index of the first 'extra' parameter
         * @param  ra  right ascension of cone centre in degrees
         * @param  dec declination of cone centre in degrees
         * @param  sr  search radius of cone in degrees
         */
        protected abstract void setExtraParameters( PreparedStatement stmt,
                                                    int ipar,
                                                    double ra, double dec,
                                                    double sr )
                throws SQLException;

        public ResultSet executeQuery( double ra, double dec, double sr ) 
                throws SQLException {

            /* Work out the box cut in RA and Dec. */
            SkyBox coneBox = getConeBoxDegrees( ra, dec, sr );
            double ra1deg = coneBox.getRaRange()[ 0 ];
            double ra2deg = coneBox.getRaRange()[ 1 ];
            double ra1u = fromDegrees( ra1deg );
            double ra2u = fromDegrees( ra2deg );

            /* Identify the correct PreparedStatement to use. */
            PreparedStatement stmt;
            boolean useRa;
            if ( ra1deg <= 0 && ra2deg >= 360 ) {
                stmt = allRaStatement_;
                useRa = false;
            }
			else if ( ra1deg <= ra2deg ) {
                stmt = middleRaStatement_;
                logger_.info( middleRaClause_
                             .toSql( Double.toString( ra1u ),
                                     Double.toString( ra2u ) ) );
                useRa = true;
			}
            else if ( ra1deg > ra2deg ) {
                stmt = equinoxRaStatement_;
                useRa = true;
            }
            else {
                throw new AssertionError();
            }

            /* Set the parameters for the PreparedStatement. */
            stmt.clearParameters();
            int ipar = 0;
            setExtraParameters( stmt, ipar, ra, dec, sr );
            ipar += nExtraParams_;
            double dec1u = fromDegrees( coneBox.getDecRange()[ 0 ] );
            double dec2u = fromDegrees( coneBox.getDecRange()[ 1 ] );
            logger_.info( decClause_.toSql( Double.toString( dec1u ),
                                            Double.toString( dec2u ) ) );
            stmt.setDouble( ++ipar, dec1u );
            stmt.setDouble( ++ipar, dec2u );
            if ( useRa ) {
                stmt.setDouble( ++ipar, ra1u );
                stmt.setDouble( ++ipar, ra2u );
            }
            assert stmt.getParameterMetaData().getParameterCount() == ipar;

            /* Execute the statement and return the result. */
            return stmt.executeQuery();
        }
    }

    /**
     * Helper class for formatting parts of SQL WHERE clauses that use two
     * numeric values.
     */
    private static class RangeClause {
        private final String pre_;
        private final String mid_;
        private final String post_;

        /**
         * Constructor.
         *
         * @param  pre   part before first value
         * @param  mid   part between first and second values
         * @param  post  part after second value
         */
        RangeClause( String pre, String mid, String post ) {
            pre_ = pre;
            mid_ = mid;
            post_ = post;
        }

        /**
         * Returns the SQL text corresponding to this range for a given pair
         * of values.
         *
         * @param  x1  first value
         * @param  x2  second value
         */
        public String toSql( String x1, String x2 ) {
            return new StringBuffer()
               .append( pre_ )
               .append( x1 )
               .append( mid_ )
               .append( x2 )
               .append( post_ )
               .toString();
        }
    }

    /**
     * RangeClause subclass for use when the two values indicate a range
     * within which a target value must fall.
     */
    private static class BetweenClause extends RangeClause {

        /**
         * Constructor.
         *
         * @param   value   SQL designation for target value
         */
        BetweenClause( String value ) {
            super( "( " + value + " BETWEEN ", " AND ", " )" );
        }
    }

    /**
     * RangeClause subclass for use when the two values indicate a range
     * outside which a target value should fall.
     */
    private static class OutsideClause extends RangeClause {

        /**
         * Constructor.
         *
         * @param   value  SQL designation for target value
         */
        OutsideClause( String value ) {
            super( "( " + value + " < ", " OR " + value + " > ", " )" );
        }
    }
}
