/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.Call;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;

import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.util.SourceReader;

/**
 * Do the work of calculating aperture photometry. Contacts the
 * photometry service and makes the necessary data available for
 * it. On return the new results are made available to the controlling
 * AperturePhotometry object (which arranges to update the PhotomList
 * and associated figures).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhotometryWorker
{
    public PhotometryWorker()
    {
        // Nothing to do.
    }

    private static URL endpoint = null;


    /**
     * Do the photometry calculations.
     */
    public void calculate( Ndx ndx, PhotomList photomList,
                           PhotometryGlobals globals,
                           AperturePhotometry  aperturePhotometry )
        throws ServiceException
    {
        //  Stop now if the NDX cannot be seen outside the current
        //  application.
        if ( ! ndx.isPersistent() ) {
            throw new ServiceException(
                "Virtual NDX " + ndx + " cannot be displayed externally" );
        }

        //  Get the services end-point. TODO: work this out from a
        //  WSDL file and maybe make the resource realisable...
        if ( endpoint == null ) {
            try {
                endpoint =
                  new URL( "http://localhost:8083/services/PhotomWSServices" );

//                endpoint =
//                  new URL( "http://starpc1:8083/services/PhotomWSServices" );
            }
            catch (Exception e) {
                // Never happens...
            }
        }

        //  Get an Element that contains the full NDX.
        Element ndxElement;
        try {
            ndxElement =
                (Element) new SourceReader().getDOM( ndx.toXML( null ) );
        }
        catch ( TransformerException e ) {
            throw new ServiceException( "Error getting XML from NDX", e );
        }

        //  Transform the PhotomList into an Element.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        }
        catch (Exception e) {
            throw new ServiceException("Error converting PhotomList to XML",e);
        }
        Document document = builder.newDocument();
        Element photomListElement = 
            document.createElement( photomList.getTagName() );
        photomList.encode( photomListElement );

        //  Transform the PhotometryGlobals into an Element.
        Element globalsElement = 
            document.createElement( globals.getTagName() );
        globals.encode( globalsElement );

        //  TODO: encode PhotomList and maybe globals into an HDX
        //  document or the etc component of the NDX.

        //  Set up to call the web service.
        Service service = new Service();
        org.apache.axis.client.Call call =
            (org.apache.axis.client.Call) service.createCall();
        call.setTargetEndpointAddress( endpoint );

        call.setOperationName( "autophotom" );

        //  We send the NDX and PhotomList and global parameters as XML.
        call.addParameter( "ndx", XMLType.SOAP_ELEMENT, ParameterMode.IN );
        call.addParameter( "photomlist", XMLType.SOAP_ELEMENT,
                           ParameterMode.IN );
        call.addParameter( "globals", XMLType.SOAP_ELEMENT,
                           ParameterMode.IN );

        //  And get back a PhotomList.
        call.setReturnType( XMLType.SOAP_ELEMENT );

        //  Create the argument array and add the arguments in the
        //  correct order.
        Object[] args = new Object[3];
        args[0] = ndxElement;
        args[1] = photomListElement;
        args[2] = globalsElement;
        try {
            Element result = (Element) call.invoke( args );

            // If successful tell the AperturePhotometry controller
            // that's there's work to do, but first update the list of
            // results.
            photomList.decode( result );
            aperturePhotometry.calculationsDone();
        }
        catch (Exception e) {
            // Do nothing.
            e.printStackTrace();
        }
    }
}
