package uk.ac.starlink.hds;

import java.io.IOException;
import java.nio.Buffer;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NioArrayImpl;
import uk.ac.starlink.array.Type;

/**
 * An ArrayImpl based on an HDS array object.
 * HDSArrayImpls always have a pixel ordering of {@link Order#COLUMN_MAJOR}
 * and a bad value as per {@link HDSType#getBadValue}.
 *
 * @author   Mark Taylor (Starlink)
 */
class HDSArrayImpl extends NioArrayImpl {

    private HDSObject aryobj;

    /**
     * Constructs an ArrayImpl based on an existing HDS ArrayStructure
     * using its natural data type. 
     * A clone of the HDSObject in <code>ary</code> will be retained
     * and set primary; it is annulled when the ArrayImpl is closed.
     * Therefore the data of <code>ary</code> may be primary or 
     * secondary on input, and may be annulled afterwards.
     *
     * @param   ary    the array structure on which the ArrayImpl will be based
     * @param   mode   the read/write/update access mode with which the
     *                 structure is to be opened
     */
    public HDSArrayImpl( ArrayStructure ary, AccessMode mode ) 
            throws HDSException {
        this( ary, ary.getType().getJavaType(), mode );
    }

    /**
     * Constructs an ArrayImpl based on an existing HDS ArrayStructure 
     * using a specified data type.
     * A clone of the HDSObject in <code>ary</code> will be retained
     * and set primary; it is annulled when the ArrayImpl is closed.
     * Therefore the data of <code>ary</code> may be primary or 
     * secondary on input, and may be annulled afterwards.
     *
     * @param   ary    the array structure no which the ArrayImpl will be based
     * @param   mapType the primitive array type which the resulting
     *                  ArrayImpl will have
     * @param   mode   the read/write/update access mode with which the
     *                 structure is to be opened
     */
    public HDSArrayImpl( ArrayStructure ary, Type mapType, AccessMode mode )
            throws HDSException {
        this( ary, mapType, HDSType.fromJavaType( mapType ), mode );
    }

    private HDSArrayImpl( ArrayStructure ary, Type mapType, HDSType hType,
                          AccessMode mode ) throws HDSException {
        super( new BufGet( ary.getData(), hType, mode ),
               ary.getShape(), mapType, hType.getBadValue() );

        /* Store a reference to this array and make it primary.  
         * The reference is not used by any methods
         * of this class, but is required so that the HDS object is not
         * finalised and annulled by the garbage collector while we are
         * still using it. */
        aryobj = ary.getHDSObject().datClone();
        aryobj.datPrmry( true );
    }

    /**
     * Annuls the HDS object holding the array data and calls the 
     * superclass close method.
     */
    public void close() throws IOException {
        super.close();
        try {
            if ( aryobj.datValid() ) {
                aryobj.datAnnul();
            }
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() ) 
                               .initCause( e );
        }
    }


    /**
     * Private class implementing the buffer creator object required
     * for deferred mapping of the HDS array.
     */
    private static class BufGet implements NioArrayImpl.BufferGetter {

        private final HDSObject hobj;
        private final HDSType htype;
        private final String mode;
        private final boolean readonly;

        public BufGet( HDSObject hobj, HDSType htype, AccessMode mode ) {
            this.hobj = hobj;
            this.htype = htype;
            if ( mode == AccessMode.READ ) {
                this.readonly = true;
                this.mode = "READ";
            }
            else if ( mode == AccessMode.UPDATE ) {
                this.readonly = false;
                this.mode = "UPDATE";
            }
            else if ( mode == AccessMode.WRITE ) {
                this.readonly = false;
                this.mode = "WRITE/BAD";
            }
            else {
                throw new AssertionError();
            }
        }
        public Buffer getBuffer() throws IOException {
            try {
                return hobj.datMapv( htype.getName(), mode );
            }
            catch ( HDSException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
        public void releaseBuffer() throws IOException {
            try {
                hobj.datUnmap();
            }
            catch ( HDSException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
        public boolean isReadOnly() {
            return readonly;
        }
    }

}
