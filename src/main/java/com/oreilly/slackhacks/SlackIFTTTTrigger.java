package com.oreilly.slackhacks;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class SlackIFTTTTrigger {

    private static final String TOKEN = "insert your token here";
    private static final String IFTTT_SECRET_KEY = "insert your IFTTT Maker secret key here";

    public static void main(String[] args) throws Exception {
        //creating the session
        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(TOKEN);
        //adding a message listener to the session
        session.addMessagePostedListener(SlackIFTTTTrigger::listenToIFTTTCommand);
        //connecting the session to the Slack team
        session.connect();
        //delegating all the event management to the session
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void listenToIFTTTCommand(SlackMessagePosted event, SlackSession session) {
        if (!event.getChannel().isDirect() || event.getSender().isBot()) {
            return;
        }
        if (!event.getMessageContent().trim().startsWith("!IFTTT")) {
            return;
        }
        String[] splitCommand = event.getMessageContent().trim().split(" ");
        if (splitCommand.length < 2) {
            session.sendMessage(event.getChannel(), "You have to provide an IFTTT event to trigger, using the following syntax: !IFTTT {event_name})");
            return;
        }
        notifyIFTTT(event, session, splitCommand[1]);
    }

    private static void notifyIFTTT(SlackMessagePosted event, SlackSession session, String iftttEvent) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("https://maker.ifttt.com/trigger/" + iftttEvent + "/with/key/" + IFTTT_SECRET_KEY);
        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 400) {
                session.sendMessage(event.getChannel(), "An error occured while triggering IFTTT : " + statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            session.sendMessage(event.getChannel(), "An error occured while triggering IFTTT : " + e.getMessage());
        }
    }
}
