[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.soundvibe/reacto/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.soundvibe/reacto)
[![Build Status](https://travis-ci.org/soundvibe/reacto.png)](https://travis-ci.org/soundvibe/reacto)
[![Coverage Status](https://codecov.io/github/soundvibe/reacto/coverage.svg?branch=develop)](https://codecov.io/github/soundvibe/reacto?branch=develop)

# reacto
![logo](logo.png)

Functional reactive abstractions for the JVM (Java 8 and above), compatible with Reactive Streams. Building simple scalable micro services has never been so easy.
You are working with ordinary Flowables but they can be executing on any discovered service in the network.

Start by reading an [introduction here](https://github.com/soundvibe/reacto/wiki/Introduction)

Learn more about reacto on the [Wiki home](https://github.com/soundvibe/reacto/wiki).

Makes use of: 
* [RxJava](https://github.com/ReactiveX/RxJava) Observables for reactive async commands and events
* [Protocol Buffers](https://developers.google.com/protocol-buffers/) for efficient internal messaging

## Available implementations
* [reacto-vertx](https://github.com/soundvibe/reacto-vertx) - implements service discovery, communication through WebSockets, handlers for metrics, etc.
* [reacto-couchbase-service-registry](https://github.com/soundvibe/reacto-couchbase-service-registry) - implements service discovery backed by Couchbase

## Binaries


Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cnet.soundvibe.reacto).

Example for Gradle:

```groovy
compile 'net.soundvibe:reacto:2.1.2'
```

and for Maven:

```xml
<dependency>
    <groupId>net.soundvibe</groupId>
    <artifactId>reacto</artifactId>
    <version>2.1.2</version>
</dependency>
```


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/soundvibe/reacto/issues).

## LICENSE

Copyright 2016 Linas Naginionis

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

