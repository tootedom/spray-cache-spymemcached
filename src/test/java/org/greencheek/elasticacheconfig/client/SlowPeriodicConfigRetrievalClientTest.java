package org.greencheek.elasticacheconfig.client;

import org.greencheek.elasticacheconfig.annotations.ConfigMessage;
import org.greencheek.elasticacheconfig.annotations.DelayConfigResponse;
import org.greencheek.elasticacheconfig.annotations.SendAllMessages;
import org.greencheek.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.elasticacheconfig.domain.ConfigInfo;
import org.greencheek.elasticacheconfig.server.StringServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by dominictootell on 21/07/2014.
 */
public class SlowPeriodicConfigRetrievalClientTest {


    @Rule
    public StringServer server = new StringServer("");


    PeriodicConfigRetrievalClient client;

    @Before
    public void setUp() {

    }

    @Test
    @SendAllMessages
    @ConfigMessage(message = {"CONFIG"," cluster 0 ","147\r\n" +
            "+","1","00","\r\n" +
            "myCluster.","pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
            "END\r\n"
    })
    public void testMultiWrites() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger invalid = new AtomicInteger(0);
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                latch.countDown();
                if(!info.isValid()) invalid.incrementAndGet();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(5, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70,TimeUnit.SECONDS);
        builder.setElasticacheHost("localhost");
        builder.setElasticachePort(server.getPort());
        builder.setNumberOfInvalidConfigsBeforeReconnect(5);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(10,TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(0,invalid.get());
    }

    @Test
    @SendAllMessages
    @DelayConfigResponse(delayedForTimeUnit = TimeUnit.MILLISECONDS, delayFor = 1000)
    @ConfigMessage(message = {"CONFIG"," cluster 0 ","147\r\n" +
            "+","1","00","\r\n" +
            "myCluster.","pc4ldq.0001.use1.cache.","amazonaws.com|10.82.235.120|11211"," myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
            "END\r\n"
    })
    public void testMultipleSlowWrites() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger invalid = new AtomicInteger(0);
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                latch.countDown();
                if(!info.isValid()) invalid.incrementAndGet();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(10, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70,TimeUnit.SECONDS);
        builder.setElasticacheHost("localhost");
        builder.setElasticachePort(server.getPort());
        builder.setNumberOfInvalidConfigsBeforeReconnect(5);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(10,TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(0,invalid.get());
    }

    @After
    public void tearDown() {
        client.stop();
    }
}
