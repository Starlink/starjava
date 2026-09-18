package uk.ac.starlink.ttools.func;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class XpTest extends TestCase {

    public void testSample() throws IOException {
        StarTableFactory tfact = new StarTableFactory( false );
        tfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
        String baseName = "Gaia_DR3_65196370900147968";
        Class<?> clazz = getClass();
        StarTable continuous =
            tfact.makeStarTable(
                new URLDataSource( clazz.getResource( baseName
                                                    + "-continuous.vot" ) ),
                "votable" );
        StarTable sampled =
            tfact.makeStarTable(
                new URLDataSource( clazz.getResource( baseName
                                                    + "-sampled.vot" ) ),
                "votable" );
        Object[] crow0;
        try ( RowSequence crseq = continuous.getRowSequence() ) {
            crseq.next();
            crow0 = crseq.getRow();
            assertFalse( crseq.next() );
        }
        double[] bpCoeffs = (double[]) crow0[ 9 ];
        float[] bpCoeffErrs = (float[]) crow0[ 10 ];
        float[] bpCoeffCorrs = (float[]) crow0[ 11 ];
        double[] rpCoeffs = (double[]) crow0[ 21 ];
        float[] rpCoeffErrs = (float[]) crow0[ 22 ];
        float[] rpCoeffCorrs = (float[]) crow0[ 23 ];

        XpSpectrumSampler sampler = XpSpectrumSampler.DR3;
        double[] wavelengths = sampler.getWavelengths();
        double[] xpCalc = sampler.xpSampleFluxes( bpCoeffs, rpCoeffs );
        float[] xpErrCalc =
            sampler.xpSampleFluxErrors( bpCoeffErrs, bpCoeffCorrs,
                                        rpCoeffErrs, rpCoeffCorrs );
        assertArrayEquals( wavelengths, Gaia.XP_SAMPLED_WAVELENGTHS_DR3 );
        assertArrayEquals( xpCalc,
                           Gaia.xpSampledFluxesDr3( bpCoeffs, rpCoeffs ) );
        assertArrayEquals( xpErrCalc,
                           Gaia.xpSampledErrorsDr3(bpCoeffErrs, bpCoeffCorrs,
                                                   rpCoeffErrs, rpCoeffCorrs ));
        int irow = 0;
        try ( RowSequence csseq = sampled.getRowSequence() ) {
            while ( csseq.next() ) {
                Object[] row = csseq.getRow();
                double lambda = ((Number) row[ 0 ]).doubleValue();
                double flux = ((Number) row[ 1 ]).doubleValue();
                double fluxErr = ((Number) row[ 2 ]).doubleValue();
                assertEquals( lambda, wavelengths[ irow ] );
                assertEquals( 1.0, flux / xpCalc[ irow ], 1e-5 );
                assertEquals( 1.0, fluxErr / xpErrCalc[ irow ], 1e-5 );
                irow++;
            }
        }
        assertEquals( wavelengths.length, irow );
    }
}
