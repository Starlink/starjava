package uk.ac.starlink.hds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayBuilder;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSPackage;

/**
 * Turns URLs which reference HDS array resources into NDArray objects. 
 * <p>
 * URLs are given in the format:
 * <blockquote>
 *    <i>container</i><code>.sdf</code>
 * </blockquote>
 * or
 * <blockquote>
 *    <i>container</i><code>.sdf#</code><i>path</i>
 * </blockquote>
 * where the <i>container</i><code>.sdf</code> part is a full absolute or
 * relative URL referring
 * to the HDS container file, and the optional fragment identifier
 * gives the HDS path within that container file in the traditional
 * dot-separated format.  If there is no fragment identifier 
 * (no <code>#</code>), the object at the top level of the HDS container
 * file is understood.
 * <p>
 * This is a singleton class; use {@link #getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 * @see  HDSReference
 */
public class HDSArrayBuilder implements ArrayBuilder {

    /** Sole instance of the class. */
    private static HDSArrayBuilder instance = new HDSArrayBuilder();

    /**
     * Private sole constructor.
     */
    private HDSArrayBuilder() {}

    /**
     * Returns an HDSArrayBuilder.
     *
     * @return   the sole instance of this class
     * @throws   LinkageError  if the JNIHDS package is not available
     */
    public static HDSArrayBuilder getInstance() {
        if ( HDSPackage.isAvailable() ) {
            return instance;
        }
        else {
            throw (LinkageError) new LinkageError( "Native code for the JNIHDS "
                                                 + "package is not installed" );
        }
    }

    public NDArray makeNDArray( URL url, AccessMode mode ) throws IOException {

        LocalHDS lobj = LocalHDS.getReadableHDS( url );
        if ( lobj == null ) {
            return null;
        }
        HDSReference href = lobj.getHDSReference();
        final File file = href.getContainerFile();
        final boolean isTemp = lobj.isTemporary();

        try {

            /* Get a readable HDSObject now. */
            HDSObject aryObj = href.getObject( HDSReference.hdsMode( mode ) );
            ArrayStructure ary = new ArrayStructure( aryObj );

            /* Construct an ArrayImpl, which will remove any temporary file
             * when it is finished with. */
            ArrayImpl impl = new HDSArrayImpl( ary, mode ) {
                public void close() throws IOException {
                    super.close();
                    if ( isTemp ) {
                        file.delete();
                    }
                }
            };

            /* Annul the primary locator now it is taken care of by the 
             * HDSArrayImpl. */
            aryObj.datAnnul();

            /* Return an NDArray. */
            return new BridgeNDArray( impl, url );
        }

        /* Tidy up the temporary file if we failed. */
        catch ( HDSException e ) {
            if ( isTemp ) {
                file.delete();
            }
            throw (IOException) new IOException( e.getMessage() )
                    .initCause( e );
        }
    }

    /**
     * Constructs an NDArray based on an existing HDS array object.
     * The supplied HDSObject <code>ary</code> may be primary or secondary
     * and may be annulled following this call.
     *
     * @param  ary  the ArrayStructure to be viewed as an NDArray
     * @param  mode  the read/write/update mode for the NDArray
     * @return  the new NDArray based on <code>ary</code>
     * @throws  HDSException  if there is an error in HDS
     */
    public NDArray makeNDArray( ArrayStructure ary, AccessMode mode )
            throws HDSException {
        return new BridgeNDArray( new HDSArrayImpl( ary, mode ) );
    }


    public NDArray makeNewNDArray( URL url, NDShape shape, Type type,
                                   BadHandler bh )
            throws IOException {

        try {
            LocalHDS lobj = LocalHDS.getNewHDS( url, "ARRAY" );
            if ( lobj == null ) {
                return null;
            }
            HDSReference href = lobj.getHDSReference();

            /* Get a writable HDS structure object. */
            HDSObject aryObj = href.getObject( "WRITE" );

            /* Make a new ArrayStructure in this object. */
            HDSType htype = HDSType.fromJavaType( type );
            ArrayStructure ary = new ArrayStructure( aryObj, shape, htype );

            /* Make an ArrayImpl from it. */
            ArrayImpl impl = new HDSArrayImpl( ary, AccessMode.WRITE );

            /* Annul the primary locator now it is taken care of by the
             * HDSArrayImpl. */
            aryObj.datAnnul();

            /* Make an NDArray and return it. */
            return new BridgeNDArray( impl, url );
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

}
