package test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import util.TimeFormatUtil;

public class TestLauncher
{
	public static boolean SHORT_TEST = false;

	public static void main(String[] args)
	{
		if (args != null && args.length > 1 && args[0].equals("short"))
			SHORT_TEST = true;
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		Result result = junit.run(MappingAndExportTest.class);
		System.out.println("");
		System.out.println("Number of test failures: " + result.getFailureCount() + ", Runtime: "
				+ TimeFormatUtil.format(result.getRunTime()));
		for (Failure failure : result.getFailures())
		{
			System.out.println(failure.toString());
		}
	}
}
