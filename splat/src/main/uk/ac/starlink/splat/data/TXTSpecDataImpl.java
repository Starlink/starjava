/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
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
import java.util.StringTokenizer;

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
 *
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 * @see "The Bridge Design Pattern"
 */
public class TXTSpecDataImpl
    extends MEMSpecDataImpl
{
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
    File file = null;

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

        //  Storage of all values go into ArrayList vectors, until we
        //  know the exact sizes required.
        ArrayList[] vec = new ArrayList[3];
        vec[0] = new ArrayList();
        vec[1] = new ArrayList();
        vec[2] = new ArrayList();

        //  Read file input until end of file occurs.
        String clean = null;
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
                    //  TODO: restore shortname etc?
                } 
                else {
                    // Read at least one or two floating numbers from line
                    // and no more than 3.
                    st = new StringTokenizer( raw );
                    count = Math.min( st.countTokens(), 3 );
                    nwords = Math.max( count, nwords );
                    for ( int i = 0; i < count; i++ ) {
                        vec[i].add( new Float( st.nextToken() ) );
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
       createAst();
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
            r.write( "# File created by " +Utilities.getReleaseName()+ "\n" );
        } 
        catch (Exception e) {
            e.printStackTrace();
        }

        // Now write the data.
        if ( errors == null ) {
            for ( int i = 0; i < data.length; i++ ) {
                try {
                    r.write( coords[i]+" "+data[i]+"\n" );
                } 
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } 
        else {
            for ( int i = 0; i < data.length; i++ ) {
                try {
                    r.write( coords[i]+ " "+data[i]+" "+errors[i]+"\n");
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
}
