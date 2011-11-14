package com.googlecode.beansdb4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Beansdb client class. for the sample usage, see the {@link #main(String[])} method.
 * 
 * @author james
 *
 */
public class Beansdb {
	private static final Logger log = Logger.getLogger(Beansdb.class);
	
    /**
     *  beansdb's value space.
     */
    private static final long HASH_SPACE = (long)Math.pow(2, 32);
    /**
     * total bucket count.
     */
    private int bucketCount;
    /**
     * bucket size.
     */
    private long bucketSize;
    /**
     * replication node count.
     */
    private int n;
    /**
     * the least write-successful node count.
     */
    private int w;
    /**
     * the least read-successful node count.
     */
    private int r;

    /**
     * bucket-index -> memcachedclients
     */
    private Map<Integer, List<MCStore>> buckets;

    /**
     * host:port -> memcachedclient
     */
    private Map<InetSocketAddress, MCStore> servers;
    /**
     * host:port -> bucket-index-range
     */
    private Map<InetSocketAddress, Range> serverBuckets; 

    public Beansdb(Map<InetSocketAddress, Range> servers, int bucketCount, int n, int w, int r) {
        this.bucketCount = bucketCount; 
        this.n = n;
        this.w = w;
        this.r = r;

        this.bucketSize = (int)(HASH_SPACE / this.bucketCount);
        this.serverBuckets = new HashMap<InetSocketAddress, Range>(servers.size());
        this.buckets = new HashMap<Integer, List<MCStore>>(this.bucketCount);
        this.servers = new HashMap<InetSocketAddress, MCStore>(servers.size());
        
        try {
	        for(InetSocketAddress server : servers.keySet()) {
	        	Range range = servers.get(server);
	        	
	            this.serverBuckets.put(server, range); 
	            MCStore store = new MCStore(server);
	            this.servers.put(server, store);
	            
	            for (int i = range.getStart(); i < range.getEnd(); i++) {
	            	if (!this.buckets.containsKey(i)) {
	            		this.buckets.put(i, new ArrayList<MCStore>());
	            	}
	            	
	            	this.buckets.get(i).add(store);
	            }
	        }
        } catch(IOException e) {
        	log.error("beansdb startup failed: " + e.getMessage());
        	throw new StartupException(e);
        }
        
        if (log.isDebugEnabled()) {
        	log.debug("HASH_SPACE:\t" + HASH_SPACE 
        			+ "\nbucketCount: " + this.bucketCount 
        			+ "\nN: " + this.n 
        			+ "\nW: " + this.w
        			+ "\nR: " + this.r 
        			+ "\nbucketSize:" + this.bucketSize);
        }
    }

    /**
     * Default NWR is 311 which is very efficient for read, write, but have weak consistency.
     * 
     * @param servers
     * @param bucketCount
     */
    public Beansdb(Map<InetSocketAddress, Range> servers, int bucketCount) {
        this(servers, bucketCount, 3, 1, 1);
    }

    /**
     * Default bucket count is 16.
     * 
     * @param servers
     */
    public Beansdb(Map<InetSocketAddress, Range> servers) {
        this(servers, 16);
    }


    /**
     * Gets the value for the specified key.
     * 
     * @param key
     * @return
     */
    public Object get(String key) {
    	List<MCStore> servers = this.getServers(key);
    	
    	Object ret = null;
    	int successCount = 0;
    	for (MCStore server : servers) {
    		Object ret1 = server.get(key);
    		
    		if (log.isDebugEnabled()) {
    			log.debug("GET [" + key + "] FROM [" + server.getHost() + ":" + server.getPort() + "] [" + ret1 + "]");
    		}
    		
			if (ret1 != null) {
				ret = ret1;
				successCount += 1;
				
				if (successCount >= this.r) {
					break;
				}
			}
    		
    	}
    	
    	if (successCount > 0 && successCount < this.r) {
    		throw new ReadFailedException("not enough success read count[" + successCount + "], R=" + this.r);
    	}
    	
    	return ret;
    }    

    // TODO
    public Map<String, Object> getMulti(Collection<String> keys) {
    	// accumulate the servers for each key.
    	Map<MCStore, List<String>> serverList = new HashMap<MCStore, List<String>>();
    	for (String key : keys) {
    		List<MCStore> servers = this.getServers(key);
    		
    		for (MCStore server : servers) {
    			if (!serverList.containsKey(server)) {
    				serverList.put(server, new ArrayList<String>());
    			}
    			
    			serverList.get(server).add(key);
    		}
    	}
    	
    	// get multiple keys from each server
    	Map<String, Object> ret = new HashMap<String, Object>();
    	Map<String, Integer> successCounts = new HashMap<String, Integer>();
    	for (MCStore server : serverList.keySet()) {
    		Map<String, Object> ret1 = server.getMulti(serverList.get(server));
    		
    		if (log.isDebugEnabled()) {
    			StringBuffer keyvalue = new StringBuffer();
    	    	for (String key : serverList.get(server)) {
    	    		keyvalue.append(key).append(",");
    	    	}
    	    	log.debug("GET [" + keyvalue.toString() + "] FROM [" + server.getHost() + ":" + server.getPort() + "]");
    	    	
    			keyvalue = new StringBuffer();
    	    	for (String key : ret1.keySet()) {
    	    		keyvalue.append(key).append(":").append(ret1.get(key)).append(",");
    	    	}
    	    	
    			log.debug("GOT {" + keyvalue.toString() + "} FROM [" + server.getHost() + ":" + server.getPort() + "]");
    		}
    		
    		// calculate the success read count for the keys.
    		for (String key : ret1.keySet()) {
    			if (!successCounts.containsKey(key)) {
    				successCounts.put(key, 0);
    			}
    			
    			successCounts.put(key, successCounts.get(key) + 1);
    		}
    		
    		ret.putAll(ret1);
    	}
    	
    	// delete the keys from the return result if not enough success
    	// read return back.
    	for (String key : successCounts.keySet()) {
    		if (successCounts.get(key) < this.r) {
    			ret.remove(key);
    		}
    	}
    	
    	return ret;
    } 
    
    public void printBuckets() {
    	for (int i = 0; i < this.buckets.size(); i++) {
    		List<MCStore> servers = this.buckets.get(i);
    		log.info("[" + i + "]");
    		
    		for (MCStore server : servers) {
    			log.info("\t" + server.getHost() + ":" + server.getPort());
    		}
    	}
		
		for (InetSocketAddress server : this.serverBuckets.keySet()) {
			log.info("\t" + server.getHostName() + ":" + server.getPort() + " " + this.serverBuckets.get(server).length());
		}
    }
    
    public void set(String key, String value) {
		List<MCStore> servers = this.getServers(key);

		int successCount = 0;
		for (MCStore server : servers) {
			boolean ret = server.set(key, value);
			
    		if (log.isDebugEnabled()) {
    			log.debug("SET [" + key + "] TO [" + server.getHost() + ":" + server.getPort() + "] [" + ret + "]");
    		}
    		
			if (ret) {
				successCount += 1;
				
				if (successCount >= this.w) {
					break;
				}
			}
		}

		// make sure at least dataservers on this.W nodes are deleted.
		if (successCount < this.w) {
			if (!value.equals(this.get(key))) {
				throw new WriteFailedException("not enough success write count[" + successCount + "], W=" + this.w);
			}
		}
    }

    /**
     * Delete the specified key.
     * 
     * @param key
     * @return
     */
    public boolean delete(String key) {
    	List<MCStore> servers = this.getServers(key); 
    	
    	int successCount = 0;
        for (MCStore server : servers) {
    		boolean ret = server.delete(key);
        	successCount +=  ret ? 1 : 0;
        	
    		if (log.isDebugEnabled()) {
    			log.debug("DELETE [" + key + "] FROM [" + server.getHost() + ":" + server.getPort() + "] [" + ret + "]");
    		}
        }
        
        // make sure at least data on this.W nodes are deleted.
        return successCount >= this.w;
    }

    /**
     * Close all the beansdb clients.
     */
    public void close() {
    	for (MCStore server : this.servers.values()) {
    		server.close();
    	}
    }
    
    /**
     * Get the fnv1a hash for the specified key.
     * 
     * PS: The fnv1a algorithm here is exact the same algorithm as beansdb's python fnv1a method, so it will
     *     generate the same hash as the python version  -- which means if you store
     *     something in beansdb using python client, you can get it using the java client, 
     *     vice versa.
     * 
     * @param key
     * @return
     */
    private long fnv1a(String key) {
    	long prime = 0x01000193L;
    	long h = 0x811c9dc5L;
    	
    	for (int i = 0; i < key.length(); i++) {
    	        h = h ^ key.charAt(i);
    	        h = (h * prime) & 0xffffffffL;
    	}
    	
    	return h;
    }
    
    /**
     * Gets the servers which stores the specified key.
     * 
     * @param key
     * @return
     */
    private List<MCStore> getServers(String key) {
    	long hash = this.fnv1a(key);
        int bucketIndex = (int)(hash / this.bucketSize);

        if (log.isDebugEnabled()) {
        	log.debug("key:" + key + ", hash: " + hash + ", bucketSize: " + this.bucketSize + ", bucketIndex: " + bucketIndex);
        }
        return this.buckets.get(bucketIndex);
    }
    
    public static void main(String[] args) {
    	Map<InetSocketAddress, Range> servers = new HashMap<InetSocketAddress, Range>();
    	servers.put(new InetSocketAddress("192.168.204.11", 7900), new Range(0, 16));
    	servers.put(new InetSocketAddress("192.168.204.11", 7901), new Range(0, 16));
    	servers.put(new InetSocketAddress("192.168.204.11", 7902), new Range(0, 16));
    	Beansdb db = new Beansdb(servers, 16, 3, 2, 2);
    	
    	db.set("foo", "bar");
    	System.out.println(db.get("foo"));
    	System.out.println(db.get("foo"));
    	
    	List<String> keys = new ArrayList<String>(3);
    	keys.add("hello");
    	keys.add("james");
    	keys.add("foo");
    	Map<String, Object> ret = db.getMulti(keys);
    	
    	for (String key : ret.keySet()) {
    		System.out.println(key + " : " + ret.get(key));
    	}
    	
    	db.delete("foo");
    	db.close();
    }
}
