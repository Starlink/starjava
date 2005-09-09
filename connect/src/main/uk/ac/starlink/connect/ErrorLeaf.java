package uk.ac.starlink.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.ac.starlink.util.DataSource;

/**
 * Dummy Leaf implmentation which describes an error condition.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
public class ErrorLeaf implements Leaf {

    private final String name_;
    private final Branch parent_;
    private final Throwable err_;

    /**
     * Constructor.
     *
     * @param   parent  leaf parent
     * @param   err  the throwable which this leaf reprsents
     */
    public ErrorLeaf( Branch parent, Throwable err ) {
        parent_ = parent;
        err_ = err;
        String msg = "ERROR";
        for ( Throwable e = err; err != null; err = err.getCause() ) {
            if ( e.getMessage() != null ) {
                msg += ": " + e.getMessage();
                break;
            }
        }
        name_ = msg;
    }

    public Branch getParent() {
        return parent_;
    }

    public String getName() {
        return name_;
    }

    public DataSource getDataSource() throws IOException {
        throw (IOException) new IOException( "No connection" )
                           .initCause( err_ );
    }

    public OutputStream getOutputStream() throws IOException {
        throw (IOException) new IOException( "No connection" )
                           .initCause( err_ );
    }

    public String toString() {
        return err_.toString();
    }

}
