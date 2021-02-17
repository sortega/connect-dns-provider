package org.refeed.kafka.config;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class DnsConfigProviderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ConfigProvider provider;

    @Before
    public void setUp() {
        provider = new DnsConfigProvider();
        provider.configure(Collections.emptyMap());
    }

    @After
    public void tearDown() throws Exception {
        provider.close();
    }

    @Test
    public void testResolvingARecord() {
        String dnsName = "_kafka._tcp.test.refeed.org";
        Map<String, String> data = provider.get("", Collections.singleton(dnsName)).data();
        assertEquals(Collections.singleton(dnsName), data.keySet());
        assertThat(data.get(dnsName),
                either(equalTo("k1.example.com:9092,k2.example.com:9092"))
                        .or(equalTo("k2.example.com:9092,k1.example.com:9092")));
    }

    @Test
    public void testResolvingAnInvalidName() {
        thrown.expect(ConfigException.class);
        thrown.expectMessage(containsString("Cannot resolve SRV record for not.found.example.com"));
        provider.get("", Collections.singleton("not.found.example.com"));
    }
}