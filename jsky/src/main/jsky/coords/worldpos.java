//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class worldpos
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

/*  worldpos.c -- WCS Algorithms from Classic AIPS.
 *  February 6, 1998
 *  Copyright (C) 1994
 *  Associated Universities, Inc. Washington DC, USA.
 *  With code added by Doug Mink, Smithsonian Astrophysical Observatory

 * Module:	worldpos.c
 * Purpose:	Perform forward and reverse WCS computations for 8 projections
 * Subroutine:	worldpos() converts from pixel location to RA,Dec
 * Subroutine:	worldpix() converts from RA,Dec         to pixel location

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Library General Public License as published by
    the Free Software Foundation; either version 2 of the License, or (at your
    option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Library General Public
    License for more details.

    You should have received a copy of the GNU Library General Public License
    along with this library; if not, write to the Free Software Foundation,
    Inc., 675 Massachusetts Ave, Cambridge, MA 02139, USA.

    Correspondence concerning AIPS should be addressed as follows:
	   Internet email: aipsmail@nrao.edu
	   Postal address: AIPS Group
	                   National Radio Astronomy Observatory
	                   520 Edgemont Road
	                   Charlottesville, VA 22903-2475 USA

	         -=-=-=-=-=-=-

    These two ANSI C functions, worldpos() and worldpix(), perform
    forward and reverse WCS computations for 8 types of projective
    geometries ("-SIN", "-TAN", "-ARC", "-NCP", "-GLS", "-MER", "-AIT"
    and "-STG"):

	worldpos() converts from pixel location to RA,Dec
	worldpix() converts from RA,Dec         to pixel location

    where "(RA,Dec)" are more generically (long,lat). These functions
    are based on the WCS implementation of Classic AIPS, an
    implementation which has been in production use for more than ten
    years. See the two memos by Eric Greisen

	ftp://fits.cv.nrao.edu/fits/documents/wcs/aips27.ps.Z
	ftp://fits.cv.nrao.edu/fits/documents/wcs/aips46.ps.Z

    for descriptions of the 8 projective geometries and the
    algorithms.  Footnotes in these two documents describe the
    differences between these algorithms and the 1993-94 WCS draft
    proposal (see URL below). In particular, these algorithms support
    ordinary field rotation, but not skew geometries (CD or PC matrix
    cases). Also, the MER and AIT algorithms work correctly only for
    CRVALi=(0,0). Users should note that GLS projections with yref!=0
    will behave differently in this code than in the draft WCS
    proposal.  The NCP projection is now obsolete (it is a special
    case of SIN).  WCS syntax and semantics for various advanced
    features is discussed in the draft WCS proposal by Greisen and
    Calabretta at:

	ftp://fits.cv.nrao.edu/fits/documents/wcs/wcs.all.ps.Z

	        -=-=-=-

    The original version of this code was Emailed to D.Wells on
    Friday, 23 September by Bill Cotton <bcotton@gorilla.cv.nrao.edu>,
    who described it as a "..more or less.. exact translation from the
    AIPSish..". Changes were made by Don Wells <dwells@nrao.edu>
    during the period October 11-13, 1994:
    1) added GNU license and header comments
    2) added testpos.c program to perform extensive circularity tests
    3) changed float-->double to get more than 7 significant figures
    4) testpos.c circularity test failed on MER and AIT. B.Cotton
       found that "..there were a couple of lines of code [in] the wrong
       place as a result of merging several Fortran routines."
    5) testpos.c found 0h wraparound in worldpix() and worldpos().
    6) E.Greisen recommended removal of various redundant if-statements,
       and addition of a 360d difference test to MER case of worldpos().
    7) D.Mink changed input to data structure and implemented rotation matrix.
*/

package jsky.coords;

import java.awt.geom.*;

public class worldpos {

