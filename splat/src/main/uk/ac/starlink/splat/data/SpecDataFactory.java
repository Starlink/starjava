/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *        Original version.
 *     01-MAR-2004 (Peter W. Draper):
 *        Added table handling changes.
 *     26-AUG-2004 (Peter W. Draper):
 *        Added support for URLs.
 *     08-OCT-2004 (Peter W. Draper):
 *        Added support for collapsing and extracting spectra from
 *        2 and 3D data.
 */
package uk.ac.starlink.splat.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.MimeType;




import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import uk.ac.starlink.splat.imagedata.NDFJ;
import uk.ac.starlink.splat.util.SEDSplatException;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.SSAPAuthenticator;


import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.splat.iface.LocalLineIDManager;
import uk.ac.starlink.splat.imagedata.NDFJ;
import uk.ac.starlink.splat.util.ConstrainedList;
import uk.ac.starlink.splat.util.ConstrainedList.ConstraintType;
import uk.ac.starlink.splat.util.SEDSplatException;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.SSAPAuthenticator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.TemporaryFileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;


/**
 * This class creates and clones instances of SpecData and derived
 * classes. The type of the spectrum supplied is determined either by
 * heuristics based on the specification, or by a given, known, type.
 * <p>
 * The known types are identified by public variables or class type and are
 * described using short and long names.
 * <p>
 * If an untyped specification is given then the usual the file name
 * plus extension and any qualifiers (such as HDS object path, or FITS
 * extension number). This is parsed and identified by the InputNameParser
 * class.
 * <p>
 * EditableSpecData and LineIDSpecData instances can also be created
 * and copied.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecData
 * @see InputNameParser
 */
public class SpecDataFactory
{
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.data.SpecDataFactory" );

    //
    // Enumeration of the "types" of spectrum that can be created.
    //

    /** This type should be given to spectra with a defined type which is not supported */
    public final static int NOT_SUPPORTED = -1;
    
    /** The type should be determined only using file name rules */
    public final static int DEFAULT = 0;

    /** FITS source. */
    public final static int FITS = 1;

    /** HDS file. */
    public final static int HDS = 2;

    /** TEXT file. */
    public final static int TEXT = 3;

    /** HDX source. */
    public final static int HDX = 4;

    /** Table of unknown type. */
    public final static int TABLE = 5;

    /** Line identifier file. */
    public final static int IDS = 6;

    /** The type should be determined using the DataNode guessing mechanisms.
     *  One problem with this is that remote resources require downloading
     *  first. */
    public final static int GUESS = 7;

    /** VOTable SED source, XXX not yet a proper type */
    public final static int SED = 8;
    
    /** VOTable Datalink source */
    public final static int DATALINK = 9;


    /**
     * Short descriptions of each type.
     */
    public final static String[] shortNames = {
       // "notsupported",
        "default",
        "fits",
        "hds",
        "text",
        "hdx",
        "table",
        "line ids",
        "guess",
   
    };

    /**
     * Long descriptions of each type.
     */
    public final static String[] longNames = {
      //  "Not supported format",
        "File extension rule",
        "FITS file (spectrum/table)",
        "HDS container file",
        "TEXT files",
        "HDX/NDX/VOTable XML files",
        "Table",
        "Line identification files"
    };

    /**
     * File extensions for each type.
     */
    public final static String[][] extensions = {
  //      {""}, //
        {"*"},
        {"fits", "fit"},
        {"sdf"},
        {"txt", "lis"},
        {"xml"}, //"vot"}, // changed MCN 
        {"*"},
        {"ids"},
  //      {"*"},
  //      {"vot"}
    };

    /**
     * Our DataNodeFactory.
     */
    private DataNodeFactory dataNodeFactory = null;

    /**
     * Datanode icons symbolic names for each data type. XXX may want
     * to extend this with own (line identifiers clearly not available).
     */
    public final static short[] datanodeIcons = {
        IconFactory.FILE,
        IconFactory.FITS,
        IconFactory.NDF,
        IconFactory.DATA,
        IconFactory.NDX,
        IconFactory.TABLE,
        IconFactory.ARY2
    };

    /**
     * Policy used for table backing store.
     */
    private static final StoragePolicy storagePolicy =
        StoragePolicy.getDefaultPolicy();

    /**
     *  Create the single class instance.
     */
    private static SpecDataFactory instance = null;
    
    /** the authenticator for access control **/
    private SSAPAuthenticator authenticator;
    
    /**
     *  Hide the constructor from use.
     */
    private SpecDataFactory() {}

    /**
     *  Return reference to the only allowed instance of this class.
     *
     *  @return reference to only instance of this class.
     */
    public static SpecDataFactory getInstance()
    {
        if ( instance == null ) {
            instance = new SpecDataFactory();
        }
        return instance;
    }

    /**
     *  Attempt to open a given specification as a known type. If this
     *  fails then a SplatException will be thrown. Note that if the
     *  type is DEFAULT then this is equivalent to calling
     *  {@link #get(String)}.
     *
     *  @param specspec the specification of the spectrum to be opened.
     *  @param type the type of the spectrum (one of types defined in
     *              this class).
     *  @return the list of SpecData objects created from the given
     *          specification.
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public List<SpecData> get( String specspec, int type ) 
    		throws SplatException {
    	List<SpecData> spectra = getAll(specspec, type);
    	
    	if (spectra != null && !spectra.isEmpty())
    	//	return spectra.get(0);
    	    return spectra;
    	else
    		return null;
    }
    
    /**
     *  Attempt to open a given specification as a known type. If this
     *  fails then a SplatException will be thrown. Note that if the
     *  type is DEFAULT then this is equivalent to calling
     *  {@link #get(String)}.
     *
     *  @param specspec the specification of the spectrum to be opened.
     *  @param type the type of the spectrum (one of types defined in
     *              this class).
     *  @return the first SpecData object created from the given
     *          specification.
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public SpecData get1( String specspec, int type ) 
            throws SplatException {
        List<SpecData> spectra = getAll(specspec, type);
        
        if (spectra != null && !spectra.isEmpty())
          return spectra.get(0);
           // return spectra;
        else
            return null;
    }
    
    /**
     *  Attempt to open a given specification as a known type. If this
     *  fails then a SplatException will be thrown. Note that if the
     *  type is DEFAULT then this is equivalent to calling
     *  {@link #get(String)}.
     *
     *  @param specspec the specification of the spectrum to be opened.
     *  @param type the type of the spectrum (one of types defined in
     *              this class).
     *  @return the List of SpecData objects created from the given
     *          specification.
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */

