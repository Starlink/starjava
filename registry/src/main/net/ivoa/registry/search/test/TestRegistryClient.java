/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search.test;

import net.ivoa.registry.search.RegistrySearchClient;
import net.ivoa.registry.search.SOAPSearchClient;
import net.ivoa.registry.search.ServiceCaller;
import net.ivoa.registry.search.Records;
import net.ivoa.registry.search.Identifiers;
import net.ivoa.registry.search.VOResource;
import net.ivoa.registry.search.MessagePrintingServiceCaller;
import net.ivoa.registry.RegistryServiceException;
import net.ivoa.registry.RegistryAccessException;
import net.ivoa.registry.RegistryCommException;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.zip.Adler32;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.IOException;

import ncsa.horizon.util.CmdLine;



public class TestRegistryClient {

    static PrintStream pout = System.out;

    public static void main(String[] args) {

        CmdLine cl = new CmdLine("vxqos:m:e:", (CmdLine.RELAX|CmdLine.USRWARN));

        // parse the command line
        try {
            cl.setCmdLine(args); 
        } catch (CmdLine.UnrecognizedOptionException ex) { }

        RegistrySearchClient client = null;
        if (cl.isSet('e')) {
            try {
                URL url = new URL(cl.getValue('e'));
                client = new RegistrySearchClient(url);
                if (cl.isSet('v')) 
                    pout.println("Accessing registry at " + url);
                if (cl.isSet('q'))
                    client.qualifySoapArgs(true);
            } catch (MalformedURLException ex) {
                System.err.println("Bad URL: " + cl.getValue('e'));
            }
            if (cl.isSet('x')) 
                client.setCaller(new MessagePrintingServiceCaller());
        }
        else {
            client = new RegistrySearchClient();
            if (cl.isSet('q'))
                client.qualifySoapArgs(true);
            client.setCaller(
                 new MessagePrintingServiceCaller(new SimSOAPConnection()));
        }

        if (cl.getNumArgs() == 0) {
            runTests(client);
            System.exit(0);
        }

        String method = (String) cl.arguments().nextElement();
        if (method.equalsIgnoreCase("getIdentity")) {
            handleGetIdentity(client,cl);
        }
        else if (method.equalsIgnoreCase("getResource")) {
            handleGetResource(client,cl);
        }
        else if (method.equalsIgnoreCase("searchByADQL")) {
            handleSearchByADQL(client,cl);
        }
        else if (method.equalsIgnoreCase("searchByKeywords")) {
            handleSearchByKeywords(client,cl);
        }
        else if (method.equalsIgnoreCase("idsByADQL")) {
            handleIDsByADQL(client,cl);
        }
        else if (method.equalsIgnoreCase("idsByKeywords")) {
            handleIDsByKeywords(client,cl);
        }
//         else if (method.equalsIgnoreCase("xquery")) {
//             handleXQuery(client,cl);
//         }
        else {
            System.err.println("Unknown operation: " + method);
            usage();
            System.exit(1);
        }

        System.exit(0);
    }

