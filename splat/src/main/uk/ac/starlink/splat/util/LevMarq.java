package uk.ac.starlink.splat.util;

/**
 * Levenberg-Marquardt non-linear least squares fit class. To use this
 * class you need to create a class that implements the {@link LevMarqFunc}
 * interface. To see how to use this class have a look at one of the
 * classes that extend the {@link AbstractFunctionFitter} class.
 * <p>
 * <b>Note:</b> when entering data, use the FORTRAN array numbering
 * convention i.e.  point 1 is x[1] do NOT use the C/C++ convention
 * where the first point is x[0].
 *
 * @version $Id$
 * @since 03-JAN-2001
 * @author Peter W. Draper
 *
 * @see LevMarqFunc
 */
public class LevMarq
{
    /*
     * Special notes.
     * --------------
     * This version is a re-enginering of an implementation of the L-M
     * method from "Numerical Recipes" by:
     * <pre>
     *    Andrew Robinson
     *    Nanoscale Physics Research Laboratory
     *    School of Physics and Astronomy
     *    The University of Birmingham
     *    Edgbaston
     *    Birmingham B15 2TT
     *    U.K.
     *    A.W.Robinson@bham.ac.uk
     * </pre>
     * Changes made by P.W.Draper, are that the code has been
     * converted to use variable size parameters and position arrays
     * and has been converted to provide javadoc comments. The
     * function that is used to provide information about the data fit
     * (i.e.<!-- --> that evaluates the chi-square and partial
     * derivate values for a given model) has been moved into an
     * external class that implements the {@link LevMarqFunc}
     * interface.
     */

    // Local variables.
    private double a[];
    private double alambda;
    private double alpha[][];
    private double aold[];
    private double beta[];
    private double chisq;
    private double converge;
    private double covar[][];
    private double da[];
    private double dyda[];
    private double fit[];
    private double oldchisq;
    private double oneda[];
    private double sigma[];
    private double x[];
    private double y[];
    private int ia[];
    private int indexc[];
    private int indexr[];
    private int ipiv[];
    private int iterations = 20;
    private int ndata = 200;
    private int mfit;
    private int mparam = 20;

    private boolean converged = false;

    private LevMarqFunc funcs = null;

    /**
     *  Constructor. Data values are provided using the set methods
     * (individual values at time).
     *
     *  @param ndata number of points that will be used.
     *  @param mparam number of fitting parameters.
     */
    public LevMarq( LevMarqFunc funcs, int ndata, int mparam )
    {
        setFunc( funcs );
        initLocalArrays( ndata, mparam );

        //  Initialise local variables.
        mfit = mparam;
        converge = 0.999;
    }

    /**
     * Set the function class that evaluates the model and partial
     * derivatives (this must implement the LevMarqFunc interface).
     */
    public void setFunc( LevMarqFunc funcs )
    {
        this.funcs = funcs;
    }

    /**
     *  Allocate local arrays. Any existing data areas are cleared.
     *
     *  @param ndata number of points that will be used.
     *  @param mparam number of fitting parameters.
     */
    public void initLocalArrays( int ndata, int mparam )
    {
        setNumData( ndata );
        setNumParams( mparam );
    }

    /**
     *  Sets the x data point x[n] to the value xin.
     */
    public void setX( int n, double xin )
    {
        x[n] = xin;
    }

    /**
     *  Sets the y data point y[n] to the value yin.
     */
    public void setY( int n, double yin )
    {
        y[n] = yin;
    }

    /**
     *  Sets the variance sigma[n] in the data to sigin.
     */
    public void setSigma( int n, double sigin )
    {
        sigma[n] = sigin;
    }

    /**
     *  Returns the value of the x data at point n.
     */
    public double getX( int n )
    {
        return x[n];
    }

    /**
     *  Returns the value of the y data at point n.
     */
    public double getY( int n )
    {
        return y[n];
    }

