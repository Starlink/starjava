package uk.ac.starlink.ttools.func;

import gaia.cu9.tools.parallax.PDF.ExpDecrVolumeDensityDEM;
import gaia.cu9.tools.parallax.PDF.PDF;
import gaia.cu9.tools.parallax.util.CdfIntegration;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.dpac.math.Edsd;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.TablePipe;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.util.LogUtils;
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
     *    random_index,ref_epoch,l,b,ecl_lon,ecl_lat,
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
     * from gaiadr2.gaia_source
     * where parallax is not null
     * and radial_velocity is not null
     * order by random_index asc
     */
    private static final String tname = "ap2b.vot";
    private StarTable table_;

    public GaiaTest() throws IOException {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
        URL turl = GaiaTest.class.getResource( tname );
        table_ = new StarTableFactory()
                .makeStarTable( new URLDataSource( turl ) );
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
                                     ip / (double) np, Scale.LOG );
            for ( int ie = 0; ie <= ne; ie++ ) {
                double eplx =
                    PlotUtil.scaleValue( eplxMin, eplxMax,
                                         ie / (double) ne, Scale.LOG );

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
           .setValue( "in", table_ )
           .setValue( "cmd", cmd );
        new TablePipe().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }

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

    public void testEpochProp() {

        // This tests against the examples included for ivo_epoch_prop
        // in the UDF Catalogue Endorsed Note
        // https://www.ivoa.net/documents/udf-catalogue/.
        // These are from text introduced at
        // https://github.com/ivoa-std/udf-catalogue/pull/20
        // which introduced changes in NaN/NULL handling.
        // Not in PEN (or master) yet so subject to change,
        // but I'm expecting them to be accepted.
        double tYr = 1992.25 - 2016.0;
        double NaN = Double.NaN;
        checkProp( tYr,
            7.606083572, 11.79044105, 125, 300, -428.8, 52.51,
            7.6040614046279735, 11.793270382827929, 125.01993165584682,
            300.09877325973605, -428.934593565712, 52.50880381775256 );
        checkProp( tYr,
            7.606083572, 11.79044105, 125, NaN, NaN, NaN,
            7.606083572, 11.79044105, 125, NaN, NaN, NaN );
        checkProp( tYr,
            7.606083572, 11.79044105, NaN, 300, -428.8, 52.51,
            7.604061727024453, 11.79326993174991, NaN,
            300.0030911918108, -428.79783514643105, 52.51 );
        // This corner case is in the UDF docs, but to me it's arguable what
        // the correct behaviour is, so I'm not going to reproduce it.
        // checkProp( tYr,   // this one is arguable
        //     7.606083572, 11.79044105, 125, 21, NaN, NaN,
        //     7.606083572, 11.79044105, 125.0, NaN, NaN, NaN );
        checkProp( tYr,
            7.606083572, 11.79044105, 125, 300, -428.8, NaN,
            7.604061727024712, 11.793269931749549, 124.9999997730655,
            300.0030911152833, -428.79783503705033, NaN  );

        // This one isn't from the UDF docs.
        checkProp( tYr,
            7.606083572, 11.79044105, 125, NaN, NaN, 21,
            7.606083572, 11.79044105, 125, NaN, NaN, 21 );
    }

    private void checkProp( double tYr,
                            double inLon, double inLat, double inPlx,
                            double inPmlon, double inPmlat, double inRv,
                            double outLon, double outLat, double outPlx,
                            double outPmlon, double outPmlat, double outRv ){
        double[] out6 =
            Gaia.epochProp( tYr, new double[] { inLon, inLat, inPlx,
                                                inPmlon, inPmlat, inRv } );
        assertEquals( out6[ 0 ], outLon, 1e-6 );
        assertEquals( out6[ 1 ], outLat, 1e-6 );
        assertEquals( out6[ 2 ], outPlx, 1e-6 );
        assertEquals( out6[ 3 ], outPmlon, 1e-6 );
        assertEquals( out6[ 4 ], outPmlat, 1e-6 );
        assertEquals( out6[ 5 ], outRv, 1e-6 );
    }

    public void testXyz() throws IOException, TaskException {

        // Perform various tests including propagating the velocities
        // in astrometric coordinate space (epochProp function) and in
        // 6-d Cartesian phase space and comparing the results.
        // This should be a good test of the algebra in both coordinate
        // systems, since the implementations are entirely separate.
        String dT = "100";  // years
        String cmd = DocUtils.join( new String[] {
            "addcol radius 1000./parallax",
            "addcol eXyz polarXYZ(ra,dec,radius)",
            "addcol gXyz polarXYZ(l,b,radius)",
            "addcol lXyz polarXYZ(ecl_lon,ecl_lat,radius)",
            "addcol aeXyz astromXYZ(ra,dec,parallax)",

            "addcol cgXyz icrsToGal(eXyz)",
            "addcol clXyz icrsToEcl(eXyz)",
            "addcol gceXyz galToIcrs(gXyz)",
            "addcol lceXyz eclToIcrs(lXyz)",

            "addcol dgeXyz subtract(eXyz,gceXyz)",
            "addcol dleXyz subtract(eXyz,lceXyz)",
            "addcol dgXyz subtract(gXyz,cgXyz)",
            "addcol dlXyz subtract(lXyz,clXyz)",
            "addcol daeXyz subtract(eXyz,aeXyz)",

            "addcol asix0 array(ra,dec,parallax,pmra,pmdec,radial_velocity)",
            "addcol asix1 epochProp(" + dT + ",asix0)",
            "addcol uvw astromUVW(asix0)",
            "addcol uvw1 astromUVW(asix1)",
            "addcol dUvw subtract(uvw1,uvw)",

            "addcol scXyz multiply(uvw," + dT + "/PC_YRKMS)",
            "addcol aXyz1 astromXYZ(asix1[0],asix1[1],asix1[2])",
            "addcol spXyz subtract(aXyz1,eXyz)",
            "addcol cXyz1 add(eXyz,spXyz)",
            "addcol rc hypot(scXyz)",
            "addcol rp hypot(spXyz)",
            "addcol dR array((rc-rp)/rp)",
            "addcol dS multiply(subtract(scXyz,spXyz),1./rp)",

            "addcol gUvw icrsToGal(uvw)",
            "addcol sgXyz multiply(gUvw," + dT + "/PC_YRKMS)",
            "addcol gXyz1 add(cgXyz,sgXyz)",
            "addcol cgXyz1 icrsToGal(cXyz1)",
            "addcol dgXyz1 subtract(gXyz1,cgXyz1)",
        } );
        MapEnvironment env = new MapEnvironment()
           .setValue( "in", table_ )
           .setValue( "cmd", cmd );
        new TablePipe().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }

        assertArrayColumnNearZero( result, "dgeXyz", 1e-8 );    // pc
        assertArrayColumnNearZero( result, "dleXyz", 1e-8 );    // pc
        assertArrayColumnNearZero( result, "dgXyz", 1e-8 );     // pc
        assertArrayColumnNearZero( result, "dlXyz", 1e-8 );     // pc
        assertArrayColumnNearZero( result, "dUvw", 1e-8 );      // km/s
        assertArrayColumnNearZero( result, "dR", 1e-8 );        // relative
        assertArrayColumnNearZero( result, "dS", 1e-8 );        // relative
        assertArrayColumnNearZero( result, "dgXyz1", 1e-8 );    // pc
    }

    public void testUvw() {

        /* Compare values with UVW calculated in the literature.
         * The comparison is with HD95418 (Beta UMa), considered in
         * Johnson and Soderblom 1987 (1987AJ.....93..864J), which is
         * a pedagogic paper about calculating UVW values.
         * The position is taken from SIMBAD, the other four astrometric
         * parameters are from J+S, as is the UVW vector calculated there.
         * This comparison proves that the values I'm calculating are
         * about right and match standard conventions (i.e. I'm not
         * using a reversed coordinate system or something). */
        double ra = 165.46037718;
        double dec = +56.38245362;
        double pmra = +83;
        double pmdec = +29;
        double parallax = 53.1;
        double radial_velocity = -12.0;
        double[] buma6 = { ra, dec, parallax, pmra, pmdec, radial_velocity };
        double[] uvwGal = Gaia.icrsToGal( Gaia.astromUVW( buma6 ) );
        assertEquals( +11.9, uvwGal[ 0 ], 0.1 );
        assertEquals(  +1.1, uvwGal[ 1 ], 0.1 );
        assertEquals(  -8.0, uvwGal[ 2 ], 0.2 );

        /* Sanity check that it's about the right distance away. */
        assertEquals( 19,
                      Maths.hypot( Gaia.polarXYZ( ra, dec, 1000./parallax ) ),
                      1.0 );
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
