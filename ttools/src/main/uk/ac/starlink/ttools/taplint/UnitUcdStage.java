package uk.ac.starlink.ttools.taplint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.starlink.ttools.votlint.UcdStatus;
import uk.ac.starlink.ttools.votlint.UnitStatus;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapService;

/**
 * Validation stage for checking column metadata elements from
 * controlled vocabularies; for now this is units and UCDs.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2021
 */
public class UnitUcdStage implements Stage {

    private final MetadataHolder metaHolder_;

    private static final int MAX_LOCS = 4;
    private static final ItemChecker UNIT_CHECKER = createUnitChecker();
    private static final ItemChecker UCD_CHECKER = createUcdChecker();

    /**
     * Constructor.
     *
     * @param  metaHolder  service metadata 
     */
    public UnitUcdStage( MetadataHolder metaHolder ) {
        metaHolder_ = metaHolder;
    }

    public String getDescription() {
        return "Check column units and UCDs are legal";
    }

    public void run( Reporter reporter, TapService tapService ) {

        /* Retrieve list of tables in service. */
        SchemaMeta[] smetas = metaHolder_.getTableMetadata();
        List<TableMeta> tlist = new ArrayList<TableMeta>();
        if ( smetas != null ) {
            for ( SchemaMeta smeta : smetas ) {
                for ( TableMeta tmeta : smeta.getTables() ) {
                    tlist.add( tmeta );
                }
            }
        }

        /* In case of no table metadata bail out. */
        TableMeta[] tmetas = tlist.toArray( new TableMeta[ 0 ] );
        if ( tmetas.length == 0 ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata available"
                           + " (earlier stages failed/skipped?)"
                           + " - will not check units/UCDs" );
            return;
        }

