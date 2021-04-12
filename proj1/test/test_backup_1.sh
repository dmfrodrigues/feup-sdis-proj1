#!/bin/bash

TIMEOUT=60
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
cp testfiles/* .
timeout $TIMEOUT java PeerDriver $VERSION 1 service1 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID1=$!
timeout $TIMEOUT java PeerDriver $VERSION 2 service2 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID2=$!
echo "Started peers with PIDs $PID1, $PID2"
sleep 1

test "test-backup-1-01-1" "timeout $TIMEOUT java TestApp service1 BACKUP source_Release 1" "echo"
sleep 2
FILE=2/storage/chunks/8C5A4F80497BC0C4719B9DCE7CCC75C36BCB3938A65FB65F7CC0CA0074279526
test "test-backup-1-01-2" "cat $FILE-0" "cat source_Release"

test "test-backup-1-02-1" "timeout $TIMEOUT java TestApp service1 BACKUP Release 1" "echo"
sleep 3
FILE=2/storage/chunks/14C33F2915CA0D86673BCF9A54BC42F73F8A31E0ED6B3EF0D203EAC500F9047D
test "test-backup-1-02-2" "cat $FILE-0 $FILE-1" "cat Release"

test "test-backup-1-03-1" "timeout $TIMEOUT java TestApp service1 BACKUP ChangeLog 1" "echo"
sleep 40
FILE=2/storage/chunks/92A9228D827C2065A9E6A907E2369E0B2FE49C2647E7DCF06F30543E24A2F211
test "test-backup-1-03-2" "cat $FILE-0 $FILE-1 $FILE-2 $FILE-3 $FILE-4 $FILE-5 $FILE-6 $FILE-7 $FILE-8 $FILE-9 $FILE-10 $FILE-11 $FILE-12 $FILE-13 $FILE-14 $FILE-15 $FILE-16 $FILE-17 $FILE-18 $FILE-19 $FILE-20 $FILE-21 $FILE-22 $FILE-23 $FILE-24 $FILE-25 $FILE-26 $FILE-27 $FILE-28 $FILE-29 $FILE-30 $FILE-31 $FILE-32 $FILE-33 $FILE-34 $FILE-35" "cat ChangeLog"

kill $PID1
kill $PID2
