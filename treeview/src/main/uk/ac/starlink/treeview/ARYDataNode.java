package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.nio.*;
import javax.swing.*;
import javax.swing.table.*;
import uk.ac.starlink.hds.*;

/**
 * A {@link uk.ac.starlink.treeview.DataNode} representing an 
 * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun11.htx/sun11.html">ARY</a>
 * object.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ARYDataNode extends HDSDataNode {
    /*
     * This class is implemented to access data using HDSObject rather
     * than the ARY library.
     */
    private static IconFactory iconMaker = IconFactory.getInstance();
    private static DataNodeFactory defaultChildMaker;

    private static final short PRIMITIVE = 1;
    private static final short SIMPLE = 2;
    public static int MAX_TABLE_CELLS = Integer.MAX_VALUE;

    private HDSObject hobj;
    private String type;
    private short variant;
    private Cartesian shape;
    private Cartesian origin;
    private HDSObject data;
    private int nComp;
    private JComponent fullView;
    private DataNodeFactory childMaker;

    /**
     * Constructs an ARYDataNode from a Hierarchical.
     */
    public ARYDataNode( HDSObject hobj ) throws NoSuchDataException {
        super( hobj );
        this.hobj = hobj;
        boolean broken = false;
        try {

            /* See if it looks like a Simple type array. */
            if ( hobj.datStruc() && hobj.datShape().length == 0 ) {
                variant = SIMPLE;
                data = hobj.datFind( "DATA" );
                shape = new Cartesian( data.datShape() );
                int ndim = shape.getNdim();
                if ( ndim == 0 ) {
                   throw new NoSuchDataException( "Not an ARY" );
                }
                nComp = hobj.datNcomp();

                /* Find and read an Origin component, or set a null one. */
                if ( hobj.datThere( "ORIGIN" ) ) {
                    HDSObject oObj = hobj.datFind( "ORIGIN" );
                    Cartesian oShape = new Cartesian( oObj.datShape() );
                    if ( oShape.getNdim() == 1 && oShape.numCells() == ndim ) {
                        int[] oels = oObj.datGetvi();
                        origin = new Cartesian( ndim );
                        for ( int i = 0; i < ndim; i++ ) {
                            origin.setCoord( i, (long) oels[ i ] );
                        }
                    }
                    else {
                        broken = true;
                    }
                }
            } 

            /* It looks like a Primitive type array. */
            else if ( ! hobj.datStruc() && 
                      hobj.datShape().length > 0 ) {
                variant = PRIMITIVE;
                data = hobj;
                shape = new Cartesian( data.datShape() );
                origin = null;
                nComp = 0;
            }

            /* This is not an array. */
            else {
                throw new NoSuchDataException( "Object is not an ARY array" );
            }
            if ( broken ) {
                throw new NoSuchDataException( "Object is not an ARY array" );
            }
            type = data.datType();
        }
        catch ( HDSException e ) { 
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    /**
     * Constructs an ARYDataNode from an HDS path.
     */
    public ARYDataNode( String path ) throws NoSuchDataException {
        this( getHDSFromPath( path ) );
    }


    public String getDescription() {
        return shape.shapeDescriptionWithOrigin( origin ) + "  <" + type + ">";
    }

    public Icon getIcon() {
        return iconMaker.getArrayIcon( shape.getNdim() );
    }

    /**
     * Returns the string "ARY".
     *
     * @return  "ARY"
     */
    public String getNodeTLA() {
        return "ARY";
    }

    public String getNodeType() {
        return "HDS array structure";
    }
    

    public DataNode[] getChildren() {
        int nChildren = nComp;
        DataNode[] children = new DataNode[ nChildren ];
        for ( int i = 0; i < nChildren; i++ ) {
            try {
                children[ i ] = 
                    getChildMaker()
                   .makeDataNode( hobj.datIndex( i + 1 ) );

            }
            catch ( HDSException e ) {
                children[ i ] = new DefaultDataNode( e );
            }
            catch ( NoSuchDataException e ) {
                children[ i ] = new DefaultDataNode( e );
            }
        }
        return children;
    }

    public void setChildMaker( DataNodeFactory factory ) {
        childMaker = factory;
    }
    public DataNodeFactory getChildMaker() {
        if ( defaultChildMaker == null ) {
            defaultChildMaker = new DataNodeFactory();
            defaultChildMaker.removeNodeClass( ARYDataNode.class );
            defaultChildMaker.removeNodeClass( WCSDataNode.class );
            defaultChildMaker.removeNodeClass( NDFDataNode.class );
        }
        if ( childMaker == null ) {
            childMaker = defaultChildMaker;
        }
        return childMaker;
    }

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            int ndim = shape.getNdim();
            dv.addSeparator();
            dv.addKeyedItem( "Dimensions", Integer.toString( ndim ) );
            String dims = "";
            for ( int i = 0; i < ndim; i++ ) {
                dims += shape.getCoord( i )
                      + ( ( i + 1 < ndim ) ? " x " : "" );
            }
            dv.addKeyedItem( "Shape", dims );
            dv.addKeyedItem( "Pixel bounds",
                             shape.shapeDescriptionWithOrigin( origin ) );
            dv.addSeparator();
            dv.addKeyedItem( "Type", getType() );
            dv.addKeyedItem( "Variant", getVariant() );
            addDataViews( dv, data, origin );
        }
        return fullView;
    }

    public HDSObject getData() {
        return data;
    }
    public String getType() {
        return type;
    }
    public String getVariant() {
        switch ( variant ) {
            case PRIMITIVE:
                return "PRIMITIVE";
            case SIMPLE:
                return "SIMPLE";
            default:
                return "<UNKNOWN>";
        }
    }
    public Cartesian getShape() {
        return shape;
    }
    public Cartesian getOrigin() {
        Cartesian result;
        if ( origin == null ) {
            /*
             * Hmmm... I don't know if I should return a vector of zeros 
             * instead of a vector of ones for a null origin...
             */
            long[] o = new long[ shape.getNdim() ];
            for ( int i = 0; i < o.length; i++ ) {
                o[ i ] = 1;
            }
            result = new Cartesian( o );
        }
        else {
            result = origin;
        }
        return result;
    }

}
