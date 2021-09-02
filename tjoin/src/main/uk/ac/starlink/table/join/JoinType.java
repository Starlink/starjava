package uk.ac.starlink.table.join;

import java.util.BitSet;
import uk.ac.starlink.table.Tables;

/**
 * Defines how a set of matched rows from input tables 
 * ({@link RowLink} objects) are used to select rows for inclusion
 * in an output table which represents the result of the matching operation.
 * This corresponds to the type of the join in database terminology, though
 * the naming of the instances of this class do not follow that 
 * terminology (left outer join etc), and the options here present a more
 * complete set.
 *
 * <p>Instances of this class are available as public static members of it.
 * Currently, these instances should only be used with the results of
 * pair matches (ones involving exactly two tables).
 *
 * @author   Mark Taylor
 * @since    5 Sep 2005
 */
public abstract class JoinType {

    private final String name_;
    private final String longName_;

    /**
     * Private sole constructor.
     *
     * @param  name  name
     */
    private JoinType( String name, String longName ) {
        name_ = name;
        longName_ = longName;
    }

    /**
     * Turns a set of links which represent matches from the matching
     * operation into a set of links which represent the rows to output.
     * <code>links</code> is a set of {@link RowLink} objects;
     * if the matching operation was between two tables, each element 
     * will represent a pair match (have two <code>RowRef</code>s,
     * with table indices of 0 and 1 respectively).
     * The ordering of <code>links</code> may influence the order of
     * the returned collection.  <code>links</code> may be modified
     * by this method, and the returned value may or may not be the
     * same object as the input <code>links</code> itself.
     *
     * @param  links  set of RowLinks representing actual matches
     * @param  rowCounts numbers of rows in the tables on which the 
     *         match was performed
     * @return  set of RowLinks representing rows for the output table
     */
    public abstract LinkSet processLinks( LinkSet links, int[] rowCounts );

    /**
     * Returns an array of flags indicating whether each of the tables in
     * the input will ever have non-empty rows in the output.
     *
     * @return   2-element boolean array indicating whether first and second
     *           input tables appear in output
     */
    public abstract boolean[] getUsedTableFlags();

    /**
     * Returns a value which indicates whether the matchScore is ever
     * non-empty in the output table.
     *
     * @return  true iff the matchScore may be used in the output
     */
    public abstract boolean getUsedMatchFlag();

    /**
     * Returns the name of this type.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short textual description of this type.
     *
     * @return  description
     */
    public abstract String getDescription();

    /**
     * Returns the name of this type.
     */
    public String toString() {
        return longName_;
    }

    /**
     * Returns a list of all the known types which apply to two-table matches.
     *
     * @return   array of the 7 known types
     */
    public static JoinType[] getPairTypes() {
        return new JoinType[] {
            _1AND2, _1OR2, _ALL1, _ALL2, _1NOT2, _2NOT1, _1XOR2,
        };
    }

    /** Selects only rows with input from both input tables. */
    public static final JoinType _1AND2 = new JoinType( "1and2", "1 and 2" ) {
        public String getDescription() {
            return "An output row for each row represented in both " +
                   "input tables (INNER JOIN)";
        }
        public LinkSet processLinks( LinkSet links, int[] rowCounts ) {
            return links;
        }
        public boolean[] getUsedTableFlags() {
            return new boolean[] { true, true };
        }
        public boolean getUsedMatchFlag() {
            return true;
        }
    };

    /** Selects rows with input from either or both input tables. */
    public static final JoinType _1OR2 = new JoinType( "1or2", "1 or 2" ) {
        public String getDescription() {
            return "An output row for each row represented in either or " +
                   "both of the input tables (FULL OUTER JOIN)";
        }
        public LinkSet processLinks( LinkSet links, int[] rowCounts ) {
            BitSet[] present = new BitSet[ 2 ];
            for ( int iTable = 0; iTable < 2; iTable++ ) {
                present[ iTable ] = getInclusion( links, iTable );
            }
            for ( int iTable = 0; iTable < 2; iTable++ ) {
                for ( int iRow = 0; iRow < rowCounts[ iTable ]; iRow++ ) {
                    if ( ! present[ iTable ].get( iRow ) ) {
                        links.addLink( new RowLink1( new RowRef( iTable,
                                                                 iRow ) ) );
                    }
                }
            }
            return links;
        }
        public boolean[] getUsedTableFlags() {
            return new boolean[] { true, true };
        }
        public boolean getUsedMatchFlag() {
            return true;
        }
    };