    /* Routine to determine accurate position for pixel coordinates */
    /* returns 0 if successful otherwise 1 = angle too large for projection; */
    /* does: -SIN, -TAN, -ARC, -NCP, -GLS, -MER, -AIT projections */
    /* anything else is linear */
    /* Input: */
    /* x pixel number  (RA or long without rotation) */
    /* y pixel number  (Dec or lat without rotation) */
    /* Output: */
    /* x (RA) coordinate (deg) */
    /* y (dec) coordinate (deg) */
    public static Point2D.Double getPosition(double xpix, double ypix, WCSTransform wcs) {
        double cosr, sinr, dx, dy, dz, tx;
        double sins, coss, dect = 0.0, rat = 0.0, dt, l, m, mg, da, dd, cos0, sin0;
        double dec0, ra0, decout, raout;
        double geo1, geo2, geo3;
        double cond2r = 1.745329252e-2;
        double twopi = 6.28318530717959;
        double deps = 1.0e-5;

        /* Structure elements */
        double xref;		/* X reference coordinate value (deg) */
        double yref;		/* Y reference coordinate value (deg) */
        double xrefpix;	/* X reference pixel */
        double yrefpix;	/* Y reference pixel */
        double xinc;		/* X coordinate increment (deg) */
        double yinc;		/* Y coordinate increment (deg) */
        double rot;		/* Optical axis rotation (deg)  (N through E) */
        int itype = wcs.pcode;

        double xpos = 0.0;
        double ypos = 0.0;

        /* Set local projection parameters */
        xref = wcs.xref;
        yref = wcs.yref;
        xrefpix = wcs.xrefpix;
        yrefpix = wcs.yrefpix;
        xinc = wcs.xinc;
        yinc = wcs.yinc;
        rot = wcs.degrad(wcs.rot);
        cosr = Math.cos(rot);
        sinr = Math.sin(rot);

        /* Offset from ref pixel */
        dx = xpix - xrefpix;
        dy = ypix - yrefpix;

        /* Scale and rotate using CD matrix */
        if (wcs.rotmat > 0) {
            tx = dx * wcs.cd11 + dy * wcs.cd12;
            dy = dx * wcs.cd21 + dy * wcs.cd22;
            dx = tx;
        }
        else {
            /* Check axis increments - bail out if either 0 */
            if ((xinc == 0.0) || (yinc == 0.0)) {
                return null;
            }

            /* Scale using CDELT */
            dx = dx * xinc;
            dy = dy * yinc;

            /* Take out rotation from CROTA */
            if (rot != 0.0) {
                tx = dx * cosr - dy * sinr;
                dy = dx * sinr + dy * cosr;
                dx = tx;
            }
        }

        /* Default, linear result for error or pixel return  */
        xpos = xref + dx;
        ypos = yref + dy;
        if (itype < 0) {
            return new Point2D.Double(xpos, ypos);
        }

        /* Convert to radians  */
        if (wcs.coorflip > 0) {
            dec0 = wcs.degrad(xref);
            ra0 = wcs.degrad(yref);
            tx = dx;
            dx = dy;
            dy = tx;
        }
        else {
            ra0 = wcs.degrad(xref);
            dec0 = wcs.degrad(yref);
        }

        l = wcs.degrad(dx);
        m = wcs.degrad(dy);
        sins = l * l + m * m;
        decout = 0.0;
        raout = 0.0;
        cos0 = Math.cos(dec0);
        sin0 = Math.sin(dec0);

        /* process by case  */
        switch (itype) {
        case -1:   /* pixel */
        case 0:   /* linear */
            rat = ra0 + l;
            dect = dec0 + m;
            break;

        case 1:   /* -SIN sin*/
            if (sins > 1.0) return null;
            coss = Math.sqrt(1.0 - sins);
            dt = sin0 * coss + cos0 * m;
            if ((dt > 1.0) || (dt < -1.0)) return null;
            dect = Math.asin(dt);
            rat = cos0 * coss - sin0 * m;
            if ((rat == 0.0) && (l == 0.0)) return null;
            rat = Math.atan2(l, rat) + ra0;
            break;

        case 2:   /* -TAN tan */
            if (sins > 1.0) return null;
            dect = cos0 - m * sin0;
            if (dect == 0.0) return null;
            rat = ra0 + Math.atan2(l, dect);
            dect = Math.atan(Math.cos(rat - ra0) * (m * cos0 + sin0) / dect);
            break;

        case 3:   /* -ARC Arc*/
            if (sins >= twopi * twopi / 4.0) return null;
            sins = Math.sqrt(sins);
            coss = Math.cos(sins);
            if (sins != 0.0)
                sins = Math.sin(sins) / sins;
            else
                sins = 1.0;
            dt = m * cos0 * sins + sin0 * coss;
            if ((dt > 1.0) || (dt < -1.0)) return null;
            dect = Math.asin(dt);
            da = coss - dt * sin0;
            dt = l * sins * cos0;
            if ((da == 0.0) && (dt == 0.0)) return null;
            rat = ra0 + Math.atan2(dt, da);
            break;

        case 4:   /* -NCP North celestial pole*/
            dect = cos0 - m * sin0;
            if (dect == 0.0) return null;
            rat = ra0 + Math.atan2(l, dect);
            dt = Math.cos(rat - ra0);
            if (dt == 0.0) return null;
            dect = dect / dt;
            if ((dect > 1.0) || (dect < -1.0)) return null;
            dect = Math.acos(dect);
            if (dec0 < 0.0) dect = -dect;
            break;

        case 5:   /* -GLS global sinusoid */
            dect = dec0 + m;
            if (Math.abs(dect) > twopi / 4.0) return null;
            coss = Math.cos(dect);
            if (Math.abs(l) > twopi * coss / 2.0) return null;
            rat = ra0;
            if (coss > deps) rat = rat + l / coss;
            break;

        case 6:   /* -MER mercator*/
            dt = yinc * cosr + xinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dy = wcs.degrad(yref / 2.0 + 45.0);
            dx = dy + dt / 2.0 * cond2r;
            dy = Math.log(Math.tan(dy));
            dx = Math.log(Math.tan(dx));
            geo2 = wcs.degrad(dt) / (dx - dy);
            geo3 = geo2 * dy;
            geo1 = Math.cos(wcs.degrad(yref));
            if (geo1 <= 0.0) geo1 = 1.0;
            rat = l / geo1 + ra0;
            if (Math.abs(rat - ra0) > twopi) return null; /* added 10/13/94 DCW/EWG */
            dt = 0.0;
            if (geo2 != 0.0) dt = (m + geo3) / geo2;
            dt = Math.exp(dt);
            dect = 2.0 * Math.atan(dt) - twopi / 4.0;
            break;

        case 7:   /* -AIT Aitoff*/
            dt = yinc * cosr + xinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dt = wcs.degrad(dt);
            dy = wcs.degrad(yref);
            dx = Math.sin(dy + dt) / Math.sqrt((1.0 + Math.cos(dy + dt)) / 2.0) -
                    Math.sin(dy) / Math.sqrt((1.0 + Math.cos(dy)) / 2.0);
            if (dx == 0.0) dx = 1.0;
            geo2 = dt / dx;
            dt = xinc * cosr - yinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dt = wcs.degrad(dt);
            dx = 2.0 * Math.cos(dy) * Math.sin(dt / 2.0);
            if (dx == 0.0) dx = 1.0;
            geo1 = dt * Math.sqrt((1.0 + Math.cos(dy) * Math.cos(dt / 2.0)) / 2.0) / dx;
            geo3 = geo2 * Math.sin(dy) / Math.sqrt((1.0 + Math.cos(dy)) / 2.0);
            rat = ra0;
            dect = dec0;
            if ((l == 0.0) && (m == 0.0)) break;
            dz = 4.0 - l * l / (4.0 * geo1 * geo1) - ((m + geo3) / geo2) * ((m + geo3) / geo2);
            if ((dz > 4.0) || (dz < 2.0)) return null;
            dz = 0.5 * Math.sqrt(dz);
            dd = (m + geo3) * dz / geo2;
            if (Math.abs(dd) > 1.0) return null;
            dd = Math.asin(dd);
            if (Math.abs(Math.cos(dd)) < deps) return null;
            da = l * dz / (2.0 * geo1 * Math.cos(dd));
            if (Math.abs(da) > 1.0) return null;
            da = Math.asin(da);
            rat = ra0 + 2.0 * da;
            dect = dd;
            break;

        case 8:   /* -STG Sterographic*/
            dz = (4.0 - sins) / (4.0 + sins);
            if (Math.abs(dz) > 1.0) return null;
            dect = dz * sin0 + m * cos0 * (1.0 + dz) / 2.0;
            if (Math.abs(dect) > 1.0) return null;
            dect = Math.asin(dect);
            rat = Math.cos(dect);
            if (Math.abs(rat) < deps) return null;
            rat = l * (1.0 + dz) / (2.0 * rat);
            if (Math.abs(rat) > 1.0) return null;
            rat = Math.asin(rat);
            mg = 1.0 + Math.sin(dect) * sin0 + Math.cos(dect) * cos0 * Math.cos(rat);
            if (Math.abs(mg) < deps) return null;
            mg = 2.0 * (Math.sin(dect) * cos0 - Math.cos(dect) * sin0 * Math.cos(rat)) / mg;
            if (Math.abs(mg - m) > deps) rat = twopi / 2.0 - rat;
            rat = ra0 + rat;
            break;
        }

        /*  return ra in range  */
        raout = rat;
        decout = dect;
        if (raout - ra0 > twopi / 2.0) raout = raout - twopi;
        if (raout - ra0 < -twopi / 2.0) raout = raout + twopi;
        if (raout < 0.0) raout += twopi; /* added by DCW 10/12/94 */

        /*  correct units back to degrees  */
        xpos = wcs.raddeg(raout);
        ypos = wcs.raddeg(decout);

        return new Point2D.Double(xpos, ypos);
    }  /* End of worldpos */

