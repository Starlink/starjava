//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class wcscon
//
//--- Description -------------------------------------------------------------
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	07/16/98	J. Jones / 588
//
//		Original implementation.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

/*** File wcscon.c
 *** Doug Mink, Harvard-Smithsonian Center for Astrophysics
 *** Based on Starlink subroutines by Patrick Wallace
 *** March 20, 1997

 * Module:	wcscon.c (World Coordinate System conversion)
 * Purpose:	Convert between various sky coordinate systems
 * Subroutine:	fk524 (ra,dec) Convert J2000(FK5) to B1950(FK4) coordinates
 * Subroutine:	fk524e (ra, dec, epoch) (more accurate for known position epoch
 * Subroutine:	fk524m (ra,dec,rapm,decpm) exact
 * Subroutine:	fk425 (ra,dec) Convert B1950(FK4) to J2000(FK5) coordinates
 * Subroutine:	fk425e (ra, dec, epoch) (more accurate for known position epoch
 * Subroutine:	fk425m (ra, dec, rapm, decpm) exact
 * Subroutine:	fk42gal (dtheta,dphi) Convert B1950(FK4) to galactic coordinates
 * Subroutine:	fk52gal (dtheta,dphi) Convert J2000(FK5) to galactic coordinates
 * Subroutine:	gal2fk4 (dtheta,dphi) Convert galactic coordinates to B1950(FK4)
 * Subroutine:	gal2fk5 (dtheta,dphi) Convert galactic coordinates to J2000<FK5)
 * Subroutine:  fk5prec (ep0, ep1, ra, dec) Precession ep0 to ep1, FK5 system
 * Subroutine:  fk4prec (ep0, ep1, ra, dec) Precession ep0 to ep1, FK4 system
 */

package jsky.coords;

import java.awt.geom.*;

public class wcscon {

    /*  Constant vector and matrix (by columns)
        These values were obtained by inverting C.Hohenkerk's forward matrix
        (private communication), which agrees with the one given in reference
        2 but which has one additional decimal place.  */
    static double a[] = {-1.62557e-6, -0.31919e-6, -0.13843e-6};
    static double ad[] = {1.245e-3, -1.580e-3, -0.659e-3};

    /* FK524  convert J2000 FK5 star data to B1950 FK4
       based on Starlink sla_fk524 by P.T.Wallace 27 October 1987 */
    static double[][] emi = {
        {0.999925679499910, -0.011181482788805, -0.004859004008828,
         -0.000541640798032, -0.237963047085011, 0.436218238658637},

        {0.011181482840782, 0.999937484898031, -0.000027155744957,
         0.237912530551179, -0.002660706488970, -0.008537588719453},

        {0.004859003889183, -0.000027177143501, 0.999988194601879,
         -0.436101961325347, 0.012258830424865, 0.002119065556992},

        {-0.000002423898405, 0.000000027105439, 0.000000011777422,
         0.999904322043106, -0.011181451601069, -0.004858519608686},

        {-0.000000027105439, -0.000002423927017, 0.000000000065851,
         0.011181451608968, 0.999916125340107, -0.000027162614355},

        {-0.000000011777422, 0.000000000065846, -0.000002424049954,
         0.004858519590501, -0.000027165866691, 0.999966838131419}};

    /* Right ascension in degrees (J2000 in, B1950 out) */
    /* Declination in degrees (J2000 in, B1950 out) */
    public static Point2D.Double fk524(Point2D.Double input) {
        /* Proper motion in right ascension */
        /* Proper motion in declination  */
        /* In:  deg/jul.yr.  Out: deg/trop.yr.  */

        Point2D.Double pm = new Point2D.Double(0.0, 0.0);

        return fk524m(input, pm);
    }

    /* Right ascension in degrees (J2000 in, B1950 out) */
    /* Declination in degrees (J2000 in, B1950 out) */
    /* Besselian epoch in years */
    public static Point2D.Double fk524e(Point2D.Double input, double epoch) {
        /* Proper motion in right ascension */
        /* Proper motion in declination  */
        /* In:  deg/jul.yr.  Out: deg/trop.yr.  */

        Point2D.Double pm = new Point2D.Double(0.0, 0.0);

        Point2D.Double output = fk524m(input, pm);

        output.x = output.x + (pm.x * (epoch - 1950.0));
        output.y = output.y + (pm.y * (epoch - 1950.0));

        return output;
    }

    /* Right ascension in degrees (J2000 in, B1950 out) */
    /* Declination in degrees (J2000 in, B1950 out) */
    /* Proper motion in right ascension */
    /* Proper motion in declination */
    /* In:  deg/jul.yr.  Out: deg/trop.yr.  */

