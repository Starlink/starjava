#+
#  Name:
#     SrcReader.pm

#  Purpose:
#     Provide functions for extracting SST-type comments from AST source.

#  Usage:
#     use SrcReader

#  Type of Module:
#     Perl module.

#  Description:
#     This module enables wholesale snarfing of SST-type comments from
#     prologues in the C source files comprising the AST package.
#     Functions are provided which pull out prologue 'Name' words, 
#     'Parameters' block names and descriptions, and 'Purpose', 
#     'Description' and other blocks from class, method and 
#     attribute prologues.
#
#     For a perl script to use the module, it must contain the 
#     'use SrcReader' invocation.  The script must also be invoked
#     with the name of the AST C source file to be plundered as 
#     its first command line argument.

#  Author:
#     MBT: Mark Taylor (Starlink)

#  History:
#     1-OCT-2001 (MBT):
#        Original version.
#-


package SrcReader;
use Exporter;
use warnings FATAL => qw(all);

@ISA = qw( Exporter );
@EXPORT = qw(
   ClassPurpose
   ClassDescrip
   FuncPurpose
   FuncDescrip
   ArgDescrip
   ReturnDescrip
   AttPurpose
   AttDescrip
   AttApplic
);

use strict;

my $classPurpose = "";
my $classDescrip = "";
my %funcPurpose = ();
my %funcDescrip = ();
my %argDescrip = ();
my %returnDescrip = ();
my %attPurpose = ();
my %attDescrip = ();
my %attApplic = ();


my $chr1Use_rx = '[c\*]';
my $chr1Ignore_rx = '[f]';
my @ignoreStanzas = qw( Name Purpose Description Applicability Return
                        Type Synopsis Bugs Constructor Inheritance
                        Attribute Copyright Function Author History 
                        Class Parameters Examples 
                        Sub-Pixel
                        Control
                        Data );
my $ignoreStanzas_rx = '(' . join( '|', @ignoreStanzas ) . ')';



sub ClassPurpose {
   return $classPurpose;
}

sub ClassDescrip {
   return $classDescrip;
}

sub FuncPurpose {
   my( $name ) = namify( shift );
   return $funcPurpose{ $name } || $funcPurpose{ "ast$name" };
}

sub FuncDescrip {
   my( $name ) = namify( shift );
   return $funcDescrip{ $name } || $funcDescrip{ "ast$name" };
}

sub ArgDescrip {
   my( $funcname ) = namify( shift );
   my( $argname ) = namify( shift );
   return ${$argDescrip{ $funcname }}{ $argname }  
       || ${$argDescrip{ "ast$funcname" }}{ $argname };
}

sub ReturnDescrip {
   my( $funcname ) = namify( shift );
   return $returnDescrip{ $funcname } || $returnDescrip{ "ast$funcname" };
}

sub AttPurpose {
   my( $name ) = namify( shift );
   return $attPurpose{ $name };
}

sub AttDescrip {
   my( $name ) = namify( shift );
   return $attDescrip{ $name };
}

sub AttApplic {
   my( $name ) = namify( shift );
   return $attApplic{ $name };
}


#  Get the name of the source file we need.
my( $srcname ) = $ARGV[ 0 ] or die( "Usage: $0 sourcefile\n" );

#  Open and read all the (non-fortran) comment lines from the source file.
open( SRC, $srcname ) or die( "Failed to open '$srcname'\n" );
my( $fileText ) = "*\n";
while ( <SRC> ) {
   s/^[c]/\*/;
   if ( /^\*/ || /^ *$/ ) {
      $fileText .= $_;
   }
}
close( SRC );

#  Parse the source file.

$fileText =~ /\n\*class\+\+ *\n(.*?)\*class\-\- *\n/s;
my( $classPrologue ) = $1;
$classPurpose = getPurpose( $classPrologue );
$classDescrip = getDescrip( $classPrologue );

while ( $fileText =~ /\n\*att\+\+ *\n(.*?)\*att\-\- *\n/gs ) {
   my( $attPrologue ) = $1;
   my( $attName ) = namify( getName( $attPrologue ) );
   $attPurpose{ $attName } = getPurpose( $attPrologue );
   $attDescrip{ $attName } = getDescrip( $attPrologue );
   $attApplic{ $attName } = getApplic( $attPrologue );
}

