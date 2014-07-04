package test;

import gui.LaunchCheSMapper;
import io.SDFUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import main.CheSMapping;
import main.Settings;

import org.junit.Assert;
import org.junit.Test;

import property.ListedFragmentSet;
import property.PropertySetProvider;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.ObjectUtil;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.cluster.WekaClusterer;
import alg.cluster.r.DynamicTreeCutHierarchicalRClusterer;
import alg.embed3d.AbstractRTo3DEmbedder;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import alg.embed3d.r.Sammon3DEmbedder;
import cluster.ClusteringImpl;
import cluster.ExportData;
import data.ClusteringData;
import data.DatasetFile;
import data.fragments.MatchEngine;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertySet.Type;
import dataInterface.CompoundPropertyUtil;
import dataInterface.FragmentProperty.SubstructureType;
import dataInterface.FragmentPropertySet;

public class MappingAndExportTest
{
	static class Dataset
	{
		String name;
		int size;
		String integratedNonMissing[];
		String integratedMissing[];
		String integratedFeature;

		public Dataset(String name, int size, String[] integratedNonMissing, String[] integratedMissing,
				String integratedFeature)
		{
			this.name = name;
			this.size = size;
			this.integratedNonMissing = integratedNonMissing;
			this.integratedMissing = integratedMissing;
			this.integratedFeature = integratedFeature;
			if (this.integratedMissing == null)
				this.integratedMissing = new String[0];
		}
	}

	static Dataset D_INCHI = new Dataset("compounds_inchi.csv", 7, new String[] { "name" }, null, "");
	static Dataset D_SDF = new Dataset(
			"12compounds.sdf",
			12,
			new String[] { "DSSTox_RID", "DSSTox_CID", "DSSTox_Generic_SID", "DSSTox_FileID", "STRUCTURE_Formula",
					"STRUCTURE_MolecularWeight", "STRUCTURE_ChemicalType", "STRUCTURE_TestedForm_DefinedOrganic",
					"STRUCTURE_Shown", "TestSubstance_ChemicalName", "TestSubstance_CASRN",
					"TestSubstance_Description", "STRUCTURE_ChemicalName_IUPAC", "STRUCTURE_SMILES",
					"STRUCTURE_Parent_SMILES", "STRUCTURE_InChI", "STRUCTURE_InChIKey", "StudyType", "Endpoint",
					"Species", "ChemClass_ERB", "ER_RBA", "LOG_ER_RBA", "ActivityScore_NCTRER",
					"ActivityOutcome_NCTRER", "ActivityCategory_ER_RBA", "ActivityCategory_Rationale_ChemClass_ERB",
					"F1_Ring", "F2_AromaticRing", "F3_PhenolicRing", "F4_Heteroatom", "F5_Phenol3nPhenyl",
					"F6_OtherKeyFeatures", "LOGP" },
			new String[] { "Mean_ER_RBA_ChemClass" },
			"F1_Ring,F2_AromaticRing,F3_PhenolicRing,F4_Heteroatom,F5_Phenol3nPhenyl,F6_OtherKeyFeatures,LOGP,STRUCTURE_MolecularWeight");
	static Dataset D_SMI = new Dataset("demo.smi", 10, new String[] {}, null, "");
	static Dataset D_CSV = new Dataset("caco2_20.csv", 20, new String[] { "name", "caco2", "logD", "rgyr", "HCPSA",
			"fROTB" }, null, "logD,rgyr,HCPSA,fROTB");
	static Dataset datasets[] = new Dataset[] { D_SMI, D_INCHI, D_SDF, D_CSV };

	static class Feature
	{
		String shortName;
		String featureNames[];
		Integer minFreq;
		private MatchEngine matchEngine = MatchEngine.OpenBabel;
		HashMap<Dataset, Integer> numFragments = new HashMap<Dataset, Integer>();

		public Feature(String shortName, String[] featureNames)
		{
			this.shortName = shortName;
			this.featureNames = featureNames;
		}

