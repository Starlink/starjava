package uk.ac.starlink.ttools.func;

import gaia.cu9.tools.parallax.PDF.ExpDecrVolumeDensityDEM;
import gaia.cu9.tools.parallax.PDF.PDF;
import gaia.cu9.tools.parallax.util.CdfIntegration;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import uk.ac.starlink.dpac.math.Edsd;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.TablePipe;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.util.URLDataSource;

public class GaiaTest extends TestCase {

    /**
     * select top 50
     *    ra,dec,parallax,pmra,pmdec,radial_velocity,
     *    ra_error,dec_error,parallax_error,
     *    pmra_error,pmdec_error,radial_velocity_error,
     *    ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,
     *    dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,
     *    parallax_pmra_corr,parallax_pmdec_corr,
     *    pmra_pmdec_corr,
     *    random_index,ref_epoch,
     *    astrometric_parameters(ra,dec,parallax,pmra,pmdec,radial_velocity)
     *       as ap,
     *    astrometric_parameter_error(
     *          ra_error,dec_error,parallax_error,pmra_error,pmdec_error,
     *          ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,
     *          dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,
     *          parallax_pmra_corr,parallax_pmdec_corr,
     *          pmra_pmdec_corr,
     *          parallax,radial_velocity,radial_velocity_error)
     *    as aperr,
     *    epoch_prop(ra,dec,parallax,pmra,pmdec,radial_velocity,2015.5,2015.5)
     *       as ga0,
     *    epoch_prop(ra,dec,parallax,pmra,pmdec,radial_velocity,2015.5,3015.5)
     *       as ga1000,
     *    epoch_prop_error(
     *       astrometric_parameters(ra,dec,parallax,pmra,pmdec,radial_velocity),
     *       astrometric_parameter_error(
     *          ra_error,dec_error,parallax_error,pmra_error,pmdec_error,
     *          ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,
     *          dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,
     *          parallax_pmra_corr,parallax_pmdec_corr,
     *          pmra_pmdec_corr,
     *          parallax,radial_velocity,radial_velocity_error),
     *       2015.5,2015.5) as gc0,
     *    epoch_prop_error(
     *       astrometric_parameters(ra,dec,parallax,pmra,pmdec,radial_velocity),
     *       astrometric_parameter_error(
     *          ra_error,dec_error,parallax_error,pmra_error,pmdec_error,
     *          ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,
     *          dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,
     *          parallax_pmra_corr,parallax_pmdec_corr,
     *          pmra_pmdec_corr,
     *          parallax,radial_velocity,radial_velocity_error),
     *       2015.5,3015.5) as gc1000
     * from user_dr2int6.gaia_source
     * where parallax is not null
     * and radial_velocity is not null
     * order by random_index asc
     */
    private static final String tname = "ap2b.vot";