        /* Otherwise check units and UCDs. */
        reportBadValues( reporter, tmetas,
                         "unit", ColumnMeta::getUnit, UNIT_CHECKER );
        reportBadValues( reporter, tmetas,
                         "UCD", ColumnMeta::getUcd, UCD_CHECKER );
    }

    /**
     * Assemble and report information about checkable column metadata items.
     *
     * @param  reporter   reporter
     * @param  tmetas   table metadata
     * @param  itemType  user-readable name of metadata item being checked
     * @param  itemFunc  extracts the metadata item from the column metadata
     * @param  checker   checks item values
     */
    private void reportBadValues( Reporter reporter, TableMeta[] tmetas,
                                  String itemType,
                                  Function<ColumnMeta,String> itemFunc,
                                  ItemChecker checker ) {

        /* Prepare a list of distinct non-compliant item values along with
         * reporting information.  By doing the collection and reporting
         * in two stages we can make the reporting more readable by
         * reducing the number of reports. */
        Set<String> okSet = new HashSet<>();
        Map<String,Issue> issueMap = new LinkedHashMap<>();
        for ( TableMeta tmeta : tmetas ) {
            for ( ColumnMeta cmeta : tmeta.getColumns() ) {
                String item = itemFunc.apply( cmeta );
                if ( okSet.contains( item ) ) {
                    // no action required
                }
                else if ( issueMap.containsKey( item ) ) {
                    issueMap.get( item ).locs_
                            .add( new Location( tmeta, cmeta ) );
                }
                else {
                    Message message = checker.check( item );
                    if ( message == null ) {
                        okSet.add( item );
                    }
                    else {
                        issueMap.put( item,
                                      new Issue( message, tmeta, cmeta ) );
                    }
                }
            }
        }

        /* For each non-compliant value, issue a report giving specific
         * validation information as well as a list of the columns in
         * which it appears. */
        for ( Map.Entry<String,Issue> entry : issueMap.entrySet() ) {
            String item = entry.getKey();
            Issue issue = entry.getValue();
            Message message = issue.message_;
            List<Location> locs = issue.locs_;
            StringBuffer sbuf = new StringBuffer()
               .append( message.code_.getType() == ReportType.ERROR
                        ? "Bad "
                        : "Questionable " )
               .append( itemType )
               .append( " \"" )
               .append( item )
               .append( "\"; " )
               .append( message.txt_ );
            int nloc = locs.size();
            if ( nloc == 1 ) {
                sbuf.append( " in column " ) 
                    .append( locs.get( 0 ) );
            }
            else {
                sbuf.append( " in " )
                    .append( nloc )
                    .append( " columns: " )
                    .append( locs.stream()
                                 .limit( MAX_LOCS )
                                 .map( Location::toString )
                                 .collect( Collectors.joining( ", " ) ) );
                if ( nloc > MAX_LOCS ) {
                    sbuf.append( ", ..." );
                }
            }
            reporter.report( message.code_, sbuf.toString() );
        }
    }

    /**
     * Creates a checker for VOUnits.
     *
     * @param  syntax  unit syntax instance
     * @return  new checker
     */
    private static ItemChecker createUnitChecker() {
        return unit -> {
            UnitStatus status = UnitStatus.getStatus( unit );
            if ( status != null ) {
                UnitStatus.Code ucode = status.getCode();

                /* A VOUnits error is not a TAP error, since neither TAP
                 * (TAP_SCHEMA) nor VODataService (/tables endpoint) mandates
                 * use of VOUnits for column unit metadata.
                 * VOTable notes VOUnits as "recommended".
                 * So the worst report level we will flag here is Warning. */
                final ReportCode rcode;
                final String descrip;
                if ( ucode.isError() ) {
                    rcode = FixedCode.W_VUNE;
                    descrip = "Not legal";
                }
                else if ( ucode.isWarning() ) {
                    rcode = FixedCode.W_VUNR;
                    descrip = "Questionable";
                }
                else {
                    rcode = null;
                    descrip = null;
                }
                if ( rcode != null ) {
                    String txt = new StringBuffer()
                       .append( descrip )
                       .append( " VOUnits string (" )
                       .append( ucode )
                       .append( "): " )
                       .append( status.getMessage() )
                       .toString();
                    return new Message( rcode, txt );
                }
            }
            return null;
        };
    }

    /**
     * Creates a checker for UCDs.
     *
     * @return  new checker
     */
    private static ItemChecker createUcdChecker() {
        return ucd -> {
            UcdStatus status = UcdStatus.getStatus( ucd );
            if ( status != null ) {
                UcdStatus.Code code = status.getCode();

                /* TAP itself does not mention UCD syntax.
                 * VODataService says "There are no requirements for
                 * compliance with any particular UCD standard", so
                 * UCD1+, UCD1 and SIAv1 may be used. 
                 * VOTable 1.4 says "UCD1+ usage is recommended, but
                 * applications using the older vocabulary are still
                 * acceptable".  I take from this that UCDs must conform
                 * to some version of the UCD syntax, but failure to
                 * conform to UCD1+ is not an error. */
                if ( code.isError() ) {
                    return new Message( FixedCode.E_UCDX,
                                        code + ": " + status.getMessage() );
                }
                if ( code.isWarning() ) {
                    return new Message( FixedCode.W_UCDQ,
                                        code + ": " + status.getMessage() );
                }
            }
            return null;
        };
    }

    /**
     * Checks values from a controlled vocabulary.
     */
    @FunctionalInterface
    private static interface ItemChecker {

        /**
         * Checks a value for compliance with some rule.
         *
         * @param  value   string that ought to match this checker's constraints
         * @return   null if value passes checks,
         *           or a message indicating the problem if it fails
         */
        Message check( String value );
    }

    /**
     * Utility class to aggregate a ReportCode and message string.
     */
    private static class Message {
        final ReportCode code_;
        final String txt_;
        private Message( ReportCode code, String txt ) {
            code_ = code;
            txt_ = txt;
        }
    }

    /**
     * Utility class to aggregate a column and its parent table.
     */
    private static class Location {
        final TableMeta tmeta_;
        final ColumnMeta cmeta_;
        Location( TableMeta tmeta, ColumnMeta cmeta ) {
            tmeta_ = tmeta;
            cmeta_ = cmeta;
        }
        public String toString() {
            return tmeta_.getName() + "." + cmeta_.getName();
        }
    }

    /**
     * Utility class to aggregate a Message and the Locations to which
     * it applies.
     */
    private static class Issue {
        final Message message_;
        final List<Location> locs_;
        Issue( Message message ) {
            message_ = message;
            locs_ = new ArrayList<Location>();
        }
        Issue( Message message, TableMeta tmeta, ColumnMeta cmeta ) {
            this( message );
            locs_.add( new Location( tmeta, cmeta ) );
        }
    }
}
