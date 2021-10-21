package uk.ac.starlink.votable.datalink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.NodeList;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.ValuesElement;

/**
 * Contains methods to parse service descriptor elements in a VOTable document.
 *
 * @author   Mark Taylor
 * @since    7 Dec 2017
 * @see   <a href="http://www.ivoa.net/documents/DataLink/"
 *           >DataLink 1.0 or 1.1, Sec 4</a>
 */
public class ServiceDescriptorFactory {

    /**
     * Reads ServiceDescriptor descendants of a given element.
     * These are constructed from any RESOURCE element with
     * with @type="meta" and @utype="adhoc:service".
     *
     * @param  el  container element
     * @return  list of zero or more service descriptors found
     */
    public ServiceDescriptor[] readAllServiceDescriptors( VOElement el ) {
        List<ServiceDescriptor> descriptorList =
            new ArrayList<ServiceDescriptor>();
        for ( VOElement resEl : findServiceDescriptorResources( el ) ) {
            descriptorList.add( createServiceDescriptor( resEl ) );
        }
        return descriptorList.toArray( new ServiceDescriptor[ 0 ] );
    }

    /**
     * Locates all the service descriptors that correspond to a given TABLE
     * element.
     *
     * <p>This is not elegant.
     * We are looking for SDs in the following positions:
     * <pre>
     *   SomeElement
     *     RESOURCE
     *       TABLE
     *     RESOURCE-with-ServiceDescriptor
     * </pre>
     * or
     * <pre>
     *   RESOURCE
     *     TABLE
     *     RESOURCE-with-ServiceDescriptor
     * </pre>
     * but we don't know which.
     * First, find the widest scope within which an SD for this tableEl
     * might be found.  That is the parent of this TABLE's RESOURCE parent.
     * Then, look for all the SDs in this scope, but discount any which
     * have TABLE siblings apart from the supplied tableEl.
     *
     * @param   tableEl  target TABLE element
     * @return  service descriptors corresponding to <code>tableEl</code>
     */
    public ServiceDescriptor[]
            readTableServiceDescriptors( TableElement tableEl ) {

        /* Get the scope.  This is the parent RESOURCE of the parent RESOURCE
         * of the TABLE element, or failing that the top-level Document
         * element itself.  If the supplied table element has any associated
         * service descriptors, they will be descendants of this scope node. */
        VOElement scopeEl =
            getAncestorResource( getAncestorResource( tableEl ) );
        if ( scopeEl == null ) {
            scopeEl = (VOElement) tableEl.getOwnerDocument()
                                         .getDocumentElement();
        }

        /* Locate all the RESOURCE elements in the scope that appear to
         * represent service descriptors. */
        VOElement[] sdResources = findServiceDescriptorResources( scopeEl );

        /* Make service descriptors out of each one *unless* it has a
         * TABLE sibling other than the one we're interested in.
         * If it has the current TABLE as a sibling then it does
         * belong to it; if it has no TABLE siblings then the document
         * is probably arranged so that the RESOURCE parents, rather than
         * the TABLE elements themselves, are siblings of the service
         * descriptors. */
        List<ServiceDescriptor> sdList = new ArrayList<ServiceDescriptor>();
        for ( VOElement sdEl : sdResources ) {
            VOElement parent = sdEl.getParent();
            VOElement[] siblingTables = parent.getChildrenByName( "TABLE" );
            if ( siblingTables == null ||
                 siblingTables.length == 0 ||
                 Arrays.asList( siblingTables ).contains( tableEl ) ) {
                sdList.add( createServiceDescriptor( sdEl ) );
            }
        }
        return sdList.toArray( new ServiceDescriptor[ 0 ] );
    }

    /**
     * Parses a DOM element as a ServiceDescriptor.
     * The supplied element will normally be a RESOURCE with
     * with @type="meta" and utype="adhoc:service".
     *
     * @param  resourceEl  DOM element
     * @return   service descriptor
     */
    public ServiceDescriptor createServiceDescriptor( VOElement resourceEl ) {
        final String descriptorId = resourceEl.hasAttribute( "ID" )
                                  ? resourceEl.getAttribute( "ID" )
                                  : null;
        final String name = resourceEl.hasAttribute( "name" )
                          ? resourceEl.getAttribute( "name" )
                          : null;
        Map<String,String> paramMap = new HashMap<String,String>();
        for ( VOElement pEl : resourceEl.getChildrenByName( "PARAM" ) ) {
            ParamElement paramEl = (ParamElement) pEl;
            Object value = paramEl.getObject();
            if ( value instanceof String ) {
                paramMap.put( paramEl.getName(), (String) value );
            }
        }
        final String accessUrl = paramMap.get( "accessURL" );
        final String standardId = paramMap.get( "standardID" );
        final String resourceIdentifier = paramMap.get( "resourceIdentifier" );
        final String contentType = paramMap.get( "contentType" );
        List<ServiceParam> inParamList = new ArrayList<ServiceParam>();
        for ( VOElement groupEl : resourceEl.getChildrenByName( "GROUP" ) ) {
            if ( "inputParams".equals( groupEl.getName() ) ) {
                for ( VOElement pEl : groupEl.getChildrenByName( "PARAM" ) ) {
                    inParamList.add( createServiceParam( (ParamElement) pEl ) );
                }
            }
        }
        VOElement descripEl = resourceEl.getChildByName( "DESCRIPTION" );
        final String description = descripEl == null
                                 ? null
                                 : DOMUtils.getTextContent( descripEl );
        final ServiceParam[] inputParams =
            inParamList.toArray( new ServiceParam[ 0 ] );
        return new ServiceDescriptor() {
            public String getDescriptorId() {
                return descriptorId;
            }
            public String getAccessUrl() {
                return accessUrl;
            }
            public String getStandardId() {
                return standardId;
            }
            public String getResourceIdentifier() {
                return resourceIdentifier;
            }
            public String getContentType() {
                return contentType;
            }
            public String getName() {
                return name;
            }
            public String getDescription() {
                return description;
            }
            public ServiceParam[] getInputParams() {
                return inputParams;
            }
        };
    }

