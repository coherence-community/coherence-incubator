package com.oracle.coherence.patterns.eventdistribution.configuration;


import com.tangosol.internal.util.ObjectFormatter;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;


/**
 * CODI based Event Distribution configuration unit tests.
 *
 * @author Paul Mackin
 */
public class CodiEventDistributorTest
{
    @Test
    public void testDistributor()
    {
        String sXml = "<cache-config" +
                " xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                "    xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\"" +
                "" +
                "    xmlns:event=\"class://com.oracle.coherence.patterns.eventdistribution.configuration.EventDistributionNamespaceHandler\"" +
                "    xmlns:element=\"class://com.oracle.coherence.common.namespace.preprocessing.XmlPreprocessingNamespaceHandler\"" +
                "   xmlns:jndi=\"class://com.oracle.coherence.environment.extensible.namespaces.JndiNamespaceContentHandler\"" +
                "> " +
                " <defaults>" +
                "  <serializer>pof</serializer>" +
                " </defaults>" +
                "" +
                " <caching-scheme-mapping>" +
                "  <cache-mapping>" +
                "   <cache-name>publishing-*</cache-name>" +
                "   <scheme-name>distributed-scheme-with-publishing-cachestore</scheme-name>" +
                "" +
                "   <event:distributor>" +
                "    <event:distributor-name>{cache-name}</event:distributor-name>" +
                "    <event:distributor-external-name>{site-name}-{cluster-name}-{cache-name}</event:distributor-external-name>" +
                "" +
                "    <event:distributor-scheme>" +
                "      <event:coherence-based-distributor-scheme/>" +
                "    </event:distributor-scheme>" +
                "" +
                "    <event:distribution-channels>" +
                "     <event:distribution-channel>" +
                "      <event:channel-name>Remote Cluster Channel</event:channel-name>" +
                "      <event:starting-mode system-property=\"channel.starting.mode\">enabled</event:starting-mode>" +
                "" +
                "      <event:channel-scheme>" +
                "       <event:remote-cluster-channel-scheme>" +
                "        <event:remote-invocation-service-name>remote-site</event:remote-invocation-service-name>" +
                "        <event:remote-channel-scheme>" +
                "         <event:local-cache-channel-scheme>" +
                "          <event:target-cache-name>{cache-name}</event:target-cache-name>" +
                "          <event:conflict-resolver-scheme>" +
                "           <class-scheme>" +
                "            <class-name system-property=\"conflict.resolver.classname\">" +
                "             com.oracle.coherence.patterns.eventdistribution.channels.cache.BruteForceConflictResolver" +
                "            </class-name>" +
                "           </class-scheme>" +
                "          </event:conflict-resolver-scheme>" +
                "         </event:local-cache-channel-scheme>" +
                "        </event:remote-channel-scheme>" +
                "       </event:remote-cluster-channel-scheme>" +
                "      </event:channel-scheme>" +
                "     </event:distribution-channel>" +
                "    </event:distribution-channels>" +
                "   </event:distributor>" +
                "  </cache-mapping>" +
                " </caching-scheme-mapping>" +
                "" +
                " <caching-schemes>" +
                "  <remote-invocation-scheme>" +
                "   <service-name>remote-site</service-name>" +
                "   <initiator-config>" +
                "    <tcp-initiator>" +
                "     <remote-addresses>" +
                "      <socket-address>" +
                "       <address system-property=\"remote.host\">localhost</address>" +
                "       <port system-property=\"remote.port\" />" +
                "      </socket-address>" +
                "     </remote-addresses>" +
                "     <connect-timeout>2s</connect-timeout>" +
                "    </tcp-initiator>" +
                "    <outgoing-message-handler>" +
                "     <request-timeout>5s</request-timeout>" +
                "    </outgoing-message-handler>" +
                "   </initiator-config>" +
                "  </remote-invocation-scheme>" +
                "" +
                "  <proxy-scheme>" +
                "   <service-name>ExtendTcpProxyService</service-name>" +
                "   <acceptor-config>" +
                "    <tcp-acceptor>" +
                "     <local-address>" +
                "      <address system-property=\"proxy.address\">localhost</address>" +
                "      <port system-property=\"proxy.port\">10000</port>" +
                "     </local-address>" +
                "    </tcp-acceptor>" +
                "   </acceptor-config>" +
                "   <autostart>true</autostart>" +
                "  </proxy-scheme>" +
                " </caching-schemes>" +
                "</cache-config>";

        XmlElement xml = XmlHelper.loadXml(sXml);

        ExtensibleConfigurableCacheFactory.Dependencies deps =
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xml);

        EventDistributorTemplate template = deps.getResourceRegistry().getResource(EventDistributorTemplate.class);

