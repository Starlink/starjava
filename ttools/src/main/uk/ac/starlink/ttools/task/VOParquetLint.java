package uk.ac.starlink.ttools.task;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.parquet.ParquetIO;
import uk.ac.starlink.parquet.ParquetStarTable;
import uk.ac.starlink.parquet.ParquetUtil;
import uk.ac.starlink.parquet.ParquetTableBuilder;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.func.Times;
import uk.ac.starlink.ttools.votlint.SaxMessager;
import uk.ac.starlink.ttools.votlint.VersionDetector;
import uk.ac.starlink.ttools.votlint.VotLintCode;
import uk.ac.starlink.ttools.votlint.VotLintContext;
import uk.ac.starlink.ttools.votlint.VotLinter;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.Timesys;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableDOMBuilder;
import uk.ac.starlink.votable.VOTableVersion;

/**
 * Checks compliance of a Parquet file with the VOParquet convention.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2024
 */
public class VOParquetLint implements Task {

    private final Parameter<String> locParam_;
    private final Parameter<String> reportParam_;
    private final BooleanParameter ucdParam_;
    private final BooleanParameter unitParam_;
    private final BooleanParameter timeParam_;
    private final StringParameter votableParam_;
    private final BooleanParameter voparquetParam_;
    private final Parameter<?>[] params_;

    private static final VOTableVersion DFLT_VOTABLE_VERSION =
        VOTableVersion.V15;
    private static final double UNIX_EPOCH_AS_MJD =
        Times.mjdToJd( Times.unixMillisToMjd( 0 ) );

