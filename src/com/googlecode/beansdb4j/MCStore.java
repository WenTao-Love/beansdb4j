package com.googlecode.beansdb4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.MemcachedClient;

public class MCStore {
    private MemcachedClient mc;
    private String host;
    private int port;

    public MCStore(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.mc = new MemcachedClient(new InetSocketAddress(host, port));
    }

    public MCStore(ServerItem server) throws IOException {
        this(server.getHost(), server.getPort());
    }

    public Object get(String key) {
        return this.mc.get(key); 
    } 

    public boolean set(String key, Object value) {
        try {
			return this.mc.set(key, 0, value).get();
		} catch (InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			return false;
		}
    }
    
    public boolean delete(String key) {
        try {
			return this.mc.delete(key).get();
		} catch (InterruptedException e) {
			return false;
		} catch (ExecutionException e) {
			return false;
		}
    }
    
    public Map<String, Object> getMulti(Collection<String> keys) {
    	return this.mc.getBulk(keys);
    }
    
    public void close() {
    	this.mc.shutdown();
    }

	public MemcachedClient getMc() {
		return mc;
	}

	public void setMc(MemcachedClient mc) {
		this.mc = mc;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean equals(Object server) {
		if (server == null || !(server instanceof MCStore)) {
			return false;
		}
		
		MCStore mcServer = (MCStore)server;
		
		return this.host.equals(mcServer.getHost()) && this.port == mcServer.getPort();
	}
	
	public int hashCode() {
		return host.hashCode() * port;
	}
}
    
