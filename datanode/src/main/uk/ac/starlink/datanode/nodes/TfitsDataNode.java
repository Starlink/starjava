package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import uk.ac.starlink.fits.AsciiTableStarTable;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.FitsHeader;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.fits.InputFactory;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * DataNode class for representing FITS objects.
 * This does not use nom.tam.fits; it understands BINTABLE and TABLE
 * extensions, but not array-type HDUs.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2022
 */
public class TfitsDataNode extends DefaultDataNode {

    private String name;
    private String description;
    private DataSource datsrc;

    /**
     * Constructor.
     *
     * @param  datsrc  the source of the data
     */
    public TfitsDataNode( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        boolean isMagic;
        try {
            isMagic = isMagic( datsrc.getIntro() );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "Can't read intro" );
        }
        if ( ! isMagic ) {
            throw new NoSuchDataException( "Wrong magic number for FITS" );
        }
        InputStream in = null;
        try {
            in = datsrc.getInputStream();
            FitsUtil.readHeader( in );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        finally {
            if ( in != null ) {
                try {
                    in.close();
                }
                catch ( IOException e ) {
                }
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

    public Iterator getChildIterator() {
        final DataNode parent = this;
        final InputStream in;
        final FitsHeader firstHeader;
        try {
            in = datsrc.getInputStream();
            firstHeader = FitsUtil.readHeader( in );
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        final long firstHeaderSize = firstHeader.getHeaderByteCount();
        return new Iterator() {
            private int nchild = 0;
            private boolean broken = false;
            private FitsHeader nextHeader = firstHeader;
            private long nextHeaderSize = firstHeaderSize;
            private long hduStart = 0L;

            public Object next() {
                if ( hasNext() ) {

                    FitsHeader hdr = nextHeader;
                    long headsize = nextHeaderSize;
                    long datsize; 
                    long hdusize;
                    DataNode dnode;
                    try {
                        datsize = hdr.getDataByteCount();
                        hdusize = headsize + datsize;
                        long datStart = hduStart + headsize;
                        IOUtils.skip( in, datsize );
                        InputFactory inFact =
                            InputFactory
                           .createFactory( datsrc, datStart, datsize );
                        hduStart += hdusize;
                        String xtension = hdr.getStringValue( "XTENSION" );
                        StarTable table;
                        if ( "TABLE".equals( xtension ) ) {
                            table = AsciiTableStarTable
                                   .createTable( hdr, inFact );
                        }
                        else if ( "BINTABLE".equals( xtension ) ) {
                            table = BintableStarTable
                                   .createTable( hdr, inFact, WideFits.DEFAULT);
                        }
                        else {
                            table = null;
                        }
                        dnode = new TfitsHduDataNode( hdr, nchild, table );
                    }
                    catch ( Exception e ) {
                        dnode = makeErrorChild( e );
                    }

                    /* Remember parentage of the new node. */
                    getChildMaker().configureDataNode( dnode, parent, null );

                    /* Read the next header. */
                    nchild++;
                    try {
                        nextHeader = FitsUtil.readHeader( in );
                        nextHeaderSize = nextHeader.getHeaderByteCount();
                    }
                    catch ( IOException e ) {
                        nextHeader = null;
                        try {
                            in.close();
                        }
                        catch ( IOException e1 ) {
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
     * Indicates whether a buffer of bytes looks like the start of a FITS
     * file.
     *
     * @param    buffer   the first few bytes of a potential stream
     * @return   true if <code>buffer</code> matches the FITS file magic number
     */
    public static boolean isMagic( byte[] buffer ) {
        return FitsUtil.isMagic( buffer );
    }
}
