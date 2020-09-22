package uk.ac.starlink.mirage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.formats.DocumentedStreamStarTableWriter;

public class MirageTableWriter extends DocumentedStreamStarTableWriter {

    public MirageTableWriter() {
        super( new String[] { "mirage" } );
    }

    public void writeStarTable( StarTable startab, OutputStream out )
            throws IOException {
        PrintStream pstrm = out instanceof PrintStream 
                          ? (PrintStream) out
                          : new PrintStream( out );
        MirageFormatter mf = new MirageFormatter( pstrm );
        mf.writeMirageFormat( startab );
        pstrm.flush();
    }

    public String getFormatName() {
        return "mirage";
    }

    public String getMimeType() {
        return "text/plain";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>Mirage was a nice standalone tool for analysis of",
            "multidimensional data, from which TOPCAT took some inspiration.",
            "It was described in a 2007 paper",
            "<a href='https://ui.adsabs.harvard.edu/abs/2007ASPC..371..391H/'"
              + ">2007ASPC..371..391H</a>,",
            "but no significant development seems to have taken place",
            "since then.",
            "This format is therefore probably obsolete, but you can still",
            "write table output in Mirage-compatible format",
            "if you like.",
            "</p>",
        "" );
    }
}
