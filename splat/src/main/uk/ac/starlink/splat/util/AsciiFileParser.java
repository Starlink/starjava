package uk.ac.starlink.splat.util;

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
 * <li>The number of fields (i.e. columns) in the table is fixed and
 *     it is an error is this isn't true.</li>
 * <li>The data formats of each field are not fixed and can be
 *     requested for conversion to various formats.
 * <li>Fields are separated by a given character (i.e. space, comma,
 *     tab) and multiple repeats of these are contracted to one
 *     instance (so multiple spaces are just one separator).
 * </ul>
 * This class is only suitable for files that are expected to contain
 * small numbers of data.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class AsciiFileParser
{
    /**
     * The number of fields in the file.
     */
    protected int nFields = 0;

    /**
     * The number of data containing rows in the file.
     */
    protected int nRows = 0;

    /**
     * The list of all extracted fields as Strings.
     */
    protected ArrayList pList = new ArrayList();

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
     * Create an instance and parse a given File.
     *
     * @param file reference a File that describes the input file.
     */
    public AsciiFileParser( File file )
    {
        parse( file );
    }

    /**
     * Parse a file using the current configuration.
     *
     * @param file reference a File that describes the input file.
     */
    public void parse( File file )
    {
        pList.clear();
        decode( file );
    }

    /**
     * Get the number of fields located in the file.
     */
    public int getNFields()
    {
        return nFields;
    }

    /**
     * Get the number of rows located in the file.
     */
    public int getNRows()
    {
        return nRows;
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
        if ( row < nRows && column < nFields ) {
            return (String) pList.get(indexOf(row,column));
        } else {
            return null;
        }
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
        if ( row < nRows && column < nFields ) {
            return Integer.parseInt((String)pList.get(indexOf(row,column)));
        } else {
            return 0;
        }
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
        if ( row < nRows && column < nFields ) {
            return Double.parseDouble((String)pList.get(indexOf(row,column)));
        } else {
            return 0.0;
        }
    }

    /**
     * Get the index of a field in the storage array.
     * @param row the row index of the field required.
     * @param column the column index of the field required.
     *
     * @return index (in pList) of the required element.
     */
    protected int indexOf( int row, int column )
    {
        return row * nFields + column;
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

        //  Read file input until end of file occurs.
        String raw = null;
        String clean = null;
        int nlines = 0;
        int nwords = 0;
        try {
            while ( ( raw = r.readLine() ) != null ) {

                //  Skip blank and comment lines.
                if ( raw.length() == 0 ||
                     raw.charAt(0) == singleComment ) {
                    continue;
                } else {

                    //  Trim any trailing comments.
                    int trail = raw.indexOf( inlineComment );
                    if ( trail != -1 ) {
                        raw = raw.substring( 0, trail -1 );
                    }

                    //  Tokenize the line.
                    StringTokenizer st = null;
                    if ( delims == null ) {
                        st = new StringTokenizer( raw );
                    } else {
                        st = new StringTokenizer( raw, delims );
                    }
                    int count = st.countTokens();
                    if ( nRows != 0 && count != nRows ) {
                        System.err.println( "File contains incorrect "+
                                            "number of fields (line '" + 
                                            raw + "') -- ignored" );
                        continue; // wrong number of columns
                    }
                    nFields = count;
                    for ( int i = 0; i < count; i++ ) {
                        pList.add( st.nextToken() );
                    }
                    nlines++;
                }
            }
            nRows = nlines;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            r.close();
            f.close();
        } catch (Exception e) {
            //  Do nothing.
        }
    }
}
