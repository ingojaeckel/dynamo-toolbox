package com.ea;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ScalarAttributeType;
import com.amazonaws.services.dynamodb.model.TableStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DynamoHelper {
	private static final long WAIT_TIME = 500;
	private static final long WAIT_TIMEOUT = 60 * 1000;
	private final AmazonDynamoDBAsyncClient aws;

	@Inject
	public DynamoHelper(final AmazonDynamoDBAsyncClient aws) {
		this.aws = aws;
	}

	public boolean createTable(final String tableName, final String hashKeyName) {
		final KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName(hashKeyName).withAttributeType(ScalarAttributeType.S);
		final ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput().withReadCapacityUnits(5l).withWriteCapacityUnits(5l);
		aws.createTable(new CreateTableRequest(tableName, new KeySchema(hashKeyElement)).withProvisionedThroughput(provisionedThroughput));

		return waitForTableToBeInStatus(tableName, TableStatus.ACTIVE);
	}

	public boolean isTableInStatus(final String tableName, final TableStatus desiredStatus) {
		final DescribeTableResult result = aws.describeTable(new DescribeTableRequest().withTableName(tableName));
		final TableStatus currentStatus = TableStatus.valueOf(result.getTable().getTableStatus());

		return currentStatus == desiredStatus;
	}

	public boolean deleteTable(final String table) {
		try {
			aws.deleteTable(new DeleteTableRequest(table));
			return waitForTableToBeDeleted(table);
		} catch (AmazonServiceException exception) {
			// Table did not exist
			return false;
		}
	}

	public boolean purgeTable(final String table) {
		try {
			final CreateTableRequest request = toCreateTableRequest(aws.describeTable(new DescribeTableRequest().withTableName(table)));

			if (deleteTable(table)) {
				aws.createTable(request);

				if (waitForTableToBeInStatus(table, TableStatus.ACTIVE)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (AmazonServiceException exception) {
			// Maybe the table was not found.
			return false;
		}
	}
	
	private CreateTableRequest toCreateTableRequest(final DescribeTableResult describeResult) {
		final long reads = describeResult.getTable().getProvisionedThroughput().getReadCapacityUnits();
		final long writes = describeResult.getTable().getProvisionedThroughput().getWriteCapacityUnits();
		final ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput().withReadCapacityUnits(reads).withWriteCapacityUnits(writes);
		final KeySchema keySchema = describeResult.getTable().getKeySchema();
		final String tableName = describeResult.getTable().getTableName();

		return new CreateTableRequest(tableName, keySchema).withProvisionedThroughput(provisionedThroughput);
	}

	private boolean waitForTableToBeInStatus(final String tableName, final TableStatus desiredStatus) {
		boolean isInDesiredStatus;
		long totalWaitTime = 0;

		do {
			totalWaitTime += sleep();
			isInDesiredStatus = isTableInStatus(tableName, desiredStatus);
		} while (totalWaitTime < WAIT_TIMEOUT && !isInDesiredStatus);

		return totalWaitTime < WAIT_TIMEOUT;
	}

	private boolean waitForTableToBeDeleted(final String tableName) {
		try {
			long totalWaitTime = 0;

			while (totalWaitTime < WAIT_TIMEOUT) {
				totalWaitTime += sleep();
				aws.describeTable(new DescribeTableRequest().withTableName(tableName));
			}

			return false;
		} catch (AmazonServiceException se) {
			// Table does not exist anymore
			return true;
		}
	}

	private long sleep() {
		try {
			Thread.sleep(WAIT_TIME);
		} catch (InterruptedException e) {
		}
		return WAIT_TIME;
	}
}
