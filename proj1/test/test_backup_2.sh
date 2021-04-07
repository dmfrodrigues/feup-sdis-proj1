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
    if [ $? == 0 ] && [ "$output" == "$expected" ]; then
        echo -e "\e[1m\e[32m[Passed]\e[0m"
    else
        echo -e "\e[1m\e[31m[Failed]\e[0m"
        kill $PID1
        kill $PID2
        kill $PID3
        kill $PID4
        kill $PID5
        exit 1
    fi
}

cd bin
rm -rf 1 2 3 4 5
curl http://ftp.debian.org/debian/dists/jessie/main/source/Release -o source_Release  # 102B
curl http://ftp.debian.org/debian/dists/jessie/Release             -o Release         # 77.3KB
timeout $TIMEOUT java PeerDriver $VERSION 1 service1 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID1=$!
timeout $TIMEOUT java PeerDriver $VERSION 2 service2 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID2=$!
timeout $TIMEOUT java PeerDriver $VERSION 3 service3 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID3=$!
timeout $TIMEOUT java PeerDriver $VERSION 4 service4 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID4=$!
timeout $TIMEOUT java PeerDriver $VERSION 5 service5 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID5=$!
echo "Started peers with PIDs $PID1, $PID2, $PID3, $PID4, $PID5"
sleep 1

test "test1-01-1" "timeout $TIMEOUT java TestApp service1 BACKUP source_Release 2" "echo"
sleep 2
FILE=3/storage/chunks/8C5A4F80497BC0C4719B9DCE7CCC75C36BCB3938A65FB65F7CC0CA0074279526
test "test1-01-2" "cat $FILE-0" "cat source_Release"

test "test1-02-1" "timeout $TIMEOUT java TestApp service1 BACKUP Release 2" "echo"
sleep 3
FILE=4/storage/chunks/14C33F2915CA0D86673BCF9A54BC42F73F8A31E0ED6B3EF0D203EAC500F9047D
test "test1-02-2" "cat $FILE-0 $FILE-1" "cat Release"

kill $PID1
kill $PID2
kill $PID3
kill $PID4
kill $PID5