    /*  This routine converts stars from the new, IAU 1976, FK5, Fricke
        system, to the old, Bessel-Newcomb, FK4 system, using Yallop's
        implementation (see ref 2) of a matrix method due to Standish
        (see ref 3).  The numerical values of ref 2 are used canonically.

     *  Notes:

          1)  The proper motions in ra are dra / dt rather than
                cos(dec) * dra / dt, and are per year rather than per century.

          2)  Note that conversion from Julian epoch 2000.0 to Besselian
                epoch 1950.0 only is provided for.  Conversions involving
                other epochs will require use of the appropriate precession,
                proper motion, and e-terms routines before and/or after
                fk524 is called.

          3)  In the fk4 catalogue the proper motions of stars within
                10 degrees of the poles do not embody the differential
                e - term effect and should, strictly speaking, be handled
                in a different manner from stars outside these regions.
                however, given the general lack of homogeneity of the star
                data available for routine astrometry, the difficulties of
                handling positions that may have been determined from
                astrometric fields spanning the polar and non - polar regions,
                the likelihood that the differential e - terms effect was not
                taken into account when allowing for proper motion in past
                astrometry, and the undesirability of a discontinuity in
                the algorithm, the decision has been made in this routine to
                include the effect of differential e - terms on the proper
                motions for all stars, whether polar or not.  at epoch 2000,
                and measuring on the sky rather than in terms of dra, the
                errors resulting from this simplification are less than
                1 milliarcsecond in position and 1 milliarcsecond per
                century in proper motion.

       References:

          1  "Mean and apparent place computations in the new IAU System.
              I. The transformation of astrometric catalog systems to the
              equinox J2000.0." Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Yallop, B.D.; Hohenkerk, C.Y.
              Astronomical Journal vol. 97, Jan. 1989, p. 265-273.

          2  "Mean and apparent place computations in the new IAU System.
              II. Transformation of mean star places from FK4 B1950.0 to
              FK5 J2000.0 using matrices in 6-space."  Yallop, B.D.;
              Hohenkerk, C.Y.; Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Astronomical Journal vol. 97, Jan. 1989,
              p. 274-279.

          3  "Conversion of positions and proper motions from B1950.0 to the
              IAU system at J2000.0", Standish, E.M.  Astronomy and
              Astrophysics, vol. 115, no. 1, Nov. 1982, p. 20-22.

       P.T.Wallace   Starlink   27 October 1987
       Doug Mink     Smithsonian Astrophysical Observatory  7 June 1995 */
    public static Point2D.Double fk524m(Point2D.Double input, Point2D.Double pm) {
        double r2000,d2000;	/* J2000.0 ra,dec (radians) */
        double dr2000,dd2000;	/* J2000.0 proper motions (rad/jul.yr)*/
        double r1950,d1950;	/* B1950.0 ra,dec (rad) */
        double dr1950,dd1950;	/* B1950.0 proper motions (rad/trop.yr) */

        /* Miscellaneous */
        double ur,ud;
        double sr,cr,sd,cd,x,y,z,w;
        double v1[] = new double[6], v2[] = new double[6];
        double xd,yd,zd;
        double rxyz,rxysq,rxy;
        double dra,ddec,scon,tcon;
        int i,j;
        int diag = 0;

        /* Constants */
        double d2pi = 6.283185307179586476925287;	/* two PI */
        double pmf;	/* radians per year to arcsec per century */
        double tiny = 1.e-30; /* small number to avoid arithmetic problems */
        double zero = (double) 0.0;

        pmf = 100. * 60. * 60. * 360. / d2pi;

        /* Pick up J2000 data (units radians and arcsec / jc) */
        r2000 = WCSTransform.degrad(input.x);
        d2000 = WCSTransform.degrad(input.y);
        dr2000 = WCSTransform.degrad(pm.x);
        dd2000 = WCSTransform.degrad(pm.y);
        ur = dr2000 * pmf;
        ud = dd2000 * pmf;

        /* Spherical to Cartesian */
        sr = Math.sin(r2000);
        cr = Math.cos(r2000);
        sd = Math.sin(d2000);
        cd = Math.cos(d2000);

        x = cr * cd;
        y = sr * cd;
        z = sd;

        v1[0] = x;
        v1[1] = y;
        v1[2] = z;

        if (ur != zero || ud != zero) {
            v1[3] = -(ur * y) - (cr * sd * ud);
            v1[4] = (ur * x) - (sr * sd * ud);
            v1[5] = (cd * ud);
        }
        else {
            v1[3] = zero;
            v1[4] = zero;
            v1[5] = zero;
        }

        /* Convert position + velocity vector to bn system */
        for (i = 0; i < 6; i++) {
            w = zero;
            for (j = 0; j < 6; j++) {
                w = w + emi[j][i] * v1[j];
            }
            v2[i] = w;
        }

        /* Vector components */
        x = v2[0];
        y = v2[1];
        z = v2[2];
        xd = v2[3];
        yd = v2[4];
        zd = v2[5];

        /* Magnitude of position vector */
        rxyz = Math.sqrt(x * x + y * y + z * z);

        /* Include e-terms */
        x = x + a[0] * rxyz;
        y = y + a[1] * rxyz;
        z = z + a[2] * rxyz;
        xd = xd + ad[0] * rxyz;
        yd = yd + ad[1] * rxyz;
        zd = zd + ad[2] * rxyz;

        /* Convert to spherical */
        rxysq = x * x + y * y;
        rxy = Math.sqrt(rxysq);

        if (x == zero && y == zero)
            r1950 = zero;
        else {
            r1950 = Math.atan2(y, x);
            if (r1950 < zero)
                r1950 = r1950 + d2pi;
        }
        d1950 = Math.atan2(z, rxy);

        if (rxy > tiny) {
            ur = (x * yd - y * xd) / rxysq;
            ud = (zd * rxysq - z * (x * xd + y * yd)) / ((rxysq + z * z) * rxy);
        }
        dr1950 = ur / pmf;
        dd1950 = ud / pmf;

        /* Return results */
        input.x = WCSTransform.raddeg(r1950);
        input.y = WCSTransform.raddeg(d1950);
        pm.x = WCSTransform.raddeg(dr1950);
        pm.y = WCSTransform.raddeg(dd1950);

        if (diag > 0) {
            scon = WCSTransform.raddeg(3.6e3);
            tcon = WCSTransform.raddeg(2.4e2);
            dra = tcon * (r1950 - r2000);
            ddec = scon * (d1950 - d2000);
            //printf("B1950-J2000: dra= %11.5f sec  ddec= %f11.5f arcsec\n",
            //		dra, ddec);
        }

        return input;
    }


