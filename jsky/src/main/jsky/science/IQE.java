/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: IQE.java,v 1.4 2002/08/20 09:57:58 brighton Exp $
 *
 * Original C header comment:
 *
 * .COPYRIGHT  (c)  1995  European Southern Observatory
 * .IDENT      iqefunc.c
 * .LANGUAGE   C
 * .AUTHOR     P.Grosbol,  IPG/ESO
 * .KEYWORDS   Image Quality Estimate, PSF
 * .PURPOSE    Routines for Image Quality Estimate
 *  holds
 *  iqe, iqebgv, iqemnt, iqesec, iqefit
 * .VERSION    1.0  1995-Mar-16 : Creation, PJG
 * .VERSION    1.1  1995-Jun-22 : Correct derivatives in 'g2efunc', PJG
 */

package jsky.science;


/**
 * Estimate parameters for the Image Quality using a small frame around the object.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton (Java port), P.Grosbol (ESO, original C version)
 */
public class IQE implements MrqFit.MrqFunc {

    /** constant 0.5*sqrt(2) */
    private static final double _HSQ2 = 0.7071067811865475244;

    /** Sigma to FWHM constant */
    private static final double _S2F = 2.0 * Math.sqrt(2.0 * Math.log(2.0));

    /** Radian to Degrees      */
    private static final double _R2D = 45.0 / Math.atan(1.0);

    /* No. of variables */
    private static final int _MA = 6;

    /* Max. no. of iterations */
    private static final int _MITER = 64;


    /** The center X position in image coordinates */
    private double _meanX;

    /** The center Y position in image coordinates */
    private double _meanY;

    /** FWHM in X */
    private double _fwhmX;

    /** FWHM in Y */
    private double _fwhmY;

    /** angle of major axis, degrees, along X */
    private double _angle;

    /** peak value of object above background */
    private double _objectPeak;

    /** mean background level */
    private double _meanBackground;

    /** Status of constructor (true is okay) */
    private boolean _status = true;

    /** Used in the mrq callback */
    private int _mp, _mx;
    private int _pwght;
    private float[] _pval;
    private double[] _w = new double[9];
    private double[] _xi = new double[9];
    private double[] _yi = new double[9];


    /**
     * Estimates parameters for the Image Quality using a small frame
     * around the object. The results may be accessed through the
     * public methods in this class (getMeanX(), getMeanY(), etc...).
     *
     * @param pfm array containing the image data in float format
     * @param mx no. of pixels in x
     * @param my no. of pixels in y
     */
    public IQE(float[] pfm, int mx, int my) {
        // used as reference parameters below
        int[] nbg = new int[1];
        float[] bgv = new float[1];
        float[] bgs = new float[1];
        float[] ap = new float[_MA];
        float[] cv = new float[_MA];
        float[] est = new float[_MA];
        float[] sec = new float[_MA];

        if (_iqebgv(pfm, mx, my, bgv, bgs, nbg) != 0) {
            _status = false;
            return;
        }
        _meanBackground = bgv[0];

        if (_iqemnt(pfm, mx, my, bgv[0], bgs[0], est) != 0) {
            _status = false;
            return;
        }
        _meanX = est[1];
        _fwhmX = _S2F * est[2];
        _meanY = est[3];
        _fwhmY = _S2F * est[4];
        _objectPeak = est[0];

        if (_iqesec(pfm, mx, my, bgv, est, sec) != 0) {
            _status = false;
            return;
        }
        _angle = _R2D * sec[5];

        if (_iqefit(pfm, mx, my, bgv, sec, ap, cv) < 0) {
            _status = false;
            return;
        }
        _meanX = ap[1];
        _fwhmX = _S2F * ap[2];
        _meanY = ap[3];
        _fwhmY = _S2F * ap[4];
        _angle = (_R2D * ap[5] + 180.0) % 180.0; // XXX: allan: note that image will be displayed with Y axis reversed...
        _objectPeak = ap[0];
    }


    /** The center X position in image coordinates */
    public double getMeanX() {
        return _meanX;
    }

    /** The center Y position in image coordinates */
    public double getMeanY() {
        return _meanY;
    }

    /** FWHM in X */
    public double getFwhmX() {
        return _fwhmX;
    }

    /** FWHM in Y */
    public double getFwhmY() {
        return _fwhmY;
    }

    /** angle of major axis, degrees, along X */
    public double getAngle() {
        return _angle;
    }

