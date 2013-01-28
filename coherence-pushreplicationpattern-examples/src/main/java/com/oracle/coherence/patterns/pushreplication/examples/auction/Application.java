/*
 * File: Application.java
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

package com.oracle.coherence.patterns.pushreplication.examples.auction;

import com.oracle.coherence.patterns.pushreplication.examples.NamedCacheModel;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ExtractorProcessor;
import com.tangosol.util.processor.UpdaterProcessor;

import net.miginfocom.swing.MigLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.UnknownHostException;

import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The Push Replication Auction Example {@link Application}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class Application
{
    /**
     * The label of the START button.
     */
    public static final String START = "Start";

    /**
     * The label of the STOP button.
     */
    public static final String        STOP = "Stop";

    private JFrame                    frmCoherencePushReplication;
    private JTable                    table;
    private JScrollPane               scrollPane;
    private JSlider                   sliderNumberOfBidders;
    private JButton                   btnBidding;
    private JLabel                    lblAuctionStatus;
    private JProgressBar              progressBar;
    private JButton                   btnAuctioneer;
    private CustomerReferenceRenderer bidderNameRenderer;
    private SiteNameRenderer          siteNameRenderer;
    private Auctioneer                auctioneer;
    private AutomatedBidGenerator     automatedBidGenerator;
    private String                    site;


    /**
     * Create the application.
     *
     * @throws UnknownHostException
     */
    public Application() throws UnknownHostException
    {
        this.site             = System.getProperty("tangosol.coherence.site");
        bidderNameRenderer    = new CartoonCharacterNameRenderer();
        siteNameRenderer      = new SiteNameRenderer();
        automatedBidGenerator = new AutomatedBidGenerator(site);
        auctioneer            = new Auctioneer(10, Integer.parseInt(System.getProperty("auction.duration", "3")));

        initialize();

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setModel(new NamedCacheModel<String, BiddingItem>(CacheFactory.getCache("bidding-cache"),
                                                                new BiddingItemRowDataProvider(bidderNameRenderer,
                                                                                               siteNameRenderer)));

        NamedCache controlCache = CacheFactory.getCache(AuctionControl.CACHE_NAME);

        controlCache.addMapListener(new MultiplexingMapListener()
        {
            @Override
            protected void onMapEvent(MapEvent mapEvent)
            {
                final AuctionControl auctionControl = mapEvent.getId() == MapEvent.ENTRY_DELETED
                                                      ? null : (AuctionControl) mapEvent.getNewValue();

                if (!auctionControl.isActive())
                {
                    automatedBidGenerator.stop();
                }

                if (auctionControl != null && auctionControl.isActive())
                {
                    lblAuctionStatus.setText(String.format("Auction Countdown: %d secs",
                                                           auctionControl.getRemainingDuration() / 1000));
                    progressBar.setValue(auctionControl.getPercentageRemaining());
                    btnBidding.setEnabled(true);
                }
                else
                {
                    lblAuctionStatus.setText("Auction Status: Inactive");
                    progressBar.setValue(0);
                    btnBidding.setEnabled(false);
                    btnBidding.setText(START);
                    btnAuctioneer.setText(START);
                }
            }
        });
    }


    /**
     * Launch the application.
     */
    public static void main(final String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                Application window = null;

                try
                {
                    window = new Application();
                    window.frmCoherencePushReplication.setVisible(true);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Initialize the contents of the frame.
     */
    @SuppressWarnings("serial")
    private void initialize()
    {
        frmCoherencePushReplication = new JFrame();
        this.frmCoherencePushReplication.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
                automatedBidGenerator.kill();
                auctioneer.kill();
            }
        });
        frmCoherencePushReplication.setTitle(String.format("Coherence Multi-Site Auction Demonstration (%s)",
                                                           siteNameRenderer.getDisplayName(site)));
        frmCoherencePushReplication.setBounds(100, 100, 578, 387);
        frmCoherencePushReplication.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmCoherencePushReplication.getContentPane().setLayout(new MigLayout("",
                                                                             "[][119.00,left][418px,grow,left]",
                                                                             "[17px,top][14px,top][14px,top][161px,grow,top][][]"));

        JLabel captionApplicationTitle = new JLabel(frmCoherencePushReplication.getTitle());

        captionApplicationTitle.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        frmCoherencePushReplication.getContentPane().add(captionApplicationTitle, "cell 1 0 2 1,growx,aligny top");

        JLabel captionCopyright = new JLabel("Copyright (c) 2011, Oracle Corporation");

        captionCopyright.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        frmCoherencePushReplication.getContentPane().add(captionCopyright, "cell 1 1 2 1,growx,aligny top");

        JLabel captionVersion = new JLabel("Version 1.0.0");

        captionVersion.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        frmCoherencePushReplication.getContentPane().add(captionVersion, "cell 1 2 2 1,growx,aligny top");

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        tabbedPane.setBackground(UIManager.getColor("CheckBox.background"));
        frmCoherencePushReplication.getContentPane().add(tabbedPane, "cell 1 3 2 1,grow");

        JPanel tabAuctionMonitor = new JPanel();

        tabAuctionMonitor.setOpaque(false);
        tabbedPane.addTab("Auction Monitor", null, tabAuctionMonitor, null);
        tabAuctionMonitor.setLayout(new MigLayout("", "[427px,grow]", "[14px,top][84px,grow,top]"));

        JLabel lblAuctionItems = new JLabel("Auction Items:");

        lblAuctionItems.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        tabAuctionMonitor.add(lblAuctionItems, "cell 0 0,growx,aligny top");

        this.table = new JTable()
        {
            public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,
                                                      int                                 row,
                                                      int                                 column)
            {
                {
                    Component component = super.prepareRenderer(renderer, row, column);

                    if (!getSelectionModel().isSelectedIndex(row))
                    {
                        if (row % 2 == 0 &&!isCellSelected(row, column))
                        {
                            component.setBackground(new Color(0xee, 0xee, 0xee));
                        }
                        else
                        {
                            component.setBackground(getBackground());
                        }
                    }

                    return component;
                }
            }
        };

        this.scrollPane = new JScrollPane();
        scrollPane.addInputMethodListener(new InputMethodListener()
        {
            public void caretPositionChanged(InputMethodEvent arg0)
            {
            }
            public void inputMethodTextChanged(InputMethodEvent arg0)
            {
            }
        });
        this.scrollPane.setViewportView(this.table);
        tabAuctionMonitor.add(this.scrollPane, "cell 0 1,grow");

        JPanel tabAutomatedBidding = new JPanel();

        tabAutomatedBidding.setOpaque(false);
        tabbedPane.addTab("Automated Bidding", null, tabAutomatedBidding, null);
        tabAutomatedBidding.setLayout(new MigLayout("", "[][grow][33.00]", "[][][]"));

        final JLabel captionNumberOfBidders = new JLabel("Number of Bidders:");

        captionNumberOfBidders.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        tabAutomatedBidding.add(captionNumberOfBidders, "cell 0 0,aligny center");

        final JLabel lblNumberOfBidders = new JLabel("1");

        lblNumberOfBidders.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        lblNumberOfBidders.setHorizontalAlignment(SwingConstants.LEFT);
        tabAutomatedBidding.add(lblNumberOfBidders, "cell 2 0,aligny center");

        sliderNumberOfBidders = new JSlider();
        sliderNumberOfBidders.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                lblNumberOfBidders.setText(String.format("%d", sliderNumberOfBidders.getValue()));

                if (automatedBidGenerator != null)
                {
                    automatedBidGenerator.setNumberOfBidders(sliderNumberOfBidders.getValue());
                }
            }
        });
        sliderNumberOfBidders.setSnapToTicks(true);
        sliderNumberOfBidders.setValue(1);
        sliderNumberOfBidders.setMaximum(10);
        tabAutomatedBidding.add(sliderNumberOfBidders, "cell 1 0,growx,aligny center");

        final JLabel captionBidsPerSecond = new JLabel("Bids Per Second:");

        captionBidsPerSecond.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        tabAutomatedBidding.add(captionBidsPerSecond, "cell 0 1,aligny center");

        final JLabel lblBidsPerSecond = new JLabel("1");

        lblBidsPerSecond.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        lblBidsPerSecond.setHorizontalAlignment(SwingConstants.LEFT);
        tabAutomatedBidding.add(lblBidsPerSecond, "cell 2 1,aligny center");

        final JSlider sliderBidsPerSecond = new JSlider();

        sliderBidsPerSecond.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                lblBidsPerSecond.setText(String.format("%d", sliderBidsPerSecond.getValue()));

                if (automatedBidGenerator != null)
                {
                    automatedBidGenerator.setBidsPerSecond(sliderBidsPerSecond.getValue());
                }
            }
        });
        sliderBidsPerSecond.setSnapToTicks(true);
        sliderBidsPerSecond.setMaximum(10);
        sliderBidsPerSecond.setValue(1);
        tabAutomatedBidding.add(sliderBidsPerSecond, "cell 1 1,growx,aligny center");

        JLabel captionAutomatedBidding = new JLabel("Automated Bidding:");

        captionAutomatedBidding.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        tabAutomatedBidding.add(captionAutomatedBidding, "cell 0 2,aligny center");

        btnBidding = new JButton(START);
        btnBidding.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (btnBidding.getText().equals(START))
                {
                    automatedBidGenerator.start();
                    btnBidding.setText(STOP);
                }
                else
                {
                    automatedBidGenerator.stop();
                    btnBidding.setText(START);
                }
            }
        });
        btnBidding.setEnabled(false);
        tabAutomatedBidding.add(btnBidding, "cell 1 2");

        lblAuctionStatus = new JLabel("Auction Status: Inactive");
        lblAuctionStatus.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        frmCoherencePushReplication.getContentPane().add(lblAuctionStatus, "cell 1 4");

        progressBar = new JProgressBar();
        frmCoherencePushReplication.getContentPane().add(progressBar, "cell 2 4,growx,aligny top");

        JLabel captionAuctioneer = new JLabel("Auctioneer:");

        captionAuctioneer.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        this.frmCoherencePushReplication.getContentPane().add(captionAuctioneer, "flowx,cell 1 5");
        captionAuctioneer.setVisible(site.equals("Site1"));

        btnAuctioneer = new JButton("Start");
        btnAuctioneer.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (btnAuctioneer.getText().equals(START))
                {
                    auctioneer.start();
                    btnAuctioneer.setText(STOP);
                }
                else
                {
                    auctioneer.stop();
                    btnAuctioneer.setText(START);
                }
            }
        });
        this.frmCoherencePushReplication.getContentPane().add(btnAuctioneer, "cell 1 5");
        btnAuctioneer.setEnabled(true);
        btnAuctioneer.setVisible(site.equals("Site1"));
    }


    /**
     * An {@link Auctioneer} controls the life-cycle of an auction.
     */
    private static class Auctioneer implements Runnable
    {
        private enum State {Stopped,
                            Running,
                            Dead}


        private State  state;
        private int    totalItems;
        private long   auctionDurationMS;
        private Thread thread;


        /**
         * Constructs an {@link Auctioneer} to control an auction of
         * a specified duration with a specified number of items.
         *
         * @param totalItems
         * @param auctionDurationMins
         */
        public Auctioneer(int totalItems,
                          int auctionDurationMins)
        {
            this.state             = State.Stopped;

            this.totalItems        = totalItems;
            this.auctionDurationMS = auctionDurationMins * 1000 * 60;

            thread                 = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }


        /**
         * Starts the {@link Auctioneer} (and auction)
         */
        public synchronized void start()
        {
            if (state == State.Stopped)
            {
                // create the auction items
                NamedCache biddingCache = CacheFactory.getCache(BiddingItem.CACHE_NAME);

                Random     generator    = new Random();

                // Create a BiddingItem object with random starting prices from $10 to
                // $1000.
                for (long i = 1; i <= totalItems; i++)
                {
                    long randomLong = generator.nextLong();

                    if (randomLong < 0)
                    {
                        randomLong = randomLong * -1;
                    }

                    long price = randomLong % 1000;

                    if (price < 10)
                    {
                        price = 10;
                    }

                    //
                    // If auctioneer is run multiple times against a cache server,
                    // it will want to clean out old items and prices of a previous
                    // auction, so a remove is executed.
                    //

                    biddingCache.put("" + i, new BiddingItem(price));
                }

                // wake up the thread to start the auction
                state = State.Running;
                notify();
            }
        }


        /**
         * Stops the {@link Auctioneer} (and auction)
         */
        public synchronized void stop()
        {
            if (state == State.Running)
            {
                NamedCache controlCache = CacheFactory.getCache(AuctionControl.CACHE_NAME);

                controlCache.invoke("control",
                                    new ConditionalProcessor(new EqualsFilter("isActive",
                                                                              Boolean.TRUE),
                                                             new UpdaterProcessor("reduceDurationRemaining",
                                                                                  auctionDurationMS)));

                state = State.Stopped;
                notify();
            }
        }


        /**
         * Kills the {@link Auctioneer} (and auction, after which the auction
         * can not be restarted).
         */
        public synchronized void kill()
        {
            state = State.Dead;
            notify();
        }


        /**
         * Obtain the current state of the {@link Auctioneer} (and auction).
         *
         * @return the current state
         */
        public synchronized State getState()
        {
            return state;
        }


        /**
         * {@inheritDoc}
         */
        public void run()
        {
            try
            {
                while (getState() != State.Dead)
                {
                    synchronized (this)
                    {
                        wait();
                    }

                    if (getState() == State.Running)
                    {
                        // create the auction control
                        NamedCache controlCache = CacheFactory.getCache(AuctionControl.CACHE_NAME);

                        controlCache.invoke("control",
                                            new ConditionalPut(new NotFilter(PresentFilter.INSTANCE),
                                                               new AuctionControl(totalItems)));

                        // set the time the auction has to run (this starts the auction)
                        controlCache.invoke("control", new UpdaterProcessor("startBidding", auctionDurationMS));

                        // determine how long the auction has left to run
                        long  activeTimeLeftMillis = auctionDurationMS;

                        State state                = getState();

                        while (state == State.Running && activeTimeLeftMillis > 0)
                        {
                            long ldtStart = Base.getSafeTimeMillis();

                            Thread.sleep(1000);

                            long ldtEnd         = Base.getSafeTimeMillis();

                            long activeTimeUsed = ldtEnd - ldtStart;

                            // Reduce the time remaining in the auction.  When the "reduceDurationRemaining"
                            // method sees that no time is remaining it will set m_IsActive to false.  This
                            // terminates the auction.

                            controlCache.invoke("control",
                                                new ConditionalProcessor(new EqualsFilter("isActive",
                                                                                          Boolean.TRUE),
                                                                         new UpdaterProcessor("reduceDurationRemaining",
                                                                                              activeTimeUsed)));

                            activeTimeLeftMillis = activeTimeLeftMillis - activeTimeUsed;

                            state                = getState();
                        }

                        stop();
                    }
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }


    /**
     * A {@link AutomatedBidGenerator} is a robot that generates random
     * bids on random items for a specific site on behalf of a number of
     * bidders.
     */
    private static class AutomatedBidGenerator implements Runnable
    {
        private enum State {Stopped,
                            Running,
                            Dead}


        private String siteName;
        private State  state;
        private int    numberOfBidders;
        private int    bidsPerSecond;
        private Random random;
        private Thread thread;


        /**
         * Constructs an {@link AutomatedBidGenerator}.
         *
         * @param siteName
         */
        public AutomatedBidGenerator(String siteName)
        {
            this.siteName   = siteName;

            numberOfBidders = 1;
            bidsPerSecond   = 1;
            state           = State.Stopped;

            random          = new Random(System.currentTimeMillis());

            thread          = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }


        /**
         * Obtain the name of the site for which the {@link AutomatedBidGenerator}
         * will generate bids.
         *
         * @return  the site name for the {@link AutomatedBidGenerator}
         */
        public synchronized String getSiteName()
        {
            return siteName;
        }


        /**
         * Sets the number of bidders the {@link AutomatedBidGenerator}
         * should generate bids for.
         *
         * @param numberOfBidders
         */
        public synchronized void setNumberOfBidders(int numberOfBidders)
        {
            this.numberOfBidders = numberOfBidders;
        }


        /**
         * Obtain the number of bidders the {@link AutomatedBidGenerator}
         * is bidding on behalf of.
         *
         * @return the number of bidders
         */
        public synchronized int getNumberOfBidders()
        {
            return numberOfBidders;
        }


        /**
         * Sets the number of bids per second the {@link AutomatedBidGenerator}
         * should place per bidder.
         *
         * @param bidsPerSecond
         */
        public synchronized void setBidsPerSecond(int bidsPerSecond)
        {
            this.bidsPerSecond = bidsPerSecond;
        }


        /**
         * Obtains the number of bids per second to place.
         *
         * @return the number of bids per second
         */
        public synchronized int getBidsPerSecond()
        {
            return bidsPerSecond;
        }


        /**
         * Starts the bidding.
         */
        public synchronized void start()
        {
            if (state == State.Stopped && state != State.Dead)
            {
                state = State.Running;
            }
        }


        /**
         * Stops the bidding.
         */
        public synchronized void stop()
        {
            if (state == State.Running && state != State.Dead)
            {
                state = State.Stopped;
            }
        }


        /**
         * Kills the bidder, after which no more bids can be placed.
         */
        public synchronized void kill()
        {
            state = State.Dead;
        }


        /**
         * Obtains the current state of the {@link AutomatedBidGenerator}.
         *
         * @return the current state
         */
        public synchronized State getState()
        {
            return state;
        }


        /**
         * Generate a positive random long number > 0 and <= some upper bound.
         *
         * @param upperBound
         *
         * @return a positive random long number
         */
        private long getRandomPositiveLong(long upperBound)
        {
            long randomLong = random.nextLong();

            if (randomLong < 0)
            {
                randomLong = randomLong * -1;
            }

            return (randomLong % upperBound) + 1;
        }


        /**
         * {@inheritDoc}
         */
        public void run()
        {
            State state = getState();

            while (state != State.Dead)
            {
                if (state == State.Running && getBidsPerSecond() > 0 && getNumberOfBidders() > 0)
                {
                    AuctionControl control =
                        (AuctionControl) CacheFactory.getCache(AuctionControl.CACHE_NAME).get("control");

                    if (control != null && control.isActive() && control.getTotalItems() > 0)
                    {
                        for (int i = 0; i < getNumberOfBidders(); i++)
                        {
                            String     itemKey          = "" + getRandomPositiveLong(control.getTotalItems());

                            NamedCache biddingItemCache = CacheFactory.getCache(BiddingItem.CACHE_NAME);
                            Long currentPrice = (Long) biddingItemCache.invoke(itemKey,
                                                                               new ExtractorProcessor("getPrice"));

                            if (currentPrice != null)
                            {
                                long              newPrice          = currentPrice + 10 + getRandomPositiveLong(50);

                                int               customerId        = (int) getRandomPositiveLong(10) - 1;

                                CustomerReference customerReference = new CustomerReference(getSiteName(), customerId);

                                biddingItemCache.invoke(itemKey, new PlaceBidProcessor(customerReference, newPrice));
                            }
                        }
                    }
                }

                try
                {
                    long sleepDurationMS;

                    if (getBidsPerSecond() > 0)
                    {
                        sleepDurationMS = 1000 / getBidsPerSecond();
                    }
                    else
                    {
                        sleepDurationMS = 1000;
                    }

                    Thread.sleep(sleepDurationMS);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                state = getState();
            }
        }
    }
}
