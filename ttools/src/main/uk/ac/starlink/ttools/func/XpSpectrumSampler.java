package uk.ac.starlink.ttools.func;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts Gaia continuous BP and RP spectra to XP sampled spectra.
 * This uses coefficients and algorithm derived from the DR3 paper
 * by Montegriffo et al.
 *
 * @see <a href="https://doi.org/10.1051/0004-6361/202243880"
 *         >Montegriffo et al., "Gaia Data Release 3: External calibration
 *          of BP/RP low-resolution spectroscopic data", A&amp;A 674, A3
 *          (2023)</a>
 * @see <a href="https://www.cosmos.esa.int/web/gaia/dr3-xpmergexpsampling"
 *         >Gaia DR3 XP-MERGE/XP-SAMPLING TABLES</a>
 */
class XpSpectrumSampler {

    private final long solutionId_;
    private final int nBase_;
    private final int nSample_;
    private final double[] wavelengths_;
    private final double[][] bpMatrix_;
    private final double[][] rpMatrix_;
    private final double[] bpMerge_;
    private final double[] rpMerge_;

    /** Instance for DR3 spectra. */
    public static final XpSpectrumSampler DR3 = createDr3Sampler();

    /** Non-working instance. */
    public static final XpSpectrumSampler DUMMY = createDummySampler();

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.func" );

    /**
     * Constructor.
     *
     * @param   solutionId  AGIS solution ID
     * @param   nBase  number of inverted bases for both BP and RP
     * @param   nSample  number of wavelength grid points in sampled spectra
     * @param   wavelengths  nSample-element array giving sampled
     *                       wavelength grid coordinates (nm)
     * @param   bpMatrix   matrix[nSample][nBase] giving inverted bases for BP
     *                     sampled onto the default grid
     * @param   rpMatrix   matrix[nSample][nBase] giving inverted bases for RP
     *                     sampled onto the default grid
     * @param   bpMerge   nSample-element array giving weights of BP elements
     *                    sampled onto the default grid
     * @param   rpMerge   nSample-element array giving weights of BP elements
     *                    sampled onto the default grid
     */
    public XpSpectrumSampler( long solutionId, int nBase, int nSample,
                              double[] wavelengths,
                              double[][] bpMatrix, double[][] rpMatrix,
                              double[] bpMerge, double[] rpMerge ) {
        solutionId_ = solutionId;
        nBase_ = nBase;
        nSample_ = nSample;
        wavelengths_ = wavelengths;
        bpMatrix_ = bpMatrix;
        rpMatrix_ = rpMatrix;
        bpMerge_ = bpMerge;
        rpMerge_ = rpMerge;
    }

    /**
     * Returns the number of base coefficients in continuous BP and RP
     * spectra.
     *
     * @return   base count
     */
    public int getBaseCount() {
        return nBase_;
    }

    /**
     * Returns the number of samples on the sampled wavelength grid.
     *
     * @param  wavelength sample count
     */
    public int getSampleCount() {
        return nSample_;
    }

    /**
     * Returns the wavelength coordinate values in nanometers
     * on the sampled grid
     *
     * @param   sampleCount-element array of wavelengths in nm
     */
    public double[] getWavelengths() {
        return wavelengths_.clone();
    }

    /**
     * Returns the Gaia solution ID for this sampler.
     *
     * @return  solution ID
     */
    public long getSolutionId() {
        return solutionId_;
    }

    /**
     * Calculates flux values sampled onto the default wavelength grid
     * from BP/RP base coefficient arrays as supplied from
     * the Gaia DataLink table XP_CONTINUOUS_MEAN_SPECTRUM.
     *
     * <p>The arguments correspond to columns
     * <code>bp/rp_coefficients</code>
     * in the DataLink table.
     *
     * @param  bpCoeffs  BP spectrum base coefficients
     * @param  rpCoeffs  RP spectrum base coefficients
     * @return   nSample-element merged XP sampled spectrum flux array
     *           in W.m**-2.nm**-1,
     *           or null for bad inputs
     */
    public double[] xpSampleFluxes( double[] bpCoeffs, double[] rpCoeffs ) {

        /* Validate inputs. */
        if ( bpCoeffs == null || bpCoeffs.length != nBase_ ||
             rpCoeffs == null || rpCoeffs.length != nBase_ ) {
            return null;
        }

        double[] sampled = new double[ nSample_ ];
        for ( int is = 0; is < nSample_; is++ ) {

            /* Resample coefficient to wavelength basis,
             * Montegriffo et al. eq (34). */
            double[] bpRow = bpMatrix_[ is ];
            double[] rpRow = rpMatrix_[ is ];
            double bpSample = 0;
            double rpSample = 0;
            for ( int ib = 0; ib < nBase_; ib++ ) {
                bpSample += bpCoeffs[ ib ] * bpRow[ ib ];
                rpSample += rpCoeffs[ ib ] * rpRow[ ib ];
            }

            /* Merge BP and RP. */
            sampled[ is ] = bpSample * bpMerge_[ is ]
                          + rpSample * rpMerge_[ is ];
        }
        return sampled;
    }

