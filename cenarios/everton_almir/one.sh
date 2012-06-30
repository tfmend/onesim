#! /bin/sh
java -Xmx1024M -cp .:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
