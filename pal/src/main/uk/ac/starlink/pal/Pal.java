/**
 *  Starlink Positional Astronomy Library ( Java version )
 */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/**
 *  Positional Astronomy Library.
 *  @author R T Platon (Starlink)
 *  @version 1.0
 *  [Latest Revision: January 2003]
 *  <p>Based on the C version of slalib written by P T Wallace.
 */
public class Pal {

/* Various Constants based on PI
/* pi = 3.1415926535897932384626433832795028841971693993751 */
    private static final double DPI    = Math.PI;

/* 2pi = 6.2831853071795864769252867665590057683943387987502 */
    private static final double D2PI   = 2*DPI;

/* 1/(2pi) = 0.15915494309189533576888376337251436203445964574046 */
    private static final double D1B2PI = 1/D2PI;

/* 4pi = 12.566370614359172953850573533118011536788677597500 */
    private static final double D4PI   = 4*DPI;

/* 1/(4pi) = 0.079577471545947667884441881686257181017229822870228 */
    private static final double D1B4PI = 1/D4PI;

/* pi^2 = 9.8696044010893586188344909998761511353136994072408 */
    private static final double DPISQ  = DPI*DPI;

/* sqrt(pi) = 1.7724538509055160272981674833411451827975494561224 */
    private static final double DSQRPI = Math.sqrt(DPI);

/* pi/2 = 1.5707963267948966192313216916397514420985846996876
 * 90 degrees in radians */
    private static final double DPIBY2 = DPI/2;

/* pi/180 = 0.017453292519943295769236907684886127134428718885417
 * degrees to radians */
    private static final double DD2R   = DPI/180;

/* 180/pi = 57.295779513082320876798154814105170332405472466564
 * radians to degrees */
    private static final double DR2D   = 180/DPI;

/* pi/(180*3600) = 4.8481368110953599358991410235794797595635330237270e-6
 * arcseconds to radians */
    private static final double DAS2R  = DPI/(180*3600);

/* 180*3600/pi = 2.0626480624709635515647335733077861319665970087963e5
 * radians to arcseconds */
    private static final double DR2AS  = 180*3600/DPI;

/* pi/12 = 0.26179938779914943653855361527329190701643078328126
 * hours to radians */
    private static final double DH2R   = DPI/12;

/* 12/pi = 3.8197186342054880584532103209403446888270314977709
 * radians to hours */
    private static final double DR2H   = 12/DPI;

/* pi/(12*3600) = 7.2722052166430399038487115353692196393452995355905e-5
 * seconds of time to radians */
    private static final double DS2R   = DPI/(12*3600);

/* 12*3600/pi = 1.3750987083139757010431557155385240879777313391975e4
 * radians to seconds of time */
    private static final double DR2S   = 12*3600/DPI;

/* 15/(2pi) =2.3873241463784300365332564505877154305168946861068
 * hours to degrees x radians to turns */
    private static final double D15B2P = 15/D2PI;

/* Arc seconds in a full circle */
    private static final double TURNAS = 1296000.0;
/* Reference epoch (J2000), MJD */
    private static final double DJM0 = 51544.5;
/* Days per Julian century */
    private static final double DJC = 36525.0;

/** Gravitational radius of the Sun x 2: (2*mu/c**2, au)
 */
    public static final double GR2 = 1.974126e-8;  

/* Turns to arc seconds */
    private static final double T2AS = 1296000.0;

/* Units of 0.0001 arcsec to radians */
    private static final double U2R = 0.4848136811095359949e-9;

/** Km/s to AU/year
 */
    public static final double VF = 0.21094502;

/** Speed of light (AU per day)
 */
    public static final double C = 173.14463331;

/** Ratio between solar and sidereal time
 */
    public static final double SOLSID = 1.00273790935;

/* Numerical integration: maximum number of strips. */
    private static final int ISMAX = 16384; 
    private static final double TINY = 1e-20;
/* Small number to avoid arithmetic problems */
    private static final double VERYTINY = 1.0e-30;

/** Nominal mean sidereal speed of Earth equator in km/s
 *  (the actual value is about 0.4651)
 */
    public static final double ESPEED = 0.4655;  

/** Seconds in a day
 */
    public static final double D2S = 86400.0;

/** Astronomical unit to kilometers
 */
    public static final double AUKM = 149.597870E6;

/** Light time for 1 AU (sec)
 */
    public static final double AUSEC = 499.004782;

/** Degrees to radians
 */
    public static final double R2D = 57.29577951308232087679815;

/** Current Status flag
 */
    public int Status = 0;

/** Flag for additional status information
 */
    public int Flag = 0;

/*
 * Various internal functions (taken from C defines)
 */

/* The refraction integrand */
    private static double refi( double DN, double RDNDR) {
        return RDNDR / (DN+RDNDR);
    }
    private static double dmod( double a, double b ) {
        double c;
        if ( b != 0.0 ) {
            if ( (a*b)  > 0.0 ) {
                c = a - ( b*Math.floor(a/b) );
            } else {
                c = a + ( b*Math.floor(-a/b) );
            }
        } else {
            c = a;
        }
        return c;
    }
    private static double gmax( double a, double b ) { 
        return ( a > b ? a : b );
    }
    private static double gmin( double a, double b ) { 
        return ( a < b ? a : b );
    }
    private static int dint( double a ) { 
        return ( a < 0.0 ? (int) Math.ceil(a) : (int) Math.floor(a) );
    }
    private static double dnint( double a ) { 
        return ( a < 0.0 ? Math.ceil(a-0.5) : Math.floor(a+0.5) );
    }

/*****************************************************************************/

/**
 *  Add the e-terms (elliptic component of annual aberration) to a
 *  pre IAU 1976 mean place to conform to the old catalogue convention.
 *
 *  <dl>
 *  <dt>Explanation:</dt>
 *  <dd>
 *     Most star positions from pre-1984 optical catalogues (or
 *     derived from astrometry using such stars) embody the
 *     e-terms.  If it is necessary to convert a formal mean
 *     place (for example a pulsar timing position) to one
 *     consistent with such a star catalogue, then the RA,Dec
 *     should be adjusted using this routine.
 *  </dd>
 *  <dt>
 *  Reference:</dt>
 *  <dd>
 *     Explanatory Supplement to the Astronomical Almanac,
 *     ed P.K.Seidelmann (1992), page 169.
 *  </dd>
 *  </dl>
 *
 *  @param m RA,Dec (radians) without e-terms
 *  @param eq Besselian epoch of mean equator and equinox
 *  @return RA,dec (radians) with e-terms included
 */

/*  Latest Revision: 29 November 2001
 *
 *  Called:
 *     Etrms, Dcs2c, Dcc2s, Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Addet ( AngleDR m, double eq )
    {

        double a[];    /* Elliptic components of annual aberration vector */
        double v[];    /* Cartesian equivalant of RA,Dec */
        AngleDR c;
        int i;

        /* E-terms vector */
        a = Etrms ( eq );

        /* Spherical to Cartesian */
        v = Dcs2c ( m );

        /* Include the e-terms */
        for ( i=0; i < 3; i++ ) {
            v[i] += a[i];
        }

        /* Cartesian to spherical */
        c = Dcc2s ( v );

        /* Bring RA into conventional range */
        c.setAlpha( Dranrm ( c.getAlpha() ) );

        return c;
    }

/**
 *  Convert star RA,Dec from geocentric apparent to mean place.
 *  <p>
 *  The mean coordinate system is the post IAU 1976 system,
 *  loosely called FK5.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The distinction between the required TDB and TT is always
 *      negligible.  Moreover, for all but the most critical
 *      applications UTC is adequate.
 *  </li>
 *  <li>Iterative techniques are used for the aberration and light
 *      deflection corrections so that the routines Amp (or
 *      Ampqk) and Map (or Mapqk) are accurate inverses;
 *      even at the edge of the Sun's disc the discrepancy is only
 *      about 1 nanoarcsecond.
 *  </li>
 *  <li> Where multiple apparent places are to be converted to mean
 *       places, for a fixed date and equinox, it is more efficient to
 *       use the Mappa routine to compute the required parameters
 *       once, followed by one call to Ampqk per star.
 *  </li>
 *  <li>The accuracy is sub-milliarcsecond, limited by the
 *       precession-nutation model (IAU 1976 precession, Shirai &amp;
 *       Fukushima 2001 forced nutation and precession corrections).
 *  </li>
 *  <li>The accuracy is further limited by the routine Evp, called
 *       by Mappa, which computes the Earth position and velocity
 *       using the methods of Stumpff.  The maximum error is about
 *       0.3 mas.
 *  </li>
 *  </ol> </dd>
 *  <dt> References:</dt>
 *  <dd>
 *      1984 Astronomical Almanac, pp B39-B41.
 *      (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)
 *  </dd>
 *  </dl>
 *
 *  @param ap apparent RA &amp; Dec (radians)
 *  @param date TDB for apparent place (JD-2400000.5)
 *  @param eq equinox:  Julian epoch of mean place
 *  @return mean RA &amp; Dec (Radians)
 */

/*  Latest Revision: 17 November 2001 (RTP)
 * 
 *  Called:  Mappa, Ampqk
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 *
 */
    public AngleDR Amp ( AngleDR ap, double date, double eq )
    {
        AMParams amprms;    /* Mean-to-apparent parameters */

        amprms = Mappa ( eq, date );

        return ( Ampqk ( ap, amprms ) );
    }

/**
 *  Convert star RA,Dec from geocentric apparent to mean place.
 *  <p>
 *  The mean coordinate system is the post IAU 1976 system,
 *  loosely called FK5.
 *  </p>
 *  <p>
 *  Use of this routine is appropriate when efficiency is important
 *  and where many star positions are all to be transformed for
 *  one epoch and equinox.  The star-independent parameters can be
 *  obtained by calling the Mappa routine.
 *  </p>
 *  <dl>
 *  <dt>References:</dt>
 *  <dd>
 *     1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)
 *  </dd>
 *
 *  <dt>Note:</dt>
 *  <dd>
 *     Iterative techniques are used for the aberration and
 *     light deflection corrections so that the routines
 *     Amp (or Ampqk) and Map (or Mapqk) are
 *     accurate inverses;  even at the edge of the Sun's disc
 *     the discrepancy is only about 1 nanoarcsecond.
 *  </dd>
 *  </dl>
 *
 *  @param ap apparent RA &amp; Dec (radians)
 *  @param amprms star-independent mean-to-apparent parameters
 *  @return mean RA &amp; Dec (radians)
*/
/*  Latest Revision: 29 November 2001 (RTP)
 *
 *  Given:
 *     ra       double      apparent RA (radians)
 *     da       double      apparent Dec (radians)
 *
 *     amprms   double[21]  star-independent mean-to-apparent parameters:
 *
 *       (0)      time interval for proper motion (Julian years)
 *       (1-3)    barycentric position of the Earth (AU)
 *       (4-6)    heliocentric direction of the Earth (unit vector)
 *       (7)      (grav rad Sun)*2/(Sun-Earth distance)
 *       (8-10)   abv: barycentric Earth velocity in units of c
 *       (11)     sqrt(1-v*v) where v=modulus(abv)
 *       (12-20)  precession/nutation (3,3) matrix
 *
 *  Returned:
 *     *rm      double      mean RA (radians)
 *     *dm      double      mean Dec (radians)
 *
 *  Called:  Dcs2c, Dimxv, Dvdv, Dvn, Dcc2s,
 *           Dranrm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
*/
    public AngleDR Ampqk ( AngleDR ap, AMParams amprms )
    {
        double gr2e;    /* (grav rad Sun)*2/(Sun-Earth distance) */
        double ab1;     /* sqrt(1-v*v) where v=modulus of Earth vel */
        double ehn[] = new double[3];  /* Earth position wrt Sun (unit vector, FK5) */
        double abv[] = new double[3];  /* Earth velocity wrt SSB (c, FK5) */
        double p[] = new double[3], p1[] = new double[3];
        double p2[] = new double[3], p3[] = new double[3];  /* work vectors */
        double ab1p1, p1dv= 0.0, p1dvp1, w, pde=0.0, pdep1;
        int i, j;

/* Unpack some of the parameters */
        gr2e = amprms.getGrad();
        ab1  = amprms.getRoot();
        ehn = amprms.getHelio();
        abv = amprms.getEarthv();

/* Apparent RA,Dec to Cartesian */
        p3 = Dcs2c ( ap );

/* Precession and nutation */
        p2 = Dimxv ( amprms.getPrecess(), p3 );

/* Aberration */
        ab1p1 = ab1 + 1.0;
        for ( i = 0; i < 3; i++ ) {
           p1[i] = p2[i];
        }
        for ( j = 0; j < 2; j++ ) {
           p1dv = Dvdv ( p1, abv );
           p1dvp1 = 1.0 + p1dv;
           w = 1.0 + p1dv / ab1p1;
           for ( i = 0; i < 3; i++ ) {
              p1[i] = ( p1dvp1 * p2[i] - w * abv[i] ) / ab1;
           }
           w = Dvn ( p1, p3 );
           for ( i = 0; i < 3; i++ ) {
              p1[i] = p3[i];
           }
        }

/* Light deflection */
        for ( i = 0; i < 3; i++ ) {
           p[i] = p1[i];
        }
        for ( j = 0; j < 5; j++ ) {
           pde = Dvdv ( p, ehn );
           pdep1 = 1.0 + pde;
           w = pdep1 - gr2e * pde;
           for ( i = 0; i < 3; i++ ) {
              p[i] = ( pdep1 * p1[i] - gr2e * ehn[i] ) / w;
           }
           w = Dvn ( p, p2 );
           for ( i = 0; i < 3; i++ ) {
              p[i] = p2[i];
           }
        }

/* Mean RA,Dec */
        ap = Dcc2s ( p );
        ap.setAlpha ( Dranrm ( ap.getAlpha() ) );
        return ap;
    }

/** <p>
 *  Precompute apparent to observed place parameters required by
 *  Aopqk and Oapqk.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd><ol>
 *  <li>  It is advisable to take great care with units, as even
 *       unlikely values of the input parameters are accepted and
 *       processed in accordance with the models used.
 *  </li>
 *  <li>  The date argument is UTC expressed as an MJD.  This is,
 *        strictly speaking, improper, because of leap seconds.  However,
 *        as long as the delta UT and the UTC are consistent there
 *        are no difficulties, except during a leap second.  In this
 *        case, the start of the 61st second of the final minute should
 *        begin a new MJD day and the old pre-leap delta UT should
 *        continue to be used.  As the 61st second completes, the MJD
 *        should revert to the start of the day as, simultaneously,
 *        the delta UTC changes by one second to its post-leap new value.
 *  </li>
 *  <li>   The delta UT (UT1-UTC) is tabulated in IERS circulars and
 *        elsewhere.  It increases by exactly one second at the end of
 *        each UTC leap second, introduced in order to keep delta UT
 *        within +/- 0.9 seconds.
 *  </li>
 *  <li>  IMPORTANT -- TAKE CARE WITH THE LONGITUDE SIGN CONVENTION.
 *        The longitude required by the present routine is east-positive,
 *        in accordance with geographical convention (and right-handed).
 *        In particular, note that the longitudes returned by the
 *        Obs routine are west-positive, following astronomical
 *        usage, and must be reversed in sign before use in the present
 *        routine.
 *  </li>
 *  <li>  The polar coordinates xp,yp can be obtained from IERS
 *        circulars and equivalent publications.  The maximum amplitude
 *        is about 0.3 arcseconds.  If xp,yp values are unavailable,
 *        use xp=yp=0.0.  See page B60 of the 1988 Astronomical Almanac
 *        for a definition of the two angles.
 *  </li>
 *  <li>  The height above sea level of the observing station, hm,
 *        can be obtained from the Astronomical Almanac (Section J
 *        in the 1988 edition), or via the routine Obs.  If p,
 *        the pressure in millibars, is available, an adequate
 *        estimate of hm can be obtained from the expression
 *  <p>
 *             hm = -29.3 * tsl * log ( p / 1013.25 );
 *  </p>
 *        where tsl is the approximate sea-level air temperature
 *        in deg K (See Astrophysical Quantities, C.W.Allen,
 *        3rd edition, section 52).  Similarly, if the pressure p
 *        is not known, it can be estimated from the height of the
 *        observing station, hm as follows:
 *  <p>
 *             p = 1013.25 * exp ( -hm / ( 29.3 * tsl ) );
 *  </p>
 *        Note, however, that the refraction is proportional to the
 *        pressure and that an accurate p value is important for
 *        precise work.
 *  </li>
 *  <li>  Repeated, computationally-expensive, calls to Aoppa for
 *        times that are very close together can be avoided by calling
 *        Aoppa just once and then using Aoppat for the subsequent
 *        times.  Fresh calls to Aoppa will be needed only when changes
 *        in the precession have grown to unacceptable levels or when
 *        anything affecting the refraction has changed.
 *  </li> </ol> </dd> </dl>
 *
 *  @param date UTC date/time (Modified Julian Date, JD-2400000.5) &amp;
 *              delta UT:  UT1-UTC (UTC seconds)
 *  @param pm mean longitude of the observer (radians, east +ve),
 *            mean geodetic latitude of the observer (radians),
 *            observer's height above sea level (metres) &amp;
 *            polar motion x-coordinate (radians)
 *  @param tdk local ambient temperature (DegK; std=273.155)
 *  @param pmb local atmospheric pressure (mB; std=1013.25)
 *  @param rh local relative humidity (in the range 0.0-1.0)
 *  @param wl effective wavelength (micron, e.g. 0.55)
 *  @param tlr tropospheric lapse rate (DegK/metre, e.g. 0.0065)
 *  @return aoprms star-independent apparent-to-observed parameters
 */

/*  Latest Revision: 29 November 2001 (RTP)
 *
 *  Given:
 *     date   d      UTC date/time (Modified Julian Date, JD-2400000.5)
 *     dut    d      delta UT:  UT1-UTC (UTC seconds)
 *     elongm d      mean longitude of the observer (radians, east +ve)
 *     phim   d      mean geodetic latitude of the observer (radians)
 *     hm     d      observer's height above sea level (metres)
 *     xp     d      polar motion x-coordinate (radians)
 *     yp     d      polar motion y-coordinate (radians)
 *     tdk    d      local ambient temperature (DegK; std=273.155)
 *     pmb    d      local atmospheric pressure (mB; std=1013.25)
 *     rh     d      local relative humidity (in the range 0.0-1.0)
 *     wl     d      effective wavelength (micron, e.g. 0.55)
 *     tlr    d      tropospheric lapse rate (DegK/metre, e.g. 0.0065)
 *
 *  Returned:
 *     aoprms d[14]  star-independent apparent-to-observed parameters:
 *
 *       (0)      geodetic latitude (radians)
 *       (1,2)    sine and cosine of geodetic latitude
 *       (3)      magnitude of diurnal aberration vector
 *       (4)      height (hm)
 *       (5)      ambient temperature (tdk)
 *       (6)      pressure (pmb)
 *       (7)      relative humidity (rh)
 *       (8)      wavelength (wl)
 *       (9)      lapse rate (tlr)
 *       (10,11)  refraction constants A and B (radians)
 *       (12)     longitude + eqn of equinoxes + sidereal DUT (radians)
 *       (13)     local apparent sidereal time (radians)
 *
 *  Defined in mac.h:  D2PI, DS2R
 *
 *  Called:  Geoc, Refco, Eqeqx, Aoppat
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AOParams Aoppa ( UTCdate date, ObsPosition obs,
                Cartesian pm, double tdk, double pmb, double rh,
                double wl, double tlr )
    {
        double cphim, xt, yt, zt, xc, yc, zc, elong, phi, uau, vau;
        double dut = date.getDeriv();
        double elongm = obs.getLongitude();
        double phim = obs.getLatitude();
        double hm = obs.getHeight();
        double xp = pm.getX();
        double yp = pm.getY();
        AOParams aoprms = new AOParams( ); 

/* Observer's location corrected for polar motion */
        cphim = Math.cos( phim );
        xt = Math.cos ( elongm ) * cphim;
        yt = Math.sin ( elongm ) * cphim;
        zt = Math.sin ( phim );
        xc = xt - xp * zt;
        yc = yt + yp * zt;
        zc = xp * xt - yp * yt + zt;

        elong = ( xc != 0.0 || yc != 0.0 ) ? Math.atan2 ( yc, xc ) : 0.0;

        phi = Math.atan2 ( zc, Math.sqrt ( xc * xc + yc * yc ) );
        aoprms.setLat( phi );

/* Magnitude of the diurnal aberration vector */
        double[] g = Geoc ( phi, hm );
        uau = g[0]; vau = g[1];
        aoprms.setDabb( D2PI * uau * SOLSID / C );

/* Copy the refraction parameters and compute the A &amp; B constants */
        aoprms.setHeight ( hm );
        aoprms.setTemp ( tdk );
        aoprms.setPressure ( pmb );
        aoprms.setHumidity ( rh );
        aoprms.setWavelength ( wl );
        aoprms.setLapserate ( tlr );
        double a[] = Refco ( hm, tdk, pmb, rh, wl, phi, tlr, 1e-10 );
        aoprms.setRefractA ( a[0] );
        aoprms.setRefractB ( a[1] );

/* Longitude + equation of the equinoxes + sidereal equivalent of DUT */
        aoprms.setLongplus ( elong + Eqeqx ( date.getDate() )
                             + dut * SOLSID * DS2R );

/* Sidereal time */
        Aoppat ( date.getDate(), aoprms );
        return aoprms;
    }

/** 
 *  Recompute the sidereal time in the apparent to observed place
 *  star-independent parameter block.
 *  <p>
 *  For more information, see Aoppa.
 *  </p>
 *
 *  @param date UTC date/time (Modified Julian Date, JD-2400000.5)
 *             (see Aoppa source for comments on leap seconds)
 *  @param aoprms star-independent apparent-to-observed parameters
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     date   double      UTC date/time (Modified Julian Date, JD-2400000.5)
 *                        (see Aoppa source for comments on leap seconds)
 *     aoprms double[14]  star-independent apparent-to-observed parameters
 *
 *       (0-11)   not required
 *       (12)     longitude + eqn of equinoxes + sidereal dut
 *       (13)     not required
 *
 *  Returned:
 *     aoprms double[14]  star-independent apparent-to-observed parameters:
 *
 *       (0-12)   not changed
 *       (13)     local apparent sidereal time (radians)
 *
 *  For more information, see Aoppa.
 *
 *  Called:  Gmst
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public void Aoppat ( double date, AOParams aoprms )
    {
        aoprms.setLocalTime ( Gmst ( date ) + aoprms.getLongplus() );
        return;
    }

/**
 *  Gregorian calendar to Modified Julian Date.
 *  <p> 
 *  (Includes century default feature:  use Cldj for years before 100AD.)
 *  </p>
 *
 *  @param iy Year in Gregorian calendar
 *  @param im Month in Gregorian calendar
 *  @param id Day in Gregorian calendar
 *  @return Modified Julian Date (JD-2400000.5) for 0 hrs
 *  @throws palError if bad day, month or year
 *
 *  <pre>
 *  0 = ok
 *  1 = bad year   (MJD not computed)
 *  2 = bad month  (MJD not computed)
 *  3 = bad day    (MJD computed)
 *  
 *  Acceptable years are 00-49, interpreted as 2000-2049,
 *                       50-99,     "       "  1950-1999,
 *                       100 upwards, interpreted literally.
 *  </pre>
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     iy,im,id   int      year, month, day in Gregorian calendar
 *
 *  Returned:
 *     *djm       double   Modified Julian Date (JD-2400000.5) for 0 hrs
 *     *j         int      status:
 *
 *  Called:  Cldj
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Caldj ( int iy, int im, int id ) throws palError
    {
        int ny;
        double d = 0.0;

/* Default century if appropriate */
        if ( ( iy >= 0 ) && ( iy <= 49 ) )
            ny = iy + 2000;
        else if ( ( iy >= 50 ) && ( iy <= 99 ) )
            ny = iy + 1900;
        else
            ny = iy;

/* Modified Julian Date */
        try {
           return Cldj ( ny, im, id );
        }
        catch ( palError e ) { throw e; }
    }

/**
 *  Gregorian calendar to Modified Julian Date.
 *  <p>
 *  The year must be -4699 (i.e. 4700BC) or later.
 *  </p> <p>
 *  The algorithm is derived from that of Hatcher 1984 (QJRAS 25, 53-55).
 *  </p>
 *
 *  @param iy Year in Gregorian calendar
 *  @param im Month in Gregorian calendar
 *  @param id Day in Gregorian calendar
 *  @return Modified Julian Date (JD-2400000.5) for 0 hrs
 *  @throws palError if bad day, month or year
 *
 *  <pre>
 *  0 = OK
 *  1 = bad year   (MJD not computed)
 *  2 = bad month  (MJD not computed)
 *  3 = bad day    (MJD computed)
 *  </pre>
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     iy,im,id     int    year, month, day in Gregorian calendar
 *
 *  Returned:
 *     *djm         double Modified Julian Date (JD-2400000.5) for 0 hrs
 *     *j           int    status:
 *                           0 = OK
 *                           1 = bad year   (MJD not computed)
 *                           2 = bad month  (MJD not computed)
 *                           3 = bad day    (MJD computed)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Cldj ( int iy, int im, int id ) throws palError
    {
        long iyL, imL;

/* Month lengths in days */
        final int mtab[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

/* Validate year */
        if ( iy < -4699 ) {
            Status = 1;
            throw new palError( Status, "Cldj: Bad Year" );
        }

/* Validate month */
        if ( ( im < 1 ) || ( im > 12 ) ) {
            Status = 2;
            throw new palError( Status, "Cldj: Bad Month" );
        }

/* Allow for leap year */
        mtab[1] = ( ( ( iy % 4 ) == 0 ) &&
             ( ( ( iy % 100 ) != 0 ) || ( ( iy % 400 ) == 0 ) ) ) ?
             29 : 28;

/* Validate day */
        if ( id < 1 || id > mtab[im-1] ) {
            Status = 3;
            throw new palError ( Status, "Cldj: Bad Day" );
        }

/* Lengthen year and month numbers to avoid overflow */
        iyL = (long) iy;
        imL = (long) im;

/* Perform the conversion */
        return ( ( 1461L * ( iyL - ( 12L - imL ) / 10L + 4712L ) ) / 4L
           + ( 306L * ( ( imL + 9L ) % 12L ) + 5L ) / 10L
           - ( 3L * ( ( iyL - ( 12L - imL ) / 10L + 4900L ) / 100L ) ) / 4L
           + (long) id - 2399904L );
    }


/**
 *  Convert degrees, arcminutes, arcseconds to radians.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd><ol>
 *  <li>The result is computed even if any of the range checks fail.</li>
 *  <li>The sign must be dealt with outside this routine.</li>
 *  </ol> </dd> </dl>
 *  @param ideg  Degrees
 *  @param iamin Arcminutes
 *  @param asec  Arcseconds
 *  @return Angle in radians
 *  @throws palError degrees, arcmins or arcsecs out of range
 *  <pre>
 *  Status returned:
 *      1 = ideg outside range 0-359
 *      2 = iamin outside range 0-59
 *      3 = asec outside range 0-59.999...
 *  </pre>
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     ideg        int       degrees
 *     iamin       int       arcminutes
 *     asec        double    arcseconds
 *
 *  Returned:
 *     *rad        double    angle in radians
 *     *j          int       status:  0 = OK
 *                                    1 = ideg outside range 0-359
 *                                    2 = iamin outside range 0-59
 *                                    3 = asec outside range 0-59.999...
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Daf2r ( int ideg, int iamin, double asec ) throws palError
    {
/* Preset status */
         Status = 0;
         double rad = 0.0;

/* Validate arcsec, arcmin, deg */
         if ( ( asec < 0.0 ) || ( asec >= 60.0 ) ) {
             Status = 3;
             throw new palError( Status, "Daf2r: asec outside range 0-59.999..." );
         }
         if ( ( iamin < 0 ) || ( iamin > 59 ) ) {
             Status = 2;
             throw new palError( Status, "Daf2r: iamin outside range 0-59" );
         }
         if ( ( ideg < 0 ) || ( ideg > 359 ) ) {
             Status = 1;
             throw new palError( Status, "Daf2r: ideg outside range 0-359" );
         }

/* Compute angle */
         rad = DAS2R * ( 60.0 * ( 60.0 * (double) ideg
                                  + (double) iamin )
                                           + asec );
         return rad;
    }

/**
 *  Increment to be applied to Coordinated Universal Time UTC to give
 *  International Atomic Time TAI.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The UTC is specified to be a date rather than a time to indicate
 *     that care needs to be taken not to specify an instant which lies
 *     within a leap second.  Though in most cases the utc argument can
 *     include the fractional part, correct behaviour on the day of a
 *     leap second can only be guaranteed up to the end of the second
 *     23:59:59.
 *  </li> <li>
 *  For epochs from 1961 January 1 onwards, the expressions from the
 *     file ftp://maia.usno.navy.mil/ser7/tai-utc.dat are used.
 *  </li> <li>
 *  The 5ms timestep at 1961 January 1 is taken from 2.58.1 (p87) of
 *     the 1992 Explanatory Supplement.
 *  </li> <li>
 *  UTC began at 1960 January 1.0 (JD 2436934.5) and it is improper
 *     to call the routine with an earlier epoch.  However, if this
 *     is attempted, the TAI-UTC expression for the year 1960 is used.
 *  </li> </ol>
 *  <dt>Latest leap second:</dt>
 *  <dd>2017 January 1</dd>
 *  </dl>
 *  @param utc UTC date as a modified JD (JD-2400000.5)
 *  @return TAI-UTC in seconds
 */

/*  Latest Revision: 21 November 2001 (RTP)
 *                   15 January 2009 (PWD)
 *                   23 April 2015 (MBT)
 *                   19 July 2016 (MBT)
 *
 *  Given:
 *     utc      double      UTC date as a modified JD (JD-2400000.5)
 *
 *  Result:  TAI-UTC in seconds
 *
 *     :-----------------------------------------:
 *     :                                         :
 *     :                IMPORTANT                :
 *     :                                         :
 *     :  This routine must be updated on each   :
 *     :     occasion that a leap second is      :
 *     :                announced                :
 *     :                                         :
 *     :  Latest leap second:  2017 January 1    :
 *     :                                         :
 *     :-----------------------------------------:
 *
 *  Copyright 1999 P.T.Wallace.  All rights reserved.
 */
    public double Dat ( double utc )
    {

/* - - - - - - - - - - - - - - - - - - - - - */
/* Add new code here on each occasion that a */
/* leap second is announced, and also update */
/* the preamble comments appropriately.      */
/* - - - - - - - - - - - - - - - - - - - - - */

/* 2017 January 1. */
        if ( utc >= 57754.0 ) return 37.0;

/* 2015 July 1. */
        if ( utc >= 57204.0 ) return 36.0;

/* 2012 July 1. */
        if ( utc >= 56109.0 ) return 35.0;

/* 2009 January 1 */
        if ( utc >= 54832.0 ) return 34.0;

/* 2006 January 1 */
        if ( utc >= 53736.0 ) return 33.0;

/* 1999 January 1 */
        if ( utc >= 51179.0 ) return 32.0;

/* 1997 July 1 */
        if ( utc >= 50630.0 ) return 31.0;

/* 1996 January 1 */
        if ( utc >= 50083.0 ) return 30.0;

/* 1994 July 1 */
        if ( utc >= 49534.0 ) return 29.0;

/* 1993 July 1 */
        if ( utc >= 49169.0 ) return 28.0;

/* 1992 July 1 */
        if ( utc >= 48804.0 ) return 27.0;

/* 1991 January 1 */
        if ( utc >= 48257.0 ) return 26.0;

/* 1990 January 1 */
        if ( utc >= 47892.0 ) return 25.0;

/* 1988 January 1 */
        if ( utc >= 47161.0 ) return 24.0;

/* 1985 July 1 */
        if ( utc >= 46247.0 ) return 23.0;

/* 1983 July 1 */
        if ( utc >= 45516.0 ) return 22.0;

/* 1982 July 1 */
        if ( utc >= 45151.0 ) return 21.0;

/* 1981 July 1 */
        if ( utc >= 44786.0 ) return 20.0;

/* 1980 January 1 */
        if ( utc >= 44239.0 ) return 19.0;

/* 1979 January 1 */
        if ( utc >= 43874.0 ) return 18.0;

/* 1978 January 1 */
        if ( utc >= 43509.0 ) return 17.0;

/* 1977 January 1 */
        if ( utc >= 43144.0 ) return 16.0;

/* 1976 January 1 */
        if ( utc >= 42778.0 ) return 15.0;

/* 1975 January 1 */
        if ( utc >= 42413.0 ) return 14.0;

/* 1974 January 1 */
        if ( utc >= 42048.0 ) return 13.0;

/* 1973 January 1 */
        if ( utc >= 41683.0 ) return 12.0;

/* 1972 July 1 */
        if ( utc >= 41499.0 ) return 11.0;

/* 1972 January 1 */
        if ( utc >= 41317.0 ) return 10.0;

/* 1968 February 1 */
        if ( utc >= 39887.0 ) return 4.2131700 + ( utc - 39126.0 ) * 0.002592;

/* 1966 January 1 */
        if ( utc >= 39126.0 ) return 4.3131700 + ( utc - 39126.0 ) * 0.002592;

/* 1965 September 1 */
        if ( utc >= 39004.0 ) return 3.8401300 + ( utc - 38761.0 ) * 0.001296;

/* 1965 July 1 */
        if ( utc >= 38942.0 ) return 3.7401300 + ( utc - 38761.0 ) * 0.001296;

/* 1965 March 1 */
        if ( utc >= 38820.0 ) return 3.6401300 + ( utc - 38761.0 ) * 0.001296;

/* 1965 January 1 */
        if ( utc >= 38761.0 ) return 3.5401300 + ( utc - 38761.0 ) * 0.001296;

/* 1964 September 1 */
        if ( utc >= 38639.0 ) return 3.4401300 + ( utc - 38761.0 ) * 0.001296;

/* 1964 April 1 */
        if ( utc >= 38486.0 ) return 3.3401300 + ( utc - 38761.0 ) * 0.001296;

/* 1964 January 1 */
        if ( utc >= 38395.0 ) return 3.2401300 + ( utc - 38761.0 ) * 0.001296;

/* 1963 November 1 */
        if ( utc >= 38334.0 ) return 1.9458580 + ( utc - 37665.0 ) * 0.0011232;

/* 1962 January 1 */
        if ( utc >= 37665.0 ) return 1.8458580 + ( utc - 37665.0 ) * 0.0011232;

/* 1961 August 1 */
        if ( utc >= 37512.0 ) return 1.3728180 + ( utc - 37300.0 ) * 0.001296;

/* 1961 January 1 */
        if ( utc >= 37300.0 ) return 1.4228180 + ( utc - 37300.0 ) * 0.001296;

/* Before that. */
        return 1.4178180 + ( utc - 37300.0 ) * 0.001296;

    }

/**
 *  Form the rotation matrix corresponding to a given axial vector.
 *  <p>
 *  A rotation matrix describes a rotation about some arbitrary axis.
 *  The axis is called the Euler axis, and the angle through which the
 *  reference frame rotates is called the Euler angle.  The axial
 *  vector supplied to this routine has the same direction as the
 *  Euler axis, and its magnitude is the Euler angle in radians.
 *  </p> <p>
 *  If axvec is null, the unit matrix is returned.
 *  </p> <p>
 *  The reference frame rotates clockwise as seen looking along
 *  the axial vector from the origin.
 *  </p>
 *
 *  @param axvec Axial vector (radians)
 *  @return Rotation matrix
 */

/*  Latest Revision: 29 November 2001 (RTP)
 *
 *  Given:
 *    axvec  double[3]     axial vector (radians)
 *
 *  Returned:
 *    rmat   double[3][3]  rotation matrix
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Dav2m ( double axvec[] )
    {
        double x, y, z, phi, s, c, w;
        double rmat[][] = new double[3][3];

/* Euler angle - magnitude of axial vector - and functions */
        x   = axvec[0];
        y   = axvec[1];
        z   = axvec[2];
        phi = Math.sqrt ( x * x + y * y + z * z );
        s   = Math.sin ( phi );
        c   = Math.cos ( phi );
        w   = 1.0 - c;

/* Euler axis - direction of axial vector (perhaps null) */
        if ( phi != 0.0 ) {
            x = x / phi;
            y = y / phi;
            z = z / phi;
        }

/* Compute the rotation matrix */
        rmat[0][0] = x * x * w + c;
        rmat[0][1] = x * y * w + z * s;
        rmat[0][2] = x * z * w - y * s;
        rmat[1][0] = x * y * w - z * s;
        rmat[1][1] = y * y * w + c;
        rmat[1][2] = y * z * w + x * s;
        rmat[2][0] = x * z * w + y * s;
        rmat[2][1] = y * z * w - x * s;
        rmat[2][2] = z * z * w + c;
        return rmat;
    }

/**
 *  Convert free-format input into double precision floating point,
 *  using Dfltin but with special syntax extensions.
 *  <p>
 *  The purpose of the syntax extensions is to help cope with mixed
 *  FK4 and FK5 data.  In addition to the syntax accepted by Dfltin,
 *  the following two extensions are recognized by dbjin:
 *  </p>
 *  <ol>
 *  <li>A valid non-null field preceded by the character 'B'
 *        (or 'b') is accepted.
 *  <li>A valid non-null field preceded by the character 'J'
 *         (or 'j') is accepted.
 *  </ol>
 *  <p>
 *  The calling program is notified of the incidence of either of these
 *  extensions through an supplementary status argument.  The rest of
 *  the arguments are as for Dfltin.
 *  </p>
 *  <p>
 *  The <strong>Status</strong> returned is one of the following:
 *  <dl>
 *  <dd>-1 or 0 = OK</dd>
 *  <dd>1 = null field</dd>
 *  <dd>2 = error</dd>
 *  </dl>
 *  <p>
 *  And the additional <strong>Flag</strong> is one of the following:
 *  <dl>
 *  <dd>0 = normal Dfltin syntax</dd>
 *  <dd>1 = 'B' or 'b'</dd>
 *  <dd>2 = 'J' or 'j'</dd>
 *  </dl>
 *  <p>
 *  For details of the basic syntax, see Dfltin.
 *  </p>
 *
 *  @param string String containing field to be decoded
 *  @param dreslt Previous Result
 *  @return Result
 */

