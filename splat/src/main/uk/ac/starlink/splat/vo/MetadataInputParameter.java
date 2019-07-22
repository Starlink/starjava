package uk.ac.starlink.splat.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.starlink.votable.ParamElement;

/**
 * The MetadataInputParameter class contains the parameter element and the services that support this parameter
 */

public class MetadataInputParameter //implements java.io.Serializable
{
    
    private static final long serialVersionUID = 1L;
//      ParamElement param;
    String name = null;
    
    String description = null;
    
    String unit = null;
    String UCD = null;
    String value = null;
    String nullValue = null;
//    String[] options;
    //ValuesElement values = null;
    SupportingServers servers=null;
    //List <String> servers=null;
    String datatype = null;
 //   String min=null;    
 //   String max=null;
    Boolean checked=false; // selected by user on GUI
   
   public MetadataInputParameter() {
       servers = new SupportingServers();
   }

    public MetadataInputParameter(ParamElement param, String shortName) {
        
        name = param.getName();
        description = param.getDescription();
        if (description == null)
            description = "";
        datatype = param.getDatatype();
        value = param.getValue();
        servers = new SupportingServers();
        servers.addServer(shortName);
        
        unit = param.getUnit();
        
        UCD = param.getUcd();
        this.checked=false;
        
        /* To be done later after some questions have been cleared:
         * 
         * 
        ValuesElement values = param.getActualValues(); // or legal values??
        if  (values != null) {
                options = values.getOptions();
                max = values.getMaximum();
                min = values.getMinimum();
                nullValue=values.getNull();
        }
         */             
    }
 
    public MetadataInputParameter(MetadataInputParameter mip) {
        this.name = mip.getName();
        this.description = mip.getDescription() ;
        this.unit = mip.getUnit() ;
        this.UCD = mip.getUCD() ;
        this.value = mip.getValue() ;
        this.nullValue = null ;
        this.servers = new SupportingServers();
        setServers( mip.getServers() );
        this.datatype = mip.getDatatype();
 //       this.min= null;    
 //       this.max= null;     
        
    }
    
    public MetadataInputParameter(MetadataInputParameter mip, boolean check) {
        this(mip);
        this.checked=check;
    }
   
    public String getName() {
        return name;
    }
    /**
     * @return
     * @uml.property  name="value"
     */
    public String getValue() {
        return value;
    }
    /**
     * @return
     * @uml.property  name="description"
     */
    public String getDescription() {
        return description;
    }
    /**
     * @return
     * @uml.property  name="datatype"
     */
    public String getDatatype() {
        return datatype;
    }
    /**
     * @return
     * @uml.property  name="unit"
     */
    public String getUnit() {
        return unit;
    }
    
    
    /**
     * @return
     * @uml.property  name="UCD"
     */
    public String getUCD() {
        return UCD;
    }
    
    
    public List<String> getServers() {
       return servers.getServers();
    }
    public void setServers(List<String> s) {
        servers.setServers(s);
        
    }
  
    public void setName(String paramName) {
        this.name = paramName;
   }
   
    public void setDescription(String description) {
         this.description = description;
    }
    
    public void setValue(String value) {
     this.value = value;
    }
    
    public void setUCD(String ucd) {
         this.UCD = ucd;
    }
 
    public boolean isChecked() {
        return this.checked;
   }
    public void setChecked(boolean check) {
        this.checked=check;
   }


    public class SupportingServers {
        List<String> servers;
        public SupportingServers(){
            servers = new ArrayList<String>();
        }
   
        public void addServer(String name) {
            servers.add(name);
        }
        public void setServers(List<String> s) {
            servers = new ArrayList<String>();
            if (s != null)
                servers.addAll(s);
        }
        public List<String> getServers() {
            return servers;
        }
    }
    
    
} // MetadataInputParameter