    /**
     * Calculates flux error values sampled onto the default wavelength grid
     * from BP/RP base coefficient errors and sub-diagonal correlation
     * matrices as supplied from the Gaia DataLink table
     * XP_CONTINUOUS_MEAN_SPECTRUM.
     *
     * <p>The arguments correspond to columns
     * <code>bp/rp_coefficient_errors</code> and
     * <code>bp/rp_coefficient_correlations</code>
     * in the DataLink table.
     *
     * @param  bpCoeffErrs  nBase-element array of BP coefficients
     * @param  bpCoeffCorrsSubdiag  correlation matrix for BP coefficients
     *                              in subdiagonal form
     * @param  rpCoeffErrs  nBase-element array of RP coefficients
     * @param  bpCoeffCorrsSubdiag  correlation matrix for RP coefficients
     *                              in subdiagonal form
     * @return  nSample-element merged XP sampled flux error array
     *          in W.m**-2.nm**-1,
     *          or null for bad inputs
     */
    public float[] xpSampleFluxErrors( float[] bpCoeffErrs,
                                       float[] bpCoeffCorrsSubdiag,
                                       float[] rpCoeffErrs,
                                       float[] rpCoeffCorrsSubdiag ) {

        /* Validate inputs. */
        int nCorrSubdiag = nBase_ * ( nBase_ - 1 ) / 2;
        if ( bpCoeffErrs == null || bpCoeffErrs.length != nBase_ ||
             rpCoeffErrs == null || rpCoeffErrs.length != nBase_ ||
             bpCoeffCorrsSubdiag == null ||
             bpCoeffCorrsSubdiag.length != nCorrSubdiag ||
             rpCoeffCorrsSubdiag == null ||
             rpCoeffCorrsSubdiag.length != nCorrSubdiag ) {
            return null;
        }

        /* Prepare square covariance matrices from subdiagonal correlation
         * matrices. */
        double[][] bpCoeffCovars =
            toCovariance( nBase_, bpCoeffCorrsSubdiag, bpCoeffErrs );
        double[][] rpCoeffCovars =
            toCovariance( nBase_, rpCoeffCorrsSubdiag, rpCoeffErrs );

        /* Resample coefficient to wavelength basis and retrieve the
         * leading diagonal to get errors in the new basis -
         * Montegriffo et al. eq (37). */
        double[] bpFluxVariances =
            congruenceLeadingDiagonal( bpMatrix_, bpCoeffCovars );
        double[] rpFluxVariances =
            congruenceLeadingDiagonal( rpMatrix_, rpCoeffCovars );

        /* Merge BP and RP errors in quadrature. */
        float[] fluxErrs = new float[ nSample_ ];
        for ( int is = 0; is < nSample_; is++ ) {
            double fluxVar =
                bpFluxVariances[ is ] * bpMerge_[ is ] * bpMerge_[ is ] +
                rpFluxVariances[ is ] * rpMerge_[ is ] * rpMerge_[ is ];
            fluxErrs[ is ] = (float) Math.sqrt( fluxVar );
        }
        return fluxErrs;
    }

    /**
     * Prepares a square symmetric covariance matrix from a
     * subdiagonal correlation matrix and associated errors.
     *
     * @param  n   linear dimension of matrix
     * @param  corrsSubdiag  (n*(n-1)/2)-element vector giving subdiagonal
     *                       elements of the correlation matrix
     *                       (1-element row, 2-element row, 3-element row, ...)
     * @param  errs   n-element array giving errors
     * @return  n*n covariance matrix
     */
    private static double[][] toCovariance( int n, float[] corrsSubdiag,
                                            float[] errs ) {
        double[][] covars = new double[ n ][ n ];
        int k = 0;
        for ( int i = 1; i < n; i++ ) {
            for ( int j = 0; j < i; j++ ) {
                double d = corrsSubdiag[ k++ ] * errs[ i ] * errs[ j ];
                covars[ j ][ i ] = d;
                covars[ i ][ j ] = d;
            }
        }
        for ( int i = 0; i < n; i++ ) {
            covars[ i ][ i ] = 1.0 * errs[ i ] * errs[ i ];
        }
        return covars;
    }

    /**
     * Behaves as if performing the congruence transformation
     * A * B * A^T for a square matrix B, and then returning the
     * leading diagonal of the result.
     *
     * @param   a  n*m matrix A
     * @param   b  n*n matrix B
     * @return  m-element vector giving leading diagonal of A*B*A^T
     */
    private static double[] congruenceLeadingDiagonal( double[][] a,
                                                       double[][] b ) {
        int m = a.length;
        int n = b.length;
        double[][] left = new double[ m ][ n ];
        for ( int i = 0; i < m; i++ ) {
            for ( int j = 0; j < n ; j++ ) {
                for ( int k = 0; k < n; k++ ) {
                    left[ i ][ j ] += a[ i ][ k ] * b[ k ][ j ];
                }
            }
        }
        double[] diag = new double[ m ];
        for ( int ij = 0; ij < m; ij++ ) {
            for ( int k = 0; k < n; k++ ) {
                diag[ ij ] += left[ ij ][ k ] * a[ ij ][ k ];
            }
        }
        return diag;
    }

