package com.googlecode.beansdb4j;

public class WriteFailedException extends RuntimeException {
	private static final long serialVersionUID = 1312408966850766757L;

	public WriteFailedException(String msg) {
		super(msg);
	}
}