while ( $fileText =~ /\n\*\+\+ *\n(.*?)\*\-\- *\n/gs ) {
   my( $funcPrologue ) = $1;
   my( $funcName ) = namify( getName( $funcPrologue ) );
   $funcPurpose{ $funcName } = getPurpose( $funcPrologue );
   $funcDescrip{ $funcName } = getDescrip( $funcPrologue );
   $returnDescrip{ $funcName } = getReturn( $funcPrologue );
   my( $args ) = getArgs( $funcPrologue ) . 'X';
   while ( $args =~ /^(\S.*?)(?=\n\S)/gsm ) {
      my( $argBlock ) = $1 . "\n";
      $argBlock =~ /^(\w+)[^\n]*\n(.*)/s;
      my( $argName ) = $1;
      my( $argDescrip ) = $2;
      if ( $argName && $argDescrip ) {
         $argName = namify( $argName );
         $argDescrip = strip( 3, $argDescrip );
         ${$argDescrip{ $funcName }}{ $argName } = $argDescrip;
      }
   } 
}



sub getName {
   my( $name ) = getStanza( $_[ 0 ], "Name" );
   $name =~ /(\w[a-zA-Z_0-9<>]*)/;
   $name = $1;
   return $name;
}
   
sub getPurpose {
   my( $purpose ) = getStanza( $_[ 0 ], "Purpose" );
   $purpose = strip( 6, $purpose );
   $purpose =~ s/\?\s*$//;
   return $purpose;
}

sub getDescrip1 {
   my( $descrip ) = getStanza( $_[ 0 ], "Description" );
   $descrip = strip( 6, $descrip );
   return $descrip;
}

sub getDescrip {
   my( $prologue ) = $_[ 0 ] . "*  X";
   my( $descrip ) = getStanza( $prologue, "Description" );
   $descrip = strip( 6, $descrip );
   while ( $prologue =~ /\*  (\w[\w ]*):? *\n(.*?)(?=\n\*  \S)/sg ) {
      my( $heading ) = $1;
      my( $block ) = $2;
      if ( $heading !~ /^$ignoreStanzas_rx/i ) {
         # print STDERR "$heading\n";
         $descrip .= "<h4>$heading</h4>\n";
         $descrip .= strip( 6, $block );
      }
   }
   return $descrip;
}

sub getApplic {
   my( $applic ) = getStanza( $_[ 0 ], "Applicability" );
   $applic = strip( 6, $applic );
   $applic = "<dl>\n" . $applic . "\n</dl>\n";
   $applic =~ s%^([A-Z][A-Za-z]+) *$%<dt>$1</dt><dd>%mg;
   return $applic;
}

sub getReturn {
   my( $return ) = getStanza( $_[ 0 ], "Return" );
   $return = strip( 6, $return );
   if ( $return =~ /^\S.*\n   \S/ ) {
      $return =~ s/^.*\n//;
      $return = strip( 3, $return );
   }
   return $return;
}

sub getArgs {
   my( $args ) = getStanza( $_[ 0 ], "(?:Parameter|Argument)" );
   $args = strip( 6, $args );
   return $args;
}

sub getStanza {
   my( $prologue ) = shift;
   $prologue .= '*  X';
   my( $name_rx ) = shift;
   $prologue =~ /\*  ${name_rx}[^\n]*\n(.*?)(?=\n\*  \S)/s;
   my( $stanza ) = $1;
   return $stanza;
}

sub strip {
   my( $num, $text ) = @_;
   my( $rx ) = ".{0,$num}";
   $text =~ s/^$rx//gm;
   return $text;
}


sub namify {
   my( $text ) = $_[ 0 ] || "";
   if ( $text ) {
      $text =~ tr/A-Z/a-z/;
      $text =~ /^\s*(\w[A-Za-z_0-9<>]*)/;
      return $1;
   }
   else {
      return "";
   }
}

1;

# $Id$
