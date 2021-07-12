FROM mcr.microsoft.com/java/jdk:11-zulu-alpine
CMD uname

RUN apk add libc6-compat
RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

FROM mcr.microsoft.com/java/jdk:11-zulu-alpine
CMD uname

# artifact: copy the main jar and dir containing dependency jars
ADD target/standalone-event-hub-receiver.jar /run/standalone-event-hub-receiver.jar
COPY target/dependency-jars /run/dependency-jars

# jprofiler: jprofiler-agent requires ld-linux-x86-64.so.2
RUN apk add libc6-compat
RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

# jprofiler: download & extract jprofiler agent
RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_11_1_4.tar.gz -P /tmp/ &&\
tar -xzf /tmp/jprofiler_linux_11_1_4.tar.gz -C /usr/local &&\
rm /tmp/jprofiler_linux_11_1_4.tar.gz

# jprofiler: expose agent port
EXPOSE 8849

## app: run the app
ENTRYPOINT ["java", \
"-agentpath:/usr/local/jprofiler11.1.4/bin/linux-x64/libjprofilerti.so=port=8849", \
"-jar", \
 "run/standalone-event-hub-receiver.jar", \
 "-D exec.mainClass=\"com.contoso.anuchan.App\""]

