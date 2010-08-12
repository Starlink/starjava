package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.List;
import net.ivoa.registry.search.VOResource;
import net.ivoa.registry.search.Metadata;

/**
 * RegResource implementation based on a <code>VOResource</code> object.
 *
 * @author   Mark Taylor
 * @since    17 Dec 2008
 */
public class VORegResource implements RegResource {

    private final String shortName_;
    private final String title_;
    private final String identifier_;
    private final String publisher_;
    private final String contact_;
    private final String[] subjects_;
    private final String referenceUrl_;
    private final RegCapabilityInterface[] capabilities_;

    /**
     * Constructor.
     *
     * @param   resource   resource object
     */
    public VORegResource( VOResource resource ) {
        shortName_ = resource.getParameter( "shortName" );
        title_ = resource.getParameter( "title" );
        identifier_ = resource.getParameter( "identifier" );
        publisher_ = resource.getParameter( "curation/publisher" );
        String contactName = resource.getParameter( "curation/contact/name" );
        String contactEmail = resource.getParameter( "curation/contact/email" );
        if ( contactEmail != null && contactEmail.trim().length() > 0 &&
             contactName != null && contactName.trim().length() > 0 ) {
            contact_ = contactName + " <" + contactEmail + ">";
        }
        else if ( contactEmail != null && contactEmail.trim().length() > 0 ) {
            contact_ = contactEmail;
        }
        else if ( contactName != null && contactName.trim().length() > 0 ) {
            contact_ = contactName;
        }
        else {
            contact_ = null;
        }
        subjects_ = resource.getParameters( "content/subject" );
        referenceUrl_ = resource.getParameter( "content/referenceURL" );
        Metadata[] capBlocks = resource.getBlocks( "capability" );
        List capList = new ArrayList();
        for ( int ic = 0; ic < capBlocks.length; ic++ ) {
            Metadata capBlock = capBlocks[ ic ];
            final String standardId = capBlock.getParameter( "@standardID" );
            final String xsiType = capBlock.getParameter( "@xsi:type" );
            final String description = capBlock.getParameter( "description" );
            Metadata[] intfs = capBlock.getBlocks( "interface" );
            for ( int ii = 0; ii < intfs.length; ii++ ) {
                Metadata intf = intfs[ ii ];
                final String accessUrl = intf.getParameter( "accessURL" );
                final String version = intf.getParameter( "@version" );
                final String role = intf.getParameter( "@role" );
                RegCapabilityInterface rci = new RegCapabilityInterface() {
                    public String getAccessUrl() {
                        return accessUrl;
                    }
                    public String getStandardId() {
                        return standardId;
                    }
                    public String getXsiType() {
                        return xsiType;
                    }
                    public String getDescription() {
                        return description; 
                    }
                    public String getVersion() {
                        return version;
                    }
                };
                capList.add( "std".equals( role ) ? 0 : capList.size(),
                             rci );
            }
        }
        capabilities_ = (RegCapabilityInterface[])
                        capList.toArray( new RegCapabilityInterface[ 0 ] );
    }

    public String getShortName() {
        return shortName_;
    }

    public String getTitle() {
        return title_;
    }

    public String getIdentifier() {
        return identifier_;
    }

    public String getPublisher() {
        return publisher_;
    }

    public String getContact() {
        return contact_;
    }

    public String[] getSubjects() {
        return subjects_;
    }

    public String getReferenceUrl() {
        return referenceUrl_;
    }

    public RegCapabilityInterface[] getCapabilities() {
        return capabilities_;
    }
}
