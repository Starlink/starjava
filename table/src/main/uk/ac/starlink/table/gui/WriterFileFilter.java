package uk.ac.starlink.table.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import uk.ac.starlink.table.StarTableWriter;

/**
 * Makes a <tt>FileFilter</tt> out of a <tt>StarTableWriter</tt>.
 */
class WriterFileFilter extends FileFilter {

    private StarTableWriter handler;

    /**
     * Constructs a new file filter from a StarTableWriter.
     *
     * @param   handler  the StarTableWriter
     */
    public WriterFileFilter( StarTableWriter handler ) {
        this.handler = handler;
    }

    public boolean accept( File file ) {
        return handler.looksLikeFile( file.toString() );
    }

    public String getDescription() {
        return handler.getFormatName();
    }
}
