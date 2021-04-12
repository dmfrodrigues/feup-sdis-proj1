#!/bin/bash

TIMEOUT=100
VERSION=1.0
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
    if [ $? != 0 ]; then
        echo -e "\e[1m\e[31m[Failed]\e[0m: return code is not zero"
        kill $PID1
        kill $PID2
        exit 1
    fi
    echo $expected > expected.txt
    echo $output > output.txt
    if ! diff expected.txt output.txt > /dev/null ; then
        echo -e "\e[1m\e[31m[Failed]\e[0m: expected different from output"
        kill $PID1
        kill $PID2
        exit 1
    fi
    echo -e "\e[1m\e[32m[Passed]\e[0m"
}

cd build
rm -rf 1 2
mkdir -p testfiles
if ! [ -f testfiles/source_Release ]; then curl http://ftp.debian.org/debian/dists/jessie/main/source/Release -o testfiles/source_Release; fi # 102B
if ! [ -f testfiles/Release        ]; then curl http://ftp.debian.org/debian/dists/jessie/Release             -o testfiles/Release       ; fi # 77.3KB
if ! [ -f testfiles/ChangeLog      ]; then curl http://ftp.debian.org/debian/dists/jessie/ChangeLog           -o testfiles/ChangeLog     ; fi # 2.3MB
timeout $TIMEOUT java PeerDriver $VERSION 1 service1 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID1=$!
timeout $TIMEOUT java PeerDriver $VERSION 2 service2 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID2=$!
echo "Started peers with PIDs $PID1, $PID2"
sleep 1

cp testfiles/source_Release .
timeout $TIMEOUT java TestApp service1 BACKUP source_Release 1
sleep 2
rm source_Release
timeout $TIMEOUT java TestApp service1 RESTORE source_Release
sleep 2
test "test-restore-1-01" "cat source_Release" "cat testfiles/source_Release"

cp testfiles/Release .
timeout $TIMEOUT java TestApp service1 BACKUP Release 1
sleep 3
rm Release
timeout $TIMEOUT java TestApp service1 RESTORE Release
sleep 2
test "test-restore-1-02" "cat Release" "cat testfiles/Release"

cp testfiles/ChangeLog .
timeout $TIMEOUT java TestApp service1 BACKUP ChangeLog 1
sleep 40
rm ChangeLog
timeout $TIMEOUT java TestApp service1 RESTORE ChangeLog
sleep 40
test "test-restore-1-03" "cat ChangeLog" "cat testfiles/ChangeLog"

kill $PID1
kill $PID2
