Changelog
###


### Version 3.6.2

Release date: _18. June 2019_

* attempt to
fix https://issues.jenkins-ci.org/browse/JENKINS-48430[JENKINS-48430] provided
config file is sometimes not complete

### Version 3.6.1

Release date: _5. June 2019_

* fix https://issues.jenkins-ci.org/browse/JENKINS-57417[JENKINS-57417] 'configFileProvider
/ withCredentials not working in declarative pipeline' → properly
populate environment variables (thanks to Richard Davis/crashvb)
* fix https://issues.jenkins-ci.org/browse/JENKINS-48562[JENKINS-48562] Inline
Environment Variables not replaced in config file
* integrate https://github.com/jenkinsci/config-file-provider-plugin/pull/64[PR#64] display
provider id in the list (thanks to imaemo)

### Version 3.6

Release date: _22. Mar. 2019_

* add new config file type: Properties
(PR https://github.com/jenkinsci/config-file-provider-plugin/pull/58[#58],
thanks to victorsalaun)
* https://issues.jenkins-ci.org/browse/JENKINS-55787[JENKINS-55787]
Switch labels from entry to checkbox
(https://github.com/jenkinsci/config-file-provider-plugin/pull/62[PR
#62] thanks to jsoref)
* localize provider config
(https://github.com/jenkinsci/config-file-provider-plugin/pull/61[PR
#61], thanks to jsoref)
* Provides documentation for configuration
(https://github.com/jenkinsci/config-file-provider-plugin/pull/60[PR
#60], thanks to damianszczepanik)
* Link the radio buttons with the label
(https://github.com/jenkinsci/config-file-provider-plugin/pull/59[PR
#59], thanks to MRamonLeon)

### Version 3.5 

Release date: _28. Jan. 2019_

* https://jenkins.io/security/advisory/2019-01-28/[Fix security issue]

### Version 3.4.1 

Release date: _1. Nov. 2018_

* fix JCasC support
(https://github.com/jenkinsci/config-file-provider-plugin/pull/56[PR
#56])

### Version 3.3

Release date: _11. Oct. 2018_

* https://issues.jenkins-ci.org/browse/JENKINS-53247[JENKINS-53247] support
for JCasC

### Version 3.2 

Release date: _25. Sept. 2018_

* https://jenkins.io/security/advisory/2018-09-25/#SECURITY-1080[Fix XSS
vulnerability]
* https://jenkins.io/security/advisory/2018-09-25/#SECURITY-938[Fix CSRF
vulnerability]

### Version 3.0 

Release date: _3. Sept. 2018_

* [.s1]#fix AnonymousClassWarnings
https://github.com/jenkinsci/config-file-provider-plugin/pull/50[PR
#50]#
* [.s1]##make sure config file Ids are ##alphanumeric

### Version 2.18 

Release date: _12. Mar. 2018_

* https://issues.jenkins-ci.org/browse/JENKINS-40803[JENKINS-40803]
allow plugins to specify the config file resolution context
* https://issues.jenkins-ci.org/browse/JENKINS-47502[JENKINS-47502] fix
broken Alphabetical Sort by Name

### Version 2.17 

Release date: _17. Jan. 2018_

* https://issues.jenkins-ci.org/browse/JENKINS-48476[JENKINS-48476] PCT
fails against core 2.73+
* integrate
https://github.com/jenkinsci/config-file-provider-plugin/pull/43[PR
#43] If credentials are missing, at least note this in the build log
* https://issues.jenkins-ci.org/browse/JENKINS-48956[JENKINS-48956] -
Enable PCT and whitelist java.util.Collections$EmptyMap

### Version 2.16.4 

Release date: _22. Sept. 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-46925[JENKINS-46925] add
support for custom folder types

### Version 2.16.3 

Release date: _14. August 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-45399[JENKINS-45399] View
selected scripts doesn't show correct script

### Version 2.16.2 

Release date: _7. August 2017_

* https://jenkins.io/security/advisory/2017-08-07/[SECURITY-513] protect
access to config files, only allow users with granted permissions to
view config files

### Version 2.16.1 

Release date: _26. July 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-44740[JENKINS-44740] Wrong
permission type for Config File Provider credentials

### Version 2.16.0 (

Release date: _22. May 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-42877[JENKINS-42877] Improve
Config File Management screen inside a folder
* https://issues.jenkins-ci.org/browse/JENKINS-43787[JENKINS-43787] GlobalMavenSettingsConfig#getServerCredentialMappings()
may return null when MavenSettingsConfig#getServerCredentialMappings()
doesn't
* https://issues.jenkins-ci.org/browse/JENKINS-43372[JENKINS-43372] IllegalArgumentException:
providerId can NOT be null when using configFiles
* https://issues.jenkins-ci.org/browse/JENKINS-42262[JENKINS-42262] Provide
a method where extension could implement own logic for the content of
custom Config

### Version 2.15.7 

Release date: _17. March 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-42389[JENKINS-42389] Modifying
Folder configuration removes all config files

### Version 2.15.6 

Release date: _13. Feb. 2017_

* https://issues.jenkins-ci.org/browse/JENKINS-41871[JENKINS-41871] provide
a way to create config files via script -> _provider.newConfig(id, name,
comment, content)_
* fix https://issues.jenkins-ci.org/browse/JENKINS-41767[JENKINS-41767] default
implementation for _config.getDescriptor()_

### Version 2.15.5 

Release date: _19. Jan. 2017_

* fix https://issues.jenkins-ci.org/browse/JENKINS-41161[JENKINS-41161]
Config files not being updated
* fix https://issues.jenkins-ci.org/browse/JENKINS-40973[JENKINS-40973]
clearer error message if Id is not given to configFileProvider pipeline
step
* integrate https://issues.jenkins-ci.org/browse/JENKINS-12114[JENKINS-12114]
users having permission to configure folder, are now allowed to edit
config files on the same folder
* Delete per-provider configuration files that are not needed any longer
(https://github.com/jenkinsci/config-file-provider-plugin/pull/32[PR#32])

### Version 2.15.4 

Release date: _13. Jan. 2017_

* fix https://issues.jenkins-ci.org/browse/JENKINS-40901[JENKINS-40901]
Update plugin versions to avoid PCT issues.
* fix https://issues.jenkins-ci.org/browse/JENKINS-40981[JENKINS-40981]
IllegalStateException when installing plugin
* fix https://issues.jenkins-ci.org/browse/JENKINS-40943[JENKINS-40943]
Config files are not updated via Job DSL plugin
* Partialy restore binary compatibility with 2.13 and older releases
(https://github.com/jenkinsci/config-file-provider-plugin/pull/29[PR#29])

### Version 2.15.3 

Release date: _06. Jan. 2016_

* fix https://issues.jenkins-ci.org/browse/JENKINS-40788[JENKINS-40788] https://issues.jenkins-ci.org/browse/JENKINS-40844[JENKINS-40844] https://issues.jenkins-ci.org/browse/JENKINS-40773[JENKINS-40773] https://issues.jenkins-ci.org/browse/JENKINS-40737[JENKINS-40737] Managed
script/configs aren't found/cannot be read/throw exceptions

### Version 2.15.1 

Release date: _27. Dec. 2016_ DO NOT INSTALL!!

* fix https://issues.jenkins-ci.org/browse/JENKINS-39991[JENKINS-39991] Credential
replacement in settings.xml losts configuration, filePermissions and
directoryPermissions elements

### Version 2.15 

Release date: _27. Dec. 2016_ DO NOT INSTALL!!

*This version has a lot a lot of the internal changes to
support https://wiki.jenkins.io/display/JENKINS/CloudBees+Folders+Plugin[CloudBees
Folders Plugin] - to make this work, the configuration is now saved
different then it used to and a rollback of this release is not
supported. If you'r unsure, please save your configuration before
updating.*

*Because of this change, also a couple of other plugins had to be
updated (some might not have released there latest changes):*

- https://wiki.jenkins.io/display/JENKINS/Managed+Script+Plugin[Managed
Script Plugin] -> version 1.3 +
- https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin[Job DSL Plugin]
-> version 1.56 +
- https://wiki.jenkins.io/display/JENKINS/Pipeline+Maven+Plugin[Pipeline
Maven
Plugin] https://github.com/jenkinsci/pipeline-maven-plugin/pull/10[PR#10] +
- https://wiki.jenkins.io/display/JENKINS/Openstack+Cloud+Plugin[Openstack
Cloud
Plugin] https://github.com/jenkinsci/openstack-cloud-plugin/pull/121[PR#121]

* integrate https://wiki.jenkins.io/display/JENKINS/CloudBees+Folders+Plugin[CloudBees
Folders Plugin] plugin
(https://issues.jenkins-ci.org/browse/JENKINS-38872[JENKINS-38872]) This
is a major refactoring and 
* fix
https://issues.jenkins-ci.org/browse/JENKINS-39998[JENKINS-39998] Token
expansion of a config file not happening in pipelines

### Version 2.13 

Release date: _9. Sep. 2016_

* add proper pipeline DSL support
* fix error after trying to view json
file https://issues.jenkins-ci.org/browse/JENKINS-36442[JENKINS-36442] 

### Version 2.11 

Release date: _24. June 2016_

* Allow to manage SSH keys in settings.xml
- https://issues.jenkins-ci.org/browse/JENKINS-33165[JENKINS-33165],
https://github.com/jenkinsci/config-file-provider-plugin/pull/15[PR#15] (thanks
to Cyrille Le Clerc)
* migrate to credentials plugin 2.1+
- https://issues.jenkins-ci.org/browse/JENKINS-35527[JENKINS-35527]
* support variable substitution in file content with TokenMacro for
buildwrapper and build step
- https://issues.jenkins-ci.org/browse/JENKINS-32908[JENKINS-32908]
* clarify use of "Replace All" for Maven settings.xml -
 https://issues.jenkins-ci.org/browse/JENKINS-30458[JENKINS-30458]

