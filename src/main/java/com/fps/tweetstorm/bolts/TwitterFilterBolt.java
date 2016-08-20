package com.fps.tweetstorm.bolts;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.TwitterException;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fps.tweetstorm.spouts.TwitterSuckerSpout.RAW_TWEET_STORM_F;

/**
 * Created by fperezsorrosal on 07/12/16
 *
 * Task: Filter offensive terms defined by the user
 */
public class TwitterFilterBolt extends BaseBasicBolt {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterFilterBolt.class);

    public static final String NON_OFFENSIVE_TWEET_OUTPUT = "non-offensive-tweet";
    public static final String OFFENSIVE_TWEET_OUTPUT = "offensive-tweet";
    public static final String TWEET_ID_STORM_F = "tweet_id";
    public static final String TWEET_TEXT_STORM_F = "text";
    public static final String TWEET_WRITER_ID_STORM_F = "writer_id";

    public static final String TWEETER_TEXT = "text";
    public static final String TWEETER_TWEET_ID = "id_str";
    public static final String TWEETER_USER = "user";
    public static final String TWEETER_USER_ID = "id_str";
    public static final String TWEETER_USER_SCREEN_NAME = "screen_name";
    public static final String TWITTER_FILTER_BOLT_NAME = "twitterFilter";

    private Set<String> offensiveTerms;

    public TwitterFilterBolt(Config config) throws TwitterException {
        offensiveTerms = config.offensiveTerms;
    }

    @Override
    public void prepare(Map conf, TopologyContext context) {

    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {

        long startTime = System.currentTimeMillis();
        JsonObject tweetAsJSON = getJsonRepresentation(tuple);
        String twitId = tweetAsJSON.get(TWEETER_TWEET_ID).getAsString();
        String text = tweetAsJSON.get(TWEETER_TEXT).getAsString();
        String senderId = tweetAsJSON.get(TWEETER_USER).getAsJsonObject().get(TWEETER_USER_ID).getAsString();
        String sender = tweetAsJSON.get(TWEETER_USER).getAsJsonObject().get(TWEETER_USER_SCREEN_NAME).getAsString();

        // Compare tweet words against offensive terms discarding it if necessary
        Set<String> words = tokenizeAndClean(text);
        boolean setHasChanged = words.retainAll(offensiveTerms);
        if (setHasChanged && words.isEmpty()) {
            LOG.info("Omid Twit: {} : {} : {}", new Object[]{twitId, text, senderId});
            collector.emit(NON_OFFENSIVE_TWEET_OUTPUT, new Values(twitId, text.replaceAll("\\r|\\n", ""), senderId));
        } else {
            LOG.info("Offensive Twit detected: {}/{}/{} Notifiying watchmen", new Object[]{twitId, text, senderId});
            collector.emit(OFFENSIVE_TWEET_OUTPUT, new Values(twitId, text.replaceAll("\\r|\\n", ""), sender));
        }
        LOG.info("ElapsedTime: {}", System.currentTimeMillis() - startTime);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(NON_OFFENSIVE_TWEET_OUTPUT, new Fields(TWEET_ID_STORM_F,
                                                                      TWEET_TEXT_STORM_F,
                                                                      TWEET_WRITER_ID_STORM_F));
        declarer.declareStream(OFFENSIVE_TWEET_OUTPUT, new Fields(TWEET_ID_STORM_F,
                                                                  TWEET_TEXT_STORM_F,
                                                                  TWEET_WRITER_ID_STORM_F));
    }

    JsonObject getJsonRepresentation(Tuple tuple) {
        String rawTwit = tuple.getStringByField(RAW_TWEET_STORM_F);
        LOG.info("Raw Twit: {}", rawTwit);
        JsonParser parser = new JsonParser();
        return parser.parse(rawTwit).getAsJsonObject();
    }

    // Get a string, tokenize, lowercase, remove special chars and create a bag of words
    private Set<String> tokenizeAndClean(String text) {
        return Pattern.compile(" ")
                      .splitAsStream(text)
                      .parallel().map(String::toLowerCase)
                      .parallel().map(word -> word.replaceAll("[-+.^:;,!?\\r\\n]", ""))
                      .collect(Collectors.toSet());
    }

    public static class Config implements Serializable {

        private static final long serialVersionUID = 5276718733211623322L;

        public final Set<String> offensiveTerms;

        public Config(String offensiveTerms) {
            // Get list of comma-separated list of offensive terms, lowercase them and create a bag of words
            this.offensiveTerms = Pattern.compile(",")
                                         .splitAsStream(offensiveTerms)
                                         .parallel().map(String::toLowerCase)
                                         .collect(Collectors.toSet());
        }

    }

}