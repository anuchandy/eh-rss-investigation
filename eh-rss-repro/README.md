### Prerequisite:

You'll need [docker-desktop](https://www.docker.com/products/docker-desktop) to run the java projects under `eh-rss-repro` in containers.

### Project Structure:

Under `eh-rss-repro`, there are two projects, each exercising the event hub receive API;
The project `standalone-event-hub-receiver` uses the latest EH SDK.
The project `standalone-event-hub-receiver-old` uses the old (Track1) EH SDK.

```
eh-rss-repro
 |
 |-- standalone-event-hub-receiver
 |              |-- src
 |              |-- Dockerfile
 |
 |-- standalone-event-hub-receiver-old
 |              |-- src
 |              |-- Dockerfile
```

### Running projects in Docker:

To run each project in a Docker container, create a file named `docker-env-vars.txt` in the following format and put it in the same level as `Dockerfile`

```
EH_NS_CON_PROCESSOR=Endpoint=sb://<eh-namespace-name>-eventhubs.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<key>
EH_NAME_PROCESSOR=<eh-name>
EH_CG_NAME_PROCESSOR=<eh-consumer-group-name>
EH_STG_URL_PROCESSOR=https://<stg-account-name>.blob.core.windows.net/
EH_STG_CON_STR_PROCESSOR=DefaultEndpointsProtocol=https;AccountName=<stg-account-name>;AccountKey=<key>;EndpointSuffix=core.windows.net
EH_USE_INMEMORY_STATE_PROCESSOR=False 
```

> Note: While you can share the EH instance for both projects, you'll need to create two consumer groups, one for each project and use that as the value of `EH_CG_NAME_PROCESSOR`

```
eh-rss-repro
 |
 |-- standalone-event-hub-receiver
 |              |-- src
 |              |-- Dockerfile
 |              |-- docker-env-vars.txt
 |
 |-- standalone-event-hub-receiver-old
 |              |-- src
 |              |-- Dockerfile
 |              |-- docker-env-vars.txt
```

As the next step, run `mvn package` for each project:

```
>/eh-rss-investigation/eh-rss-repro/standalone-event-hub-receiver$ mvn package
>/eh-rss-investigation/eh-rss-repro/standalone-event-hub-receiver-old$ mvn package
```

As a result of the above `mvn package`, maven will produce the artifacts under the `target` directory. 

```
eh-rss-repro
 |
 |-- standalone-event-hub-receiver
 |              |-- src
 |              |-- Dockerfile
 |              |-- docker-env-vars.txt
 |              |-- target
 |                     |-- dependency-jars 
 |                     |-- standalone-event-hub-receiver.jar
 |
 |-- standalone-event-hub-receiver-old
 |              |-- src
 |              |-- Dockerfile
 |              |-- docker-env-vars.txt
 |              |-- target
 |                     |-- dependency-jars 
 |                     |-- standalone-event-hub-receiver-old.jar
```

The `dependency-jars` contains all the dependency jar files required to run the "main-jar-file" in the same directory (i.e. `standalone-event-hub-receiver.jar` and `standalone-event-hub-receiver-old.jar` )

The "main-jar-file" has `META-INF\MANIFEST.MF` file setting the `Class-Path` with the path to jars in `dependency-jars`

Now you can use the `Dockerfile` in each project to deploy the `dependency-jar` dir and "main-jar-file" to two container instances.

> Note_1: The `Dockerfile` file download the `JProfiler-Agent` [jprofiler_linux_11_1_4.tar.gz](https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_11_1_4.tar.gz) and exposes the agent port `8849`. 
> This enables us to attach `JProfiler` running on the host machine to the JVM in the containers and profile it.
> If you don't want to profile, then comment out JProfiler related entries from `Dockerfile`.

> Note_2: The version of `JProfiler` in the host machine and `JProfiler-Agent` (that Dockerfile pulls) must match. You can find the version of `JProfiler` in the host machine from the menu-option `About`  e.g. `JProfiler 11.1.4`.
> So for the `JProfiler` version 11.1.4, the corresponding `JProfiler-Agent` is - https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_11_1_4.tar.gz
> The Dockerfile is hardcoded to download `jprofiler_linux_11_1_4.tar.gz`, change the version-part if you have a different `JProfiler` version in host machine.

#### Build docker-images:

```
>standalone-event-hub-receiver$ docker build -f Dockerfile -t rssrepro/standalone-event-hub-receiver .
>standalone-event-hub-receiver-old$ docker build -f Dockerfile -t rssrepro/standalone-event-hub-receiver-old .
```

<details><summary>Example output of "docker build"</summary>