    /**
     *  Returns the value of the fitted data at point n.
     */
    public double getFit( int n )
    {
        return fit[n];
    }

    /**
     *  Returns the value of the variance at point n.
     */
    public double getSigma( int n )
    {
        return sigma[n];
    }

    /**
     *  Puts value f into parameter n.
     */
    public void setParam( int n, double f )
    {
        if ( n <= mparam ) a[n] = f;

    }

    /**
     *  Puts value f into parameter n and sets the floating/fixed attribute.
     *
     *  Use nf = 0 for fixed, nf = 1 for floating. Defaults to fixed
     *  in the constructor
     */
    public void setParam( int n, double f, int nf )
    {
        if ( n <= mparam ) {
            a[n] = f;
            if( nf == 0 ) {
                ia[n] = 0;      // fix the parameter with value f
            }
            else {
                ia[n] = 1;      // parameter has initial value f,
                                // but can float during fit
            }
        }
    }

    /**
     *  Puts value f into parameter n and sets the floating/fixed attribute.
     */
    public void setParam( int n, double f, boolean fixed )
    {
        if ( n <= mparam ) {
            a[n] = f;
            if( fixed ) {
                ia[n] = 0;      // fix the parameter with value f
            }
            else {
                ia[n] = 1;      // parameter has initial value f,
                                // but can float during fit
            }
        }
    }

    /**
     *  Returns the value of the nth parameter.
     */
    public double getParam( int n )
    {
        if ( n <= mparam ) {
            return a[n];
        }
        else {
            return 0;
        }
    }

    /**
     *  Returns the total number of parameters.
     */
    public int getNumParams()
    {
        return mparam;
    }

    /**
     * Sets the total number of parameters. Resets any existing
     * parameter based workspace.
     */
    public void setNumParams( int n )
    {
        mparam = n + 1; // Index starts at 1 not 0.

        a = new double[mparam];
        aold= new double[mparam];
        alpha = new double[mparam][mparam];
        beta = new double[mparam];
        da = new double[mparam];
        ia  = new int[mparam];
        for ( int i = 0; i < mparam; i++ ) {
            ia[i] = 1;  //  Parameters are floating by default.
        }
        oneda = new double[mparam];
        dyda =new double[mparam];
        covar = new double[mparam][mparam];
        ipiv = new int[mparam];
        indexr = new int[mparam];
        indexc = new int[mparam];

        mparam = n;
    }

    /**
     *  Returns true if nth parameter is floating, false otherwise.
     */
    public boolean isParamFloating( int n )
    {
        if ( n <= mparam && ia[n] == 1 ) {
           return true;
        }
        return false;
    }

    /**
     *  Sets parameter n to float during fitting.
     */
    public void setParamFloating( int n )
    {
        if ( n <= mparam ) ia[n] = 1;
    }

    /**
     *  Sets parameter n to be fixed or floating during fitting.
     */
    public void setParamFixed( int n, boolean fixed )
    {
        if ( n <= mparam ) {
            if ( fixed ) {
                ia[n] = 0;
            }
            else {
                ia[n] = 1;
            }
        }
    }

    /**
     *  Sets parameter n to be fixed.
     */
    public void setParamFixed( int n )
    {
        if ( n <= mparam ) ia[n] = 0;
    }

    /**
     *  Returns the number of floating parameters.
     */
    public int getNumFloating()
    {
        return mfit;
    }

    /**
     *  Returns the chi-squared value from the current fit.
     */
    public double getChisq()
    {
        return chisq;
    }

    /**
     *  Sets the number of data points. Any existing data points are
     *  discarded.
     */
    public void setNumData( int n )
    {
        ndata = n + 1;
        x = new double[ndata];
        y = new double[ndata];
        sigma = new double[ndata];
        fit = new double[ndata];
        ndata = n;
    }

    /**
     *  Returns the number of data points.
     */
    public int getNumData()
    {
        return ndata;
    }

