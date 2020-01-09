# [![Tursi logo](https://raw.githubusercontent.com/schaetzc/tursi/master/docs/img/logo.png)](https://schaetzc.github.io/tursi/) Tursi

![Tursi screenshot](https://raw.githubusercontent.com/schaetzc/tursi/master/docs/img/gui.png)

## About

Tursi is a simulator for deterministic single-tape Turing machines.
I started it in 2013 at the end of my first semester to replace an
[outdated applet](http://ais.informatik.uni-freiburg.de/turing-applet/)
we were using in a lecture.

By now, Tursi seems rater outdated itself.
However, I publish it here in the hope that it will be useful to someone.

## Features

- Cross-platform Java application
- Debug your turing machine with history and break states
- Export turing tables to state diagrams
- Fast console mode for time-consuming turing machines
- Wildcards for the transition table

## Using Tursi

- Download the `.jar` file from the latest release.
- Install a Java Runtime Environment (JRE 8 or higher) if you haven't already.
	- Windows users should be able to start it by simply double clicking the `.jar` file.
	- Linux users may have to make the file executable first  
	  or start it from the console using `java -jar /path/to/Tursi.jar`. 

There is a comprehensive [tutorial](https://schaetzc.github.io/tursi/tutorial.html)
and a [manual](https://schaetzc.github.io/tursi/manual.html) on
[Tursi's website](https://schaetzc.github.io/tursi/).
For a quick start you may also want to look at the
[examples](https://schaetzc.github.io/tursi/dnld/tm-examples.zip)
in the [download section](https://schaetzc.github.io/tursi/downloads.html).

## Compiling

Tursi's first version was written in Java 1.6. Tursi itself hasn't changed much since then, but I made sure
that it can be compiled flawlessly with newer Java versions. Tursi's current version was tested with
Java 8 to 13. To compile it yourself ...

- Install a Java Development Kit (JDK 8 or higher) and Apache Ant if you haven't already.
- Open a terminal in this repo's root directory and run `ant`.
  This will create the file `bin/Tursi.jar`.

