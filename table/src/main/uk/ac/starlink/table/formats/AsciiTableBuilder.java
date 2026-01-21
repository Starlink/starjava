package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DataSource;

/**
 * A table builder which reads tables in simple ASCII format.
 * The detailed format of input file which is understood is documented
 * fully in the {@link AsciiStarTable} class.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AsciiTableBuilder extends RowEvaluatorTableBuilder {

    public AsciiTableBuilder() {
        super( new String[] { "txt" }, StandardCharsets.UTF_8 );
    }

    public String getFormatName() {
        return "ASCII";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws TableFormatException, IOException {
        return new AsciiStarTable( datsrc, getEncoding(), getMaxSample(),
                                   getDecoders() );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "AsciiTableBuilder.xml" );
    }
}