    /**
     *  Sets the convergence criterion for fitting.
     *
     *  If the ratio of chisq/oldchisq > f, the fitting is ended
     */
    public void setConverge( double f )
    {
        converge = f;
    }

    /**
     *  Returns the current convergence criterion.
     */
    public double getConverge()
    {
        return converge;
    }

    /**
     *  Sets the maximum number of iterations for convergence.
     */
    public void setIterations( int n )
    {
        iterations = n;
    }

    /**
     *  Returns an element in the covariance matrix - covar[n][n].
     */
    public double getCovariance( int n )
    {
        if ( n <= mparam ) {
            return covar[n][n];
        }
        return 0;
    }

    /**
     *  Returns the (n,m) th element in the covariance matrix
     */
    public double getCovariance( int n, int m )
    {
        if ( ( n <= mparam ) && ( m <= mparam ) ) {
            return covar[n][m];
        }
        return 0;
    }

    /**
     *  Returns the square root of the covariance matrix element covar[n][n]
     *  which is the variance of the nth fitting parameter.
     *
     *  If rebase is true then the error will be scaled so that the current
     *  chi-square represents a good fit (scaled to 1). This may give a useful
     *  error estimate when no actual sigma values were available (and unit
     *  values were used).
     */
    public double getError( int n, boolean rebase )
    {
        double result = 0.0;
        if ( n <= mparam && covar[n][n] > 0.0 ) {
            result = Math.sqrt( covar[n][n] );
            if ( rebase ) {
                int nfree = ndata - mparam;
                result = result * Math.sqrt( chisq / nfree );
            }
        }
        return result;
    }

    /**
     *  Alambda is an internal variable in the fitting procedure.
     *  In the numerical recipes implementation, alambda = -1 to
     *  initialise the fitting and is then allowed to vary.  Once
     *  fitting has been finished, it is set to 0, to sort the
     *  covariance matrix correctly
     *
     *  If the fitData() function call is made, this parameter is set
     *  automatically, so do not alter it!
     */
    public void setAlambda( double f )
    {
        alambda = f;
    }

    /**
     * Sort covariances.
     */
    private void covsrt()
    {
        // Needs to have access to mfit (no of parameters) and mparam,
        // the number of parameters and covar, the covariance matrix.
        int i;
        int j;
        int k;
        double temp = 0 ;

        mfit = 0;

        //  Count number of floating parameters.
        for ( i = 1; i <= mparam; i++ ) {
            if( ia[i] == 1 ) {
                mfit++;
            }
        }

        //  Clear covariances.
        for ( i = mfit + 1; i <= mparam; i++ ) {
            for ( j = 1; j <= i; j++ ) {
                covar[i][j] = covar[j][i] = 0.0;
            }
        }
        k = mfit;
        for ( j = mparam; j >= 1;j-- ) {
            if( ia[j] == 1 ) {
                for( i = 1; i <= mparam; i++ )  {
                    temp = covar[i][k];
                    covar[i][k] = covar[i][j];
                    covar[i][j]= temp;
                }
                for( i = 1; i <= mparam; i++ ) {
                    temp = covar[k][i] ;
                    covar[k][i] = covar[j][i];
                    covar[j][i]= temp;
                }
                k--;
            }
        }
    }

