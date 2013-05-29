package com.cubiclegrizzly.hornetstress;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;


public class HiveWorker implements Runnable {

    private String rootURL;
    private int acceptWait = 10; // "-1" will disable.
    public boolean running = true;
    public boolean hasSub = false;
    public long startTime = 0L;
    public long timeToSub = 0L;

    private int mID = -1;


    // Max. sleep time before beginning. (ms)
    private static final int STARTUP_SLEEP = 10000;
    // Sleep between calls. (ms)
    private static final int SLEEP_BETWEEN_INVOCATIONS = 1500;
    // Max amount of time that a pull-subscription request can take before we complain.
    private static final int SLOW_SUBSCRIPTION_CREATION = 10000;

    public HiveWorker(String rootURL, int pID, int acceptWait) {
        this.mID = pID;
        this.rootURL = rootURL;
        this.acceptWait = acceptWait;
    }

    public synchronized boolean hasSubscription() {
        return hasSub;
    }

    public synchronized boolean isAlive() {
        return running;
    }

    private synchronized void stopRunning() {
        running = false;
    }

    private void makeCall() {
        try {
            // There is a default sleep here so that we don't flood the hell out of httpd
            // on startup.
            Random r = new Random(STARTUP_SLEEP);
            Integer startSleep = r.nextInt(10000);
            Thread.sleep(startSleep);

            // TODO figure out how to obliterate SSL validation and allow over https/443
            DefaultHttpClient client = new DefaultHttpClient();

            // Get the head request.
            // This will send back the "msg-pull-subscriptions" header.
            HttpHead head = new HttpHead(rootURL);
            head.setHeader("Content-Type", "application/xml");
            HttpResponse resp = client.execute(head);

            // Get our pull subscription header.
            String pullSub = resp.getHeaders("msg-pull-subscriptions")[0].getValue();

            head.releaseConnection();

            String nextURL = pullSub;
            if (!hasSub) {
                startTime = System.currentTimeMillis();
            }

            boolean skipUpdate = false;
            while (true) {
                // (2) POST to that url. over and over. again and a again.
                HttpPost post = new HttpPost(nextURL);
                if (acceptWait > 0) {
                    post.addHeader("Accept-Wait", "10");
                }
                HttpResponse postResp = client.execute(post);
                int status = postResp.getStatusLine().getStatusCode();

                if (status == 503) {
                    // The msg-consumer-next header will only appear on
                    // a "good" 503 (i.e. no data, loop again).
                    // A "bad" 503 would be httpd failing to proxy.
                    try {
                        postResp.getHeaders("msg-consume-next")[0].getValue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        HivePrinter.printErr(mID, "503 APACHE: Sleeping one second.");
                        Thread.sleep(1000);
                        skipUpdate = true;
                    }
                } else if (status == 200) {
                    try {
                        // Get a message!
                        HivePrinter.printM();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (status == 201) {
                    // We got a subscription! This is what happens on the first call.
                    if (!hasSub) {
                        timeToSub = System.currentTimeMillis() - startTime;
                        if (timeToSub > SLOW_SUBSCRIPTION_CREATION) {
                            HivePrinter.printErr(mID, "TOOK " + timeToSub + "ms to get subscription");
                        }
                    }
                    hasSub = true;
                } else if (status == 405) {
                    // The subscription has died. This is bad.
                    HivePrinter.printErr(mID, "Status 405! The subscription has died.");
                    return;
                } else {
                    // Something horrible has happened.
                    HivePrinter.printErr(mID, "UNKNOWN STATUS CODE: " + postResp.getStatusLine().toString() +
                            "\n" + IOUtils.toString(postResp.getEntity().getContent()));
                    return;
                }

                //Get the msg-consume-next header.
                if (!skipUpdate) {
                    String nextPull = postResp.getHeaders("msg-consume-next")[0].getValue();
                    nextURL = nextPull;
                } else {
                    skipUpdate = false;
                }
                post.releaseConnection();

                // Sleep every time.
                Thread.sleep(SLEEP_BETWEEN_INVOCATIONS);
            }

        } catch (org.apache.http.conn.HttpHostConnectException e) {
            HivePrinter.printErr(mID, "Connection failed! Sleeping, then re-trying.");
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            // Die on awkward exceptions.
            return;
        }
    }

    @Override
    public void run() {
        makeCall();
        stopRunning();
        HivePrinter.printLn(mID, "Ending.");
    }


}
