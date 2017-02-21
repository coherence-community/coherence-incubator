/*
 * File: MessagingTests.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.patterns.messaging.test;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.options.Diagnostics;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.coherence.common.identifiers.Identifier;
import com.oracle.coherence.patterns.messaging.DefaultMessagingSession;
import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;
import com.oracle.coherence.patterns.messaging.TopicSubscriptionConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration Tests for the Messaging Pattern.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 * @author Brian Oliver
 */
public class MessagingTests
{
    /**
     * Establish a {@link CoherenceClusterResource} to orchestrate a {@link CoherenceCluster}.
     */
    @ClassRule
    public static CoherenceClusterResource clusterResource =
        new CoherenceClusterResource().with(CacheConfig.of("coherence-messagingpattern-cache-config.xml"),
                                            Diagnostics.enabled(),
                                            Console.system(),
                                            IPv4Preferred.yes(),
                                            Pof.config("coherence-messagingpattern-test-pof-config.xml"),
                                            Pof.enabled()).include(2,
                                                                   LocalStorage.enabled(),
                                                                   DisplayName.of("storage"));

    /**
     * The {@link ConfigurableCacheFactory} for the tests.
     */
    private static ConfigurableCacheFactory factory;

    /**
     * The {@link MessagingSession} to be used by all tests.
     */
    protected MessagingSession messagingSession;


    @Before
    public void setup()                                                           
    {
        // establish the CCF to interact with Coherence
        factory = clusterResource.createSession(SessionBuilders.storageDisabledMember());

        // create a shared MessagingSession for the tests
        messagingSession = DefaultMessagingSession.getInstance();
    }


    // ------------------------------------------------------------------------------------------
    // ------------------------------------ QUEUE TESTS -----------------------------------------
    // ------------------------------------------------------------------------------------------

    /**
     * Ensure that we can auto-commit messages and retrieve them in order.
     */
    @Test
    public void autoCommitSingleSubscriberTest()
    {
        Identifier queueIdentifier = messagingSession.createQueue("queue-autoCommitSingleSubscriberTest");

        Subscriber subscriber      = messagingSession.subscribe(queueIdentifier);

        messagingSession.publishMessage(queueIdentifier, "Hello");
        messagingSession.publishMessage(queueIdentifier, "Gudday");
        messagingSession.publishMessage(queueIdentifier, "Bonjour");
        messagingSession.publishMessage(queueIdentifier, "Howdy");

        assert "Hello".equals(subscriber.getMessage());

        // the following statement should have no effect
        subscriber.rollback();

        assert "Gudday".equals(subscriber.getMessage());

        // the following statement should have no effect
        subscriber.commit();

        assert "Bonjour".equals(subscriber.getMessage());
        assert "Howdy".equals(subscriber.getMessage());

        subscriber.unsubscribe();
    }


    /**
     * Ensure that we can manually commit messages and retrieve them in order.
     */
    @Test
    public void manualCommitSingleSubscriberTest()
    {
        Identifier queueIdentifier = messagingSession.createQueue("queue-manualCommitSingleSubscriberTest");

        Subscriber subscriber      = messagingSession.subscribe(queueIdentifier);

        subscriber.setAutoCommit(false);

        // the following statement should have no effect
        subscriber.rollback();

        messagingSession.publishMessage(queueIdentifier, "Hello");

        assert "Hello".equals(subscriber.getMessage());
        subscriber.commit();

        messagingSession.publishMessage(queueIdentifier, "Gudday");
        messagingSession.publishMessage(queueIdentifier, "Bonjour");
        messagingSession.publishMessage(queueIdentifier, "Howdy");
        subscriber.rollback();

        // the following statement should have no effect
        subscriber.commit();

        assert "Gudday".equals(subscriber.getMessage());
        assert "Bonjour".equals(subscriber.getMessage());
        assert "Howdy".equals(subscriber.getMessage());

        subscriber.commit();

        subscriber.unsubscribe();
    }


