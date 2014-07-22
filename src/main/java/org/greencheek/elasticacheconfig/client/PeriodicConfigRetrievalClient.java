package org.greencheek.elasticacheconfig.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.greencheek.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.elasticacheconfig.decoder.ConfigInfoDecoder;
import org.greencheek.elasticacheconfig.handler.ClientInfoClientHandler;
import org.greencheek.elasticacheconfig.handler.RequestConfigInfoScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class PeriodicConfigRetrievalClient
{

    private static final Logger log = LoggerFactory.getLogger(PeriodicConfigRetrievalClient.class);

    private final String elasticachehost;
    private final int elasticacheport;

    private final TimeUnit idleTimeoutTimeUnit;
    private final long readTimeout;
    private final ClientInfoClientHandler handler;


    private NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();


    public PeriodicConfigRetrievalClient(ConfigRetrievalSettings settings)
    {
        this.elasticachehost = settings.getElasticacheConfigHost();
        this.elasticacheport = settings.getElasticacheConfigPort();
        this.idleTimeoutTimeUnit = settings.getIdleTimeoutTimeUnit();
        this.readTimeout = settings.getIdleReadTimeout();
        this.handler = createHandler(settings.getScheduledConfigRetrieval(),settings.getConfigInfoMessageHandler(),
                settings.getReconnectDelayTimeUnit(),settings.getReconnectDelay(),idleTimeoutTimeUnit,readTimeout,
                settings.getNumberOfConsecutiveInvalidConfigsBeforeReconnect());


    }

    public ClientInfoClientHandler createHandler(RequestConfigInfoScheduler scheduledRequester,
                                                 AsyncConfigInfoMessageHandler configReadHandler,
                                                 TimeUnit reconnectTimeUnit,
                                                 long reconnectionDelay,
                                                 TimeUnit idleTimeoutTimeUnit,
                                                 long idleReadTimeout,
                                                 int noConsecutiveInvalidConfigsBeforeReconnect) {
        return new ClientInfoClientHandler(scheduledRequester,configReadHandler,reconnectTimeUnit,reconnectionDelay,
                idleTimeoutTimeUnit,idleReadTimeout,this.elasticachehost,this.elasticacheport,noConsecutiveInvalidConfigsBeforeReconnect);
    }


    public void start() {
        configureBootstrap(this.elasticachehost,this.elasticacheport,handler,new Bootstrap(),nioEventLoopGroup,idleTimeoutTimeUnit,readTimeout);
    }

    public void stop() {
        nioEventLoopGroup.shutdownGracefully();
        handler.shutdown();
    }

    public static ChannelFuture configureBootstrap(String host,int port,final ClientInfoClientHandler handler,
                                                   Bootstrap b, EventLoopGroup g, final TimeUnit idleTimeoutTimeUnit, final long idleTimeout) {
        b.group(g)
                .channel(NioSocketChannel.class)
                .remoteAddress(host,port)

                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ConfigInfoDecoder());
                        ch.pipeline().addLast(new IdleStateHandler(idleTimeout, 0, 0,idleTimeoutTimeUnit));
                        ch.pipeline().addLast(handler);
                    }
                });

        return b.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.cause() != null) {
                    log.warn("Failed to connect: {}", future.cause());
                }
            }
        });

    }
}
