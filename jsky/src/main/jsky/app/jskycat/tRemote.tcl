#!/usr/local/bin/tcl
#
# tRemote.tcl 
#
# Test the remote socket interface to jskycat.
#
# Usage: First start jskycat with the '-port 9624' option.
#        Make sure there are a bunch of FITS files of the same size in ./test.
#        Then run 'tcl tRemote.tcl'.
#
#

# Host where jskycat is running
set host localhost

# port number passed to jskycat with the port option
set port 9624

# open a socket to a running jskycat application and return the file
# descriptor for remote commands

proc connect_to_jskycat {host port} {
    set fd [server_connect -nobuf $host $port]
    return $fd
}

# send the command to jskycat and return the results or generate an error

proc send_to_jskycat {args} {
    global jskycat_fd
    puts $jskycat_fd $args
    lassign [gets $jskycat_fd] status length
    set result {}
    if {$length > 0} {
	set result [read $jskycat_fd $length]
    }
    if {$status != 0} {
	error $result
    }
    return $result
}


# test usage of the jskycat image update command
proc test1 {} {
    puts "testing image update with the jskycat 'update' command"
    set files [glob "[pwd]/test/*.fits"]
    set tmpfile [pwd]/tmp.fits
    exec cp [lindex $files 0] $tmpfile
    send_to_jskycat config -file $tmpfile
    while {1} {
	foreach file $files {
	    exec cp $file $tmpfile
	    set ms [lindex [time "send_to_jskycat update" 1] 0]
	    puts "$file: [expr $ms/1000000.] secs"
	    after 500
	}
    }
}

# test image update with the jskycat "config -file" command
proc test2 {} {
    puts "testing image update with the jskycat 'config -file' command"
    set files [glob "[pwd]/test/*.fits"]
    set tmpfile [pwd]/tmp.fits
    while {1} {
	foreach file $files {
	    exec cp $file $tmpfile
	    set ms [lindex [time "send_to_jskycat config -file $tmpfile" 1] 0]
	    puts "$file: [expr $ms/1000000.] secs"
	    after 500
	}
    }
}

# test image update with the jskycat "config -file" command
proc test3 {} {
    puts "testing image update with the jskycat 'config -file' command and multiple files"
    set files [glob "[pwd]/test/*.fits"]
    while {1} {
	foreach file $files {
	    set ms [lindex [time "send_to_jskycat config -file $file" 1] 0]
	    puts "$file: [expr $ms/1000000.] secs"
	    after 500
	}
    }
}

# test image update with the jpeg files
proc test4 {} {
    puts "testing image update with the jskycat 'config -file' command and multiple files"
    set files [glob "[pwd]/test/*.jpg"]
    while {1} {
	foreach file $files {
	    set ms [lindex [time "send_to_jskycat config -file $file" 1] 0]
	    puts "$file: [expr $ms/1000000.] secs"
	    after 500
	}
    }
}

puts "testing the JSkyCat remote interface..."

# open the connection
set jskycat_fd [connect_to_jskycat $host $port]

# send some commands to JSkyCat to be evaluated 
puts "WCS image center is: [send_to_jskycat wcscenter]"

test3
