package uk.ac.starlink.ttools;

import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.ttools.task.LineInvoker;

public class Stilts {

    private static ObjectFactory taskFactory_;
    private static ObjectFactory modeFactory_;
    static { init(); }

    public static void main( String[] args ) {
        new LineInvoker( "stilts", taskFactory_ ).invoke( args );
    }

    public static ObjectFactory getModeFactory() {
        return modeFactory_;
    }

    private static void init() {
        taskFactory_ = new ObjectFactory( Task.class );
        String taskPkg = "uk.ac.starlink.ttools.task.";
        taskFactory_.register( "tcopy", taskPkg + "TableCopy" );
        taskFactory_.register( "tpipe", taskPkg + "TablePipe" );
        taskFactory_.register( "votlint", taskPkg + "VotLint" );

        modeFactory_ = new ObjectFactory( ProcessingMode.class );
        String modePkg = "uk.ac.starlink.ttools.mode.";
        modeFactory_.register( "out", modePkg + "CopyMode" );
        modeFactory_.register( "tosql", modePkg + "JdbcMode" );
        modeFactory_.register( "meta", modePkg + "MetadataMode" );
        modeFactory_.register( "stats", modePkg + "StatsMode" );
        modeFactory_.register( "count", modePkg + "CountMode" );
        modeFactory_.register( "topcat", modePkg + "TopcatMode" );
    }

    
}
