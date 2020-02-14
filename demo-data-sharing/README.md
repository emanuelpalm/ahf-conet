# Data-Sharing Demo

This folder contains systems and other configuration files useful for using the contract system in
a concrete data-sharing demo.

## Building Demo

### Building Source Code

TODO

## Running Demo

To run the demo, `docker` and `docker-compose` must be installed on your system.
To start up the demo clouds, enter into this directory in a terminal and then enter the following command:

```sh
$ docker-compose up --build
```

If you have not run the command before, you will be requested to create two external Docker volumes.
The commands for doing that are given in the Docker error message in question.
After creating the volumes, enter the above command again.

To shut down the local clouds, use CTRL-D (or whatever key command your terminal requires to send
an End-of-File message to the Docker Daemon).
After the systems have all shut down, please type the following command to clean up any data cached
by the Docker daemon:

```sh
$ docker-compose down
```
