package uk.ac.starlink.treeview;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

/**
 * A {@link DataNode} representing a zip file.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ZipFileDataNode extends DefaultDataNode {
    private static Icon icon;

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

    /**
     * Returns all the entries in the zipfile.  Note that this is a flat
     * list; any directory structure implicit in the contents of the
     * zip file is ignored.
     *
     * @return   an array of <code>DataNode</code>s representing the 
     *           entries in the zip file
     */
    public Iterator getChildIterator() {

        /* Note that when constructing the child nodes here we first construct
         * DataNodes corresponding to the entries in the ZipFile 
         * (these will be ZipBranchDataNodes and ZipLeafDataNodes), 
         * then pass these to the DataNodeFactory so that any class who
         * wishes to make something more specific from it can do 
         * (if they have a constructor which takes a ZipLeafDataNode). */
        DataNode[] nodes = getEntriesAtLevel( zfile, "" );
        final Iterator nodeIt = Arrays.asList( nodes ).iterator();
        return new Iterator() {
            public boolean hasNext() {
                return nodeIt.hasNext();
            }
            public Object next() {
                DataNode node = (DataNode) nodeIt.next();
                try {
                   return getChildMaker()
                         .makeDataNode( ZipFileDataNode.this, node );
                }
                catch ( NoSuchDataException e ) {
                    DataNode parent = ZipFileDataNode.this;
                    node.setCreator( new CreationState( parent ) );
                    return node;
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
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.ZIPFILE );
        }
        return icon;
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
     * Returns a list of the DataNodes at a given level (i.e. path prefix)
     * in the zip file.  These will be ZipBranchDataNodes and 
     * ZipLeafDataNodes.  Since the file access proved by the java.util.zip
     * classes is not itself hierarchical (you just get a list of entries)
     * this is a bit fiddly to do.  Moreover (I think) you can have files
     * (leaves) within a zip directory (branch) in a zip file without 
     * branch actually having an entry in the zip file, so we have to watch
     * out for this and construct the corresponding ZipBranchDataNodes
     * specially if required.
     */
    static DataNode[] getEntriesAtLevel( ZipFile zfile, String level ) {

        /* Loop over all the entries in the zip file, and make map entries
         * for those which are at the right level.  Each entry has
         * key=name and value=ZipEntry value.  Directories are identified
         * by a name that ends in '/'.  In the case of a phantom directory,
         * the ZipEntry is null.  I'm assuming that within a zip file,
         * the directory separator character is '/' and all directory
         * entry names end in a '/'. */
        Enumeration entEn = zfile.entries();
        SortedMap map = new TreeMap();
        int lleng = level.length();
        while ( entEn.hasMoreElements() ) {
            ZipEntry entry = (ZipEntry) entEn.nextElement();
            String name = entry.getName();
            if ( name.startsWith( level ) && ! name.equals( level ) ) {
                String subname = name.substring( lleng );
                int slashix = subname.indexOf( '/' );
                String dirname = subname.substring( 0, slashix + 1 );
                if ( slashix >= 0 ) {
                    if ( slashix == subname.length() - 1 ) {
                        map.put( dirname, entry );
                    }
                    else if ( ! map.containsKey( dirname ) ) {
                        map.put( dirname, null );
                    }
                }
                else {
                    map.put( subname, entry );
                }
            }
        }

        /* Construct an array of DataNodes from the map entries. */
        DataNode[] nodes = new DataNode[ map.size() ];
        Iterator itemIt = map.entrySet().iterator();
        for ( int i = 0; itemIt.hasNext(); i++ ) {
            Map.Entry item = (Map.Entry) itemIt.next();
            String name = (String) item.getKey();
            ZipEntry zent = (ZipEntry) item.getValue(); 
            if ( zent == null ) {
                nodes[ i ] = new ZipBranchDataNode( zfile, level + name );
            }
            else if ( zent.isDirectory() ) {
                nodes[ i ] = new ZipBranchDataNode( zfile, zent );
            }
            else {
                nodes[ i ] = new ZipLeafDataNode( zfile, zent );
            }
        }
        return nodes;
    }

    public static boolean isMagic( byte[] magic ) {
        return (char) magic[ 0 ] == 'P'
            && (char) magic[ 1 ] == 'K'
            && magic[ 2 ] == 3
            && magic[ 3 ] == 4;
    }

}
