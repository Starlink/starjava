package uk.ac.starlink.hds;

import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;

/**
 * Represents an array object within an HDS file, as understood by the
 * Starlink ARY library.  This is more than an
 * HDS primitive array since it has an explicit (simple array) or 
 * implicit (primitive array) origin as well as a data array.  
 * 
 * <h3>Array file structure</h3>
 * The structures read and written by the Starlink ARY library are not
 * as far as I know documented anywhere, but are defined implicitly by
 * the ARY implementation itself.
 * This class currently makes assumptions about the form of ARY structures
 * based on reverse engineering of HDS/NDF files found in the field.
 * <ul>
 * <li>An ARY structure may be PRIMITIVE or SIMPLE.
 * <li>A PRIMITIVE ARY structure is any HDS primitive array
 * <li>A SIMPLE ARY structure is a scalar HDS structure which conforms to the 
 *     following conditions:
 *    <ul>
 *    <li>The structure has a type of "ARRAY"
 *    <li>The structure contains a primitive array called "DATA" containing
 *        the primitive data of the array object
 *    <li>The structure contains a 1-d N-element array of type _INTEGER
 *        for N-dimensional primitive array giving the origin
 *    <li>The structure may contain a _LOGICAL scalar called BAD_PIXEL
 *        indicating whether the data array may contain bad pixels or not
 *    </ul>
 * </ul>
 * This class should be modified if the above assumptions turn out to
 * be incomplete or erroneous.
 *
 * @author   Mark Taylor (Starlink)
 * @see  <a href="http://www.starlink.ac.uk/cgi-bin/htxserver/sun11.htx/">SUN/11</a>
 * @see  <a href="http://www.starlink.ac.uk/cgi-bin/htxserver/sgp38.htx/">SGP/38</a>
 */
public class ArrayStructure {

    private final HDSObject hobj;
    private final HDSObject dataObj;
    private final OrderedNDShape oshape;
    private final HDSType htype;
    private final String storage;

    private final static long[] SCALAR_DIMS = new long[ 0 ];

    /** 
     * Creates an ArrayStructure from an existing HDS object.
     *
     * @param   hobj   the HDSObject at which the array object is to be found.
     * @throws  HDSException   if an error occurred in traversing the HDS 
     *          tree or <code>hobj</code> does not represent an array
     */
    public ArrayStructure( HDSObject hobj ) throws HDSException {
        this.hobj = hobj;

        /* See if we appear to have a SIMPLE array. */
        HDSObject dat;
        if ( hobj.datStruc() && 
             hobj.datShape().length == 0 &&
             hobj.datType().equals( "ARRAY" ) &&
             hobj.datThere( "DATA" ) &&
             ( ( dat = hobj.datFind( "DATA" ) ) != null ) &&
             ( ! dat.datStruc() )  &&
             dat.datShape().length > 0 ) {
            storage = "SIMPLE";
            dataObj = dat;
            long[] dims = dataObj.datShape();
            long[] origin;
            if ( hobj.datThere( "ORIGIN" ) ) {
                HDSObject orgObj = hobj.datFind( "ORIGIN" );
                long[] orgShape = orgObj.datShape();
                if ( orgShape.length != 1 || 
                     orgShape[ 0 ] != dims.length ||
                     ! orgObj.datType().equals( "_INTEGER" ) ) {
                    throw new HDSException( 
                        "Format of ARY object is unexpected" );
                }
                origin = NDShape.intsToLongs( orgObj.datGetvi() );
            }
            else {
                origin = new long[ dims.length ];
                for ( int i = 0; i < dims.length; i++ ) {
                    origin[ i ] = 1L;
                }
            }
            oshape = new OrderedNDShape( new NDShape( origin, dims ),
                                         Order.COLUMN_MAJOR );
        }

        /* See if we appear to have a PRIMITIVE array. */
        else if ( ! hobj.datStruc() &&
                  hobj.datShape().length > 0 ) {
            storage = "PRIMITIVE";
            dataObj = hobj.datClone();
            long[] dims = dataObj.datShape();
            int ndim = dims.length;
            long[] origin = new long[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                origin[ i ] = 1L;
            }
            oshape = new OrderedNDShape( new NDShape( origin, dims ),
                                         Order.COLUMN_MAJOR );
        }

        else {
            throw new HDSException( "No array structure found in HDS object " 
                                  + hobj.datRef() );
        }
        htype = HDSType.fromName( dataObj.datType() );
    }

