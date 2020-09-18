package uk.ac.starlink.table.formats;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StreamStarTableWriter;

/**
 * Partial StarTableWriter implementation for use by writers which
 * just write to output streams, and which also implements DocumentedIOHandler.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2020
 */
public abstract class DocumentedStreamStarTableWriter
        implements StarTableWriter, DocumentedIOHandler {

    private final String[] extensions_;

    /**
     * Constructor.
     *
     * @param  extensions  list of lower-cased filename extensions,
     *                     excluding the '.' character
     */
    protected DocumentedStreamStarTableWriter( String[] extensions ) {
        extensions_ = extensions;
    }

    public String[] getExtensions() {
        return extensions_;
    }

    public boolean looksLikeFile( String filename ) {
        return DocumentedIOHandler.matchesExtension( this, filename );
    }

    public void writeStarTable( StarTable table, String location,
                                StarTableOutput sto ) throws IOException {
        StreamStarTableWriter.writeStarTable( this, table, location, sto );
    }
}
