// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Functions for calculating K-corrections.
 *
 * @author   Mark Taylor
 * @author   Igor Chilingarian
 * @author   Anne-Laure Melchior
 * @author   Ivan Zolotukhin
 * @since    9 Nov 2012
 */
public class KCorrections {

    /**
     * Private constructor prevents instantiation.
     */
    private KCorrections() {
    }

    /**
     * Calculates K-corrections. This allows you to determine K-corrections
     * for a galaxy, given its redshift and a colour. Filters for GALEX,
     * SDSS, UKIDSS, Johnson, Cousins and 2MASS are covered.
     *
     * <p>To define the calculation you must choose both a filter,
     * specified as a <code>KCF_*</code> constant,
     * and a colour (filter pair) specified as a <code>KCC_*</code> constant.
     * For each available filter, only certain colours are available,
     * as described in the documentation of the relevant <code>KCF_*</code>
     * constant.
     *
     * <p>The algorithm used is described at
     * <a href="http://kcor.sai.msu.ru/">http://kcor.sai.msu.ru/</a>.
     * This is based on the paper
     * <em>"Analytical Approximations of K-corrections in
     * Optical and Near-Infrared Bands"</em>
     * by I.Chilingarian, A.-L.Melchior and I.Zolotukhin
     * (<a href="https://ui.adsabs.harvard.edu/abs/2010MNRAS.405.1409C"
     *                                            >2010MNRAS.405.1409C</a>),
     * but extended to include GALEX UV bands and with redshift
     * coverage up to 0.5 as described in
     * <em>"Universal UV-optical Colour-Colour-Magnitude Relation of
     * Galaxies"</em> by I.Chilingarian and I.Zolotukhin
     * (<a href="https://ui.adsabs.harvard.edu/abs/2012MNRAS.419.1727C"
     *                                            >2012MNRAS.419.1727C</a>).
     *
     * @example <code>kCorr(KCF_g, 0.16, KCC_gr, -0.8) = 3.593</code>
     * @example <code>kCorr(KCF_FUV, 0.48, KCC_FUVu, 0.31) = -0.170</code>
     *
     * @param   filter  <code>KCF_*</code> constant defining the filter
     *                  for which you want to calculate the K-correction
     * @param   redshift  galaxy redshift; this should be in the range 0-0.5
     * @param   colorType <code>KCC_*</code> constant defining the
     *                    filter pair for the calculation; check the
     *                    <code>KCF_*</code> constant documentation to see
     *                    which ones are permitted for a given filter
     * @param   colorValue  the value of the colour
     * @return   K correction
     */
    public static double kCorr( KFilter filter, double redshift,
                                KColor colorType, double colorValue ) {
        double[][] coeffs = filter.getCoeffs( colorType );
        if ( coeffs == null ) {
            return Double.NaN;
        }
        int nx = coeffs.length;
        int ny = coeffs[ 0 ].length;
        double kcor = 0;
        for ( int ix = 0; ix < nx; ix++ ) {
            for ( int iy = 0; iy < ny; iy++ ) {
                double cxy = coeffs[ ix ][ iy ];
                kcor += cxy * Math.pow( redshift, ix )
                            * Math.pow( colorValue, iy );
            }
        }
        return kcor;
    }

    /**
     * Characterises a filter for which K-corrections can be calculated.
     */
    static class KFilter {
        private final CoeffMap cmap_;
        private final String fname_;

        /**
         * Constructor.
         *
         * @param   fname  filter name
         * @param   cmap   map of colours to coefficients
         */
        private KFilter( String fname, CoeffMap cmap ) {
            fname_ = fname;
            cmap_ = cmap;
        }

        /**
         * Returns the coefficient array for a given colour.
         *
         * @param  colour to query
         * @param   coefficients array
         */
        private double[][] getCoeffs( KColor color ) {
            return cmap_.map_.get( color );
        }

