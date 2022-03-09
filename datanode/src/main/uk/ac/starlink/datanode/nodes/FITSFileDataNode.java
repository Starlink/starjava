package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import nom.tam.util.ArrayDataInput;
import uk.ac.starlink.oldfits.MappedFile;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * An implementation of the {@link DataNode} interface for
 * representing FITS objects stored on disk. 
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class FITSFileDataNode extends FITSDataNode {

    private File file;
    private String name;
    private FileDataSource fdatsrc;

    public FITSFileDataNode( FileDataSource fdatsrc ) 
            throws NoSuchDataException {
        super( fdatsrc );
        this.fdatsrc = fdatsrc;
        file = fdatsrc.getFile();
        name = file.getName();
        setLabel( name );
    }

    public FITSFileDataNode( File file ) throws NoSuchDataException {
        this( makeFileDataSource( file ) );
    }

    public String getName() {
        return name;
    }

    protected ArrayDataMaker getArrayData( final long start, final long size ) {
        return new ArrayDataMaker() {
            public ArrayDataInput getArrayData() throws IOException {
                RandomAccessFile raf = 
                    new RandomAccessFile( file.getPath(), "r" );
                FileChannel chan = raf.getChannel();
                ByteBuffer niobuf = chan.map( FileChannel.MapMode.READ_ONLY,
                                              start, size );
                chan.close();
                return new MappedFile( niobuf );
            }
            public DataSource getDataSource() {
                return fdatsrc;
            }
            public long getOffset() {
                return start;
            }
            public String toString() {
                return file + ":" + start + "+" + size;
            }
        };
    }

    public static FileDataSource makeFileDataSource( File file ) 
            throws NoSuchDataException {
        try {
            FileDataSource fdatsrc = new FileDataSource( file );
            fdatsrc.setName( file.getName() );
            return fdatsrc;
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

}
