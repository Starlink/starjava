package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
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
    private static IconFactory iconMaker = IconFactory.getInstance();

    private String name;
    private JPanel viewPanel;
    private File file;
    private File parentFile;
    private JComponent fullView;
    private static Map knowndirs = new HashMap();

    /**
     * Initialises a <code>FileDataNode</code> from a <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file from
     *               which the node is to be created
     */
    public FileDataNode( File file ) throws NoSuchDataException {
        this.file = file;
        if ( file.getPath().equals( "/" ) ) {
            name = "/";
        }
        else {
            name = file.getName();
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
    }

    /**
     * Initialises a <code>FileDataNode</code> from a <code>String</code>.
     *
     * @param  fileName  the absolute or relative name of the file from
     *                   which the node is to be created
     */
    public FileDataNode( String fileName ) throws NoSuchDataException {
        this( new File( fileName ) );
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
        return file.isDirectory();
    }

    public Iterator getChildIterator() {
        final DataNode parent = this;
        final File[] subFiles = file.listFiles();
        Arrays.sort( subFiles );
        return new Iterator() {
            private int index = 0;
            public boolean hasNext() {
                return index < subFiles.length;
            }
            public Object next() {
                DataNode child;
                try {
                    child = getChildMaker()
                           .makeDataNode( parent, subFiles[ index ] );
                }
                catch ( NoSuchDataException e ) {
                    child = getChildMaker().makeErrorDataNode( parent, e );
                }
                index++;
                return child;
            }
            public void remove() {
                throw new UnsupportedOperationException( "No remove" );
            }
        };
    }

    public boolean hasParentObject() {
        return parentFile != null;
    }

    public Object getParentObject() {
        return parentFile;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return iconMaker.getIcon( file.isDirectory() ? IconFactory.DIRECTORY
                                                     : IconFactory.FILE );
    }

    public String getPath() {
        return file.getPath();
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

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "Size", file.length() );
            dv.addKeyedItem( "Last modified", 
                              new Date( file.lastModified() ).toString() );
            dv.addKeyedItem( "Read access", file.canRead() ? "yes" : "no" );
            dv.addKeyedItem( "Write access", file.canWrite() ? "yes" : "no" );

            /* If it looks like a text file, add the option to view the
             * content. */
            try {
                DataSource datsrc = new FileDataSource( file );
                boolean isText = datsrc.isASCII();
                boolean isHTML = datsrc.isHTML();
                datsrc.close();

            //  HTML viewing does work but there are problems with it; 
            //  for one thing I can't make the HTML load asynchronously.
            //  If I do have HTML viewing, I'm not sure if it should be 
            //  here or (more likely) an HTMLDataNode.
            //  if ( isHTML ) {
            //      dv.addPane( "HTML view", new ComponentMaker() {
            //          public JComponent getComponent() throws IOException {
            //              return new HTMLViewer( file );
            //          }
            //      } );
            //  }
                if ( isText ) {
                    dv.addPane( "File text", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            return new TextViewer( new FileReader( file ) );
                        }
                    } );
                }
                else {
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
        return fullView;
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