    /**
     * Creates the components of a new SIMPLE array object in a suitable
     * structure.  Any existing children of the structure will be removed
     * and new DATA and ORIGIN children will be added describing the
     * array object.
     *
     * @param  struct  an HDS structure scalar or array element of type ARRAY.
     * @param  shape   the shape of the new array
     * @param  htype   the HDS primitive type of the new array
     * @throws  HDSException  if an error occurs manipulating the HDS tree
     */
    public ArrayStructure( HDSObject struct, NDShape shape, HDSType htype )
            throws HDSException {

        /* Check we have been given a suitable location. */
        if ( ! struct.datStruc() ) {
            throw new HDSException( "HDS object is not a structure" );
        }
        if ( ! struct.datType().equals( "ARRAY" ) ) {
            throw new HDSException( "HDS structure type is '" 
                                  + struct.datType() + "' not 'ARRAY'" );
        }

        /* Clear out anything already there. */
        for ( int i = struct.datNcomp(); i > 0; i-- ) {
            HDSObject sub = struct.datIndex( i );
            String subname = sub.datName();
            sub.datAnnul();
            struct.datErase( subname );
        }

        /* Create the origin and data components of a SIMPLE array. */
        populateArrayStructure( struct, shape, htype );

        /* Store the fields of this object. */
        this.hobj = struct;
        this.dataObj = struct.datFind( "DATA" );
        this.oshape = new OrderedNDShape( shape, Order.COLUMN_MAJOR );
        this.htype = htype;
        this.storage = "SIMPLE";
    }
    
    /**
     * Creates a new SIMPLE array object below the given parent HDS object.
     *
     * @param   parent  the object below which the new array structure
     *                  will be created
     * @param   name    the name of the new array structure
     * @param   htype   the HDS primitive type of the new array
     * @param   shape   the shape of the new array
     * @throws  HDSException  if an error occurs manipulating the HDS tree
     */
    public ArrayStructure( HDSObject parent, String name, HDSType htype, 
                           NDShape shape ) throws HDSException {
        parent.datNew( name, "ARRAY", SCALAR_DIMS );
        HDSObject struct = parent.datFind( name );
        populateArrayStructure( struct, shape, htype );
        this.hobj = struct;
        this.dataObj = struct.datFind( "DATA" );
        this.oshape = new OrderedNDShape( shape, Order.COLUMN_MAJOR );
        this.htype = htype;
        this.storage = "SIMPLE";
    }

   
    /**
     * Creates and populates an ORIGIN structure and creates a DATA 
     * structure within a given empty structure object.
     */
    private static void populateArrayStructure( HDSObject struct, NDShape shape,
                                                HDSType htype ) 
            throws HDSException {
        long[] dims = shape.getDims();
        struct.datNew( "ORIGIN", "_INTEGER", new long[] { dims.length } );
        struct.datFind( "ORIGIN" )
              .datPutvi( NDShape.longsToInts( shape.getOrigin() ) );
        struct.datNew( "DATA", htype.getName(), dims );
    }

    /**
     * Gets the HDS object representing the data array itself.
     * This will be a primitive array with dimensions given by 
     * <code>getShape().getDims()</code>.
     *
     * @return   the primitive array containing the actual data
     */
    public HDSObject getData() {
        return dataObj;
    }

    /**
     * Gets the shape of the array.  This includes the origin and dimensions
     * information.  The pixel ordering scheme is always 
     * {@link uk.ac.starlink.array.Order#COLUMN_MAJOR}.
     *
     * @return  the shape of the array
     */
    public OrderedNDShape getShape() {
        return oshape;
    }

    /**
     * Gets the storage format; either "SIMPLE" or "PRIMITIVE".
     *
     * @return  the storage format
     */
    public String getStorage() {
        return storage;
    }

    /**
     * Returns the HDS object at which this array resides.  This will be the
     * ARRAY structure containing it for a SIMPLE array, or the 
     * primitive array object itself for a PRIMITIVE array.
     *
     * @return   the HDS object holding this array object
     */
    public HDSObject getHDSObject() {
        return hobj;
    }

    /**
     * Returns the HDS type of the primitives in the array.
     *
     * @return  the primitive type of the array
     */
    public HDSType getType() {
        return htype;
    }

}
