package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;

public class GetDataTable {
    
    private VOElement params;
  
    List<ParamElement> paramList; 
    // Logger.
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.splat.vo.GetDataParameters" );


    public GetDataTable( TableElement votable) {
     
      
        paramList = new ArrayList<ParamElement>();
        VOElement[] voels = votable.getChildrenByName( "PARAM" );
        int i=0;
        while ( i < voels.length ) {
       
                ParamElement pel = (ParamElement) voels[i];
                paramList.add(pel);
      
            i++;
        }
    }
    
    /**
     * Returns list containing the PARAM elements. 
     *
     * @param voe - the VO Element containing the parameters
     * @return the PARAM elements which are children of this table
     */
    public ParamElement[] getParams() {
        if (paramList==null)
            return null;
        return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );
    }
    
    

 
}