```

anuthomaschandy@lipeng-43933457: standalone-event-hub-receiver$main$ docker build -f Dockerfile -t rssrepro/standalone-event-hub-receiver .
[+] Building 46.6s (11/11) FINISHED                                                                                                                                                                                                    
 => [internal] load build definition from Dockerfile                                                                                                                                                                              0.0s
 => => transferring dockerfile: 1.12kB                                                                                                                                                                                            0.0s
 => [internal] load .dockerignore                                                                                                                                                                                                 0.0s
 => => transferring context: 2B                                                                                                                                                                                                   0.0s
 => [internal] load metadata for mcr.microsoft.com/java/jdk:11-zulu-alpine                                                                                                                                                        0.0s
 => CACHED [stage-1 1/6] FROM mcr.microsoft.com/java/jdk:11-zulu-alpine                                                                                                                                                           0.0s
 => [internal] load build context                                                                                                                                                                                                 1.2s
 => => transferring context: 11.31MB                                                                                                                                                                                              1.1s
 => [stage-1 2/6] ADD target/standalone-event-hub-receiver.jar /run/standalone-event-hub-receiver.jar                                                                                                                             0.2s
 => [stage-1 3/6] COPY target/dependency-jars /run/dependency-jars                                                                                                                                                                0.1s
 => [stage-1 4/6] RUN apk add libc6-compat                                                                                                                                                                                        2.7s
 => [stage-1 5/6] RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2                                                                                                                                                  0.6s
 => [stage-1 6/6] RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_11_1_4.tar.gz -P /tmp/ &&tar -xzf /tmp/jprofiler_linux_11_1_4.tar.gz -C /usr/local &&rm /tmp/jprofiler_linux_11_1_4.tar.gz        39.5s
 => exporting to image                                                                                                                                                                                                            1.9s
 => => exporting layers                                                                                                                                                                                                           1.9s
 => => writing image sha256:ae0db2014f1be7ce9b0c3eee5ed4436568ffff41c0f65ba92f3e23d82615bc9c                                                                                                                                      0.0s
 => => naming to docker.io/rssrepro/standalone-event-hub-receiver
 
```

</details>

#### Run container using the above images:

```
>standalone-event-hub-receiver$ docker run --rm -p 8848:8849 --env-file docker-env-vars.txt rssrepro/standalone-event-hub-receiver
>standalone-event-hub-receiver-old$ docker run --rm -p 8850:8849 --env-file docker-env-vars.txt rssrepro/standalone-event-hub-receiver-old
```

<details><summary>Example output of "docker run"</summary>

```
anuthomaschandy@lipeng-43933457: standalone-event-hub-receiver$main$ docker run --rm -p 8848:8849 --env-file docker-env-vars.txt rssrepro/standalone-event-hub-receiver
JProfiler> Protocol version 63
JProfiler> Java 11 detected.
JProfiler> 64-bit library
JProfiler> Listening on port: 8849.
JProfiler> Enabling native methods instrumentation.
JProfiler> Can retransform classes.
JProfiler> Can retransform any class.
JProfiler> Native library initialized
JProfiler> VM initialized
JProfiler> Retransforming 110 base class files.
JProfiler> Base classes instrumented.
JProfiler> Waiting for a connection from the JProfiler GUI ...

```

</details>

### Attaching JProfiler:

The `run` command maps the `JProfiler-Agent` port 8849 to a unique port in the host machine. In the above example, 8848 for `standalone-event-hub-receiver` and 8850 for `standalone-event-hub-receiver-old`.

At this point `JProfiler-Agent` is waiting for the `JProfiler` to connect from the host machine:

In the below screenshot `JProfiler` connects to localhost port `8848`, which is mapped to the container port `8849` running `standalone-event-hub-receiver`.

<img width="1121" alt="Screen Shot 2021-07-12 at 12 41 46 PM" src="https://user-images.githubusercontent.com/1471612/125346462-a6a7e380-e30e-11eb-8040-b7cfb6670680.png">

Once the `JProfiler` is connected, the `JProfiler-Agent` will hand over control to the main App exercising the EH receiver SDK API.

### Watching for RSS:

The running containers can be listed using `docker container ls` command:

```
$ docker container ls
CONTAINER ID   IMAGE                                    COMMAND                  CREATED          STATUS          PORTS                                       NAMES
37b3c14273d3   rssrepro/standalone-event-hub-receiver   "java -agentpath:/us…"   14 minutes ago   Up 14 minutes   0.0.0.0:8848->8849/tcp, :::8848->8849/tcp   sleepy_sanderson
```

The Id (e.g. 37b3c14273d3) of the container can be used to output RSS usage:

```
$ docker exec -it 37b3c14273d3 ps -o pid,user,vsz,rss,comm,args
PID   USER     VSZ  RSS  COMMAND          COMMAND
    1 root     2.2g 357m java             java -agentpath:/usr/local/jprofiler1
```

The script `util-scripts/capture-container-rss.py` can be used to record the RSS in every 10 minutes into a file.

```
python capture-container-rss.py <containerId>
```

E.g:

```
python capture-container-rss.py 37b3c14273d3
```

Executing above command creates a CSV file with name `rss-captured-<containerId>.csv` and captures the RSS.