/*
 * Copyright (C) 2003-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     11-APR-2003 (Peter W. Draper):
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
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mortbay.util.QuotedStringTokenizer;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;

/**
 * A LineIDSpecDataImpl that provides facilities for reading text files
 * that have a coordinate, value and an associated String, typically a
 * spectral line identification.
 * <p>
 * Text files are assumed to be plain and contain either one, two, three
 * or more whitespace separated columns. If one column is present then that is
 * assumed to be the wavelength, and empty labels are used, if two columns are
 * present then these are the wavelength and label, if three or more then the
 * second column should be position of the label and the third the label.
 * <P.
 * An optional feature is support for a header section that defines
 * useful elements, such as the AST attributes of the coordinates, any known
 * data units and a name for the content. The header section starts at the
 * first line with #BEGIN and ends on the line #END. The attributes are simple
 * comment lines in between of the form "# name value".
 *  <p>
 * Whitespace separators are the space character and the tab character.
 * Any comments in the file must start in the first column and be indicated by
 * the characters "!", "#" or "*". Blank lines are also permitted. Strings can
 * be quoted using single or double quotes, so can have whitespace characters
 * embedded.
 *  <p>
 * Since the actual storage for text files is memory resident this
 * class extends MEMSpecDataImpl, providing the ability to
 * change the content as this is an EditableSpecDataImpl (although
 * this ability may not be expressed in any other interfaces)...
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineIDTXTSpecDataImpl
    extends LineIDMEMSpecDataImpl
{
    /**
     * Empty constructor for sub-classes.
     */
    protected LineIDTXTSpecDataImpl()
        throws SplatException
    {
        // Do nothing, repeat sequence of calls to next constructor.
        super( null );
    }

    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param fileName the name of the text file.
     */
    public LineIDTXTSpecDataImpl( String fileName )
        throws SplatException
    {
        super( fileName );
        this.fullName = fileName;
        readFromFile( new File( fileName ) );
    }

    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param file the text file.
     */
    public LineIDTXTSpecDataImpl( File file )
        throws SplatException
    {
        super( file.getPath() );
        this.fullName = file.getAbsolutePath();
        readFromFile( file );
    }

    /**
     * Create an object by reading values from an existing LineIDSpecData
     * object. The text file is associated (so can be a save target),
     * but not opened.
     *
     * @param fileName the name of the text file.
     */
    public LineIDTXTSpecDataImpl( String fileName, LineIDSpecData source )
        throws SplatException
    {
        super( fileName, source );
        this.fullName = fileName;
        setLabels( source.getLabels() );
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "Line Identifiers (Text)";
    }

    /**
     * Save the state to disk-file.
     */
    public void save()
        throws SplatException
    {
        saveToFile( fullName );
    }