		public Feature(String shortName, String[] featureNames, int minFreq, MatchEngine matchEngine)
		{
			this.shortName = shortName;
			this.featureNames = featureNames;
			this.minFreq = minFreq;
			if (matchEngine == null)
				throw new IllegalStateException("do not set match engine to null");
			this.matchEngine = matchEngine;
		}
	}

	static List<Feature> features = new ArrayList<Feature>();
	static
	{
		LaunchCheSMapper.init();
		LaunchCheSMapper.setExitOnClose(false);

		//for (PropertySetShortcut c : new PropertySetShortcut[] { PropertySetShortcut.cdkFunct })
		//		for (PropertySetShortcuts c : new PropertySetShortcuts[] { PropertySetShortcuts.obFP2,
		//				PropertySetShortcuts.obFP3, PropertySetShortcuts.obFP4, PropertySetShortcuts.obMACCS,
		//				PropertySetShortcuts.benigniBossa })
		//		for (PropertySetShortcuts c : new PropertySetShortcuts[] { PropertySetShortcuts.ob,
		//				PropertySetShortcuts.cdk })
		for (PropertySetProvider.PropertySetShortcut c : PropertySetProvider.PropertySetShortcut.values())
		{
			if (c == PropertySetProvider.PropertySetShortcut.fminer)
				continue;

			List<String> props = new ArrayList<String>();
			CompoundPropertySet sets[] = null;
			if (c != PropertySetProvider.PropertySetShortcut.integrated)
			{
				sets = PropertySetProvider.INSTANCE.getDescriptorSets(null, c);
				for (CompoundPropertySet set : sets)
				{
					if ((c == PropertySetProvider.PropertySetShortcut.cdk || c == PropertySetProvider.PropertySetShortcut.ob)
							&& set.getType() != Type.NUMERIC)
						continue;
					if (!set.isSizeDynamic())
						for (int i = 0; i < set.getSize(null); i++)
							props.add(CompoundPropertyUtil.propToExportString(set.get(null, i)));
				}
			}

			Feature f = new Feature(c.toString(), ListUtil.toArray(String.class, props));
			features.add(f);
			if (sets != null && sets[0] instanceof FragmentPropertySet)
			{
				Feature fOB1 = f;
				fOB1.minFreq = 1;
				if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
				{
					fOB1.numFragments.put(D_INCHI, 0);
					fOB1.numFragments.put(D_SDF, 1);
					fOB1.numFragments.put(D_CSV, 6);
					fOB1.numFragments.put(D_SMI, 2);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP2)
				{
					fOB1.numFragments.put(D_CSV, 825);
					fOB1.numFragments.put(D_SMI, 432);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP3)
				{
					fOB1.numFragments.put(D_INCHI, 8);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP4)
				{
					fOB1.numFragments.put(D_INCHI, 19);
					fOB1.numFragments.put(D_SMI, 48);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obMACCS)
				{
					fOB1.numFragments.put(D_SMI, 111);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.cdkFunct)
				{
					fOB1.matchEngine = MatchEngine.CDK;
					fOB1.numFragments.put(D_SDF, 14);
					fOB1.numFragments.put(D_INCHI, 6);
					fOB1.numFragments.put(D_CSV, 38);
				}

				Feature fOB2 = new Feature(f.shortName, f.featureNames, 2, MatchEngine.OpenBabel);
				features.add(fOB2);
				if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
				{
					fOB2.numFragments.put(D_INCHI, 0);
					fOB2.numFragments.put(D_SDF, 1);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP3)
				{
					fOB2.numFragments.put(D_INCHI, 8);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.cdkFunct)
					fOB2.matchEngine = MatchEngine.CDK;

				Feature fOB0 = new Feature(f.shortName, f.featureNames, 0, MatchEngine.OpenBabel);
				features.add(fOB0);
				if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
				{
					fOB0.numFragments.put(D_INCHI, 0);
					fOB0.numFragments.put(D_SDF, 1);
					fOB0.numFragments.put(D_SMI, 2);
					fOB0.numFragments.put(D_CSV, 6);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP4)
				{
					fOB0.numFragments.put(D_INCHI, 19);
					fOB0.numFragments.put(D_SMI, 48);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obMACCS)
				{
					fOB0.numFragments.put(D_SMI, 111);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.obFP2)
				{
					fOB0.numFragments.put(D_CSV, 825);
					fOB0.numFragments.put(D_SMI, 432);
				}
				else if (c == PropertySetProvider.PropertySetShortcut.cdkFunct)
				{
					fOB0.matchEngine = MatchEngine.CDK;
					fOB0.numFragments.put(D_INCHI, 6);
					fOB0.numFragments.put(D_CSV, 40);
				}

				if (sets[0].getSubstructureType() == SubstructureType.MATCH && sets[0] instanceof ListedFragmentSet)
				{
					Feature fCDK1 = new Feature(f.shortName, f.featureNames, 1, MatchEngine.CDK);
					if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
					{
						fCDK1.numFragments.put(D_INCHI, 0);
						fCDK1.numFragments.put(D_SMI, 1);
						fCDK1.numFragments.put(D_SDF, 0);
						fCDK1.numFragments.put(D_CSV, 3);
					}
					Feature fCDK2 = new Feature(f.shortName, f.featureNames, 2, MatchEngine.CDK);
					if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
					{
						fCDK2.numFragments.put(D_INCHI, 0);
						fCDK2.numFragments.put(D_SMI, 1);
						fCDK2.numFragments.put(D_SDF, 0);
					}
					Feature fCDK0 = new Feature(f.shortName, f.featureNames, 0, MatchEngine.CDK);
					if (c == PropertySetProvider.PropertySetShortcut.benigniBossa)
					{
						fCDK0.numFragments.put(D_CSV, 3);
						fCDK0.numFragments.put(D_INCHI, 0);
						fCDK0.numFragments.put(D_SMI, 1);
						fCDK0.numFragments.put(D_SDF, 0);
					}
					features.add(fCDK1);
					features.add(fCDK2);
					features.add(fCDK0);
				}
			}
		}
	}

