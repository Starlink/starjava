package uk.ac.starlink.treeview;

import java.io.IOException;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSArrayBuilder;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSType;

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

    private ArrayStructure aryobj;
    private JComponent fullView;
    private DataNodeFactory childMaker;
    private NDShape shape;

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

    public Icon getIcon() {
        return iconMaker.getArrayIcon( shape.getNumDims() );
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
            dv.addSeparator();
            dv.addKeyedItem( "Dimensionality", "" + shape.getNumDims() );
            dv.addKeyedItem( "Origin", NDShape.toString( shape.getOrigin() ) );
            dv.addKeyedItem( "Dimensions", 
                             NDShape.toString( shape.getDims() ) );
            dv.addKeyedItem( "Pixel bounds", 
                             NDArrayDataNode.boundsString( shape ) );
            dv.addSeparator();
            dv.addKeyedItem( "Type", aryobj.getType().toString() );
            dv.addSeparator();
            dv.addKeyedItem( "Storage variant", aryobj.getStorage() );

            try {
                NDArray nda = HDSArrayBuilder.getInstance()
                             .makeNDArray( aryobj, AccessMode.READ );
                NDArrayDataNode.addDataViews( dv, nda, null );
            }
            catch ( HDSException e ) {
                dv.logError( e );
            }
            catch ( IOException e ) {
                dv.logError( e );
            }
        }
        return fullView;
    }

}
