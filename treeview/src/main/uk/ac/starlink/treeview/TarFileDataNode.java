package uk.ac.starlink.treeview;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.Icon;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

public class TarFileDataNode extends DefaultDataNode {

    private TarInputStream tarStrm;
    private String name;
    private Icon icon;
    private TarEntry[] entries;
    private File file;

    /**
     * Constructs a TarFileDataNode from an input stream.
     *
     * @param  istrm  the stream
     */
    public TarFileDataNode( InputStream istrm ) throws NoSuchDataException {
        int magicsize = 300;
        if ( ! istrm.markSupported() ) {
            istrm = new BufferedInputStream( istrm );
        }
        try {
            if ( ! isMagic( startBytes( istrm, 300 ) ) ) {
                throw new NoSuchDataException( "Wrong magic number for tar" );
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        try {
            tarStrm = new TarInputStream( istrm );
        }
        catch ( Exception e ) {
            throw new NoSuchDataException( "Not a tar stream", e );
        }
    }

    /**
     * Constructs a TarFileDataNode from a file.
     *
     * @param  file  the file
     */
    public TarFileDataNode( File file ) throws NoSuchDataException {
        this( getInputStream( file ) );
        this.file = file;
        name = file.getName();
        setLabel( name );
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

    public String getPath() {
        return ( file != null ) ? file.getAbsolutePath()
                                : super.getPath();
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
        TarEntry[] tents = getEntriesAtLevel( level );
        final Iterator tentIt = Arrays.asList( tents ).iterator();
        return new Iterator() {
            public boolean hasNext() {
                return tentIt.hasNext();
            }
            public Object next() {
                TarFileDataNode tfdn = TarFileDataNode.this;
                TarEntry tent = (TarEntry) tentIt.next();
                try {
                    DataNode dnode = new TarEntryDataNode( tfdn, tent );
                    dnode.setCreator( new CreationState( parent ) );
                    return dnode;
                }
                catch ( NoSuchDataException e ) {
                    return getChildMaker().makeErrorDataNode( parent, e );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the stream representing the content of this tar file, 
     * if one exists, or <tt>null</tt> if it can't be done.
     *
     * @return  a TarInputStream holding this tar archive, or <tt>null</tt>
     */
    public TarInputStream getTarInputStream() throws IOException {
        return ( file == null ) 
                   ? null
                   : new TarInputStream( new FileInputStream( file ) );
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
         * implied by entries contained within them.  First work out a 
         * list of all real and implied entries at the given level. */
        SortedMap map = new TreeMap();
        int lleng = level.length();
        for ( int i = 0; i < entries.length; i++ ) {
            TarEntry ent = entries[ i ];
            String name = ent.getName();
            if ( name.startsWith( level ) & ! name.equals( level ) ) {
                String subname = name.substring( lleng );
                int slashix = subname.indexOf( '/' );
                String dirname = subname.substring( 0, slashix + 1 );
                if ( slashix >= 0 ) {
                    if ( slashix == subname.length() - 1 ) {
                        map.put( dirname, ent );
                    }
                    else if ( ! map.containsKey( dirname ) ) {
                        map.put( dirname, null );
                    }
                }
                else {
                    map.put( subname, ent );
                }
            }
        }

        /* Then construct a list containing the found entries and dummy 
         * entries for implied directories. */
        TarEntry[] tents = new TarEntry[ map.size() ];
        Iterator itemIt = map.entrySet().iterator();
        for ( int i = 0; itemIt.hasNext(); i++ ) {
            Map.Entry item = (Map.Entry) itemIt.next();
            String name = (String) item.getKey();
            TarEntry tent = (TarEntry) item.getValue();
            if ( tent == null ) {
                tents[ i ] = new TarEntry( name );
            }
            else {
                tents[ i ] = tent;
            }
        }
        return tents;
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
