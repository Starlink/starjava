/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.data.EditableSpecData;
import javax.swing.JMenuBar;

/**
 * Provides facilities for generating new error column values
 * for a spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ErrorColumnGenerator
    extends ColumnGenerator
{
    // Known expression templates.
    protected static String[][] templates = {
        { "Poisson errors", "true", 
                            "sqrt(data)/gain", 
                            "gain", "1.0" },
        { "Sky subtracted errors", "true", 
                                   "sqrt((data+meansky)/gain)", 
                                   "meansky", "0.0",
                                   "gain", "1.0" },
        { "Gaussian errors", "true", 
                             "const", 
                             "const", "100.0" },
        { "Linear transform of data", "true", 
                                      "data*scale+zero", 
                                      "scale", "1.0", 
                                      "zero", "0.0" },
        { "Linear transform of errors", "true", 
                                        "error*scale+zero", 
                                        "scale", "1.0", 
                                        "zero", "0.0" },
        { "Poisson distribution", "true",
                                  "sqrt(poisson(mean))", 
                                  "mean", "10.0" },
        { "Poisson distribution added to data", "true",
                                                "data+poisson(mean)", 
                                                "mean", "10.0" },
        { "Gaussian distribution", "true",
                                   "gauss(mean,sigma)", 
                                   "mean", "10.0", 
                                   "sigma", "1.0" },
        { "Gaussian distribution added to data", "true",
                                                 "data+gauss(mean,sigma)", 
                                                 "mean", "10.0",  
                                                 "sigma", "1.0" },
        { "Random distribution", "true",  
                                 "rand(lower,upper)", 
                                 "lower", "0.0", 
                                 "upper", "1.0" },
        { "Log of error (natural)", "true", 
                                    "error/data" },
        { "Log of error (base a)", "true", 
                                   "error/(data*log(a))", 
                                   "a", "10" },
        { "Envelope function", "true", 
                               "error*exp(-0.5*(radius/sigma)**2)", 
                               "radius", "coord-center",
                               "center", "100.0",
                               "sigma", "50.0" },
    };

    /**
     * Create an instance.
     */
    public ErrorColumnGenerator( EditableSpecData specData,
                                 ColumnGeneratorListener listener )
    {
        super( specData, templates, listener );
    }

    /**
     * Add help information
     */
    public void addHelp( JMenuBar menuBar )
    {
        // XXXX Next time do help for this window.
        //HelpFrame.createHelpMenu( "error-generator", "Help on window",
        //                          menuBar, null );
    }

    /**
     * Return a suitable title.
     */
    public String getTitle()
    {
        return "Generate or modify error column values";
    }

    /**
     * Deal with any special functions. These are unknown to MathMap
     * and should be shown in an uneditable state (essential fixed
     * functions with known parameters). This implementation has none.
     */
    protected double[] handleSpecialFunction( int templateIndex, 
                                              String[] parameters )
    {
        return null;
    }
}
