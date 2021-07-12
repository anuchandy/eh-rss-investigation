package com.contoso.anuchan.processor;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.LoadBalancingStrategy;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Track2_EventProcessor {
    private final String connectionString;
    private final String eventHubName;
    private final String consumerGroup;
    private final String storageUrl;
    private final String storageConnectionString;
    private final boolean inMemoryState;
    private BlobContainerAsyncClient containerClient;

    public Track2_EventProcessor(String connectionString,
                                 String eventHubName,
                                 String consumerGroup,
                                 String storageUrl,
                                 String storageConnectionString,
                                 boolean inMemoryState) {
        this.connectionString = connectionString;
        this.eventHubName = eventHubName;
        this.consumerGroup = consumerGroup;
        this.storageUrl = storageUrl;
        this.storageConnectionString = storageConnectionString;
        this.inMemoryState = inMemoryState;
        if (this.inMemoryState) {
            this.containerClient = null;
        } else {
            final String containerName = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
            this.containerClient = new BlobContainerClientBuilder()
                .connectionString(this.storageConnectionString)
                .containerName(containerName)
                .buildAsyncClient();

            final Mono<Void> createContainerMono = containerClient.exists().flatMap(exists -> {
                if (exists) {
                    return containerClient.delete().then(containerClient.create());
                } else {
                    return containerClient.create();
                }
            });
            createContainerMono.block();
        }
    }

    public void run() {
        Map<String, EventPosition> initialPositions = new HashMap<>();

        // 1. Initialize the initialPositions to read from EARLIST
        final EventHubClientBuilder ehBuilder = new EventHubClientBuilder()
            .connectionString(this.connectionString, this.eventHubName)
            .transportType(AmqpTransportType.AMQP);
        final EventHubProducerAsyncClient ehClient = ehBuilder.buildAsyncProducerClient();
        ehClient.getEventHubProperties()
            .doOnNext(properties -> {
                for (String partitionId : properties.getPartitionIds()) {
                    initialPositions.put(partitionId, EventPosition.earliest());
                }
            }).block();

        // 2. Initialize the Processor
        EventProcessorClientBuilder processorBuilder = new EventProcessorClientBuilder()
            .connectionString(this.connectionString, this.eventHubName)
            .consumerGroup(this.consumerGroup)
            .loadBalancingStrategy(LoadBalancingStrategy.GREEDY)
            .initialPartitionEventPosition(initialPositions);
        if (this.inMemoryState) {
            processorBuilder.checkpointStore(new SampleCheckpointStore());
        } else  {
            final BlobCheckpointStore checkpointStore = new BlobCheckpointStore(containerClient);
            processorBuilder.checkpointStore(checkpointStore);
        }

        final long[] eventCount = new long[1];
        eventCount[0] = 0;

        processorBuilder.processError(context -> {
                final String partitionId = context.getPartitionContext().getPartitionId();
                System.err.printf("processError (PartitionId: %s. Error: %s%n)", partitionId, context.getThrowable());
            })
            .processPartitionInitialization(context -> {
                final String partitionId = context.getPartitionContext().getPartitionId();
                System.err.printf("init (partitionId: %s. onOpen%n)", partitionId);
            })
            .processPartitionClose(context -> {
                final String partitionId = context.getPartitionContext().getPartitionId();
                System.err.printf("init (partitionId: %s. onClose%n)", partitionId);
            })
            .processEvent(context -> {
                eventCount[0]++;
                if (eventCount[0] >= 10000) {
                    System.out.println("Got " + eventCount[0] + "events:");
                    eventCount[0] = 0;
                }
            });
            processorBuilder.transportType(AmqpTransportType.AMQP);
            final EventProcessorClient processorClient = processorBuilder.buildEventProcessorClient();

        // 3. Run processor
        Mono.usingWhen(Mono.just(processorClient),
                processor -> {
                    System.out.println("Starting run:T2");
                    processor.start();
                    return Mono.delay(Duration.ofDays(7)).then();
                },
                processor-> {
                    System.out.println("Completed run:T2");
                    return Mono.delay(Duration.ofMillis(500), Schedulers.boundedElastic())
                        .then(Mono.fromRunnable(() -> processor.stop()));
                })
                .block();
    }
}