    /**
     * Parses a VOTable DOM element as a ServiceParam.
     * The supplied PARAM element will normally be a child of a
     * GROUP element with @name="inputParams".
     *
     * @param  paramEl  PARAM element
     * @return  serviceParam  object
     */
    public ServiceParam createServiceParam( ParamElement paramEl ) {
        final String name = paramEl.getName();
        String v = paramEl.getValue();
        final String value = v == null || v.trim().length() == 0
                           ? null
                           : paramEl.getValue();
        final String id = paramEl.getID();
        final String ref = paramEl.hasAttribute( "ref" )
                         ? paramEl.getAttribute( "ref" )
                         : null;
        final String datatype = paramEl.getDatatype();
        final String unit = paramEl.getUnit();
        final String ucd = paramEl.getUcd();
        final String utype = paramEl.getUtype();
        final String xtype = paramEl.getXtype();
        final String description = paramEl.getDescription();
        final int[] arraysize = longsToInts( paramEl.getArraysize() );
        ValuesElement valuesEl =
            (ValuesElement) paramEl.getChildByName( "VALUES" );
        final String[] minmax = valuesEl == null
                              ? null
                              : new String[] { valuesEl.getMinimum(),
                                               valuesEl.getMaximum() };
        final String[] options = valuesEl == null
                               ? null
                               : valuesEl.getOptions();
        return new ServiceParam() {
            public String getName() {
                return name;
            }
            public String getValue() {
                return value;
            }
            public String getId() {
                return id;
            }
            public String getRef() {
                return ref;
            }
            public String getDatatype() {
                return datatype;
            }
            public String getUnit() {
                return unit;
            }
            public String getUcd() {
                return ucd;
            }
            public String getUtype() {
                return utype;
            }
            public String getXtype() {
                return xtype;
            }
            public String getDescription() {
                return description;
            }
            public int[] getArraysize() {
                return arraysize;
            }
            public String[] getMinMax() {
                return minmax;
            }
            public String[] getOptions() {
                return options;
            }
        };
    }

    /**
     * Finds all the RESOURCE elements descendant from the given element
     * that appear to represent Service Descriptors.
     *
     * @param  el  ancestor element (not included in result)
     * @return  descendant RESOURCEs with type="meta" and utype="adhoc:service"
     */
    private VOElement[] findServiceDescriptorResources( VOElement el ) {
        List<VOElement> resList = new ArrayList<VOElement>();
        NodeList resourceList = el.getElementsByVOTagName( "RESOURCE" );
        for ( int i = 0; i < resourceList.getLength(); i++ ) {
            VOElement resEl = (VOElement) resourceList.item( i );
            if ( "meta".equals( resEl.getAttribute( "type" ) ) &&
                 "adhoc:service".equals( resEl.getAttribute( "utype" ) ) ) {
                resList.add( resEl );
            }
        }
        return resList.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns the closest ancestor of the given element that is a RESOURCE,
     * or null if it has no RESOURCE ancestors.
     *
     * @param   el   target element
     * @return   closest RESOURCE ancestor, or null
     */
    private VOElement getAncestorResource( VOElement el ) {
        if ( el != null ) {
            do {
               el = el.getParent();
            } while ( el != null && ! "RESOURCE".equals( el.getVOTagName() ) );
        }
        return el;
    }

    /**
     * Converts a long[] array to an int[] array.  Uses simple casting.
     *
     * @param  larray  input array
     * @return  converted array
     */
    private static int[] longsToInts( long[] larray ) {
        if ( larray == null ) {
            return null;
        }
        else {
            int[] iarray = new int[ larray.length ];
            for ( int i = 0; i < larray.length; i++ ) {
                iarray[ i ] = (int) larray[ i ];
            }
            return iarray;
        }
    }
}
