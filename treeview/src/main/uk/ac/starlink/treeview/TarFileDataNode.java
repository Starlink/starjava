package uk.ac.starlink.treeview;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

public class TarFileDataNode extends DefaultDataNode {

    private TarInputStream tarStrm;
    private String name;
    private Icon icon;
    private TarEntry[] entries;
    private DataSource source;

    /**
     * Constructs a TarFileDataNode from an input stream.
     *
     * @param  istrm  the stream
     */
    public TarFileDataNode( DataSource source ) throws NoSuchDataException {
        this.source = source;
        byte[] magic = new byte[ 300 ];
        try {
            int nmag = source.getMagic( magic );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        if ( ! isMagic( magic ) ) {
            throw new NoSuchDataException( "Wrong magic number for tar" );
        }
        try {
            tarStrm = new TarInputStream( source.getInputStream() );
        }
        catch ( Exception e ) {
            throw new NoSuchDataException( "Not a tar stream", e );
        }
        name = source.getName();
        setLabel( name );
    }

    /**
     * Constructs a TarFileDataNode from a file.
     *
     * @param  file  the file
     */
    public TarFileDataNode( File file ) throws NoSuchDataException {
        this( new FileDataSource( file ) );
    }

    /**
     * Constructs a TarFileDataNode from a filename.
     *
     * @param   fileName  the file name
     */
    public TarFileDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return ":";
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.TARFILE );
        }
        return icon;
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
        final DataNodeFactory childMaker = getChildMaker();
        TarEntry[] tents = getEntriesAtLevel( level );
        final Iterator tentIt = Arrays.asList( tents ).iterator();
        final int lleng = level.length();
        final TarFileDataNode tfdn = TarFileDataNode.this;
        return new Iterator() {
            public boolean hasNext() {
                return tentIt.hasNext();
            }
            public Object next() {
                final TarEntry tent = (TarEntry) tentIt.next();
                boolean isDir = tent.isDirectory();
                final String subname = tent.getName().substring( lleng );
                if ( isDir ) {
                    DataNode dnode = new TarBranchDataNode( tfdn, tent );
                    dnode.setCreator( new CreationState( parent ) );
                    dnode.setLabel( subname );
                    return dnode;
                }
                else {
                    DataSource datsrc = new DataSource() {
                        public String getName() {
                            return subname;
                        }
                        protected InputStream getRawInputStream()
                                throws IOException {
                            return getContentStream( tent );
                        }
                    };
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


    private TarEntry[] getEntriesAtLevel( String level ) {

        /* Set up the list of entries if this is not already done. */
        if ( entries == null ) {
            List ents = new ArrayList();
            try {
                for ( TarEntry ent; 
                      ( ent = tarStrm.getNextEntry() ) != null; ) {
                    ents.add( ent );
                }
                tarStrm.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
            entries = (TarEntry[]) ents.toArray( new TarEntry[ 0 ] );
        }

        /* Get the entries at the requested level.  This is harder work than
         * you might think, since we need to pick up directories which 
         * don't actually exist in the archive, but whose presence is
         * implied by entries contained within them. */
        Set realDirs = new HashSet();
        Set impliedDirs = new TreeSet();
        List levEnts = new ArrayList();
        int lleng = level.length();
        for ( int i = 0; i < entries.length; i++ ) {
            TarEntry ent = entries[ i ];
            String entname = ent.getName();
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
            levEnts.add( new TarEntry( dirname ) );
        }

        /* Return an array of all the entries. */
        return (TarEntry[]) levEnts.toArray( new TarEntry[ 0 ] );
    }

    /**
     * Returns the stream representing the content of this tar file.
     *
     * @return  a TarInputStream holding this tar archive
     */
    public TarInputStream getTarInputStream() throws IOException {
        return new TarInputStream( source.getInputStream() );
    }

    /**
     * Returns a stream holding the content of a given tar file entry.
     */
    public InputStream getContentStream( TarEntry tent ) throws IOException {
        TarInputStream tstrm = getTarInputStream();
        while ( true ) {
            TarEntry ent = tstrm.getNextEntry();
            if ( ent.equals( tent ) ) {
                return tstrm;
            }
            if ( ent == null ) {
                throw new FileNotFoundException( 
                    "Entry " + tent + " not found in file" );
            }
        }
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
