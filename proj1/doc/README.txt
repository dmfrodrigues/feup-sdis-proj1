# SDIS Project 1 - Distributed Backup Service

- Breno Accioly de Barros Pimentel (up201800170@fe.up.pt)
- Diogo Miguel Ferreira Rodrigues (up201806429@fe.up.pt)

## Compile

cd build
javac ../src/*.java ../src/sdis/*.java ../src/sdis/*/*.java -cp ../src -d .

## Run

cd build
java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT

Call "java PeerDriver" for more information on the meaning of each argument.
