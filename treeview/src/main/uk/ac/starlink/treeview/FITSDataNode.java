package uk.ac.starlink.treeview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.*;
import nom.tam.fits.*;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.util.MappedFile;

/**
 * An implementation of the {@link DataNode} interface for
 * representing FITS files.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class FITSDataNode extends DefaultDataNode {
    private ArrayList childList;
    private boolean isIterating;
    private Icon icon;
    private String name;
    private String description;
    private List children;
    private BufferMaker bufmake;
    private FileChannel chan;

    /**
     * Initialises a <code>FITSDataNode</code> from a <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file 
     *               from which the node is to be created.
     */
    public FITSDataNode( File file ) throws NoSuchDataException {
        if ( ! checkCouldBeFITS( file ) ) {
            throw new NoSuchDataException( "Wrong magic number for FITS" );
        }
        MappedFile istrm = null;
        try {
            RandomAccessFile raf = new RandomAccessFile( file.getPath(), "r" );
            chan = raf.getChannel();
            if ( raf.length() > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException(
                    "File is too large"
                + " (" + raf.length() + " > " + Integer.MAX_VALUE + ")" );
            }
            bufmake = new BufferMaker( chan, 0, raf.length() );
            ByteBuffer niobuf = bufmake.makeBuffer();
            istrm = new MappedFile( niobuf );

            /* Try to make a header - if this throws an exception it's not
             * FITS. */
            Header primaryHeader = new Header( istrm );

            /* Characterise the file. */
            long headerSize = istrm.getFilePointer();
            long dataSize = getDataSize( primaryHeader );
            long fileSize = raf.length();
            if ( fileSize == headerSize + dataSize ) {
                long[] dims = ImageHDUDataNode
                             .getDimsFromHeader( primaryHeader );
                if ( dims.length == 0 ) {
                    description = "(header only)";
                }
                else {
                    description = NDShape.toString( dims );
                }
            }
            else if ( fileSize - headerSize - dataSize >= 2880 ) {
                // it's multi-extension
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( 
                "IO trouble while reading FITS header", e );
        }
        catch ( TruncatedFileException e ) {
            throw new NoSuchDataException( "File \"" + file 
                                         + "\" is not a FITS file" 
                                         + "(" + e.getMessage() + ")" );
        }
        finally {
            if ( istrm != null ) { 
                istrm.close();
            }
        }
 
        name = file.getName();
        setLabel( name );
    }




    /**
     * Initialises a <code>FITSDataNode</code> from a <code>String</code>
     * which gives the file name.
     *
     * @param  the absolute or relative path name of the name of a FITS file.
     */
    public FITSDataNode( String name ) throws NoSuchDataException {
        this( new File( name ) );
    }

    public String getDescription() {
        return description;
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() { 
        final MappedFile istrm;
        try {
            istrm = new MappedFile( bufmake.makeBuffer() );
        }
        catch ( IOException e ) {
            return Collections
                  .singletonList( getChildMaker().makeErrorDataNode( this, e ) )
                  .iterator();
        }
      
        return new Iterator() {
            private boolean broken = false;
            private int nchild = 0;
            public Object next() {
                if ( hasNext() ) {
                    long prepos = istrm.getFilePointer();
                    try {
                        Header hdr = new Header( istrm );
                        DataNode node = null;
                        if ( node == null ) {
                            try {
                                if ( AsciiTableHDU.isHeader( hdr ) ) {
                                    node = new TableHDUDataNode( hdr, istrm );
                                }
                            }
                            catch ( NullPointerException e ) {
                                // known NullPointerException crops up here
                            }
                        }
                        if ( node == null ) {
                            if ( BinaryTableHDU.isHeader( hdr ) ) {
                                node = new TableHDUDataNode( hdr, istrm );
                            }
                        }
                        if ( node == null ) {
                            if ( ImageHDU.isHeader( hdr ) ) {
                                BufferMaker bufmkr;
                                if ( chan != null ) {
                                    long postpos = istrm.getFilePointer();
                                    int bufsiz = (int) ( getRawSize( hdr ) 
                                                       + postpos - prepos );
                                    bufmkr = new BufferMaker( chan, prepos,
                                                              bufsiz );
                                }
                                else {
                                    bufmkr = null;
                                }
                                istrm.skip( getDataSize( hdr ) );
                                node = new ImageHDUDataNode( hdr, bufmkr );
                            }
                        }
                        if ( node == null ) {
                            node = new HDUDataNode( hdr );
                        }
                        node.setLabel( nchild == 0 ? "Primary HDU"
                                                   : ( "HDU " + nchild ) );
                        nchild++;
                        return node;
                    }
                    catch ( Exception e ) {
                        return new ErrorDataNode( e );
                    }
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public boolean hasNext() {
                return ! broken &&
                       istrm.getFilePointer() < ( bufmake.getSize() - 1 );
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.FITS );
        }
        return icon;
    }

    public String getPathSeparator() {
        return "#";
    }

    public String getNodeTLA() {
        return "FIT";
    }

    public String getNodeType() {
        return "FITS data";
    }

    public static boolean isMagic( byte[] magic ) {
        try {
            return new String( magic, "US-ASCII" ).startsWith( "SIMPLE  =" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError( "Of course it's supported" );
        }
    }

    /*
     * Throws a NoSuchDataException if this file isn't worth trying.
     * This is not required, but speeds up the DataNodeFactory's operation
     * a great deal.
     */
    private static boolean checkCouldBeFITS( File file ) {
        try {
            return ( isMagic( startBytes( file, 80 ) ) );
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /*
     * Utility function to find the number of bytes in the data segment
     * of an HDU.  As far as I can see, Header.getDataSize() ought to
     * do this, but it doesn't seem to.
     */
    private static long getDataSize( Header hdr ) {
        long nel = getRawSize( hdr );
        if ( nel % 2880 == 0 ) {
            return nel;
        }
        else {
            return ( ( nel / 2880 ) + 1 ) * 2880;
        }
    }

    private static long getRawSize( Header hdr ) {
        int naxis = hdr.getIntValue( "NAXIS", 0 );
        if ( naxis <= 0 ) {
            return 0;
        }
        int bitpix = hdr.getIntValue( "BITPIX" );
        long nel = Math.abs( bitpix ) / 8;
        for ( int i = 1; i <= naxis; i++ ) {
            nel *= hdr.getLongValue( "NAXIS" + i );
        }
        return nel;
    }
}
