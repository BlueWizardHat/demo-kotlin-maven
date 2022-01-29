DemoApp (Work-In-Progress)
==================================================================================================

This project is a collection of good practices I've have learned over the years and also serves
as a place I can lookup how to do things I've often done before.


## Building this project

### Pre-requisites

* Maven - version 3.8 was used to develop this, it may or may not work with older versions.
* Java JDK 17 - [Eclipse Temurin 17](https://adoptium.net/?variant=openjdk17&jvmVariant=hotspot) from Adoptium was used to develop this project.
* Docker + Docker-Compose - Docker version 20.10.12 and docker-compose version 1.29.2 was used to develop this project.
* Bash - version 5+ - Most scripts in this project are Bash scripts - Linux and Mac should have that already installed, but if you are on windows you might need to install.


### Build with maven

A simple
```bash
mvn clean install
```
is all that is needed to build all the services and modules in the project.

It may also be advantages to pre-download sources for dependencies in advance so they are already
downloaded when your IDE needs them.
```bash
mvn clean install dependency:sources
```

For running it locally please refer to the section below named [Running this project / runLocal.sh](https://github.com/BlueWizardHat/demo-kotlin-maven#running-this-project--runlocalsh).


## Good practices


### Consistent package naming

Always name your packages beginning with a domain you own, follow that with other naming that is
consistent within your organization, for example

```
<orgDomain> = net.bluewizardhat
<appName> = demoapp
<serviceName> = account [-service]
```
Gives ```net.bluewizardhat.demoapp.account```


### Always include .gitignore, .gitattributes and .editorconfig in the root of the project

* [.gitignore](https://git-scm.com/docs/gitignore) - You should already know this - Makes git ignore files you don't want committet to your git repository (compiled files, files generated by your build, files made by your editor/IDE, etc.)
* [.gitattributes](https://git-scm.com/docs/gitattributes) - Tells git which line endings are used among other things - especially important if not all developers on your project use the same system/OS.
* [.editorconfig](https://editorconfig.org/) - Tells the editor/IDE you are using which line endings are used, which encoding to use, how to indent and other things - Many editors and IDEs now support ```.editorconfig``` out of the box and some support it with addons and even using an editor that does not support it ```.editorconfig``` can still serve as documentation to developers what is expected in the project.

Other files:

* [lombok.config](https://projectlombok.org/) - If you use lombok (and if you use Java you should use lombok) always include ```lombok.config``` - even if it's just to specify ```config.stopBubbling = true``` - The same applies for any other tool that reads config files during build, make sure to configure them to not read any config files further out than the root of your project.

### Generate source jars

Generating (and publishing) source jars allows IDEs such as Eclipse and IDEA/IntelliJ to not only
show the source code for imported jars, but also just showing the javadoc is incredible usefull.
If you intend for any part of your project to be reusable generating and publishing source jars is a must.
All major open source projects publish source jars.

And even if your project is private generating source jars won't hurt, making it a habit just means you
don't forget when you make something you intend to be reusable.


### Have a set up that allows rapid deploy to a local environment

Being able to easily and quickly run and test any services you make locally can significantly speed up
development as you can quickly test if some code works as intended. If at all possible make your project
in such a way that it can easily be run locally in a way that simulates how it is supposed to run
in a production environment.

In my opinion that is usually best done using a docker setup and some helper scripts that removes the need
to remember project specific arguments to maven, docker, etc. If there is any special set up that is
needed to run the project let the script handle it. If every one uses the script it will be maintained,
which is better than a ```README.md``` that is out-of-date.


### Don't include environment specific config in your config files

Environment specific config should be supplied the way you expect them to be supplied in production.
If for example you expect database connection details to be provided via environment variables then
specifying environment variables in a docker-compose.yml file can do the trick.

If you absolutely must include environment specific config in your config files at least try to separate
it from non-specific config, either by placing it in another file or if using Spring put it in a separate
spring profile.

Having it separated makes it much easier to figure out which values are supposed to be supplied in another
environment when your documentation is not 100% updated (I've never been in a project where the documentaton
keeps up with development - and neither have you).


### Split your project and services in your project into logical modules depending on responsibility

Splitting up your projects and services into modules according to responsibility makes the project
more readable and also makes it easier to refactor, remove or replace parts that needs it. Keep your
database functionality in one module, REST services in another, etc.

Each module should have the minimum dependencies it needs.

This can sometimes take a little extra effort to set up with dependencies between modules, but it pays of
in the end by having a much more organized and clean project and can also help prevent making cyclic
dependencies between packages. And as an added bonus it also makes it easier to bring other
developers up to speed if you need to bring in new developer on the project.

Your Spring Boot app or shadow jar tool can collect all the modules at compile time.


### Have a template project that can be easily cloned and adapted

Having a template project that is fully functional out-of-the box means it is easier to create new services
as you don't need to go through the same setup process all the time.

Your template project should be set up the way your project or organization prefers to set up projects and
include examples of the functionality that is most often used in your project/organization. If all your
projects connects to a database include a (fully working) example of database set up so it is easily replicated.
But also don't include every functionality under the sun, the template is a starting point not the end-all-be-all
of projects.


### Script as much as possible / Automate all the things

Don't have a bunch of manual steps if a script can take care of it. Manual processes will often fail
since humans aren't perfect. If you can script it do it. It is also usually faster to let the computer
do the work than to do it manually.


### Have a root-POM / super-POM that configures all your most used plugins

It just simplifies things to not have every project repeat the same configuration.


### Document as much as possible in the source code

Write a little javadoc for every public class and method unless it's obvious. Doesn't have to be much, as
long as it gives the reader an idea of what it does and/or it's intended purpose. Makes it much easier for
other developers to understand the project and also yourself when you haven't worked on it for 6 months.

Prefer documenting stuff in READMEs in the project rather than external tools such as Confluence. Not all
documentation can be in READMEs but what can be there should be there. Not only is it more likely to be
maintained, but it is also more likely to be read.

We all know that Confluence is where documentation goes to die.. :)


### Don't make huge classes

If classes gets too large they are harder to understand and maintain, if a service or class grows to much
try to see if it can be split up or if maybe some functionality can be delegated to helper classes or utility
classes. There is no right or wrong answer as to how to do this or an exact line number that is the limit, but
I recently had to edit a 1500+ line java class and that is definately far above any sensible limit.


## Running this project / runLocal.sh

```runLocal.sh``` is a script that runs commands to help speed up the implementation and testing of services by running services
in docker containers and allowing you to quickly build and redeploy.

Commands:

* refresh - Looks through the project to find 'localdev.config' files and uses them to (re-)configure the localdev setup
* build - builds selected services with maven using the demo profile
* qbuild - (quick)builds selected services with maven using the demo profile but without clean and skips tests
* start|up|run - start all services in docker
* debug - start all services in docker in debug mode
* restart - restarts selected services
* log or logs - tails the logs of selected services (by default all java/kotlin services but not 3rd party like postgres or redis)
* alllogs - tails the logs of all services including 3rd party
* pause - stops all containers but does not remove them, they can later be restarted again
* stop or down - stops and removes all containers
* dbconnect - drops you into a psql command line of the selected database

Options:

* -s - adds a service to the list of selected services (by default all of them), accepts a comma-separated list as well as using -s multiple times
* -a - resets the services/modules to work on to all

```runLocal.sh``` processes arguments in the order they are given, so using -s and -a only affects later commands.


### Examples

To build the project, start containers and follow the logs:
```bash
./runLocal.sh build start logs
```

To quickbuild the project, restart java/kotlin containers and follow the logs:
```bash
./runLocal.sh qbuild restart logs
```

To quickbuild only the account-service (assuming you have such a service), restart account-service container and follow the logs of all services:
```bash
./runLocal.sh -s account-service qbuild restart -a logs
```

If account-service has the alias 'account' the above can also be done with:
```bash
./runLocal.sh -s account qbuild restart -a logs
```

To quickbuild the account-service and book-service, restart their containers and follow the logs of those two services:
```bash
./runLocal.sh -s account -s book qbuild restart logs
```
or
```bash
./runLocal.sh -s account,book qbuild restart logs
```

To build and completely restart containers from scratch (including 3rd party) and follow logs:
```bash
./runLocal.sh stop build start logs
```

To connect to a database named 'account' with the user 'account' (advanced):
```bash
./runLocal.sh dbconnect account
```

To shutdown the servics:
```bash
./runLocal.sh stop
```

## Mono-repo or multiple repos

It is up to you. This project is a mono-repo simply because I don't have a Nexus or Artifactory where I
can publish artifacts and I don't want to host one either so it is easier for all the functionality to
be in one repo.

But for a multiple repo approach it should not be difficult to adapt, you would need to simplify
```runLocal.sh``` and the localdev setup and change the template copying script but should not be a big
issue. You could also re-use the root-POM here for a super-POM.

Converting this to a multiple repo approach I'll leave as an exercise left to the reader :)
