package uk.ac.starlink.ecsv;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.DataSource;

/**
 * TableBuilder implementation for ECSV tables.
 * The format currently supported is ECSV 0.9, as documented at
 * <a href="https://github.com/astropy/astropy-APEs/blob/master/APE6.rst"
 *    >Astropy APE6</a>.
 *
 * @author   Mark Taylor
 * @since    29 Apr 2020
 */
public class EcsvTableBuilder extends DocumentedTableBuilder {

    private final YamlParser yamlParser_;

    /**
     * Constructor.
     */
    public EcsvTableBuilder() {
        super( new String[] { "ecsv" } );
        yamlParser_ = new SnakeYamlParser();
    }

    public String getFormatName() {
        return "ECSV";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        try ( EcsvReader reader = createEcsvReader( in ) ) {
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
        if ( ! EcsvHeader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "No ECSV header" );
        }
        EcsvMeta meta;
        try ( EcsvReader reader = createEcsvReader( datsrc.getInputStream()) ) {
            meta = reader.getMeta();
        }
        return new EcsvStarTable( meta ) {
            public RowSequence getRowSequence() throws IOException {
                final EcsvReader rdr =
                    createEcsvReader( datsrc.getInputStream() );
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
     * Creates an EcsvReader given an input stream.
     *
     * @param   in   input stream
     * @return   reader
     */
    private EcsvReader createEcsvReader( InputStream in )
            throws IOException {
        try {
            return new EcsvReader( in, yamlParser_ );
        }
        catch ( EcsvFormatException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }
}
