# SDIS Project 1 - Distributed Backup Service

[![build](https://github.com/dmfrodrigues/feup-sdis-proj1/actions/workflows/build.yml/badge.svg)](https://github.com/dmfrodrigues/feup-sdis-proj1/actions/workflows/build.yml)
[![report](https://github.com/dmfrodrigues/feup-sdis-proj1/actions/workflows/report.yml/badge.svg)](https://github.com/dmfrodrigues/feup-sdis-proj1/actions/workflows/report.yml)

- **Project name:** Distributed Backup Service
- **Short description:** Distributed backup service across a local area network
- **Environment:** Unix/Windows console
- **Tools:** Java, RMI
- **Institution:** [FEUP](https://sigarra.up.pt/feup/en/web_page.Inicial)
- **Course:** [SDIS](https://sigarra.up.pt/feup/en/UCURR_GERAL.FICHA_UC_VIEW?pv_ocorrencia_id=459489) (Distributed Systems)
- **Project grade:** 18.2/20.0
- **Group:** g06
- **Group members:**
    - [Diogo Miguel Ferreira Rodrigues](https://github.com/dmfrodrigues) (up201806429@fe.up.pt)
    - [Breno Accioly de Barros Pimentel](https://github.com/BrenoAccioly) (up201800170@fe.up.pt)

Requires at least JDK 14.0.2 (2020-07-14)

## Compile

```sh
cd build
javac ../src/*.java ../src/sdis/*.java ../src/sdis/*/*.java -cp ../src -d .
```

## Run

```sh
cd build
java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT
```

Call `java PeerDriver` for more information on the meaning of each argument.

If the PeerDriver process is killed with SIGINT, it will exit gracefully and unregister itself from RMI.
