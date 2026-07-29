package uk.ac.starlink.ant.types;

import org.apache.tools.ant.BuildException; 
import uk.ac.starlink.ant.tasks.UpdateDownloads;

/**
 * DownloadItem implementation for use as a nested tyep with the
 * {@link uk.ac.starlink.ant.tasks.UpdateDownloads} task.
 *
 * @author   Mark Taylor
 * @since    29 Jul 2026
 */
public class JarDownload implements UpdateDownloads.DownloadItem {

    private String repo_;
    private String owner_;
    private String name_;
    private String version_;

    public JarDownload() {
        repo_ = "https://repo1.maven.org/maven2";
    }

    public void setRepo( String repo ) {
        repo_ = repo;
    }

    public void setOwner( String owner ) {
        owner_ = owner;
    }

    public void setName( String name ) {
        name_ = name;
    }

    public void setVersion( String version ) {
        version_ = version;
    }

    public String getFilename() {
        return name_ + "-" + version_ + ".jar";
    }

    public String getUrl() throws BuildException {
        if ( repo_ == null || repo_.trim().length() == 0 ) {
            throw new BuildException( "Missing repo "
                                    + "(e.g. https://repo1.maven.org/maven2)" );
        }
        if ( owner_ == null || owner_.trim().length() == 0 ) {
            throw new BuildException( "Missing owner "
                                    + "(e.g. uk/ac/starlink)" );
        }
        if ( name_ == null || name_.trim().length() == 0 ) {
            throw new BuildException( "Missing name "
                                    + "(e.g. stil)" );
        }
        if ( version_ == null || version_.trim().length() == 0 ) {
            throw new BuildException( "Missing version "
                                    + "(e.g. 4.3.5)" );
        }
        return repo_ + "/" + owner_ + "/" + name_ + "/" + version_
             + "/" + getFilename();
    }
}
