/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: MrqFit.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 *
 * Original C header comment:
 *
 * Copyright (C) 1995 European Southern Observatory (ESO)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Massachusetss Ave, Cambridge,
 * MA 02139, USA.
 *
 * Corresponding concerning ESO-MIDAS should be addressed as follows:
 *	Internet e-mail: midas@eso.org
 *	Postal address: European Southern Observatory
 *			Data Management Division
 *			Karl-Schwarzschild-Strasse 2
 *			D 85748 Garching bei Muenchen
 *			GERMANY
 *
 * .COPYRIGHT  (c)  1995  European Southern Observatory
 * .IDENT      mrqfit.c
 * .LANGUAGE   C
 * .AUTHOR     P.Grosbol,  IPG/ESO
 * .COMMENT    Algorithm taken from 'Numerical Recipes in C' s.14.4, p.545
 *             Combination of mrqmin() and mrqcof() with modified func().
 *             NOTE: Data arrays start  with index 0.
 *                 FORTRAN order> a[ir][ic] = a[ir+ic*n]
 * .KEYWORDS   Nonlinear Model fit
 * .VERSION    1.0  1994-May-23 : Creation, PJG
 * .VERSION    1.1  1995-Apr-29 : Correct problem when mfit!=ma, PJG
 */

package jsky.science;


/**
 * Nonlinear Model fit.
 * Algorithm taken from 'Numerical Recipes in C' s.14.4, p.545
 * Combination of mrqmin() and mrqcof() with modified func().
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton (Java port), P.Grosbol (ESO, original C version)
 */
public class MrqFit {

    /* Max. no. of variables */
    private static final int _MMA = 16;

    static float[] atry = new float[_MMA];
    static double[] da = new double[_MMA];
    static double[] oneda = new double[_MMA];
    static double[] beta = new double[_MMA];
    static double[] cv = new double[_MMA * _MMA];
    static double ochisq;

    /** The funcs argument must implement this interface */
    public interface MrqFunc {

        /*
	 * Evaluate function value for given index
	 * @return status,  0: OK, 1: error - bad pixel no.
	 */
        public int mrqFunc(int idx, float[] val, float[] fval, float[] psig, float[] a, float[] dyda, int ma);
    }


    /**
     * @return status,  0: OK, -1: Bad permutation LISTA 1,
     *                 -2: Bad permutation LISTA 2, -3: too many variables,
     *                 -4: No points (chisq<=0), -5: error in matrix inversion
     */
    public static int mrqmin(int ndata, float[] a, int ma, int[] lista, int mfit, double[] covar,
                             double[] alpha, double[] chisq, MrqFunc funcs, double[] alamda) {
        int k, kk, j, ihit;

        if (alamda[0] < 0.0) {
            if (_MMA < ma || ma < mfit)
                return -3;
            kk = mfit;
            for (j = 0; j < ma; j++) {
                ihit = 0;
                for (k = 0; k < mfit; k++)
                    if (lista[k] == j) ihit++;
                if (ihit == 0)
                    lista[kk++] = j;
                else if (ihit > 1)
                    return -1;
            }
            if (kk != ma)
                return -2;
            alamda[0] = 0.001;
            _mrqcof(ndata, a, ma, lista, mfit, alpha, beta, chisq, funcs);
            if (chisq[0] <= 0.0)
                return -4;
            ochisq = chisq[0];
        }

        for (j = 0; j < mfit; j++) {
            for (k = 0; k < mfit; k++)
                covar[j + k * ma] = cv[j + k * mfit] = alpha[j + k * ma];
            covar[j + j * ma] = cv[j + j * mfit] = alpha[j + j * ma] * (1.0 + alamda[0]);
            oneda[j] = beta[j];
        }

        if (_gaussj(cv, mfit, oneda, 1) != 0)
            return -5;
        for (j = 0; j < mfit; j++)
            da[j] = oneda[j];

        if (alamda[0] == 0.0) {
            for (j = 0; j < mfit; j++)
                for (k = 0; k < mfit; k++)
                    covar[j + k * ma] = cv[j + k * mfit];
            _covsrt(covar, ma, lista, mfit);
            return 0;
        }

        for (j = 0; j < ma; j++)
            atry[j] = a[j];
        for (j = 0; j < mfit; j++)
            atry[lista[j]] = (float) (a[lista[j]] + da[j]);

        _mrqcof(ndata, atry, ma, lista, mfit, covar, da, chisq, funcs);
        if (0.0 < chisq[0] && chisq[0] < ochisq) {
            alamda[0] *= 0.1;
            ochisq = chisq[0];
            for (j = 0; j < mfit; j++) {
                for (k = 0; k < mfit; k++)
                    alpha[j + k * ma] = covar[j + k * ma];
                beta[j] = da[j];
                a[lista[j]] = atry[lista[j]];
            }
        }
        else {
            alamda[0] *= 10.0;
            chisq[0] = ochisq;
        }

        return 0;
    }


