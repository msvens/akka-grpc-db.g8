# Akka GRPC Db 

Generate a [Akka GRPC](https://doc.akka.io/docs/akka-grpc/current/index.html) service that connects to a 
database using [slick](http://slick.lightbend.com/).

This template generates a multi project consisting of the following projects
* common - contains protobuf and database code
* server - contains akka grpc service
* client - contains an example cli based on [scallop](https://github.com/scallop/scallop)

Dependencies are defined in project/Dependencies. Rudamentary testing is provided that can
easily expanded

## Quickstart guide

1. install [sbt](https://www.scala-sbt.org/1.0/docs/Setup.html)
2. sbt new msvens/akka-grpc-db.g8
3. sbt test

## Running Server and Client

1. Open up a shell
2. go to project
3. sbt
4. project server
5. run
6. Open up a second shell
7. go to project
8. sbt
9. project client
10. run