    /** peak value of object above background */
    public double getObjectPeak() {
        return _objectPeak;
    }

    /** mean background level */
    public double getMeanBackground() {
        return _meanBackground;
    }

    /** Return the status of the constructor */
    public boolean getStatus() {
        return _status;
    }


    /**
     * Estimate background level for subimage
     *
     * @param pfm (in)  array containing the image data in float format
     * @param mx  (in)  no. of pixels in x
     * @param my  (in)  no. of pixels in y
     * @param bgm (out) mean background level
     * @param bgs (out)
     * @param nbg (out)
     * @return status, 0:OK, -1:out of range
     */
    private int _iqebgv(float[] pfm, int mx, int my, float[] bgm, float[] bgs, int[] nbg) {
        int n, m, ns, ms, nt, mt;
        float[] pfb;
        int pwb, pf, pw;
        int pf0, pf1, pf2, pf3, pfs0, pfs1, pfs2, pfs3;
        int pw0, pw1, pw2, pw3, pws0, pws1, pws2, pws3;
        double val, fks, ba, bm, bs;

        bgm[0] = 0.0F;
        bgs[0] = 0.0F;
        nbg[0] = 0;

        pfs0 = 0;
        pfs1 = mx - 1;
        pfs2 = mx * (my - 1);
        pfs3 = mx * my - 1;

        ns = (mx < my) ? mx - 1 : my - 1;
        ms = (mx < my) ? mx / 4 : my / 4;
        pfb = new float[8 * ns * ms];
        pwb = 4 * ns * ms;

        /* extrat edge of matrix from each corner  */

        nt = 0;
        pf = 0;
        pw = pwb;
        for (m = 0; m < ms; m++) {
            pf0 = pfs0;
            pf1 = pfs1;
            pf2 = pfs2;
            pf3 = pfs3;
            for (n = 0; n < ns; n++) {
                pfb[pf++] = pfm[pf0++];
                pfb[pf++] = pfm[pf1];
                pf1 += mx;
                pfb[pf++] = pfm[pf2];
                pf2 -= mx;
                pfb[pf++] = pfm[pf3--];
            }
            nt += 4 * ns;
            ns -= 2;
            pfs0 += mx + 1;
            pfs1 += mx - 1;
            pfs2 -= mx - 1;
            pfs3 -= mx + 1;
        }

        /*  skip all elements with zero weight and sort clean array */

        pf = pf0 = 0;
        pw = pwb;
        n = nt;
        mt = nt;
        while (n-- > 0)
            pfb[pw++] = 1.0F;
        _hsort(mt, pfb);
        nt = mt;

        /* first estimate of mean and rms   */

        m = mt / 2;
        n = mt / 20;
        ba = pfb[m];
        bs = 0.606 * (ba - pfb[n]);               /*  5% point at 1.650 sigma */
        if (bs <= 0.0)
            bs = Math.sqrt(Math.abs(ba));     /* assume sigma of Poisson dist. */
        bgm[0] = (float) ba;

        /* then do 5 loops kappa sigma clipping  */

        for (m = 0; m < 5; m++) {
            pf = 0;
            pw = pwb;
            fks = 5.0 * bs;
            bm = bs = 0.0;
            mt = 0;
            for (n = 0; n < nt; n++, pw++) {
                val = pfb[pf++];
                if (0.0 < pfb[pw] && Math.abs(val - ba) < fks) {
                    bm += val;
                    bs += val * val;
                    mt++;
                }
                else
                    pfb[pw] = 0.0F;
            }
            if (mt < 1)
                return -2;

            ba = bm / mt;
            bs = bs / mt - ba * ba;
            bs = (0.0 < bs) ? Math.sqrt(bs) : 0.0F;
        }

        /* set return values and clean up     */

        bgm[0] = (float) ba;
        bgs[0] = (float) bs;
        nbg[0] = mt;

        return 0;
    }


