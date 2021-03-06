/*
 * Copyright (C) 2015-2017 NS Solutions Corporation, All Rights Reserved.
 */
package com.htmlhifive.pitalium.explorer.response;

import org.junit.Assert;
import org.junit.Test;

import com.htmlhifive.pitalium.explorer.entity.TestExecution;
import com.htmlhifive.pitalium.explorer.response.TestExecutionResult;

public class TestExecutionResultTest {
	@Test
	public void testGetterSetter()
	{
		TestExecution te = new TestExecution();
		TestExecution te2 = new TestExecution();
		TestExecutionResult t = new TestExecutionResult(te, 17L, 42L);
		t.setTestExecution(te2);
		Assert.assertEquals(te2, t.getTestExecution());

		Assert.assertEquals(17, t.getPassedCount().intValue());
		t.setPassedCount(42);
		Assert.assertEquals(42, t.getPassedCount().intValue());

		Assert.assertEquals(42, t.getTotalCount().intValue());
		t.setTotalCount(52);
		Assert.assertEquals(52, t.getTotalCount().intValue());
	}
}
