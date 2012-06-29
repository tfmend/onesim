#! /bin/sh
java -Xmx2048M -cp .:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
