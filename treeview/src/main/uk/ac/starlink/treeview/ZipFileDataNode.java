package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.Icon;
import uk.ac.starlink.util.DataSource;


/**
 * A {@link DataNode} representing a zip file.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ZipFileDataNode extends DefaultDataNode {

    private String name;
    private ZipFile zfile;
    private File file;

    /**
     * Initialises a <code>ZipFileDataNode</code> from a 
     * <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file from
     *               which the node is to be created
     */
    public ZipFileDataNode( File file ) throws NoSuchDataException {
        if ( ! checkCouldBeZip( file ) ) {
            throw new NoSuchDataException( "Wrong magic number for Zip" );
        }
        try {
            zfile = new ZipFile( file );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        this.file = file;
        name = file.getName();
        setLabel( name );
    }

    /**
     * Initialises a <code>ZipFileDataNode</code> from a <code>String</code>.
     *
     * @param  fileName  the absolute or relative name of the zip file.
     */
    public ZipFileDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return getChildIteratorAtLevel( "", this );
    }

    Iterator getChildIteratorAtLevel( String level, final DataNode parent ) {
        final DataNodeFactory childMaker = getChildMaker();
        ZipEntry[] zents = getEntriesAtLevel( level );
        final Iterator zentIt = Arrays.asList( zents ).iterator();
        final int lleng = level.length();
        final ZipFileDataNode zfdn = ZipFileDataNode.this;
        return new Iterator() {
            public boolean hasNext() {
                return zentIt.hasNext();
            }
            public Object next() {
                final ZipEntry zent = (ZipEntry) zentIt.next();
                boolean isDir = zent.isDirectory();
                final String subname = zent.getName().substring( lleng );
                if ( isDir ) {
                    DataNode dnode = new ZipBranchDataNode( zfdn, zent );
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
                            return zfile.getInputStream( zent );
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

    public boolean hasParentObject() {
        return file.getAbsoluteFile().getParentFile() != null;
    }

    public Object getParentObject() {
        return file.getAbsoluteFile().getParentFile();
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return IconFactory.getInstance().getIcon( IconFactory.ZIPFILE );
    }

    public String getPathSeparator() {
        return ":";
    }

    /**
     * Returns the string "ZIP".
     *
     * @return  "ZIP"
     */
    public String getNodeTLA() {
        return "ZIP";
    }

    public String getNodeType() {
        return "Zip file";
    }

    /*
     * Throws a NoSuchDataException if this file isn't worth trying.
     * This is not required, but speeds up the DataNodeFactory's operation
     * a great deal.
     */
    private static boolean checkCouldBeZip( File file ) {
        try {
            return ( isMagic( startBytes( file, 80 ) ) );
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /*
     * Returns a list of the ZipEntries at a given level (i.e. path prefix)
     * in the zip file.  Since the file access proved by the java.util.zip
     * classes is not itself hierarchical (you just get a list of entries)
     * this is a bit fiddly to do.  Moreover (I think) you can have files
     * (leaves) within a zip directory (branch) in a zip file without 
     * branch actually having an entry in the zip file, so we have to watch
     * out for this and construct the corresponding (dummy) ZipEntries
     * specially if required.
     */
    private ZipEntry[] getEntriesAtLevel( String level ) {

        /* Loop over all the entries in the zip file, getting entries at
         * the requested level.  This is harder work than you might think,
         * since we need to pick up directories which don't actually
         * exist in the archive, but whose presence is implied by
         * entries contained within them. */
        /* I'm assuming that within a zip file, the directory separator
         * character is '/' and all directory entry names end in a '/'. */
        Set realDirs = new HashSet();
        Set impliedDirs = new TreeSet();
        List levEnts = new ArrayList();
        int lleng = level.length();
        for ( Enumeration entEn = zfile.entries(); entEn.hasMoreElements(); ) {
            ZipEntry ent = (ZipEntry) entEn.nextElement();
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
            levEnts.add( new ZipEntry( dirname ) );
        }

        /* Return an array of all the entries. */
        return (ZipEntry[]) levEnts.toArray( new ZipEntry[ 0 ] );
    }

    public static boolean isMagic( byte[] magic ) {
        return (char) magic[ 0 ] == 'P'
            && (char) magic[ 1 ] == 'K'
            && magic[ 2 ] == 3
            && magic[ 3 ] == 4;
    }

}
