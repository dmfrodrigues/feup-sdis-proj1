# SDIS Project 1 - Distributed Backup Service

- Breno Accioly de Barros Pimentel (up201800170@fe.up.pt)
- Diogo Miguel Ferreira Rodrigues (up201806429@fe.up.pt)

Requires at least JDK 14.0.2 (2020-07-14)

## Compile

cd build
javac ../src/*.java ../src/sdis/*.java ../src/sdis/*/*.java -cp ../src -d .

## Run

cd build
java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT

Call "java PeerDriver" for more information on the meaning of each argument.

If the PeerDriver process is killed with SIGINT, it will exit gracefully and unregister itself from RMI.
