package com.ea;

import java.io.Console;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodb.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class DynamoConsole {
	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(new AccessModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(Console.class).toInstance(System.console());
			}
		});

		final DynamoConsole consoleTest = injector.getInstance(DynamoConsole.class);
		consoleTest.start();
	}
	
	private final Console console;
	private final DynamoHelper helper;
	private final AmazonDynamoDBAsyncClient aws;

	@Inject
	public DynamoConsole(final AmazonDynamoDBAsyncClient aws, final Console console, final DynamoHelper helper) {
		this.aws = aws;
		this.helper = helper;
		this.console = console;
	}
	
	public void start() {
		printHelp();
		
		while (eatCommand()) {
		}
		console.printf("Bye, bye.\n");
	}

	private boolean eatCommand() {
		final String commandStr = console.readLine("> ");
		final String[] args = commandStr.split("\\s+");

		final String command = args[0]; // assume "g" for "get"

		if (command.isEmpty() || command.equals("q")) {
			return false;
		} else if ("s".equals(command) && args.length == 2) {
			handleScan(args[1]);
		} else if ("p".equals(command) && args.length == 2) {
			handlePurge(args[1]);
		} else if ("g".equals(command) && args.length == 3) {
			handleGet(args);
		} else { // } else if ("h".equals(command)) {
			printHelp();
		}

		return true;
	}

	private void handleScan(final String tableName) {
		loading();
		
		final ScanResult result = aws.scan(new ScanRequest(tableName).withLimit(10));
		final List<Map<String, AttributeValue>> items = result.getItems();
		
		for (final Map<String, AttributeValue> item: items) {
			console.printf("item: '%s'\n", item.toString());
		}
		
		done();
	}

	private void handlePurge(final String tableName) {
		if (confirm("This will delete and re-create '%s'.\nMake sure do have a backup of all your data!\nAre you sure? [yN] ", tableName)) {
			if (helper.purgeTable(tableName)) {
				done();
			} else {
				error();
			}			
		} else {
			status("Cancelled by user.\n");
		}
	}
	
	private void done() {
		console.printf("Done.\n");
	}

	private boolean confirm(final String formatString, final Object...arguments) {
		return "y".equals(console.readLine(formatString, arguments));
	}

	private void error() {
		console.printf("An error occurred\n");
	}

	private void status(final String message) {
		console.printf(message);
	}

	private void handleGet(final String[] args) {
		final String table = args[1];
		final String id = args[2];

		loading();
		final GetItemResult result = aws.getItem(new GetItemRequest(table, new Key(new AttributeValue(id))));
		final Map<String, AttributeValue> item = result.getItem();

		if (item == null) {
			console.printf("Not found.\n");
		} else {
			console.printf("item: '%s'\n", item.toString());
		}
	}

	private void loading() {
		console.printf("Loading..\n");
	}

	private void printHelp() {
		console.printf("h           show help\n");
		console.printf("q           quit\n");
		console.printf("p TABLE     purge; drop all data of the table, re-creates the table\n");
		console.printf("s TABLE     scan; show the first 10 items of the table\n");
		console.printf("g TABLE ID  get item from table\n");
		console.printf("\n");
	}
}