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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    /**
     * Short descriptions of each type.
     */
    public final static String[] shortNames = {
        "default",
        "fits",
        "hds",
        "text",
        "hdx",
        "table",
        "line ids",
        "guess"
   
    };

    /**
     * Long descriptions of each type.
     */
    public final static String[] longNames = {
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
        {"*"},
        {"fits", "fit"},
        {"sdf"},
        {"txt", "lis"},
        {"xml", "vot"}, // changed MCN 
        {"*"},
        {"ids"}
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
     *  @return the SpecData object created from the given
     *          specification.
     *  @exception SplatException thrown if specification does not
     *             specify a spectrum that can be accessed.
     */

    public SpecData get( String specspec, int type )
        throws SplatException
    {
        //  Type established by file name rules.
        if ( type == DEFAULT ) {
            return get( specspec );
          
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
            SpecDataImpl impl = null;
            switch (type)
            {
                case FITS: {
                    impl = makeFITSSpecDataImpl( specspec );
                }
                    break;
                case HDS: {
                    impl = makeNDFSpecDataImpl( namer.getName() );
                }
                    break;
                case TEXT: {
                    impl = new TXTSpecDataImpl( specspec );
                }
                    break;
                case HDX: {
                    //  HDX should download remote files as it needs to keep
                    //  the basename to locate other references.
                    if ( namer.isRemote() ) {
                        impl = new NDXSpecDataImpl( namer.getURL() );
                    }
                    else {
                        impl = new NDXSpecDataImpl( specspec );
                    }
                }
                    break;
                case TABLE: {
                   impl = makeTableSpecDataImpl( specspec );

                }
                    break;
                case IDS: {
                    impl = new LineIDTXTSpecDataImpl( specspec );
                }
                    break;
                default: {
                    throw new SplatException( "Spectrum '" + specspec + 
                                              "' supplied with an unknown "+
                                              "type: " + type );
                }
            }
            if ( impl == null ) {
                throwReport( specspec, true, null );
            }
            return makeSpecDataFromImpl( impl, isRemote, namer.getURL() );
        }

        //  Only get here for guessed spectra.
        return makeGuessedSpecData( specspec, namer.getURL() );
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
            throws SplatException
     {
        SpecDataImpl impl = null;
        boolean isRemote = false;
        String guessedType = null;
        URL specurl = null;
      
        
        //  See what kind of specification we have.
        try {
            NameParser namer = new NameParser( specspec );
            isRemote = namer.isRemote();
     
            specurl = namer.getURL();
            //  Remote HDX/VOTable-like files should be downloaded by the
            //  library. A local copy loses the basename context.
            if ( isRemote && namer.getFormat().equals( "XML" ) ) {
                impl = makeXMLSpecDataImpl( specspec, true, specurl );
            }
            else {
                //  Remote plainer formats (FITS, NDF) need a local copy.
                if ( isRemote ) {
                    PathParser p = remoteToLocalFile( specurl, DEFAULT  );
                                           namer = new NameParser( p.ndfname() );                   
                }
                guessedType = namer.getFormat();
                impl = makeLocalFileImpl( namer.getName(), namer.getFormat() );
            }
        }
        catch (SEDSplatException se) {
            throw se;
        }
        catch (Exception e ) {
            impl = null;
        }

        //  Try construct an intelligent report.
        if ( impl == null ) {
            throwReport( specspec, false, guessedType );
        }
        return makeSpecDataFromImpl( impl, isRemote, specurl );
    }

    /**
     * Make a SpecDataImpl for a known local file with the given format.
     */
    protected SpecDataImpl makeLocalFileImpl( String name, String format )
        throws SplatException
    {
        SpecDataImpl impl = null;
        if ( format.equals( "NDF" ) ) {
            impl = makeNDFSpecDataImpl( name );
        }
        else if ( format.equals( "FITS" ) ) {
            impl = makeFITSSpecDataImpl( name );
        }
        else if ( format.equals( "TEXT" ) ) {
            impl = new TXTSpecDataImpl( name );
        }
        else if ( format.equals( "XML" ) ) {
            impl = makeXMLSpecDataImpl( name, false, null );
        }
        else if ( format.equals( "IDS" ) ) {
            impl = new LineIDTXTSpecDataImpl( name );
        }
        else {
            throw new SplatException
                ( "Spectrum '" + name + "' has an unknown format" + 
                  " (guessed: " + format + " )" );
        }
        return impl;
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
        SpecDataImpl impl = null;
        impl = new FITSSpecDataImpl( specspec );

        // Table, if it is an table extension, or the data array size is 0
        // (may be primary).
        String exttype = impl.getProperty( "XTENSION" ).trim().toUpperCase();
        int dims[] = impl.getDims();
        if ( exttype.equals( "TABLE" ) || exttype.equals( "BINTABLE" ) ||
             dims == null || dims[0] == 0 ) {
            try {
                DataSource datsrc = new FileDataSource( specspec );
                StarTable starTable =
                    new FitsTableBuilder().makeStarTable( datsrc, true,
                                                          storagePolicy );
                impl = new TableSpecDataImpl( starTable, specspec,
                                              datsrc.getURL().toString() );
            }
            catch (SEDSplatException se) {
                throw se;
            }
            catch (Exception e) {
                throw new SplatException( "Failed to open FITS table", e );
            }
        }
        return impl;
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
            if ( starTable != null ) {
                return new TableSpecDataImpl( starTable );
            }
        }
        catch (Exception e) {
            tableException = e;
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
        try {
            
            //  Contact the resource.
           
            
            URLConnection connection = url.openConnection();

            //  Handle switching from HTTP to HTTPS, if a HTTP 30x redirect is
            //  returned, as Java doesn't do this by default (security issues
            //  when moving from secure to non-secure).
            if ( connection instanceof HttpURLConnection ) {
                int code = ((HttpURLConnection)connection).getResponseCode();
                if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                     code == HttpURLConnection.HTTP_MOVED_TEMP ) {
                    String newloc = connection.getHeaderField( "Location" );
                    URL newurl = new URL( newloc );
                    connection = newurl.openConnection();
                }
            }
            InputStream is = connection.getInputStream();

            //  And read it into a local file. Use the existing file extension
            //  if available and we're not guessing the type.
            namer = new PathParser( url.toString() );

            //  Create a temporary file. Use a file extension based on the
            //  type, if known.
            String stype = null;
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
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
        return namer;
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
            throw new SplatException( "Failed to open SED VOTable", e );
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
                        if ( "sed:Segment".equals( utype ) ) {
                            try {
                                table = new VOStarTable( (TableElement) child[j] );
                                specData = new SpecData( new TableSpecDataImpl(table) );
                                specList.add( specData );
                            }
                            catch (Exception e) {
                                throw new SplatException( e );
                            }
                        }
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
    public int mimeToSPLATType( String type )
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
        else if ( simpleType.startsWith( "spectrum/fits" ) ) {
            //  FITS format, is that image or table? Don't know who
            //  thought this was a mime-type?
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
            // VOTable spectrum, open as a table. Is really the SSAP native
            // XML representation so could an SED? In which case this might be
            // better as SpecDataFactory.SED, we'll see.
            stype = SpecDataFactory.TABLE;
        }
        else if ( simpleType.startsWith( "spectrum/votable" ) ||
                  simpleType.equals( "votable" ) ) {
            stype = SpecDataFactory.TABLE;

            //  XXX this used to be SpecDataFactory.SED with some note about
            //  the SDSS service requiring it. Don't see that service anymore
            //  so go back to the standard behaviour.
        }
        return stype;
    }

    //
    //  DataNode guessing.
    //
    public SpecData makeGuessedSpecData( String specspec, URL url )
        throws SplatException
    {
        SpecData specData = null;
        if ( dataNodeFactory == null ) {
            dataNodeFactory = new DataNodeFactory();
            SplatDataNode.customiseFactory( dataNodeFactory );
        }

        try {
            DataNode node =
                dataNodeFactory.makeDataNode( null, new File( specspec ) );
            specData = SplatDataNode.makeSpecData( node );
            if ( specData != null ) {
                specData.setShortName( url.toString() );
            }
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
        return specData;
    }

    public void setAuthenticator(SSAPAuthenticator auth) {
        authenticator=auth;
    }
}


