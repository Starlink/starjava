/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.starlink.ast.DSBSpecFrame;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 *  This class provides an implementation of SpecDataImpl to access
 *  spectra stored in text files.
 *  <p>
 *  Text files are assumed to be plain and contain either one, two,
 *  three or more whitespace separated columns. If two columns are present
 *  then these are the wavelength and data count, if three or more
 *  then the third column should be the error in the count. If only
 *  one column is present then this is the data count, and a false
 *  axis of indices is created (starting at 1).
 *  <p>
 *  Whitespace separators are the space character, the tab character,
 *  the newline character, the carriage-return character, and the
 *  form-feed character. Any comments in the file must start in the
 *  first column and be indicated by the characters "!" or "#". Blank
 *  lines are also permitted.
 *  <p>
 *  Since the actual storage for text files is memory resident this
 *  class extends MEMSpecDataImpl, providing the ability to
 *  change the content as this is an EditableSpecDataImpl (although
 *  this ability may not be expressed in any other interfaces)...
 *  <p>
 *  An optional feature is support for a header section that defines
 *  useful elements, such as the AST attributes of the coordinates and
 *  a name for the content. The header section starts at the first line
 *  with #BEGIN and ends on the line #END. The attributes are simple
 *  comment lines in between of the form "# name value".
 *
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 * @see "The Bridge Design Pattern"
 */