    /**
     *  Perform Gauss-Jordan elimination.
     */
    private void gaussj( int n, int m )
    {
        // The original C++ code had
        //    void gaussj( double a[20][20], int n, double b[20], int m );
        // as the GaussJorden elimination function call this routine
        // was originally a general one; in this application we only
        // call it one, so we can rewrite the method to avoid passing
        // the matrices aa and bb
        //    gaussj( covar, mfit, oneda, 1 );
        // n is the number of floating fit parameters and m the
        // dimension of the oneda matrix (==1)
        int i = 0;
        int icol = 0;
        int irow =  0;
        int j = 0;
        int k = 0;
        int l = 0;
        int ll = 0;
        double big = 0;
        double dum = 0;
        double pivinv = 0;
        double piv = 0;
        double temp = 0;

        for( j = 1; j <= n; j++ ) {
            ipiv[j] = 0;
            indexr[j] = 0;
            indexc[j] = 0;
        }
        for ( i = 1; i <= n; i++ ) {
            big = 0.0;
            for ( j = 1; j <= n; j++ ) {
                if ( ipiv[j] != 1 ) {
                    for ( k = 1; k <= n;k++ ) {
                        if ( ipiv[k] == 0 ) {
                            if ( Math.abs( covar[j][k] ) >= big ) {
                                big =Math.abs( covar[j][k] );
                                irow = j;
                                icol = k;
                            }
                        }
                        else if ( ipiv[k] > 1 ) {
                            System.err.println(
                       "fitting error - singular matrix in LevMarq.gaussj() ");
                            return;
                        }
                    }
                }
            }

            ipiv[icol] += 1;

            if ( irow != icol ) {
                for ( l = 1; l <= n; l++ ) {
                    temp = covar[irow][l];
                    covar[irow][l]= covar[icol][l];
                    covar[icol][l]= temp;
                }
                for ( l = 1; l <= m; l++ ) {
                    temp = oneda[irow];
                    oneda[irow] = oneda[icol];
                    oneda[icol]= temp;
                }
            }
            indexr[i] = irow;
            indexc[i] = icol;
            if ( covar[icol][icol] == 0.0 ) {
                System.err.println(
           "fitting error - singular matrix in LevMarq.gaussj()  at " + icol );
                return;
            }
            pivinv = 1.0 / covar[icol][icol];
            covar[icol][icol] = 1.0;
            for ( l = 1; l <= n; l++ ) {
                covar[icol][l] *= pivinv;
            }
            for ( l = 1; l <= m; l++ ) {
                oneda[icol] *= pivinv;
            }
            for ( ll = 1; ll <= n; ll++ ) {
                if( ll != icol ) {
                    dum = covar[ll][icol];
                    covar[ll][icol] = 0.0;
                    for ( l = 1; l <= n; l++ ) {
                        covar[ll][l] -= covar[icol][l] * dum;
                    }
                    for ( l = 1; l <= m; l++ ) {
                        oneda[ll] -= oneda[icol] * dum;
                    }
                }
            }
        }
        for ( l = n;l >= 1; l-- ) {
            if ( indexr[l] != indexc[l] ) {
                for ( k = 1; k <= n; k++ ) {
                    temp = covar[k][indexr[l]];
                    covar[k][indexr[l]] = covar[k][indexc[l]];
                    covar[k][indexc[l]] = temp;
                }
            }
        }
    }


    /**
     *  Levenberg-Marquart minimisation.
     */
    private double mrqmin()
    {
        int j = 0;
        int k = 0;
        int l = 0;
        int m = 0;

        //  Perform initialisations.
        if ( alambda < 0.0 ) {

            //  Count number of floating parameters.
            for ( mfit = 0, j = 1; j <= mparam; j++ ) {
                if ( ia[j] == 1 ) mfit++;
            }
            alambda = 0.001;
            mrqcof( alpha, beta );
            oldchisq = chisq;
        }
        for ( j = 0, l = 1; l <= mparam;l++ ) {
            if ( ia[l] == 1 ) {
                for( j++, k=0, m=1; m <= mparam; m++ ) {
                    if( ia[m] == 1 ) {
                        k++;
                        covar[j][k] = alpha[j][k];
                    }
                }
                covar[j][j] = alpha[j][j] * (1.0 + alambda);
                oneda[j] = beta[j];
            }
        }

        gaussj( mfit, 1 );

        for ( j = 1; j <= mfit; j++ ) {
            da[j] = oneda[j];
        }
        if ( alambda == 0.0 ) {
            covsrt();
            return 0;
        }
        for ( j = 0, l = 1; l <= mparam; l++ ) {
            if ( ia[l] == 1 ) {
                aold[l] = a[l];
                a[l] = a[l] + da[++j];
            }
        }

        mrqcof( covar, da );

        if ( chisq < oldchisq ) {
            alambda *= 0.1;
            oldchisq = chisq;
            for ( j = 0, l = 1; l <= mparam; l++ ) {
                if ( ia[l] == 1 ) {
                    for ( j++, k=0, m=1; m <= mparam; m++) {
                        if ( ia[m] == 1 ) {
                            k++;
                            alpha[j][k] = covar[j][k];
                        }
                    }
                    beta[j] = da[j];
                }
            }
        }
        else {
            for ( j = 0, l = 1; l <= mparam;l++ ) {
                if( ia[l] == 1 ) {
                    a[l] = aold[l];
                }
            }
            alambda *= 10.;
            chisq = oldchisq;
        }
        return chisq;
    }


