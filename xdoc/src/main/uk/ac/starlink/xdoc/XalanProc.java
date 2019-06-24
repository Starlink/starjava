package uk.ac.starlink.xdoc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;


/**
 * Performs XSLT transformation using Java's javax.xml.transform package.
 * At Sun's J2SE1.4 this is XAlan.
 *
 * @author   Mark Taylor (Starlink)
 */
public class XalanProc {
    public static void main( String[] args ) throws TransformerException {
        String usage = "Usage: XalanProc [-param name value ...] "
                     + "stylesheet [doc]";

        /* Process flags. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        Map<String,String> params = new HashMap<String,String>();
        while ( argList.size() > 0 && argList.get( 0 ).startsWith( "-" ) ) {
            String flag = argList.remove( 0 );
            if ( flag.endsWith( "-param" ) && argList.size() >= 2 ) {
                params.put( argList.remove( 0 ), argList.remove( 0 ) );
            }
            else {
                System.err.println( usage );
                System.exit( 1 ); 
            }
        }

        /* Check arguments. */
        if ( argList.size() < 1 || argList.size() > 2 ) {
            System.err.println( usage );
            System.exit( 1 ); 
        }

        /* Get stylesheet source. */
        File stylesheet = new File( argList.get( 0 ) );
        if ( ! stylesheet.isFile() ) {
            System.err.println( "No stylesheet " + stylesheet );
            System.exit( 1 );
        }
        Source styleSrc = new StreamSource( stylesheet );
        
        /* Get document source. */
        Source docSrc;
        if ( args.length > 1 ) {
            docSrc = new StreamSource( argList.get( 1 ) );
        }
        else {
            docSrc = new StreamSource( System.in );
        }

        /* Prepare the transformer. */
        Transformer trans = TransformerFactory
                           .newInstance()
                           .newTransformer( styleSrc );
        for ( Map.Entry<String,String> entry : params.entrySet() ) {
            trans.setParameter( entry.getKey(), entry.getValue() );
        }
       
        /* Do the transformation. */
        Result res = new StreamResult( System.out );
        trans.transform( docSrc, res  );
    }
}
