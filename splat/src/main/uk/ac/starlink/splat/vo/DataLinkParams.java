/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.FieldElement;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.TabularData;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * @author Margarida Castro Neves
 *
 */
public class DataLinkParams {
    
    
    ArrayList <DataLinkService> service ;
    private String id_source; // the field used as datalink id source
    private ParamElement request; // 
    private String accessURL; // the access URL for the service
    private String contentType; // 
    HashMap <String,String> paramMap= new HashMap<String,String>(); 
    int queryIndex = -1;
   
    ArrayList<ParamElement> paramList = null;
    
    /**
     * Constructs an empty instance. Information will be added with addparam(VOElement).
     *
     */
    public DataLinkParams()  {
      
        service = new ArrayList <DataLinkService>();
        id_source=null;
        request=null;
        accessURL=null;
        contentType=null;
    }
    
    
    /**
     * Constructs the DataLink Parameters from an URL string  containing a
     *  VOTable with DataLink information
     *  The VOTABLE must contain only Datalink info! ????? or?
     * @throws IOException 
     */
    public DataLinkParams( String dataLinksrc) throws IOException {
        id_source=null;
        request=null;
        accessURL=null;
        contentType=null;
        service = new ArrayList<DataLinkService>();
        DataLinkService thisService = new DataLinkService();

        URL dataLinkURL = new URL(dataLinksrc);
        DataSource  datsrc = new URLDataSource( dataLinkURL );

        StarTable starTable =
                new VOTableBuilder().makeStarTable( datsrc, true,
                        StoragePolicy.getDefaultPolicy() );
      
        int ncol = starTable.getColumnCount();

        for( int k = 0; k < ncol; k++ ) {
            ColumnInfo colInfo = starTable.getColumnInfo( k );

            //   String utype = colInfo.getUtype();
            String name = colInfo.getName();
            if ( name != null )
                thisService.addParam(name, (String) starTable.getCell(0, k));

            //  if ( utype != null ) {
            //     utype = utype.toLowerCase();
            //     if ( utype.endsWith( "datalink.accessurl" ) ) {
            //        thisService.addParam("accessURL", (String) starTable.getCell(0, k));
            //     }
            //     if ( utype.endsWith( "datalink.contenttype" ) ) {
            //         thisService.addParam("contentType", (String) starTable.getCell(0, k));
            // }
        }
        service.add(thisService);
    }
    
    /**
     * Constructs the DataLink Parameters from a VOElement (<RESOURCE type="service" >)
     * 
     */
    public DataLinkParams( VOElement voel) {
     
        //DataLinkService thisService = new DataLinkService();
        service = new ArrayList<DataLinkService>();
        addService( voel );
  
    }
    
    /*
     * gets as input an VO Element of the type <RESOURCE type="service">
     * and reads the datalink parameters from it
     */
   
    public void addService(VOElement serviceEl)  // TO DO - how to check which service is wanted????????
    {
       DataLinkService thisService = new DataLinkService();
       
       // handle the PARAM elements
       VOElement[] voels = serviceEl.getChildrenByName( "PARAM" ); // the param elements
        int i=0;
        while ( i < voels.length ) {
            ParamElement pel = (ParamElement) voels[i];
            thisService.addParam(pel.getAttribute("name"), pel.getAttribute("value"));
            i++;
        }
        
       i=0;
      
       // handle the GROUP with name=InputParams element and its parameters
       VOElement[] grpels = serviceEl.getChildrenByName( "GROUP"/*, "name", "inputParams"*/ ); // the GROUP Element
       VOElement grpel = null;
        while ( i < grpels.length && grpel == null) {
            VOElement gel =  grpels[i];
            String name = gel.getAttribute("name");
           
            if (name.equalsIgnoreCase("InputParams")) {
                grpel = gel;
            }
            i++;
        }

        if (grpel != null )
        {
            VOElement[] grpParams = grpel.getChildrenByName( "PARAM" ); // the param elements
            // handle the group  PARAM elements
            int size=grpParams.length;
            for ( int j=0; j < size ; j++ ) {
                ParamElement pel = (ParamElement) grpParams[j];
                if ( pel.getAttribute("name").equals("ID")) {
                    VOElement el = pel.getChildByName("LINK"); // link inside a group element
                    if (el.getAttribute( "content-role").equals("ddl:id-source") )
                        id_source = el.getAttribute("value"); 
                    thisService.addParam("idSource", el.getAttribute("value"));
                } else {
                    thisService.addGroupParam(pel);
                    queryIndex = j;
                }
            }

            service.add(thisService);
        }
    }
    
    public  ParamElement [] getQueryParams(int queryIndex) {    
        if (service.get(queryIndex) != null)
            return service.get(queryIndex).getQueryParams();
        else return null;
    }
    public boolean hasQueryParams(int i) { // hack - to differenciate a service with query parameters where user inputs the values and
                                           // services containing only links. SHOULD BE DONE DIFFERENTLY (HOW?)
        return (service.get(queryIndex).getQueryParams().length != 0);
        
    }
    public int getServiceCount() {
        return service.size();
    }
    /*
    public String  getCurrentQueryIdSource() {     // return the information for the last added service
        int current=service.size();
        if (current > 0)
            return service.get(current-1).getParam("idSource");
        else return (null);
    }
    
 //   public ParamElement  getCurrentQueryRequest() {
//        return request;
 //   }
    
    public String getCurrentQueryAccessURL() { 
        int current=service.size();
        if (current > 0)
            return service.get(current-1).getParam("accessURL");
        else return (null);
    }
    public String  getQueryIdSource(int queryIndex) {       
        return service.get(queryIndex).getParam("idSource");
    }
    
 //   public ParamElement  getQueryRequest(int queryIndex) {
 //       return service.get(queryIndex).getParam("idSource");
 ///       return request;
    }
    */
    public String getQueryAccessURL(int queryIndex) {
        if (queryIndex >= 0 && queryIndex < getServiceCount())
            return service.get(queryIndex).getParam("accessURL");
        else return null;
            
    }
  
    public String getQueryContentType(int queryIndex) {
        if (queryIndex >= 0 && queryIndex < getServiceCount())
            return service.get(queryIndex).getParam("contentType");
        else return null;
        
    }
    
    public String  getQueryIdSource(int queryIndex) {   
        if (queryIndex >= 0 && queryIndex < getServiceCount())
            return service.get(queryIndex).getParam("idSource");
        else return null;
    }

 //   public ParamElement[]  getParams() {
 //       return (ParamElement[]) paramList.toArray( new ParamElement[ 0 ] );
 //   }

   
    
  /*
   *********************************************************
   * DataLinkService
   * describes one datalink service
   * (identified in a VOTable as <RESOURCE type="service">)
   * 
   * @author Margarida Castro Neves
   *
   *********************************************************
   */
    
    protected class DataLinkService {
        
        //  parameter -  value 
        HashMap <String,String> paramMap= null; 
        // parameters inside a GROUP element
        ArrayList <ParamElement> groupParams = null;
        
        
        protected DataLinkService() {
            paramMap= new HashMap<String,String>(); 
            groupParams= new ArrayList<ParamElement>();
        }
        
        protected  void addParam(String key, String value) {
            paramMap.put(key, value);
            
        }
        protected  void addGroupParam(ParamElement param) {
            
            groupParams.add(param);
            
        }
        protected ParamElement [] getQueryParams() {
      
            return (ParamElement[]) groupParams.toArray(new ParamElement[]{});
        }
        
        protected String getParam(String name) {
            return paramMap.get(name);
        }
        
    }
    
}
