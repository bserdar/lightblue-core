package com.redhat.lightblue.metadata.types;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObjectTypeTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testType() {
		ObjectType objectType = ObjectType.TYPE;
		assertNotNull(objectType);
	}

}
