package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ast.CmpMap;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;

/**
 * A {@link DataNode} representing an
 * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun33.htx/sun33.html">NDF</a>
 * object.
 * <p>
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class NDFDataNode extends HDSDataNode implements Draggable {

    private HDSObject ndfobj;
    private NDShape shape;
    private FrameSet wcs;
    private JComponent fullView;
    private DataNodeFactory axisChildMaker;
    private DataNodeFactory ndfChildMaker;
    private Ndx ndx;

    private static final int GRID_FRAME = 1;
    private static final int PIXEL_FRAME = 2;
    private static final int AXIS_FRAME = 3;

    // NDF components
    private String name;
    private String title;
    private String label;
    private String units;
    private ArrayStructure dataArray;
    private ArrayStructure varianceArray;
    private ArrayStructure qualityArray;
    private byte qualityBadbits;
    private WCSDataNode wcsComponent;
    private HistoryDataNode historyComponent;
    private HDSObject axes;
    private HDSObject extensions;


    /**
     * Constructs an NDFDataNode from an HDSObject.
     */
    public NDFDataNode( HDSObject hobj ) throws NoSuchDataException {
        super( hobj );
        ndfobj = hobj;

        try {
            /* Look for a DATA array - without it this is not an NDF. */
            dataArray = getArrayComponent( "DATA_ARRAY" );
            if ( dataArray == null ) {
                throw new NoSuchDataException( 
                    "This has no DATA_ARRAY component" );
            }
            name = ndfobj.datName();
            setLabel( name );
            shape = dataArray.getShape();

            /* Get the other known array components, checking shapes
             * as we go. */
            varianceArray = getArrayComponent( "VARIANCE" );
            if ( varianceArray != null && 
                 ! varianceArray.getShape().equals( shape ) ) {
                throw new NoSuchDataException( 
                    "DATA and VARIANCE components have different shapes" );
            }

            if ( ndfobj.datThere( "QUALITY" ) ) {
                try {
                    HDSObject qobj = ndfobj.datFind( "QUALITY" );
                    qualityArray =
                        new ArrayStructure( qobj.datFind( "QUALITY" ) );
                    if ( qobj.datThere( "BADBITS" ) ) {
                        HDSObject qbb = qobj.datFind( "BADBITS" );
                        qualityBadbits = (byte) qbb.datGet0i();
                    }
                }
                catch ( Exception e ) {
                }
            }
            if ( qualityArray != null &&
                 ! qualityArray.getShape().equals( shape ) ) {
                throw new NoSuchDataException(
                    "DATA and QUALITY components have different shapes" );
            }

            /* If we have got this far, we'll call it a legitimate NDF. */

            /* Get known character components. */
            title = getCharacterComponent( "TITLE" );
            label = getCharacterComponent( "LABEL" );
            units = getCharacterComponent( "UNITS" );

            /* Try to find an AXIS component that looks right. */
            if ( ndfobj.datThere( "AXIS"  ) ) {
                HDSObject ax = ndfobj.datFind( "AXIS" );
                if ( ax.datShape().length == 1 ) {
                    axes = ax;
                }
                else { 
                    axes = null;
                }
            }

            /* Try to find a WCS component that looks right. */
            if ( TreeviewUtil.hasAST() && ndfobj.datThere( "WCS" ) ) {
                try {
                    DataNode wcsnode = makeNDFChild( ndfobj.datFind( "WCS" ) );
                    if ( wcsnode instanceof WCSDataNode ) {
                        wcsComponent = (WCSDataNode) wcsnode;
                    }
                    else {
                        throw new NoSuchDataException( "Not a WCSDataNode" );
                    }

                    /* The WCS component stored in the HDSObject requires some
                     * doctoring, since its PIXEL and AXIS Frames are not
                     * trustworthy (under normal circumstances these would be
                     * ignored and regenerated during a call to NDF_GTWCS). */
                    wcs = wcsComponent.getWcs();

                    /* Remap a PIXEL Frame correctly using the GRID Frame 
                     * and the origin offset. */
                    int ndim = shape.getNumDims();
                    double[] ina = new double[ ndim ];
                    double[] inb = new double[ ndim ];
                    double[] outa = new double[ ndim ];
                    double[] outb = new double[ ndim ];
                    long[] origin = shape.getOrigin();
                    for ( int i = 0; i < ndim; i++ ) {
                        ina[ i ] = 0.0;
                        inb[ i ] = 1.0;
                        outa[ i ] = ina[ i ] + origin[ i ] - 1.5;
                        outb[ i ] = inb[ i ] + origin[ i ] - 1.5;
                    }
                    Mapping pmap = 
                        new CmpMap( wcs.getMapping( PIXEL_FRAME, GRID_FRAME ),
                                    new WinMap( ndim, ina, inb, outa, outb ),
                                    true )
                           .simplify();
                    wcs.remapFrame( PIXEL_FRAME, pmap );

                    /* It would probably be quite hard to come up with a
                     * correctly mapped AXIS Frame, so for now we just copy
                     * it from the PIXEL frame.  If there are no AXIS 
                     * components in the NDF this will be correct; 
                     * otherwise, just note for now that it is broken. */
                    Mapping amap = wcs.getMapping( AXIS_FRAME, PIXEL_FRAME );
                    wcs.remapFrame( AXIS_FRAME, amap );
                    if ( axes != null ) {
                        uk.ac.starlink.ast.Frame afrm = 
                            wcs.getFrame( AXIS_FRAME );
                        afrm.setDomain( afrm.getDomain() + "-BROKEN" );
                    }
                }
                catch ( Exception e ) {
                    wcsComponent = null;
                }
            }

            /* Try to find a HISTORY component that looks right. */
            if ( ndfobj.datThere( "HISTORY" ) ) {
                DataNode histnode = makeNDFChild( ndfobj.datFind( "HISTORY" ) );
                if ( histnode instanceof HistoryDataNode ) {
                    historyComponent = (HistoryDataNode) histnode;
                }
                else {
                    historyComponent = null;
                }
            }

            /* Try to find a MORE component that looks right. */
            if ( ndfobj.datThere( "MORE" ) ) {
                HDSObject more = ndfobj.datFind( "MORE" );
                if ( more.datStruc() ) {
                    extensions = more;
                }
                else {
                    extensions = null;
                }
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    /**
     * Constructs an NDFDataNode from the file name of a container file.
     */
    public NDFDataNode( File file ) throws NoSuchDataException {
        this( getHDSFromFile( file ) );
        setLabel( file.getName() );
        setPath( file.getAbsolutePath() );
    }

    /**
     * Constructs an NDFDataNode from an HDS path.
     */
    public NDFDataNode( String path ) throws NoSuchDataException {
        this( getHDSFromPath( path ) );
    }

    public boolean allowsChildren() {
        return true;
    } 

    /**
     * Returns the standard NDF components in a standard order.
     */
    public Iterator getChildIterator() {
        int nChild;
        try {
            nChild = ndfobj.datNcomp();
        }
        catch ( HDSException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        List clist = new ArrayList( nChild );
        Set used = new HashSet( nChild );
        try {

            /* First add all the standard components in a standard order. */
            if ( title != null ) {
                clist.add( makeNDFChild( ndfobj.datFind( "TITLE" ) ) );
                used.add( "TITLE" );
            }
            if ( label != null ) {
                clist.add( makeNDFChild( ndfobj.datFind( "LABEL" ) ) );
                used.add( "LABEL" );
            }
            if ( units != null ) {
                clist.add( makeNDFChild( ndfobj.datFind( "UNITS" ) ) );
                used.add( "UNITS" );
            }
            if ( dataArray != null ) {
                clist.add( makeNDFChild( dataArray ) );
                used.add( "DATA_ARRAY" );
            }
            if ( varianceArray != null ) {
                clist.add( makeNDFChild( varianceArray ) );
                used.add( "VARIANCE" );
            }
            if ( qualityArray != null ) {
                clist.add( makeNDFChild( qualityArray ) );
                used.add( "QUALITY" );
            }
            if ( wcsComponent != null ) {
                clist.add( wcsComponent );
                used.add( "WCS" );
            }
            if ( historyComponent != null ) {
                clist.add( historyComponent );
                used.add( "HISTORY" );
            }
            if ( axes != null ) {
                DataNode axnode = makeNDFChild( ndfobj.datFind( "AXIS" ) );
                axnode.setChildMaker( getAxisChildMaker() );
                clist.add( axnode );
                used.add( "AXIS" );
            }
            if ( extensions != null ) {
                clist.add( makeNDFChild( ndfobj.datFind( "MORE" ) ) );
                used.add( "MORE" );
            }
         
            /* Then add any remaining ones (shouldn't really be any) at the
             * end. */
            if ( used.size() < nChild ) {
                for ( int i = 0; i < nChild; i++ ) {
                    HDSObject hobj = ndfobj.datIndex( i + 1 );
                    if ( ! used.contains( hobj.datName().toUpperCase() ) ) {
                        clist.add( makeNDFChild( hobj ) );
                    }
                }
            }
        }
        catch ( HDSException e ) {
            clist.add( makeErrorChild( e ) );
        }
        return clist.iterator();
    }

    public Icon getIcon() {
        return IconFactory.getIcon( IconFactory.NDF );
    }

    public String getDescription() {
        return NDShape.toString( shape );
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the string "NDF".
     *
     * @return  "NDF"
     */
    public String getNodeTLA() {
        return "NDF";
    }

    public String getNodeType() {
        return "NDF data structure";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            dv.addSeparator();
            int ndim = shape.getNumDims();
            if ( title != null ) {
                dv.addKeyedItem( "Title", title );
            }
            if ( label != null ) {
                dv.addKeyedItem( "Label", label );
            }
            if ( units != null ) {
                dv.addKeyedItem( "Units", units );
            }

            dv.addSeparator();
            dv.addKeyedItem( "Dimensionality", ndim );
            dv.addKeyedItem( "Origin", NDShape.toString( shape.getOrigin() ) );
            dv.addKeyedItem( "Dimensions",
                             NDShape.toString( shape.getDims() ) );
            dv.addKeyedItem( "Pixel bounds",
                             NDArrayDataNode.boundsString( shape ) );

            describeArrayInDetailViewer( dv, "Data component", 
                                             dataArray );
            describeArrayInDetailViewer( dv, "Variance component", 
                                             varianceArray );
            describeArrayInDetailViewer( dv, "Quality component", 
                                             qualityArray );
            if ( qualityArray != null ) {
                dv.addKeyedItem( "Badbits flag", 
                                 Byte.toString( qualityBadbits ) + 
                                 " (binary " +
                                 Integer.toBinaryString( qualityBadbits ) + 
                                 ")" );
            }
            if ( wcsComponent != null ) {
                int cur = wcs.getCurrent();
                dv.addSubHead( "World Coordinate Systems" );
                dv.addKeyedItem( "Number of frames", wcs.getNframe() );
                dv.addKeyedItem( "Current frame",
                                 Integer.toString( cur ) + " (" + 
                                 wcs.getFrame( cur ).getDomain() + ")" );
            }
            if ( extensions != null ) {
                dv.addSubHead( "Extensions" );
                try {
                    for ( int i = 0; i < extensions.datNcomp(); i++ ) {
                        HDSObject ext = extensions.datIndex( i + 1 );
                        dv.addText( ext.datName() );
                    }
                }
                catch ( HDSException e ) {
                    dv.addText( e.toString() );
                }
            }

            try {
                NdxDataNode.addDataViews( dv, getNdx() );
            }
            catch ( HDSException e ) {
                dv.logError( e );
            }
            catch ( IOException e ) {
                dv.logError( e );
            }

            fullView = dv.getComponent();
        }
        return fullView;
    }

    public void customiseTransferable( DataNodeTransferable trans ) {
        try {
            NdxDataNode.customiseTransferable( trans, getNdx() );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        catch ( HDSException e ) {
            e.printStackTrace();
        }
    }

    public byte getQualityBadbits() {
        return qualityBadbits;
    }

    private Ndx getNdx() throws IOException, HDSException {
        if ( ndx == null ) {
            URL ndurl;
            try {
                ndurl = new HDSReference( ndfobj ).getURL();
            }
            catch ( HDSException e ) {
                ndurl = null;
            }
            Ndx baseNdx = NDFNdxHandler.getInstance()
                         .makeNdx( ndfobj, ndurl, AccessMode.READ );
            ndx = new DefaultMutableNdx( baseNdx );
            ((MutableNdx) ndx).setWCS( wcs );
        }
        return ndx;
    }

    private DataNode makeNDFChild( Object childObj ) {
        if ( ndfChildMaker == null ) {
            ndfChildMaker = (DataNodeFactory) getChildMaker().clone();
            ndfChildMaker.removeNodeClass( NDFDataNode.class );
        }
        return makeChild( childObj, this, ndfChildMaker );
    }

    private DataNodeFactory getAxisChildMaker() {
        if ( axisChildMaker == null ) {
            axisChildMaker = (DataNodeFactory) getChildMaker().clone();
            axisChildMaker.removeNodeClass( NDFDataNode.class );
            axisChildMaker.removeNodeClass( ARYDataNode.class );
        }
        return axisChildMaker;
    }

    private void describeArrayInDetailViewer( DetailViewer dv, String name,
                                              ArrayStructure ary ) {
        if ( ary != null ) {
            dv.addSubHead( name );
            dv.addKeyedItem( "Type", ary.getType() );
            dv.addKeyedItem( "Variant", ary.getStorage() );
        }
    }

    /*
     * Returns null if no scalar character component by this name exists.
     */
    private String getCharacterComponent( String name )
            throws NoSuchDataException {
        try {
            if ( ndfobj.datThere( name ) ) {
                HDSObject comp = ndfobj.datFind( name );
                if ( comp.datShape().length == 0 && 
                     comp.datType().startsWith( "_CHAR" ) ) {
                    return comp.datGet0c();
                }
            }
            return null;
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    /*
     * Returns null if no array component by this name exists.
     */
    private ArrayStructure getArrayComponent( String name ) 
            throws NoSuchDataException {
        try {
            if ( ndfobj.datThere( name ) ) {
                return new ArrayStructure( ndfobj.datFind( name ) );
            }
            else {
                return null;
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e );
        }
    }

}