/*  Latest Revision: 28 November 2001 (RTP)
 *
 *  Given:
 *     *string    char      string containing field to be decoded
 *     *nstrt     int       where to start decode (1st = 1)
 *
 *
 *  Returned:
 *     *nstrt     int       incremented
 *     *dreslt    double    result
 *     *jf1       int       dfltin status: -1 = -OK
 *                                          0 = +OK
 *                                         +1 = null field
 *                                         +2 = error
 *     *jf2       int       syntax flag:  0 = normal Dfltin syntax
 *                                       +1 = 'B' or 'b'
 *                                       +2 = 'J' or 'j'
 *
 *  Called:  Dfltin
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dbjin ( palString string, double dreslt )
    {
        int lenstr, na, j1a, nb, j1b;
        char c;
        double reslt = dreslt;

/* Preset syntax flag */
        Status = 0;
        Flag = 0;

/* Length of string */
        lenstr = string.length();

/* Attempt normal decode */
        dreslt = Dfltin ( string, dreslt );
        na = string.getPos();
        j1a = Status;

/* Proceed only if pointer still within string */
        if ( ( na > 0 ) && ( na <= lenstr ) ) {

/* See if Dfltin reported a null field */
            if ( j1a == 1 ) {

/* It did: examine character it stuck on */
                c = string.getChar( na );
                if ( c == 'B' || c == 'b' ) {

         /* 'B' or 'b' - provisionally note */
                    Flag = 1;
                } else if ( c == 'J' || c == 'j' ) {

         /* 'J' or 'j' - provisionally note */
                    Flag = 2;
                }

      /* Following B or J, attempt to decode a number */
                if ( Flag == 1 || Flag == 2 ) {
                     nb = na + 1; string.incrChar();
                     dreslt = Dfltin ( string, dreslt );
                     j1b = Status;

         /* If successful, copy pointer and status */
                     if ( j1b <= 0 ) {
                         na = nb;
                         j1a = j1b;

         /* If not, forget about the B or J */
                     } else {
                         Flag = 0;
                         string.setPos( na );
                     }
                 }
            }
       }

/* Return argument values and exit */
        Status = j1a;
        return dreslt;
    }

/**
 *  Conversion of position &amp; velocity in Cartesian coordinates
 *  to spherical coordinates.
 *
 *  @param v Cartesian position &amp; velocity vector
 *  @return Spherical Coordinates (Radians) -
 *         Longitude, Latitude, Radial plus derivitives
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     v     double[6]  Cartesian position &amp; velocity vector
 *
 *  Returned:
 *     *a    double     longitude (radians)
 *     *b    double     latitude (radians)
 *     *r    double     radial coordinate
 *     *ad   double     longitude derivative (radians per unit time)
 *     *bd   double     latitude derivative (radians per unit time)
 *     *rd   double     radial derivative
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Spherical Dc62s ( Cartesian v )
    {
       double x, y, z, xd, yd, zd, rxy2, rxy, r2, xyp;
       double a, b, r, ad, bd, rd;

/* Components of position/velocity vector. */
        x = v.getX();
        y = v.getY();
        z = v.getZ();
        xd = v.getXvel();
        yd = v.getYvel();
        zd = v.getZvel();

/* Component of R in XY plane squared. */
        rxy2 = x * x + y * y;

/* Modulus squared, with protection against null vector. */
        if ( ( r2 = rxy2 + z * z ) == 0.0 ) {
           x = xd;
           y = yd;
           z = zd;
           rxy2 = x * x + y * y;
           r2 = rxy2 + z * z;
        }

/* Position and velocity in spherical coordinates. */
        rxy = Math.sqrt ( rxy2 );
        xyp = x * xd + y * yd;
        if ( rxy2 != 0.0 ) {
           a = Math.atan2 ( y, x );
           b = Math.atan2 ( z, rxy );
           ad = ( x * yd - y * xd ) / rxy2;
           bd = ( zd * rxy2 - z * xyp ) / ( r2 * rxy );
        } else {
           a = 0.0;
           b = ( z != 0.0 ) ? Math.atan2 ( z, rxy ) : 0.0;
           ad = 0.0;
           bd = 0.0;
        }
        rd = ( ( r = Math.sqrt ( r2 ) ) != 0.0 ) ? ( xyp + z * zd ) / ( r ) : 0.0;
        return new Spherical( a, b, r, ad, bd, rd );
    }

/**
 *  Direction cosines to spherical coordinates.
 *  <p>
 *  The spherical coordinates are longitude (+ve anticlockwise
 *  looking from the +ve latitude pole) and latitude.  The
 *  Cartesian coordinates are right handed, with the x axis
 *  at zero longitude and latitude, and the z axis at the
 *  +ve latitude pole.
 *  </p> <p>
 *  If v is null, zero a and b are returned.
 *  At either pole, zero a is returned.
 *  </p>
 *
 *  @param v x, y, z vector
 *  @return spherical coordinates in radians (RA, Dec)
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *  Given:
 *     v      double[3]   x,y,z vector
 *
 *  Returned:
 *     *a,*b  double      spherical coordinates in radians
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Dcc2s ( double v[] )
    {
        double x, y, z, r;
        double a, b;

        x = v[0];
        y = v[1];
        z = v[2];
        r = Math.sqrt ( x * x + y * y );

        a = ( r != 0.0 ) ? Math.atan2 ( y, x ) : 0.0;
        b = ( z != 0.0 ) ? Math.atan2 ( z, r ) : 0.0;

        return new AngleDR( a, b );
    }

/**
 *  Spherical coordinates to direction cosines.
 *  <p>
 *  The spherical coordinates are longitude (+ve anticlockwise
 *  looking from the +ve latitude pole) and latitude.  The
 *  Cartesian coordinates are right handed, with the x axis
 *  at zero longitude and latitude, and the z axis at the
 *  +ve latitude pole.
 *  </p>
 *
 *  @param a spherical coordinates in radians (RA,Dec)
 *  @return x, y, z unit vector
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *
 *  Given:
 *     a,b       double      spherical coordinates in radians
 *                           (RA,Dec), (long,lat) etc
 *
 *  Returned:
 *     v         double[3]   x,y,z unit vector
 *
 *  Copyright P.T.Wallace.  All rights reserved.
*/
    public double[] Dcs2c ( AngleDR a )
    {
    
        double cosb;
        double v[] = new double[3];
        double a0 = a.getAlpha();
        double a1 = a.getDelta();

        cosb = Math.cos ( a1 );
        v[0] = Math.cos ( a0 ) * cosb;
        v[1] = Math.sin ( a0 ) * cosb;
        v[2] = Math.sin ( a1 );
 
        return v;
    }

/**
 *  Convert an interval in days into hours, minutes, seconds.
 *
 *  @param days interval in days
 *  @return hours, minutes, seconds, fraction
 */

/*  Latest Revision: 15 January 2002 (RTP)
 *
 *  Given:
 *     ndp       int      number of decimal places of seconds
 *     days      double   interval in days
 *
 *  Returned:
 *     *sign     char     '+' or '-'
 *     ihmsf     int[4]   hours, minutes, seconds, fraction
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public palTime Dd2tf ( double days )
    {
        double a, af;
        int ih, im, is;

/* Handle sign */
        char sign = (char) ( ( days < 0.0 ) ?  '-' : '+' );

/* Separate into fields */
        a = D2S * Math.abs ( days );
        ih = dint ( a / 3600.0 );
        a  = a - ih * 3600.0;
        im = dint ( a / 60.0 );
        a  = a - im * 60.0;
        is = dint ( a );
        af = (a - is);

/* Return results */
        palTime ihmsf = new palTime( ih, im, is, af, sign);
        return ihmsf;
    }

/**
 *  Form a rotation matrix from the Euler angles - three successive
 *  rotations about specified Cartesian axes.
 *  <p>
 *  A rotation is positive when the reference frame rotates
 *  anticlockwise as seen looking towards the origin from the
 *  positive region of the specified axis.
 *  </p> <p>
 *  The characters of order define which axes the three successive
 *  rotations are about.  A typical value is 'zxz', indicating that
 *  rmat is to become the direction cosine matrix corresponding to
 *  rotations of the reference frame through phi radians about the
 *  old z-axis, followed by theta radians about the resulting x-axis,
 *  then psi radians about the resulting z-axis.
 *  </p> <p>
 *  The axis names can be any of the following, in any order or
 *  combination:  x, y, z, uppercase or lowercase, 1, 2, 3.  Normal
 *  axis labelling/numbering conventions apply;  the xyz (=123)
 *  triad is right-handed.  Thus, the 'zxz' example given above
 *  could be written 'zxz' or '313' (or even 'zxz' or '3xz').  Order
 *  is terminated by length or by the first unrecognized character.
 *  </p> <p>
 *  Fewer than three rotations are acceptable, in which case the later
 *  angle arguments are ignored.  Zero rotations leaves rmat set to the
 *  identity matrix.
 *  </p>
 *
 *  @param order specifies about which axes the rotations occur
 *  @param phi 1st rotation (radians)
 *  @param theta 2nd rotation (   "   )
 *  @param psi 3rd rotation (   "   )
 *  @return rotation matrix
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *    *order char     specifies about which axes the rotations occur
 *    phi    double   1st rotation (radians)
 *    theta  double   2nd rotation (   "   )
 *    psi    double   3rd rotation (   "   )
 *
 *  Returned:
 *    rmat   double[3][3]  rotation matrix
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Deuler ( String order, double phi, double theta,
                 double psi )
    {
        int j, i, l, n, k;
        double result[][] = new double[3][3];
        double rotn[][] = new double[3][3];
        double angle, s, c , w;
        double wm[][] = new double[3][3];
        char axis;

/* Initialize result matrix */
        for ( j = 0; j < 3; j++ ) {
           for ( i = 0; i < 3; i++ ) {
             result[i][j] = ( i == j ) ? 1.0 : 0.0;
           }
        }

/* Establish length of axis string */
        l = order.length();

/* Look at each character of axis string until finished */
        for ( n = 0; n < 3; n++ ) {
            if ( n < l ) {

      /* Initialize rotation matrix for the current rotation */
                for ( j = 0; j < 3; j++ ) {
                    for ( i = 0; i < 3; i++ ) {
                        rotn[i][j] = ( i == j ) ? 1.0 : 0.0;
                    }
                }

      /* Pick up the appropriate Euler angle and take sine & cosine */
                switch ( n ) {
                    case 0 :
                         angle = phi;
                         break;
                    case 1 :
                         angle = theta;
                         break;
                    default:
                         angle = psi;
                         break;
                }
                s = Math.sin ( angle );
                c = Math.cos ( angle );

      /* Identify the axis */
                axis =  order.charAt( n );
                if ( ( axis == 'X' ) || ( axis == 'x' ) || ( axis == '1' ) ) {

         /* Matrix for x-rotation */
                    rotn[1][1] = c;
                    rotn[1][2] = s;
                    rotn[2][1] = -s;
                    rotn[2][2] = c;
                }
                else if ( ( axis == 'Y' ) || ( axis == 'y' ) || ( axis == '2' ) ) {

         /* Matrix for y-rotation */
                    rotn[0][0] = c;
                    rotn[0][2] = -s;
                    rotn[2][0] = s;
                    rotn[2][2] = c;
                }
                else if ( ( axis == 'Z' ) || ( axis == 'z' ) || ( axis == '3' ) ) {

         /* Matrix for z-rotation */
                    rotn[0][0] = c;
                    rotn[0][1] = s;
                    rotn[1][0] = -s;
                    rotn[1][1] = c;
                } else {

         /* Unrecognized character - fake end of string */
                    l = 0;
                }

      /* Apply the current rotation (matrix rotn x matrix result) */
                for ( i = 0; i < 3; i++ ) {
                    for ( j = 0; j < 3; j++ ) {
                        w = 0.0;
                        for ( k = 0; k < 3; k++ ) {
                             w += rotn[i][k] * result[k][j];
                        }
                        wm[i][j] = w;
                    }
                }
                for ( j = 0; j < 3; j++ ) {
                    for ( i= 0; i < 3; i++ ) {
                        result[i][j] = wm[i][j];
                    }
                }
           }
       }

/* Copy the result */
//       for ( j = 0; j < 3; j++ ) {
//           for ( i = 0; i < 3; i++ ) {
//               rmat[i][j] = result[i][j];
//           }
//       }
       return result;
    }

/* Definitions shared between Dfltin and idchf */
    private static final int NUMBER = 0;
    private static final int SPACE  = 1;
    private static final int EXPSYM = 2;
    private static final int PERIOD = 3;
    private static final int PLUS   = 4;
    private static final int MINUS  = 5;
    private static final int COMMA  = 6;
    private static final int OTHER  = 7;
    private static final int END    = 8;


/**
 *  Convert free-format input into double precision floating point.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *   <li>    A tab character is interpreted as a space, and lower
 *           case d,e are interpreted as upper case.
 *   </li>
 *   <li>    The basic format is #^.^@#^ where # means + or -,
 *           ^ means a decimal subfield and @ means D or E.
 *   </li>
 *   <li>    Spaces:
 *             Leading spaces are ignored.
 *             Embedded spaces are allowed only after # and D or E,
 *             and after . where the first ^ is absent.
 *             Trailing spaces are ignored;  the first signifies
 *             end of decoding and subsequent ones are skipped.
 *   </li>
 *   <li>    Field separators:
 *             Any character other than +,-,0-9,.,D,E or space may be
 *             used to end a field.  Comma is recognized by Dfltin
 *             as a special case; it is skipped, leaving the
 *             pointer on the next character.  See 12, below.
 *   </li>
 *   <li>    Both signs are optional.  The default is +.
 *   </li>
 *   <li>    The mantissa defaults to 1.
 *   </li>
 *   <li>    The exponent defaults to e0.
 *   </li>
 *   <li>    The decimal subfields may be of any length.
 *   </li>
 *   <li>    The decimal point is optional for whole numbers.
 *   </li>
 *   <li>    A null field is one that does not begin with
 *           +,-,0-9,.,D or E, or consists entirely of spaces.
 *           If the field is null, jflag is set to 1 and dreslt
 *           is left untouched.
 *   </li>
 *   <li>    nstrt = 1 for the first character in the string.
 *   </li>
 *   <li>    On return from Dfltin, nstrt is set ready for the next
 *           decode - following trailing blanks and (if used) the
 *           comma separator.  If a separator other than comma is
 *           being used, nstrt must be incremented before the next
 *           call to Dfltin.
 *   </li>
 *   <li>    Errors (jflag=2) occur when:
 *             a)  A +, -, D or E is left unsatisfied.
 *             b)  The decimal point is present without at least
 *                 one decimal subfield.
 *             c)  An exponent more than 100 has been presented.
 *   </li>
 *   <li>    When an error has been detected, nstrt is left
 *           pointing to the character following the last
 *           one used before the error came to light.  This
 *           may be after the point at which a more sophisticated
 *           program could have detected the error.  For example,
 *           Dfltin does not detect that '1e999' is unacceptable
 *           until the whole field has been read.
 *   </li>
 *   <li>    Certain highly unlikely combinations of mantissa &amp;
 *           exponent can cause arithmetic faults during the
 *           decode, in some cases despite the fact that they
 *           together could be construed as a valid number.
 *   </li>
 *   <li>    Decoding is left to right, one pass.
 *   </li>
 *   <li>    End of field may occur in either of two ways:
 *             a)  As dictated by the string length.
 *             b)  Detected during the decode.
 *                 (b overrides a.)
 *   </li>
 *   <li>    See also Flotin and Intin.
 *   </li>
 *  </ol> </dd> </dl>
 *  @param string String containing field to be decoded
 *  @param dreslt Previous result
 *  @return Result
 */

/*  Latest Revision: 22 November 2001 (RTP)
 *
 *  Given:
 *     *string     char       string containing field to be decoded
 *     *nstrt      int        where to start decode (1st = 1)
 *
 *  Returned:
 *     *nstrt      int        advanced to next field
 *     *dreslt     double     result
 *     *jflag      int        -1 = -OK, 0 = +OK, 1 = null field, 2 = error
 *
 *  Called:  idchf
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dfltin ( palString string, double dreslt )
    {
        int l_string, nptr, digit = 0, jflag;
        double reslt = dreslt;

/* Current state of the decode and the values it can take */

        int state;

        final int seek_sign = 100;
        final int neg_mant = 200;
        final int seek_1st_leading_digit = 300;
        final int accept_leading_digit = 400;
        final int seek_digit_when_none_before_pt = 500;
        final int seek_trailing_digit = 600;
        final int accept_trailing_digit = 700;
        final int accept_uns_exp_no_mant = 800;
        final int seek_sign_exp = 900;
        final int neg_exp = 1000;
        final int seek_1st_exp_digit  = 1100;
        final int accept_exp_digit  = 1200;
        final int end_of_field  = 1300;
        final int build_result  = 1310;
        final int seeking_end_of_field = 1620;
        final int next_field_OK = 1720;
        final int next_field_default = 9100;
        final int null_field = 9110;
        final int next_field_error = 9200;
        final int error = 9210;
        final int done = 9900;


        int msign, nexp, ndp, isignx, nstrt;
        double dmant;
        String Errmsg = null;

/* Find string length */
        l_string = string.length( );

/* Current character index */
        nptr = string.getPos();

/* Set defaults: mantissa & sign, exponent & sign, decimal place count */
        dmant = 0.0;
        msign = 1;
        nexp = 0;
        isignx = 1;
        ndp = 0;

/* Initialize state to "looking for sign" */
        state = seek_sign;

/* Loop until decode is complete */
        while ( state != done ) {
            switch ( state ) {

                case seek_sign :

            /* Look for sign */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_leading_digit;
                            digit = string.getDigit();
                            break;
                        case SPACE :
                            state = seek_sign;
                            break;
                        case EXPSYM :
                            state = accept_uns_exp_no_mant;
                            break;
                        case PERIOD :
                            state = seek_digit_when_none_before_pt;
                            break;
                        case PLUS :
                            state = seek_1st_leading_digit;
                            break;
                        case MINUS :
                            state = neg_mant;
                            break;
                        case OTHER :
                            state = next_field_default;
                            break;
                        case COMMA :
                        case END :
                            state = null_field;
                            break;
                        default :
                            state = error;
                    }
                    break;

                case neg_mant :

      /* Negative mantissa */
                   msign = -1;

                case seek_1st_leading_digit :

      /* Look for first leading decimal */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_leading_digit;
                            digit = string.getDigit();
                           break;
                        case SPACE :
                            state = seek_1st_leading_digit;
                            break;
                        case EXPSYM :
                            state = accept_uns_exp_no_mant;
                            break;
                        case PERIOD :
                            state = seek_digit_when_none_before_pt;
                            break;
                        case PLUS :
                        case MINUS :
                        case COMMA :
                        case OTHER :
                            state = next_field_error;
                            break;
                        case END :
                        default :
                            state = error;
                    }
                    break;

                case accept_leading_digit :

      /* Accept leading decimals */
                    dmant = dmant * 1e1 + digit;
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_leading_digit;
                            digit = string.getDigit();
                            break;
                        case SPACE :
                            state = build_result;
                            break;
                        case EXPSYM :
                            state = seek_sign_exp;
                            break;
                        case PERIOD :
                            state = seek_trailing_digit;
                            break;
                        case PLUS :
                        case MINUS :
                        case COMMA :
                        case OTHER :
                            state = end_of_field;
                            break;
                        case END :
                            state = build_result;
                            break;
                        default :
                            state = error;
                    }
                    break;

                case seek_digit_when_none_before_pt :

      /* Look for decimal when none preceded the point */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_trailing_digit;
                            digit = string.getDigit();
                            break;
                        case SPACE :
                            state = seek_digit_when_none_before_pt;
                            break;
                        case EXPSYM :
                        case PERIOD :
                        case PLUS :
                        case MINUS :
                        case COMMA :
                        case OTHER :
                            state = next_field_error;
                            break;
                        case END :
                        default :
                            state = error;
                    }
                    break;

                case seek_trailing_digit :

      /* Look for trailing decimals */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_trailing_digit;
                            digit = string.getDigit();
                            break;
                        case EXPSYM :
                            state = seek_sign_exp;
                            break;
                        case PERIOD :
                        case PLUS :
                        case MINUS :
                        case COMMA :
                        case OTHER :
                            state = end_of_field;
                            break;
                        case SPACE :
                        case END :
                            state = build_result;
                            break;
                        default :
                            state = error;
                    }
                    break;

                case accept_trailing_digit :

      /* Accept trailing decimals */
                    ndp++;
                    digit = string.getDigit();
                    dmant = dmant * 1e1 + digit;
                    state = seek_trailing_digit;
                    break;

                case accept_uns_exp_no_mant :

      /* Exponent symbol first in field: default mantissa to 1 */
                    dmant = 1.0;

                case seek_sign_exp :

      /* Look for sign of exponent */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_exp_digit;
                            digit = string.getDigit();
                            break;
                        case SPACE :
                            state = seek_sign_exp;
                            break;
                        case PLUS :
                            state = seek_1st_exp_digit;
                            break;
                        case MINUS :
                            state = neg_exp;
                            break;
                        case EXPSYM :
                        case PERIOD :
                        case COMMA :
                        case OTHER :
                            state = next_field_error;
                            break;
                        case END :
                        default :
                            state = error;
                    }
                    break;

                case neg_exp :

      /* Exponent negative */
                    isignx = -1;

                case seek_1st_exp_digit :

      /* Look for first digit of exponent */
                    nptr++;
                    switch ( idchf ( string ) ) {
                        case NUMBER :
                            state = accept_exp_digit;
                            digit = string.getDigit();
                            break;
                        case SPACE :
                            state = seek_1st_exp_digit;
                            break;
                        case EXPSYM :
                        case PERIOD :
                        case PLUS :
                        case MINUS :
                        case COMMA :
                        case OTHER :
                            state = next_field_error;
                            break;
                        case END :
                        default :
                            state = error;
                    }
                    break;

                case accept_exp_digit :

      /* Use exponent digit */
                    digit = string.getDigit();
                    nexp = nexp * 10 + digit;
                    if ( nexp > 100 ) {
                        state = next_field_error;
                    } else {

         /* Look for subsequent digits of exponent */
                        nptr++;
                        switch ( idchf ( string ) ) {
                            case NUMBER :
                                state = accept_exp_digit;
                                digit = string.getDigit();
                                break;
                            case SPACE :
                                state = build_result;
                                break;
                            case EXPSYM :
                            case PERIOD :
                            case PLUS :
                            case MINUS :
                            case COMMA :
                            case OTHER :
                                state = end_of_field;
                                break;
                            case END :
                                state = build_result;
                                break;
                            default :
                                state = error;
                        }
                    }
                    break;

                case end_of_field :

      /* Off the end of the field: move pointer back */
                    nptr--; string.backChar();

                case build_result :

      /* Combine exponent and decimal place count */
                    nexp = nexp * isignx - ndp;

      /* Sign of exponent? */
                    if ( nexp >= 0 ) {

         /* Positive exponent: scale up */
                        while ( nexp >= 10 ) {
                           dmant *= 1e10;
                           nexp -= 10;
                        }
                        while ( nexp >= 1 ) {
                           dmant *= 1e1;
                           nexp--;
                        }
                    } else {

         /* Negative exponent: scale down */
                        while ( nexp <= -10 ) {
                           dmant /= 1e10;
                           nexp += 10;
                        }
                        while ( nexp <= -1 ) {
                           dmant /= 1e1;
                           nexp++;
                        }
                    }

      /* Get result & status */
                    if ( msign == 1 ) {
                         reslt = dmant;
                         Status = 0;
                    } else {
                         reslt = -dmant;
                         Status = -1;
                    }

                case seeking_end_of_field :

      /* Skip to end of field */
                    switch ( idchf ( string ) ) {
                        case SPACE :
                            state = seeking_end_of_field;
                            break;
                        case NUMBER :
                        case EXPSYM :
                        case PERIOD :
                        case PLUS :
                        case MINUS :
                        case OTHER :
                            state = next_field_OK;
                            break;
                        case COMMA :
                        case END :
                            state = done;
                            break;
                        default :
                            state = error;
                    }
                    break;

                case next_field_OK :

      /* Next field terminates successful decode */
                    nptr--; string.backChar();
                    state = done;
                    break;

                case next_field_default :

      /* Next field terminates null decode */
                    nptr--; string.backChar();

                case null_field :

      /* Null decode */
                    Status = 1;
                    Errmsg = "Dfltin: Null decode";
                    state = done;
                    break;

                case next_field_error :

      /* Next field detected prematurely */
                    nptr--; string.backChar();

                case error :

      /* Decode has failed: set bad status */
                    Status = 2;
                    Errmsg = "Dfltin: Decode has failed";
                    state = done;
                    string.incrChar();
                    break;

                default :
                    state = error;
            }
        }
//        if ( Status > 0 ) throw new palError( Status, Errmsg );

/* Finished: return updated pointer and the status */
        return reslt;
    }

/*
 *  Internal routine used by Dfltin:
 *  identify next character in string.
 *
 *  Given:
 *     l_string    int         length of string
 *     string      char*       string
 *     nptr        int*        character to be identified (1st = 0)
 *
 *  Returned:
 *     nptr        int*        incremented unless end of field
 *     digit      int*        0-9 if character was a numeral
 *     digit       double*     (double) digit
 *
 *  Returned (function value):
 *     idchf       int         vector for identified character:
 *
 *                                value   meaning
 *
 *                                NUMBER  0-9
 *                                SPACE   space or tab
 *                                EXPSYM  D, d, E or e
 *                                PERIOD  .
 *                                PLUS    +
 *                                MINUS   -
 *                                COMMA   ,
 *                                OTHER   else
 *                                END     outside field
 *
 *  Last revision:   22 November 2001 (RTP)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    private static int idchf ( palString string )
    {
        int ivec, ictab;
        char c = ' ';
        int nptr = string.getPos();
        int digit = 0;
        int l_string = string.length( );

/* Character/vector tables */

        final int NCREC = 20;
        final char kctab[] = { '0','1','2','3','4','5','6','7','8','9',
                                ' ','\t',
                                'D','d','E','e',
                                '.', '+', '-', ',' };

        final int kvtab[] = { NUMBER, NUMBER, NUMBER, NUMBER, NUMBER,
                               NUMBER, NUMBER, NUMBER, NUMBER, NUMBER,
                               SPACE, SPACE,
                               EXPSYM, EXPSYM, EXPSYM, EXPSYM,
                               PERIOD, PLUS, MINUS, COMMA };


/* Initialize returned value */
        ivec = OTHER;

/* Pointer outside field? */
        if ( nptr < 0 || nptr >= l_string ) {

   /* Yes: prepare returned value */
            ivec = END;

        } else {

   /* Not end of field: identify character */
            c = string.getNextChar ( );
            for ( ictab = 0; ictab < NCREC; ictab++ ) {
                if ( kctab [ ictab ] == c ) {

         /* Recognized */
                    ivec = kvtab [ ictab ];

         /* Allow for numerals */
                    digit = ictab;

         /* Quit the loop */
                    break;
                }
            }

   /* Increment pointer */
            nptr++;
        }
        string.setDigit( digit );

/* Return the value identifying the character */
        return ivec;
    }

/**
 *  Performs the 3-d backward unitary transformation.
 *  <dl>
 *  <dd> 
 *  vector vb = (inverse of matrix dm) * vector va
 *  </dd>
 *  </dl>
 *  <p>
 *  (n.b. The matrix must be unitary, as this routine assumes that
 *  the inverse and transpose are identical).
 *  </p>
 *
 *  @param dm n x n matrix
 *  @param va vector
 *  @return vector
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     dm       double[3][3]   matrix
 *     va       double[3]      vector
 *
 *  Returned:
 *     vb       double[3]      result vector
 *
 *  Note:  va and vb may be the same array.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Dimxv ( double dm[][], double va[] )
    {
        double w;
        double vw[] = new double[3];

/* Inverse of matrix dm * vector va -> vector vw */
        for ( int j = 0; j < 3; j++ ) {
            w = 0.0;
            for ( int i = 0; i < 3; i++ ) {
                w += dm[i][j] * va[i];
            }
            vw[j] = w;
        }
        return vw;
    }

/**
 *  Modified Julian Date to Gregorian calendar, expressed
 *  in a form convenient for formatting messages (namely
 *  rounded to a specified precision, and with the fields
 *  stored in a single array).
 *  <p> Any date after 4701BC March 1 is accepted.
 *  </p>
 *  <p> Large ndp values risk internal overflows.  It is typically safe
 *  to use up to ndp=4.
 *  </p>
 *  <p> The algorithm is derived from that of Hatcher 1984 (QJRAS 25, 53-55).
 *  </P>
 *
 *  @param djm Modified Julian Date (JD-2400000.5)
 *  @return year, month, day, fraction in Gregorian calendar
 */

/*  Latest Revision: 15 January 2002 (RTP)
 *
 *  Given:
 *     ndp      int       number of decimal places of days in fraction
 *     djm      double    Modified Julian Date (JD-2400000.5)
 *
 *  Returned:
 *     iymdf    int[4]    year, month, day, fraction in Gregorian calendar
 *     *j       int       status:  nonzero = out of range
 *
 *  Defined in mac.h:  dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public mjDate Djcal ( double djm ) throws palError
    {
        double fd, df, f, d;
        long jd, n4, nd10;
        int yr, mn, dy;
        Status = 0;

/* Validate */
        if ( ( djm <= -2395520.0 ) || ( djm >= 1.0e9 ) ) {
            Status = - 1;
            throw new palError( Status, "Djcal: Date out of Range");
        } else {

   /* Separate day and fraction */
            d = Math.floor( djm );
            f = djm - d;
            if ( f < 0.0 ) f += 1.0;

   /* Express day in Gregorian calendar */
            jd = (long) dnint ( d ) + 2400001L;
            n4 = 4L * ( jd + ( ( 2L * ( ( 4L * jd - 17918L ) / 146097L)
                                       * 3L ) / 4L + 1L ) / 2L - 37L );
            nd10 = 10L * ( ( ( n4 - 237L ) % 1461L ) / 4L ) + 5L;
            yr = (int) ( ( n4 / 1461L ) - 4712L );
            mn = (int) ( ( ( nd10 / 306L + 2L ) % 12L ) + 1L );
            dy = (int) ( ( nd10 % 306L ) / 10L + 1L );
        }
        try {
            mjDate date = new mjDate(yr, mn, dy, f);
            return date;
        }
        catch ( palError e ) {
            throw e;
        }
    }

/**
 *  Modified Julian Date to Gregorian year, month, day,
 *  and fraction of a day.
 *  <p>
 *  The algorithm is derived from that of Hatcher 1984 (QJRAS 25, 53-55).
 *  </p>
 *  @param djm Modified Julian Date (JD-2400000.5)
 *  @return Year, month, day and fraction of day
 *  @throws palError unacceptable date (before 4701BC March 1)
 *
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     djm      double     Modified Julian Date (JD-2400000.5)
 *
 *  Returned:
 *     *iy      int        year
 *     *im      int        month
 *     *id      int        day
 *     *fd      double     fraction of day
 *     *j       int        status:
 *                      -1 = unacceptable date (before 4701BC March 1)
 *
 *  Defined in mac.h:  dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public mjDate Djcl ( double djm ) throws palError
    {
        double f, d;
        long jd, n4, nd10;
        Status = 0;

/* Check if date is acceptable */
        if ( ( djm <= -2395520.0 ) || ( djm >= 1e9 ) ) {
            Status = -1;
            throw new palError( Status,
                          "Djcl: unacceptable date (before 4701BC March 1)" );
        } else {

   /* Separate day and fraction */
            f = dmod ( djm, 1.0 );
            if ( f < 0.0 ) f += 1.0;
            d = djm - f;
            d = dnint ( d );

   /* Express day in Gregorian calendar */
            jd = (long) dnint ( d ) + 2400001;
            n4 = 4L*(jd+((6L*((4L*jd-17918L)/146097L))/4L+1L)/2L-37L);
            nd10 = 10L*(((n4-237L)%1461L)/4L)+5L;
            try {
                mjDate date = new mjDate ( (int) (n4/1461L-4712L),
                   (int) (((nd10/306L+2L)%12L)+1L),
                   (int) ((nd10%306L)/10L+1L),
                   f );
                return date;
            }
            catch ( palError e ) {
                throw e;
            }
        }
    }

/**
 *  From a rotation matrix, determine the corresponding axial vector.
 *  <p>
 *  A rotation matrix describes a rotation about some arbitrary axis.
 *  The axis is called the Euler axis, and the angle through which the
 *  reference frame rotates is called the Euler angle.  The axial
 *  vector returned by this routine has the same direction as the
 *  Euler axis, and its magnitude is the Euler angle in radians.  (The
 *  magnitude and direction can be separated by means of the routine
 *  Dvn.)
 *  </p>
 *  <p> The reference frame rotates clockwise as seen looking along
 *  the axial vector from the origin.
 *  </p>
 *  <p> If rmat is null, so is the result.
 *  </p>
 *
 *  @param rmat Rotation matrix
 *  @return Axial vector (radians)
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *    rmat   double[3][3]   rotation matrix
 *
 *  Returned:
 *    axvec  double[3]      axial vector (radians)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Dm2av ( double rmat[][] )
    {
        double x, y, z, s2, c2, phi, f;
        double axvec[] = new double[3];

        x = rmat[1][2] - rmat[2][1];
        y = rmat[2][0] - rmat[0][2];
        z = rmat[0][1] - rmat[1][0];
        s2 = Math.sqrt ( x * x + y * y + z * z );
        if ( s2 != 0.0 )
        {
            c2 = rmat[0][0] + rmat[1][1] + rmat[2][2] - 1.0;
            phi = Math.atan2 ( s2, c2 );
            f = phi / s2;
            axvec[0] = x * f;
            axvec[1] = y * f;
            axvec[2] = z * f;
        } else {
            axvec[0] = 0.0;
            axvec[1] = 0.0;
            axvec[2] = 0.0;
        }
        return axvec;
    }

/**
 *  Matrix inversion &amp; solution of simultaneous equations.
 *  <dl>
 *  <dt>For the set of n simultaneous equations in n unknowns:</dt>
 *  <dd>   a.y = x </dd>
 *
 *  <dt>where:</dt>
 *  <dd>   a is a non-singular n x n matrix
 *  <br>   y is the vector of n unknowns
 *  <br>   x is the known vector
 *  </dd>
 *  <dt>Dmat computes:</dt>
 *  <dd>   the inverse of matrix a
 *  <br>   the determinant of matrix a
 *  <br>   the vector of n unknowns
 *  </dd>
 *  <dt>Arguments:</dt>
 *  <dd><table>
 *  <tr><th>symbol  </th> <th>type</th> <th>dimension</th> <th>before</th>
 *      <th>after</tr>
 *  <tr><td>   n </td> <td>  int</td>     <td> </td> <td>no. of unknowns</td>
 *      <td>unchanged</td> </tr>
 *  <tr><td>  *a </td> <td>  double </td> <td> [n][n]</td> <td>matrix  </td>
 *      <td>inverse</td> </tr>
 *  <tr><td>  *y </td> <td>  double </td> <td>  [n]  </td> <td>vector  </td>
 *      <td>solution</td> </tr>
 *  <tr><td>  *d </td> <td>  double </td> <td>       </td> <td> -      </td>
 *      <td>determinant</td> </tr>
 *  <tr><td> *jf </td> <td> int </td>    <td>       </td> <td> -      </td>
 *      <td># singularity flag</td> </tr>
 *  <tr><td> *iw </td> <td> int </td>    <td>   [n] </td> <td> -      </td>
 *      <td>workspace</td> </tr>
 *  </table> </dd>
 *  <dd># jf is the singularity flag.  If the matrix is non-singular,
 *       jf=0 is returned.  If the matrix is singular, jf=-1 &amp; d=0.0 are
 *       returned.  In the latter case, the contents of array a on return
 *       are undefined.</dd>
 *
 *  <dt> Algorithm:</dt>
 *  <dd>   Gaussian elimination with partial pivoting.</dd>
 *
 *  <dt> Speed:</dt>
 *  <dd>   Very fast.</dd>
 *
 *  <dt> Accuracy:</dt>
 *  <dd>   Fairly accurate - errors 1 to 4 times those of routines optimized
 *     for accuracy.</dd>
 *  </dl>
 *
 *  @param a Matrix
 *  @param y Vector
 *  @return Determinant
 */

/*  Latest Revision: 22 November 2001 (RTP)
 *
 *  Example call (note handling of "adjustable dimension" 2D array):
 *
 *     double a[MP][MP], v[MP], d;
 *     int j, iw[MP];
 *      :
 *     Dmat ( n, (double *) a, v, &amp;d, &amp;j, iw );
 *
 *  Last revision:   22 November 2001 (RTP)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dmat ( double a[][], double y[] )
    {
       int k, imx, i, j, ki;
       double amx, t, yk;
       int n = y.length;
       int iw[] = new int[n];

/* Pointers to beginnings of rows in matrix a[n][n] */

        double ak[],    /* row k    */
               ai[],    /* row i    */
               aimx[];  /* row imx  */

        int jf = 0;
        double d = 1.0;
