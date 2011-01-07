/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     30-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Combines a set of {@link FunctionGenerator}s so that they are
 * presented as a single {@link FunctionFitter}. The individual components
 * are combined as a linear combination.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class CompositeFunctionFitter
    extends AbstractFunctionFitter
{
    // XXX could optimise a lot of this. Use FunctionGenerator[]
    // array, pre-allocate suba and subdyda...
    //
    // XXX Sitting here with better things to do an obvious refactoring of the
    // Fitter and Generator classes is to make Generators for each model the
    // basic type and use this class to create Fitters for the specific
    // models, but history has the logic the other way around as Fitters came
    // first.

    /**
     * The list of {@link FunctionGenerator}s that are being managed.
     */
    private ArrayList funcs;

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

    /**
     * Did minimisation converge before exiting?
     */
    protected boolean converged = false;

    /**
     * Default constructor.
     */
    public CompositeFunctionFitter()
    {
        funcs = new ArrayList();
    }

    /**
     * Perform the fit on all FunctionGenerators using unit weights.
     */
    public void doFit( double[] x, double[] y )
    {
        doFit( x, y, null );
    }

    /**
     * Perform the fit on all FunctionGenerators.
     */
    public void doFit( double[] x, double[] y, double[] w )
    {
        // Number of parameters.
        int npar = getNumParams();

        // Create the LevMarq minimisation object.
        LevMarq lm = new LevMarq( this, x.length, npar );

        // Pass it the data.
        boolean unitweights = false;
        if ( w == null ) {
            unitweights = true;
        }
        for ( int i = 0; i < x.length; i++ ) {
            lm.setX( i + 1, x[i] );
            lm.setY( i + 1, y[i] );
            if ( w == null || w[i] == 0.0 ) {
                lm.setSigma( i + 1, 1.0 );
            }
            else {
                lm.setSigma( i + 1, w[i] );
            }
        }

        // Need to access and set all parameters.
        double[] params = getParams();
        boolean[] fixed = getFixed();
        for ( int i = 0; i < params.length; i++ ) {
            lm.setParam( i + 1, params[i], fixed[i] );
        }

        // And mimimise.
        lm.fitData();

        // Record estimate of goodness of fit.
        chiSquare = lm.getChisq();

        // How did minimisation complete?
        converged = lm.isConverged();

        //  And reset all FunctionGenerators to the new values.
        double[] errors = new double[params.length];
        for ( int i = 0; i < params.length; i++ ) {
            params[i] = lm.getParam( i + 1 );
            errors[i] = lm.getError( i + 1, unitweights );
        }
        setParams( params );
        setPErrors( errors );
    }

    /**
     * Add a FunctionGenerator.
     */
    public void addFunctionGenerator( FunctionGenerator generator )
    {
        funcs.add( generator );
    }

    /**
     * Remove a FunctionGenerator.
     */
    public void removeFunctionGenerator( FunctionGenerator generator )
    {
        funcs.remove( generator );
    }

    /**
     * Find out how many FunctionGenerators are added.
     */
    public int functionGeneratorCount()
    {
        return funcs.size();
    }

    /**
     * How did the minimisation complete?
     */
    public boolean isConverged()
    {
        return converged;
    }


    /**
     * Return an Iterator over the FunctionGenerators.
     */
    public Iterator getIterator()
    {
        return funcs.iterator();
    }

    public double getCentre()
    {
        //  Return mean position.
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                sum += ((FunctionGenerator) funcs.get( i )).getCentre();
            }
            sum /= (double) size;
        }
        return sum;
    }

    public double getScale()
    {
        //  Hmm, what does this mean? Maximum, mean or sum? I choose mean.
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                sum += ((FunctionGenerator) funcs.get( i )).getScale();
            }
            sum /= (double) size;
        }
        return sum;
    }

    public double getFlux()
    {
        //  Sum flux of all components.
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                sum += ((FunctionGenerator) funcs.get( i )).getFlux();
            }
        }
        return sum;
    }

    public double getChi()
    {
        return chiSquare;
    }

    // Evaluate the composite function at a series of positions.
    public double[] evalYDataArray( double[] x )
    {
        int size = funcs.size();
        double[] sums = null;
        if ( size > 0 ) {
            sums = ((FunctionGenerator) funcs.get( 0 )).evalYDataArray( x );
            double[] values;
            for ( int i = 1; i < size; i++ ) {
                values =
                    ((FunctionGenerator) funcs.get( i )).evalYDataArray( x );
                for ( int j = 0; j < values.length; j++ ) {
                    sums[j] += values[j];
                }
            }
        }
        return sums;
    }

    // Evaluate the composite function at a given position.
    public double evalYData( double x )
    {
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                sum += ((FunctionGenerator) funcs.get( i )).evalYData( x );
            }
        }
        return sum;
    }

    // Return the total number of parameters.
    public int getNumParams()
    {
        int size = funcs.size();
        int sum = 0;
        if ( size > 0 ) {
            for ( int i = 0; i < size; i++ ) {
                sum += ((FunctionGenerator) funcs.get( i )).getNumParams();
            }
        }
        return sum;
    }

    // Get all parameters in a single array.
    public double[] getParams()
    {
        int total = getNumParams();
        double[] params = new double[total];

        int size = funcs.size();
        double[] pars;
        int offset = 0;
        for ( int i = 0; i < size; i++ ) {
            pars = ((FunctionGenerator) funcs.get( i )).getParams();
            for ( int j = 0; j < pars.length; j++ ) {
                params[offset++] = pars[j];
            }
        }
        return params;
    }

    // Set all params, does the reverse of getParams. Order depends on
    // the FunctionGenerators in use, so not much use to anyone else.
    public void setParams( double[] params )
    {
        int size = funcs.size();
        double[] pars;
        int offset = 0;
        int npars = 0;
        FunctionGenerator g;
        for ( int i = 0; i < size; i++ ) {
            g = ((FunctionGenerator) funcs.get( i ));
            npars = g.getNumParams();
            pars = new double[npars];
            for ( int j = 0; j < pars.length; j++ ) {
                pars[j] = params[offset++];
            }
            g.setParams( pars );
        }
    }

    // Get all parameter errors in a single array.
    public double[] getPErrors()
    {
        int total = getNumParams();
        double[] errors = new double[total];

        int size = funcs.size();
        double[] errs;
        int offset = 0;
        for ( int i = 0; i < size; i++ ) {
            errs = ((FunctionGenerator) funcs.get( i )).getPErrors();
            for ( int j = 0; j < errs.length; j++ ) {
                errors[offset++] = errs[j];
            }
        }
        return errors;
    }

    // Set all parameter errors, complementary to setParams.
    public void setPErrors( double[] errors )
    {
        int size = funcs.size();
        double[] errs;
        int offset = 0;
        int nerrs = 0;
        FunctionGenerator g;
        for ( int i = 0; i < size; i++ ) {
            g = ((FunctionGenerator) funcs.get( i ));
            nerrs = g.getNumParams();
            errs = new double[nerrs];
            for ( int j = 0; j < errs.length; j++ ) {
                errs[j] = errors[offset++];
            }
            g.setPErrors( errs );
        }
    }

    // Get whether parameters are fixed in a single array.
    public boolean[] getFixed()
    {
        int total = getNumParams();
        boolean[] fixed = new boolean[total];

        int size = funcs.size();
        boolean[] fix;
        int offset = 0;
        for ( int i = 0; i < size; i++ ) {
            fix = ((FunctionGenerator) funcs.get( i )).getFixed();
            for ( int j = 0; j < fix.length; j++ ) {
                fixed[offset++] = fix[j];
            }
        }
        return fixed;
    }

    // Set whether parameters are fixed... This depends on the order of the
    // given FunctionGenerators
    public void setFixed( boolean[] fixed )
    {
        int size = funcs.size();
        boolean[] fixs;
        int offset = 0;
        int nfixs = 0;
        FunctionGenerator g;
        for ( int i = 0; i < size; i++ ) {
            g = ((FunctionGenerator) funcs.get( i ));
            nfixs = g.getNumParams();
            fixs = new boolean[nfixs];
            for ( int j = 0; j < fixs.length; j++ ) {
                fixs[j] = fixed[offset++];
            }
            g.setFixed( fixs );
        }
    }

    //
    // Evaluate the composite function given a set of full parameters. Returns
    // the derivate of the function at the position. Part of the
    // {@link LevMarqFunc} interface.
    //
    public double eval( double x, double[] a, int na, double[] dyda )
    {
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            double[] suba;
            double[] subdyda;
            int npar;
            int offset = 1;
            FunctionGenerator gen;
            for ( int i = 0; i < size; i++ ) {
                gen = (FunctionGenerator) funcs.get( i );

                // Copy the parameters needed for this sub-function.
                npar = gen.getNumParams();
                suba = new double[npar+1];
                for ( int j = 1, k = offset; j <= npar; j++ ) {
                    suba[j] = a[k++];
                }

                //  Evaluate, copying the derivates returned.
                subdyda = new double[npar+1];
                sum += gen.eval( x, suba, npar, subdyda );
                for ( int j = 1; j <= npar; j++ ) {
                    dyda[offset++] = subdyda[j];
                }
            }
        }
        return sum;
    }
}
