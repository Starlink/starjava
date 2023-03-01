package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.util.IntList;

/**
 * Filter for identifying and removing columns with constant content.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2023
 */
public class ConstFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public ConstFilter() {
        super( "constcol",
               "[-noparam] [-acceptnull] [-[no]parallel] "
             + "[<colid-list>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Identifies columns with constant values.",
            "Such columns are removed from the table and by default",
            "their fixed value is added to the table",
            "as a table parameter",
            "with the same name as the removed column.",
            "Such columns may have scalar or array values.",
            "</p>",
            "<p>The <code>-noparam</code> flag",
            "controls whether constant columns identified are recorded instead",
            "as table parameters (per-table metadata items).",
            "By default they are, but supplying <code>-noparam</code>",
            "means these values will just be discarded.",
            "</p>",
            "<p>The <code>-acceptnull</code> flag",
            "controls how blank values in candidate columns are treated.",
            "By default, all values in a column must be strictly the same",
            "for a column to be identified as constant value,",
            "but if <code>-acceptnull</code> is supplied",
            "then a column will be treated as constant if all its entries",
            "are <em>either</em> a single fixed value <em>or</em> blank.",
            "</p>",
            "<p>The <code>-[no]parallel</code> flag",
            "controls whether processing is done using multithreading",
            "for large tables.",
            "</p>",
            "<p>The <code>&lt;colid-list&gt;</code>",
            "gives the columns to be assessed by this filter;",
            "if not supplied, all columns will be examined.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        boolean acceptNull = false;
        boolean toParam = true;
        boolean isParallel = false;
        String colIdList = null;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            argIt.remove();
            if ( "-param".equalsIgnoreCase( arg ) ) {
                toParam = true;
            }
            else if ( "-noparam".equalsIgnoreCase( arg ) ) {
                toParam = false;
            }
            else if ( "-acceptnull".equalsIgnoreCase( arg ) ) {
                acceptNull = true;
            }
            else if ( "-noacceptnull".equalsIgnoreCase( arg ) ) {
                acceptNull = false;
            }
            else if ( "-parallel".equalsIgnoreCase( arg ) ) {
                isParallel = true;
            }
            else if ( "-noparallel".equalsIgnoreCase( arg ) ) {
                isParallel = false;
            }
            else if ( arg.startsWith( "-" ) ) {
                throw new ArgException( "Unknown flag \"" + arg + "\"" );
            }
            else if ( colIdList == null ) {
                colIdList = arg;
            }
            else {
                throw new ArgException( "Multiple column lists" );
            }
        }
        final boolean acceptNull0 = acceptNull;
        final boolean toParam0 = toParam;
        final String colIdList0 = colIdList;
        final RowRunner runner = isParallel ? RowRunner.DEFAULT
                                            : RowRunner.SEQUENTIAL;
        return new ProcessingStep() {
            public StarTable wrap( StarTable inTable ) throws IOException {

                /* Work out which columns to assess. */
                int nc = inTable.getColumnCount();
                int[] icols =
                      colIdList0 == null
                    ? IntStream.range( 0, nc ).toArray()
                    : new ColumnIdentifier( inTable )
                     .getColumnIndices( colIdList0 );

                /* Read table and identify constant-value columns. */
                State[] states =
                    runner.collect( new StateCollector( icols, acceptNull0 ),
                                    inTable );
                IntList iconstList = new IntList();
                List<State> constStates = new ArrayList<>();
                for ( int is = 0; is < icols.length; is++ ) {
                    State state = states[ is ];
                    if ( ! state.hasDifferent_ ) {
                        iconstList.add( icols[ is ] );
                        constStates.add( state );
                    }
                }
                int[] iconsts = iconstList.toIntArray();

                /* If there are no constant columns, return the input table
                 * unchanged. */
                if ( iconsts.length == 0 ) {
                    return inTable;
                }

                /* Otherwise prepare a new output table in which the
                 * constant columns are removed, and optionally their
                 * fixed values are inserted as parameters. */
                else {
                    StarTable outTable =
                        ColumnPermutedStarTable
                       .deleteColumns( inTable, iconsts );
                    if ( toParam0 ) {
                        for ( int is = 0; is < iconsts.length; is++ ) {
                            ColumnInfo cinfo =
                                inTable.getColumnInfo( iconsts[ is ] );
                            ValueInfo vinfo = new DefaultValueInfo( cinfo );
                            Object value = constStates.get( is ).value_;
                            outTable.getParameters()
                                    .add( new DescribedValue( vinfo, value ) );
                        }
                    }
                    return outTable;
                }
            }
        };
    }

    /**
     * Collector for identifying fixed-value state of certain columns
     * in a table.
     *
     * <p>The accumulator is an array of <code>State</code> objects,
     * one per column under assessment.
     */
    private static class StateCollector extends RowCollector<State[]> {

        private final int[] icols_;
        private final boolean acceptNull_;
        private final int ns_;

        /**
         * Constructor.
         *
         * @param  icols  column indices of columns to assess
         * @param  acceptNull  if true, null values are ignored when
         *                     identifying constant values;
         *                     if false, all column entries must be
         *                     strictly equal
         */
        StateCollector( int[] icols, boolean acceptNull ) {
            icols_ = icols;
            acceptNull_ = acceptNull;
            ns_ = icols.length;
        }

        public State[] createAccumulator() {
            State[] states = new State[ ns_ ];
            for ( int is = 0; is < ns_; is++ ) {
                states[ is ] = new State();
            }
            return states;
        }

        public State[] combine( State[] states1, State[] states2 ) {
            for ( int is = 0; is < ns_; is++ ) {
                State s1 = states1[ is ];
                State s2 = states2[ is ];
                if ( s2.hasDifferent_ ) {
                    s1.hasDifferent_ = true;
                }
                else if ( s2.hasValue_ ) {
                    if ( s1.hasValue_ ) {
                        if ( isDifferent( s1.value_, s2.value_ ) ) {
                            s1.hasDifferent_ = true;
                        }
                    }
                    else {
                        states1[ is ] = s2;
                    }
                }
            }
            return states1;
        }

        public void accumulateRows( RowSplittable rseq, State[] states )
                throws IOException {
            while ( ! allDifferent( states ) && rseq.next() ) {
                for ( int is = 0; is < ns_; is++ ) {
                    Object v = rseq.getCell( icols_[ is ] );
                    State s0 = states[ is ];
                    if ( ! s0.hasDifferent_ ) {
                        if ( !acceptNull_ || !Tables.isBlank( v ) ) {
                            if ( s0.hasValue_ ) {
                                if ( isDifferent( s0.value_, v ) ) {
                                    s0.hasDifferent_ = true;
                                }
                            }
                            else {
                                s0.hasValue_ = true;
                                s0.value_ = v;
                            }
                        }
                    }
                }
            }
        }

        /**
         * Assesses whether two values in a column are materially different.
         *
         * @param  v1  first value
         * @param  v2  second value
         * @return  true iff they count as different
         */
        private boolean isDifferent( Object v1, Object v2 ) {
            boolean null1 = Tables.isBlank( v1 );
            boolean null2 = Tables.isBlank( v2 );
            if ( null1 && null2 ) {
                return false;
            }
            else if ( null1 || null2 ) {
                return ! acceptNull_;
            }
            else {
                Class<?> clazz = v1.getClass();
                if ( ! clazz.equals( v2.getClass() ) ) {
                    return true;
                }
                else {
                    return clazz.getComponentType() == null
                         ? ! v1.equals( v2 )
                         : ! arrayEquals( v1, v2 );
                }
            }
        }
    }

    /**
     * Assess equality of two array values of the same type.
     *
     * @param  a1  non-null array of primitives or objects
     * @param  a2  non-null array with the same class as <code>a1</code>
     */
    private static boolean arrayEquals( Object a1, Object a2 ) {
        Class<?> clazz = a1.getClass();
        if ( byte[].class.equals( clazz ) ) {
            return Arrays.equals( (byte[]) a1, (byte[]) a2 );
        }
        else if ( short[].class.equals( clazz ) ) {
            return Arrays.equals( (short[]) a1, (short[]) a2 );
        }
        else if ( int[].class.equals( clazz ) ) {
            return Arrays.equals( (int[]) a1, (int[]) a2 );
        }
        else if ( long[].class.equals( clazz ) ) {
            return Arrays.equals( (long[]) a1, (long[]) a2 );
        }

        // Note that Arrays.equals() for float[] and double[],
        // unlike the == operator, does treat one NaN array element
        // as equal to a matching NaN array element.
        else if ( float[].class.equals( clazz ) ) {
            return Arrays.equals( (float[]) a1, (float[]) a2 );
        }
        else if ( double[].class.equals( clazz ) ) {
            return Arrays.equals( (double[]) a1, (double[]) a2 );
        }
        else if ( boolean[].class.equals( clazz ) ) {
            return Arrays.equals( (boolean[]) a1, (boolean[]) a2 );
        }
        else if ( char[].class.equals( clazz ) ) {
            return Arrays.equals( (char[]) a1, (char[]) a2 );
        }
        else if ( a1 instanceof Object[] ) {
            return Arrays.equals( (Object[]) a1, (Object[]) a2 );
        }
        else {
            assert false : clazz;
            return false;
        }
    }

    /**
     * Determines whether all of the states in a supplied array
     * have observed a difference.
     *
     * @param  states  array of states
     * @return  true iff hasDifferent_ is true for every element of states
     */
    private static boolean allDifferent( State[] states ) {
        int ns = states.length;
        for ( int is = 0; is < ns; is++ ) {
            if ( ! states[ is ].hasDifferent_ ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Records the current state of observation of a column.
     * Null may or may not count as "notable" value depending on
     * the acceptNull configuration.
     */
    private static class State {

        /**
         * Initially false, but set true once a notable value
         * has been observed.
         */
        boolean hasValue_;

        /**
         * Initially false, but set true once more than one distinct
         * notable value has been observed.
         */
        boolean hasDifferent_;

        /**
         * Contains the unique observed notable value,
         * but only if <code>hasValue_</code> is true
         * and <code>hasDifferent_</code> is false;
         * otherwise does not contain useful information.
         */
        Object value_;
    }
}
