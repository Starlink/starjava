package uk.ac.starlink.treeview;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import uk.ac.starlink.util.DataSource;

/**
 * DataNode representing a zip archive got from a stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ZipStreamDataNode extends ZipArchiveDataNode {

    private DataSource datsrc;
    private ZipInputStream zstream;
    private List entries;
    private int zpos;

    /**
     * Constructs a ZipStreamDataNode from a DataSource object.
     */
    public ZipStreamDataNode( DataSource datsrc ) throws NoSuchDataException {
        super( getName( datsrc ), getMagic( datsrc ) );
        this.datsrc = datsrc;
        String path = getPath( datsrc );
        if ( path != null ) {
            setPath( path );
        }
    }

    protected synchronized List getEntries() throws IOException {
        if ( entries == null ) {
            entries = new ArrayList();
            ZipInputStream zs = getZipInputStream();
            for ( ZipEntry zent; 
                  ( zent = (ZipEntry) zs.getNextEntry() ) != null; ) {
                entries.add( zent );

                /* This sometimes throws an EOFException for some reason
                 * (and sometimes not) - just break out if so. */
                try {
                    zs.closeEntry();
                }
                catch ( EOFException e ) {
                    break;
                }
            }
            zs.close();
        }
        return entries;
    }

    protected synchronized InputStream getEntryInputStream( ZipEntry reqEnt ) 
            throws IOException {

        /* See if we have an open ZipInputStream which is positioned ahead
         * of the entry that has been requested. */
        int reqPos = entries.indexOf( reqEnt );
        if ( reqPos < 0 ) {
            throw new IllegalArgumentException( 
                "Entry " + reqEnt + " is not in this archive" );
        }

        /* If not, we have to close the current stream and open a new one
         * at the start. */
        if ( zstream == null || reqPos < zpos  ) {
            if ( zstream != null ) {
                zstream.close();
            }
            zstream = getZipInputStream();
            zpos = 0;
        }

        /* Flip through the entries in this stream in sequence until we
         * get the one we're after. */
        for ( ZipEntry ent; 
              ( ent = (ZipEntry) zstream.getNextEntry() ) != null; ) {

            /* Update the record of the one we've got to. */
            zpos++;

            /* If we find the one we want, return an input stream with its
             * content.  Note that we have to override the close method
             * to close the entry not the ZipInputStream itself, since
             * we may want to use that one later. */
            if ( ent.getName().equals( reqEnt.getName() ) ) {
                return new FilterInputStream( zstream ) {
                    public boolean markSupported() {
                        return false;
                    }
                    public void close() throws IOException {
                        zstream.closeEntry();
                    }
                };
            }

            /* Otherwise, close this entry and move to the next one. */
            else {
                zstream.closeEntry();
            }
        }

        /* Fell off the end. */
        throw new ZipException( "Entry " + reqEnt + " said it was in " +
                                "the ZipInputStream but wasn't" );
    }

    private ZipInputStream getZipInputStream() throws IOException {
        return new ZipInputStream( datsrc.getInputStream() );
    }

    private static byte[] getMagic( DataSource datsrc )
            throws NoSuchDataException {
        try {
            byte[] magic = new byte[ 8 ];
            datsrc.getMagic( magic );
            return magic;
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }
    
}
