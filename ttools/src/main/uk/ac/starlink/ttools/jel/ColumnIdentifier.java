package uk.ac.starlink.ttools.jel;

import java.io.IOException;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.IntList;

/**
 * Can identify columns of a table using string identifiers.
 * Permitted identifiers are (currently) column name (case insensitive),
 * column index (1-based), ucd$* / utype$* style UCD/Utype specifiers
 * (see {@link JELRowReader}) 
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
     * table, an <code>IOException</code> is thrown.
     *
     * @param   colid   identifying string
     * @return  column index
     * @throws  IOException  if <code>colid</code> does not name a column
     */
    public int getColumnIndex( String colid ) throws IOException {
        return getScalarColumnIndex( colid, NotFoundMode.FAIL );
    }

    /**
     * Returns an array of column indices from a 
     * <code>&lt;colid-list&gt;</code> string.
     * The string is split up into whitespace-separated tokens, 
     * each of which must be one of:
     * <ul>
     * <li>identifier for an individual column
     * <li>glob-like pattern containing "*", matching zero or more columns
     * <li>column range of the form &lt;col1&gt;-&lt;colN&gt; (inclusive)
     * </ul>
     * and each element must either be the identifier of an individual
     * column or a non-trivial glob-like pattern which may match
     * zero or more columns, or a column range.
     * 
     * @param   colidList  string containing a representation of a list
     *          of columns
     * @return  array of column indices
     * @throws  IOException  if <code>colid</code> doesn't look like a 
     *          colid-list specifier
     */
    public int[] getColumnIndices( String colidList ) throws IOException {
        IntList colIds = new IntList();
        for ( String token : colidList.trim().split( "\\s+" ) ) {
            colIds.addAll( tokenToColumnIndices( token ) );
        }
        return colIds.toIntArray();
    }

    /**
     * Returns an array of column indices identified by a single
     * colid-list token.
     *
     * @param  token  indicating zero or more columns
     * @return   array of column indices; may be empty but not null
     * @throws  IOException  if <code>token</code> doesn't look like a 
     *          colid-list specifier
     */
    private int[] tokenToColumnIndices( String token ) throws IOException {

        /* Single column identifier? */
        int icol = getScalarColumnIndex( token, NotFoundMode.RETURN );
        if ( icol >= 0 ) {
            return new int[] { icol };
        }

        /* Glob pattern? */
        Pattern regex = globToRegex( token, isCaseSensitive() );
        if ( regex != null ) {
            return findMatchingColumns( regex );
        }

        /* Range? */
        int iMinus = token.indexOf( '-' );
        if ( iMinus >= 0 && token.length() > 1 ) {
            int ic0 = iMinus > 0
                    ? getScalarColumnIndex( token.substring( 0, iMinus ),
                                            NotFoundMode.FAIL )
                    : 0;
            int icN = iMinus < token.length() - 1
                    ? getScalarColumnIndex( token.substring( iMinus + 1 ),
                                            NotFoundMode.FAIL )
                    : ncol_ - 1;
            if ( icN < ic0 ) {
                throw new IOException( "Negative column range \"" + token + "\""
                                     + " (" + ic0 + "-" + icN + ")" );
            }
            int[] icols = new int[ icN - ic0 + 1 ];
            for ( int ic = ic0; ic <= icN; ic++ ) {
                icols[ ic - ic0 ] = ic;
            }
            return icols;
        }

        /* Fail, producing informative error message. */
        getScalarColumnIndex( token, NotFoundMode.FAIL );
        assert false;
        return new int[ 0 ];
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
     * @throws  IOException  if <code>colid</code> doesn't look like a 
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
     * If the string can't be identified as a column name,
     * behaviour depends on the <code>notFound</code> parameter.
     *
     * @param   colid  identifying string
     * @param   notFound  describes behaviour if column is not found
     * @return  column index, or -1 if not found and notFound == RETURN
     * @throws  IOException  if not found and notFound == FAIL
     */
    private int getScalarColumnIndex( String colid, NotFoundMode notFound )
            throws IOException {
        colid = colid.trim();

        /* Empty string not allowed. */
        if ( colid.length() == 0 ) {
            return notFound.returnValue( "Blank column ID not allowed" );
        }

        /* Column index or _index. */
        if ( colid.matches( "\\Q" + JELRowReader.COLUMN_ID_CHAR + "\\E?" +
                            "[0-9]+" ) ) {
            if ( colid.charAt( 0 ) == JELRowReader.COLUMN_ID_CHAR ) {
                colid = colid.substring( 1 );
            }
            int ix1 = Integer.parseInt( colid );
            if ( ix1 < 1 || ix1 > ncol_ ) {
                return notFound
                      .returnValue( "Column index " + ix1 + " out of range "
                                  + "1.." + ncol_ );
            }
            return ix1 - 1;
        }

        /* Prefixed UCD designator. */
        String ucd = JELRowReader
                    .stripPrefix( colid, StarTableJELRowReader.UCD_PREFIX );
        if ( ucd != null ) {
            Pattern ucdRegex = StarTableJELRowReader.getUcdRegex( ucd );
            for ( int icol = 0; icol < ncol_; icol++ ) {
                String colUcd = colUcds_[ icol ];
                if ( colUcd != null && ucdRegex.matcher( colUcd ).matches() ) {
                    return icol;
                }
            }
            return notFound.returnValue( "No column with UCD matching " + ucd );
        }

        /* Prefixed Utype designator. */
        String utype = JELRowReader
                      .stripPrefix( colid, StarTableJELRowReader.UTYPE_PREFIX );
        if ( utype != null ) {
            Pattern utypeRegex = StarTableJELRowReader.getUtypeRegex( utype );
            for ( int icol = 0; icol < ncol_; icol++ ) {
                String colUtype = colUtypes_[ icol ];
                if ( colUtype != null &&
                     utypeRegex.matcher( colUtype ).matches() ) {
                    return icol;
                }
            }
            return notFound
                  .returnValue( "No column with Utype matching " + utype );
        }

        /* If none of the above succeeded,
         * look for a direct match to the column name. */
        for ( int icol = 0; icol < ncol_; icol++ ) {
            String name = colNames_[ icol ];
            if ( isCaseSensitive() ? colid.equals( name )
                                   : colid.equalsIgnoreCase( name ) ) {
                return icol;
            }
        }

        /* No luck. */
        return notFound.returnValue( "No column matching \"" + colid + "\"" );
    }

    /**
     * Returns an array of column indices whose names match the given
     * match pattern.  It is not an error for no columns to match.
     *
     * @param   regex   pattern to match column names
     * @return  array of matched column indices 
     */
    private int[] findMatchingColumns( Pattern regex ) {
        int ncol = table_.getColumnCount();
        IntList icolList = new IntList();
        for ( int icol = 0; icol < ncol; icol++ ) {
            String colName = table_.getColumnInfo( icol ).getName();
            if ( colName != null ) {
                if ( regex.matcher( colName.trim() ).matches() ) {
                    icolList.add( icol );
                }
            }
        }
        return icolList.toIntArray();
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

    /**
     * Describes behaviour when a column is not found.
     */
    private enum NotFoundMode {

        /** Return value is -1. */
        RETURN() {
            int returnValue( String msg ) {
                return -1;
            }
        },

        /** Return value throws exception. */
        FAIL() {
            int returnValue( String msg ) throws IOException {
                throw new IOException( msg );
            }
        };

        /**
         * Provides the integer return value indicating not found status.
         *
         * @param  msg  message describing why finding failed
         * @return  -1  for mode RETURN
         * @throws   IOException  for mode FAIL
         */
        abstract int returnValue( String msg ) throws IOException;
    }
}
