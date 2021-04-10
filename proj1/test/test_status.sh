#!/bin/bash

TIMEOUT=60
VERSION=1.0
PEER1_ID=1
PEER1_ID=2
SERVICE_ACCESS_POINT1=service1
SERVICE_ACCESS_POINT2=service2
MC_ADDR=230.0.0.1
MC_PORT=8888
MDB_ADDR=230.0.0.2
MDB_PORT=8888
MDR_ADDR=230.0.0.3
MDR_PORT=8888

test () {
    echo -en "$1\t"
    expected=$($3)
    output=$($2)
    if [ $? == 0 ] && [ "$output" == "$expected" ]; then
        echo -e "\e[1m\e[32m[Passed]\e[0m"
    else
        echo -e "\e[1m\e[31m[Failed]\e[0m"
        kill $PID1
        kill $PID2
        exit 1
    fi
}

cd build
rm -rf 1 2
if ! [ -f source_Release ]; then curl http://ftp.debian.org/debian/dists/jessie/main/source/Release -o source_Release; fi # 102B
if ! [ -f Release        ]; then curl http://ftp.debian.org/debian/dists/jessie/Release             -o Release       ; fi # 77.3KB
if ! [ -f ChangeLog      ]; then curl http://ftp.debian.org/debian/dists/jessie/ChangeLog           -o ChangeLog     ; fi # 2.3MB
timeout $TIMEOUT java PeerDriver $VERSION 1 service1 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID1=$!
timeout $TIMEOUT java PeerDriver $VERSION 2 service2 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID2=$!
echo "Started peers with PIDs $PID1, $PID2"
sleep 1
timeout $TIMEOUT java TestApp service1 BACKUP source_Release 1
timeout $TIMEOUT java TestApp service1 BACKUP Release 1

java TestApp service1 STATE
java TestApp service2 STATE

kill $PID1
kill $PID2
