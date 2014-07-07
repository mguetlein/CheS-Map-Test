package test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import util.TimeFormatUtil;

public class TestLauncher
{
	public static enum MappingTest
	{
		all, single, debug
	}

	public static MappingTest MAPPING_TEST = MappingTest.debug;

	public static void main(String[] args)
	{
		String cmd = "";
		if (args != null && args.length >= 1)
			cmd = args[0];
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		Result result;
		if (cmd.equals("gui"))
			result = junit.run(WizardTest.class, ViewerTest.class);
		else if (cmd.equals("wizard"))
			result = junit.run(WizardTest.class);
		else if (cmd.equals("viewer"))
			result = junit.run(ViewerTest.class);
		else
		{
			if (cmd.equals("short") || cmd.equals("single"))
				MAPPING_TEST = MappingTest.single;
			else if (cmd.equals("debug"))
				MAPPING_TEST = MappingTest.debug;
			else
				MAPPING_TEST = MappingTest.all;
			result = junit.run(MappingAndExportTest.class);
		}
		System.out.println("");
		System.out.println("Number of test failures: " + result.getFailureCount() + ", Runtime: "
				+ TimeFormatUtil.format(result.getRunTime()));
		for (Failure failure : result.getFailures())
		{
			System.out.println(failure.toString());
		}
		System.exit(0);
	}
}
