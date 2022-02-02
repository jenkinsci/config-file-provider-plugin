Contributing
============

This plugin is heavily dependent on Maven. 
Even to some of its side effects. 
It therefore can be challenging to execute and debug it in an IDE. 
IDEs are known not to be of equal cleverness when it comes to interpret Maven `pom.xml`.
This can be frustrating during a first encounter with this plugin. 
Stepping through test code is indeed a good way to explore and understand how the plugin works.

This is one possible way and sequence to solve the issue. 
Mileage may depend on particular situations. 
It is confirmed to work reliably with IntelliJ IDE 2021.3 (CE).

It is assumed that you start by having downloaded a fresh copy of the plugin's sources (aka `git clone`).

Pre-flight check:
-----------------
* Open a terminal window and position yourself in the project's directory
* Verify that you have Java 1.8 JDK correctly configured (`java -version`)
* Verify that maven is correctly configured to use the correct java version (`mvn --version`)

Process:
--------
* *Before* starting the IDE, compile the project with Maven and run the tests (`mvn clean verify -ntp`).
  The `-ntp` option avoids the distracting display of download progress. 
* Open the IntelliJ IDE and make sure to configure it to use for the JDK 1.8
* Make sure that "run/Debug" (upper right of the main screen) is correctly configured:
  * select "edit configuration" in the dropdown and edit the "all in config-file-provider-plugin"
  * make sure that
    * run is set to "JDK 8", with `-ea` as parameter
    * "All in Package" is selected in the dropdown
    * "Working directory" is set to `$MODULE_WORKING_DIR$`
    * "Search for test" should be set to `In single module`
  * Add as a "Before Launch" command, "Run Maven Goal test-compile". 
    This is very important. 
    It will make sure that the test prerequisite are always in place before running or debugging tests.

Using this procedure seems to have fixed my irritating problems.

"Bear traps" to watch:
----------------------
* Mixing JDKs. Although this plugin's `pom.xml` explicitly compiles Java 1.8 bytecode, 
  the result whether compiled with JDK8 or JDK11 doesn't seem to be compatible in the IDE.
* Although tempting, use `mvn clean` wisely when running Maven from the command line.
* Make sure that `mvn test-compile` is run before executing tests. 