    /* Convert B1950.0 fk4 star data to J2000.0 fk5 */
    static double[][] em = {
        {0.999925678186902, 0.011182059571766, 0.004857946721186,
         -0.000541652366951, 0.237917612131583, -0.436111276039270},

        {-0.011182059642247, 0.999937478448132, -0.000027147426498,
         -0.237968129744288, -0.002660763319071, 0.012259092261564},

        {-0.004857946558960, -0.000027176441185, 0.999988199738770,
         0.436227555856097, -0.008537771074048, 0.002119110818172},

        {0.000002423950176, 0.000000027106627, 0.000000011776559,
         0.999947035154614, 0.011182506007242, 0.004857669948650},

        {-0.000000027106627, 0.000002423978783, -0.000000000065816,
         -0.011182506121805, 0.999958833818833, -0.000027137309539},

        {-0.000000011776558, -0.000000000065874, 0.000002424101735,
         -0.004857669684959, -0.000027184471371, 1.000009560363559}};

    /* Right ascension in degrees (B1950 in, J2000 out) */
    /* Declination in degrees (B1950 in, J2000 out) */
    public static Point2D.Double fk425(Point2D.Double input) {
        /* Proper motion in right ascension */
        /* Proper motion in declination  */
        /* In: rad/trop.yr.  Out:  rad/jul.yr. */

        Point2D.Double pm = new Point2D.Double(0.0, 0.0);

        return fk425m(input, pm);
    }

    /* Right ascension in degrees (B1950 in, J2000 out) */
    /* Declination in degrees (B1950 in, J2000 out) */
    /* Besselian epoch in years */
    public static Point2D.Double fk425e(Point2D.Double input, double epoch) {
        /* Proper motion in right ascension */
        /* Proper motion in declination  */
        /* In: rad/trop.yr.  Out:  rad/jul.yr. */

        Point2D.Double pm = new Point2D.Double(0.0, 0.0);

        Point2D.Double output = fk425m(input, pm);

        output.x = output.x + (pm.x * (epoch - 2000.0));
        output.y = output.y + (pm.y * (epoch - 2000.0));

        return output;
    }

