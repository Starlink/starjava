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

/**
 * Combines a set of {@link FunctionGenerator}s so that they are
 * presented as a single FunctionFitter. The individual components
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

    /**
     * The list of {@link FunctionGenerator}s that are being managed.
     */
    private ArrayList funcs;

    /**
     * The chi square of the fit.
     */
    protected double chiSquare = 0.0;

    /**
     * Default constructor.
     */
    public CompositeFunctionFitter()
    {
        funcs = new ArrayList();
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
        for ( int i = 0; i < x.length; i++ ) {
            lm.setX( i + 1, x[i] );
            lm.setY( i + 1, y[i] );
            if ( w[i] == 0.0 ) {
                lm.setSigma( i + 1, 1.0 );
            }
            else {
                lm.setSigma( i + 1, 1.0 / w[i] );
            }
        }

        // Need to access and set all parameters.
        double[] params = getParams();
        for ( int i = 0; i < params.length; i++ ) {
            lm.setParam( i + 1, params[i] );
        }

        // And mimimise.
        lm.fitData();

        // Record estimate of goodness of fit.
        chiSquare = lm.getChisq();
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
     * Return an Iterator over the FunctionGenerators.
     */
    public Iterator getIterator()
    {
        return funcs.iterator();
    }

    public double getCentre()
    {
        return 0.0;
    }

    public double getScale()
    {
        return 0.0;
    }

    public double getFlux()
    {
        return 0.0;
    }

    public double getChi()
    {
        return 0.0;
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
                for ( int j = 0; j < values.length; i++ ) {
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

    // Set all params... Does nothing just required for the interface.
    public void setParams( double[] params )
    {
        // Do nothing.
    }

    //
    // Evaluate the composite function given a set of full
    // parameters. Returns the derivate of the function at the
    // position. Part of the LevMarqFunc interface.
    //
    public double eval( double x, double[] a, int na, double[] dyda )
    {
        int size = funcs.size();
        double sum = 0.0;
        if ( size > 0 ) {
            double[] suba;
            double[] subdyda;
            int npar;
            int offset = 0;
            FunctionGenerator gen;
            for ( int i = 0; i < size; i++ ) {
                gen = (FunctionGenerator) funcs.get( i );

                // Copy the parameters needed for this sub-function.
                npar = gen.getNumParams();
                suba = new double[npar];
                for ( int j = 0, k = offset; j < npar; j++ ) {
                    suba[j] = a[k++];
                }

                //  Evaluate, copying the derivates returned.
                subdyda = new double[npar];
                sum += gen.eval( x, suba, npar, subdyda );
                for ( int j = 0; j < npar; j++ ) {
                    dyda[offset++] = subdyda[j];
                }
            }
        }
        return sum;
    }
}