    static void handleSearchByADQL(RegistrySearchClient client, CmdLine cl) {
        Enumeration args = cl.arguments();
        args.nextElement();
        StringBuffer adql = new StringBuffer();
        while (args.hasMoreElements()) {
            adql.append(args.nextElement());
            if (adql.length() > 0 && args.hasMoreElements()) adql.append(' ');
        }
        if (adql.length() == 0) 
            throw new IllegalArgumentException("No search constraints given");
        if (cl.isSet('v')) 
            pout.println("Query constraints: " + adql.toString());

        Records recs = null;
        try { 
            recs = client.searchByADQL(adql.toString());

            if (cl.isSet('v')) 
                pout.println("Retrieved " + recs.getRetrievedCount() + 
                                   " records so far.");

            int max = 20;
            if (cl.isSet('m')) {
                try {
                    max = Integer.parseInt(cl.getValue('m'));
                } catch (NumberFormatException ex) {
                    System.err.println("Non-integer given for -m: " + 
                                       cl.getValue('m'));
                    System.exit(1);
                }
            }
            
            Vector fields = new Vector(2);
            if (cl.isSet('s')) {
                StringTokenizer st = new StringTokenizer(cl.getValue('s'),",");
                while (st.hasMoreTokens()) {
                    fields.addElement(st.nextToken());
                }
            }
            if (fields.size() == 0) {
                fields.addElement("identifier");
                fields.addElement("title");
                fields.addElement("capability/interface/accessURL");
            }

            VOResource rec = null;
            int n = 0;
            while (recs.hasNext()) {
                rec = recs.next();
                pout.println("Record " + (++n) + ":");
                for(int i=0; i < fields.size(); i++) {
                    String name = (String) fields.elementAt(i);
                    String[] vals = rec.getParameters(name);
                    for(int j=0; j < vals.length; j++) 
                        pout.println("  " + name + ": " + vals[j]);
                }
                if (n >= max) {
                    if (recs.hasNext()) 
                        pout.println("More unprinted records found " + 
                                    "after printing " + n + " records");
                    break;
                }
            }
            if (cl.isSet('v'))
                pout.println("Retrieved a total of " + 
                            recs.getRetrievedCount() + " records");
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    static void handleSearchByKeywords(RegistrySearchClient client, CmdLine cl)
    {
        Enumeration args = cl.arguments();
        args.nextElement();
        StringBuffer kw = new StringBuffer();
        while (args.hasMoreElements()) {
            kw.append(args.nextElement());
            if (kw.length() > 0 && args.hasMoreElements()) kw.append(' ');
        }
        if (kw.length() == 0) 
            throw new IllegalArgumentException("No keyword constraints given");
        if (cl.isSet('v')) 
            pout.println("Query constraints: " + kw.toString());

        Records recs = null;
        try { 
            recs = client.searchByKeywords(kw.toString(), cl.isSet('o'));

            if (cl.isSet('v')) 
                pout.println("Retrieved " + recs.getRetrievedCount() + 
                            " records so far.");

            int max = 20;
            if (cl.isSet('m')) {
                try {
                    max = Integer.parseInt(cl.getValue('m'));
                } catch (NumberFormatException ex) {
                    System.err.println("Non-integer given for -m: " + 
                                       cl.getValue('m'));
                    System.exit(1);
                }
            }
            
            Vector fields = new Vector(2);
            if (cl.isSet('s')) {
                StringTokenizer st = new StringTokenizer(cl.getValue('s'),",");
                while (st.hasMoreTokens()) {
                    fields.addElement(st.nextToken());
                }
            }
            if (fields.size() == 0) {
                fields.addElement("identifier");
                fields.addElement("title");
                fields.addElement("capability/interface/accessURL");
            }

            VOResource rec = null;
            int n = 0;
            while (recs.hasNext()) {
                rec = recs.next();
                pout.println("Record " + (++n) + ":");
                for(int i=0; i < fields.size(); i++) {
                    String name = (String) fields.elementAt(i);
                    String[] vals = rec.getParameters(name);
                    for(int j=0; j < vals.length; j++) 
                        pout.println("  " + name + ": " + vals[j]);
                }
                if (n >= max) {
                    if (recs.hasNext()) 
                        pout.println("More unprinted records found " + 
                                    "after printing " + n + " records");
                    break;
                }
            }
            if (cl.isSet('v'))
                pout.println("Retrieved a total of " + 
                            recs.getRetrievedCount() + " records");
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    static void handleIDsByADQL(RegistrySearchClient client, CmdLine cl) {
        Enumeration args = cl.arguments();
        args.nextElement();
        StringBuffer adql = new StringBuffer();
        while (args.hasMoreElements()) {
            adql.append(args.nextElement());
            if (adql.length() > 0 && args.hasMoreElements()) adql.append(' ');
        }
        if (adql.length() == 0) 
            throw new IllegalArgumentException("No search constraints given");
        if (cl.isSet('v')) 
            pout.println("Query constraints: " + adql.toString());

        Identifiers recs = null;
        try { 
            recs = client.identifiersByADQL(adql.toString());

            if (cl.isSet('v')) 
                pout.println("Retrieved " + recs.getRetrievedCount() + 
                            "records so far.");

            int max = 20;
            if (cl.isSet('m')) {
                try {
                    max = Integer.parseInt(cl.getValue('m'));
                } catch (NumberFormatException ex) {
                    System.err.println("Non-integer given for -m: " + 
                                       cl.getValue('m'));
                    System.exit(1);
                }
            }
            
            String rec = null;
            int n = 0;
            while (recs.hasNext()) {
                rec = recs.next();
                pout.println("Record " + (++n) + ": " + rec);
                if (n >= max) {
                    if (recs.hasNext()) 
                        pout.println("More unprinted records found " + 
                                    "after printing " + n + " records");
                    break;
                }
            }
            if (cl.isSet('v'))
                pout.println("Retrieved a total of " + 
                            recs.getRetrievedCount() + " records");
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    static void handleIDsByKeywords(RegistrySearchClient client, CmdLine cl) {
        Enumeration args = cl.arguments();
        args.nextElement();
        StringBuffer kw = new StringBuffer();
        while (args.hasMoreElements()) {
            kw.append(args.nextElement());
            if (kw.length() > 0 && args.hasMoreElements()) kw.append(' ');
        }
        if (kw.length() == 0) 
            throw new IllegalArgumentException("No search constraints given");
        if (cl.isSet('v')) 
            pout.println("Query constraints: " + kw.toString());

        Identifiers recs = null;
        try { 
            recs = client.identifiersByKeywords(kw.toString(), cl.isSet('o'));

            if (cl.isSet('v')) 
                pout.println("Retrieved " + recs.getRetrievedCount() + 
                            "records so far.");

            int max = 20;
            if (cl.isSet('m')) {
                try {
                    max = Integer.parseInt(cl.getValue('m'));
                } catch (NumberFormatException ex) {
                    System.err.println("Non-integer given for -m: " + 
                                       cl.getValue('m'));
                    System.exit(1);
                }
            }
            
            String rec = null;
            int n = 0;
            while (recs.hasNext()) {
                rec = recs.next();
                pout.println("Record " + (++n) + ": " + rec);
                if (n >= max) {
                    if (recs.hasNext()) 
                        pout.println("More unprinted records found " + 
                                    "after printing " + n + " records");
                    break;
                }
            }
            if (cl.isSet('v'))
                pout.println("Retrieved a total of " + 
                            recs.getRetrievedCount() + " records");
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    static void handleGetIdentity(RegistrySearchClient client, CmdLine cl) {
        Vector fields = new Vector(3);
        if (cl.isSet('s')) {
            StringTokenizer st = new StringTokenizer(cl.getValue('s'),",");
            while (st.hasMoreTokens()) {
                fields.addElement(st.nextToken());
            }
        }
        if (fields.size() == 0) {
            fields.addElement("identifier");
            fields.addElement("title");
            fields.addElement("capability/interface/accessURL");
        }

        try {
            VOResource rec = client.getIdentity();

            for(int i=0; i < fields.size(); i++) {
                String name = (String) fields.elementAt(i);
                String[] vals = rec.getParameters(name);
                for(int j=0; j < vals.length; j++) 
                    pout.println("  " + name + ": " + vals[j]);
            }
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }

    }

    static void handleGetResource(RegistrySearchClient client, CmdLine cl) {
        Vector fields = new Vector(3);
        if (cl.isSet('s')) {
            StringTokenizer st = new StringTokenizer(cl.getValue('s'),",");
            while (st.hasMoreTokens()) {
                fields.addElement(st.nextToken());
            }
        }
        if (fields.size() == 0) {
            fields.addElement("identifier");
            fields.addElement("title");
            fields.addElement("capability/interface/accessURL");
        }

        Enumeration args = cl.arguments();
        args.nextElement();
        if (! args.hasMoreElements()) {
            System.err.println("No resource identifier provided");
            System.exit(1);
        }

        try {
            VOResource rec = client.getResource((String) args.nextElement());

            for(int i=0; i < fields.size(); i++) {
                String name = (String) fields.elementAt(i);
                String[] vals = rec.getParameters(name);
                for(int j=0; j < vals.length; j++) 
                    pout.println("  " + name + ": " + vals[j]);
            }
        }
        catch (RegistryCommException ex) {
            System.err.println("Search communication error: "+ex.getMessage());
            Exception wrapped = ex.getTargetException();
            if (wrapped != null) 
                wrapped.printStackTrace();
            else 
                ex.printStackTrace();
            System.exit(1);
        }
        catch (RegistryAccessException ex) {
            System.err.println("Search failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }

    }

    static void runTests(RegistrySearchClient client) {
        String od = System.getProperty("ivoaregistry.tmpdir");
        if (od == null) od = ".";
        File outdir = new File(od);
        File outfile = new File(od, "GetIdentity.txt");
        try {
            pout = new PrintStream(new FileOutputStream(outfile));
        }
        catch (IOException ex) {
            throw new InternalError("Problem opening " + outfile + 
                                    " for write");
        }

        MessagePrintingServiceCaller caller = 
            new MessagePrintingServiceCaller(new SimSOAPConnection());
        caller.setMessageDestination(pout);
        client.setCaller(caller);

        CmdLine cl = new CmdLine("vxos:m:e:", (CmdLine.RELAX|CmdLine.USRWARN));

        // parse the command line
        try {
            cl.setCmdLine(new String[] { "-vx", "getIdentity" }); 
        } catch (CmdLine.UnrecognizedOptionException ex) { }

        try {
            handleGetIdentity(client, cl);
            pout.close();
        }
        catch (Exception ex) {
            System.err.println("Problem running getIdentity");
            System.exit(5);
        }

        long tst = 0, std=0;
        try {
            tst = getChecksum(new FileInputStream(outfile));
        }
        catch (IOException ex) {
            throw new Error("I/O error during checksums: " + ex.getMessage());
        }
        try {
            std = getChecksum(
                 client.getClass().getResourceAsStream("test/GetIdentity.txt"));

        }
        catch (IOException ex) {
            throw new Error("I/O error during checksums: " + ex.getMessage());
        }

        if (tst != std) 
            throw new Error("getIdentity output does not verify");

//         System.err.println("No automatic tests built in yet");
//         System.exit(1);
    }

    static long getChecksum(InputStream is) throws IOException {
        Adler32 cs = new Adler32();
        BufferedInputStream bis = new BufferedInputStream(is);
        byte[] buf = new byte[1024];
        int n = 0;
        while ((n = bis.read(buf)) > 0) {
            cs.update(buf,0,n);
        }
        bis.close();
        return cs.getValue();
    }

    public static void usage() {
        System.err.println("TestRegistryClient [[options] command [args]]");
    }

    static class SimSOAPConnection extends SOAPConnection {
        MessageFactory soapfactory = null;
        public SimSOAPConnection() {
            try {
                soapfactory = MessageFactory.newInstance();
            }
            catch (SOAPException ex) {
                throw new InternalError("installation/config error: " + 
                                        ex.getMessage());
            }
        }
        public SOAPMessage call(SOAPMessage request, Object endpoint) 
             throws SOAPException
        {
            String action = getAction(request);

            if (action.equals(SOAPSearchClient.GETIDENTITY_ACTION) ||
                action.equals(SOAPSearchClient.GETRESOURCE_ACTION)) 
            {
                return getIdentity();
            }

            if (true) throw new SOAPException("operation unavailable");
            return null;
        }
        public void close() { }

        String getAction(SOAPMessage req) {
            String[] matches = req.getMimeHeaders().getHeader("SOAPAction");
            return ((matches.length > 0) ? matches[0] : null);
        }

        public SOAPMessage getIdentity() throws SOAPException {
            InputStream res = getClass().getResourceAsStream("registry.xml");
            if (res == null) 
                throw new SOAPException("Missing resource: registry.xml");

            SOAPMessage out = soapfactory.createMessage();
            SOAPEnvelope env = out.getSOAPPart().getEnvelope();
            SOAPBody body = env.getBody();

            Document wrapper = null;
            try {
                DocumentBuilderFactory factory = 
                    DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                Document doc = builder.parse(res);
                Element vor = doc.getDocumentElement();

                // concatonate split text values 
                vor.normalize();

                wrapper = builder.newDocument();
                Element root = wrapper.createElement("ResolveResponse");
                root.setAttribute("xmlns", SOAPSearchClient.WSDL_NS);
                wrapper.appendChild(root);
                vor = (Element) wrapper.importNode(vor, true);
                vor.setAttribute("xmlns", "");
                root.appendChild(vor);
            }
            catch (ParserConfigurationException e) {
                throw new InternalError("XML Parser Configuration problem: " + 
                                        e.getMessage());
            }
            catch (SAXException e) {
                throw new InternalError("XML parsing problem on registry.xml: "+
                                        e.getMessage());
            }
            catch (IOException e) {
                throw new InternalError("IO problem on registry.xml: "+
                                        e.getMessage());
            }
            
            body.addDocument(wrapper);
            return out;
        }
    }
}

