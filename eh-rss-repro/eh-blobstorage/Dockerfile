FROM mcr.microsoft.com/java/jdk:11-zulu-alpine
CMD uname

RUN apk add libc6-compat
RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

FROM mcr.microsoft.com/java/jdk:11-zulu-alpine
CMD uname

# artifact: copy the main jar and dir containing dependency jars
ADD target/eh-blobstorage.jar /run/eh-blobstorage.jar
COPY target/dependency-jars /run/dependency-jars

# jprofiler: jprofiler-agent requires ld-linux-x86-64.so.2
RUN apk add libc6-compat
RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

# jprofiler: download & extract jprofiler agent
RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_3.tar.gz -P /tmp/ &&\
tar -xzf /tmp/jprofiler_linux_12_0_3.tar.gz -C /usr/local &&\
rm /tmp/jprofiler_linux_12_0_3.tar.gz

# jprofiler: expose agent port
EXPOSE 8849

## app: run the app
ENTRYPOINT ["java", \
"-XX:NativeMemoryTracking=detail", \
"-agentpath:/usr/local/jprofiler12.0.3/bin/linux-x64/libjprofilerti.so=port=8849", \
"-jar", \
 "run/eh-blobstorage.jar", \
 "-D exec.mainClass=\"com.conniey.Program\""]

