package uk.ac.starlink.treeview;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.SkyFrame;
import uk.ac.starlink.fits.FitsArrayBuilder;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.MappedFile;
import uk.ac.starlink.splat.util.SplatException;

/**
 * An implementation of the {@link DataNode} interface for
 * representing Header and Data Units (HDUs) in FITS files.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ImageHDUDataNode extends HDUDataNode {
    private Icon icon;
    private String name;
    private String description;
    private String hduType;
    private JComponent fullview;
    private Header header;
    private NDShape shape;
    private final String dataType;
    private String blank;
    private BufferMaker bufmaker;
    private Number badval;
    private FrameSet wcs;
    private String wcsEncoding;

    /**
     * Initialises an <code>ImageHDUDataNode</code> from a <code>Header</code> 
     * object.
     *
     * @param   hdr  a FITS header object
     *               from which the node is to be created.
     * @param   bufmaker  an object capable of constructing the NIO buffer
     *          containing the header+data on demand
     */
    public ImageHDUDataNode( Header hdr, BufferMaker bufmaker )
            throws NoSuchDataException {
        super( hdr );

        this.header = hdr;
        this.bufmaker = bufmaker;
        hduType = getHduType();
        if ( hduType != "Image" ) {
            throw new NoSuchDataException( "Not an Image HDU" );
        }

        long[] axes = getDimsFromHeader( hdr );
        int ndim = axes.length;
        if ( axes != null && ndim > 0 ) {
            shape = new NDShape( axes );
        }

        boolean hasBlank = hdr.containsKey( "BLANK" );
        badval = null;
        blank = "<none>";
        switch ( hdr.getIntValue( "BITPIX" ) ) {
            case BasicHDU.BITPIX_BYTE:
                dataType = "byte";
                if ( hasBlank ) {
                    int val = hdr.getIntValue( "BLANK" );
                    blank = "" + val;
                    badval = new Byte( (byte) val );
                }
                break;
            case BasicHDU.BITPIX_SHORT:
                dataType = "short";
                if ( hasBlank ) {
                    int val = hdr.getIntValue( "BLANK" );
                    blank = "" + val;
                    badval = new Short( (short) val );
                }
                break;
            case BasicHDU.BITPIX_INT:
                dataType = "int";
                if ( hasBlank ) {
                    int val = hdr.getIntValue( "BLANK" );
                    blank = "" + val;
                    badval = new Integer( val );
                }
                break;
            case BasicHDU.BITPIX_FLOAT:
                dataType = "float";
                blank = null;
                break;
            case BasicHDU.BITPIX_DOUBLE:
                dataType = "double";
                blank = null;
                break;
            case BasicHDU.BITPIX_LONG:
                throw new NoSuchDataException(
                    "64-bit integers not supported by FITS" );
            default:
                dataType = null;
        }

        if ( Driver.hasAST ) {
            try {
                final Iterator hdrIt = hdr.iterator();
                Iterator lineIt = new Iterator() {
                    public boolean hasNext() {
                        return hdrIt.hasNext();
                    }
                    public Object next() {
                        return hdrIt.next().toString();
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
                FitsChan chan = new FitsChan( lineIt );
                wcsEncoding = chan.getEncoding();
                AstObject aobj = chan.read();
                if ( aobj != null && aobj instanceof FrameSet ) {
                    wcs = (FrameSet) aobj;
                }
                else {
                    wcsEncoding = null;
                    wcs = null;
                }
            }
            catch ( AstException e ) {
                wcsEncoding = null;
                wcs = null;
            }
        }
        else {
            wcsEncoding = null;
            wcs = null;
        }

        description = "(" + hduType
                    + ( ( shape != null ) 
                         ? ( " " + NDShape.toString( shape.getDims() ) + " " ) 
                         : "" )
                    + ")";
    }

    public boolean allowsChildren() {
        // return false;
        return wcs != null;
    }

    public DataNode[] getChildren() {
        if ( wcs == null ) {
        // if ( true ) {
            return new DataNode[ 0 ];
        }
        else {
            DataNode wcschild;
            try {
                wcschild = new WCSDataNode( wcs );
            }
            catch ( NoSuchDataException e ) {
                wcschild = new ErrorDataNode( e );
            }
            return new DataNode[] { wcschild };
        }
    }

    public Icon getIcon() {
        if ( icon == null ) {
            if ( shape != null ) {
                icon = IconFactory.getInstance()
                                  .getArrayIcon( shape.getNumDims() );
            }
            else {
                icon = IconFactory.getInstance()
                                  .getIcon( IconFactory.HDU );
            }
        }
        return icon;
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullview == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullview = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "HDU type", hduType );
            if ( shape != null ) {
                dv.addKeyedItem( "Shape", NDShape.toString( shape.getDims() ) );
            }
            if ( dataType != null ) {
                dv.addKeyedItem( "Pixel type", dataType ); 
            }
            if ( blank != null ) {
                dv.addKeyedItem( "Blank value", blank );
            }
            dv.addSeparator();
            dv.addKeyedItem( "Number of header cards", 
                             header.getNumberOfCards() );
            dv.addKeyedItem( "Blocks in header", header.getSize() / 2880 );
            dv.addKeyedItem( "Blocks of data", 
                             FitsConstants.getDataSize( header ) / 2880 );

            if ( wcs != null ) {
                dv.addSubHead( "World coordinate system" );
                dv.addKeyedItem( "Encoding", wcsEncoding );
                uk.ac.starlink.ast.Frame frm = 
                    wcs.getFrame( FrameSet.AST__CURRENT );
                dv.addKeyedItem( "Naxes", frm.getNaxes() );
                if ( frm instanceof SkyFrame ) {
                    SkyFrame sfrm = (SkyFrame) frm;
                    dv.addKeyedItem( "Epoch", sfrm.getEpoch() );
                    dv.addKeyedItem( "Equinox", sfrm.getEquinox() );
                    dv.addKeyedItem( "Projection", sfrm.getProjection() );
                    dv.addKeyedItem( "System", sfrm.getSystem() );
                }
            }
            dv.addPane( "Header cards", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( header.iterator() );
                }
            } );
            if ( shape != null && bufmaker != null ) {
                try {
                    ByteBuffer bbuf = bufmaker.makeBuffer();
                    MappedFile mf = new MappedFile( bbuf );
                    NDArray nda = FitsArrayBuilder.getInstance()
                                 .makeNDArray( mf, AccessMode.READ );
                    if ( ! nda.getShape().equals( shape ) ) {
                        nda = new BridgeNDArray( 
                            new MouldArrayImpl( nda, shape ) );
                    }
                    NDArrayDataNode.addDataViews( dv, nda, wcs );
                }
                catch ( IOException e ) {
                    dv.logError( e );
                }
            }
        }
        return fullview;
    }

    public String getDescription() {
        return description;
    }

    public String getNodeTLA() {
        return "IMG";
    }

    public String getNodeType() {
        return "FITS Image HDU";
    }

    static long[] getDimsFromHeader( Header hdr ) {
        try {
            int naxis = hdr.getIntValue( "NAXIS" );
            long[] dimensions = new long[ naxis ];
            for ( int i = 0; i < naxis; i++ ) {
                String key = "NAXIS" + ( i + 1 );
                if ( hdr.containsKey( key ) ) {
                    dimensions[ i ] =  hdr.getLongValue( key );
                }
                else {
                    throw new FitsException( "No header card + " + key );
                }
            }
            return dimensions;
        }
        catch ( Exception e ) {
            return null;
        }
    }

}
