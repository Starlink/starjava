package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.nio.Buffer;
import javax.swing.*;
import uk.ac.starlink.ast.*;
import uk.ac.starlink.hds.*;

/**
 * A {@link DataNode} representing an
 * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun33.htx/sun33.html">NDF</a>
 * object.
 * <p>
 * Note that the WCS FrameSet, which is read using HDS rather than 
 * NDF_GTWCS, may not be consistent with the NDF; the GRID, PIXEL and
 * AXIS Frames are likely to be incorrect.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class NDFDataNode extends HDSDataNode {
    private static Icon icon;
    private static DataNodeFactory defaultChildMaker;
    private static DataNodeFactory axisChildMaker;

    private HDSObject ndfobj;
    private Cartesian shape;
    private Cartesian origin;
    private FrameSet wcs;
    private JComponent fullView;
    private DataNodeFactory childMaker;

    private static final int GRID_FRAME = 1;
    private static final int PIXEL_FRAME = 2;
    private static final int AXIS_FRAME = 3;

    // NDF components
    private String name;
    private String title;
    private String label;
    private String units;
    private ARYDataNode dataArray;
    private ARYDataNode varianceArray;
    private ARYDataNode qualityArray;
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
            origin = dataArray.getOrigin();

            /* Get the other known array components, checking shapes
             * as we go. */
            varianceArray = getArrayComponent( "VARIANCE" );
            if ( varianceArray != null && 
               ( ! varianceArray.getShape().equals( shape ) ||
                 ! varianceArray.getOrigin().equals( origin ) ) ) {
                throw new NoSuchDataException( 
                    "DATA and VARIANCE components have different shapes" );
            }

            if ( ndfobj.datThere( "QUALITY" ) ) {
                try {
                    HDSObject qobj = ndfobj.datFind( "QUALITY" );
                    qualityArray = new ARYDataNode( qobj.datFind( "QUALITY" ) );
                    if ( qobj.datThere( "BADBITS" ) ) {
                        HDSObject qbb = qobj.datFind( "BADBITS" );
                        qualityBadbits = (byte) qbb.datGet0i();
                    }
                }
                catch ( Exception e ) {
                }
            }
            if ( qualityArray != null &&
                 ( ! qualityArray.getShape().equals( shape ) ||
                   ! qualityArray.getOrigin().equals( origin ) ) ) {
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
            if ( ndfobj.datThere( "WCS" ) ) {
                try {
                    wcsComponent = new WCSDataNode( ndfobj.datFind( "WCS" ) );

                    /* The WCS component stored in the HDSObject requires some
                     * doctoring, since its PIXEL and AXIS Frames are not
                     * trustworthy (under normal circumstances these would be
                     * ignored and regenerated during a call to NDF_GTWCS). */
                    wcs = wcsComponent.getWcs();

                    /* Remap a PIXEL Frame correctly using the GRID Frame 
                     * and the origin offset. */
                    int ndim = shape.getNdim();
                    double[] ina = new double[ ndim ];
                    double[] inb = new double[ ndim ];
                    double[] outa = new double[ ndim ];
                    double[] outb = new double[ ndim ];
                    for ( int i = 0; i < ndim; i++ ) {
                        ina[ i ] = 0.0;
                        inb[ i ] = 1.0;
                        outa[ i ] = ina[ i ] + origin.getCoord( i ) - 1.5;
                        outb[ i ] = inb[ i ] + origin.getCoord( i ) - 1.5;
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
                        afrm.setTitle( afrm.getTitle() 
                                     + " (broken in Treeview)" );
                    }
                }
                catch ( Exception e ) {
                    wcsComponent = null;
                }
            }

            /* Try to find a HISTORY component that looks right. */
            if ( ndfobj.datThere( "HISTORY" ) ) {
                try {
                    historyComponent = 
                        new HistoryDataNode( ndfobj.datFind( "HISTORY" ) );
                }
                catch ( NoSuchDataException e ) {
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
    public DataNode[] getChildren() {
        int nChild;
        try {
            nChild = ndfobj.datNcomp();
        }
        catch ( HDSException e ) {
            return new DataNode[] { new DefaultDataNode( e ) };
        }
        List clist = new ArrayList( nChild );
        Set used = new HashSet( nChild );
        try {

            /* First add all the standard components in a standard order. */
            if ( title != null ) {
                clist.add( getChildMaker()
                          .makeDataNode( ndfobj.datFind( "TITLE" ) ) );
                used.add( "TITLE" );
            }
            if ( label != null ) {
                clist.add( getChildMaker()
                          .makeDataNode( ndfobj.datFind( "LABEL" ) ) );
                used.add( "LABEL" );
            }
            if ( units != null ) {
                clist.add( getChildMaker()
                          .makeDataNode( ndfobj.datFind( "UNITS" ) ) );
                used.add( "UNITS" );
            }
            if ( dataArray != null ) {
                clist.add( dataArray );
                used.add( "DATA_ARRAY" );
            }
            if ( varianceArray != null ) {
                clist.add( varianceArray );
                used.add( "VARIANCE" );
            }
            if ( qualityArray != null ) {
                clist.add( qualityArray );
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
                DataNode axnode = 
                    getChildMaker()
                   .makeDataNode( ndfobj.datFind( "AXIS" ) );
                axnode.setChildMaker( getAxisChildMaker() );
                clist.add( axnode );
                used.add( "AXIS" );
            }
            if ( extensions != null ) {
                clist.add( getChildMaker()
                          .makeDataNode( ndfobj.datFind( "MORE" ) ) );
                used.add( "MORE" );
            }
         
            /* Then add any remaining ones (shouldn't really be any) at the
             * end. */
            if ( used.size() < nChild ) {
                for ( int i = 0; i < nChild; i++ ) {
                    HDSObject hobj = ndfobj.datIndex( i + 1 );
                    if ( ! used.contains( hobj.datName().toUpperCase() ) ) {
                        clist.add( getChildMaker()
                                  .makeDataNode( hobj ) );
                    }
                }
            }
        }
        catch ( HDSException e ) {
            clist.add( new DefaultDataNode( e ) );
        }
        catch ( NoSuchDataException e ) {
            clist.add( new DefaultDataNode( e ) );
        }
        return (DataNode[]) clist.toArray( new DataNode[ 0 ] );
    }


    public String getDescription() {
        return shape.shapeDescriptionWithOrigin( origin );
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.NDF );
        }
        return icon;
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
            int ndim = shape.getNdim();
            if ( title != null ) {
                dv.addKeyedItem( "Title", title );
            }
            if ( label != null ) {
                dv.addKeyedItem( "Label", label );
            }
            if ( units != null ) {
                dv.addKeyedItem( "Units", units );
            }
            dv.addKeyedItem( "Dimensions", 
                             Integer.toString( ndim ) );
            String dims = "";
            for ( int i = 0; i < ndim; i++ ) {
                dims += new Long( shape.getCoord( i ) ).toString()
                      + ( ( i + 1 < ndim ) ? " x " : "" );
            }
            dv.addKeyedItem( "Shape", dims );
            dv.addKeyedItem( "Pixel bounds",
                             shape.shapeDescriptionWithOrigin( origin ) );
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
                dv.addKeyedItem( "Number of frames", 
                                 Integer.toString( wcs.getNframe() ) );
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
            if ( wcsComponent != null && ndim == 2 ) {
                dv.addPane( "WCS grids",
                            new ComponentMaker() {
                                public JComponent getComponent() {
                                    return 
                                        new GridPlotter( 200,
                                                         shape.getCoords(),
                                                         wcs );
                                }
                            } );
            }
            if ( ndim == 2 ) {
                dv.addPane( "Image view", new ComponentMaker() {
                    public JComponent getComponent() throws HDSException {
                        Buffer niobuf = dataArray
                                       .getNioBuf( dataArray.getData() );
                        return new ImageViewer( niobuf, shape, 
                                                origin.getCoords(), wcs );
                    }
                } );
            }
            fullView = dv.getComponent();
        }
        return fullView;
    }

    public void setChildMaker( DataNodeFactory fact ) {
        childMaker = fact;
    }
    public DataNodeFactory getChildMaker() {
        if ( defaultChildMaker == null ) {
            defaultChildMaker = new DataNodeFactory();
            defaultChildMaker.removeNodeClass( NDFDataNode.class );
        }
        if ( childMaker == null ) {
            childMaker = defaultChildMaker;
        }
        return childMaker;
    }

    public byte getQualityBadbits() {
        return qualityBadbits;
    }

    private DataNodeFactory getAxisChildMaker() {
        if ( axisChildMaker == null ) {
            axisChildMaker = new DataNodeFactory();
            axisChildMaker.removeNodeClass( NDFDataNode.class );
            axisChildMaker.removeNodeClass( ARYDataNode.class );
        }
        return axisChildMaker;
    }

    private void describeArrayInDetailViewer( DetailViewer dv, String name,
                                              ARYDataNode aryNode ) {
        if ( aryNode != null ) {
            dv.addSubHead( name );
            dv.addKeyedItem( "Type", aryNode.getType() );
            dv.addKeyedItem( "Variant", aryNode.getVariant() );
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
     * Returns null if no array component by this name exists or if any
     * error results in looking for it.
     */
    private ARYDataNode getArrayComponent( String name ) 
            throws NoSuchDataException {
        try {
            if ( ndfobj.datThere( name ) ) {
                return new ARYDataNode( ndfobj.datFind( name ) );
            }
            else {
                return null;
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

}
