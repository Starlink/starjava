package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import org.xml.sax.SAXException;

/**
 * Stage for checking content of TAPRegExt capability metadata.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class CapabilityStage implements Stage, CapabilityHolder {

    private TapCapability tcap_;

    public String getDescription() {
        return "Check content of TAPRegExt capabilities record";
    }

    /**
     * Returns the TAP capability record obtained by the last run of this stage.
     *
     * @return   tap capability object
     */
    public TapCapability getCapability() {
        return tcap_;
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        final TapCapability tcap;
        reporter.report( ReportType.INFO, "CURL",
                         "Reading capability metadata from "
                       + serviceUrl + "/capabilities" );
        try {
            tcap = TapQuery.readTapCapability( serviceUrl );
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.ERROR, "FLSX",
                             "Error parsing capabilities metadata", e );
            return;
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "FLIO",
                             "Error reading capabilities metadata", e );
            return;
        }
        checkCapability( reporter, tcap );
        tcap_ = tcap;
    }

    private void checkCapability( Reporter reporter, TapCapability tcap ) {

        /* Check query languages. */
        String[] languages = tcap.getLanguages();
        if ( languages.length == 0 ) {
            reporter.report( ReportType.ERROR, "NOQL",
                             "No query languages declared" );
        }
        else {
            boolean hasAdql2 = false;
            boolean hasAdql = false;
            for ( int il = 0; il < languages.length; il++ ) {
                String lang = languages[ il ];
                if ( lang.startsWith( "ADQL-" ) ) {
                    hasAdql = true;
                }
                if ( lang.equals( "ADQL-2.0" ) ) {
                    hasAdql2 = true;
                }
            }
            if ( ! hasAdql ) {
                reporter.report( ReportType.ERROR, "ADQX",
                                 "ADQL not declared as a query language" );
            }
            else if ( ! hasAdql2 ) {
                reporter.report( ReportType.WARNING, "AD2X",
                                 "ADQL-2.0 not declared as a query language" );
            }
        }

        /* Check upload methods. */
        String[] upMethods = tcap.getUploadMethods();
        String stdPrefix = TapCapability.UPLOADS_URI;
        Collection<String> mandatorySuffixList =
            Arrays.asList( new String[] { "inline", "http" } ); // TAP 2.5.{1,2}
        Collection<String> stdSuffixList =
            Arrays.asList( new String[] { "inline", "http", "https", "ftp" } );
        for ( int iu = 0; iu < upMethods.length; iu++ ) {
            String upMethod = upMethods[ iu ];
            if ( upMethod.startsWith( stdPrefix ) ) {
                String frag = upMethod.substring( stdPrefix.length() );
                if ( ! stdSuffixList.contains( frag ) ) {
                    reporter.report( ReportType.ERROR, "UPBD",
                                     "Unknown suffix \"" + frag
                                   + "\" for upload method" );
                }
            }
            else {
                reporter.report( ReportType.WARNING, "UPCS",
                                 "Custom upload method \"" + upMethod + "\"" );
            }
        }
        if ( upMethods.length > 0 ) {
            for ( String msuff : mandatorySuffixList ) {
                String mmeth = stdPrefix + msuff;
                if ( ! Arrays.asList( upMethods ).contains( mmeth ) ) {
                    String msg = new StringBuilder()
                       .append( "Mandatory upload method " )
                       .append( mmeth )
                       .append( " not declared" )
                       .append( ", though uploads are apparently supported" )
                       .toString();
                    reporter.report( ReportType.ERROR, "MUPM", msg );
                }
            }
        }
    }
}
