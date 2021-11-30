package uk.ac.starlink.pds4;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * TableBuilder implementation for the NASA PDSv4 format.
 *
 * @author  Mark Taylor
 * @since   24 Nov 2021
 * @see   <a href="https://pds.nasa.gov/datastandards/documents/"
 *                >https://pds.nasa.gov/datastandards/documents/</a>
 */
public class Pds4TableBuilder extends DocumentedTableBuilder
                              implements MultiTableBuilder {

    private boolean checkMagic_;
    private boolean observationalOnly_;
    private String[] blankSpecials_;

    // From https://pds.nasa.gov/pds4/pds/v1/PDS4_PDS_1G00.xsd
    private static final Collection<String> MAGIC_NAMESPACES =
            new HashSet<>( Arrays.asList( new String[] {
        "http://pds.nasa.gov/pds4/pds/v1",
    } ) );
 
    // From PDS Data Provider's Handbook version 1.16.0, sec 6.2.1.
    private static final Collection<String> MAGIC_ELEMENTS =
            new HashSet<>( Arrays.asList( new String[] {
        "Product_Ancillary",
        "Product_Browse",
        "Product_Bundle",
        "Product_Collection",
        "Product_Context",
        "Product_Document",
        "Product_File_Text",
        "Product_Native",
        "Product_Observational",
        "Product_SPICE_Kernel",
        "Product_Thumbnail",
        "Product_XML_Schema",
        "Product_Zipped",
    } ) );

    /**
     * Constructor.
     */
    public Pds4TableBuilder() {

        // The .lblx extension is not yet current at PDS4 1.16.0,
        // which mandates ".xml" for file extensions,
        // but apparently will be in future PDS4 versions.
        super( new String[] { ".lblx" } );
        checkMagic_ = true;
        observationalOnly_ = false;
        blankSpecials_ = LabelParser.DEFAULT_BLANK_SPECIALS;
    }

    public String getFormatName() {
        return "PDS4";
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>NASA's Planetary Data System version 4 format is described at",
            DocumentedIOHandler.toLink( "https://pds.nasa.gov/datastandards/" )
            + ".",
            "This implementation is based on v1.16.0 of PDS4.",
            "</p>",
            "<p>PDS4 files consist of an XML <em>Label</em> file which",
            "provides detailed metadata, and which may also contain references",
            "to external data files stored alongside it.",
            "This input handler looks for (binary, character or delimited)",
            "tables in the Label;",
            "depending on the configuration it may restrict them to those",
            "in the <code>File_Area_Observational</code> area.",
            "The Label is the file which has to be presented to this",
            "input handler to read the table data.",
            "Because of the relationship between the label and the data files,",
            "it is usually necessary to move them around together.",
            "</p>",
            "<p>If there are multiple tables in the label,",
            "you can refer to an individual one using the \"<code>#</code>\"",
            "specifier after the label file name by table <code>name</code>,",
            "<code>local_identifier</code>, or 1-based index",
            "(e.g. \"<code>label.xml#1</code>\" refers to the first table).",
            "</p>",
            "<p>If there are <code>Special_Constants</code> defined",
            "in the label, they are in most cases interpreted as blank values",
            "in the output table data.",
            "At present, the following special values are interpreted",
            "as blanks:",
            Arrays.stream( blankSpecials_ )
                  .map( s -> "  <code>" + s + "</code>" )
                  .collect( Collectors.joining( ",\n" ) ),
            ".",
            "</p>",
            "<p>Fields within top-level Groups are interpreted",
            "as array values.",
            "Any fields in nested groups are ignored.",
            "For these array values only limited null-value substitution",
            "can be done (since array elements are primitives and so cannot",
            "take null values).",
            "</p>",
            "<p>This input handler is somewhat experimental,",
            "and the author is not a PDS expert.",
            "If it behaves strangely or you have suggestions for how it",
            "could work better, please contact the author.",
            "</p>",
        "" );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * No chance of streaming a format which stores the data in a
     * different file.
     */
    public boolean canStream() {
        return false;
    }

    /**
     * Sets whether the handler will attempt to guess by looking at
     * the file whether it appears to be a PDS4 file before attempting
     * to parse it as one.  This is generally a good idea,
     * since otherwise it will attempt to parse any old file as PDS4
     * which may consume resources (especially if it's a large XML file),
     * but you can set it false to try to parse PDS4 files with
     * unexpected first few bytes.
     *
     * @param   checkMagic  true to require magic number presence
     */
    @ConfigMethod(
        property = "checkmagic",
        doc = "<p>Determines whether an initial test is made to see whether\n"
            + "the file looks like PDS4 before attempting to read it as one.\n"
            + "The tests are ad-hoc and look for certain elements\n"
            + "and namespaces that are expected to appear near the start of\n"
            + "a table-containing PDS4 file, but it's not bulletproof.\n"
            + "Setting this true is generally a good idea\n"
            + "to avoid attempting to parse non-PDS4 files,\n"
            + "but you can set it false to attempt to read an PDS4 file\n"
            + "that starts with the wrong sequence.\n"
            + "</p>",
        example = "false"
    )
    public void setCheckMagic( boolean checkMagic ) {
        checkMagic_ = checkMagic;
    }

    /**
     * Sets whether only tables within File_Area_Observational elements
     * of the PDS4 label are interpreted as StarTables.
     *
     * @param  observationalOnly  if true, only observational tables are found,
     *                            if false, others may be found as well
     */
    @ConfigMethod(
        property = "observational",
        doc = "<p>Determines whether only tables within a\n"
            + "<code>&lt;File_Area_Observational&gt;</code> element\n"
            + "of the PDS4 label should be included.\n"
            + "If true, only observational tables are found,\n"
            + "if false, other tables will be found as well.\n"
            + "</p>",
        example = "true"
    )
    public void setObservationalOnly( boolean observationalOnly ) {
        observationalOnly_ = observationalOnly;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream" );
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage )
            throws IOException {
        String pos = datsrc.getPosition();


        int ifile = 0;
        Label label = parseLabel( datsrc );
        Table[] tables = label.getTables();
        final Table table;
        if ( tables.length == 0 ) {
            throw new TableFormatException( "No tables in PDS4 label" );
        }
        else if ( pos != null && pos.trim().length() > 0 ) {
            table = getPositionedTable( tables, pos.trim() );
        }
        else {
            table = tables[ 0 ];
        }
        return createStarTable( table, label.getContextUrl() );
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy storage )
            throws IOException {
        Label label = parseLabel( datsrc );
        URL contextUrl = label.getContextUrl();
        Table[] tables = label.getTables();
        return new TableSequence() {
            int iNext_;
            public StarTable nextTable() throws IOException {
                return iNext_ < tables.length
                     ? createStarTable( tables[ iNext_++ ], contextUrl )
                     : null;
            }
        };
    }

    /**
     * Determines whether a byte buffer contains what looks like the start
     * of a PDS4 file.  Not bulletproof.
     *
     * @param  intro  first few bytes of file
     * @return  true if the intro looks like the start of a PDS4 file
     */
    public static boolean isMagic( byte[] intro ) {

        /* If it's really short, it's not going to find PDS4-ness,
         * so don't bother. */
        if ( intro.length < 32 ) {
            return false;
        }

        /* See if we can find a namespace or an opening tag that might
         * occur in the first part of the document.  This assumes that
         * the byte stream is UTF-8 encoded (or similar).  That's likely
         * but not necessarily the case. */
        String content;
        try {
            content = new String( intro, "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            assert false;
            content = "";
        }
        for ( String tagName : MAGIC_ELEMENTS ) {
            if ( content.contains( "<" + tagName ) ) {
                return true;
            }
        }
        for ( String ns : MAGIC_NAMESPACES ) {
            if ( content.contains( ns ) ) {
                return true;
            }
        }

        /* Attempt an XML parse of the file, and see if it looks like PDS4.
         * The parse isn't likely to complete, but we can still poll the
         * parser to find out the result. */
        MagicHandler handler = new MagicHandler();
        try {
            SAXParserFactory.newInstance().newSAXParser()
           .parse( new ByteArrayInputStream( intro ), handler );
        }
        catch ( IOException | SAXException | ParserConfigurationException e ) {
        }
        if ( handler.isPds4() ) {
            return true;
        }        

        /* Haven't found anything that looks likely; report that it doesn't
         * look like PDS4.  But some real PDS4 files could slip through
         * the net. */
        return false;
    }

    /**
     * Turns a DataSource into a Label.
     *
     * @param  datsrc  data source
     * @return   label
     */
    private Label parseLabel( DataSource datsrc ) throws IOException {
        if ( ( ! checkMagic_ ) || isMagic( datsrc.getIntro() ) ) {
            LabelParser parser =
                new LabelParser( observationalOnly_, blankSpecials_ );
            if ( datsrc instanceof FileDataSource ) {
                return parser.parseLabel( ((FileDataSource) datsrc).getFile() );
            }
            else {
                return parser.parseLabel( datsrc.getURL() );
            }
        }
        else {
            throw new TableFormatException( "Not a PDS4 label file" );
        }
    }

    /**
     * Turns a PDS4 Table object into a StarTable.
     *
     * @param  table  table object on which this table is based
     * @param  contextUrl   parent URL for the PDS4 label
     * @return  star table
     */
    private StarTable createStarTable( Table table, URL contextUrl )
            throws IOException {
        TableType ttype = table.getTableType();
        if ( table instanceof BaseTable ) {
            return new BasePds4StarTable( (BaseTable) table, contextUrl );
        }
        else if ( table instanceof DelimitedTable ) {
            return new DelimitedPds4StarTable( (DelimitedTable) table,
                                               contextUrl );
        }
        else {
            assert false;
            throw new TableFormatException( "what?" );
        }
    }

    /**
     * Returns a table from a given array that matches the given position
     * string.  If there is no match, an informative TableFormatException
     * must be thrown.
     *
     * @param  tables  non-empty array of candidate tables
     * @param  pos    non-empty, non-null position designator
     * @return  selected table, not null
     * @throws  TableFormatException  if no such table
     */
    private Table getPositionedTable( Table[] tables, String pos )
            throws TableFormatException {
        int nt = tables.length;
        try {
            int jpos = Integer.parseInt( pos );
            if ( jpos > 0 && jpos <= nt ) {
                return tables[ jpos - 1 ];
            }
            else {
                String msg = "Table index " + pos + " out of range"
                           + " 1-" + nt;
                throw new TableFormatException( msg );
            }
        }
        catch ( RuntimeException e ) {
            // Not numeric, OK.
        }
        for ( Table table : tables ) {
            if ( pos.equalsIgnoreCase( table.getName() ) ||
                 pos.equalsIgnoreCase( table.getLocalIdentifier() ) ) {
                return table;
            }
        }
        throw new TableFormatException( "No table in label matching position "
                                      + "\"" + pos + "\"" );
    }

    /**
     * SAX handler that tries to spot signs of a PDS4 file.
     */
    private static class MagicHandler extends DefaultHandler {
        boolean isPds4_;

        /**
         * Returns true if following a parse or part of a parse,
         * some events have occured that seem to confirm
         * a PDS4 file is under inspection.
         *
         * @return  true if the parsed file looks like PDS4
         */
        boolean isPds4() {
            return isPds4_;
        }

        @Override
        public void startElement( String uri, String localName, String qName,
                                  Attributes atts ) {
            if ( MAGIC_ELEMENTS.contains( localName ) ||
                 MAGIC_ELEMENTS.contains( qName ) ) {
                isPds4_ = true;
            }
        }

        @Override
        public void startPrefixMapping( String prefix, String uri ) {
            if ( MAGIC_NAMESPACES.contains( uri ) ) {
                isPds4_ = true;
            }
        }

        @Override
        public void processingInstruction( String target, String data ) {
            // PDS4 files often have <?xml-model> Processing Instructions.
            // I don't know really what goes in there (something to do with
            // schematron), but if it includes the nasa pds4 address
            // it's probably OK.
            if ( data != null && data.indexOf( "pds.nasa.gov/pds4" ) >= 0 ) {
                isPds4_ = true;
            }
        }
    }
}
