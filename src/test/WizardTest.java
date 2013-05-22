package test;

import gui.AlignWizardPanel;
import gui.Build3DWizardPanel;
import gui.CheSMapperWizard;
import gui.ClusterWizardPanel;
import gui.DatasetWizardPanel;
import gui.EmbedWizardPanel;
import gui.FeatureWizardPanel;
import gui.LaunchCheSMapper;
import gui.Selector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import main.Settings;

import org.junit.Assert;
import org.junit.Test;

import util.SwingTestUtil;
import util.ThreadUtil;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.embed3d.ThreeDEmbedder;
import dataInterface.MoleculePropertySet;

public class WizardTest
{
	public static String DATA_DIR = "data/";

	static CheSMapperWizard wizard;
	static JButton nextButton;
	static JButton prevButton;
	static JButton closeButton;
	static JButton startButton;

	public WizardTest()
	{
		if (wizard == null)
		{
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					LaunchCheSMapper.main(new String[] { "default", "no-properties" });
				}
			});
			th.start();
			while (Settings.TOP_LEVEL_FRAME == null || !Settings.TOP_LEVEL_FRAME.isVisible())
				ThreadUtil.sleep(50);
			ThreadUtil.sleep(250);
			wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			nextButton = SwingTestUtil.getButton(wizard, "Next");
			prevButton = SwingTestUtil.getButton(wizard, "Previous");
			closeButton = SwingTestUtil.getButton(wizard, "Close");
			startButton = SwingTestUtil.getButton(wizard, "Start mapping");
			Assert.assertTrue(closeButton.isEnabled());
			Assert.assertFalse(startButton.isEnabled());
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
		Assert.assertFalse(wizard.isBlocked());
		Assert.assertTrue(buttonLoad.isEnabled());
		Assert.assertFalse(nextButton.isEnabled());
		buttonLoad.doClick();
		Assert.assertTrue(wizard.isBlocked());
		SwingTestUtil.assertErrorDialog(wizard, "ERROR - Loading dataset file", "not found");
		Assert.assertFalse(wizard.isBlocked());

		textField.setText(DATA_DIR + "broken_smiles.csv");
		Assert.assertFalse(wizard.isBlocked());
		Assert.assertTrue(buttonLoad.isEnabled());
		Assert.assertFalse(nextButton.isEnabled());
		buttonLoad.doClick();
		Assert.assertTrue(wizard.isBlocked());
		while (SwingTestUtil.getOnlyVisibleDialog(wizard) == null
				|| SwingTestUtil.getOnlyVisibleDialog(wizard).getTitle().equals("Loading dataset file"))
			ThreadUtil.sleep(50);
		SwingTestUtil.assertErrorDialog(wizard, "ERROR - Loading dataset file", "illegal smiles");
		Assert.assertFalse(wizard.isBlocked());

		textField.setText(DATA_DIR + "basicTestSet.sdf");
		Assert.assertFalse(wizard.isBlocked());
		buttonLoad.doClick();
		Assert.assertTrue(wizard.isBlocked());
		while (wizard.isBlocked())
			ThreadUtil.sleep(50);
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
	public void testExtractFeaturesPanel()
	{
		FeatureWizardPanel panel = (FeatureWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		@SuppressWarnings("unchecked")
		Selector<MoleculePropertySet> selector = (Selector<MoleculePropertySet>) SwingTestUtil.getOnlySelector(panel);
		MoleculePropertySet set[] = selector.getSelected();
		Assert.assertTrue(set.length == 0);

		noFeatures();

		selector.setCategorySelected("Included in the Dataset", true);
		set = selector.getSelected();
		Assert.assertTrue(set.length == 5);

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof ClusterWizardPanel);
	}

	private void noFeatures()
	{
		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof ClusterWizardPanel);
		ClusterWizardPanel panel = (ClusterWizardPanel) wizard.getCurrentPanel();

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "No");
		Assert.assertTrue(radio.isShowing());
		Assert.assertFalse(radio.isSelected());
		Assert.assertFalse(nextButton.isEnabled());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof EmbedWizardPanel);
		EmbedWizardPanel panel2 = (EmbedWizardPanel) wizard.getCurrentPanel();
		radio = SwingTestUtil.getRadioButton(panel2, "No");
		Assert.assertTrue(radio.isShowing());
		Assert.assertFalse(radio.isSelected());
		Assert.assertFalse(nextButton.isEnabled());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());
		Assert.assertTrue(startButton.isEnabled());

		prevButton.doClick();
		prevButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof FeatureWizardPanel);
	}

	@Test
	public void testClusterPanel()
	{
		ClusterWizardPanel panel = (ClusterWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JButton toggleButton = SwingTestUtil.getButton(panel, "Advanced >>");

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "Yes (recommended)");
		Assert.assertTrue(radio.isShowing());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("<< Simple"));
		Assert.assertFalse(radio.isShowing());
		JList list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), DatasetClusterer.CLUSTERERS[i]);

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("Advanced >>"));
		Assert.assertTrue(radio.isShowing());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof EmbedWizardPanel);
	}

	@Test
	public void testEmbedPanel()
	{
		EmbedWizardPanel panel = (EmbedWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JButton toggleButton = SwingTestUtil.getButton(panel, "Advanced >>");

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "Yes (recommended, applies 'PCA 3D Embedder (WEKA)')");
		Assert.assertTrue(radio.isShowing());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("<< Simple"));
		Assert.assertFalse(radio.isShowing());
		JList list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDEmbedder.EMBEDDERS[i]);

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("Advanced >>"));
		Assert.assertTrue(radio.isShowing());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof AlignWizardPanel);
	}

	@Test
	public void testAlignPanel()
	{
		AlignWizardPanel panel = (AlignWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertFalse(nextButton.isEnabled());

		JList list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDAligner.ALIGNER[i]);
	}

	@Test
	public void testCloseWizard() throws Exception
	{
		closeButton.doClick();
		Assert.assertFalse(wizard.isVisible());
	}
}
