package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.AccessImpl;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Requirements;

/**
 * Implements bulk data access for the case in which the arrays are 
 * inherently unified.  A AccessBulkDataImpl can be constructed from
 * an {@link NdxAccessMaker} object which knows its default array
 * characteristics (shape, type etc) and which can supply
 * {@link NdxAccess} objects on demand.
 * This will typically be some object wrapping (and modifying) the 
 * bulk data of an underlying Ndx.
 */
public class AccessBulkDataImpl implements BulkDataImpl {

    private final NdxAccessMaker acmaker;

    public AccessBulkDataImpl( NdxAccessMaker acmaker ) {
        this.acmaker = acmaker;
    }

    public boolean hasVariance() {
        return acmaker.hasVariance();
    }

    public boolean hasQuality() {
        return acmaker.hasQuality();
    }

    public NDArray getImage() {
        ArrayImpl impl = new NdxAccessMakerArrayImpl( acmaker ) {
            public AccessImpl getAccess() throws IOException {
                final NdxAccess acc = 
                    acmaker.getAccess( null, true, false, false, (byte) 0 );
                return new AccessImpl() {
                    public void setOffset( long off ) throws IOException {
                        acc.setOffset( off );
                    }
                    public void read( Object buffer, int start, int size )
                            throws IOException {
                        acc.read( buffer, null, null, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        acc.write( buffer, null, null, start, size );
                    }
                    public void close() throws IOException {
                        acc.close();
                    }
                };
            }
        };
        return new BridgeNDArray( impl );
    }

    public NDArray getVariance() {
        ArrayImpl impl = new NdxAccessMakerArrayImpl( acmaker ) {
            public AccessImpl getAccess() throws IOException {
                final NdxAccess acc = 
                    acmaker.getAccess( null, false, true, false, (byte) 0 );
                return new AccessImpl() {
                    public void setOffset( long off ) throws IOException {
                        acc.setOffset( off );
                    }
                    public void read( Object buffer, int start, int size )
                            throws IOException {
                        acc.read( null, buffer, null, start, size );
                    }
                    public void write( Object buffer, int start, int size )
                            throws IOException {
                        acc.write( null, buffer, null, start, size );
                    }
                    public void close() throws IOException {
                        acc.close();
                    }
                };
            }
        };
        return new BridgeNDArray( impl );
    }

    public NDArray getQuality() {
        ArrayImpl impl = new NdxAccessMakerArrayImpl( acmaker ) {
            public AccessImpl getAccess() throws IOException {
                final NdxAccess acc = 
                    acmaker.getAccess( null, false, false, true, (byte) 0 );
                return new AccessImpl() {
                    public void setOffset( long off ) throws IOException {
                        acc.setOffset( off );
                    }
                    public void read( Object buffer, int start, int size ) 
                            throws IOException {
                        if ( ! buffer.getClass().getName().equals( "[B" ) ) {
                            throw new IllegalArgumentException( 
                                "Buffer " + buffer + " not of type byte[]" );
                        }
                        acc.read( null, null, (byte[]) buffer, start, size );
                    }
                    public void write( Object buffer, int start, int size ) 
                            throws IOException {
                        if ( ! buffer.getClass().getName().equals( "[B" ) ) {
                            throw new IllegalArgumentException(
                                "Buffer " + buffer + " not of type byte[]" );
                        }
                        acc.write( null, null, (byte[]) buffer, start, size );
                    }
                    public void close() throws IOException {
                        acc.close();
                    }
                };
            }
        };
        return new BridgeNDArray( impl );
    }

    public NdxAccess getAccess( Requirements req, boolean wantImage, 
                                boolean wantVariance, boolean wantQuality, 
                                byte badbits ) throws IOException {
        return acmaker.getAccess( req, wantImage, wantVariance, wantQuality,
                                  badbits );
    }
}