    /*-----------------------------------------------------------------------*/
    /* routine to determine accurate pixel coordinates for an RA and Dec     */
    /* returns 0 if successful otherwise:                                    */
    /*  1 = angle too large for projection;                                  */
    /*  2 = bad values                                                       */
    /* does: -SIN, -TAN, -ARC, -NCP, -GLS, -MER, -AIT projections            */
    /* anything else is linear                                               */
    /* Input: */
    /* x (RA) coordinate (deg) */
    /* y (dec) coordinate (deg) */
    /* Output: */
    /* x pixel number  (RA or long without rotation) */
    /* y pixel number  (dec or lat without rotation) */
    public static Point2D.Double getPixels(double xpos, double ypos, WCSTransform wcs) {
        double dx = 0.0, dy = 0.0, ra0 = 0.0, dec0 = 0.0, ra = 0.0, dec = 0.0, coss = 0.0, sins = 0.0, dt = 0.0, da = 0.0, dd = 0.0, sint = 0.0;
        double l = 0.0, m = 0.0, geo1, geo2, geo3, sinr, cosr, tx;
        double cond2r = 1.745329252e-2, deps = 1.0e-5, twopi = 6.28318530717959;

        /* Structure elements */
        double xref;		/* x reference coordinate value (deg) */
        double yref;		/* y reference coordinate value (deg) */
        double xrefpix;	/* x reference pixel */
        double yrefpix;	/* y reference pixel */
        double xinc;		/* x coordinate increment (deg) */
        double yinc;		/* y coordinate increment (deg) */
        double rot;		/* Optical axis rotation (deg)  (from N through E) */
        double mrot;		/* Chip rotation (deg)  (from N through E) */
        int itype;

        /* Set local projection parameters */
        xref = wcs.xref;
        yref = wcs.yref;
        xrefpix = wcs.xrefpix;
        yrefpix = wcs.yrefpix;
        xinc = wcs.xinc;
        yinc = wcs.yinc;
        rot = wcs.degrad(wcs.rot);
        cosr = Math.cos(rot);
        sinr = Math.sin(rot);

        /* Projection type */
        itype = wcs.pcode;

        /* Nonlinear position */
        if (itype > 0 && itype < 9) {
            if (wcs.coorflip > 0) {
                dec0 = wcs.degrad(xref);
                ra0 = wcs.degrad(yref);
                dt = xpos - yref;
            }
            else {
                ra0 = wcs.degrad(xref);
                dec0 = wcs.degrad(yref);
                dt = xpos - xref;
            }

            /* 0h wrap-around tests added by D.Wells 10/12/94: */
            if (itype >= 0) {
                if (dt > 180.0) xpos -= 360.0;
                if (dt < -180.0) xpos += 360.0;
                /* NOTE: changing input argument xpos is OK (call-by-value in C!) */
            }

            ra = wcs.degrad(xpos);
            dec = wcs.degrad(ypos);

            /* Compute direction cosine */
            coss = Math.cos(dec);
            sins = Math.sin(dec);
            l = Math.sin(ra - ra0) * coss;
            sint = sins * Math.sin(dec0) + coss * Math.cos(dec0) * Math.cos(ra - ra0);
        }

        /* Process by case  */
        switch (itype) {
        case 1:   /* -SIN sin*/
            if (sint < 0.0) return null;
            m = sins * Math.cos(dec0) - coss * Math.sin(dec0) * Math.cos(ra - ra0);
            break;

        case 2:   /* -TAN tan */
            if (sint <= 0.0) return null;
            m = sins * Math.sin(dec0) + coss * Math.cos(dec0) * Math.cos(ra - ra0);
            l = l / m;
            m = (sins * Math.cos(dec0) - coss * Math.sin(dec0) * Math.cos(ra - ra0)) / m;
            break;

        case 3:   /* -ARC Arc*/
            m = sins * Math.sin(dec0) + coss * Math.cos(dec0) * Math.cos(ra - ra0);
            if (m < -1.0) m = -1.0;
            if (m > 1.0) m = 1.0;
            m = Math.acos(m);
            if (m != 0)
                m = m / Math.sin(m);
            else
                m = 1.0;
            l = l * m;
            m = (sins * Math.cos(dec0) - coss * Math.sin(dec0) * Math.cos(ra - ra0)) * m;
            break;

        case 4:   /* -NCP North celestial pole*/
            if (dec0 == 0.0)
                return null;  /* can't stand the equator */
            else
                m = (Math.cos(dec0) - coss * Math.cos(ra - ra0)) / Math.sin(dec0);
            break;

        case 5:   /* -GLS global sinusoid */
            dt = ra - ra0;
            if (Math.abs(dec) > twopi / 4.0) return null;
            if (Math.abs(dec0) > twopi / 4.0) return null;
            m = dec - dec0;
            l = dt * coss;
            break;

        case 6:   /* -MER mercator*/
            dt = yinc * cosr + xinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dy = wcs.degrad(yref / 2.0 + 45.0);
            dx = dy + dt / 2.0 * cond2r;
            dy = Math.log(Math.tan(dy));
            dx = Math.log(Math.tan(dx));
            geo2 = wcs.degrad(dt) / (dx - dy);
            geo3 = geo2 * dy;
            geo1 = Math.cos(wcs.degrad(yref));
            if (geo1 <= 0.0) geo1 = 1.0;
            dt = ra - ra0;
            l = geo1 * dt;
            dt = dec / 2.0 + twopi / 8.0;
            dt = Math.tan(dt);
            if (dt < deps) return null;
            m = geo2 * Math.log(dt) - geo3;
            break;

        case 7:   /* -AIT Aitoff*/
            l = 0.0;
            m = 0.0;
            da = (ra - ra0) / 2.0;
            if (Math.abs(da) > twopi / 4.0) return null;
            dt = yinc * cosr + xinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dt = wcs.degrad(dt);
            dy = wcs.degrad(yref);
            dx = Math.sin(dy + dt) / Math.sqrt((1.0 + Math.cos(dy + dt)) / 2.0) -
                    Math.sin(dy) / Math.sqrt((1.0 + Math.cos(dy)) / 2.0);
            if (dx == 0.0) dx = 1.0;
            geo2 = dt / dx;
            dt = xinc * cosr - yinc * sinr;
            if (dt == 0.0) dt = 1.0;
            dt = wcs.degrad(dt);
            dx = 2.0 * Math.cos(dy) * Math.sin(dt / 2.0);
            if (dx == 0.0) dx = 1.0;
            geo1 = dt * Math.sqrt((1.0 + Math.cos(dy) * Math.cos(dt / 2.0)) / 2.0) / dx;
            geo3 = geo2 * Math.sin(dy) / Math.sqrt((1.0 + Math.cos(dy)) / 2.0);
            dt = Math.sqrt((1.0 + Math.cos(dec) * Math.cos(da)) / 2.0);
            if (Math.abs(dt) < deps) return null;
            l = 2.0 * geo1 * Math.cos(dec) * Math.sin(da) / dt;
            m = geo2 * Math.sin(dec) / dt - geo3;
            break;

        case 8:   /* -STG Sterographic*/
            da = ra - ra0;
            if (Math.abs(dec) > twopi / 4.0) return null;
            dd = 1.0 + sins * Math.sin(dec0) + coss * Math.cos(dec0) * Math.cos(da);
            if (Math.abs(dd) < deps) return null;
            dd = 2.0 / dd;
            l = l * dd;
            m = dd * (sins * Math.cos(dec0) - coss * Math.sin(dec0) * Math.cos(da));
            break;
        }  /* end of itype switch */

        /* Back to degrees  */
        if (itype > 0 && itype < 9) {
            dx = wcs.raddeg(l);
            dy = wcs.raddeg(m);
        }
        /* For linear or pixel projection */
        else {
            dx = xpos - xref;
            dy = ypos - yref;
        }

        if (wcs.coorflip > 0) {
            tx = dx;
            dx = dy;
            dy = tx;
        }

        /* Scale and rotate using CD matrix */
        if (wcs.rotmat > 0) {
            tx = dx * wcs.dc11 + dy * wcs.dc12;
            dy = dx * wcs.dc21 + dy * wcs.dc22;
            dx = tx;
        }
        else {
            /* Correct for rotation */
            if (rot != 0.0) {
                tx = dx * cosr + dy * sinr;
                dy = dy * cosr - dx * sinr;
                dx = tx;
            }

            /* Scale using CDELT */
            if (xinc != 0.)
                dx = dx / xinc;
            if (yinc != 0.)
                dy = dy / yinc;
        }

        /* Convert to pixels  */
        double xpix = dx + xrefpix;
        double ypix = dy + yrefpix;

        return new Point2D.Double(xpix, ypix);
    }  /* end worldpix */

    /* Oct 26 1995	Fix bug which interchanged RA and Dec twice when coorflip
     * Oct 31 1996	Fix CD matrix use in WORLDPIX
     * Nov  4 1996	Eliminate extra code for linear projection in WORLDPIX
     * Nov  5 1996	Add coordinate flip in WORLDPIX
     *
     * May 22 1997	Avoid angle wraparound when CTYPE is pixel
     * Jun  4 1997	Return without angle conversion from worldpos if type is PIXEL
     * Oct 20 1997	Add chip rotation; compute rotation angle trig functions
     * Feb  6 1998	Move coordinate exchange to correct place
     * Feb  6 1998	Drop chip rotation; more CD->rotation to WCSINIT()
     */
}
