package uk.ac.starlink.topcat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * Class containing miscellaneous static methods and constants 
 * for use in TOPCAT.
 *
 * @author   Mark Taylor
 * @since    19 Aug 2004
 */
public class TopcatUtils {

    private static Boolean canSog_;
    private static Boolean canExec_;

    /**
     * Column auxiliary metadata key identifying the uniqe column identifier
     * for use in algebraic expressions.
     */
    public static final ValueInfo COLID_INFO =
        new DefaultValueInfo( "$ID", String.class, "Unique column ID" );

    /**
     * Column auxiliary metadata key identifying the description of
     * columns which also have an expression (EXPR_INFO) entry.
     */
    public static final ValueInfo BASE_DESCRIPTION_INFO =
        new DefaultValueInfo( "Base Description", String.class,
                              "Description omitting expression" );

    /**
     * Column auxiliary metadata key identifying the text string which
     * gives an expression for a synthetic column.
     */
    public final static ValueInfo EXPR_INFO =
        new DefaultValueInfo( "Expression", String.class,
                              "Algebraic expression for column value" );

    /**
     * Returns the 'base description' of a column info.  This is the same
     * as the description, except for synthetic columns, where it
     * doesn't contain a respresentation of the algebraic expression.
     *
     * @param  column info
     * @return  basic description of <tt>colinfo</tt>
     */
    public static String getBaseDescription( ColumnInfo info ) {
        DescribedValue descValue = info.getAuxDatum( BASE_DESCRIPTION_INFO );
        if ( descValue == null ) { 
            return info.getDescription();
        }
        else {
            Object desc = descValue.getValue();
            return desc instanceof String ? (String) desc 
                                          : info.getDescription();
        }
    }

    /**
     * Sets the 'basic description' of a column info.  This sets the
     * future result of calls to {@link #getBasicDescription} and also
     * the description string itself
     * ({@link uk.ac.starlink.table.ColumnInfo#getDescription}).
     *
     * @param  info  column info to modify
     * @param  desc  basic description string (don't include expression text)
     */
    public static void setBaseDescription( ColumnInfo info, String desc ) {
        DescribedValue descValue = info.getAuxDatum( BASE_DESCRIPTION_INFO );
        if ( descValue == null ) {
            info.setDescription( desc );
        }
        else {
            descValue.setValue( desc );
            String descrip = desc;
            String expr = getExpression( info );
            if ( expr != null && expr.trim().length() > 0 ) {
                descrip += " (" + expr + ")";
            }
        }
    }

    /**
     * Returns the expression text for a column.
     * This should only have a non-null value for synthetic columns.
     *
     * @param  info   column info
     * @return  synthetic expression string
     */
    public static String getExpression( ColumnInfo info ) {
        DescribedValue exprValue = info.getAuxDatum( EXPR_INFO );
        if ( exprValue == null ) {
            return null;
        }
        else {
            Object expr = exprValue.getValue();
            return expr == null ? null : expr.toString();
        }
    }

    /**
     * Returns the base name of a column; that is one without any 
     * suffix based on <tt>baseSuffix</tt>.
     * This method is used in conjunction with {@link #getDistinctName}.
     *
     * @param  origName  full name, possibly including bits of suffix
     * @param  baseSuffix   the base suffix string
     * @return  name without any suffix-like elements of the sort 
     *          specified by <tt>baseSuffix</tt>
     */
    public static String getBaseName( String origName, String baseSuffix ) {
        return suffixPattern( baseSuffix )
              .matcher( origName )
              .replaceFirst( "" );
    }

    /**
     * Returns a column name based on a given one which is guaranteed 
     * distinct from any others in the column list.
     * If the submitted <tt>origName</tt> is already unique, it may be
     * returned.  Otherwise a new name may be made which involves appending
     * the given <tt>baseSuffix</tt> to it.
     *
     * @param  colList  column list within which distinct naming is required
     * @param  oldName  initial name
     * @param  baseSuffix  suffix used for deduplication
     * @return  a name resembling <tt>origName</tt> which is not the same
     *          as any existing column names in <tt>colList</tt>
     * @see  #getBaseName
     */
    public static String getDistinctName( ColumnList colList, String origName, 
                                          String baseSuffix ) {
        Pattern suffixPattern = suffixPattern( baseSuffix );

        /* Get the base name - that's one as it would have been prior to
         * having been mangled by this process before. */
        String baseName = getBaseName( origName, baseSuffix );
        int baseLeng = baseName.length();

        /* Go through all the known columns looking for ones that match this
         * suffix pattern. */
        int ncol = colList.size();
        int nextFreeIndex = 0;
        boolean unique = true;
        for ( int ic = 0; ic < ncol; ic++ ) {
            String colName = ((StarTableColumn) colList.getColumn( ic ))
                            .getColumnInfo().getName();
            if ( colName.startsWith( baseName ) ) {
                if ( colName.equals( baseName ) ) {
                    unique = false;
                }
                Matcher matcher = suffixPattern
                                 .matcher( colName.substring( baseLeng ) );
                if ( matcher.matches() ) {
                    unique = false;
                    String suffIndex = matcher.group( 1 );
                    int isuf = ( suffIndex == null || suffIndex.length() == 0 )
                             ? 0
                             : Integer.parseInt( suffIndex );
                    if ( isuf >= nextFreeIndex ) {
                        nextFreeIndex = isuf + 1;
                    }
                }
            }
        }

        /* Calculate and return the new column name. */
        if ( unique ) {
            return origName;
        }
        else {
            String newName = baseName + baseSuffix;
            if ( nextFreeIndex > 0 ) {
                newName += nextFreeIndex;
            }
            return newName;
        }
    }

    /**
     * Returns a Pattern object which will match a suffix of the sort
     * used by {@link #getDistinctName}.
     *
     * @param   baseSuffix  suffix base string
     * @return  end-of-string matching pattern
     * @see    #getDistinctName
     * @see    #getBaseName
     */
    private static Pattern suffixPattern( String baseSuffix ) {
        return Pattern.compile( "\\Q" + baseSuffix + "\\E" + "([0-9]*)$" );
    }

    /**
     * Indicates whether there are enough classes to make SoG work at runtime.
     *
     * @return  true iff it's safe to use a SoG-based viewer
     */
    public static boolean canSog() {
        if ( canSog_ == null ) {
            synchronized ( TopcatUtils.class ) {
                try {
                    Class.forName( "uk.ac.starlink.sog.SOG" );
                    canSog_ = Boolean.TRUE;
                }
                catch ( Throwable th ) {
                    canSog_ = Boolean.FALSE;
                }
            }
        }
        return canSog_.booleanValue();
    }

    /**
     * Indicates whether we have System.exec permission.
     * 
     * @return  true  if it's possible to exec
     */
    public static boolean canExec() {
        if ( canExec_ == null ) {
            synchronized ( TopcatUtils.class ) {
                SecurityManager sman = System.getSecurityManager();
                if ( sman != null ) {
                    try {
                        sman.checkExec( null );
                    }
                    catch ( SecurityException e ) {
                        canExec_ = Boolean.FALSE;
                    }
                }
                canExec_ = Boolean.TRUE;
            }
        }
        return canExec_.booleanValue();
    }

}
