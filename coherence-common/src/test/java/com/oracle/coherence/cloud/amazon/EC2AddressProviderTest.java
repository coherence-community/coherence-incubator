/*
 * File: EC2AddressProviderTest.java
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
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.oracle.tools.junit.AbstractTest;
import com.oracle.tools.runtime.network.Constants;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * A {@link EC2AddressProviderTest} class testing the combinations of Elastic IP to instance mappings that the EC2 API can yield.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class EC2AddressProviderTest extends AbstractTest
{
    /**
     * Tests generation of WKA list where there is one Elastic IP matching an instance.
     */
    @Test
    public void generateWKAListTest()
    {
        AmazonEC2               ec2              = mock(AmazonEC2.class);
        DescribeInstancesResult descResult       = mock(DescribeInstancesResult.class);
        ArrayList<Reservation>  reservationsList = new ArrayList<Reservation>();
        Reservation             mockReservation  = mock(Reservation.class);
        Instance                mockInstance     = mock(Instance.class);

        reservationsList.add(mockReservation);
        stub(ec2.describeInstances()).toReturn(descResult);
        stub(descResult.getReservations()).toReturn(reservationsList);

        ArrayList<Instance> instanceList = new ArrayList<Instance>();

        instanceList.add(mockInstance);
        stub(mockReservation.getInstances()).toReturn(instanceList);

        ArrayList<Address> elasticIPList        = new ArrayList<Address>();
        Address            mockElasticIPAddress = mock(Address.class);

        elasticIPList.add(mockElasticIPAddress);

        DescribeAddressesResult descAddressesResult = mock(DescribeAddressesResult.class);

        stub(ec2.describeAddresses()).toReturn(descAddressesResult);
        stub(descAddressesResult.getAddresses()).toReturn(elasticIPList);

        stub(mockElasticIPAddress.getInstanceId()).toReturn("wka-instance");
        stub(mockInstance.getInstanceId()).toReturn("wka-instance");

        stub(mockInstance.getPrivateIpAddress()).toReturn(Constants.getLocalHost());

        EC2AddressProvider      addressProvider = new EC2AddressProvider("dummy");

        List<InetSocketAddress> wkaList         = addressProvider.generateWKAList(ec2);

        assertTrue(wkaList.size() > 0);
        assertEquals("127.0.0.1", wkaList.get(0).getAddress().getHostAddress());
        assertTrue(wkaList.get(0).getPort() == 8088);
    }


    /**
     * Tests generation of WKA list where there is one Elastic IP matching an instance and we set the port to a non-default port.
     */
    @Test
    public void generateWKAListTestSettingPort()
    {
        System.setProperty("tangosol.coherence.ec2addressprovider.port", "9999");

        AmazonEC2               ec2              = mock(AmazonEC2.class);
        DescribeInstancesResult descResult       = mock(DescribeInstancesResult.class);
        ArrayList<Reservation>  reservationsList = new ArrayList<Reservation>();
        Reservation             mockReservation  = mock(Reservation.class);
        Instance                mockInstance     = mock(Instance.class);

        reservationsList.add(mockReservation);
        stub(ec2.describeInstances()).toReturn(descResult);
        stub(descResult.getReservations()).toReturn(reservationsList);

        ArrayList<Instance> instanceList = new ArrayList<Instance>();

        instanceList.add(mockInstance);
        stub(mockReservation.getInstances()).toReturn(instanceList);

        ArrayList<Address> elasticIPList        = new ArrayList<Address>();
        Address            mockElasticIPAddress = mock(Address.class);

        elasticIPList.add(mockElasticIPAddress);

        DescribeAddressesResult descAddressesResult = mock(DescribeAddressesResult.class);

        stub(ec2.describeAddresses()).toReturn(descAddressesResult);
        stub(descAddressesResult.getAddresses()).toReturn(elasticIPList);

        stub(mockElasticIPAddress.getInstanceId()).toReturn("wka-instance");
        stub(mockInstance.getInstanceId()).toReturn("wka-instance");

        stub(mockInstance.getPrivateIpAddress()).toReturn(Constants.getLocalHost());

        EC2AddressProvider      addressProvider = new EC2AddressProvider("dummy");

        List<InetSocketAddress> wkaList         = addressProvider.generateWKAList(ec2);

        assertTrue(wkaList.size() > 0);
        assertEquals("127.0.0.1", wkaList.get(0).getAddress().getHostAddress());
        assertTrue(wkaList.get(0).getPort() == 9999);
    }


    /**
     * Tests generation of WKA list where there is no match between running instances and the  Elastic IP.
     */
    @Test(expected = RuntimeException.class)
    public void generateWKAListTestNoMatch()
    {
        AmazonEC2               ec2              = mock(AmazonEC2.class);
        DescribeInstancesResult descResult       = mock(DescribeInstancesResult.class);
        ArrayList<Reservation>  reservationsList = new ArrayList<Reservation>();
        Reservation             mockReservation  = mock(Reservation.class);
        Instance                mockInstance     = mock(Instance.class);

        reservationsList.add(mockReservation);
        stub(ec2.describeInstances()).toReturn(descResult);
        stub(descResult.getReservations()).toReturn(reservationsList);

        ArrayList<Instance> instanceList = new ArrayList<Instance>();

        instanceList.add(mockInstance);
        stub(mockReservation.getInstances()).toReturn(instanceList);

        ArrayList<Address> elasticIPList        = new ArrayList<Address>();
        Address            mockElasticIPAddress = mock(Address.class);

        elasticIPList.add(mockElasticIPAddress);

        DescribeAddressesResult descAddressesResult = mock(DescribeAddressesResult.class);

        stub(ec2.describeAddresses()).toReturn(descAddressesResult);
        stub(descAddressesResult.getAddresses()).toReturn(elasticIPList);

        stub(mockElasticIPAddress.getInstanceId()).toReturn("elastic-instance");
        stub(mockInstance.getInstanceId()).toReturn("another-instance");

        stub(mockInstance.getPrivateIpAddress()).toReturn(Constants.getLocalHost());

        EC2AddressProvider      addressProvider = new EC2AddressProvider("dummy");

        List<InetSocketAddress> wkaList         = addressProvider.generateWKAList(ec2);

        assertTrue(wkaList.size() == 0);

    }


    /**
     * Tests generation of WKA list where there is no Elastic IP Address.
     */
    @Test(expected = RuntimeException.class)
    public void generateWKAListTestNoElasticIP()
    {
        AmazonEC2               ec2              = mock(AmazonEC2.class);
        DescribeInstancesResult descResult       = mock(DescribeInstancesResult.class);
        ArrayList<Reservation>  reservationsList = new ArrayList<Reservation>();
        Reservation             mockReservation  = mock(Reservation.class);
        Instance                mockInstance     = mock(Instance.class);

        reservationsList.add(mockReservation);
        stub(ec2.describeInstances()).toReturn(descResult);
        stub(descResult.getReservations()).toReturn(reservationsList);

        ArrayList<Instance> instanceList = new ArrayList<Instance>();

        instanceList.add(mockInstance);
        stub(mockReservation.getInstances()).toReturn(instanceList);

        ArrayList<Address>      elasticIPList       = new ArrayList<Address>();
        DescribeAddressesResult descAddressesResult = mock(DescribeAddressesResult.class);

        stub(ec2.describeAddresses()).toReturn(descAddressesResult);
        stub(descAddressesResult.getAddresses()).toReturn(elasticIPList);

        stub(mockInstance.getInstanceId()).toReturn("another-instance");

        stub(mockInstance.getPrivateIpAddress()).toReturn(Constants.getLocalHost());

        EC2AddressProvider      addressProvider = new EC2AddressProvider("dummy");

        List<InetSocketAddress> wkaList         = addressProvider.generateWKAList(ec2);

        assertTrue(wkaList.size() == 0);

    }


    /**
     * Tests passing in credentials using system properties.
     *
     * @throws IOException if reading a resource fails
     */
    @Test
    public void testBasicCredentials() throws IOException
    {
        System.setProperty("tangosol.coherence.ec2addressprovider.accesskey", "accessKey");
        System.setProperty("tangosol.coherence.ec2addressprovider.secretkey", "secretKey");

        EC2AddressProvider addressProvider = new EC2AddressProvider("dummy");
        AWSCredentials     credentials     = addressProvider.determineCredentials();

        assertTrue(credentials.getAWSAccessKeyId().equals("accessKey"));
        assertTrue(credentials.getAWSSecretKey().equals("secretKey"));
    }


    /**
     * Tests passing in credentials using system properties.
     * Expects a RuntimeException because there are no credentials.
     *
     * @throws IOException if reading a resource fails
     */
    @Test(expected = RuntimeException.class)
    public void testNoCredentials() throws IOException
    {
        System.setProperty("tangosol.coherence.ec2addressprovider.accesskey", "");
        System.setProperty("tangosol.coherence.ec2addressprovider.secretkey", "");

        EC2AddressProvider addressProvider = new EC2AddressProvider("dummy");

        addressProvider.determineCredentials();
    }


    /**
     * Tests passing in credentials using resource file.
     *
     * @throws IOException if reading a resource fails
     */
    @Test
    public void testResourceCredentials() throws IOException
    {
        System.setProperty("tangosol.coherence.ec2addressprovider.propertyfile",
                           "com/oracle/coherence/cloud/amazon/ec2credentials.properties");

        EC2AddressProvider addressProvider = new EC2AddressProvider("dummy");
        AWSCredentials     credentials     = addressProvider.determineCredentials();

        assertTrue(credentials.getAWSAccessKeyId().equals("properties-accesskey"));
        assertTrue(credentials.getAWSSecretKey().equals("properties-secretkey"));

    }
}
