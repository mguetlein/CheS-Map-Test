package util;

import gui.BlockableFrame;
import gui.Selector;
import gui.swing.ComponentFactory.ClickableLabel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import org.junit.Assert;

public class SwingTestUtil
{
	public static void clickButton(final AbstractButton b)
	{
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				b.doClick();
			}
		});
	}

	public static void setText(final JTextField tf, final String text)
	{
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				tf.setText(text);
			}
		});
	}

	public static JFrame getOnlyVisibleFrame()
	{
		JFrame f = null;
		for (Window w : Window.getWindows())
			if (w instanceof JFrame && w.isShowing())
			{
				if (f != null)
					throw new IllegalStateException("num frames > 1");
				f = (JFrame) w;
			}
		return f;
	}

	public static JDialog getOnlyVisibleDialog(Window owner)
	{
		JDialog d = null;
		for (Window w : Window.getWindows())
		{
			//			System.out.println("found " + w + " " + w.getOwner());
			if (w instanceof JDialog && w.isShowing() && ((JDialog) w).getOwner() == owner)
			{
				if (d != null)
					throw new IllegalStateException("num dialogs > 1");
				d = (JDialog) w;
			}
		}
		return d;
	}

	public static JDialog getVisibleDialog(Window owner, String title)
	{
		for (Window w : Window.getWindows())
			if (w instanceof JDialog && w.isShowing() && ((JDialog) w).getOwner() == owner
					&& ((JDialog) w).getTitle().equals(title))
				return (JDialog) w;
		return null;
	}

	public static void assertErrorDialog(Window owner, String titleMatch, String contentMatch)
	{
		SwingTestUtil.waitForGUI(250);
		JDialog d = getOnlyVisibleDialog(owner);
		Assert.assertNotNull("no visible dialog found with owner " + owner, d);
		Assert.assertTrue("title is '" + d.getTitle() + "', does not match '" + titleMatch + "'",
				d.getTitle().matches("(?i).*" + titleMatch + ".*"));
		//		String content = getAllText(d);
		//		String contentMatchRegexp = "(?i).*" + contentMatch + ".*";
		//		boolean b = content.matches(contentMatchRegexp);
		Assert.assertTrue("content is '" + getAllText(d) + "', does not contain '" + contentMatch + "'", getAllText(d)
				.matches("(?i).*" + contentMatch + ".*"));
		JButton close = getButton(d, "Close");
		Assert.assertNotNull(close);
		SwingTestUtil.clickButton(close);
		Assert.assertFalse(d.isShowing());
		SwingTestUtil.waitForGUI(250);
	}

	public static void waitForGUI(long milliseconds)
	{
		SwingUtil.waitForAWTEventThread();
		ThreadUtil.sleep(milliseconds);
		SwingUtil.waitForAWTEventThread();
	}

	public static JMenuItem getMenuItem(JMenuBar menu, String text)
	{
		for (int i = 0; i < menu.getMenuCount(); i++)
		{
			JMenuItem m = getMenuItem(menu.getMenu(i), text);
			if (m != null)
				return m;
		}
		return null;
	}

	private static JMenuItem getMenuItem(JMenu menu, String text)
	{
		for (Component c : menu.getMenuComponents())
		{
			if (c instanceof JMenu)
			{
				JMenuItem m = getMenuItem((JMenu) c, text);
				if (m != null)
					return m;
			}
			else if (c instanceof JMenuItem && ((JMenuItem) c).getText().equals(text))
			{
				return (JMenuItem) c;
			}
		}
		return null;
	}

	public static JButton getButton(Container owner, String text)
	{
		return (JButton) getAbstractButton(owner, text);
	}

	public static JCheckBox getCheckBox(Container owner, String text)
	{
		return (JCheckBox) getAbstractButton(owner, text);
	}

	public static JRadioButton getRadioButton(Container owner, String text)
	{
		return (JRadioButton) getAbstractButton(owner, text);
	}

	public static JMenuItem getMenuItem(Container owner, String text)
	{
		return (JMenuItem) getAbstractButton(owner, text);
	}

	private static AbstractButton getAbstractButton(Container owner, String text)
	{
		for (Component c : owner.getComponents())
		{
			if (c instanceof AbstractButton && ((AbstractButton) c).getText() != null
					&& ((AbstractButton) c).getText().equals(text))
				return (AbstractButton) c;
			else if (c instanceof JComponent)
			{
				AbstractButton b = getAbstractButton((JComponent) c, text);
				if (b != null)
					return b;
			}
		}
		return null;
	}

	public static JTextField getTextFieldWithName(Container owner, String name)
	{
		return (JTextField) getComponentWithName(owner, name, JTextField.class);
	}

	public static JButton getButtonWithName(Container owner, String name)
	{
		return (JButton) getComponentWithName(owner, name, JButton.class);
	}

	private static JComponent getComponentWithName(Container owner, String name, Class<?> clazz)
	{
		for (Component c : owner.getComponents())
		{
			if (clazz.isInstance(c) && ((JComponent) c).getName() != null && ((JComponent) c).getName().equals(name))
				return (JComponent) c;
			else if (c instanceof JComponent)
			{
				JComponent b = getComponentWithName((JComponent) c, name, clazz);
				if (b != null)
					return b;
			}
		}
		return null;
	}

	public static String getAllText(Container owner)
	{
		String content = "";
		String sep = "";//"\n"
		for (JComponent comp : getComponents(owner, JTextComponent.class))
			content += ((JTextComponent) comp).getText() + sep;
		for (JComponent comp : getComponents(owner, JLabel.class))
			content += ((JLabel) comp).getText() + sep;
		for (JComponent comp : getComponents(owner, AbstractButton.class))
			content += ((AbstractButton) comp).getText() + sep;
		return content;
	}

	public static JTextField getOnlyTextField(Container owner)
	{
		return (JTextField) getOnlyComponent(owner, JTextField.class);
	}

	public static JList<?> getOnlyList(Container owner)
	{
		return (JList<?>) getOnlyComponent(owner, JList.class);
	}

	public static JButton getOnlyButton(Container owner)
	{
		return (JButton) getOnlyComponent(owner, JButton.class);
	}

	public static JPopupMenu getOnlyPopupMenu(Container owner)
	{
		return (JPopupMenu) getOnlyComponent(owner, JPopupMenu.class);
	}

	public static Selector<?, ?> getOnlySelector(Container owner)
	{
		return (Selector<?, ?>) getOnlyComponent(owner, Selector.class);
	}

	public static JSpinner getOnlySpinner(Container owner)
	{
		return (JSpinner) getOnlyComponent(owner, JSpinner.class);
	}

	public static JCheckBox getOnlyCheckBox(Container owner)
	{
		return (JCheckBox) getOnlyComponent(owner, JCheckBox.class);
	}

	public static JComboBox<?> getOnlyComboBox(Container owner)
	{
		return (JComboBox<?>) getOnlyComponent(owner, JComboBox.class);
	}

	public static ClickableLabel getVisibleClickableLabel(Container owner, int swingConstantOrientation)
	{
		ClickableLabel l = null;
		for (JComponent c : getComponents(owner, ClickableLabel.class))
		{
			if (c.isShowing())
			{
				if (l != null)
				{
					if (swingConstantOrientation == SwingConstants.TOP)
						l = (ClickableLabel) ((l.getBounds().y < c.getBounds().y) ? l : c);
					else
						throw new IllegalStateException();
				}
				else
					l = (ClickableLabel) c;
			}
		}
		return l;
	}

	private static JComponent getOnlyComponent(Container owner, Class<?> clazz)
	{
		List<JComponent> list = getComponents(owner, clazz);
		if (list.size() != 1)
			throw new IllegalStateException("num compounds found " + list.size());
		return list.get(0);
	}

	public static List<JComponent> getComponents(Container owner, Class<?> clazz)
	{
		List<JComponent> list = new ArrayList<JComponent>();
		for (Component c : owner.getComponents())
		{
			if (clazz.isInstance(c))
				list.add((JComponent) c);
			else if (c instanceof JComponent)
			{
				for (JComponent b : getComponents((JComponent) c, clazz))
					list.add(b);
			}
		}
		return list;
	}

	public static void clickList(JList<?> list, int index)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen((Window) list.getTopLevelAncestor())));
			Point p = list.getLocationOnScreen();
			Point p2 = list.indexToLocation(index);
			r.mouseMove(p.x + p2.x + 5, p.y + p2.y + 5);
			waitForGUI(250);
			r.mousePress(InputEvent.BUTTON1_MASK);
			r.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void clickXButton(JComponent lab)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen((Window) lab.getTopLevelAncestor())));
			Point p = lab.getLocationOnScreen();
			r.mouseMove(p.x + 5, p.y + 5);
			waitForGUI(250);
			r.mousePress(InputEvent.BUTTON1_MASK);
			r.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String args[])
	{
		final JList<String> l = new JList<String>(
				"alsjkf asölkfjaölskfj ölaskjflaskdjf iosdfosdf sdk slfdjk aölskfj lskfdj sldkfj".split(" "));
		JScrollPane scroll = new JScrollPane(l);
		SwingUtil.showInDialog(scroll, "test", null, new Runnable()
		{

			@Override
			public void run()
			{
				//				ThreadUtil.sleep(1000);
				SwingTestUtil.clickList(l, 6);

			}
		});

		//		JButton b = new JButton("b");
		//		b.setName("b");
		//		final JPanel p = new JPanel();
		//		p.add(b);
		//		SwingUtil.showInDialog(p, "test", null, new Runnable()
		//		{
		//
		//			@Override
		//			public void run()
		//			{
		//				System.out.println(SwingTestUtil.getButton(p.getTopLevelAncestor(), "b"));
		//
		//			}
		//		});
		System.exit(0);
	}

	public static void waitWhileBlocked(BlockableFrame viewer, String msg)
	{
		waitWhileBlocked(viewer, msg, true);
	}

	public static void waitWhileBlocked(BlockableFrame viewer, String msg, boolean checkBlock)
	{
		SwingTestUtil.waitForGUI(50);
		//SwingUtil.waitForAWTEventThread();
		if (checkBlock)
			Assert.assertTrue(viewer.isBlocked());
		while (viewer.isBlocked())
		{
			SwingTestUtil.waitForGUI(50);
			System.out.println(msg);
		}
	}

}
