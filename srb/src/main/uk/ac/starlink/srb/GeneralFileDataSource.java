package uk.ac.starlink.srb;

import java.io.IOException;
import java.io.InputStream;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.FileFactory;
import uk.ac.starlink.util.DataSource;

/**
 * DataSource implementation based on a JARGON GeneralFile object.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class GeneralFileDataSource extends DataSource {

    private final GeneralFile gf_;

    /**
     * Constructor.
     *
     * @param   gf  general file object on which this DataSource is based.
     */
    public GeneralFileDataSource( GeneralFile gf ) {
        gf_ = gf;
    }

    public String getName() {
        return gf_.getName();
    }

    public InputStream getRawInputStream() throws IOException {
        return FileFactory.newFileInputStream( gf_ );
    }
}