        System.out.println(new ObjectFormatter().format("template", template));

    }

    @Test
    public void testParallel()
    {
        String sXml =  "<cache-config " +
                "        xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" " +
                "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "        xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd\" " +
                " " +
                "        xmlns:event=\"class://com.oracle.coherence.patterns.eventdistribution.configuration.EventDistributionNamespaceHandler\" " +
                "        xmlns:element=\"class://com.oracle.coherence.common.namespace.preprocessing.XmlPreprocessingNamespaceHandler\" " +
                " " +
                "> " +
                " <defaults> " +
                "  <serializer>pof</serializer> " +
                " </defaults> " +
                " " +
                " <caching-scheme-mapping> " +
                "  <cache-mapping> " +
                "   <cache-name>publishing-*</cache-name> " +
                "   <scheme-name>distributed-scheme-with-publishing-cachestore</scheme-name> " +
                " " +
                "            <init-params> " +
                "                <init-param> " +
                "                    <param-name>write-behind-delay</param-name> " +
                "                    <param-value system-property=\"write.behind.delay\">0</param-value> " +
                "                </init-param> " +
                "            </init-params> " +
                " " +
                "   <event:distributor> " +
                "    <event:distributor-name>{cache-name}</event:distributor-name> " +
                "    <event:distributor-external-name>{site-name}-{cluster-name}-{cache-name}</event:distributor-external-name> " +
                " " +
                " " +
                "    <event:distribution-channels> " +
                "     <event:distribution-channel> " +
                "      <event:channel-name>Remote Cluster Channel</event:channel-name> " +
                "      <event:starting-mode system-property=\"channel.starting.mode\">enabled</event:starting-mode> " +
                " " +
                "      <event:channel-scheme> " +
                "       <event:remote-cluster-channel-scheme> " +
                "        <event:remote-invocation-service-name>remote-site</event:remote-invocation-service-name> " +
                "        <event:remote-channel-scheme> " +
                "         <event:parallel-local-cache-channel-scheme> " +
                "                                        <event:target-cache-name>{cache-name}</event:target-cache-name> " +
                "                                        <event:conflict-resolver-scheme> " +
                "                                            <class-scheme> " +
                "                                                <class-name system-property=\"conflict.resolver.classname\"> " +
                "                                                    com.oracle.coherence.patterns.eventdistribution.channels.cache.BruteForceConflictResolver " +
                "                                                </class-name> " +
                "                                            </class-scheme> " +
                "                                        </event:conflict-resolver-scheme> " +
                "         </event:parallel-local-cache-channel-scheme>> " +
                "        </event:remote-channel-scheme> " +
                "       </event:remote-cluster-channel-scheme> " +
                "      </event:channel-scheme> " +
                "     </event:distribution-channel> " +
                "    </event:distribution-channels> " +
                "   </event:distributor> " +
                "  </cache-mapping> " +
                " </caching-scheme-mapping> " +
                " " +
                " <caching-schemes> " +
                "  <remote-invocation-scheme> " +
                "   <service-name>remote-site</service-name> " +
                "   <initiator-config> " +
                "    <tcp-initiator> " +
                "     <remote-addresses> " +
                "      <socket-address> " +
                "       <address system-property=\"remote.host\">localhost</address> " +
                "       <port system-property=\"remote.port\" /> " +
                "      </socket-address> " +
                "     </remote-addresses> " +
                "     <connect-timeout>2s</connect-timeout> " +
                "    </tcp-initiator> " +
                "    <outgoing-message-handler> " +
                "     <request-timeout>5s</request-timeout> " +
                "    </outgoing-message-handler> " +
                "   </initiator-config> " +
                "  </remote-invocation-scheme> " +
                " " +
                "  <proxy-scheme> " +
                "   <service-name>ExtendTcpProxyService</service-name> " +
                "   <acceptor-config> " +
                "    <tcp-acceptor> " +
                "     <local-address> " +
                "      <address system-property=\"proxy.address\">localhost</address> " +
                "      <port system-property=\"proxy.port\">10000</port> " +
                "     </local-address> " +
                "    </tcp-acceptor> " +
                "   </acceptor-config> " +
                "   <autostart>true</autostart> " +
                "  </proxy-scheme> " +
                " </caching-schemes> " +
                "</cache-config>";

        XmlElement xml = XmlHelper.loadXml(sXml);

        ExtensibleConfigurableCacheFactory.Dependencies deps =
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xml);

        EventDistributorTemplate template = deps.getResourceRegistry().getResource(EventDistributorTemplate.class);

        System.out.println(new ObjectFormatter().format("template", template));
    }

}
