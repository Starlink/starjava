
package uk.ac.starlink.splat.vo;

import uk.ac.starlink.vo.RegCapabilityInterface;

/**
 *  Concrete implementation of {@link RegCapabilityInterface} that
 *  allows mutability and bean access.
 */
public class SSAPRegCapability 
    implements RegCapabilityInterface
{
    private String accessUrl;
    private String standardId;
    private String description;
    private String version;
    private String xsiType;

    /**
     * Constructor.
     */
    public SSAPRegCapability()
    {
        //  Do nothing.
    }

    /**
     * Constructor with settings from a {@link RegCapabilityInterface}
     * instance.
     */
    public SSAPRegCapability( RegCapabilityInterface rci )
    {
        accessUrl = rci.getAccessUrl();
        standardId= rci.getStandardId();
        description = rci.getDescription();
        version = rci.getVersion();
        xsiType = rci.getXsiType();
    }

    public String getAccessUrl()
    {
        return accessUrl;
    }

    public void setAccessUrl( String accessUrl )
    {
        this. accessUrl = accessUrl;;
    }

    public String getStandardId()
    {
        return standardId;
    }

    public void setStandardId( String standardId )
    {
        this.standardId = standardId;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getVersion()
    {
        return version;
    }
    
    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getXsiType()
    {
        return xsiType;
    }

    public void setXsiType( String xsiType )
    {
        this.xsiType = xsiType;
    }
}
