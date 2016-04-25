# 简介 #

这是beansdb的java客户端, 它使用了和python客户端一模一样的hash算法， 所以它和python客户端是完全兼容 — 这意味着你可以用python客户端存一个东西进beansdb， 然后用java客户端把它取出来。

This is the java client for beansdb(http://code.google.com/p/beansdb). because it use the exactly the same hash function as beansdb python client, so it is totally compatible with the python client  ----  which means you can save something into beansdb with the python client, then get it using the java client.

Usage Sample:
```

// specify the beansdb nodes.
Map<InetSocketAddress, Range> servers = new HashMap<InetSocketAddress, Range>();
servers.put(new InetSocketAddress("localhost", 7900), new Range(0, 16));
servers.put(new InetSocketAddress("localhost", 7901), new Range(0, 16));
servers.put(new InetSocketAddress("localhost", 7902), new Range(0, 16));

// 3,2,2 is the NRW number in the Dynamo thesis
Beansdb db = new Beansdb(servers, 16, 3, 2, 2);

// set the key: foo to value: bar
db.set("foo", "bar");

// get the value of foo
System.out.println(db.get("foo"));

// get the value for keys: hello, james, foo
List<String> keys = new ArrayList<String>(3);
keys.add("hello");
keys.add("james");
keys.add("foo");
Map<String, Object> ret = db.getMulti(keys);

for (String key : ret.keySet()) {
System.out.println(key + " : " + ret.get(key));
}

// delete the key: foo
db.delete("foo");

// close the db connection
db.close();
```

# 依赖包(Dependencies) #
  * [memcached client: spy memcached](http://code.google.com/p/beansdb4j/source/browse/trunk/lib/spymemcached-2.7.3.jar)
  * [log4j](http://code.google.com/p/beansdb4j/source/browse/trunk/lib/log4j-1.2.16.jar)

# 版本历史 (Release News) #
  * 2011-11-16 1.0版本发布
get, get\_multi, set, delete等api都已经实现

# Roadmap #
  * 实现Read-Repair