//
// Implementation specific methods and variables.
//
    /**
     * Default delimeters for tokenization.
     */
    public static final String DELIMS = " \t";

    
    /**
     * Map of any attributes read from file.
     */
    protected Map attributes = null;

    /**
     * Open an existing text file and read the contents.
     *
     * @param file the text file.
     */
    private void readFromFile( File file )
        throws SplatException
    {
        //  Check file exists.
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            file = null;
            throw new SplatException( "Cannot access file: " + file );
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
        File file = new File( fileName );
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
    private void readData( File file )
        throws SplatException
    {
        //  Get a BufferedReader to read the file line-by-line. Note
        //  we are avoiding using StreamTokenizer directly, and doing
        //  our own parsing, as this doesn't deal with floating point
        //  values very well. Later, we also use QuotedStringTokenizer
        //  so that we can have labels in double and single quotes.
        FileInputStream f = null;
        BufferedReader r = null;
        try {
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }

        // Look for a header section. Leaves r positioned after header.
        int nhead = readHeaders( r );

        //  First pass of file. Read file input until end of file
        //  occurs.
        int count = 0;
        int nlines = 0;
        int nwords = 0;
        String raw;
        QuotedStringTokenizer st;
        try {
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' || raw.charAt(0) == '*' ) {
                    continue;
                } else {

                    // Read at least 1 word from line and no more than 3.
                    st = new QuotedStringTokenizer( raw, DELIMS );
                    count = Math.min( st.countTokens(), 3 );
                    nwords = Math.max( count, nwords );
                    nlines++;
                }
            }

            // Second pass. Read the values.
            coords = new double[nlines];
            labels = new String[nlines];
            data = new double[nlines];
            nlines = 0;

            f.close(); // Guaranteed rewind.
            r.close();
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );

            haveDataPositions = ( nwords == 3 );
            boolean needLabels = ( nwords == 1 );

            // Skip any header section. Not really needed as comments.
            if ( nhead > 0 ) {
                for ( int i = 0; i < nhead; i++ ) {
                    r.readLine();
                }
            }
            String token;
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' || raw.charAt(0) == '*' ) {
                    continue;
                }
                else {
                    // Should be nwords per line.
                    st = new QuotedStringTokenizer( raw, DELIMS );
                    token = st.nextToken();
                    coords[nlines] = Float.parseFloat( token );
                    if ( needLabels ) {
                        //  No labels so use coordinate.
                        labels[nlines] = token;
                    }
                    else if ( haveDataPositions ) {
                        data[nlines] = Float.parseFloat( st.nextToken() );
                        labels[nlines] = st.nextToken();
                    }
                    else {
                        labels[nlines]  = st.nextToken();
                        data[nlines] = SpecData.BAD;
                    }
                    nlines++;
                }
            }
        }
        catch (NumberFormatException ne) {
            if ( haveDataPositions ) {
                throw new SplatException( "Error reading line identifiers"+
                                          " from file: " + file + ", failed" +
                                          " to read a valid number : " +
                                          ne.getMessage() +
                                          " (expected three columns)", ne );
            }
            throw new SplatException( "Error reading line identifiers"+
                                      " from file: " + file + ", failed" +
                                      " to read a valid number : \n" +
                                      ne.getMessage(), ne );
        }
        catch (Exception e) {
            try {
                r.close();
                f.close();
            }
            catch ( Exception ex ) {
                ex.printStackTrace();
            }
            throw new SplatException( "Error reading line identifiers"+
                                      " from file: " + file + " (" +
                                      e.getMessage() + ")", e );
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        if ( attributes == null ) {
            createAst();
        }
        else {
            createSpecFrameAst();
        }

        // Close input file.
        try {
            r.close();
            f.close();
        }
        catch (Exception e) {
            //  Do nothing.
        }
    }

    /**
     * Locate a header section in the given BufferedReader, and if found
     * record the WCS information in the attributes map. The BufferedReader is
     * left positioned after the last read and the number of lines read is
     * returned.
     */
    protected int readHeaders( BufferedReader r )
    {
        int nhead = 0;
        QuotedStringTokenizer st = null;
        String raw = null;
        try {
            //  Allow for rewind of one line.
            r.mark( 256 );
            raw = r.readLine();
            if ( "#BEGIN".equals( raw ) ) {
                attributes = new LinkedHashMap();
                //  Default set for laboratory lines.
                attributes.put( "StdOfRest", "Topocentric" );
                attributes.put( "SourceVRF", "Topocentric" );
                attributes.put( "SourceSys", "VELO" );
                attributes.put( "SourceVel", "0.0" );
                nhead++;

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
                    st = new QuotedStringTokenizer( raw, DELIMS );
                    st.nextToken(); // Skip comment
                    attributes.put( st.nextToken(), st.nextToken() );
                    nhead++;
                }
                if ( nhead == 0 ) attributes = null;
            }
            else {
                // Rewind back one line as no header.
                r.reset();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return nhead;
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
        writeHeaders( r );

        // Now write the data.
        for ( int i = 0; i < data.length; i++ ) {
            try {
                if ( data[i] == SpecData.BAD ) {
                    r.write( coords[i] + " " + labels[i] + "\n" );
                }
                else {
                    r.write( coords[i]+ " " +data[i]+ " " +labels[i]+ "\n" );
                }
            }
            catch (Exception e) {
                e.printStackTrace();
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


    /**
     * Write a header section to a BufferedWriter.
     */
    protected void writeHeaders( BufferedWriter r )
    {
        try {
            r.write( "#BEGIN\n" );
            r.write( "# File created by " +Utilities.getReleaseName()+ "\n" );
            r.write( "# name " + shortName + "\n" );
            writeAstAtt( r, "System" );
            writeAstAtt( r, "Unit" );
            writeAstAtt( r, "StdOfRest" );
            writeAstAtt( r, "SourceVRF" );
            writeAstAtt( r, "SourceSys" );
            writeAstAtt( r, "SourceVel" );
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
    }

    private void writeAstAtt( BufferedWriter r, String attr )
        throws Exception
    {
        String value = astref.getC( attr );
        if ( value != null && ! "".equals( value ) ) {
            r.write( "# " + attr + " " + value + "\n" );
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
    protected void createSpecFrameAst()
    {
        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates. Note we no longer
        //  label these as known.
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Index" );
        SpecFrame currentframe = new SpecFrame();

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

        //  Only one value. Double up LutMap.
        if ( coords.length == 1 ) {
            double[] newCoords = new double[2];
            newCoords[0] = coords[0];
            newCoords[1] = coords[0];
            coords = newCoords;
        }
        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );
        Mapping simple = lutmap.simplify();

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, simple, currentframe );
    }
}
