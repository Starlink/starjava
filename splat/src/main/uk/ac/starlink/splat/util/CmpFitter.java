/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-FEB-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;

/**
 * Wrapper class for performing the fitting of composite spectral models to a
 * spectrum.
 * <p>
 * To use this class you need a spectrum specification that can be
 * understood by the {@link SpecDataFactory} and a configuration file that
 * describes the model components that you want to fit to the spectrum data.
 * Note that the spectrum should be background subtracted.
 * <p>
 * The format of the configuration file is line-based with each line
 * representing a component and having the following format:
 * <pre>
 *    l|g|v scale centre sigma|width|gwidth [lwidth]
 * </pre>
 * where <code>l</code> means a Lorentzian, <code>g</code> a Gaussian and
 * <code>v</code> a Voigt profile. The <code>scale</code> is the size of the
 * line, <code>centre</code> the central position (wavelength) and
 * <code>sigma|width|gwidth</code> the width according to the line type. The
 * <code>lwidth</code> value is needed for Voigt profiles.
 * <p>
 * On completion text files are produced of the various elements of the fit,
 * together with a summary report of the fit which is presented on the
 * terminal.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class CmpFitter
{
    protected CompositeFunctionFitter fitter = new CompositeFunctionFitter();
    protected AsciiFileParser parser = null;

    public CmpFitter( String specSpec, String configSpec )
    {
        SpecData specData = null;
        try {
            specData = SpecDataFactory.getReference().get( specSpec );
        }
        catch (SplatException e) {
            e.printStackTrace();
            return;
        }

        File config = new File( configSpec );
        if ( ! config.exists() ) {
            System.out.println( "Configuration file " + configSpec +
                                " doesn't exist" );
            return;
        }
        parseConfig( config );

        double[] x = specData.getXData();
        double[] y = specData.getYData();
        double[] w = specData.getYDataErrors();

        //  Write original data for completeness.
        System.out.println( "Writing fit data to original.txt" );
        writeData( "original.txt", x, y );

        //  Write initial guess complete spectrum to disk file.
        double[] fit = fitter.evalYDataArray( x );
        System.out.println( "Writing first guess to initial.txt" );
        writeData( "initial.txt", x, fit );

        System.out.println( "SOLUTION" );
        fitter.doFit( x, y, w );
        System.out.println( "FINISHED - converged: " + fitter.isConverged() );

        //  Write the solution to disk.
        fit = fitter.evalYDataArray( x );
        System.out.println( "Writing fit to solution.txt" );
        writeData( "solution.txt", x, fit );

        //  Echo componnent.
        Iterator i = fitter.getIterator();
        int j = 0;
        while ( i.hasNext() ) {
            FunctionGenerator fg = (FunctionGenerator) i.next();
            fit = fg.evalYDataArray( x );
            String name = "fg" + j +".txt";
            System.out.println( "Writing component " + j + " to " + name );
            writeData( name, x, fit );
            j++;
        }

        //  Write report on fit.
        writeReport();
    }

    //  Parse config file creating FunctionGenerators for each known type
    //  and adding these to the CompositeFunctionFitter.
    public void parseConfig( File config )
    {
        parser = new AsciiFileParser( config, false );
        int rows = parser.getNRows();
        FunctionGenerator fg = null;
        for ( int i = 0; i < rows; i++ ) {
            String type = parser.getStringField( i, 0 );
            if ( "g".equals( type ) ) {
                System.out.println( "Gaussian" );
                double scale = parser.getDoubleField( i, 1 );
                double centre = parser.getDoubleField( i, 2 );
                double sigma = parser.getDoubleField( i, 3 );
                fg = new GaussianGenerator( scale, centre, sigma );
            }
            else if ( "l".equals( type ) ) {
                System.out.println( "Lorenztian" );
                double scale = parser.getDoubleField( i, 1 );
                double centre = parser.getDoubleField( i, 2 );
                double lwidth = parser.getDoubleField( i, 3 );
                fg = new LorentzGenerator( scale, centre, lwidth );
            }
            else if ( "v".equals( type ) ) {
                System.out.println( "Voigt" );
                double scale = parser.getDoubleField( i, 1 );
                double centre = parser.getDoubleField( i, 2 );
                double gwidth = parser.getDoubleField( i, 3 );
                double lwidth = parser.getDoubleField( i, 4 );
                fg = new VoigtGenerator( scale, centre, gwidth, lwidth );
            }
            else {
                System.out.println( "Unrecognised line type: " + type );
                continue;
            }
            fitter.addFunctionGenerator( fg );
        }
    }

    public static void writeData( String fileName, double[] x, double[] y )
    {
        File file = new File( fileName );
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );

            for ( int i = 0; i < x.length; i++ ) {
                r.write( x[i] + " " + y[i] );
                r.newLine();
            }
            r.close();
            f.close();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    protected void writeReport()
    {
        System.out.println( "Writing fit report to report.log" );

        File file = new File( "report.log" );
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );

            Iterator i = fitter.getIterator();
            int j = 0;
            while ( i.hasNext() ) {
                FunctionGenerator fg = (FunctionGenerator) i.next();
                String[] line = parser.getRow( j++ );
                r.write( "Original: " );
                for ( int k = 0; k < line.length; k++ ) {
                    r.write( line[k] + " " );
                }
                r.write( "\n" );
                r.write( "Fit: " + fg.toString() + "\n" );
            }

            r.close();
            f.close();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public static void main( String[] args )
    {
        if ( args.length != 2 ) {
            System.out.println("Usage: CmpFitter spectrum configuration_file");
            System.exit( 1 );
        }
        CmpFitter cmp = new CmpFitter( args[0], args[1] );
    }
}