    /**
     * Returns a BufferedReader to read from a given URL.
     *
     * @param  url  URL
     * @return  buffered reader
     */
    private static BufferedReader openStream( URL url )
            throws IOException {
        return new BufferedReader(
                       new InputStreamReader( url.openStream(),
                                              StandardCharsets.UTF_8 ) );
    }

    /**
     * Reads a fixed number of double values from a comma-separated string.
     *
     * @param  line  input text
     * @param  n   required number of values to read
     * @return   n-element array of values read
     * @throws  IllegalArgumentException  if wrong number of fields
     * @throws  NumberParseException  if they're not all numeric
     */
    private static double[] readDoubles( String line, int n ) {
        String[] fields = line.split( "," );
        if ( fields.length != n ) {
            throw new IllegalArgumentException( "Wrong number of fields" );
        }
        double[] values = new double[ n ];
        for ( int i = 0; i < n; i++ ) {
            values[ i ] = Double.parseDouble( fields[ i ] );
        }
        return values;
    }

    /**
     * Returns an instance that doesn't work.
     *
     * @return  dummy instance
     */
    private static XpSpectrumSampler createDummySampler() {
        return new XpSpectrumSampler( 0, 0, 0, new double[0],
                                      new double[0][0], new double[0][0],
                                      new double[0], new double[0] );
    }

    /**
     * Returns an instance for working with Gaia DR3 data.
     *
     * @return  Gaia DR3 instance
     */
    private static XpSpectrumSampler createDr3Sampler() {
        Class<?> clazz = XpSpectrumSampler.class;
        URL samplingUrl = clazz.getResource( "XpSampling_v375wiv142r.csv" );
        URL mergeUrl = clazz.getResource( "XpMerge_v375wiv142r.csv" );
        try {
            return createSampler( samplingUrl, mergeUrl );
        }
        catch ( IOException e ) {
            logger_.log( Level.SEVERE,
                         "Failed to load Gaia DR3 XP coefficients", e );
            return createDummySampler();
        }
    }

    /**
     * Returns a sampler instance based on XpSampling and XpMerge files
     * in the format supplied by Gaia DR3 Auxiliary Data.
     *
     * @param  samplingUrl  URL for XpSampling file
     * @param  mergeUrl    URL for XpMerge file
     * @return   XpSpectrumSampler instance
     * @throws  IOException  if the the resources are missing or broken
     */
    public static XpSpectrumSampler createSampler( URL samplingUrl,
                                                   URL mergeUrl )
            throws IOException {
        final long solutionId;
        final int nSample;
        final int nBase;
        final double[][] bpMatrix;
        final double[][] rpMatrix;
        try ( BufferedReader sIn = openStream( samplingUrl ) ) {
            String[] fields0 = sIn.readLine().split( "," );
            if ( fields0.length != 4 ) {
                throw new IllegalArgumentException( "Bad sampling header" );
            }
            solutionId = Long.parseLong( fields0[ 0 ] );
            nSample = Integer.parseInt( fields0[ 1 ] );
            int nBaseRp = Integer.parseInt( fields0[ 2 ] );
            int nBaseBp = Integer.parseInt( fields0[ 3 ] );
            if ( nBaseRp == nBaseBp ) {
                nBase = nBaseRp;
            }
            else {
                throw new IllegalArgumentException( "nBaseRp != nBaseBp" );
            }
            bpMatrix = new double[ nSample ][];
            for ( int is = 0; is < nSample; is++ ) {
                bpMatrix[ is ] = readDoubles( sIn.readLine(), nBase );
            }
            rpMatrix = new double[ nSample ][];
            for ( int is = 0; is < nSample; is++ ) {
                rpMatrix[ is ] = readDoubles( sIn.readLine(), nBase );
            }
        }
        catch ( RuntimeException e ) {
            throw new IOException( "Format error in XpSampling file "
                                 + samplingUrl, e );
        }
        final double[] wavelengths;
        final double[] bpMerge;
        final double[] rpMerge;
        try ( BufferedReader mIn = openStream( mergeUrl ) ) {
            String[] fields0 = mIn.readLine().split( "," );
            if ( fields0.length != 2 ) {
                throw new IllegalArgumentException( "Bad merge header" );
            }
            if ( Long.parseLong( fields0[ 0 ] ) != solutionId ) {
                throw new IllegalArgumentException( "Solution ID mismatch" );
            }
            if ( Integer.parseInt( fields0[ 1 ] ) != nSample ) {
                throw new IllegalArgumentException( "Sample count mismatch" );
            }
            wavelengths = readDoubles( mIn.readLine(), nSample );
            bpMerge = readDoubles( mIn.readLine(), nSample );
            rpMerge = readDoubles( mIn.readLine(), nSample );
        }
        catch ( RuntimeException e ) {
            throw new IOException( "Format error in XpMerge file "
                                 + mergeUrl, e );
        }
        return new XpSpectrumSampler( solutionId, nBase, nSample, wavelengths,
                                      bpMatrix, rpMatrix, bpMerge, rpMerge );
    }
}
