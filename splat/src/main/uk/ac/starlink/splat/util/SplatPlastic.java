/*
 * Copyright (C) 2006 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-AUG-2006 (Mark Taylor):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;

import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.SpectrumIO;

/**
 * Implements the PlasticListener interface on behalf of the SPLAT
 * application.
 *
 * @author   Mark Taylor
 * @version $Id$
 */
public class SplatPlastic
     extends HubManager
{

    /**
     * List of application-specific PLASTIC messages supported by this class.
     */
    protected static final URI[] SUPPORTED_MESSAGES = new URI[] {
        MessageId.SPECTRUM_LOADURL,
        MessageId.FITS_LOADLINE,
        MessageId.INFO_GETDESCRIPTION,
        MessageId.INFO_GETICONURL,
    };

    /**
     * PlasticCommunicator object associated with this.
     */
    private PlasticCommunicator communicator;

    /**
     * Model controlling whether spectra received as spectrum messages are
     * accepted.
     */
    protected ButtonModel acceptSpectrumModel = new DefaultButtonModel();

    /**
     * Model controlling whether spectra received as FITS lines are accepted.
     */
    protected ButtonModel acceptFITSLineModel = new DefaultButtonModel();

    /**
     * Create a new plastic listener for SPLAT.
     */
    public SplatPlastic( PlasticCommunicator communicator )
    {
        super( "splat-vo", SUPPORTED_MESSAGES );
        this.communicator = communicator;
        acceptSpectrumModel.setSelected( true );
        acceptFITSLineModel.setSelected( true );
    }

    /**
     * Does the work for processing a request from the hub.
     *
     * @param   sender  sender ID
     * @param   message message ID
     * @param   args    message argument list
     * @return  return value requested by message
     */
    public Object doPerform( URI sender, URI message, List args )
        throws IOException
    {
        //  Return a description of this application.
        if ( MessageId.INFO_GETDESCRIPTION.equals( message ) ) {
            return Utilities.getFullDescription();
        }

        //  Return the URL of the SPLAT icon.
        else if ( MessageId.INFO_GETICONURL.equals( message ) ) {
            return "http://star-www.dur.ac.uk/~pdraper/splat/splat.gif";
        }

        //  Load a spectrum from a spectrum message.
        else if ( MessageId.SPECTRUM_LOADURL.equals( message ) &&
                  checkArgs( args, new Class[] { String.class, String.class,
                                                 Map.class, } ) ) {
            if ( acceptSpectrumModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                String id = args.get( 1 ).toString();
                Map meta = (Map) args.get( 2 );
                SpectrumIO.Props props = getProps( location, meta );
                return Boolean.valueOf( communicator.addSpectrum( props ) );
            }
            else {
                return Boolean.FALSE;
            }
        }

        //  Load a spectrum from a FITS 1-d array.
        else if ( MessageId.FITS_LOADLINE.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            if ( acceptFITSLineModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                return Boolean.valueOf( communicator
                                       .addSpectrum( location,
                                                     SpecDataFactory.FITS ) );
            }
            else {
                return Boolean.FALSE;
            }
        }

        //  Unknown message (the superclass shouldn't hand one of these down).
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets the button model which determines whether incoming messages
     * requesting load of a spectrum as such will be acted on or ignored.
     *
     * @param model  button model
     */
    public void setAcceptSpectrumModel( ButtonModel model )
    {
        acceptSpectrumModel = model;
    }

    /**
     * Sets the button model which determines whether incoming messages
     * requesting load of a 1-d FITS file will be acted on or ignored.
     *
     * @param model  button model
     */
    public void setAcceptFITSLineModel( ButtonModel model )
    {
        acceptFITSLineModel = model;
    }

    /**
     * Convert a spectrum specification (a URL and Map of SSAP metadata) into
     * a SpectrumIO.Prop instance.
     *
     * @param location URL of spectrum.
     * @param meta a key-value map of SSAP metadata that describes the
     *             spectrum to be accessed.
     */
    public static SpectrumIO.Props getProps( String location, Map meta )
    {
        SpectrumIO.Props props = new SpectrumIO.Props( location );
        if ( meta != null && meta.size() > 0 ) {
            SpecDataFactory specDataFactory = SpecDataFactory.getInstance();
            Set keys = meta.keySet();
            Iterator i = keys.iterator();
            String key;
            String value;
            String axes[];
            String units[];
            while( i.hasNext() ) {
                key = (String) i.next();
                value = String.valueOf( meta.get( key ) );
                key = key.toLowerCase();

                //  UTYPEs and UCDs, maybe UTYPES should be sdm:ssa.xxx.
                //  Many of the SSAP response UTYPEs don't seem documented yet.
                if ( key.equals( "vox:spectrum_format" ) ||
                     utypeMatches( key, "access.format" ) ) {
                    props.setType( specDataFactory.mimeToSPLATType( value ) );
                }
                else if ( key.equals( "vox:image_title" ) ||
                          utypeMatches( key, "target.name" ) ) {
                    props.setShortName( value );
                }
                else if ( key.equals( "vox:spectrum_axes" ) ) {
                    axes = value.split( "\\s" );
                    if ( axes.length > 0  ) {
                        props.setCoordColumn( axes[0] );
                        if ( axes.length > 1 ) {
                            props.setDataColumn( axes[1] );
                            if ( axes.length == 3 ) {
                                props.setErrorColumn( axes[2] );
                            }
                        }
                    }
                }
                else if ( utypeMatches( key, "Dataset.SpectralAxis" ) ) {
                    props.setCoordColumn( value );
                }
                else if ( utypeMatches( key, "Dataset.FluxAxis" ) ) {
                    props.setDataColumn( value );
                }
                else if ( key.equals( "vox:spectrum_units" ) ) {
                    units = value.split("\\s");
                    if ( units.length > 0  ) {
                        props.setCoordUnits( units[0] );
                        if ( units.length > 1 ) {
                            props.setDataUnits( units[1] );
                        }
                    }
                }
            }
        }
        return props;
    }

    /**
     * Determines whether a given map key corresponds to a utype string.
     *
     * @param   key  provided map key
     * @param   utype  UType to test against, without namespacing
     * @return  true iff they appear to match
     */
    private static boolean utypeMatches( String key, String utype )
    {
        // Not sure what the correct utype namespacing is, if anything;
        // be liberal about that, and about case sensitivity, for now.
        if ( key == null ) {
            return false;
        }
        String lKey = key.toLowerCase();
        String lUtype = utype.toLowerCase();
        return lKey.equals( lUtype )
            || lKey.endsWith( ":" + lUtype );
    }
}
