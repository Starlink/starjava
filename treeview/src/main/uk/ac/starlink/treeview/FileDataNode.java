package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * A {@link DataNode} representing a file or directory in the 
 * Unix file system.  If the <code>FileDataNode</code> represents a 
 * directory, then its children are the files which it contains.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class FileDataNode extends DefaultDataNode {

    private String name;
    private JPanel viewPanel;
    private File file;
    private File parentFile;
    private static boolean showHidden = false;
    private static Map knowndirs = new HashMap();

    /**
     * Initialises a <code>FileDataNode</code> from a <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file from
     *               which the node is to be created
     */
    public FileDataNode( File file ) throws NoSuchDataException {
        this.file = file;
        name = file.getName();
        if ( name.length() == 0 ) {
            name = file.getAbsolutePath(); // cope with root directory
        }
        setLabel( name );
        if ( ! existsInDirectory( file ) ) {
            throw new NoSuchDataException( "No such file " + file );
        }
        try {
            this.parentFile = file.getCanonicalFile().getParentFile();
        }
        catch ( IOException e ) {
            this.parentFile = null;
        }
        setIconID( file.isDirectory() ? IconFactory.DIRECTORY 
                                      : IconFactory.FILE );
    }

    /**
     * Initialises a <code>FileDataNode</code> from a top-level HDSObject.
     *
     * @param  hobj  an HDSObject at the top of its container file
     * @throws  NoSuchDataException  if <tt>hobj</tt> is not at top level
     */
    public FileDataNode( HDSObject hobj ) throws NoSuchDataException {
        this( getTopLevelFile( hobj ) );
    }

    public boolean allowsChildren() {
        return file.isDirectory() && file.canRead();
    }

    public Iterator getChildIterator() {
        File[] subFiles = file.listFiles();
        List files = Arrays.asList( file.listFiles() );
        if ( ! showHidden ) {
            files = new ArrayList( files );
            for ( Iterator it = files.iterator(); it.hasNext(); ) {
                if ( ((File) it.next()).isHidden() ) {
                    it.remove();
                }
            }
        }
        Collections.sort( files );
        final Iterator it = files.iterator();
        return new Iterator() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                File file = (File) it.next();
                DataNode child = makeChild( file );
                child.setLabel( file.getName() );
                return child;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Object getParentObject() {
        return parentFile;
    }

    public String getName() {
        return name;
    }

    public String getPathElement() {
        return name;
    }

    public String getPathSeparator() {
        return File.separator;
    }

    /**
     * Returns either "DIR" or "FILE" for a directory or non-directory file
     * respectively.
     *
     * @return  a short description of the file type
     */
    public String getNodeTLA() {
        return file.isDirectory() ? "DIR" : "FILE";
    }

    public String getNodeType() {
        return file.isDirectory() ? "Directory" : "File";
    }

    /**
     * Determines whether hidden files are included in the list of 
     * children of a directory file node.
     *
     * @param  showHidden  <tt>true</tt> iff you want hidden files to
     *         be included in the child list
     */
    public static void setShowHidden( boolean showHidden ) {
        FileDataNode.showHidden = showHidden;
    }

    /*
     * Determine whether a directory entry of this name exists.   The 
     * exists() method of File is not suitable since it returns false
     * for a symbolic link with no referent.  The only way I can think
     * of to tell if the file exists in this sense is to list the files
     * in the parent directory of the referent and see if the referent
     * appears in it.  So that checking every file in a large directory 
     * does not keep having to list all the files in it, we maintain 
     * a static Map of all directories searched in this way, and the
     * files known to exist in each.
     * 
     * This whole thing is a bit naughty, since I am relying on the
     * implementation-dependent fact that getAbsoluteFile returns the
     * referent of a symlink, but if I don't do it, there will be
     * confusion.
     */
    private static boolean existsInDirectory( File file ) {
        boolean here;
        if ( file.isDirectory() ) {
            here = file.exists();
        }
        else {
            here = false;
            Set knownfiles = null;
            File parent = file.getAbsoluteFile().getParentFile();
            String dirkey = parent.toString();
            if ( knowndirs.containsKey( dirkey ) ) {
                knownfiles = (Set) knowndirs.get( dirkey );
                here = knownfiles.contains( file.getName().intern() );
            }
            if ( ! here ) {
                String[] flist = parent.list();
                if ( flist == null ) {
                    return false;
                }
                knownfiles = new HashSet( flist.length );
                for ( int i = 0; i < flist.length; i++ ) {
                    knownfiles.add( flist[ i ] );
                }
                knowndirs.put( dirkey, knownfiles );
            }
            here = knownfiles.contains( file.getName().intern() );
        }
        return here;
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Size", file.length() );
        dv.addKeyedItem( "Last modified", 
                          new Date( file.lastModified() ).toString() );
        dv.addKeyedItem( "Read access", file.canRead() ? "yes" : "no" );
        dv.addKeyedItem( "Write access", file.canWrite() ? "yes" : "no" );
        dv.addKeyedItem( "Absolute path", file.getAbsolutePath() );

        /* If it's a directory, comment on the files it contains. */
        File[] entries = file.listFiles();
        if ( entries != null ) {
            dv.addSeparator();
            dv.addKeyedItem( "Number of files", entries.length );
        }

        /* If it looks like a text file, add the option to view the
         * content. */
        if ( file.canRead() && ! file.isDirectory() ) {
            try {

                /* See if it looks like ASCII. */
                InputStream strm = new FileInputStream( file );
                int nTest = 512;
                byte[] buf = new byte[ nTest ];
                int nGot = strm.read( buf );
                strm.close();
                if ( nGot > 0 ) {
                    if ( nGot < nTest ) {
                        byte[] buf1 = new byte[ nGot ];
                        System.arraycopy( buf, 0, buf1, 0, nGot );
                        buf = buf1;
                    }
                    byte[] buf1 = new byte[ nGot ];
                    boolean isText = TreeviewUtil.isASCII( buf );

                //  HTML viewing does work but there are problems with it; 
                //  for one thing I can't make the HTML load asynchronously.
                //  If I do have HTML viewing, I'm not sure if it should be 
                //  here or (more likely) an HTMLDataNode.
                //  if ( datsrc.isHTML() ) {
                //      dv.addPane( "HTML view", new ComponentMaker() {
                //          public JComponent getComponent()
                //                  throws IOException {
                //              return new HTMLViewer( file );
                //          }
                //      } );
                //  }
                    if ( isText ) {
                        dv.addPane( "File text", new ComponentMaker() {
                            public JComponent getComponent()
                                    throws IOException {
                                return new TextViewer( new FileReader( file ) );
                            }
                        } );
                    }
                    dv.addPane( "Hex dump", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            RandomAccessFile raf =
                                    new RandomAccessFile( file, "r" );
                            return new HexDumper( raf );
                        }
                    } );
                }
            }
            catch ( final IOException e ) {
                dv.addPane( "Error reading file", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new TextViewer( e );
                    }
                } );
            }
        }
    }

    /**
     * Gets the container file in which a given HDSObject is the 
     * top level item.
     *
     * @param  hobj  an HDSObject at the top of its container file
     * @throws  NoSuchDataException  if <tt>hobj</tt> is not at top level
     */
    private static File getTopLevelFile( HDSObject hobj )
            throws NoSuchDataException {

        /* Get the container file name and path. */
        String[] trace = new String[ 2 ];
        int level; 
        try {
            level = hobj.hdsTrace( trace );
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e );
        }

        /* See if there is a parent. */
        if ( level > 1 ) {
            throw new NoSuchDataException( 
                          "HDSObject is not at the top of container file" );
        }
        return new File( trace[ 1 ] );
    }
}
