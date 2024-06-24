package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;

/**
 * Provides a copy of an existing NDArray with its data backed by a fast
 * random-access scratch array in memory or on local disk.  
 * This may be used for instance to provide fast
 * access to an NDArray whose pixel values may be passing through
 * computationally intensive 'wrapper' layers, or to provide random 
 * access to data supplied from a non-random stream.
 * <p>
 * The data and characteristics of the returned copy, including URL,
 * will be identical to that of the base array,
 * except that it will have random access, read access and write
 * access, and it is likely to have mapped access 
 * (though cannot do so in the case in which the requested
 * dimensions imply more than Integer.MAX_VALUE pixels).
 * Note that the guaranteed readability and writability of the
 * returned object does not indicate that reading from or writing to
 * it will be affected by/have effects on the data of the
 * base array. 
 * <p>
 * In construction an AccessMode parameter is used, which controls when data 
 * gets copied.
 * If and only if the mode parameter represents read access
 * (READ or UPDATE) then all the data will be copied from the base 
 * array to the scratch copy at construction time.  
 * If and only if it represents write access (WRITE or UPDATE)
 * then all the data will be copied from the scratch copy to the 
 * base array when the CopyNDArray is closed.  The CopyNDArray
 * will close the base array when it is closed itself.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CopyNDArray extends ScratchNDArray {

    private NDArray nda;
    private final boolean copyOnClose;
    private final URL url;

    /**
     * Constructs a new NDArray containing the same data as a given one,
     *
     * @param   nda   the base NDArray
     * @param   mode  the access mode with which the data needs to be
     *                accessed; this controls the copying of data.
     *                If null, read/writability is taken from the 
     *                isReadable/isWritable flags of <code>nda</code>
     * @throws  IOException  if an I/O error occurs during the copy of 
     *                       data from the base NDArray
     */
    public CopyNDArray( NDArray nda, AccessMode mode ) throws IOException {
        super( nda );
        this.url = nda.getURL();

        /* Set the mode if we don't have one. */
        if ( mode == null ) {
            if ( nda.isReadable() && nda.isWritable() ) {
                mode = AccessMode.UPDATE;
            }
            else if ( nda.isReadable() ) {
                mode = AccessMode.READ;
            }
            else if ( nda.isWritable() ) {
                mode = AccessMode.WRITE;
            }
            else {
                throw new IllegalArgumentException(
                    "NDArray has neither read nor write access" );
            }
        }

        /* If read access is required, copy the data from the base array
         * into the new one. */
        if ( mode.isReadable() ) {
            NDArrays.copy( nda, this );
        }

        /* If write access is required, arrange to copy the data back to
         * the base array on close. */
        if ( mode.isWritable() ) {
            copyOnClose = true;
        }

        /* If write access is not required, we have no further use for the
         * base array - discard the reference. */
        else {
            copyOnClose = false;
            nda = null;
        }

        this.nda = nda;
    }

    public URL getURL() {
 
        /* We have the same data as base NDArray, so use the same URL. */
        return url;
    }

    public void close() throws IOException {

        /* If necessary, copy all data back to the base NDArray, close it,
         * and discard the reference. */
        if ( nda != null ) {
            if ( copyOnClose ) {
                NDArrays.copy( this, nda );
            }
            nda.close();
            nda = null;
        }
    }
}
