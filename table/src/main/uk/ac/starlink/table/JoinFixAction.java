package uk.ac.starlink.table;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class defining the possible actions for doctoring
 * column names when joining tables.
 * Joining tables can cause confusion if columns with the same names
 * exist in some of them.  An instance of this class defines
 * how the join should behave in this case.
 *
 * @author   Mark Taylor
 * @since    8 Sep 2005
 * @see      JoinStarTable
 */
public abstract class JoinFixAction {

    private final String name_;

    /** Action which causes names to be left alone. */
    public static final JoinFixAction NO_ACTION =
        new JoinFixAction( "No Action" ) {
            public String getFixedName( String orig, Collection others ) {
                return orig;
            }
        };

    /**
     * Constructor.
     *
     * @param  name  label for this action
     */
    protected JoinFixAction( String name ) {
        name_ = name;
    }

    /**
     * Returns a, possibly modified, name for a column in the context of
     * existing column names.
     *
     * @param  origName    input name
     * @param  otherNames  list of names which may be duplicates of 
     *                     <code>origName</code>
     * @return output name - may or may not be the same as <code>origName</code>
     */
    public abstract String getFixedName( String origName,
                                         Collection otherNames );

    /**
     * Returns this action's name.
     */
    public String toString() {
        return name_;
    }