// C        for ( k = 0, ak = a; k < n; k++, ak += n ) {
        for ( k = 0; k < n; k++ ) {
            ak = a[k];
            amx = Math.abs ( ak[k] );
            imx = k;
            aimx = ak;
            if ( k != n ) {
// C                for ( i = k + 1, ai = ak + n; i < n; i++, ai += n ) {
                for ( i = k + 1; i < n; i++ ) {
                    ai = a[i];
                    t = Math.abs ( ai[k] );
                    if ( t > amx ) {
                        amx = t;
                        imx = i;
                        aimx = ai;
                    }
                }
            }
            if ( amx < TINY ) {
                jf = -1;
            } else {
                if ( imx != k ) {
                   for ( j = 0; j < n; j++ ) {
                       t = ak[j];
                       ak[j] = aimx[j];
                       aimx[j] = t;
                   }
                   t = y[k];
                   y[k] = y[imx];
                   y[imx] = t;
                   d = - d;
                }
                iw[k] = imx;
                d *= ak[k];
                if ( Math.abs ( d ) < TINY ) {
                    jf = -1;
                } else {
                    ak[k] = 1.0 / ak[k];
                    for ( j = 0; j < n; j++ ) {
                        if ( j != k ) {
                            ak[j] *= ak[k];
                        }
                    }
                    yk = y[k] * ak[k];
                    y[k] = yk;
// C                    for ( i = 0, ai = a; i < n; i++, ai += n ) {
                    for ( i = 0; i < n; i++ ) {
                        ai = a[i];
                        if ( i != k ) {
                            for ( j = 0; j < n; j++ ) {
                                if ( j != k ) {
                                    ai[j] -= ai[k] * ak[j];
                                }
                            }
                            y[i] -= ai[k] * yk;
                        }
                    }
// C                    for ( i = 0, ai = a; i < n; i++, ai += n ) {
                    for ( i = 0; i < n; i++ ) {
                        ai = a[i];
                        if ( i != k ) {
                             ai[k] *= - ak[k];
                        }
                    }
                }
            }
        }
        if ( jf != 0 ) {
            d = 0.0;
        } else {
            for ( k = n; k-- > 0; ) {
                ki = iw[k];
                if ( k != ki ) {
// C                    for ( i = 0, ai = a; i < n; i++, ai += n ) {
                    for ( i = 0; i < n; i++ ) {
                        ai = a[i];
                        t = ai[k];
                        ai[k] = ai[ki];
                        ai[ki] = t;
                    }
                }
            }
        }
        return d;
    }

/**
 *  Product of two 3x3 matrices.
 *  <dl>
 *  <dd> matrix c  =  matrix a  x  matrix b</dd>
 *  <dt>Note:</dt>
 *  <dd>the same array may be nominated more than once.</dd>
 *  </dl>
 *
 *  @param a Matrix
 *  @param b Matrix
 *  @return Matrix result
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     a      double[3][3]        matrix
 *     b      double[3][3]        matrix
 *
 *  Returned:
 *     c      double[3][3]        matrix result
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Dmxm ( double a[][], double b[][] )
    {
        double w;
        double c[][] = new double[3][3];

/* Multiply into scratch matrix */
        for ( int i = 0; i < 3; i++ ) {
           for ( int j = 0; j < 3; j++ ) {
              w = 0.0;
              for ( int k = 0; k < 3; k++ ) {
                 w += a[i][k] * b[k][j];
              }
              c[i][j] = w;
           }
        }
        return c;

    }

/**
 *  Performs the 3-d forward unitary transformation.
 *
 *  <dl>
 *  <dd>vector vb = matrix dm * vector va</dd>
 *  </dl>
 *
 *  @param dm Matrix
 *  @param va Vector
 *  @return Result vector
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     dm       double[3][3]    matrix
 *     va       double[3]       vector
 *
 *  Returned:
 *     vb       double[3]       result vector
 *
 *  Note:  va and vb may be the same array.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Dmxv ( double dm[][], double va[] )
    {
       int i, j;
       double w, vb[] = new double[3];

/* Matrix dm * vector va -> vector vw */
       for ( j = 0; j < 3; j++ ) {
           w = 0.0;
           for ( i = 0; i < 3; i++ ) {
               w += dm[j][i] * va[i];
           }
           vb[j] = w;
       }
       return vb;
    }

/**
 *  Convert an angle in radians into degrees, arcminutes, arcseconds.
 *  WARNING: broken doesn't preserve sign in "palTime.toString()".
 *
 *  @param angle angle in radians
 *  @return Time as degrees, arcminutes, arcseconds, fraction
 */

/*  Latest Revision: 15 January 2002 (RTP)
 *
 *  Given:
 *     ndp       int      number of decimal places of arcseconds
 *     angle     double    angle in radians
 *
 *  Returned:
 *     sign      *char    '+' or '-'
 *     idmsf     int[4]   degrees, arcminutes, arcseconds, fraction
 *
 *  Called:
 *     Dd2tf
 *
 *  Defined in mac.h:  D15B29
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public palTime Dr2af ( double angle )
    {
/* Scale then use days to h,m,s routine */
        return Dd2tf ( (double) angle * D15B2P );
    }

/**
 *  Convert an angle in radians to hours, minutes, seconds.
 *
 *  @param angle Angle in radians
 *  @return Time as hours, minutes, seconds, fraction(integer)
 */

/*  Latest Revision: 15 January 2002 (RTP)
 *
 *  Given:
 *     ndp       int          number of decimal places of seconds
 *     angle     double       angle in radians
 *
 *  Returned:
 *     sign      char*        '+' or '-'
 *     ihmsf     int[4]       hours, minutes, seconds, fraction
 *
 *  Called:
 *     Dd2tf
 *
 *  Defined in mac.h:  D2PI
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public palTime Dr2tf ( double angle )
    {
/* Scale then use days to h,m,s routine */
        return Dd2tf ( angle / D2PI );
    }

/**
 *  Normalize angle into range +/- pi.
 *
 *  @param angle Angle in radians
 *  @return Angle expressed in the range +/- &pi;
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     angle     double      the angle in radians
 *
 *  The result is angle expressed in the +/- pi (double precision).
 *
 *  Defined in mac.h:  DPI, D2PI, dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Drange ( double angle )
    {
        double w, w1;

        w = dmod ( angle, D2PI );
//        return ( Math.abs ( w ) < DPI ) ? w : w - dsign ( D2PI, angle );
        w1 = ( angle < 0.0 ? -D2PI : D2PI);
        return ( Math.abs ( w ) < DPI  ? w : w - w1 );
    }

/**
 *  Normalize angle into range 0-2 &pi;.
 *
 *  @param angle Angle in radians
 *  @return Angle expressed in the range 0-2 pi
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *  Given:
 *     angle     double      the angle in radians
 *
 *  The result is angle expressed in the range 0-2 pi (double).
 *
 *  Defined in mac.h:  D2PI, dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dranrm ( double angle ){

        double w;

        w = dmod ( angle, D2PI );
        return ( w >= 0.0 ? w : w + D2PI );
    }

/**
 *  Velocity component in a given direction due to Earth rotation.
 *  <dl>
 *  <dt>Sign convention:</dt>
 *  <dd>The result is +ve when the observer is receding from the
 *     given point on the sky.
 *  </dd>
 *  <dt>Accuracy:</dt>
 *  <dd>The simple algorithm used assumes a spherical Earth, of
 *     a radius chosen to give results accurate to about 0.0005 km/s
 *     for observing stations at typical latitudes and heights.  For
 *     applications requiring greater precision, use the routine
 *     Pvobs.
 *  </dd>
 *  </dl>
 *  @param phi Latitude of observing station (geodetic)
 *  @param r Apparent RA,Dec (radians)
 *  @param st local apparent sidereal time
 *  @return  Component of Earth rotation in direction ra,da (km/s)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     phi     float    latitude of observing station (geodetic)
 *     ra,da   float    apparent RA,Dec
 *     st      float    local apparent sidereal time
 *
 *     phi, ra, dec and st are all in radians.
 *
 *  Result:
 *     Component of Earth rotation in direction ra,da (km/s)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */

    public double Drverot ( double phi, AngleDR r, double st )
    {
        return ESPEED * Math.cos ( phi ) * Math.sin ( st - r.getAlpha() ) *
                        Math.cos ( r.getDelta() );
    }

/**
 *  Velocity component in a given direction due to the rotation
 *  of the Galaxy.
 *  <dl>
 *  <dt>Sign convention:</dt>
 *  <dd>   The result is +ve when the dynamical LSR is receding from the
 *     given point on the sky.
 *  </dd>
 *  <dt>Note:</dt>
 *  <dd>The Local Standard of Rest used here is a point in the
 *      vicinity of the Sun which is in a circular orbit around
 *      the Galactic centre.
 *      Sometimes called the "dynamical" LSR,
 *      it is not to be confused with a "kinematical" LSR, which
 *      is the mean standard of rest of star catalogues or stellar
 *      populations.
 *  </dd>
 *  <dt>Reference:</dt>
 *  <dd>The orbital speed of 220 km/s used here comes from
 *      Kerr &amp; Lynden-Bell (1986), MNRAS, 221, p1023.
 *  </dd>
 *  </dl>
 *  @param r2000 J2000.0 mean RA,Dec (radians)
 *  @return Component of dynamical LSR motion in direction r2000,d2000 (km/s)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     r2000,d2000   float    J2000.0 mean RA,Dec (radians)
 *
 *  Result:
 *     Component of dynamical LSR motion in direction r2000,d2000 (km/s)
 *
 *  Called:
 *     Cs2c, Vdv
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Drvgalc ( AngleDR r2000 )
    {
/*
 *
 *  LSR velocity due to Galactic rotation
 *
 *  Speed = 220 km/s
 *
 *  Apex  = L2,B2  90deg, 0deg
 *        = RA,Dec  21 12 01.1  +48 19 47  J2000.0
 *
 *  This is expressed in the form of a J2000.0 x,y,z vector:
 *
 *      va(1) = x = -speed*cos(ra)*cos(dec)
 *      va(2) = y = -speed*sin(ra)*cos(dec)
 *      va(3) = z = -speed*sin(dec)
 */
        final double va[] = { -108.70408, 97.86251, -164.33610 };
        double vb[];

/* Convert given J2000 RA,dec to x,y,z */
        vb = Dcs2c ( r2000 );

/* Compute dot product with LSR motion vector */
        return Dvdv ( va, vb );
    }

/**
 *  Velocity component in a given direction due to the combination
 *  of the rotation of the Galaxy and the motion of the Galaxy
 *  relative to the mean motion of the local group.
 *  <dl>
 *  <dt> Sign convention:</dt>
 *  <dd> The result is +ve when the Sun is receding from the
 *     given point on the sky.
 *  </dd>
 *  <dt>Reference:</dt>
 *  <dd> IAU trans 1976, 168, p201. </dd>
 *  </dl>
 *  @param r2000 J2000.0 mean RA,Dec (radians)
 *  @return Component of solar motion in direction r2000,d2000 (km/s)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     r2000,d2000   float    J2000.0 mean RA,Dec (radians)
 *
 *  Result:
 *     Component of solar motion in direction r2000,d2000 (km/s)
 *
 *  Called:
 *     Cs2c, Vdv
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Drvlg ( AngleDR r2000 )
    {
/*
 *  Solar velocity due to galactic rotation and trantion
 *
 *  speed = 300 km/s
 *
 *  apex  = l2,b2  90deg, 0deg
 *        = RA,dec  21 12 01.1  +48 19 47  J2000.0
 *
 *  This is expressed in the form of a J2000.0 x,y,z vector:
 *
 *      va(1) = x = -speed*cos(ra)*cos(dec)
 *      va(2) = y = -speed*sin(ra)*cos(dec)
 *      va(3) = z = -speed*sin(dec)
 */
        final double va[] = { -148.23284, 133.44888, -224.09467 };
        double vb[];

/* Convert given J2000 RA,dec to x,y,z */
        vb = Dcs2c ( r2000 );

/* Compute dot product with solar motion vector */
        return Dvdv ( va, vb );
    }

/**
 *  Velocity component in a given direction due to the Sun's
 *  motion with respect to the dynamical Local Standard of Rest.
 *  <dl>
 *  <dt>Sign convention:</dt>
 *  <dd>The result is +ve when the Sun is receding
 *      from the given point on the sky.
 *  </dd>
 *  <dt>Note:</dt>
 *  <dd>The Local Standard of Rest used here is the "dynamical" LSR,
 *      a point in the vicinity of the Sun which is in a circular
 *      orbit around the Galactic centre.  The Sun's motion with
 *      respect to the dynamical LSR is called the "peculiar" solar motion.
 *  <p>
 *      There is another type of LSR, called a "kinematical" LSR.  A
 *      kinematical LSR is the mean standard of rest of specified star
 *      catalogues or stellar populations, and several slightly
 *      different kinematical LSRs are in use.  The Sun's motion with
 *      respect to an agreed kinematical LSR is known as the "standard"
 *      solar motion.  To obtain a radial velocity correction with
 *      respect to an adopted kinematical LSR use the routine Rvlsrk.
 *  </p> </dd>
 *  <dt>Reference:</dt>
 *  <dd>Delhaye (1965), in "Stars and Stellar Systems", vol 5, p73.
 *  </dd> </dl>
 *  @param r2000 J2000.0 mean RA,Dec (radians)
 *  @return Component of "peculiar" solar motion in direction R2000,D2000 (km/s)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     r2000,d2000   float    J2000.0 mean RA,Dec (radians)
 *
 *  Result:
 *     Component of "peculiar" solar motion in direction R2000,D2000 (km/s)
 *
 *  Called:  Cs2c, Vdv
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Drvlsrd ( AngleDR r2000 )
    {
/*
 *  Peculiar solar motion from Delhaye 1965: in Galactic Cartesian
 *  coordinates (+9,+12,+7) km/s.  This corresponds to about 16.6 km/s
 *  towards Galactic coordinates L2 = 53 deg, B2 = +25 deg, or RA,Dec
 *  17 49 58.7 +28 07 04 J2000.
 *
 *  The solar motion is expressed here in the form of a J2000.0
 *  equatorial Cartesian vector:
 *
 *      va(1) = x = -speed*cos(ra)*cos(dec)
 *      va(2) = y = -speed*sin(ra)*cos(dec)
 *      va(3) = z = -speed*sin(dec)
 */
        final double va[] = { 0.63823, 14.58542, -7.80116 };
        double vb[];

/* Convert given J2000 RA,dec to x,y,z */
        vb = Dcs2c ( r2000 );

/* Compute dot product with solar motion vector */
        return Dvdv ( va, vb );
    }

/**
 *  Velocity component in a given direction due to the Sun's motion
 *  with respect to an adopted kinematic Local Standard of Rest.
 *  <dl>
 *  <dt>Sign convention:</dt>
 *  <dd>The result is +ve when the Sun is receding from the given point on
 *      the sky.
 *  </dd>
 *  <dt>Note:</dt>
 *  <dd>The Local Standard of Rest used here is one of several
 *      "kinematical" LSRs in common use.  A kinematical LSR is the
 *      mean standard of rest of specified star catalogues or stellar
 *      populations.  The Sun's motion with respect to a kinematical
 *      LSR is known as the "standard" solar motion.
 *  <p>
 *         There is another sort of LSR, the "dynamical" LSR, which is a
 *         point in the vicinity of the Sun which is in a circular orbit
 *         around the Galactic centre.  The Sun's motion with respect to
 *         the dynamical LSR is called the "peculiar" solar motion.  To
 *         obtain a radial velocity correction with respect to the
 *         dynamical LSR use the routine Rvlsrd.
 *  </p> </dd>
 *
 *  <dt>Reference:</dt>
 *  <dd>Delhaye (1965), in "Stars and Stellar Systems", vol 5, p73.
 *  </dd> </dl>
 *  @param  r2000 J2000.0 mean RA,Dec (radians)
 *  @return Component of "standard" solar motion in direction R2000,D2000 (km/s)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     r2000,d2000   float    J2000.0 mean RA,Dec (radians)
 *
 *  Result:
 *     Component of "standard" solar motion in direction R2000,D2000 (km/s)
 *
 *  Called:  Cs2c, Vdv
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Drvlsrk ( AngleDR r2000 )
    {
/*
 *
 *  Standard solar motion (from Methods of Experimental Physics, ed Meeks,
 *  vol 12, part C, sec 6.1.5.2, p281):
 *
 *  20 km/s towards RA 18h Dec +30d (1900).
 *
 *  The solar motion is expressed here in the form of a J2000.0
 *  equatorial Cartesian vector:
 *
 *      va(1) = x = -speed*cos(ra)*cos(dec)
 *      va(2) = y = -speed*sin(ra)*cos(dec)
 *      va(3) = z = -speed*sin(dec)
 */
        final double va[] = { -0.29000, 17.31726, -10.00141 };
        double vb[];

/* Convert given J2000 RA,dec to x,y,z */
        vb = Dcs2c ( r2000 );

/* Compute dot product with solar motion vector */
        return Dvdv ( va, vb );
    }

/**
 *  Conversion of position &amp; velocity in spherical coordinates
 *  to Cartesian coordinates.
 *
 *  @param s Spherical coordinates (longitude, latitude, radial)
 *  @return Cartesian position &amp; velocity vector
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     a     double      longitude (radians)
 *     b     double      latitude (radians)
 *     r     double      radial coordinate
 *     ad    double      longitude derivative (radians per unit time)
 *     bd    double      latitude derivative (radians per unit time)
 *     rd    double      radial derivative
 *
 *  Returned:
 *     v     double[6]   Cartesian position &amp; velocity vector
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Cartesian Ds2c6 ( Spherical s )
    {
        double sa, ca, sb, cb, rcb, x, y, rbd, w;
        Cartesian v;
        double a = s.getLong(), b = s.getLat(), r = s.getRadial();
        double ad = s.getLongDeriv(), bd = s.getLatDeriv(),
               rd = s.getRadialDeriv();

/* Useful functions */
        sa = Math.sin ( a );
        ca = Math.cos ( a );
        sb = Math.sin ( b );
        cb = Math.cos ( b );
        rcb = r * cb;
        x = rcb * ca;
        y = rcb * sa;
        rbd = r * bd;
        w = rbd * sb - cb * rd;

/* Position */
//        v[0] = x;
//        v[1] = y;
//        v[2] = r * sb;

/* Velocity */
//        v[3] = -y * ad - w * ca;
//        v[4] = x * ad - w * sa;
//        v[5] = rbd * cb + sb * rd;
        return new Cartesian( x, y, r*sb,
                                -y * ad - w * ca,
                                 x * ad - w * sa,
                                 rbd * cb + sb * rd );
    }

/**
 *   Projection of spherical coordinates onto tangent plane
 *  ('gnomonic' projection - 'standard coordinates').

 *  @param r spherical coordinates of point to be projected
 *  @param rz spherical coordinates of tangent point
 *  @return rectangular coordinates on tangent plane (xi, eta)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     ra,dec      double   spherical coordinates of point to be projected
 *     raz,decz    double   spherical coordinates of tangent point
 *
 *  Returned:
 *     *xi,*eta    double   rectangular coordinates on tangent plane
 *     *j          int      status:   0 = OK, star on tangent plane
 *                                    1 = error, star too far from axis
 *                                    2 = error, antistar on tangent plane
 *                                    3 = error, antistar too far from axis
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Ds2tp ( AngleDR r, AngleDR rz ) throws palError
    {
        double sdecz, sdec, cdecz, cdec, radif, sradif, cradif, denom, xi, eta;
        double ra = r.getAlpha(), dec = r.getDelta();
        double raz = rz.getAlpha(), decz = rz.getDelta();
        String Errmsg = null;

/* Trig functions */
        sdecz = Math.sin ( decz );
        sdec = Math.sin ( dec );
        cdecz = Math.cos ( decz );
        cdec = Math.cos ( dec );
        radif = ra - raz;
        sradif = Math.sin ( radif );
        cradif = Math.cos ( radif );

/* Reciprocal of star vector length to tangent plane */
        denom = sdec * sdecz + cdec * cdecz * cradif;

/* Handle vectors too far from axis */
        if ( denom > TINY ) {
           Status = 0;
        } else if ( denom >= 0.0 ) {
           Status = 1; Errmsg = "star too far from axis";
           denom = TINY;
        } else if ( denom > -TINY ) {
           Status = 2; Errmsg = "antistar on tangent plane";
           denom = -TINY;
        } else {
           Status = 3; Errmsg = "antistar too far from axis";
        }

/* Compute tangent plane coordinates (even in dubious cases) */
        xi = cdec * sradif / denom;
        eta = ( sdec * cdecz - cdec * sdecz * cradif ) / denom;
        return new AngleDR ( xi, eta );
    }

/**
 *  Estimate the offset between dynamical time and Universal Time
 *  for a given historical epoch.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>Depending on the epoch, one of three parabolic approximations
 *     is used:
 *  <dl> <dd><table>
 *  <tr><td>before 979 </td>
 *      <td>Stephenson &amp; Morrison's 390 BC to AD 948 model</td> </tr>
 *  <tr><td>   979 to 1708 </td>
 *      <td>  Stephenson &amp; Morrison's 948 to 1600 model</td> </tr>
 *  <tr><td>   after 1708 </td>
 *      <td>   McCarthy &amp; Babcock's post-1650 model</td> </tr>
 *  </table> </dd> </dl>
 *     The breakpoints are chosen to ensure continuity:  they occur
 *     at places where the adjacent models give the same answer as
 *     each other. </li>
 *
 *  <li>The accuracy is modest, with errors of up to 20 sec during
 *     the interval since 1650, rising to perhaps 30 min by 1000 BC.
 *     Comparatively accurate values from AD 1600 are tabulated in
 *     the Astronomical Almanac (see section K8 of the 1995 AA).
 *
 *  <li>The use of double-precision for both argument and result is
 *     purely for compatibility with other LIB time routines.
 *
 *  <li>The models used are based on a lunar tidal acceleration value
 *     of -26.00 arcsec per century.
 *  </ol> </dd>
 *  <dt>Reference:</dt>
 *  <dd>Explanatory Supplement to the Astronomical Almanac,
 *      ed P.K.Seidelmann, University Science Books (1992),
 *      section 2.553, p83.  This contains references to
 *      the Stephenson &amp; Morrison and McCarthy &amp; Babcock
 *      papers.
 *  </dd> </dl>
 *
 *  @param epoch (Julian) epoch (e.g. 1850.0)
 *  @return Estimate of ET-UT (after 1984, TT-UT1) at
 *          the given epoch, in seconds
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     epoch    double    (Julian) epoch (e.g. 1850.0)
 *
 *  The result is a rough estimate of ET-UT (after 1984, TT-UT1) at
 *  the given epoch, in seconds.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dt ( double epoch )
    {
        double t, w, s;

/* Centuries since 1800 */
        t = ( epoch - 1800.0 ) / 100.0;

/* Select model */
        if ( epoch >= 1708.185161980887 ) {

/* Post-1708: use McCarthy & Babcock */
            w = t - 0.19;
            s = 5.156 + 13.3066 * w * w;
        } else {
            if ( epoch >= 979.0258204760233 ) {

/* 979-1708: use Stephenson & Morrison's 948-1600 model */
                s = 25.5 * t * t;
            } else {

/* Pre-979: use Stephenson & Morrison's 390 BC to AD 948 model */
                s = 1360.0 + ( 320.0 + 44.3 * t ) * t;
            }
        }

/* Result */
        return s;
    }

/**
 *  Convert hours, minutes, seconds to days.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The result is computed even if any of the range checks fail.</li>
 *
 *  <li>The sign must be dealt with outside this routine.</li>
 *  </ol> </dd> </dl>
 *
 *  @param ihour Hours
 *  @param imin Minutes
 *  @param sec Seconds
 *  @return Interval in days
 *  @throws palError Hour, Min or Sec out of range
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     ihour       int           hours
 *     imin        int           minutes
 *     sec         double        seconds
 *
 *  Returned:
 *     *days       double        interval in days
 *     *j          int           status:  0 = OK
 *                                        1 = ihour outside range 0-23
 *                                        2 = imin outside range 0-59
 *                                        3 = sec outside range 0-59.999...
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dtf2d ( int ihour, int imin, double sec ) throws palError
    {
/* Preset status */
        Status = 0;

/* Validate sec, min, hour */
        if ( ( sec < 0.0 ) || ( sec >= 60.0 ) ) {
            Status = 1;
            throw new palError( 1, "Dtf2d: sec outside range 0-59.999..." );
        }
        if ( ( imin < 0 ) || ( imin > 59 ) ) {
            Status = 2;
            throw new palError( 1, "Dtf2d: min outside range 0-59" );
        }
        if ( ( ihour < 0 ) || ( ihour > 23 ) ) {
            Status = 1;
            throw new palError( 1, "Dtf2d: hour outside range 0-23" );
        }

/* Compute interval */
        return ( 60.0 * ( 60.0 * (double) ihour + (double) imin ) + sec ) / D2S;
    }

/**
 *  Convert hours, minutes, seconds to radians.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The result is computed even if any of the range checks fail.</li>
 *
 *  <li>The sign must be dealt with outside this routine.</li>
 *  </ol> </dd> </dl>
 *
 *  @param ihour Hours
 *  @param imin Minutes
 *  @param sec Seconds
 *  @return Angle in radians
 *  @throws palError Hour, Min or Sec out of range
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     ihour       int           hours
 *     imin        int           minutes
 *     sec         double        seconds
 *
 *  Returned:
 *     *rad        double        angle in radians
 *     *j          int           status:  0 = OK
 *                                        1 = ihour outside range 0-23
 *                                        2 = imin outside range 0-59
 *                                        3 = sec outside range 0-59.999...
 *
 *  Called:
 *     Dtf2d
 *
 *  Defined in mac.h:  D2PI
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dtf2r ( int ihour, int imin, double sec ) throws palError
    {
        double turns;

/* Convert to turns */
        try {
            turns = Dtf2d ( ihour, imin, sec );
        }
        catch ( palError e ) { 
            throw e;
        }

/* To radians */
        return D2PI * turns;
    }

/**
 * Transform tangent plane coordinates into spherical.
 *
 *  @param x Tangent plane rectangular coordinates (xi, eta)
 *  @param rz Spherical coordinates of tangent point (ra, dec)
 *  @return  Spherical coordinates (0-2pi,+/-pi/2)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     xi,eta      double   tangent plane rectangular coordinates
 *     raz,decz    double   spherical coordinates of tangent point
 *
 *  Returned:
 *     *ra,*dec    double   spherical coordinates (0-2pi,+/-pi/2)
 *
 *  Called:  Dranrm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Dtp2s ( AngleDR x, AngleDR rz )
    {
        double sdecz, cdecz, denom, a1, d1;
        double xi = x.getAlpha(), eta = x.getDelta();
        double raz = rz.getAlpha(), decz = rz.getDelta();

        sdecz = Math.sin ( decz );
        cdecz = Math.cos ( decz );
        denom = cdecz - eta * sdecz;
        a1 = Dranrm ( Math.atan2 ( xi, denom ) + raz );
        d1 = Math.atan2 ( sdecz + eta * cdecz,
                        Math.sqrt ( xi * xi + denom * denom ) );
        return new AngleDR ( a1, d1 );
    }

/**
 *  Increment to be applied to Coordinated Universal Time UTC to give
 *  Terrestrial Time TT (formerly Ephemeris Time ET).
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The UTC is specified to be a date rather than a time to indicate
 *     that care needs to be taken not to specify an instant which lies
 *     within a leap second.  Though in most cases UTC can include the
 *     fractional part, correct behaviour on the day of a leap second
 *     can only be guaranteed up to the end of the second 23:59:59.</li>
 *
 *  <li>Pre 1972 January 1 a fixed value of 10 + ET-TAI is returned.</li>
 *
 *  <li>See also the routine Dt, which roughly estimates ET-UT for
 *     historical epochs.</li>
 *  </ol> </dd> </dl>
 *
 *  @param utc UTC date as a modified JD (JD-2400000.5)
 *  @return TT-UTC in seconds
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     utc    double    UTC date as a modified JD (JD-2400000.5)
 *
 *  Result:  TT-UTC in seconds
 *
 *  Called:  Dat
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dtt ( double utc )
    {
        return 32.184 + Dat ( utc );
    }

/**
 *  Scalar product of two 3-vectors.
 *
 *  @param va First vector
 *  @param vb Second vector
 *  @return   The scalar product va.vb
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *      va      double(3)     first vector
 *      vb      double(3)     second vector
 *
 *
 *  The result is the scalar product va.vb (double precision)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dvdv ( double va[], double vb[] )
    {
        return va[0] * vb[0] + va[1] * vb[1] + va[2] * vb[2];
    }

/**
 *  Normalizes a 3-vector also giving the modulus.
 *  <dl>
 *  <dt>Note:</dt>
 *  <dd>v and uv may be the same array.</dd>
 *  </dl>
 *  <p>
 *  If the modulus of v is zero, uv is set to zero as well.
 *  </p>
 *
 *  @param v Vector
 *  @param uv (Returned) Unit vector in direction of v
 *  @return modulus of v
 */
 
/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     v       double[3]      vector
 *
 *  Returned:
 *     uv      double[3]      unit vector in direction of v
 *     *vm     double         modulus of v
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Dvn ( double v[], double uv[] )
    {
        double w1, w2, vm;

/* Modulus */
        w1 = 0.0;
        for ( int i = 0; i < 3; i++ ) {
           w2 = v[i];
           w1 += w2 * w2;
        }
        w1 = Math.sqrt ( w1 );
        vm = w1;

/* Normalize the vector */
        w1 = ( w1 > 0.0 ) ? w1 : 1.0;

        for ( int i = 0; i < 3; i++ ) {
           uv[i] = v[i] / w1;
        }
        return vm;
    }

/**
 *  Vector product of two 3-vectors.
 *  <dl>
 *  <dt>Note:</dt>
 *  <dd>the same vector may be specified more than once.</dd>
 *  </dl>
 *
 *  @param va First vector
 *  @param vb Second vector 
 *  @return   Vector result
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     va      double[3]     first vector
 *     vb      double[3]     second vector
 *
 *  Returned:
 *     vc      double[3]     vector result
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Dvxv ( double va[], double vb[] )
    {
        double vw[] = new double[3];
        int i;

/* Form the vector product va cross vb */
        vw[0] = va[1] * vb[2] - va[2] * vb[1];
        vw[1] = va[2] * vb[0] - va[0] * vb[2];
        vw[2] = va[0] * vb[1] - va[1] * vb[0];

/* Return the result */
        return vw;
   }

/**
 *  Transformation from ecliptic coordinates to J2000.0
 *  equatorial coordinates.
 *
 *  @param dl ecliptic longitude and latitude
 *                            (mean of date, IAU 1980 theory, radians)
 *  @param date TDB (loosely ET) as Modified Julian Date (JD-2400000.5)
 *  @return J2000.0 mean (RA, Dec) (radians)
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     dl,db       double      ecliptic longitude and latitude
 *                             (mean of date, IAU 1980 theory, radians)
 *     date        double      TDB (loosely ET) as Modified Julian Date
 *                                              (JD-2400000.5)
 *  Returned:
 *     *dr,*dd     double      J2000.0 mean RA,Dec (radians)
 *
 *  Called:
 *     Dcs2c, Ecmat, Dimxv, Prec, Epj, Dcc2s,
 *     Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Ecleq ( AngleDR dl, double date )
    {
        double rmat[][], v1[], v2[];

/* Spherical to Cartesian */
        v1 = Dcs2c ( dl );

/* Ecliptic to equatorial */
        rmat = Ecmat ( date );
        v2 = Dimxv ( rmat, v1 );

/* Mean of date to J2000 */
        rmat = Prec ( 2000.0, Epj ( date ) );
        v1 = Dimxv ( rmat, v2 );

/* Cartesian to spherical */
        AngleDR dd = Dcc2s ( v1 );

/* Express in conventional ranges */
        dd.setAlpha( Dranrm ( dd.getAlpha() ) );
        dd.setDelta( Drange ( dd.getDelta() ) );
        return dd;
    }

/**
 *  Form the equatorial to ecliptic rotation matrix (IAU 1980 theory).
 *  <dl>
 *  <dt>References:</dt>
 *  <dd>Murray, C.A., Vectorial Astrometry, section 4.3.</dd>
 *
 *  <dt>Note:</dt>
 *  <dd>The matrix is in the sense   v[ecl]  =  rmat * v[equ];  the
 *      equator, equinox and ecliptic are mean of date. </dd>
 *  </dl>
 *
 *  @param date TDB (loosely ET) as Modified Julian Date (JD-2400000.5)
 *  @return Rotation matrix
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     date     double         TDB (loosely ET) as Modified Julian Date
 *                                            (JD-2400000.5)
 *  Returned:
 *     rmat     double[3][3]   matrix
 *
 *  Called:  Deuler
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Ecmat ( double date )
    {
        double t, eps0;

/* Interval between basic epoch J2000.0 and current epoch (JC) */
        t = ( date - 51544.5 ) / 36525.0;

/* Mean obliquity */
        eps0 = DAS2R *
            ( 84381.448 + ( -46.8150 + ( -0.00059 + 0.001813 * t ) * t ) * t );

/* Matrix */
        return Deuler ( "X", eps0, 0.0, 0.0 );
    }

/**
 *  Conversion of Modified Julian Date to Besselian epoch.
 *  <dl>
 *  <dt>Reference:</dt>
 *  <dd>Lieske,J.H., 1979. Astron. Astrophys.,73,282.</dd>
 *  </dl>
 *
 *  @param date Modified Julian Date (JD - 2400000.5)
 *  @return The Besselian epoch
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     date     double      Modified Julian Date (JD - 2400000.5)
 *
 *  The result is the Besselian epoch.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Epb ( double date )
    {
        return 1900.0 + ( date - 15019.81352 ) / 365.242198781;
    }

/**
 * Conversion of Besselian epoch to Modified Julian Date.
 *  <dl>
 *  <dt>Reference:</dt>
 *  <dd>Lieske,J.H., 1979. Astron. Astrophys.,73,282.</dd>
 *  </dl>
 *
 *  @param epb Besselian epoch
 *  @return Modified Julian Date (JD - 2400000.5)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     epb      double       Besselian epoch
 *
 *  The result is the Modified Julian Date (JD - 2400000.5).
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Epb2d ( double epb )
    {
        return 15019.81352 + ( epb - 1900.0 ) * 365.242198781;
    }

/**
 *  Convert an epoch into the appropriate form - 'B' or 'J'.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li> The result is always either equal to or very close to
 *       the given epoch e.  The routine is required only in
 *       applications where punctilious treatment of heterogeneous
 *       mixtures of star positions is necessary.</li>
 *
 *  <li> k0 and k are not validated, and only their first characters
 *       are used, interpreted as follows:
 *       <ul>
 *       <li>If k0 and k are the same the result is e.</li>
 *       <li>If k0 is 'B' or 'b' and k isn't, the conversion is J to B.</li>
 *       <li>In all other cases, the conversion is B to J.</li>
 *       </ul> </li>
 *  </ol> </dd> </dl>
 *
 *  @param k0 Form of result:  'B'=Besselian, 'J'=Julian
 *  @param k Form of given epoch:  'B' or 'J'
 *  @param e Epoch
 *  @return Epoch in appropriate form
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     k0    char        form of result:  'B'=Besselian, 'J'=Julian
 *     k     char        form of given epoch:  'B' or 'J'
 *     e     double      epoch
 *
 *  Called:  Epb, Epj2d, Epj, Epb2d
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Epco ( char k0, char k, double e )
    {
        double result;
        char c;

        c = Character.toUpperCase( k0 );
        if ( c == Character.toUpperCase( k ) ) {
           result = e;
        } else {
           if ( c == 'B' ) {
              result = Epb ( Epj2d ( e ) );
           } else {
              result = Epj ( Epb2d ( e ) );
           }
        }
        return ( result );
    }

/**
 *  Conversion of Modified Julian Date to Julian epoch.
 *  <dl>
 *  <dt>Reference:</dt>
 *  <dd>Lieske,J.H., 1979. Astron. Astrophys.,73,282.</dd>
 *  </dl>
 *
 *  @param date Modified Julian Date (JD - 2400000.5)
 *  @return Julian epoch
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     date     double      Modified Julian Date (JD - 2400000.5)
 *
 *  The result is the Julian epoch.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Epj ( double date )
    {
        return 2000.0 + ( date - 51544.5 ) / 365.25;
    }

/**
 *  Conversion of Julian epoch to Modified Julian Date.
 *  <dl>
 *  <dt>Reference:</dt>
 *  <dd>Lieske,J.H., 1979. Astron. Astrophys.,73,282.</dd>
 *  </dl>
 *
 *  @param epj Julian epoch
 *  @return Modified Julian Date (JD - 2400000.5)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     epj      double       Julian epoch
 *
 *  The result is the Modified Julian Date (JD - 2400000.5).
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Epj2d ( double epj )
    {
        return 51544.5 + ( epj - 2000.0 ) * 365.25;
    }

/**
 *  Transformation from J2000.0 equatorial coordinates to
 *  ecliptic coordinates.
 *
 *  @param d J2000.0 mean RA,Dec (radians)
 *  @param date TDB (loosely ET) as Modified Julian Date (JD-2400000.5)
 *  @return ecliptic longitude and latitude 
 *            (mean of date, IAU 1980 theory, radians)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     dr,dd       double      J2000.0 mean RA,Dec (radians)
 *     date        double      TDB (loosely ET) as Modified Julian Date
 *                                              (JD-2400000.5)
 *  Returned:
 *     *dl,*db     double      ecliptic longitude and latitude
 *                             (mean of date, IAU 1980 theory, radians)
 *
 *  Called:
 *     Dcs2c, Prec, Epj, Dmxv, Ecmat, Dcc2s,
 *     Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Eqecl ( AngleDR d, double date )
    {
        double rmat[][], v1[], v2[];

/* Spherical to Cartesian */
        v1 = Dcs2c ( d );

