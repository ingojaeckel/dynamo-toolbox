package com.ea;

import java.io.File;
import java.io.IOException;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBAsyncClient;
import com.google.inject.AbstractModule;

public class AccessModule extends AbstractModule {
	@Override
	protected void configure() {
		try {
			bind(AmazonDynamoDBAsyncClient.class).toInstance(new AmazonDynamoDBAsyncClient(new PropertiesCredentials(new File("aws.properties"))));
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