    /**
     * Constructor.
     */
    public VOParquetLint() {
        List<Parameter<?>> paramList = new ArrayList<>();

        locParam_ = new StringParameter( "in" );
        locParam_.setPosition( 1 );
        locParam_.setPrompt( "Location of parquet file" );
        locParam_.setUsage( "<filename>" );
        locParam_.setDescription( new String[] {
            "<p>Name of the parquet file to check.",
            "</p>",
        } );
        paramList.add( locParam_ );

        voparquetParam_ = new BooleanParameter( "voparquet" );
        voparquetParam_.setPrompt( "Dataless VOTable required?" );
        voparquetParam_.setBooleanDefault( false );
        voparquetParam_.setDescription( new String[] {
            "<p>Configures whether a data-less VOTable is required",
            "in the parquet file or not.",
            "If this parameter is true, absence of any metadata VOTable",
            "will generate an Error report.",
            "Otherwise, it will merely result in an Info report.",
            "</p>",
        } );
        paramList.add( voparquetParam_ );

        reportParam_ = new StringParameter( "report" );
        reportParam_.setPrompt( "Message types to report" );
        reportParam_.setUsage( "[EWI]+" );
        reportParam_.setDescription( new String[] {
            "<p>Letters indicating which message types should be output.",
            "Each character of the string is one of the letters",
            "<code>E</code> (for Error),",
            "<code>W</code> (for Warning) and",
            "<code>I</code> (for Info).",
            "So to suppress Info messages",
            "set the value to \"<code>EW</code>\".",
            "</p>",
        } );
        reportParam_.setStringDefault( "EWI" );
        paramList.add( reportParam_ );

        ucdParam_ = new BooleanParameter( "ucd" );
        ucdParam_.setBooleanDefault( true );
        ucdParam_.setPrompt( "Check ucd attributes for UCD1+ syntax?" );
        ucdParam_.setDescription( new String[] {
            "<p>If true, the <code>ucd</code> attributes",
            "on <code>FIELD</code> and <code>PARAM</code> elements etc",
            "in the VOTable metadata table",
            "are checked for conformance against the UCD1+ standard",
            "or a list of known UCD1 terms.",
            "</p>",
        } );
        paramList.add( ucdParam_ );

        unitParam_ = new BooleanParameter( "unit" );
        unitParam_.setNullPermitted( true );
        unitParam_.setPrompt( "Check unit attributes for VOUnit syntax?" );
        unitParam_.setDescription( new String[] {
            "<p>If true, the <code>unit</code> attributes",
            "on <code>FIELD</code> and <code>PARAM</code> elements",
            "are checked for conformance against the VOUnits standard;",
            "if false, no such checks are made.",
            "</p>",
            "<p>The VOTable standard version 1.4 and later",
            "recommends use of VOUnits",
            "(there are some inconsistencies in the text on this topic",
            "in VOTable 1.4, but these are cleared up in V1.5).",
            "Earlier VOTable versions refer to a different (CDS) unit syntax,",
            "which is not checked by this tool.",
            "So by default unit syntax is checked when the VOTable is 1.4",
            "or greater, and not for earlier versions,",
            "but that can be overridden by giving a <code>true</code>",
            "or <code>false</code> value for this parameter.",
            "</p>",
            "<p>The wording of the VOTable and VOUnit standards",
            "do not strictly require use of VOUnit syntax even at VOTable 1.4,",
            "so failed checks result in Warning rather than Error reports.",
            "</p>",
        } );
        paramList.add( unitParam_ );

        timeParam_ = new BooleanParameter( "time" );
        timeParam_.setBooleanDefault( true );
        timeParam_.setPrompt( "Check TIMESTAMP/DATE columns" );
        timeParam_.setDescription( new String[] {
            "<p>If true, then parquet columns with the logical types",
            "<code>TIMESTAMP</code> or <code>DATE</code>",
            "will be checked against their VOTable counterparts",
            "for suitable metadata.",
            "Since parquet TIMESTAMP and DATE columns have an associated unit,",
            "a Warning is reported when the corresponding VOTable FIELD",
            "does not declare the same unit attribute.",
            "Since parquet TIMESTAMP and DATE columns also have an implicit",
            "zero point",
            "(the Unix epoch 1970-01-01, equivalent to JD 2440587.5),",
            "a Warning is also reported if no compatible TIMESYS element",
            "is referenced by the corresponding VOTable FIELD.",
            "If this parameter is set false, no such reports are made.",
            "</p>",
        } );
        paramList.add( timeParam_ );

        votableParam_ = new StringParameter( "votable" );
        votableParam_.setNullPermitted( true );
        votableParam_.setPrompt( "Location of data-less VOTable" );
        votableParam_.setUsage( "<filename-or-url>" );
        votableParam_.setDescription( new String[] {
            "<p>This parameter can be used to specify the location",
            "(filename or URL) of a data-less VOTable document",
            "that describes the parquet file under evaluation.",
            "Normally this is not necessary, since the VOTable is found in a",
            "well-known location in the metadata of the parquet file itself,",
            "as specified by the VOParquet convention.",
            "However if this parameter is set to a non-blank value then",
            "the internal VOTable, if any, will be ignored,",
            "and the UTF-8-encoded VOTable at the supplied location",
            "will be used instead.",
            "This can be useful when debugging a VOParquet file.",
            "</p>",
        } );
        paramList.add( votableParam_ );

        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    public String getPurpose() {
        return "Checks parquet file compliance with VOParquet convention";
    }

    public Parameter<?>[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        String loc = locParam_.stringValue( env );
        boolean requireVoparquet = voparquetParam_.booleanValue( env );
        boolean tryUrl = false;
        ParquetTableBuilder builder = new ParquetTableBuilder();
        String rtypes = reportParam_.stringValue( env );
        boolean[] ewiFlags = new boolean[ 3 ];
        for ( int ic = 0; ic < rtypes.length(); ic++ ) {
            switch ( rtypes.charAt( ic ) ) {
                case 'E': case 'e':
                    ewiFlags[ 0 ] = true;
                    break;
                case 'W': case 'w':
                    ewiFlags[ 1 ] = true;
                    break;
                case 'I': case 'i':
                    ewiFlags[ 2 ] = true;
                    break;
                default:
                    throw new ParameterValueException( reportParam_,
                                                       "Not of form [EWI]+" );
            }
        }
        Reporter reporter = new Reporter( env.getOutputStream(), ewiFlags );
        String votableLoc = votableParam_.stringValue( env );
        boolean checkUcd = ucdParam_.booleanValue( env );
        Boolean checkUnit = unitParam_.objectValue( env );
        boolean checkTime = timeParam_.booleanValue( env );
        LintConfig lintConfig = new LintConfig() {
            public boolean isCheckUcd() {
                return checkUcd;
            }
            public Boolean isCheckUnit() {
                return checkUnit;
            }
            public boolean isCheckTime() {
                return checkTime;
            }
        };
        return () -> {
            ParquetIO parquetIo = ParquetUtil.getIO();
            DataSource datsrc = DataSource.makeDataSource( loc );
            boolean useCache = false;
            ParquetStarTable.Config config = new ParquetStarTable.Config() {
                public boolean includeUnsupportedColumns() {
                    return true;
                }
            };
            ParquetStarTable parquetTable =
                parquetIo.readParquet( datsrc, builder, config,
                                       useCache, tryUrl );
            if ( votableLoc != null ) {
                String votableTxt =
                    ParquetTableBuilder.readUtf8FromLocation( votableLoc );
                parquetTable.setVOTableMetadataText( votableTxt );
            }
            checkVOParquetKeys( parquetTable, reporter );
            String votableTxt = parquetTable.getVOTableMetadataText();
            if ( votableTxt == null ) {
                String msg =
                    "No VOTable document found attached to parquet file";
                if ( requireVoparquet ) {
                    reporter.error( msg );
                }
                else {
                    reporter.info( msg );
                }
            }
            else {
                Bi<VODocument,VOTableVersion> parseResult =
                    validatingParseVOTable( votableTxt, lintConfig, reporter );
                VODocument vodoc = parseResult.getItem1();
                VOTableVersion votVersion = parseResult.getItem2();
                TableElement tableEl =
                    getVOParquetTableElement( vodoc, reporter );
                if ( tableEl == null ) {
                    reporter.error( "No DATA-less TABLE element found"
                                  + " in attached VOTable document" );
                }
                else {
                    VOStarTable votable;
                    try {
                        votable = new VOStarTable( tableEl );
                    }
                    catch ( IOException e ) {
                        reporter.error( "Failed to interpret TABLE element: "
                                      + e );
                        votable = null;
                    }
                    if ( votable != null ) {
                        compareMetadata( parquetTable, votable, votVersion,
                                         lintConfig, reporter );
                    }
                }
            }
        };
    }

    /**
     * Examines the key-value metadata for entries that look like they
     * have to do with the VOParquet convention, and report on the results.
     *
     * @param  ptable  parquet table
     * @param  reporter   message destination
     */
    private void checkVOParquetKeys( ParquetStarTable ptable,
                                     Reporter reporter ) {
        String votmetaTxt = null;
        String votmetaVersion = null;
        for ( Map.Entry<String,String> entry :
              ptable.getExtraMetadataMap().entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ( ParquetStarTable.VOTMETA_KEY.equals( key ) ) {
                votmetaTxt = value;
            }
            else if ( ParquetStarTable.VOTMETAVERSION_KEY.equals( key ) ) {
                votmetaVersion = value;
            }
            else if ( key.toLowerCase()
                         .startsWith( ParquetStarTable.VOTMETA_NAMESPACE
                                                      .toLowerCase() ) ) {
                reporter.warning( "Unknown key " + key + " found in or near "
                                + "VOParquet namespace"
                                + " (not " + ParquetStarTable.VOTMETA_KEY
                                + " or " + ParquetStarTable.VOTMETAVERSION_KEY
                                + ")" );
            }
        }
        if ( votmetaTxt != null ) {
            if ( votmetaVersion == null ) {
                reporter.error( ParquetStarTable.VOTMETA_KEY
                              + " present but "
                              + ParquetStarTable.VOTMETAVERSION_KEY
                              + " missing" );
            }
            else if ( ! ParquetStarTable.REQUIRED_VOTMETAVERSION
                                        .equals( votmetaVersion ) ) {
                reporter.warning( ParquetStarTable.VOTMETA_KEY
                                + " present but "
                                + ParquetStarTable.VOTMETAVERSION_KEY
                                + " has unknown value "
                                + votmetaVersion
                                + " (not "
                                + ParquetStarTable.REQUIRED_VOTMETAVERSION
                                + ")" );
            }
        }
        else {
            if ( ParquetStarTable.REQUIRED_VOTMETAVERSION
                                 .equals( votmetaVersion ) ) {
                reporter.error( ParquetStarTable.VOTMETA_KEY + " = "
                              + votmetaVersion + " but "
                              + ParquetStarTable.VOTMETA_KEY + " is missing" );
            }
            else if ( votmetaVersion != null ) {
                reporter.warning( ParquetStarTable.VOTMETAVERSION_KEY
                                + " has unknown value " + votmetaVersion + ", "
                                + ParquetStarTable.VOTMETA_KEY
                                + " not present" );
            }
        }
    }

    /**
     * Parses a VOTable in a supplied string with validation.
     * Since this is intended for a metadata-only VOTable,
     * the assumption is that it's not enormous.
     *
     * @param  votableTxt  string containing the VOTable XML
     * @param  config    validation configuration options
     * @param  reporter   message destination
     * @return  pair of VOTable document and parse version;
     *          document may be null in case of error
     */
    private Bi<VODocument,VOTableVersion>
            validatingParseVOTable( String votableTxt, LintConfig config,
                                    Reporter reporter ) {
        SaxMessager messager = new SaxMessager() {
            public void reportMessage( SaxMessager.Level level,
                                       VotLintCode code, String msg,
                                       Locator locator ) {
                String txt = "[" + code + "] " + msg;
                switch ( level ) {
                    case INFO:
                        reporter.info( txt );
                        break;
                    case WARNING:
                        reporter.warning( txt );
                        break;
                    case ERROR:
                        reporter.error( txt );
                        break;
                    default:
                        assert false;
                        reporter.error( txt );
                }
            }
        };
        String versionString;
        try {
            versionString =
                VersionDetector
               .getVersionString(
                    new BufferedInputStream(
                        new ByteArrayInputStream(
                            votableTxt.getBytes( StandardCharsets.UTF_8 ) ) ) );
        }
        catch ( IOException e ) {
            reporter.error( e.toString() );
            versionString = null;
        }
        VOTableVersion version =
            VOTableVersion.getKnownVersions().get( versionString );
        if ( version == null ) {
            if ( versionString == null ) {
                reporter.warning( "VOTable version undeclared: "
                                + "use default value " + version );
            }
            version = DFLT_VOTABLE_VERSION;
        }
        boolean validate = true;
        VotLintContext vlContext =
            new VotLintContext( version, validate, messager );
        vlContext.setCheckUcd( config.isCheckUcd() );
        Boolean checkUnit = config.isCheckUnit();
        vlContext.setCheckUnit( checkUnit == null ? version.isVOUnitSyntax()
                                                  : checkUnit.booleanValue() );
        VOTableDOMBuilder domHandler =
            new VOTableDOMBuilder( StoragePolicy.PREFER_MEMORY, true );
        try {
            XMLReader parser =
                new VotLinter( vlContext ).createParser( domHandler );
            parser.parse( new InputSource( new StringReader( votableTxt ) ) );
        }
        catch ( IOException | SAXException e ) {
            reporter.error( "VOTable parsing failed: " + e );
        }
        return new Bi<VODocument,VOTableVersion>( domHandler.getDocument(),
                                                  version );
    }

    /**
     * Extracts from a supplied VOTable document the TABLE element
     * that's supposed to contain the table metadata.
     *
     * @param  vodoc  DOM
     * @param  reporter  message destination
     * @return   metadata TABLE element or null if it can't be found
     */
    private TableElement getVOParquetTableElement( VODocument vodoc,
                                                   Reporter reporter ) {
        VOElement topel = (VOElement) vodoc.getDocumentElement();
        NodeList tlist = topel.getElementsByVOTagName( "TABLE" );
        if ( tlist.getLength() == 0 ) {
            reporter.error( "No TABLE elements in VOTable" );
            return null;
        }
        TableElement tableEl = (TableElement) tlist.item( 0 );
        if ( tableEl.getChildByName( "DATA" ) != null ) {
            reporter.error( "First TABLE element has a DATA child"
                          + "; use it anyway" );
        }
        return tableEl;
    }

    /**
     * Compares the column metadata from a parquet data table and a
     * VOParquet data-less VOTable, reporting on the results.
     *
     * @param  pTable  parquet table
     * @param  vTable  VOTable
     * @param  votVersion  VOTable version of VOTable
     * @param  config    linter configuration
     * @param  reporter  message destination
     */
    private void compareMetadata( ParquetStarTable pTable, VOStarTable vTable,
                                  VOTableVersion votVersion, LintConfig config,
                                  Reporter reporter ) {
        int nc = pTable.getColumnCount();
        if ( vTable.getColumnCount() != nc ) {
            reporter.error( "Column count mismatch: "
                          + "parquet has " + pTable.getColumnCount() + ", "
                          + "VOTable has " + vTable.getColumnCount() );
            return;
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            String ic1 = Integer.toString( ic + 1 );
            ColumnInfo pInfo = pTable.getColumnInfo( ic );
            ColumnInfo vInfo = vTable.getColumnInfo( ic );
            ColumnDescriptor pDescriptor =
                pTable.getInputColumn( ic ).getColumnDescriptor();
            PrimitiveType ptype = pDescriptor.getPrimitiveType();
            LogicalTypeAnnotation ltype = ptype.getLogicalTypeAnnotation();
            String pName = pInfo.getName();
            String vName = vInfo.getName();

            /* Report on column name mismatches. */
            if ( ! pName.equals( vName ) ) {
                reporter.warning( "Column name mismatch at column " + ic1 + ": "
                                + "parquet is \"" + pName + "\", "
                                + "VOTable is \"" + vName + "\"" );
            }

            /* Report on parquet types unsupported in VOTable. */
            if ( Boolean.TRUE
                .equals( pInfo
                        .getAuxDatumValue( ParquetStarTable.UNSUPPORTED_INFO,
                                           Boolean.class ) ) ) {
                String msg = new StringBuffer()
                   .append( "Parquet column #" )
                   .append( ic1 )
                   .append( " (" )
                   .append( ptype == null ? "?" : ptype.toString() )
                   .append( ") " )
                   .append( "not supported by STIL" )
                   .toString();
                reporter.warning( msg );
            }

            /* Report on apparent data type mismatches. */
            else {
                String colTxt = "Column " + pName + " (#" + ic1 + "): ";
                Class<?> pClazz = pInfo.getContentClass();
                Class<?> vClazz = vInfo.getContentClass();
                if ( ! pClazz.equals( vClazz ) ) {
                    boolean isUnsignedPromotion =
                           ( pClazz.equals( Byte.class ) &&
                             vClazz.equals( Short.class ) )
                        || ( pClazz.equals( Short.class ) &&
                             vClazz.equals( Integer.class ) )
                        || ( pClazz.equals( Integer.class ) &&
                             vClazz.equals( Long.class ) )
                        || ( pClazz.equals( byte[].class ) &&
                             vClazz.equals( short[].class ) )
                        || ( pClazz.equals( short[].class ) &&
                             vClazz.equals( int[].class ) )
                        || ( pClazz.equals( int[].class ) &&
                             vClazz.equals( long[].class ) );
                    String msg = new StringBuffer()
                       .append( colTxt )
                       .append( "parquet/VOTable type mismatch, " )
                       .append( pClazz.getSimpleName() )
                       .append( " != " )
                       .append( vClazz.getSimpleName() )
                       .toString();
                    if ( isUnsignedPromotion ) {
                        reporter.info( msg
                                     + " - probably something to do with"
                                     + " signed/unsigned integers" );
                    }
                    else {
                        reporter.warning( msg );
                    }
                }

                /* Report on TIMESTAMP and DATE columns, which in VOTable
                 * should have particular unit attributes and ideally
                 * associated TIMESYS elements. */
                if ( config.isCheckTime() &&
                     ( ltype instanceof LogicalTypeAnnotation
                                       .TimestampLogicalTypeAnnotation ||
                       ltype instanceof LogicalTypeAnnotation
                                       .DateLogicalTypeAnnotation ) ) {
                    String tunit;
                    if ( ltype instanceof LogicalTypeAnnotation
                                         .DateLogicalTypeAnnotation ) {
                        tunit = "d";
                    }
                    else if ( ltype instanceof LogicalTypeAnnotation
                                              .TimestampLogicalTypeAnnotation ){
                        switch ( ((LogicalTypeAnnotation
                                  .TimestampLogicalTypeAnnotation) ltype)
                                 .getUnit() ) {
                            case MILLIS:
                               tunit = "ms";
                               break;
                            case MICROS:
                               tunit = "us";
                               break;
                            case NANOS:
                               tunit = "ns";
                               break;
                            default:
                               assert false;
                               tunit = null; 
                        }
                    }
                    else {
                        assert false;
                        tunit = null;
                    }
                    String vunit = vInfo.getUnitString();
                    String voriginTxt =
                        Tables
                       .getAuxDatumValue( vInfo,
                                          VOStarTable.TIMESYS_TIMEORIGIN_INFO,
                                          String.class );
                    double vorigin = Timesys.decodeTimeorigin( voriginTxt );
                    double torigin = UNIX_EPOCH_AS_MJD;
                    if ( tunit != null ) {
                        if ( vunit == null ) {
                            String msg = new StringBuffer()
                               .append( colTxt )
                               .append( "missing VOTable units for " )
                               .append( ltype )
                               .append( "; suggest " )
                               .append( "unit='" )
                               .append( tunit )
                               .append( "'" )
                               .toString();
                            reporter.warning( msg );
                        }
                        else if ( ! vunit.equals( tunit ) ) {
                            String msg = new StringBuffer()
                               .append( colTxt )
                               .append( "unit mismatch, " )
                               .append( "parquet " )
                               .append( ltype )
                               .append( ", votable unit='" )
                               .append( vunit )
                               .append( "', should be '" )
                               .append( tunit )
                               .append( "'" )
                               .toString();
                            reporter.error( msg );
                        }
                    }
                    if ( ! Double.isNaN( torigin ) ) {
                        if ( Double.isNaN( vorigin ) &&
                             votVersion.allowTimesys() ) {
                            String msg = new StringBuffer()
                               .append( colTxt )
                               .append( "missing TIMESYS for " )
                               .append( ltype )
                               .append( "; suggest " )
                               .append( "<TIMESYS timeorigin='" )
                               .append( torigin )
                               .append( "' timescale='UTC' .../>" )
                               .toString();
                            reporter.warning( msg );
                        }
                        else if ( vorigin != torigin ) {
                            String msg = new StringBuffer()
                               .append( colTxt )
                               .append( "time origin mismatch, " )
                               .append( "parquet " )
                               .append( ltype )
                               .append( ", votable TIMESYS timeorigin=" )
                               .append( vorigin )
                               .append( ", should be Unix epoch in JD = " )
                               .append( torigin )
                               .toString();
                            reporter.error( msg );
                        }
                    }
                }
            }
        }
    }

    /**
     * Abstraction for reporting information to the user.
     */
    private static class Reporter {
        private final PrintStream out_;
        private final boolean hasError_;
        private final boolean hasWarning_;
        private final boolean hasInfo_;

        /**
         * Constructor.
         *
         * @param   out  destination stream
         */
        Reporter( PrintStream out, boolean[] ewiFlags ) {
            out_ = out;
            hasError_ = ewiFlags[ 0 ];
            hasWarning_ = ewiFlags[ 1 ];
            hasInfo_ = ewiFlags[ 2 ];
        }

        /**
         * Report information text.
         *
         * @param  txt  message
         */
        public void info( String txt ) {
            if ( hasInfo_ ) {
                report( "INFO", txt );
            }
        }

        /**
         * Report warning text.
         *
         * @param  txt  message
         */
        public void warning( String txt ) {
            if ( hasWarning_ ) {
                report( "WARNING", txt );
            }
        }

        /**
         * Report error text.
         *
         * @param  txt  message
         */
        public void error( String txt ) {
            if ( hasError_ ) {
                report( "ERROR", txt );
            }
        }

        /**
         * Output a message.
         *
         * @param  level  level indicator
         * @param  txt    message
         */
        private void report( String level, String txt ) {
            out_.println( ( level == null ? "" : ( level + ": " ) ) + txt );
        }
    }

    /**
     * Aggregates options for VOTable validation.
     */
    private static interface LintConfig {

        /**
         * Whether to validate UCD attributes.
         *
         * @param  true to validate, false to ignore
         */
        boolean isCheckUcd();

        /**
         * Whether to validate unit attributes.
         *
         * @param TRUE to validate, FALSE to ignore,
         *        null to use (VOTable version-dependent) default behaviour
         */
        Boolean isCheckUnit();

        /**
         * Whether to validate TIMESTAMP/DATE metadata.
         *
         * @param  true to validate, false to ignore
         */
        boolean isCheckTime();
    }
}