### Version 2.10.1 

Release date: _29. March 2015_

* integrate fix for
https://issues.jenkins-ci.org/browse/JENKINS-27152[JENKINS-27152] Use a
standardized directory for managed files
(https://github.com/jenkinsci/config-file-provider-plugin/pull/13[PR
#13])

### Version 2.10.0 

Release date: _7. Dec 2015_

* integrate
https://github.com/jenkinsci/config-file-provider-plugin/pull/12[PR #12]
Support user defined config file ID to ease usage of config file
provider in workflow (thanks to Cyrille Le Clerc)

### Version 2.9.3 

Release date: _20. Aug 2015_

* fix https://issues.jenkins-ci.org/browse/JENKINS-29805[JENKINS-29805]
server credentials don't show up in maven settings.xml
* fix https://issues.jenkins-ci.org/browse/JENKINS-24441[JENKINS-24441]
some icons missing

### Version 2.9.2 

Release date: _4. Aug 2015_

* add support for
https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin[Pipeline Plugin]
(https://issues.jenkins-ci.org/browse/JENKINS-26339[JENKINS-26339])
* fix https://issues.jenkins-ci.org/browse/JENKINS-25031[JENKINS-25031]
Credentials metadata leak in ServerCredentialMapping

### Version 2.8.1 

Release date: _11. May 2015_

* add option to preserve 'server's in managed maven settings.xml

### Version 2.7.5 

Release date: _8. Aug 2014_

* fix https://issues.jenkins-ci.org/browse/JENKINS-20482[JENKINS-20482]
NPE in config-file-provider cleanup task

### Version 2.7.4 

Release date: _30.March 2014_

* fix https://issues.jenkins-ci.org/browse/JENKINS-21494[JENKINS-21494]
Credentials are not injected in the settings via the environment
variable
https://github.com/jenkinsci/config-file-provider-plugin/pull/6[PR #6]
* Improved target path parsing for Config File
https://github.com/jenkinsci/config-file-provider-plugin/pull/5[PR #5]
* some spelling fixes
https://github.com/jenkinsci/config-file-provider-plugin/pull/7[PR #7]

### Version 2.7.1 

Release date: _4.Nov. 2013_

* fix https://issues.jenkins-ci.org/browse/JENKINS-20403[JENKINS-20403]
HTTP 500 error on "view selected file"
* fix supplySettings() https://gist.github.com/olamy/7301273[Gist
7301273]

### Version 2.7 

Release date: _3. Nov. 2013_

* implement https://issues.jenkins-ci.org/browse/JENKINS-16705[JENKINS-16705]
support for server credentials in settings.xml (integrated with
https://wiki.jenkins.io/display/JENKINS/Credentials+Plugin[Credentials
Plugin])

