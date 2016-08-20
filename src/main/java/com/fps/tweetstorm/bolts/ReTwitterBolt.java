package com.fps.tweetstorm.bolts;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.Serializable;

import static com.fps.tweetstorm.bolts.TwitterFilterBolt.*;
import static com.fps.tweetstorm.bolts.TwitterFilterBolt.TWEET_ID_STORM_F;

/**
 * Created by fperezsorrosal on 07/12/16
 */
public class ReTwitterBolt extends BaseBasicBolt {

    private static final Logger LOG = LoggerFactory.getLogger(ReTwitterBolt.class);
    public static final String RETWEET_BOLT_NAME = "reTwitter";

    private TwitterFactory factory;
    private Twitter twitter;
    private long myTwitterId;

    public ReTwitterBolt(Config config) throws TwitterException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
          .setOAuthConsumerKey(config.consumerKey)
          .setOAuthConsumerSecret(config.consumerSecret)
          .setOAuthAccessToken(config.token)
          .setOAuthAccessTokenSecret(config.secret);
        this.factory = new TwitterFactory(cb.build());
        this.twitter = factory.getInstance();
        myTwitterId = twitter.getId();
        LOG.info("My twitter id is {}", myTwitterId);
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {

        System.out.println(tuple.getValue(0));System.out.println(tuple.getValue(1));System.out.println(tuple.getValue(2));

        long tweetId = Long.valueOf(tuple.getStringByField(TWEET_ID_STORM_F));
        long twitterId = Long.valueOf(tuple.getStringByField(TWEET_WRITER_ID_STORM_F));

        try {
            // Don't retweet my own tweets!
            if (twitterId != myTwitterId) {
                twitter.retweetStatus(tweetId);
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    public static class Config implements Serializable {
        private static final long serialVersionUID = 5276718733211623335L;
        public final String consumerKey;
        public final String consumerSecret;
        public final String token;
        public final String secret;

        public Config(String consumerKey, String consumerSecret, String token, String secret) {
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
            this.token = token;
            this.secret = secret;
        }
    }

}