package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.*;
import javax.swing.*;
import nom.tam.fits.*;
import nom.tam.util.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.PermMap;
import uk.ac.starlink.ast.SkyFrame;
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
    private Cartesian shape;
    private Cartesian effectiveShape;
    private final String dataType;
    private String blank;
    private BufferMaker bufmaker;
    private Number badval;
    private FrameSet wcs;
    private FrameSet wcs2;
    private FrameSet wcsSlice;
    private double[] lower2;
    private double[] upper2;
    private String wcsEncoding;

    /**
     * Initialises an <code>ImageHDUDataNode</code> from a <code>Header</code> 
     * object.
     *
     * @param   hdr  a FITS header object
     *               from which the node is to be created.
     * @param   bufmaker  an object capable of constructing the NIO buffer
     *          containing the data on demand
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
        if ( axes != null && axes.length > 0 ) {
            shape = new Cartesian( axes );
            effectiveShape = effectiveShape( shape );
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
            case BasicHDU.BITPIX_LONG:
                dataType = "long";
                if ( hasBlank ) {
                    long val = hdr.getLongValue( "BLANK" );
                    blank = "" + val;
                    badval = new Long( val );
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
            default:
                dataType = null;
        }

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
            wcs2 = null;
            if ( aobj != null && aobj instanceof FrameSet ) {
                wcs = (FrameSet) aobj;

                /* See if there are any degenerate dimensions we can strip
                 * out to get a 2-d frame for display. */
                int ndim = shape.getNdim();
                if ( ndim > 2 ) {
                    try {
                        int baseNdim = wcs.getFrame( FrameSet.AST__BASE )
                                          .getNaxes();
                        double[] baseLo = new double[ baseNdim ];
                        double[] baseHi = new double[ baseNdim ];
                        for ( int i = 0; i < ndim; i++ ) {
                            baseLo[ i ] = 0.5;
                            baseHi[ i ] = shape.getCoords()[ i ] - 0.5;
                        }
                        wcs2 = significantAxes( wcs, baseLo, baseHi ); 
                        if ( wcs2.getFrame( FrameSet.AST__CURRENT )
                                 .getNaxes() == 2 ) {
                            lower2 = new double[] { baseLo[ 0 ], baseLo[ 1 ] };
                            upper2 = new double[] { baseHi[ 0 ], baseHi[ 1 ] };
                        }
                        else {
                            wcs2 = null;
                        }
                    }
                    catch ( AstException e ) {
                        wcs2 = null;
                    }
                    try {
                        wcsSlice = (FrameSet) wcs.copy();
                        int[] picker = new int[] { 1, 2 };
                        Mapping[] bmap = new Mapping[ 1 ];
                        Mapping[] cmap = new Mapping[ 1 ];
                        Frame bfrm = wcsSlice.getFrame( FrameSet.AST__BASE )
                                             .pickAxes( 2, picker, bmap );
                        Frame cfrm = wcsSlice.getFrame( FrameSet.AST__CURRENT )
                                             .pickAxes( 2, picker, cmap );
                        int icur = wcsSlice.getCurrent();
                        wcsSlice.addFrame( FrameSet.AST__BASE, 
                                           bmap[ 0 ], bfrm );
                        wcsSlice.setBase( FrameSet.AST__CURRENT );
                        wcsSlice.addFrame( icur, cmap[ 0 ], cfrm );
                    }
                    catch ( AstException e ) {
                        wcsSlice = null;
                    }
                }
            }
            else {
                wcsEncoding = null;
                wcs = null;
                wcs2 = null;
            }
        }
        catch ( AstException e ) {
            wcs = null;
            wcs2 = null;
        }

        description = "(" + hduType
                    + ( ( shape != null ) 
                         ? ( " " + shape.toString() + " " ) 
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
                wcschild = new DefaultDataNode( e );
            }
            return new DataNode[] { wcschild };
        }
    }

    public Icon getIcon() {
        if ( icon == null ) {
            if ( shape != null ) {
                icon = IconFactory.getInstance()
                                  .getArrayIcon( shape.getNdim() );
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
                dv.addKeyedItem( "Shape", shape.toString() );
            }
            if ( dataType != null ) {
                dv.addKeyedItem( "Pixel type", dataType ); 
            }
            if ( blank != null ) {
                dv.addKeyedItem( "Blank value", blank );
            }
            dv.addKeyedItem( "Number of header cards", 
                             Integer.toString( header.getNumberOfCards() ) );
            if ( wcs != null ) {
                dv.addSubHead( "World coordinate system" );
                dv.addKeyedItem( "Encoding", wcsEncoding );
                uk.ac.starlink.ast.Frame frm = 
                    wcs.getFrame( FrameSet.AST__CURRENT );
                dv.addKeyedItem( "Naxes", "" + frm.getNaxes() );
                if ( frm instanceof SkyFrame ) {
                    SkyFrame sfrm = (SkyFrame) frm;
                    dv.addKeyedItem( "Epoch", "" + sfrm.getEpoch() );
                    dv.addKeyedItem( "Equinox", "" + sfrm.getEquinox() );
                    dv.addKeyedItem( "Projection", "" + sfrm.getProjection() );
                    dv.addKeyedItem( "System", "" + sfrm.getSystem() );
                }
            }
            dv.addPane( "Header cards", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( header.iterator() );
                }
            } );
            if ( shape != null && bufmaker != null ) {
                final int ndim = shape.getNdim();
                final int endim = effectiveShape.getNdim();
                dv.addPane( "Array data", new ComponentMaker() {
                    public JComponent getComponent() throws IOException {
                        Buffer niobuf = typedBuffer( bufmaker.makeBuffer() );
                        Cartesian origin = new Cartesian( endim );
                        return new ArrayBrowser( niobuf, badval, 
                                                 origin, effectiveShape );
                    }
                } );
                if ( endim == 1 ) {
                    dv.addPane( "Graph view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException,
                                                                SplatException {
                            Buffer niobuf = 
                                typedBuffer( bufmaker.makeBuffer() );
                            return new SpectrumViewer( niobuf, 0L, name );
                        }
                    } );
                }
                if ( ndim == 2 && wcs != null ) {
                    dv.addPane( "WCS grid", new ComponentMaker() {
                        public JComponent getComponent() {
                            return new GridPlotter( 200, shape.getCoords(),
                                                    wcs );
                        }
                    } );
                }
                else if ( wcs2 != null ) {
                    dv.addPane( "WCS grid", new ComponentMaker() {
                        public JComponent getComponent() {
                            return new GridPlotter( 200, lower2, upper2, wcs2 );
                        }
                    } );
                }
                if ( ndim == 2 ) {
                    dv.addPane( "Image view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            Buffer niobuf = 
                                    typedBuffer( bufmaker.makeBuffer() );
                            return new ImageViewer( niobuf, shape, 
                                                    new long[] { 0L, 0L },
                                                    wcs );
                        }
                    } );
                }
                else if ( endim == 2 ) {
                    dv.addPane( "Image view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            Buffer niobuf = 
                                typedBuffer( bufmaker.makeBuffer() );
                            return new ImageViewer( niobuf, effectiveShape,
                                                    new long[] { 0L, 0L },
                                                    wcs2 );
                        }
                    } );
                }
                else if ( ndim > 2 ) {
                    dv.addPane( "Slice view", new ComponentMaker2() {
                        public JComponent[] getComponents() throws Exception {
                            Buffer niobuf =
                                typedBuffer( bufmaker.makeBuffer() );
                            long[] origin = new long[ shape.getNdim() ];
                            ComponentMaker2 cv = 
                                new CubeViewer( niobuf, shape, origin, 
                                                wcsSlice );
                            return cv.getComponents();
                        }
                    } );
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

    private static long[] getDimsFromHeader( Header hdr ) {
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

    private Buffer typedBuffer( ByteBuffer buf ) {
        if ( dataType == "byte" ) {
            return buf;
        }
        else if ( dataType == "short" ) {
            return buf.asShortBuffer();
        }
        else if ( dataType == "int" ) {
            return buf.asIntBuffer();
        }
        else if ( dataType == "long" ) {
            return buf.asLongBuffer();
        }
        else if ( dataType == "float" ) {
            return buf.asFloatBuffer();
        }
        else if ( dataType == "double" ) {
            return buf.asDoubleBuffer();
        }
        else {
            // assert false;
            return null; 
        }
    }

    /**
     * Gets the effective shape of this FITS array - one in which any
     * degenerate axes are ignored. 
     */
    private static Cartesian effectiveShape( Cartesian shape ) {
        int isig = 0;
        long[] coords = shape.getCoords();
        long[] coords1 = new long[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            if ( coords[ i ] > 1 ) {
                coords1[ isig++ ] = coords[ i ];
            }
        }
        long[] coords2 = new long[ isig ];
        System.arraycopy( coords1, 0, coords2, 0, isig );
        return new Cartesian( coords2 );
    }

    /**
     * Returns a new FrameSet based on an old one in which any significant
     * (non-degenerate) axes are removed.  A degenerate axis is one 
     * which corresponds to no width.
     * The base frame and current frame are both doctored in a similar 
     * fashion.
     * <p>
     * Most of the algorithm is nicked from the KAPLIBS routine KPG1_ASSIG.
     *
     * @param   fset    the basic frameset
     * @param   baseLo  the lower bounds of the base frame of fset.
     *                  On exit, the significant lower bounds will form the
     *                  first nsig few elements of the array, if there are 
     *                  nsig non-degenerate axes in the returned frameset
     *                  base frame
     * @param   baseHi  the upper bounds of the base frame of fset
     *                  On exit, the significant upper bounds will form the
     *                  first nsig few elements of the array, if there are 
     *                  nsig non-degenerate axes in the returned frameset
     *                  base frame
     * @return    a new FrameSet same as fset but containing only 
     *            the significant axes in the current and base frames.
     */
    private static FrameSet significantAxes( FrameSet fset,
                                             double[] baseLo, double[] baseHi ){

        /* Find the significant axes in the base frame. */
        int baseNdim = fset.getFrame( FrameSet.AST__BASE ).getNaxes();
        int baseNsig = 0;
        int baseNinsig = 0;
        int[] baseInprm = new int[ baseNdim ];
        int[] baseOutprm = new int[ baseNdim ];
        double[] baseAxval = new double[ baseNdim ];
        for ( int i = 0; i < baseNdim; i++ ) {
            if ( areSame( baseLo[ i ], baseHi[ i ] ) ) {
                baseNinsig++;
                baseInprm[ i ] = -baseNinsig;
                baseAxval[ baseNinsig - 1 ] = 0.5 * ( baseLo[ i ] 
                                                    + baseHi[ i ] );
            }
            else {
                baseNsig++;
                baseInprm[ i ] = baseNsig;
                baseOutprm[ baseNsig - 1 ] = i + 1;
            }
        }
        
        /* Find the significant axes in the current frame. */
        int currNdim = fset.getFrame( FrameSet.AST__CURRENT ).getNaxes();
        Mapping map = fset.getMapping( FrameSet.AST__BASE, 
                                       FrameSet.AST__CURRENT )
                          .simplify();
        int currNinsig = 0;
        int currNsig = 0;
        int[] currInprm = new int[ currNdim ];
        int[] currOutprm = new int[ currNdim ];
        double[] currAxval = new double[ currNdim ];
        for ( int i = 0; i < currNdim; i++ ) {
            double[] bounds = map.mapBox( baseLo, baseHi, true, i + 1, 
                                          null, null );
            double lo = bounds[ 0 ];
            double hi = bounds[ 1 ];
            if ( areSame( lo, hi ) ) {
                currNinsig++;
                currInprm[ i ] = -currNinsig;
                currAxval[ currNinsig - 1 ] = 0.5 * ( hi + lo );
            }
            else {
                currNsig++;
                currInprm[ i ] = currNsig;
                currOutprm[ currNsig - 1 ] = i + 1;
            }
        }


        FrameSet fset2 = (FrameSet) fset.copy();
        if ( baseNinsig > 0 && baseNinsig == currNinsig ) {

            /* Add a new base frame */
            Mapping basePmap = new PermMap( baseNdim, baseInprm, baseNsig,
                                            baseOutprm, baseAxval );
            uk.ac.starlink.ast.Frame basePfrm =
                fset2.getFrame( FrameSet.AST__BASE )
                     .pickAxes( baseNsig, baseOutprm, null );
            int ifcurr = fset2.getCurrent();
            fset2.addFrame( FrameSet.AST__BASE, basePmap, basePfrm );
            fset2.setBase( FrameSet.AST__CURRENT );
            fset2.setCurrent( ifcurr );

            /* Add a new current frame. */
            Mapping currPmap = new PermMap( currNdim, currInprm, currNsig, 
                                            currOutprm, currAxval );
            uk.ac.starlink.ast.Frame currPfrm = 
                fset2.getFrame( FrameSet.AST__CURRENT )
                     .pickAxes( currNsig, currOutprm, null );
            fset2.addFrame( FrameSet.AST__CURRENT, currPmap, currPfrm );

            /* Set the output base frame bounds. */
            double[] oldLo = (double[]) baseLo.clone();
            double[] oldHi = (double[]) baseHi.clone();
            for ( int i = 0; i < baseNsig; i++ ) {
                baseLo[ i ] = oldLo[ baseOutprm[ i ] - 1 ];
                baseHi[ i ] = oldHi[ baseOutprm[ i ] - 1 ];
            }
        }

        return fset2;
    }

    private static final double EPS = 100.0 * Double.MIN_VALUE;

    private static boolean areSame( double a, double b ) {
        return Math.abs( a - b ) 
             < EPS * Math.max( Math.abs( a ), Math.abs( b ) );
    }

  

}
