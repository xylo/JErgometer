#!/bin/bash

DIR=`dirname $0`
cd $DIR/..

# check for Linux or Mac
OS=`uname -s`
case $OS in
	Darwin)
		OS=MacOSX
		JFLAGS=-d32
		;;
esac

# check for 32/64 bit
ARCH=`uname -m`
case $ARCH in
	i686)
		ARCH=i386
		;;
	x86_64)
		ARCH=amd64
		;;
esac

java $JFLAGS -Djava.library.path=lib/dlls/$OS/$ARCH -cp jergometer.jar:lib/RXTXcomm.jar:lib/velocity-1.7-dep.jar org.jergometer.Jergometer "$@"
