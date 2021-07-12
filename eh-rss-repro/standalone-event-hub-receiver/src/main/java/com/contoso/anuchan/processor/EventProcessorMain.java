package com.contoso.anuchan.processor;

public class EventProcessorMain {
    private static String connectionString = System.getenv("EH_NS_CON_PROCESSOR");
    private static String eventHubName = System.getenv("EH_NAME_PROCESSOR");
    private static String consumerGroup = System.getenv("EH_CG_NAME_PROCESSOR");
    private static String storageUrl = System.getenv("EH_STG_URL_PROCESSOR");
    private static String storageConnectionString = System.getenv("EH_STG_CON_STR_PROCESSOR");
    private static String inMemoryState = System.getenv("EH_USE_INMEMORY_STATE_PROCESSOR");

    public static void run() {
        if (connectionString == null) {
            throw new NullPointerException("EH_NS_CON_PROCESSOR env must be set");
        }
        if (eventHubName == null) {
            throw new NullPointerException("EH_NAME_PROCESSOR env must be set");
        }
        if (consumerGroup == null) {
            throw new NullPointerException("EH_CG_NAME_PROCESSOR env must be set");
        }
        if (storageUrl == null) {
            throw new NullPointerException("EH_STG_URL_PROCESSOR env must be set");
        }
        if (storageConnectionString == null) {
            throw new NullPointerException("EH_STG_CON_STR_PROCESSOR env must be set");
        }
        if (inMemoryState == null) {
            throw new NullPointerException("EH_USE_INMEMORY_STATE_PROCESSOR env must be set");
        }
        new Track2_EventProcessor(connectionString,
                eventHubName,
                consumerGroup,
                storageUrl,
                storageConnectionString,
                Boolean.parseBoolean(inMemoryState)).run();
    }
}
