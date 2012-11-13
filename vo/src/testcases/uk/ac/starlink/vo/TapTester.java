package uk.ac.starlink.vo;

import java.io.IOException;
import junit.framework.TestCase;
import org.xml.sax.SAXException;

public class TapTester extends TestCase {

    public void testExamples() throws IOException, SAXException {
        AdqlExample[] examples = AbstractAdqlExample.createSomeExamples();
        TableMeta[] tables =
            TableSetSaxHandler
           .readTableSet( TapTester.class
                         .getResource( "gavo_tables.xml" ) );
        TapCapability tcap =
            TapCapability
           .readTapCapability( TapTester.class
                              .getResource( "gavo_capabilities.xml" ) );
        showExamples( examples, tables, tcap );
    }

    private void showExamples( AdqlExample[] examples, TableMeta[] tables,
                               TapCapability tcap ) {
        TapLanguage[] langs = tcap.getLanguages();
        String lang = langs != null && langs.length > 0
                    ? langs[ 0 ].getName()
                    : null;
        for ( int ie = 0; ie < examples.length; ie++ ) {
            AdqlExample example = examples[ ie ];
            String exTxt =
                example.getText( true, lang, tcap, tables, tables[ 0 ] );
            if ( exTxt != null ) {
                System.out.println( example.getName() );
                System.out.println( exTxt.replaceAll( "(?m)^", "   " ) );
            }
        }
    }
}
