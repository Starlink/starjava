package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.jel.Constant;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * Random JELRowReader which in addition to the variables recognised 
 * by the superclass, also recognises named row subsets 
 * (<tt>RowSubset</tt> inclusion flag vectors):
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
 * </dl>
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public class TopcatJELRowReader extends RandomJELRowReader {

    private final TopcatModel tcModel_;
    private final List<RowSubset> rdrSubsets_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /** Prefix identifying a unique subset identifier. */
    public static final char SUBSET_ID_CHAR = '_';

    /**
     * Constructs a new row reader for a TopcatModel.
     *
     * @param   tcModel   topcat model
     */
    public TopcatJELRowReader( TopcatModel tcModel ) {
        super( tcModel.getDataModel() );
        assert getTable().isRandom();
        tcModel_ = tcModel;
        rdrSubsets_ = new ArrayList<RowSubset>();
    }

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
     * <li>a <tt>Short</tt> (the subset index) if the column specification
     *     appears to reference a known row subset
     * </ul>
     * @param  name  the name of the variable-like object to evaluate
     * @return  a numeric object corresponding to an object which we
     *          know how to evaluate
     * @see    "JEL manual"
     */
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
            return new Short( isub );
        }

        /* Otherwise, it is unrecognised. */
        return null;
    }

    /**
     * Returns the actual subset value for the current row and a given
     * column.
     *
     * @param  isub  index of the subset to evaluate at the current row
     * @return result of the <tt>isIncluded</tt> method of the
     *         <tt>RowSubset</tt> indicated at the current row
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
     * @return  subset index into <tt>subsets</tt> list, or -1 if the
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

    /**
     * Returns a constant which is evaluated at runtime.
     * This is more appropriate than the inherited (evaluate at call time)
     * behaviour, since within TOPCAT the constant's value may change as a
     * result of user intervention during the lifetime of the returned object.
     */
    @Override
    protected Constant createDescribedValueConstant( final
                                                     DescribedValue dval ) {
        final Class clazz = dval.getInfo().getContentClass();
        return new Constant() {
            public Class getContentClass() {
                return clazz;
            }
            public Object getValue() {
                Object val = dval.getValue();
                return Tables.isBlank( val ) ? null : val;
            }
        };
    }
}
