/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search.test;

import net.ivoa.registry.search.SOAPSearchClient;
import net.ivoa.registry.RegistryServiceException;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import org.w3c.dom.Element;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

public class TestSOAPProduction extends SOAPSearchClient {

    private static URL dummyurl = null;
    static {
        try {
            dummyurl = new URL("http://test.net/dummy");
        }
        catch (MalformedURLException ex) {
            // shouldn't get here
            throw new InternalError("programmer error: " + ex.getMessage());
        }
    }

    public TestSOAPProduction() {
        this(dummyurl);
    }

    public TestSOAPProduction(URL endpointURL) {
        super(endpointURL);
    }

    /**
     * submit the soap message
     */
    protected Element call(SOAPMessage msg, String actionURI) 
         throws RegistryServiceException, SOAPException
    {
        try {
            msg.writeTo(System.err);
            System.err.println("");
        } catch (IOException ex) {
            throw new RegistryServiceException(ex.getMessage());
        }
        return (Element) msg.getSOAPBody().getChildElements().next();
    }

    public static void main(String[] args) {
        SOAPSearchClient client = new TestSOAPProduction();
        try {
            if (args.length <= 0 || args[0].equals("getIdentity"))
                client.getIdentity();
            else if (args[0].equals("getResource"))
                client.getResource("ivo://ivoa.net");
            else if (args[0].equals("keywordSearch")) 
                client.keywordSearch("quasars [black hole]", true, 0, 0, false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