/* Mean J2000 to mean of date */
        rmat = Prec ( 2000.0, Epj ( date ) );
        v2 = Dmxv ( rmat, v1 );

/* Equatorial to ecliptic */
        rmat = Ecmat ( date );
        v1 = Dmxv ( rmat, v2 );

/* Cartesian to spherical */
        AngleDR db = Dcc2s ( v1 );

/* Express in conventional ranges */
        db.setAlpha ( Dranrm ( db.getAlpha() ) );
        db.setDelta ( Drange ( db.getDelta() ) );
        return db;
    }

/**
 *  Equation of the equinoxes (IAU 1994, double precision).
 *  <dl>
 *  <dd><p>Greenwich apparent ST = Greenwich mean ST + equation of the equinoxes</p></dd>
 *  
 *  <dt>References:</dt>
 *  <dd>IAU Resolution C7, Recommendation 3 (1994)
 *               Capitaine, N. &amp; Gontier, A.-M., Astron. Astrophys.,
 *               275, 645-650 (1993)</dd>
 *  </dl>
 *
 *  @param date  TDB (loosely ET) as Modified Julian Date (JD-2400000.5)
 *  @return Equation of the equinoxes (in radians)
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     date    double      TDB (loosely ET) as Modified Julian Date
 *                                          (JD-2400000.5)
 *
 *  The result is the equation of the equinoxes (double precision)
 *  in radians:
 *
 *  Greenwich apparent ST = Greenwich mean ST + equation of the equinoxes
 *
 *  References:  IAU Resolution C7, Recommendation 3 (1994)
 *               Capitaine, N. &amp; Gontier, A.-M., Astron. Astrophys.,
 *               275, 645-650 (1993)
 *
 *  Called:  Nutc
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Eqeqx ( double date )
    {
        double t, om;

/* Interval between basic epoch J2000.0 and current epoch (JC) */
        t = ( date - 51544.5 ) / 36525.0;

/* Longitude of the mean ascending node of the lunar orbit on the
   ecliptic, measured from the mean equinox of date */
        om = DAS2R * ( 450160.280 + ( -5.0 * T2AS - 482890.539
                               + ( 7.455 + 0.008 * t ) * t ) * t );

/* Nutation */
        double ps[] = Nutc ( date );

/* Equation of the equinoxes */
        return ps[0] * Math.cos ( ps[2] ) + DAS2R * ( 0.00264 *  Math.sin ( om ) +
                                         0.000063 *  Math.sin ( om + om ) );
    }

/**
 *  Transformation from J2000.0 equatorial coordinates to
 *  IAU 1958 Galactic coordinates.
 *  <dl>
 *  <dt>Note:</dt>
 *  <dd> The equatorial coordinates are J2000.0.  Use the routine
 *       slaEg50 if conversion from B1950.0 'FK4' coordinates is required.</dd>
 *
 *  <dt>Reference:</dt>
 *  <dd>Blaauw et al, Mon.Not.R.astron.Soc.,121,123 (1960)</dd>
 *  </dl>
 *
 *  @param dr  J2000.0 (RA, Dec) (in radians)
 *  @return Galactic longitude and latitude (l2, b2) (in radians)
 */

/*  Latest Revision: 11 January 2002 (RTP)
 *
 *  Given:
 *     dr,dd       double       J2000.0 RA,Dec
 *
 *  Returned:
 *     *dl,*db     double       Galactic longitude and latitude l2,b2
 *
 *  (all arguments are radians)
 *
 *  Called:
 *     slaDcs2c, slaDmxv, slaDcc2s, slaDranrm, slaDrange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Galactic Eqgal ( AngleDR dr )
    {

/*
 *  l2,b2 system of Galactic coordinates
 *
 *  p = 192.25       RA of Galactic north pole (mean B1950.0)
 *  q =  62.6        inclination of Galactic to mean B1950.0 equator
 *  r =  33          longitude of ascending node
 *
 *  p,q,r are degrees
 *
 *  Equatorial to Galactic rotation matrix (J2000.0), obtained by
 *  applying the standard FK4 to FK5 transformation, for zero proper
 *  motion in FK5, to the columns of the B1950 equatorial to
 *  Galactic rotation matrix:
 */
       double rmat[][] = new double[3][3];

       rmat[0][0] = -0.054875539726;
       rmat[0][1] = -0.873437108010;
       rmat[0][2] = -0.483834985808;
       rmat[1][0] =  0.494109453312;
       rmat[1][1] = -0.444829589425;
       rmat[1][2] =  0.746982251810;
       rmat[2][0] = -0.867666135858;
       rmat[2][1] = -0.198076386122;
       rmat[2][2] =  0.455983795705;

/* Spherical to Cartesian */
       double v1[] = Dcs2c ( dr );

/* Equatorial to Galactic */
       double v2[] = Dmxv ( rmat, v1 );

/* Cartesian to spherical */
       AngleDR db = Dcc2s ( v2 );

/* Express in conventional ranges */
       return new Galactic ( Dranrm ( db.getAlpha() ), Drange ( db.getDelta() ) );
    }
/**
 *  Compute the e-terms (elliptic component of annual aberration) vector.
 *  <dl>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li> Smith, C.A. et al, 1989.  "The transformation of astrometric
 *       catalog systems to the equinox J2000.0".  Astron.J. 97, 265.</li>
 *
 *  <li> Yallop, B.D. et al, 1989.  "Transformation of mean star places
 *       from FK4 B1950.0 to FK5 J2000.0 using matrices in 6-space".
 *       Astron.J. 97, 274.</li>
 *  </ol> </dd>
 *  <dt>Note the use of the J2000 aberration constant (20.49552 arcsec).
 *  This is a reflection of the fact that the e-terms embodied in
 *  existing star catalogues were computed from a variety of
 *  aberration constants.  Rather than adopting one of the old
 *  constants the latest value is used here.</dt>
 *  </dl>
 *
 *  @param ep Besselian epoch
 *  @return E-terms as ( dx, dy, dz )
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *  Given:
 *     ep      double      Besselian epoch
 *
 *  Returned:
 *     ev      double[3]   e-terms as (dx,dy,dz)
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Etrms ( double ep )
    {

        double t, e, e0, p, ek, cp;
        double ev[] = new double[3];

        /* Julian centuries since B1950 */
        t = ( ep - 1950 ) * 1.00002135903e-2;

        /* Eccentricity */
        e = 0.01673011 - ( 0.00004193 + 0.000000126 * t ) * t;

        /* Mean obliquity */
        e0 = ( 84404.836 -
              ( 46.8495 + ( 0.00319 + 0.00181 * t ) * t ) * t ) * DAS2R;

        /* Mean longitude of perihelion */
        p = ( 1015489.951 +
              ( 6190.67 + ( 1.65 + 0.012 * t ) * t ) * t ) * DAS2R;

        /* E-terms */
        ek = e * 20.49552 * DAS2R;
        cp = Math.cos ( p );
        ev[0] = ek * Math.sin ( p );
        ev[1] = -ek * cp * Math.cos ( e0 );
        ev[2] = -ek * cp * Math.sin ( e0 );

        return ev;
    }

/**
 *  Barycentric and heliocentric velocity and position of the Earth.
 *  <dl>
 *  <dt>Accuracy:</dt>
 *
 *  <dd> The maximum deviations from the JPL DE96 ephemeris are as follows:
 *  <table>
 *    <tr> <td> barycentric velocity </td> <td> 42  cm/s</td> </tr>
 *    <tr> <td> barycentric position </td> <td> 6900  km</td> </tr>
 *
 *    <tr> <td> heliocentric velocity </td> <td> 42  cm/s</td> </tr>
 *    <tr> <td> heliocentric position </td> <td> 1600  km</td> </tr>
 *  </table>
 *  </dd> </dl>
 *  <p>
 *  This routine is adapted from the BARVEL and BARCOR Fortran
 *  subroutines of P.Stumpff, which are described in
 *  Astron. Astrophys. Suppl. Ser. 41, 1-8 (1980).  The present
 *  routine uses double precision throughout;  most of the other
 *  changes are essentially cosmetic and do not affect the
 *  results.  However, some adjustments have been made so as to
 *  give results that refer to the new (IAU 1976 "FK5") equinox
 *  and precession, although the differences these changes make
 *  relative to the results from Stumpff's original "FK4" version
 *  are smaller than the inherent accuracy of the algorithm.  One
 *  minor shortcoming in the original routines that has not been
 *  corrected is that better numerical accuracy could be achieved
 *  if the various polynomial evaluations were nested.  Note also
 *  that one of Stumpff's precession constants differs by 0.001 arcsec
 *  from the value given in the Explanatory Supplement to the A.E.
 *  </p> <p>
 *  (Units are AU/s for velocity and AU for position)
 *  </p>
 *
 *  @param date TDB (loosely ET) as a Modified Julian Date (JD-2400000.5)
 *  @param deqx Julian epoch (e.g. 2000.0) of mean equator and
 *                equinox of the vectors returned.  If deqx &lt;= 0.0,
 *                all vectors are referred to the mean equator and
 *                equinox (FK5) of epoch date
 *  @param dvb (Returned) barycentric velocity
 *  @param dpb (Returned) barycentric position
 *  @param dvh (Returned) heliocentric velocity
 *  @param dph (Returned) heliocentric position
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *
 *     date    double     TDB (loosely ET) as a Modified Julian Date
 *                                         (JD-2400000.5)
 *
 *     deqx    double     Julian epoch (e.g. 2000.0) of mean equator and
 *                        equinox of the vectors returned.  If deqx <= 0.0,
 *                        all vectors are referred to the mean equator and
 *                        equinox (FK5) of epoch date.
 *
 *  Returned (all 3D Cartesian vectors):
 *
 *     dvb,dpb double[3]  barycentric velocity, position
 *
 *     dvh,dph double[3]  heliocentric velocity, position
 *
 *  Called:  Epj, Prec
 *
 *  Defined in mac.h:  D2PI, DS2R, dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public void Evp ( double date, double deqx, double dvb[],
              double dpb[], double dvh[], double dph[] )
    {
        int ideq, i, j, k;

        double a, pertl,
               pertld, pertr, pertrd, cosa, sina, e, twoe, esq, g, twog,
               phi, f, sf, cf, phid, psid, pertp, pertpd, tl, sinlm, coslm,
               sigma, b, plon, pomg, pecc, flatm, flat;

        double dt, dlocal, dml=0.0,
               deps, dparam, dpsi, d1pdro, drd, drld, dtl, dsinls,
               dcosls, dxhd, dyhd, dzhd, dxbd, dybd, dzbd, dcosep,
               dsinep, dyahd, dzahd, dyabd, dzabd, dr,
               dxh, dyh, dzh, dxb, dyb, dzb, dyah, dzah, dyab,
               dzab, depj, deqcor;

        double sn[] = new double [4], forbel[] = new double [7],
               sorbel[] = new double [17], sinlp[] = new double [4],
               coslp[] = new double [4];

        double dprema[][] = new double [3][3], w, vw[] = new double [3];

/* Sidereal rate dcsld in longitude, rate ccsgd in mean anomaly */
        final double dcsld = 1.990987e-7;
        final double ccsgd = 1.990969e-7;

/* Some constants used in the calculation of the lunar contribution */
        final double cckm  = 3.122140e-5;
        final double ccmld = 2.661699e-6;
        final double ccfdi = 2.399485e-7;

/* Besselian epoch 1950.0 expressed as a Julian epoch */
        final double b1950 = 1949.9997904423;

/*
 * ccpamv(k)=a*m*dl/dt (planets), dc1mme=1-mass(Earth+Moon)
 */
        final double ccpamv[] = {
            8.326827e-11,
            1.843484e-11,
            1.988712e-12,
            1.881276e-12
        };
        final double dc1mme = 0.99999696;

/*
 * ccpam(k)=a*m(planets)
 * ccim=inclination(Moon)
 */
        final double ccpam[] = {
           4.960906e-3,
           2.727436e-3,
           8.392311e-4,
           1.556861e-3
        };
        final double ccim = 8.978749e-2;

/*
 * Constants dcfel(i,k) of fast changing elements
 */
        final double dcfel[][] = {
           {  1.7400353,                /* dcfel[0][0] */
              6.2565836,                /* dcfel[0][1] */
              4.7199666,                /* dcfel[0][2] */
              1.9636505e-1,             /* dcfel[0][3] */
              4.1547339,                /* dcfel[0][4] */
              4.6524223,                /* dcfel[0][5] */
              4.2620486,                /* dcfel[0][6] */
              1.4740694 },              /* dcfel[0][7] */
           {  6.2833195099091e+2,       /* dcfel[1][0] */
              6.2830194572674e+2,       /* dcfel[1][1] */
              8.3997091449254e+3,       /* dcfel[1][2] */
              8.4334662911720e+3,       /* dcfel[1][3] */
              5.2993466764997e+1,       /* dcfel[1][4] */
              2.1354275911213e+1,       /* dcfel[1][5] */
              7.5025342197656,          /* dcfel[1][6] */
              3.8377331909193 },        /* dcfel[1][7] */
           {  5.2796e-6,                /* dcfel[2][0] */
             -2.6180e-6,                /* dcfel[2][1] */
             -1.9780e-5,                /* dcfel[2][2] */
             -5.6044e-5,                /* dcfel[2][3] */
              5.8845e-6,                /* dcfel[2][4] */
              5.6797e-6,                /* dcfel[2][5] */
              5.5317e-6,                /* dcfel[2][6] */
              5.6093e-6 }               /* dcfel[2][7] */
        };

/*
 * Constants dceps and ccsel(i,k) of slowly changing elements
 */
        final double dceps[] = {
            4.093198e-1,
           -2.271110e-4,
           -2.860401e-8
        };
        final double ccsel[][] = {
           {  1.675104e-2,              /* ccsel[0][0]  */
              2.220221e-1,              /* ccsel[0][1]  */
              1.589963,                 /* ccsel[0][2]  */
              2.994089,                 /* ccsel[0][3]  */
              8.155457e-1,              /* ccsel[0][4]  */
              1.735614,                 /* ccsel[0][5]  */
              1.968564,                 /* ccsel[0][6]  */
              1.282417,                 /* ccsel[0][7]  */
              2.280820,                 /* ccsel[0][8]  */
              4.833473e-2,              /* ccsel[0][9]  */
              5.589232e-2,              /* ccsel[0][10] */
              4.634443e-2,              /* ccsel[0][11] */
              8.997041e-3,              /* ccsel[0][12] */
              2.284178e-2,              /* ccsel[0][13] */
              4.350267e-2,              /* ccsel[0][14] */
              1.348204e-2,              /* ccsel[0][15] */
              3.106570e-2 },            /* ccsel[0][16] */
           { -4.179579e-5,              /* ccsel[1][0]  */
              2.809917e-2,              /* ccsel[1][1]  */
              3.418075e-2,              /* ccsel[1][2]  */
              2.590824e-2,              /* ccsel[1][3]  */
              2.486352e-2,              /* ccsel[1][4]  */
              1.763719e-2,              /* ccsel[1][5]  */
              1.524020e-2,              /* ccsel[1][6]  */
              8.703393e-3,              /* ccsel[1][7]  */
              1.918010e-2,              /* ccsel[1][8]  */
              1.641773e-4,              /* ccsel[1][9]  */
             -3.455092e-4,              /* ccsel[1][10] */
             -2.658234e-5,              /* ccsel[1][11] */
              6.329728e-6,              /* ccsel[1][12] */
             -9.941590e-5,              /* ccsel[1][13] */
             -6.839749e-5,              /* ccsel[1][14] */
              1.091504e-5,              /* ccsel[1][15] */
             -1.665665e-4 },            /* ccsel[1][16] */
            { -1.260516e-7,              /* ccsel[2][0]  */
              1.852532e-5,              /* ccsel[2][1]  */
              1.430200e-5,              /* ccsel[2][2]  */
              4.155840e-6,              /* ccsel[2][3]  */
              6.836840e-6,              /* ccsel[2][4]  */
              6.370440e-6,              /* ccsel[2][5]  */
             -2.517152e-6,              /* ccsel[2][6]  */
              2.289292e-5,              /* ccsel[2][7]  */
              4.484520e-6,              /* ccsel[2][8]  */
             -4.654200e-7,              /* ccsel[2][9]  */
             -7.388560e-7,              /* ccsel[2][10] */
              7.757000e-8,              /* ccsel[2][11] */
             -1.939256e-9,              /* ccsel[2][12] */
              6.787400e-8,              /* ccsel[2][13] */
             -2.714956e-7,              /* ccsel[2][14] */
              6.903760e-7,              /* ccsel[2][15] */
             -1.590188e-7 }             /* ccsel[2][16] */
        };

/*
 * Constants of the arguments of the short-period perturbations
 * by the planets:   dcargs(i,k)
 */
        final double dcargs[][] = {
        {  5.0974222,                /* dcargs[0][0]  */
           3.9584962,                /* dcargs[0][1]  */
           1.6338070,                /* dcargs[0][2]  */
           2.5487111,                /* dcargs[0][3]  */
           4.9255514,                /* dcargs[0][4]  */
           1.3363463,                /* dcargs[0][5]  */
           1.6072053,                /* dcargs[0][6]  */
           1.3629480,                /* dcargs[0][7]  */
           5.5657014,                /* dcargs[0][8]  */
           5.0708205,                /* dcargs[0][9]  */
           3.9318944,                /* dcargs[0][10] */
           4.8989497,                /* dcargs[0][11] */
           1.3097446,                /* dcargs[0][12] */
           3.5147141,                /* dcargs[0][13] */
           3.5413158 },              /* dcargs[0][14] */
        { -7.8604195454652e+2,       /* dcargs[1][0]  */
          -5.7533848094674e+2,       /* dcargs[1][1]  */
          -1.1506769618935e+3,       /* dcargs[1][2]  */
          -3.9302097727326e+2,       /* dcargs[1][3]  */
          -5.8849265665348e+2,       /* dcargs[1][4]  */
          -5.5076098609303e+2,       /* dcargs[1][5]  */
          -5.2237501616674e+2,       /* dcargs[1][6]  */
          -1.1790629318198e+3,       /* dcargs[1][7]  */
          -1.0977134971135e+3,       /* dcargs[1][8]  */
          -1.5774000881978e+2,       /* dcargs[1][9]  */
           5.2963464780000e+1,       /* dcargs[1][10] */
           3.9809289073258e+1,       /* dcargs[1][11] */
           7.7540959633708e+1,       /* dcargs[1][12] */
           7.9618578146517e+1,       /* dcargs[1][13] */
          -5.4868336758022e+2 }      /* dcargs[1][14] */
     };

/*
 * Amplitudes ccamps(n,k) of the short-period perturbations
 */
     final double ccamps[][] = {
        { -2.279594e-5,              /* ccamps[0][0]  */
          -3.494537e-5,              /* ccamps[0][1]  */
           6.593466e-7,              /* ccamps[0][2]  */
           1.140767e-5,              /* ccamps[0][3]  */
           9.516893e-6,              /* ccamps[0][4]  */
           7.310990e-6,              /* ccamps[0][5]  */
          -2.603449e-6,              /* ccamps[0][6]  */
          -3.228859e-6,              /* ccamps[0][7]  */
           3.442177e-7,              /* ccamps[0][8]  */
           8.702406e-6,              /* ccamps[0][9]  */
          -1.488378e-6,              /* ccamps[0][10] */
          -8.043059e-6,              /* ccamps[0][11] */
           3.699128e-6,              /* ccamps[0][12] */
           2.550120e-6,              /* ccamps[0][13] */
          -6.351059e-7 },            /* ccamps[0][14] */
        {  1.407414e-5,              /* ccamps[1][0]  */
           2.860401e-7,              /* ccamps[1][1]  */
           1.322572e-5,              /* ccamps[1][2]  */
          -2.049792e-5,              /* ccamps[1][3]  */
          -2.748894e-6,              /* ccamps[1][4]  */
          -1.924710e-6,              /* ccamps[1][5]  */
           7.359472e-6,              /* ccamps[1][6]  */
           1.308997e-7,              /* ccamps[1][7]  */
           2.671323e-6,              /* ccamps[1][8]  */
          -8.421214e-6,              /* ccamps[1][9]  */
          -1.251789e-5,              /* ccamps[1][10] */
          -2.991300e-6,              /* ccamps[1][11] */
          -3.316126e-6,              /* ccamps[1][12] */
          -1.241123e-6,              /* ccamps[1][13] */
           2.341650e-6 },            /* ccamps[1][14] */
        {  8.273188e-6,              /* ccamps[2][0]  */
           1.289448e-7,              /* ccamps[2][1]  */
           9.258695e-6,              /* ccamps[2][2]  */
          -4.747930e-6,              /* ccamps[2][3]  */
          -1.319381e-6,              /* ccamps[2][4]  */
          -8.772849e-7,              /* ccamps[2][5]  */
           3.168357e-6,              /* ccamps[2][6]  */
           1.013137e-7,              /* ccamps[2][7]  */
           1.832858e-6,              /* ccamps[2][8]  */
          -1.372341e-6,              /* ccamps[2][9]  */
           5.226868e-7,              /* ccamps[2][10] */
           1.473654e-7,              /* ccamps[2][11] */
           2.901257e-7,              /* ccamps[2][12] */
           9.901116e-8,              /* ccamps[2][13] */
           1.061492e-6 },            /* ccamps[2][14] */
        {  1.340565e-5,              /* ccamps[3][0]  */
           1.627237e-5,              /* ccamps[3][1]  */
          -4.674248e-7,              /* ccamps[3][2]  */
          -2.638763e-6,              /* ccamps[3][3]  */
          -4.549908e-6,              /* ccamps[3][4]  */
          -3.334143e-6,              /* ccamps[3][5]  */
           1.119056e-6,              /* ccamps[3][6]  */
           2.403899e-6,              /* ccamps[3][7]  */
          -2.394688e-7,              /* ccamps[3][8]  */
          -1.455234e-6,              /* ccamps[3][9]  */
          -2.049301e-7,              /* ccamps[3][10] */
          -3.154542e-7,              /* ccamps[3][11] */
           3.407826e-7,              /* ccamps[3][12] */
           2.210482e-7,              /* ccamps[3][13] */
           2.878231e-7 },            /* ccamps[3][14] */
        { -2.490817e-7,              /* ccamps[4][0]  */
          -1.823138e-7,              /* ccamps[4][1]  */
          -3.646275e-7,              /* ccamps[4][2]  */
          -1.245408e-7,              /* ccamps[4][3]  */
          -1.864821e-7,              /* ccamps[4][4]  */
          -1.745256e-7,              /* ccamps[4][5]  */
          -1.655307e-7,              /* ccamps[4][6]  */
          -3.736225e-7,              /* ccamps[4][7]  */
          -3.478444e-7,              /* ccamps[4][8]  */
          -4.998479e-8,              /* ccamps[4][9]  */
           0.0,                      /* ccamps[4][10] */
           0.0,                      /* ccamps[4][11] */
           0.0,                      /* ccamps[4][12] */
           0.0,                      /* ccamps[4][13] */
           0.0 }                     /* ccamps[4][14] */
      };

/*
 * Constants of the secular perturbations in longitude
 * ccsec3 and ccsec(n,k)
 */
      final double ccsec3 = -7.757020e-8;
      final double ccsec[][] = {
         {  1.289600e-6,              /* ccsec[0][0] */
            3.102810e-5,              /* ccsec[0][1] */
            9.124190e-6,              /* ccsec[0][2] */
            9.793240e-7 },            /* ccsec[0][3] */
         {  5.550147e-1,              /* ccsec[1][0] */
            4.035027,                 /* ccsec[1][1] */
            9.990265e-1,              /* ccsec[1][2] */
            5.508259 },               /* ccsec[1][3] */
         {  2.076942,                 /* ccsec[2][0] */
            3.525565e-1,              /* ccsec[2][1] */
            2.622706,                 /* ccsec[2][2] */
            1.559103e+1 }             /* ccsec[2][3] */
      };

/*
 * Constants dcargm(i,k) of the arguments of the perturbations
 * of the motion of the Moon
 */
        final double dcargm[][] = {
           {  5.167983,                 /* dcargm[0][0] */
              5.491315,                 /* dcargm[0][1] */
              5.959853 },               /* dcargm[0][2] */
           {  8.3286911095275e+3,       /* dcargm[1][0] */
             -7.2140632838100e+3,       /* dcargm[1][1] */
              1.5542754389685e+4 }      /* dcargm[1][2] */
        };

/*
 * Amplitudes ccampm(n,k) of the perturbations of the Moon
 */
        final double ccampm[][] = {
           {  1.097594e-1,              /* ccampm[0][0] */
             -2.223581e-2,              /* ccampm[0][1] */
              1.148966e-2 },            /* ccampm[0][2] */
           {  2.896773e-7,              /* ccampm[1][0] */
              5.083103e-8,              /* ccampm[1][1] */
              5.658888e-8 },            /* ccampm[1][2] */
           {  5.450474e-2,              /* ccampm[2][0] */
              1.002548e-2,              /* ccampm[2][1] */
              8.249439e-3 },            /* ccampm[2][2] */
           {  1.438491e-7,              /* ccampm[3][0] */
             -2.291823e-8,              /* ccampm[3][1] */
              4.063015e-8 }             /* ccampm[3][2] */
        };

/*
 *
 * Execution
 * ---------
 *
 * Control parameter ideq, and time arguments
 */
        ideq = ( deqx <= 0.0 ) ? 0 : 1;
        dt = ( date - 15019.5 ) / 36525.0;

/* Values of all elements for the instant date */
        for ( k = 0; k < 8; k++ ) {
            dlocal = dmod ( dcfel[0][k]
               + dt * ( dcfel[1][k]
               + dt * dcfel[2][k] ), D2PI );
            if ( k == 0 ) {
               dml = dlocal;
            } else {
               forbel[k-1] = dlocal;
            }
        }
        deps = dmod ( dceps[0]
               + dt * ( dceps[1]
               + dt * dceps[2] ) , D2PI );
        for ( k = 0; k < 17; k++ ) {
            sorbel[k] = dmod ( ccsel[0][k]
                + dt * ( ccsel[1][k]
                + dt * ccsel[2][k] ), D2PI );
        }

/* Secular perturbations in longitude */
        for ( k = 0; k < 4; k++ ) {
            a = dmod ( ccsec[1][k] + dt * ccsec[2][k] , D2PI );
            sn[k] = Math.sin ( a );
        }

/* Periodic perturbations of the EMB (Earth-Moon barycentre) */
        pertl = ccsec[0][0] * sn[0]
             + ccsec[0][1] * sn[1]
             + ( ccsec[0][2] + dt * ccsec3 ) * sn[2]
             + ccsec[0][3] * sn[3];
        pertld = 0.0;
        pertr = 0.0;
        pertrd = 0.0;
        for ( k = 0; k < 15; k++ ) {
           a = dmod ( dcargs[0][k] + dt * dcargs[1][k] , D2PI );
           cosa = Math.cos ( a );
           sina = Math.sin ( a );
           pertl += ccamps[0][k] * cosa + ccamps[1][k] * sina;
           pertr += ccamps[2][k] * cosa + ccamps[3][k] * sina;
           if ( k < 10 ) {
              pertld += ( ccamps[1][k] * cosa
                   - ccamps[0][k] * sina ) * ccamps[4][k];
              pertrd += ( ccamps[3][k] * cosa
                   - ccamps[2][k] * sina ) * ccamps[4][k];
           }
        }

/* Elliptic part of the motion of the EMB */
        e = sorbel[0];
        twoe = e + e;
        esq = e * e;
        dparam = 1.0 - esq;
        g = forbel[0];
        twog = g + g;
        phi = twoe * ( ( 1.0 - esq / 8.0 ) * Math.sin ( g )
                + 5.0 * e * Math.sin ( twog ) / 8.0
                + 13.0 * esq * Math.sin ( g + twog ) / 24.0 );
        f = forbel[0] + phi;
        sf = Math.sin ( f );
        cf = Math.cos ( f );
        dpsi = dparam / ( 1.0 + e * cf );
        phid = twoe * ccsgd * ( ( 1.0 + esq * 1.5 ) * cf
                         + e * ( 1.25 - sf * sf / 2.0 ) );
        psid = ccsgd * e * sf / Math.sqrt ( dparam );

/* Perturbed heliocentric motion of the EMB */
        d1pdro = 1.0 + pertr;
        drd = d1pdro * ( psid + dpsi * pertrd );
        drld = d1pdro * dpsi * ( dcsld + phid + pertld );
        dtl = dmod ( dml + phi + pertl , D2PI );
        dsinls = Math.sin ( dtl );
        dcosls = Math.cos ( dtl );
        dxhd = drd * dcosls - drld * dsinls;
        dyhd = drd * dsinls + drld * dcosls;

/* Influence of eccentricity, evection and variation on the
 * geocentric motion of the Moon
 */
        pertl = 0.0;
        pertld = 0.0;
        pertp = 0.0;
        pertpd = 0.0;
        for ( k = 0; k < 3; k++ ) {
           a = dmod ( dcargm[0][k] + dt * dcargm[1][k] , D2PI );
           sina = Math.sin ( a );
           cosa = Math.cos ( a );
           pertl += ccampm[0][k] * sina;
           pertld += ccampm[1][k] * cosa;
           pertp += ccampm[2][k] * cosa;
           pertpd += - ccampm[3][k] * sina;
        }

/* Heliocentric motion of the Earth */
        tl = forbel[1] + pertl;
        sinlm = Math.sin ( tl );
        coslm = Math.cos ( tl );
        sigma = cckm / ( 1.0 + pertp );
        a = sigma * ( ccmld + pertld );
        b = sigma * pertpd;
        dxhd  += a * sinlm + b * coslm;
        dyhd  += - a * coslm + b * sinlm;
        dzhd  = - sigma * ccfdi * Math.cos ( forbel[2] );

/* Barycentric motion of the Earth */
        dxbd = dxhd * dc1mme;
        dybd = dyhd * dc1mme;
        dzbd = dzhd * dc1mme;
        for ( k = 0; k < 4; k++ ) {
           plon = forbel[k+3];
           pomg = sorbel[k+1];
           pecc = sorbel[k+9];
           tl = dmod( plon + 2.0 * pecc * Math.sin ( plon - pomg ) , D2PI );
           sinlp[k] = Math.sin ( tl );
           coslp[k] = Math.cos ( tl );
           dxbd += ccpamv[k] * ( sinlp[k] + pecc * Math.sin ( pomg ) );
           dybd += - ccpamv[k] * ( coslp[k] + pecc * Math.cos ( pomg ) );
           dzbd += - ccpamv[k] * sorbel[k+13] * Math.cos ( plon - sorbel[k+5] );
        }

/* Transition to mean equator of date */
        dcosep = Math.cos ( deps );
        dsinep = Math.sin ( deps );
        dyahd  = dcosep * dyhd - dsinep * dzhd;
        dzahd  = dsinep * dyhd + dcosep * dzhd;
        dyabd  = dcosep * dybd - dsinep * dzbd;
        dzabd  = dsinep * dybd + dcosep * dzbd;

/* Heliocentric coordinates of the Earth */
        dr = dpsi * d1pdro;
        flatm = ccim * Math.sin ( forbel[2] );
        a = sigma * Math.cos ( flatm );
        dxh = dr * dcosls - a * coslm;
        dyh = dr * dsinls - a * sinlm;
        dzh = - sigma * Math.sin ( flatm );

/* Barycentric coordinates of the Earth */
        dxb = dxh * dc1mme;
        dyb = dyh * dc1mme;
        dzb = dzh * dc1mme;
        for ( k = 0; k < 4; k++ ) {
           flat = sorbel[k+13] * Math.sin ( forbel[k+3] - sorbel[k+5] );
           a = ccpam[k] * (1.0 - sorbel[k+9] * Math.cos ( forbel[k+3] - sorbel[k+1]));
           b = a * Math.cos(flat);
           dxb -= b * coslp[k];
           dyb -= b * sinlp[k];
           dzb -= a * Math.sin ( flat );
        }

/* Transition to mean equator of date */
        dyah = dcosep * dyh - dsinep * dzh;
        dzah = dsinep * dyh + dcosep * dzh;
        dyab = dcosep * dyb - dsinep * dzb;
        dzab = dsinep * dyb + dcosep * dzb;

/* Copy result components into vectors, correcting for FK4 equinox */
        depj = Epj ( date );
        deqcor = DS2R * ( 0.035 + ( 0.00085 * ( depj - b1950 ) ) );
        dvh[0] = dxhd - deqcor * dyahd;
        dvh[1] = dyahd + deqcor * dxhd;
        dvh[2] = dzahd;
        dvb[0] = dxbd - deqcor * dyabd;
        dvb[1] = dyabd + deqcor * dxbd;
        dvb[2] = dzabd;
        dph[0] = dxh - deqcor * dyah;
        dph[1] = dyah + deqcor * dxh;
        dph[2] = dzah;
        dpb[0] = dxb - deqcor * dyab;
        dpb[1] = dyab + deqcor * dxb;
        dpb[2] = dzab;

/* Was precession to another equinox requested? */
        if ( ideq != 0 ) {

/* Yes: compute precession matrix from MJD date to Julian Epoch deqx */
           dprema = Prec ( depj, deqx );

/* Rotate dvh */
           for ( j = 0; j < 3; j++ ) {
              w = 0.0;
              for ( i = 0; i < 3; i++ ) {
                 w += dprema[j][i] * dvh[i];
              }
              vw[j] = w;
           }
           for ( j = 0; j < 3; j++ ) {
              dvh[j] = vw[j];
           }

/* Rotate dvb */
           for ( j = 0; j < 3; j++ ) {
              w = 0.0;
              for ( i = 0; i < 3; i++ ) {
                 w += dprema[j][i] * dvb[i];
              }
              vw[j] = w;
           }
           for ( j = 0; j < 3; j++ ) {
              dvb[j] = vw[j];
           }

/* Rotate dph */
           for ( j = 0; j < 3; j++ ) {
              w = 0.0;
              for ( i = 0; i < 3; i++ ) {
                 w += dprema[j][i] * dph[i];
              }
              vw[j] = w;
           }
           for ( j = 0; j < 3; j++ ) {
              dph[j] = vw[j];
           }

/* Rotate dpb */
           for ( j = 0; j < 3; j++ ) {
              w = 0.0;
              for ( i = 0; i < 3; i++ ) {
                      w += dprema[j][i] * dpb[i];
              }
              vw[j] = w;
           }
           for ( j = 0; j < 3; j++ ) {
              dpb[j] = vw[j];
           }
       }
    }

/**
 * Convert B1950.0 FK4 star data to J2000.0 FK5.
 *
 *  <p>
 *  This routine converts stars from the old, Bessel-Newcomb, FK4
 *  system to the new, IAU 1976, FK5, Fricke system.  The precepts
 *  of Smith et al (Ref 1) are followed, using the implementation
 *  by Yallop et al (Ref 2) of a matrix method due to Standish.
 *  Kinoshita's development of Andoyer's post-Newcomb precession is
 *  used.  The numerical constants from Seidelmann et al (Ref 3) are
 *  used canonically.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The proper motions in RA are dRA/dt rather than
 *      cos(Dec)*dRA/dt, and are per year rather than per century. </li>
 *
 *  <li>Conversion from Besselian epoch 1950.0 to Julian epoch
 *      2000.0 only is provided for.  Conversions involving other
 *      epochs will require use of the appropriate precession,
 *      proper motion, and E-terms routines before and/or
 *      after FK425 is called. </li>
 *
 *  <li>In the FK4 catalogue the proper motions of stars within
 *      10 degrees of the poles do not embody the differential
 *      E-term effect and should, strictly speaking, be handled
 *      in a different manner from stars outside these regions.
 *      However, given the general lack of homogeneity of the star
 *      data available for routine astrometry, the difficulties of
 *      handling positions that may have been determined from
 *      astrometric fields spanning the polar and non-polar regions,
 *      the likelihood that the differential E-terms effect was not
 *      taken into account when allowing for proper motion in past
 *      astrometry, and the undesirability of a discontinuity in
 *      the algorithm, the decision has been made in this routine to
 *      include the effect of differential E-terms on the proper
 *      motions for all stars, whether polar or not.  At epoch 2000,
 *      and measuring on the sky rather than in terms of dRA, the
 *      errors resulting from this simplification are less than
 *      1 milliarcsecond in position and 1 milliarcsecond per
 *      century in proper motion. </li>
 *   </ol> </dd>
 *   <dt>References:</dt>
 *   <dd> <ol>
 *   <li> Smith, C.A. et al, 1989.  "The transformation of astrometric
 *        catalog systems to the equinox J2000.0".  Astron.J. 97, 265.</li>
 *
 *   <li> Yallop, B.D. et al, 1989.  "Transformation of mean star places
 *        from FK4 B1950.0 to FK5 J2000.0 using matrices in 6-space".
 *        Astron.J. 97, 274.</li>
 *
 *   <li> Seidelmann, P.K. (ed), 1992.  "Explanatory Supplement to
 *        the Astronomical Almanac", ISBN 0-935702-68-7.</li>
 *  </ol> </dd> </dl>
 *
 *  @param s1950 B1950.0 RA,dec (rad), 
 *               proper motions (rad/trop.yr),
 *               parallax (arcsec),
 *               radial velocity (km/s, +ve = moving away)
 *  @return J2000.0 RA,dec (rad),
 *          proper motions (rad/trop.yr),
 *          parallax (arcsec),
 *          radial velocity (km/s, +ve = moving away)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:  (all B1950.0,FK4)
 *
 *     r1950,d1950     double    B1950.0 RA,dec (rad)
 *     dr1950,dd1950   double    B1950.0 proper motions (rad/trop.yr)
 *     p1950           double    parallax (arcsec)
 *     v1950           double    radial velocity (km/s, +ve = moving away)
 *
 *  Returned:  (all J2000.0,FK5)
 *
 *     *r2000,*d2000   double    J2000.0 RA,dec (rad)
 *     *dr2000,*dd2000 double    J2000.0 proper motions (rad/jul.yr)
 *     *p2000          double    parallax (arcsec)
 *     *v2000          double    radial velocity (km/s, +ve = moving away)
 *
 *  Defined in mac.h:  D2PI
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Stardata Fk425 ( Stardata s1950 )
    {
        double r, d, ur, ud, px, rv, sr, cr, sd, cd, w, wd,
               x, y, z, xd, yd, zd,
               rxysq, rxyzsq, rxy, rxyz, spxy, spxyz;
        int i, j;

/* Star position and velocity vectors */
        double r0[] = new double[3], rd0[] = new double[3];