    /**
     * Find center of object and do simple moment analysis
     *
     * @param pfm (in)  array containing the image data in float format
     * @param mx  (in)  no. of pixels in x
     * @param my  (in)  no. of pixels in y
     * @param bgm (in)  mean background level
     * @param bgs (in)
     * @param amm (out) amm[0] = amplitude over background,
     *                  amm[1] = X center,
     *                  amm[2] = X sigma,
     *                  amm[3] = Y center,
     *                  amm[4] = Y sigma,
     *                  amm[5] = angle of major axis
     * @return status, 0:OK, -1:out of range
     */
    private int _iqemnt(float[] pfm, int mx, int my, float bgv, float bgs, float[] amm) {
        int n, nx, ny, nt, nxc, nyc, ndx = 0, ndy = 0, ioff;
        int k, ki, ks, kn;
        float[] av = new float[1], dx = new float[1], dy = new float[1];
        int pf, pw;
        double val, x, y, dv, xc, yc, xm, ym;
        double am, ax, ay, axx, ayy, axy;

        dv = 5.0 * bgs;
        xm = mx - 1.0;
        ym = my - 1.0;
        for (nx = 0; nx < 6; nx++)
            amm[nx] = 0.0F;

        /* get approx. center of object by going up along the gradient    */

        n = nx = ny = 1;
        nxc = mx / 2;
        nyc = my / 2;
        nt = (nxc < nyc) ? nxc : nyc;
        while (nt-- > 0) {
            if (_estm9p(pfm, mx, my, nxc, nyc, av, dx, dy) != 0)
                break;

            if (n != 0)
                n = 0;
            else {
                if (dx[0] * ndx < 0.0)
                    nx = 0;
                if (dy[0] * ndy < 0.0)
                    ny = 0;
            }
            if (nx == 0 && ny == 0)
                break;

            ndx = (0.0 < dx[0]) ? nx : -nx;
            ndy = (0.0 < dy[0]) ? ny : -ny;
            nxc += ndx;
            nyc += ndy;
        }

        /* then try a simple moment of pixels above 5 sigma  */

        y = 0.0;
        nt = 0;
        ny = my;
        pf = 0;
        ax = ay = 0.0;
        while (ny-- > 0) {
            x = 0.0;
            nx = my;
            while (nx-- > 0) {
                val = pfm[pf++] - bgv;
                if (dv < val) {
                    ax += x;
                    ay += y;
                    nt++;
                }
                x += 1.0;
            }
            y += 1.0;
        }
        if (nt < 1)
            return -1;
        nx = (int) Math.floor(ax / nt);
        ny = (int) Math.floor(ay / nt);
        val = pfm[nx + mx * ny];
        if (av[0] < val) {
            /* the higher peak wins  */
            nxc = nx;
            nyc = ny;
        }

        /* finally, compute moments just around this position  */

        nt = 0;
        nx = 1;
        x = nxc;
        y = nyc;
        ioff = nxc + mx * nyc;
        n = (mx < my) ? mx - 1 : my - 1;
        pf = ioff;

        val = pfm[pf] - bgv;
        am = val;
        ax = val * x;
        ay = val * y;
        axx = val * x * x;
        ayy = val * y * y;
        axy = val * x * y;
        nt++;

        ki = ks = kn = 1;
        while (n-- > 0) {
            k = kn;
            if (ki == 0 && ks == -1) {
                if (nx != 0)
                    nx = 0;
                else
                    break;
            }
            ioff = (ki != 0) ? ks : ks * mx;
            while (k-- > 0) {
                if (ki != 0)
                    x += ks;
                else
                    y += ks;
                if (x < 0.0 || y < 0.0 || xm < x || ym < y)
                    break;
                pf += ioff;
                try {
                    val = pfm[pf] - bgv; // XXX array bounds?
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    //System.out.println("XXX pfm.length = " + pfm.length + ", pf = " + pf);
                    //e.printStackTrace();
                    //throw e;
                    return -1;
                }
                if (dv < val) {
                    am += val;
                    ax += val * x;
                    ay += val * y;
                    axx += val * x * x;
                    ayy += val * y * y;
                    axy += val * x * y;
                    nt++;
                    nx++;
                }
            }
            if (ki != 0) {
                ki = 0;
            }
            else {
                ki = 1;
                ks = -ks;
                kn++;
            }
        }
        if (am <= 0.0)
            return -1;

        /* normalize the moments and put them in to the output array   */

        amm[1] = (float) (ax / am);
        amm[3] = (float) (ay / am);
        axx = axx / am - amm[1] * amm[1];
        amm[2] = (float) ((0.0 < axx) ? Math.sqrt(axx) : 0.0F);
        ayy = ayy / am - amm[3] * amm[3];
        amm[4] = (float) ((0.0 < ayy) ? Math.sqrt(ayy) : 0.0F);
        axy = (axy / am - amm[1] * amm[3]) / axx;
        amm[5] = (float) ((Math.atan(axy) + 4.0 * Math.atan(1.0)) % (4.0 * Math.atan(1.0)));
        nx = (int) amm[1];
        ny = (int) amm[3];
        amm[0] = pfm[nx + ny * mx] - bgv;

        return 0;
    }