	static DatasetClusterer clusterers[] = new DatasetClusterer[] { NoClusterer.INSTANCE, //
			WekaClusterer.WEKA_CLUSTERER[0], //
			DynamicTreeCutHierarchicalRClusterer.INSTANCE, //
	};

	static ThreeDEmbedder embedders[] = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE, // 
			WekaPCA3DEmbedder.INSTANCE, //
			Sammon3DEmbedder.INSTANCE, //
	};

	@Test
	public void test()
	{
		Random r = new Random();
		ArrayUtil.scramble(datasets, r);
		ListUtil.scramble(features, r);
		ArrayUtil.scramble(clusterers, r);
		ArrayUtil.scramble(embedders, r);
		boolean CACHING[] = new boolean[] { false, false, true };

		HashMap<String, String> positions = new HashMap<String, String>();
		HashMap<String, String> outfiles = new HashMap<String, String>();

		int max = CACHING.length * datasets.length * features.size() * clusterers.length * embedders.length;
		int count = 0;

		//boolean cache = false;
		int cacheIdx = 0;
		for (boolean cache : CACHING)
		{
			Settings.CACHING_ENABLED = cache;

			int dIdx = 0;
			for (Dataset data : datasets)
			{
				DatasetFile d = null;
				DoubleKeyHashMap<String, Feature, Integer> numFeatures = new DoubleKeyHashMap<String, Feature, Integer>();

				int fIdx = 0;
				for (Feature feat : features)
				{
					int cIdx = 0;
					for (DatasetClusterer clust : clusterers)
					{
						int eIdx = 0;
						for (ThreeDEmbedder emb : embedders)
						{
							if ((clusterers.length == 1 && embedders.length == 1)
									|| (clust == NoClusterer.INSTANCE && emb != Random3DEmbedder.INSTANCE)
									|| (clust != NoClusterer.INSTANCE && emb == Random3DEmbedder.INSTANCE))
							{
								//					LaunchCheSMapper.main(new String[] { "-e", "--rem-missing-above-ratio", "1",
								//							"--keep-uniform-values", "-d", "data/" + data.name, "-f", feat.shortName, "-o", res });

								DescriptorSelection feats = new DescriptorSelection(feat.shortName,
										data.integratedFeature, null, null, null);
								if (feat.minFreq != null)
								{
									if (feat.minFreq == 0)
										feats.setFingerprintSettings(1, false, feat.matchEngine);
									else
										feats.setFingerprintSettings(feat.minFreq, true, feat.matchEngine);
								}
								if (clust instanceof DynamicTreeCutHierarchicalRClusterer)
									((DynamicTreeCutHierarchicalRClusterer) clust).setMinClusterSize(2);

								Properties props = MappingWorkflow.createMappingWorkflow("data/" + data.name, feats,
										clust, emb);

								CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
								d = mapping.getDatasetFile();

								System.err.println(d + " " + d.hashCode());

								ClusteringData clusteringData = mapping.doMapping();
								ClusteringImpl clustering = new ClusteringImpl();
								clustering.newClustering(clusteringData);

								// check features
								Assert.assertEquals(feats.getFeatures(d).size(), mapping.getNumFeatureSets());
								System.err.println(data.name + " " + feat.shortName + " " + feat.minFreq + " "
										+ feat.matchEngine + " num-features:" + clustering.getFeatures().size());
								if (feat.numFragments.containsKey(data))
									Assert.assertTrue(clustering.getFeatures().size() == feat.numFragments.get(data));
								else if (feat.shortName.equals(PropertySetProvider.PropertySetShortcut.integrated
										.toString()) && ObjectUtil.equals(data.integratedFeature, ""))
									Assert.assertTrue(clustering.getFeatures().size() == 0);
								else
									Assert.assertTrue(clustering.getFeatures().size() > 0);

								if (feat.minFreq != null)
								{
									String key = feat.shortName + feat.matchEngine;
									numFeatures.put(key, feat, clustering.getFeatures().size());
									Feature minFreq[] = ArrayUtil.toArray(new ArrayList<Feature>(numFeatures
											.keySet2(key)));
									for (int i = 0; i < minFreq.length - 1; i++)
									{
										for (int j = i + 1; j < minFreq.length; j++)
										{
											Assert.assertNotEquals(minFreq[i].minFreq, minFreq[j].minFreq);
											System.err.println("minF: " + minFreq[i].minFreq + ", num-features: "
													+ numFeatures.get(key, minFreq[i]));
											System.err.println("minF2: " + minFreq[j].minFreq + ", num-features: "
													+ numFeatures.get(key, minFreq[j]));
											if (minFreq[i].numFragments.containsKey(data)
													&& minFreq[j].numFragments.containsKey(data))
											{
												//do nothing, is checked explicitely
											}
											else if (minFreq[i].minFreq < minFreq[j].minFreq)
												Assert.assertTrue(numFeatures.get(key, minFreq[i]) > numFeatures.get(
														key, minFreq[j]));
											else
												Assert.assertTrue(numFeatures.get(key, minFreq[i]) < numFeatures.get(
														key, minFreq[j]));
										}
									}
								}

								// check clustering result
								DatasetClusterer usedClust = clusteringData.getDatasetClusterer();
								if (clustering.getFeatures().size() == 0)
									Assert.assertEquals(NoClusterer.INSTANCE, usedClust);
								else
									Assert.assertEquals(clust, usedClust);
								if (usedClust != NoClusterer.INSTANCE)
								{
									Assert.assertTrue(clustering.getNumClusters() > 1);
								}
								else
								{
									Assert.assertTrue(clustering.getNumClusters() == 1);
								}

								// check embedding result
								ThreeDEmbedder usedEmb = clusteringData.getThreeDEmbedder();
								if (clustering.getFeatures().size() == 0
										|| (emb instanceof AbstractRTo3DEmbedder && mapping.getEmbedException() != null
												&& clustering.getFeatures().size() <= 10 && mapping.getEmbedException()
												.getMessage().contains("Too few unique data points")))
									Assert.assertEquals(Random3DEmbedder.INSTANCE, usedEmb);
								else
									Assert.assertEquals(emb, usedEmb);
								if (usedEmb == Random3DEmbedder.INSTANCE)
								{
									Assert.assertNull(clusteringData.getEmbeddingQualityProperty());
								}
								else
								{
									Assert.assertNotNull(clusteringData.getEmbeddingQualityProperty());
								}
								String idxStr = dIdx + "." + fIdx + "." + cIdx + "." + eIdx + ".";
								if (!positions.containsKey(idxStr))
									positions.put(idxStr, ListUtil.toString(usedEmb.getPositions()));
								else
								{
									System.err.println("checking that positions are equal " + idxStr);
									Assert.assertEquals(positions.get(idxStr),
											ListUtil.toString(usedEmb.getPositions()));
								}

								// check exporting result
								String nonMissingValueProps[] = new String[0];
								for (String output : new String[] { "csv", "sdf" })
								{
									String outfile = "/tmp/" + data.name + "." + cacheIdx + "." + idxStr + output;
									ExportData.scriptExport(clustering, outfile, true, 1.0);
									String key = idxStr + output;
									if (!outfiles.containsKey(key))
										outfiles.put(key, outfile);
									else
									{
										System.err.println("checking that outfiles are equal " + outfiles.get(key)
												+ " and " + outfile);
										Assert.assertEquals(FileUtil.getMD5String(outfiles.get(key)),
												FileUtil.getMD5String(outfile));
									}

									String[] clusterProp = new String[0];
									if (usedClust != NoClusterer.INSTANCE)
										clusterProp = new String[] { (usedClust.getName() + " cluster assignement")
												.replace(' ', '_') };
									if (output.equals("csv"))
										nonMissingValueProps = verifyExportResultCSV(outfile, data.size,
												ArrayUtil.concat(String.class, data.integratedNonMissing, clusterProp),
												ArrayUtil.concat(String.class, feat.featureNames,
														data.integratedMissing));
									else
										verifyExportResultSDF(outfile, data.size, nonMissingValueProps);
								}
							}
							else
							{
								System.err.println("skipping cluster - embedding combination");
							}
							System.err.println("XXXXXXXXXXXX\ncache:" + cache + " " + (++count) + "/" + max + " "
									+ "\nXXXXXXXXXXXX");
							eIdx++;
						}
						cIdx++;
					}
					fIdx++;
				}
				dIdx++;
				d.clear();
			}
			cacheIdx++;
		}
	}

	public String[] verifyExportResultCSV(String csvFile, int numCompounds, String nonMissingValueProps[],
			String potentiallyMissingValueProps[])
	{
		CSVFile f = FileUtil.readCSV(csvFile, ",");
		Assert.assertEquals(numCompounds, f.content.size() - 1);
		Assert.assertEquals("SMILES", f.getHeader()[0]);

		List<String> nonMissingValueProperties = new ArrayList<String>();
		for (String p : nonMissingValueProps)
		{
			Assert.assertNotEquals("not found: " + p + ", " + ArrayUtil.toString(f.getHeader()), f.getColumnIndex(p),
					-1);
			Assert.assertTrue("has missing: " + p, ArrayUtil.indexOf(f.getColumn(p), null) == -1);
			nonMissingValueProperties.add(p);
		}

		for (String p : potentiallyMissingValueProps)
		{
			Assert.assertNotEquals("not found: " + p + ", " + ArrayUtil.toString(f.getHeader()), f.getColumnIndex(p),
					-1);
			if (ArrayUtil.indexOf(f.getColumn(p), null) == -1)
				nonMissingValueProperties.add(p);
			else
				System.err.println("missing: " + p);
		}

		System.err.println("csv checked! " + nonMissingValueProps.length + " " + potentiallyMissingValueProps.length);
		return ListUtil.toArray(String.class, nonMissingValueProperties);
	}

	public void verifyExportResultSDF(String sdfFile, int numCompounds, String nonMissingValueProps[])
	{
		String s[] = SDFUtil.readSdf(sdfFile);
		Assert.assertEquals(numCompounds, s.length);
		for (int i = 0; i < s.length; i++)
			for (String p : nonMissingValueProps)
				Assert.assertTrue("not found: " + p + "\n" + s[i], s[i].indexOf("<" + p + ">") != -1);
		System.err.println("sdf checked! " + nonMissingValueProps.length);
	}
}