/* Combined position and velocity vectors */
        double v1[] = new double[6], v2[] = new double[6];

/* Radians per year to arcsec per century */
        final double pmf = 100.0 * 60.0 * 60.0 * 360.0 / D2PI;

/*
 * Canonical constants  (see references)
 */

/*
 * Km per sec to AU per tropical century
 * = 86400 * 36524.2198782 / 1.49597870e8
 */
        double vf = 21.095;

/* Constant vector and matrix (by rows) */
        final double a[]  = { -1.62557e-6,  -0.31919e-6, -0.13843e-6 };
        final double ad[] = {  1.245e-3,     -1.580e-3,   -0.659e-3  };
        final double em[][] =
        {
          {  0.9999256782,              /* em[0][0] */
            -0.0111820611,              /* em[0][1] */
            -0.0048579477,              /* em[0][2] */
             0.00000242395018,          /* em[0][3] */
            -0.00000002710663,          /* em[0][4] */
            -0.00000001177656 },        /* em[0][5] */

          {  0.0111820610,              /* em[1][0] */
             0.9999374784,              /* em[1][1] */
            -0.0000271765,              /* em[1][2] */
             0.00000002710663,          /* em[1][3] */
             0.00000242397878,          /* em[1][4] */
            -0.00000000006587 },        /* em[1][5] */

          {  0.0048579479,              /* em[2][0] */
            -0.0000271474,              /* em[2][1] */
             0.9999881997,              /* em[2][2] */
             0.00000001177656,          /* em[2][3] */
            -0.00000000006582,          /* em[2][4] */
             0.00000242410173 },        /* em[2][5] */

          { -0.000551,                  /* em[3][0] */
            -0.238565,                  /* em[3][1] */
             0.435739,                  /* em[3][2] */
             0.99994704,                /* em[3][3] */
            -0.01118251,                /* em[3][4] */
            -0.00485767 },              /* em[3][5] */

          {  0.238514,                  /* em[4][0] */
            -0.002667,                  /* em[4][1] */
            -0.008541,                  /* em[4][2] */
             0.01118251,                /* em[4][3] */
             0.99995883,                /* em[4][4] */
            -0.00002718 },              /* em[4][5] */

          { -0.435623,                  /* em[5][0] */
             0.012254,                  /* em[5][1] */
             0.002117,                  /* em[5][2] */
             0.00485767,                /* em[5][3] */
            -0.00002714,                /* em[5][4] */
             1.00000956 }               /* em[5][5] */
        };

/* Pick up B1950 data (units radians and arcsec/tc) */
        AngleDR rd = s1950.getAngle();
        r = rd.getAlpha();
        d = rd.getDelta();
        double pm[] = s1950.getMotion();
        ur = pm[0] * pmf;
        ud = pm[1] * pmf;
        px = s1950.getParallax();
        rv = s1950.getRV();

/* Spherical to Cartesian */
        sr = Math.sin ( r );
        cr = Math.cos ( r );
        sd = Math.sin ( d );
        cd = Math.cos ( d );

        r0[0] = cr * cd;
        r0[1] = sr * cd;
        r0[2] = sd;

        w = vf * rv * px;

        rd0[0] = ( -sr * cd * ur ) - ( cr * sd * ud ) + ( w * r0[0] );
        rd0[1] = ( cr * cd * ur ) - ( sr * sd * ud ) + ( w * r0[1] );
        rd0[2] = ( cd * ud ) + ( w * r0[2] );

/* Allow for e-terms and express as position+velocity 6-vector */
        w = ( r0[0] * a[0] ) + ( r0[1] * a[1] ) + ( r0[2] * a[2] );
        wd = ( r0[0] * ad[0] ) + ( r0[1] * ad[1] ) + ( r0[2] * ad[2] );

        for ( i = 0; i < 3; i++ ) {
           v1[i] = r0[i]  - a[i]  + w * r0[i];
           v1[i+3] = rd0[i] - ad[i] + wd * r0[i];
        }

/* Convert position+velocity vector to Fricke system */
        for ( i = 0; i < 6; i++ ) {
           w = 0.0;
           for ( j = 0; j < 6; j++ ) {
              w += em[i][j] * v1[j];
           }
           v2[i] = w;
        }

/* Revert to spherical coordinates */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        xd = v2[3];
        yd = v2[4];
        zd = v2[5];

        rxysq = ( x * x ) + ( y * y );
        rxyzsq = ( rxysq ) + ( z * z );
        rxy = Math.sqrt ( rxysq );
        rxyz = Math.sqrt (  rxyzsq );

        spxy = ( x * xd ) + ( y * yd );
        spxyz = spxy + ( z * zd );

        r = ( x != 0.0 || y != 0.0 ) ? Math.atan2 ( y, x ) : 0.0;
        if ( r < 0.0 ) r += D2PI;
        d = Math.atan2 ( z, rxy );

        if ( rxy > VERYTINY ) {
           ur = ( ( x * yd ) - ( y * xd ) ) / rxysq;
           ud = ( ( zd * rxysq ) - ( z * spxy ) ) / ( rxyzsq * rxy );
        }

        if ( px > VERYTINY ) {
           rv = spxyz / ( px * rxyz * vf );
           px = px / rxyz;
        }

/* Return results */
        AngleDR ang = new AngleDR( r, d );
        double pm1[] = { ur / pmf, ud / pmf };
        return new Stardata( ang, pm1, px, rv );
     }

/**
 *  Convert B1950.0 FK4 star data to J2000.0 FK5 assuming zero
 *  proper motion in the FK5 frame (double precision).
 *  <p>
 *  This routine converts stars from the old, Bessel-Newcomb, FK4
 *  system to the new, IAU 1976, FK5, Fricke system, in such a
 *  way that the FK5 proper motion is zero.  Because such a star
 *  has, in general, a non-zero proper motion in the FK4 system,
 *  the routine requires the epoch at which the position in the
 *  FK4 system was determined.
 *  </p> <p>
 *  The method is from Appendix 2 of Ref 1, but using the constants of Ref 4.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The epoch BEPOCH is strictly speaking Besselian, but
 *      if a Julian epoch is supplied the result will be
 *      affected only to a negligible extent.</li>
 *
 *  <li>Conversion from Besselian epoch 1950.0 to Julian epoch
 *      2000.0 only is provided for.  Conversions involving other
 *      epochs will require use of the appropriate precession,
 *      proper motion, and E-terms routines before and/or
 *      after FK45Z is called.</li>
 *
 *  <li>In the FK4 catalogue the proper motions of stars within
 *      10 degrees of the poles do not embody the differential
 *      E-term effect and should, strictly speaking, be handled
 *      in a different manner from stars outside these regions.
 *      However, given the general lack of homogeneity of the star
 *      data available for routine astrometry, the difficulties of
 *      handling positions that may have been determined from
 *      astrometric fields spanning the polar and non-polar regions,
 *      the likelihood that the differential E-terms effect was not
 *      taken into account when allowing for proper motion in past
 *      astrometry, and the undesirability of a discontinuity in
 *      the algorithm, the decision has been made in this routine to
 *      include the effect of differential E-terms on the proper
 *      motions for all stars, whether polar or not.  At epoch 2000,
 *      and measuring on the sky rather than in terms of dRA, the
 *      errors resulting from this simplification are less than
 *      1 milliarcsecond in position and 1 milliarcsecond per
 *      century in proper motion.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li> Aoki,S., et al, 1983.  Astron. Astrophys., 128, 263.</li>
 *
 *  <li> Smith, C.A. et al, 1989.  "The transformation of astrometric
 *       catalog systems to the equinox J2000.0".  Astron.J. 97, 265.</li>
 *
 *  <li> Yallop, B.D. et al, 1989.  "Transformation of mean star places
 *       from FK4 B1950.0 to FK5 J2000.0 using matrices in 6-space".
 *       Astron.J. 97, 274.</li>
 *
 *  <li> Seidelmann, P.K. (ed), 1992.  "Explanatory Supplement to
 *       the Astronomical Almanac", ISBN 0-935702-68-7.</li>
 *  </ol> </dd> </dl>
 *
 *  @param r1950 B1950.0 FK4 RA,Dec at epoch (rad)
 *  @param bepoch Besselian epoch (e.g. 1979.3)
 *  @return J2000.0 FK5 RA,Dec (rad)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     r1950,d1950     double   B1950.0 FK4 RA,Dec at epoch (rad)
 *     bepoch          double   Besselian epoch (e.g. 1979.3)
 *
 *  Returned:
 *     *r2000,*d2000   double   J2000.0 FK5 RA,Dec (rad)
 *
 *  Called:  Dcs2c, Epj, Epb2d, Dcc2s, Dranrm
 *
 *  Defined in mac.h:  D2PI
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Fk45z ( AngleDR r1950, double bepoch )
    {
        double w;
        int i, j;

/* Position and position+velocity vectors */
        double r0[], a1[] = new double[3], v1[] = new double[3],
                     v2[] = new double[6];

/* Radians per year to arcsec per century */
        final double pmf = 100.0 * 60.0 * 60.0 * 360.0 / D2PI;

/*
 * Canonical constants  (see references)
*/

/* vectors a and adot, and matrix m (only half of which is needed here) */
        final double a[]  = { -1.62557e-6,  -0.31919e-6, -0.13843e-6 };
        final double ad[] = {  1.245e-3,    -1.580e-3,   -0.659e-3 };
        final double em[][] =
        {
          {  0.9999256782, -0.0111820611, -0.0048579477 },
          {  0.0111820610,  0.9999374784, -0.0000271765 },
          {  0.0048579479, -0.0000271474,  0.9999881997 },
          { -0.000551,     -0.238565,      0.435739     },
          {  0.238514,     -0.002667,     -0.008541     },
          { -0.435623,      0.012254,      0.002117     }
        };

/* Spherical to Cartesian */
        r0 = Dcs2c ( r1950 );

/* Adjust vector a to give zero proper motion in FK5 */
        w = ( bepoch - 1950.0 ) / pmf;
        for ( i = 0; i < 3; i++ ) {
           a1[i] = a[i] + w * ad[i];
        }

/* Remove e-terms */
        w = r0[0] * a1[0] + r0[1] * a1[1] + r0[2] * a1[2];
        for ( i = 0; i < 3; i++ ) {
           v1[i] = r0[i] - a1[i] + w * r0[i];
        }

/* Convert position vector to Fricke system */
        for ( i = 0; i < 6; i++ ) {
           w = 0.0;
           for ( j = 0; j < 3; j++ ) {
              w += em[i][j] * v1[j];
           }
           v2[i] = w;
        }

/* Allow for fictitious proper motion in FK4 */
        w = ( Epj ( Epb2d ( bepoch ) ) - 2000.0 ) / pmf;
        for ( i = 0; i < 3; i++ ) {
           v2[i] += w * v2[i+3];
        }

/* Revert to spherical coordinates */
        AngleDR r2000 = Dcc2s ( v2 );
        r2000.setAlpha( Dranrm ( r2000.getAlpha() ) );
        return r2000;
    }

/**
 *  Convert J2000.0 FK5 star data to B1950.0 FK4.
 *
 *  <p>
 *   This routine converts stars from the new, IAU 1976, FK5, Fricke
 *   system, to the old, Bessel-Newcomb, FK4 system.  The precepts
 *   of Smith et al (Ref 1) are followed, using the implementation
 *   by Yallop et al (Ref 2) of a matrix method due to Standish.
 *   Kinoshita's development of Andoyer's post-Newcomb precession is
 *   used.  The numerical constants from Seidelmann et al (Ref 3) are
 *   used canonically.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The proper motions in RA are dRA/dt rather than
 *      cos(Dec)*dRA/dt, and are per year rather than per century.</li>
 *
 *  <li>Note that conversion from Julian epoch 2000.0 to Besselian
 *      epoch 1950.0 only is provided for.  Conversions involving
 *      other epochs will require use of the appropriate precession,
 *      proper motion, and E-terms routines before and/or after
 *      FK524 is called.</li>
 *
 *  <li>In the FK4 catalogue the proper motions of stars within
 *      10 degrees of the poles do not embody the differential
 *      E-term effect and should, strictly speaking, be handled
 *      in a different manner from stars outside these regions.
 *      However, given the general lack of homogeneity of the star
 *      data available for routine astrometry, the difficulties of
 *      handling positions that may have been determined from
 *      astrometric fields spanning the polar and non-polar regions,
 *      the likelihood that the differential E-terms effect was not
 *      taken into account when allowing for proper motion in past
 *      astrometry, and the undesirability of a discontinuity in
 *      the algorithm, the decision has been made in this routine to
 *      include the effect of differential E-terms on the proper
 *      motions for all stars, whether polar or not.  At epoch 2000,
 *      and measuring on the sky rather than in terms of dRA, the
 *      errors resulting from this simplification are less than
 *      1 milliarcsecond in position and 1 milliarcsecond per
 *      century in proper motion.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li> Smith, C.A. et al, 1989.  "The transformation of astrometric
 *       catalog systems to the equinox J2000.0".  Astron.J. 97, 265.</li>
 *
 *  <li> Yallop, B.D. et al, 1989.  "Transformation of mean star places
 *       from FK4 B1950.0 to FK5 J2000.0 using matrices in 6-space".
 *       Astron.J. 97, 274.</li>
 *
 *  <li> Seidelmann, P.K. (ed), 1992.  "Explanatory Supplement to
 *       the Astronomical Almanac", ISBN 0-935702-68-7.</li>
 *  </ol> </dd> </dl>
 *
 *    @param j2000 J2000.0 RA,Dec (rad),
 *           J2000.0 proper motions (rad/Jul.yr),
 *           parallax (arcsec),
 *           radial velocity (km/s, +ve = moving away)
 *    @return B1950.0 RA,Dec (rad),
 *            proper motions (rad/trop.yr),
 *            parallax (arcsec),
 *            radial velocity (km/s, +ve = moving away)
 *    
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:  (all J2000.0,FK5)
 *     r2000,d2000      double    J2000.0 RA,Dec (rad)
 *     dr2000,dd2000    double    J2000.0 proper motions (rad/Jul.yr)
 *     p2000            double    parallax (arcsec)
 *     v2000            double    radial velocity (km/s, +ve = moving away)
 *
 *  Returned:  (all B1950.0,FK4)
 *     *r1950,*d1950    double    B1950.0 RA,Dec (rad)
 *     *dr1950,*dd1950  double    B1950.0 proper motions (rad/trop.yr)
 *     *p1950           double    parallax (arcsec)
 *     *v1950           double    radial velocity (km/s, +ve = moving away)
 *
 *  Defined in mac.h:  D2PI
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Stardata Fk524 ( Stardata j2000 )
    {

/* Miscellaneous */
        double r, d, ur, ud, px, rv;
        double sr, cr, sd, cd, x, y, z, w;
        double v1[] = new double[6], v2[] = new double[6];
        double xd, yd, zd;
        double rxyz, wd, rxysq, rxy;
        int i,j;

/* Radians per year to arcsec per century */
        final double pmf = 100.0 * 60.0 * 60.0 * 360.0 / D2PI;

/*
 * Canonical constants  (see references)
 */

/*
 * km per sec to AU per tropical century
 * = 86400 * 36524.2198782 / 1.49597870e8
 */
        final double vf = 21.095;

/* Constant vector and matrix (by rows) */
        final double a[] = { -1.62557e-6,   -0.31919e-6, -0.13843e-6,
                              1.245e-3,     -1.580e-3,   -0.659e-3 };

        final double emi[][] =
        {
          {  0.9999256795,              /* emi[0][0] */
             0.0111814828,              /* emi[0][1] */
             0.0048590039,              /* emi[0][2] */
            -0.00000242389840,          /* emi[0][3] */
            -0.00000002710544,          /* emi[0][4] */
            -0.00000001177742 },        /* emi[0][5] */

          { -0.0111814828,              /* emi[1][0] */
             0.9999374849,              /* emi[1][1] */
            -0.0000271771,              /* emi[1][2] */
             0.00000002710544,          /* emi[1][3] */
            -0.00000242392702,          /* emi[1][4] */
             0.00000000006585 },        /* emi[1][5] */

          { -0.0048590040,              /* emi[2][0] */
            -0.0000271557,              /* emi[2][1] */
             0.9999881946,              /* emi[2][2] */
             0.00000001177742,          /* emi[2][3] */
             0.00000000006585,          /* emi[2][4] */
            -0.00000242404995 },        /* emi[2][5] */

          { -0.000551,                  /* emi[3][0] */
             0.238509,                  /* emi[3][1] */
            -0.435614,                  /* emi[3][2] */
             0.99990432,                /* emi[3][3] */
             0.01118145,                /* emi[3][4] */
             0.00485852 },              /* emi[3][5] */

          { -0.238560,                  /* emi[4][0] */
            -0.002667,                  /* emi[4][1] */
             0.012254,                  /* emi[4][2] */
            -0.01118145,                /* emi[4][3] */
             0.99991613,                /* emi[4][4] */
            -0.00002717 },              /* emi[4][5] */

          {  0.435730,                  /* emi[5][0] */
            -0.008541,                  /* emi[5][1] */
             0.002117,                  /* emi[5][2] */
            -0.00485852,                /* emi[5][3] */
            -0.00002716,                /* emi[5][4] */
             0.99996684 }               /* emi[5][5] */
        };

/* Pick up J2000 data (units radians and arcsec/JC) */
        r = j2000.getAngle().getAlpha();
        d = j2000.getAngle().getDelta();
        ur = j2000.getMotion()[0] * pmf;
        ud = j2000.getMotion()[1] * pmf;
        px = j2000.getParallax();
        rv = j2000.getRV();

/* Spherical to Cartesian */
        sr = Math.sin ( r );
        cr = Math.cos ( r );
        sd = Math.sin ( d );
        cd = Math.cos ( d );

        x = cr * cd;
        y = sr * cd;
        z = sd;

        w = vf * rv * px;

        v1[0] = x;
        v1[1] = y;
        v1[2] = z;

        v1[3] =  - ur * y - cr * sd * ud + w * x;
        v1[4] = ur * x - sr * sd * ud + w * y;
        v1[5] = cd * ud + w * z;

/* Convert position+velocity vector to BN system */
        for ( i = 0; i < 6; i++ ) {
           w = 0.0;
           for ( j = 0; j < 6; j++ ) {
              w += emi[i][j] * v1[j];
           }
           v2[i] = w;
        }

/* Position vector components and magnitude */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        rxyz = Math.sqrt ( x * x + y * y + z * z );

/* Include e-terms */
        w = x * a[0] + y * a[1] + z * a[2];
        x += a[0] * rxyz - w * x;
        y += a[1] * rxyz - w * y;
        z += a[2] * rxyz - w * z;

/* Recompute magnitude */
        rxyz = Math.sqrt ( x * x + y * y + z * z );

/* Apply E-terms to both position and velocity */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        w = x * a[0] + y * a[1] + z * a[2];
        wd = x * a[3] + y * a[4] + z * a[5];
        x += a[0] * rxyz - w * x;
        y += a[1] * rxyz - w * y;
        z += a[2] * rxyz - w * z;
        xd = v2[3] + a[3] * rxyz - wd * x;
        yd = v2[4] + a[4] * rxyz - wd * y;
        zd = v2[5] + a[5] * rxyz - wd * z;

/* Convert to spherical */
        rxysq = x * x + y * y;
        rxy = Math.sqrt ( rxysq );

        r = ( x != 0.0 || y != 0.0 ) ? Math.atan2 ( y, x ) : 0.0;
        if ( r < 0.0 ) r += D2PI;
        d = Math.atan2 ( z, rxy );

        if (rxy > VERYTINY) {
           ur = ( x * yd - y * xd ) / rxysq;
           ud = ( zd * rxysq - z * ( x * xd + y * yd ) ) /
                ( ( rxysq + z * z ) * rxy );
        }

/* Radial velocity and parallax */
        if ( px > VERYTINY )
        {
           rv = ( x * xd + y * yd + z * zd ) / ( px * vf * rxyz );
           px = px / rxyz;
        }

/* Return results */
        AngleDR r1950 = new AngleDR ( r, d );
        double d1950[] = { ur/pmf, ud/pmf };
        return new Stardata( r1950, d1950, px, rv );
    }

/**
 *  Convert a J2000.0 FK5 star position to B1950.0 FK4 assuming
 *  zero proper motion and parallax.
 *  <p>
 *  This routine converts star positions from the new, IAU 1976,
 *  FK5, Fricke system to the old, Bessel-Newcomb, FK4 system.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The proper motion in RA is dRA/dt rather than cos(Dec)*dRA/dt.</li>
 *
 *  <li>Conversion from Julian epoch 2000.0 to Besselian epoch 1950.0
 *      only is provided for.  Conversions involving other epochs will
 *      require use of the appropriate precession routines before and
 *      after this routine is called.</li>
 *
 *  <li>Unlike in the FK524 routine, the FK5 proper motions, the
 *      parallax and the radial velocity are presumed zero.</li>
 *
 *  <li>It is the intention that FK5 should be a close approximation
 *      to an inertial frame, so that distant objects have zero proper
 *      motion;  such objects have (in general) non-zero proper motion
 *      in FK4, and this routine returns those fictitious proper
 *      motions.</li>
 *
 *  <li>The position returned by this routine is in the B1950
 *      reference frame but at Besselian epoch bepoch.  For
 *      comparison with catalogues the bepoch argument will
 *      frequently be 1950.0.</li>
 *  </ol> </dd> </dl>
 *
 *  @param r2000 J2000.0 FK5 RA,Dec (rad)
 *  @param bepoch Besselian epoch (e.g. 1950)
 *  @return B1950.0 FK4 RA,Dec (rad) at epoch BEPOCH,
 *          B1950.0 FK4 proper motions (rad/trop.yr)
 */

/*  Latest Revision: 26 November 2001 (RTP)
 *
 *  Given:
 *     r2000,d2000     double     J2000.0 FK5 RA,Dec (rad)
 *     bepoch          double     Besselian epoch (e.g. 1950)
 *
 *  Returned:
 *     *r1950,*d1950    double    B1950.0 FK4 RA,Dec (rad) at epoch BEPOCH
 *     *dr1950,*dd1950  double    B1950.0 FK4 proper motions (rad/trop.yr)
 *
 *  Called:  Fk524, Pm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Stardata Fk54z ( AngleDR r2000, double bepoch )
    {
        final double zero = 0.0;

        double r, d, px, rv;

/* FK5 equinox J2000 (any epoch) to FK4 equinox B1950 epoch B1950 */
        double v[] = { zero, zero };
        Stardata s = new Stardata ( r2000, v, zero, zero );
        Stardata t = Fk524 ( s );
        AngleDR r1950 = t.getAngle();
        double m1950[] = t.getMotion();

/* Fictitious proper motion to epoch bepoch */
        AngleDR ang =  Pm ( r1950, m1950, zero, zero, 1950.0, bepoch );
        return new Stardata( ang, m1950, zero, zero );
    }

/**
 *  Transformation from IAU 1958 Galactic coordinates to
 *  J2000.0 equatorial coordinates.
 *  <dl>
 *  <dt>Note:</dt>
 *  <dd>
 *     The equatorial coordinates are J2000.0.  Use the routine
 *     Ge50 if conversion to B1950.0 'FK4' coordinates is
 *     required.
 *  </dd>
 *  <dt>Reference:</dt>
 *  <dd>Blaauw et al, Mon.Not.R.astron.Soc.,121,123 (1960)</dd>
 *  </dl>
 *
 *  @param gl Galactic longitude and latitude l2, b2
 *  @return J2000.0 RA, dec
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     dl,db       double      galactic longitude and latitude l2,b2
 *
 *  Returned:
 *     *dr,*dd     double      J2000.0 RA,dec
 *
 *  (all arguments are radians)
 *
 *  Called:
 *     Dcs2c, Dimxv, Dcc2s, Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Galeq ( Galactic gl )
    {
        double v1[], v2[] = new double[3];
        AngleDR d = new AngleDR ( gl.getLongitude(), gl.getLatitude() );

/*
 *  l2,b2 system of Galactic coordinates
 *
 *  p = 192.25       RA of Galactic north pole (mean B1950.0)
 *  q =  62.6        inclination of Galactic to mean B1950.0 equator
 *  r =  33          longitude of ascending node
 *
 *  p,q,r are degrees
 *
 *  Equatorial to Galactic rotation matrix (J2000.0), obtained by
 *  applying the standard FK4 to FK5 transformation, for zero proper
 *  motion in FK5, to the columns of the B1950 equatorial to
 *  Galactic rotation matrix:
 */
        final double rmat[][] =
        {
           { -0.054875539726, -0.873437108010, -0.483834985808 },
           {  0.494109453312, -0.444829589425,  0.746982251810 },
           { -0.867666135858, -0.198076386122,  0.455983795705 }
        };

/* Spherical to Cartesian */
        v1 = Dcs2c ( d );

/* Galactic to equatorial */
        v2 = Dimxv ( rmat, v1 );

/* Cartesian to spherical */
        d = Dcc2s ( v2 );
        d.setAlpha( Dranrm ( d.getAlpha() ) );
        d.setDelta( Drange ( d.getDelta() ) );

/* Express in conventional ranges */
        return d;
}

/**
 *  Transformation from IAU 1958 Galactic coordinates to
 *  De Vaucouleurs supergalactic coordinates.
 *  <dl>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li>De Vaucouleurs, De Vaucouleurs, &amp; Corwin, Second reference
 *     catalogue of bright galaxies, U. Texas, page 8.</li>
 *
 *     <li>Systems &amp; Applied Sciences Corp., Documentation for the
 *     machine-readable version of the above catalogue,
 *     contract NAS 5-26490.</li>
 *  </ol> 
 *  <dd>(These two references give different values for the Galactic
 *     longitude of the Supergalactic origin.  Both are wrong;  the
 *     correct value is l2 = 137.37.)
 *  </dd> </dl>
 *
 *  @param gl Galactic longitude and latitude l2,b2
 *  @return Supergalactic longitude and latitude
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     dl,db       double       Galactic longitude and latitude l2,b2
 *
 *  Returned:
 *     *dsl,*dsb   double       Supergalactic longitude and latitude
 *
 *  (all arguments are radians)
 *
 *  Called:
 *     Dcs2c, Dmxv, Dcc2s, Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Galactic Galsup ( Galactic gl )
    {
       double v1[], v2[];
       AngleDR d = new AngleDR ( gl.getLongitude(), gl.getLatitude() );

/*
 *  System of Supergalactic coordinates:
 *
 *    SGl   SGb        l2     b2      (deg)
 *     -    +90      47.37  +6.32
 *     0     0         -      0
 *
 *  Galactic to Supergalactic rotation matrix:
 */
        final double rmat[][] =
        {
           { -0.735742574804,  0.677261296414,  0.0            },
           { -0.074553778365, -0.080991471307,  0.993922590400 },
           {  0.673145302109,  0.731271165817,  0.110081262225 }
        };

/* Spherical to Cartesian */
        v1 = Dcs2c ( d );

/* Galactic to Supergalactic */
        v2 = Dmxv ( rmat, v1 );

/* Cartesian to spherical */
        d = Dcc2s ( v2 );

/* Express in conventional ranges */
        return new Galactic ( Dranrm ( d.getAlpha() ),
                                 Drange ( d.getDelta() ) );
    }

/**
 *  Convert geodetic position to geocentric.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li> Geocentric latitude can be obtained by evaluating atan2(z,r).</li>
 *
 *  <li> IAU 1976 constants are used.</li>
 *  </ol> </dd>
 *  <dt>Reference:</dt>
 *  <dd> Green,R.M., Spherical Astronomy, CUP 1985, p98.</dd>
 *  </dl>
 *
 *  @param p latitude (geodetic, radians)
 *  @param h height above reference spheroid (geodetic, metres)
 *  @return distance from Earth axis (AU),
 *          distance from plane of Earth equator (AU)
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     p     double     latitude (geodetic, radians)
 *     h     double     height above reference spheroid (geodetic, metres)
 *
 *  Returned:
 *     *r    double     distance from Earth axis (AU)
 *     *z    double     distance from plane of Earth equator (AU)
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Geoc ( double p, double h )
    {
        double sp, cp, c, s;
        double r[] = new double[2];

/* Earth equatorial radius (metres) */
        final double a0 = 6378140.0;

/* Reference spheroid flattening factor and useful function thereof */
        final double f = 1.0 / 298.257;
        double b = ( 1.0 - f ) * ( 1.0 - f );

/* Astronomical unit in metres */
        final double au = 1.49597870e11;

/* Geodetic to geocentric conversion */
        sp = Math.sin ( p );
        cp = Math.cos ( p );
        c = 1.0 / Math.sqrt ( cp * cp + b * sp * sp );
        s = b * c;
        r[0] = ( a0 * c + h ) * cp / au;
        r[1] = ( a0 * s + h ) * sp / au;
        return r;
     }

/**
 *  Conversion from Universal Time to Sidereal Time.
 *  <p>
 *  The IAU 1982 expression (see page S15 of the 1984 Astronomical
 *  Almanac) is used, but rearranged to reduce rounding errors.
 *  This expression is always described as giving the GMST at
 *  0 hours UT.  In fact, it gives the difference between the
 *  GMST and the UT, which happens to equal the GMST (modulo
 *  24 hours) at 0 hours UT each day.  In this routine, the
 *  entire UT is used directly as the argument for the
 *  standard formula, and the fractional part of the UT is
 *  added separately;  note that the factor 1.0027379... does
 *  not appear.
 *  </p> <p>
 *  See also the routine Gmsta, which delivers better numerical
 *  precision by accepting the UT date and time as separate arguments.
 *  </p>
 *
 *  @param ut1 Universal Time (strictly UT1) expressed as
 *                    Modified Julian Date (JD-2400000.5)
 *  @return Greenwich Mean Sidereal Time (radians)
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *    ut1    double     Universal Time (strictly UT1) expressed as
 *                      Modified Julian Date (JD-2400000.5)
 *
 *  The result is the Greenwich Mean Sidereal Time (double
 *  precision, radians).
 *
 *  Called:  Dranrm
 *
 *  Defined in mac.h:  D2PI, DS2R, dmod
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Gmst ( double ut1 )
    {
        double tu;

/* Julian centuries from fundamental epoch J2000 to this UT */
        tu = ( ut1 - 51544.5 ) / 36525.0;

/* GMST at this UT */
        return Dranrm ( dmod ( ut1, 1.0 ) * D2PI +
                       ( 24110.54841 +
                       ( 8640184.812866 +
                       ( 0.093104 - 6.2e-6 * tu ) * tu ) * tu ) * DS2R );
    }

/**
 *  Select epoch prefix 'B' or 'J'.
 *
 *  @param jb Dbjin prefix status:  0=none, 1='B', 2='J'
 *  @param e epoch - Besselian or Julian
 *  @return 'B' or 'J'
 *  @throws palError Illegal prefix
 *  <p>
 *  If jb=0, B is assumed for e &lt; 1984.0, otherwise J.
 *  </p>
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     jb     int         Dbjin prefix status:  0=none, 1='B', 2='J'
 *     e      double      epoch - Besselian or Julian
 *
 *  Returned:
 *     *k     char        'B' or 'J'
 *     *j     int         status:  0=OK
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public char Kbj ( int jb, double e ) throws palError
    {

/* Preset status */
        Status = 0;
        char k;

/* If prefix given expressly, use it */
        if ( jb == 1 ) {
            k = 'B';
        } else if ( jb == 2 ) {
            k = 'J';

/* If no prefix, examine the epoch */
        } else if ( jb == 0 ) {

/* If epoch is pre-1984.0, assume Besselian;  otherwise Julian */
            if ( e < 1984.0 ) {
                k = 'B';
            } else {
                k = 'J';
            }

/* If illegal prefix, return error status */
        } else {
            k = ' ';
            Status = 1;
            throw new palError(Status, "Kbj: Illegal prefix");
        }
        return k;
    }

/**
 *  Transform star RA,Dec from mean place to geocentric apparent.
 *  <p>
 *    The reference frames and timescales used are post IAU 1976.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>eq is the Julian epoch specifying both the reference frame and
 *      the epoch of the position - usually 2000.  For positions where
 *      the epoch and equinox are different, use the routine Pm to
 *      apply proper motion corrections before using this routine.</li>
 *
 *  <li>The distinction between the required TDB and TT is always
 *      negligible.  Moreover, for all but the most critical
 *      applications UTC is adequate.</li>
 *
 *  <li>The proper motions in RA are dRA/dt rather than cos(Dec)*dRA/dt.</li>
 *
 *  <li>This routine may be wasteful for some applications because it
 *      recomputes the Earth position/velocity and the precession-
 *      nutation matrix each time, and because it allows for parallax
 *      and proper motion.  Where multiple transformations are to be
 *      carried out for one epoch, a faster method is to call the
 *      Mappa routine once and then either the Mapqk routine
 *      (which includes parallax and proper motion) or Mapqkz (which
 *      assumes zero parallax and proper motion).</li>
 *
 *  <li>The accuracy is sub-milliarcsecond, limited by the
 *      precession-nutation model (IAU 1976 precession, Shirai &amp;
 *      Fukushima 2001 forced nutation and precession corrections).</li>
 *
 *  <li>The accuracy is further limited by the routine Evp, called
 *      by Mappa, which computes the Earth position and velocity
 *      using the methods of Stumpff.  The maximum error is about
 *      0.3 mas.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd>1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)</dd>
 *  </dl>
 *
 *  @param sd  mean RA,Dec (rad)
 *             proper motions (RA,Dec changes per Julian year),
 *             parallax (arcsec),
 *             radial velocity (km/sec, +ve if receding)
 *  @param epq  Epoch and equinox of star data (Julian)
 *  @param date TDB for apparent place (JD-2400000.5)
 *  @return     Apparent RA,Dec (rad)
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     rm,dm    double     mean RA,Dec (rad)
 *     pr,pd    double     proper motions:  RA,Dec changes per Julian year
 *     px       double     parallax (arcsec)
 *     rv       double     radial velocity (km/sec, +ve if receding)
 *     eq       double     epoch and equinox of star data (Julian)
 *     date     double     TDB for apparent place (JD-2400000.5)
 *
 *  Returned:
 *     *ra,*da  double     apparent RA,Dec (rad)
 *
 *  Called:
 *     Mappa       star-independent parameters
 *     Mapqk       quick mean to apparent
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Map ( Stardata sd, double epq, double date )
    {
        AMParams amprms;

/* Star-independent parameters */
        amprms = Mappa ( epq, date );

/* Mean to apparent */
        AngleDR ra = Mapqk ( sd, amprms );
        return ra;
    }

/**
 *  Compute star-independent parameters in preparation for
 *  conversions between mean place and geocentric apparent place.
 *  <p>
 *  The parameters produced by this routine are required in the
 *  parallax, light deflection, aberration, and precession/nutation
 *  parts of the mean/apparent transformations.
 *  </p>
 *  <p>
 *  The reference frames and timescales used are post IAU 1976.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>For date, the distinction between the required TDB and TT
 *      is always negligible.  Moreover, for all but the most
 *      critical applications UTC is adequate.</li>
 *
 *  <li>The vectors amprms(1-3) and amprms(4-6) are referred to the
 *      mean equinox and equator of epoch eq.</li>
 *
 *  <li>The parameters AMPRMS produced by this routine are used by
 *      Ampqk, Mapqk and Mapqkz.</li>
 *
 *  <li>The accuracy is sub-milliarcsecond, limited by the
 *      precession-nutation model (IAU 1976 precession, Shirai &amp;
 *      Fukushima 2001 forced nutation and precession corrections).</li>
 *
 *  <li>A further limit to the accuracy of routines using the parameter
 *      array AMPRMS is imposed by the routine Evp, used here to
 *      compute the Earth position and velocity by the methods of
 *      Stumpff.  The maximum error in the resulting aberration
 *      corrections is about 0.3 milliarcsecond.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd>1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)
 *  </dd> </dl>
 *
 *  @param eq epoch of mean equinox to be used (Julian)
 *  @param date TDB (JD-2400000.5)
 *  @return star-independent mean-to-apparent parameters
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *  Given:
 *     eq       double      epoch of mean equinox to be used (Julian)
 *     date     double      TDB (JD-2400000.5)
 *
 *  Returned:
 *     amprms   double[21]  star-independent mean-to-apparent parameters:
 *
 *       (0)      time interval for proper motion (Julian years)
 *       (1-3)    barycentric position of the Earth (AU)
 *       (4-6)    heliocentric direction of the Earth (unit vector)
 *       (7)      (grav rad Sun)*2/(Sun-Earth distance)
 *       (8-10)   abv: barycentric Earth velocity in units of c
 *       (11)     sqrt(1-v *2) where v=modulus(abv)
 *       (12-20)  precession/nutation (3,3) matrix
 *
 *  Called:
 *     Epj, Evp, Dvn, Prenut
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AMParams Mappa ( double eq, double date )
    {
       int i;
       AMParams amprms = new AMParams( );

       double ebd[] = new double[3], ehd[] = new double[3],
              eh[] = new double[3], e=1.0, vn[] = new double[3], vm = 0.0;

/* Time interval for proper motion correction */
       amprms.setTimeint( Epj ( date ) - eq );

