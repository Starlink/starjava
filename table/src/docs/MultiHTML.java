import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class MultiHTML {

    public static void main( String[] args ) 
            throws IOException, TransformerException, SAXException,
                   ParserConfigurationException {
        String inName = args[ 0 ];
        String outName = args[ 1 ];
        File outDir = new File( outName );
        outDir.mkdir();

        Transformer trans = TransformerFactory.newInstance()
                           .newTransformer( new StreamSource( "multi.xslt" ) );
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
                                    .newDocumentBuilder();
        Document doc = inName.equals( "-" ) ? docBuilder.parse( System.in )
                                            : docBuilder.parse( inName );
        Element docEl = doc.getDocumentElement();
        for ( Node node = docEl.getFirstChild(); node != null; 
              node = node.getNextSibling() ) {
            if ( node instanceof Element && 
                 node.getNodeName().equals( "filesection" ) ) {
                Element el = (Element) node;
                File outFile = new File( outDir, el.getAttribute( "file" ) );
                System.out.println( "Output file " + outFile );
                trans.transform( new DOMSource( el ), 
                                 new StreamResult( outFile ) );
            }
        }
    }
}
