package com.googlecode.beansdb4j;

import java.io.IOException;

public class StartupException extends RuntimeException {
	private static final long serialVersionUID = 2465345734817060231L;

	public StartupException(IOException e) {
		super(e);
	}
	
}
