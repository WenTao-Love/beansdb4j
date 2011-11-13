package com.googlecode.beansdb4j;

public class ServerItem {
    private String host;
    private int port;

    public ServerItem(String host, int port) {
        this.host = host;
        this.port = port;
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
		if (server == null || !(server instanceof ServerItem)) {
			return false;
		}
		
		ServerItem serverItem = (ServerItem)server;
		
		return this.host.equals(serverItem.getHost()) && this.port == serverItem.getPort();
	}
	
	public int hashCode() {
		return host.hashCode() * port;
	}
}
