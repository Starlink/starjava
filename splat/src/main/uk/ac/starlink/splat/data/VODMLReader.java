package uk.ac.starlink.splat.data;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jsky.util.Logger;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.VODocument;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

/*
 * This class allows xml parsing of a VO Document.
 * 
 */

public class VODMLReader  {

    static String dataProductType = "/VOTABLE/VODML/TEMPLATES/INSTANCE[@dmtype='ds:Dataset']/ATTRIBUTE[@dmrole='dataProductType']/LITERAL";
    static String timeFrameKind = "/VOTABLE/VODML/TEMPLATES/INSTANCE[@dmtype='stc2:Coords']/ATTRIBUTE[@dmrole='time']/INSTANCE[@dmtype='stc2:Coord']/ATTRIBUTE[@dmrole='frame']/INSTANCE[@dmtype='stc2:TimeFrame']/ATTRIBUTE[@dmrole='kind']/LITERAL";
    
    VODocument doc;
    XPath xPath;
    
    VODMLReader(VOElement element) {
        doc = (VODocument) element.getOwnerDocument();
        xPath =  XPathFactory.newInstance().newXPath();       
    }
    
    VODMLReader(DataSource datasrc) throws SplatException {
        //  Access the VOTable.
        VOElement root = null;
        try {
        	root = new VOElementFactory().makeVOElement(datasrc);                       
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open VOTable"+e.getMessage(), e );
        }
        VOElement element = root.getChildByName("VODML");
        doc = (VODocument) element.getOwnerDocument();
        xPath =  XPathFactory.newInstance().newXPath();    
    }
 
    public String getDataProductType() {
        try {
            return xPath.compile(dataProductType).evaluate(doc);
        } catch (XPathExpressionException e) {
            Logger.warn(this, e.getMessage());
        }
        return "";
    }
    public String getTimeFrameKindParameter() {
        try {
            return xPath.compile(timeFrameKind).evaluate(doc);
        } catch (XPathExpressionException e) {
            Logger.warn(this, e.getMessage());
        }
        return "";
    }
    public String getXpath( String expression ) {
        try {
            return xPath.compile(expression).evaluate(doc);
        } catch (XPathExpressionException e) {
            Logger.warn(this, e.getMessage());
        }
        return "";
    }
}
