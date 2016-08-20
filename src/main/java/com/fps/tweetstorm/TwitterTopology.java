package com.fps.tweetstorm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import com.fps.tweetstorm.bolts.WatchDogBolt;
import com.fps.tweetstorm.bolts.ReTwitterBolt;
import com.fps.tweetstorm.bolts.TwitterFilterBolt;
import com.fps.tweetstorm.spouts.TwitterSuckerSpout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.TwitterException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.fps.tweetstorm.bolts.ReTwitterBolt.*;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.*;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.NON_OFFENSIVE_TWEET_OUTPUT;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.OFFENSIVE_TWEET_OUTPUT;
import static com.fps.tweetstorm.bolts.WatchDogBolt.*;
import static com.fps.tweetstorm.spouts.TwitterSuckerSpout.*;

/**
 * Created by fperezsorrosal on 7/11/16.
 */
public class TwitterTopology {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterTopology.class);

    public static final String TOPOLOGY_NAME = "twitter-ingester";

    private static final String TW_CONSUMER_KEY = "twitter.consumerKey";
    private static final String TW_CONSUMER_SECRET = "twitter.consumerSecret";
    private static final String TW_ACCESS_TOKEN = "twitter.accessToken";
    private static final String TW_ACCESS_SECRET = "twitter.accessSecret";

    protected Properties config;

    private LocalCluster cluster;

    public TwitterTopology(String configFileLocation, LocalCluster cluster) throws Exception {

        this.cluster = cluster;
        this.config = new Properties();
        try {
            InputStream is = new FileInputStream(configFileLocation);
            config.load(is);
        } catch (IOException e) {
            LOG.error("Error while reading configuration: " + e.getMessage());
            throw e;
        }

    }

    private void buildAndSubmit() throws Exception {

        TopologyBuilder builder = new TopologyBuilder();
        configureTwitterSpout(builder);
        configureBolts(builder);

        Config config = new Config();

        StormSubmitter.setLocalNimbus(cluster);
        StormSubmitter.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());

    }


    private void configureTwitterSpout(TopologyBuilder topology) {

        TwitterSuckerSpout.Config spoutConfig = new TwitterSuckerSpout.Config(this.config.getProperty(TW_CONSUMER_KEY),
                                                                              this.config.getProperty(TW_CONSUMER_SECRET),
                                                                              this.config.getProperty(TW_ACCESS_TOKEN),
                                                                              this.config.getProperty(TW_ACCESS_SECRET),
                                                                              this.config.getProperty("twitter.termsToObserve"));
        topology.setSpout(TWITTER_SUCKER_NAME, new TwitterSuckerSpout(spoutConfig));

    }

    private void configureBolts(TopologyBuilder topology) throws TwitterException {


        TwitterFilterBolt.Config twitterFilterConfig = new TwitterFilterBolt.Config(this.config.getProperty("twitter.offensiveTerms"));
        topology.setBolt(TWITTER_FILTER_BOLT_NAME, new TwitterFilterBolt(twitterFilterConfig), 4).shuffleGrouping(TWITTER_SUCKER_NAME);

        ReTwitterBolt.Config reTwitterConfig = new ReTwitterBolt.Config(this.config.getProperty(TW_CONSUMER_KEY),
                                                                        this.config.getProperty(TW_CONSUMER_SECRET),
                                                                        this.config.getProperty(TW_ACCESS_TOKEN),
                                                                        this.config.getProperty(TW_ACCESS_SECRET));
        topology.setBolt(RETWEET_BOLT_NAME, new ReTwitterBolt(reTwitterConfig), 4).shuffleGrouping(TWITTER_FILTER_BOLT_NAME,
                                                                                                   NON_OFFENSIVE_TWEET_OUTPUT);

        WatchDogBolt.Config notifierConfig = new WatchDogBolt.Config(this.config.getProperty(TW_CONSUMER_KEY),
                                                                     this.config.getProperty(TW_CONSUMER_SECRET),
                                                                     this.config.getProperty(TW_ACCESS_TOKEN),
                                                                     this.config.getProperty(TW_ACCESS_SECRET),
                                                                     this.config.getProperty("twitter.watchmen"));
        topology.setBolt(WATCHDOG_BOLT_NAME, new WatchDogBolt(notifierConfig), 4).shuffleGrouping(TWITTER_FILTER_BOLT_NAME,
                                                                                                  OFFENSIVE_TWEET_OUTPUT);

    }

    public static void main(String[] args) throws Exception {

        final LocalCluster cluster = new LocalCluster();
        TwitterTopology topology = new TwitterTopology(args[0], cluster);
        topology.buildAndSubmit();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cluster.killTopology(args[0]);
                cluster.shutdown();
            }
        });

    }

}