    public List<SpecData> getAll( String specspec, int type )
        throws SplatException
    {
    	List<SpecData> specDataList = new ConstrainedList<SpecData>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
    	//SpecData specData;

    	//  Type established by file name rules.
        if ( type == DEFAULT ) {
            return getAll( specspec );
          
        }

        //  The specification could be for a local or remote file and in URL
        //  or local file format. We need to know so that we can make a local
        //  copy and construct a RemoteSpecData. Note that when we're using
        //  the full guessing mechanisms remote resources are always
        //  downloaded by SPLAT.
        
        NameParser namer = new NameParser( specspec );
  
        boolean isRemote = namer.isRemote();
        if ( isRemote ) {
                
             if ( ( type != TABLE && type != HDX ) || ( type == GUESS ) ) {
                PathParser pathParser = remoteToLocalFile( namer.getURL(), type );
                specspec = pathParser.ndfname();
             }
        } 

        if ( type != GUESS ) {
            //SpecDataImpl impl = null;
        	boolean added = true;
            List<SpecDataImpl> impls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
            switch (type)
            {
                case FITS: {
                	added = impls.addAll(makeFITSSpecDataImplList(specspec));
                    //impl = makeFITSSpecDataImpl( specspec );
                }
                    break;
                case HDS: {
                	added = impls.add(makeNDFSpecDataImpl( namer.getName() ));
                    //impl = makeNDFSpecDataImpl( namer.getName() );
                }
                    break;
                case TEXT: {
                	added = impls.add(new TXTSpecDataImpl( specspec ));
                	//impl = new TXTSpecDataImpl( specspec );
                }
                    break;
                case HDX: {
                    //  HDX should download remote files as it needs to keep
                    //  the basename to locate other references.
                    if ( namer.isRemote() ) {
                    	added = impls.add(new NDXSpecDataImpl( namer.getURL() ));
                    	//impl = new NDXSpecDataImpl( namer.getURL() );
                    }
                    else {
                    	added = impls.add(new NDXSpecDataImpl( specspec ));
                    	//impl = new NDXSpecDataImpl( specspec );
                    }
                }
                    break;
                case DATALINK: 
                case TABLE: {
                	added = impls.add(makeTableSpecDataImpl( specspec ));
                	//impl = makeTableSpecDataImpl( specspec );

                }
                    break;
                case IDS: {
                	added = impls.add(new LineIDTXTSpecDataImpl( specspec ));
                	//impl = new LineIDTXTSpecDataImpl( specspec );
                }
                    break;
              //  case NOT_SUPPORTED: {
             //       throw new SplatException( "Format not supported by SPLAT in Spectrum '" + specspec );   
             //   }
                default: {
                    throw new SplatException( "Spectrum '" + specspec + 
                                              "' supplied with an unknown "+
                                              "type: " + type );
                }
            }
            //if ( impl == null ) {
            if ( !added ) {
                throwReport( specspec, true, null );
            }
            
            for (SpecDataImpl impl : impls) {
            	specDataList.add(makeSpecDataFromImpl( impl, isRemote, namer.getURL() ));
            }
            
            return specDataList;
        }

        //  Only get here for guessed spectra.
        //specDataList.add(makeGuessedSpecData( specspec, namer.getURL() ));
        specDataList.addAll(makeGuessedSpecDataList(specspec, namer.getURL()));
        return specDataList;
      }
    
    /**
     *  Check the format of the incoming specification and create an
     *  instance of SpecData for it.
     *
     *  @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the SpecData object created from the given
     *          specification.
     *
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public SpecData get( String specspec )
    		throws SplatException {
    	
    	List<SpecData> spectra = getAll(specspec);
    	
    	if (spectra != null && !spectra.isEmpty())
    		return spectra.get(0);
    	else
    		return null;
    }
    /**
     *  Check the format of the incoming specification and create an
     *  instance of SpecData for it.
     *
     *  @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the List of SpecData objects created from the given
     *          specification.
     *
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */
    public List<SpecData> getAll( String specspec )
            throws SplatException
     {
    	List<SpecData> specDataList = new ConstrainedList<SpecData>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
    	List<SpecDataImpl> impls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
        
    	//SpecDataImpl impl = null;
        boolean isRemote = false;
        String guessedType = null;
        URL specurl = null;
      
        
        //  See what kind of specification we have.
        try {
            NameParser namer = new NameParser( specspec );
            isRemote = namer.isRemote();
     
            specurl = namer.getURL();
            //  Remote HDX/VOTable-like files should be downloaded by thile
            
            //  library. A local copy loses the basename context.
            if ( isRemote && namer.getFormat().equals( "XML" ) ) {
                //impl = makeXMLSpecDataImpl( specspec, true, specurl );
            	impls.add(makeXMLSpecDataImpl( specspec, true, specurl ));
            }
            else {
                //  Remote plainer formats (FITS, NDF) need a local copy.
                if ( isRemote ) {
                    PathParser p = remoteToLocalFile( specurl, DEFAULT  );
                                           namer = new NameParser( p.ndfname() );                   
                }
                guessedType = namer.getFormat();
                //impl = makeLocalFileImpl( namer.getName(), namer.getFormat() );
                impls.addAll(makeLocalFileImplList( namer.getName(), namer.getFormat() ));
            }
        }
        catch (SEDSplatException se) {
            throw se;
        }
        catch (Exception e ) {
            //impl = null;
        	impls.clear();
        }

        //  Try construct an intelligent report.
        //if ( impl == null ) {
        if ( impls.isEmpty() ) {
            throwReport( specspec, false, guessedType );
        }
        
    	for (SpecDataImpl impl : impls) {
    		SpecData specData = makeSpecDataFromImpl( impl, isRemote, specurl );
    		if (specData != null)
            	specDataList.add(specData);
    	}
    
    	/*SpecData specData = makeSpecDataFromImpl( impl, isRemote, specurl );
        if (specData != null)
        	specDataList.add(specData);*/
        
        return specDataList;
    }


    /**
     *  Create an instance of SpecData for the given format.
     *
     *  @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *                  
     * @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the SpecData object created from the given
     *          specification.
     *
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     *             
     *  @author Margarida Castro Neves (adapted for "strange" URL formats
     *          that cannot be easily guessed, like in getdata request.) 
     *  @author David Andresic (adapted for multi-HDU FITS files)
     */
    public SpecData get( String specspec, String format )
            throws SplatException
     {
    	List<SpecData> spectra = getAll(specspec, format);
    	
    	if (spectra != null && !spectra.isEmpty())
    		return spectra.get(0);
    	else
    		return null;
     }
    
    /**
     *  Create a List of SpecData instances for the given format.
     *
     *  @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *                  
     * @param specspec the specification of the spectrum to be
     *                  opened (i.e. file.fits, file.sdf,
     *                  file.fits[2], file.more.ext_1 etc.).
     *
     *  @return the List of SpecData objects created from the given
     *          specification.
     *
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     *             
     *  @author Margarida Castro Neves (adapted for "strange" URL formats
     *          that cannot be easily guessed, like in getdata request.) 
     *  @author David Andresic (adapted for multi-HDU FITS files)
     */
    public List<SpecData> getAll( String specspec, String format )
            throws SplatException
     {
        //SpecDataImpl impl = null;
    	List<SpecDataImpl> impls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
    	List<SpecData> spectra = new LinkedList<SpecData>();
        boolean isRemote = false;
        String guessedType = null;
        URL specurl = null;
        int ftype = GUESS;
        boolean notable=false;
        
        //  See what kind of specification we have.
        try {
            NameParser namer = new NameParser( specspec );
            isRemote = namer.isRemote();
     
            specurl = namer.getURL();
            //  Remote HDX/VOTable-like files should be downloaded by thile
            
            //  library. A local copy loses the basename context.
            if ( isRemote && format.equals( "XML" ) ) {
                //impl = makeXMLSpecDataImpl( specspec, true, specurl );
            	impls.add(makeXMLSpecDataImpl( specspec, true, specurl ));
                ftype=HDX;
            }
            else {
                //  Remote plainer formats (FITS, NDF) need a local copy.
                if ( isRemote ) {
                   
                    if (format.equals("FITS"))
                        ftype = FITS;
                    else if (format.equals("TEXT"))
                        ftype = TEXT;
                    else if (format.equals("XML"))
                        ftype = HDX;
                    else ftype = DEFAULT;
                    PathParser p = remoteToLocalFile( specurl, ftype  );
                                           namer = new NameParser( p.ndfname() );                   
                }
                //impl = makeLocalFileImpl( namer.getName(), format );
                impls.addAll(makeLocalFileImplList( namer.getName(), format ));
            }
        }
        catch (SEDSplatException se) {
            se.setType(ftype);
            throw se;
        }
        catch (Exception e ) {
            //impl = null;
            impls.clear();
            if (e.getMessage().contains("No TABLE element found")) {
                // if a VOTABLE with no TABLE is returned (for example, getData with wrong parameters)
                // a report should not be given.
                notable=true;
                throw (new SplatException(e));
            }          
        }

        //  Try construct an intelligent report.
        if ( impls.isEmpty() && ! notable ) {
            throwReport( specspec, false, format);
        }
        for (SpecDataImpl impl : impls) {
        	spectra.add(makeSpecDataFromImpl( impl, isRemote, specurl ));
        }
        //return makeSpecDataFromImpl( impl, isRemote, specurl );
        return spectra;
    }