    /**
     * Utility method to determine whether a given name is a duplicate of
     * any in a given collection of other names.
     *
     * @param  name  name to test
     * @param  others  potential duplicates of <code>name</code>
     * @param  caseSensitive  true iff matching is to be done in a 
     *         case-sensitive fashion
     * @return  true iff <code>name</code> matches any of the entries in
     *          <code>others</code>
     */
    public static boolean isDuplicate( String name, Collection others,
                                       boolean caseSensitive ) {
        if ( caseSensitive ) {
            return others.contains( name );
        }
        else {
            name = String.valueOf( name ).toLowerCase();
            for ( Iterator it = others.iterator(); it.hasNext(); ) {
                if ( String.valueOf( it.next() ).toLowerCase()
                                                .equals( name ) ) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Utility method which repeatedly doctors a name until it fails to
     * match any of the others in a given collection.  Currently some
     * arbitrary scheme of appending characters is used.
     *
     * @param  name  input name
     * @param  others  list of names that must not match
     * @param  caseSensitive  whether matching others is case sensitive
     * @return  name similar to <code>name</code> but not matching
     *          <code>others</code>
     */
    public static String ensureUnique( String name, Collection others,
                                       boolean caseSensitive ) {
        if ( ! isDuplicate( name, others, caseSensitive ) ) {
            return name;
        }
        for ( int i = 0; i < Integer.MAX_VALUE; i++ ) {

            String namex = name + toLetters( i );
            if ( ! isDuplicate( namex, others, caseSensitive ) ) {
                return namex;
            }
        }
        assert false;
        return name;
    }

    /**
     * Returns a string representing a number.  
     * Currently this is in base 26 with digit a=0 etc.
     *
     * @param  index  numeric value
     * @return string value
     */
    static String toLetters( int index ) {
        StringBuffer sbuf = new StringBuffer( Integer.toString( index, 26 ) );
        for ( int j = 0; j < sbuf.length(); j++ ) {
            char c = sbuf.charAt( j );
            if ( c >= '0' && c <= '9' ) {
                c = (char) ( c - '0' + 'a' );
            }
            else if ( c >= 'a' && c <= ( 'z' - 10 ) ) {
                c += 10;
            }
            assert c >= 'a' && c <= 'z';
            sbuf.setCharAt( j, c );
        }
        return sbuf.toString();
    }

    /**
     * Returns an action for renaming duplicated column names with 
     * default options.
     *
     * @param  appendage  string to append to duplicate columns
     * @return  fixer
     */
    public static JoinFixAction makeRenameDuplicatesAction( String appendage ) {
        return makeRenameDuplicatesAction( appendage, false, true );
    }

    /**
     * Returns an action for renaming duplicated column names with 
     * additional options.
     * The action indicates that column names which would be
     * duplicated elsewhere in the result table should be modified
     * by appending a given string.
     *
     * @param  appendage  string to append to duplicate columns
     * @param  caseSensitive  whether duplicate location should be case
     *                        sensitive
     * @param  ensureUnique  if true, every effort will be made to ensure
     *                       that the output name matches none of the others;
     *                       if false, the output name may still match 
     *                       (but differently from the input one)
     * @return  fixer
     */
    public static JoinFixAction makeRenameDuplicatesAction(
                                               final String appendage,
                                               final boolean caseSensitive,
                                               final boolean ensureUnique ) {
        return new JoinFixAction( "Fix Duplicates: " + appendage ) {
            public String getFixedName( String orig, Collection others ) {
                if ( isDuplicate( orig, others, caseSensitive ) ) {
                    String name = orig + appendage;
                    return ensureUnique
                         ? ensureUnique( name, others, caseSensitive )
                         : name;
                }
                else {
                    return orig;
                }
            }
        };
    }

    /**
     * Returns an action for renaming all column names with default options.
     *
     * @param  appendage  string to append to columns
     * @return  fixer
     */
    public static JoinFixAction makeRenameAllAction( String appendage ) {
        return makeRenameAllAction( appendage, false, true );
    }
                       
    /**
     * Returns an action for renaming all columns with additional options.
     * The action indicates that all names will be modified by 
     * appending a given string. 
     *
     * @param  appendage  string to append to columns
     * @param  caseSensitive  whether duplicate location should be case
     *                        sensitive (only relevant if 
     *                        <code>ensureUnique</code> is true)
     * @param  ensureUnique  if true, every effort will be made to ensure
     *                       that the output name matches none of the others;
     * @return  fixer
     */
    public static JoinFixAction makeRenameAllAction(
                                              final String appendage,
                                              final boolean caseSensitive,
                                              final boolean ensureUnique ) {
        return new JoinFixAction( "Rename All: " + appendage ) {
            public String getFixedName( String orig, Collection others ) {
                String name = orig + appendage;
                return ensureUnique
                     ? ensureUnique( name, others, caseSensitive )
                     : name;
            }
        };
    }

    /**
     * Returns an action which will deduplicate names by appending a 
     * numeric value to them.  The number is guaranteed unique; the
     * value used is one higher than the currently highest used one
     *
     * @param  delimiter  string used to separate main part of name from
     *                    numeric part
     * @param  caseSensitive  whether duplicate location is case sensitive
     * @return   fixer
     */
    public static JoinFixAction makeNumericDeduplicationAction(
                                             String delimiter,
                                             final boolean caseSensitive ) {
        final String delim = delimiter == null ? "" : delimiter;
        String name = delim.length() == 0 ? "Numeric Renamer" 
                                          : "Numeric Renamer: " + delim;
        final Pattern regex =
            Pattern.compile( "(.*)\\Q" + delim + "\\E([0-9]+)" );
        return new JoinFixAction( name ) {
            public String getFixedName( String orig, Collection others ) {
                if ( ! isDuplicate( orig, others, caseSensitive ) ) {
                    return orig;
                }
                Matcher oMatch = regex.matcher( orig );
                String origStrip = oMatch.matches() ? oMatch.group( 1 )
                                                    : orig;
                long maxNum = 0;
                for ( Iterator it = others.iterator(); it.hasNext(); ) {
                    String other = String.valueOf( it.next() );
                    Matcher matcher = regex.matcher( other );
                    if ( matcher.matches() ) {
                        String strip = matcher.group( 1 );
                        boolean same = caseSensitive
                                     ? strip.equals( origStrip )
                                     : strip.equalsIgnoreCase( origStrip );
                        if ( same ) {
                            long num = Long.parseLong( matcher.group( 2 ) );
                            maxNum = Math.max( maxNum, num );
                        }
                    }
                }
                return origStrip + delim + ( maxNum + 1 );
            }
        };
    }
}
