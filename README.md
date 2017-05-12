# AudioTactile

Generation of Audio-Tactile diagrams from SVG and XML.

## Building

Use [Maven](https://maven.apache.org/) to build. 

### Build Examples (Linux)

Build the command line app in the AudioTactile directory:

    mvn package appassembler:assemble

Clean build from anywhere:

    mvn -f PATH-TO/AudioTactile/pom.xml -e clean package appassembler:assemble

Build a simple jar file

    mvn package jar:jar

Build a jar file containing all dependencies

    mvn package assembly:single


## Running


### Command Line (Linux)

Execute via the assembled app in a command line. E.g.,

    ./target/appassembler/bin/atDiagram.sh samples/aspirin-enr.svg samples/aspirin-enr.cml 

or with some additional parameters:

    ./target/appassembler/bin/atDiagram.sh -v -d -o phys -i samples/Capacitor_resistor_series.svg samples/Capacitor_resistor_series.xml


### Jar File

Depending on what jar file you have build, either run

    java -jar target/audiotactile-0.1-SNAPSHOT.jar

This will only run if all dependencies are available in your CLASSPATH. 

Alternatively, run

    java -jar target/audiotactile-0.1-SNAPSHOT-jar-with-dependencies.jar

which includes all dependencies. This should work anywhere and is a fully portable jar.