/* Get Earth barycentric and heliocentric position and velocity */
       Evp ( date, eq, ebd, amprms.getBary(), ehd, eh );

/* Heliocentric direction of Earth (normalized) and modulus */
       e = Dvn ( eh, amprms.getHelio() );

/* Light deflection parameter */
       amprms.setGrad( GR2 / e );

/* Aberration parameters */
       for ( i = 0; i < 3; i++ ) {
          ebd[i] = ( ebd[i] * AUSEC );
       }
       amprms.setEarthv( ebd );
       vm = Dvn ( ebd, vn );
       amprms.setRoot( Math.sqrt ( 1.0 - vm * vm ) );

/* Precession/nutation matrix */
       amprms.setPrecess( Prenut ( eq, date ) );

       return amprms;
    }

/**
 *  Quick mean to apparent place:  transform a star RA,Dec from
 *  mean place to geocentric apparent place, given the
 *  star-independent parameters.
 *  <p>
 *  Use of this routine is appropriate when efficiency is important
 *  and where many star positions, all referred to the same equator
 *  and equinox, are to be transformed for one epoch.  The
 *  star-independent parameters can be obtained by calling the
 *  Mappa routine.
 *  </p>
 *  <p>
 *  If the parallax and proper motions are zero the Mapqkz
 *  routine can be used instead.
 *  </p>
 *  <p>
 *  The reference frames and timescales used are post IAU 1976.
 *  </p>
 *
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li> The vectors amprms(1-3) and amprms(4-6) are referred to
 *       the mean equinox and equator of epoch eq.</li>
 *
 *  <li> Strictly speaking, the routine is not valid for solar-system
 *       sources, though the error will usually be extremely small.
 *       However, to prevent gross errors in the case where the
 *       position of the Sun is specified, the gravitational
 *       deflection term is restrained within about 920 arcsec of the
 *       centre of the Sun's disc.  The term has a maximum value of
 *       about 1.85 arcsec at this radius, and decreases to zero as
 *       the centre of the disc is approached.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd>1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)</dd>
 *  </dl>
 *
 *  @param s Mean RA,Dec (rad),
 *           proper motions (RA,Dec changes per Julian year),
 *           parallax (arcsec),
 *           radial velocity (km/sec, +ve if receding)
 *  @param amprms star-independent mean-to-apparent parameters
 *  @return Apparent RA,Dec (rad)
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     rm,dm    double      mean RA,Dec (rad)
 *     pr,pd    double      proper motions:  RA,Dec changes per Julian year
 *     px       double      parallax (arcsec)
 *     rv       double      radial velocity (km/sec, +ve if receding)
 *
 *     amprms   double[21]  star-independent mean-to-apparent parameters:
 *
 *       (0)      time interval for proper motion (Julian years)
 *       (1-3)    barycentric position of the Earth (AU)
 *       (4-6)    heliocentric direction of the Earth (unit vector)
 *       (7)      (grav rad Sun)*2/(Sun-Earth distance)
 *       (8-10)   barycentric Earth velocity in units of c
 *       (11)     sqrt(1-v *2) where v=modulus(abv)
 *       (12-20)  precession/nutation (3,3) matrix
 *
 *  Returned:
 *     *ra,*da  double      apparent RA,Dec (rad)
 *
 *  Called:
 *     Dcs2c       spherical to Cartesian
 *     Dvdv        dot product
 *     Dmxv        matrix x vector
 *     Dcc2s       Cartesian to spherical
 *     Dranrm      normalize angle 0-2pi
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Mapqk ( Stardata s, AMParams amprms )
    {
        int i;
        double pmt, gr2e, ab1, eb[], ehn[], abv[],
          q[], pxr, w, em[] = new double[3], p[] = new double[3],
          pn[] = new double[3], pde, pdep1,
          p1[] = new double[3], p1dv, p2[] = new double[3], p3[] = new double[3];

/* Unpack scalar and vector parameters */
        pmt = amprms.getTimeint();
        gr2e = amprms.getGrad();
        ab1 = amprms.getRoot();
        eb = amprms.getBary();
        ehn = amprms.getHelio();
        abv = amprms.getEarthv();

/* Spherical to x,y,z */
        AngleDR r = s.getAngle();
        double rm = r.getAlpha();
        double dm = r.getDelta();
        double pm[] = s.getMotion();
        double px = s.getParallax();
        double rv = s.getRV();
        q = Dcs2c ( r );

/* Space motion (radians per year) */
        pxr = px * DAS2R;
        w = VF * rv * pxr;
        em[0] = (-pm[0] * q[1]) - ( pm[1] * Math.cos ( rm ) * Math.sin ( dm ) ) +
                ( w * q[0] );
        em[1] = ( pm[0] * q[0]) - ( pm[1] * Math.sin ( rm ) * Math.sin ( dm ) ) +
                ( w * q[1] );
        em[2] =                   ( pm[1] * Math.cos ( dm ) ) +
                ( w * q[2] );

/* Geocentric direction of star (normalized) */
        for ( i = 0; i < 3; i++ ) {
            p[i] = q[i] + ( pmt * em[i] ) - ( pxr * eb[i] );
        }
        w = Dvn ( p, pn );

/* Light deflection (restrained within the Sun's disc) */
        pde = Dvdv ( pn, ehn );
        pdep1 = 1.0 + pde;
        w = gr2e / gmax ( pdep1, 1.0e-5 );
        for ( i = 0; i < 3; i++ ) {
            p1[i] = pn[i] + ( w * ( ehn[i] - pde * pn[i] ) );
        }

/* Aberration (normalization omitted) */
        p1dv = Dvdv ( p1, abv );
        w = 1.0 + p1dv / ( ab1 + 1.0 );
        for ( i = 0; i < 3; i++ ) {
            p2[i] = ab1 * p1[i] + w * abv[i];
        }

/* Precession and nutation */
        p3 = Dmxv ( amprms.getPrecess(), p2 );

/* Geocentric apparent RA,dec */
        AngleDR ra = Dcc2s ( p3 );

        ra.setAlpha( Dranrm ( ra.getAlpha()) );
        return ra;
    }

/**
 *  Quick mean to apparent place:  transform a star RA,dec from
 *  mean place to geocentric apparent place, given the
 *  star-independent parameters, and assuming zero parallax
 *  and proper motion.
 *  <p>
 *   Use of this routine is appropriate when efficiency is important
 *   and where many star positions, all with parallax and proper
 *   motion either zero or already allowed for, and all referred to
 *   the same equator and equinox, are to be transformed for one
 *   epoch.  The star-independent parameters can be obtained by
 *   calling the Mappa routine.
 *  </p> <p>
 *   The corresponding routine for the case of non-zero parallax
 *   and proper motion is Mapqk.
 *  </p> <p>
 *  The reference frames and timescales used are post IAU 1976.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>  The vectors amprms(1-3) and amprms(4-6) are referred to the
 *        mean equinox and equator of epoch eq.</li>
 *
 *  <li>  Strictly speaking, the routine is not valid for solar-system
 *        sources, though the error will usually be extremely small.
 *        However, to prevent gross errors in the case where the
 *        position of the Sun is specified, the gravitational
 *        deflection term is restrained within about 920 arcsec of the
 *        centre of the Sun's disc.  The term has a maximum value of
 *        about 1.85 arcsec at this radius, and decreases to zero as
 *        the centre of the disc is approached.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd>1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)</dd>
 *  </dl>
 *
 *  @param rm     Mean RA,dec (rad)
 *  @param amprms Star-independent mean-to-apparent parameters
 *  @return Apparent RA,dec (rad)
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *     rm,dm    double      mean RA,dec (rad)
 *     amprms   double[21]  star-independent mean-to-apparent parameters:
 *
 *       (0-3)    not used
 *       (4-6)    heliocentric direction of the Earth (unit vector)
 *       (7)      (grav rad Sun)*2/(Sun-Earth distance)
 *       (8-10)   abv: barycentric Earth velocity in units of c
 *       (11)     sqrt(1-v *2) where v=modulus(abv)
 *       (12-20)  precession/nutation (3,3) matrix
 *
 *  Returned:
 *     *ra,*da  double      apparent RA,dec (rad)
 *
 *  Called:
 *     Dcs2c       spherical to Cartesian
 *     Dvdv        dot product
 *     Dmxv        matrix x vector
 *     Dcc2s       Cartesian to spherical
 *     Dranrm      normalize angle 0-2pi
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Mapqkz ( AngleDR rm, AMParams amprms )
    {
        double gr2e, ab1, p[], pde, pdep1, w, p1dv, p1dvp1,
          p1[] = new double[3], p2[] = new double[3];

/* Unpack scalar and vector parameters */
        gr2e = amprms.getGrad();
        ab1 = amprms.getRoot();
        double ehn[] = amprms.getHelio();
        double abv[] = amprms.getEarthv();

/* Spherical to x,y,z */
        p = Dcs2c ( rm );

/* Light deflection */
        pde = Dvdv ( p, ehn );
        pdep1 = pde + 1.0;
        w = gr2e / gmax ( pdep1, 1e-5 );
        for ( int i = 0; i < 3; i++ ) {
           p1[i] = p[i] + w * ( ehn[i] - pde * p[i] );
        }

/* Aberration */
        p1dv = Dvdv ( p1, abv );
        p1dvp1 = p1dv + 1.0;
        w = 1.0 + p1dv / ( ab1 + 1.0 );
        for ( int i = 0; i < 3; i++ ) {
           p2[i] = ( ( ab1 * p1[i] ) + ( w * abv[i] ) ) / p1dvp1;
        }

/* Precession and nutation */
        double p3[] = Dmxv ( amprms.getPrecess(), p2 );

/* Geocentric apparent RA,dec */
        AngleDR a = Dcc2s ( p3 );
        a.setAlpha( Dranrm ( a.getAlpha() ) );
        return a;
}

/**
 *  Form the matrix of nutation for a given date (IAU 1980 theory).
 *
 *  @param date TDB (loosely ET) as Modified Julian Date (=JD-2400000.5)
 *  @return Nutation matrix
 *  <p>
 *  The matrix is in the sense   v(true)  =  rmatn * v(mean) .
 *  </p>
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  References:
 *     Shirai, T. &amp; Fukushima, T., Astron.J. 121, 3270-3283 (2001).
 *
 *  Given:
 *     date   double        TDB (loosely ET) as Modified Julian Date
 *                                           (=JD-2400000.5)
 *
 *  Returned:
 *     rmatn  double[3][3]  nutation matrix
 *
 *  Called:   Nutc, Deuler
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Nut ( double date )
    {
        double nc[];
        double rmatn[][];

/* Nutation components and mean obliquity */
        nc = Nutc ( date );
        double dpsi = nc[0], deps = nc[1], eps0 = nc[2];

/* Rotation matrix */
        rmatn = Deuler ( "xzx", eps0, -dpsi, - ( eps0 + deps ) );
        return rmatn;
    }

/**
 *  Nutation:  longitude &amp; obliquity components and
 *             mean obliquity (IAU 1980 theory).
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The routine predicts forced nutation (but not free core nutation)
 *      plus corrections to the IAU 1976 precession model.</li>
 *
 *  <li>Earth attitude predictions made by combining the present nutation
 *     model with IAU 1976 precession are accurate to 1 mas (with respect
 *     to the ICRF) for a few decades around 2000.</li>
 *
 *  <li>The slaNutc80 routine is the equivalent of the present routine
 *     but using the IAU 1980 nutation theory.  The older theory is less
 *     accurate, leading to errors as large as 350 mas over the interval
 *     1900-2100, mainly because of the error in the IAU 1976 precession.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li>Shirai, T. &amp; Fukushima, T., Astron.J. 121, 3270-3283 (2001).</li>
 *
 *  <li>Fukushima, T., 1991, Astron.Astrophys. 244, L11 (1991).</li>
 *
 *  <li>Simon, J. L., Bretagnon, P., Chapront, J., Chapront-Touze, M.,
 *     Francou, G. &amp; Laskar, J., Astron.Astrophys. 282, 663 (1994).</li>
 *  </ol> </dd> </dl>
 *
 *  @param date TDB (loosely ET) as Modified Julian Date (JD-2400000.5)
 *  @return Nutation in longitude, obliquity, and mean obliquity
 */

/*  Latest Revision: 6 December 2001 (RTP)
 *
 *  Called:  Deuler, Prec, Epj, Dmxm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Nutc ( double date )
    {
/*
 * --------------------------------
 * The SF2001 forced nutation model
 * --------------------------------
 */

/* Coefficients of fundamental angles */
        final int na[][] = {
           {  0,    0,    0,    0,   -1,    0,    0,    0,    0 },
           {  0,    0,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  0,    0,    0,    0,   -2,    0,    0,    0,    0 },
           {  0,    1,    0,    0,    0,    0,    0,    0,    0 },
           {  0,    1,    2,   -2,    2,    0,    0,    0,    0 },
           {  1,    0,    0,    0,    0,    0,    0,    0,    0 },
           {  0,    0,    2,    0,    1,    0,    0,    0,    0 },
           {  1,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  0,   -1,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    0,    2,   -2,    1,    0,    0,    0,    0 },
           { -1,    0,    2,    0,    2,    0,    0,    0,    0 },
           { -1,    0,    0,    2,    0,    0,    0,    0,    0 },
           {  1,    0,    0,    0,    1,    0,    0,    0,    0 },
           {  1,    0,    0,    0,   -1,    0,    0,    0,    0 },
           { -1,    0,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    0,    2,    0,    1,    0,    0,    0,    0 },
           { -2,    0,    2,    0,    1,    0,    0,    0,    0 },
           {  0,    0,    0,    2,    0,    0,    0,    0,    0 },
           {  0,    0,    2,    2,    2,    0,    0,    0,    0 },
           {  2,    0,    0,   -2,    0,    0,    0,    0,    0 },
           {  2,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  1,    0,    2,   -2,    2,    0,    0,    0,    0 },
           { -1,    0,    2,    0,    1,    0,    0,    0,    0 },
           {  2,    0,    0,    0,    0,    0,    0,    0,    0 },
           {  0,    0,    2,    0,    0,    0,    0,    0,    0 },
           {  0,    1,    0,    0,    1,    0,    0,    0,    0 },
           { -1,    0,    0,    2,    1,    0,    0,    0,    0 },
           {  0,    2,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    0,    2,   -2,    0,    0,    0,    0,    0 },
           { -1,    0,    0,    2,   -1,    0,    0,    0,    0 },
           {  0,    1,    0,    0,   -1,    0,    0,    0,    0 },
           {  0,    2,    0,    0,    0,    0,    0,    0,    0 },
           { -1,    0,    2,    2,    1,    0,    0,    0,    0 },
           {  1,    0,    2,    2,    2,    0,    0,    0,    0 },
           {  0,    1,    2,    0,    2,    0,    0,    0,    0 },
           { -2,    0,    2,    0,    0,    0,    0,    0,    0 },
           {  0,    0,    2,    2,    1,    0,    0,    0,    0 },
           {  0,   -1,    2,    0,    2,    0,    0,    0,    0 },
           {  0,    0,    0,    2,    1,    0,    0,    0,    0 },
           {  1,    0,    2,   -2,    1,    0,    0,    0,    0 },
           {  2,    0,    0,   -2,   -1,    0,    0,    0,    0 },
           {  2,    0,    2,   -2,    2,    0,    0,    0,    0 },
           {  2,    0,    2,    0,    1,    0,    0,    0,    0 },
           {  0,    0,    0,    2,   -1,    0,    0,    0,    0 },
           {  0,   -1,    2,   -2,    1,    0,    0,    0,    0 },
           { -1,   -1,    0,    2,    0,    0,    0,    0,    0 },
           {  2,    0,    0,   -2,    1,    0,    0,    0,    0 },
           {  1,    0,    0,    2,    0,    0,    0,    0,    0 },
           {  0,    1,    2,   -2,    1,    0,    0,    0,    0 },
           {  1,   -1,    0,    0,    0,    0,    0,    0,    0 },
           { -2,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  0,   -1,    0,    2,    0,    0,    0,    0,    0 },
           {  3,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  0,    0,    0,    1,    0,    0,    0,    0,    0 },
           {  1,   -1,    2,    0,    2,    0,    0,    0,    0 },
           {  1,    0,    0,   -1,    0,    0,    0,    0,    0 },
           { -1,   -1,    2,    2,    2,    0,    0,    0,    0 },
           { -1,    0,    2,    0,    0,    0,    0,    0,    0 },
           {  2,    0,    0,    0,   -1,    0,    0,    0,    0 },
           {  0,   -1,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    1,    2,    0,    2,    0,    0,    0,    0 },
           {  2,    0,    0,    0,    1,    0,    0,    0,    0 },
           {  1,    1,    0,    0,    0,    0,    0,    0,    0 },
           {  1,    0,   -2,    2,   -1,    0,    0,    0,    0 },
           {  1,    0,    2,    0,    0,    0,    0,    0,    0 },
           { -1,    1,    0,    1,    0,    0,    0,    0,    0 },
           {  1,    0,    0,    0,    2,    0,    0,    0,    0 },
           { -1,    0,    1,    0,    1,    0,    0,    0,    0 },
           {  0,    0,    2,    1,    2,    0,    0,    0,    0 },
           { -1,    1,    0,    1,    1,    0,    0,    0,    0 },
           { -1,    0,    2,    4,    2,    0,    0,    0,    0 },
           {  0,   -2,    2,   -2,    1,    0,    0,    0,    0 },
           {  1,    0,    2,    2,    1,    0,    0,    0,    0 },
           {  1,    0,    0,    0,   -2,    0,    0,    0,    0 },
           { -2,    0,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    1,    2,   -2,    2,    0,    0,    0,    0 },
           { -2,    0,    2,    4,    2,    0,    0,    0,    0 },
           { -1,    0,    4,    0,    2,    0,    0,    0,    0 },
           {  2,    0,    2,   -2,    1,    0,    0,    0,    0 },
           {  1,    0,    0,   -1,   -1,    0,    0,    0,    0 },
           {  2,    0,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    0,    0,    2,    1,    0,    0,    0,    0 },
           {  3,    0,    0,    0,    0,    0,    0,    0,    0 },
           {  0,    0,    2,   -2,   -1,    0,    0,    0,    0 },
           {  3,    0,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    0,    4,   -2,    2,    0,    0,    0,    0 },
           { -1,    0,    0,    4,    0,    0,    0,    0,    0 },
           {  0,    1,    2,    0,    1,    0,    0,    0,    0 },
           {  0,    0,    2,   -2,    3,    0,    0,    0,    0 },
           { -2,    0,    0,    4,    0,    0,    0,    0,    0 },
           { -1,   -1,    0,    2,    1,    0,    0,    0,    0 },
           { -2,    0,    2,    0,   -1,    0,    0,    0,    0 },
           {  0,    0,    2,    0,   -1,    0,    0,    0,    0 },
           {  0,   -1,    2,    0,    1,    0,    0,    0,    0 },
           {  0,    1,    0,    0,    2,    0,    0,    0,    0 },
           {  0,    0,    2,   -1,    2,    0,    0,    0,    0 },
           {  2,    1,    0,   -2,    0,    0,    0,    0,    0 },
           {  0,    0,    2,    4,    2,    0,    0,    0,    0 },
           { -1,   -1,    0,    2,   -1,    0,    0,    0,    0 },
           { -1,    1,    0,    2,    0,    0,    0,    0,    0 },
           {  1,   -1,    0,    0,    1,    0,    0,    0,    0 },
           {  0,   -1,    2,   -2,    0,    0,    0,    0,    0 },
           {  0,    1,    0,    0,   -2,    0,    0,    0,    0 },
           {  1,   -1,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    0,    0,    2,   -1,    0,    0,    0,    0 },
           { -1,    1,    2,    2,    2,    0,    0,    0,    0 },
           {  3,    0,    2,    0,    1,    0,    0,    0,    0 },
           {  0,    1,    2,    2,    2,    0,    0,    0,    0 },
           {  1,    0,    2,   -2,    0,    0,    0,    0,    0 },
           { -1,    0,   -2,    4,   -1,    0,    0,    0,    0 },
           { -1,   -1,    2,    2,    1,    0,    0,    0,    0 },
           {  0,   -1,    2,    2,    1,    0,    0,    0,    0 },
           {  2,   -1,    2,    0,    2,    0,    0,    0,    0 },
           {  0,    0,    0,    2,    2,    0,    0,    0,    0 },
           {  1,   -1,    2,    0,    1,    0,    0,    0,    0 },
           { -1,    1,    2,    0,    2,    0,    0,    0,    0 },
           {  0,    1,    0,    2,    0,    0,    0,    0,    0 },
           {  0,    1,    2,   -2,    0,    0,    0,    0,    0 },
           {  0,    3,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    0,    0,    1,    1,    0,    0,    0,    0 },
           { -1,    0,    2,    2,    0,    0,    0,    0,    0 },
           {  2,    1,    2,    0,    2,    0,    0,    0,    0 },
           {  1,    1,    0,    0,    1,    0,    0,    0,    0 },
           {  2,    0,    0,    2,    0,    0,    0,    0,    0 },
           {  1,    1,    2,    0,    1,    0,    0,    0,    0 },
           { -1,    0,    0,    2,    2,    0,    0,    0,    0 },
           {  1,    0,   -2,    2,    0,    0,    0,    0,    0 },
           {  0,   -1,    0,    2,   -1,    0,    0,    0,    0 },
           { -1,    0,    1,    0,    2,    0,    0,    0,    0 },
           {  0,    1,    0,    1,    0,    0,    0,    0,    0 },
           {  1,    0,   -2,    2,   -2,    0,    0,    0,    0 },
           {  0,    0,    0,    1,   -1,    0,    0,    0,    0 },
           {  1,   -1,    0,    0,   -1,    0,    0,    0,    0 },
           {  0,    0,    0,    4,    0,    0,    0,    0,    0 },
           {  1,   -1,    0,    2,    0,    0,    0,    0,    0 },
           {  1,    0,    2,    1,    2,    0,    0,    0,    0 },
           {  1,    0,    2,   -1,    2,    0,    0,    0,    0 },
           { -1,    0,    0,    2,   -2,    0,    0,    0,    0 },
           {  0,    0,    2,    1,    1,    0,    0,    0,    0 },
           { -1,    0,    2,    0,   -1,    0,    0,    0,    0 },
           { -1,    0,    2,    4,    1,    0,    0,    0,    0 },
           {  0,    0,    2,    2,    0,    0,    0,    0,    0 },
           {  1,    1,    2,   -2,    1,    0,    0,    0,    0 },
           {  0,    0,    1,    0,    1,    0,    0,    0,    0 },
           { -1,    0,    2,   -1,    1,    0,    0,    0,    0 },
           { -2,    0,    2,    2,    1,    0,    0,    0,    0 },
           {  2,   -1,    0,    0,    0,    0,    0,    0,    0 },
           {  4,    0,    2,    0,    2,    0,    0,    0,    0 },
           {  2,    1,    2,   -2,    2,    0,    0,    0,    0 },
           {  0,    1,    2,    1,    2,    0,    0,    0,    0 },
           {  1,    0,    4,   -2,    2,    0,    0,    0,    0 },
           {  1,    1,    0,    0,   -1,    0,    0,    0,    0 },
           { -2,    0,    2,    4,    1,    0,    0,    0,    0 },
           {  2,    0,    2,    0,    0,    0,    0,    0,    0 },
           { -1,    0,    1,    0,    0,    0,    0,    0,    0 },
           {  1,    0,    0,    1,    0,    0,    0,    0,    0 },
           {  0,    1,    0,    2,    1,    0,    0,    0,    0 },
           { -1,    0,    4,    0,    1,    0,    0,    0,    0 },
           { -1,    0,    0,    4,    1,    0,    0,    0,    0 },
           {  2,    0,    2,    2,    1,    0,    0,    0,    0 },
           {  2,    1,    0,    0,    0,    0,    0,    0,    0 },
           {  0,    0,    5,   -5,    5,   -3,    0,    0,    0 },
           {  0,    0,    0,    0,    0,    0,    0,    2,    0 },
           {  0,    0,    1,   -1,    1,    0,    0,   -1,    0 },
           {  0,    0,   -1,    1,   -1,    1,    0,    0,    0 },
           {  0,    0,   -1,    1,    0,    0,    2,    0,    0 },
           {  0,    0,    3,   -3,    3,    0,    0,   -1,    0 },
           {  0,    0,   -8,    8,   -7,    5,    0,    0,    0 },
           {  0,    0,   -1,    1,   -1,    0,    2,    0,    0 },
           {  0,    0,   -2,    2,   -2,    2,    0,    0,    0 },
           {  0,    0,   -6,    6,   -6,    4,    0,    0,    0 },
           {  0,    0,   -2,    2,   -2,    0,    8,   -3,    0 },
           {  0,    0,    6,   -6,    6,    0,   -8,    3,    0 },
           {  0,    0,    4,   -4,    4,   -2,    0,    0,    0 },
           {  0,    0,   -3,    3,   -3,    2,    0,    0,    0 },
           {  0,    0,    4,   -4,    3,    0,   -8,    3,    0 },
           {  0,    0,   -4,    4,   -5,    0,    8,   -3,    0 },
           {  0,    0,    0,    0,    0,    2,    0,    0,    0 },
           {  0,    0,   -4,    4,   -4,    3,    0,    0,    0 },
           {  0,    1,   -1,    1,   -1,    0,    0,    1,    0 },
           {  0,    0,    0,    0,    0,    0,    0,    1,    0 },
           {  0,    0,    1,   -1,    1,    1,    0,    0,    0 },
           {  0,    0,    2,   -2,    2,    0,   -2,    0,    0 },
           {  0,   -1,   -7,    7,   -7,    5,    0,    0,    0 },
           { -2,    0,    2,    0,    2,    0,    0,   -2,    0 },
           { -2,    0,    2,    0,    1,    0,    0,   -3,    0 },
           {  0,    0,    2,   -2,    2,    0,    0,   -2,    0 },
           {  0,    0,    1,   -1,    1,    0,    0,    1,    0 },
           {  0,    0,    0,    0,    0,    0,    0,    0,    2 },
           {  0,    0,    0,    0,    0,    0,    0,    0,    1 },
           {  2,    0,   -2,    0,   -2,    0,    0,    3,    0 },
           {  0,    0,    1,   -1,    1,    0,    0,   -2,    0 },
           {  0,    0,   -7,    7,   -7,    5,    0,    0,    0 }
        };

/* Nutation series:  longitude. */
        final double psi[][] = {
           {  3341.5,  17206241.8,    3.1,  17409.5 },
           { -1716.8,  -1317185.3,    1.4,   -156.8 },
           {   285.7,   -227667.0,    0.3,    -23.5 },
           {   -68.6,   -207448.0,    0.0,    -21.4 },
           {   950.3,    147607.9,   -2.3,   -355.0 },
           {   -66.7,    -51689.1,    0.2,    122.6 },
           {  -108.6,     71117.6,    0.0,      7.0 },
           {    35.6,    -38740.2,    0.1,    -36.2 },
           {    85.4,    -30127.6,    0.0,     -3.1 },
           {     9.0,     21583.0,    0.1,    -50.3 },
           {    22.1,     12822.8,    0.0,     13.3 },
           {     3.4,     12350.8,    0.0,      1.3 },
           {   -21.1,     15699.4,    0.0,      1.6 },
           {     4.2,      6313.8,    0.0,      6.2 },
           {   -22.8,      5796.9,    0.0,      6.1 },
           {    15.7,     -5961.1,    0.0,     -0.6 },
           {    13.1,     -5159.1,    0.0,     -4.6 },
           {     1.8,      4592.7,    0.0,      4.5 },
           {   -17.5,      6336.0,    0.0,      0.7 },
           {    16.3,     -3851.1,    0.0,     -0.4 },
           {    -2.8,      4771.7,    0.0,      0.5 },
           {    13.8,     -3099.3,    0.0,     -0.3 },
           {     0.2,      2860.3,    0.0,      0.3 },
           {     1.4,      2045.3,    0.0,      2.0 },
           {    -8.6,      2922.6,    0.0,      0.3 },
           {    -7.7,      2587.9,    0.0,      0.2 },
           {     8.8,     -1408.1,    0.0,      3.7 },
           {     1.4,      1517.5,    0.0,      1.5 },
           {    -1.9,     -1579.7,    0.0,      7.7 },
           {     1.3,     -2178.6,    0.0,     -0.2 },
           {    -4.8,      1286.8,    0.0,      1.3 },
           {     6.3,      1267.2,    0.0,     -4.0 },
           {    -1.0,      1669.3,    0.0,     -8.3 },
           {     2.4,     -1020.0,    0.0,     -0.9 },
           {     4.5,      -766.9,    0.0,      0.0 },
           {    -1.1,       756.5,    0.0,     -1.7 },
           {    -1.4,     -1097.3,    0.0,     -0.5 },
           {     2.6,      -663.0,    0.0,     -0.6 },
           {     0.8,      -714.1,    0.0,      1.6 },
           {     0.4,      -629.9,    0.0,     -0.6 },
           {     0.3,       580.4,    0.0,      0.6 },
           {    -1.6,       577.3,    0.0,      0.5 },
           {    -0.9,       644.4,    0.0,      0.0 },
           {     2.2,      -534.0,    0.0,     -0.5 },
           {    -2.5,       493.3,    0.0,      0.5 },
           {    -0.1,      -477.3,    0.0,     -2.4 },
           {    -0.9,       735.0,    0.0,     -1.7 },
           {     0.7,       406.2,    0.0,      0.4 },
           {    -2.8,       656.9,    0.0,      0.0 },
           {     0.6,       358.0,    0.0,      2.0 },
           {    -0.7,       472.5,    0.0,     -1.1 },
           {    -0.1,      -300.5,    0.0,      0.0 },
           {    -1.2,       435.1,    0.0,     -1.0 },
           {     1.8,      -289.4,    0.0,      0.0 },
           {     0.6,      -422.6,    0.0,      0.0 },
           {     0.8,      -287.6,    0.0,      0.6 },
           {   -38.6,      -392.3,    0.0,      0.0 },
           {     0.7,      -281.8,    0.0,      0.6 },
           {     0.6,      -405.7,    0.0,      0.0 },
           {    -1.2,       229.0,    0.0,      0.2 },
           {     1.1,      -264.3,    0.0,      0.5 },
           {    -0.7,       247.9,    0.0,     -0.5 },
           {    -0.2,       218.0,    0.0,      0.2 },
           {     0.6,      -339.0,    0.0,      0.8 },
           {    -0.7,       198.7,    0.0,      0.2 },
           {    -1.5,       334.0,    0.0,      0.0 },
           {     0.1,       334.0,    0.0,      0.0 },
           {    -0.1,      -198.1,    0.0,      0.0 },
           {  -106.6,         0.0,    0.0,      0.0 },
           {    -0.5,       165.8,    0.0,      0.0 },
           {     0.0,       134.8,    0.0,      0.0 },
           {     0.9,      -151.6,    0.0,      0.0 },
           {     0.0,      -129.7,    0.0,      0.0 },
           {     0.8,      -132.8,    0.0,     -0.1 },
           {     0.5,      -140.7,    0.0,      0.0 },
           {    -0.1,       138.4,    0.0,      0.0 },
           {     0.0,       129.0,    0.0,     -0.3 },
           {     0.5,      -121.2,    0.0,      0.0 },
           {    -0.3,       114.5,    0.0,      0.0 },
           {    -0.1,       101.8,    0.0,      0.0 },
           {    -3.6,      -101.9,    0.0,      0.0 },
           {     0.8,      -109.4,    0.0,      0.0 },
           {     0.2,       -97.0,    0.0,      0.0 },
           {    -0.7,       157.3,    0.0,      0.0 },
           {     0.2,       -83.3,    0.0,      0.0 },
           {    -0.3,        93.3,    0.0,      0.0 },
           {    -0.1,        92.1,    0.0,      0.0 },
           {    -0.5,       133.6,    0.0,      0.0 },
           {    -0.1,        81.5,    0.0,      0.0 },
           {     0.0,       123.9,    0.0,      0.0 },
           {    -0.3,       128.1,    0.0,      0.0 },
           {     0.1,        74.1,    0.0,     -0.3 },
           {    -0.2,       -70.3,    0.0,      0.0 },
           {    -0.4,        66.6,    0.0,      0.0 },
           {     0.1,       -66.7,    0.0,      0.0 },
           {    -0.7,        69.3,    0.0,     -0.3 },
           {     0.0,       -70.4,    0.0,      0.0 },
           {    -0.1,       101.5,    0.0,      0.0 },
           {     0.5,       -69.1,    0.0,      0.0 },
           {    -0.2,        58.5,    0.0,      0.2 },
           {     0.1,       -94.9,    0.0,      0.2 },
           {     0.0,        52.9,    0.0,     -0.2 },
           {     0.1,        86.7,    0.0,     -0.2 },
           {    -0.1,       -59.2,    0.0,      0.2 },
           {     0.3,       -58.8,    0.0,      0.1 },
           {    -0.3,        49.0,    0.0,      0.0 },
           {    -0.2,        56.9,    0.0,     -0.1 },
           {     0.3,       -50.2,    0.0,      0.0 },
           {    -0.2,        53.4,    0.0,     -0.1 },
           {     0.1,       -76.5,    0.0,      0.0 },
           {    -0.2,        45.3,    0.0,      0.0 },
           {     0.1,       -46.8,    0.0,      0.0 },
           {     0.2,       -44.6,    0.0,      0.0 },
           {     0.2,       -48.7,    0.0,      0.0 },
           {     0.1,       -46.8,    0.0,      0.0 },
           {     0.1,       -42.0,    0.0,      0.0 },
           {     0.0,        46.4,    0.0,     -0.1 },
           {     0.2,       -67.3,    0.0,      0.1 },
           {     0.0,       -65.8,    0.0,      0.2 },
           {    -0.1,       -43.9,    0.0,      0.3 },
           {     0.0,       -38.9,    0.0,      0.0 },
           {    -0.3,        63.9,    0.0,      0.0 },
           {    -0.2,        41.2,    0.0,      0.0 },
           {     0.0,       -36.1,    0.0,      0.2 },
           {    -0.3,        58.5,    0.0,      0.0 },
           {    -0.1,        36.1,    0.0,      0.0 },
           {     0.0,       -39.7,    0.0,      0.0 },
           {     0.1,       -57.7,    0.0,      0.0 },
           {    -0.2,        33.4,    0.0,      0.0 },
           {    36.4,         0.0,    0.0,      0.0 },
           {    -0.1,        55.7,    0.0,     -0.1 },
           {     0.1,       -35.4,    0.0,      0.0 },
           {     0.1,       -31.0,    0.0,      0.0 },
           {    -0.1,        30.1,    0.0,      0.0 },
           {    -0.3,        49.2,    0.0,      0.0 },
           {    -0.2,        49.1,    0.0,      0.0 },
           {    -0.1,        33.6,    0.0,      0.0 },
           {     0.1,       -33.5,    0.0,      0.0 },
           {     0.1,       -31.0,    0.0,      0.0 },
           {    -0.1,        28.0,    0.0,      0.0 },
           {     0.1,       -25.2,    0.0,      0.0 },
           {     0.1,       -26.2,    0.0,      0.0 },
           {    -0.2,        41.5,    0.0,      0.0 },
           {     0.0,        24.5,    0.0,      0.1 },
           {   -16.2,         0.0,    0.0,      0.0 },
           {     0.0,       -22.3,    0.0,      0.0 },
           {     0.0,        23.1,    0.0,      0.0 },
           {    -0.1,        37.5,    0.0,      0.0 },
           {     0.2,       -25.7,    0.0,      0.0 },
           {     0.0,        25.2,    0.0,      0.0 },
           {     0.1,       -24.5,    0.0,      0.0 },
           {    -0.1,        24.3,    0.0,      0.0 },
           {     0.1,       -20.7,    0.0,      0.0 },
           {     0.1,       -20.8,    0.0,      0.0 },
           {    -0.2,        33.4,    0.0,      0.0 },
           {    32.9,         0.0,    0.0,      0.0 },
           {     0.1,       -32.6,    0.0,      0.0 },
           {     0.0,        19.9,    0.0,      0.0 },
           {    -0.1,        19.6,    0.0,      0.0 },
           {     0.0,       -18.7,    0.0,      0.0 },
           {     0.1,       -19.0,    0.0,      0.0 },
           {     0.1,       -28.6,    0.0,      0.0 },
           {     4.0,       178.8,  -11.8,      0.3 },
           {    39.8,      -107.3,   -5.6,     -1.0 },
           {     9.9,       164.0,   -4.1,      0.1 },
           {    -4.8,      -135.3,   -3.4,     -0.1 },
           {    50.5,        75.0,    1.4,     -1.2 },
           {    -1.1,       -53.5,    1.3,      0.0 },
           {   -45.0,        -2.4,   -0.4,      6.6 },
           {   -11.5,       -61.0,   -0.9,      0.4 },
           {     4.4,       -68.4,   -3.4,      0.0 },
           {     7.7,       -47.1,   -4.7,     -1.0 },
           {   -42.9,       -12.6,   -1.2,      4.2 },
           {   -42.8,        12.7,   -1.2,     -4.2 },
           {    -7.6,       -44.1,    2.1,     -0.5 },
           {   -64.1,         1.7,    0.2,      4.5 },
           {    36.4,       -10.4,    1.0,      3.5 },
           {    35.6,        10.2,    1.0,     -3.5 },
           {    -1.7,        39.5,    2.0,      0.0 },
           {    50.9,        -8.2,   -0.8,     -5.0 },
           {     0.0,        52.3,    1.2,      0.0 },
           {   -42.9,       -17.8,    0.4,      0.0 },
           {     2.6,        34.3,    0.8,      0.0 },
           {    -0.8,       -48.6,    2.4,     -0.1 },
           {    -4.9,        30.5,    3.7,      0.7 },
           {     0.0,       -43.6,    2.1,      0.0 },
           {     0.0,       -25.4,    1.2,      0.0 },
           {     2.0,        40.9,   -2.0,      0.0 },
           {    -2.1,        26.1,    0.6,      0.0 },
           {    22.6,        -3.2,   -0.5,     -0.5 },
           {    -7.6,        24.9,   -0.4,     -0.2 },
           {    -6.2,        34.9,    1.7,      0.3 },
           {     2.0,        17.4,   -0.4,      0.1 },
           {    -3.9,        20.5,    2.4,      0.6 }
        };