    /*
     * Estimate parameters for 3x3 pixel region.
     *
     * @param pfm (in)  array containing the image data in float format
     * @param mx  (in)  no. of pixels in x
     * @param my  (in)  no. of pixels in y
     * @param nx  (in)
     * @param ny  (in)
     * @param rm  (out)
     * @param dx  (out)
     * @param dy  (out)
     * @return status, 0:OK, -1:out of range
     */
    private int _estm9p(float[] pfm, int mx, int my, int nx, int ny, float[] rm, float[] dx, float[] dy) {
        int pf = nx - 1 + mx * (ny - 1);
        int n, nt, ix, iy = 3;
        int[] idx = new int[9];
        float a, am, ss;
        float[] fb = new float[9];
        int pfb = 0;
        float[] wb = new float[9];
        int pwb = 0;

        /* check if 3x3 region is fully within frame   */

        if (nx < 1 || mx < nx - 2 || ny < 1 || my < ny - 2)
            return -1;

        /* extract region into local array and generate a rank index for it */

        while (iy-- > 0) {
            ix = 3;
            while (ix-- > 0) {
                try {
                    fb[pfb++] = pfm[pf++];  // XXX array bounds
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    //System.out.println("XXX fb.length = " + fb.length + ", pfb = " + pfb + ", pfm.length = " + pfm.length + ", pf = " + pf);
                    //e.printStackTrace();
                    //throw e;
                    return -1;
                }
                wb[pwb++] = 1.0F;
            }
            pf += mx - 3;
        }

        _indexx(9, fb, idx);

        /* omit largest value and estimate mean     */

        wb[idx[8]] = 0.0F;

        nt = 0;
        am = 0.0F;
        for (n = 0; n < 9; n++) {
            if (0.0 < wb[n]) {
                am += fb[n];
                nt++;
            }
        }
        rm[0] = am / nt;

        /* calculate mean gradient in X and Y */

        a = am = 0.0F;
        ix = iy = 0;
        for (n = 0; n < 9; n += 3) {
            if (0.0 < wb[n]) {
                a += fb[n];
                ix++;
            }
            if (0.0 < wb[n + 2]) {
                am += fb[n + 2];
                iy++;
            }
        }
        dx[0] = (float) (0.5 * (am / iy - a / ix));

        a = am = 0.0F;
        ix = iy = 0;
        for (n = 0; n < 3; n++) {
            if (0.0 < wb[n]) {
                a += fb[n];
                ix++;
            }
            if (0.0 < wb[n + 6]) {
                am += fb[n + 6];
                iy++;
            }
        }

        dy[0] = (float) (0.5 * (am / iy - a / ix));

        return 0;
    }