public class TXTSpecDataImpl
    extends MEMSpecDataImpl
{
    // XXX notes: this class and LineIDTXTSpecDataImpl need combining
    // (probably a proxy class that could be used for code sharing).

    //
    // Implementation of abstract methods.
    //

    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param fileName the name of the text file.
     */
    public TXTSpecDataImpl( String fileName )
        throws SplatException
    {
        super( fileName );
        this.fullName = fileName;
        readFromFile( fileName );
    }

    /**
     * Create an object by reading values from an existing SpecData
     * object. The text file is associated (so can be a save target),
     * but not opened.
     *
     * @param fileName the name of the text file.
     */
    public TXTSpecDataImpl( String fileName, SpecData source )
        throws SplatException
    {
        super( fileName, source );
        this.fullName = fileName;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "TEXT";
    }

    /**
     * Save the spectrum to disk-file.
     */
    public void save() throws SplatException
    {
        saveToFile( fullName );
    }

//
// Implementation specific methods and variables.
//
    /**
     * Reference to the file.
     */
    protected File file = null;

    /**
     * Map of any attributes read from file.
     */
    protected Map attributes = null;

    /**
     * Open an existing text file and read the contents.
     *
     * @param fileName diskfile name of the text file.
     */
    protected void readFromFile( String fileName )
        throws SplatException
    {
        //  Check file exists.
        file = new File( fileName );
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            file = null;
            throw new SplatException( "Cannot access file: " + fileName );
        }
        readData( file );
    }

    /**
     * Open an new text file and write data as its contents.
     *
     * @param fileName diskfile name of the text file.
     */
    protected void saveToFile( String fileName )
        throws SplatException
    {
        // If file exists, then we need to be able to overwrite it.
        file = new File( fileName );
        if ( file.exists() && file.isFile() && ! file.canWrite() ) {
            file = null;
            throw new SplatException( "Cannot write to file: " + fileName );
        }
        writeData( file );
    }

    /**
     * Read in the data from the file.
     *
     * @param file File object.
     */
    protected void readData( File file )
        throws SplatException
    {
        //  Get a BufferedReader to read the file line-by-line. Note
        //  we are avoiding using StreamTokenizer directly, and doing
        //  our own parsing, as this doesn't deal with floating point
        //  values very well.
        FileInputStream f = null;
        BufferedReader r = null;
        try {
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }

        // Look for a header section.
        int nhead = 0;
        String words[];
        String raw = null;
        boolean dsbspecframe = false;
        attributes = null;
        try {
            raw = r.readLine();
            if ( "#BEGIN".equals( raw ) ) {
                attributes = new LinkedHashMap();
                nhead++;

                String key;
                String value;
                while ( ( raw = r.readLine() ) != null ) {
                    if ( "#END".equals( raw ) ) {
                        nhead++;
                        break;
                    }

                    // Skip blank lines and other comment lines.
                    if ( raw.length() == 0 || raw.charAt(0) == '!'
                         || raw.charAt(0) == '*' ) {
                        continue;
                    }
                    words = raw.trim().split( "\\s+", 3 );
                    key = words[1];
                    value = words[2];
                    if ( "sideband".equalsIgnoreCase( key ) ) {
                        dsbspecframe = true;
                    }
                    attributes.put( key, value );
                    nhead++;
                }
                if ( nhead == 0 ) attributes = null;
            }
            else {
                // Rewind.
                r.close();
                f.close();
                f = new FileInputStream( file );
                r = new BufferedReader( new InputStreamReader( f ) );
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //  Storage of all values go into ArrayList vectors, until we
        //  know the exact sizes required.
        ArrayList[] vec = new ArrayList[3];
        vec[0] = new ArrayList();
        vec[1] = new ArrayList();
        vec[2] = new ArrayList();

        //  Read file input until end of file occurs.
        String clean = null;
        int count = 0;
        int nlines = 0;
        int nwords = 0;
        try {
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' ) {
                    continue;
                }
                else {
                    // Read at least one or two floating numbers from line
                    // and no more than 3.
                    words = raw.trim().split( "\\s+" );
                    count = Math.min( words.length, 3 );
                    nwords = Math.max( count, nwords );
                    for ( int i = 0; i < count; i++ ) {
                        vec[i].add( new Float( words[i] ) );
                    }
                    for ( int i = count; i < 3; i++ ) {
                        vec[i].add( new Float( 0.0 ) );
                    }
                    nlines++;
                }
            }
        }
        catch ( Exception e ) {
            try {
                r.close();
                f.close();
            }
            catch ( Exception ex ) {
                ex.printStackTrace();
            }
            throw new SplatException( "Error reading values from file: " +
                                      file + " (" + e.getMessage() + ")", e );
        }
        try {
            r.close();
            f.close();
        }
        catch (Exception e) {
            //  Do nothing, it's not fatal.
        }

        //  Create memory needed to store these coordinates.
        data = new double[nlines];
        coords = new double[nlines];
        if ( nwords == 3 ) {
            errors = new double[nlines];
        }

        //  Now copy data into arrays and record the data range.
        if ( nwords == 3 ) {
            for ( int i = 0; i < nlines; i++ ) {
                coords[i] = ((Float)vec[0].get(i)).floatValue();
                data[i] = ((Float)vec[1].get(i)).floatValue();
                errors[i] = ((Float)vec[2].get(i)).floatValue();
            }
        }
        else if ( nwords == 2 ) {
            for ( int i = 0; i < nlines; i++ ) {
                coords[i] = ((Float)vec[0].get(i)).floatValue();
                data[i] = ((Float)vec[1].get(i)).floatValue();
            }
        }
        else if ( nwords == 1 ) {
            for ( int i = 0; i < nlines; i++ ) {
                coords[i] = i + 1;
                data[i] = ((Float)vec[0].get(i)).floatValue();
            }
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        if ( attributes == null || attributes.size() == 0 ) {
            createAst();
        }
        else {
            createSpecFrameAst( dsbspecframe );
        }
    }

    /**
     * Write spectral data to the file.
     *
     * @param file File object.
     */
    protected void writeData( File file )
        throws SplatException
    {
        //  Get a BufferedWriter to write the file line-by-line.
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }

        // Add a header to the file.
        try {
            r.write( "#BEGIN\n" );
            r.write( "# File created by "+ Utilities.getReleaseName() + "\n" );
            r.write( "# name " + shortName + "\n" );

            // All known attributes, including DSBSpecFrames.
            String[] atts = {
                "System",
                "Unit",
                "AlignSystem",
                "Domain",
                "Epoch",
                "Format",
                "Label",
                "ObsLat",
                "ObsLon",
                "Symbol",
                "Title",
                "AlignStdOfRest",
                "RefDec",
                "RefRA",
                "RestFreq",
                "SourceVRF",
                "SourceSys",
                "SourceVel",
                "StdOfRest",
                "DSBCentre",
                "IF",
                "SideBand",
                "ImageFreq"
            };
            for ( int i = 0; i < atts.length; i++ ) {
                writeAstAtt( r, atts[i] );
            }

            String units = getDataUnits();
            if ( units != null ) {
                r.write( "# DataUnits " + units + "\n" );
            }
            String label = getDataLabel();
            if ( label != null ) {
                r.write( "# DataLabel " + label + "\n" );
            }
            r.write( "#END\n" );
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Now write the data.
        if ( errors == null ) {
            for ( int i = 0; i < data.length; i++ ) {
                try {
                    r.write( coords[i] + " " + data[i] + "\n" );
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            for ( int i = 0; i < data.length; i++ ) {
                try {
                    r.write( coords[i] +" "+ data[i] +" "+ errors[i] +"\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            r.newLine();
            r.close();
            f.close();
        }
        catch (Exception e) {
            //  Do nothing.
        }
    }

    private void writeAstAtt( BufferedWriter r, String attr )
    {
        try {
            String value = astref.getC( attr );
            if ( value != null && ! "".equals( value ) ) {
                r.write( "# " + attr + " " + value + "\n" );
            }
        }
        catch (Exception e) {
            // Do nothing. It's more important to write the data...
        }
    }

    /**
     * Create an AST frameset that relates the spectrum coordinate to
     * data values positions. This is straight-forward and just
     * creates a look-up-table to map the given coordinates to grid
     * positions. The lookup table mapping is simplified as this
     * should cause any linear transformations to be replaced with a
     * WinMap. This version also creates a SpecFrame and configures it
     * with attributes obtained from the file header section.
     */
    protected void createSpecFrameAst( boolean dsbspecframe )
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates. Note we no longer
        //  label these as known.
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Data count" );
        SpecFrame currentframe = null;
        if ( dsbspecframe ) {
            currentframe = new DSBSpecFrame();
        }
        else {
            currentframe = new SpecFrame();
        }

        // Set all the attributes.
        Set entrySet = attributes.entrySet();
        Iterator i = entrySet.iterator();
        while ( i.hasNext() ) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if ( "name".equalsIgnoreCase( key ) ) {
                shortName = value;
            }
            else if ( "file".equalsIgnoreCase( key ) ) {
                continue;
            }
            else if ( "dataunits".equalsIgnoreCase( key ) ) {
                setDataUnits( value );
            }
            else if ( "datalabel".equalsIgnoreCase( key ) ) {
                setDataLabel( value );
            }
            else {
                currentframe.setC( key, value );
            }
        }

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.
        if ( coords == null ) {
            createCoords();
        }
        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );
        Mapping simple = lutmap.simplify();

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, simple, currentframe );
    }
}
