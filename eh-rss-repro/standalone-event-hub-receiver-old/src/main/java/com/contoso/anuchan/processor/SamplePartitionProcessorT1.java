// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.contoso.anuchan.processor;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes a single partition.
 */
class SamplePartitionProcessorT1 implements IEventProcessor {
    private final Logger logger = LoggerFactory.getLogger(SamplePartitionProcessorT1.class);
    private final AtomicBoolean isStopped = new AtomicBoolean();
    final long[] eventCount = new long[1];

    /**
     * Invoked when partition is claimed.
     *
     * @param context Context associated with partition.
     */
    @Override
    public void onOpen(PartitionContext context) {
        if (isStopped.get()) {
            System.out.printf("OnOpen: Already stopped partition %s%n", context.getPartitionId());
            return;
        }

        logger.trace("PartitionId[{}] OnOpen", context.getPartitionId());
    }

    /**
     * Invoked when partition is closed.
     *
     * @param context Context associated with partition.
     * @param reason Reason for losing partition.
     *
     * @throws RuntimeException if a counter that is started could not be found.
     */
    @Override
    public void onClose(PartitionContext context, CloseReason reason) {
        if (isStopped.get()) {
            System.out.printf("OnClose: Already stopped partition %s%n", context.getPartitionId());
            return;
        }

        logger.info("PartitionId[{}] OnClose {}", context.getPartitionId(), reason);
    }

    /**
     * Invoked when partition events are received.
     *
     * @param context Context associated with partition.
     * @param events Events received.
     *
     * @throws RuntimeException if a counter that is started could not be found.
     */
    @Override
    public void onEvents(PartitionContext context, Iterable<EventData> events) {
        if (isStopped.get()) {
            System.out.printf("OnEvents: Already stopped partition %s%n", context.getPartitionId());
            return;
        }
        eventCount[0]++;
        if (eventCount[0] >= 10000) {
            System.out.println("Got " + eventCount[0] + "events:");
            eventCount[0] = 0;
        }
    }

    /**
     * Invoked when an error occurs.
     *
     * @param context Context associated with partition.
     * @param error Error received.
     */
    @Override
    public void onError(PartitionContext context, Throwable error) {
        logger.warn("PartitionId[{}] onError", context.getPartitionId(), error);
    }

    /**
     * Stops the partition processor entirely and closes any open EventCounters.
     */
    void onStop() {
        if (isStopped.getAndSet(true)) {
            return;
        }
    }
}
