package uk.ac.starlink.ecsv;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.URLUtils;

/**
 * TableBuilder implementation for ECSV tables.
 * The format currently supported is ECSV 1.0, as documented at
 * <a href="https://github.com/astropy/astropy-APEs/blob/master/APE6.rst"
 *    >Astropy APE6</a>
 * (V1.0 <a href="https://doi.org/10.5281/zenodo.4792325"
 *                           >DOI:10.5281/zenodo.4792325</a>).
 *
 * @author   Mark Taylor
 * @since    29 Apr 2020
 */
public class EcsvTableBuilder extends DocumentedTableBuilder {

    private final YamlParser yamlParser_;
    private String headerLoc_;
    private Charset encoding_;
    private MessagePolicy colCheck_;
    private byte[] headerBuf_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ecsv" );

    /**
     * Constructor.
     */
    public EcsvTableBuilder() {
        super( new String[] { "ecsv" } );
        yamlParser_ = new SnakeYamlParser();
        encoding_ = StandardCharsets.US_ASCII;
        colCheck_ = MessagePolicy.WARN;
    }

    public String getFormatName() {
        return "ECSV";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Sets the location of a header file which will be prepended to
     * any file read by this handler.  It can be set to a file containing
     * an ECSV header, so that this handler can read plain CSV files
     * but include prepared YAML metadata.
     *
     * @param  headerLoc  filename or URL pointing to a header file
     */
    @ConfigMethod(
        property = "header",
        doc = "<p>Location of a file containing a header to be applied\n"
            + "to the start of the input file.\n"
            + "By using this you can apply your own ECSV-format metadata\n"
            + "to plain CSV files.\n"
            + "</p>",
        usage = "<filename-or-url>",
        example = "http://cdn.gea.esac.esa.int/Gaia/gedr3/ECSV_headers/"
                + "gaia_source.header",
        sequence = 1
    )
    public void setHeader( String headerLoc ) {
        headerLoc_ = headerLoc;
    }

    /**
     * Returns the location of a header file which will be prepended
     * to any file read by this handler.
     *
     * @return  header file location
     */
    public String getHeader() {
        return headerLoc_;
    }

    /**
     * Sets the character encoding for the input stream.
     * ECSV 1.0 by the book supports only ASCII, so that's the default,
     * but this allows you to choose a different encoding such as UTF-8,
     * which works without problems.
     *
     * @param  encoding  character encoding
     */
    @ConfigMethod(
        property = "encoding",
        usage = "ASCII|UTF-8|UTF-16|...",
        example = "UTF-8",
        doc = "<p>Specifies the character encoding used to interpret "
            + "the input file.\n"
            + "ECSV 1.0 is defined to use ASCII-encoded header and data "
            + "sections,\n"
            + "so setting this encoding to a non-ASCII value such as UTF-8\n"
            + "is in violation of the ECSV 1.0 standard.\n"
            + "However, if the header or data in your ECSV-like file\n"
            + "contains non-ASCII characters,\n"
            + "then setting this configuration option will allow them\n"
            + "to be read as intended.\n"
            + "</p>",
        sequence = 2
    )
    public void setEncoding( Charset encoding ) {
        encoding_ = encoding;
    }

    /**
     * Returns the character encoding for the input stream.
     *
     * @return  character encoding
     */
    public Charset getEncoding() {
        return encoding_;
    }

    /**
     * Sets the column checking message policy.
     *
     * @param  colCheck  determines action if YAML header columns don't match
     *                   CSV header
     */
    @ConfigMethod(
        property = "colcheck",
        doc = "<p>Determines the action taken if the columns named\n"
            + "in the YAML header differ from the columns named in the\n"
            + "first line of the CSV part of the file.\n"
            + "</p>",
        example = "FAIL",
        sequence = 3
    )
    public void setColcheck( MessagePolicy colCheck ) {
        colCheck_ = colCheck;
    }

    /**
     * Returns the column checking message policy.
     *
     * @return  determines action if YAML header columns don't match
     *          CSV header
     */
    public MessagePolicy getColcheck() {
        return colCheck_;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        try ( EcsvReader reader = createEcsvReader( in, colCheck_ ) ) {
            EcsvStarTable stMeta = new EcsvStarTable( reader.getMeta() ) {
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
            };
            sink.acceptMetadata( stMeta );
            while ( reader.next() ) {
                sink.acceptRow( reader.getRow() );
            }
            sink.endRows();
        }
        catch ( EcsvFormatException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( headerLoc_ == null && ! EcsvHeader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "No ECSV header" );
        }
        EcsvMeta meta;
        try ( EcsvReader reader =
                  createEcsvReader( datsrc.getInputStream(), colCheck_ ) ) {
            meta = reader.getMeta();
        }
        EcsvColumn<?>[] ecols = meta.getColumns();
        for ( int ic = 0; ic < ecols.length; ic++ ) {
            EcsvColumn<?> ecol = ecols[ ic ];
            String msg = ecol.getDecoder().getWarning();
            if ( msg != null ) {
                logger_.warning( "Column " + ecol.getName()
                               + " (#" + ( ic + 1 ) + "): " + msg );
            }
        }
        return new EcsvStarTable( meta ) {
            public RowSequence getRowSequence() throws IOException {
                final EcsvReader rdr =
                    createEcsvReader( datsrc.getInputStream(),
                                      MessagePolicy.IGNORE );
                return new RowSequence() {
                    public boolean next() throws IOException {
                        try {
                            return rdr.next();
                        }
                        catch ( EcsvFormatException e ) {
                            throw new TableFormatException( e.getMessage(), e );
                        }
                    }
                    public Object[] getRow() {
                        return rdr.getRow();
                    }
                    public Object getCell( int icol ) {
                        return rdr.getCell( icol );
                    }
                    public void close() throws IOException {
                        rdr.close();
                    }
                };
            }
        };
    }

    public String getXmlDescription() {
        return readText( "EcsvTableBuilder.xml" );
    }

    public boolean canStream() {
        return true;
    }

    public boolean docIncludesExample() {
        return false;
    }

    /**
     * Returns an input stream which is based on the supplied stream,
     * but with the content of this handler's header file, if any,
     * inserted at the start.
     *
     * @param  in   base input stream
     * @return   input stream with possible adjustment
     */
    private InputStream applyHeader( InputStream in ) throws IOException {
        return headerLoc_ == null
             ? in
             : new SequenceInputStream(
                       new ByteArrayInputStream( getHeaderBytes() ),
                                                 in );
    }

    /**
     * Returns the content of this handler's header file.
     *
     * @return   lazily read header file content as a byte array
     */
    private byte[] getHeaderBytes() throws IOException {
        if ( headerLoc_ == null ) {
            return new byte[ 0 ];
        }
        else if ( headerBuf_ == null ) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy( getInputStream( headerLoc_ ), bout );
            headerBuf_ = bout.toByteArray();
        }
        return headerBuf_;
    }

    /**
     * Returns an input stream specified by a string location.
     *
     * @param   location  filename or URL
     * @return   input stream
     * @throws  IOException  if no such file
     */
    private static InputStream getInputStream( String location )
            throws IOException {
        File file = new File( location );
        if ( file.exists() ) {
            return new FileInputStream( file );
        }
        else {
            URL url;
            try {
                url = URLUtils.newURL( location );
            }
            catch ( MalformedURLException e ) {
                String msg = "No file or URL \"" + location + "\"";
                throw new FileNotFoundException( msg );
            }
            return url.openStream();
        }
    }    

    /**
     * Creates an EcsvReader given an input stream.
     *
     * @param   in   input stream
     * @param   colCheck  behviour for YAML/CSV column name mismatch
     * @return   reader
     */
    private EcsvReader createEcsvReader( InputStream in,
                                         MessagePolicy colCheck )
            throws IOException {
        LineReader lineRdr =
            LineReader.createCharsetLineReader( applyHeader( in ), encoding_ );
        try {
            return new EcsvReader( lineRdr, yamlParser_, colCheck );
        }
        catch ( EcsvFormatException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }
}
