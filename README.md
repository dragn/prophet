prophet
=======

`prophet` is a set of tools for web sites classification.

Why you may need it? For building your own site classifier! You may categorize any sites with your custom categories. To achieve this you only need to provide enough data for learning.
`prophet` also aims to provide the tools for building learning dataset. For now, there is only one method supported - parsing Yandex web sites catalogues sites. But you are free to implement your own (refer to `CatalogueFetcher` class).

Disclaimer
==========

I do not, in any way, support this software now. I do not guarantee that any of it works. Basically, WTFPL license is applied to this code: http://www.wtfpl.net/.

This repository is open only because I need room for private repos. But, if you have any questions or considerations about this code, please feel free to contact me.

Build
=====

You should have: Java 8+.

```Shell
$ ./gradlew install -Dwhere=/path/to/dir
```

The binary will be placed in `/path/to/dir/bin`

Command line interface
======================

'prophet' includes command line interface:

```Shell
$ prophet help
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

    benchmark      Run prediction quality evaluation against provided data
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
