# SCIP: Single-Cell Image Processing

SCIP is a software suite optimized for processing single-cell images,
such as those acquired on an imaging flow cytometer. It is built on 
the SciJava framework, more specifically it uses the following
components:
- ImgLib2,
- SCIFIO,
- BioFormats,
- ImageJ-ops,
- ImageJ Common
- and SciJava Common.

SCIP can be used as a plugin in ImageJ, or a as a standalone CLI.

A JAR and accompanying libraries folder can be built by cloning this repository and
executing:
```bash
mvn clean package
```
The JAR can be run by adding the resulting `target/lib` and `target` to the Java `CLASSPATH`
and executing:
```bash
java be.maximl.app.FeatureApp
```
Calling without arguments shows the usage information.