    public GaiaTest() {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testDistanceGrid() {
        // This range of quantities was obtained by eyeballing the plx/eplx
        // distribution from DR2:
        //    select top 100000
        //           parallax, parallax_error, parallax_over_error, random_index
        //    from user_dr2int6.gaia_source
        //    where parallax is not null and parallax_error is not null
        //    order by random_index
        double absPlxMin = 1e-4;
        double absPlxMax = 40;
        double eplxMin = 0.015;
        double eplxMax = 4;
        int np = 10;
        int ne = 10;
        for ( int ip = 0; ip <= np; ip++ ) {
            double absPlx =
                PlotUtil.scaleValue( absPlxMin, absPlxMax,
                                     ip / (double) np, true );
            for ( int ie = 0; ie <= ne; ie++ ) {
                double eplx =
                    PlotUtil.scaleValue( eplxMin, eplxMax,
                                         ie / (double) ne, true );

                compareDistances( absPlx, eplx, 1350 );
                compareDistances( absPlx, eplx, 110 );
                compareDistances( -absPlx, eplx, 1350 );
                compareDistances( -absPlx, eplx, 110 );
            }
        }
    }

    private void compareDistances( double plxMas, double eplxMas, double lPc ) {
        double funcBest = Gaia.distanceEstimateEdsd( plxMas, eplxMas, lPc );
        double[] funcQuantiles =
            Gaia.distanceQuantilesEdsd( plxMas, eplxMas, lPc,
                                        0.05, 0.50, 0.95 );

        PDF pdf = new ExpDecrVolumeDensityDEM( plxMas, eplxMas, lPc * 0.001 )
                 .getDistancePDF();
        double cu9BestPc = pdf.getBestEstimation() * 1000;
        CdfIntegration cdfint = new CdfIntegration();
        // Defaults taken from DistanceEstimator class.
        double rMin = 0.001;  // kpc
        double rMax = 100;    // kpc
        int nPoints = 16380;
        boolean logAxis = true;
        boolean normalisePdf = false;
        boolean integrateToInfinite = true;
        double[][] cdf = cdfint.getCdf( pdf, logAxis, nPoints, rMin, rMax,
                                        normalisePdf, integrateToInfinite );
        double[] cu9Quantiles = cdfint.getPercentiles( cdf, 0.05, 0.50, 0.95 );
        double cu9PdfMode = pdf.getUnnormalizedProbabilityAt(cu9BestPc * .001);
        double funcPdfMode = new Edsd( plxMas, eplxMas, lPc * .001 )
                            .getPdf().f( cu9BestPc * .001 );
        boolean cu9Ok = !Double.isNaN( cu9PdfMode )
                     && !Double.isInfinite( cu9PdfMode )
                     && !(cu9PdfMode == 0);
        boolean funcOk = !Double.isNaN( funcPdfMode )
                      && !Double.isInfinite( funcPdfMode )
                      && !(funcPdfMode == 0);
        assertTrue( funcOk );

        // Don't test comparison in this region, since the CU9 code falls
        // foul of numerical issues: the exponent in the
        // getUnnormalizedProbabilityAt method gets so large
        // that the exponential returns NaN or Infinity.
        // The Edsd code works round this.
        // There are in any case not many measurements in this region in DR2.
        // Those that do do exist (large negative parallax, small error)
        // don't make much sense anyway.
        boolean cu9FailRegion =
            plxMas < 0 && Math.abs( eplxMas / plxMas ) < 0.05;
        assertTrue( cu9Ok || cu9FailRegion );

        // Test comparisons in other cases.
        for ( int i = 0; i < 3; i++ ) {
            double funcq = funcQuantiles[ i ];
            if ( cu9Ok ) {
                double cu9q = cu9Quantiles[ i ] * 1000.;
                double rdiff = Math.abs( funcq - cu9q ) / cu9q;
                assertEquals( "plx=" + plxMas + "; eplx=" + eplxMas
                                     + "; l=" + lPc
                            + "; cu9=" + cu9q + "; funcq=" + funcq,
                              0.0, rdiff, 5e-4 );
            }
        }
    }

    public void testEpoch() throws IOException, TaskException {
        URL turl = GaiaTest.class.getResource( tname );
        StarTable table = new StarTableFactory()
                         .makeStarTable( new URLDataSource( turl ) );

        // When this test was initially committed to a public repository,
        // the DR2 test data was still under embargo.
        // It has been used privately to run these tests,
        // but in the initial public commit, all data rows have been
        // removed from it.  Following DR2 the data rows will be restored.
        if ( table.getRowCount() == 0 ) {
            System.err.println( "No meaningful epochProp tests performed"
                             +  " - requires embargoed DR2 data" );
        }

        // These manipulations are preparing columns in the same form
        // as the results of the func.Gaia epochProp and epochPropErr
        // functions under test, arrived at by different means.
        String cmd = DocUtils.join( new String[] {
            "colmeta -shape 6 ap;",
            "colmeta -shape 21 aperr;",
            "colmeta -shape 6 ga0;",
            "colmeta -shape 21 gc0;",
            "colmeta -shape 6 ga1000;",
            "colmeta -shape 21 gc1000;",
            "addcol zeta rvKmsToMasyr(radial_velocity,parallax)",
            "addcol ezeta hypot(parallax*radial_velocity_error,"
                             + "radial_velocity*parallax_error,"
                             + "radial_velocity_error*parallax_error)/AU_YRKMS",
            "addcol czeta (parallax_error/ezeta)*(radial_velocity/AU_YRKMS)",
            "addcol -shape 6 ap_t "
              + "array(ra,dec,parallax,pmra,pmdec,zeta);",
            "addcol -shape 21 aperr_t array("
              + "ra_error,dec_error,parallax_error,pmra_error,pmdec_error,"
                  + "ezeta,"
              + "ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,"
                  + "czeta*ra_parallax_corr,"
              + "dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,"
                  + "czeta*dec_parallax_corr,"
              + "parallax_pmra_corr,parallax_pmdec_corr,czeta*1,"
              + "pmra_pmdec_corr,czeta*parallax_pmra_corr,"
              + "czeta*parallax_pmdec_corr);",
            "addcol -shape 6 ast6_0 "
              + "array(ra,dec,parallax,pmra,pmdec,radial_velocity);",
            "addcol -shape 22 ast22_0 array("
              + "ra,dec,parallax,pmra,pmdec,radial_velocity,"
              + "ra_error,dec_error,parallax_error,pmra_error,pmdec_error,"
              + "radial_velocity_error,"
              + "ra_dec_corr,ra_parallax_corr,ra_pmra_corr,ra_pmdec_corr,"
              + "dec_parallax_corr,dec_pmra_corr,dec_pmdec_corr,"
              + "parallax_pmra_corr,parallax_pmdec_corr,"
              + "pmra_pmdec_corr);",
            "addcol -shape 6 ast6_t0 epochProp(0,ast6_0);",
            "addcol -shape 22 ast22_t0 epochPropErr(0,ast22_0);",
            "addcol -shape 6 ast6_t1000 epochProp(1000,ast6_0);",
            "addcol -shape 22 ast22_t1000 epochPropErr(1000,ast22_0);",
            "addcol -shape 6 ast6_g0 array("
              + "ga0[0],ga0[1],ga0[2],ga0[3],ga0[4],"
              + "rvMasyrToKms(ga0[5],ga0[2]));",
            "addcol -shape 22 ast22_g0 array("
              + "ga0[0],ga0[1],ga0[2],ga0[3],ga0[4],"
              + "rvMasyrToKms(ga0[5],ga0[2]),"
              + "gc0[0],gc0[1],gc0[2],gc0[3],gc0[4],"
              + "sqrt((square(AU_YRKMS*gc0[5])-"
                   +  "square(rvMasyrToKms(ga0[5],ga0[2])*gc0[2]))/"
                   + "(ga0[2]*ga0[2]+gc0[2]*gc0[2])),"
              + "gc0[6],gc0[7],gc0[8],gc0[9],"
              + "gc0[11],gc0[12],gc0[13],"
              + "gc0[15],gc0[16],"
              + "gc0[18]);",
            "addcol -shape 6 ast6_g1000 array("
              + "ga1000[0],ga1000[1],ga1000[2],ga1000[3],ga1000[4],"
              + "rvMasyrToKms(ga1000[5],ga1000[2]));",
            "addcol -shape 22 ast22_g1000 array("
              + "ga1000[0],ga1000[1],ga1000[2],ga1000[3],ga1000[4],"
              + "rvMasyrToKms(ga1000[5],ga1000[2]),"
              + "gc1000[0],gc1000[1],gc1000[2],gc1000[3],gc1000[4],"
              + "sqrt((square(AU_YRKMS*gc1000[5])-"
                   +  "square(rvMasyrToKms(ga1000[5],ga1000[2])*gc1000[2]))/"
                   + "(ga1000[2]*ga1000[2]+gc1000[2]*gc1000[2])),"
              + "gc1000[6],gc1000[7],gc1000[8],gc1000[9],"
              + "gc1000[11],gc1000[12],gc1000[13],"
              + "gc1000[15],gc1000[16],"
              + "gc1000[18]);",

            "addcol -shape 6 dist_ap subtract(ap,ap_t);",
            "addcol -shape 21 dist_aperr subtract(aperr,aperr_t);",

            "addcol -shape 6 dist6_p0 subtract(ga0,ap);",
            "addcol -shape 21 dist21_p0 subtract(gc0,aperr);",

            "addcol -shape 6 dist6_t0 subtract(ast6_t0,ast6_0);",
            "addcol -shape 22 dist22_t0 subtract(ast22_t0,ast22_0);",
            "addcol -shape 6 rdist6_t0 divide(dist6_t0,ast6_0);",
            "addcol -shape 22 rdist22_t0 divide(dist22_t0,ast22_0);",

            "addcol -shape 6 dist6_t1000 subtract(ast6_t1000,ast6_0);",
            "addcol -shape 6 dist6_g1000 subtract(ast6_g1000,ast6_0);",
            "addcol -shape 22 dist22_t1000 subtract(ast22_t1000,ast22_0);",
            "addcol -shape 22 dist22_g1000 subtract(ast22_g1000,ast22_0);",

            "addcol -shape 6 diff6_1000 subtract(dist6_t1000,dist6_g1000);",
            "addcol -shape 22 diff22_1000 subtract(dist22_t1000,dist22_g1000);",
            "addcol -shape 6 rdiff6_1000 divide(diff6_1000,dist6_g1000);",
            "addcol -shape 22 rdiff22_1000 divide(diff22_1000,dist22_g1000);",

            "addcol -shape 6 dist6_g0 subtract(ast6_g0,ast6_0);",
            "addcol -shape 22 dist22_g0 subtract(ast22_g0,ast22_0);",
            "addcol -shape 6 rdist6_g0 divide(dist6_g0,ast6_0);",
            "addcol -shape 22 rdist22_g0 divide(dist22_g0,ast22_0);",
        } );
        MapEnvironment env = new MapEnvironment()
           .setValue( "in", table )
           .setValue( "cmd", cmd );
        new TablePipe().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
        new StarTableOutput().writeStarTable( result, "out.vot", "votable" );

        // This tests the results of the GACS ASTROMETRIC_PARAMETERS
        // and ASTROMETRIC_PARAMETER_ERROR functions with what I think
        // they ought to be.
        assertArrayColumnNearZero( result, "dist_ap", 1e-13 );
        assertArrayColumnNearZero( result, "dist_aperr", 1e-13 );

        // This at least tests that propagation zero years makes zero
        // difference.  It should pick up some basic typos in the
        // conversion harness functions.
        assertArrayColumnNearZero( result, "rdist6_t0", 1e-13 );
        assertArrayColumnNearZero( result, "rdist22_t0", 1e-12 );

        // This tests the same thing for the GACS propagation functions
        // that take the same input and output arrays.
        assertArrayColumnNearZero( result, "dist6_p0", 1e-13 );
        assertArrayColumnNearZero( result, "dist21_p0", 1e-13 );

        // This tests whether propagating zero years using the GACS
        // propagation functions with the I/O arrays I have defined
        // makes zero difference; it makes sure that I am invoking
        // them in a way that I understand.
        assertArrayColumnNearZero( result, "rdist6_g0", 1e-13 );
        assertArrayColumnNearZero( result, "rdist22_g0", 1e-12 );

        // Now test that the func.Gaia propagation functions give the
        // same results as the GACS ones when propagating to an epoch
        // that is actually distinct.
        assertArrayColumnNearZero( result, "rdiff6_1000", 1e-8 );
        assertArrayColumnNearZero( result, "rdiff22_1000", 1e-8 );
    }

    private void assertArrayColumnNearZero( StarTable t, String cname,
                                            double tol )
            throws IOException {
        int icol = -1;
        for ( int ic = 0; ic < t.getColumnCount(); ic++ ) {
            if ( cname.equals( t.getColumnInfo( ic ).getName() ) ) {
                icol = ic;
            }
        }
        for ( RowSequence rseq = t.getRowSequence(); rseq.next();
              rseq.close() ) {
            double[] arr = (double[]) rseq.getCell( icol );
            for ( int i = 0; i < arr.length; i++ ) {
                assertEquals( "element #" + i, 0., arr[ i ], tol );
            }
        }
    }
}