    /**
     * Calculate fitting matrix and current chi-square.
     *
     * @param alpha fitting matrix, alpha for initialisation pass,
     *              covar for fit.
     * @param beta beta for initialisation pass, da for fit.
     */
    void mrqcof( double[][] alpha, double[] beta )
    {
        int i = 0;
        int j = 0;
        int k = 0;
        int l = 0;
        int m = 0;

        double wt = 0;
        double sig2i = 0;
        double dy;

        mfit=0;
        for ( j = 1; j <= mparam; j++ ) {
            if ( ia[j] == 1 ) {
                mfit++;
            }
        }
        for (j = 1; j <= mfit; j++ ) {
            for ( k = 1; k <= j; k++ ) {
                alpha[j][k]= 0.0;
            }
            beta[j] = 0.0;
        }
        chisq = 0;
        for ( i = 1; i <= ndata; i++ ) {
            fit[i] = funcs.eval( x[i], a, mparam, dyda );
            sig2i = 1.0 / ( sigma[i] * sigma[i] );
            dy = y[i] - fit[i];
            for ( j = 0, l = 1; l <= mparam; l++ ) {
                if ( ia[l] == 1 ) {
                    wt = dyda[l] * sig2i;
                    for ( j++, k=0, m=1; m <= l; m++ ) {
                        if ( ia[m] == 1 ) {
                            alpha[j][++k] += wt * dyda[m];
                        }
                    }
                    beta[j] += dy * wt;
                }
            }
            chisq += dy * dy * sig2i;
        }
        for ( j = 2; j <= mfit;j++ ) {
            for ( k = 1; k < j;k++ ) {
                alpha[k][j] = alpha[j][k];
            }
        }
    }


    /**
     * Do the fit to the data using the current parameter values as
     * the starting position.
     */
    public double fitData()
    {
        int ncount = 1;
        double ratio;
        double chi2;
        double oldchi2;
        double oldalambda;

        alambda = -1.0F;
        chi2 = mrqmin();
        converged = true;
        do {
            oldchi2 = chi2;
            oldalambda = alambda;

            // Do minimisation.
            chi2 = mrqmin();

            // If fit isn't changed or bettered for required number of
            // iterations then break out of loop.
            if ( oldalambda <= alambda ) {
                ratio = 0;
                ncount++;
                if ( ncount == iterations ) {
                    ratio = 1.0;
                    converged = false;
                }
            }
            else {
                ratio = chi2 / oldchi2;
                ncount = 1;
            }
        }
        while ( ratio <= converge );

        alambda = 0.0;
        mrqmin();
        return 0;
    }

    /**
     * Update array of "fit" function values to match those of the
     * current model parameters. One use for this method is after
     * updating the parameters manually (say to set the starting
     * position) and then get model coordinates using getX and getFit.
     */
    public void fitWithParams()
    {
        for ( int i = 1; i <= ndata; i++ ) {
            fit[i] = funcs.eval( x[i], a, mparam, dyda );
        }
    }

    /**
     * Find out how the minimisation halted.
     */
    public boolean isConverged()
    {
        return converged;
    }
}
