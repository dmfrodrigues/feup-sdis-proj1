#!/bin/bash

TIMEOUT=60
VERSION=1.5
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
        kill $PID3
        kill $PID4
        kill $PID5
        exit 1
    fi
    echo $expected > expected.txt
    echo $output > output.txt
    if [ "$output" != "$expected" ]; then
        echo -e "\e[1m\e[31m[Failed]\e[0m: expected different from output"
        echo $expected > expected.txt
        echo $output > output.txt
        kill $PID1
        kill $PID2
        kill $PID3
        kill $PID4
        kill $PID5
        exit 1
    fi
    echo -e "\e[1m\e[32m[Passed]\e[0m"
}

cd build
rm -rf 1 2 3 4 5
curl http://ftp.debian.org/debian/dists/jessie/main/source/Release -o source_Release  # 102B
timeout $TIMEOUT java PeerDriver $VERSION 1 service1 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID1=$!
timeout $TIMEOUT java PeerDriver $VERSION 2 service2 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID2=$!
timeout $TIMEOUT java PeerDriver $VERSION 3 service3 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID3=$!
timeout $TIMEOUT java PeerDriver $VERSION 4 service4 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID4=$!
timeout $TIMEOUT java PeerDriver $VERSION 5 service5 $MC_ADDR $MC_PORT $MDB_ADDR $MDB_PORT $MDR_ADDR $MDR_PORT > /dev/null & PID5=$!
echo "Started peers with PIDs $PID1, $PID2, $PID3, $PID4, $PID5"
sleep 1

test "test-backup-4-01-1" "timeout $TIMEOUT java TestApp service1 BACKUP source_Release 2" "echo"
sleep 2
FILE=storage/chunks/8C5A4F80497BC0C4719B9DCE7CCC75C36BCB3938A65FB65F7CC0CA0074279526
contents=$(cat 2/$FILE-0 3/$FILE-0 4/$FILE-0 5/$FILE-0)
test "test-backup-4-01-2" "echo $contents" "cat source_Release source_Release"

kill $PID1
kill $PID2
kill $PID3
kill $PID4
kill $PID5