    /**
     * Ensure that we can auto-commit message and retrieve from multiple subscribers.
     */
    @Test
    public void autoCommittingMultipleSubscriberTest()
    {
        Identifier queueIdentifier = messagingSession.createQueue("queue-autoCommitingMultipleSubscriberTest");

        Subscriber subscriber1     = messagingSession.subscribe(queueIdentifier);
        Subscriber subscriber2     = messagingSession.subscribe(queueIdentifier);

        messagingSession.publishMessage(queueIdentifier, "Hello");
        messagingSession.publishMessage(queueIdentifier, "Gudday");

        // the following statement should have no effect
        subscriber1.commit();

        // the following statement should have no effect
        subscriber2.rollback();

        assert "Hello".equals(subscriber1.getMessage());
        assert "Gudday".equals(subscriber2.getMessage());

        // the following statement should have no effect
        subscriber2.commit();

        messagingSession.publishMessage(queueIdentifier, "Bonjour");
        messagingSession.publishMessage(queueIdentifier, "Howdy");
        messagingSession.publishMessage(queueIdentifier, "Hi");

        assert "Bonjour".equals(subscriber2.getMessage());
        assert "Howdy".equals(subscriber1.getMessage());

        subscriber1.unsubscribe();
        subscriber2.unsubscribe();

        Subscriber subscriber3 = messagingSession.subscribe(queueIdentifier);

        assert "Hi".equals(subscriber3.getMessage());

        subscriber3.unsubscribe();
    }


    /**
     * Ensure that we an concurrency publish messages.
     */
    @Test
    public void concurrentPublisherTest()
    {
        Identifier queueIdentifier = messagingSession.createQueue("queue-concurrentPublisherTest");

        Runnable   publisher       = new Runnable()
        {
            public void run()
            {
                for (int i = 1; i <= 10; i++)
                {
                    messagingSession.publishMessage("queue-concurrentPublisherTest", Integer.valueOf(i));
                }
            }
        };

        Thread publisherThread = new Thread(publisher);

        publisherThread.setDaemon(true);
        publisherThread.start();

        Subscriber subscriber = messagingSession.subscribe(queueIdentifier);
        int        count      = 1;

        while (count <= 10)
        {
            {
                Integer i = (Integer) subscriber.getMessage();

                Assert.assertThat(i, Matchers.is(count));
                count++;
            }
        }

        try
        {
            publisherThread.join();
        }
        catch (InterruptedException e)
        {
            Assert.fail("Publisher Thread failed to join");
        }
    }


    /**
     * Ensure that we can concurrently consume messages.
     *
     * @throws InterruptedException exception
     */
    @Test
    public void concurrentSubscriberTest() throws InterruptedException
    {
        Identifier queueIdentifier    = messagingSession.createQueue("queue-concurrentSubscriberTest");

        final int  nrMessages         = 10;
        final int  nrSubscribers      = 10;

        Runnable   subscriberRunnable = new Runnable()
        {
            public void run()
            {
                Subscriber subscriber                = messagingSession.subscribe("queue-concurrentSubscriberTest");
                long       lastMessageNumberConsumed = -1;

                for (int messagesToConsume = nrMessages; messagesToConsume > 0; messagesToConsume--)
                {
                    long messageNumber = (Long) subscriber.getMessage();

                    Assert.assertThat(lastMessageNumberConsumed, Matchers.lessThan(messageNumber));
                    lastMessageNumberConsumed = messageNumber;
                }

                subscriber.unsubscribe();
            }
        };

        ArrayList<Thread> subscriberThreads = new ArrayList<Thread>(nrSubscribers);

        for (int i = 1; i <= nrSubscribers; i++)
        {
            Thread consumerThread = new Thread(subscriberRunnable);

            consumerThread.setName("Subscriber Thread " + i);
            consumerThread.setDaemon(true);
            consumerThread.start();
            subscriberThreads.add(consumerThread);
        }

        for (int i = 1; i <= nrMessages * nrSubscribers; i++)
        {
            messagingSession.publishMessage(queueIdentifier, Long.valueOf(i));
        }

        for (int i = 0; i < nrSubscribers; i++)
        {
            subscriberThreads.get(i).join();
        }
    }


