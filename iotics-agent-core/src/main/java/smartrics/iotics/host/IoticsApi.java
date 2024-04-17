package smartrics.iotics.host;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iotics.api.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import smartrics.iotics.host.grpc.HostManagedChannelBuilderFactory;
import smartrics.iotics.identity.IdentityManager;
import smartrics.iotics.identity.SimpleConfig;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.time.Duration;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IoticsApi {
    protected final SimpleIdentityManager sim;
    protected final ManagedChannel channel;
    private final TwinAPIGrpc.TwinAPIFutureStub twinAPIFutureStub;
    private final FeedAPIGrpc.FeedAPIFutureStub feedAPIFutureStub;
    private final FeedAPIGrpc.FeedAPIStub feedAPIStub;
    private final InputAPIGrpc.InputAPIFutureStub inputAPIFutureStub;
    private final InterestAPIGrpc.InterestAPIStub interestAPIStub;
    private final InterestAPIGrpc.InterestAPIBlockingStub interestAPIBlockingStub;
    private final SearchAPIGrpc.SearchAPIStub searchAPIStub;
    private final MetaAPIGrpc.MetaAPIStub metaAPIStub;
    private final Timer timer;

    public IoticsApi(IoticSpace ioticSpace, SimpleConfig userConf, SimpleConfig agentConf, Duration tokenValidityDuration) {
        sim = SimpleIdentityManager.Builder
                .anIdentityManager()
                .withAgentKeyID(agentConf.keyId())
                .withUserKeyID(userConf.keyId())
                .withAgentKeyName(agentConf.keyName())
                .withUserKeyName(userConf.keyName())
                .build();
        timer = new Timer("token-scheduler");

        ManagedChannelBuilder<?> channelBuilder = new HostManagedChannelBuilderFactory()
                .withSimpleIdentityManager(sim)
                .withTimer(timer)
                .withSGrpcEndpoint(ioticSpace.endpoints().grpc())
                .withTokenTokenDuration(tokenValidityDuration)
                .makeManagedChannelBuilder();
        channel = channelBuilder
                .executor(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("iot-grpc-%d").build()))
//                .keepAliveWithoutCalls(true)
                .build();

        this.twinAPIFutureStub = TwinAPIGrpc.newFutureStub(channel);
        this.feedAPIFutureStub = FeedAPIGrpc.newFutureStub(channel);
        this.feedAPIStub = FeedAPIGrpc.newStub(channel);
        this.inputAPIFutureStub = InputAPIGrpc.newFutureStub(channel);
        this.metaAPIStub = MetaAPIGrpc.newStub(channel);
        this.interestAPIStub = InterestAPIGrpc.newStub(channel);
        this.interestAPIBlockingStub = InterestAPIGrpc.newBlockingStub(channel);
        this.searchAPIStub = SearchAPIGrpc.newStub(channel);
    }

    public void stop(Duration timeout) {
        timer.cancel();
        try {
            channel.shutdown().awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
            channel.shutdownNow();
        } catch (InterruptedException e) {
            // TOOD fix this handling of IEx
            throw new RuntimeException(e);
        }

    }

    public TwinAPIGrpc.TwinAPIFutureStub twinAPIFutureStub() {
        return twinAPIFutureStub;
    }

    public FeedAPIGrpc.FeedAPIFutureStub feedAPIFutureStub() {
        return feedAPIFutureStub;
    }

    public FeedAPIGrpc.FeedAPIStub feedAPIStub() {
        return feedAPIStub;
    }

    public InputAPIGrpc.InputAPIFutureStub inputAPIFutureStub() {
        return inputAPIFutureStub;
    }

    public InterestAPIGrpc.InterestAPIStub interestAPIStub() {
        return interestAPIStub;
    }

    public InterestAPIGrpc.InterestAPIBlockingStub interestAPIBlockingStub() {
        return interestAPIBlockingStub;
    }

    public SearchAPIGrpc.SearchAPIStub searchAPIStub() {
        return searchAPIStub;
    }

    public MetaAPIGrpc.MetaAPIStub metaAPIStub() {
        return metaAPIStub;
    }

    public IdentityManager getSim() {
        return this.sim;
    }
}