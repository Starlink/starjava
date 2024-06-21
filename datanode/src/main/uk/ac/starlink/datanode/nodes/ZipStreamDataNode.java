package uk.ac.starlink.datanode.nodes;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.util.DataSource;

/**
 * DataNode representing a zip archive got from a stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ZipStreamDataNode extends ZipArchiveDataNode {

    private DataSource datsrc;
    private List entries;

    /**
     * Constructs a ZipStreamDataNode from a DataSource object.
     */
    public ZipStreamDataNode( DataSource datsrc ) throws NoSuchDataException {
        super( datsrc );
        this.datsrc = datsrc;
    }

    protected synchronized List getEntries() throws IOException {
        if ( entries == null ) {
            entries = new ArrayList();
            ZipInputStream zs = getZipInputStream();
            for ( ZipEntry zent; ( zent = zs.getNextEntry() ) != null; ) {
                entries.add( zent );
            }
            zs.close();
        }
        return entries;
    }

    protected Iterator getChildIteratorAtLevel( String level,
                                                final DataNode parent )
            throws IOException {
        final ZipArchiveDataNode zadn = this;
        final DataNodeFactory childMaker = getChildMaker();
        final int lleng = level.length();

        /* Get an iterator over all the ZipEntries at the requested level. */
        final Iterator zentIt = getEntriesAtLevel( level ).iterator();

        /* Get a stream which will run over the whole archive. */
        final ZipInputStream zstream = getZipInputStream();

        /* Return an iterator which makes DataNodes from each entry. */
        return new Iterator() {
            public Object next() {

                /* Get the next entry at the requested level. */
                final ZipEntry zent = (ZipEntry) zentIt.next();
                final String zname = zent.getName();
                final String subname = zname.substring( lleng );

                /* If it is a directory, make a ZipBranchDataNode from it. */
                if ( zent.isDirectory() ) {
                    DataNode dnode = new ZipBranchDataNode( zadn, zent );
                    getChildMaker().configureDataNode( dnode, parent, null );
                    dnode.setLabel( subname );
                    return dnode;
                }

                /* If it's a file, turn it into a DataSource and get the 
                 * DataNodeFactory to make something appropriate from it. */
                else {

                    /* See the similar TarStreamDataNode implementation for
                     * further comments on the following steps. */
                    DataSource childSrc;
                    try {

                        /* Advance the ZipInputStream to the current entry. */
                        boolean found = false;
                        for ( ZipEntry ent;
                              (ent = zstream.getNextEntry()) != null;) {
                            if ( ent.getName().equals( zname ) ) {
                                found = true;
                                break;
                            }
                        }
                        if ( ! found ) {
                            try {
                                throw new AssertionError( "Entry " + zname + 
                                                          " not found" );
                            }
                            catch ( AssertionError e ) {
                                DataNode node = getChildMaker()
                                               .makeErrorDataNode( parent, e );
                                node.setLabel( subname );
                                return node;
                            }
                        }

                        /* Make a DataSource which will use our zstream now,
                         * but a new ZipInputStream later. */
                        SwitchDataSource ssrc = 
                            new SwitchDataSource( zent.getSize() ) {
                                public InputStream getBackupRawInputStream()
                                        throws IOException {
                                    InputStream strm = 
                                        getEntryInputStream( zent );
                                    return strm;
                                }
                                public URL getURL() {
                                    return null;
                                }
                            };
                        ssrc.setName( subname );
                        ssrc.setProvisionalStream(
                            new FilterInputStream( zstream ) {
                                public void close() throws IOException {
                                    zstream.closeEntry();
                                }
                                public boolean markSupported() {
                                    return false;
                                }
                            } );
                        ssrc.getIntro();
                        ssrc.setProvisionalStream( null );
                        ssrc.close();
                        childSrc = ssrc;
                    }
                    catch ( IOException e ) {
                        return getChildMaker()
                              .makeErrorDataNode( parent, e );
                    }

                    /* If we are at the end of the children, close the zip 
                     * stream. */
                    if ( ! hasNext() ) {
                        try {
                            zstream.close();
                        }
                        catch ( IOException e ) {
                            // oh well
                        }
                    }
 
                    /* Construct the node as normal from its source. */
                    DataNode node = getChildMaker()
                                   .makeChildNode( parent, childSrc );
                    node.setLabel( subname );
                    return node;
                }
            }
            public boolean hasNext() {
                return zentIt.hasNext();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns an input stream giving the data from a given entry in this
     * archive.This stream is independent of any other streams associated
     * with the archive, so it can (and should) be closed by the user
     * when it's finished with.  It has to read through the whole zip
     * input stream from the beginning to get this, so it can be
     * expensive if you want a stream from a long way into a large 
     * archive on which reads are not cheap (for instance one coming
     * over the wire, or a compressed one).
     *
     * @param   reqEnt  the entry for which the stream data is required
     * @return  a stream containing the data in <code>reqEnt</code>
     */
    private InputStream getEntryInputStream( ZipEntry reqEnt ) 
            throws IOException {
        String reqName = reqEnt.getName();
        ZipInputStream zstream = getZipInputStream();
        for ( ZipEntry ent; ( ent = zstream.getNextEntry() ) != null; ) {
            if ( ent.getName().equals( reqName ) ) {
                return zstream;
            }
        }
        zstream.close();
        throw new IOException( "Entry " + reqEnt + " not in this archive" );
    }


    private ZipInputStream getZipInputStream() throws IOException {
        return new ZipInputStream( datsrc.getInputStream() );
    }

}
