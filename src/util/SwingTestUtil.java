package util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.Assert;

public class SwingTestUtil
{
	public static void assertErrorDialog(Window owner, String titleMatch)
	{
		ThreadUtil.sleep(200);
		JDialog d = null;
		for (Window w : Window.getWindows())
			if (w instanceof JDialog && ((JDialog) w).getOwner() == owner)
				d = (JDialog) w;
		Assert.assertNotNull(d);
		Assert.assertTrue("title is '" + d.getTitle() + "'", d.getTitle().matches("(?i).*" + titleMatch + "*"));
		d.dispose();
		Assert.assertFalse(d.isVisible());
		ThreadUtil.sleep(200);
	}

	public static JButton getButton(Container owner, String text)
	{
		for (Component c : owner.getComponents())
		{
			if (c instanceof JButton && ((JButton) c).getText().equals(text))
				return (JButton) c;
			else if (c instanceof JComponent)
			{
				JButton b = getButton((JComponent) c, text);
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

	public static JTextField getOnlyTextField(Container owner)
	{
		return (JTextField) getOnlyComponent(owner, JTextField.class);
	}

	public static JList getOnlyList(Container owner)
	{
		return (JList) getOnlyComponent(owner, JList.class);
	}

	private static JComponent getOnlyComponent(Container owner, Class<?> clazz)
	{
		List<JComponent> list = getComponents(owner, clazz);
		if (list.size() != 1)
			throw new IllegalStateException("num compounds found " + list.size());
		return list.get(0);
	}

	private static List<JComponent> getComponents(Container owner, Class<?> clazz)
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

	public static void main(String args[])
	{
		JButton b = new JButton("b");
		b.setName("b");
		final JPanel p = new JPanel();
		p.add(b);
		SwingUtil.showInDialog(p, "test", null, new Runnable()
		{

			@Override
			public void run()
			{
				System.out.println(SwingTestUtil.getButton(p.getTopLevelAncestor(), "b"));

			}
		});
		System.exit(0);
	}
}
