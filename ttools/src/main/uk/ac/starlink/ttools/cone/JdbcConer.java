package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.SequentialResultSetStarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.ConnectionParameter;

/**
 * Coner implementation which works by performing SELECT statements over a
 * JDBC database connection.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2007
 */
public class JdbcConer implements Coner {

    private final ConnectionParameter connParam_;
    private final Parameter dbtableParam_;
    private final Parameter dbraParam_;
    private final Parameter dbdecParam_;
    private final Parameter colsParam_;
    private final Parameter whereParam_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     */
    public JdbcConer() {
        String sys = getSkySystem();
        sys = ( sys == null ) ? ""
                              : ( sys + " " );

        connParam_ = new ConnectionParameter( "db" );

        dbtableParam_ = new Parameter( "dbtable" );
        dbtableParam_.setUsage( "<table-name>" );
        dbtableParam_.setPrompt( "Name of table in database" );
        dbtableParam_.setDescription( new String[] {
            "<p>The name of the table in the SQL database which provides",
            "the remote data.",
            "</p>",
        } );

        dbraParam_ = new Parameter( "dbra" );
        dbraParam_.setUsage( "<sql-col>" );
        dbraParam_.setPrompt( "Name of right ascension column in database" );
        dbraParam_.setDescription( new String[] {
            "<p>The name of a column in the SQL database table",
            "<code>" + dbtableParam_.getName() + "</code>",
            "which gives the " + sys + "right ascension in degrees.",
            "</p>",
        } );

        dbdecParam_ = new Parameter( "dbdec" );
        dbdecParam_.setUsage( "<sql-col>" );
        dbdecParam_.setPrompt( "Name of declination column in database" );
        dbdecParam_.setDescription( new String[] {
            "<p>The name of a column in the SQL database table",
            "<code>" + dbtableParam_.getName() + "</code>",
            "which gives the " + sys + "declination in degrees.",
            "</p>",
        } );

        colsParam_ = new Parameter( "selectcols" );
        colsParam_.setUsage( "<sql-cols>" );
        colsParam_.setPrompt( "Database columns to select" );
        colsParam_.setDescription( new String[] {
            "<p>An SQL expression for the list of columns to be selected",
            "from the table in the database.",
            "A value of \"<code>*</code>\" retrieves all columns.",
            "</p>",
        } );
        colsParam_.setDefault( "*" );

        whereParam_ = new Parameter( "where" );
        whereParam_.setUsage( "<sql-condition>" );
        whereParam_.setPrompt( "Additional WHERE restriction on selection" );
        whereParam_.setNullPermitted( true );
        whereParam_.setDescription( new String[] {
            "<p>An SQL expression further limiting the rows to be selected",
            "from the database.  This will be combined with the constraints",
            "on position implied by the cone search centres and radii.",
            "The value of this parameter should just be a condition,",
            "it should not contain the <code>WHERE</code> keyword.",
            "A null value indicates no additional criteria.",
            "</p>",
        } );
    }

    /**
     * Returns the empty string.  No particular coordinate system is
     * mandated by this object.
     */
    public String getSkySystem() {
        return "";
    }

    public Parameter[] getParameters() {
        List pList = new ArrayList();
        pList.add( connParam_ );
        pList.addAll( Arrays.asList( connParam_.getAssociatedParameters() ) );
        pList.add( dbtableParam_ );
        pList.add( dbraParam_ );
        pList.add( dbdecParam_ );
        pList.add( colsParam_ );
        pList.add( whereParam_ );
        return (Parameter[]) pList.toArray( new Parameter[ 0 ] );
    }

    public ConeSearcher createSearcher( Environment env, boolean bestOnly )
            throws TaskException {
        Connection connection = connParam_.connectionValue( env );
        String table = dbtableParam_.stringValue( env );
        String raCol = dbraParam_.stringValue( env );
        String decCol = dbdecParam_.stringValue( env );
        String cols = colsParam_.stringValue( env );
        String where = whereParam_.stringValue( env );
        if ( where != null &&
             where.toLowerCase().trim().startsWith( "where" ) ) {
            String msg = "Omit <code>WHERE</code> keyword from "
                       + "<code>" + whereParam_.getName() + "</code> parameter";
            throw new ParameterValueException( whereParam_, msg );
        }
        return new JdbcSearcher( connection, table, raCol, decCol,
                                 cols, where, bestOnly );
    }

    /**
     * ConeSearcher implementation using SQL.
     */
    private static class JdbcSearcher implements ConeSearcher {

        private final String raCol_;
        private final String decCol_;
        private PreparedStatement allRaStatement_;
        private PreparedStatement middleRaStatement_;
        private PreparedStatement equinoxRaStatement_;

        /**
         * Constructor.
         *
         * @param  connection   live connection to database
         * @param  tableName  name of a table in the database to search
         * @param  raCol  name of table column containing right ascension
         *                in degrees
         * @param  decCol name of table column containing declination
         *                in degrees
         * @param  cols   list of column names for the SELECT statement
         * @param  where  additional WHERE clause constraints
         * @param  bestOnly  true iff only the closest match is required (hint)
         */
        JdbcSearcher( Connection connection, String tableName,
                      String raCol, String decCol,
                      String cols, String where, boolean bestOnly )
                throws TaskException {
            raCol_ = raCol;
            decCol_ = decCol;

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
            try {
                logger_.info( allRaSelectSql );
                allRaStatement_ =
                    connection.prepareStatement( allRaSelectSql );
                logger_.info( middleRaSelectSql );
                middleRaStatement_ =
                    connection.prepareStatement( middleRaSelectSql );
                logger_.info( equinoxRaSelectSql );
                equinoxRaStatement_ =
                    connection.prepareStatement( equinoxRaSelectSql );
            }
            catch ( SQLException e ) {
                throw new TaskException( "Error preparing SQL statement: "
                                       + e.getMessage(), e );
            }
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
                stmt.clearParameters();
                stmt.setDouble( 1, minDec );
                stmt.setDouble( 2, maxDec );
                if ( stmt != allRaStatement_ ) {
                    stmt.setDouble( 3, minRa );
                    stmt.setDouble( 4, maxRa );
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
            try {
                return new SequentialResultSetStarTable( rset );
            }
            catch ( SQLException e ) {
                throw (IOException)
                      new IOException( "Error retrieving data from SQL "
                                     + "statement: " + e.getMessage() )
                     .initCause( e );
            }
        }

        public int getRaIndex( StarTable result ) {
            try {
                return ((SequentialResultSetStarTable) result)
                      .getResultSet()
                      .findColumn( raCol_ ) - 1;
            }
            catch ( SQLException e ) {
                return -1;
            }
        }

        public int getDecIndex( StarTable result ) {
            try {
                return ((SequentialResultSetStarTable) result)
                      .getResultSet()
                      .findColumn( decCol_ ) - 1;
            }
            catch ( SQLException e ) {
                return -1;
            }
        }
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