### Version 2.6.2 

Release date: _17. Sep. 2013_

* fix https://issues.jenkins-ci.org/browse/JENKINS-19076[JENKINS-19076]
Guice provision errors at startup

### Version 2.6.1 

Release date: _1. Aug. 2013_

* disabling support for credentials in settings.xml - sorry, there are
some concept changes to be done to keep it in sync with the credentials
plugin

### Version 2.6 

Release date: _23. July 2013_

* Adds support for credentials in settings.xml
https://issues.jenkins-ci.org/browse/JENKINS-16705[JENKINS-16705]
* Order config files by name
https://issues.jenkins-ci.org/browse/JENKINS-18325[JENKINS-18325]

### Version 2.5.1 

Release date: _2. June 2013_

* fix https://issues.jenkins-ci.org/browse/JENKINS-17555[JENKINS-17555]
Environment variables not set for maven projects types

### Version 2.5 

Release date: _15. March 2013_

* fix https://issues.jenkins-ci.org/browse/JENKINS-17031[JENKINS-17031]
add support for JSON files by adding a new provider/file type
* fix https://issues.jenkins-ci.org/browse/JENKINS-16694[JENKINS-16694]
Config-file-provider does not submit modified file

### Version 2.4 

Release date: _12. Dez 2012_

* implement
https://issues.jenkins-ci.org/browse/JENKINS-15962[JENKINS-15962]
Provide BuildStep that publishes selected config file.
* implement
https://issues.jenkins-ci.org/browse/JENKINS-14823[JENKINS-14823]  allow
environment variables in Target definition
* fix https://issues.jenkins-ci.org/browse/JENKINS-15976[JENKINS-15976]
Provided maven settings.xml in maven builder is lost