        @Override
        public String toString() {
            return fname_;
        }
    }

    /**
     * Characterises an ordered pair of filters.
     */
    static class KColor {
        private final String f1_;
        private final String f2_;

        /**
         * Constructs a filter corresponding to
         * <code>f1</code>-<code>f2</code>.
         *
         * @param  f1  name of first filter
         * @param  f2  name of second filter
         */
        private KColor( String f1, String f2 ) {
            f1_ = f1;
            f2_ = f2;
        }

        @Override
        public String toString() {
            return f1_ + "-"  + f2_;
        }
    }

    /**
     * Stores one or more sets of correction coefficients keyed by
     * KColor.
     */
    private static class CoeffMap {
        private final Map<KColor,double[][]> map_;

        /**
         * Constructor.
         */
        private CoeffMap() {
            map_ = new LinkedHashMap<KColor,double[][]>();
        }

        /**
         * Adds coefficent array for a given colour.
         *
         * @param  color  filter pair
         * @param  coeffs  coefficient array
         */
        private CoeffMap add( KColor color, double[][] coeffs ) {
            map_.put( color, coeffs );
            return this;
        }
    }

    /** GALEX FUV filter (AB).  Use with KCC_FUVNUV or KCC_FUVu. */
    public static final KFilter KCF_FUV;

    /** GALEX NUV filter (AB).  Use with KCC_NUVg or KCC_NUVr. */
    public static final KFilter KCF_NUV;

    /** SDSS u filter (AB).  Use with KCC_ur, KCC_ui or KCC_uz. */
    public static final KFilter KCF_u;

    /** SDSS g filter (AB).  Use with KCC_gr, KCC_gi or KCC_gz. */
    public static final KFilter KCF_g;

    /** SDSS r filter (AB).  Use with KCC_gr or KCC_ur. */
    public static final KFilter KCF_r;

    /** SDSS i filter (AB).  Use with KCC_gi or KCC_ui. */
    public static final KFilter KCF_i;

    /** SDSS z filter (AB).  Use with KCC_rz, KCC_gz or KCC_uz. */
    public static final KFilter KCF_z;

    /** UKIDSS Y filter (AB).  Use with KCC_YH or KCC_YK. */
    public static final KFilter KCF_Y;

    /** UKIDSS J filter (AB).  Use with KCC_JK or KCC_JH. */
    public static final KFilter KCF_J;

    /** UKIDSS H filter (AB).  Use with KCC_HK or KCC_JH. */
    public static final KFilter KCF_H;

    /** UKIDSS K filter (AB).  Use with KCC_JK or KCC_HK. */
    public static final KFilter KCF_K;

    /** Johnson U filter (Vega).  Use with KCC_URc. */
    public static final KFilter KCF_U;

    /** Johnson B filter (Vega).  Use with KCC_BRc or KCC_BIc. */
    public static final KFilter KCF_B;

    /** Johnson V filter (Vega).  Use with KCC_VIc or KCC_VRc. */
    public static final KFilter KCF_V;

    /** Cousins Rc filter (Vega).  Use with KCC_BRc or KCC_VRc. */
    public static final KFilter KCF_Rc;

    /** Cousins Ic filter (Vega).  Use with KCC_VIc. */
    public static final KFilter KCF_Ic;

    /** 2MASS J filter (Vega).  Use with KCC_J2Ks2 or KCC_J2H2. */
    public static final KFilter KCF_J2;

    /** 2MASS H filter (Vega).  Use with KCC_H2Ks2 or KCC_J2H2. */
    public static final KFilter KCF_H2;

    /** 2MASS Ks filter (Vega).  Use with KCC_J2Ks2 or KCC_H2Ks2. */
    public static final KFilter KCF_Ks2;


    /** Johnson B - Cousins Ic colour. */
    public static final KColor KCC_BIc = new KColor( "B", "Ic" );

    /** Johnson B - Cousins Rc colour. */
    public static final KColor KCC_BRc = new KColor( "B", "Rc" );

