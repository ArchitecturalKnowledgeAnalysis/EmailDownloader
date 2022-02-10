# ApacheEmailDownloader
Program which automatically downloads email archives from lists.apache.org

This program can be run as a simple command-line utility, using Java 17 or higher.

Usage information, as per `java -jar ApacheEmailDownloader.jar --help`:
```
Usage: ApacheEmailDownloader [-hV] -d=<domainName> [--from=<fromDate>]
                             -l=<listName> [-o=<outputDir>]
                             [--until=<untilDate>]
  -d=<domainName>            The domain to download from. For example, "hadoop.
                               apache.org"
      --from=<fromDate>      Latest period to download from, inclusive.
                               Formatted as yyyy-mm.
  -h, --help                 Show this help message and exit.
  -l=<listName>              Name of the mailing list to download from.
  -o, --output=<outputDir>   Directory to place downloaded files in. Will
                               create it if it doesn't exist.
      --until=<untilDate>    Earliest period to download from, inclusive.
                               Formatted as yyyy-mm.
  -V, --version              Print version information and exit.

Process finished with exit code 0
```

