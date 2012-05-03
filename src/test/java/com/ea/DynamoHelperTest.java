package com.ea;

import java.util.Random;

import junit.framework.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.amazonaws.services.dynamodb.model.TableStatus;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class DynamoHelperTest {
	private DynamoHelper helper;
	private static final Random random = new Random(System.currentTimeMillis());

	@BeforeMethod
	public void setup() {
		final Injector injector = Guice.createInjector(new AccessModule());
		this.helper = injector.getInstance(DynamoHelper.class);
	}

	@Test(enabled = false)
	public void createAndDeleteTable() {
		final String tableName = getRandomTableName();
		Assert.assertTrue(helper.createTable(tableName, "name"));
		Assert.assertTrue(helper.isTableInStatus(tableName, TableStatus.ACTIVE));
		Assert.assertTrue(helper.deleteTable(tableName));
	}

	@Test(enabled = false)
	public void deleteNonExistingTable() {
		Assert.assertFalse(helper.deleteTable(getRandomTableName()));
	}

	@Test(enabled = false)
	public void purgeTable() {
		final String tableName = getRandomTableName();
		Assert.assertTrue(helper.createTable(tableName, "name"));
		Assert.assertTrue(helper.purgeTable(tableName));
		Assert.assertTrue(helper.isTableInStatus(tableName, TableStatus.ACTIVE));

		helper.deleteTable(tableName);
	}

	@Test
	public void listTables() {
		Assert.assertNotNull(helper.listTables());
	}

	private String getRandomTableName() {
		return "tableName" + random.nextLong();
	}
}
