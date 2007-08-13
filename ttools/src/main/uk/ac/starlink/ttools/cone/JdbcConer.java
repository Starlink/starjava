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
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.ConnectionParameter;

public class JdbcConer implements Coner {

    private final ConnectionParameter connParam_;
    private final Parameter dbtableParam_;
    private final Parameter dbraParam_;
    private final Parameter dbdecParam_;
    private final Parameter colsParam_;
    private final Parameter whereParam_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    public JdbcConer() {
        connParam_ = new ConnectionParameter( "db" );
        connParam_.setPrompt( "JDBC-type URL for database connection" );
        connParam_.setDescription( new String[] {
            "<p>URL which defines the connection to the database.",
            "The format of this is database- and driver-dependent, but may",
            "look something like <code>jdbc:mysql://localhost/dbname</code>.",
            "</p>",
        } );

        dbtableParam_ = new Parameter( "dbtable" );
        dbtableParam_.setPrompt( "Name of table in database" );
        dbtableParam_.setDescription( new String[] {
            "<p>The name of the table in the SQL database which provides",
            "the remote data.",
            "</p>",
        } );

        dbraParam_ = new Parameter( "dbra" );
        dbraParam_.setPrompt( "Name of right ascension column in database" );
        dbraParam_.setDescription( new String[] {
            "<p>The name of a column in the SQL database table",
            "<code>" + dbtableParam_.getName() + "</code>",
            "which gives the J2000 right ascension in degrees.",
            "</p>",
        } );

        dbdecParam_ = new Parameter( "dbdec" );
        dbdecParam_.setPrompt( "Name of declination column in database" );
        dbdecParam_.setDescription( new String[] {
            "<p>The name of a column in the SQL database table",
            "<code>" + dbtableParam_.getName() + "</code>",
            "which gives the J2000 declination in degrees.",
            "</p>",
        } );

        colsParam_ = new Parameter( "selectcols" );
        colsParam_.setPrompt( "Database columns to select" );
        colsParam_.setDescription( new String[] {
            "<p>An SQL expression for the list of columns to be selected",
            "from the table in the database.",
            "A value of \"<code>*</code>\" retrieves all columns.",
            "</p>",
        } );
        colsParam_.setDefault( "*" );

        whereParam_ = new Parameter( "where" );
        whereParam_.setPrompt( "Additional WHERE restriction on selection" );
        whereParam_.setNullPermitted( true );
        whereParam_.setDescription( new String[] {
            "<p>An SQL expression further limiting the rows to be selected",
            "from the database.  This will be combined with the constraints",
            "on position implied by the cone search centres and radii.",
            "A null value indicates no additional criteria.",
            "</p>",
        } );
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
        return new JdbcSearcher( connection, table, raCol, decCol,
                                 cols, where, bestOnly );
    }

    private static class JdbcSearcher implements ConeSearcher {

        private final PreparedStatement stmt_;
        private final String raCol_;
        private final String decCol_;

        JdbcSearcher( Connection connection, String tableName,
                      String raCol, String decCol,
                      String cols, String where, boolean bestOnly )
                throws TaskException {
            raCol_ = raCol;
            decCol_ = decCol;
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "SELECT" )
                .append( ' ' )
                .append( cols )
                .append( ' ' )
                .append( "FROM" )
                .append( ' ' )
                .append( tableName )
                .append( ' ' )
                .append( "WHERE" )
                .append( ' ' )
                .append( "( " + raCol + " - ? < ? )" )   // ra, maxDeltaRa
                .append( " AND " )
                .append( "( ? - " + raCol + " < ? )" )   // ra, maxDeltaRa
                .append( " AND " )
                .append( "( " + decCol + " - ? < ? )" )  // dec, sr
                .append( " AND " )
                .append( "( ? - " + decCol + " < ? )" )  // dec, sr
                ;
            String sql = sbuf.toString();
            logger_.info( sql );
            try {
                stmt_ = connection.prepareStatement( sql );
            }
            catch ( SQLException e ) {
                throw new TaskException( "Error preparing SQL statement: "
                                       + e.getMessage(), e );
            }
        }

        public StarTable performSearch( double ra, double dec, double sr )
                throws IOException {
            double maxDeltaRa;
            double maxDec = Math.max( Math.abs( dec + sr ),
                                      Math.abs( dec - sr ) );
  // is this geometry correct?  I'm mostly just guessing.
  // it doesn't cover RA wraparound for sure.
            if ( maxDec >= Math.PI / 2 ) {
                maxDeltaRa = 360;
            }
            else {
                maxDeltaRa = sr / Math.cos( maxDec );
            }
            try {
                stmt_.clearParameters();
                stmt_.setDouble( 1, ra );
                stmt_.setDouble( 2, maxDeltaRa );
                stmt_.setDouble( 3, ra );
                stmt_.setDouble( 4, maxDeltaRa );
                stmt_.setDouble( 5, dec );
                stmt_.setDouble( 6, sr );
                stmt_.setDouble( 7, dec );
                stmt_.setDouble( 8, sr );
            }
            catch ( SQLException e ) {
                throw (IOException)
                      new IOException( "Error configuring SQL statement: "
                                     + e.getMessage() )
                     .initCause( e );
            }
            ResultSet rset;
            try {
                rset = stmt_.executeQuery();
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
                      .findColumn( raCol_ );
            }
            catch ( SQLException e ) {
                return -1;
            }
        }

        public int getDecIndex( StarTable result ) {
            try {
                return ((SequentialResultSetStarTable) result)
                      .getResultSet()
                      .findColumn( decCol_ );
            }
            catch ( SQLException e ) {
                return -1;
            }
        }
    }
}
