package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
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
import uk.ac.starlink.oldfits.FitsArrayBuilder;
import uk.ac.starlink.oldfits.FitsConstants;
import uk.ac.starlink.oldfits.MappedFile;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.Ndx;

/**
 * An implementation of the {@link DataNode} interface for
 * representing Header and Data Units (HDUs) in FITS files.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ImageHDUDataNode extends HDUDataNode {
    private String name;
    private String description;
    private String hduType;
    private Header header;
    private NDShape shape;
    private final String dataType;
    private String blank;
    private FITSFileDataNode.ArrayDataMaker hdudata;
    private Number badval;
    private FrameSet wcs;
    private String wcsEncoding;
    private Ndx ndx;

    /**
     * Initialises an <code>ImageHDUDataNode</code> from a <code>Header</code> 
     * object.
     *
     * @param   hdr  a FITS header object
     *               from which the node is to be created.
     * @param   hdudata  an object capable of returning the array data for
     *                   the image
     */
    public ImageHDUDataNode( Header hdr, FITSDataNode.ArrayDataMaker hdudata )
            throws NoSuchDataException {
        super( hdr, hdudata );

        this.header = hdr;
        this.hdudata = hdudata;
        hduType = getHduType();
        if ( hduType != "Image" ) {
            throw new NoSuchDataException( "Not an Image HDU" );
        }

        long[] axes = getDimsFromHeader( hdr );
        int ndim = axes.length;
        boolean ok = axes != null && ndim > 0;
        if ( ok ) {
            for ( int i = 0; i < ndim; i++ ) {
                ok = ok && axes[ i ] > 0;
            }
        }
        if ( ok ) {
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
                    badval = Byte.valueOf( (byte) val );
                }
                break;
            case BasicHDU.BITPIX_SHORT:
                dataType = "short";
                if ( hasBlank ) {
                    int val = hdr.getIntValue( "BLANK" );
                    blank = "" + val;
                    badval = Short.valueOf( (short) val );
                }
                break;
            case BasicHDU.BITPIX_INT:
                dataType = "int";
                if ( hasBlank ) {
                    int val = hdr.getIntValue( "BLANK" );
                    blank = "" + val;
                    badval = Integer.valueOf( val );
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

        if ( NodeUtil.hasAST() ) {
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

        /* Set the icon based on the shape of the image. */
        short iconID;
        if ( shape == null ) {
            iconID = IconFactory.HDU;
        }
        else {
            int nd = shape.getNumPixels() == 1 ? 0 : shape.getNumDims();
            iconID = IconFactory.getArrayIconID( nd );
        }
        setIconID( iconID );
    }

    public boolean allowsChildren() {
        // return false;
        return wcs != null;
    }

    public Iterator getChildIterator() {
        List children = new ArrayList();
        if ( wcs != null ) {
            children.add( makeChild( wcs ) );
        }
        return children.iterator();
    }

    public void configureDetail( DetailViewer dv ) {
        super.configureDetail( dv );
        dv.addSeparator();
        if ( shape != null ) {
            dv.addKeyedItem( "Shape", NDShape.toString( shape.getDims() ) );
        }
        if ( dataType != null ) {
            dv.addKeyedItem( "Pixel type", dataType ); 
        }
        if ( blank != null ) {
            dv.addKeyedItem( "Blank value", blank );
        }

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

    public boolean hasDataObject( DataType dtype ) {
        if ( dtype == DataType.NDX ) {
            return shape != null;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        if ( hasDataObject( dtype ) && dtype == DataType.NDX ) {
            try {
                return getNdx();
            }
            catch ( IOException e ) {
                throw new DataObjectException( e );
            }
        }
        else {
            return super.getDataObject( dtype );
        }
    }

    private synchronized Ndx getNdx() throws IOException {
        if ( ndx == null ) {
            ArrayDataInput data = hdudata.getArrayData();
            NDArray nda = FitsArrayBuilder.getInstance()
                         .makeNDArray( data, AccessMode.READ );
            if ( ! nda.getShape().equals( shape ) ) {
                nda = new BridgeNDArray( new MouldArrayImpl( nda, shape ) );
            }
            ndx = new DefaultMutableNdx( nda );
            if ( wcs != null ) {
                ((DefaultMutableNdx) ndx).setWCS( wcs );
            }
        }
        return ndx;
    }
}
