package com.fps.tweetstorm.spouts;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by fperezsorrosal on 7/11/16.
 */
public class TwitterSuckerSpout extends BaseRichSpout {

    public static final String TWITTER_SUCKER_NAME = "twitterSpout";

    public static final String RAW_TWEET_STORM_F = "raw_tweet";

    private BlockingQueue<String> queue = new LinkedBlockingQueue<>(10000);

    private Authentication auth;
    private BasicClient client;

    private List<String> twitterTerms = new ArrayList<>();

    private SpoutOutputCollector collector;
    private Config config;

    public TwitterSuckerSpout(Config config) {

        this.config = config;
        this.twitterTerms = config.twitterTerms;

    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {

        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        hosebirdEndpoint.trackTerms(twitterTerms);

        // Define endpoint: By default, delimited=length is set (we need this for our processor)
        // and stall warnings are on.
        StatusesSampleEndpoint endpoint = new StatusesSampleEndpoint();
        endpoint.stallWarnings(false);
        this.auth = new OAuth1(config.consumerKey, config.consumerSecret, config.token, config.secret);
        this.collector = spoutOutputCollector;

        // Create a new Twitter client
        this.client = new ClientBuilder()
                .name("twitterSucker")
                .hosts(Constants.STREAM_HOST)
                .endpoint(hosebirdEndpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();
        client.connect();

    }

    @Override
    public void close() {

        client.stop();
        // Print some stats
        System.out.printf("The client read %d messages!\n", client.getStatsTracker().getNumMessages());

    }

    @Override
    public void nextTuple() {

        String ret = queue.poll();
        if (ret == null) {
            Utils.sleep(50);
        } else {
            collector.emit(new Values(ret));
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

        outputFieldsDeclarer.declare(new Fields(RAW_TWEET_STORM_F));

    }

    public static class Config implements Serializable {

        private static final long serialVersionUID = 5276718734571623335L;
        public final String consumerKey;
        public final String consumerSecret;
        public final String token;
        public final String secret;
        public final List<String> twitterTerms;

        public Config(String consumerKey,
                      String consumerSecret,
                      String token,
                      String secret,
                      String commaSeparatedTwitterTerms) {
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
            this.token = token;
            this.secret = secret;
            this.twitterTerms = Arrays.asList(commaSeparatedTwitterTerms.split("\\s*,\\s*"));
        }

    }

}