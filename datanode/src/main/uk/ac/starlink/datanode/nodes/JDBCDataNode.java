package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.jdbc.JDBCStarTable;

/**
 * DataNode representing a JDBC StarTable.
 */
public class JDBCDataNode extends StarTableDataNode {

    private JDBCStarTable jdbcTable;

    public JDBCDataNode( String url ) throws NoSuchDataException {
        this( makeJDBCTable( url ) );
    }

    public JDBCDataNode( JDBCStarTable jdbcTable ) throws NoSuchDataException {
        super( jdbcTable );
        this.jdbcTable = jdbcTable;

        /* Remove the SQL parameter since we will present it elsewhere. */
        DescribedValue sqlparam = jdbcTable.getParameterByName( "SQL" );
        if ( sqlparam != null ) {
            jdbcTable.getParameters().remove( sqlparam );
        }
    }

    public String getNodeTLA() {
        return "SQL";
    }

    public String getNodeType() {
        return "JDBC table";
    }

    public void configureDetail( DetailViewer dv ) {

        /* JDBC specifics. */
        dv.addSubHead( "Database connection" );
        try {
            DatabaseMetaData dbmeta = jdbcTable.getConnection().getMetaData();
            dv.addKeyedItem( "SQL query", jdbcTable.getSql() );
            dv.addKeyedItem( "URL", dbmeta.getURL() );
            dv.addKeyedItem( "User", dbmeta.getUserName() );
            dv.addKeyedItem( "Database name", dbmeta.getDatabaseProductName() );
            dv.addKeyedItem( "Database version", dbmeta
                                                .getDatabaseProductVersion() );
            dv.addKeyedItem( "Driver name", dbmeta.getDriverName() );
            dv.addKeyedItem( "Driver version", dbmeta.getDriverVersion() );
        }
        catch ( SQLException e ) {
            dv.logError( e );
        }

        /* Generic table stuff. */
        dv.addSeparator();
    }

    private static JDBCStarTable makeJDBCTable( String url )
            throws NoSuchDataException {
        if ( ! url.startsWith( "jdbc:" ) ) {
            throw new NoSuchDataException( 
                "URL " + url + " doesn't start \"jdbc:\"" );
        }
        else {
            try {
                return (JDBCStarTable) 
                       getTableFactory().getJDBCHandler()
                                        .makeStarTable( url, false );
            }
            catch ( IOException e ) {
                throw new NoSuchDataException( e );
            }
        }
    }
}
