# okapi-cli

Copyright (C) 2017-2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Okapi command-line interface (yet another one)

### Installation

The Okapi CLI software has the following compile-time dependencies:

* Java 8

* Apache Maven 3.3.x or higher

In addition, the test suite must be able to bind to port 9230 to succeed.

Running

    $ mvn install

should produce fat jar. Invoke with

    $ java -jar target/okapi-cli-fat.jar [args]

Refer to the Bourne shell script `okapi-cli` which invokes the java 
runtime with the fat jar.

## Using okapi-cli

Okapi-cli takes options - with leading double dash and commands. Options
takes two forms: a value-less form, Eg `--myopt` and with a value
`--myopt=value`. Options must precede commands in order to take effect for
the command that follows.

Commands takes zero or more arguments. They usually interact with
Okapi in one way or another, while the options merely tune the
commands.

The `help` command displays supported commands and options.

Okapi-cli persists some values in in  $HOME/.okapi.cli`, such as
the URL for Okapi, URL for remote repo (pull) and the Okapi session
token.

Acting as tenant can be done in two ways. The `tenant` command sets
the tenant (X-Okapi-Tenant header). This is fine in some cases, but
if permissins require that you login you'll have to use the second
way: the `login` command which takes tenant, user and password.
The `logout` command clears the session. At this stage, this does not
interact with Okapi, but it might in the future. For now it simply clears
the Tenant/Token so that the Okapi-cli acts as the supertenant (which is
what happens if X-Okapi-Tenant is unset).

There are 4 fundamental HTTP commands `post`, `put`, `get`, and `update`
that offers general interaction with Okapi.

When commands takes a <body> argument (pushed HTTP content) okapi-cli reads
verbatim from the command line arg. However, if prefixed with `@` the remaining
characters are treated as a filename and contens is read from that file.

### Example 1: get remote modules and list them

    $ okapi-cli --okapi-url=http://localhost:9130
    $ okapi-cli pull
    $ okapi-cli get /_/proxy/modules


### Example 2: enable a module for a tenant

    $ okapi-cli post /_/proxy/tenants '{"id":"diku"}'
    $ okapi-cli --tenant=diku
    $ okapi-cli --enable=mod-users install

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

See also [Okapi](https://github.com/folio-org/okapi) itself.

### Issue tracker

See project [OKCLI](https://issues.folio.org/browse/OKCLI)
at the [FOLIO issue tracker](https://dev.folio.org/community/guide-issues).

