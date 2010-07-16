package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;

/**
 * Can identify columns of a table using string identifiers.
 * Permitted identifiers are (currently) column name (case insensitive),
 * column index (1-based), ucd$* style UCD specifiers (see JELRowReader) 
 * and where requested cases simple wildcarding expressions.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class ColumnIdentifier {

    private final StarTable table_;
    private final int ncol_;
    private final String[] colNames_;
    private final String[] colUcds_;
    private final String[] colUtypes_;
    private boolean caseSensitive_;

    /**
     * Constructor.
     *
     * @param  table  table whose columns this identifier can identify
     */
    public ColumnIdentifier( StarTable table ) {
        table_ = table;
        ncol_ = table_.getColumnCount();
        colNames_ = new String[ ncol_ ];
        colUcds_ = new String[ ncol_ ];
        colUtypes_ = new String[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo info = table_.getColumnInfo( icol );
            String name = info.getName();
            if ( name != null ) {
                colNames_[ icol ] = name.trim();
            }
            String ucd = info.getUCD();
            if ( ucd != null ) {
                colUcds_[ icol ] = ucd.trim();
            }
            String utype = info.getUtype();
            if ( utype != null ) {
                colUtypes_[ icol ] = utype.trim();
            }
        }
    }

    /**
     * Sets whether case is significant in column names.
     * By default it is not.
     *
     * @param  caseSensitive  is matching case sensitive?
     */
    public void setCaseSensitive( boolean caseSensitive ) {
        caseSensitive_ = caseSensitive;
    }

    /**
     * Determines whether case is significant in column names.
     * By default it is not.
     *
     * @return   true iff matching is case sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive_;
    }

    /**
     * Returns the index of a column given an identifying string.
     * If the string can't be identified as a column of this object's
     * table, an <tt>IOException</tt> is thrown.
     *
     * @param   colid   identifying string
     * @return  column index
     * @throws  IOException  if <tt>colid</tt> does not name a column
     */
    public int getColumnIndex( String colid ) throws IOException {
        int ix = getScalarColumnIndex( colid );
        if ( ix < 0 ) {
            throw new IOException( "No such column " + colid );
        }
        else {
            return ix;
        }
    }

    /**
     * Returns an array of column indices from a 
     * <code>&lt;colid-list&gt;</code> string.
     * The string is split up into whitespace-separated tokens, 
     * and each element must either be the identifier of an individual
     * column or a non-trivial glob-like pattern which may match
     * zero or more columns.
     * 
     * @param   colidList  string containing a representation of a list
     *          of columns
     * @return  array of column indices
     * @throws  IOException  if <tt>colid</tt> doesn't look like a 
     *          colid-list specifier
     */
    public int[] getColumnIndices( String colidList ) throws IOException {
        String[] colIds = colidList.trim().split( "\\s+" );
        List icolList = new ArrayList();
        for ( int i = 0; i < colIds.length; i++ ) {
            String colId = colIds[ i ];
            int icol = getScalarColumnIndex( colId );
            if ( icol >= 0 ) {
                icolList.add( new Integer( icol ) );
            }
            else {
                int[] jcols;
                try {
                    jcols = findMatchingColumns( colId );
                }
                catch ( IllegalArgumentException e ) {
                    String ucdSpec = JELRowReader
                       .stripPrefix( colId, StarTableJELRowReader.UCD_PREFIX );
                    String utypeSpec = JELRowReader
                       .stripPrefix( colId,
                                     StarTableJELRowReader.UTYPE_PREFIX );
                    final String msg;
                    if ( ucdSpec != null ) {
                        msg = "No column with UCD matching " + ucdSpec;
                    }
                    else if ( utypeSpec != null ) {
                        msg = "No column with Utype matching " + utypeSpec;
                    }
                    else {
                        msg = "Not a column ID or wildcard: " + colId;
                    }
                    throw new IOException( msg );
                }
                for ( int j = 0; j < jcols.length; j++ ) {
                    icolList.add( new Integer( jcols[ j ] ) );
                }
            }
        }
        int ncol = icolList.size();
        int[] icols = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            icols[ i ] = ((Integer) icolList.get( i )).intValue();
        }
        return icols;
    }

    /**
     * Returns an array of flags, the same length as the number of
     * columns in the table, with an element set true for each column
     * which is specified in <code>colIdList</code>.
     * This convenience function just works on the result of
     * {@link #getColumnIndices}.
     *
     * @param   colIdList  string containing a representation of a list
     *          of columns
     * @return  array of column inclusion flags
     * @throws  IOException  if <tt>colid</tt> doesn't look like a 
     *          colid-list specifier
     */
    public boolean[] getColumnFlags( String colIdList ) throws IOException {
        boolean[] colFlags = new boolean[ ncol_ ];
        int[] icols = getColumnIndices( colIdList );
        for ( int i = 0; i < icols.length; i++ ) {
            colFlags[ icols[ i ] ] = true;
        }
        return colFlags;
    }

    /**
     * Returns the column index associated with a given string.
     * If the string can't be identified as a column name, -1 is returned.
     *
     * @param   colid  identifying string
     * @return  column index, or -1
     * @throws  IOException  if the index is out of range
     */
    private int getScalarColumnIndex( String colid ) throws IOException {
        colid = colid.trim();
        if ( colid.length() == 0 ) {
            throw new IOException( "Blank column ID not allowed" );
        }
        else if ( colid.charAt( 0 ) == '-' ) {
            throw new IOException( "Found " + colid + 
                                   " while looking for column ID" );
        }
        if ( colid.matches( "\\Q" + JELRowReader.COLUMN_ID_CHAR + "\\E?" +
                            "[0-9]+" ) ) {
            if ( colid.charAt( 0 ) == JELRowReader.COLUMN_ID_CHAR ) {
                colid = colid.substring( 1 );
            }
            int ix1 = Integer.parseInt( colid );
            if ( ix1 < 1 || ix1 > ncol_ ) {
                throw new IOException( "Column index " + ix1 + " out of range "
                                      + "1.." + ncol_ );
            }
            return ix1 - 1;
        }
        else if ( JELRowReader
                 .stripPrefix( colid,
                               StarTableJELRowReader.UCD_PREFIX ) != null ) {
            Pattern ucdRegex =
                StarTableJELRowReader
               .getUcdRegex( JELRowReader
                            .stripPrefix( colid,
                                          StarTableJELRowReader.UCD_PREFIX ) );
            for ( int icol = 0; icol < ncol_; icol++ ) {
                String ucd = colUcds_[ icol ];
                if ( ucd != null && ucdRegex.matcher( ucd ).matches() ) {
                    return icol;
                }
            }
            return -1;
        }
        else if ( JELRowReader
                 .stripPrefix( colid,
                               StarTableJELRowReader.UTYPE_PREFIX ) != null ) {
            Pattern utypeRegex =
                StarTableJELRowReader
               .getUtypeRegex( JELRowReader
                              .stripPrefix( colid,
                                            StarTableJELRowReader
                                           .UTYPE_PREFIX ) );
            for ( int icol = 0; icol < ncol_; icol++ ) {
                String utype = colUtypes_[ icol ];
                if ( utype != null && utypeRegex.matcher( utype ).matches() ) {
                    return icol;
                }
            }
            return -1;
        }
        else {
            for ( int icol = 0; icol < ncol_; icol++ ) {
                String name = colNames_[ icol ];
                if ( isCaseSensitive() ? colid.equals( name )
                                       : colid.equalsIgnoreCase( name ) ) {
                    return icol;
                }
            }
            return -1;
        }
    }

    /**
     * Returns an array of column indices whose names match the given
     * match pattern.  It is not an error for no columns to match, but
     * it is an error if the match pattern contains no wild cards.
     *
     * @param   glob  pattern to match column names
     * @return  array of matched column indices 
     * @throws  IllegalArgumentException   if <code>glob</code> doesn't
     *          contain any wildcard expressions
     * @see  #globToRegex
     */
    private int[] findMatchingColumns( String glob ) {
        Pattern regex = globToRegex( glob, isCaseSensitive() );
        if ( regex == null ) {
            throw new IllegalArgumentException( "Not a wildcard expression: "
                                              + glob );
        }
        int ncol = table_.getColumnCount();
        List icolList = new ArrayList();
        for ( int icol = 0; icol < ncol; icol++ ) {
            String colName = table_.getColumnInfo( icol ).getName();
            if ( colName != null ) {
                if ( regex.matcher( colName.trim() ).matches() ) {
                    icolList.add( new Integer( icol ) );
                }
            }
        }
        int nfound = icolList.size();
        int[] icols = new int[ nfound ];
        for ( int i = 0; i < nfound; i++ ) {
            icols[ i ] = ((Integer) icolList.get( i )).intValue();
        }
        return icols;
    }

    /**
     * Turns a glob-type pattern into a regular expression Pattern.
     * Currently the only construction recognised is a "*" at one or
     * more places in the string, which will match any sequence of
     * characters.
     *
     * <strong>Note:</strong> If <code>glob</code> contains no wildcards, 
     * <code>null</code> will be returned.
     *
     * @param   glob  glob pattern
     * @param   caseSensitive  whether matching should be case sensitive
     * @return   equivalent regular expression pattern, or null if 
     *           <code>glob</code> is trivial
     */
    public static Pattern globToRegex( String glob, boolean caseSensitive ) {
        if ( glob.indexOf( '*' ) < 0 ) {
            return null;
        }
        else if ( glob.indexOf( "**" ) >= 0 ) {
            throw new IllegalArgumentException( "Bad pattern (adjacent " +
                                                "wildcards): " + glob );
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            boolean quoted = false;
            for ( int i = 0; i < glob.length(); i++ ) {
                char c = glob.charAt( i );
                if ( c == '*' ) {
                    if ( quoted ) {
                        sbuf.append( "\\E" );
                        quoted = false;
                    }
                    sbuf.append( ".*" );
                }
                else {
                    if ( ! quoted ) {
                        sbuf.append( "\\Q" );
                        quoted = true;
                    }
                    sbuf.append( c );
                }
            }
            String regex = sbuf.toString();
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile( regex, flags );
        }
    }
}
