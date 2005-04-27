/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-APR-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;

import java.util.Arrays;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.LineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.data.LineIDSpecData;

/**
 *  Utility class to create the distributable line identifier tree from
 *  a simple directory tree of line identifiers stored in SPLAT text format.
 *  <p>
 *  The distributable form is intended to be wrapped as a single jar file and
 *  contains each of the known line id spectra in serialized form, plus a text
 *  file that describes each of the spectra, that is their name and the
 *  applicable coordinate range. This is to allow the matching of lines to
 *  displayed spectra, without opening all the line spectra, and so that all
 *  the lines (when wrapped in a jar file) can distributed as part of a Java
 *  Webstart version of SPLAT.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineIDTree
{
    private StringBuffer dataBase = new StringBuffer();
    private int rootDirLength = 0;

    /**
     * Create an instance and process all the files. The two directories must
     * exist.
     */
    public LineIDTree( File rootInDir, File rootOutDir )
    {
        this.rootDirLength = rootInDir.getAbsolutePath().length();
        process( rootInDir, rootOutDir );
        writeDescription( rootOutDir );
    }

    /**
     * Start the scan and process the input tree into the output tree.
     * The output spectra are a serialized form so that they can
     * be restored from within a jar file with the minimum of fuss.
     */
    public void process( File inDir, File outDir )
    {
        File[] list = inDir.listFiles();
        Arrays.sort( list );
        if ( list.length > 0 ) {
            for ( int i = 0; i < list.length; i++ ) {
                String name = list[i].getName();
                if ( list[i].isDirectory() ) {
                    //  Create the shadow output directory.
                    File newDir = new File( outDir, name );
                    newDir.mkdir();
                    process( list[i], newDir );
                }
                else {
                    //  Stop processing extraneous files by insisting on the
                    //  ".ids" file extension.
                    if ( name.endsWith( ".ids" ) ) {
                        processSpectrum( list[i], outDir, name );
                    }
                    else {
                        System.out.println( "Skipped: " + list[i] );
                    }
                }
            }
        }
        return;
    }

    /**
     * Create a LineIDSpecData instance from the given line id spectrum file
     * and serialize it into the output file in the mirror tree (note unlike
     * stacks we do not compress, this will be done by the jar file anyway).
     * <p>
     * This method also adds a description to the dataBase.
     */
    private void processSpectrum( File inSpec, File outDir, String newName )
    {
        try {
            LineIDTXTSpecDataImpl impl = new LineIDTXTSpecDataImpl( inSpec );
            LineIDSpecData specData = new LineIDSpecData( impl );
            File newFile = new File( outDir, newName );

            //  Get the description.
            addDescription( inSpec, specData );

            //  Write the output file.
            FileOutputStream fos = new FileOutputStream( newFile );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( specData );
            oos.close();
            fos.close();
            System.out.println( "Processed: " + inSpec );
        }
        catch (Exception e) {
            System.out.println( "Didn't process: " + inSpec );
            System.out.println( e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * Add a description of a LineIDSpecData object to the dataBase.
     */
    private void addDescription( File inSpec, LineIDSpecData specData )
    {
        //  Get name relative to the root directory.
        String inFile = inSpec.getAbsolutePath();
        String name = inFile.substring( rootDirLength );

        //  Get the coordinate range.
        specData.setRange();
        double[] range = specData.getRange();

        //  And the units and system of the coordinates.
        FrameSet frameSet = specData.getFrameSet();
        String units = frameSet.getC( "unit(1)" );
        String system = frameSet.getC( "system(1)" );
        
        dataBase.append( name     + "\t" + 
                         range[0] + "\t" + 
                         range[1] + "\t" +
                         units    + "\t" + 
                         system   + "\n" );
    }

    /**
     * Write the description file that details the names of all the
     * spectra (cannot easily walk the contents of a jar file) and
     * lists their coordinate ranges and units.
     */
    private void writeDescription( File outDir )
    {
        File out = new File( outDir, "description.txt" );
        try {
            FileWriter fw = new FileWriter( out );
            fw.write( dataBase.toString() );
            fw.close();
            System.out.println( "Description file updated" );
        }
        catch (Exception e) {
            System.out.println( "Failed to write description file" );
            throw new RuntimeException( e );
        }
    }

    /**
     * Main entry point. This requires two arguments. The root of the
     * directory tree containing the line identifier spectra in SPLAT text
     * format, plus the directory to contain the converted form.
     */
    public static void main( String[] args )
    {
        if ( args.length != 2 ) {
            System.out.println( "Usage: " + LineIDTree.class +
                                "root_of_line_identifier_files new_root" );
            return;
        }

        File inDir = new File( args[0] );
        File outDir = new File( args[1] );

        if ( ! inDir.isDirectory() ) {
            System.out.println( inDir + " is not a directory" );
            return;
        }

        if ( ! outDir.isDirectory() ) {
            try {
                outDir.mkdirs();
            }
            catch (Exception e) {
                System.out.println( outDir  + " no output directory created" );
                e.printStackTrace();
                return;
            }
        }
        LineIDTree tree = new LineIDTree( inDir, outDir );
        return;
    }
}
