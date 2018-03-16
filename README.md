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

(should be called from a shell script in the future)

### Other documentation

Other [modules](http://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](http://dev.folio.org/)

See also [Okapi](https://github.com/folio-org/okapi) itself.

### Issue tracker

See project [OKCLI](https://issues.folio.org/browse/OKCLI)
at the [FOLIO issue tracker](http://dev.folio.org/community/guide-issues).

