/**
 * 
 */
package uk.ac.starlink.splat.vo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.ValuesElement;

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
    private String format;
    private ArrayList<VOElement> serviceElement;

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
        format = null;
        serviceElement=null;
    }
    
    
    /**
     * Constructs the DataLink Parameters from an URL string  containing a
     *  VOTable with DataLink information
     *  The VOTABLE must contain only Datalink info
     * @throws IOException 
     */
    public DataLinkParams( String dataLinksrc) throws IOException {
        id_source=null;
        request=null;
        accessURL=null;
        contentType=null;
        format = ""; 
        service = new ArrayList<DataLinkService>();
        DataLinkService thisService = new DataLinkService();

        URL dataLinkURL = new URL(dataLinksrc);
        DataSource  datsrc = new URLDataSource( dataLinkURL );

        StarTable starTable =
                new VOTableBuilder().makeStarTable( datsrc, true,
                        StoragePolicy.getDefaultPolicy() );
      
        int ncol = starTable.getColumnCount();
        long nrow = starTable.getRowCount();
        String [] columnNames = new String[ncol];

        int semanticsColumn = -1; // semantics
        
        for ( int i = 0; i < columnNames.length; i++ ) {
            ColumnInfo colInfo = starTable.getColumnInfo( i );
            columnNames[i] = colInfo.getName().replaceAll( "\\s", "_" );
            if (columnNames[i].equals("semantics") )
                semanticsColumn = i;
        }
        
        for( int j = 0; j < nrow; j++ ) {
            String semantics = (String) starTable.getCell(j, semanticsColumn).toString();
            if ( semantics.equalsIgnoreCase("#this") || semantics.equalsIgnoreCase("#self")) { // at the moment only the #self/#this is being retrieved
                for (int k=0;k< ncol; k++) {
                    if ( columnNames[k] != null && starTable.getCell(j, k)!= null) {
                        thisService.addParam(columnNames[k], (String) starTable.getCell(j, k).toString());
                    }
                }
            }
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
           
            if (name.equalsIgnoreCase("input")||name.equalsIgnoreCase("inputparams")) { // InputParams
                // TODO 2015 remove inputparams, accept only input
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
                    id_source = pel.getAttribute("ref");
                    if (id_source.startsWith("#") )
                            id_source=id_source.substring(1);
                    // LINK will not be used anymore
                    // will be removed soon
                  /*  if (id_source == null ) {
                        VOElement el = pel.getChildByName("LINK"); // link inside a group element
                        if (el.getAttribute( "content-role").equals("ddl:id-source") )
                            id_source = el.getAttribute("value"); 
                    }*/
                    thisService.addParam("idSource", id_source );
       /*         } else if ( pel.getAttribute("name").equals("FORMAT") ) { //choose best format
                    ValuesElement values = (ValuesElement) pel.getChildByName("VALUES");
                    String [] options = values.getOptions();
                    format = "application/x-votable"; // default value
                    for (i=0;i<options.length; i++) {
                        if ( options[i].contains("application/x-votable")) {
                           format=options[i];
                        } else if ( format == null && options[i].contains("application/fits")) {
                            format=options[i];
                        }
                    }
                   
                    thisService.addParam("FORMAT", format);
                   // thisService.addParam("FORMAT", "application/x-votable");
        */            
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
 
    public String getQueryAccessURL(int queryIndex) {
        if (queryIndex >= 0 && queryIndex < getServiceCount()) {
            String idsrc;
            idsrc = service.get(queryIndex).getParam("access_url");//("accessURL");
            if (idsrc != null) 
                return idsrc;
            idsrc = service.get(queryIndex).getParam("accessURL"); // temporary -- to b removed
            if (idsrc != null) 
                return idsrc;
        }
        return null;
            
    }
  
    public String getQueryContentType(int queryIndex) {
        if (queryIndex >= 0 && queryIndex < getServiceCount()) {
            String conttype = service.get(queryIndex).getParam("content_type"); //("contentType");
            if (conttype != null) 
                return conttype;
            conttype = service.get(queryIndex).getParam("contentType");// temporary -- to b removed
            if (conttype != null) 
                return conttype;
        }
        return null;
        
    }
    
    public String  getQueryIdSource(int queryIndex) {   
        if (queryIndex >= 0 && queryIndex < getServiceCount()) {
            String idsrc= service.get(queryIndex).getParam("ID");  // ("idSource");
            if (idsrc != null) 
                return idsrc;
            idsrc= service.get(queryIndex).getParam("idSource");  // temporary -- to b removed
            if (idsrc != null) 
                return idsrc;
        }
        return null;
    }

    
    public String  getQueryFormat(int queryIndex) {   
        if (queryIndex >= 0 && queryIndex < getServiceCount()) {
            String format = service.get(queryIndex).getQueryParam("FORMAT");
            //if (format == null)
            //   format = service.get(queryIndex).getQueryParam("content_type");
            return format;
        }
        else return null;
    }
 

    public void setServiceElement(ArrayList<VOElement> serviceEl) {
        serviceElement=serviceEl;
        
    }
    public ArrayList<VOElement> getServiceElement() {
        return serviceElement;        
    }
   
    public void writeParamToFile( BufferedWriter writer, String sname ) throws IOException {
        //writer.write( "<RESOURCE type=\"meta\" utype=\"adhoc:service\" name=\""+name+"\">" );
        // 

        for (int i=0; i<service.size(); i++) {
            ParamElement[] pel = service.get(i).getQueryParams();
            if (pel.length >0) {
                writer.write( "<RESOURCE utype=\"adhoc:service\" name=\""+sname+"\">" );
                writer.newLine();
                writer.write( "<GROUP name=\"inputparams\">");
                writer.newLine();
                writer.write( "<PARAM arraysize=\"*\" datatype=\"char\" name=\"ID\" ref=\"ssa_pubDID\" ucd=\"meta.id;meta.main\" value=\""+id_source+"\"></PARAM>");
                writer.newLine();
                
                for (int j=0;j<pel.length; j++) {
                    String arraysize=pel[j].getAttribute("arraysize");
                    String datatype=pel[j].getAttribute("datatype");
                    String name=pel[j].getAttribute("name");
                    String ucd=pel[j].getAttribute("ucd");
                    String utype=pel[j].getAttribute("utype");
                    String attrvalue=pel[j].getAttribute("value");

                    ValuesElement values = (ValuesElement) pel[j].getChildByName("VALUES");
                    if (values != null ) {
                        String [] options = values.getOptions(); 
                        if (options != null && options.length >0) {                   
                            writer.write("<PARAM arraysize=\""+arraysize+ "\" datatype=\""+datatype+"\" name=\""+name+ 
                                    "\" ucd=\""+ucd+ "\" utype=\""+utype+ "\" value=\""+attrvalue+"\">");
                            writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                            writer.newLine();
                            writer.write("<VALUES>" );
                            writer.newLine();
                            for (int k=0;k<options.length; k++) {
                                writer.write("<OPTION value=\""+options[k]+ "\"></OPTION>" );
                            }
                            writer.newLine();
                            writer.write("</VALUES>");
                            writer.newLine();
                            writer.write("</PARAM>");
                            writer.newLine();
                        } else {
                            writer.write("<PARAM ID=\"id"+j+"\" datatype=\""+datatype+"\" name=\""+name+ 
                                    "\" ucd=\""+ucd+ "\" utype=\""+utype+"\" unit=\""+pel[j].getUnit()+ "\" value=\""+attrvalue+"\">");
                            writer.newLine();
                            writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                            writer.newLine();
                            String max = values.getMaximum();
                            String min=values.getMinimum();
                            if (! max.isEmpty() || !min.isEmpty() ) {
                                writer.write("<VALUES>" );
                                writer.newLine();
                                if (!min.isEmpty())
                                    writer.write("<MIN value=\""+min+"\" ></MIN>" );
                                writer.newLine();
                                if (!max.isEmpty())
                                    writer.write("<MAX value=\""+max+"\" ></MAX>" );
                                writer.newLine();
                                writer.write("</VALUES>");
                                writer.newLine();
                                writer.write("</PARAM>");
                                writer.newLine();
                            }
                        }
                    } else {
                        writer.write("<PARAM ID=x datatype=\""+datatype+"\" name=\""+name+ 
                                "\" ucd=\""+ucd+ "\" utype=\""+utype+"\" unit=\""+pel[j].getUnit()+ "\" value=\""+attrvalue+"\"></PARAM>");
                        writer.newLine();
                        writer.write("<DESCRIPTION>"+pel[j].getDescription()+"</DESCRIPTION>");
                        writer.newLine();
                        writer.write("</PARAM>");
                        writer.newLine();
                    }
                } // for j
                writer.write("</GROUP>");
                writer.newLine();
       //         writer.write("<GROUP name=\"inputParams\">");
       //         writer.newLine();
       ////         writer.write("<PARAM arraysize=\"*\" datatype=\"char\" name=\"ID\" ref=\"ssa_pubDID\" ucd=\"meta.id;meta.main\" value=\"\">");
        //        writer.newLine();
        //        writer.write("<LINK content-role=\"ddl:id-source\" value=\"#"+id_source+"\"></LINK></PARAM>");
        //        writer.newLine();
         //       writer.write("</GROUP>");
        //        writer.newLine();
                writer.write("<PARAM arraysize=\"*\" datatype=\"char\" name=\"accessURL\" ucd=\"meta.ref.url\" value=\""+getQueryAccessURL(i)+"\"></PARAM>");
                writer.newLine();
                writer.write("</RESOURCE>");
                writer.newLine();
            }
        } // for i       
      
    }
    
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
        HashMap <String,String> paramMap = null; 
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
        
        protected String getQueryParam( String paramName ) {
             
             for (int i=0;i<groupParams.size(); i++ ) {
                 ParamElement pel = groupParams.get(i);
                 if (pel.getName().equals(paramName))
                     return pel.getValue();
             }
             return null;
        }
        
        protected String getParam(String name) {
            return paramMap.get(name);
        }
        
    }
    
}