    /**
     * Randomly publish and consume messages using multiple threads.
     *
     * @throws InterruptedException exception
     */
    @Test
    public void randomQueueTest() throws InterruptedException
    {
        final int                         NR_MESSAGES_TO_PUBLISH      = 1000;
        final int                         NR_PUBLISHER_THREADS        = 10;
        final int                         NR_SUBSCRIBER_THREADS       = 10;
        final int                         MAX_PUBLISHER_WAIT_TIME_MS  = 200;
        final int                         MAX_SUBSCRIBER_WAIT_TIME_MS = 500;

        final AtomicLong                  remainingPublishers         = new AtomicLong(NR_PUBLISHER_THREADS);
        final AtomicLong                  nextMessageNumber           = new AtomicLong(1);
        final ConcurrentLinkedQueue<Long> receivedMessages            = new ConcurrentLinkedQueue<Long>();
        final Random                      random                      = new Random();

        messagingSession.createQueue("queue-randomTest");

        // ------------------
        // setup publishers
        Runnable publisherRunnable = new Runnable()
        {
            public void run()
            {
                System.out.printf("Commenced %s\n", Thread.currentThread().getName());

                try
                {
                    for (int i = 1; i <= NR_MESSAGES_TO_PUBLISH / NR_PUBLISHER_THREADS; i++)
                    {
                        messagingSession.publishMessage("queue-randomTest", nextMessageNumber.getAndIncrement());

                        Thread.sleep(random.nextInt(MAX_PUBLISHER_WAIT_TIME_MS));
                    }
                }
                catch (InterruptedException interruptedException)
                {
                    interruptedException.printStackTrace();
                }

                // if this is the last publisher, publish null's for each subscriber to terminate them
                if (remainingPublishers.decrementAndGet() == 0)
                {
                    for (int i = 1; i <= NR_SUBSCRIBER_THREADS; i++)
                    {
                        messagingSession.publishMessage("queue-randomTest", null);
                    }
                }

                System.out.printf("Completed %s\n", Thread.currentThread().getName());
            }
        };

        for (int i = 1; i <= NR_PUBLISHER_THREADS; i++)
        {
            Thread publisherThread = new Thread(publisherRunnable);

            publisherThread.setName(String.format("Publisher #%d", i));
            publisherThread.setDaemon(true);
            publisherThread.start();
        }

        // ------------------
        // setup subscribers
        Runnable subscriberRunnable = new Runnable()
        {
            public void run()
            {
                System.out.printf("Commenced %s\n", Thread.currentThread().getName());

                Subscriber subscriber = messagingSession.subscribe("queue-randomTest");
                Long       message;

                try
                {
                    do
                    {
                        Thread.sleep(random.nextInt(MAX_SUBSCRIBER_WAIT_TIME_MS));

                        message = (Long) subscriber.getMessage();

                        // add the message received to a set that we can confirm in order delivery
                        if (message != null)
                        {
                            receivedMessages.add(message);
                        }
                    }
                    while (message != null);
                }
                catch (InterruptedException interruptedException)
                {
                    interruptedException.printStackTrace();
                }

                subscriber.unsubscribe();
                System.out.printf("Completed %s\n", Thread.currentThread().getName());
            }
        };

        ArrayList<Thread> subscriberThreads = new ArrayList<Thread>(NR_SUBSCRIBER_THREADS);

        for (int i = 1; i <= NR_SUBSCRIBER_THREADS; i++)
        {
            Thread consumerThread = new Thread(subscriberRunnable);

            consumerThread.setName(String.format("Subscriber #%d", i));
            consumerThread.setDaemon(true);
            consumerThread.start();
            subscriberThreads.add(consumerThread);
        }

        for (int i = 0; i < NR_SUBSCRIBER_THREADS; i++)
        {
            subscriberThreads.get(i).join();
        }

        Assert.assertThat((long) receivedMessages.size(), Matchers.is(nextMessageNumber.get() - 1));
        System.out.printf("Received %d Messages\n", nextMessageNumber.get() - 1);
    }


    // ------------------------------------------------------------------------------------------
    // ------------------------------------ TOPIC TESTS -----------------------------------------
    // ------------------------------------------------------------------------------------------

    /**
     * Ensure we can retrieve messages in order using durable topic subscriptions.
     */
    @Test
    public void autoCommitDurableSubscriberTest()
    {
        Identifier topicIdentifier         = messagingSession.createTopic("topic-autoCommitDurableSubscriberTest");

        String     durableSubscriptionName = "durable-subscription";
        Subscriber durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                                  TopicSubscriptionConfiguration.newDurableConfiguration(durableSubscriptionName));