    /** GALEX FUV - NUV colour. */
    public static final KColor KCC_FUVNUV = new KColor( "FUV", "NUV" );

    /** GALEX FUV - SDSS u colour. */
    public static final KColor KCC_FUVu = new KColor( "FUV", "u" );

    /** SDSS g - i colour. */
    public static final KColor KCC_gi = new KColor( "g", "i" );

    /** SDSS g - r colour. */
    public static final KColor KCC_gr = new KColor( "g", "r" );

    /** SDSS g - z colour. */
    public static final KColor KCC_gz = new KColor( "g", "z" );

    /** 2MASS H - Ks colour. */
    public static final KColor KCC_H2Ks2 = new KColor( "H2", "Ks2" );

    /** UKIDSS H - K colour. */
    public static final KColor KCC_HK = new KColor( "H", "K" );

    /** 2MASS J - H colour. */
    public static final KColor KCC_J2H2 = new KColor( "J2", "H2" );

    /** 2MASS J - Ks colour. */
    public static final KColor KCC_J2Ks2 = new KColor( "J2", "Ks2" );

    /** UKIDSS J - H colour. */
    public static final KColor KCC_JH = new KColor( "J", "H" );

    /** UKIDSS J - K colour. */
    public static final KColor KCC_JK = new KColor( "J", "K" );

    /** GALEX NUV - SDSS g colour. */
    public static final KColor KCC_NUVg = new KColor( "NUV", "g" );

    /** GALEX NUV - SDSS r colour. */
    public static final KColor KCC_NUVr = new KColor( "NUV", "r" );

    /** SDSS r - SDSS z colour. */
    public static final KColor KCC_rz = new KColor( "r", "z" );

    /** SDSS u - SDSS i colour. */
    public static final KColor KCC_ui = new KColor( "u", "i" );

    /** Johnson U - Cousins Rc colour. */
    public static final KColor KCC_URc = new KColor( "U", "Rc" );

    /** SDSS u - r colour. */
    public static final KColor KCC_ur = new KColor( "u", "r" );

    /** SDSS u - z colour. */
    public static final KColor KCC_uz = new KColor( "u", "z" );

    /** Johnson V - Cousins Ic colour. */
    public static final KColor KCC_VIc = new KColor( "V", "Ic" );

    /** Johnson V - Cousins Rc colour. */
    public static final KColor KCC_VRc = new KColor( "V", "Rc" );

    /** UKIDSS Y - H colour. */
    public static final KColor KCC_YH = new KColor( "Y", "H" );

    /** UKIDSS Y - K colour. */
    public static final KColor KCC_YK = new KColor( "Y", "K" );

