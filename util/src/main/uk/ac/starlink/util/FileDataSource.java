package uk.ac.starlink.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataSource implementation based on a {@link @java.io.File}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FileDataSource extends DataSource {

    private File file;

    public FileDataSource( File file ) {
        this.file = file;
    }

    public String getName() {
        return file.toString();
    }

    protected InputStream getRawInputStream() throws IOException {
        return new FileInputStream( file );
    }
}
