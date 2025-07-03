package uk.ac.starlink.topcat;

import gnu.jel.CompiledExpression;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.jel.Constant;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * Random JELRowReader with which recognises some expressions in addition
 * to those of the superclass.
 * 
 * <dl>
 * <dt>Row Subset _ID identifiers:
 * <dd>The character '_'
 *     followed by the 1-based index of a defined row subset
 *     returns true iff the current row is part of the subset.
 *
 * <dt>Row Subset names:
 * <dd>The name of a subset (case-insensitive) returns true iff the current
 *     row is part of the named subset.
 *
 * <dt>Apparent table index:
 * <dd>The tokens "<code>$index0</code>" or "<code>$00</code>"
 *     (case insensitive)
 *     are evaluated as the index of the current row in the apparent table;
 *     this differs from <code>$index</code>/<code>$0</code>
 *     if a non-default sort order or current subset is in force.
 *
 * <dt>Apparent table row count:
 * <dd>The token "<code>$nrow0</code>" is the number of rows in the
 *     apparent table; this differs from <code>$nrow</code> if a non-default
 *     current subset is in force.
 *
 * <dt>Apparent table column count:
 * <dd>The token "<code>$ncol0</code> is the number of columns in the
 *     apparent table; this differs from <code>$ncol</code> if some
 *     columns are hidden.
 *
 * </dl>
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public abstract class TopcatJELRowReader extends RandomJELRowReader {

    private final TopcatModel tcModel_;
    private final List<RowSubset> rdrSubsets_;
    private final Set<Integer> translatedSubsetIds_;
    private final Map<String,UserConstant<?>> constMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /** Prefix identifying a unique subset identifier. */
    public static final char SUBSET_ID_CHAR = '_';

    /**
     * Constructs a new row reader for a TopcatModel.
     *
     * @param   tcModel   topcat model
     */
    @SuppressWarnings("this-escape")
    protected TopcatJELRowReader( TopcatModel tcModel ) {
        super( tcModel.getDataModel() );
        assert getTable().isRandom();
        tcModel_ = tcModel;
        rdrSubsets_ = new ArrayList<RowSubset>();
        translatedSubsetIds_ = new LinkedHashSet<Integer>();
        constMap_ = VariablePanel.getInstance().getVariables();
    }

    /**
     * Returns the topcat model on which this row reader is based.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        return tcModel_;
    }


    /**
     * Evaluates a given compiled expression of boolean return type
     * at a given row.
     *
     * @param  compEx  compiled boolean expression
     * @param  lrow   row at which to evaluate
     * @return   expression value
     */
    public abstract boolean evaluateBooleanAtRow( CompiledExpression compEx,
                                                  long lrow )
            throws Throwable;

    /**
     * Overrides superclass implementation to recognise row subsets
     * by name or _ID.
     *
     * @param  name  the variable name
     * @return  corresponding method name fragment
     * @see    "JEL manual"
     */
    public String getTypeName( String name ) {

        /* If the superclass implementation can identify the string, 
         * use that. */
        String typeName = super.getTypeName( name );
        if ( typeName != null ) {
            return typeName;
        }

        /* Otherwise, see if it's a known subset, in which case it 
         * will have a boolean return type. */
        short isub = getSubsetIndex( name );
        if ( isub >= 0 ) {
            return "Boolean";
        }

        /* Otherwise, we don't know what it is. */
        return null;
    }

    /**
     * Overrides superclass implementation to recognise subsets as well as
     * the other special objects.  The additional return type is:
     * <ul>
     * <li>a <code>Short</code> (the subset index) if the column specification
     *     appears to reference a known row subset
     * </ul>
     * @param  name  the name of the variable-like object to evaluate
     * @return  a numeric object corresponding to an object which we
     *          know how to evaluate
     * @see    "JEL manual"
     */
    @Override
    public Object translate( String name ) {

        /* If the superclass implementation recognises it, use that. */
        Object translation = super.translate( name );
        assert ! ( translation instanceof Short );
        if ( translation != null ) {
            return translation;
        }

        /* Otherwise, see if it corresponds to a defined subset. */
        short isub = getSubsetIndex( name );
        if ( isub >= 0 ) {
            return Short.valueOf( isub );
        }

        /* Otherwise, it is unrecognised. */
        return null;
    }

    /**
     * Returns a set (no duplicated elements) of the subset IDs
     * for which this RowReader has been asked to provide translation values.
     * In practice that means the ID
     * (in the sense of the <code>OptionsListModel</code> returned by
     * <code>TopcatModel.getSubsets</code>)
     * of every RowSubset which has been directly referenced in a JEL
     * expression which this RowReader has been used to compile.
     *
     * @return   list of distinct subset IDs which this row reader has had to
     *           reference in compiling JEL expressions
     */
    public int[] getTranslatedSubsetIds() {
        int ns = translatedSubsetIds_.size();
        int[] ids = new int[ ns ];
        int i = 0;
        for ( Integer id : translatedSubsetIds_ ) {
            ids[ i++ ] = id.intValue();
        }
        assert i == ns;
        return ids;
    }

    /**
     * Returns the actual subset value for the current row and a given
     * column.
     *
     * @param  isub  index of the subset to evaluate at the current row
     * @return result of the <code>isIncluded</code> method of the
     *         <code>RowSubset</code> indicated at the current row
     */
    public boolean getBooleanProperty( short isub ) {
        return getSubset( isub ).isIncluded( getCurrentRow() );
    }

    /**
     * Returns the index into the subsets list which corresponds to a given
     * subset name.  The current formats are
     * <ul>
     * <li> subset name (case insensitive, first occurrence used)
     * <li> SUBSET_ID_CHAR+(index+1) (so first subset in list would be "_1")
     * </ul>
     * Note this method is only called during expression compilation,
     * so it doesn't need to be particularly efficient.
     *
     * <p>If this method successfully identifies a subset
     * (returns a non-negative value) that subset is saved in this object
     * for later use.  Only subsets that have been retrieved using
     * this method are available for later evaluation; the later
     * evaluation does not examine the TopcatModel itself for subsets,
     * it uses the ones stored in this object.  That protects
     * expression evaluations from failing in the case that the subset
     * has disappeared between expression compilation and evaluation;
     * it also <em>may</em> (depending on the implementation of the subset
     * in question) mean that the evaluation is based on the state
     * of the named subset at compilation time rather than evaluation time
     * (it's arguable which of those options is preferable).
     * This scheme works because expression compilation is usually done
     * near to the creation time of this object, and compilation triggers
     * calls to this method as required for the subsets referenced.
     * Evaluations may happen much later.
     *
     * @param  name  subset identifier
     * @return  subset index into <code>subsets</code> list, or -1 if the
     *          subset was not known
     */
    private short getSubsetIndex( String name ) {

        /* Get a list of the subsets in the topcat model.  If the named
         * subset is one of those then (a) make sure that subset is stored
         * in this object for later reference and (b) return an index
         * that can refer to it. */
        OptionsListModel<RowSubset> subsets = tcModel_.getSubsets();
        int nsub = subsets.size();

        /* Try the '_' + number format. */
        if ( name.charAt( 0 ) == SUBSET_ID_CHAR ) {
            try {
                int subsetId = Integer.parseInt( name.substring( 1 ) ) - 1;
                for ( int isub = 0; isub < nsub; isub++ ) {
                    if ( subsetId == subsets.indexToId( isub ) ) {
                        translatedSubsetIds_.add( Integer.valueOf( subsetId ) );
                        return getSubsetIndex( subsets.get( isub ) );
                    }
                }
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }

        /* Try the subset name. */
        for ( int isub = 0; isub < nsub; isub++ ) {
            RowSubset rset = subsets.get( isub );
            if ( rset.getName().equalsIgnoreCase( name ) ) {
                translatedSubsetIds_.add( Integer.valueOf( subsets
                                                          .indexToId( isub ) ));
                return getSubsetIndex( rset );
            }
        }

        /* It's not a subset. */
        return (short) -1;
    }

    /**
     * Returns an index into this object's list of subsets that identifies
     * a given subset, adding it into that list if it's not already present.
     *
     * @param  rset  row subset required for later use
     * @return   index using which the given subset can be retrieved later
     */
    private short getSubsetIndex( RowSubset rset ) {
        int nsub = rdrSubsets_.size();

        /* If we've already stored it, return the existing index. */
        for ( int is = 0; is < nsub; is++ ) {
            if ( rset == rdrSubsets_.get( is ) ) {
                return (short) is;
            }
        }

        /* Otherwise add it to the list and return the newly-occupied index. */
        if ( nsub < Short.MAX_VALUE ) {
            rdrSubsets_.add( rset );
            assert (short) nsub == nsub;
            return (short) nsub;
        }

        /** ... unless it's out of range (unlikely). */
        else {
            logger_.warning( ">" + Short.MAX_VALUE
                           + " subsets in JEL expression??" );
            return (short) -1;
        }
    }

    /**
     * Returns the subset at a given index.
     * If the supplied index is the return value of an earlier call to the
     * {@link #getSubsetIndex} method, this will return the subset that
     * was passed to that method.
     *
     * @param  isub  subset index
     * @return  subset stored under supplied index
     */
    private RowSubset getSubset( int isub ) {
        return rdrSubsets_.get( isub );
    }

    @Override
    protected Constant<?> getSpecialByName( String name ) {

        /* Add some specials based on the apparent table. */
        if ( name.equalsIgnoreCase( "$index0" ) ||
             name.equals( "$00" ) ) {
            final ViewerTableModel viewModel = tcModel_.getViewModel();
            return new Constant<Integer>() {
                public Class<Integer> getContentClass() {
                    return Integer.class;
                }
                public Integer getValue() {
                    int jrow = viewModel.getViewRow( getCurrentRow() );
                    return jrow >= 0 ? Integer.valueOf( 1 + jrow ) : null;
                }
                public boolean requiresRowIndex() {
                    return true;
                }
            };
        }
        else if ( name.equalsIgnoreCase( "$nrow0" ) ) {
            final ViewerTableModel viewModel = tcModel_.getViewModel();
            return new Constant<Integer>() {
                public Class<Integer> getContentClass() {
                    return Integer.class;
                }
                public Integer getValue() {
                    return Integer.valueOf( viewModel.getRowCount() );
                }
                public boolean requiresRowIndex() {
                    return false;
                }
            };
        }
        else if ( name.equalsIgnoreCase( "$ncol0" ) ) {
            final TableColumnModel colModel = tcModel_.getColumnModel();
            return new Constant<Integer>() {
                public Class<Integer> getContentClass() {
                    return Integer.class;
                }
                public Integer getValue() {
                    return Integer.valueOf( colModel.getColumnCount() );
                }
                public boolean requiresRowIndex() {
                    return false;
                }
            };
        }

        /* Look for user constants. */
        Constant<?> userConst = constMap_.get( name );
        if ( userConst != null ) {
            return userConst;
        }

        /* Otherwise fall back to superclass behaviour. */
        return super.getSpecialByName( name );
    }

    /**
     * Returns a constant which is evaluated at runtime.
     * This is more appropriate than the inherited (evaluate at call time)
     * behaviour, since within TOPCAT the constant's value may change as a
     * result of user intervention during the lifetime of the returned object.
     */
    @Override
    protected Constant<?> createDescribedValueConstant( DescribedValue dval ) {
        final Class<?> clazz = dval.getInfo().getContentClass();
        Supplier<Object> valueSupplier = () -> {
            Object val = dval.getValue();
            return Tables.isBlank( val ) ? null : val;
        };
        return createConstant( clazz, valueSupplier );
    }
    private static <T> Constant<T>
            createConstant( Class<T> clazz, Supplier<Object> valueSupplier ) {
        return new Constant<T>() {
            public Class<T> getContentClass() {
                return clazz;
            }
            public T getValue() {
                Object value = valueSupplier.get();
                return clazz.isInstance( value ) ? clazz.cast( value ) : null;
            }
            public boolean requiresRowIndex() {
                return false;
            }
        };
    }

    /**
     * Returns a reader that uses the threadsafe random access methods
     * of the TopcatModel's data model.
     *
     * @param   tcModel   topcat model
     * @return   threadsafe row reader
     */
    public static TopcatJELRowReader
            createConcurrentReader( TopcatModel tcModel ) {
        final StarTable table = tcModel.getDataModel();
        return new TopcatJELRowReader( tcModel ) {
            private long lrow_ = -1;
            public long getCurrentRow() {
                return lrow_;
            }
            public synchronized Object getCell( int icol ) throws IOException {
                return table.getCell( lrow_, icol );
            }
            public synchronized Object evaluateAtRow( CompiledExpression compEx,
                                                      long lrow )
                    throws Throwable {
                lrow_ = lrow;
                return evaluate( compEx );
            }
            public synchronized boolean
                    evaluateBooleanAtRow( CompiledExpression compEx, long lrow )
                    throws Throwable {
                lrow_ = lrow;
                return evaluateBoolean( compEx );
            }
        };
    }

    /**
     * Returns a reader that uses a RowAccess object from the
     * TopcatModel's data model.
     *
     * @param   tcModel   topcat model
     * @return   row reader suitable for use only within a single thread
     */
    public static TopcatJELRowReader createAccessReader( TopcatModel tcModel ) {
        final RowAccess racc = tcModel.getDataModel().getRowAccess();
        return new TopcatJELRowReader( tcModel ) {
            private long lrow_ = -1;
            public long getCurrentRow() {
                return lrow_;
            }
            public Object getCell( int icol ) throws IOException {
                return racc.getCell( icol );
            }
            public Object evaluateAtRow( CompiledExpression compEx, long lrow )
                    throws Throwable {
                if ( lrow != lrow_ ) {
                    racc.setRowIndex( lrow );
                    lrow_ = lrow;
                }
                return evaluate( compEx );
            }
            public boolean evaluateBooleanAtRow( CompiledExpression compEx,
                                                 long lrow )
                    throws Throwable {
                if ( lrow != lrow_ ) {
                    racc.setRowIndex( lrow );
                    lrow_ = lrow;
                }
                return evaluateBoolean( compEx );
            }
        };
    }

    /**
     * Returns a reader that doesn't do any actual data access.
     * Suitable for testing compilation success etc.
     *
     * @param   tcModel   topcat model
     * @return   row reader that throws UnsupportedOperationException
     *           for data access methods
     */
    public static TopcatJELRowReader createDummyReader( TopcatModel tcModel ) {
        return new TopcatJELRowReader( tcModel ) {
            public long getCurrentRow() {
                throw new UnsupportedOperationException();
            }
            public Object getCell( int icol ) {
                throw new UnsupportedOperationException();
            }
            public Object evaluateAtRow( CompiledExpression compEx, long lr ) {
                throw new UnsupportedOperationException();
            }
            public boolean evaluateBooleanAtRow( CompiledExpression compEx,
                                                 long lrow ) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
