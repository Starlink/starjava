package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSArrayBuilder;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hds.HDSType;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A {@link DataNode} representing an
 * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/sun92.html">HDS</a>
 * object.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HDSDataNode extends DefaultDataNode {
    private static IconFactory iconMaker = IconFactory.getInstance();

    private HDSObject hobj;
    private OrderedNDShape shape;   // null for scalar
    private String type;
    private boolean isStruct;
    private JComponent fullView;
    private String name;
    private Buffer niobuf;

    /**
     * The maximum number of cells of an array of structures to be 
     * considered as its children - more could be unwieldy.
     * Actually this doesn't need to be either static or final, it just
     * feels like a constant.
     */
    public static final int MAX_CHILDREN_PER_ARRAY = 50;


    /**
     * Constructs an HDSDataNode from an HDSObject.
     */
    public HDSDataNode( HDSObject hobj ) throws NoSuchDataException {
        try {
            this.hobj = hobj;
            this.type = hobj.datType();
            this.name = hobj.datName();
            this.isStruct = hobj.datStruc();
            long[] dims = hobj.datShape();
            int ndim = dims.length;
            if ( ndim > 0 ) {
                long[] origin = new long[ ndim ];
                Arrays.fill( origin, 1L );
                this.shape = 
                    new OrderedNDShape( origin, dims, Order.COLUMN_MAJOR );
            }
            else {
                this.shape = null;
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        setLabel( name );
    }

    /**
     * Constructs an HDSDataNode from the file name of a container file.
     */
    public HDSDataNode( File file ) throws NoSuchDataException {
        this( getHDSFromFile( file ) );
        setLabel( file.getName() );
    }

    /**
     * Constructs an HDSDataNode from an HDS path.
     */
    public HDSDataNode( String path ) throws NoSuchDataException {
        this( getHDSFromPath( path ) );
    }

    protected static HDSObject getHDSFromPath( String path )
            throws NoSuchDataException {
        if ( ! Driver.hasHDS ) {
            throw new NoSuchDataException( "HDS subsystem not installed" );
        }
        if ( path.endsWith( ".sdf" ) ) {
            path = path.substring( 0, path.length() - 4 );
        }
        HDSReference ref;
        try { 
            ref = new HDSReference( path );
        }
        catch ( Exception e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        try {
            return ref.getObject( "READ" );
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }

    protected static HDSObject getHDSFromFile( File file ) 
            throws NoSuchDataException {
        HDSReference ref;
        if ( ! Driver.hasHDS ) {
            throw new NoSuchDataException( "HDS subsystem not installed" );
        }
        try {
            ref = new HDSReference( file );
        }
        catch ( Exception e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
        try {
            return ref.getObject( "READ" );
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
        }
    }


    /**
     * Indicates whether this node allows child nodes or not.
     * We consider that an <code>HDSDataNode</code> may allow children 
     * in two ways: it may either be a scalar structure, in which case 
     * its children will be its components, or it may be an array of
     * structures, in which case its children will be the elements of
     * the array.  Here the term <i>structure</i> is used in the HDS sense.
     * <p>
     * We do not consider the elements of an array to be children if
     * the if there are 'too many' elements (more than MAX_CHILDREN_PER_ARRAY).
     * This is arbitrary, but is intended to prevent display getting 
     * too unwieldy.
     * <p>
     * Another possibility would be to allow the elements of an array 
     * of primitives to be considered as elements.  Following the 
     * (sensible) behaviour of HDSTRACE We don't do this.
     */
    public boolean allowsChildren() {
        try {
            boolean isStruct = hobj.datStruc();
        }
        catch ( HDSException e ) {
            throw new RuntimeException( "Unexpected HDS error" );
        }
        return ( isStruct && shape == null )
            || ( isStruct && shape.getNumPixels() < MAX_CHILDREN_PER_ARRAY );
    }

    /**
     * Returns the children of this node.  This will either be the 
     * components of a scalar structure, or the elements of an array of
     * structures.
     *
     * @return  an array of <code>DataNode</code>s considered to be the
     *          children of this node
     */
    public DataNode[] getChildren() {
        DataNode[] children;

        /* If it's a scalar structure, its children are its components. */
        if ( isStruct && shape == null ) {
            int nChildren;
            try {
                nChildren = hobj.datNcomp();
            }
            catch ( HDSException e ) {
                return new DataNode[] { new ErrorDataNode( e ) };
            }
            children = new DataNode[ nChildren ];
            for ( int i = 0; i < nChildren; i++ ) {
                try {
                    children[ i ] = 
                        getChildMaker()
                       .makeDataNode( hobj.datIndex( i + 1 ) );
                }
                catch ( HDSException e ) {
                    children[ i ] = new ErrorDataNode( e );
                }
                catch ( NoSuchDataException e ) {
                    children[ i ] = new ErrorDataNode( e );
                }
            }
        }

        /* If it's an array structure, its children are its elements. */
        else if ( isStruct && shape.getNumPixels() < MAX_CHILDREN_PER_ARRAY ) {
            int nChildren = (int) shape.getNumPixels();
            children = new DataNode[ nChildren ];
            int ichild = 0;
            for ( Iterator pit = shape.pixelIterator(); pit.hasNext(); ) {
                long[] pos = (long[]) pit.next();
                try {
                    children[ ichild ] = getChildMaker()
                                        .makeDataNode( hobj.datCell( pos ) );
                }
                catch ( HDSException e ) {
                    children[ ichild ] = new ErrorDataNode( e );
                }
                catch ( NoSuchDataException e ) {
                    children[ ichild ] = new ErrorDataNode( e );
                }
                children[ ichild ].setLabel( children[ ichild ].getName() 
                                           + NDShape.toString( pos ) );
                ichild++;
            }
        }
        else {
            throw new RuntimeException( "I have no children!" 
                                      + "(programming error)" );
        }
        return children;
    }

    public String getDescription() {
        StringBuffer descrip = new StringBuffer();
        if ( shape != null ) {
            descrip.append( NDShape.toString( shape.getDims() ) );
        }

        descrip.append( "  <" )
               .append( type ) 
               .append( ">" );
        
        if ( ! isStruct && shape == null ) {
            try {
                if ( type.startsWith( "_CHAR" ) ) {
                    descrip.append( '"' )
                           .append( hobj.datGet0c() )
                           .append( '"' );
                }
                else {
                    descrip.append( "  " )
                           .append( hobj.datGet0c() );
                }
            }
            catch ( HDSException e ) {
                descrip.append( "  ???" );
            }
        }
        return descrip.toString();
    }

    public Icon getIcon() {
        return allowsChildren() 
            ? iconMaker.getIcon( IconFactory.STRUCTURE )
            : iconMaker.getArrayIcon( shape != null ? shape.getNumDims() : 0 );
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the string "HDS".
     *
     * @return  "HDS"
     */
    public String getNodeTLA() {
        return "HDS";
    }

    public String getNodeType() {
        return "HDS data structure";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            if ( isStruct ) {
                dv.addKeyedItem( "Structure type", type );
            }
            else {
                dv.addKeyedItem( "Data type", type );
            }

            /* It's an array of some kind. */
            if ( shape != null ) {
                long[] dims = shape.getDims();
                dv.addKeyedItem( "Dimensions", dims.length );
                StringBuffer sdims = new StringBuffer();
                for ( int i = 0; i < dims.length; i++ ) {
                    if ( i > 0 ) {
                        sdims.append( " x " );
                    }
                    sdims.append( dims[ i ] );
                }
                dv.addKeyedItem( "Shape", sdims );
            }

            /* It's a scalar. */
            if ( shape == null && ! isStruct ) {
                dv.addSeparator();
                String value;
                try {
                    value = hobj.datGet0c();
                }
                catch ( HDSException e ) {
                    value = e.getMessage();
                }
                dv.addKeyedItem( "Value", value );
            }

            /* If it's a numeric primitive array, turn it into an NDArray and
             * let NDArrayDataNode do the work. */
            try {
                if ( shape != null && ! isStruct 
                     && HDSType.fromName( type ) != null ) {
                    NDArray nda = HDSArrayBuilder.getInstance()
                                 .makeNDArray( new ArrayStructure( hobj ),
                                               AccessMode.READ );
                    NDArrayDataNode.addDataViews( dv, nda, null );
                }
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

    public static boolean isMagic( byte[] magic ) {
        return (char) magic[ 0 ] == 'S'
            && (char) magic[ 1 ] == 'D'
            && (char) magic[ 2 ] == 'S';
    }

}