    /**
     * Make a SpecDataImpl for a known local file with the given format.
     */
    protected SpecDataImpl makeLocalFileImpl( String name, String format )
        throws SplatException
    {
    	List<SpecDataImpl> impls = makeLocalFileImplList(name, format);
    	
    	if (impls != null && !impls.isEmpty())
    		return impls.get(0);
    	else
    		return null;
    }
    
    /**
     * Make a List of SpecDataImpl for a known local file with the given format.
     */
    protected List<SpecDataImpl> makeLocalFileImplList( String name, String format )
        throws SplatException
    {
    	List<SpecDataImpl> impls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
    	//SpecDataImpl impl = null;
        if ( format.equals( "NDF" ) ) {
            //impl = makeNDFSpecDataImpl( name );
        	impls.add(makeNDFSpecDataImpl( name ));
        }
        else if ( format.equals( "FITS" ) ) {
            //impl = makeFITSSpecDataImpl( name );
        	impls.addAll(makeFITSSpecDataImplList( name ));
        }
        else if ( format.equals( "TEXT" ) ) {
            //impl = new TXTSpecDataImpl( name );
        	impls.add(new TXTSpecDataImpl( name ));
        }
        else if ( format.equals( "XML" ) ) {
            //impl = makeXMLSpecDataImpl( name, false, null );
        	impls.add(makeXMLSpecDataImpl( name, false, null ));
        }
        else if ( format.equals( "IDS" ) ) {
            //impl = new LineIDTXTSpecDataImpl( name );
        	impls.add(new LineIDTXTSpecDataImpl( name ));
        }
        else {
            throw new SplatException
                ( "Spectrum '" + name + "' has an unknown format" + 
                  " (guessed: " + format + " )" );
        }
        return impls;
    }

    /**
     * Make an implementation for an NDF. If native NDF supported
     * isn't available then an attempt to create a wrapping NDX is made.
     */
    protected SpecDataImpl makeNDFSpecDataImpl( String specspec )
        throws SplatException
    {
        if ( NDFJ.supported() ) {
            return new NDFSpecDataImpl( specspec );
        }

        //  No native NDF available, use NDX access.
        logger.info
            ("No native NDF support, using less efficient NDX/JNIHDS access");

        URL url = null;
        try {
            url = new URL( "file:" + specspec + ".sdf" );
        }
        catch (MalformedURLException e) {
            throw new SplatException( e );
        }
        return new NDXSpecDataImpl( url );
    }

    /**
     * Make an implementation for an FITS file HDU. This could be either a
     * spectrum or a table, so we need to find out first.
     */
    protected SpecDataImpl makeFITSSpecDataImpl( String specspec )
        throws SplatException
    {
    	List<SpecDataImpl> specDataImpls = makeFITSSpecDataImplList(specspec);
    	
    	if (specDataImpls != null && !specDataImpls.isEmpty())
    		return specDataImpls.get(0);
    	else
    		return null;
    }
    
    /**
     * Make an implementation for an FITS file HDUs. This could be either a
     * spectra or tables, so we need to find out first.
     */
    protected List<SpecDataImpl> makeFITSSpecDataImplList( String specspec )
        throws SplatException
    {
    	List<SpecDataImpl> specDataImpls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);
    	boolean singleDish = false;
    	SpecDataImpl implGlobal = null;
    	boolean success=true;
    	
        implGlobal = new FITSSpecDataImpl( specspec );
    //    if (is_sdfits(implGlobal))
        
        for (int i = 0; i < ((FITSSpecDataImpl)implGlobal).hdurefs.length; i++) {
            singleDish = false;
        	SpecDataImpl impl = new FITSSpecDataImpl( specspec, i );
            // Table, if it is an table extension, or the data array size is 0
            // (may be primary).
            String exttype = impl.getProperty( "XTENSION" ).trim().toUpperCase();
            int dims[] = impl.getDims();
            DataSource datsrc;
            StarTable starTable;
            long rowCount = 0;
            String pos = i+"";
            
            if ( exttype.equals( "TABLE" ) || exttype.equals( "BINTABLE" ) ||
                    dims == null || dims[0] == 0 ) {
                try {
                    datsrc = new FileDataSource( specspec );
                    if ( i>0 )
                        datsrc.setPosition(pos);
                    starTable = new FitsTableBuilder().makeStarTable( datsrc, true, storagePolicy );

                    rowCount = starTable.getRowCount();
                    if ( rowCount == 0 )
                        throw new Exception( "The TABLE is empty");

                    if (starTable.getName().equals("SINGLE DISH") /*&& i==1*/) { // SDFITS format
                        singleDish = true;
                        if ( i == 1) {// skip first header
                            String url = datsrc.getURL().toString();
                            Header header = ((FITSSpecDataImpl)impl).getFitsHeaders();

                            for (int row=0;  row<rowCount; row++) {  // SDFITS: each row is a spectrum
                                impl = new SDFitsTableSpecDataImpl( starTable, url, header, row );
                                specDataImpls.add(impl);   
                            }
                        }// if i==1
                    } else { 
                        impl = new TableSpecDataImpl( starTable, specspec, datsrc.getURL().toString(),
                                ((FITSSpecDataImpl)impl).getFitsHeaders());
                    } // not SDFITS
                }
                catch (SEDSplatException se) {
                    se.setType(FITS);
                    se.setSpec(specspec);
                    logger.info(se.getMessage());
                    success=false;
                    //throw se;
                }
                catch (Exception e) {
                    if (e.getMessage().contains("HDU "+ i +"TABLE is empty")) {
                        impl=null;
                        logger.info( e.getMessage() );                       
                        // throw new SplatException (e);
                    }
                    else logger.info( "Failed to open FITS table "+e.getMessage() );
                    //throw new SplatException( "Failed to open FITS table", e );
                    success=false;
                }
            } 

                /* add only if data array size is not 0 
                 * (we can do this since we loop over all
                 * found HDUs so any relevant, non-zero HDUs
                 * will be treated correctly)
                 */
                if ( ! singleDish ) { // single dish spectra have been added already
                    if (success && (dims == null || (dims !=null && dims[0] != 0)))
                        specDataImpls.add(impl);
                    else
                        logger.info(String.format("Ignoring HDU #%d in '%s' (no spectra/data array size 0)", i, impl.getFullName()));
                }
            
    	} // for 
        
