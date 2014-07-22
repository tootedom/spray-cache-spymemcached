package org.greencheek.elasticacheconfig.client;

import org.greencheek.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.elasticacheconfig.handler.RequestConfigInfoScheduler;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class ConfigRetrievalSettings {

    private final RequestConfigInfoScheduler scheduledConfigRetrieval;
    private final AsyncConfigInfoMessageHandler configInfoMessageHandler;

    private final String elasticacheConfigHost;
    private final int elasticacheConfigPort;

    private final TimeUnit idleTimeoutTimeUnit;
    private final long idleReadTimeout;

    private final TimeUnit reconnectDelayTimeUnit;
    private final long reconnectDelay;

    private final int numberOfConsecutiveInvalidConfigsBeforeReconnect;

    public ConfigRetrievalSettings(RequestConfigInfoScheduler scheduledConfigRetrieval,
                                   AsyncConfigInfoMessageHandler obtainedConfigHandler,
                                   String elasticacheConfigHost,
                                   int elasticacheConfigPort,
                                   TimeUnit idleTimeoutTimeUnit,
                                   long idleReadTimeout,
                                   TimeUnit reconnectDelayTimeUnit,
                                   long reconnectDelay,
                                   int noInvalidConfigsBeforeReconnect) {
        this.scheduledConfigRetrieval = scheduledConfigRetrieval;
        this.configInfoMessageHandler = obtainedConfigHandler;
        this.elasticacheConfigHost = elasticacheConfigHost;
        this.elasticacheConfigPort = elasticacheConfigPort;
        this.idleTimeoutTimeUnit = idleTimeoutTimeUnit;
        this.idleReadTimeout = idleReadTimeout;
        this.reconnectDelayTimeUnit = reconnectDelayTimeUnit;
        this.reconnectDelay = reconnectDelay;
        this.numberOfConsecutiveInvalidConfigsBeforeReconnect = noInvalidConfigsBeforeReconnect;
    }

    public RequestConfigInfoScheduler getScheduledConfigRetrieval() {
        return scheduledConfigRetrieval;
    }

    public AsyncConfigInfoMessageHandler getConfigInfoMessageHandler() {
        return configInfoMessageHandler;
    }

    public String getElasticacheConfigHost() {
        return elasticacheConfigHost;
    }

    public int getElasticacheConfigPort() {
        return elasticacheConfigPort;
    }

    public TimeUnit getIdleTimeoutTimeUnit() {
        return idleTimeoutTimeUnit;
    }

    public long getIdleReadTimeout() {
        return idleReadTimeout;
    }

    public TimeUnit getReconnectDelayTimeUnit() {
        return reconnectDelayTimeUnit;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public int getNumberOfConsecutiveInvalidConfigsBeforeReconnect() {
        return numberOfConsecutiveInvalidConfigsBeforeReconnect;
    }
}
