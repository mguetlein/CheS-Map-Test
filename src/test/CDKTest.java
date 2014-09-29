package test;

import gui.LaunchCheSMapper;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import main.CheSMapping;
import main.Settings;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openscience.cdk.isomorphism.MCSComputer;

import property.PropertySetCategory;
import property.PropertySetProvider;
import util.ArrayUtil;
import util.FileUtil;
import workflow.MappingWorkflow;
import alg.align3d.MCSAligner;
import data.ClusteringData;
import data.DatasetFile;
import data.FeatureService;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;
import dataInterface.NumericProperty;
import dataInterface.SubstructureSmartsType;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CDKTest
{
	public CDKTest()
	{
		LaunchCheSMapper.init();
		Settings.CACHING_ENABLED = false;
	}

	@Test
	public void align3D() throws Exception
	{
		int tryCount = 0;
		while (true)
		{
			Properties props = MappingWorkflow.createMappingWorkflow("data/cox2_13_mcs.sdf", null, null, null, null,
					MCSAligner.INSTANCE);
			final CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
			DatasetFile d = mapping.getDatasetFile();
			ClusteringData data = mapping.doMapping();
			Assert.assertEquals(1, data.getNumClusters());
			Assert.assertEquals("O=S(=O)(c1ccc(cc1)n2ccnc2(cc))C",
					data.getClusters().get(0).getSubstructureSmarts(SubstructureSmartsType.MCS));
			boolean b = sdfEquals("data/cox2_13_mcs_aligned.sdf", d.getAlignSDFilePath(), true);
			if (!b)
			{
				tryCount++;
				System.err.println("XXXXXXXXXXXXXXXX\nalign test failed, retry " + tryCount + "!\nXXXXXXXXXXXXXXXX");
			}
			else
				break;
		}
	}

	@Test
	public void testDescriptors() throws Exception
	{
		DatasetFile d = DatasetFile.localFile(new File("data/cox2_13_mcs.sdf").getAbsolutePath());
		d.loadDataset();

		PropertySetCategory set = PropertySetProvider.INSTANCE.getCDKCategory();
		PropertySetCategory categories[] = set.getSubCategory();
		Assert.assertEquals(5, categories.length);
		String names[] = new String[] { "electronic", "constitutional", "topological", "hybrid", "geometrical" };
		int num[] = new int[] { 7, 14, 23, 2, 5 };
		Set<CompoundPropertySet> computed = new HashSet<CompoundPropertySet>();

		String namesCmp[] = FileUtil.readStringFromFile("data/cox2_13_mcs_CDK-names.txt").split("\n");
		String valuesCmp[] = FileUtil.readStringFromFile("data/cox2_13_mcs_CDK-values.txt").split("\n");
		int line = 0;
		//		String nameRes = "";
		//		String valuesRes = "";

		for (PropertySetCategory cat : categories)
		{
			int idx = ArrayUtil.indexOf(names, cat.toString());
			Assert.assertTrue(cat.toString() + " not included in " + names, idx != -1);
			Assert.assertEquals(num[idx], cat.getPropertySet(d).length);

			for (CompoundPropertySet ps : cat.getPropertySet(d))
			{
				Assert.assertTrue("Already computed: " + ps, !ps.isComputed(d) || computed.contains(ps));

				if (!ps.isComputed(d) && !ps.isComputationSlow())
				{
					Assert.assertTrue(ps.compute(d));
					Assert.assertTrue(ps.isComputed(d));
					computed.add(ps);

					for (int i = 0; i < ps.getSize(d); i++)
					{
						CompoundProperty p = ps.get(d, i);
						Double v[] = ((NumericProperty) p).getDoubleValues();
						String name = ps.toString() + " " + p.getName();
						//						String valuesStr = ArrayUtil.toCSVString(v);
						//						nameRes += name + "\n";
						//						valuesRes += valuesStr + "\n";
						Assert.assertEquals(namesCmp[line], name);
						Double vCmp[] = ArrayUtil.doubleFromCSVString(valuesCmp[line]);
						Assert.assertEquals(v.length, vCmp.length);
						for (int j = 0; j < vCmp.length; j++)
							if (v[j] == null)
								Assert.assertNull(vCmp[j]);
							else
								Assert.assertEquals(vCmp[j], v[j], 0.00000001);
						line++;
					}
				}
			}
		}
		//		FileUtil.writeStringToFile("data/cox2_13_mcs_CDK-names.txt", nameRes);
		//		FileUtil.writeStringToFile("data/cox2_13_mcs_CDK-values.txt", valuesRes);

	}

	@Test
	public void testSmilesProp() throws Exception
	{
		Assert.assertTrue(FeatureService.testSmilesProp());
	}

	@Test
	public void testMCS() throws Exception
	{
		String data[] = { "data/mcs.smi", "data/cox2_13_mcs.sdf" };
		int num[] = { 3, 13 };
		String mcs[] = { "CCCCOCCNC", "O=S(=O)(c1ccc(cc1)n2ccnc2(cc))C" };
		MCSComputer.DEBUG = true;
		for (int i = 0; i < mcs.length; i++)
		{
			DatasetFile d = DatasetFile.localFile(new File(data[i]).getAbsolutePath());
			d.loadDataset();
			Assert.assertEquals(num[i], d.numCompounds());
			Assert.assertEquals(mcs[i], MCSComputer.computeMCS(d));
		}
		MCSComputer.DEBUG = false;
	}

	@Test
	public void testCreateSDF() throws Exception
	{
		DatasetFile d = DatasetFile.localFile(new File("data/demo.smi").getAbsolutePath());
		d.loadDataset();
		Assert.assertEquals(10, d.numCompounds());
		sdfEquals("data/demo.2dconverted.sdf", d.getSDF());
	}

	@Test
	public void z_resetCachingEnabled()
	{
		Assert.assertFalse(Settings.CACHING_ENABLED);
		Settings.CACHING_ENABLED = true;
	}

	private void sdfEquals(String file1, String file2)
	{
		sdfEquals(file1, file2, false);
	}

	private boolean sdfEquals(String file1, String file2, boolean noAsserts)
	{
		String sdf1[] = FileUtil.readStringFromFile(file1).split("\n");
		String sdf2[] = FileUtil.readStringFromFile(file2).split("\n");
		if (noAsserts)
		{
			if (sdf1.length != sdf2.length)
				return false;
		}
		else
			Assert.assertEquals(sdf1.length, sdf2.length);
		for (int i = 0; i < sdf2.length; i++)
		{
			String s1 = sdf1[i];
			String s2 = sdf2[i];
			if (s1.matches("  CDK     [0-9]*") && s2.matches("  CDK     [0-9]*"))
				continue;
			if (noAsserts)
			{
				if (!s1.equals(s2))
					return false;
			}
			else
				Assert.assertEquals(s1, s2);
		}
		return true;
	}
}
