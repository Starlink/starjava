package uk.ac.starlink.treeview;

import java.util.*;
import java.io.*;
import java.nio.Buffer;
import javax.swing.*;
import uk.ac.starlink.hds.*;
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
    private Cartesian shape;
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
     * The maximum number of cells allowed in an array for browsing in
     * an ArrayBrowser (JTable).  Not sure if high numbers will cause 
     * trouble?  JTable may be smart enough that they don't.
     */
    public static final int MAX_TABLE_CELLS = Integer.MAX_VALUE;

    /**
     * The maximum number of cells of an array of primitives to be 
     * displayed in the detail viewer - more could be unwieldy.
     * Actually this doesn't need to be either static or final, it just
     * feels like a constant.
     */
    public static final int MAX_ELEMENTS_PER_ARRAY = 10;


    /**
     * Constructs an HDSDataNode from an HDSObject.
     */
    public HDSDataNode( HDSObject hobj ) throws NoSuchDataException {
        try {
            this.hobj = hobj;
            this.type = hobj.datType();
            this.shape = new Cartesian( hobj.datShape() );
            this.name = hobj.datName();
            this.isStruct = hobj.datStruc();
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
        return ( isStruct && shape.getNdim() == 0 )
            || ( isStruct && shape.numCells() < MAX_CHILDREN_PER_ARRAY );
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
        if ( isStruct && shape.getNdim() == 0 ) {
            int nChildren;
            try {
                nChildren = hobj.datNcomp();
            }
            catch ( HDSException e ) {
                return new DataNode[] { new DefaultDataNode( e ) };
            }
            children = new DataNode[ nChildren ];
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
        }

        /* If it's a vector structure, its children are its elements. */
        else if ( isStruct && shape.numCells() < MAX_CHILDREN_PER_ARRAY ) {
            int nChildren = (int) shape.numCells();
            children = new DataNode[ nChildren ];
            Iterator cellIt = shape.cellIterator();
            for ( int i = 0; cellIt.hasNext(); i++ ) {
                Cartesian pos = (Cartesian) cellIt.next();
                try {
                    children[ i ] = 
                        getChildMaker()
                       .makeDataNode( hobj.datCell( pos.getCoords() ) );
                }
                catch ( HDSException e ) {
                    children[ i ] = new DefaultDataNode( e );
                }
                catch ( NoSuchDataException e ) {
                    children[ i ] = new DefaultDataNode( e );
                }
                children[ i ].setLabel( children[ i ].getName() + pos );
            }
        }
        else {
            throw new RuntimeException( "I have no children!" 
                                      + "(programming error)" );
        }
        return children;
    }

    public String getDescription() {
        String descrip;
        descrip = shape.toString() + "  <" + type + ">";
        if ( ! isStruct && shape.getNdim() == 0 ) {
            try {
                if ( type.startsWith( "_CHAR" ) ) {
                    descrip += "  \"" + hobj.datGet0c() + "\"";
                }
                else {
                    descrip += "  " + hobj.datGet0c();
                }
            }
            catch ( HDSException e ) {
                descrip += "  ???";
            }
        }
        return descrip;
    }

    public Icon getIcon() {
        return allowsChildren() 
            ? iconMaker.getIcon( IconFactory.STRUCTURE )
            : iconMaker.getArrayIcon( shape.getNdim() );
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
            int ndim = shape.getNdim();
            dv.addKeyedItem( "Dimensions", Integer.toString( ndim ) );
            if ( ndim > 0 ) {
                String dims = "";
                for ( int i = 0; i < shape.getNdim(); i++ ) {
                   dims += shape.getCoord( i )
                         + ( ( i + 1 < shape.getNdim() ) ? " x " : "" );
                }
                dv.addKeyedItem( "Shape", dims );
            }
            if ( ndim == 0 && ! isStruct ) {
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
            if ( ndim > 0 && ! isStruct ) {
                addDataViews( dv, hobj, null );
            }
        }
        return fullView;
    }

    public Cartesian getShape() {
        return shape;
    }

    public String getType() {
        return type;
    }

    protected Buffer getNioBuf( HDSObject obj ) throws HDSException {
        if ( niobuf == null ) {
            String maptype = obj.datType();
            if ( maptype.equals( "_UBYTE" ) ) {
                maptype = "_WORD";
            }
            else if ( maptype.equals( "_UWORD" ) ) {
                maptype = "_INTEGER";
            }
            niobuf = obj.datMapv( maptype, "READ" );
        }
        return niobuf;
    }

    private Number getBadValue( HDSObject obj ) throws HDSException {
        String htype = obj.datType();
        if ( htype.equals( "_BYTE" ) ) {
            return new Byte( (byte) 0x80 );
        }
        else if ( htype.equals( "_UBYTE" ) ) {
            return new Short( (short) (byte) 0xff );
        }
        else if ( htype.equals( "_WORD" ) ) {
            return new Short( (short) 0x8000 );
        }
        else if ( htype.equals( "_UWORD" ) ) {
            return new Integer( (int) (short) 0xffff );
        }
        else if ( htype.equals( "_INTEGER" ) ) {
            return new Integer( 0x80000000 );
        }
        else if ( htype.equals( "_REAL" ) ) {
            return new Float( Float.intBitsToFloat( 0xff7fffff ) );
        }
        else if ( htype.equals( "_DOUBLE" ) ) {
            return new Double( Double.longBitsToDouble( 0xffefffffffffffffL ) );
        }
        else {
            // assert false;
            return null;
        }
    }

    protected void addDataViews( DetailViewer dv, final HDSObject dataobj, 
                                 final Cartesian origin ) {
        final Cartesian dataShape = getShape();
        String dataType = getType();
        int ndim = dataShape.getNdim();
        if ( dataShape.numCells() > MAX_ELEMENTS_PER_ARRAY &&
             dataShape.numCells() < MAX_TABLE_CELLS &&
             ( dataType.equals( "_UBYTE" ) ||
               dataType.equals( "_BYTE" ) ||
               dataType.equals( "_UWORD" ) ||
               dataType.equals( "_WORD" ) ||
               dataType.equals( "_INTEGER" ) ||
               dataType.equals( "_REAL" ) ||
               dataType.equals( "_DOUBLE" ) ) ) {
            dv.addPane( "Array data", new ComponentMaker() {
                public JComponent getComponent() throws HDSException {
                    return new ArrayBrowser( getNioBuf( dataobj ),
                                             getBadValue( dataobj ),
                                             origin, dataShape );
                }
            } );
            if ( ndim == 1 ) {
                dv.addPane( "Graph view", new ComponentMaker() {
                    public JComponent getComponent() throws HDSException,
                                                            SplatException {
                        long start = ( origin == null ) ? 1 
                                                        : origin.getCoord( 0 );
                        return new SpectrumViewer( dataobj, start, getName() );
                    }
                } );
            }
            if ( ndim == 2 ) {
                dv.addPane( "Image view", new ComponentMaker() {
                    public JComponent getComponent() throws HDSException {
                        return new ImageViewer( getNioBuf( dataobj ), 
                                                dataShape );
                    }
                } );
            }
        }
        else {
            dv.addSubHead( "Values" );
            Iterator elit = dataShape.cellIterator();
            int nel = 0;
            while ( elit.hasNext() && nel++ < MAX_ELEMENTS_PER_ARRAY ) {
                Cartesian pos = (Cartesian) elit.next();
                String val;
                try {
                    val = dataobj.datCell( pos.getCoords() ).datGet0c();
                }
                catch ( HDSException e ) {
                    val = e.toString();
                }
                dv.addText( pos.toString() + ":  " + val );
            }
            if ( elit.hasNext() ) {
                dv.addText( "             . . . " );
            }
        }
    }
}

