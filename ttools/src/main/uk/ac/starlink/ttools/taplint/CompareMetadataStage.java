package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;

/**
 * Validation stage for checking that two sets of table metadata
 * (presumably obtained via different routes) are equivalent to each other.
 *
 * @author   Mark Taylor
 * @since    7 Jun 2011
 */
public abstract class CompareMetadataStage implements Stage {

    private final String srcDesc1_;
    private final String srcDesc2_;
    private TableMeta[] tmetas1_;
    private TableMeta[] tmetas2_;

    /**
     * Constructor.
     *
     * @param  srcDesc1  short description of source of first metadata set
     * @param  srcDesc2  short description of source of second metadata set
     */
    protected CompareMetadataStage( String srcDesc1, String srcDesc2 ) {
        srcDesc1_ = srcDesc1;
        srcDesc2_ = srcDesc2;
    }

    public String getDescription() {
        return "Compare table metadata from " + srcDesc1_ + " and " + srcDesc2_;
    }

    /**
     * Returns first metadata set.
     * Called from {@link #run} method.
     */
    protected abstract TableMeta[] getMetas1();

    /**
     * Returns second metadata set.
     * Called from {@link #run} method.
     */
    protected abstract TableMeta[] getMetas2();

    public void run( URL serviceUrl, Reporter reporter ) {
        TableMeta[] tmetas1 = getMetas1();
        TableMeta[] tmetas2 = getMetas2();
        if ( tmetas1 == null || tmetas2 == null ) {
            reporter.report( Reporter.Type.WARNING, "NOTM",
                             "Don't have two metadata sets to compare" );
            return;
        }
        compareTables( reporter, tmetas1, tmetas2 );
    }

    /**
     * Compares the content of two arrays of TableMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  tmetas1  first table metadata set
     * @param  tmetas2  second table metadata set
     */
    private void compareTables( Reporter reporter,
                                TableMeta[] tmetas1, TableMeta[] tmetas2 ) {
        Map<String,TableMeta> tmMap1 = createNameMap( tmetas1 );
        Map<String,TableMeta> tmMap2 = createNameMap( tmetas2 );
        Collection<String> tNames =
            getIntersection( reporter, 'T', "Table", tmetas1, tmetas2 );
        for ( String tname : tNames ) {
            TableMeta tm1 = tmMap1.get( tname );
            TableMeta tm2 = tmMap2.get( tname );
            assert tm1 != null && tm2 != null : tm1 + " " + tm2;
            compareColumns( reporter, tname,
                            tm1.getColumns(), tm2.getColumns() );
            compareForeignKeys( reporter, tname,
                                tm1.getForeignKeys(), tm2.getForeignKeys() );
        }
    }

    /**
     * Compares the content of two arrays of ColumnMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  cmetas1  first column metadata set
     * @param  cmetas2  second column metadata set
     */
    private void compareColumns( Reporter reporter, String tableName,
                                 ColumnMeta[] cmetas1, ColumnMeta[] cmetas2 ) {
        Map<String,ColumnMeta> cmMap1 = createNameMap( cmetas1 );
        Map<String,ColumnMeta> cmMap2 = createNameMap( cmetas2 );
        Collection<String> cNames =
            getIntersection( reporter, 'C', "Column", cmetas1, cmetas2 );
        for ( String cname : cNames ) {
            ColumnMeta cm1 = cmMap1.get( cname );
            ColumnMeta cm2 = cmMap2.get( cname );
            Checker checker =
                new Checker( reporter, "Column", tableName + ":" + cname );
            checker.check( "Datatype", "CDTP",
                           cm1.getDataType(), cm2.getDataType() );
            checker.check( "UCD", "CUCD", cm1.getUcd(), cm2.getUcd() );
            checker.check( "Utype", "CUTP", cm1.getUtype(), cm2.getUtype() );
            checker.check( "Unit", "CUNI", cm1.getUnit(), cm2.getUnit() );
            checker.check( "IsIndexed", "CIDX",
                           cm1.isIndexed(), cm2.isIndexed() );
        }
    }

    /**
     * Compares the content of two arrays of ForeignMeta objects.
     *
     * @param  reporter  destination for validation messages
     * @param  fmetas1  first foreign key metadata set
     * @param  fmetas2  second foreign key metadata set
     */
    private void compareForeignKeys( Reporter reporter, String tableName,
                                     ForeignMeta[] fmetas1,
                                     ForeignMeta[] fmetas2 ) {

        /* ForeignMeta's toString method encodes most of its useful
         * content, so the checking done in getIntersection does pretty
         * much everything required.  Also, there is no identifier field
         * for foreign keys, so apart from that content, it's not clear
         * how you would work out which pairs are supposed to correspond. */
        getIntersection( reporter, 'F', "Foreign key", fmetas1, fmetas2 );
    }

