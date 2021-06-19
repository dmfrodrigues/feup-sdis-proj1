#! /usr/bin/bash

set -ex

rm -rf src/build

cd src
../scripts/compile.sh
cd build
../../scripts/setup_testfiles.sh
rmiregistry & PID_RMI=$!
../../scripts/peer.sh 1.0 1 service1 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888 & PID1=$!
../../scripts/peer.sh 1.0 2 service2 230.0.0.1 8888 230.0.0.2 8888 230.0.0.3 8888 & PID2=$!
sleep 1
cp testfiles/source_Release .
../../scripts/test.sh service1 BACKUP source_Release 1
sleep 2
rm source_Release
../../scripts/test.sh service1 RESTORE source_Release
sleep 2
diff source_Release testfiles/source_Release
../../scripts/test.sh service1 DELETE source_Release
sleep 2
../../scripts/cleanup.sh 1
../../scripts/cleanup.sh 2

kill $PID1
kill $PID2
kill $PID_RMI