    /* Right ascension and declination in degrees
                               input:  B1950.0,fk4	returned:  J2000.0,fk5 */
    /* Proper motion in right ascension and declination
                               input:  B1950.0,fk4	returned:  J2000.0,fk5
                                       deg/trop.yr.            deg/jul.yr.  */
    /* This routine converts stars from the old, Bessel-Newcomb, FK4
       system to the new, IAU 1976, FK5, Fricke system, using Yallop's
       implementation (see ref 2) of a matrix method due to Standish
       (see ref 3).  The numerical values of ref 2 are used canonically.

       Notes:

          1)  The proper motions in ra are dra/dt rather than
               cos(dec)*dra/dt, and are per year rather than per century.

          2)  Conversion from besselian epoch 1950.0 to Julian epoch
               2000.0 only is provided for.  Conversions involving other
               epochs will require use of the appropriate precession,
               proper motion, and e-terms routines before and/or
               after fk425 is called.

          3)  In the FK4 catalogue the proper motions of stars within
               10 degrees of the poles do not embody the differential
               e-term effect and should, strictly speaking, be handled
               in a different manner from stars outside these regions.
               However, given the general lack of homogeneity of the star
               data available for routine astrometry, the difficulties of
               handling positions that may have been determined from
               astrometric fields spanning the polar and non-polar regions,
               the likelihood that the differential e-terms effect was not
               taken into account when allowing for proper motion in past
               astrometry, and the undesirability of a discontinuity in
               the algorithm, the decision has been made in this routine to
               include the effect of differential e-terms on the proper
               motions for all stars, whether polar or not.  At epoch 2000,
               and measuring on the sky rather than in terms of dra, the
               errors resulting from this simplification are less than
               1 milliarcsecond in position and 1 milliarcsecond per
               century in proper motion.

       References:

          1  "Mean and apparent place computations in the new IAU System.
              I. The transformation of astrometric catalog systems to the
              equinox J2000.0." Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Yallop, B.D.; Hohenkerk, C.Y.
              Astronomical Journal vol. 97, Jan. 1989, p. 265-273.

          2  "Mean and apparent place computations in the new IAU System.
              II. Transformation of mean star places from FK4 B1950.0 to
              FK5 J2000.0 using matrices in 6-space."  Yallop, B.D.;
              Hohenkerk, C.Y.; Smith, C.A.; Kaplan, G.H.; Hughes, J.A.;
              Seidelmann, P.K.; Astronomical Journal vol. 97, Jan. 1989,
              p. 274-279.

          3  "Conversion of positions and proper motions from B1950.0 to the
              IAU system at J2000.0", Standish, E.M.  Astronomy and
              Astrophysics, vol. 115, no. 1, Nov. 1982, p. 20-22.

       P.T.Wallace   Starlink   27 October 1987
       Doug Mink     Smithsonian Astrophysical Observatory  7 June 1995 */
    public static Point2D.Double fk425m(Point2D.Double input, Point2D.Double pm) {
        double r1950,d1950;	/* B1950.0 ra,dec (rad) */
        double dr1950,dd1950;	/* B1950.0 proper motions (rad/trop.yr) */
        double r2000,d2000;	/* J2000.0 ra,dec (rad) */
        double dr2000,dd2000;	/*J2000.0 proper motions (rad/jul.yr) */

        /* Miscellaneous */
        double ur,ud,sr,cr,sd,cd,w,wd;
        double x,y,z,xd,yd,zd, dra,ddec,scon,tcon;
        double rxysq,rxyzsq,rxy,spxy;
        int i,j;
        int diag = 0;

        double r0[] = new double[3], r1[] = new double[3];	/* star position and velocity vectors */
        double v1[] = new double[6], v2[] = new double[6];	/* combined position and velocity vectors */

        /* Constants */
        double d2pi = 6.283185307179586476925287;	/* two PI */
        double pmf;	/* radians per year to arcsec per century */
        double tiny = 1.e-30; /* small number to avoid arithmetic problems */
        double zero = (double) 0.0;

        pmf = 100 * 60 * 60 * 360 / d2pi;

        /* Pick up B1950 data (units radians and arcsec / tc) */
        r1950 = WCSTransform.degrad(input.x);
        d1950 = WCSTransform.degrad(input.y);
        dr1950 = WCSTransform.degrad(pm.x);
        dd1950 = WCSTransform.degrad(pm.y);
        ur = dr1950 * pmf;
        ud = dd1950 * pmf;

        /* Spherical to cartesian */
        sr = Math.sin(r1950);
        cr = Math.cos(r1950);
        sd = Math.sin(d1950);
        cd = Math.cos(d1950);

        r0[0] = cr * cd;
        r0[1] = sr * cd;
        r0[2] = sd;

        r1[0] = -sr * cd * ur - cr * sd * ud;
        r1[1] = cr * cd * ur - sr * sd * ud;
        r1[2] = cd * ud;

        /* Allow for e-terms and express as position + velocity 6-vector */
        w = r0[0] * a[0] + r0[1] * a[1] + r0[2] * a[2];
        wd = r0[0] * ad[0] + r0[1] * ad[1] + r0[2] * ad[2];
        for (i = 0; i < 3; i++) {
            v1[i] = r0[i] - a[i] + w * r0[i];
            v1[i + 3] = r1[i] - ad[i] + wd * r0[i];
        }

        /* Convert position + velocity vector to Fricke system */
        for (i = 0; i < 6; i++) {
            w = zero;
            for (j = 0; j < 6; j++) {
                w = w + em[j][i] * v1[j];
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

        rxysq = x * x + y * y;
        rxyzsq = rxysq + z * z;
        rxy = Math.sqrt(rxysq);

        spxy = x * xd + y * yd;

        if (x == zero && y == zero)
            r2000 = zero;
        else {
            r2000 = Math.atan2(y, x);
            if (r2000 < zero)
                r2000 = r2000 + d2pi;
        }
        d2000 = Math.atan2(z, rxy);

        if (rxy > tiny) {
            ur = (x * yd - y * xd) / rxysq;
            ud = (zd * rxysq - z * spxy) / (rxyzsq * rxy);
        }
        dr2000 = ur / pmf;
        dd2000 = ud / pmf;

        /* Return results */
        input.x = WCSTransform.raddeg(r2000);
        input.y = WCSTransform.raddeg(d2000);
        pm.x = WCSTransform.raddeg(dr2000);
        pm.y = WCSTransform.raddeg(dd2000);

        if (diag > 0) {
            scon = WCSTransform.raddeg(3.6e3);
            tcon = WCSTransform.raddeg(2.4e2);
            dra = tcon * (r2000 - r1950);
            ddec = scon * (d2000 - d1950);
            //printf("J2000-B1950: dra= %11.5f sec  ddec= %f11.5f arcsec\n",
            //dra, ddec);
        }

        return input;
    }

    static int idg = 0;

    /*  l2,b2 system of galactic coordinates
     *  p = 192.25       ra of galactic north pole (mean b1950.0)
     *  q =  62.6        inclination of galactic to mean b1950.0 equator
     *  r =  33          longitude of ascending node
     *  p,q,r are degrees

     *  Equatorial to galactic rotation matrix
        (The Eulerian angles are p, q, 90-r)
            +cp.cq.sr-sp.cr	+sp.cq.sr+cp.cr	-sq.sr
            -cp.cq.cr-sp.sr	-sp.cq.cr+cp.sr	+sq.cr
            cp.sq		+sp.sq		+cq
     */

    static double[][] bgal = {
        {-0.066988739415, -0.872755765852, -0.483538914632},
        {0.492728466075, -0.450346958020, 0.744584633283},
        {-0.867600811151, -0.188374601723, 0.460199784784}};

    /*---  Transform b1950.0 'fk4' equatorial coordinates to
     *     IAU 1958 galactic coordinates */

    /* b1950.0 'fk4' ra in degrees
                       Galactic longitude (l2) in degrees (returned) */
    /* b1950.0 'fk4' dec in degrees
                       Galactic latitude (b2) in degrees (returned) */

    /*  Note:   The equatorial coordinates are b1950.0 'fk4'.  use the
                routine jpgalj if conversion from j2000.0 coordinates
                is required.
                Reference: blaauw et al, MNRAS,121,123 (1960) */

    public static Point2D.Double fk42gal(Point2D.Double input) {
        double pos[] = new double[3],pos1[] = new double[3],dl,db,rra,rdec,dra,ddec;
        int i;

        dra = input.x;
        ddec = input.y;
        rra = WCSTransform.degrad(dra);
        rdec = WCSTransform.degrad(ddec);

        /*  remove e-terms */
        /*	call jpabe (rra,rdec,-1,idg) */

        /*  Spherical to Cartesian */
        Point2D.Double pos0 = new Point2D.Double();
        Point2D.Double pos2 = new Point2D.Double();
        jpcop(rra, rdec, 1.0, pos0, pos2);
        pos[0] = pos0.x;
        pos[1] = pos0.y;
        pos[2] = pos2.x;

        /*  rotate to galactic */
        for (i = 0; i < 3; i++) {
            pos1[i] = pos[0] * bgal[i][0] + pos[1] * bgal[i][1] + pos[2] * bgal[i][2];
        }

        /*  Cartesian to spherical */
        Point2D.Double rPos = new Point2D.Double();
        Point2D.Double r = new Point2D.Double();
        jpcon(pos1, rPos, r);

        dl = WCSTransform.raddeg(rPos.x);
        db = WCSTransform.raddeg(rPos.y);
        input.x = dl;
        input.y = db;

        /*  Print result if in diagnostic mode */
        if (idg > 0) {
            //eqcoor = eqstrn (dra,ddec);
            //printf ("FK42GAL: B1950 RA,Dec= %s\n",eqcoor);
            //printf ("FK42GAL: long = %.5f lat = %.5f\n",dl,db);
            //free (eqcoor);
        }

        return input;
    }


    /*--- Transform IAU 1958 galactic coordinates to B1950.0 'fk4'
     *    equatorial coordinates */

    /* Galactic longitude (l2) in degrees
                       B1950 FK4 RA in degrees (returned) */
    /* Galactic latitude (b2) in degrees
                       B1950 FK4 Dec in degrees (returned) */

    /*  Note:
           The equatorial coordinates are B1950.0 FK4.  Use the
           routine GAL2FK5 if conversion to J2000 coordinates
           is required.
        Reference:  Blaauw et al, MNRAS,121,123 (1960) */

    public static Point2D.Double gal2fk4(Point2D.Double input) {
        double pos[] = new double[3],pos1[] = new double[3],dl,db,rl,rb,rra,rdec,dra,ddec;
        int i;

        /*  spherical to cartesian */
        dl = input.x;
        db = input.y;
        rl = WCSTransform.degrad(dl);
        rb = WCSTransform.degrad(db);

        Point2D.Double pos0 = new Point2D.Double();
        Point2D.Double pos2 = new Point2D.Double();
        jpcop(rl, rb, 1.0, pos0, pos2);
        pos[0] = pos0.x;
        pos[1] = pos0.y;
        pos[2] = pos2.x;

        /*  rotate to equatorial coordinates */
        for (i = 0; i < 3; i++) {
            pos1[i] = pos[0] * bgal[0][i] + pos[1] * bgal[1][i] + pos[2] * bgal[2][i];
        }

        /*  cartesian to spherical */
        Point2D.Double rPos = new Point2D.Double();
        Point2D.Double r = new Point2D.Double();
        jpcon(pos1, rPos, r);

        /*  introduce e-terms */
        /*	jpabe (rra,rdec,-1,idg); */

        dra = WCSTransform.raddeg(rPos.x);
        ddec = WCSTransform.raddeg(rPos.y);
        input.x = dra;
        input.y = ddec;

        /*  print result if in diagnostic mode */
        if (idg > 0) {
            //printf ("GAL2FK4: long = %.5f lat = %.5f\n",dl,db);
            //eqcoor = eqstrn (dra,ddec);
            //printf ("GAL2FK4: B1950 RA,Dec= %s\n",eqcoor);
            //free (eqcoor);
        }

        return input;
    }


    /*  l2,b2 system of galactic coordinates
        p = 192.25       ra of galactic north pole (mean b1950.0)
        q =  62.6        inclination of galactic to mean b1950.0 equator
        r =  33          longitude of ascending node
        p,q,r are degrees */

    /*  Equatorial to galactic rotation matrix
        The eulerian angles are p, q, 90-r
            +cp.cq.sr-sp.cr     +sp.cq.sr+cp.cr     -sq.sr
            -cp.cq.cr-sp.sr     -sp.cq.cr+cp.sr     +sq.cr
            +cp.sq              +sp.sq              +cq		*/

    static double[][] jgal = {
        {-0.054875539726, -0.873437108010, -0.483834985808},
        {0.494109453312, -0.444829589425, 0.746982251810},
        {-0.867666135858, -0.198076386122, 0.455983795705}};

    /* Transform J2000 equatorial coordinates to IAU 1958 galactic coordinates */

    /* J2000 right ascension in degrees
                       Galactic longitude (l2) in degrees (returned) */
    /* J2000 declination in degrees
                       Galactic latitude (b2) in degrees (returned) */

    /* Rotation matrices by P.T.Wallace, Starlink eqgal and galeq, March 1986 */
    /*  Note:
            The equatorial coordinates are J2000 FK5.  Use the routine
            GAL2FK4 if conversion from B1950 FK4 coordinates is required.
        Reference: Blaauw et al, MNRAS,121,123 (1960) */

    public static Point2D.Double fk52gal(Point2D.Double input) {
        double pos[] = new double[3],pos1[] = new double[3],dl,db,rl,rb,rra,rdec,dra,ddec;
        int i;

        /*  Spherical to cartesian */
        dra = input.x;
        ddec = input.y;
        rra = WCSTransform.degrad(dra);
        rdec = WCSTransform.degrad(ddec);

        Point2D.Double pos0 = new Point2D.Double();
        Point2D.Double pos2 = new Point2D.Double();
        jpcop(rra, rdec, 1.0, pos0, pos2);
        pos[0] = pos0.x;
        pos[1] = pos0.y;
        pos[2] = pos2.x;

        /*  Rotate to galactic */
        for (i = 0; i < 3; i++) {
            pos1[i] = pos[0] * jgal[i][0] + pos[1] * jgal[i][1] + pos[2] * jgal[i][2];
        }

        /*  Cartesian to spherical */
        Point2D.Double rPos = new Point2D.Double();
        Point2D.Double r = new Point2D.Double();
        jpcon(pos1, rPos, r);

        dl = WCSTransform.raddeg(rPos.x);
        db = WCSTransform.raddeg(rPos.y);
        input.x = dl;
        input.y = db;

        /*  Print result if in diagnostic mode */

        if (idg > 0) {
            //eqcoor = eqstrn (dra,ddec);
            //printf ("FK52GAL: J2000 RA,Dec= %s\n",eqcoor);
            //printf ("FK52GAL: long = %.5f lat = %.5f\n",dl,db);
            //free (eqcoor);
        }

        return input;
    }


    /*--- Transform IAU 1958 galactic coordinates to J2000 equatorial coordinates */

    /* Galactic longitude (l2) in degrees
                       J2000.0 ra in degrees (returned) */
    /* Galactic latitude (b2) in degrees
                       J2000.0 dec in degrees (returned) */

    /*  Note:
           The equatorial coordinates are J2000.  Use the routine FK42GAL
           if conversion to J2000 coordinates is required.
        Reference: Blaauw et al, MNRAS,121,123 (1960) */

    public static Point2D.Double gal2fk5(Point2D.Double input) {
        double pos[] = new double[3],pos1[] = new double[3],dl,db,rl,rb,rra,rdec,dra,ddec;
        int i;

        /*  Spherical to Cartesian */
        dl = input.x;
        db = input.y;
        rl = WCSTransform.degrad(dl);
        rb = WCSTransform.degrad(db);

        Point2D.Double pos0 = new Point2D.Double();
        Point2D.Double pos2 = new Point2D.Double();
        jpcop(rl, rb, 1.0, pos0, pos2);
        pos[0] = pos0.x;
        pos[1] = pos0.y;
        pos[2] = pos2.x;

        /*  Rotate to equatorial coordinates */
        for (i = 0; i < 3; i++) {
            pos1[i] = pos[0] * jgal[0][i] + pos[1] * jgal[1][i] + pos[2] * jgal[2][i];
        }

        /*  Cartesian to Spherical */
        Point2D.Double rPos = new Point2D.Double();
        Point2D.Double r = new Point2D.Double();
        jpcon(pos1, rPos, r);

        dra = WCSTransform.raddeg(rPos.x);
        ddec = WCSTransform.raddeg(rPos.y);

        input.x = dra;
        input.y = ddec;

        /*  Print result if in diagnostic mode */
        if (idg > 0) {
            //printf ("GAL2FK5: long = %.5f lat = %.5f\n",dl,db);
            //eqcoor = eqstrn (dra,ddec);
            //printf ("GAL2FK5: J2000 RA,Dec= %s\n",eqcoor);
            //free (eqcoor);
        }

        return input;
    }


    /* Convert geocentric equatorial rectangular coordinates to
       right ascension and declination, and distance */

    /* x,y,z geocentric equatorial position of object */
    /* Right ascension in radians */
    /* Declination in radians */
    /* Distance to object in same units as pos */
    protected static void jpcon(double pos[], Point2D.Double rPos, Point2D.Double r) {
        double x,y,z,rxy,rxy2,z2;

        x = pos[0];
        y = pos[1];
        z = pos[2];

        rPos.x = Math.atan2(y, x);
        if (rPos.x < 0.) rPos.x = rPos.x + 6.283185307179586;

        rxy2 = x * x + y * y;
        rxy = Math.sqrt(rxy2);
        rPos.y = Math.atan2(z, rxy);

        z2 = z * z;
        r.x = Math.sqrt(rxy2 + z2); // store r in x component so can return through object ref
    }


    /* Convert right ascension, declination, and distance to
       geocentric equatorial rectangular coordinates */

    /* Right ascension in radians */
    /* Declination in radians */
    /* Distance to object in same units as pos */
    /* x,y,z geocentric equatorial position of object */

    protected static void jpcop(double rra, double rdec, double r, Point2D.Double result0, Point2D.Double result2) {
        result0.x = r * Math.cos(rra) * Math.cos(rdec);
        result0.y = r * Math.sin(rra) * Math.cos(rdec);
        result2.x = r * Math.sin(rdec);
    }

    /* The following routines are almost verbatim from Patrick Wallace's SLALIB */

    /* Starting Besselian epoch */
    /* Ending Besselian epoch */
    /* RA in degrees mean equator & equinox of epoch ep0
                          mean equator & equinox of epoch ep1 (returned) */
    /* Dec in degrees mean equator & equinox of epoch ep0
                           mean equator & equinox of epoch ep1 (returned) */
    /*
    **  slaPreces:
    **  Precession - FK4 (Bessel-Newcomb, pre-IAU1976)
    **
    **  Note:
    **      This routine will not correctly convert between the old and
    **      the new systems - for example conversion from B1950 to J2000.
    **      For these purposes see fk425, fk524, fk45m and fk54m.
    **
    **  P.T.Wallace   Starlink   22 December 1993
    */
    public static Point2D.Double fk4prec(double ep0, double ep1, Point2D.Double input) {
        double rra = WCSTransform.degrad(input.x);
        double rdec = WCSTransform.degrad(input.y);

        /* Generate appropriate precession matrix */
        double[][] pm = mprecfk4(ep0, ep1);

        /* Convert RA,Dec to x,y,z */
        double[] v1 = slasubs.slaDcs2c(rra, rdec);

        /* Precess */
        double[] v2 = slasubs.slaDmxv(pm, v1);

        /* Back to RA,Dec */
        Point2D.Double rPoint = slasubs.slaDcc2s(v2);
        rra = rPoint.x;
        rdec = rPoint.y;

        rra = slasubs.slaDranrm(rra);

        input.x = WCSTransform.raddeg(rra);
        input.y = WCSTransform.raddeg(rdec);

        return input;
    }

    /* Starting epoch */
    /* Ending epoch */
    /* RA in degrees mean equator & equinox of epoch ep0
                          mean equator & equinox of epoch ep1 (returned) */
    /* Dec in degrees mean equator & equinox of epoch ep0
                           mean equator & equinox of epoch ep1 (returned) */
    /*
    **  slaPreces:
    **  Precession -  FK5 (Fricke, post-IAU1976)
    **
    **  Note:
    **      This routine will not correctly convert between the old and
    **      the new systems - for example conversion from B1950 to J2000.
    **      For these purposes see fk425, fk524, fk45m and fk54m.
    **
    **  P.T.Wallace   Starlink   22 December 1993
    */
    public static Point2D.Double fk5prec(double ep0, double ep1, Point2D.Double input) {
        double rra = WCSTransform.degrad(input.x);
        double rdec = WCSTransform.degrad(input.y);

        /* Generate appropriate precession matrix */
        double[][] pm = mprecfk5(ep0, ep1);

        /* Convert RA,Dec to x,y,z */
        double[] v1 = slasubs.slaDcs2c(rra, rdec);

        /* Precess */
        double[] v2 = slasubs.slaDmxv(pm, v1);

        /* Back to RA,Dec */
        Point2D.Double rPoint = slasubs.slaDcc2s(v2);
        rra = rPoint.x;
        rdec = rPoint.y;

        rra = slasubs.slaDranrm(rra);

        input.x = WCSTransform.raddeg(rra);
        input.y = WCSTransform.raddeg(rdec);

        return input;
    }


    /* pi/(180*3600):  arcseconds to radians */
    public static final double DAS2R = 4.8481368110953599358991410235794797595635330237270e-6;

    /* Beginning Besselian epoch */
    /* Ending Besselian epoch */
    /* 3x3 Precession matrix (returned) */
    /*
    **  slaPrebn:
    **  Generate the matrix of precession between two epochs,
    **  using the old, pre-IAU1976, Bessel-Newcomb model, using
    **  Kinoshita's formulation (double precision)
    **
    **  The matrix is in the sense   v(bep1)  =  rmatp * v(bep0)
    **
    **  Reference:
    **     Kinoshita, H. (1975) 'Formulas for precession', SAO Special
    **     Report No. 364, Smithsonian Institution Astrophysical
    **     Observatory, Cambridge, Massachusetts.
    **
    **  P.T.Wallace   Starlink   30 October 1993
    */
    public static double[][] mprecfk4(double bep0, double bep1) {
        double bigt, t, tas2r, w, zeta, z, theta;

        /* Interval between basic epoch B1850.0 and beginning epoch in TC */
        bigt = (bep0 - 1850.0) / 100.0;

        /* Interval over which precession required, in tropical centuries */
        t = (bep1 - bep0) / 100.0;

        /* Euler angles */
        tas2r = t * DAS2R;
        w = 2303.5548 + (1.39720 + 0.000059 * bigt) * bigt;
        zeta = (w + (0.30242 - 0.000269 * bigt + 0.017996 * t) * t) * tas2r;
        z = (w + (1.09478 + 0.000387 * bigt + 0.018324 * t) * t) * tas2r;
        theta = (2005.1125 + (-0.85294 - 0.000365 * bigt) * bigt +
                (-0.42647 - 0.000365 * bigt - 0.041802 * t) * t) * tas2r;

        /* Rotation matrix */
        return slasubs.slaDeuler("ZYZ", -zeta, theta, -z);
    }

    /* Beginning epoch */
    /* Ending epoch */
    /* 3x3 Precession matrix (returned) */
    /*
    **  slaPrec:
    **  Form the matrix of precession between two epochs (IAU 1976, FK5).
    **  Notes:
    **  1)  The epochs are TDB (loosely ET) Julian epochs.
    **  2)  The matrix is in the sense   v(ep1)  =  rmatp * v(ep0) .
    **
    **  References:
    **     Lieske,J.H., 1979. Astron. Astrophys.,73,282.
    **          equations (6) & (7), p283.
    **     Kaplan,G.H., 1981. USNO circular no. 163, pa2.
    **
    **  P.T.Wallace   Starlink   31 October 1993
    */
    public static double[][] mprecfk5(double ep0, double ep1) {
        double t0, t, tas2r, w, zeta, z, theta;

        /* Interval between basic epoch J2000.0 and beginning epoch (JC) */
        t0 = (ep0 - 2000.0) / 100.0;

        /* Interval over which precession required (JC) */
        t = (ep1 - ep0) / 100.0;

        /* Euler angles */
        tas2r = t * DAS2R;
        w = 2306.2181 + ((1.39656 - (0.000139 * t0)) * t0);
        zeta = (w + ((0.30188 - 0.000344 * t0) + 0.017998 * t) * t) * tas2r;
        z = (w + ((1.09468 + 0.000066 * t0) + 0.018203 * t) * t) * tas2r;
        theta = ((2004.3109 + (-0.85330 - 0.000217 * t0) * t0)
                + ((-0.42665 - 0.000217 * t0) - 0.041833 * t) * t) * tas2r;

        /* Rotation matrix */
        return slasubs.slaDeuler("ZYZ", -zeta, theta, -z);
    }
    /*
     * Nov  6 1995	Include stdlib.h instead of malloc.h
     * Apr  1 1996	Add arbitrary epoch precession
     * Apr 26 1996	Add FK4 <-> FK5 subroutines for use when epoch is known
     * Aug  6 1996	Clean up after lint
     * Nov  4 1996	Break SLA subroutines into separate file slasubs.c
     * Dec  9 1996	Change arguments to degrees in FK4 and FK5 precession programs
     * Dec 10 1996	All subroutine arguments are degrees except vector conversions
     *
     * Mar 20 1997	Drop unused variables after lint
     */
}
