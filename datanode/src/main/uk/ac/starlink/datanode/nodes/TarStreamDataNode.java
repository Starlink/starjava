package uk.ac.starlink.datanode.nodes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * DataNode representing a Tar archive.
 */
public class TarStreamDataNode extends DefaultDataNode {

    private DataSource datsrc;
    private String name;
    private List entries;

    /**
     * Constructs a TarStreamDataNode from a DataSource.
     *
     * @param  datsrc the source
     */
    public TarStreamDataNode( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        byte[] magic;
        try {
            magic = datsrc.getIntro();
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ! isMagic( magic ) ) {
            throw new NoSuchDataException( "Wrong magic number for tar" );
        }
        name = datsrc.getName();
        setLabel( name );
        setIconID( IconFactory.TARFILE );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return ":";
    }

    public String getNodeTLA() {
        return "TAR";
    }

    public String getNodeType() {
        return "Tar archive";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return getChildIteratorAtLevel( "", this );
    }

    Iterator getChildIteratorAtLevel( String level, final DataNode parent ) {
        final TarStreamDataNode tsdn = this;
        final DataNodeFactory childMaker = getChildMaker();
        final int lleng = level.length();
        final TarInputStream tstream;

        /* Get an iterator over all the TarEntries at the requested level. */
        final Iterator tentIt;
        try {
            tentIt = getEntriesAtLevel( level ).iterator();
            tstream = getTarInputStream();
        }
        catch ( IOException e ) {
            return Collections.singleton( getChildMaker()
                                         .makeErrorDataNode( parent, e ) )
                              .iterator();
        }

        /* Return an iterator which makes DataNodes from each entry. */
        return new Iterator() {

            public boolean hasNext() {
                return tentIt.hasNext();
            }

            public Object next() {

                /* Get the entry at the requested level. */
                final TarEntry tent = (TarEntry) tentIt.next();
                final String tname = tent.getName();
                final String subname = tname.substring( lleng );

                /* If it is a directory, make a TarBranchDataNode from it. */
                if ( tent.isDirectory() ) {
                    DataNode dnode = new TarBranchDataNode( tsdn, tent );
                    getChildMaker().configureDataNode( dnode, parent, null );
                    dnode.setLabel( subname );
                    return dnode;
                }

                /* If it's a file, turn it into a DataSource and get the
                 * DataNodeFactory to make something appropriate from it. */
                else {

                    /* This is slightly tricky.  We construct a DataSource
                     * which uses the data in the current tar stream the 
                     * first time it is used, but on subsequent occasions
                     * it creates a new one.  This means that for running
                     * through all the children probably the expensive
                     * getEntryInputStream does not need to get used,
                     * but it will be necessary (there is no way round it)
                     * if subsequent clients want an input stream.  */
                    DataSource childSrc;
                    try {

                        /* First advance the TarInputStream to the start of the
                         * current entry. */
                        boolean found = false;
                        for ( TarEntry ent; 
                              ( ent = getNextEntry( tstream ) ) != null; ) {
                            if ( ent.getName().equals( tname ) ) {
                                found = true;
                                break;
                            }
                        }

                        /* In case we can't find the entry (shouldn't happen) */
                        if ( ! found ) {
                            try {
                                throw new AssertionError( "Can't find entry" 
                                                        + tname );
                            }
                            catch ( AssertionError e ) {
                                DataNode node = getChildMaker()
                                               .makeErrorDataNode( parent, e );
                                node.setLabel( tname );
                                return node;
                            }
                        }

                        /* Make a DataSource out of it which will, for now,
                         * use the TarInputStream for its raw data. */
                        SwitchDataSource ssrc = 
                            new SwitchDataSource( tent.getSize() ) {
                                public InputStream getBackupRawInputStream()
                                        throws IOException {
                                    InputStream strm = 
                                        getEntryInputStream( tent );

                                    /* For reasons I entirely fail to
                                     * understand, unless this stream is
                                     * wrapped in a BufferedInputStream here,
                                     * it can lead to compressed sources
                                     * within tar files (and elsewhere?)
                                     * apprently returning corrupted data
                                     * (gzip header absent/incorrect).
                                     * My attempts to track this down by
                                     * placing traces on the read() methods
                                     * of various streams have been fruitless.
                                     * This does _not_ appear to the the
                                     * problem with compressed streams
                                     * claiming they support mark/reset
                                     * when they don't.
                                     * So reluctantly I wrap it here. */
                                    strm = new BufferedInputStream( strm );
                                    return strm;
                                }
                                public URL getURL() {
                                    return null;
                                }
                            };
                        ssrc.setName( subname );
                        ssrc.setProvisionalStream( 
                            new FilterInputStream( tstream ) {
                                public void close() {
                                    // do not close TarInputStream
                                }
                            } );

                        /* Read some data from the data source; the source will
                         * cache this and can use it for magic number requests
                         * during node construction attempts.  This should
                         * mean that in most cases to construct a node the
                         * childMaker below will not need to do any more
                         * reads on the input (and hence will not need to
                         * call the expensive getEntryInputStream). */
                        ssrc.getIntro();

                        /* Now prevent the provisional source from using
                         * the TarInputStream any more so that subsequent
                         * reads will need to open their own, safe, input
                         * stream if they in fact do need a stream. 
                         * The TarInputStream is still available for 
                         * further use within this child iterator. */
                        ssrc.setProvisionalStream( null );
                        ssrc.close();

                        /* We have our source. */
                        childSrc = ssrc;
                    }
                    catch ( IOException e ) {
                        return getChildMaker().makeErrorDataNode( parent, e );
                    }

                    /* If we are at the end of the children, close the 
                     * tar stream. */
                    if ( ! hasNext() ) {
                        try {
                            tstream.close();
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
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a list of all the entries in this archive whose name starts
     * with a given string.
     *
     * @param   level  the required prefix
     * @return  a list of all the <code>TarEntry</code> objects in this archive 
     *          whose names begin with <code>level</code>.  They appear in the
     *          list in the same order as they appear in the archive
     */
    protected List getEntriesAtLevel( String level ) throws IOException {
        Set realDirs = new HashSet();
        Set impliedDirs = new TreeSet();
        List levEnts = new ArrayList();
        int lleng = level.length();

        /* Iterate over all entries in the archive. */
        for ( Iterator entIt = getEntries().iterator(); entIt.hasNext(); ) {
            TarEntry ent = (TarEntry) entIt.next();
            String entname = ent.getName();

            /* Select only those entries with the right prefix. */
            if ( entname.startsWith( level ) && ! entname.equals( level ) ) {
                String subname = entname.substring( lleng );
                int slashix = subname.indexOf( '/' );

                /* Entry is a directory, real or implied. */
                if ( slashix >= 0 ) {
                    String dirname = subname.substring( 0, slashix + 1 );
                    if ( slashix == subname.length() - 1 ) {
                        realDirs.add( dirname );
                        levEnts.add( ent );
                    }
                    else {
                        impliedDirs.add( dirname );
                    }
                }

                /* Entry is a file. */
                else {
                    levEnts.add( ent );
                }
            }
        }

        /* Add dummy entries for phantom directories. */
        impliedDirs.removeAll( realDirs );
        for ( Iterator phIt = impliedDirs.iterator(); phIt.hasNext(); ) {
            String dirname = (String) phIt.next();
            levEnts.add( new TarEntry( level + dirname ) );
        }

        /* Return all the entries. */
        return levEnts;
    }

    /**
     * Returns a list of all the <code>TarEntry</code> objects in this archive.
     *
     * @return  a list of the entries of this archive, in order of their
     *          appearance in the archive
     */
    private synchronized List getEntries() throws IOException {
        if ( entries == null ) {
            entries = new ArrayList();
            TarInputStream ts = getTarInputStream();
            for ( TarEntry tent; ( tent = getNextEntry( ts ) ) != null; ) {
                entries.add( tent );
            }
            ts.close();
        }
        return entries;
    }

    /**
     * Returns an input stream giving the data from a given entry in this 
     * archive.  This stream is independent of any other streams associated
     * with the archive, so it can (and should) be closed by the user
     * when it's finished with.  It has to read through the whole tar
     * input stream from the beginning to get this, so it can be 
     * expensive if you want a stream from a long way into a large tar
     * archive on which reads are not cheap (for instance one coming
     * over the wire, or a compressed one).
     *
     * @param   reqEnt  the entry for which the stream data is required
     * @return  a stream containing the data in <code>reqEnt</code>
     */
    private InputStream getEntryInputStream( TarEntry reqEnt ) 
            throws IOException {
        String reqName = reqEnt.getName();
        TarInputStream tstream = getTarInputStream();
        for ( TarEntry ent; ( ent = getNextEntry( tstream ) ) != null; ) {
            if ( ent.getName().equals( reqName ) ) {
                return tstream;
            }
        }
        tstream.close();
        throw new IOException( "Entry " + reqEnt + " not in this archive" );
    }

    /**
     * Returns a new TarInputStream associated with this archive.
     *
     * @return  a tar input stream
     */
    private TarInputStream getTarInputStream() throws IOException {
        return new TarInputStream( datsrc.getInputStream() );
    }

    /**
     * Reads an entry from a TarInputStream.
     * This does much the same as <code>tstrm.getNextEntry()</code>, but 
     * does a bit of essential doctoring on the entry name.
     *
     * @param  tstrm  the tar input stream
     * @return  the next tar entry, or <code>null</code> if there is none or if
     *          any I/O error occurred
     */
    private static TarEntry getNextEntry( TarInputStream tstrm ) {
        try {
            TarEntry tent = tstrm.getNextEntry();
            if ( tent != null && 
                 tent.isDirectory() && 
                 ! tent.getName().endsWith( "/" ) ) {
                tent.setName( tent.getName() + '/' );
            }
            return tent;
        }
        catch ( IOException e ) {
            return null;
        }
    }

    /**
     * Indicates whether the given bytes look like the start of a tar archive.
     *
     * @param   magic  a buffer of bytes containing at least the first
     *                 264 bytes of a potential tar stream
     * @return  true   if <code>magic</code> looks like the start
     *                 of a tar stream
     */
    public static boolean isMagic( byte[] magic ) {
        return magic.length > 264
            && (char) magic[ 257 ] == 'u'
            && (char) magic[ 258 ] == 's'
            && (char) magic[ 259 ] == 't'
            && (char) magic[ 260 ] == 'a'
            && (char) magic[ 261 ] == 'r'
            && ( (    (char) magic[ 262 ] == '\0' )
              || (    (char) magic[ 262 ] == ' '
                   && (char) magic[ 263 ] == ' '
                   && (char) magic[ 264 ] == '\0' ) );
    }

}
