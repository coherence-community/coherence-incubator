/*
 * File: EC2AddressProvider.java
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

package com.oracle.coherence.cloud.amazon;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.tangosol.net.AddressProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link EC2AddressProvider} is an {@link AddressProvider} that relies on
 * EC2 characteristics to provide addresses to a Coherence cluster.
 * <p>
 * The {@link EC2AddressProvider} queries EC2 to determine what instances are
 * up and running and finds all instances that have an "Elastic IP" assigned to it.
 * The instances with an Elastic IP assigned to it will be selected as the WKA
 * nodes in the Coherence cluster.
 * <p>
 * <strong>Note:</strong> It is not the "Elastic IP" address itself that becomes
 * the WKA IP, instead the private IP-address of those instances are used. </p>
 * <p>
 * The default port used is 8088, however that can be overridden by using the
 * system property: <code>tangosol.coherence.ec2addressprovider.port</code>
 *
 * <h2>Authentication of EC2 calls</h2>
 * In order to make the necessary calls in to the EC2 API, we need the access
 * key and the secret key for the AWS account we are using. There are two ways
 * of specifying the keys. One is to provide the access key and the secret key
 * as Java system properties like this:
 * <p>
 * <ul>
 *    <li>tangosol.coherence.ec2addressprovider.accesskey=YourAccessKey</li>
 *    <li>tangosol.coherence.ec2addressprovider.secretkey=YourSecretKey</li>
 * </ul>
 * <p>
 * An alternative way is to provide an embedded properties file with the name
 * AwsCredentials.properties:
 * (the name of the properties file can be overridden with the property
 * tangosol.coherence.ec2addressprovider.propertyfile).
 * <p>
 * The properties file need to contain the secret key and the access key for your Amazon Web Services account.
 * <ul>
 *    <li>secretKey=YourSecretKey</li>
 *    <li>accessKey=YourAccessKey</li>
 * </ul>
 * <p>
 * <strong>Please note the camel casing of the key in the properties file!</strong>
 *
 * <h2>Configuring Coherence to use the EC2AddressProvider</h2>
 * In order to configure Coherence to use the EC2AddressProvider,
 * an override needs to be specified as a system property.
 * <p>
 * <code>tangosol.coherence.override=ec2addressprovider-override.xml</code>
 * <p>
 * If your application needs further overrides, you will have to refer to the
 * ec2addressprovider-override.xml file like this:
 * <pre>
 *     {@code <coherence xml-override="/tangosol-coherence-override.xml">}
 * </pre>
 * <p>
 * <h2>Starting an EC2 Coherence cluster with the EC2AddressProvider</h2>
 *
 * To start an EC2 Coherence cluster you will have to follow these steps:
 * <ul>
 *    <li>Start an instance</li>
 *    <li>Associate an Elastic IP to that instance</li>
 *    <li>Start Coherence with the EC2AddressProvider</li>
 *    <li>Start other instances where Coherence auto-starts with the EC2AddressProvider.</li>
 * </ul>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class EC2AddressProvider implements AddressProvider
{
    /**
     * The logger to use.
     */
    private static final Logger logger = Logger.getLogger(EC2AddressProvider.class.getName());

    /**
     * The list of socket addresses to EC2 .
     */
    private List<InetSocketAddress> wkaAddressList;

    /**
     * The current index in to the ArrayList.
     */
    private Iterator<InetSocketAddress> wkaIterator;


    /**
     * Standard constructor.
     *
     * @throws IOException if reading fails
     */
    public EC2AddressProvider() throws IOException
    {
        if (logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "Initializing WKA list from EC2 Elastic IP to instance mapping.");
        }

        wkaAddressList = generateWKAList(new AmazonEC2Client(determineCredentials()));
        wkaIterator    = wkaAddressList.iterator();
    }


    /**
     * Protected Constructor used for unit testing.
     *
     * @param dummyDifferentiator parameter to make this parameter list different from the default constructor.
     */
    protected EC2AddressProvider(String dummyDifferentiator)
    {
    }


    /**
     * Generates the WKA list using an AmazonEC2 client.
     *
     * @param ec2 the {@link AmazonEC2} client to use.
     *
     * @return the WKA list
     */
    protected List<InetSocketAddress> generateWKAList(AmazonEC2 ec2)
    {
        String                  portString = System.getProperty("tangosol.coherence.ec2addressprovider.port", "8088");
        int                     wkaPort                 = Integer.parseInt(portString);

        List<InetSocketAddress> resultList              = new ArrayList<InetSocketAddress>();

        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        List<Reservation>       reservations            = describeInstancesResult.getReservations();
        Set<Instance>           instances               = new HashSet<Instance>();

        for (Reservation reservation : reservations)
        {
            instances.addAll(reservation.getInstances());

            if (logger.isLoggable(Level.CONFIG))
            {
                logger.log(Level.CONFIG, "Examining EC2 reservation:" + reservation);
            }
        }

        logAllInstances(instances);

        DescribeAddressesResult elasticAddressesResult = ec2.describeAddresses();

        if (elasticAddressesResult != null)
        {
            for (Iterator<Address> elasticAddressIter = elasticAddressesResult.getAddresses().iterator();
                elasticAddressIter.hasNext(); )
            {
                Address elasticAddress = elasticAddressIter.next();

                for (Iterator<Instance> instIter = instances.iterator(); instIter.hasNext(); )
                {
                    Instance instance = instIter.next();

                    if (instance.getInstanceId().equals(elasticAddress.getInstanceId()))
                    {
                        // Now we have a match - add with default port
                        if (logger.isLoggable(Level.CONFIG))
                        {
                            logger.log(Level.CONFIG,
                                       "EC2AddressProvider - adding {0} from instance {1} to WKA list",
                                       new Object[] {instance.getPrivateIpAddress(), instance});
                        }

                        resultList.add(new InetSocketAddress(instance.getPrivateIpAddress(), wkaPort));
                    }
                }
            }

            if (resultList.size() == 0)
            {
                throw new RuntimeException("The EC2AddressProvider could not find any instance mapped to an Elastic IP");
            }
        }
        else
        {
            throw new RuntimeException("The EC2AddressProvider could not enumerate the Elastic IP Addresses");
        }

        return resultList;
    }


    /**
     * This method determines what credentials to use for EC2 authentication.
     *
     * @return the {@link AWSCredentials}
     *
     * @throws IOException if reading the property file fails
     */
    protected AWSCredentials determineCredentials() throws IOException
    {
        String accessKey = System.getProperty("tangosol.coherence.ec2addressprovider.accesskey");
        String secretKey = System.getProperty("tangosol.coherence.ec2addressprovider.secretkey");

        if ((accessKey == null) || (secretKey == null) || accessKey.equals("") || secretKey.equals(""))
        {
            if (logger.isLoggable(Level.CONFIG))
            {
                logger.log(Level.CONFIG, "No EC2AddressProvider credential system properties provided.");
            }

            // Retrieve the credentials from a properties resource instead.

            String propertyResource = System.getProperty("tangosol.coherence.ec2addressprovider.propertyfile",
                                                         "AwsCredentials.properties");
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyResource);

            if (stream != null)
            {
                return new PropertiesCredentials(stream);
            }
            else
            {
                throw new RuntimeException("The EC2AddressProvider could not find any credentials, neither as system properties, nor as "
                                           + propertyResource + " resource");
            }
        }
        else
        {
            return new BasicAWSCredentials(accessKey, secretKey);
        }
    }


    /**
     * Logs the instances we found.
     *
     * @param instances the instances we are to log
     */
    private void logAllInstances(Set<Instance> instances)
    {
        if (logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "The following instances were found:");

            for (Iterator<Instance> instIter = instances.iterator(); instIter.hasNext(); )
            {
                logger.log(Level.CONFIG, "EC2 instance:", instIter.next());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void accept()
    {
        // Not called
    }


    /**
    * {@inheritDoc}
    */
    public InetSocketAddress getNextAddress()
    {
        // Always increase index before use - initialized to -1
        if (wkaIterator.hasNext())
        {
            InetSocketAddress address = wkaIterator.next();

            if (logger.isLoggable(Level.FINEST))
            {
                logger.log(Level.FINEST, "Returning WKA address {0}", address);
            }

            return address;
        }
        else
        {
            // We must now return null according to the AddressProvider contract to terminate iteration.
            // However, we must also reset the iterator so that the next call starts from the
            // beginning of the WKA list
            wkaIterator = wkaAddressList.iterator();

            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void reject(Throwable exception)
    {
        // Not called
    }
}
