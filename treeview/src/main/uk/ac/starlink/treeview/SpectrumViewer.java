package uk.ac.starlink.treeview;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataImpl;
import uk.ac.starlink.splat.data.MEMSpecDataImpl;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NDArrayFactory;
import uk.ac.starlink.hdx.array.NDShape;
import uk.ac.starlink.hdx.array.Requirements;
import uk.ac.starlink.hdx.array.Type;

class SpectrumViewer extends DivaPlot {

    public SpectrumViewer( HDSObject hobj, long origin, String name )
            throws HDSException, SplatException {
        super( new SpecDataComp(
                   new SpecData( 
                       getSpecDataImpl( hobj, origin, name ) ) ) );
    }

    public SpectrumViewer( Buffer niobuf, long origin, String name ) 
            throws SplatException {
        super( new SpecDataComp(
                   new SpecData(
                       getSpecDataImpl( niobuf, origin, name ) ) ) );
                
    }

    public SpectrumViewer( NDArray nda, String name )
            throws SplatException, IOException {
        super( new SpecDataComp(
                   new SpecData(
                       getSpecDataImpl( nda, name ) ) ) );
    }

    private static SpecDataImpl getSpecDataImpl( double[] dataArray, 
                                                 long origin,
                                                 String name ) {
        int nel = dataArray.length;
        final double[] dArray;
        final double[] cArray;

        int ngood = 0;
        for ( int i = 0; i < nel; i++ ) {
            if ( ! Double.isNaN( dataArray[ i ] ) ) ngood++;
        }
        if ( ngood == nel ) {
            dArray = dataArray;
        }
        else {
            dArray = new double[ ngood ];
            for ( int i = 0, j = 0; i < nel; i++ ) {
                if ( ! Double.isNaN( dataArray[ i ] ) ) {
                    dArray[ j++ ] = dataArray[ i ];
                }
            }
        }
        cArray = new double[ ngood ];
        for ( int i = 0; i < ngood; i++ ) {
            cArray[ i ] = (double) origin + i;
        }
        return new MEMSpecDataImpl( name ) {
            { setData( dArray, cArray ); }
        };
    }

    private static SpecDataImpl getSpecDataImpl( HDSObject hobj, long origin,
                                                 String name )
            throws HDSException {
        int nel = (int) Math.min( hobj.datSize(), (long) Integer.MAX_VALUE );
        String type = hobj.datType();
        final double[] dataArray;

        if ( type.equals( "_REAL" ) || type.equals( "_DOUBLE" ) ) {
            double badval;
            if ( type.equals( "_REAL" ) ) {
                badval = (double) Float.intBitsToFloat( 0xff7fffff );
            }
            else if ( type.equals( "_DOUBLE" ) ) {
                badval = Double.longBitsToDouble( 0xffefffffffffffffL );
            }
            else {
                throw new AssertionError();
            }
            dataArray = hobj.datGetvd();
            int npix = dataArray.length;
            for ( int i = 0; i < npix; i++ ) {
                if ( dataArray[ i ] == badval ) {
                    dataArray[ i ] = Double.NaN;
                }
            }
        }
        else {
            int[] intArray = hobj.datGetvi();
            int badval;
            if ( type.equals( "_BYTE" ) ) {
                badval = (int) (byte) 0x80;
            }
            else if ( type.equals( "_UBYTE" ) ) {
                badval = (int) (byte) 0xff;
            }
            else if ( type.equals( "_WORD" ) ) {
                badval = (int) (short) 0x8000;
            }
            else if ( type.equals( "_UWORD" ) ) {
                badval = (int) (short) 0xffff;
            }
            else if ( type.equals( "_INTEGER" ) ) {
                badval = 0x80000000;
            }
            else {
                // assert false;
                throw new Error( "Unknown type" );
            }
            int npix = intArray.length;
            dataArray = new double[ npix ];
            for ( int i = 0; i < npix; i++ ) {
                int val = intArray[ i ];
                dataArray[ i ] = ( val == badval ) ? Double.NaN : (double) val;
            }
        }

        return getSpecDataImpl( dataArray, origin, name );
    }

    private static SpecDataImpl getSpecDataImpl( Buffer niobuf, long origin,
                                                 String name ) {
        final double[] dataArray;
        if ( niobuf instanceof ByteBuffer ) {
            ByteBuffer buf = (ByteBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else if ( niobuf instanceof ShortBuffer ) {
            ShortBuffer buf = (ShortBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else if ( niobuf instanceof IntBuffer ) {
            IntBuffer buf = (IntBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else if ( niobuf instanceof LongBuffer ) {
            LongBuffer buf = (LongBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else if ( niobuf instanceof FloatBuffer ) {
            FloatBuffer buf = (FloatBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else if ( niobuf instanceof DoubleBuffer ) {
            DoubleBuffer buf = (DoubleBuffer) niobuf;
            int nel = buf.remaining();
            dataArray = new double[ nel ];
            for ( int i = 0; i < nel; dataArray[ i++ ] = (double) buf.get() );
        }
        else {
            // assert false;
            dataArray = null;
        }
        return getSpecDataImpl( dataArray, origin, name );
    }

    private static SpecDataImpl getSpecDataImpl( NDArray nda, String name )
            throws IOException {
        Requirements req = new Requirements( Requirements.Mode.READ )
                          .setType( Type.DOUBLE )
                          .setStart( 0L );
        nda = NDArrayFactory.toRequiredNDArray( nda, req );
        NDShape shape = nda.getShape();
        int nel = (int) shape.getNumPixels();
        double[] dataArray = new double[ nel ];
        nda.read( dataArray, 0, nel );
        return getSpecDataImpl( dataArray, shape.getOrigin()[ 0 ], name );
    }

}
