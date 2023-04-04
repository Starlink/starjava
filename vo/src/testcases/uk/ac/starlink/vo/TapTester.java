package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;

public class TapTester extends TestCase {

    public void testExamples() throws IOException, SAXException {
        AdqlExample[] examples = AbstractAdqlExample.createSomeExamples();
        SchemaMeta[] schemas =
            TableSetSaxHandler
           .readTableSet( TapTester.class.getResource( "gavo_tables.xml" ),
                          ContentCoding.NONE );
        List<TableMeta> tableList = new ArrayList<TableMeta>();
        for ( SchemaMeta schema : schemas ) {
            tableList.addAll( Arrays.asList( schema.getTables() ) );
        }
        TableMeta[] tables = tableList.toArray( new TableMeta[ 0 ] );
        TapCapability tcap =
            TapCapabilitiesDoc
           .readCapabilities( TapTester.class
                             .getResource( "gavo_capabilities.xml" ) )
           .getTapCapability();
        showExamples( examples, tables, tcap );
    }

    private void showExamples( AdqlExample[] examples, TableMeta[] tables,
                               TapCapability tcap ) {
        TapLanguage[] langs = tcap.getLanguages();
        TapLanguage tlang = langs != null && langs.length > 0
                          ? langs[ 0 ]
                          : null;
        String[] versions = tlang == null ? null : tlang.getVersions();
        VersionedLanguage vlang = versions == null || versions.length == 0
                                ? null
                                : new VersionedLanguage( tlang, versions[ 0 ] );
        double[] skypos = null;
        for ( int ie = 0; ie < examples.length; ie++ ) {
            AdqlExample example = examples[ ie ];
            String exTxt = example.getAdqlText( true, vlang, tcap, tables,
                                                tables[ 0 ], skypos );
            if ( exTxt != null ) {
                System.out.println( example.getName() );
                System.out.println( exTxt.replaceAll( "(?m)^", "   " ) );
            }
        }
    }
}
