/*
 * File: TopicTests.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

import com.oracle.coherence.common.identifiers.Identifier;

import com.oracle.coherence.patterns.messaging.MessagingSession;
import com.oracle.coherence.patterns.messaging.Subscriber;
import com.oracle.coherence.patterns.messaging.TopicSubscriptionConfiguration;

import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Functional Tests for Topics using the Messaging Pattern.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 *  @author Paul Mackin
 *  @author Brian Oliver
 */
public class TopicTests extends AbstractMessagingTest
{
    /**
     * Ensure that we can auto-commit messages and retrieve them in order.
     */
    @Test
    public void autoCommitTest()
    {
        MessagingSession messagingSession = getMessagingSession();
        Identifier       topicIdentifier  = messagingSession.createTopic("topic-autoCommitTest");

        Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

        messagingSession.publishMessage(topicIdentifier, "Hello");
        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        String str1 = (String) subscriber.getMessage();
        String str2 = (String) subscriber.getMessage();
        String str3 = (String) subscriber.getMessage();
        String str4 = (String) subscriber.getMessage();

        Assert.assertThat("Hello", Matchers.is(str1));

        // the following statement should have no effect
        subscriber.rollback();

        Assert.assertThat("Gudday", Matchers.is(str2));

        // the following statement should have no effect
        subscriber.commit();

        Assert.assertThat("Bonjour", Matchers.is(str3));
        Assert.assertThat("Howdy", Matchers.is(str4));

        subscriber.unsubscribe();
    }


    /**
     * Ensure that we can rollback retrieved messages and receive them in order.
     */
    @Test
    public void nonDurableRollbackTest()
    {
        MessagingSession messagingSession = getMessagingSession();
        Identifier       topicIdentifier  = messagingSession.createTopic("topic-nonDurableRollbackTest");

        Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

        subscriber.setAutoCommit(false);

        // the following statement should have no effect
        subscriber.rollback();

        messagingSession.publishMessage(topicIdentifier, "Hello");
        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        // the following statement should have no effect
        subscriber.commit();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        subscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Gudday", Matchers.is(subscriber.getMessage()));
        subscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Gudday", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Bonjour", Matchers.is(subscriber.getMessage()));
        subscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Gudday", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Bonjour", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Howdy", Matchers.is(subscriber.getMessage()));
        subscriber.rollback();

        subscriber.unsubscribe();
    }


    /**
     * Ensure that we can rollback retrieved messages and receive them in order.
     */
    @Test
    public void nonDurableMixedCommitTest()
    {
        MessagingSession messagingSession = getMessagingSession();
        Identifier       topicIdentifier  = messagingSession.createTopic("topic-nonDurableMixedCommitTest");

        Subscriber       subscriber       = messagingSession.subscribe(topicIdentifier);

        subscriber.setAutoCommit(false);

        // the following statement should have no effect
        subscriber.rollback();

        messagingSession.publishMessage(topicIdentifier, "Hello");
        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        // the following statement should have no effect
        subscriber.commit();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        subscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Gudday", Matchers.is(subscriber.getMessage()));

        // this method will roll back also.
        subscriber.setAutoCommit(true);

        Assert.assertThat("Hello", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Gudday", Matchers.is(subscriber.getMessage()));

        // This should have no affect
        subscriber.rollback();

        Assert.assertThat("Bonjour", Matchers.is(subscriber.getMessage()));
        Assert.assertThat("Howdy", Matchers.is(subscriber.getMessage()));

        subscriber.unsubscribe();
    }


    /**
     * Ensure we can retrieve messages in order using durable topic subscriptions.
     */
    @Test
    public void autoCommitDurableSubscriberTest()
    {
        MessagingSession messagingSession        = getMessagingSession();
        Identifier       topicIdentifier         =
            messagingSession.createTopic("topic-autoCommitDurableSubscriberTest");

        String           durableSubscriptionName = "durable-subscription";
        Subscriber durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                                  TopicSubscriptionConfiguration
                                                                      .newDurableConfiguration(durableSubscriptionName));

        messagingSession.publishMessage(topicIdentifier, "Hello");

        // the following statement should have no effect
        durableSubscriber.rollback();

        Assert.assertThat("Hello", Matchers.is(durableSubscriber.getMessage()));
        durableSubscriber.release();

        messagingSession.publishMessage(topicIdentifier, "Gudday");
        messagingSession.publishMessage(topicIdentifier, "Bonjour");
        messagingSession.publishMessage(topicIdentifier, "Howdy");

        durableSubscriber =
            messagingSession.subscribe(topicIdentifier,
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
        MessagingSession messagingSession        = getMessagingSession();
        Identifier       topicIdentifier = messagingSession.createTopic("topic-manualCommitDurableSubscriberTest");

        String           durableSubscriptionName = "durable-subscription";
        Subscriber durableSubscriber = messagingSession.subscribe(topicIdentifier,
                                                                  TopicSubscriptionConfiguration
                                                                      .newDurableConfiguration(durableSubscriptionName));

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

        durableSubscriber =
            messagingSession.subscribe(topicIdentifier,
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
    public void concurrentPublisherTest()
    {
        final MessagingSession messagingSession = getMessagingSession();
        Identifier             topicIdentifier  = messagingSession.createTopic("topic-concurrentPublisherTest");
        Subscriber             subscriber       = messagingSession.subscribe(topicIdentifier);

        Runnable               publisher        = new Runnable()
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
    public void concurrentSubscriberTest() throws InterruptedException
    {
        final MessagingSession messagingSession   = getMessagingSession();
        Identifier             topicIdentifier    = messagingSession.createTopic("topic-concurrentSubscriberTest");

        final int              nrMessages         = 10;
        final int              nrSubscribers      = 10;

        Runnable               subscriberRunnable = new Runnable()
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
    public void randomTest() throws InterruptedException
    {
        final MessagingSession messagingSession            = getMessagingSession();
        final int              NR_MESSAGES_TO_PUBLISH      = 100;
        final int              NR_PUBLISHER_THREADS        = 10;
        final int              NR_SUBSCRIBER_THREADS       = 10;
        final int              MAX_PUBLISHER_WAIT_TIME_MS  = 200;
        final int              MAX_SUBSCRIBER_WAIT_TIME_MS = 500;

        final AtomicLong       remainingPublishers         = new AtomicLong(NR_PUBLISHER_THREADS);
        final AtomicLong       nextMessageNumber           = new AtomicLong(1);
        final Random           random                      = new Random();

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
