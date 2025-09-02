package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.starlink.parquet.ParquetDump;
import uk.ac.starlink.parquet.ParquetStarTable;
import uk.ac.starlink.parquet.ParquetUtil;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.MultiChoiceParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;

/**
 * Utility for examining metadata of Parquet files.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2024
 */
public class ParquetTool implements Task {

    private final Parameter<String> locParam_;
    private final MultiChoiceParameter<MetaItem> itemsParam_;
    private final Parameter<?>[] params_;

    private static final String DFLT_TOKEN = "default";
    private static final int MAXCHAR = 75;

    /**
     * Constructor.
     */
    public ParquetTool() {
        List<Parameter<?>> paramList = new ArrayList<>();

        locParam_ = new StringParameter( "in" );
        locParam_.setPosition( 1 );
        locParam_.setPrompt( "Location of parquet file" );
        locParam_.setUsage( "<filename>" );
        locParam_.setDescription( new String[] {
            "<p>Name of the parquet file to examine.",
            "</p>",
        } );
        paramList.add( locParam_ );

        itemsParam_ =
            new MultiChoiceParameter<MetaItem>( "items", MetaItem.class, ',',
                                                MetaItem.DFLT_ITEMS,
                                                DFLT_TOKEN );
        itemsParam_.setStringDefault( DFLT_TOKEN );
        itemsParam_.setNullPermitted( true );
        itemsParam_.setPrompt( "Metadata items to display" );
        List<MetaItem> itemList =
            new ArrayList<>( Arrays.asList( MetaItem.DFLT_ITEMS ) );
        for ( MetaItem extraItem : MetaItem.EXTRA_ITEMS ) {
            itemsParam_.addOption( extraItem );
            itemList.add( extraItem );
        }
        itemsParam_.setDescription( new String[] {
            "<p>Selects which items of metadata about the parquet file",
            "to display.  The value is a comma-separated list,",
            "containing zero or more of the following:",
            DocUtils.describedList( itemList.toArray( new MetaItem[ 0 ] ),
                                    m -> m.name_, m -> m.description_, false ),
            "If the value is the special token",
            "\"<code>" + DFLT_TOKEN + "</code>\"",
            "then all the items above",
            "except for",
            Arrays.stream( MetaItem.EXTRA_ITEMS )
                  .map( m -> "\"<code>" + m + "</code>\"" )
                  .collect( Collectors.joining( ", " ) ),
            "will be output.",
            "If the value is blank then none of these will be output.",
            "Either way, informational reports will still be written",
            "as requested.",
            "</p>",
            "<p>The text is written to standard output.",
            "If there are multiple items to display, they are indented",
            "and presented under headings.",
            "In the case that there is only one however, it is output",
            "without adornment.",
            "</p>",
        } );
        paramList.add( itemsParam_ );

        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public String getPurpose() {
        return "Presents information about a parquet file";
    }

    public Parameter<?>[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        String loc = locParam_.stringValue( env );
        boolean tryUrl = false;
        PrintStream out = env.getOutputStream();
        MetaItem[] items = itemsParam_.objectValue( env );
        return () -> {
            ParquetStarTable starTable = ParquetDump.readParquetTable( loc );
            ParquetDump dump = new ParquetDump( starTable );
            if ( items == null || items.length == 0 ) {
            }
            else if ( items.length == 1 ) {
                String txt = items[ 0 ].action_.apply( dump );
                if ( txt != null && txt.trim().length() > 0 ) {
                    out.println( txt );
                }
            }
            else {
                for ( MetaItem item : items ) {
                    String txt = item.action_.apply( dump );
                    if ( txt == null ) {
                        txt = "";
                    }
                    out.println( formatLines( item.heading_, txt ) );
                }
            }
        };
    }

    /**
     * Displays a multi-line string under a given heading.
     *
     * @param  heading  heading text
     * @param  lines  multi-line string
     * @return   multi-line string containing heading and content
     */
    private String formatLines( String heading, String lines ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( heading )
            .append( ":\n" );
        if ( lines != null ) {
            for ( String line : lines.split( "[\n\r]+" ) ) {
                sbuf.append( "   " )
                    .append( line )
                    .append( '\n' );
            }
        }
        return sbuf.toString();
    }

    /**
     * Describes a metadata item that can be shown to the user.
     */
    public static class MetaItem {

        final String name_;
        final String heading_;
        final String description_;
        final Function<ParquetDump,String> action_;

        /** Parquet schema. */
        public static final MetaItem SCHEMA =
            new MetaItem( "schema", "Parquet Schema",
                          "displays the parquet schema",
                          ParquetDump::formatSchema );

        /** Parquet key-value metadata, compact format. */
        public static final MetaItem KV_COMPACT =
            new MetaItem( "keyvalue", "Key-Value Metadata, compact format",
                          "displays the parquet per-table " +
                          "key-value metadata pairs in a compact format; " +
                          "long or multi-line values are summarised",
                          d -> d.formatKeyValuesCompact( MAXCHAR ) );

        /** Parquet key-value metadata, full content. */
        public static final MetaItem KV_FULL =
            new MetaItem( "keyvaluefull", "Key-Value Metadata, full content",
                          "displays the full content of parquet per-table " +
                          "key-value metadata pairs",
                          ParquetDump::formatKeyValuesFull );

        /** Data blocks. */
        public static final MetaItem BLOCKS =
            new MetaItem( "blocks", "Data Blocks",
                          "displays information about the parquet data blocks",
                          ParquetDump::formatBlocks );

        /** Column chunks. */
        public static final MetaItem CHUNKS =
            new MetaItem( "chunks", "Column Chunks",
                          "displays information about the column chunks in "
                        + "the parquet file",
                          ParquetDump::formatColumnChunks );

        /** VOParquet data-less VOTable document. */
        public static final MetaItem VOTABLE =
            new MetaItem( "votable", "VOParquet VOTable",
                          "displays the VOTable document providing " +
                          "additional metadata according to " +
                          "the VOParquet convention",
                          dump -> dump.getTable().getVOTableMetadataText() );

        /** Array of default list of items in some reasonable order. */
        public static final MetaItem[] DFLT_ITEMS = new MetaItem[] {
            SCHEMA, KV_COMPACT, BLOCKS, CHUNKS, VOTABLE,
        };

        /** Array of additional items in some reasonable order. */
        public static final MetaItem[] EXTRA_ITEMS = new MetaItem[] {
            KV_FULL,
        };

        /**
         * Constructor.
         *
         * @param  name  item name, as selected by user
         * @param  heading  item heading for display
         * @param  description  description of function
         * @param  action   maps a ParquetDump object to a multi-line string
         *                  containing the information to be displayed
         */
        MetaItem( String name, String heading, String description,
                  Function<ParquetDump,String> action ) {
            name_ = name;
            heading_ = heading;
            description_ = description;
            action_ = action;
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}
