#!/bin/bash

paths=$(cat estacionamento_caminhos.wkt | tr ',' '\n'| grep -v '^$')

max_x=0
max_y=0
min_x=9999
min_y=9999

while read line
do
    xy=$(echo $line | sed -e 's/[A-Z (]*\([0-9.]*\) \([0-9.]*\)[)]*/\1:\2/')
    x=$(echo $xy | awk -F: '{print $1}' | awk -F. '{print $1}')
    y=$(echo $xy | awk -F: '{print $2}' | awk -F. '{print $1}')

    if [ "$x" -lt "$min_x" ]; then min_x=$x; fi
    if [ "$y" -lt "$min_y" ]; then min_y=$y; fi

    if [ "$x" -gt "$max_x" ]; then max_x=$x; fi
    if [ "$y" -gt "$max_y" ]; then max_y=$y; fi
    
done < <(echo "$paths")

#echo $max_x, $max_y, $min_x, $min_y

i=1

while read line
do
    if [ "$line" != "" ]
    then
        #break
        i=$((++i))
        #echo $line | sed -e 's/POINT (\([0-9.]*\) \([0-9.]*\))/Group'$i'.nodeLocation=\1, \2/'
        xy=$(echo $line | sed -e 's/POINT (\([0-9]*\)[0-9.]* \([0-9]*\)[0-9.]*)/\1,\2/')
        x=$(echo $xy | awk -F, '{print $1}')
        y=$(echo $xy | awk -F, '{print $2}')

        echo Group${i}.nodeLocation=$((x-min_x)), $((max_y-y))
    fi
done < estacionamento_sensor.wkt

echo Scenario.nrofHostGroups=$i