    /*
     * Perform a sector analysis of object. Estimates for center and
     * size are given in 'est' which is used for bootstrap.
     *
     * @param pfm (in)  array containing the image data in float format
     * @param mx  (in)  no. of pixels in x
     * @param my  (in)  no. of pixels in y
     * @param bgv (in)
     * @param est (out) set like the sec parameter
     * @param sec (out) sec[0] = amplitude over background,
     *                  sec[1] = X center,
     *                  sec[2] = X sigma,
     *                  sec[3] = Y center,
     *                  sec[4] = Y sigma,
     *                  sec[5] = angle of major axis
     *
     * @return    status, 0:OK, -1: no buffer,
     */
    private int _iqesec(float[] pfm, int mx, int my, float[] bgv, float[] est, float[] sec) {
        int n, ix, iy, nx, ny, nxs, nys;
        int k, ki, ks, kn, nxc, nyc, ioff, idx;
        int[] ns = new int[8];
        int pf, pw, f;
        double x, y, xm, ym, xc, yc, fac, dx, dy;
        double r, rl, rh, a1r, a1i, a2r, a2i;
        double[] sb = new double[8];

        /* initiate basic variables    */

        fac = 1.0 / Math.atan(1.0);
        for (n = 0; n < 6; n++)
            sec[n] = 0.0F;
        for (n = 0; n < 8; n++) {
            sb[n] = 0.0;
            ns[n] = 0;
        }
        xc = x = est[1];
        xm = mx - 1.0;
        yc = y = est[3];
        ym = my - 1.0;
        if (est[2] < est[4]) {
            rl = 2.0 * est[2];
            rh = 4.0 * est[4];
            n = (int) Math.ceil(16.0 * est[4]);
        }
        else {
            rl = 2.0 * est[4];
            rh = 4.0 * est[2];
            n = (int) Math.ceil(16.0 * est[2]);
        }

        /* extract the sectors around the center of the object  */

        nxc = (int) Math.floor(x + 0.5);
        nyc = (int) Math.floor(y + 0.5);
        ioff = nxc + mx * nyc;
        pf = ioff;
        pw = ioff;

        ki = ks = kn = 1;
        while (n-- > 0) {
            k = kn;
            ioff = (ki != 0) ? ks : ks * mx;
            while (k-- > 0) {
                if (ki != 0)
                    x += ks;
                else
                    y += ks;
                if (x < 0.0 || y < 0.0 || xm < x || ym < y)
                    break;
                pf += ioff;
                pw += ioff;
                dx = x - xc;
                dy = y - yc;
                r = Math.sqrt(dx * dx + dy * dy);
                if (rl < r && r < rh && pf >= 0 && pf < pfm.length) { // allan: added bounds check
                    f = (int) (pfm[pf] - bgv[0]);
                    idx = ((int) (fac * Math.atan2(y - yc, x - xc) + 8.5)) % 8;
                    sb[idx] += (0.0 < f) ? f : 0.0;
                    ns[idx]++;
                }
            }
            if (ki != 0) {
                ki = 0;
            }
            else {
                ki = 1;
                ks = -ks;
                kn++;
            }
        }

        /* normalize the sector array and do explicit FFT for k=1,2  */

        for (n = 0; n < 8; n++) {
            if (ns[n] < 1)
                ns[n] = 1;
            sb[n] /= ns[n];
        }

        a1r = sb[0] + _HSQ2 * sb[1] - _HSQ2 * sb[3] - sb[4] - _HSQ2 * sb[5] + _HSQ2 * sb[7];
        a1i = _HSQ2 * sb[1] + sb[2] + _HSQ2 * sb[3] - _HSQ2 * sb[5] - sb[6] - _HSQ2 * sb[7];
        a2r = sb[0] - sb[2] + sb[4] - sb[6];
        a2i = sb[1] - sb[3] + sb[5] - sb[7];

        for (n = 0; n < 6; n++)
            sec[n] = est[n];        /* copy estimates over  */
        if (a2r == 0.0 && a2i == 0.0)
            return -2;
        sec[5] = (float) ((0.5 * Math.atan2(a2i, a2r)) % (4.0 / fac));

        return 0;
    }

    /*
     * Fit 2D Gaussian function to PSF
     *
     * @param pfm (in)  array containing the image data in float format
     * @param mx  (in)  no. of pixels in x
     * @param my  (in)  no. of pixels in y
     * @param bgv (in)
     * @param est (out) set like the est parameter
     * @param ap (out)  ap[0] = amplitude over background
                        ap[1] = X center,
			ap[2] = X sigma,
                        ap[3] = Y center,
			ap[4] = Y sigma,
                        ap[5] = angle of major axis
     * @param cm (out)
     *
     * @return no. of iterations, <0: error, -10: no buffer
     */

    private int _iqefit(float[] pfm, int mx, int my, float[] bgv, float[] est, float[] ap, float[] cm) {
        int fb, n, ix, iy, nx, ny, nxs, nys;
        float[] pfb;
        int pwb, pf, pw;
        double[] chi = new double[1];

        /* initialize basic variables    */

        for (n = 0; n < 6; n++)
            ap[n] = cm[n] = 0.0F;


        /* allocate buffer for a 4 sigma region around the object */

        nxs = (int) (Math.floor(est[1] - 4.0 * est[2]));
        if (nxs < 0)
            nxs = 0;
        nys = (int) (Math.floor(est[3] - 4.0 * est[4]));
        if (nys < 0)
            nys = 0;
        nx = (int) (Math.ceil(8.0 * est[2]));
        if (mx < nxs + nx)
            nx = my - nxs;
        ny = (int) (Math.ceil(8.0 * est[4]));
        if (my < nys + ny)
            ny = my - nys;

        pfb = new float[2 * nx * ny];
        pwb = nx * ny;


        /* extrac region from extranal buffer */

        fb = nxs + mx * nys;
        pf = 0;
        pw = pwb;
        iy = ny;

        while (iy-- > 0) {
            ix = nx;
            while (ix-- > 0) {
                pfb[pf++] = pfm[fb++] - bgv[0];
                pfb[pw++] = 1.0F;
            }
            fb += mx - nx;
        }


        /* initialize parameters for fitting    */

        ap[0] = est[0];
        ap[1] = est[1] - nxs;
        ap[2] = est[2];
        ap[3] = est[3] - nys;
        ap[4] = est[4];
        ap[5] = est[5];

        /* perform actual 2D Gauss fit on small subimage  */

        n = _g2efit(pfb, pwb, nx, ny, ap, cm, chi);

        /* normalize parameters and uncertainties, and exit   */

        ap[1] += nxs;
        ap[3] += nys;

        return n;
    }


