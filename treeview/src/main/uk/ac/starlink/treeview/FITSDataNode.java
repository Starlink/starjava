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
    private List children;
    private BufferMaker bufmake;
    private FileChannel chan;

    public static String[] FITSExtensions = new String[] {
        ".fit", ".dst", ".fits", ".fts", ".lilo", ".lihi", ".silo", 
        ".sihi", ".mxlo", ".mxhi", ".rilo", ".rihi", ".vdlo", ".vdhi", 
    };

    /**
     * Initialises a <code>FITSDataNode</code> from a <code>File</code> object.
     *
     * @param  file  a <code>File</code> object representing the file 
     *               from which the node is to be created.
     */
    public FITSDataNode( File file ) throws NoSuchDataException {
        checkCouldBeFITS( file );
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
            ArrayDataInput istrm = new MappedFile( niobuf );
            Header primaryHeader = new Header( istrm );
        }
        catch ( Exception e ) {
            throw new NoSuchDataException( "File \"" + file 
                                         + "\" is not a FITS file" 
                                         + "(" + e.getMessage() + ")" );
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
                  .singletonList( new DefaultDataNode( e ) )
                  .iterator();
        }
      
        return new Iterator() {
            private boolean broken = false;
            private int nchild = 0;
            public Object next() {
                if ( hasNext() ) {
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
                                    bufmkr = 
                                        new BufferMaker( chan,
                                                         istrm.getFilePointer(),
                                                         getRawSize( hdr ) );
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
                        node.setLabel( nchild++ == 0 ? "Primary HDU"
                                                     : "HDU " + nchild );
                        return node;
                    }
                    catch ( Exception e ) {
                        return new DefaultDataNode( e );
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

    public String getNodeTLA() {
        return "FIT";
    }

    public String getNodeType() {
        return "FITS data";
    }

    /*
     * Throws a NoSuchDataException if this file isn't worth trying.
     * This is not required, but speeds up the DataNodeFactory's operation
     * a great deal.
     */
    private void checkCouldBeFITS( File file ) throws NoSuchDataException {
        String fname = file.getName().toLowerCase();
        for ( int i = 0; i < FITSExtensions.length; i++ ) {
            if ( fname.endsWith( FITSExtensions[ i ] ) ) {
                return;
            }
        }
        throw new NoSuchDataException( "Wrong extension for a FITS file" );
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