    /**
     * compute covarient matrix and chisq from all values
     */
    private static void _mrqcof(int ndata, float[] a, int ma, int[] lista, int mfit,
                                double[] alpha, double[] veta, double[] chisq, MrqFunc funcs) {
        int k, j, i;
        float wt, dy;
        float[] y = new float[1];
        float[] ymod = new float[1];
        float[] sig2i = new float[1];
        float[] dyda = new float[_MMA];

        for (j = 0; j < mfit; j++) {
            for (k = 0; k <= j; k++) alpha[j + k * ma] = 0.0;
            veta[j] = 0.0;
        }

        chisq[0] = 0.0;
        for (i = 0; i < ndata; i++) {
            if (funcs.mrqFunc(i, y, ymod, sig2i, a, dyda, ma) != 0)
                continue;
            dy = y[0] - ymod[0];
            for (j = 0; j < mfit; j++) {
                wt = dyda[lista[j]] * sig2i[0];
                for (k = 0; k <= j; k++)
                    alpha[j + k * ma] += wt * dyda[lista[k]];
                veta[j] += dy * wt;
            }
            chisq[0] += dy * dy * sig2i[0];
        }

        for (j = 1; j < mfit; j++)
            for (k = 0; k < j; k++)
                alpha[k + j * ma] = alpha[j + k * ma];
    }

    /**
     * Compute covariance matrix.
     */
    private static void _covsrt(double[] covar, int ma, int[] lista, int mfit) {
        int i, j;
        double swap;

        for (j = 0; j < ma - 1; j++)
            for (i = j + 1; i < ma; i++) covar[i + j * ma] = 0.0;

        for (i = 0; i < mfit - 1; i++)
            for (j = i + 1; j < mfit; j++) {
                if (lista[j] > lista[i])
                    covar[lista[j] + lista[i] * ma] = covar[i + j * ma];
                else
                    covar[lista[i] + lista[j] * ma] = covar[i + j * ma];
            }

        swap = covar[0];
        for (j = 0; j < ma; j++) {
            covar[j * ma] = covar[j + j * ma];
            covar[j + j * ma] = 0.0;
        }

        covar[lista[0] + lista[0] * ma] = swap;
        for (j = 1; j < mfit; j++) covar[lista[j] + lista[j] * ma] = covar[j * ma];
        for (j = 1; j < ma; j++)
            for (i = 0; i < j; i++) covar[i + j * ma] = covar[j + i * ma];
    }


    /**
     * Swap two elements in an array
     */
    private static void _swap(double[] ar, int a, int b) {
        double temp = ar[a];
        ar[a] = ar[b];
        ar[b] = temp;
    }


    /**
     * Inverse matrix using Gauss-Jordan elimination
     *
     * @return status,  0: OK, -1: Singular matrix 1, -2: Singular matrix 2,
     *                 -3: matrix too big
     */
    private static int _gaussj(double[] a, int n, double[] b, int m) {
        int i, icol = 0, irow = 0, j, k, l, ll;
        int[] indxc = new int[_MMA], indxr = new int[_MMA], ipiv = new int[_MMA];
        double big, dum, pivinv;

        if (_MMA < n)
            return -3;
        for (j = 0; j < n; j++)
            ipiv[j] = 0;

        for (i = 0; i < n; i++) {
            big = 0.0;
            for (j = 0; j < n; j++)
                if (ipiv[j] != 1) {
                    for (k = 0; k < n; k++) {
                        if (ipiv[k] == 0) {
                            dum = Math.abs(a[j + k * n]);
                            if (dum >= big) {
                                big = dum;
                                irow = j;
                                icol = k;
                            }
                        }
                        else if (ipiv[k] > 1)
                            return -1;
                    }
                }

            ++(ipiv[icol]);
            if (irow != icol) {
                for (l = 0; l < n; l++)
                    _swap(a, irow + l * n, icol + l * n);
                for (l = 0; l < m; l++)
                    _swap(b, irow + l * n, icol + l * n);
            }

            indxr[i] = irow;
            indxc[i] = icol;
            if (a[icol + icol * n] == 0.0)
                return -2;
            pivinv = 1.0 / a[icol + icol * n];
            a[icol + icol * n] = 1.0;
            for (l = 0; l < n; l++)
                a[icol + l * n] *= pivinv;
            for (l = 0; l < m; l++)
                b[icol + l * n] *= pivinv;
            for (ll = 0; ll < n; ll++)
                if (ll != icol) {
                    dum = a[ll + icol * n];
                    a[ll + icol * n] = 0.0;
                    for (l = 0; l < n; l++) a[ll + l * n] -= a[icol + l * n] * dum;
                    for (l = 0; l < m; l++) b[ll + l * n] -= b[icol + l * n] * dum;
                }
        }

        for (l = n - 1; l >= 0; l--) {
            if (indxr[l] != indxc[l])
                for (k = 0; k < n; k++)
                    _swap(a, k + indxr[l] * n, k + indxc[l] * n);
        }

        return 0;
    }
}


