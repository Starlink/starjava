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

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.util.AsciiFileParser;

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
 *    l|g|v scale t|f centre t|f sigma|lwidth|gwidth t|f [lwidth] t|f
 * </pre>
 * where <code>l</code> means a Lorentzian, <code>g</code> a Gaussian and
 * <code>v</code> a Voigt profile. The <code>scale</code> is the size of the
 * line, <code>centre</code> the central position (wavelength) and
 * <code>sigma|lwidth|gwidth</code> the width according to the line type. The
 * <code>lwidth</code> value is needed for Voigt profiles. The
 * <code>t|f</code> strings indicate if the value is to be fixed or allowed to
 * float during the minimisation.
 * <p>
 * Example:
 * <pre>
 * l 10000 f 6500 f 5 f
 * l  5000 f 6510 f 3 f
 * l  1000 f 6520 f 2 f
 * </pre>
 * Indicates a blend of three Lorentzian profiles should be
 * fitted. All parameters are floating and will be changed during the
 * minimisation.
 * <p>
 * On completion spectra are produced of the various elements of the fit,
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
    private static final int UNKNOWN = -1;
    private static final int GAUSSIAN = 0;
    private static final int LORENTZIAN = 1;
    private static final int VOIGT = 2;
    protected SpecDataFactory factory = SpecDataFactory.getInstance();
    /**
     * Perform the fit of a composite spectrum to a given spectrum.
     *
     * @param spec the name of the spectrum to fit.
     * @param config the name of the file containing the description of
     *               the composite model to fit.
     * @param model name for the initial spectral model.
     * @param fit name for the result spectrum -- the model after fitting.
     * @param prefix prefix name for the component spectra.
     */
    public CmpFitter( String specFile, String configFile, String modelFile,
                      String fitFile, String cmpPrefix )
    {
        //  Access the spectrum. If not available give up.
        SpecData specData = null;
        try {
            specData = factory.get( specFile );
        }
        catch (SplatException e) {
            e.printStackTrace();
            return;
        }

        //  Open and process the configuration file.
        File config = new File( configFile );
        if ( ! config.exists() ) {
            System.out.println( "Configuration file " + configFile +
                                " doesn't exist" );
            return;
        }
        parseConfig( config );

        //  Access the spectral data.
        double[] x = specData.getXData();
        double[] y = specData.getYData();
        double[] w = specData.getYDataErrors();
        FrameSet frameSet = specData.getFrameSet();
        String dataUnits = specData.getCurrentDataUnits();

        //  Write initial guess complete spectrum to disk file.
        try {
            EditableSpecData memSpec = factory.createEditable( modelFile,
                                                               specData );
            double[] modelYData = fitter.evalYDataArray( x );
            memSpec.setSimpleUnitDataQuick( frameSet, x, dataUnits, 
                                            modelYData );
            SpecData initialSpec = factory.getClone( memSpec, modelFile );
            initialSpec.save();
            System.out.println( "Saved initial model as: " + modelFile );
        }
        catch (SplatException e) {
            System.out.println( "Failed to create initial model spectrum: " +
                                modelFile );
            e.printStackTrace();
        }

        //  Do the fit to the data.
        fitter.doFit( x, y, w );

        //  Write the solution to disk.
        try {
            EditableSpecData memSpec = factory.createEditable( fitFile,
                                                               specData );
            double[] fitYData = fitter.evalYDataArray( x );
            double[] resids = fitter.calcResiduals( x, y );
            //  If NDF must be positive if we store as sigmas/variances.
            for ( int i = 0; i < resids.length; i++ ) {
                resids[i] = Math.abs( resids[i] );
            }
            memSpec.setSimpleUnitDataQuick( frameSet, x, dataUnits, fitYData, 
                                            resids );
            SpecData fitSpec = factory.getClone( memSpec, fitFile );
            fitSpec.save();
            System.out.println( "Saved fit as: " + fitFile );
        }
        catch (SplatException e) {
            System.out.println( "Failed to create fit model spectrum: " +
                                fitFile );
            e.printStackTrace();
        }

        //  Create components as text files.
        Iterator i = fitter.getIterator();
        int j = 0;
        while ( i.hasNext() ) {
            String name = cmpPrefix + j + ".txt";
            try {
                EditableSpecData memSpec = factory.createEditable( name,
                                                                   specData );
                FunctionGenerator fg = (FunctionGenerator) i.next();
                double[] fitYData = fg.evalYDataArray( x );
                memSpec.setSimpleUnitDataQuick( frameSet, x, dataUnits, 
                                                fitYData, null );
                SpecData cmpSpec = factory.getClone( memSpec, name );
                cmpSpec.save();
                System.out.println( "Saved component line: " + name );
            }
            catch (SplatException e) {
                System.out.println( "Failed to create component spectrum: " +
                                    name );
                e.printStackTrace();
            }
            j++;
        }

        //  Write report on fit use same format as input.
        writeNewConfig();
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
            String stype = parser.getStringField( i, 0 );
            int type = UNKNOWN;
            if ( "g".equals( stype ) ) {
                type = GAUSSIAN;
                System.out.println( "GAUSSIAN" );
            }
            else if ( "l".equals( stype ) ) {
                type = LORENTZIAN;
                System.out.println( "LORENTZIAN" );
            }
            else if ( "v".equals( stype ) ) {
                type = VOIGT;
                System.out.println( "VOIGT" );
            }

            if ( type != UNKNOWN ) {
                int nfields = parser.getNFields( i );
                if ( nfields != 7 && nfields != 9 ) {
                    System.out.println( "Skiping line " + i + " of " + config);
                    System.out.println( "Wrong number of fields (must be 7 " +
                                        "or 9)");
                    continue;
                }
                double scale = parser.getDoubleField( i, 1 );
                boolean scaleFixed = parser.getBooleanField( i, 2 );
                double centre = parser.getDoubleField( i, 3 );
                boolean centreFixed = parser.getBooleanField( i, 4 );
                double width = parser.getDoubleField( i, 5 );
                boolean widthFixed = parser.getBooleanField( i, 6 );
                switch ( type ) {
                    case GAUSSIAN: {
                        fg = new GaussianGenerator( scale, scaleFixed,
                                                    centre, centreFixed,
                                                    width, widthFixed );
                    }
                    break;
                    case LORENTZIAN: {
                        fg = new LorentzGenerator( scale, scaleFixed,
                                                   centre, centreFixed,
                                                   width, widthFixed );
                    }
                    break;
                    case VOIGT: {
                        double lwidth = parser.getDoubleField( i, 7 );
                        boolean lwidthFixed = parser.getBooleanField( i, 8 );
                        fg = new VoigtGenerator( scale, scaleFixed,
                                                 centre, centreFixed,
                                                 width, widthFixed,
                                                 lwidth, lwidthFixed );
                    }
                    break;
                }
                fitter.addFunctionGenerator( fg );
            }
            else {
                System.out.println( "Unrecognised line type: " + stype );
                continue;
            }
        }
        if ( fitter.functionGeneratorCount() == 0 ) {
            throw new
                RuntimeException( "Failed to aquire any components to fit ");
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

            r.write( "# Report by SPLAT \n" );
            r.write( "Total flux = " + fitter.getFlux() + "\n" );
            r.write( "Chi square = " + fitter.getChi() + "\n" );
            r.write( "\n" );
            r.write( "Component parameters:\n" );

            Iterator i = fitter.getIterator();
            int j = 0;
            while ( i.hasNext() ) {
                FunctionGenerator fg = (FunctionGenerator) i.next();
                String[] line = parser.getRow( j++ );
                r.write( "Pre-fit: " );
                for ( int k = 0; k < line.length; k++ ) {
                    r.write( line[k] + " " );
                }
                r.write( "\n" );
                r.write( "Post-fit: " + fg.toString() + "\n" );
            }

            r.close();
            f.close();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    protected void writeNewConfig()
    {
        System.out.println( "Writing fit configuration to report.config" );

        File file = new File( "report.config" );
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );

            Iterator i = fitter.getIterator();
            int j = 0;
            FunctionGenerator fg = null;
            GaussianGenerator gg = null;
            LorentzGenerator lg = null;
            VoigtGenerator vg = null;
            while ( i.hasNext() ) {
                fg = (FunctionGenerator) i.next();
                if ( fg instanceof GaussianGenerator ) {
                    gg = (GaussianGenerator) fg;
                    r.write( "g" );
                    r.write( " " + gg.getScale() );
                    r.write( " " + gg.getScaleFixed() );
                    r.write( " " + gg.getCentre() );
                    r.write( " " + gg.getCentreFixed() );
                    r.write( " " + gg.getSigma() );
                    r.write( " " + gg.getSigmaFixed() );
                }
                else if ( fg instanceof LorentzGenerator ) {
                    lg = (LorentzGenerator) fg;
                    r.write( "l " );
                    r.write( " " + lg.getScale() );
                    r.write( " " + lg.getScaleFixed() );
                    r.write( " " + lg.getCentre() );
                    r.write( " " + lg.getCentreFixed() );
                    r.write( " " + lg.getWidth() );
                    r.write( " " + lg.getWidthFixed() );
                }
                else if ( fg instanceof VoigtGenerator ) {
                    vg = (VoigtGenerator) fg;
                    r.write( "v " );
                    r.write( " " + vg.getScale() );
                    r.write( " " + vg.getScaleFixed() );
                    r.write( " " + vg.getCentre() );
                    r.write( " " + vg.getCentreFixed() );
                    r.write( " " + vg.getLWidth() );
                    r.write( " " + vg.getLWidthFixed() );
                    r.write( " " + vg.getGWidth() );
                    r.write( " " + vg.getGWidthFixed() );
                }
                r.write( "\n" );
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
        if ( args.length != 5 && args.length != 2 ) {
            System.out.println( "Usage: CmpFitter " +
                                "spectrum " +
                                "configuration_file " +
                                "[initial_model_spectrum] " +
                                "[final_model_spectrum] " +
                                "[prefix_component_spectra]" );
            System.exit( 1 );
        }
        if ( args.length == 5 ) {
            CmpFitter cmp = new CmpFitter( args[0], args[1], args[2], args[3],
                                           args[4] );
        }
        else {
            CmpFitter cmp = new CmpFitter( args[0], args[1], "model.txt",
                                           "result.txt", "cmp" );
        }
    }
}
