package uk.ac.starlink.vo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Defines an ordering for TAP metadata items:
 * schemas and the tables within them.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2024
 */
public enum TapMetaOrder {

    /** Alphabetical ordering. */
    ALPHABETIC( comparator1( SchemaMeta::getName ),
                comparator1( TableMeta::getName ) ),

    /** Ordering based on schema/table index (may or may not be reliable). */
    INDEXED( comparator2( SchemaMeta::getIndex, SchemaMeta::getName ),
             comparator2( TableMeta::getIndex, TableMeta::getName ) );

    private final Comparator<SchemaMeta> schemaComparator_;
    private final Comparator<TableMeta> tableComparator_;

    /**
     * Constructor.
     *
     * @param  schemaComparator  comparator for schemas
     * @param  tableComparator  comparator for tables
     */
    TapMetaOrder( Comparator<SchemaMeta> schemaComparator,
                  Comparator<TableMeta> tableComparator ) {
        schemaComparator_ = schemaComparator;
        tableComparator_ = tableComparator;
    }

    /**
     * Modifies a set of schemas to obey this ordering.
     * The order of the elements of the supplied array may change in place,
     * and the ordering of tables within each one may also change.
     *
     * @param  schemas  array of schemas to alter in place
     */
    public void sortSchemas( SchemaMeta[] schemas ) {
        Arrays.sort( schemas, schemaComparator_ );
        for ( SchemaMeta schema : schemas ) {
            schema.setTableOrder( tableComparator_ );
        }
    }

    /**
     * Returns a comparator that orders based on a sort key that may be null
     * extracted from the objects to be compared.
     *
     * @param  keyFunc  function used to extract a sort key which may be null
     * @return  comparator
     */
    private static <T,U extends Comparable<? super U>>
            Comparator<T> comparator1( Function<T,U> keyFunc ) {
        return Comparator
              .comparing( keyFunc,
                          Comparator.nullsLast( Comparator.naturalOrder() ) );
    }

    /**
     * Returns a comparator that orders on two sort keys that may be null
     * extracted from the objects to be compared.
     *
     * @param  keyFunc1  initial function used to extract a sort key
     *                   which may be null
     * @param  keyFunc2  fallback function used to extract a sort key
     *                   which may be null
     * @return  comparator
     */
    private static <T,U extends Comparable<? super U>,
                      V extends Comparable<? super V>>
            Comparator<T> comparator2( Function<T,U> keyFunc1,
                                       Function<T,V> keyFunc2 ) {
        return comparator1( keyFunc1 ).thenComparing( comparator1( keyFunc2 ) );
    }
}
