package uk.ac.starlink.topcat;

import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
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
 *     returns true iff the current column is part of the subset.
 *
 * <dt>Row Subset names:
 * <dd>The name of a subset (case-insensitive) returns true iff the current
 *     column is part of the named subset.
 *
 * </dl>
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public class TopcatJELRowReader extends RandomJELRowReader {

    private final RowSubset[] subsets_;
    private final int[] subsetIds_;

    /** Prefix identifying a unique subset identifier. */
    public static final char SUBSET_ID_CHAR = '_';

    /**
     * Constructs a new row reader for a random-access table.
     *
     * @param   table  table object
     * @param   subsets  array of {@link RowSubset} objects which 
     *          this reader will recognise (may be null)
     * @param   subsetIds  array of integer identifiers by which the
     *          <code>subsets</code> array will be identified;
     *          must be the same length as <code>subsets</code>
     * @throws  IllegalArgumentException  if <tt>table.isRandom()</tt>
     *          returns false
     */
    public TopcatJELRowReader( StarTable table,
                               RowSubset[] subsets, int[] subsetIds ) {
        super( table );
        if ( ! table.isRandom() ) {
            throw new IllegalArgumentException( "Table is not random-access" );
        }
        subsets_ = subsets == null ? new RowSubset[ 0 ] : subsets;
        subsetIds_ = subsetIds == null ? new int[ 0 ] : subsetIds;
        if ( subsets_.length != subsetIds_.length ) {
            throw new IllegalArgumentException( "arg length mismatch" );
        }
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
        return subsets_[ (int) isub ].isIncluded( getCurrentRow() );
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
     * @param  name  subset identifier
     * @return  subset index into <tt>subsets</tt> list, or -1 if the
     *          subset was not known
     */
    private short getSubsetIndex( String name ) {

        /* Try the '_' + number format. */
        if ( name.charAt( 0 ) == SUBSET_ID_CHAR ) {
            try {
                int subsetId = Integer.parseInt( name.substring( 1 ) ) - 1;
                for ( int isub = 0; isub < subsetIds_.length; isub++ ) {
                    if ( subsetId == subsetIds_[ isub ] ) {
                        if ( isub >= Short.MIN_VALUE &&
                             isub <= Short.MAX_VALUE ) {
                            return (short) isub;
                        }
                        else {
                            /* 2^15 subsets have been defined?  Unlikely. */
                        }
                    }
                }
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }

        /* Try the subset name. */
        for ( int isub = 0; isub < subsets_.length; isub++ ) {
            if ( subsets_[ isub ].getName().equalsIgnoreCase( name ) ) {
                if ( isub >= Short.MIN_VALUE &&
                     isub <= Short.MAX_VALUE ) {
                    return (short) isub;
                }
                else {
                    /* 2^15 subsets have been defined?  Unlikely. */
                }
            }
        }

        /* It's not a subset. */
        return (short) -1;
    }

    /**
     * Returns a constant which is evaluated at runtime.
     * This is more appropriate than the inherited (evaluate at call time)
     * behaviour, since within TOPCAT the constant's value may change as a
     * result of user intervention during the lifetime of the returned object.
     */
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
