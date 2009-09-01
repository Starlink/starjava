/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     25-APR-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.SpecFrame;

import uk.ac.starlink.splat.data.LineIDMEMSpecDataImpl;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.LineIDTXTSpecDataImpl;
import uk.ac.starlink.splat.data.SpecData;

/**
 *  Utility class to create the distributable line identifier tree from
 *  a directory tree of line identifiers stored in SPLAT text format
 *  and the JAC XML format.
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
    private static Logger logger =
        Logger.getLogger( LineIDTree.class.getName() );
    private static int MAX_ITEMS = 16;

    /**
     * Create an instance and process all the files. The two directories must
     * exist.
     */
    public LineIDTree( File rootInDir, File rootOutDir )
    {
        this.rootDirLength = rootOutDir.getAbsolutePath().length();
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
                    //  ".ids" and ".xml" file extensions.
                    if ( name.endsWith( ".ids" ) ) {
                        processSpectrum( list[i], outDir, name );
                    }
                    else if ( name.endsWith( ".xml" ) ) {
                        try {
                            processJACDataBase( list[i], outDir );
                        }
                        catch (Exception e) {
                            logger.log( Level.INFO, e.getMessage(), e );
                        }
                    }
                    else {
                        logger.fine( "Skipped: " + list[i] );
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
            addDescription( newFile, specData );

            //  Write the output file.
            FileOutputStream fos = new FileOutputStream( newFile );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( specData );
            oos.close();
            fos.close();
            logger.fine( "Processed: " + inSpec );
        }
        catch (Exception e) {
            logger.warning( "Failed to process: " + inSpec );
            logger.log( Level.INFO, e.getMessage(), e );
        }
    }

    /**
     * Add a description of a LineIDSpecData object to the dataBase.
     */
    private void addDescription( File outSpec, LineIDSpecData specData )
    {
        //  Get name relative to the root directory.
        String outFile = outSpec.getAbsolutePath();
        String name = outFile.substring( rootDirLength );

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
            logger.fine( "Description file updated" );
        }
        catch (Exception e) {
            logger.info( "Failed to write description file" );
            throw new RuntimeException( e );
        }
    }

    /**
     * Handle the JAC XML sub-millimetre line database. The format of this
     * is
     * <pre>
     * <LineCatalog>
     *   <species name = "CO">
     *      <transition name="2  - 1 " frequency="230538.0"/>
     *      ......
     *   </species>
     *   <species name = "13-CO">
     *      ......
     *   </species>
     *   ......
     * </LineCatalog>
     * </pre>
     * all measurements are in MHz.
     */
    private void processJACDataBase( File database, File outDir )
        throws ParserConfigurationException, SAXException,
               SplatException, IOException
    {
        //  Read the input document.
        DocumentBuilder docBuilder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse( database );

        //  Work through the top-level elements of the input XML.
        NodeList speciesList =
            doc.getDocumentElement().getElementsByTagName( "species" );

        //  If there are more than MAX_ITEMS then we split into various
        //  sub-menus so that we can see them all and the eye doesn't
        //  get tried scanning down.
        int nspecies = speciesList.getLength();
        File subDir = outDir;
        int ndirs = 1;
        if ( nspecies > MAX_ITEMS ) {
            subDir = new File( outDir.toString() + ndirs );
            ndirs++;
        }

        for ( int i = 0, k = 0; i < nspecies; i++, k++ ) {
            Element species = (Element) speciesList.item( i );

            //  Create name for the pseudo output file.
            String newName = species.getAttribute( "name" );
            File newFile = new File( subDir, newName );

            //  Make a memory line identifier to fill with the extracted
            //  content.
            LineIDMEMSpecDataImpl impl =
                new LineIDMEMSpecDataImpl( getShortName( newFile ) );

            //  Read transitions.
            NodeList transList = species.getElementsByTagName( "transition" );
            int nele = transList.getLength();
            if ( nele == 0 ) {
                logger.info( "Warning species: " + newName +
                             " has no transitions, skipped" );
                continue;
            }
            double freq[] = new double[nele];
            double data[] = new double[nele];
            String names[] = new String[nele];
            for ( int j = 0; j < nele; j++ ) {
                Element trans = (Element) transList.item( j );
                freq[j] = Double.parseDouble(trans.getAttribute("frequency"));
                names[j] = cleanLabel( trans.getAttribute( "name" ) );
                data[j] = SpecData.BAD;
            }

            //  Create the FrameSet with SpecFrame in Mhz.
            FrameSet frameSet = createJACFrameSet( freq );

            //  Setup the the implementation.
            impl.setLabels( names );
            impl.setSimpleUnitDataQuick( frameSet, freq, "Unknown", data );
            impl.checkHaveDataPositions();

            //  Create the SpecData.
            LineIDSpecData specData = new LineIDSpecData( impl );

            //  Add the description to the description catalogue.
            addDescription( newFile, specData );

            //  Write the output file.
            newFile.getParentFile().mkdir();
            FileOutputStream fos = new FileOutputStream( newFile );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( specData );
            oos.close();
            fos.close();
            logger.fine( "Processed: " + newFile );

            if ( k > MAX_ITEMS ) {
                subDir = new File( outDir.toString() + ndirs );
                k = 0;
                ndirs++;
            }
        }
    }

    /**
     * Create a FrameSet that describes a JAC line identifier species.
     */
    private FrameSet createJACFrameSet( double[] freq )
    {
        //  The SpecFrame, standard line identifier std of rest, in Mhz.
        SpecFrame specFrame = new SpecFrame();
        specFrame.setC( "System", "FREQ" );
        specFrame.setC( "Unit", "MHz" );
        specFrame.setC( "StdOfRest", "Source" );
        specFrame.setC( "SourceVRF", "Topocentric" );
        specFrame.setD( "SourceVel", 0.0 );

        //  Pixel index frame.
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Index" );

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates. Need to handle case of only 1
        //  value.
        if ( freq.length == 1 ) {
            double[] newCoords = new double[2];
            newCoords[0] = freq[0];
            newCoords[1] = freq[0];
            freq = newCoords;
        }
        LutMap lutmap = new LutMap( freq, 1.0, 1.0 );
        Mapping simple = lutmap.simplify();

        //  Now create a frameset and add all these to it.
        FrameSet frameSet = new FrameSet( baseframe );
        frameSet.addFrame( 1, simple, specFrame );

        return frameSet;
    }

    /**
     * Convert the name of a File into a suitable short name. That is remove
     * the directory information, replace any spaces with underscore and
     * append "_lines".
     */
    private String getShortName( File file )
    {
        return file.getName().replace( ' ', '_' ) + "_lines";
    }

    /**
     * Clean a label by replacing all repeated blanks with a single blank,
     * the string " - " with "-", and all single blanks with an underscore.
     */
    private String cleanLabel( String label )
    {
        String s = label.trim();
        s = s.replaceAll( "  ", " " );  //  All repeated blanks with one.
        s = s.replaceAll( " - ", "-" );    //  " - " to "-"
        s = s.replace( ' ', '_' );      //  " " to "_"
        return s;
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
            logger.warning( inDir + " is not a directory" );
            return;
        }

        if ( ! outDir.isDirectory() ) {
            try {
                outDir.mkdirs();
            }
            catch (Exception e) {
                logger.warning( outDir  + " no output directory created" );
                e.printStackTrace();
                return;
            }
        }
        LineIDTree tree = new LineIDTree( inDir, outDir );
        return;
    }
}
