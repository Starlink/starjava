package uk.ac.starlink.treeview;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import javax.swing.Icon;
import uk.ac.starlink.util.DataSource;

/**
 * DataNode representing a Zip archive.
 * This abstract class embodies the common functionality required by
 * its subclasses {@link ZipFileDataNode} and {@link ZipStreamDataNode}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ZipArchiveDataNode extends DefaultDataNode {

    private String name;
    private List entries;

    /**
     * Constructs a ZipArchiveDataNode.
     *
     * @param  name  the name of the node
     * @param  magic  a byte array at least 4 elements long containing the
     *         first few bytes of the zip file, used to check whether
     *         this looks like a zip archive or not
     */
    protected ZipArchiveDataNode( String name, byte[] magic ) 
            throws NoSuchDataException {
        this.name = name;
        setLabel( name );
        if ( ! isMagic( magic ) ) {
            throw new NoSuchDataException( "Wrong magic number for zip" );
        }
    }

    /**
     * Returns an input stream containing the data from a given ZipEntry
     * object in this archive.
     *
     * @param  zent the ZipEntry object whose data is required
     * @return an InputStream giving the contents of <tt>zent</tt>
     */
    protected abstract InputStream getEntryInputStream( ZipEntry zent ) 
        throws IOException;

    /**
     * Returns a list of all the <tt>ZipEntry</tt> objects in 
     * this zip archive.  This will only be called once, so does not
     * need to cache its return value.
     *
     * @return  a List of all the {@link java.util.zip.ZipEntry} objects
     *          which make up this zip archive.
     */
    protected abstract List getEntries() throws IOException;

    /**
     * Tests whether the presented byte array looks like the start of a
     * Zip archive.
     *
     * @param  magic  a byte array of at least 4 elements containing the
     *         first few bytes of a source which might be a zip
     * @return true iff <tt>magic</tt> represents the magic number of a 
     *         zip archive
     */
    public static boolean isMagic( byte[] magic ) {
        return (char) magic[ 0 ] == 'P'
            && (char) magic[ 1 ] == 'K'
            && magic[ 2 ] == 3
            && magic[ 3 ] == 4;
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
        return "Zip archive";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return getChildIteratorAtLevel( "", this );
    }

    /**
     * Returns an iterator over the DataNodes at a given level in the
     * hierarchy of this archive.  The iterator creates DataNodes for
     * each ZipEntry in this archive whose name begins with the 
     * supplied string <tt>level</tt>.
     *
     * @param  level  the required starting substring of the name of all
     *         ZipEntries to be represented in the result
     * @param  parent  the DataNode whose children the resulting nodes will be
     * @return  an Iterator over {@link DataNode} objects corresponding to
     *          the ZipEntry objects specified by <tt>level</tt>
     */
    Iterator getChildIteratorAtLevel( String level, final DataNode parent ) {
        final ZipArchiveDataNode zadn = this;
        final DataNodeFactory childMaker = getChildMaker();
        final int lleng = level.length();
        final String pathHead = getPath() + getPathSeparator() + level;

        /* Get an iterator over all the ZipEntries at the requested level. */
        final Iterator zentIt;
        try {
            zentIt = getEntriesAtLevel( level ).iterator();
        }
        catch ( IOException e ) {
            DataNode bumNode = childMaker.makeErrorDataNode( parent, e );
            return Collections.singleton( bumNode ).iterator();
        }

        /* Return an iterator which makes DataNodes from each ZipEntry. */
        return new Iterator() {
            public boolean hasNext() {
                return zentIt.hasNext();
            }
            public Object next() {

                /* Get the next ZipEntry at the requested level. */
                final ZipEntry zent = (ZipEntry) zentIt.next();
                final String subname = zent.getName().substring( lleng );
                boolean isDir = zent.isDirectory();

                /* If it is a directory, make a ZipBranchDataNode from it. */
                if ( isDir ) {
                    DataNode dnode = new ZipBranchDataNode( zadn, zent );
                    dnode.setCreator( new CreationState( parent ) );
                    dnode.setLabel( subname );
                    return dnode;
                }

                /* If it's a file, turn it into a DataSource and get the
                 * DataNodeFactory to make something appropriate of it. */
                else {
                    DataSource datsrc = new PathedDataSource() {
                        public String getPath() {
                            return pathHead + subname;
                        }
                        protected long getRawLength() {
                            return zent.getSize();
                        }
                        protected InputStream getRawInputStream()
                                throws IOException {
                            return getEntryInputStream( zent );
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

    /**
     * Returns all the ZipEntry objects in this archive at a given level
     * in the hierarchy.  The selected entries are all those whose name
     * starts with the supplied string <tt>level</tt>.
     *
     * @param  level the required starting substring of the name of all
     *         ZipEntries to be returned
     * @return a list of all the {@link java.util.zip.ZipEntry}
     *         objects at the given <tt>level</tt>
     */
    protected List getEntriesAtLevel( String level ) throws IOException {

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

        /* Iterate over all entries in the archive. */
        if ( entries == null ) {
            entries = getEntries();
        }
        for ( Iterator entIt = entries.iterator(); entIt.hasNext(); ) {
            ZipEntry ent = (ZipEntry) entIt.next();
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
            levEnts.add( new ZipEntry( level + dirname ) );
        }

        /* Return all the entries. */
        return levEnts;
    }

}