        messagingSession.publishMessage(topicIdentifier, "Hello");

        // the following statement should have no effect
        durableSubscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(durableSubscriber.getMessage()));
        durableSubscriber.release();

        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                       TopicSubscriptionConfiguration.newDurableConfiguration(durableSubscriptionName));

        // the following statement should have no effect
        durableSubscriber.commit();

        Assert.assertThat("Gudday", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Bonjour", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Howdy", Matchers.is(durableSubscriber.getMessage()));

        durableSubscriber.unsubscribe();
    }


    /**
     * Ensure that we can use manual commits with durable topic subscriptions.
     */
    @Test
    public void manualCommitDurableSubscriberTest()
    {
        Identifier topicIdentifier         = messagingSession.createTopic("topic-manualCommitDurableSubscriberTest");

        String     durableSubscriptionName = "durable-subscription";
        Subscriber durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                                  TopicSubscriptionConfiguration.newDurableConfiguration(durableSubscriptionName));

        durableSubscriber.setAutoCommit(false);

        messagingSession.publishMessage(topicIdentifier, "Hello");

        // the following statement should have no effect
        durableSubscriber.commit();

        Assert.assertThat("Hello", Matchers.is(durableSubscriber.getMessage()));
        durableSubscriber.commit();
        durableSubscriber.release();

        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                       TopicSubscriptionConfiguration.newDurableConfiguration(durableSubscriptionName));
        durableSubscriber.setAutoCommit(false);

        // the following statement should have no effect
        durableSubscriber.commit();

        Assert.assertThat("Gudday", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Bonjour", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Howdy", Matchers.is(durableSubscriber.getMessage()));
        durableSubscriber.rollback();

        Assert.assertThat("Gudday", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Bonjour", Matchers.is(durableSubscriber.getMessage()));
        Assert.assertThat("Howdy", Matchers.is(durableSubscriber.getMessage()));
        durableSubscriber.commit();

        durableSubscriber.unsubscribe();
    }


    /**
     * Ensure that we can publish concurrently to a topic.
     */
    @Test
    public void concurrentTopicPublisherTest()
    {
        Identifier topicIdentifier = messagingSession.createTopic("topic-concurrentPublisherTest");
        Subscriber subscriber      = messagingSession.subscribe(topicIdentifier);

        Runnable   publisher       = new Runnable()
        {
            public void run()
            {
                for (int i = 0; i < 10; i++)
                {
                    messagingSession.publishMessage("topic-concurrentPublisherTest", Integer.valueOf(i));
                }
            }
        };

        Thread publisherThread = new Thread(publisher);

        publisherThread.setDaemon(true);
        publisherThread.start();

        int count = 0;

        while (count < 10)
        {
            Integer i = (Integer) subscriber.getMessage();

            Assert.assertThat(Integer.valueOf(count), Matchers.is(i));
            count++;
        }
    }


    /**
     * Ensure that we can concurrently retrieve messages from a topic.
     *
     * @throws InterruptedException exception
     */
    @Test
    public void concurrentTopicSubscriberTest() throws InterruptedException
    {
        Identifier topicIdentifier    = messagingSession.createTopic("topic-concurrentSubscriberTest");

        final int  nrMessages         = 10;
        final int  nrSubscribers      = 10;

        Runnable   subscriberRunnable = new Runnable()
        {
            public void run()
            {
                Subscriber subscriber                = messagingSession.subscribe("topic-concurrentSubscriberTest");
                long       lastMessageNumberConsumed = -1;

                for (int messagesToConsume = nrMessages * nrSubscribers; messagesToConsume > 0; messagesToConsume--)
                {
                    long messageNumber = (Long) subscriber.getMessage();

                    Assert.assertThat(lastMessageNumberConsumed, Matchers.lessThan(messageNumber));
                    lastMessageNumberConsumed = messageNumber;
                }

                subscriber.unsubscribe();
            }
        };

        ArrayList<Thread> subscriberThreads = new ArrayList<Thread>(nrSubscribers);

        for (int i = 1; i <= nrSubscribers; i++)
        {
            Thread subscriberThread = new Thread(subscriberRunnable);

            subscriberThread.setName("Subscriber Thread " + i);
            subscriberThread.setDaemon(true);
            subscriberThread.start();
            subscriberThreads.add(subscriberThread);
        }

        // we wait a bit for the subscribers to start (including subscribe)
        Thread.sleep(5000);

        for (int i = 1; i <= nrMessages * nrSubscribers; i++)
        {
            messagingSession.publishMessage(topicIdentifier, Long.valueOf(i));
        }

        for (int i = 0; i < nrSubscribers; i++)
        {
            subscriberThreads.get(i).join();
        }
    }


    /**
     * Randomly publish and consume messages using multiple threads.
     *
     * @throws InterruptedException exception
     */
    @Test
    public void randomTopicTest() throws InterruptedException
    {
        final int        NR_MESSAGES_TO_PUBLISH      = 100;
        final int        NR_PUBLISHER_THREADS        = 10;
        final int        NR_SUBSCRIBER_THREADS       = 10;
        final int        MAX_PUBLISHER_WAIT_TIME_MS  = 200;
        final int        MAX_SUBSCRIBER_WAIT_TIME_MS = 500;

        final AtomicLong remainingPublishers         = new AtomicLong(NR_PUBLISHER_THREADS);
        final AtomicLong nextMessageNumber           = new AtomicLong(1);
        final Random     random                      = new Random();

        messagingSession.createTopic("topic-randomTest");

        // ------------------
        // setup subscribers
        Runnable subscriberRunnable = new Runnable()
        {
            public void run()
            {
                Subscriber       subscriber       = messagingSession.subscribe("topic-randomTest");
                LinkedList<Long> receivedMessages = new LinkedList<Long>();

                Long             message;

                try
                {
                    do
                    {
                        Thread.sleep(random.nextInt(MAX_SUBSCRIBER_WAIT_TIME_MS));

                        message = (Long) subscriber.getMessage();

                        // add the message received to a set that we can confirm
                        // in order delivery
                        if (message != null)
                        {
                            receivedMessages.add(message);
                        }
                    }
                    while (message != null);
                }
                catch (InterruptedException interruptedException)
                {
                    interruptedException.printStackTrace();
                }

                subscriber.unsubscribe();

                Assert.assertThat(receivedMessages.size(), Matchers.is(NR_MESSAGES_TO_PUBLISH));
            }
        };

        ArrayList<Thread> subscriberThreads = new ArrayList<Thread>(NR_SUBSCRIBER_THREADS);

        for (int i = 1; i <= NR_SUBSCRIBER_THREADS; i++)
        {
            Thread consumerThread = new Thread(subscriberRunnable);

            consumerThread.setName(String.format("Subscriber #%d", i));
            consumerThread.setDaemon(true);
            consumerThread.start();
            subscriberThreads.add(consumerThread);
        }

        Thread.sleep(2000);

        // ------------------
        // setup publishers
        Runnable publisherRunnable = new Runnable()
        {
            public void run()
            {
                try
                {
                    for (int i = 1; i <= NR_MESSAGES_TO_PUBLISH / NR_PUBLISHER_THREADS; i++)
                    {
                        messagingSession.publishMessage("topic-randomTest", nextMessageNumber.getAndIncrement());

                        Thread.sleep(random.nextInt(MAX_PUBLISHER_WAIT_TIME_MS));
                    }
                }
                catch (InterruptedException interruptedException)
                {
                    interruptedException.printStackTrace();
                }

                // if this is the last publisher, publish a null so each
                // subscriber will terminate
                if (remainingPublishers.decrementAndGet() == 0)
                {
                    messagingSession.publishMessage("topic-randomTest", null);
                }
            }
        };

        for (int i = 1; i <= NR_PUBLISHER_THREADS; i++)
        {
            Thread publisherThread = new Thread(publisherRunnable);

            publisherThread.setName(String.format("Publisher #%d", i));
            publisherThread.setDaemon(true);
            publisherThread.start();
        }

        for (int i = 0; i < NR_SUBSCRIBER_THREADS; i++)
        {
            subscriberThreads.get(i).join();
        }
    }
}