    private static class AllType extends JoinType {
        final int iTable_;
        AllType( String name, String longName, int iTable ) {
            super( name, longName );
            iTable_ = iTable;
        }
        public String getDescription() {
            return new StringBuffer()
               .append( "An output row for each matched or unmatched row in " )
               .append( "table " )
               .append( iTable_ + 1 )
               .append( " (" )
               .append( iTable_ == 0 ? "LEFT" : "RIGHT" )
               .append( " OUTER JOIN)" )
               .toString();
        }
        public LinkSet processLinks( LinkSet links, int[] rowCounts ) {
            BitSet present = getInclusion( links, iTable_ );
            for ( int irow = 0; irow < rowCounts[ iTable_ ]; irow++ ) {
                if ( ! present.get( irow ) ) {
                    links.addLink( new RowLink1( new RowRef( iTable_, irow ) ));
                }
            }
            return links;
        }
        public boolean[] getUsedTableFlags() {
            return new boolean[] { true, true };
        }
        public boolean getUsedMatchFlag() {
            return true;
        }
    };

    /** Selects all output rows with input from the first input table. */
    public static final JoinType _ALL1 =
        new AllType( "all1", "All from 1", 0 );

    /** Selects all output rows with input from the second input table. */
    public static final JoinType _ALL2 =
        new AllType( "all2", "All from 2", 1 );

    private static class NotType extends JoinType {
        final int yesTable_;
        final int noTable_;
        NotType( String name, String longName, int noTable ) {
            super( name, longName );
            noTable_ = noTable;
            yesTable_ = 1 - noTable_;
        }
        public String getDescription() {
            String[] ordinals = new String[] { "first", "second" };
            return "An output row only for rows which appear in the "
                 + ordinals[ yesTable_ ]
                 + " table but are not matched in the "
                 + ordinals[ noTable_ ] + " table";
        }
        public LinkSet processLinks( LinkSet links, int[] rowCounts ) {
            BitSet matched = new BitSet();
            for ( RowLink link : links ) {
                int nref = link.size();
                long noRow = -1;
                long yesRow = -1;
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    if ( ref.getTableIndex() == noTable_ ) {
                        noRow = ref.getRowIndex();
                    }
                    else if ( ref.getTableIndex() == yesTable_ ) {
                        yesRow = ref.getRowIndex();
                    }
                }
                if ( noRow >= 0 && yesRow >= 0 ) {
                    matched.set( Tables.checkedLongToInt( yesRow ) );
                }
            }
            links = createLinkSet();
            for ( int irow = 0; irow < rowCounts[ yesTable_ ]; irow++ ) {
                if ( ! matched.get( irow ) ) {
                    links.addLink( new RowLink1( new RowRef( yesTable_,
                                                             irow ) ) );
                }
            }
            return links;
        }
        public boolean[] getUsedTableFlags() {
            boolean[] flags = new boolean[ 2 ];
            flags[ noTable_ ] = false;
            flags[ yesTable_ ] = true;
            return flags;
        }
        public boolean getUsedMatchFlag() {
            return false;
        }
    }

    /**
     * Selects only rows in the second input table which are not matched
     * by any row in the first input table.
     */
    public static final JoinType _2NOT1 =
        new NotType( "2not1", "2 not 1", 0 );

    /**
     * Selects only rows in the first input table which are not matched
     * by any row in the second input table.
     */
    public static final JoinType _1NOT2 =
        new NotType( "1not2", "1 not 2", 1 );

    /**
     * Selects only rows with input from exactly one of the two input
     * tables.
     */
    public static final JoinType _1XOR2 = new JoinType( "1xor2", "1 xor 2" ) {
        public String getDescription() {
            return "An output row only for rows represented in one of "
                 + "the input tables but not the other one";
        }
        public LinkSet processLinks( LinkSet links, int[] rowCounts ) {
            BitSet[] present = new BitSet[ 2 ]; 
            for ( int iTable = 0; iTable < 2; iTable++ ) {
                present[ iTable ] = getInclusion( links, iTable );
            }
            links = createLinkSet();
            for ( int iTable = 0; iTable < 2; iTable++ ) {
                for ( int iRow = 0; iRow < rowCounts[ iTable ]; iRow++ ) {
                    if ( ! present[ iTable ].get( iRow ) ) {
                        links.addLink( new RowLink1( new RowRef( iTable,
                                                                 iRow ) ) );
                    }
                }
            }
            return links;
        }
        public boolean[] getUsedTableFlags() {
            return new boolean[] { true, true };
        }
        public boolean getUsedMatchFlag() {
            return false;
        }
    };

    /**
     * Constructs and returns a new empty mutable link set.
     *
     * @return   new link set
     */
    private static LinkSet createLinkSet() {
        return new HashSetLinkSet();
    }

    /**
     * Returns a flag vector indicating which rows for a given table
     * are represented in a link set.
     *
     * @param  links  link set
     * @param  iTable  table index of interest
     * @return  flag vector with a true element for every row which 
     *          appears for table index <code>iTable</code> in 
     *          <code>links</code>
     */
    private static BitSet getInclusion( LinkSet links, int iTable ) {
        BitSet present = new BitSet();
        for ( RowLink link : links ) {
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() == iTable ) {
                    present.set( Tables.checkedLongToInt( ref.getRowIndex() ) );
                }
            }
        }
        return present;
    }

}
