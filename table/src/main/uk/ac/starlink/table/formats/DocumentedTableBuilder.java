package uk.ac.starlink.table.formats;

import uk.ac.starlink.table.TableBuilder;

/**
 * Partial TableBuilder implementation that adds some behaviour
 * useful when auto-generating XML user documentation for I/O handlers.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2020
 */
public abstract class DocumentedTableBuilder
        implements TableBuilder, DocumentedIOHandler {

    private final String[] extensions_;

    /**
     * Constructor.
     *
     * @param  extensions  list of lower-cased filename extensions,
     *                     excluding the '.' character
     */
    protected DocumentedTableBuilder( String[] extensions ) {
        extensions_ = extensions;
    }

    public String[] getExtensions() {
        return extensions_;
    }

    public boolean looksLikeFile( String filename ) {
        return DocumentedIOHandler.matchesExtension( this, filename );
    }

    /**
     * Indicates whether this handler can read tables from a stream.
     *
     * @return   true iff this handler can read from streams
     */
    public abstract boolean canStream();
}
