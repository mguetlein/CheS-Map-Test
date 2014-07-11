package test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import util.ArrayUtil;
import util.TimeFormatUtil;

public class TestLauncher
{
	public static enum MappingTest
	{
		single, wizard, cache, all, debug
	}

	public static MappingTest MAPPING_TEST;// = MappingTest.all_gui;

	public static void main(String[] args)
	{
		if (args == null || args.length != 2)
		{
			System.err
					.println("first param: gui|mapping, second param for gui: wizard|viewer|both, second param for mapping: "
							+ ArrayUtil.toString(MappingTest.values(), "|", "", "", ""));
			System.exit(1);
		}
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		Result result = null;
		if (args[0].equals("gui"))
		{
			if (args[1].equals("both"))
				result = junit.run(WizardTest.class, ViewerTest.class);
			else if (args[1].equals("wizard"))
				result = junit.run(WizardTest.class);
			else if (args[1].equals("viewer"))
				result = junit.run(ViewerTest.class);
		}
		else if (args[0].equals("mapping"))
		{
			MAPPING_TEST = MappingTest.valueOf(args[1]);
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
