package uk.ac.starlink.splat.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.votable.ParamElement;

/**
 * {@link RegResource} implementation that offers mutability and bean access.
 *
 * @author   Peter W. Draper
 * @since    22 Dec 2008
 */
public class SSAPRegResource implements RegResource
{
    private String shortName;
    private String title;
    private String identifier;
    private String publisher;
    private String contact;
    private String referenceUrl;
    private String version;
    private String contentType;
    private String tableName;
    private MetadataParams metadata = null;
    private SSAPRegCapability[] capabilities;
    private String[] subjects = null;
    private String [] waveband = null;
     
    /**
     * Constructor.
     */
    public SSAPRegResource()
    {
        metadata=new MetadataParams();
    }

    /**
     * Constructor. Initialised from a {@link RegResource}.
     *
     * @param   resource   resource object
     */
    public SSAPRegResource( SSAPRegResource resource )
    {
        shortName = resource.getShortName();
        title = resource.getTitle();
        identifier = resource.getIdentifier();
        publisher = resource.getPublisher();
        contact = resource.getContact();
        referenceUrl = resource.getReferenceUrl();
        subjects = resource.getSubjects();
        version = resource.getVersion();
        waveband = resource.getWaveband();
        contentType = resource.getContentType();
        tableName = resource.getTableName();
        
        metadata = new MetadataParams();
        metadata.setParams(resource.getMetadata() );
        
        
        
        //  Need a copy of each capability.
        SSAPRegCapability[] rci = (SSAPRegCapability[]) resource.getCapabilities();
        capabilities = new SSAPRegCapability[rci.length];
        for ( int i = 0; i < rci.length; i++ ) {
            capabilities[i] = new SSAPRegCapability( rci[i] );
        }
        // for backwards compatibility
        if ((contentType == null || contentType.isEmpty()) && capabilities.length >0)
            contentType = capabilities[0].getDataType();
        
    }

    /**
     * Constructor. Initialised from a {@link AddNewServerFrame}, allows
     * manual insertion of a server}.
     *
     * @param   newshortName     resource short name
     * @param   newTitle         resource title
     * @param   newDescription   capability description
     * @param   newAccessUrl     capability access url
     */
    public SSAPRegResource( String newShortName, String newTitle, String newDescription, String newAccessUrl )
    {
        setShortName(newShortName);
        setTitle(newTitle);
        capabilities = new SSAPRegCapability[1];
        capabilities[0] = new SSAPRegCapability( newDescription, newAccessUrl );
        metadata=new MetadataParams();
    }

    /**
     * Constructor. Initialised from a {@link AddNewServerFrame}, allows
     * manual insertion of a server}.
     *
     * @param   newshortName     resource short name
     * @param   newTitle         resource title
     * @param   newDescription   capability description
     * @param   newAccessUrl     capability access url
     * @param   newWaveBandl     wave band
     * @param   newDataSource    data source
     */
    public SSAPRegResource( String newShortName, String newTitle, String newDescription, String newAccessUrl, String[] newWaveBand, String newDataSource )
    {
        setShortName(newShortName);
        setTitle(newTitle);
        setWaveband(newWaveBand);
        capabilities = new SSAPRegCapability[1];
        capabilities[0] = new SSAPRegCapability( newDescription, newAccessUrl, newDataSource );
        metadata = new MetadataParams();
    }

    public String getShortName()
    {
        return shortName;
    }

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( String identifier )
    {
        this.identifier = identifier;
    }

    public String getPublisher()
    {
        return publisher;
    }

    public void setPublisher( String publisher )
    {
        this.publisher = publisher;
    }

    public String getContact()
    {
        return contact;
    }

    public void setContact( String contact )
    {
        this.contact = contact;
    }

    public String getReferenceUrl()
    {
        return referenceUrl;
    }

    public void setReferenceUrl( String referenceUrl )
    {
        this.referenceUrl = referenceUrl;
    }

    public SSAPRegCapability[] getCapabilities()
    {
        return capabilities;
    }

    public void setCapabilities( SSAPRegCapability[] capabilities )
    {
        this.capabilities = capabilities;
    }

    public String[] getSubjects() 
    {
        return subjects;
    }
    public void setSubjects( String[] subjects )
    {
        this.subjects = subjects;
    }

    public String[] getWaveband() 
    {
        return waveband;
    }
    
    public void setWaveband( String[] waveband )
    {
        this.waveband = waveband;
    }

    public String getTableName() 
    {
        return tableName;
    }
    
    public void setTableName( String tableName )
    {
        this.tableName = tableName;
    }

   
    public String getVersion() 
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }
    public String getContentType() 
    {
        return contentType;
    }

    public void setContentType( String contentType )
    {
        this.contentType = contentType;
    }
    
    public List<MetadataInputParameter> getMetadata() 
    {
        return metadata.getParams();
    }

    public void setMetadata(List<MetadataInputParameter> mips) {
        if (mips != null)
            metadata.setParams(mips);        
    }
    
             
 
    public class MetadataParams  {
        
        List<MetadataInputParameter>  metadata_=null;
        
       public  MetadataParams() {
            metadata_ = new ArrayList<MetadataInputParameter>();
        }
              
        protected void addParam(MetadataInputParameter mip) {
            metadata_.add(mip);
        }
        protected void setParams(List<MetadataInputParameter> list) {
            if (list != null) {
                metadata_= new ArrayList<MetadataInputParameter>();
                metadata_.addAll(list);
            } else
                metadata=null;
        }
        protected List<MetadataInputParameter> getParams() {
            return metadata_;
        }
        
    }



	public void setAccessUrl(String accessUrl) {
		capabilities = new SSAPRegCapability[1];
		capabilities[0].setAccessUrl(accessUrl);
		
	}
}
