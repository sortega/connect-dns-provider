# DNS resolution for Kafka Connect configurations

This is a small library to leverage DNS records of type SRV to avoid
hardcoding lists of servers in Kafka Connect connector configurations

## What are SRV DNS records?

DNS most well known record types are A and CNAME, both of which allow us to
resolve names like `example.com` into IPs like `93.184.216.34`. However,
there are many more types of records like MX for managing email and SRV for
service resolution.

SRV records allow you to list which hostnames and ports are offering some
service. This is specially handy to find several nodes of a cluster. For
example Cassandra contact points, Kafka brokers or Zookeeper ensemble nodes.

This is an example for a small Kafka cluster that you might define for your
private DNS zone:

```
_kafka._tcp.default.svc.cluster.local.   86400 IN SRV 0 0 9093 broker-a4f3.default.svc.cluster.local.
_kafka._tcp.default.svc.cluster.local.   86400 IN SRV 0 0 9093 broker-bf33.default.svc.cluster.local.
_kafka._tcp.default.svc.cluster.local.   86400 IN SRV 0 0 9093 broker-z98a.default.svc.cluster.local.
```

You can check SRV records with `dig`:

```bash
> dig _kafka._tcp.default.svc.cluster.local srv +short
0 0 9093 broker-a4f3.default.svc.cluster.local.
0 0 9093 broker-bf33.default.svc.cluster.local.
0 0 9093 broker-z98a.default.svc.cluster.local.
```

## How to use

1. Add this library to Kafka connect classpath. TODO: how?
   
2. Configure `connect-distributed.properties`:

   ```properties
   config.providers=dns
   config.providers.dns.class=org.refeed.kafka.config.DnsConfigProvider
   ```
3. Use `${dns:}` expressions in connector configurations. For example:

   ```
   "cassandra.contact.points": "${dns:_cassandra._tcp.cluster1.example.com}"
   ```
   
## Testing

TODO