package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * DataNode representing a Tar archive.
 */
public class TarStreamDataNode extends DefaultDataNode {

    private DataSource datsrc;
    private String name;
    private List entries;
    private TarInputStream tstream;
    private int tpos;

    /**
     * Constructs a TarStreamDataNode from a DataSource.
     *
     * @param  datsrc the source
     */
    public TarStreamDataNode( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        byte[] magic = new byte[ 300 ];
        try {
            int nmag = datsrc.getMagic( magic );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ! isMagic( magic ) ) {
            throw new NoSuchDataException( "Wrong magic number for tar" );
        }
        name = getName( datsrc );
        setLabel( name );
        String path = getPath( datsrc );
        if ( path != null ) {
            setPath( path );
        }
    }

    /**
     * Constructs a TarStreamDataNode from a File.
     * 
     * @param  file  the file
     */
    public TarStreamDataNode( File file ) throws NoSuchDataException {
        this( makeDataSource( file ) );
    }

    /**
     * Constructs a TarStreamDataNode from a filename.
     *
     * @param  fileName  the filename
     */
    public TarStreamDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return ":";
    }

    public Icon getIcon() {
        return IconFactory.getInstance().getIcon( IconFactory.TARFILE );
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
        final String pathHead = getPath() + getPathSeparator() + level;

        /* Get an iterator over all the TarEntries at the requested level. */
        final Iterator tentIt;
        try {
            tentIt = getEntriesAtLevel( level ).iterator();
        }
        catch ( IOException e ) {
            DataNode bumNode = childMaker.makeErrorDataNode( parent, e );
            return Collections.singleton( bumNode ).iterator();
        }

        /* Return an iterator which makes DataNodes from each entry. */
        return new Iterator() {
            public boolean hasNext() {
                return tentIt.hasNext();
            }
            public Object next() {

                /* Get the entry at the requested level. */
                final TarEntry tent = (TarEntry) tentIt.next();
                final String subname = tent.getName().substring( lleng );
                boolean isDir = tent.isDirectory();

                /* If it is a directory, make a TarBranchDataNode from it. */
                if ( isDir ) {
                    DataNode dnode = new TarBranchDataNode( tsdn, tent );
                    dnode.setCreator( new CreationState( parent ) );
                    dnode.setLabel( subname );
                    return dnode;
                }

                /* If it's a file, turn it into a DataSource and get the
                 * DataNodeFactory to make something appropriate from it. */
                else {
                    DataSource datsrc = new PathedDataSource() {
                        public String getPath() {
                            return pathHead + subname;
                        }
                        protected long getRawLength() {
                            return tent.getSize();
                        }
                        protected InputStream getRawInputStream()
                                throws IOException {
                            return getEntryInputStream( tent );
                        }
                    };
                    datsrc.setName( subname );
                    try {
                        return childMaker.makeDataNode( parent, datsrc );
                    }
                    catch ( NoSuchDataException e ) {
                        return childMaker.makeErrorDataNode( parent, e );
                    }
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private List getEntriesAtLevel( String level ) throws IOException {
        Set realDirs = new HashSet();
        Set impliedDirs = new TreeSet();
        List levEnts = new ArrayList();
        int lleng = level.length();

        /* Iterate over all entries in the archive. */
        if ( entries == null ) {
            entries = getEntries();
        }
        for ( Iterator entIt = entries.iterator(); entIt.hasNext(); ) {
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

    private synchronized List getEntries() throws IOException {
        if ( entries == null ) {
            entries = new ArrayList();
            TarInputStream ts = getTarInputStream();
            for ( TarEntry tent;
                  ( tent = (TarEntry) ts.getNextEntry() ) != null; ) {
                entries.add( tent );
            }
            ts.close();
        }
        return entries;
    }

    private synchronized InputStream getEntryInputStream( TarEntry reqEnt ) 
            throws IOException {
        int reqPos = entryPosition( reqEnt );
        if ( reqPos < 0 ) {
            throw new IllegalArgumentException(
                "Entry " + reqEnt.getName() + " is not in the archive" );
        }

        if ( tstream == null || reqPos < tpos ) {
            if ( tstream != null ) {
                tstream.close();
            }
            tstream = getTarInputStream();
            tpos = 0;
        }

        for ( TarEntry ent;
              ( ent = (TarEntry) tstream.getNextEntry() ) != null; ) {
            tpos++;

            if ( ent.getName().equals( reqEnt.getName() ) ) {
                return new FilterInputStream( tstream ) {
                    public boolean markSupported() {
                        return false;
                    }
                    public void close() throws IOException {
                        // don't close the TarInputStream!
                    }
                };
            }
        }

        throw new IOException( "Entry " + reqEnt + " said it was in " +
                               "the TarInputStream but wasn't" );
    }

    private TarInputStream getTarInputStream() throws IOException {
        return new TarInputStream( datsrc.getInputStream() );
    }

    private int entryPosition( TarEntry reqEnt ) throws IOException {
        if ( entries == null ) {
            entries = getEntries();
        }
        int i = 0;
        String reqName = reqEnt.getName();
        for ( Iterator it = entries.iterator(); it.hasNext(); ) {
            if ( ( (TarEntry) it.next() ).getName().equals( reqName ) ) {
                return i;
            }
            i++;
        }
        return -1;
    }

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
