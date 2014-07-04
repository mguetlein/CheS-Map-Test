package test;

import gui.BlockableFrame;
import gui.LaunchCheSMapper;
import gui.swing.ComponentFactory.ClickableLabel;

import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import main.CheSMapping;
import main.Settings;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import property.PropertySetProvider;
import util.ListUtil;
import util.ScreenUtil;
import util.SwingTestUtil;
import util.ThreadUtil;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import alg.cluster.WekaClusterer;
import alg.embed3d.WekaPCA3DEmbedder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ViewerTest
{
	static BlockableFrame viewer;
	public static String DATA_DIR = "data/";
	static Random random = new Random();

	static String dataset = "basicTestSet.sdf";
	int numClusters = 8;
	int numCompounds = 16;
	int numCompoundsInClusters[] = { 2, 3, 2, 5, 1, 1, 1, 1 };

	static
	{
		if (viewer == null)
		{
			LaunchCheSMapper.init();
			LaunchCheSMapper.setExitOnClose(false);

			Properties props = MappingWorkflow.createMappingWorkflow(DATA_DIR + dataset, new DescriptorSelection(
					PropertySetProvider.PropertySetShortcut.integrated, "logD,rgyr,HCPSA,fROTB", null, null, null),
					WekaClusterer.WEKA_CLUSTERER[0], WekaPCA3DEmbedder.INSTANCE);
			CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
			mapping.getDatasetFile();
			mapping.doMapping();
			LaunchCheSMapper.start(mapping);

			//			Thread th = new Thread(new Runnable()
			//			{
			//				public void run()
			//				{
			//					LaunchCheSMapper.main(new String[] { "--no-properties" });
			//				}
			//			});
			//			th.start();
			//			while (SwingTestUtil.getOnlyVisibleFrame() == null)
			//				ThreadUtil.sleep(50);
			//			SwingTestUtil.waitForGUI(250);
			//			Assert.assertNotNull(Settings.TOP_LEVEL_FRAME);
			//			Assert.assertTrue(Settings.TOP_LEVEL_FRAME.isVisible());
			//			CheSMapperWizard wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			//			JButton nextButton = SwingTestUtil.getButton(wizard, "Next");
			//			Assert.assertTrue(wizard.getCurrentPanel() instanceof DatasetWizardPanel);
			//			DatasetWizardPanel panel = (DatasetWizardPanel) wizard.getCurrentPanel();
			//			JTextField textField = SwingTestUtil.getOnlyTextField(panel);
			//			JButton buttonLoad = SwingTestUtil.getButton(panel, "Load Dataset");
			//			textField.setText(DATA_DIR + dataset);
			//			buttonLoad.doClick();
			//			while (wizard.isBlocked())
			//			{
			//				ThreadUtil.sleep(250);
			//				System.out.println("waiting for panel to stop loading");
			//			}
			//			nextButton.doClick();
			//			nextButton.doClick();
			//			FeatureWizardPanel panel2 = (FeatureWizardPanel) wizard.getCurrentPanel();
			//			@SuppressWarnings("unchecked")
			//			Selector<PropertySetCategory, CompoundPropertySet> selector = (Selector<PropertySetCategory, CompoundPropertySet>) SwingTestUtil
			//					.getOnlySelector(panel2);
			//			selector.setCategorySelected(PropertySetProvider.INSTANCE.getIntegratedCategory(), true);
			//			JButton startButton = SwingTestUtil.getButton(wizard, "Start mapping");
			//			startButton.doClick();
			//			while (wizard.isVisible())
			//			{
			//				ThreadUtil.sleep(250);
			//				System.out.println("waiting for wizard to close");
			//			}
			//			while (SwingTestUtil.getOnlyVisibleDialog(null) == null)
			//			{
			//				System.out.println("waiting for loading dialog to show");
			//				ThreadUtil.sleep(250);
			//			}
			SwingTestUtil.waitForGUI(250);
			loadingDialog();

			viewer = (BlockableFrame) Settings.TOP_LEVEL_FRAME;
			Assert.assertTrue("Wrong title: " + viewer.getTitle(), viewer.getTitle().contains(dataset));
			Assert.assertTrue("Wrong title: " + viewer.getTitle(), viewer.getTitle().contains("CheS-Mapper"));
		}
		if (viewer == null)
			System.exit(0);
	}

	private static void loadingDialog()
	{
		Window w[] = Window.getWindows();
		Assert.assertEquals(2, w.length);
		JDialog d = (JDialog) w[0];
		while (d.isVisible())
		{
			Assert.assertTrue(d.getTitle().matches(".*[0-9]++%.*"));
			ThreadUtil.sleep(250);
			System.out.println("waiting for loading dialog to close");
		}
		SwingTestUtil.waitForGUI(250);

		while (SwingTestUtil.getOnlyVisibleFrame() == null)
		{
			ThreadUtil.sleep(250);
			System.out.println("waiting for viewer to show up");
		}
		SwingTestUtil.waitForGUI(250);
		Assert.assertNotNull(Settings.TOP_LEVEL_FRAME);
		Assert.assertTrue(Settings.TOP_LEVEL_FRAME.isVisible());
	}

	private JMenuItem getPopupMenuItem(String text)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen(viewer)));
			Point loc = viewer.getLocationOnScreen();
			r.mouseMove(viewer.getWidth() / 2 + (int) loc.getX(), viewer.getHeight() / 2 + (int) loc.getY());
			SwingTestUtil.waitForGUI(250);
			r.mousePress(InputEvent.BUTTON3_MASK);
			r.mouseRelease(InputEvent.BUTTON3_MASK);
			SwingTestUtil.waitForGUI(250);
			JPopupMenu popup = SwingTestUtil.getOnlyPopupMenu(viewer);
			return SwingTestUtil.getMenuItem(popup, text);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static void waitWhileBlocked(String msg)
	{
		waitWhileBlocked(msg, true);
	}

	private static void waitWhileBlocked(String msg, boolean checkBlock)
	{
		SwingTestUtil.waitForGUI(250);
		//SwingUtil.waitForAWTEventThread();
		if (checkBlock)
			Assert.assertTrue(viewer.isBlocked());
		while (viewer.isBlocked())
		{
			SwingTestUtil.waitForGUI(250);
			System.out.println(msg);
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void test1ClusterAndCompoundList()
	{
		List<JList> lists = ListUtil.cast(JList.class, SwingTestUtil.getComponents(viewer, JList.class));
		Assert.assertEquals(lists.size(), 2);

		JList clusterList = lists.get(0);
		JList compoundList = lists.get(1);
		Assert.assertEquals(clusterList.getModel().getSize() - 1, numClusters);

		Assert.assertFalse(compoundList.isShowing());
		SwingTestUtil.clickList(clusterList, 0);
		waitWhileBlocked("waiting to switch to single compound selection", false);
		Assert.assertTrue(compoundList.isShowing());
		Assert.assertEquals(compoundList.getModel().getSize(), numCompounds);
		int randomCompound = random.nextInt(Math.min(10, numCompounds));
		SwingTestUtil.clickList(compoundList, randomCompound);
		waitWhileBlocked("waiting to zoom into compound");

		int numCompoundsInCluster = compoundList.getModel().getSize();
		ClickableLabel lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
		SwingTestUtil.clickXButton(lab);
		waitWhileBlocked("waiting to zoom out of compound to cluster");
		if (compoundList.isShowing())
		{
			Assert.assertTrue(numCompoundsInCluster > 1);
			SwingTestUtil.clickXButton(lab);
			waitWhileBlocked("waiting to zoom out of cluster");
		}
		else
			Assert.assertTrue(numCompoundsInCluster == 1);
		Assert.assertFalse(compoundList.isShowing());
		Assert.assertFalse(lab.isShowing());

		for (int i = 0; i < numClusters; i++)
		{
			Assert.assertFalse(compoundList.isShowing());
			Assert.assertFalse(viewer.isBlocked());
			SwingTestUtil.clickList(clusterList, i + 1);
			waitWhileBlocked("waiting to zoom into cluster");
			Assert.assertTrue(compoundList.isShowing());
			Assert.assertEquals(compoundList.getModel().getSize(), numCompoundsInClusters[i]);
			randomCompound = random.nextInt(Math.min(10, numCompoundsInClusters[i]));
			SwingTestUtil.clickList(compoundList, randomCompound);
			waitWhileBlocked("waiting to zoom into compound");

			lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
			SwingTestUtil.clickXButton(lab);
			waitWhileBlocked("waiting to zoom out of compound to cluster");
			if (compoundList.isShowing())
			{
				Assert.assertTrue(numCompoundsInClusters[i] > 1);
				SwingTestUtil.clickXButton(lab);
				waitWhileBlocked("waiting to zoom out of cluster");
			}
			else
				Assert.assertTrue(numCompoundsInClusters[i] == 1);
			Assert.assertFalse(compoundList.isShowing());
			Assert.assertFalse(lab.isShowing());
		}
	}

	//	@Test
	//	public void test2Export()
	//	{
	//		final JMenuItem export = SwingTestUtil.getMenuItem(viewer.getJMenuBar(), "Export cluster/s");
	//		Assert.assertNotNull(export);
	//		SwingUtilities.invokeLater(new Runnable()
	//		{
	//			@Override
	//			public void run()
	//			{
	//				export.getAction().actionPerformed(new ActionEvent(this, -1, ""));
	//			}
	//		});
	//		JDialog dialog = null;
	//		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for cluster dialog");
	//		}
	//		Assert.assertTrue(dialog.getTitle().equals("Export Cluster/s"));
	//		JList list = SwingTestUtil.getOnlyList(dialog);
	//		Assert.assertEquals(list.getModel().getSize(), numClusters);
	//
	//		JCheckBox selectAll = SwingTestUtil.getCheckBox(dialog, "Select all");
	//		selectAll.doClick();
	//		JButton buttonOK = SwingTestUtil.getButton(dialog, "OK");
	//		buttonOK.doClick();
	//		Assert.assertFalse(dialog.isVisible());
	//
	//		dialog = null;
	//		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for filechooser dialog");
	//		}
	//		Assert.assertEquals(dialog.getTitle(), "Save");
	//		JTextField textField = SwingTestUtil.getOnlyTextField(dialog);
	//		String tmpFile = "/tmp/destinationfile.sdf";
	//		Assert.assertFalse(new File(tmpFile).exists());
	//		textField.setText(tmpFile);
	//
	//		final JButton save = SwingTestUtil.getButton(dialog, "Save");
	//		SwingUtilities.invokeLater(new Runnable()
	//		{
	//			@Override
	//			public void run()
	//			{
	//				save.doClick();
	//			}
	//		});
	//		while (!new File(tmpFile).exists())
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for sdf file");
	//		}
	//		ThreadUtil.sleep(200);
	//		Assert.assertEquals(SDFUtil.countCompounds(tmpFile), numCompounds);
	//		File f = new File(tmpFile);
	//		f.delete();
	//	}

	//	@Test
	//	public void test3Exit()
	//	{
	//		JMenuItem exit = getPopupMenuItem("Exit");
	//		Assert.assertNotNull("Exit popup menu item not found", exit);
	//		exit.doClick();
	//		Assert.assertFalse(true);
	//	}
}
