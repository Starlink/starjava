package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * A DataNode representing a zip archive stored in a file.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ZipFileDataNode extends ZipArchiveDataNode {

    private ZipFile zfile;
    private File file;
    private List entries;

    /**
     * Initialises a <code>ZipFileDataNode</code> from a
     * <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file from
     *               which the node is to be created
     */
    public ZipFileDataNode( File file ) throws NoSuchDataException {
        super( getDataSource( file ) );
        try {
            zfile = new ZipFile( file );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        this.file = file;
        setName( file.getName() );
        setLabel( file.getName() );
    }

    public Object getParentObject() {
        return file.getAbsoluteFile().getParentFile();
    }

    protected List getEntries() throws IOException {
        if ( entries == null ) {
            entries = new ArrayList();
            for ( Enumeration enEn = zfile.entries();
                  enEn.hasMoreElements(); ) {
                entries.add( enEn.nextElement() );
            }
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

        /* Return an iterator which makes DataNodes from each ZipEntry. */
        return new Iterator() {
            public Object next() {

                /* Get the next ZipEntry at the requested level. */
                final ZipEntry zent = (ZipEntry) zentIt.next();
                final String subname = zent.getName().substring( lleng );

                /* If it is a directory, make a ZipBranchDataNode from it. */
                if ( zent.isDirectory() ) {
                    DataNode dnode = new ZipBranchDataNode( zadn, zent );
                    getChildMaker().configureDataNode( dnode, parent, null );
                    dnode.setLabel( subname );
                    return dnode;
                }

                /* If it's a file, turn it into a DataSource pass it to
                 * the DataNodeFactory. */
                else {
                    DataSource datsrc = new DataSource() {
                        public long getRawLength() {
                            return zent.getSize();
                        }
                        protected InputStream getRawInputStream() 
                                throws IOException {
                            return zfile.getInputStream( zent );
                        }
                        public URL getURL() {
                            return null;
                        }
                    };
                    datsrc.setName( subname );
                    DataNode node = childMaker.makeChildNode( parent, datsrc );
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

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Length", file.length() );
        dv.addKeyedItem( "Number of entries", zfile.size() );
    }

    private static DataSource getDataSource( File file )
            throws NoSuchDataException {
        try {
            DataSource datsrc = new FileDataSource( file );
            datsrc.setIntroLimit( 12 );  // enough for zip magic number
            datsrc.getIntro();
            datsrc.close();
            return datsrc;
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

}
