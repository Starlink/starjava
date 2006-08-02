package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * A table builder which reads tables in Tab-Separated Table format.
 * This is used by GAIA/SkyCat amongst other software.
 * Documentation of the format can be found in Starlink System Note 75
 * (<a href="http://www.starlink.rl.ac.uk/star/docs/ssn75.htx">SSN/75</a>).
 *
 * @author   Mark Taylor
 * @since    1 Aug 2006
 */
public class TstTableBuilder implements TableBuilder {

    public String getFormatName() {
        return "TST";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws TableFormatException, IOException {
        return new TstStarTable( datsrc );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream TST format tables" );
    }
}
