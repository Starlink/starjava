package uk.ac.starlink.ast;

import java.io.*;

/**
 * Java interface to the AST IntraMap
 * class
 * - map points using a private transformation function.
 * The <code>IntraMap</code> allows you to use a custom transformation
 * function, as implemented by a {@link Transformer} object, 
 * to perform the transformations used by a <code>Mapping</code>.
 * This allows you to create <code>Mapping</code>s that perform any
 * conceivable coordinate transformation.
 * <p>
 * However, an <code>IntraMap</code> is intended for use within a 
 * given software environment, and if they are to be communicated 
 * via {@link Channel}s can only be used where class libraries containing
 * the appropriate <code>Transformer</code> classes are available
 * (and only if those classes are <code>Serializable</code>).
 *
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_IntraMap'>AST IntraMap</a>
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class IntraMap extends Mapping implements NeedsChannelizing {

    /* The Transformer object which actually performs the transformations. */
    private Transformer trans;

    /**
     * Constructs an <code>IntraMap</code> from a given <code>Transformer</code>
     * object.
     *
     * @param   trans  the Transformer which will do the actual transformations
     * @param   nin    the number of input coordinates for the forward 
     *                 transformation
     *                 (or output coordinates for the inverse transformation)
     * @param   nout   the number of output coordinates for the forward
     *                 transformation
     *                 (or input coordinates for the inverse transformation)
     * @throws  AstException  if an error arose in the AST library, or 
     *                        if <code>trans</code> is not able to transform
     *                        from <code>nin</code> to <code>nout</code>
     *                        coordinates
     */
    public IntraMap( Transformer trans, int nin, int nout ) {
        this.trans = trans;
        construct( trans, nin, nout );
    }
    private native void construct( Transformer trans, int nin, int nout );

    /**
     * Constructs a two-dimensional <code>IntraMap</code> from a given 
     * <code>Transformer2</code> object.
     *
     * @param  trans  the <code>Transformer2</code> which will do the actual
     *                two-coordinate to two-coordinate transformations
     */
    public IntraMap( Transformer2 trans ) {
        this.trans = trans;
        construct( trans, 2, 2 );
    }

    /**
     * Constructs a one-dimensional <code>IntraMap</code> from a given
     * <code>Transformer1</code> object.
     *
     * @param  trans  the <code>Transformer1</code> which will do the actual
     *                one-coordinate to one-coordinate transformations
     */
    public IntraMap( Transformer1 trans ) {
        this.trans = trans;
        construct( trans, 1, 1 );
    }

    /**
     * Finalizes the object.  Certain resources allocated by the native
     * code are freed, and the finalizer of the superclass is called.
     */
    public void finalize() throws Throwable {
        destroy();
        this.trans = null;
        super.finalize();
    }
    private native void destroy();

    /**
     * This method, called by Channel's write method, attempts to serialize
     * this IntraMap's Transformer object into the IntraFlag attribute.
     * This will allow it to persist out of memory.
     *
     * @throws  NotSerializableException  if the <code>Transformer</code>
     *                                    used by this mapping cannot be
     *                                    serialized
     * @throws  IOException  if there was some error in I/O
     */
    public void channelize() throws IOException {

        /* Attempt serialization into the IntraFlag attribute. */
        if ( trans instanceof Serializable ) {

            /* Serialize the Transformer object into a byte buffer. */
            Serializable strans = (Serializable) trans;
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream( bstream );
            ostream.writeObject( strans );
            ostream.flush();

            /* Encode it in printable form so Channel.write will not choke
             * on it. */
            byte[] transRep = encodePrintable( bstream.toByteArray() );

            /* And save it in the IntraFlag attribute, where it will get
             * get written by Channel.write. */
            setC( "IntraFlag", new String( transRep ) );
        }

        /* The Transformer is not Serializable. */
        else {
            throw new NotSerializableException( trans.getClass().getName() );
        }
    }

    /**
     * This method, called by Channel's read method, recovers the 
     * serialized Transformer object from the IntraFlag attribute of the
     * AST object, and invokes the non-trivial parts of the IntraMap's
     * construction.
     *
     * @throws  IOException  if there was some error in I/O
     */
    public void unChannelize() throws IOException {

        /* Get the number of input and output coordinates of the Mapping. */
        int nin = getNin();
        int nout = getNout();

        /* Read the encoded byte array representing the Transformer from the
         * IntraFlag attribute. */
        String transString = getC( "IntraFlag" );
        byte[] transEncBytes = transString.getBytes( "UTF-8" );

        /* Decode it. */
        byte[] transBytes = decodePrintable( transEncBytes );

        /* Extract the Transformer object from it. */
        ByteArrayInputStream bstream = new ByteArrayInputStream( transBytes );
        ObjectInputStream ostream = new ObjectInputStream( bstream );
        try {
            this.trans = (Transformer) ostream.readObject();
        }
        catch ( ClassNotFoundException e ) {
            throw new Error( e.getMessage() );
        }
      
        /* Invoke the native method which does the underlying AST 
         * object construction. */
        construct( trans, nin, nout );
    }

    /**
     * Returns a fairly deep copy of this object.
     * The <tt>Transformer</tt> of the returned copy however 
     * is a reference to the same object as the transformer of this object.
     *
     * @return  copy
     */
    public AstObject copy() {
        AstObject copy = super.copy();
        ((IntraMap) copy).trans = this.trans;
        return copy;
    }

    /* Printable characters used for encoding and decoding. */
    private static final byte[] Chrs64 = new byte[] {
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,  // ABCD EFGH
        0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,  // IJKL MNOP
        0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,  // QRST UVWX
        0x59, 0x5A, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66,  // YZab cdef
        0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E,  // ghij klmn
        0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76,  // opqr stuv
        0x77, 0x78, 0x79, 0x7A, 0x30, 0x31, 0x32, 0x33,  // wxyz 0123
        0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2B, 0x2F   // 4567 89+/
    };
    private static final byte[] Inv64 = new byte[ 256 ];
    static {
        for ( byte i = 0; i < 64; i++ ) {
            Inv64[ Chrs64[ i ] ] = i;
        }
    }


    /*
     * Encodes an arbitrary string of bytes into printable form.  This is
     * required for outputting serialized objects via the Channel 
     * mechanism.  Since persistent representations (files archived on disks)
     * are involved, this method should be as stable as possible. 
     * It must perform the opposite transformation to decodePrintable.
     * The useful part of the result must be terminated with a null (zero)
     * byte, and other zero bytes may follow.
     * It currently uses a base64-like scheme.
     */
    private static byte[] encodePrintable( byte[] plain ) {
        int leng = plain.length;
        byte[] coded = new byte[ ( leng / 3 + 2 ) * 4 ];
        int ic = 0;
        int ip = 0;

        while ( ip < leng ) {
            byte b0 = ( ip < leng ) ? plain[ ip++ ] : 0;
            byte b1 = ( ip < leng ) ? plain[ ip++ ] : 0;
            byte b2 = ( ip < leng ) ? plain[ ip++ ] : 0;

            byte ix0 = (byte) ( (b0 & 0x3f) );
            byte ix1 = (byte) ( ((b0 & 0xc0) >>> 6) | ((b1 & 0x0f) << 2 ) );
            byte ix2 = (byte) ( ((b1 & 0xf0) >>> 4) | ((b2 & 0x03) << 4 ) );
            byte ix3 = (byte) ( ((b2 & 0xfc) >>> 2) );

            coded[ ic++ ] = Chrs64[ ix0 ];
            coded[ ic++ ] = Chrs64[ ix1 ];
            coded[ ic++ ] = Chrs64[ ix2 ];
            coded[ ic++ ] = Chrs64[ ix3 ];
        }
        return coded;
    }

    /*
     * Decodes a string of printable bytes into arbitrary form.  This is
     * required for inputting serialized objects vias the Channel 
     * mechanism.  Since persistent representations (files archived on disks)
     * are involved, this method should be as stable as possible. 
     * It must perform the opposite transformation to encodePrintable.
     * Null (zero) bytes at the end of the input string may be ignored -
     * the length of the array does not need to be preserved.
     * It currently uses a base64-like scheme.
     */
    private static byte[] decodePrintable( byte[] coded ) {
        int leng = coded.length;
        byte[] plain = new byte[ ( leng / 4 + 2 ) * 3 ];
        int ic = 0;
        int ip = 0;

        while ( ic < leng ) {
            byte b0 = ( ic < leng ) ? Inv64[ coded[ ic++ ] ] : 0;
            byte b1 = ( ic < leng ) ? Inv64[ coded[ ic++ ] ] : 0;
            byte b2 = ( ic < leng ) ? Inv64[ coded[ ic++ ] ] : 0;
            byte b3 = ( ic < leng ) ? Inv64[ coded[ ic++ ] ] : 0;

            byte iv0 = (byte) ( b0 | (b1 << 6) );
            byte iv1 = (byte) ( (b1 >>> 2) | (b2 << 4) );
            byte iv2 = (byte) ( (b2 >>> 4) | (b3 << 2) );

            plain[ ip++ ] = iv0;
            plain[ ip++ ] = iv1;
            plain[ ip++ ] = iv2;
        }
        return plain;
    }
}

