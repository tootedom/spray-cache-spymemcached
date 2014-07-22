package org.greencheek.elasticacheconfig;

import org.greencheek.elasticacheconfig.client.ConfigRetrievalSettings;
import org.greencheek.elasticacheconfig.client.ConfigRetrievalSettingsBuilder;
import org.greencheek.elasticacheconfig.client.PeriodicConfigRetrievalClient;

import java.util.concurrent.*;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class Client {

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        final PeriodicConfigRetrievalClient client = new PeriodicConfigRetrievalClient(new ConfigRetrievalSettingsBuilder().build());
        client.start();

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("========");
                System.out.println("Stopping");
                System.out.println("========");
                client.stop();

                executorService.shutdownNow();
            }
        },10,60, TimeUnit.SECONDS);
    }
}
