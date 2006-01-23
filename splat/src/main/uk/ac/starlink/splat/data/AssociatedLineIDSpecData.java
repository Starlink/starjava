/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     19-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import uk.ac.starlink.splat.util.SplatException;

/**
 * A type of {@link LineIDSpecData} that adds an addional column for
 * associating each line with a spectrum. The column contains a spectral
 * specification that can be understood by the usual factory creation
 * methods.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AssociatedLineIDSpecData
    extends LineIDSpecData
    implements Serializable
{
    /**
     * Serialization version ID string.
     */
    static final long serialVersionUID = -8002304252380814451L;

    /**
     * Serialised form of the associtions, usually null.
     */
    protected String[] serializedAssociations = null;

    /**
     * Constructor, takes an AssociatedLineIDSpecDataImpl.
     */
    public AssociatedLineIDSpecData( AssociatedLineIDSpecDataImpl lineIDImpl )
        throws SplatException
    {
        super( lineIDImpl );
    }

    /**
     * Get the associated file specifications. These may be null.
     */
    public String[] getAssociations()
    {
        if ( impl != null && impl instanceof AssociatedLineIDSpecDataImpl ) {
            return ((AssociatedLineIDSpecDataImpl)impl).getAssociations();
        }
        return null;
    }

    /**
     * Set the association specifications.
     */
    public void setAssociations( String[] associations )
        throws SplatException
    {
        if ( impl != null && impl instanceof AssociatedLineIDSpecDataImpl ) {
            ((AssociatedLineIDSpecDataImpl)impl)
                .setAssociations( associations );
        }
    }

    /**
     * Set a specific association.
     */
    public void setAssociations( int index, String association )
    {
        if ( impl != null && impl instanceof AssociatedLineIDSpecDataImpl ) {
            ((AssociatedLineIDSpecDataImpl)impl)
                .setAssociation( index, association );
        }
    }

    /**
     * Return if the backing implementation has valid associations.
     */
    public boolean haveAssociations()
    {
        if ( impl != null && impl instanceof AssociatedLineIDSpecDataImpl ) {
            return ((AssociatedLineIDSpecDataImpl)impl).haveAssociations();
        }
        return false;
    }

//
//  Serializable interface.
//
//  Note: private signature required, so methods repeated at each level.

    private void writeObject( ObjectOutputStream out )
        throws IOException
    {
        //  Need to write out persistent associations.
        serializedAssociations = getAssociations();

        //  Need to write out persistent data labels.
        serializedLabels = getLabels();

        //  And store all member variables.
        out.defaultWriteObject();

        //  Finished.
        serializedLabels = null;
        serializedAssociations = null;
    }

    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        //  Note we use this method as we need a different impl object from
        //  SpecData and need to restore the data labels and associations.
        try {
            // Restore state of member variables.
            in.defaultReadObject();

            //  Create the backing impl, this will supercede one created by
            //  the SpecData readObject.
            LineIDMEMSpecDataImpl newImpl =
                new LineIDMEMSpecDataImpl( shortName, this );
            fullName = null;

            //  Restore data labels.
            if ( serializedLabels != null ) {
                newImpl.setLabels( serializedLabels );
                serializedLabels = null;
            }

            //  Restore associations.
            if ( serializedAssociations != null ) {
                ((AssociatedLineIDSpecDataImpl)impl)
                    .setAssociations( serializedAssociations );
                serializedAssociations = null;
            }
            this.impl = newImpl;

            //  Full reset of state.
            readData();
            setRange();
        }
        catch ( SplatException e ) {
            e.printStackTrace();
        }





    }
}
