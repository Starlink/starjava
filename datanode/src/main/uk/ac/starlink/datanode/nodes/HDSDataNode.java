package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import javax.swing.JComponent;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.datanode.viewers.ArrayBrowser;
import uk.ac.starlink.hds.ArrayStructure;
import uk.ac.starlink.hds.HDSArrayBuilder;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hds.HDSType;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.Ndx;

/**
 * A {@link DataNode} representing an
 * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/sun92.html">HDS</a>
 * object.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class HDSDataNode extends DefaultDataNode {

    private HDSObject hobj;
    private HDSObject hparent;
    private OrderedNDShape shape;   // null for scalar
    private String type;
    private boolean isStruct;
    private String name;
    private Buffer niobuf;
    private String path;
    private Ndx ndx;

    /**
     * The maximum number of cells of an array of structures to be 
     * considered as its children - more could be unwieldy.
     * Actually this doesn't need to be either static or final, it just
     * feels like a constant.
     */
    public static final int MAX_CHILDREN_PER_ARRAY = 1000;


    /**
     * Constructs an HDSDataNode from an HDSObject.
     */
    public HDSDataNode( HDSObject hobj ) throws NoSuchDataException {
        try {
            this.hobj = hobj;
            this.type = hobj.datType();
            this.name = hobj.datName();
            this.isStruct = hobj.datStruc();
            this.path = hobj.datRef();
            try {
                this.hparent = hobj.datParen();
            }
            catch ( HDSException e ) {
                this.hparent = null;
            }
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
            setIconID( hobj.datStruc() 
                           ? IconFactory.STRUCTURE
                           : IconFactory.getArrayIconID( shape != null 
                                                            ? shape.getNumDims()
                                                            : 0 ) );
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
        if ( ! NodeUtil.hasHDS() ) {
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
        if ( ! NodeUtil.hasHDS() ) {
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
    public Iterator getChildIterator() {

        /* If it's a scalar structure, its children are its components. */
        if ( isStruct && shape == null ) {
            try {
                List childList = new ArrayList();
                int nChildren = hobj.datNcomp();
                for ( int i = 0; i < nChildren; i++ ) {
                    try {
                        childList.add( makeChild( hobj.datIndex( i + 1 ) ) );
                    }
                    catch ( HDSException e ) {
                        childList.add( makeErrorChild( e ) );
                    }
                }
                return childList.iterator();
            }
            catch ( HDSException e ) {
                return Collections.singleton( makeErrorChild( e ) ).iterator();
            }
        }

        /* If it's an array structure, its children are its elements. */
        else if ( isStruct && shape.getNumPixels() < MAX_CHILDREN_PER_ARRAY ) {
            final Iterator it = shape.pixelIterator();
            return new Iterator() {
                public boolean hasNext() {
                    return it.hasNext();
                }
                public Object next() {
                    long[] pos = (long[]) it.next();
                    DataNode child;
                    try {
                        child = makeChild( hobj.datCell( pos ) );
                    }
                    catch ( HDSException e ) {
                        child = makeErrorChild( e );
                    }
                    child.setLabel( child.getName() + NDShape.toString( pos ) );
                    return child;
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /* Well it must be one or the other. */
        else {
            throw new AssertionError();
        }
    }

    public Object getParentObject() {
        return hparent;
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

    public String getPathElement() {
        return name;
    }

    public String getPathSeparator() {
        return ".";
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

    public void configureDetail( DetailViewer dv ) {
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

        /* Is it a primitive array? */
        if ( shape != null && ! isStruct ) {

            /* If it's non-numeric, present a view of its data.
             * If it's numeric, then applications can do something 
             * sensible with it using has/getDataObject. */
            try {
                if ( HDSType.fromName( type ) == null ) {
                    dv.addScalingPane( "Array data", new ComponentMaker() {
                        public JComponent getComponent() 
                                throws Exception {
                            ArrayStructure ary = new ArrayStructure( hobj );
                            return new ArrayBrowser( ary );
                        }
                    } );
                }
            }
            catch ( Exception e ) {
                dv.logError( e );
            }
        }
    }

    public boolean hasDataObject( DataType dtype ) {
        if ( dtype == DataType.NDX ) {

            /* It counts as an array if it's an array  of primitives. */
            return shape != null 
                && ! isStruct 
                && HDSType.fromName( type ) != null;
        }
        else {
            return super.hasDataObject( dtype );
        }
    }

    public Object getDataObject( DataType dtype ) throws DataObjectException {
        if ( dtype == DataType.NDX && hasDataObject( DataType.NDX ) ) {
            try {
                return getNdx();
            }
            catch ( HDSException e ) {
                throw new DataObjectException( e );
            }
        }
        else {
            return super.getDataObject( dtype );
        }
    }

    private Ndx getNdx() throws HDSException {
        if ( ndx == null ) {
            URL hurl;
            try {
                hurl = new HDSReference( hobj ).getURL();
            }
            catch ( HDSException e ) {
                hurl = null;
            }
            NDArray nda = HDSArrayBuilder.getInstance()
                         .makeNDArray( new ArrayStructure( hobj ),
                                       AccessMode.READ );

            ndx = new DefaultMutableNdx( nda );
        }
        return ndx;
    }

    public static boolean isMagic( byte[] magic ) {
        return magic.length > 4
            && (char) magic[ 0 ] == 'S'
            && (char) magic[ 1 ] == 'D'
            && (char) magic[ 2 ] == 'S'
            && magic[ 3 ] > 0 && magic[ 3 ] <= 4;  // HDS version
    }

}
