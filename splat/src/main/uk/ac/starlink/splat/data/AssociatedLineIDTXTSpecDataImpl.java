/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     19-JAN-2006 (Peter W. Draper):
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
 * A {@link LineIDSpecDataImpl} that provides facilities for reading text files
 * that have a coordinate, with an optional associated String, typically
 * another spectrum specification (this class supports the line visitor
 * concept where lines that are visited may be in other spectra to the one
 * currently displayed). The associations are optional on a line-by-line
 * basis.
 * <p>
 * Text file lines are assumed to be plain and contain either one, two, three
 * or more whitespace separated, possibly quoted columns. If one column is
 * present then that is assumed to be the wavelength, and empty associations
 * and labels are used, if two columns are present then these are the
 * wavelength and an association spectrum, if three or more then the second
 * column is the label (in the sense of the superclass) and the third the
 * association.
 * <P.
 * An optional feature is support for a header section that defines
 * useful elements, such as the AST attributes of the coordinates, any known
 * data units and a name for the content. The header section starts at the
 * first line with #BEGIN and ends on the line #END. The attributes are simple
 * comment lines in between of the form "# name value".
 *  <p>
 * Whitespace separators are the space character and the tab character.
 * Any comments in the file must start in the first column and be indicated by
 * the characters "!", "#" or "*". Blank lines are also permitted. Quoted
 * filenames can use single or double quotes.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AssociatedLineIDTXTSpecDataImpl
    extends LineIDTXTSpecDataImpl
    implements AssociatedLineIDSpecDataImpl
{
    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param fileName the name of the text file.
     */
    public AssociatedLineIDTXTSpecDataImpl( String fileName )
        throws SplatException
    {
        this( new File( fileName ) );
    }

    /**
     * Create an object by opening a text file and reading its
     * content.
     *
     * @param file the text file.
     */
    public AssociatedLineIDTXTSpecDataImpl( File file )
        throws SplatException
    {
        super();
        this.shortName = file.getPath();
        this.fullName = this.shortName;
        readFromFile( file );
    }

    /**
     * Create an object by reading values from an existing
     * AssociatedLineIDSpecData object. The text file is associated
     * (so can be a save target), but not opened.
     *
     * @param fileName the name of the text file.
     */
    public AssociatedLineIDTXTSpecDataImpl( String fileName,
                                            AssociatedLineIDSpecData source )
        throws SplatException
    {
        super( fileName, source );
        setAssociations( source.getAssociations() );
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "Associated Lines (Text)";
    }

    /**
     * Return the associations.
     */
    public String[] getAssociations()
    {
        return associations;
    }

    /**
     * Set all the associations. Make a copy if asked, otherwise hold just the
     * reference.
     */
    public void setAssociations( String[] associations, boolean copy )
        throws SplatException
    {
        String[] localAssociations = associations;
        if ( copy ) {
            localAssociations = new String[associations.length];
            for ( int i = 0; i < associations.length; i++ ) {
                localAssociations[i] = associations[i];
            }
        }
        setLabels( localAssociations );
    }

    /**
     * Set all the associations
     */
    public void setAssociations( String[] associations )
        throws SplatException
    {
        if ( coords == null || ( associations.length == coords.length ) ) {
            this.associations = associations;
        }
        else {
            throw new SplatException( "Array length must match coordinates" );
        }
    }

    /**
     * Get a specific association, value will be null if no association is
     * available.
     */
    public String getAssociation( int index )
    {
        if ( associations != null && ( index < associations.length ) ) {
            return associations[index];
        }
        return null;
    }

    /**
     * Set a specific association.
     */
    public void setAssociation( int index, String association )
    {
        if ( getAssociation( index ) != null ) {
            associations[index] = association;
        }
    }

    /**
     * Return if there were some associations.
     */
    public boolean haveAssociations()
    {
        return haveAssociations;
    }

//
// Implementation specific methods and variables.
//
    /**
     * Reference to the line associations.
     */
    protected String[] associations = null;

    /**
     * Whether there are some associations.
     */
    protected boolean haveAssociations = false;

    /**
     * Whether there are some labels.
     */
    protected boolean haveLabels = false;

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
     * Read in the data from the file.
     *
     * @param file File object.
     */
    private void readData( File file )
        throws SplatException
    {
        //  Get a BufferedReader to read the file line-by-line. Note we are
        //  avoiding using StreamTokenizer directly, and doing our own
        //  parsing, as this doesn't deal with floating point values very
        //  well. Later, switch to QuotedStringTokenizer to handle quoted
        //  strings too.
        FileInputStream f = null;
        BufferedReader r = null;
        try {
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }

        // Look for a header section, leaves r positioned after headers.
        int nhead = readHeaders( r );

        //  First pass of file. Read file input until end of file to count
        //  number of lines.
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
                    nlines++;
                }
            }

            // Second pass. Read the values.
            coords = new double[nlines];
            associations = new String[nlines];
            labels = new String[nlines];
            data = new double[nlines];
            nlines = 0;

            f.close(); // Guaranteed rewind.
            r.close();
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );

            // Skip any header section. Not really needed as comments.
            if ( nhead > 0 ) {
                for ( int i = 0; i < nhead; i++ ) {
                    r.readLine();
                }
            }
            String token;
            int maxCount = 0;
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 || raw.charAt(0) == '!' ||
                     raw.charAt(0) == '#' || raw.charAt(0) == '*' ) {
                    continue;
                }
                else {
                    // Line of data.
                    st = new QuotedStringTokenizer( raw, DELIMS );
                    count = st.countTokens();
                    maxCount = Math.max( count, maxCount );
                    token = st.nextToken();
                    coords[nlines] = Float.parseFloat( token );
                    data[nlines] = SpecData.BAD;

                    //  Line has either label+association or just
                    //  association. Missing associations are left null,
                    //  missing labels are set to the coordinate.

                    if ( count >= 3 ) {
                        labels[nlines] = st.nextToken();
                        associations[nlines] = st.nextToken();
                    }
                    else {
                        labels[nlines] = token;
                        if ( count == 2 ) {
                            associations[nlines] = st.nextToken();
                        }
                    }
                    nlines++;
                }
            }
            haveDataPositions = false;
            haveAssociations = (maxCount >= 2 );
            haveLabels = (maxCount >= 3 );
       }
        catch (NumberFormatException ne) {
            throw new SplatException( "Error reading coordinate "+
                                      " from file: " + file + ", failed" +
                                      " to read a valid number : " +
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
            throw new SplatException( "Error reading line associations "+
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
                r.write( coords[i] + " " );
                if ( haveLabels ) {
                    r.write( labels[i] + " " );
                }
                if ( associations[i] != null ) {
                    r.write( associations[i] );
                }
                r.write( "\n" );
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
