package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import nom.tam.fits.Header;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TruncatedFileException;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedFile;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.oldfits.AbstractArrayDataIO;
import uk.ac.starlink.oldfits.FitsConstants;
import uk.ac.starlink.oldfits.MappedFile;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Abstract DataNode class for representing FITS objects.
 * Subclasses are provided for FITS objects on disk or in a stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class FITSDataNode extends DefaultDataNode {

    private String name;
    private String description;
    private DataSource datsrc;

    /**
     * Constructs a FITSDataNode from a data source.
     *
     * @param  datsrc  the source of the data
     */
    public FITSDataNode( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        ArrayDataInput istrm = null;
        try {

            /* Check magic number. */
            if ( ! isMagic( datsrc.getIntro() ) ) {
                throw new NoSuchDataException( "Wrong magic number for FITS" );
            }

            /* Try to make a header - if this throws an exception it's 
             * not FITS. */
            istrm = getDataInput();
            Header primaryHeader = new Header( istrm );

            /* Characterise the file if possible. */
            if ( istrm instanceof BufferedFile ||
                 istrm instanceof MappedFile ) {
                long headerSize;
                long fileSize;
                if ( istrm instanceof BufferedFile ) {
                    headerSize = ((BufferedFile) istrm).getFilePointer();
                    fileSize = ((BufferedFile) istrm).length();
                }
                else if ( istrm instanceof AbstractArrayDataIO &&
                          istrm instanceof RandomAccess ) {
                    headerSize = ((RandomAccess) istrm).getFilePointer();
                    fileSize = ((AbstractArrayDataIO) istrm).length();
                }
                else {
                    throw new AssertionError();
                }
                if ( headerSize != -1 ) {
                    long dataSize = FitsConstants.getDataSize( primaryHeader );
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
                }
            }
        }
        catch ( FitsException e ) {
            throw new NoSuchDataException( e );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        finally { 
            try {
                datsrc.close();
                if ( istrm != null ) {
                    istrm.close();
                }
            }
            catch ( IOException e ) {
                // too bad
            }
        }
        this.name = datsrc.getName();
        setLabel( name );
        setIconID( IconFactory.FITS );
        registerDataObject( DataType.DATA_SOURCE, datsrc );
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean allowsChildren() {
        return true;
    }

    /**
     * Returns an ArrayDataMaker encapsulating the same input stream as the
     * on one which this FITSDataNode is based, but starting at a given
     * offset <tt>start</tt> into the stream and <tt>size</tt> bytes long.
     *
     * @param  start  the offset into this FITSDataNode's stream at which
     *         the returned source's streams should start
     * @param  size   the number of bytes contained by the returned
     *         source's stream
     */
    abstract protected ArrayDataMaker getArrayData( long start, long size );

    public Iterator getChildIterator() {
        final DataNode parent = this;
        final ArrayDataInput istrm;
        final Header firstHeader; 
        final int firstHeaderSize;
        try {
            istrm = getDataInput();
            firstHeader = new Header();
            firstHeaderSize = FitsConstants.readHeader( firstHeader, istrm );
        }
        catch ( TruncatedFileException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        return new Iterator() {
            private int nchild = 0;
            private boolean broken = false;
            private Header nextHeader = firstHeader;
            private int nextHeaderSize = firstHeaderSize;
            private long hduStart = 0L;

            public Object next() {
                DataNode dnode;
                if ( hasNext() ) {

                    /* Construct a new node from the header we have ready. */
                    try {
                        Header hdr = nextHeader;
                        int headsize = nextHeaderSize;
                        long datsize = FitsConstants.getDataSize( hdr );
                        long hdusize = headsize + datsize;
                        for ( long nskip = datsize; nskip > 0; ) {
                             int skipped = (int) istrm.skip( nskip );
                             nskip -= skipped;
                             if ( skipped == 0 ) {
                                 istrm.readByte();
                                 nskip--;
                             }
                        }
                        ArrayDataMaker hdudata = 
                            getArrayData( hduStart, hdusize );
                        hduStart += hdusize;
                        try {
                            if ( hdr.containsKey( "XTENSION" ) && 
                                 AsciiTableHDU.isHeader( hdr ) ) {
                                dnode = new TableHDUDataNode( hdr, hdudata );
                            }
                            else if ( BinaryTableHDU.isHeader( hdr ) ) {
                                dnode = new TableHDUDataNode( hdr, hdudata );
                            }
                            else if ( ImageHDU.isHeader( hdr ) ) {
                                dnode = new ImageHDUDataNode( hdr, hdudata );
                            }
                            else {
                                dnode = null;
                            }
                        }
                        catch ( NoSuchDataException e ) {
                            dnode = null;
                        }
                        if ( dnode == null ) {
                            dnode = new HDUDataNode( hdr, hdudata );
                        }
                        dnode.setLabel( ( nchild == 0 ) ? "Primary HDU"
                                                        : "HDU " + nchild );
                        if ( dnode instanceof HDUDataNode ) {
                            ((HDUDataNode) dnode).setHDUIndex( nchild );
                        }
                    }
                    catch ( NoSuchDataException e ) {
                        dnode = makeErrorChild( e );
                    }
                    catch ( IOException e ) {
                        dnode = makeErrorChild( e );
                    }

                    /* Remember parentage of the new node. */
                    getChildMaker().configureDataNode( dnode, parent, null );

                    /* Read the next header. */
                    nchild++;
                    try {
                        nextHeader = new Header();
                        nextHeaderSize = 
                            FitsConstants.readHeader( nextHeader, istrm );
                    }
                    catch ( IOException e ) {
                        nextHeader = null;
                    }
                    catch ( TruncatedFileException e ) {
                        nextHeader = null;
                    }
                    if ( nextHeader == null ) {
                        try {
                            istrm.close();
                        }
                        catch ( IOException e ) {
                            // no action
                        }
                    }

                    /* Return the new datanode. */
                    return dnode;
                }
                else {
                    throw new NoSuchElementException();
                }
            }

            /* If we have a header ready, we can produce another child. */
            public boolean hasNext() {
                return nextHeader != null;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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

    /**
     * Returns an ArrayDataInput object containing the data from this 
     * FITS object.
     */
    protected ArrayDataInput getDataInput() throws IOException {
        if ( datsrc instanceof FileDataSource && 
             datsrc.getCompression() == Compression.NONE ) {
            String fileName = ((FileDataSource) datsrc).getFile().getPath();
            return new MappedFile( fileName );
        }
        else {
            return new BufferedDataInputStream( datsrc.getInputStream() );
        }
    }

    /**
     * Indicates whether a buffer of bytes looks like the start of a FITS
     * file.
     *
     * @param    buffer   the first few bytes of a potential stream
     * @return   true if <tt>buffer</tt> matches the FITS file magic number
     */
    public static boolean isMagic( byte[] buffer ) {
        return FitsConstants.isMagic( buffer );
    }

    /**
     * Interface used for objects which can supply an ArrayDataInput 
     * object on demand (more than once if necessary).
     */
    public static interface ArrayDataMaker {

        /**
         * Returns an ArrayDataInput object which can supply the array data.
         */
        ArrayDataInput getArrayData() throws IOException;

        /**
         * Returns a DataSource which can supply the input stream again.
         */
        DataSource getDataSource();

        /**
         * Returns the offset into the datasource's input stream at which
         * the ArrayDataInput data starts.
         */
        long getOffset();
    }

}
