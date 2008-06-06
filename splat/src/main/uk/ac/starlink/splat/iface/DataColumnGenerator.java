/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     06-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.util.VoigtGenerator;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Provides facilities for generating new column values for the data
 * of a spectrum. The transformations are all represented by symbolic
 * c-like statements, some examples of which are available from
 * pre-load (linear, special noise functions etc.).
 * <p>
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DataColumnGenerator
    extends ColumnGenerator
{
    // Known expression templates.
    protected static String[][] templates = {
        { "Linear transform of data", "true",
                                      "data*scale+zero",
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
        { "Log of data (natural)", "true",
                                   "log(data)" },
        { "Log of data (base 10)", "true",
                                   "log10(data)" },
        { "Voigt function", "false",
                            "scale*voigt(coord-center,gwidth,lwidth)",
                            "scale", "1.0",
                            "center", "100.0",
                            "gwidth", "1.0",
                            "lwidth", "1.0" },
        { "Voigt function added to data", "false",
                            "data+scale*voigt(coord-center,gwidth,lwidth)",
                            "scale", "1.0",
                            "center", "100.0",
                            "gwidth", "1.0",
                            "lwidth", "1.0" },
        { "Voigt function subtracted from data", "false",
                            "data-scale*voigt(coord-center,gwidth,lwidth)",
                            "scale", "1.0",
                            "center", "100.0",
                            "gwidth", "1.0",
                            "lwidth", "1.0" },
        { "Lorenzian function", "true",
                                "scale/(1.0+0.5*(radius/width)**2)",
                                "radius", "coord-center",
                                "center", "100.0",
                                "scale", "10.0",
                                "width", "5.0" },
        { "Gaussian function", "true",
                               "scale*exp(-0.5*(radius/sigma)**2)",
                               "radius", "coord-center",
                               "center", "100.0",
                               "scale", "10.0",
                               "sigma", "5.0" },
        { "Envelope function", "true",
                               "data*exp(-0.5*(radius/sigma)**2)",
                               "radius", "coord-center",
                               "center", "100.0",
                               "sigma", "50.0" },
    };

    /**
     * Create an instance.
     */
    public DataColumnGenerator( EditableSpecData specData,
                                ColumnGeneratorListener listener )
    {
        super( specData, templates, listener );
    }

    /**
     * Add help information
     */
    public void addHelp( JMenuBar menuBar )
    {
        // XXX Next time do help for this window.
        //HelpFrame.createHelpMenu( "data-generator", "Help on window",
        //                          menuBar, null );
    }

    /**
     * Return a suitable title.
     */
    public String getTitle()
    {
        return "Generate or modify data column values";
    }

    /**
     * Deal with any special functions. These are unknown to MathMap
     * and should be shown in an uneditable state (essential fixed
     * functions with known parameters).
     */
    protected double[] handleSpecialFunction( int templateIndex,
                                              String[] parameters )
    {
        if ( templateIndex == 8 || templateIndex == 9 || 
             templateIndex == 10 ) {
            try {
                //  Need to deal with voigt function, which cannot be
                //  translated into an AstMathMap function.
                double[] coord = specData.getXData();
                
                //  Get the various parameter values.
                double scale = Double.parseDouble( parameters[0] );
                double centre = Double.parseDouble( parameters[1] );
                double gwidth = Double.parseDouble( parameters[2] );
                double lwidth = Double.parseDouble( parameters[3] );
                
                VoigtGenerator generator = new VoigtGenerator( scale, centre,
                                                               gwidth, lwidth);
                double[] function = generator.evalYDataArray( coord );
                double[] result = function;

                //  If needed either add or subtract result from
                //  existing data.
                if ( templateIndex == 9 ) {
                    result = specData.getYData();
                    for ( int i = 0; i < result.length; i++ ) {
                        result[i] += function[i];
                    }
                }
                else if ( templateIndex == 10 ) {
                    result = specData.getYData();
                    for ( int i = 0; i < result.length; i++ ) {
                        result[i] -= function[i];
                    }
                }
                return result;
            }
            catch (Exception e) {
                ErrorDialog.showError( this, e );
            }
        }
        else {
            //  Don't know this function.
            JOptionPane.showMessageDialog
                ( this, "Cannot modify the data column, unknown function " +
                  templates[templateIndex][0], "Not special (internal error)",
                  JOptionPane.ERROR_MESSAGE );
        }
        return null;
    }
}