### Version 2.3 

Release date: _19. Nov 2012_

* depends on core 1.491
* Implement new EP of core to provide maven settings and global settings
for maven project type and maven build step
* remove dependency to maven-plugin
* fix https://issues.jenkins-ci.org/browse/JENKINS-14914[JENKINS-14914]
Errors in hudson-dev:run relating to config-file-provider
* implement
https://issues.jenkins-ci.org/browse/JENKINS-15197[JENKINS-15197] Add
support for Maven toolchains configuration file (pull
https://github.com/jenkinsci/config-file-provider-plugin/pull/3[#3])

### Version 2.2.1 

Release date: _27. April 2012_

* Fixed
https://issues.jenkins-ci.org/browse/JENKINS-13533[JENKINS-13533] Maven
build fails on CleanTempFilesAction#tempFiles serialization

### Version 2.1 

Release date: _16. April 2012_

* Fixed
https://issues.jenkins-ci.org/browse/JENKINS-12823[JENKINS-12823] Clean
up temporary config files at the very end, not before post-build

### Version 2.0 

Release date: _3. April 2012_

* lift dependency to core 1.458
* change dependency direction between maven-plugin and
config-file-provider-plugin (maven-plugin -> (optional)
config-file-provider-plugin)
* upgrade to version 1.2 config-provider-model

### Version 1.2 

Release date: _8. Nov. 2011_

* upgrade to version 1.1 config-provider-model
* upgrade to dependency to core 1.438 (required to better decouple
dependencies)
* enhance API to better support config-file-provider extensionpoint

### Version 1.1 

Release date: _29th August 2011_

* fix JENKINS-10770 - make sure config files are available for
m2-extra-steps
* add new file types: groovy, xml
* allow definition of a variable name to access the config file location

### Version 1.0 

Release date: _16th August 2011_

