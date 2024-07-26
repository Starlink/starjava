/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Generalised parser for data stored as a table in a plain text
 * file. The following assumptions are made about the structure of
 * these files:
 * <ul>
 * <li>They may have comments which are whole line and in-line.
 *     Comments are indicated by a single character. Totally empty
 *     lines are ignored.</li>
 * <li>The number of fields (i.e. columns) in the table may be fixed
 *     it can be an error if this isn't true.</li>
 * <li>The data formats of each field are not known and can be
 *     requested for conversion to various formats.
 * <li>Fields are separated by a given character (i.e. space, comma,
 *     tab) and multiple repeats of these are contracted to one
 *     instance (so multiple spaces are just one separator).
 * </ul>
 * This class is only suitable for files that are expected to contain
 * small numbers of data.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AsciiFileParser
{
    /**
     * Whether the number of fields is fixed.
     */
    protected boolean fixed = false;

    /**
     * The number of fixed fields in the file.
     */
    protected int nFields = 0;

    /**
     * A list that contains arrays of each set of Strings parsed from each row.
     */
    protected ArrayList<String[]> rowList = new ArrayList<String[]>();

    /**
     * The character used for single-line comments. Defaults to #.
     */
    protected char singleComment = '#';

    /**
     * The character used for inline comments. Defaults to !.
     */
    protected char inlineComment = '!';

    /**
     * The permissible delimeters between fields. The defaults are
     * from StringTokenizer: " \t\n\r\f", the space character, the tab
     * character, the newline character, the carriage-return
     * character, and the form-feed character.
     */
    protected String delims = null;

    /**
     * Create an instance.
     */
    public AsciiFileParser()
    {
        //  Do nothing.
    }

    /**
     * Create an instance.
     *
     * @param fixed whether fixed format is required.
     */
    @SuppressWarnings("this-escape")
    public AsciiFileParser( boolean fixed )
    {
        setFixed( fixed );
    }

    /**
     * Create an instance and parse a given File.
     *
     * @param file reference a File that describes the input file.
     */
    @SuppressWarnings("this-escape")
    public AsciiFileParser( File file )
    {
        parse( file );
    }

    /**
     * Create an instance and parse a given File.
     *
     * @param file reference a File that describes the input file.
     * @param fixed whether fixed format is required.
     */
    @SuppressWarnings("this-escape")
    public AsciiFileParser( File file, boolean fixed )
    {
        setFixed( fixed );
        parse( file );
    }

    /**
     * Set whether the file is expected to have a fixed number of fields.
     *
     * @param fixed whether fixed format is required.
     */
    public void setFixed( boolean fixed )
    {
        this.fixed = fixed;
    }

    /**
     * Get whether the file is expected to have a fixed number of fields.
     *
     * @return true if a fixed number of fields is expected.
     */
    public boolean isFixed()
    {
        return fixed;
    }

    /**
     * Parse a file using the current configuration.
     *
     * @param file reference a File that describes the input file.
     */
    public void parse( File file )
    {
        rowList.clear();
        decode( file );
    }

    /**
     * Get the number of fields located in the file. If not fixed this is the
     * minimum.
     */
    public int getNFields()
    {
        return nFields;
    }

    /**
     * Get the number of fields in a row.
     */
    public int getNFields( int row )
    {
        if ( rowList.size() > row ) {
            return ( rowList.get( row ) ).length;
        }
        return 0;
    }

    /**
     * Get the number of rows located in the file.
     */
    public int getNRows()
    {
        return rowList.size();
    }

    /**
     * Get the parsed Strings in a row.
     */
    public String[] getRow( int row )
    {
        if ( rowList.size() > row ) {
            return rowList.get( row );
        }
        return null;
    }


    /**
     * Get the String value of a field.
     *
     * @param row the row index of the field required.
     * @param column the column index of the field required.
     *
     * @return the field value if available, otherwise null.
     */
    public String getStringField( int row, int column )
    {
        String[] line = getRow( row );
        if ( line != null ) {
            if ( column < line.length ) {
                return line[column];
            }
        }
        return null;
    }

    /**
     * Get the integer value of a field.
     *
     * @param row the row index of the field required.
     * @param column the column index of the field required.
     *
     * @return the field value if available, otherwise 0.
     */
    public int getIntegerField( int row, int column )
    {
        String value = getStringField( row, column );
        if ( value != null ) {
            return Integer.parseInt( value );
        }
        return 0;
    }

    /**
     * Get the double precision value of a field.
     *
     * @param row the row index of the field required.
     * @param column the column index of the field required.
     *
     * @return the field value if available, otherwise 0.0.
     */
    public double getDoubleField( int row, int column )
    {
        String value = getStringField( row, column );
        if ( value != null ) {
            return Double.parseDouble( value );
        }
        return 0.0;
    }

    /**
     * Get the boolean value of a field. Any string starting with "t" or "T"
     * is considered true, otherwise the value is false.
     *
     * @param row the row index of the field required.
     * @param column the column index of the field required.
     *
     * @return true or false
     */
    public boolean getBooleanField( int row, int column )
    {
        String value = getStringField( row, column );
        if ( value.charAt(0) == 't' || value.charAt(0) == 'T' ) {
            return true;
        }
        return false;
    }

    /**
     * Set the character used for single line comments.
     */
    public void setSingleCommentChar( char singleComment )
    {
        this.singleComment = singleComment;
    }

    /**
     * Get the character used for single line comments.
     */
    public char getSingleCommentChar()
    {
        return singleComment;
    }

    /**
     * Set the character used for in-line comments.
     */
    public void setInlineCommentChar( char inlineComment )
    {
        this.inlineComment = inlineComment;
    }

    /**
     * Get the character used for in-line comments.
     */
    public char getInlineCommentChar()
    {
        return inlineComment;
    }

    /**
     * Set the characters used as field delimeters.
     *
     * @param delims list of characters to be used as field
     *               delimiters.
     */
    public void setDelimeters( String delims )
    {
        this.delims = delims;
    }

    /**
     * Get the character used as field delimeters.
     *
     * @return the delimeter string, if set, null if defaults apply.
     */
    public String getDelimeters()
    {
        return delims;
    }

    /**
     * Open, read and decode the contents of the file.
     *
     * @param file reference a File that describes the input file.
     */
    protected void decode( File file )
    {
        //  Check file exists. If not do nothing.
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            return;
        }

        //  Get a BufferedReader to read the file line-by-line.
        FileInputStream f = null;
        BufferedReader r = null;
        try {
            f = new FileInputStream( file );
            r = new BufferedReader( new InputStreamReader( f ) );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        //  Read file input until end of file occurs.
        String raw = null;
        String clean = null;
        int nlines = 0;
        int nwords = 0;
        int trail = 0;
        int count = 0;
        nFields = 4096;                // Large number.
        StringTokenizer st = null;
        String[] items = null;
        try {
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 ||
                     raw.charAt(0) == singleComment ) {
                    continue;
                }
                else {

                    //  Trim any trailing comments.
                    trail = raw.indexOf( inlineComment );
                    if ( trail != -1 ) {
                        raw = raw.substring( 0, trail -1 );
                    }

                    //  Tokenize the line.
                    if ( delims == null ) {
                        st = new StringTokenizer( raw );
                    }
                    else {
                        st = new StringTokenizer( raw, delims );
                    }
                    count = st.countTokens();

                    // If this isn't the first proper line and we want a fixed
                    // number of fields, then test it has changed.
                    if ( fixed && nlines != 0 && count != nFields ) {
                        System.err.println( "File contains incorrect "+
                                            "number of fields (line '" +
                                            raw + "') -- ignored" );
                        continue; // wrong number of columns
                    }
                    if ( fixed ) {
                        nFields = count;
                    }
                    else {
                        nFields = Math.min( nFields, count );
                    }
                    items = new String[count];
                    for ( int i = 0; i < count; i++ ) {
                        items[i] = st.nextToken();
                    }
                    rowList.add( items );
                    nlines++;
                }
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            r.close();
            f.close();
        }
        catch (Exception e) {
            //  Do nothing.
        }
    }
}