    static {
        KCF_B = new KFilter( "Johnson B", new CoeffMap()
            .add( KCC_BRc, new double[][] {
                {0,0,0,0},
                {-1.99412,3.45377,0.818214,-0.630543},
                {15.9592,-3.99873,6.44175,0.828667},
                {-101.876,-44.4243,-12.6224,0},
                {299.29,86.789,0,0},
                {-304.526,0,0,0},
            } )
            .add( KCC_BIc, new double[][] {
                {0,0,0,0},
                {2.11655  ,  -5.28948  ,  4.5095  ,  -0.8891},
                {24.0499  ,  -4.76477  ,  -1.55617  ,  1.85361},
                {-121.96  ,  7.73146  ,  -17.1605  ,  0},
                {236.222  ,  76.5863  ,  0  ,  0},
                {-281.824  ,  0  ,  0  ,  0},
            } ) );

        KCF_H2 = new KFilter( "2MASS H", new CoeffMap()
            .add( KCC_H2Ks2, new double[][] {
                {0,0,0,0},
                {-1.88351,1.19742,10.0062,-18.0133},
                {11.1068,20.6816,-16.6483,139.907},
                {-79.1256,-406.065,-48.6619,-430.432},
                {551.385,1453.82,354.176,473.859},
                {-1728.49,-1785.33,-705.044,0},
                {2027.48,950.465,0,0},
                {-741.198,0,0,0},
            } )
            .add( KCC_J2H2, new double[][] {
                {0,0,0,0},
                {-4.99539,5.79815,4.19097,-7.36237},
                {70.4664,-202.698,244.798,-65.7179},
                {-142.831,553.379,-1247.8,574.124},
                {-414.164,1206.23,467.602,-799.626},
                {763.857,-2270.69,1845.38,0},
                {-563.812,-1227.82,0,0},
                {1392.67,0,0,0},
            } ) );

        KCF_Ic = new KFilter( "Cousins Ic", new CoeffMap()
            .add( KCC_VIc, new double[][] {
                {0,0,0,0},
                {-7.92467,17.6389,-15.2414,5.12562},
                {15.7555,-1.99263,10.663,-10.8329},
                {-88.0145,-42.9575,46.7401,0},
                {266.377,-67.5785,0,0},
                {-164.217,0,0,0},
            } ) );

        KCF_J2 = new KFilter( "2MASS J", new CoeffMap()
            .add( KCC_J2Ks2, new double[][] {
                {0,0,0,0},
                {-2.85079,1.7402,0.754404,-0.41967},
                {24.1679,-34.9114,11.6095,0.691538},
                {-32.3501,59.9733,-29.6886,0},
                {-30.2249,43.3261,0,0},
                {-36.8587,0,0,0},
            } )
            .add( KCC_J2H2, new double[][] {
                {0,0,0,0},
                {-0.905709,-4.17058,11.5452,-7.7345},
                {5.38206,-6.73039,-5.94359,20.5753},
                {-5.99575,32.9624,-72.08,0},
                {-19.9099,92.1681,0,0},
                {-45.7148,0,0,0},
            } ) );

        KCF_Ks2 = new KFilter( "2MASS Ks", new CoeffMap()
            .add( KCC_J2Ks2, new double[][] {
                {0,0,0,0},
                {-5.08065,-0.15919,4.15442,-0.794224},
                {62.8862,-61.9293,-2.11406,1.56637},
                {-191.117,212.626,-15.1137,0},
                {116.797,-151.833,0,0},
                {41.4071,0,0,0},
            } )
            .add( KCC_H2Ks2, new double[][] {
                {0,0,0,0},
                {-3.90879,5.05938,10.5434,-10.9614},
                {23.6036,-97.0952,14.0686,28.994},
                {-44.4514,266.242,-108.639,0},
                {-15.8337,-117.61,0,0},
                {28.3737,0,0,0},
            } ) );

        KCF_Rc = new KFilter( "Cousins Rc", new CoeffMap()
            .add( KCC_BRc, new double[][] {
                {0,0,0,0},
                {-2.83216,4.64989,-2.86494,0.90422},
                {4.97464,5.34587,0.408024,-2.47204},
                {-57.3361,-30.3302,18.4741,0},
                {224.219,-19.3575,0,0},
                {-194.829,0,0,0},
            } )
            .add( KCC_VRc, new double[][] {
                {0,0,0,0},
                {-3.39312  ,  16.7423  ,  -29.0396  ,  25.7662 },
                {5.88415  ,  6.02901  ,  -5.07557  ,  -66.1624 },
                {-50.654  ,  -13.1229  ,  188.091  ,  0 },
                {131.682  ,  -191.427  ,  0  ,  0 },
                {-36.9821  ,  0  ,  0  ,  0 },
            } ) );

        KCF_U = new KFilter( "Johnson U", new CoeffMap()
            .add( KCC_URc, new double[][] {
                {0,0,0,0},
                {2.84791,2.31564,-0.411492,-0.0362256},
                {-18.8238,13.2852,6.74212,-2.16222},
                {-307.885,-124.303,-9.92117,12.7453},
                {3040.57,428.811,-124.492,-14.3232},
                {-10677.7,-39.2842,197.445,0},
                {16022.4,-641.309,0,0},
                {-8586.18,0,0,0},
            } ) );

        KCF_V = new KFilter( "Johnson V", new CoeffMap()
            .add( KCC_VIc, new double[][] {
                {0,0,0,0},
                {-1.37734,-1.3982,4.76093,-1.59598},
                {19.0533,-17.9194,8.32856,0.622176},
                {-86.9899,-13.6809,-9.25747,0},
                {305.09,39.4246,0,0},
                {-324.357,0,0,0},
            } )
            .add( KCC_VRc, new double[][] {
                {0,0,0,0},
                {-2.21628,8.32648,-7.8023,9.53426},
                {13.136,-1.18745,3.66083,-41.3694},
                {-117.152,-28.1502,116.992,0},
                {365.049,-93.68,0,0},
                {-298.582,0,0,0},
            } ) );

        KCF_FUV = new KFilter( "GALEX FUV", new CoeffMap()
            .add( KCC_FUVNUV, new double[][] {
                {  0,0,0,0 },
                {  -0.866758,0.2405,0.155007,0.0807314 },
                {  -1.17598,6.90712,3.72288,-4.25468 },
                {  135.006,-56.4344,-1.19312,25.8617 },
                {  -1294.67,245.759,-84.6163,-40.8712 },
                {  4992.29,-477.139,174.281,0 },
                {  -8606.6,316.571,0,0 },
                {  5504.2,0,0,0 },
            } )
            .add( KCC_FUVu, new double[][] {
                {  0,0,0,0 },
                {  -1.67589,0.447786,0.369919,-0.0954247 },
                {  2.10419,6.49129,-2.54751,0.177888 },
                {  15.6521,-32.2339,4.4459,0 },
                {  -48.3912,37.1325,0,0 },
                {  37.0269,0,0,0 },
            } ) );

        KCF_g = new KFilter( "SDSS g", new CoeffMap()
            .add( KCC_gi, new double[][] {
                {  0,0,0,0 },
                {  1.59269,-2.97991,7.31089,-3.46913 },
                {  -27.5631,-9.89034,15.4693,6.53131 },
                {  161.969,-76.171,-56.1923,0 },
                {  -204.457,217.977,0,0 },
                {  -50.6269,0,0,0 },
            } )
            .add( KCC_gz, new double[][] {
                {  0,0,0,0 },
                {  2.37454,-4.39943,7.29383,-2.90691 },
                {  -28.7217,-20.7783,18.3055,5.04468 },
                {  220.097,-81.883,-55.8349,0 },
                {  -290.86,253.677,0,0 },
                {  -73.5316,0,0,0 },
            } )
            .add( KCC_gr, new double[][] {
                {  0,0,0,0 },
                {  -2.45204,4.10188,10.5258,-13.5889 },
                {  56.7969,-140.913,144.572,57.2155 },
                {  -466.949,222.789,-917.46,-78.0591 },
                {  2906.77,1500.8,1689.97,30.889 },
                {  -10453.7,-4419.56,-1011.01,0 },
                {  17568,3236.68,0,0 },
                {  -10820.7,0,0,0 },
            } ) );

        KCF_H = new KFilter( "UKIDSS H", new CoeffMap()
            .add( KCC_JH, new double[][] {
                {  0,0,0,0 },
                {  -1.6196,3.55254,1.01414,-1.88023 },
                {  38.4753,-8.9772,-139.021,15.4588 },
                {  -417.861,89.1454,808.928,-18.9682 },
                {  2127.81,-405.755,-1710.95,-14.4226 },
                {  -5719,731.135,1284.35,0 },
                {  7813.57,-500.95,0,0 },
                {  -4248.19,0,0,0 },
            } )
            .add( KCC_HK, new double[][] {
                {  0,0,0,0 },
                {  0.812404,7.74956,1.43107,-10.3853 },
                {  -23.6812,-235.584,-147.582,188.064 },
                {  283.702,2065.89,721.859,-713.536 },
                {  -1697.78,-7454.39,-1100.02,753.04 },
                {  5076.66,11997.5,460.328,0 },
                {  -7352.86,-7166.83,0,0 },
                {  4125.88,0,0,0 },
            } ) );

    KCF_i = new KFilter( "SDSS i", new CoeffMap()
            .add( KCC_gi, new double[][] {
                {  0,0,0,0 },
                {  -2.21853,3.94007,0.678402,-1.24751 },
                {  -15.7929,-19.3587,15.0137,2.27779 },
                {  118.791,-40.0709,-30.6727,0 },
                {  -134.571,125.799,0,0 },
                {  -55.4483,0,0,0 },
            } )
            .add( KCC_ui, new double[][] {
                {  0,0,0,0 },
                {  -3.91949,3.20431,-0.431124,-0.000912813 },
                {  -14.776,-6.56405,1.15975,0.0429679 },
                {  135.273,-1.30583,-1.81687,0 },
                {  -264.69,15.2846,0,0 },
                {  142.624,0,0,0 },
            } ) );

            KCF_J = new KFilter( "UKIDSS J", new CoeffMap()
            .add( KCC_JH, new double[][] {
                {  0,0,0,0 },
                {  0.129195,1.57243,-2.79362,-0.177462 },
                {  -15.9071,-2.22557,-12.3799,-2.14159 },
                {  89.1236,65.4377,36.9197,0 },
                {  -209.27,-123.252,0,0 },
                {  180.138,0,0,0 },
            } )
            .add( KCC_JK, new double[][] {
                {  0,0,0,0 },
                {  0.0772766,2.17962,-4.23473,-0.175053 },
                {  -13.9606,-19.998,22.5939,-3.99985 },
                {  97.1195,90.4465,-21.6729,0 },
                {  -283.153,-106.138,0,0 },
                {  272.291,0,0,0 },
            } ) );

        KCF_K = new KFilter( "UKIDSS K", new CoeffMap()
            .add( KCC_HK, new double[][] {
                {  0,0,0,0 },
                {  -2.83918,-2.60467,-8.80285,-1.62272 },
                {  14.0271,17.5133,42.3171,4.8453 },
                {  -77.5591,-28.7242,-54.0153,0 },
                {  186.489,10.6493,0,0 },
                {  -146.186,0,0,0 },
            } )
            .add( KCC_JK, new double[][] {
                {  0,0,0,0 },
                {  -2.58706,1.27843,-5.17966,2.08137 },
                {  9.63191,-4.8383,19.1588,-5.97411 },
                {  -55.0642,13.0179,-14.3262,0 },
                {  131.866,-13.6557,0,0 },
                {  -101.445,0,0,0 },
            } ) );

        KCF_NUV = new KFilter( "GALEX NUV", new CoeffMap()
            .add( KCC_NUVr, new double[][] {
                {  0,0,0,0 },
                {  2.2112,-1.2776,0.219084,0.0181984 },
                {  -25.0673,5.02341,-0.759049,-0.0652431 },
                {  115.613,-5.18613,1.78492,0 },
                {  -278.442,-5.48893,0,0 },
                {  261.478,0,0,0 },
            } )
            .add( KCC_NUVg, new double[][] {
                {  0,0,0,0 },
                {  2.60443,-2.04106,0.52215,0.00028771 },
                {  -24.6891,5.70907,-0.552946,-0.131456 },
                {  95.908,-0.524918,1.28406,0 },
                {  -208.296,-10.2545,0,0 },
                {  186.442,0,0,0 },
            } ) );

        KCF_r = new KFilter( "SDSS r", new CoeffMap()
            .add( KCC_gr, new double[][] {
                {  0,0,0,0 },
                {  1.83285,-2.71446,4.97336,-3.66864 },
                {  -19.7595,10.5033,18.8196,6.07785 },
                {  33.6059,-120.713,-49.299,0 },
                {  144.371,216.453,0,0 },
                {  -295.39,0,0,0 },
            } )
            .add( KCC_ur, new double[][] {
                {  0,0,0,0 },
                {  3.03458,-1.50775,0.576228,-0.0754155 },
                {  -47.8362,19.0053,-3.15116,0.286009 },
                {  154.986,-35.6633,1.09562,0 },
                {  -188.094,28.1876,0,0 },
                {  68.9867,0,0,0 },
            } ) );

        KCF_u = new KFilter( "SDSS u", new CoeffMap()
            .add( KCC_ur, new double[][] {
                {  0,0,0,0 },
                {  10.3686,-6.12658,2.58748,-0.299322 },
                {  -138.069,45.0511,-10.8074,0.95854 },
                {  540.494,-43.7644,3.84259,0 },
                {  -1005.28,10.9763,0,0 },
                {  710.482,0,0,0 },
            } )
            .add( KCC_ui, new double[][] {
                {  0,0,0,0 },
                {  11.0679,-6.43368,2.4874,-0.276358 },
                {  -134.36,36.0764,-8.06881,0.788515 },
                {  528.447,-26.7358,0.324884,0 },
                {  -1023.1,13.8118,0,0 },
                {  721.096,0,0,0 },
            } )
            .add( KCC_uz, new double[][] {
                {  0,0,0,0 },
                {  11.9853,-6.71644,2.31366,-0.234388 },
                {  -137.024,35.7475,-7.48653,0.655665 },
                {  519.365,-20.9797,0.670477,0 },
                {  -1028.36,2.79717,0,0 },
                {  767.552,0,0,0 },
            } ) );

        KCF_Y = new KFilter( "UKIDSS Y", new CoeffMap()
            .add( KCC_YH, new double[][] {
                {  0,0,0,0 },
                {  -2.81404,10.7397,-0.869515,-11.7591 },
                {  10.0424,-58.4924,49.2106,23.6013 },
                {  -0.311944,84.2151,-100.625,0 },
                {  -45.306,3.77161,0,0 },
                {  41.1134,0,0,0 },
            } )
        .add( KCC_YK, new double[][] {
                {  0,0,0,0 },
                {  -0.516651,6.86141,-9.80894,-0.410825 },
                {  -3.90566,-4.42593,51.4649,-2.86695 },
                {  -5.38413,-68.218,-50.5315,0 },
                {  57.4445,97.2834,0,0 },
                {  -64.6172,0,0,0 },
            } ) );

        KCF_z = new KFilter( "SDSS z", new CoeffMap()
            .add( KCC_gz, new double[][] {
                {  0,0,0,0 },
                {  0.30146,-0.623614,1.40008,-0.534053 },
                {  -10.9584,-4.515,2.17456,0.913877 },
                {  66.0541,4.18323,-8.42098,0 },
                {  -169.494,14.5628,0,0 },
                {  144.021,0,0,0 },
            } )
            .add( KCC_rz, new double[][] {
                {  0,0,0,0 },
                {  0.669031,-3.08016,9.87081,-7.07135 },
                {  -18.6165,8.24314,-14.2716,13.8663 },
                {  94.1113,11.2971,-11.9588,0 },
                {  -225.428,-17.8509,0,0 },
                {  197.505,0,0,0 },
            } )
            .add( KCC_uz, new double[][] {
                    {  0,0,0,0 },
                {  0.623441,-0.293199,0.16293,-0.0134639 },
                {  -21.567,5.93194,-1.41235,0.0714143 },
                {  82.8481,-0.245694,0.849976,0 },
                {  -185.812,-7.9729,0,0 },
                {  168.691,0,0,0 },
            } ) );
    }
}
