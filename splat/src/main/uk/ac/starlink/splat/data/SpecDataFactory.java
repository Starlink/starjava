/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     01-SEP-2000 (Peter W. Draper):
 *        Original version.
 *     01-MAR-2004 (Peter W. Draper):
 *        Added table handling changes.
 *     26-AUG-2004 (Peter W. Draper):
 *        Added support for URLs.
 */
package uk.ac.starlink.splat.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.net.MalformedURLException;

import uk.ac.starlink.splat.imagedata.NDFJ;
import uk.ac.starlink.splat.util.SplatException;

import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.TemporaryFileDataSource;
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
    //
    // Enumeration of the "types" of spectrum that can be created.
    //

    /** The type should be determined only using file name rules */
    public final static int DEFAULT = 0;

    /** The file is a FITS file. */
    public final static int FITS = 1;

    /** The file is a HDS file. */
    public final static int HDS = 2;

    /** The file is a TEXT file. */
    public final static int TEXT = 3;

    /** The file is an HDX file. */
    public final static int HDX = 4;

    /** The file is a table. */
    public final static int TABLE = 5;

    /** The file is an line identifier file. */
    public final static int IDS = 6;

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
        "line ids"
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
        {"xml"},
        {"*"},
        {"ids"}
    };


    /**
     * Treeview icons symbolic names for each data type. XXX may want
     * to extend this with own (line identifiers clearly not available).
     */
    public final static String[] treeviewIcons = {
        "FILE",
        "FITS",
        "NDF",
        "DATA",
        "NDX",
        "TABLE",
        "ARY2"
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
        if ( type == DEFAULT ) {
            return get( specspec );
        }

        //  The specification could be for a local or remote file and in URL
        //  or local file format. We need to know so that we can make a local
        //  copy and construct a RemoteSpecData.
        NameParser namer = new NameParser( specspec );
        boolean isRemote = namer.isRemote();
        if ( namer.isRemote() && type != TABLE && type != HDX ) {
            PathParser pathParser = remoteToLocalFile( namer.getURL() );
            specspec = pathParser.ndfname();
        }

        SpecDataImpl impl = null;
        switch (type)
        {
            case FITS: {
                impl = makeFITSSpecDataImpl( specspec );
            }
            break;
            case HDS: {
                impl = makeNDFSpecDataImpl( specspec );
            }
            break;
            case TEXT: {
                impl = new TXTSpecDataImpl( specspec );
            }
            break;
            case HDX: {
                //  HDX should download remote files as it needs to keep the
                //  basename to locate other references.
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
                throw new SplatException( "Spectrum '" + specspec + "' supplied"+
                                          " with an unknown type: " + type );
            }
        }
        if ( impl == null ) {
            throwReport( specspec, true );
        }
        return makeSpecDataFromImpl( impl, isRemote, namer.getURL() );
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
        // NOTE: This method may be used by TOPCAT classes.

        SpecDataImpl impl = null;
        URL url = null;
        boolean isRemote = false;

        //  See what kind of specification we have.
        try {
            NameParser namer = new NameParser( specspec );
            isRemote = namer.isRemote();
            url = namer.getURL();

            //  Remote HDX/VOTable-like files should be downloaded by the
            //  library. A local copy loses the basename context.
            if ( isRemote && namer.getFormat().equals( "XML" ) ) {
                impl = makeXMLSpecDataImpl( specspec, true, url );
            }
            else {
                //  Remote plainer formats (FITS, NDF) need a local copy.
                if ( isRemote ) {
                    PathParser p = remoteToLocalFile( url );
                    if ( p != null ) {
                        namer = new NameParser( p.ndfname() );
                    }
                }
                impl = makeLocalFileImpl( namer.getName(), namer.getFormat() );
            }
        }
        catch (Exception e) {
            impl = null;
        }

        //  Try construct an intelligent report.
        if ( impl == null ) {
            throwReport( specspec, false );
        }
        return makeSpecDataFromImpl( impl, isRemote, url );
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
                ( "Spectrum '" + name + "' has an unknown format." );
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
        try {
            impl = new FITSSpecDataImpl( specspec );
        }
        catch (SplatException e ) {
            e.printStackTrace();
        }

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
    private void throwReport( String specspec, boolean typed )
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
                    throw new SplatException( "Spectrum '" + specspec +
                                              "' has an unknown type, "+
                                              "format or name syntax." );
                }
            }
            else {
                throw new SplatException( "Cannot read: " + specspec );
            }
        }
        else {
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
        String sourceType = source.getDataFormat();

        //  Create an implementation object using the source to
        //  provide the content (TODO: could be more efficient?).
        SpecDataImpl impl = null;

        if ( source instanceof LineIDSpecData ) {
            impl = new LineIDTXTSpecDataImpl( specspec,
                                              (LineIDSpecData) source );
        }
        else if ( targetType.equals( "NDF" ) ) {
            impl = new NDFSpecDataImpl( namer.getName(), source );
        }
        if ( targetType.equals( "FITS" ) ) {
            impl = new FITSSpecDataImpl( namer.getName(), source );
        }
        if ( targetType.equals( "TEXT" ) ) {
            impl = new TXTSpecDataImpl( namer.getName(), source );
        }
        SpecData specData = makeSpecDataFromImpl( impl, false, null );
        specData.setType( source.getType() );
        return specData;
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
        String sourceType = source.getDataFormat();

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
                   impl = new NDFSpecDataImpl( specspec, source );
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
     * Return a list of the supported table formats.
     */
    public List getKnownTableFormats()
    {
        return TableSpecDataImpl.getKnownFormats();
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
            return new LineIDSpecData
                ( new LineIDMEMSpecDataImpl( shortname, specData ) );
        }
        else {
            return new EditableSpecData
                ( new MEMSpecDataImpl( shortname, specData ) );
        }
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
        EditableSpecData editableSpecData =
            createEditable( shortname, specData );
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
                newImpl = new LineIDTXTSpecDataImpl( specspec );
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
     * Given a URL for a remote resource make a local, temporary, copy. The
     * temporary file should have the correct file extension for the type of
     * remote data an {@link PathParser} is returned as the result (null
     * if a failure occurs).
     */
    protected PathParser remoteToLocalFile( URL url )
    {
        //  XXX how to determine the format, mime types and files
        //  types are the obvious way, but mime types are probably
        //  rarely available, so we will need to use the usual file
        //  extensions mechanisms. I'd like to let treeview sort this
        //  out, but treeview isn't a guaranteed dependency of SPLAT!

        PathParser namer = null;
        try {
            //  Contact the resource.
            InputStream is = url.openStream();

            //  And read it into a local file. Use the existing file extension
            //  if available, without a file extension this will currently
            //  fail.
            namer = new PathParser( url.getPath() );

            //  Create a temporary file.
            TemporaryFileDataSource datsrc =
                new TemporaryFileDataSource( is, url.toString(), "SPLAT",
                                             namer.type(), null );
            namer.setPath( datsrc.getFile().getCanonicalPath() );
            datsrc.close();
        }
        catch (Exception e) {
            //e.printStackTrace();
            namer = null;
        }
        return namer;
    }

    //  Types of reprocessing of 2D data files. The default is VECTORIZE
    //  which implementations should have already performed.
    public final static int COLLAPSE = 0;
    public final static int EXPAND = 1;
    public final static int VECTORIZE = 2;

    /**
     * Process a SpecData object that isn't really 1D into another
     * representation of itself. There are several ways that this reprocessing
     * can be performed:
     * <ul>
     *   <li>Collapse along the dispersion axis</li>
     *   <li>Expansion into a spectrum per line of the original data</li>
     *   <li>Vectorisation of the original data into a single spectrum</li>
     * </ul>
     * To be reprocessable a SpecData must have an implementation that is 2D
     * (or reducible to 2D).  Higher dimensions are not supported and 1D
     * spectrum require no reprocessing. In both these cases a null is
     * returned.
     *
     * @param specData the SpecData object to reprocess.
     * @param method the method to use when reprocessing, COLLAPSE, EXPAND or
     *               VECTORIZE.
     */
    public SpecData[] reprocessTo1D( SpecData specData, int method )
        throws SplatException
    {
        SpecData[] results = null;
        SpecDataImpl impl = specData.getSpecDataImpl();
        int[] dims = impl.getDims();
        if ( dims.length == 1 ) {
            return results;
        }

        // XXX Deal with special data formats. IRAF, SDSS.
        switch (method)
        {
           case COLLAPSE: {
               results = new SpecData[1];
               results[0] = collapseSpecData( specData );
           }
           break;
           case EXPAND: {
               results = expandSpecData( specData );
           }
           break;
           case VECTORIZE: {
               //  All spectra are already in this form.
               results = new SpecData[1];
               results[0] = specData;
           }
           break;
        }
        return results;
    }

    /**
     * Create a new SpecData instance by collapsing its 2D implementation
     * along the dispersion axis. This creates a new 1D SpecData object.
     */
    private SpecData collapseSpecData( SpecData specData )
        throws SplatException
    {
        SpecDataImpl newImpl = new CollapsedSpecDataImpl( specData );
        return new SpecData( newImpl );
    }

    /**
     * Create a set of new 1D SpecData instances by extracting each line of a
     * 2D implementation along the dispersion axis.
     */
    private SpecData[] expandSpecData( SpecData specData )
    {
        return null;
    }
}
