package com.conniey;

import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.EventHubConnectionStringProperties;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Program {
    private static final String CONNECTION_STRING = System.getenv("EH_NS_CON_PROCESSOR");
    private static final String EVENT_HUB_NAME = System.getenv("EH_NAME_PROCESSOR");
    private static final String CONSUMER_GROUP = System.getenv("EH_CG_NAME_PROCESSOR");
    private static final String STORAGE_CONNECTION_STRING = System.getenv("EH_STG_CON_STR_PROCESSOR");

    private final Sinks.Empty<Void> onComplete = Sinks.empty();
    private final EventHubConnectionStringProperties properties;
    private final BlobCheckpointStore checkpointStore;
    private final String eventHubName;
    private final String consumerGroup;
    private final String ownerId;
    private final HashMap<String, PartitionOwnership> partitionOwnerships;

    public static void main(String[] args) {
        final String containerName = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        final BlobContainerAsyncClient containerClient = new BlobContainerClientBuilder()
                .connectionString(STORAGE_CONNECTION_STRING)
                .containerName(containerName)
                .buildAsyncClient();

        containerClient.exists().flatMap(exists -> exists
                ? containerClient.delete().then(containerClient.create())
                : containerClient.create()).block();

        final EventHubConnectionStringProperties properties = EventHubConnectionStringProperties.parse(CONNECTION_STRING);
        final Program program = new Program(properties, containerClient, EVENT_HUB_NAME, CONSUMER_GROUP, 4);
        System.out.println("Starting run.");
        program.createOwnerships().then(program.run()).block();
        System.out.println("Stopping.");
    }

    public Program(EventHubConnectionStringProperties properties, BlobContainerAsyncClient containerClient,
            String eventHubName, String consumerGroup, int numberOfPartitions) {
        this.properties = properties;
        this.checkpointStore = new BlobCheckpointStore(containerClient);
        this.eventHubName = eventHubName;
        this.consumerGroup = consumerGroup;
        this.ownerId = UUID.randomUUID().toString();
        this.partitionOwnerships = IntStream.range(0, numberOfPartitions)
                .mapToObj(index -> createPartitionOwnershipRequest(String.valueOf(index)))
                .collect(HashMap::new,
                        (map, ownership) -> map.put(ownership.getPartitionId(), ownership),
                        HashMap::putAll);
    }


    public Mono<Void> createOwnerships() {
        return checkpointStore.claimOwnership(new ArrayList<>(partitionOwnerships.values()))
                .map(ownership -> {
                    partitionOwnerships.put(ownership.getPartitionId(), ownership);
                    return ownership;
                })
                .then();
    }

    public Mono<Void> run() {
        return Flux.interval(Duration.ofSeconds(20))
                .takeUntilOther(onComplete.asMono())
                .flatMap(index -> {
                    System.out.println("Renewing ownership.");
                    final List<PartitionOwnership> ownerships = partitionOwnerships.keySet().stream()
                            .map(partitionId -> createPartitionOwnershipRequest(partitionId))
                            .collect(Collectors.toList());
                    return checkpointStore.claimOwnership(ownerships);
                })
                .then();
    }

    private PartitionOwnership createPartitionOwnershipRequest(final String partitionIdToClaim) {
        final PartitionOwnership previousOwnership = partitionOwnerships.get(partitionIdToClaim);

        return new PartitionOwnership()
                .setFullyQualifiedNamespace(properties.getFullyQualifiedNamespace())
                .setOwnerId(ownerId)
                .setPartitionId(partitionIdToClaim)
                .setConsumerGroup(consumerGroup)
                .setEventHubName(eventHubName)
                .setETag(previousOwnership == null ? null : previousOwnership.getETag());
    }
}
