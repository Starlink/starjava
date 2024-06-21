package uk.ac.starlink.datanode.nodes;

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
     * @param  datsrc a data source corresponding to the archive; this is
     *         used for checking the magic number to see whether it looks
     *         like a zip archive or not
     */
    protected ZipArchiveDataNode( DataSource datsrc ) 
            throws NoSuchDataException {
        this.name = datsrc.getName();
        setLabel( name );
        try {
            if ( ! isMagic( datsrc.getIntro() ) ) {
                throw new NoSuchDataException( "Wrong magic number for zip" );
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "Can't see if it's zip", e );
        }
        setIconID( IconFactory.ZIPFILE );
        registerDataObject( DataType.DATA_SOURCE, datsrc );
    }

    /**
     * Returns a list of all the <code>ZipEntry</code> objects in 
     * this zip archive.
     *
     * @return  a List of all the {@link java.util.zip.ZipEntry} objects
     *          which make up this zip archive.
     */
    protected abstract List getEntries() throws IOException;

    /**
     * Returns an iterator over the DataNodes at a given level in the
     * hierarchy of this archive.  The iterator creates DataNodes for
     * each ZipEntry in this archive whose name begins with the 
     * supplied string <code>level</code>.
     *
     * @param  level  the required starting substring of the name of all
     *         ZipEntries to be represented in the result
     * @param  parent  the DataNode whose children the resulting nodes will be
     * @return  an Iterator over {@link DataNode} objects corresponding to
     *          the ZipEntry objects specified by <code>level</code>
     */
    protected abstract Iterator getChildIteratorAtLevel( String level,
                                                         DataNode parent )
            throws IOException;

    /**
     * Tests whether the presented byte array looks like the start of a
     * Zip archive.
     *
     * @param  magic  a byte array containing the
     *         first few bytes of a source which might be a zip
     * @return true iff <code>magic</code> represents the magic number of a 
     *         zip archive
     */
    public static boolean isMagic( byte[] magic ) {
        return magic.length > 4
            && (char) magic[ 0 ] == 'P'
            && (char) magic[ 1 ] == 'K'
            && magic[ 2 ] == 3
            && magic[ 3 ] == 4;
    }

    public String getName() {
        return name;
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
        try {
            return getChildIteratorAtLevel( "", this );
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
    }

    /**
     * Returns all the ZipEntry objects in this archive at a given level
     * in the hierarchy.  The selected entries are all those whose name
     * starts with the supplied string <code>level</code>.
     *
     * @param  level the required starting substring of the name of all
     *         ZipEntries to be returned
     * @return a list of all the {@link java.util.zip.ZipEntry}
     *         objects at the given <code>level</code>
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
