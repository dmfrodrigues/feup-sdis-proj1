mkdir -p testfiles
if ! [ -f testfiles/source_Release ]; then curl http://ftp.debian.org/debian/dists/jessie/main/source/Release -o testfiles/source_Release; fi # 102B
if ! [ -f testfiles/Release        ]; then curl http://ftp.debian.org/debian/dists/jessie/Release             -o testfiles/Release       ; fi # 77.3KB
if ! [ -f testfiles/ChangeLog      ]; then curl http://ftp.debian.org/debian/dists/jessie/ChangeLog           -o testfiles/ChangeLog     ; fi # 2.3MB
if ! [ -f testfiles/tux.png        ]; then curl https://www.kernel.org/theme/images/logos/tux.png             -o testfiles/tux.png       ; fi
if ! [ -f testfiles/feup.jpg       ]; then curl https://cdn.olhares.com/client/files/foto/big/601/6012552.jpg -o testfiles/feup.jpg      ; fi
if ! [ -f testfiles/debian-reference.en.pdf      ]; then curl https://www.debian.org/doc/manuals/debian-reference/debian-reference.en.pdf -o testfiles/debian-reference.en.pdf ; fi
