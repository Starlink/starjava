package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.util.LineReader;

/**
 * Provides JDBC authentication using the terminal; assumes that someone
 * is sitting at <code>System.in</code>.
 */
public class TerminalAuthenticator implements JDBCAuthenticator {

    private final PrintStream err_;
    private String user_;
    private String pass_;

    /**
     * Constructs a new authenticator with a given stream to use for
     * writing prompts.
     *
     * @param  promptStrm  output stream for prompting
     */
    public TerminalAuthenticator( PrintStream promptStrm ) {
        err_ = promptStrm;
    }

    /**
     * Constructs a new authenticator which uses System.err for prompting.
     */
    public TerminalAuthenticator() {
        this( System.err );
    }

    public String[] authenticate() throws IOException {
        if ( user_ == null ) {
            user_ = readUser();
            pass_ = readPassword();
        }
        return new String[] { user_, pass_ };
    }

    /**
     * Prompts to the prompt stream and reads the user name from standard
     * input.
     *
     * @return  user name obtained from user
     */
    public String readUser() throws IOException {
        return LineReader.readString( "JDBC User: ", err_ );
    }

    /**
     * Prompts to the prompt stream and reads the password from standard
     * input.
     *
     * @return  password obtained from user
     */
    public String readPassword() throws IOException {
        return LineReader.readMaskedString( "JDBC Password: ", err_ );
    }
}
