package test;

import gui.Build3DWizardPanel;
import gui.CheSMapperWizard;
import gui.CheSViewer;
import gui.DatasetWizardPanel;
import gui.FeatureWizardPanel;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextField;

import main.Settings;

import org.junit.Assert;
import org.junit.Test;

import util.SwingTestUtil;
import util.ThreadUtil;
import alg.build3d.ThreeDBuilder;

public class CheSMapperWizardTest
{
	public static String DATA_DIR = "data/";

	static CheSMapperWizard wizard;
	static JButton nextButton;
	static JButton prevButton;
	static JButton closeButton;

	public CheSMapperWizardTest()
	{
		if (wizard == null)
		{
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					CheSViewer.main(new String[] { "default", "no-properties" });
				}
			});
			th.start();
			while (Settings.TOP_LEVEL_FRAME == null || !Settings.TOP_LEVEL_FRAME.isVisible())
				ThreadUtil.sleep(200);
			ThreadUtil.sleep(250);
			wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			nextButton = SwingTestUtil.getButton(wizard, "Next");
			prevButton = SwingTestUtil.getButton(wizard, "Previous");
			closeButton = SwingTestUtil.getButton(wizard, "Close");
			Assert.assertTrue(wizard.getCurrentPanel() instanceof DatasetWizardPanel);
		}
	}

	@Test
	public void testDatasetPanel()
	{
		DatasetWizardPanel panel = (DatasetWizardPanel) wizard.getCurrentPanel();
		JTextField textField = SwingTestUtil.getOnlyTextField(panel);
		Assert.assertTrue(textField.getText().length() == 0);
		JButton buttonLoad = SwingTestUtil.getButton(panel, "Load Dataset");
		Assert.assertFalse(buttonLoad.isEnabled());

		textField.setText("jklsfdjklajklsfdauioes");
		Assert.assertFalse(panel.isLoading());
		Assert.assertTrue(buttonLoad.isEnabled());
		Assert.assertFalse(nextButton.isEnabled());

		buttonLoad.doClick();
		SwingTestUtil.assertErrorDialog(wizard, "Dataset could not be loaded");

		textField.setText(DATA_DIR + "basicTestSet.sdf");
		Assert.assertFalse(panel.isLoading());
		buttonLoad.doClick();
		Assert.assertTrue(panel.isLoading());
		while (panel.isLoading())
			ThreadUtil.sleep(200);
		Assert.assertFalse(buttonLoad.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof Build3DWizardPanel);
	}

	@Test
	public void testCreate3DPanel()
	{
		Build3DWizardPanel panel = (Build3DWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JList list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDBuilder.BUILDERS[i]);

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof FeatureWizardPanel);
	}

	@Test
	public void testCloseWizard() throws Exception
	{
		closeButton.doClick();
		Assert.assertFalse(wizard.isVisible());
	}
}
