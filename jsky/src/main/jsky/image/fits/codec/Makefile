# ESO Archive
# 
# $Id: Makefile,v 1.3 2002/08/16 22:21:13 brighton Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	FITSDataByte.java \
	FITSDataShort.java \
	FITSDataInt.java \
	FITSDataFloat.java \
	FITSDataDouble.java \
	FITSData.java \
	FITSDecodeParam.java \
	FITSEncodeParam.java \
	FITSImage.java \
	FITSCodec.java \
	FITSDecoder.java \
	FITSEncoder.java


# Java package name
PACKAGE_DIR = jsky/image/fits/codec

# Path to top level dir
TOP = ../../../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: all
	(cd ../../../app/jskycat; $(MAKE) test1d)


# Generate FITSData<type> sources from FITSDataFloat
DATA_TYPES = Byte Short Int Float Double
generate:
	for i in ${DATA_TYPES}; do cp FITSData$$i.java FITSData$$i.java.old; done
	sed -e s/Float/Byte/g  \
	    -e s/float/byte/g  \
	    -e s/javax.media.jai.DataBuffer/java.awt.image.DataBuffer/ \
	    -e 's/byteBuffer.asByteBuffer()/byteBuffer/' \
	     < FITSDataFloat.java > FITSDataByte.java
	sed -e s/Float/Int/g  \
	    -e s/float/int/g  \
	    -e s/javax.media.jai.DataBuffer/java.awt.image.DataBuffer/g \
	     < FITSDataFloat.java > FITSDataInt.java
	sed -e s/Float/Short/g \
            -e s/float/short/g \
            -e s/javax.media.jai.DataBuffer/java.awt.image.DataBuffer/g \
            < FITSDataFloat.java > FITSDataShort.java
	sed -e s/Float/Double/g \
            -e s/float/double/g \
            < FITSDataFloat.java > FITSDataDouble.java
