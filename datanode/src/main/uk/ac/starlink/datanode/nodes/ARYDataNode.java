package uk.ac.starlink.datanode.nodes;

import javax.swing.Icon;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSArrayBuilder;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSType;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.Ndx;

/**
 * A DataNode representing an 
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
    private ArrayStructure aryobj;
    private NDShape shape;
    private DataNodeFactory customChildMaker;
    private Ndx ndx;

    /**
     * Constructs an ARYDataNode from an HDSObject.
     */
    public ARYDataNode( HDSObject hobj ) throws NoSuchDataException {
        super( hobj );
        try {
            if ( HDSType.fromName( hobj.datType() ) == null ) {
                throw new NoSuchDataException( "Not a numeric type" );
            }
            this.aryobj = new ArrayStructure( hobj );
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( "Not an ARY structure", e );
        }
        this.shape = aryobj.getShape();
    }

    /**
     * Constructs and ARYDataNode from an ArrayStrucutre.
     */
    public ARYDataNode( ArrayStructure aryobj ) throws NoSuchDataException {
        super( aryobj.getHDSObject() );
        this.aryobj = aryobj;
        this.shape = aryobj.getShape();
    }

    /**
     * Constructs an ARYDataNode from an HDS path.
     */
    public ARYDataNode( String path ) throws NoSuchDataException {
        this( getHDSFromPath( path ) );
    }

    public String getDescription() {
        return NDShape.toString( shape )
             + "  <" + aryobj.getType() + ">";
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

    public Icon getIcon() {
        return IconFactory
              .getIcon( IconFactory.getArrayIconID( shape.getNumDims() ) );
    }
    
    /**
     * Removes the possibility of creating certain types of data node
     * prior to setting the factory (ARY, NDF, WCS).  These can never
     * be descendants of an ARY, but the descendants of an ARY can 
     * sometimes look like they are one of these to the factory.
     */
    public synchronized DataNodeFactory getChildMaker() {
        if ( customChildMaker == null ) {
            customChildMaker = new DataNodeFactory( super.getChildMaker() );
            customChildMaker.removeNodeClass( ARYDataNode.class );
            customChildMaker.removeNodeClass( WCSDataNode.class );
            customChildMaker.removeNodeClass( NDFDataNode.class );
        }
        return customChildMaker;
    }

    public synchronized void setChildMaker( DataNodeFactory childMaker ) {
        super.setChildMaker( childMaker );
        customChildMaker = null;
    }

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Dimensionality", shape.getNumDims() );
        dv.addKeyedItem( "Origin", NDShape.toString( shape.getOrigin() ) );
        dv.addKeyedItem( "Dimensions", NDShape.toString( shape.getDims() ) );
        dv.addKeyedItem( "Pixel bounds", 
                         NDArrayDataNode.boundsString( shape ) );
        dv.addSeparator();
        dv.addKeyedItem( "Type", aryobj.getType() );
        dv.addSeparator();
        dv.addKeyedItem( "Storage variant", aryobj.getStorage() );
    }

    public boolean hasDataObject( DataType dtype ) {
        if ( dtype == DataType.NDX ) {
            return true;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        if ( dtype == DataType.NDX ) {
            try {
                return getNdx();
            }
            catch ( HDSException e ) {
                throw new DataObjectException( "Error reading array", e );
            }
        }
        else {
            return super.getDataObject( dtype );
        }
    }

    private Ndx getNdx() throws HDSException {
        if ( ndx == null ) {
            NDArray nda = HDSArrayBuilder.getInstance()
                         .makeNDArray( aryobj, AccessMode.READ );
            ndx = new DefaultMutableNdx( nda );
        }
        return ndx;
    }

}
