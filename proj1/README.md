# SDIS Project 1 - Distributed Backup Service

- **Project name:** Distributed Backup Service
- **Short description:** Distributed backup service across a local area network
- **Environment:** Unix/Windows console
- **Tools:** Java, RMI
- **Institution:** [FEUP](https://sigarra.up.pt/feup/en/web_page.Inicial)
- **Course:** [SDIS](https://sigarra.up.pt/feup/en/UCURR_GERAL.FICHA_UC_VIEW?pv_ocorrencia_id=459489) (Distributed Systems)
<!-- - **Project grade:** ??/20.0 -->
- **Group:** g06
- **Group members:**
    - [Breno Accioly de Barros Pimentel](https://github.com/BrenoAccioly) (up201800170@fe.up.pt)
    - [Diogo Miguel Ferreira Rodrigues](https://github.com/dmfrodrigues) (up201806429@fe.up.pt)

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