    /**
     * Returns the list of names (toString results) which is common to
     * two arrays of objects.  If the objects differ, messages are
     * written to the reporter.
     *
     * @param  reporter  destination for validation messages
     * @param  lchr  labelling character for object type
     * @param  ltype  labelling name for object type
     * @param  items1  array of items with toString methods indicating identity
     * @param  items2  array of items with toString methods indicating identity
     * @return  collection of common toString values between input item sets
     */
    private Collection<String> getIntersection( Reporter reporter, char lchr,
                                                String ltype,
                                                Object[] items1,
                                                Object[] items2 ) {

        /* Prepare list of names. */
        List<String> names1 = new ArrayList<String>();
        for ( int i1 = 0; i1 < items1.length; i1++ ) {
            names1.add( items1[ i1 ].toString() );
        }
        List<String> names2 = new ArrayList<String>();
        for ( int i2 = 0; i2 < items2.length; i2++ ) {
            names2.add( items2[ i2 ].toString() );
        }

        /* Report on discrepancies. */
        List<String> extras1 = new ArrayList( names1 );
        List<String> extras2 = new ArrayList( names2 );
        extras1.removeAll( names2 );
        extras2.removeAll( names1 );
        for ( String ex1 : extras1 ) {
            reporter.report( Reporter.Type.ERROR, "" + lchr + "M12",
                             ltype + " " + ex1 + " in " + srcDesc1_
                           + " not " + srcDesc2_ );
        }
        for ( String ex2 : extras2 ) {
            reporter.report( Reporter.Type.ERROR, "" + lchr + "M21",
                             ltype + " " + ex2 + " in " + srcDesc2_
                           + " not " + srcDesc1_ );
        }

        /* Calculate and return intersection set. */
        List<String> intersect = new ArrayList( names1 );
        intersect.retainAll( names2 );
        return intersect;
    }

    /**
     * Returns a name->object map for a given object array, where the
     * name is obtained using the toString method.
     * No reporting or checking is done.
     *
     * @param   items   item array
     * @return  name->item map
     */
    private static <T> Map<String,T> createNameMap( T[] items ) {
        Map<String,T> map = new LinkedHashMap<String,T>();
        for ( int i = 0; i < items.length; i++ ) {
            T item = items[ i ];
            map.put( item.toString(), item );
        }
        return map;
    }

    /**
     * Utility method to construct a CompareMetadataStage instance given
     * two TableMetadataStage objects.
     *
     * @param  stage1  first metadata producing stage
     * @param  stage2  second metadata producing stage
     * @return   comparison stage
     */
    public static CompareMetadataStage
            createStage( final TableMetadataStage stage1,
                         final TableMetadataStage stage2 ) {
        return new CompareMetadataStage( stage1.getSourceDescription(),
                                         stage2.getSourceDescription() ) {
            protected TableMeta[] getMetas1() {
                return stage1.getTableMetadata();
            }
            protected TableMeta[] getMetas2() {
                return stage2.getTableMetadata();
            }
        };
    }

    /**
     * Class which checks and reports on equality of objects.
     */
    private static class Checker {
        private final Reporter reporter_;
        private final String objectType_;
        private final String objectName_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  objectType  category of object being checked
         * @param  objectName  identifier of object being checked
         */
        Checker( Reporter reporter, String objectType, String objectName ) {
            reporter_ = reporter;
            objectType_ = objectType;
            objectName_ = objectName;
        }

        /**
         * Check equality of two items with the same name.
         *
         * @param    itemName  name of item
         * @param    code  reporter code for validation messages
         * @param    item1  first item
         * @param    item2  second item
         */
        void check( String itemName, String code, Object item1, Object item2 ) {
            if ( item1 == null && item2 == null ||
                 item1 != null && item1.equals( item2 ) ) {
                // ok
            }
            else {
                String q1 = item1 instanceof String ? "\"" : "";
                String q2 = item2 instanceof String ? "\"" : "";
                String msg = new StringBuffer()
                    .append( itemName )
                    .append( " mismatch for " )
                    .append( objectType_ )
                    .append( " " )
                    .append( objectName_ )
                    .append( "; " )
                    .append( q1 )
                    .append( item1 )
                    .append( q1 )
                    .append( " != " )
                    .append( q2 )
                    .append( item2 )
                    .append( q2 )
                    .toString();
                reporter_.report( Reporter.Type.WARNING, code, msg );
            }
        }
    }
}
