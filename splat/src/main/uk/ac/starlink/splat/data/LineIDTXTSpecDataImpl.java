/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
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
import java.util.StringTokenizer;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;


/**
 * A LineIDSpecDataImpl that provides facilities for reading text files
 * that have a coordinate, value and an associated String, typically a
 * spectral line identification.
 * <p>
 * Text files are assumed to be plain and contain either two, three
 * or more whitespace separated columns. If two columns are present
 * then these are the wavelength and label, if three or more
 * then the second column should be position of the label and the
 * third the label.
 *  <p>
 * Whitespace separators are the space character, the tab character,
 * the newline character, the carriage-return character, and the
 * form-feed character. Any comments in the file must start in the
 * first column and be indicated by the characters "!" or "#". Blank
 * lines are also permitted.
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
    extends MEMSpecDataImpl
    implements LineIDSpecDataImpl
{
    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param fileName the name of the text file.
     */
    public LineIDTXTSpecDataImpl( String fileName )
    {
        super( fileName );
        this.fullName = fileName;
        readFromFile( fileName );
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
     * Return the line identification labels.
     */
    public String[] getLabels()
    {
        return labels;
    }

    /**
     * Set all the labels.
     */
    public void setLabels( String[] labels ) 
        throws SplatException
    {
        if ( coords == null || ( labels.length == coords.length ) ) {
            this.labels = labels;
        }
        else {
            throw new SplatException( "Array length must match coordinates" );
        }
    }

    /**
     * Get a specific label.
     */ 
    public String getLabel( int index )
    {
        if ( labels != null && ( index < labels.length ) ) {
            return labels[index];
        }
        return null;
    }

    /**
     * Set a specific label.
     */
    public void setLabel( int index, String label )
    {
        if ( getLabel( index ) != null ) {
            labels[index] = label;
        }
    }

    /**
     * Return if there were a complete set of data positions.
     */
    public boolean haveDataPositions()
    {
        return haveDataPositions;
    }

    /**
     * Save the state to disk-file.
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
     * Reference to the line ID strings.
     */
    protected String[] labels = null;

    /**
     * Whether there are a complete set of data positions.
     */
    protected boolean haveDataPositions = false;

    /**
     * Open an existing text file and read the contents.
     *
     * @param fileName diskfile name of the text file.
     */
    protected void readFromFile( String fileName )
    {
        //  Check file exists.
        file = new File( fileName );
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            file = null;
            return;
        }
        readData( file );
    }

    /**
     * Open an new text file and write data as its contents.
     *
     * @param fileName diskfile name of the text file.
     */
    protected void saveToFile( String fileName )
    {
        // If file exists, then we need to be able to overwrite it.
        file = new File( fileName );
        if ( file.exists() && file.isFile() && ! file.canWrite() ) {
            file = null;
            return;
        }
        writeData( file );
    }

    /**
     * Read in the data from the file.
     *
     * @param file File object.
     */
    protected void readData( File file )
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
        } catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        //  First pass of file. Read file input until end of file
        //  occurs.
        String raw = null;
        StringTokenizer st = null;
        int count = 0;
        int nlines = 0;
        int nwords = 0;
        try {
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' ) {
                    continue;
                } else {

                    // Read at least 2 words from line
                    // and no more than 3.
                    st = new StringTokenizer( raw );
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

            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' ) {
                    continue;
                } else {

                    // Should be nwords per line.
                    st = new StringTokenizer( raw );
                    coords[nlines] = Float.parseFloat( st.nextToken() );
                    if ( haveDataPositions ) {
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
        catch ( IOException e ) {
            e.printStackTrace();
            try {
                r.close();
                f.close();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
            return;
        }

        //  Create the AST frameset that describes the data-coordinate
        //  relationship.
        createAst();

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
     * Write spectral data to the file.
     *
     * @param file File object.
     */
    protected void writeData( File file )
    {
        //  Get a BufferedWriter to write the file line-by-line.
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            return;
        }


        // Add a header to the file.
        try {
            r.write( "# File created by " +Utilities.getReleaseName()+ "\n" );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Now write the data.
        for ( int i = 0; i < data.length; i++ ) {
            try {
                r.write( coords[i] + " " + data[i] + " " + labels[i] );
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
}

