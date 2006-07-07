package uk.ac.starlink.table.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for objects which can supply a JDBC Connection to
 * a single database.
 * <p>
 * <i>Should its use be replaced by use of the similar but bigger
 * (and beany) javax.sql.DataSource interface?</i>
 */
public interface Connector {

    /**
     * Returns a JDBC Connection object.
     *
     * @return   a connection
     */
    Connection getConnection() throws SQLException;
}
