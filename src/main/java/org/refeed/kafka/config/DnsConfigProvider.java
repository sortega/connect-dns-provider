package org.refeed.kafka.config;

import org.apache.kafka.common.config.ConfigData;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.provider.ConfigProvider;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.*;

/**
 * An implementation of <code>ConfigProvider</code> that retrieves SRV DNS records.
 *
 * <p>
 * How to configure in <code>connect-distributed.properties</code>:
 * </p>
 *
 * <pre>
 * config.providers=dns
 * config.providers.dns.class=org.refeed.kafka.config.DnsConfigProvider
 * </pre>
 *
 * <p>
 * How to use in a connector configuration:
 * </p>
 *
 * <pre>
 * "cassandra.contact.points": "${dns:_cassandra._tcp.cluster1.example.com}"
 * </pre>
 */
public class DnsConfigProvider implements ConfigProvider {
    private final DirContext resolver;

    public DnsConfigProvider() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        try {
            resolver = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new ConfigException("Cannot initialize DNS lookup", e);
        }
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ConfigData get(String unusedPath) {
        return new ConfigData(Collections.emptyMap());
    }

    @Override
    public ConfigData get(String unusedPath, Set<String> names) {
        Map<String,String> resolvedNames = new HashMap<>();
        for (String name: names) {
            resolvedNames.put(name, resolve(name));
        }
        return new ConfigData(resolvedNames);
    }

    private String resolve(String name) {
        final NamingEnumeration<?> records;
        try {
            records = resolver.getAttributes(name, new String[]{"SRV"}).get("srv").getAll();
        } catch (NamingException e) {
            throw new ConfigException("Cannot resolve SRV record for " + name, e);
        }
        List<String> servers = new LinkedList<>();
        while (records.hasMoreElements()) {
            String record = (String) records.nextElement();
            servers.add(parseSrvRecord(record));
        }
        return String.join(",", servers);
    }

    private String parseSrvRecord(String record) {
        String[] parts = record.split("\\s+");
        if (parts.length < 4) {
            throw new ConfigException("Malformed SRV record: '" + record +"'");
        }
        long port = Long.parseLong(parts[2]);
        String host = parts[3].substring(0, parts[3].length() - 1);
        return host + ":" + port;
    }

    @Override
    public void close() {
        try {
            resolver.close();
        } catch (NamingException e) {
            throw new ConfigException("Cannot close resolver", e);
        }
    }
}
