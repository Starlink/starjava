package uk.ac.starlink.topcat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import gnu.jel.CompilationException;
import gnu.jel.Library;
import gnu.jel.Evaluator;
import java.util.Date;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Browsers;
import uk.ac.starlink.topcat.func.Image;
import uk.ac.starlink.topcat.func.Mgc;
import uk.ac.starlink.topcat.func.Output;
import uk.ac.starlink.topcat.func.Sdss;
import uk.ac.starlink.topcat.func.SuperCosmos;
import uk.ac.starlink.topcat.func.TwoQZ;
import uk.ac.starlink.topcat.plot2.GuiCoordContent;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * This class provides some utility methods for use with the JEL
 * expression compiler.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TopcatJELUtils extends JELUtils {

    private static List<Class<?>> activationStaticClasses;
    public static final String ACTIVATION_CLASSES_PROPERTY =
        "jel.classes.activation";
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );

    /** Taken from Java Language Specification (Java 6), sec 3.9. */
    private static final Set<String> keywords_ =
            Collections
           .unmodifiableSet( new HashSet<String>( Arrays.asList( new String[] {
        "abstract", "continue", "for",        "new",       "switch",
        "assert",   "default",  "if",         "package",   "synchronized",
        "boolean",  "do",       "goto",       "private",   "this",
        "break",    "double",   "implements", "protected", "throw",
        "byte",     "else",     "import",     "public",    "throws",
        "case",     "enum",     "instanceof", "return",    "transient",
        "catch",    "extends",  "int",        "short",     "try",
        "char",     "final",    "interface",  "static",    "void",
        "class",    "finally",  "long",       "strictfp",  "volatile",
        "const",    "float",    "native",     "super",     "while",
        "true",     "false",    "null",
    } ) ) );

    /**
     * Private constructor prevents instantiation.
     */
    private TopcatJELUtils() {
    }

    /**
     * Returns a JEL Library suitable for expression evaluation.
     *
     * @param    rowReader  object which can read rows from the table to
     *           be used for expression evaluation
     * @param    activation  true iff the result is to include classes 
     *           used only for activation (e.g. write to System.out, 
     *           pop up viewers) as well as classes with methods for
     *           calculations
     * @return   a JEL library
     */
    public static Library getLibrary( JELRowReader rowReader,
                                      boolean activation ) {
        List<Class<?>> statix = new ArrayList<Class<?>>( getStaticClasses() );
        List<Class<?>> activStatix = activation ? getActivationStaticClasses()
                                                : new ArrayList<Class<?>>();
        statix.addAll( activStatix );
        Class<?>[] staticLib = statix.toArray( new Class<?>[ 0 ] );
        Class<?>[] dynamicLib = { rowReader.getClass() };
        Library lib =
            JELUtils.createLibrary( staticLib, dynamicLib, rowReader );

        /* Mark the activation methods as dynamic.  This is a bit obscure;
         * its purpose is to make sure that they get evaluated at run time
         * not compilation time even if their arguments are constant.
         * You can't do very useful things with constant arguments, but
         * you might have them while you're experimenting, and it is
         * surprising to see the expression be evaluated when you type
         * the expression in rather than when the activation takes place. */
        for ( Class<?> clazz : activStatix ) {
            for ( Method method : clazz.getMethods() ) {
                int mods = method.getModifiers();
                if ( Modifier.isPublic( mods ) &&
                     Modifier.isStatic( mods ) ) {
                    String mname = method.getName();
                    Class<?>[] argtypes = method.getParameterTypes();
                    try {
                        lib.markStateDependent( mname, argtypes );
                    }
                    catch ( CompilationException e ) {
                        String msg = "Can't mark " + method + " for JEL?";
                        throw (AssertionError)
                              new AssertionError( msg ).initCause( e );
                    }
                }
            }
        }
        return lib;
    }

    /**
     * Returns the list of classes whose static methods will be mapped
     * into the JEL evaluation namespace for activation purposes only.
     * This may be modified.
     *
     * @return  list of activation classes with static methods
     */
    public static List<Class<?>> getActivationStaticClasses() {
        if ( activationStaticClasses == null ) {

            /* Assemble the list of classes which we know we have on hand.
             * Be careful though, since we may not have the classes they
             * rely on. */
            List<Class<?>> classList = new ArrayList<Class<?>>();
            classList.add( Output.class );
            classList.add( uk.ac.starlink.topcat.func.System.class );
            classList.add( Image.class );
            classList.add( BasicImageDisplay.class );
            classList.add( Browsers.class );
            classList.add( Mgc.class );
            classList.add( Sdss.class );
            classList.add( SuperCosmos.class );
            classList.add( TwoQZ.class );

            /* Add classes specified by a system property. */
            try {
                String auxClasses =
                    System.getProperty( ACTIVATION_CLASSES_PROPERTY );
                if ( auxClasses != null && auxClasses.trim().length() > 0 ) {
                    String[] cs = auxClasses.split( ":" );
                    for ( int i = 0; i < cs.length; i++ ) {
                        String className = cs[ i ].trim();
                        Class<?> clazz = classForName( className );
                        if ( clazz != null ) {
                            if ( ! classList.contains( clazz ) ) {
                                classList.add( clazz );
                            }
                        }
                        else {
                            logger.warning( "Class not found: " + className );
                        }
                    }
                }
            }
            catch ( SecurityException e ) {
                logger.info( "Security manager prevents loading " +
                             "auxiliary JEL classes" );
            }

            /* Produce the final list. */
            activationStaticClasses = classList;
        }
        return activationStaticClasses;
    }

     /**
     * Turns a primitive class into the corresponding wrapper class.
     *
     * @param   prim  primitive class
     * @return  the corresponding non-primitive wrapper class
     */
    public static Class<?> wrapPrimitiveClass( Class<?> prim ) {
        if ( prim == boolean.class ) {
            return Boolean.class;
        }
        else if ( prim == char.class ) {
            return Character.class;
        }
        else if ( prim == byte.class ) {
            return Byte.class;
        }
        else if ( prim == short.class ) {
            return Short.class;
        }
        else if ( prim == int.class ) {
            return Integer.class;
        }
        else if ( prim == long.class ) {
            return Long.class;
        }
        else if ( prim == float.class ) {
            return Float.class;
        }
        else if ( prim == double.class ) {
            return Double.class;
        }
        else if ( prim == void.class ) {
            return Void.class;
        }
        else {
            throw new IllegalArgumentException( prim + " is not primitive" );
        }
    }

    /**
     * Returns the class with the given name, or null if it's not on the
     * path.
     *
     * @param   cname  class name
     * @return  class or null
     */
    public static Class<?> classForName( String cname ) {

        // Hmm - not sure now why I wanted to make sure I got this classloader.
        // I wonder if there is a good reason??
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            return Class.forName( cname, true, loader );
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }
    }

    /**
     * Indicates whether a given JEL expression makes direct or indirect
     * reference to an existing column in a given table.
     * If the expression cannot be compiled, false is returned
     *
     * @param  tcModel   topcat model
     * @param  icol      column index to test
     * @param  expr      JEL expression
     * @return  true iff expr references the column with index icol
     */
    public static boolean isColumnReferenced( TopcatModel tcModel, int icol,
                                              String expr ) {
        return getReferencedColumns( tcModel, expr )
              .contains( new Integer( icol ) );
    }

    /**
     * Returns a list of the column indices that are directly or indirectly
     * referenced by a given JEL expression.
     * If the expression cannot be compiled, an empty list is returned.
     *
     * @param  tcModel   topcat model
     * @param  expr      JEL expression
     * @return   set of column indices referenced
     */
    public static Set<Integer> getReferencedColumns( TopcatModel tcModel,
                                                     String expr ) {
        Set<Integer> icolSet = new HashSet<>();

        /* Compile the expression using a RowReader that we can later
         * interrogate to find out which symbols the expression referenced. */
        TopcatJELRowReader rdr =
            TopcatJELRowReader.createDummyReader( tcModel );
        Library lib = getLibrary( rdr, false );
        try {
            Evaluator.compile( expr, lib );
        }
        catch ( CompilationException e ) {
            return icolSet;
        }

        /* Record direct references to columns. */
        icolSet.addAll( IntStream
                       .of( rdr.getTranslatedColumns() )
                       .boxed()
                       .collect( Collectors.toSet() ) );

        /* Recursively record column references in the expression symbols
         * that were referenced. */
        for ( String subExpr : getReferencedExpressions( rdr ) ) {
            icolSet.addAll( getReferencedColumns( tcModel, subExpr ) );
        }

        /* Return the result. */
        return icolSet;
    }

    /**
     * Indicates whether a given JEL expression makes direct or indirect
     * reference to an existing subset in a given topcat model.
     * If the expression cannot be compiled, false is returned.
     *
     * @param  tcModel  topcat model
     * @param  rsetId    ID of row subset to test
     * @param  expr  JEL expression
     * @return  true iff expr references the subset with index rsetId
     */
    public static boolean isSubsetReferenced( TopcatModel tcModel, int rsetId,
                                              String expr ) {
        return getReferencedSubsets( tcModel, expr )
              .contains( Integer.valueOf( rsetId ) );
    }

    /**
     * Returns a list of subset IDs that are directly or indirectly
     * referenced by a given JEL expression.
     * If the expression cannot be compiled, an empty list is returned.
     *
     * @param  tcModel   topcat model
     * @param  expr      JEL expression
     * @return  set of subset IDs referenced
     */
    public static Set<Integer> getReferencedSubsets( TopcatModel tcModel,
                                                     String expr ) {
        Set<Integer> idSet = new HashSet<>();

        /* Compile the expression using a RowReader that we can later
         * interrogate to find out which symbols the expression referenced. */
        TopcatJELRowReader rdr =
            TopcatJELRowReader.createDummyReader( tcModel );
        Library lib = getLibrary( rdr, false );
        try {
            Evaluator.compile( expr, lib );
        }
        catch ( CompilationException e ) {
            return idSet;
        }

        /* Record direct references to subsets. */
        idSet.addAll( IntStream
                     .of( rdr.getTranslatedSubsetIds() )
                     .boxed()
                     .collect( Collectors.toSet() ) );

        /* Recursively record subset references in expression symbols
         * that were referenced. */
        for ( String subExpr : getReferencedExpressions( rdr ) ) {
            idSet.addAll( getReferencedSubsets( tcModel, subExpr ) );
        }

        /* Return the result. */
        return idSet;
    }

    /**
     * Indicates whether a given JEL expression makes direct or indirect
     * reference to a named subset in a given topcat model.
     * If the expression cannot be compiled, or no subset with the given
     * name exists, false is returned.
     *
     * @param  tcModel  topcat model
     * @param  rsetName    name of row subset to test
     * @param  expr  JEL expression
     * @return  true iff expr references the subset with name rsetName
     */
    public static boolean isSubsetReferenced( TopcatModel tcModel,
                                              String rsetName,
                                              String expr ) {
        OptionsListModel<RowSubset> subsets = tcModel.getSubsets();
        for ( int is = 0; is < subsets.size(); is++ ) {
            RowSubset rset = subsets.get( is );
            if ( rset.getName().equalsIgnoreCase( rsetName ) ) {
                int id = subsets.indexToId( is );
                if ( isSubsetReferenced( tcModel, id, expr ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list of all the JEL expressions corresponding to symbols
     * referenced by the expression(s) a row reader has been used to compile.
     * These expressions can then be examined recursively to see what
     * symbols they reference.
     *
     * @param   rdr   a row reader that has been used to compile one or more
     *                JEL expressions; its list of referenced columns and
     *                subsets is examined to find ones that correspond to
     *                subordinate expressions
     * @return  list of distinct expressions corresponding to parsed symbols
     */
    private static String[] getReferencedExpressions( TopcatJELRowReader rdr ) {
        Set<String> exprs = new LinkedHashSet<String>();
        TopcatModel tcModel = rdr.getTopcatModel();
        PlasticStarTable dataModel = tcModel.getDataModel();
        for ( int ic : rdr.getTranslatedColumns() ) {
            ColumnData cdata = dataModel.getColumnData( ic );
            if ( cdata instanceof SyntheticColumn ) {
                exprs.add( ((SyntheticColumn) cdata).getExpression() );
            }
        }
        OptionsListModel<RowSubset> subsets = tcModel.getSubsets();
        for ( int id : rdr.getTranslatedSubsetIds() ) {
            int isub = subsets.idToIndex( id );
            if ( isub >= 0 ) {
                RowSubset rset = subsets.get( isub );
                if ( rset instanceof SyntheticRowSubset ) {
                    exprs.add( ((SyntheticRowSubset) rset).getExpression() );
                }
            }
        }
        return exprs.toArray( new String[ 0 ] );
    }

    /**
     * Returns a single JEL-friendly expression which may be used to
     * reference a GuiCoordContent, if possible.
     * This will only succeed (return a non-null value) if the
     * supplied GuiCoordContent corresponds to a single user-supplied label.
     *
     * @param  tcModel   topcat model
     * @param  content   user specification for a plotted quantity
     * @return  JEL-safe expression for referencing the quantity, or null
     */
    public static String getDataExpression( TopcatModel tcModel,
                                            GuiCoordContent content ) {
        String[] labels = content.getDataLabels();
        String label = labels.length == 1 ? labels[ 0 ] : null;
        return getDataExpression( tcModel, label );
    }

    /**
     * Returns an array of JEL-friendly expressions which may be used to
     * reference a GuiCoordContent.
     *
     * @param  tcModel   topcat model
     * @param  content   user specification for a plotted quantity
     * @return  array of JEL-safe expressions, one for each user-supplied
     *          label in the content
     *  
     */
    public static String[] getDataExpressions( TopcatModel tcModel,
                                               GuiCoordContent content ) {
        String[] labels = content.getDataLabels();
        int nexpr = labels.length;
        String[] exprs = new String[ nexpr ];
        for ( int ie = 0; ie < nexpr; ie++ ) {
            exprs[ ie ] = getDataExpression( tcModel, labels[ ie ] );
        }
        return exprs;
    }

    /**
     * Converts a data label to a JEL-friendly expression for a table quantity.
     * The label will typically be a string that the user has selected
     * or entered to identify a quantity to be plotted.
     *
     * @param  tcModel  topcat model
     * @param  label    textual identifier for data
     */
    private static String getDataExpression( TopcatModel tcModel,
                                             String label ) {
        if ( label == null ) {
            return null;
        }

        /* Look through column names for case-sensitive match,
         * then case-insensitive match, otherwise interpret the
         * value as a JEL expression in the usual way.
         * {@see ColumnDataComboBoxModel#stringToColumnData}.
         * If the string matches a column name, return the supplied
         * string if it is syntactically permissible, otherwise the
         * column $identifier.  There could be less roundabout,
         * though possibly less robust, ways to do this by examining
         * the GuiCoordContent and e.g. its ColumnDatas. */
        TableColumnModel colModel = tcModel.getColumnModel();
        int ncol = colModel.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            StarTableColumn tcol = (StarTableColumn) colModel.getColumn( ic );
            String cname = tcol.getColumnInfo().getName();
            if ( label.equals( cname ) ) {
                return isJelIdentifier( cname ) ? cname : getColumnId( tcol );
            }
        }
        for ( int ic = 0; ic < ncol; ic++ ) {
            StarTableColumn tcol = (StarTableColumn) colModel.getColumn( ic );
            String cname = tcol.getColumnInfo().getName();
            if ( label.equalsIgnoreCase( cname ) ) {
                return isJelIdentifier( cname ) ? cname : getColumnId( tcol );
            }
        }
        return label;
    }

    /**
     * Returns a JEL-friendly expression which may be used to reference
     * a RowSubset.
     *
     * @param   tcModel  topcat model
     * @param   rset   row subset
     * @return  JEL-safe expression for subset
     */
    public static String getSubsetExpression( TopcatModel tcModel,
                                              RowSubset rset ) {
        String name = rset.getName();
        if ( isJelIdentifier( name ) ) {
            return name;
        }
        else {
            OptionsListModel<RowSubset> subsets = tcModel.getSubsets();
            int iset = subsets.indexOf( rset );
            int id = iset >= 0 ? subsets.indexToId( iset ) : -1;
            if ( id >= 0 ) {
                return Character.toString( TopcatJELRowReader.SUBSET_ID_CHAR )
                     + Integer.toString( 1 + id );
            }
            else {
                return name;
            }
        }
    }

    /**
     * Returns the JEL $Identifier for a given column.
     *
     * @param   tcol  table column from data model
     * @return   "$nn" expression referencing column
     */
    public static String getColumnId( StarTableColumn tcol ) {
        return Character.toString( JELRowReader.COLUMN_ID_CHAR )
             + Integer.toString( 1 + tcol.getModelIndex() );
    }

    /**
     * Indicates whether a given string is a syntactically legal Java
     * identifier.  It has to have the right form and not be a reserved word.
     *
     * @param   label  text to test
     * @return   true iff it can be used as an identifier in JEL expressions
     */
    public static boolean isJelIdentifier( String label ) {
        int nc = label.length();
        if ( nc == 0 ) {
            return false;
        }
        if ( ! Character.isJavaIdentifierStart( label.charAt( 0 ) ) ) { 
            return false;
        }
        for ( int i = 0; i < nc; i++ ) {
            if ( ! Character.isJavaIdentifierPart( label.charAt( i ) ) ) {
                return false;
            }
        }
        if ( keywords_.contains( label ) ) {
            return false;
        }
        return true;
    }

    /**
     * Returns a JEL expression that represents the union of a given
     * array of subsets ANDed with a given JEL expression.
     *
     * @param  tcModel  topcat model
     * @param  expr     expression to AND with
     * @param  rowSubsets   array of zero or more subsets composing union;
     *                      if none are provided, ALL is assumed
     * @return   combined expression
     */
    public static String combineSubsetsExpression( TopcatModel tcModel,
                                                   String expr,
                                                   RowSubset[] rowSubsets ) {
        List<RowSubset> rsets =
            new ArrayList<RowSubset>( Arrays.asList( rowSubsets ) );
        if ( rsets.contains( RowSubset.ALL ) ) {
            return expr;
        }
        rsets.remove( RowSubset.NONE );
        int nset = rsets.size();
        if ( nset == 0 ) {
            return expr;
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            if ( nset == 1 ) {
                sbuf.append( getSubsetExpression( tcModel, rsets.get( 0 ) ) );
            }
            else {
                sbuf.append( "(" );
                for ( int is = 0; is < nset; is++ ) {
                    if ( is > 0 ) {
                        sbuf.append( " || " );
                    }
                    sbuf.append( getSubsetExpression( tcModel,
                                                      rsets.get( is ) ) );
                }
                sbuf.append( ")" );
            }
            sbuf.append( " && " )
                .append( "(" )
                .append( expr )
                .append( ")" );
            return sbuf.toString();
        }
    }

    /**
     * Returns a JEL expression that characterises a univariate range of values.
     *
     * @param   expr  JEL expression whose value is to be constrained
     * @param   lo    lowest permissible bound for expr
     * @param   hi    highest permissible bound for expr
     * @param   isLog  true for logarithmic range, false for linear
     * @param   npix   approximate number of pixels covered by the range
     * @return  JEL expression with the meaning <code>lo&lt;=expr&lt;=hi</code>
     */
    public static String betweenExpression( String expr,
                                            double lo, double hi, boolean isLog,
                                            int npix ) {
        String exprTxt = isJelIdentifier( expr )
                       ? expr
                       : "(" + expr + ")";
        String[] limits = PlotUtil.formatAxisRangeLimits( lo, hi, isLog, npix );
        return new StringBuffer()
              .append( exprTxt )
              .append( " >= " )
              .append( limits[ 0 ] )
              .append( " && " )
              .append( exprTxt )
              .append( " <= " )
              .append( limits[ 1 ] )
              .toString();
    }
}
