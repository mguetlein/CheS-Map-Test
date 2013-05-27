package test;

import gui.BlockableFrame;
import gui.CheSMapperWizard;
import gui.DatasetWizardPanel;
import gui.FeatureWizardPanel;
import gui.LaunchCheSMapper;
import gui.Selector;
import io.SDFUtil;

import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import main.Settings;

import org.junit.Assert;
import org.junit.Test;

import util.ListUtil;
import util.ScreenUtil;
import util.SwingTestUtil;
import util.ThreadUtil;
import dataInterface.CompoundPropertySet;

public class ViewerTest
{
	static BlockableFrame viewer;
	public static String DATA_DIR = "data/";
	static Random random = new Random();

	String dataset = "basicTestSet.sdf";
	int numClusters = 2;
	int numCompounds = 16;
	int numCompoundsInClusters[] = { 6, 10 };

	public ViewerTest()
	{
		if (viewer == null)
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
			CheSMapperWizard wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			JButton nextButton = SwingTestUtil.getButton(wizard, "Next");
			Assert.assertTrue(wizard.getCurrentPanel() instanceof DatasetWizardPanel);
			DatasetWizardPanel panel = (DatasetWizardPanel) wizard.getCurrentPanel();
			JTextField textField = SwingTestUtil.getOnlyTextField(panel);
			JButton buttonLoad = SwingTestUtil.getButton(panel, "Load Dataset");
			textField.setText(DATA_DIR + dataset);
			buttonLoad.doClick();
			while (wizard.isBlocked())
			{
				ThreadUtil.sleep(50);
				System.out.println("waiting for panel to stop loading");
			}
			nextButton.doClick();
			nextButton.doClick();
			FeatureWizardPanel panel2 = (FeatureWizardPanel) wizard.getCurrentPanel();
			@SuppressWarnings("unchecked")
			Selector<CompoundPropertySet> selector = (Selector<CompoundPropertySet>) SwingTestUtil
					.getOnlySelector(panel2);
			selector.setCategorySelected("Included in the Dataset", true);
			JButton startButton = SwingTestUtil.getButton(wizard, "Start mapping");
			startButton.doClick();

			while (wizard.isVisible())
			{
				ThreadUtil.sleep(50);
				System.out.println("waiting for wizard to close");
			}
			ThreadUtil.sleep(250);
			loadingDialog();

			viewer = (BlockableFrame) Settings.TOP_LEVEL_FRAME;
			Assert.assertTrue(viewer.getTitle().contains(dataset));
			Assert.assertTrue(viewer.getTitle().contains("CheS-Mapper"));
		}
	}

	private void loadingDialog()
	{
		Window w[] = Window.getWindows();
		Assert.assertEquals(3, w.length);
		JDialog d = (JDialog) w[2];
		while (d.isVisible())
		{
			Assert.assertTrue(d.getTitle().matches(".*[0-9]++%.*"));
			ThreadUtil.sleep(50);
			System.out.println("waiting for loading dialog to close");
		}
		while (Settings.TOP_LEVEL_FRAME == null || !Settings.TOP_LEVEL_FRAME.isVisible())
		{
			ThreadUtil.sleep(50);
			System.out.println("waiting for top level frame");
		}
		ThreadUtil.sleep(500);
	}

	private JMenuItem getPopupMenuItem(String text)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen(viewer)));
			Point loc = viewer.getLocationOnScreen();
			r.mouseMove(viewer.getWidth() / 2 + (int) loc.getX(), viewer.getHeight() / 2 + (int) loc.getY());
			ThreadUtil.sleep(100);
			r.mousePress(InputEvent.BUTTON3_MASK);
			r.mouseRelease(InputEvent.BUTTON3_MASK);
			ThreadUtil.sleep(200);
			JPopupMenu popup = SwingTestUtil.getOnlyPopupMenu(viewer);
			return SwingTestUtil.getMenuItem(popup, text);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@Test
	public void testClusterAndCompoundList()
	{
		List<JList> lists = ListUtil.cast(JList.class, SwingTestUtil.getComponents(viewer, JList.class));
		Assert.assertEquals(lists.size(), 2);

		JList clusterList = lists.get(0);
		JList modelList = lists.get(1);
		Assert.assertEquals(clusterList.getModel().getSize() - 1, numClusters);

		for (int i = 0; i < numClusters; i++)
		{
			Assert.assertFalse(modelList.isShowing());
			Assert.assertFalse(viewer.isBlocked());
			SwingTestUtil.clickList(clusterList, i + 1);
			ThreadUtil.sleep(200);
			Assert.assertTrue(viewer.isBlocked());
			while (viewer.isBlocked())
			{
				ThreadUtil.sleep(200);
				System.out.println("waiting to zoom into cluster");
			}
			Assert.assertTrue(modelList.isShowing());
			Assert.assertEquals(modelList.getModel().getSize(), numCompoundsInClusters[i]);

			int randomModel = random.nextInt(numCompoundsInClusters[i]);
			SwingTestUtil.clickList(modelList, randomModel);
			ThreadUtil.sleep(200);
			Assert.assertTrue(viewer.isBlocked());
			while (viewer.isBlocked())
			{
				ThreadUtil.sleep(200);
				System.out.println("waiting to zoom into model");
			}

			SwingTestUtil.clickList(modelList, randomModel);
			ThreadUtil.sleep(200);
			Assert.assertTrue(viewer.isBlocked());
			while (viewer.isBlocked())
			{
				ThreadUtil.sleep(200);
				System.out.println("waiting to zoom out of model");
			}

			SwingTestUtil.clickList(clusterList, 0);
			ThreadUtil.sleep(200);
			Assert.assertTrue(viewer.isBlocked());
			while (viewer.isBlocked())
			{
				ThreadUtil.sleep(200);
				System.out.println("waiting to zoom out of cluster");
			}
		}
	}

	@Test
	public void testExport()
	{
		final JMenuItem export = SwingTestUtil.getMenuItem(viewer.getJMenuBar(), "Export cluster/s");
		Assert.assertNotNull(export);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				export.getAction().actionPerformed(new ActionEvent(this, -1, ""));
			}
		});
		JDialog dialog = null;
		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
		{
			ThreadUtil.sleep(200);
			System.out.println("waiting for cluster dialog");
		}
		Assert.assertTrue(dialog.getTitle().equals("Export Cluster/s"));
		JList list = SwingTestUtil.getOnlyList(dialog);
		Assert.assertEquals(list.getModel().getSize(), numClusters);

		JCheckBox selectAll = SwingTestUtil.getCheckBox(dialog, "Select all");
		selectAll.doClick();
		JButton buttonOK = SwingTestUtil.getButton(dialog, "OK");
		buttonOK.doClick();
		Assert.assertFalse(dialog.isVisible());

		dialog = null;
		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
		{
			ThreadUtil.sleep(200);
			System.out.println("waiting for filechooser dialog");
		}
		Assert.assertEquals(dialog.getTitle(), "Save");
		JTextField textField = SwingTestUtil.getOnlyTextField(dialog);
		String tmpFile = "/tmp/destinationfile.sdf";
		Assert.assertFalse(new File(tmpFile).exists());
		textField.setText(tmpFile);

		final JButton save = SwingTestUtil.getButton(dialog, "Save");
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				save.doClick();
			}
		});
		while (!new File(tmpFile).exists())
		{
			ThreadUtil.sleep(200);
			System.out.println("waiting for sdf file");
		}
		ThreadUtil.sleep(200);
		Assert.assertEquals(SDFUtil.countCompounds(tmpFile), numCompounds);
		File f = new File(tmpFile);
		f.delete();
	}

	@Test
	public void testExit()
	{
		JMenuItem exit = getPopupMenuItem("Exit");
		exit.doClick();
		Assert.assertFalse(true);
	}
}
