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
    implements RegResource
{
    private String shortName;
    private String title;
    private String identifier;
    private String publisher;
    private String contact;
    private String referenceUrl;
    private RegCapabilityInterface[] capabilities;
    private String[] subjects = null;

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
    public SSAPRegResource( RegResource resource )
    {
        shortName = resource.getShortName();
        title = resource.getTitle();
        identifier = resource.getIdentifier();
        publisher = resource.getPublisher();
        contact = resource.getContact();
        referenceUrl = resource.getReferenceUrl();
        subjects = resource.getSubjects();

        //  Need a copy of each capability.
        RegCapabilityInterface[] rci = resource.getCapabilities();
        capabilities = new SSAPRegCapability[rci.length];
        for ( int i = 0; i < rci.length; i++ ) {
            capabilities[i] = new SSAPRegCapability( rci[i] );
        }
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

    public RegCapabilityInterface[] getCapabilities()
    {
        return capabilities = capabilities;
    }

    public void setCapabilities( RegCapabilityInterface[] capabilities )
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
}