/* Nutation series:  obliquity */
        final double eps[][] = {
           { 9205365.8,  -1506.2,   885.7,  -0.2 },
           {  573095.9,   -570.2,  -305.0,  -0.3 },
           {   97845.5,    147.8,   -48.8,  -0.2 },
           {  -89753.6,     28.0,    46.9,   0.0 },
           {    7406.7,   -327.1,   -18.2,   0.8 },
           {   22442.3,    -22.3,   -67.6,   0.0 },
           {    -683.6,     46.8,     0.0,   0.0 },
           {   20070.7,     36.0,     1.6,   0.0 },
           {   12893.8,     39.5,    -6.2,   0.0 },
           {   -9593.2,     14.4,    30.2,  -0.1 },
           {   -6899.5,      4.8,    -0.6,   0.0 },
           {   -5332.5,     -0.1,     2.7,   0.0 },
           {    -125.2,     10.5,     0.0,   0.0 },
           {   -3323.4,     -0.9,    -0.3,   0.0 },
           {    3142.3,      8.9,     0.3,   0.0 },
           {    2552.5,      7.3,    -1.2,   0.0 },
           {    2634.4,      8.8,     0.2,   0.0 },
           {   -2424.4,      1.6,    -0.4,   0.0 },
           {    -123.3,      3.9,     0.0,   0.0 },
           {    1642.4,      7.3,    -0.8,   0.0 },
           {      47.9,      3.2,     0.0,   0.0 },
           {    1321.2,      6.2,    -0.6,   0.0 },
           {   -1234.1,     -0.3,     0.6,   0.0 },
           {   -1076.5,     -0.3,     0.0,   0.0 },
           {     -61.6,      1.8,     0.0,   0.0 },
           {     -55.4,      1.6,     0.0,   0.0 },
           {     856.9,     -4.9,    -2.1,   0.0 },
           {    -800.7,     -0.1,     0.0,   0.0 },
           {     685.1,     -0.6,    -3.8,   0.0 },
           {     -16.9,     -1.5,     0.0,   0.0 },
           {     695.7,      1.8,     0.0,   0.0 },
           {     642.2,     -2.6,    -1.6,   0.0 },
           {      13.3,      1.1,    -0.1,   0.0 },
           {     521.9,      1.6,     0.0,   0.0 },
           {     325.8,      2.0,    -0.1,   0.0 },
           {    -325.1,     -0.5,     0.9,   0.0 },
           {      10.1,      0.3,     0.0,   0.0 },
           {     334.5,      1.6,     0.0,   0.0 },
           {     307.1,      0.4,    -0.9,   0.0 },
           {     327.2,      0.5,     0.0,   0.0 },
           {    -304.6,     -0.1,     0.0,   0.0 },
           {     304.0,      0.6,     0.0,   0.0 },
           {    -276.8,     -0.5,     0.1,   0.0 },
           {     268.9,      1.3,     0.0,   0.0 },
           {     271.8,      1.1,     0.0,   0.0 },
           {     271.5,     -0.4,    -0.8,   0.0 },
           {      -5.2,      0.5,     0.0,   0.0 },
           {    -220.5,      0.1,     0.0,   0.0 },
           {     -20.1,      0.3,     0.0,   0.0 },
           {    -191.0,      0.1,     0.5,   0.0 },
           {      -4.1,      0.3,     0.0,   0.0 },
           {     130.6,     -0.1,     0.0,   0.0 },
           {       3.0,      0.3,     0.0,   0.0 },
           {     122.9,      0.8,     0.0,   0.0 },
           {       3.7,     -0.3,     0.0,   0.0 },
           {     123.1,      0.4,    -0.3,   0.0 },
           {     -52.7,     15.3,     0.0,   0.0 },
           {     120.7,      0.3,    -0.3,   0.0 },
           {       4.0,     -0.3,     0.0,   0.0 },
           {     126.5,      0.5,     0.0,   0.0 },
           {     112.7,      0.5,    -0.3,   0.0 },
           {    -106.1,     -0.3,     0.3,   0.0 },
           {    -112.9,     -0.2,     0.0,   0.0 },
           {       3.6,     -0.2,     0.0,   0.0 },
           {     107.4,      0.3,     0.0,   0.0 },
           {     -10.9,      0.2,     0.0,   0.0 },
           {      -0.9,      0.0,     0.0,   0.0 },
           {      85.4,      0.0,     0.0,   0.0 },
           {       0.0,    -88.8,     0.0,   0.0 },
           {     -71.0,     -0.2,     0.0,   0.0 },
           {     -70.3,      0.0,     0.0,   0.0 },
           {      64.5,      0.4,     0.0,   0.0 },
           {      69.8,      0.0,     0.0,   0.0 },
           {      66.1,      0.4,     0.0,   0.0 },
           {     -61.0,     -0.2,     0.0,   0.0 },
           {     -59.5,     -0.1,     0.0,   0.0 },
           {     -55.6,      0.0,     0.2,   0.0 },
           {      51.7,      0.2,     0.0,   0.0 },
           {     -49.0,     -0.1,     0.0,   0.0 },
           {     -52.7,     -0.1,     0.0,   0.0 },
           {     -49.6,      1.4,     0.0,   0.0 },
           {      46.3,      0.4,     0.0,   0.0 },
           {      49.6,      0.1,     0.0,   0.0 },
           {      -5.1,      0.1,     0.0,   0.0 },
           {     -44.0,     -0.1,     0.0,   0.0 },
           {     -39.9,     -0.1,     0.0,   0.0 },
           {     -39.5,     -0.1,     0.0,   0.0 },
           {      -3.9,      0.1,     0.0,   0.0 },
           {     -42.1,     -0.1,     0.0,   0.0 },
           {     -17.2,      0.1,     0.0,   0.0 },
           {      -2.3,      0.1,     0.0,   0.0 },
           {     -39.2,      0.0,     0.0,   0.0 },
           {     -38.4,      0.1,     0.0,   0.0 },
           {      36.8,      0.2,     0.0,   0.0 },
           {      34.6,      0.1,     0.0,   0.0 },
           {     -32.7,      0.3,     0.0,   0.0 },
           {      30.4,      0.0,     0.0,   0.0 },
           {       0.4,      0.1,     0.0,   0.0 },
           {      29.3,      0.2,     0.0,   0.0 },
           {      31.6,      0.1,     0.0,   0.0 },
           {       0.8,     -0.1,     0.0,   0.0 },
           {     -27.9,      0.0,     0.0,   0.0 },
           {       2.9,      0.0,     0.0,   0.0 },
           {     -25.3,      0.0,     0.0,   0.0 },
           {      25.0,      0.1,     0.0,   0.0 },
           {      27.5,      0.1,     0.0,   0.0 },
           {     -24.4,     -0.1,     0.0,   0.0 },
           {      24.9,      0.2,     0.0,   0.0 },
           {     -22.8,     -0.1,     0.0,   0.0 },
           {       0.9,     -0.1,     0.0,   0.0 },
           {      24.4,      0.1,     0.0,   0.0 },
           {      23.9,      0.1,     0.0,   0.0 },
           {      22.5,      0.1,     0.0,   0.0 },
           {      20.8,      0.1,     0.0,   0.0 },
           {      20.1,      0.0,     0.0,   0.0 },
           {      21.5,      0.1,     0.0,   0.0 },
           {     -20.0,      0.0,     0.0,   0.0 },
           {       1.4,      0.0,     0.0,   0.0 },
           {      -0.2,     -0.1,     0.0,   0.0 },
           {      19.0,      0.0,    -0.1,   0.0 },
           {      20.5,      0.0,     0.0,   0.0 },
           {      -2.0,      0.0,     0.0,   0.0 },
           {     -17.6,     -0.1,     0.0,   0.0 },
           {      19.0,      0.0,     0.0,   0.0 },
           {      -2.4,      0.0,     0.0,   0.0 },
           {     -18.4,     -0.1,     0.0,   0.0 },
           {      17.1,      0.0,     0.0,   0.0 },
           {       0.4,      0.0,     0.0,   0.0 },
           {      18.4,      0.1,     0.0,   0.0 },
           {       0.0,     17.4,     0.0,   0.0 },
           {      -0.6,      0.0,     0.0,   0.0 },
           {     -15.4,      0.0,     0.0,   0.0 },
           {     -16.8,     -0.1,     0.0,   0.0 },
           {      16.3,      0.0,     0.0,   0.0 },
           {      -2.0,      0.0,     0.0,   0.0 },
           {      -1.5,      0.0,     0.0,   0.0 },
           {     -14.3,     -0.1,     0.0,   0.0 },
           {      14.4,      0.0,     0.0,   0.0 },
           {     -13.4,      0.0,     0.0,   0.0 },
           {     -14.3,     -0.1,     0.0,   0.0 },
           {     -13.7,      0.0,     0.0,   0.0 },
           {      13.1,      0.1,     0.0,   0.0 },
           {      -1.7,      0.0,     0.0,   0.0 },
           {     -12.8,      0.0,     0.0,   0.0 },
           {       0.0,    -14.4,     0.0,   0.0 },
           {      12.4,      0.0,     0.0,   0.0 },
           {     -12.0,      0.0,     0.0,   0.0 },
           {      -0.8,      0.0,     0.0,   0.0 },
           {      10.9,      0.1,     0.0,   0.0 },
           {     -10.8,      0.0,     0.0,   0.0 },
           {      10.5,      0.0,     0.0,   0.0 },
           {     -10.4,      0.0,     0.0,   0.0 },
           {     -11.2,      0.0,     0.0,   0.0 },
           {      10.5,      0.1,     0.0,   0.0 },
           {      -1.4,      0.0,     0.0,   0.0 },
           {       0.0,      0.1,     0.0,   0.0 },
           {       0.7,      0.0,     0.0,   0.0 },
           {     -10.3,      0.0,     0.0,   0.0 },
           {     -10.0,      0.0,     0.0,   0.0 },
           {       9.6,      0.0,     0.0,   0.0 },
           {       9.4,      0.1,     0.0,   0.0 },
           {       0.6,      0.0,     0.0,   0.0 },
           {     -87.7,      4.4,    -0.4,  -6.3 },
           {      46.3,     22.4,     0.5,  -2.4 },
           {      15.6,     -3.4,     0.1,   0.4 },
           {       5.2,      5.8,     0.2,  -0.1 },
           {     -30.1,     26.9,     0.7,   0.0 },
           {      23.2,     -0.5,     0.0,   0.6 },
           {       1.0,     23.2,     3.4,   0.0 },
           {     -12.2,     -4.3,     0.0,   0.0 },
           {      -2.1,     -3.7,    -0.2,   0.1 },
           {     -18.6,     -3.8,    -0.4,   1.8 },
           {       5.5,    -18.7,    -1.8,  -0.5 },
           {      -5.5,    -18.7,     1.8,  -0.5 },
           {      18.4,     -3.6,     0.3,   0.9 },
           {      -0.6,      1.3,     0.0,   0.0 },
           {      -5.6,    -19.5,     1.9,   0.0 },
           {       5.5,    -19.1,    -1.9,   0.0 },
           {     -17.3,     -0.8,     0.0,   0.9 },
           {      -3.2,     -8.3,    -0.8,   0.3 },
           {      -0.1,      0.0,     0.0,   0.0 },
           {      -5.4,      7.8,    -0.3,   0.0 },
           {     -14.8,      1.4,     0.0,   0.3 },
           {      -3.8,      0.4,     0.0,  -0.2 },
           {      12.6,      3.2,     0.5,  -1.5 },
           {       0.1,      0.0,     0.0,   0.0 },
           {     -13.6,      2.4,    -0.1,   0.0 },
           {       0.9,      1.2,     0.0,   0.0 },
           {     -11.9,     -0.5,     0.0,   0.3 },
           {       0.4,     12.0,     0.3,  -0.2 },
           {       8.3,      6.1,    -0.1,   0.1 },
           {       0.0,      0.0,     0.0,   0.0 },
           {       0.4,    -10.8,     0.3,   0.0 },
           {       9.6,      2.2,     0.3,  -1.2 }
        };

/* Number of terms in the model */
        final int nterms = na.length;

        int j;
        double t, el, elp, f, d, om, ve, ma, ju, sa, theta, c, s, dp, de;

/* Interval between fundamental epoch J2000.0 and given epoch (JC). */
        t  =  ( date - DJM0 ) / DJC;

/* Mean anomaly of the Moon. */
        el  = 134.96340251 * DD2R +
              Math.IEEEremainder ( t * ( 1717915923.2178 +
                     t * (         31.8792 +
                     t * (          0.051635 +
                     t * (        - 0.00024470 ) ) ) ), TURNAS ) * DAS2R;

/* Mean anomaly of the Sun. */
        elp = 357.52910918 * DD2R +
              Math.IEEEremainder  ( t * (  129596581.0481 +
                     t * (        - 0.5532 +
                     t * (          0.000136 +
                     t * (        - 0.00001149 ) ) ) ), TURNAS ) * DAS2R;

/* Mean argument of the latitude of the Moon. */
        f   =  93.27209062 * DD2R +
              Math.IEEEremainder  ( t * ( 1739527262.8478 +
                     t * (       - 12.7512 +
                     t * (        - 0.001037 +
                     t * (          0.00000417 ) ) ) ), TURNAS ) * DAS2R;

/* Mean elongation of the Moon from the Sun. */
        d   = 297.85019547 * DD2R +
              Math.IEEEremainder  ( t * ( 1602961601.2090 +
                     t * (        - 6.3706 +
                     t * (          0.006539 +
                     t * (        - 0.00003169 ) ) ) ), TURNAS ) * DAS2R;

/* Mean longitude of the ascending node of the Moon. */
        om  = 125.04455501 * DD2R +
              Math.IEEEremainder  ( t * (  - 6962890.5431 +
                     t * (          7.4722 +
                     t * (          0.007702 +
                     t * (        - 0.00005939 ) ) ) ), TURNAS ) * DAS2R;

/* Mean longitude of Venus. */
        ve  = 181.97980085 * DD2R +
              Math.IEEEremainder  ( 210664136.433548 * t, TURNAS ) * DAS2R;

/* Mean longitude of Mars. */
        ma  = 355.43299958 * DD2R +
              Math.IEEEremainder  (  68905077.493988 * t, TURNAS ) * DAS2R;

/* Mean longitude of Jupiter. */
        ju  =  34.35151874 * DD2R +
              Math.IEEEremainder  (  10925660.377991 * t, TURNAS ) * DAS2R;

/* Mean longitude of Saturn. */
        sa  =  50.07744430 * DD2R +
              Math.IEEEremainder  (   4399609.855732 * t, TURNAS ) * DAS2R;

/* Geodesic nutation (Fukushima 1991) in microarcsec. */
        dp = - 153.1 * Math.sin ( elp ) - 1.9 * Math.sin( 2.0 * elp );
        de = 0.0;

/* Shirai & Fukushima (2001) nutation series. */
        for ( j = nterms - 1; j >= 0; j-- ) {
           theta = ( (double) na[j][0] ) * el +
                   ( (double) na[j][1] ) * elp +
                   ( (double) na[j][2] ) * f +
                   ( (double) na[j][3] ) * d +
                   ( (double) na[j][4] ) * om +
                   ( (double) na[j][5] ) * ve +
                   ( (double) na[j][6] ) * ma +
                   ( (double) na[j][7] ) * ju +
                   ( (double) na[j][8] ) * sa;
           c = Math.cos ( theta );
           s = Math.sin ( theta );
           dp += ( psi[j][0] + psi[j][2] * t ) * c +
                   ( psi[j][1] + psi[j][3] * t ) * s;
           de += ( eps[j][0] + eps[j][2] * t ) * c +
                   ( eps[j][1] + eps[j][3] * t ) * s;
        }

/* Change of units, and addition of the precession correction. */
        double r[] = new double[3];
        r[0] = ( dp * 1e-6 - 0.042888 - 0.29856 * t ) * DAS2R;
        r[1] = ( de * 1e-6 - 0.005171 - 0.02408 * t ) * DAS2R;

/* Mean obliquity of date (Simon et al. 1994). */
        r[2]  =  ( 84381.412 +
                   ( - 46.80927 +
                    ( - 0.000152 +
                      ( 0.0019989 +
                    ( - 0.00000051 +
                    ( - 0.000000025 ) * t ) * t ) * t ) * t ) * t ) * DAS2R;
        return r;

    }
/*
 *  Old code
 */
    private double[] Nutc_old ( double date )
    {
        double t, el, el2, el3, elp, elp2,
               f, f2, f4,
               d, d2, d4,
               om, om2,
               dp, de, a;
        double result[] = new double[3];

/* Interval between basic epoch J2000.0 and current epoch (JC) */
        t = ( date - 51544.5 ) / 36525.0;

/* Fundamental arguments in the FK5 reference system */

/* Mean longitude of the Moon minus mean longitude of the Moon's perigee */
        el = Drange ( DAS2R * dmod( 
                               485866.733 + ( 1325.0 * T2AS + 715922.633
                               + ( 31.310 + 0.064 * t ) * t ) * t , T2AS ) );

/* Mean longitude of the Sun minus mean longitude of the Sun's perigee */
        elp = Drange ( DAS2R * dmod(
                                1287099.804 + ( 99.0 * T2AS + 1292581.224
                                + ( -0.577 - 0.012 * t ) * t ) * t, T2AS ) );

/* Mean longitude of the Moon minus mean longitude of the Moon's node */
        f = Drange ( DAS2R * dmod(
                              335778.877 + ( 1342.0 * T2AS + 295263.137
                              + ( -13.257 + 0.011 * t ) * t ) * t, T2AS ) );

/* Mean elongation of the Moon from the Sun */
        d = Drange ( DAS2R * dmod(
                              1072261.307 + ( 1236.0 * T2AS + 1105601.328
                              + ( -6.891 + 0.019 * t ) * t ) * t, T2AS ) );

/* Longitude of the mean ascending node of the lunar orbit on the
   ecliptic, measured from the mean equinox of date */
        om = Drange ( DAS2R * dmod(
                               450160.280 + ( -5.0 * T2AS - 482890.539
                               + ( 7.455 + 0.008 * t ) * t ) * t, T2AS ) );

/* Multiples of arguments */
        el2 = el + el;
        el3 = el2 + el;
        elp2 = elp + elp;
        f2 = f + f;
        f4 = f2 + f2;
        d2 = d + d;
        d4 = d2 + d2;
        om2 = om + om;

/* Series for the nutation */
        dp = 0.0;
        de = 0.0;

        dp += Math.sin ( elp + d );                          /* 106  */

        dp -= Math.sin ( f2 + d4 + om2 );                    /* 105  */

        dp += Math.sin ( el2 + d2 );                         /* 104  */

        dp -= Math.sin ( el - f2 + d2 );                     /* 103  */

        dp -= Math.sin ( el + elp - d2 + om );               /* 102  */

        dp -= Math.sin ( - elp + f2 + om );                  /* 101  */

        dp -= Math.sin ( el - f2 - d2 );                     /* 100  */

        dp -= Math.sin ( elp + d2 );                         /*  99  */

        dp -= Math.sin ( f2 - d + om2 );                     /*  98  */

        dp -= Math.sin ( - f2 + om );                        /*  97  */

        dp += Math.sin ( - el - elp + d2 + om );             /*  96  */

        dp += Math.sin ( elp + f2 + om );                    /*  95  */

        dp -= Math.sin ( el + f2 - d2 );                     /*  94  */

        dp += Math.sin ( el3 + f2 - d2 + om2 );              /*  93  */

        dp += Math.sin ( f4 - d2 + om2 );                    /*  92  */

        dp -= Math.sin ( el + d2 + om );                     /*  91  */

        dp -= Math.sin ( el2 + f2 + d2 + om2 );              /*  90  */

        a = el2 + f2 - d2 + om;                         /*  89  */
        dp += Math.sin ( a );
        de -= Math.cos ( a );

        dp += Math.sin ( el - elp - d2 );                    /*  88  */

        dp += Math.sin ( - el + f4 + om2 );                  /*  87  */

        a = - el2 + f2 + d4 + om2;                      /*  86  */
        dp -= Math.sin ( a );
        de += Math.cos ( a );

        a  = el + f2 + d2 + om;                         /*  85  */
        dp -= Math.sin ( a );
        de += Math.cos ( a );

        a = el + elp + f2 - d2 + om2;                   /*  84  */
        dp += Math.sin ( a );
        de -= Math.cos ( a );

        dp -= Math.sin ( el2 - d4 );                         /*  83  */

        a = - el + f2 + d4 + om2;                       /*  82  */
        dp -= 2.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = - el2 + f2 + d2 + om2;                      /*  81  */
        dp += Math.sin ( a );
        de = de - Math.cos ( a );

        dp -= Math.sin ( el - d4 );                          /*  80  */

        a = - el + om2;                                 /*  79  */
        dp += Math.sin ( a );
        de = de - Math.cos ( a );

        a = f2 + d + om2;                               /*  78  */
        dp += 2.0 * Math.sin ( a );
        de = de - Math.cos ( a );

        dp += 2.0 * Math.sin ( el3 );                        /*  77  */

        a = el + om2;                                   /*  76  */
        dp -= 2.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = el2 + om;                                   /*  75  */
        dp += 2.0 * Math.sin ( a );
        de -= Math.cos ( a );

        a = - el + f2 - d2 + om;                        /*  74  */
        dp -= 2.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = el + elp + f2 + om2;                        /*  73  */
        dp += 2.0 * Math.sin ( a );
        de = de - Math.cos ( a );

        a = - elp + f2 + d2 + om2;                      /*  72  */
        dp -= 3.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = el3 + f2 + om2;                             /*  71  */
        dp -= 3.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = - el2 + om;                                 /*  70  */
        dp -= 2.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = - el - elp + f2 + d2 + om2;                 /*  69  */
        dp -= 3.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = el - elp + f2 + om2;                        /*  68  */
        dp -= 3.0 * Math.sin ( a );
        de += Math.cos ( a );

         dp += 3.0 * Math.sin ( el + f2 );                    /*  67  */

        dp -= 3.0 * Math.sin ( el + elp );                   /*  66  */

        dp -= 4.0 * Math.sin ( d );                          /*  65  */

        dp += 4.0 * Math.sin ( el - f2 );                    /*  64  */

        dp -= 4.0 * Math.sin ( elp - d2 );                   /*  63  */

        a = el2 + f2 + om;                              /*  62  */
        dp -= 5.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        dp += 5.0 * Math.sin ( el - elp );                   /*  61  */

        a = - d2 + om;                                  /*  60  */
        dp -= 5.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = el + f2 - d2 + om;                          /*  59  */
        dp += 6.0 * Math.sin ( a );
        de -= 3.0 * Math.cos ( a );

        a = f2 + d2 + om;                               /*  58  */
        dp -= 7.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = d2 + om;                                    /*  57  */
        dp -= 6.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = el2 + f2 - d2 + om2;                        /*  56  */
        dp += 6.0 * Math.sin ( a );
        de -= 3.0 * Math.cos ( a );

        dp += 6.0 * Math.sin ( el + d2);                     /*  55  */

        a = el + f2 + d2 + om2;                         /*  54  */
        dp -= 8.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = - elp + f2 + om2;                           /*  53  */
        dp -= 7.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = elp + f2 + om2;                             /*  52  */
        dp += 7.0 * Math.sin ( a );
        de -= 3.0 * Math.cos ( a );

        dp -= 7.0 * Math.sin ( el + elp - d2 );              /*  51  */

        a = - el + f2 + d2 + om;                        /*  50  */
        dp -= 10.0 * Math.sin ( a );
        de += 5.0 * Math.cos ( a );

        a = el - d2 + om;                               /*  49  */
        dp -= 13.0 * Math.sin ( a );
        de += 7.0 * Math.cos ( a );

        a = - el + d2 + om;                             /*  48  */
        dp += 16.0 * Math.sin ( a );
        de -= 8.0 * Math.cos ( a );

        a = - el + f2 + om;                             /*  47  */
        dp += 21.0 * Math.sin ( a );
        de -= 10.0 * Math.cos ( a );

        dp += 26.0 * Math.sin ( f2 );                        /*  46  */
        de -= Math.cos( f2 );

        a = el2 + f2 + om2;                             /*  45  */
        dp -= 31.0 * Math.sin ( a );
        de += 13.0 * Math.cos ( a );

        a = el + f2 - d2 + om2;                         /*  44  */
        dp += 29.0 * Math.sin ( a );
        de -= 12.0 * Math.cos ( a );

        dp += 29.0 * Math.sin ( el2 );                       /*  43  */
        de -= Math.cos( el2 );

        a = f2 + d2 + om2;                              /*  42  */
        dp -= 38.0 * Math.sin ( a );
        de += 16.0 * Math.cos ( a );

        a = el + f2 + om;                               /*  41  */
        dp -= 51.0 * Math.sin ( a );
        de += 27.0 * Math.cos ( a );

        a = - el + f2 + d2 + om2;                       /*  40  */
        dp -= 59.0 * Math.sin ( a );
        de += 26.0 * Math.cos ( a );

        a = - el + om;                                  /*  39  */
        dp += ( - 58.0 -  0.1 * t ) * Math.sin ( a );
        de += 32.0 * Math.cos ( a );

        a = el + om;                                    /*  38  */
        dp += ( 63.0 + 0.1 * t ) * Math.sin ( a );
        de -= 33.0 * Math.cos ( a );

        dp += 63.0 * Math.sin ( d2 );                        /*  37  */
        de -= 2.0 * Math.cos( d2 );

        a = - el + f2 + om2;                            /*  36  */
        dp += 123.0 * Math.sin ( a );
        de -= 53.0 * Math.cos ( a );

        a = el - d2;                                    /*  35  */
        dp -= 158.0 * Math.sin ( a );
        de -= Math.cos ( a );

        a = el + f2 + om2;                              /*  34  */
        dp -= 301.0 * Math.sin ( a );
        de += ( 129.0 - 0.1 * t ) * Math.cos ( a );

        a = f2 + om;                                    /*  33  */
        dp += ( - 386.0 - 0.4 * t ) * Math.sin ( a );
        de += 200.0 * Math.cos ( a );

        dp += ( 712.0 + 0.1 * t ) * Math.sin ( el );         /*  32  */
        de -= 7.0 * Math.cos( el );

        a = f2 + om2;                                   /*  31  */
        dp += ( -2274.0 - 0.2 * t ) * Math.sin ( a );
        de += ( 977.0 - 0.5 * t ) * Math.cos ( a );

        dp -= Math.sin ( elp + f2 - d2 );                    /*  30  */

        dp += Math.sin ( - el + d + om );                    /*  29  */

        dp += Math.sin ( elp + om2 );                        /*  28  */

        dp -= Math.sin ( elp - f2 + d2 );                    /*  27  */

        dp += Math.sin ( - f2 + d2 + om );                   /*  26  */

        dp += Math.sin ( el2 + elp - d2 );                   /*  25  */

        dp -= 4.0 * Math.sin ( el - d );                     /*  24  */

        a = elp + f2 - d2 + om;                         /*  23  */
        dp += 4.0 * Math.sin ( a );
        de -= 2.0 * Math.cos ( a );

        a = el2 - d2 + om;                              /*  22  */
        dp += 4.0 * Math.sin ( a );
        de -= 2.0 * Math.cos ( a );

        a = - elp + f2 - d2 + om;                       /*  21  */
        dp -= 5.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = - el2 + d2 + om;                            /*  20  */
        dp -= 6.0 * Math.sin ( a );
        de += 3.0 * Math.cos ( a );

        a = - elp + om;                                 /*  19  */
        dp -= 12.0 * Math.sin ( a );
        de += 6.0 * Math.cos ( a );

        a = elp2 + f2 - d2 + om2;                       /*  18  */
        dp += ( - 16.0 + 0.1 * t) * Math.sin ( a );
        de += 7.0 * Math.cos ( a );

        a = elp + om;                                   /*  17  */
        dp -= 15.0 * Math.sin ( a );
        de += 9.0 * Math.cos ( a );

        dp += ( 17.0 - 0.1 * t ) * Math.sin ( elp2 );        /*  16  */

        dp -= 22.0 * Math.sin ( f2 - d2 );                   /*  15  */

        a = el2 - d2;                                   /*  14  */
        dp += 48.0 * Math.sin ( a );
        de += Math.cos ( a );

        a = f2 - d2 + om;                               /*  13  */
        dp += ( 129.0 + 0.1 * t ) * Math.sin ( a );
        de -= 70.0 * Math.cos ( a );

        a = - elp + f2 - d2 + om2;                      /*  12  */
        dp += ( 217.0 - 0.5 * t ) * Math.sin ( a );
        de += ( -95.0 + 0.3 * t ) * Math.cos ( a );

        a = elp + f2 - d2 + om2;                        /*  11  */
        dp += ( - 517.0 + 1.2 * t ) * Math.sin ( a );
        de += ( 224.0 - 0.6 * t ) * Math.cos ( a );

        dp += ( 1426.0 - 3.4 * t ) * Math.sin ( elp );       /*  10  */
        de += ( 54.0 - 0.1 * t) * Math.cos ( elp );

        a = f2 - d2 + om2;                              /*   9  */
        dp += ( - 13187.0 - 1.6 * t ) * Math.sin ( a );
        de += ( 5736.0 - 3.1 * t ) * Math.cos ( a );

        dp += Math.sin ( el2 - f2 + om );                    /*   8  */

        a = - elp2 + f2 - d2 + om;                      /*   7  */
        dp -= 2.0 * Math.sin ( a );
        de +=       Math.cos ( a );

        dp -= 3.0 * Math.sin ( el - elp - d );               /*   6  */

        a = - el2 + f2 + om2;                           /*   5  */
        dp -= 3.0 * Math.sin ( a );
        de +=       Math.cos ( a );

        dp += 11.0 * Math.sin ( el2 - f2 );                  /*   4  */

        a = - el2 + f2 + om;                            /*   3  */
        dp += 46.0 * Math.sin ( a );
        de -= 24.0 * Math.cos ( a );

        dp += ( 2062.0 + 0.2 * t ) * Math.sin ( om2 );       /*   2  */
        de += ( - 895.0 + 0.5 * t ) * Math.cos ( om2 );

        dp += ( - 171996.0 - 174.2 * t) * Math.sin ( om );   /*   1  */
        de += ( 92025.0 + 8.9 * t ) * Math.cos ( om );

/* Convert results to radians */
        result[0] = dp * U2R;
        result[1] = de * U2R;

/* Mean obliquity */
        result[2] = DAS2R * ( 84381.448 +
                   ( - 46.8150 +
                   ( - 0.00059 + 0.001813 * t ) * t ) * t );
        return result;
    }

/**
 * Parameters of selected groundbased observing stations.
 *
 *  @param n Number specifying observing station
 *  @return Name of specified observing station,
 *          longitude (radians, West +ve),
 *          geodetic latitude (radians, North +ve),
 *          height above sea level (metres)
 */

/*  Latest Revision: 13 November 2002 (RTP)
 */
    public Observatory Obs ( int n )
    {
        return new Observatory( n );
    }

/**
 * Parameters of selected groundbased observing stations.
 *
 *  @param id Identifier specifying observing station
 *  @return Name of specified observing station,
 *          longitude (radians, West +ve),
 *          geodetic latitude (radians, North +ve),
 *          height above sea level (metres)
 */

/*  Latest Revision: 13 November 2002 (RTP)
 */
    public Observatory Obs ( String id )
    {
        return new Observatory( id );
    }

/**
 *  Apply corrections for proper motion to a star RA,Dec.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The proper motions in RA are dRA/dt rather than cos(Dec)*dRA/dt,
 *      and are in the same coordinate system as R0,D0.</li>
 *
 *  <li>If the available proper motions are pre-FK5 they will be per
 *      tropical year rather than per Julian year, and so the epochs
 *      must both be Besselian rather than Julian.  In such cases, a
 *      scaling factor of 365.2422D0/365.25D0 should be applied to the
 *      radial velocity before use.</li>
 *  </ol> </dd> </dl>
 *
 *  @param r0 RA,Dec at epoch ep0 (rad)
 *  @param pm proper motions:  RA,Dec changes per year of epoch
 *  @param px parallax (arcsec)
 *  @param rv radial velocity (km/sec, +ve if receding)
 *  @param ep0 start epoch in years (e.g Julian epoch)
 *  @param ep1 end epoch in years (same system as ep0)
 *  @return RA,Dec at epoch ep1 (rad)
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  References:
 *     1984 Astronomical Almanac, pp B39-B41.
 *     (also Lederle &amp; Schwan, Astron. Astrophys. 134, 1-6, 1984)
 *
 *  Given:
 *     r0,d0    double     RA,Dec at epoch ep0 (rad)
 *     pr,pd    double     proper motions:  RA,Dec changes per year of epoch
 *     px       double     parallax (arcsec)
 *     rv       double     radial velocity (km/sec, +ve if receding)
 *     ep0      double     start epoch in years (e.g Julian epoch)
 *     ep1      double     end epoch in years (same system as ep0)
 *
 *  Returned:
 *     *r1,*d1  double     RA,Dec at epoch ep1 (rad)
 *
 *  Called:  Dcs2c, Dcc2s, Dranrm
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Pm ( AngleDR r0, double pm[],
                 double px, double rv, double ep0, double ep1 )
    {
/* Km/s to AU/year multiplied by arc seconds to radians */
       final double vfr = ( 365.25 * 86400.0 / 149597870.0 ) * DAS2R;

       int i;
       double w, em[] = new double[3], t, p[] = new double[3];

/* Spherical to Cartesian */
       p = Dcs2c ( r0 );
       double r = r0.getAlpha();
       double d = r0.getDelta();

/* Space motion (radians per year) */
       w = vfr * rv * px;
       em[0] = - pm[0] * p[1] - pm[1] * Math.cos ( r ) * Math.sin ( d ) + w * p[0];
       em[1] =   pm[0] * p[0] - pm[1] * Math.sin ( r ) * Math.sin ( d ) + w * p[1];
       em[2] =               pm[1] * Math.cos ( d )              + w * p[2];

/* Apply the motion */
       t = ep1 - ep0;
       for ( i = 0; i < 3; i++ )
          p[i] = p[i] + (t * em[i]);

/* Cartesian to spherical */
       AngleDR r1 = Dcc2s ( p );
       r1.setAlpha( Dranrm ( r1.getAlpha() ) );
       return r1;
    }

/**
 *  Generate the matrix of precession between two epochs,
 *  using the old, pre-IAU1976, Bessel-Newcomb model, using
 *  Kinoshita's formulation (double precision).
 *  <p>
 *  The matrix is in the sense   v(bep1)  =  rmatp * v(bep0)
 *  </p> <dl>
 *  <dt>Reference:</dt>
 *  <dd>Kinoshita, H. (1975) 'Formulas for precession', SAO Special
 *      Report No. 364, Smithsonian Institution Astrophysical
 *      Observatory, Cambridge, Massachusetts.</dd>
 *  </dl>
 *
 *  @param bep0 Beginning Besselian epoch
 *  @param bep1 Ending Besselian epoch
 *  @return Precession matrix
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     BEP0    double        beginning Besselian epoch
 *     BEP1    double        ending Besselian epoch
 *
 *  Returned:
 *     RMATP   double[3][3]  precession matrix
 *
 *  Called:  Deuler
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Prebn ( double bep0, double bep1 )
    {
        double bigt, t, tas2r, w, zeta, z, theta;

/* Interval between basic epoch B1850.0 and beginning epoch in TC */
        bigt  = ( bep0 - 1850.0 ) / 100.0;

/* Interval over which precession required, in tropical centuries */
        t = ( bep1 - bep0 ) / 100.0;

/* Euler angles */
        tas2r = t * DAS2R;
        w = 2303.5548 + ( 1.39720 + 0.000059 * bigt ) * bigt;
        zeta = (w + ( 0.30242 - 0.000269 * bigt + 0.017996 * t ) * t ) * tas2r;
        z = (w + ( 1.09478 + 0.000387 * bigt + 0.018324 * t ) * t ) * tas2r;
        theta = ( 2005.1125 + ( - 0.85294 - 0.000365* bigt ) * bigt +
                ( - 0.42647 - 0.000365 * bigt - 0.041802 * t ) * t ) * tas2r;

/* Rotation matrix */
        return Deuler ( "ZYZ", -zeta, theta, -z );
    }

