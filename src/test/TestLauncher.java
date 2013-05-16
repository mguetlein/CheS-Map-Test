package test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestLauncher
{
	public static void main(String[] args)
	{
		Result result = JUnitCore.runClasses(MappingTest.class);
		for (Failure failure : result.getFailures())
		{
			System.out.println(failure.toString());
		}
	}
}
