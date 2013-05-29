package com.cubiclegrizzly.hornetstress;

import java.util.LinkedList;
import java.util.List;

/**
 * Simple utility to stress-test the HornetQ-REST extensions.
 *
 * Run with the argument of a URL pointing to a hornetq-REST endpoint.
 * i.e.
 * http://localhost:8080/hornet-msg/topics/jms.topic.something.something/
 *
 */
class HornetHive
{
	public static int WORKER_THREADS = 300;
	
	public static void main(String[] args){
        String rootURL = null;
		System.out.println("Starting up");

        if(args.length == 0) {
            System.err.println("Error: No URL specified! You must supply at least the URL as an argument.");
            System.exit(255);
        }

		if(args.length > 0) {
            for(String arg : args) {
                if(rootURL == null ) {
                    rootURL = arg;
                } else {
                    Integer cat = Integer.parseInt(arg);
                    WORKER_THREADS = cat;
                }
            }
		}
		System.out.println("Using: " + WORKER_THREADS + " threads.");

		List<Thread> myList = new LinkedList<Thread>();
		List<HiveWorker> workers = new LinkedList<HiveWorker>();
		for(int i =0 ; i< WORKER_THREADS; i++) {
			HiveWorker worker = new HiveWorker(rootURL, i, 60);
			workers.add(worker);
			myList.add(new Thread(worker));
		}
		for(Thread t : myList) {
			t.start();
		}

		while(true) {
			try {
				Thread.sleep(10000);
				// Every 10 seconds, give a status.
				int liveThreads = 0;
				int subCounts = 0;
				for(Thread t: myList) {
					if(t.isAlive()) liveThreads++;
				}
				for(HiveWorker w : workers) {
					if(w.hasSubscription()) {
						subCounts++;
					}
				}
				HivePrinter.printLn("**************************** " + liveThreads + " LIVE THREADS (" + subCounts + " subscribed) **************");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}