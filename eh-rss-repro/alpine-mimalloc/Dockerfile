FROM alpine as builder
RUN apk add git build-base cmake linux-headers
RUN cd /; git clone --depth 1 https://github.com/microsoft/mimalloc; cd mimalloc; mkdir build; cd build; cmake ..; make -j$(nproc); make install

FROM alpine
COPY --from=builder /mimalloc/build/*.so.* /lib
RUN ln -s /lib/libmimalloc.so.* /lib/libmimalloc.so
ENV LD_PRELOAD=/lib/libmimalloc.so
ENV MIMALLOC_LARGE_OS_PAGES=1

CMD uname

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

RUN wget --quiet https://cdn.azul.com/public_keys/alpine-signing@azul.com-5d5dc44c.rsa.pub -P /etc/apk/keys/ && \
    echo "https://repos.azul.com/zulu/alpine" >> /etc/apk/repositories && \
    apk --no-cache add zulu11-jdk=11.0.10-r1

ENV JAVA_HOME=/usr/lib/jvm/zulu11-ca

ADD ./../standalone-event-hub-receiver/target/standalone-event-hub-receiver.jar /run/standalone-event-hub-receiver.jar
COPY ./../standalone-event-hub-receiver/target/dependency-jars /run/dependency-jars

## app: run the app
ENTRYPOINT ["java", \
"-jar", \
 "run/standalone-event-hub-receiver.jar", \
 "-D exec.mainClass=\"com.contoso.anuchan.App\""]
