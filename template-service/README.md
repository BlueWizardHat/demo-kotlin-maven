Template-Service (Parent)
==================================================================================================

This service serves as a template to make other services. Contains a basic rest service that
connects to a postgresql database and to redis.

To make a copy of this service simply run

```bash
./scripts/copy-template.sh servicename
```
or

```bash
./scripts/copy-template.sh servicename packagename
```

For example

```bash
./scripts/copy-template.sh account-service
./scripts/copy-template.sh account-service org.myorg.myapp.service
```
The script will take care of renaming modules and changing package names in the new service.

## Modules

The service consist of several modules

* template-service - the spring boot app that assembles all the modules into the service
* template-service-api - the api that the service exposes
* template-service-db - database functionality
* template-service-client - a rest client that can connect to the service (for use in other services)
* template-service-integration-tests - test that run against the running template-service