    /*
     * This method is called back by the MrqFit class.
     * @return status,  0: OK, 1: error - bad pixel no.
     */
    public int mrqFunc(int idx, float[] val, float[] fval, float[] psig, float[] a, float[] dyda, int ma) {
        int n;
        double ff, fa, sum, ci, si;
        double xc, yc, xx, yy, x, y;
        double xm5, xp5, ym5, yp5;

        if (idx < 0 || _mp <= idx) return -1;                        // check index
        if (_pval != null && _pval[_pwght + idx] < 0.0) return 1;    // check if valid pixel
        if (a[2] <= 0.0 || a[4] <= 0.0) return -2;                   // negative sigmas

        xc = (double) (idx % _mx) - a[1];
        yc = (double) (idx / _mx) - a[3];

        xm5 = (double) (idx % _mx) - a[1] - 0.5;
        xp5 = xm5 + 1.0;
        ym5 = (double) (idx / _mx) - a[3] - 0.5;
        yp5 = ym5 + 1.0;

        val[0] = _pval[idx];
        psig[0] = (_pval != null) ? _pval[_pwght + idx] : 1.0F;
        si = Math.sin(a[5]);
        ci = Math.cos(a[5]);

        sum = 0.0;
        for (n = 0; n < 9; n++) {
            x = xc + _xi[n];
            y = yc + _yi[n];
            xx = (ci * x + si * y) / a[2];
            yy = (-si * x + ci * y) / a[4];
            sum += _w[n] * Math.exp(-0.5 * (xx * xx + yy * yy));
        }
        xx = (ci * xc + si * yc) / a[2];
        yy = (-si * xc + ci * yc) / a[4];

        ff = a[0] * sum;
        fval[0] = (float) ff;

        dyda[0] = (float) sum;
        dyda[1] = (float) (ff * (ci * xx / a[2] - si * yy / a[4]));
        dyda[2] = (float) (ff * xx * xx / a[2]);
        dyda[3] = (float) (ff * (si * xx / a[2] + ci * yy / a[4]));
        dyda[4] = (float) (ff * yy * yy / a[4]);
        dyda[5] = (float) (ff * ((si * xc - ci * yc) * xx / a[2] + (ci * xc + si * yc) * yy / a[4]));

        return 0;
    }