/**
 *  Form the matrix of precession between two epochs (IAU 1976, FK5).
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The epochs are TDB (loosely ET) Julian epochs.</li>
 *
 *  <li>The matrix is in the sense   v(ep1)  =  rmatp * v(ep0).</li>
 *
 *  <li>Though the matrix method itself is rigorous, the precession
 *      angles are expressed through canonical polynomials which are
 *      valid only for a limited time span.  There are also known
 *      errors in the IAU precession rate.  The absolute accuracy
 *      of the present formulation is better than 0.1 arcsec from
 *      1960AD to 2040AD, better than 1 arcsec from 1640AD to 2360AD,
 *      and remains below 3 arcsec for the whole of the period
 *      500BC to 3000AD.  The errors exceed 10 arcsec outside the
 *      range 1200BC to 3900AD, exceed 100 arcsec outside 4200BC to
 *      5600AD and exceed 1000 arcsec outside 6800BC to 8200AD.
 *      The LIB routine Precl implements a more elaborate
 *      model which is suitable for problems spanning several
 *      thousand years.</li>
 *  </ol> </dd>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *   <li>  Lieske,J.H., 1979. Astron. Astrophys.,73,282.
 *          equations (6) &amp; (7), p283.</li>
 *   <li>  Kaplan,G.H., 1981. USNO circular no. 163, pa2.</li>
 *  </ol> </dd> </dl>
 *
 *  @param ep0 Beginning epoch
 *  @param ep1 Ending epoch
 *  @return Precession matrix
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     ep0    double         beginning epoch
 *     ep1    double         ending epoch
 *
 *  Returned:
 *     rmatp  double[3][3]   precession matrix
 *
 *  Called:  Deuler
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Prec ( double ep0, double ep1 )
    {
        double t0, t, tas2r, w, zeta, z, theta;

/* Interval between basic epoch J2000.0 and beginning epoch (JC) */
        t0 = ( ep0 - 2000.0 ) / 100.0;

/* Interval over which precession required (JC) */
        t =  ( ep1 - ep0 ) / 100.0;

/* Euler angles */
        tas2r = t * DAS2R;
        w = 2306.2181 + ( ( 1.39656 - ( 0.000139 * t0 ) ) * t0 );
        zeta = (w + ( ( 0.30188 - 0.000344 * t0 ) + 0.017998 * t ) * t ) * tas2r;
        z = (w + ( ( 1.09468 + 0.000066 * t0 ) + 0.018203 * t ) * t ) * tas2r;
        theta = ( ( 2004.3109 + ( - 0.85330 - 0.000217 * t0 ) * t0 )
          + ( ( -0.42665 - 0.000217 * t0 ) - 0.041833 * t ) * t ) * tas2r;

/* Rotation matrix */   
        return Deuler ( "ZYZ", -zeta, theta, -z );
    }

/**
 *  Precession - either FK4 (Bessel-Newcomb, pre-IAU1976) or
 *  FK5 (Fricke, post-IAU1976) as required.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The epochs are Besselian if sys='FK4' and Julian if 'FK5'.
 *      For example, to precess coordinates in the old system from
 *      equinox 1900.0 to 1950.0 the call would be:
 *          Preces ( "FK4", 1900.0, 1950.0, &amp;ra, &amp;dc )</li>
 *
 *  <li>This routine will not correctly convert between the old and
 *      the new systems - for example conversion from B1950 to J2000.
 *      For these purposes see Fk425, Fk524, Fk45z and
 *      Fk54z.</li>
 *
 *  <li>If an invalid sys is supplied, values of -99.0,-99.0 will
 *      be returned for both ra and dc.</li>
 *  </ol> </dd> </dl>
 *
 *  @param sys Precession to be applied: "FK4" or "FK5"
 *  @param ep0 Starting epoch
 *  @param ep1 Ending epoch
 *  @param d RA,Dec, mean equator &amp; equinox of epoch ep0
 *  @return RA,Dec, mean equator &amp; equinox of epoch ep1
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     sys        char[]     precession to be applied: "FK4" or "FK5"
 *     ep0,ep1    double     starting and ending epoch
 *     ra,dc      double     RA,Dec, mean equator &amp; equinox of epoch ep0
 *
 *  Returned:
 *     *ra,*dc    double     RA,Dec, mean equator &amp; equinox of epoch ep1
 *
 *  Called:    Dranrm, Prebn, Prec, Dcs2c,
 *             Dmxv, Dcc2s
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Preces ( String sys, double ep0, double ep1, AngleDR d )
    {
        double pm[][], v1[], v2[];

/* Validate sys */
        boolean fk4 = sys.toUpperCase().equals("FK4");
        boolean fk5 = sys.toUpperCase().equals("FK5");
        if ( fk4 || fk5 ) {

/* Generate appropriate precession matrix */
            if ( fk4 )
                pm = Prebn ( ep0, ep1 );
            else
                pm = Prec ( ep0, ep1 );

/* Convert RA,Dec to x,y,z */
            v1 = Dcs2c ( d );

/* Precess */
            v2 = Dmxv ( pm, v1 );

/* Back to RA,Dec */
            d = Dcc2s ( v2 );
            d.setAlpha( Dranrm ( d.getAlpha() ) );
        } else {
            d = new AngleDR( -99.0, -99.0 );
        }
        return d;
    }

/**
 *  Form the matrix of precession between two epochs, using the
 *  model of Simon et al (1994), which is suitable for long
 *  periods of time.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The epochs are TDB (loosely ET) Julian epochs.</li>
 *
 *  <li>The matrix is in the sense   v(ep1)  =  rmatp * v(ep0).</li>
 *
 *  <li>The absolute accuracy of the model is limited by the
 *      uncertainty in the general precession, about 0.3 arcsec per
 *      1000 years.  The remainder of the formulation provides a
 *      precision of 1 mas over the interval from 1000AD to 3000AD,
 *      0.1 arcsec from 1000BC to 5000AD and 1 arcsec from
 *      4000BC to 8000AD.</li>
 *  </ol> </dd>
 *  <dt>Reference:</dt>
 *  <dd>Simon, J.L., et al., 1994. Astron.Astrophys., 282, 663-683.</dd>
 *  </dl>
 *
 *  @param ep0 Beginning epoch
 *  @param ep1 Ending epoch
 *  @return Precession matrix
 */
 
/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     ep0    double         beginning epoch
 *     ep1    double         ending epoch
 *
 *  Returned:
 *     rmatp  double[3][3]   precession matrix
 *
 *  Called:  Deuler
 *
 *  Defined in mac.h:  DAS2R
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Precl ( double ep0, double ep1 )
    {
        double t0, t, tas2r, w, zeta, z, theta;

/* Interval between basic epoch J2000.0 and beginning epoch (1000JY) */
        t0 = ( ep0 - 2000.0 ) / 1000.0;

/* Interval over which precession required (1000JY) */
        t =  ( ep1 - ep0 ) / 1000.0;

/* Euler angles */
        tas2r = t * DAS2R;
        w =       23060.9097 +
             ( 139.7459 +
             ( - 0.0038 +
             ( - 0.5918 +
             ( - 0.0037 +
                 0.0007 * t0 ) * t0 ) * t0 ) * t0 ) * t0;

        zeta =           ( w +
              ( 30.2226 +
             ( - 0.2523 +
             ( - 0.3840 +
             ( - 0.0014 +
                 0.0007 * t0 ) * t0 ) * t0 ) * t0 +
              ( 18.0183 +
             ( - 0.1326 +
               ( 0.0006 +
                 0.0005 * t0 ) * t0 ) * t0 +
             ( - 0.0583 +
             ( - 0.0001 +
                 0.0007 * t0 ) * t0 +
             ( - 0.0285 +
             ( - 0.0002 ) * t ) * t ) * t ) * t ) * t ) * tas2r;

        z =              ( w +
             ( 109.5270 +
               ( 0.2446 +
             ( - 1.3913 +
             ( - 0.0134 +
                 0.0026 * t0 ) * t0 ) * t0 ) * t0 +
              ( 18.2667 +
             ( - 1.1400 +
             ( - 0.0173 +
                 0.0044 * t0 ) * t0 ) * t0 +
             ( - 0.2821 +
             ( - 0.0093 +
                 0.0032 * t0 ) * t0 +
              ( -0.0301 +
                 0.0006 * t0
               - 0.0001 * t ) * t ) * t ) * t ) * t ) * tas2r;

        theta = ( 20042.0207 +
            ( - 85.3131 +
             ( - 0.2111 +
               ( 0.3642 +
               ( 0.0008 +
             ( - 0.0005 ) * t0 ) * t0 ) * t0 ) * t0 ) * t0 +
            ( - 42.6566 +
             ( - 0.2111 +
               ( 0.5463 +
               ( 0.0017 +
             ( - 0.0012 ) * t0 ) * t0 ) * t0 ) * t0 +
            ( - 41.8238 +
               ( 0.0359 +
               ( 0.0027 +
             ( - 0.0001 ) * t0 ) * t0 ) * t0 +
             ( - 0.0731 +
               ( 0.0019 +
                 0.0009 * t0 ) * t0 +
             ( - 0.0127 +
                 0.0011 * t0 + 0.0004 * t ) * t ) * t ) * t ) * t ) * tas2r;

/* Rotation matrix */
        double rmatp[][] = Deuler ( "ZYZ", -zeta, theta, -z );
        return rmatp;
    }

/**
 *  Form the matrix of precession and nutation (IAU 1976/1980/FK5).
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The epoch and date are TDB (loosely ET).</li>
 *
 *  <li>The matrix is in the sense   v(true)  =  rmatpn * v(mean).</li>
 *  </ol> </dd>
 *  </dl>
 *
 *  @param epoch Julian epoch for mean coordinates
 *  @param date Modified Julian Date (JD-2400000.5) for true coordinates
 *  @return Combined precession/nutation matrix
 */

/*  Latest Revision: 17 November 2001 (RTP)
 *
 *  Given:
 *     epoch   double         Julian epoch for mean coordinates
 *     date    double         Modified Julian Date (JD-2400000.5)
 *                            for true coordinates
 *
 *  Returned:
 *     rmatpn  double[3][3]   combined precession/nutation matrix
 *
 *  Called:  Prec, Epj, Nut, Dmxm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[][] Prenut ( double epoch, double date )
    {
        double rmatp[][], rmatn[][];

/* Precession */
        rmatp = Prec ( epoch, Epj ( date ) );

/* Nutation */
        rmatn = Nut ( date );

/* Combine the matrices:  pn = n x p */
        rmatn = Dmxm ( rmatn, rmatp );
        return rmatn;
    }

/**
 *  Determine constants A and B in atmospheric refraction model
 *  dz = A tan z + B tan^3 z.
 *  <p>
 *  z is the "observed" zenith distance (i.e. affected by
 *  refraction) and dz is what to add to z to give the "topocentric"
 *  (i.e. in vacuo) zenith distance.
 *  </p>
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>Typical values for the tlr and eps arguments might be 0.0065 and
 *     1e-10 respectively.</li>
 *
 *  <li>The radio refraction is chosen by specifying wl &gt; 100
 *      micrometres.</li>
 *
 *  <li> The routine is a slower but more accurate alternative to the
 *     Refcoq routine.  The constants it produces give perfect
 *     agreement with Refro at zenith distances arctan(1) (45 deg)
 *     and arctan(4) (about 76 deg).  It achieves 0.5 arcsec accuracy
 *     for ZD &lt; 80 deg, 0.01 arcsec accuracy for ZD &lt; 60 deg, and
 *     0.001 arcsec accuracy for ZD &lt; 45 deg.</li>
 *  </ol> </dd>
 *  </dl>
 *
 *  @param hm  Height of the observer above sea level (metre)
 *  @param tdk Ambient temperature at the observer (deg k)
 *  @param pmb Pressure at the observer (millibar)
 *  @param rh  Relative humidity at the observer (range 0-1)
 *  @param wl  Effective wavelength of the source (micrometre)
 *  @param phi Latitude of the observer (radian, astronomical)
 *  @param tlr Temperature lapse rate in the troposphere (degk/metre)
 *  @param eps Precision required to terminate iteration (radian)
 *
 *  @return tan z coefficient (radian), tan^3 z coefficient (radian)
 */

/*  Latest Revision: 20 November 2001 (RTP)
 *
 *  Given:
 *    hm    double    height of the observer above sea level (metre)
 *    tdk   double    ambient temperature at the observer (deg k)
 *    pmb   double    pressure at the observer (millibar)
 *    rh    double    relative humidity at the observer (range 0-1)
 *    wl    double    effective wavelength of the source (micrometre)
 *    phi   double    latitude of the observer (radian, astronomical)
 *    tlr   double    temperature lapse rate in the troposphere (degk/metre)
 *    eps   double    precision required to terminate iteration (radian)
 *
 *  Returned:
 *    *refa double    tan z coefficient (radian)
 *    *refb double    tan^3 z coefficient (radian)
 *
 *  Called:  Refro
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double[] Refco ( double hm, double tdk, double pmb, double rh,
                double wl, double phi, double tlr, double eps )
    {
        double r1,r2;
        double ref[] = new double[2];

/* Sample zenith distances: arctan(1) and arctan(4) */
        final double atn1 = 0.7853981633974483;  //Math.arctan(1.0)
        final double atn4 = 1.325817663668033;  //Math.arctan(4.0)

/* Determine refraction for the two sample zenith distances. */
        r1 = Refro ( atn1, hm, tdk, pmb, rh, wl, phi, tlr, eps );
        r2 = Refro ( atn4, hm, tdk, pmb, rh, wl, phi, tlr, eps );

/* Solve for refraction constants. */
        ref[0] = ( 64.0 * r1 - r2 ) / 60.0;
        ref[1] = ( r2 - 4.0 * r1 ) / 60.0;
        return ref;
    }
 
/**
 *  Atmospheric refraction for radio and optical/IR wavelengths.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>A suggested value for the tlr argument is 0.0065.  The
 *     refraction is significantly affected by tlr, and if studies
 *     of the local atmosphere have been carried out a better tlr
 *     value may be available.</li>
 *
 *  <li> A suggested value for the eps argument is 1e-8.  The result is
 *     usually at least two orders of magnitude more computationally
 *     precise than the supplied eps value.</li>
 *
 *  <li> The routine computes the refraction for zenith distances up
 *     to and a little beyond 90 deg using the method of Hohenkerk
 *     and Sinclair (NAO Technical Notes 59 and 63, subsequently adopted
 *     in the Explanatory Supplement, 1992 edition - see section 3.281).</li>
 *
 *  <li>The C code is an adaptation of the Fortran optical/IR refraction
 *     subroutine AREF of C.Hohenkerk (HMNAO, September 1984), with
 *     extensions to support the radio case.  The following modifications
 *     to the original HMNAO optical/IR refraction algorithm have been made:
 *  <ul>
 *     <li>The angle arguments have been changed to radians.</li>
 *
 *     <li>Any value of zobs is allowed (see note 6, below).</li>
 *
 *     <li>Other argument values have been limited to safe values.</li>
 *
 *     <li>Murray's values for the gas constants have been used
 *         (Vectorial Astrometry, Adam Hilger, 1983).</li>
 *
 *     <li>The numerical integration phase has been rearranged for
 *         extra clarity.</li>
 *
 *     <li>A better model for Ps(T) has been adopted (taken from
 *         Gill, Atmosphere-Ocean Dynamics, Academic Press, 1982).</li>
 *
 *     <li>More accurate expressions for Pwo have been adopted
 *         (again from Gill 1982).</li>
 *
 *     <li>Provision for radio wavelengths has been added using
 *         expressions devised by A.T.Sinclair, RGO (private
 *         communication 1989), based on the Essen &amp; Froome
 *         refractivity formula adopted in Resolution 1 of the
 *         13th International Geodesy Association General Assembly
 *         (Bulletin Geodesique 70 p390, 1963).</li>
 *
 *     <li>Various small changes have been made to gain speed.</li>
 *  </ul>
 *     None of the changes significantly affects the optical/IR results
 *     with respect to the algorithm given in the 1992 Explanatory
 *     Supplement.  For example, at 70 deg zenith distance the present
 *     routine agrees with the ES algorithm to better than 0.05 arcsec
 *     for any reasonable combination of parameters.  However, the
 *     improved water-vapour expressions do make a significant difference
 *     in the radio band, at 70 deg zenith distance reaching almost
 *     4 arcsec for a hot, humid, low-altitude site during a period of
 *     low pressure.</li>
 *
 *  <li>The radio refraction is chosen by specifying wl &gt; 100 micrometres.
 *     Because the algorithm takes no account of the ionosphere, the
 *     accuracy deteriorates at low frequencies, below about 30 MHz.</li>
 *
 *  <li>Before use, the value of zobs is expressed in the range +/- pi.
 *     If this ranged zobs is -ve, the result ref is computed from its
 *     absolute value before being made -ve to match.  In addition, if
 *     it has an absolute value greater than 93 deg, a fixed ref value
 *     equal to the result for zobs = 93 deg is returned, appropriately
 *     signed.</li>
 *
 *  <li>As in the original Hohenkerk and Sinclair algorithm, fixed
 *     values of the water vapour polytrope exponent, the height of the
 *     tropopause, and the height at which refraction is negligible are
 *     used.</li>
 *
 *  <li>The radio refraction has been tested against work done by
 *     Iain Coulson, JACH, (private communication 1995) for the
 *     James Clerk Maxwell Telescope, Mauna Kea.  For typical conditions,
 *     agreement at the 0.1 arcsec level is achieved for moderate ZD,
 *     worsening to perhaps 0.5-1.0 arcsec at ZD 80 deg.  At hot and
 *     humid sea-level sites the accuracy will not be as good.</li>
 *
 *  <li>It should be noted that the relative humidity rh is formally
 *     defined in terms of "mixing ratio" rather than pressures or
 *     densities as is often stated.  It is the mass of water per unit
 *     mass of dry air divided by that for saturated air at the same
 *     temperature and pressure (see Gill 1982).</li>
 *  </ol> </dd>
 *  </dl>
 *
 *  @param zobs Observed zenith distance of the source (radian)
 *  @param hm   Height of the observer above sea level (metre)
 *  @param tdk  Ambient temperature at the observer (deg K)
 *  @param pmb  Pressure at the observer (millibar)
 *  @param rh   Relative humidity at the observer (range 0-1)
 *  @param wl   Effective wavelength of the source (micrometre)
 *  @param phi  Latitude of the observer (radian, astronomical)
 *  @param tlr  Tropospheric lapse rate (degK/metre)
 *  @param eps  Precision required to terminate iteration (radian)
 *
 *  @return     Refraction: in vacuo ZD minus observed ZD (radian)
 */

/*  Latest Revision: 12 November 2002 (RTP)
 *
 *  Given:
 *    zobs    double  observed zenith distance of the source (radian)
 *    hm      double  height of the observer above sea level (metre)
 *    tdk     double  ambient temperature at the observer (deg K)
 *    pmb     double  pressure at the observer (millibar)
 *    rh      double  relative humidity at the observer (range 0-1)
 *    wl      double  effective wavelength of the source (micrometre)
 *    phi     double  latitude of the observer (radian, astronomical)
 *    tlr     double  tropospheric lapse rate (degK/metre)
 *    eps     double  precision required to terminate iteration (radian)
 *
 *  Returned:
 *    ref     double  refraction: in vacuo ZD minus observed ZD (radian)
 *
 *  Called:  Drange, atmt, atms
 *
 *  Defined in mac.h:  TRUE, FALSE
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Refro ( double zobs, double hm, double tdk, double pmb,
                double rh, double wl, double phi, double tlr,
                double eps )
    {
/* Fixed parameters */

        final double d93 = 1.623156204; /* 93 degrees in radians        */
        final double gcr = 8314.32;     /* Universal gas constant       */
        final double dmd = 28.9644;     /* Molecular weight of dry air  */
        final double dmw = 18.0152;     /* Molecular weight of water
                                                             vapour */
        final double s = 6378120.0;     /* Mean Earth radius (metre)    */
        final double delta = 18.36;     /* Exponent of temperature
                                         dependence of water vapour
                                                           pressure */
        final double ht = 11000.0;      /* Height of tropopause (metre) */
        final double hs = 80000.0;      /* Upper limit for refractive
                                                    effects (metre) */

/* Variables used when calling the internal routine atmt */
        double robs;   /* height of observer from centre of Earth (metre) */
        double tdkok;  /* temperature at the observer (deg K) */
        double alpha;  /* alpha          |        */
        double gamm2;  /* gamma minus 2  | see ES */
        double delm2;  /* delta minus 2  |        */
        double c1,c2,c3,c4,c5,c6;  /* various */

/* Variables used when calling the internal routine atms */
        double rt;     /* height of tropopause from centre of Earth (metre) */
        double tt;     /* temperature at the tropopause (deg k) */
        double dnt;    /* refractive index at the tropopause */
        double gamal;  /* constant of the atmospheric model = g*md/r */

        int is, k, n, i, j;
        boolean optic;
        double zobs1, zobs2, hmok, pmbok, rhok, wlok, tol, wlsq, gb,
          a, gamma, tdc, psat, pwo, w, tempo, dn0, rdndr0, sk0,
          f0, rdndrt, zt, ft, dnts, rdndrp, zts, fts, rs,
          dns, rdndrs, zs, fs, refold, z0, zrange, fb, ff, fo,
          fe, h, r, sz, rg, dr, tg, dn, rdndr, t, f;
        double ref, refp = 0.0, reft = 0.0;

/* Transform zobs into the normal range. */
        zobs1 = Drange ( zobs );
        zobs2 = Math.abs ( zobs1 );
        zobs2 = gmin ( zobs2, d93 );

/* Keep other arguments within safe bounds. */
        hmok = gmax ( hm, -1000.0 );
        hmok = gmin ( hmok, hs );
        tdkok = gmax ( tdk, 100.0 );
        tdkok = gmin ( tdkok, 500.0 );
        pmbok = gmax ( pmb, 0.0 );
        pmbok = gmin ( pmbok, 10000.0 );
        rhok  = gmax ( rh, 0.0 );
        rhok  = gmin ( rhok, 1.0 );
        wlok  = gmax ( wl, 0.1 );
        alpha = Math.abs ( tlr );
        alpha = gmax ( alpha, 0.001 );
        alpha = gmin ( alpha, 0.01 );

/* Tolerance for iteration. */
        w = Math.abs ( eps );
        w = gmax ( w, 1e-12 );
        tol = gmin ( w, 0.1 ) / 2.0;

/* Decide whether optical/IR or radio case - switch at 100 microns. */
        optic = ( wlok <= 100.0 );

/* Set up model atmosphere parameters defined at the observer. */
        wlsq = wlok * wlok;
        gb = 9.784 * ( 1.0 - 0.0026 * Math.cos ( 2.0 * phi ) - 2.8e-7 * hmok );
        a = ( optic ? ( ( 287.604 + 1.6288 / wlsq + 0.0136 / ( wlsq * wlsq ) )
                        * 273.15 / 1013.25 ) * 1e-6
                    : 77.689e-6 );
        gamal = gb * dmd / gcr;
        gamma = gamal / alpha;
        gamm2 = gamma - 2.0;
        delm2 = delta - 2.0;
        tdc = tdkok - 273.15;
        psat = Math.pow ( 10.0, ( 0.7859 + 0.03477 * tdc ) /
                         ( 1.0 + 0.00412 * tdc ) ) *
                ( 1.0 + pmbok * ( 4.5e-6 + 6e-10 * tdc * tdc ) );
        pwo = ( pmbok > 0.0 ) ?
               rhok * psat / ( 1.0 - ( 1.0 - rhok ) * psat / pmbok ) :
               0.0;
        w = pwo * ( 1.0 - dmw / dmd ) * gamma / ( delta - gamma );
        c1 = a * ( pmbok + w ) / tdkok;
        c2 = ( a * w + ( optic ? 11.2684e-6 : 6.3938e-6 ) * pwo ) / tdkok;
        c3 = ( gamma - 1.0 ) * alpha * c1 / tdkok;
        c4 = ( delta - 1.0 ) * alpha * c2 / tdkok;
        c5 = optic ? 0.0 : 375463e-6 * pwo / tdkok;
        c6 = c5 * delm2 * alpha / ( tdkok * tdkok );

/* Conditions at the observer. */
        robs = s + hmok;
        double a3[] = atmt ( robs, tdkok, alpha, gamm2, delm2,
                            c1, c2, c3, c4, c5, c6, robs );
        tempo = a3[0]; dn0 = a3[1]; rdndr0 = a3[2];
        sk0 = dn0 * robs * Math.sin ( zobs2 );
        f0 = refi ( dn0, rdndr0 );

/* Conditions at the tropopause in the troposphere. */
        rt = s + gmax( ht, hmok );
        a3 = atmt ( robs, tdkok, alpha, gamm2, delm2, c1, c2, c3, c4, c5, c6, rt );
        tt = a3[0]; dnt = a3[1]; rdndrt = a3[2];
        zt = Math.asin ( sk0 / ( rt * dnt ) );
        ft = refi ( dnt, rdndrt );

/* Conditions at the tropopause in the stratosphere. */
        double a2[] = atms ( rt, tt, dnt, gamal, rt );
        dnts = a2[0]; rdndrp = a2[1];
        zts = Math.asin ( sk0 / ( rt * dnts ) );
        fts = refi ( dnts, rdndrp );

/* Conditions at the stratosphere limit. */
        rs = s + hs;
        a2 = atms ( rt, tt, dnt, gamal, rs );
        dns = a2[0]; rdndrs = a2[1];
        zs = Math.asin ( sk0 / ( rs * dns ) );
        fs = refi ( dns, rdndrs );

/*
 * Integrate the refraction integral in two parts;  first in the
 * troposphere (k=1), then in the stratosphere (k=2).
 */

        for ( k = 1; k <= 2; k++ ) {

/* Initialize previous refraction to ensure at least two iterations. */
            refold = 1.0;

/* Start off with 8 strips. */
            is = 8;

/* Start z, z range, and start and end values. */
            if ( k == 1 ) {
                z0 = zobs2;
                zrange = zt - z0;
                fb = f0;
                ff = ft;
             } else {
                z0 = zts;
                zrange = zs - z0;
                fb = fts;
                ff = fs;
             }

/* Sums of odd and even values. */
             fo = 0.0;
             fe = 0.0;

/* First time through the loop we have to do every point. */
             n = 1;

/* Start of iteration loop (terminates at specified precision). */
             for ( ; ; ) {

/* Strip width */
                 h = zrange / (double) is;

/* Initialize distance from Earth centre for quadrature pass. */
                 r = ( k == 1 ) ? robs : rt;

/* One pass (no need to compute evens after first time). */
                 for ( i = 1; i < is; i += n ) {

/* Sine of observed zenith distance. */
                     sz = Math.sin ( z0 + h * (double) i );

/* Find r (to nearest metre, maximum four iterations). */
                     if ( sz > 1e-20 ) {
                         w = sk0 / sz;
                         rg = r;
                         j = 0;
                         do {
                             if ( k == 1 ) {
                                 a3 = atmt ( robs, tdkok, alpha, gamm2, delm2,
                                            c1, c2, c3, c4, c5, c6, rg );
                                 tg = a3[0]; dn = a3[1]; rdndr = a3[2];
                             } else {
                                 a2 = atms ( rt, tt, dnt, gamal, rg );
                                 dn = a2[0]; rdndr = a2[1];
                         }
                         dr = ( rg * dn - w ) / ( dn + rdndr );
                         rg -= dr;
                         } while ( Math.abs ( dr ) > 1.0 && j++ <= 4 );
                             r = rg;
                     }

/* Find refractive index and integrand at r. */
                     if ( k == 1 ) {
                         a3 = atmt ( robs, tdkok, alpha, gamm2, delm2,
                                    c1, c2, c3, c4, c5, c6, r );
                         t = a3[0]; dn = a3[1]; rdndr = a3[2];
                     } else {
                         a2 = atms ( rt, tt, dnt, gamal, r );
                         dn = a2[0]; rdndr = a2[1];
                     }
                     f = refi ( dn, rdndr );

/* Accumulate odd and (first time only) even values. */
                     if ( n == 1 && i % 2 == 0 ) {
                         fe += f;
                     } else {
                         fo += f;
                     }
                }

/* Evaluate the integrand using Simpson's Rule. */
                refp = h * ( fb + 4.0 * fo + 2.0 * fe + ff ) / 3.0;

/* Save troposphere component. */
                if ( k == 1 ) reft = refp;

/* If requested precision reached (or can't be), terminate the loop. */
                if ( Math.abs ( refp - refold ) <= tol || is >= ISMAX ) break;

/* Not yet: prepare for the next iteration. */
                refold = refp;   /* Save current value for convergence test */
                is += is;        /* Double the number of strips */
                fe += fo;        /* Sum of all = sum of evens next time */
                fo = 0.0;        /* Reset odds accumulator */
                n = 2;           /* Skip even values next time */
            }
        }

/* Result. */
        ref = reft + refp;
        if ( zobs1 < 0.0 ) ref = - ( ref );
        return ref;
    }

/*--------------------------------------------------------------------------*/

/*
 * Internal routine used by Refro:
 *
 *   Refractive index and derivative with respect to height for the
 *   troposphere.
 *
 *  Given:
 *    robs    double   height of observer from centre of the Earth (metre)
 *    tdkok   double   temperature at the observer (deg K)
 *    alpha   double   alpha          )
 *    gamm2   double   gamma minus 2  ) see ES
 *    delm2   double   delta minus 2  )
 *    c1      double   useful term  )
 *    c2      double   useful term  )
 *    c3      double   useful term  ) see source of
 *    c4      double   useful term  ) Refro main routine
 *    c5      double   useful term  )
 *    c6      double   useful term  )
 *    r       double   current distance from the centre of the Earth (metre)
 *
 *  Returned:
 *    *t      double   temperature at r (deg K)
 *    *dn     double   refractive index at r
 *    *rdndr  double   r * rate the refractive index is changing at r
 *
 *  This routine is derived from the ATMOSTRO routine (C.Hohenkerk,
 *  HMNAO), with enhancements specified by A.T.Sinclair (RGO) to
 *  handle the radio case.
 *
 *  Note that in the optical case c5 and c6 are zero.
 */
    private static double[] atmt ( double robs, double tdkok, double alpha,
                   double gamm2, double delm2, double c1, double c2, double c3,
                   double c4, double c5, double c6, double r )
    {
        double w, tt0, tt0gm2, tt0dm2;
        double t[] = new double[3];

        w = tdkok - alpha * ( r - robs );
        w = gmin ( w, 320.0 );
        w = gmax ( w, 100.0 );
        tt0 = w / tdkok;
        tt0gm2 = Math.pow ( tt0, gamm2 );
        tt0dm2 = Math.pow ( tt0, delm2 );
        t[0] = w;
        t[1] = 1.0 + ( c1 * tt0gm2 - ( c2 - c5 / w ) * tt0dm2 ) * tt0;
        t[2] = r * ( - c3 * tt0gm2 + ( c4 - c6 / tt0 ) * tt0dm2 );
        return t;
    }

/**
 *  Internal routine used by Refro:
 *
 *  Refractive index and derivative with respect to height for the
 *  stratosphere.
 *
 *  Given:
 *    rt      double   height of tropopause from centre of the Earth (metre)
 *    tt      double   temperature at the tropopause (deg k)
 *    dnt     double   refractive index at the tropopause
 *    gamal   double   constant of the atmospheric model = g*md/r
 *    r       double   current distance from the centre of the Earth (metre)
 *
 *  Returned:
 *    *dn     double   refractive index at r
 *    *rdndr  double   r * rate the refractive index is changing at r
 *
 *  This routine is derived from the ATMOSSTR routine (C.Hohenkerk, HMNAO).
 */
    private static double[] atms ( double rt, double tt, double dnt,
                                   double gamal, double r )
    {
        double b, w;
        double dn[] = new double[2];

        b = gamal / tt;
        w = ( dnt - 1.0 ) * Math.exp ( - b * ( r - rt ) );
        dn[0] = 1.0 + w;
        dn[1] = - r * b * w;
        return dn;
     }
/*--------------------------------------------------------------------------*/

/**
 *  Remove the e-terms (elliptic component of annual aberration)
 *  from a pre IAU 1976 catalogue RA,Dec to give a mean place.
 *  <dl>
 *  <dt>Explanation:</dt>
 *  <dd>Most star positions from pre-1984 optical catalogues (or
 *      derived from astrometry using such stars) embody the
 *      e-terms.  This routine converts such a position to a
 *      formal mean place (allowing, for example, comparison with a
 *      pulsar timing position).</dd>
 *
 *  <dt>Reference:</dt>
 *  <dd>Explanatory Supplement to the Astronomical Ephemeris,
 *      section 2D, page 48.</dd>
 *  </dl>
 *
 *  @param rc RA,Dec (radians) with e-terms included
 *  @param eq Besselian epoch of mean equator and equinox
 *  @return RA,Dec (radians) without e-terms
 */

/*  Latest Revision: 14 November 2001 (RTP)
 *
 *  Given:
 *     rc,dc     double     RA,Dec (radians) with e-terms included
 *     eq        double     Besselian epoch of mean equator and equinox
 *
 *  Returned:
 *     *rm,*dm   double     RA,Dec (radians) without e-terms
 *
 *  Called:
 *     Etrms, Dcs2c, ,dvdv, Dcc2s, Dranrm
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public AngleDR Subet ( AngleDR rc, double eq )
    {
        double a[], v[], f;
        AngleDR m;

/* E-terms */
        a = Etrms ( eq );

/* Spherical to Cartesian */
        v = Dcs2c ( rc );

/* Include the e-terms */
        f = 1.0 + Dvdv ( v, a );
        for ( int i = 0; i < 3; i++ ) {
           v[i] = f * v[i] - a[i];
        }

/* Cartesian to spherical */
        m = Dcc2s ( v );

/* Bring RA into conventional range */
        m.setAlpha( Dranrm ( m.getAlpha() ) );

        return m;
    }

/**
 *  Transformation from De Vaucouleurs supergalactic coordinates
 *  to IAU 1958 Galactic coordinates.
 *  <dl>
 *  <dt>References:</dt>
 *  <dd> <ol>
 *  <li>De Vaucouleurs, De Vaucouleurs, &amp; Corwin, Second Reference
 *     Catalogue of Bright Galaxies, U. Texas, page 8.</li>
 *
 *  <li> Systems &amp; Applied Sciences Corp., Documentation for the
 *     machine-readable version of the above catalogue,
 *     contract NAS 5-26490.</li>
 *  </ol> <p>
 *    (These two references give different values for the Galactic
 *     longitude of the supergalactic origin.  Both are wrong;  the
 *     correct value is l2=137.37.)</p>
 *  </dd>
 *  </dl>
 *
 *  @param ds Supergalactic longitude and latitude
 *  @return Galactic longitude and latitude l2,b2
 */

/*  Latest Revision: 27 November 2001 (RTP)
 *
 *  Given:
 *     dsl,dsb     double      supergalactic longitude and latitude
 *
 *  Returned:
 *     *dl,*db     double      Galactic longitude and latitude l2,b2
 *
 *  (all arguments are radians)
 *
 *  Called:
 *     Dcs2c, Dimxv, Dcc2s, Dranrm, Drange
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public Galactic Supgal ( Galactic ds )
    {
        double v1[], v2[];
/*
 *  System of supergalactic coordinates:
 *
 *    sgl   sgb        l2     b2      (deg)
 *     -    +90      47.37  +6.32
 *     0     0         -      0
 *
 *  Galactic to supergalactic rotation matrix:
*/
        final double rmat[][] =
        {
           { -0.735742574804,  0.677261296414,  0.0            },
           { -0.074553778365, -0.080991471307,  0.993922590400 },
           {  0.673145302109,  0.731271165817,  0.110081262225 }
        };

/* Spherical to Cartesian */
        AngleDR d = new AngleDR( ds.getLongitude(), ds.getLatitude() );
        v1 = Dcs2c ( d );

/* Supergalactic to Galactic */
        v2 = Dimxv ( rmat, v1 );

/* Cartesian to spherical */
        d = Dcc2s ( v2 );

/* Express in conventional ranges */
        return new Galactic ( Dranrm ( d.getAlpha() ),
                                 Drange ( d.getDelta() ) );
    }

/**
 *  HA, Dec to Zenith Distance.
 *  <dl>
 *  <dt>Notes:</dt>
 *  <dd> <ol>
 *  <li>The latitude must be geodetic.  In critical applications,
 *      corrections for polar motion should be applied.</li>
 *
 *  <li>In some applications it will be important to specify the
 *      correct type of hour angle and declination in order to
 *      produce the required type of zenith distance.  In particular,
 *      it may be important to distinguish between the zenith distance
 *      as affected by refraction, which would require the "observed"
 *      HA,Dec, and the zenith distance in vacuo, which would require
 *      the "topocentric" HA,Dec.  If the effects of diurnal aberration
 *      can be neglected, the "apparent" HA,Dec may be used instead of
 *      the topocentric HA,Dec.</li>
 *
 *  <li>No range checking of arguments is done.</li>
 *
 *  <li>In applications which involve many zenith distance calculations,
 *      rather than calling the present routine it will be more efficient
 *      to use inline code, having previously computed fixed terms such
 *      as sine and cosine of latitude, and perhaps sine and cosine of
 *      declination.</li>
 *  </ol> </dd>
 *  </dl>
 *
 *  @param ha  Hour Angle in radians
 *  @param dec Declination in radians
 *  @param phi Observatory latitude in radians
 *  @return Zenith distance in the range 0 to pi
 */

/*  Latest Revision: 23 November 2001 (RTP)
 *
 *  Given:
 *     ha     double     Hour Angle in radians
 *     dec    double     declination in radians
 *     phi    double     observatory latitude in radians
 *
 *  The result is in the range 0 to pi.
 *
 *  Copyright P.T.Wallace.  All rights reserved.
 */
    public double Zd ( double ha, double dec, double phi )
    {
        double sh, ch, sd, cd, sp, cp, x, y, z;

        sh = Math.sin ( ha );
        ch = Math.cos ( ha );
        sd = Math.sin ( dec );
        cd = Math.cos ( dec );
        sp = Math.sin ( phi );
        cp = Math.cos ( phi );

        x = ch * cd * sp - sd * cp;
        y = sh * cd;
        z = ch * cd * cp + sd * sp;

        return Math.atan2 ( Math.sqrt ( x * x + y * y ), z );
    }
}
