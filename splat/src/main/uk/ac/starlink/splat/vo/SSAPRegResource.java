package uk.ac.starlink.splat.vo;

import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;

/**
 * {@link RegResource} implementation that offers mutability and bean access.
 *
 * @author   Peter W. Draper
 * @since    22 Dec 2008
 */
public class SSAPRegResource
//    implements RegResource
{
    private String shortName;
    private String title;
    private String identifier;
    private String publisher;
    private String contact;
    private String referenceUrl;
    private String version;
    private SSAPRegCapability[] capabilities;
    private String[] subjects = null;
    private String [] waveband = null;

    /**
     * Constructor.
     */
    public SSAPRegResource()
    {
        //  Do nothing.
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
        //  Need a copy of each capability.
        SSAPRegCapability[] rci = (SSAPRegCapability[]) resource.getCapabilities();
        capabilities = new SSAPRegCapability[rci.length];
        for ( int i = 0; i < rci.length; i++ ) {
            capabilities[i] = new SSAPRegCapability( rci[i] );
        }
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
        return capabilities = capabilities;
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

   
    public String getVersion() 
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

  
}