    /*
     * Perform 2D Gauss fit.
     * @return status,  no. of iterations, else  -1: error - bad pixel no,
     */
    private int _g2efit(float[] val, int wght, int nx, int ny, float[] ap, float[] cv, double[] pchi) {
        int mt, n, na, ni;
        int[] lista = new int[_MA];
        float[] apo = new float[_MA];
        double c2, a2, pi;
        double[] a1 = new double[1];
        double[] alpha = new double[_MA * _MA], cvm = new double[_MA * _MA];
        double fh, w1, w2, w3;

        // Initialize gauss fit function, set pointers to data and weights
        if (nx < 1) {                     // if NO x-pixel set to NULL
            _pval = null;
            _pwght = 0;
            _mx = _mp = 0;
            return -1;
        }

        _mx = nx;
        _mp = (0 < ny) ? ny * nx : nx;
        _pwght = wght;
        _pval = val;

        fh = 0.5 * Math.sqrt(3.0 / 5.0);      // positions and weights for integration
        w1 = 16.0 / 81.0;
        w2 = 10.0 / 81.0;
        w3 = 25.0 / 324.0;

        _xi[0] = 0.0;
        _yi[0] = 0.0;
        _w[0] = w1;
        _xi[1] = 0.0;
        _yi[1] = fh;
        _w[1] = w2;
        _xi[2] = 0.0;
        _yi[2] = -fh;
        _w[2] = w2;
        _xi[3] = fh;
        _yi[3] = 0.0;
        _w[3] = w2;
        _xi[4] = -fh;
        _yi[4] = 0.0;
        _w[4] = w2;
        _xi[5] = fh;
        _yi[5] = fh;
        _w[5] = w3;
        _xi[6] = -fh;
        _yi[6] = fh;
        _w[6] = w3;
        _xi[7] = fh;
        _yi[7] = -fh;
        _w[7] = w3;
        _xi[8] = -fh;
        _yi[8] = -fh;
        _w[8] = w3;
        // end of gauss fit function initialization

        pi = 4.0 * Math.atan(1.0);
        a1[0] = -1.0;
        mt = nx * ny;
        for (n = 0; n < _MA; n++) {
            lista[n] = n;
            cv[n] = 0.0F;
        }

        pchi[0] = c2 = 0.0;
        a2 = 0.0;
        na = 0;
        for (ni = 0; ni < _MITER; ni++) {
            for (n = 0; n < _MA; n++)
                apo[n] = ap[n];
            if (MrqFit.mrqmin(mt, ap, _MA, lista, _MA, cvm, alpha, pchi, this, a1) != 0)
                return -2;
            if (a1[0] < a2 && Math.abs(pchi[0] - c2) < 1.0e-5 * c2)
                break;
            if (a1[0] < a2) {
                c2 = pchi[0];
                na = 0;
            }
            else
                na++;
            a2 = a1[0];
            if (5 < na)
                break;
            if (ap[0] <= 0.0)
                ap[0] = (float) (0.5 * apo[0]);
            if (ap[2] <= 0.0)
                ap[2] = (float) (0.5 * apo[2]);
            if (ap[4] <= 0.0)
                ap[4] = (float) (0.5 * apo[4]);
            ap[5] = (float) (ap[5] % pi);
            if (ap[1] < 0.0 || nx < ap[1] || ap[3] < 0.0 || ny < ap[3])
                return -3;
        }

        a1[0] = 0.0;
        if (MrqFit.mrqmin(mt, ap, _MA, lista, _MA, cvm, alpha, pchi, this, a1) != 0)
            return -2;

        ap[5] = (float) (ap[5] + pi % pi);
        for (n = 0; n < _MA; n++)
            cv[n] = (float) Math.sqrt(cvm[n + n * _MA]);

        return ((_MITER <= ni) ? -4 : ni);
    }


    /**
     * Sort array in place using heapsort.
     *
     * @param n no. of elements in array
     * @param ra pointer to array to be sorted
     */
    private void _hsort(int n, float[] ra) {
        int l, j, ir, i;
        float rra;

        l = n >> 1;
        ir = n - 1;

        while (true) {
            if (l > 0)
                rra = ra[--l];
            else {
                rra = ra[ir];
                ra[ir] = ra[0];
                if (--ir == 0) {
                    ra[0] = rra;
                    return;
                }
            }
            i = l;
            j = (l << 1) + 1;
            while (j <= ir) {
                if (j < ir && ra[j] < ra[j + 1]) ++j;
                if (rra < ra[j]) {
                    ra[i] = ra[j];
                    j += (i = j) + 1;
                }
                else
                    j = ir + 1;
            }
            ra[i] = rra;
        }
    }

    /**
     * compute indx[] so that arrin[indx[0..n]] is ascenting
     */
    private void _indexx(int n, float[] arrin, int[] indx) {
        int l, j, ir, indxt, i;
        float q;

        for (j = 0; j < n; j++) indx[j] = j;
        l = n >> 1;
        ir = n - 1;
        while (true) {
            if (l > 0) {
                indxt = indx[--l];
                q = arrin[indxt];
            }
            else {
                indxt = indx[ir];
                q = arrin[indxt];
                indx[ir] = indx[0];
                if (--ir == 0) {
                    indx[0] = indxt;
                    return;
                }
            }
            i = l;
            j = (l << 1) + 1;
            while (j <= ir) {
                if (j < ir && arrin[indx[j]] < arrin[indx[j + 1]])
                    j++;
                if (q < arrin[indx[j]]) {
                    indx[i] = indx[j];
                    j += (i = j) + 1;
                }
                else
                    break;
            }
            indx[i] = indxt;
        }
    }
}
