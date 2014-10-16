prophet
=======

'prophet' is a set of tools for web sites classification

Build
=====

You should have: Gradle 2.0+, Java 8+.

```Shell
$ gradle install -Dwhere=/path/to/dir
```

The binary will be placed in `/path/to/dir/bin`

Command line interface
======================

'prophet' includes command line interface:

```Shell
Usage: prophet [options] [command] [command options]
  Options:
    -v, --verbose
       
       Default: false
  Commands:
    run      Run detection for specified URLs
      Usage: run [options] URLs
        Options:
          -d, --crawl-depth
             Site crawling depth. How deep to follow child pages.
             Default: 2
        * -kn, --knowledge
             File, containing prophet's knowledge

    learn      Gather the knowledge for catalogue
      Usage: learn [options]
        Options:
        * -c, --catalogue
             Catalogue file path.
          -d, --crawl-depth
             Site crawling depth. How deep to follow child pages.
             Default: 1
        * -kn, --knowledge
             File, where to put learned knowledge

    fetch-catalogue      Extra utility to build websites catalogue by parsing remote catalogue site.
      Usage: fetch-catalogue [options]
        Options:
        * -c, --catalogue
             Where to store parsed catalogue.
        * -l, --links
             File, containing definition of tags and their appropriate catalogue
             section URLs.
          -p, --pages
             Maximum number of pages to parse.
             Default: 20
        * -t, --type
             Remote catalogue type. Supported: 'yandex', 'alexa'.

    benchmark      
      Usage: benchmark [options]
        Options:
        * -c, --catalogue
             Where to store parsed catalogue.
          -d, --crawl-depth
             Site crawling depth. How deep to follow child pages.
             Default: 2
        * -kn, --knowledge
             File, containing prophet's knowledge

    help      Displays help for various commands
      Usage: help [options] 
```
