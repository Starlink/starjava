package uk.ac.starlink.table.jdbc;

import java.io.IOException;

/**
 * Specifies authentication to make a JDBC connection.
 * The <tt>authenticate</tt> method can be called to retrieve the username
 * and password to be used.
 */
public interface JDBCAuthenticator {

    /**
     * Obtains username and password.
     * The return value is a two-element array containing the username
     * and password to be used, in that order.  Either or both of these
     * may be <tt>null</tt>.
     *
     * @return   <tt>String[]{username,password}</tt>
     * @throws   IOException  if there is some error
     */
    String[] authenticate() throws IOException;
}
