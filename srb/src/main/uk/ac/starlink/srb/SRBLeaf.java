package uk.ac.starlink.srb;

import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.util.DataSource;

/**
 * Leaf implementation based on an SRB file.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
class SRBLeaf extends SRBNode implements Leaf {

    /**
     * Constructor.  The <tt>file</tt> argument must be a regular 
     * (rather than an directory) type SRBFile.   No check is made in
     * the constructor of this though, since SRBFile.isDirectory seems
     * like an extremely slow call to make.
     *
     * @param  file   SRB file object on which this is based
     * @param  root   root of thie filesystem in which <tt>file</tt> lives.
     */
    public SRBLeaf( SRBFile file, SRBFile root ) {
        super( file, root );
    }

    public OutputStream getOutputStream() throws IOException {
        return new SRBFileOutputStream( getFile() );
    }

    public DataSource getDataSource() throws IOException {
        return new GeneralFileDataSource( getFile() );
    }
}
