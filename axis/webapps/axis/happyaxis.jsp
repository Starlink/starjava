<html>
<head>
<title>Axis Happiness Page</title>
</head>
<body bgcolor='#ffffff'>
<%@ page import="java.io.InputStream,
                 java.io.IOException"
    session="false" %>
<%!

    /*
     * Happiness tests for axis. These look at the classpath and warn if things
     * are missing. Normally addng this much code in a JSP page is mad
     * but here we want to validate JSP compilation too, and have a drop-in
     * page for easy re-use
     * @author Steve 'configuration problems' Loughran
     */


    /**
     * Get a string providing install information.
     * TODO: make this platform aware and give specific hints
     */
    public String getInstallHints(HttpServletRequest request) {

        String hint=
            "<B><I>Note:</I></B> On Tomcat 4.x, you may need to put libraries that contain "
            +"java.* or javax.* packages into CATALINA_HOME/commons/lib";
        return hint;
    }

    /**
     * test for a class existing
     * @param classname
     * @return class iff present
     */
    Class classExists(String classname) {
        try {
            return Class.forName(classname);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * test for resource on the classpath
     * @param resource
     * @return true iff present
     */
    boolean resourceExists(String resource) {
        boolean found;
        InputStream instream=this.getClass().getResourceAsStream(resource);
        found=instream!=null;
        if(instream!=null) {
            try {
                instream.close();
            } catch (IOException e) {
            }
        }
        return found;
    }

    /**
     * probe for a class, print an error message is missing
     * @param out stream to print stuff
     * @param category text like "warning" or "error"
     * @param classname class to look for
     * @param jarFile where this class comes from
     * @param errorText extra error text
     * @param homePage where to d/l the library
     * @return the number of missing classes
     * @throws IOException
     */
    int probeClass(JspWriter out,
                   String category,
                   String classname,
                   String jarFile,
                   String description,
                   String errorText,
                   String homePage) throws IOException {

       Class clazz = classExists(classname);
       if(clazz == null)  {
            String url="";
            if(homePage!=null) {
                url="<br>  See <a href="+homePage+">"+homePage+"</a>";
            }
            out.write("<p>"+category+": could not find class "+classname
                    +" from file <b>"+jarFile
                    +"</b><br>  "+errorText
                    +url
                    +"<p>");
            return 1;
        } else {
            String location = getLocation(out, clazz);
            if(location == null) {
                out.write("Found "+ description + " (" + classname + ")<br>");
            }
            else {
                out.write("Found "+ description + " (" + classname + ") at " + location + "<br>");
            }
            return 0;
        }
    }


    String getLocation(JspWriter out, 
                       Class clazz) throws IOException {
        try {
            java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
            String location = url.toString();
            if(location.startsWith("jar")) {
                url = ((java.net.JarURLConnection)url.openConnection()).getJarFileURL();
                location = url.toString();
            } 
            
            if(location.startsWith("file")) {
                java.io.File file = new java.io.File(url.getFile());
                return file.getAbsolutePath();
            } else {
                return url.toString();
            }
        } catch (Throwable t){
        }
        return null;
    }

    /**
     * a class we need if a class is missing
     * @param out stream to print stuff
     * @param classname class to look for
     * @param jarFile where this class comes from
     * @param errorText extra error text
     * @param homePage where to d/l the library
     * @throws IOException when needed
     * @return the number of missing libraries (0 or 1)
     */
    int needClass(JspWriter out,
                   String classname,
                   String jarFile,
                   String description,
                   String errorText,
                   String homePage) throws IOException {
        return probeClass(out,
                "<b>Error</b>",
                classname,
                jarFile,
                description,
                errorText,
                homePage);
    }

    /**
     * print warning message if a class is missing
     * @param out stream to print stuff
     * @param classname class to look for
     * @param jarFile where this class comes from
     * @param errorText extra error text
     * @param homePage where to d/l the library
     * @throws IOException when needed
     * @return the number of missing libraries (0 or 1)
     */
    int wantClass(JspWriter out,
                   String classname,
                   String jarFile,
                   String description,
                   String errorText,
                   String homePage) throws IOException {
        return probeClass(out,
                "<b>Warning</b>",
                classname,
                jarFile,
                description,
                errorText,
                homePage);
    }

    /**
     * probe for a resource existing,
     * @param out
     * @param resource
     * @param errorText
     * @throws Exception
     */
    int wantResource(JspWriter out,
                      String resource,
                      String errorText) throws Exception {
        if(!resourceExists(resource)) {
            out.write("<p><b>Warning</b>: could not find resource "+resource
                        +"<br>"
                        +errorText);
            return 0;
        } else {
            out.write("found "+resource+"<br>");
            return 1;
        }
    }
    %>
<html><head><title>Axis Happiness Page</title></head>
<body>

<h2>Examining webapp configuration</h2>

<p>
<h3>Needed Components</h3>
<%
    int needed=0,wanted=0;

    /**
     * the essentials, without these Axis is not going to work
     */
    needed=needClass(out, "javax.xml.soap.SOAPMessage",
            "saaj.jar",
            "SAAJ API",
            "Axis will not work",
            "http://xml.apache.org/axis/");

    needed+=needClass(out, "javax.xml.rpc.Service",
            "jaxrpc.jar",
            "JAX-RPC API",
            "Axis will not work",
            "http://xml.apache.org/axis/");

    needed+=needClass(out, "org.apache.axis.transport.http.AxisServlet",
            "axis.jar",
            "Apache-Axis",
            "Axis will not work",
            "http://xml.apache.org/axis/");

    needed+=needClass(out, "org.apache.commons.logging.Log",
            "commons-logging.jar",
            "Jakarta-commons logging",
            "Axis will not work",
            "http://jakarta.apache.org/commons/logging.html");

    //should we search for a javax.wsdl file here, to hint that it needs
    //to go into an approved directory? because we dont seem to need to do that.
    needed+=needClass(out, "com.ibm.wsdl.factory.WSDLFactoryImpl",
            "wsdl4j.jar",
            "IBM's WSDL4Java",
            "Axis will not work",
            null);

    needed+=needClass(out, "javax.xml.parsers.SAXParserFactory",
            "xerces.jar",
            "JAXP implementation",
            "Axis will not work",
            "http://xml.apache.org/xerces-j/");

    needed+=needClass(out,"javax.activation.DataHandler",
            "activation.jar",
            "Activation API",
            "Axis will not work",
            "http://java.sun.com/products/javabeans/glasgow/jaf.html");
%>
<h3>Optional Components</h3>
<%
    /*
     * now the stuff we can live without
     */
    wanted+=wantClass(out,"javax.mail.internet.MimeMessage",
            "mail.jar",
            "Mail API",
            "Attachments will not work",
            "http://java.sun.com/products/javamail/");

    wanted+=wantClass(out,"org.apache.xml.security.Init",
            "xmlsec.jar",
            "XML Security API",
            "XML Security is not supported",
            "http://xml.apache.org/security/");

    /*
     * resources on the classpath path
     */
    /* broken; this is a file, not a resource
    wantResource(out,"/server-config.wsdd",
            "There is no server configuration file;"
            +"run AdminClient to create one");
    */
    /* add more libraries here */

    out.write("<h3>");
    //is everythng we need here
    if(needed==0) {
       //yes, be happy
        out.write("<i>The core axis libraries are present. </i>");
    } else {
        //no, be very unhappy
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.write("<i>"
                +needed
                +" core axis librar"
                +(needed==1?"y is":"ies are")
                +" missing</i>");
    }
    //now look at wanted stuff
    if(wanted>0) {
        out.write("<i>"
                +wanted
                +" optional axis librar"
                +(wanted==1?"y is":"ies are")
                +" missing</i>");
    } else {
        out.write("The optional components are present.");
    }
    out.write("</h3>");
    //hint if anything is missing
    if(needed>0 || wanted>0 ) {
       out.write(getInstallHints(request));
    }

    %>
    <p>
    <B><I>Note:</I></B> Even if everything this page probes for is present, there is no guarantee your
    web service will work, because there are many configuration options that we do
    not check for. These tests are <i>necessary</i> but not <i>sufficient</i>
    <hr>
    <h2>Examining System Properties</h2>
<%
    /** 
     * Dump the system properties
     */
    java.util.Enumeration e;
    try {
        e= System.getProperties().propertyNames();
    } catch (SecurityException se) {
        e=null;
    }
    if(e!=null) {
        out.write("<pre>");
        for (;e.hasMoreElements();) {
            String key = (String) e.nextElement();
            out.write(key + "=" + System.getProperty(key)+"\n");
        }
        out.write("</pre><p>");
    } else {
        out.write("System properties are not accessible<p>");
    }
%>
    <hr>
    Platform: <%= getServletConfig().getServletContext().getServerInfo()  %>
</body>
</html>


