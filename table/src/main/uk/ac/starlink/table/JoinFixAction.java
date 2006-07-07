package uk.ac.starlink.table;

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
public class JoinFixAction {

    /** Column names should be left alone. */
    public static final JoinFixAction NO_ACTION = 
        new JoinFixAction( "No Action", null, false, false );

    private final String name;
    private final String appendage;
    private final boolean renameDup;
    private final boolean renameAll;

    /**
     * Private constructor.
     */
    private JoinFixAction( String name, String appendage,
                       boolean renameDup, boolean renameAll ) {
        this.name = name;
        this.appendage = appendage;
        this.renameDup = renameDup;
        this.renameAll = renameAll;
    }

    /**
     * Returns an action indicating that column names which would be 
     * duplicated elsewhere in the result table should be modified
     * by appending a given string.
     *
     * @param  appendage  string to append to duplicate columns
     */
    public static JoinFixAction makeRenameDuplicatesAction( String appendage ) {
        return new JoinFixAction( "Fix Duplicates: " + appendage, appendage, 
                                  true, false );
    }

    /**
     * Returns an action indicating that all column names should be
     * modified by appending a given string.
     *
     * @param  appendage  string to append to columns
     */
    public static JoinFixAction makeRenameAllAction( String appendage ) {
        return new JoinFixAction( "Fix All: " + appendage, appendage,
                                  true, true );
    }

    /**
     * Returns the, possibly modified, name of a column.
     *
     * @param  origName  unmodified column name
     * @param  isDup  whether the column name would be duplicated
     *         in the set of unmodified names
     */
    public String getFixedName( String origName, boolean isDup ) {
        return renameAll || ( renameDup && isDup ) ? origName + appendage 
                                                   : origName;
    }

    public String toString() {
        return name;
    }
}
