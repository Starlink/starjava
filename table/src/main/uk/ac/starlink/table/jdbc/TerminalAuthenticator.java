package uk.ac.starlink.table.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides JDBC authentication using the terminal; assumes that someone
 * is sitting at <tt>System.in</tt>/<tt>System.out</tt>.
 */
public class TerminalAuthenticator implements JDBCAuthenticator {
    public String[] authenticate() throws IOException {
        BufferedReader rdr = 
            new BufferedReader( new InputStreamReader( System.in ) );
        System.out.println( "User: " );
        String user = rdr.readLine();
        System.out.println( "Password: " );
        String password = rdr.readLine();
        return new String[] { user, password };
    }
}
