package uk.ac.starlink.treeview;

import java.io.IOException;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataImpl;
import uk.ac.starlink.splat.data.MEMSpecDataImpl;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;

class SpectrumViewer extends DivaPlot {

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


    private static SpecDataImpl getSpecDataImpl( NDArray nda, String name )
            throws IOException {
        Requirements req = new Requirements( AccessMode.READ )
                          .setType( Type.DOUBLE );
        nda = NDArrays.toRequiredArray( nda, req );
        NDShape shape = nda.getShape();
        int nel = (int) shape.getNumPixels();
        double[] dataArray = new double[ nel ];
        ArrayAccess acc = nda.getAccess();
        acc.read( dataArray, 0, nel );
        acc.close();
        return getSpecDataImpl( dataArray, shape.getOrigin()[ 0 ], name );
    }

}