        return specDataImpls;
    }

    /**
     * Make an implementation that wraps a table.
     */
    protected SpecDataImpl makeTableSpecDataImpl( String specspec )
        throws SplatException
    {
        return new TableSpecDataImpl( specspec );
    }

    /**
     * Make an implementation for an arbitrary XML file. This could be a
     * VOTable or an HDX/NDX. If isRemote is true then the resource isn't
     * local and the URL value will be used to access the file.
     */
    protected SpecDataImpl makeXMLSpecDataImpl( String specspec,
                                                boolean isRemote,
                                                URL url )
        throws SplatException
    {
        SpecDataImpl impl = null;

        //  Check if this is a VOTable first (signature easier to check).
        Exception tableException = null;
        try {
            DataSource datsrc = null;
            if ( isRemote ) {
                datsrc = new URLDataSource( url );
            }
            else {
                datsrc = new FileDataSource( specspec );
            }
            StarTable starTable =
                new VOTableBuilder().makeStarTable( datsrc, true,
                                                    storagePolicy );
            if ( starTable.getRowCount() == 0 )
                throw new Exception( "The TABLE is empty");
            if ( starTable != null ) {
                return new TableSpecDataImpl( starTable );
            }
            
        }
        catch (Exception e) {
            tableException = e;
            if (e.getMessage().contains("No TABLE element found")) {
                impl=null;
                logger.info( "VOTABLE returned no table" );
                throw new SplatException (tableException);
            }
            if (e.getMessage().contains("TABLE is empty")) {
                impl=null;
                logger.info( e.getMessage() );
                throw new SplatException (tableException);
            }
        }
        try {
            if ( isRemote ) {
                impl = new NDXSpecDataImpl( url );
            }
            else {
                impl = new NDXSpecDataImpl( specspec );
            }
        }
        catch (Exception e) {
            if ( tableException != null ) {
                throw new SplatException( tableException );
            }
            throw new SplatException( e );
        }
        return impl;
    }

    /**
     * Make a suitable SpecData for a given implementation. If the spectrum is
     * remote and not a line identifier, then a {@link RemoteSpecData} object
     * is constructed.
     */
    protected SpecData makeSpecDataFromImpl( SpecDataImpl impl,
                                             boolean isRemote, URL url  )
        throws SplatException
    {
        SpecData specData = null;
        if ( impl instanceof LineIDTXTSpecDataImpl ) {
            specData = new LineIDSpecData( (LineIDTXTSpecDataImpl) impl );
            LocalLineIDManager.getInstance()
                .addSpectrum( (LineIDSpecData) specData );
        }
        else {
            if ( isRemote ) {
                specData = new RemoteSpecData( impl, url );
            }
            else {
                specData = new SpecData( impl );
            }
        }
        return specData;
    }

    /**
     * Make up a suitable report for a spectrum that cannot be
     * processed into a implementation and throw a SplatException.
     */
    private void throwReport( String specspec, boolean typed, 
                              String guessedType )
        throws SplatException
    {
        // If specspec if just a file, then we're having some issues
        // with accessing it using the known schemes or defined type.
        File testFile = new File( specspec );
        if ( testFile.exists() ) {
            if ( testFile.canRead() ) {
                if ( typed ) {
                    //  An explicit type was specified.
                    throw new SplatException( "Spectrum '" + specspec +
                                              "' cannot be matched to " +
                                              "the requested type " );
                }
                else {
                    if ( guessedType != null ) {
                        throw new SplatException( "Spectrum '" + specspec +
                                                  "' has an unknown type, "+
                                                  "format or name syntax" +
                                                  "(guessed: " + guessedType + 
                                                  " )" );
                    }
                    else {
                        throw new SplatException( "Spectrum '" + specspec +
                                                  "' has an unknown type, "+
                                                  "format or name syntax" );
                    }
                }
            }
            else {
                throw new SplatException( "Cannot read: " + specspec );
            }
        }
        else {
            // check if there is any authentication status message
            if (authenticator.getStatus() != null ) // in this case there as an error concerning authentication
                throw new SplatException(authenticator.getStatus() + " " + specspec );
            else 
            //  Just a file that doesn't exist.
            throw new SplatException( "Spectrum not found: " + specspec );
        }
    }

    /**
     * Create an clone of an existing spectrum by transforming it into
     * another implementation format. The destination format is
     * decided using the usual rules on specification string.
     *
     * @param source SpecData object to be cloned.
     * @param specspec name of the resultant clone (defines
     *                 implementation type).
     *
     * @return the cloned SpecData object.
     * @exception SplatException maybe thrown if there are problems
     *            creating the new SpecData object or the implementation.
     */
    public SpecData getClone( SpecData source, String specspec )
        throws SplatException
    {
        NameParser namer = new NameParser( specspec );
        String targetType = namer.getFormat();

        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        SpecDataImpl impl = null;

        if ( source instanceof LineIDSpecData ) {
            impl = new LineIDTXTSpecDataImpl( specspec,
                                              (LineIDSpecData) source );
        }
        else if ( targetType.equals( "NDF" ) ) {
            if ( NDFJ.supported() ) {
                impl = new NDFSpecDataImpl( namer.getName(), source );
            }
            else {
                impl = new NDXSpecDataImpl( namer.getName(), source );
            }
        }
        if ( targetType.equals( "FITS" ) ) {
            impl = new FITSSpecDataImpl( namer.getName(), source );
        }
        if ( targetType.equals( "TEXT" ) ) {
            impl = new TXTSpecDataImpl( namer.getName(), source );
        }
        if ( targetType.equals( "XML" ) ) {
            impl = new NDXSpecDataImpl( namer.getName(), source );
        }
        if ( impl != null ) {
            SpecData specData = makeSpecDataFromImpl( impl, false, null );
            specData.setType( source.getType() );
            return specData;
        }
        else {
            throw new SplatException( "Cannot create a spectrum using name: "
                                      + specspec );
        }
    }

    /**
     * Create an clone of an existing spectrum by transforming it into
     * another implementation format. The destination format is
     * as given.
     *
     * @param source SpecData object to be cloned.
     * @param specspec name of the resultant clone (defines
     *                 implementation type).
     * @param type the type of spectrum
     * @param format if the type is table this maybe a suggested format
     *
     * @return the cloned SpecData object.
     * @exception SplatException maybe thrown if there are problems
     *            creating the new SpecData object or the implementation.
     */
    public SpecData getClone( SpecData source, String specspec, int type,
                              String format )
        throws SplatException
    {
        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        SpecDataImpl impl = null;

        // LineIDs can only be cloned to LineIDs.
        if ( source instanceof LineIDSpecData ) {
            impl = new LineIDTXTSpecDataImpl( specspec,
                                              (LineIDSpecData) source );
        }
        else {
            switch (type) {
               case FITS: {
                   impl = new FITSSpecDataImpl( specspec, source );
               }
               break;
               case HDS: {
                   if ( NDFJ.supported() ) {
                       impl = new NDFSpecDataImpl( specspec, source );
                   }
                   else {
                       impl = new NDXSpecDataImpl( specspec, source );
                   }
               }
               break;
               case TEXT: {
                   impl = new TXTSpecDataImpl( specspec, source );
               }
               break;
               case HDX: {
                   impl = new NDXSpecDataImpl( specspec, source );
               }
               break;
               case TABLE: {
                   impl = new TableSpecDataImpl( specspec, source, format );
               }
               break;
               default: {
                   // DEFAULT or unknown.
                   return getClone( source, specspec );
               }
            }
        }
        SpecData specData = makeSpecDataFromImpl( impl, false, null );
        specData.setType( source.getType() );
        return specData;
    }

    /**
     * Create an clone of an existing spectrum by transforming it into
     * table implementation format. The destination format is
     * decided using the given string or naming rules if the format is
     * null.
     *
     * @param source SpecData object to be cloned.
     * @param specspec name of the table implementation.
     * @param format the table format, null for use builtin rules.
     *
     * @return the cloned SpecData object.
     * @exception SplatException maybe thrown if there are problems
     *            creating the new SpecData object or the implementation.
     */
    public SpecData getTableClone( SpecData source, String specspec,
                                   String format )
        throws SplatException
    {
        SpecDataImpl impl = new TableSpecDataImpl( specspec, source, format );
        return new SpecData( impl );
    }

    /**
     * Return a list of the supported table formats. Extended to exclude "jdbc"
     * and add a first type of "default". A StarTable saved using default
     * requires a file extension.
     */
    public List getKnownTableFormats()
    {
        List list = TableSpecDataImpl.getKnownFormats();
        list.add( 0, "default" );
        list.remove( "jdbc" );
        return list;
    }

    /**
     * Create spectrum that can have its values modified or
     * individually edited. Initially the object contains no
     * coordinates or data, so space for these must be allocated and
     * set using the EditableSpecData.setData() methods.
     *
     * @param shortname the short name to use for the spectrum.
     *
     * @return an EditableSpecData object.
     */
    public EditableSpecData createEditable( String shortname )
        throws SplatException
    {
        return new EditableSpecData( new MEMSpecDataImpl( shortname ) );
    }

    /**
     * Copy a spectrum into one that can have its values modified or
     * individually edited. If the SpecData is an instance if
     * LineIDSpecData then another LineIDSpecData instance will be
     * returned, otherwise a plain EditableSpecData instance will be
     * returned.
     * <p>
     * Some of the rendering properties will also be copied.
     *
     * @param shortname the short name to use for the spectrum copy.
     *
     * @return an EditableSpecData object.
     */
    public EditableSpecData createEditable( String shortname,
                                            SpecData specData )
        throws SplatException
    {
        // Check the actual type to see if this needs special handling.
        if ( specData instanceof LineIDSpecData ) {
            LineIDMEMSpecDataImpl impl = new LineIDMEMSpecDataImpl( shortname,
                                                                    specData );
            LineIDSpecData newSpecData = new LineIDSpecData( impl );
            LocalLineIDManager.getInstance().addSpectrum( newSpecData );
            specData.applyRenderingProperties( newSpecData );
            return newSpecData;
        }

        MEMSpecDataImpl impl = new MEMSpecDataImpl( shortname, specData );
        EditableSpecData newSpecData = new EditableSpecData( impl );
        specData.applyRenderingProperties( newSpecData );
        return newSpecData;
    }

    /**
     * Copy a spectrum into one that can have its values modified or
     * individually edited. If sort is true then an attempt is made to sort
     * the coordinates into increasing order, if needed. During this sort any
     * duplicate values are also removed.
     *
     * @param shortname the short name to use for the spectrum copy.
     * @param sort if true then the coordinates will be sorted.
     *
     * @return an EditableSpecData object.
     */
    public EditableSpecData createEditable( String shortname,
                                            SpecData specData,
                                            boolean sort )
        throws SplatException
    {
        EditableSpecData editableSpecData = createEditable( shortname,
                                                            specData );
        if ( sort && ! specData.isMonotonic() ) {
            editableSpecData.sort();
        }
        return editableSpecData;
    }

    /**
     * Create a SpecData for a given {@link StarTable}.
     */
    public SpecData get( StarTable table )
        throws SplatException
    {
        SpecDataImpl impl = new TableSpecDataImpl( table );
        return new SpecData( impl );
    }

    /**
     * Create a SpecData for a given {@link StarTable}. Also provide a
     * short and full names for the table (these are often blank).
     */
    public SpecData get( StarTable table, String shortName, String fullName )
        throws SplatException
    {
        SpecDataImpl impl = new TableSpecDataImpl(table, shortName, fullName);
        return new SpecData( impl );
    }

    /**
     * Create a SpecData for a given {@link Ndx}.
     */
    public SpecData get( Ndx ndx )
        throws SplatException
    {
        SpecDataImpl impl = new NDXSpecDataImpl( ndx );
        return new SpecData( impl );
    }

    /**
     * Create a SpecData for a given {@link Ndx}. Also provide a short name
     * and full name.
     */
    public SpecData get( Ndx ndx, String shortName, String fullName )
        throws SplatException
    {
        SpecDataImpl impl = new NDXSpecDataImpl( ndx, shortName, fullName );
        return new SpecData( impl );
    }

    /**
     * Cause an existing SpecData object to "re-open". This causes the object
     * to re-visit the backing file if one exists. Throws a SplatException if
     * the operation fails for any reason.
     */
    public void reOpen( SpecData specData )
        throws SplatException
    {
        String specspec = specData.getFullName();
        SpecDataImpl oldImpl = specData.getSpecDataImpl();
        SpecDataImpl newImpl = null;

        try {
            if ( specData instanceof LineIDSpecData ) {
                newImpl = LocalLineIDManager.getInstance()
                    .reLoadSpecDataImpl( (LineIDSpecData) specData );
            }
            else if ( oldImpl instanceof FITSSpecDataImpl ) {
                newImpl = new FITSSpecDataImpl( specspec );
            }
            else if ( oldImpl instanceof NDFSpecDataImpl ) {
                newImpl = new NDFSpecDataImpl( specspec );
            }
            else if ( oldImpl instanceof TXTSpecDataImpl ) {
                newImpl = new TXTSpecDataImpl( specspec );
            }
            else if ( oldImpl instanceof NDXSpecDataImpl ) {
                newImpl = new NDXSpecDataImpl( specspec );
            }
            else if ( oldImpl instanceof TableSpecDataImpl ) {
                newImpl = new TableSpecDataImpl( specspec );
            }
            if ( newImpl != null ) {
                specData.setSpecDataImpl( newImpl );
            }
            else {
                throw new SplatException( "Cannot re-open: " + specspec );
            }
        }
        catch (SplatException e) {
            throw new SplatException( "Failed to re-open spectrum", e );
        }
    }

    /**
     * Return the "types" of a given {@link SpecData} instance.
     * The types are the local data type constant, DEFAULT etc., and the table
     * index, if the underlying representation is a table.
     */
    public int[] getTypes( SpecData specData )
    {
        int[] result = new int[1];
        SpecDataImpl impl = specData.getSpecDataImpl();

        if ( impl instanceof LineIDSpecDataImpl ) {
            result[0] = IDS;
        }
        else if ( impl instanceof FITSSpecDataImpl ) {
            result[0] = FITS;
        }
        else if ( impl instanceof NDFSpecDataImpl ) {
            result[0] = HDS;
        }
        else if ( impl instanceof TXTSpecDataImpl ) {
            result[0] = TEXT;
        }
        else if ( impl instanceof NDXSpecDataImpl ) {
            result[0] = HDX;
        }
        else if ( impl instanceof TableSpecDataImpl ) {
            result[0] = TABLE;
            //result[1] = ????; // What type of TABLE is this?
        }
        return result;
    }

   
    /**
     * Given a URL for a remote resource make a local, temporary, copy. The
     * temporary file should have the correct file extension for the type of
     * remote data an {@link PathParser} is returned as the result (null
     * if a failure occurs).
     */
    protected PathParser remoteToLocalFile( URL url, int type )
            throws SplatException
    {
        PathParser namer = null;
        boolean compressed = false;
        MimeType mimetype = null;
        String remotetype = null;
      
        try {
            
            //  Contact the resource.
           
            
            URLConnection connection = url.openConnection();
          
            //  Handle switching from HTTP to HTTPS, if a HTTP 30x redirect is
            //  returned, as Java doesn't do this by default (security issues
            //  when moving from secure to non-secure).
            if ( connection instanceof HttpURLConnection ) {
                int code = ((HttpURLConnection)connection).getResponseCode();
                
               
                if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                     code == HttpURLConnection.HTTP_MOVED_TEMP ||
                     code == HttpURLConnection.HTTP_SEE_OTHER ) {
                    String newloc = connection.getHeaderField( "Location" );
                    URL newurl = new URL( newloc );
                    connection = newurl.openConnection();
                }
                code = ((HttpURLConnection)connection).getResponseCode();
                if ( code >= 500  ) // 5** codes, server is not available so we can stop right now.
                {
                    throw new SplatException( "Server returned " + ((HttpURLConnection)connection).getResponseMessage() + " " + 
                            " for the URL : " + url.toString()    );
                }
                String conttype=connection.getContentType();
                if (conttype==null) { // avoids NPE
                    conttype=""; 
                    mimetype = new MimeType();
                } else
                    mimetype = new MimeType(conttype);

                compressed = ("gzip".equals(connection.getContentEncoding()) || conttype.contains("gzip"));
                remotetype = getRemoteType(connection.getHeaderField("Content-disposition"));
            }
            connection.setConnectTimeout(10*1000); // 10 seconds
            connection.setReadTimeout(30*1000); // 30 seconds read timeout??? 
            InputStream is = connection.getInputStream();
            if (compressed)
                is = new GZIPInputStream(is);
            //  And read it into a local file. Use the existing file extension
            //  if available and we're not guessing the type.
            namer = new PathParser( url.toString() );

            String stype = null;
            
            // parse mime type
            if (mimetype.getSubType().contains("votable") || mimetype.getSubType().contains("xml")) {
                type = HDX;
            } else if (mimetype.getSubType().contains("fits") ) {
                type = FITS;
            } else if (mimetype.getPrimaryType().contains("text") && mimetype.getSubType().contains("plain") ) {
                type = TEXT;
            } 
            
            //  Create a temporary file. Use a file extension based on the
            //  type, if known.
           
            switch (type) {
                case FITS: {
                    stype = ".fits";
                }
                break;
                case HDS: {
                    stype = ".sdf";
                }
                break;
                case TEXT: {
                    stype = ".txt";
                }
                break;
                case HDX: {
                    stype = ".xml";
                }
                break;
                case TABLE: {
                    stype = ".tmp";
                }
                break;
                case GUESS: {
                    stype = ".tmp";
                }
                break;
                default: {
                    if (remotetype != null) 
                        stype=remotetype; // the type of the remote filename
                    else 
                        stype = namer.type();
                    
                    if ( stype.equals( "" ) ) {
                        stype = ".tmp";
                    }
                }
            }
            
            TemporaryFileDataSource datsrc =
                new TemporaryFileDataSource( is, url.toString(), "SPLAT",
                                             stype, null );
            String tmpFile = datsrc.getFile().getCanonicalPath();
           
            namer.setPath( tmpFile );
            datsrc.close();

            //  Check file. If an error occurred with the request at the
            //  server end this will probably result in the download of an
            //  HTML file, or a file starting with NULL.
            FileInputStream fis = new FileInputStream( tmpFile );
         /*   BufferedReader br = new BufferedReader(new InputStreamReader(fis));
             String line1 = null, line2=null;
             line1 = br.readLine();
             line2 = br.readLine(); 
             br.close();*/
            // char[] header = line1.toCharArray();
            byte[] header = new byte[4];
            fis.read( header );
            fis.close();

            //  Test if equal to '<!DO' of "<!DOCTYPE" or '<HTM'
            if ( ( header[0] == '<' && header[1] == '!' &&
                   header[2] == 'D' && header[3] == 'O' ) ||
                 header[0] == '<' && header[1] == 'H' &&
                 header[2] == 'T' && header[3] == 'M' ) {
                //  Must be HTML.
                throw new SplatException( "Cannot use the file returned" +
                                          " by the URL : " + url.toString() +
                                          " it contains an HTML document" );
            }
            else if ( header[0] == 0 && header[1] == 0 ) {
                throw new SplatException( "Cannot use the file returned" +
                                          " by the URL : " + url.toString() +
                                          " as it is empty" );
            }
            
            // try to get file format from its first lines 
            // if extension is not in pathname
  
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
        return namer;
    }

    
    /*
     * getRemoteType
     * gets remote file type from remote filename contained in HTTP connection header string
     */
    private String getRemoteType(String headerstr) {
        if ( headerstr == null || headerstr.isEmpty() )
            return null;
        if (headerstr.contains("filename="))
        { 
           String remotename = headerstr.substring(headerstr.indexOf("filename=")+9);
            if (remotename != null) {
                PathParser remotenamer = new PathParser(remotename);
                return remotenamer.type();
            }
        }
        return null;
    }

    //  Types of reprocessing of 2D data files. The default is VECTORIZE
    //  which implementations should have already performed.
    public final static int COLLAPSE = 0;
    public final static int EXTRACT = 1;
    public final static int VECTORIZE = 2;

    /**
     * Process a SpecData object that isn't really 1D into other
     * representations of itself. There are several ways that this
     * reprocessing can be performed:
     * <ul>
     *   <li>Collapse onto the dispersion axis</li>
     *   <li>Expansion into a spectrum per dispersion line of the original
     *       data</li>
     *   <li>Vectorisation of the original data into a single spectrum</li>
     * </ul>
     * To be re-processable a SpecData must have an implementation that is 2D
     * or 3D (or reducible to these).  Higher dimensions are not supported and
     * 1D spectrum require no reprocessing. In both these cases a null is
     * returned. A null is also returned for VECTORIZED requests as this is
     * the natural format for all spectral data (this may change).
     *
     * @param specData the SpecData object to reprocess.
     * @param method the method to use when reprocessing, COLLAPSE, EXTRACT or
     *               VECTORIZE.
     * @param dispax the index of the dispersion axis, set to null for
     *               automatic choice.
     * @param selectax the index of the axis that will be stepped along
     *                 collapsing down onto dispersion axis, set to null for
     *                 automatic choice. This may not be the dispax.
     * @param purge whether to remove any spectra that have bad limits from
     *              the results.
     */
    public SpecData[] reprocessTo1D( SpecData specData, int method,
                                     Integer dispax, Integer selectax,
                                     boolean purge )
        throws SplatException
    {
        int dax = -1;
        if ( dispax != null ) {
            dax = dispax.intValue();
        }
        int sax = -1;
        if ( selectax != null ) {
            sax = selectax.intValue();
        }

        if ( method == VECTORIZE ) {
            //  Nothing to do, this is the native form. XXX maybe we should
            //  check the dispersion axis. If this isn't the first one then we
            //  could re-order so we run along it, not perpendicular to it.
            return null;
        }

        //  Check dimensionality, for 1D and greater than 3D we do nothing.
        SpecDims specDims = new SpecDims( specData );
        int ndims = specDims.getNumSigDims();

        SpecData[] results = null;
        if ( ndims > 1 && ndims < 4 ) {

            //  Use choice of dispersion and stepped axis.
            specDims.setDispAxis( dax, true );
            specDims.setSelectAxis( sax, true );

            if ( method == COLLAPSE ) {
                results = collapseSpecData( specData, specDims );
            }
            else if ( method == EXTRACT )  {
                String testEchelle = specData.getProperty("WAT0_001"); // fits echele in IRAF format
                if (! testEchelle.isEmpty() && testEchelle.contains("system=multispec")) {
                    results = extractEchelleSpecData( specData, specDims );
                } else 
                    results = extractSpecData( specData, specDims );
            }

            //  Purge any spectra with BAD limits.
            if ( purge && results != null ) {
                results = purgeBadLimits( results );
            }
        }
        return results;
    }

    /**
     * Remove any SpecData instance that have bad data or spectral limits from
     * an array of SpecData instances. Used after extraction or collapse to
     * clean up lists.
     */
    protected SpecData[] purgeBadLimits( SpecData spectra[] )
    {
        //  Count spectra with good limits.
        int n = 0;
        for ( int i = 0; i < spectra.length; i++ ) {
            double range[] = spectra[i].getRange();
            if ( range[0] != SpecData.BAD &&
                 range[1] != SpecData.BAD &&
                 range[2] != SpecData.BAD &&
                 range[3] != SpecData.BAD ) {
                n++;
            }
        }

        //  Purge any spectra with BAD limits.
        SpecData results[];
        if ( n != spectra.length ) {
            results = new SpecData[n];
            n = 0;
            for ( int i = 0; i < spectra.length; i++ ) {
                double range[] = spectra[i].getRange();
                if ( range[0] != SpecData.BAD &&
                     range[1] != SpecData.BAD &&
                     range[2] != SpecData.BAD &&
                     range[3] != SpecData.BAD ) {
                    results[n] = spectra[i];
                    n++;
                }
            }
        }
        else {
            results = spectra;
        }
        return results;
    }

    /**
     * Create new SpecData instances by collapsing 2D or 3D implementations
     * onto the dispersion axis. This creates new 1D SpecData objects. Only
     * one is produced if the data is 2D.
     */
    private SpecData[] collapseSpecData( SpecData specData, SpecDims specDims )
        throws SplatException
    {
        SpecData[] results = null;
        int ndims = specDims.getNumSigDims();
        if ( ndims == 2 ) {
            //  Simple 2D data.
            results = new SpecData[1];
            SpecDataImpl newImpl =
                new CollapsedSpecDataImpl( specData, specDims );
            results[0] = new SpecData( newImpl );
        }
        else {
            //  Need to pick an axis to step along collapsing each section in
            //  turn onto the dispersion axis.
            int stepaxis = specDims.getSelectAxis( true );
            int displen = specDims.getSigDims()[stepaxis];
            results = new SpecData[displen];
            for ( int i = 0; i < displen; i++ ) {
                SpecDataImpl newImpl =
                    new CollapsedSpecDataImpl( specData, specDims, i );
                results[i] = new SpecData( newImpl );
            }
        }
        return results;
    }

    /**
     * Create a set of new 1D SpecData instances by extracting each line of a
     * 2D or 3D implementation along the dispersion axis.
     */
    private SpecData[] extractSpecData( SpecData specData, SpecDims specDims )
        throws SplatException
    {
        SpecData[] results = null;
        int dispax = specDims.getDispAxis( true );
        int[] dims = specDims.getSigDims();

        if ( dims.length == 2 ) {
            //  Simple 2D data.
            if ( dispax == 1 ) {
                results = new SpecData[dims[0]];
            }
            else {
                results = new SpecData[dims[1]];
            }
            for ( int i = 0; i < results.length; i++ ) {
                SpecDataImpl newImpl =
                    new ExtractedSpecDataImpl( specData, specDims, i );
                results[i] = new SpecData( newImpl );
            }
        }
        else {
            int stepaxis = specDims.getSelectAxis( true );
            int otheraxis = specDims.getFreeAxis( true );
            int steplength = dims[stepaxis];
            int otherlength = dims[otheraxis];

            results = new SpecData[steplength*otherlength];
            int count = 0;
            for ( int j = 0; j < otherlength; j++ ) {
                for ( int i = 0; i < steplength; i++ ) {
                    SpecDataImpl newImpl =
                        new ExtractedSpecDataImpl( specData, specDims, i, j );
                    results[count++] = new SpecData( newImpl );
                }
            }
        }
        return results;
    }
    
    /**
     * Create a set of new 1D SpecData instances from IRAF Echelle FITS format by extracting each line of a
     * 2D or 3D implementation along the dispersion axis and applying the defined parameters.
     */
    private SpecData[] extractEchelleSpecData( SpecData specData, SpecDims specDims )
        throws SplatException
    {
        SpecData[] results = null;
        int dispax = specDims.getDispAxis( true );
        int[] dims = specDims.getSigDims();

        if ( dims.length == 2 ) {
            //  Simple 2D data.
            if ( dispax == 1 ) {
                results = new SpecData[dims[0]];
            }
            else {
                results = new SpecData[dims[1]];
            }

            boolean moreinfo=true;
           
            ArrayList<String> specline = new ArrayList<String>();
            String specParams = "";
            int specindex=1; int linelength=0;
            for (int i=1; moreinfo ;i++) {
                String property= "WAT2_"+ String.format("%03d", i); // think of a better algorithm!!!
                String propline = getEchelleProperty(specData, property, linelength); // Problem: getProperty always removes heading/tailing empty spaces
                int newlength = propline.length();
                linelength=newlength; // !!!HACK works only if fitst line has the right length
                
                logger.info("propline1 "+propline+"<\n");
                if (! propline.isEmpty()) {      
                    propline=specParams.concat(propline);
                    logger.info("propline2 "+propline+"<\n");
                    int specstart = propline.indexOf(" spec"+specindex);
                    if (specstart != -1) {
                        if (i > 1  ) { // do not do this at the first line
                            specParams = propline.substring(0, specstart -1);
                            specline.add(specParams);
                        }
                        specParams = propline.substring(specstart +1);
                        specindex++;
                    } else {
                        specParams = propline;
                    }

                } else {
                    moreinfo = false;
                    if (specParams != null)
                        specline.add(specParams);
                }
            }

            logger.info("results "+results+ "count "+specline.size()+"\n");
            for ( int i = 0; i < results.length; i++ ) {
                
                logger.info(specline.get(i)+"\n");
                int ind1 = specline.get(i).indexOf('"'); 
                if (ind1 < specline.get(i).length()-1)
                    ind1+=1;
            //    int ind2 = specline.get(i).indexOf('"', ind1)-1;
                String sparamstr = specline.get(i).substring(ind1 );
                String [] sparams = sparamstr.split(" ");
                SpecDataImpl newImpl = null;
                try {
                    newImpl = new ExtractedSpecDataImpl( specData, specDims, i , Integer.parseInt(sparams[2]), 
                                                    Double.parseDouble(sparams[3]), Double.parseDouble(sparams[4]), Double.parseDouble(sparams[6]));
                    results[i] = new SpecData( newImpl );
                } 
                catch (Exception e) {
                    logger.info(i+ "- " + e.getMessage());
                    results[i]=null;
                    e.printStackTrace();
                }
               
            }
        }
        else {
            int stepaxis = specDims.getSelectAxis( true );
            int otheraxis = specDims.getFreeAxis( true );
            int steplength = dims[stepaxis];
            int otherlength = dims[otheraxis];

            results = new SpecData[steplength*otherlength];
            int count = 0;
            for ( int j = 0; j < otherlength; j++ ) {
                for ( int i = 0; i < steplength; i++ ) {
                    SpecDataImpl newImpl =
                            new ExtractedSpecDataImpl( specData, specDims, i, j );
                    results[count++] = new SpecData( newImpl );
                }
            }
        }
        return results;
    }
    
  
    protected String getEchelleProperty( SpecData specdata, String key, int linelength ) {
        
        Header hdrs=specdata.getHeaders();
    
        String scard = hdrs.findKey(key.toUpperCase());
        HeaderCard card = hdrs.findCard(key.toUpperCase());
       // HeaderCard card = new HeaderCard( scard );
        String cardValue=card.getValue();  
        int ind=cardValue.indexOf("\' ");
      
        String  test=card.toString(); 
        if (scard.endsWith(" ")) 
                cardValue+=" "; // add last space if it's found in scard
        if ( linelength > cardValue.length() )
            cardValue=" "+cardValue; // add first space if string is shorter
        return cardValue;
    }

    /**
     * Process a SED (IVOA spectral data model) XML file and extract all the
     * spectra that it contains.
     *
     * @param specspec the SED specification, assumed to be a VOTable, can be
     *                 remote or local.
     * @return an array of SpecData instances, one for each spectrum located.
     */
    public SpecData[] expandXMLSED( String specspec )
        throws SplatException
    {
        ArrayList specList = new ArrayList();

        //  Access the VOTable.
        VOElement root = null;
        try {
            root = new VOElementFactory().makeVOElement( specspec );
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open VOTable"+e.getMessage(), e );
            //throw new SplatException( "Failed to open SED VOTable"+e.getMessage(), e );
        }

        //  First element should be a RESOURCE.
        VOElement[] resource = root.getChildren();
        String tagName = null;
        String utype = null;
        SpecData specData = null;
        VOStarTable table = null;
        for ( int i = 0; i < resource.length; i++ ) {
            tagName = resource[i].getTagName();
            if ( "RESOURCE".equals( tagName ) ) {

                //  Look for the TABLEs and check if any have utype
                //  "sed:Segment" these are the spectra.
                VOElement child[] = resource[i].getChildren();
                for ( int j = 0; j < child.length; j++ ) {
                    tagName = child[j].getTagName();
                    if ( "TABLE".equals( tagName ) ) {
                        utype = child[j].getAttribute( "utype" );                    
                        //   if ( "sed:Segment".equals( utype ) ) { // we try this also for multiple spectra in a VOTable which do not have this utype (i.E. echelle spectra)
                            
                                try {
                                    table = new VOStarTable( (TableElement) child[j] );
                               
                                if ( table.getRowCount() == 0 )
                                    throw new SplatException( "The table is empty: "+specspec);
                                specData = new SpecData( new TableSpecDataImpl(table) );
                               // specData.setShortName(specData.getShortName() + " " + child[j].getAttribute("name"));
                                specList.add( specData );
                                } catch (IOException e) {
                                    throw new SplatException(e);
                                }
                            
                    //    }
                    }
                }
            }
        }
        SpecData[] spectra = new SpecData[specList.size()];
        specList.toArray( spectra );
        return spectra;
    }

    /**
     * Process a SED stored in a FITS table and extract all the
     * spectra that it contains.
     *
     * @param specspec the SED FITS file containing the table.
     * @param nspec number of spectra table contains.
     * @return an array of SpecData instances, one for each spectrum located.
     */
    public SpecData[] expandFITSSED( String specspec, int nspec )
        throws SplatException
    {
        ArrayList specList = new ArrayList();

        SpecDataImpl impl = null;
        for ( int i = 0; i < nspec; i++ ) {
            impl = new TableSpecDataImpl( specspec, i );
            specList.add( makeSpecDataFromImpl( impl, false, null ) );
        }
        SpecData[] spectra = new SpecData[specList.size()];
        specList.toArray( spectra );
        return spectra;
    }

    /**
     * Convert a of mime types into the equivalent SPLAT type (these are
     * int constants defined in SpecDataFactory). Note we use the full MIME
     * types and the SSAP shorthand versions (fits, votable, xml).
     */
    public static int mimeToSPLATType( String type )
    {
        int stype = SpecDataFactory.DEFAULT;
        String simpleType = type.toLowerCase();

        //   Note allow for application/fits;xxxx, so use startsWith,
        //   same for full mime types below.
       
        if ( simpleType.startsWith( "application/fits" ) ||
             simpleType.equals( "fits" ) ) {
            //  FITS format, is that image or table?
            stype = SpecDataFactory.FITS;
        }
        else if ( simpleType.startsWith( "image/fits" ) ) {
            //  FITS (image) format... 
            stype = SpecDataFactory.FITS;
        }
        else if ( simpleType.startsWith( "spectrum/fits" ) ) {
            //  FITS format, is that image or table? Don't know who
            //  thought this was a mime-type?
            stype = SpecDataFactory.FITS;
        }
        else if (simpleType.startsWith("timeseries/fits") ) {
            stype = SpecDataFactory.FITS;
        }
        else if ( simpleType.startsWith( "text/plain" ) ) {
            //  ASCII table of some kind.
            stype = SpecDataFactory.TABLE;
        }
        else if ( simpleType.startsWith( "application/x-votable+xml" ) ||
                  simpleType.equals( "text/xml;x-votable" ) ||
                  simpleType.startsWith( "text/x-votable+xml" ) ||
                  simpleType.equals( "xml" ) ) {
            
            // VOTABLE containing DataLink information
            if (simpleType.contains("content=datalink"))
                stype = SpecDataFactory.DATALINK;
            else
            // VOTable spectrum, open as a table. Is really the SSAP native
            // XML representation so could be a SED? In which case this might be
            // better as SpecDataFactory.SED, we'll see.
               stype = SpecDataFactory.TABLE;
               // stype = SpecDataFactory.SED;
        }
        else if ( simpleType.startsWith( "spectrum/votable" ) ||
                  simpleType.equals( "votable" ) ) {
            stype = SpecDataFactory.TABLE;

        } else if (!simpleType.isEmpty())
            stype = SpecDataFactory.NOT_SUPPORTED;
        return stype;
    }

    //
    //  DataNode guessing.
    //
    public SpecData makeGuessedSpecData( String specspec, URL url )
        throws SplatException
    {
    	List<SpecData> specDataList = makeGuessedSpecDataList(specspec, url);
    	
    	if (specDataList != null && !specDataList.isEmpty())
    		return specDataList.get(0);
    	else
    		return null;
    }
    
    //
    //  DataNode guessing.
    //
    public List<SpecData> makeGuessedSpecDataList( String specspec, URL url )
        throws SplatException
    {
    	List<SpecData> specDataList = new LinkedList<SpecData>();
    	//SpecData specData = null;
        if ( dataNodeFactory == null ) {
            dataNodeFactory = new DataNodeFactory();
            SplatDataNode.customiseFactory( dataNodeFactory );
        }

        try {
            DataNode node =
                dataNodeFactory.makeDataNode( null, new File( specspec ) );
            List<SpecData> sdList = SplatDataNode.makeSpecDataList( node );
            
            for (SpecData specData : sdList) {
            	if ( specData != null ) {
                    specData.setShortName( url.toString() );
                    specDataList.add(specData);
                }
            }
            
        }
        catch (SEDSplatException se ) {
            throw se;
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
        return specDataList;
    }

    public void setAuthenticator(SSAPAuthenticator auth) {
        authenticator=auth;
    }
}


