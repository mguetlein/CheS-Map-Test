package test;

import gui.LaunchCheSMapper;
import gui.property.Property;
import io.SDFUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import main.BinHandler;
import main.CheSMapping;
import main.Settings;

import org.junit.Assert;
import org.junit.Test;

import property.CDKFingerprintSet;
import property.FminerPropertySet;
import property.OBFingerprintSet;
import property.PropertySetProvider;
import property.PropertySetProvider.PropertySetShortcut;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.ObjectUtil;
import workflow.ClustererProvider;
import workflow.DatasetLoader;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import alg.Algorithm;
import alg.align3d.NoAligner;
import alg.build3d.UseOrigStructures;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
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
import dataInterface.FragmentPropertySet;

public class MappingAndExportTest
{
	static
	{
		LaunchCheSMapper.init();
		LaunchCheSMapper.setExitOnClose(false);
		String version = BinHandler.getOpenBabelVersion();
		if (!version.equals("2.3.2"))
			throw new IllegalStateException("tests require obenbabel version 2.3.2, is: " + version);
	}

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

	static class Feature
	{
		String shortName;
		String featureNames[];
		Integer minFreq;
		private MatchEngine matchEngine = MatchEngine.OpenBabel;
		HashMap<Dataset, Integer> numFragments = new HashMap<Dataset, Integer>();

		@Override
		public String toString()
		{
			String s = shortName;
			if (minFreq != null)
				s += " f:" + minFreq + " m:" + matchEngine;
			return s;
		}

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

	final static Dataset datasets[];
	final static DatasetClusterer clusterers[];
	final static ThreeDEmbedder embedders[];
	final static PropertySetShortcut featureTypes[];
	final static int minFreq[];
	final static MatchEngine matchEngines[];
	static boolean caching[];
	final static DoubleKeyHashMap<Algorithm, String, Object> algorithmProps = new DoubleKeyHashMap<Algorithm, String, Object>();
	{
		algorithmProps.put(new ClustererProvider().getYesAlgorithm(), "minNumClusters", 3);
		algorithmProps.put(DynamicTreeCutHierarchicalRClusterer.INSTANCE,
				"Minimum number of compounds in each cluster (minClusterSize)", 2);
		algorithmProps.put(Random3DEmbedder.INSTANCE, "Random seed", 2);
		algorithmProps.put(Sammon3DEmbedder.INSTANCE, "Maximum number of iterations (niter)", 10);
	}

	static
	{//complete
		if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.single)
		{
			datasets = new Dataset[] { D_SMI };
			clusterers = new DatasetClusterer[] { NoClusterer.INSTANCE };
			embedders = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE };
			featureTypes = new PropertySetShortcut[] { PropertySetShortcut.ob };
			minFreq = null;
			matchEngines = null;
			caching = new boolean[] { false };
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.all)
		{
			datasets = new Dataset[] { D_SMI, D_INCHI, D_SDF, D_CSV };
			clusterers = new DatasetClusterer[] { NoClusterer.INSTANCE, new ClustererProvider().getYesAlgorithm(),
					DynamicTreeCutHierarchicalRClusterer.INSTANCE };
			embedders = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE, WekaPCA3DEmbedder.INSTANCE_NO_PROBS,
					Sammon3DEmbedder.INSTANCE };
			featureTypes = PropertySetShortcut.values();
			minFreq = new int[] { 0, 1, 2 };
			matchEngines = new MatchEngine[] { MatchEngine.OpenBabel, MatchEngine.CDK };
			caching = new boolean[] { false, false, true };
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.debug)
		{
			datasets = new Dataset[] { D_INCHI };
			//datasets = new Dataset[] { D_SMI, D_INCHI, D_SDF, D_CSV };
			clusterers = new DatasetClusterer[] { new ClustererProvider().getYesAlgorithm() };
			embedders = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE };
			featureTypes = new PropertySetShortcut[] { PropertySetShortcut.integrated };
			//		featureTypes = new PropertySetShortcut[] { PropertySetShortcut.cdkFunct, PropertySetShortcut.obFP2,
			//				PropertySetShortcut.obFP3, PropertySetShortcut.obFP4, PropertySetShortcut.obMACCS,
			//				PropertySetShortcut.benigniBossa };
			minFreq = new int[] { 0, 1, 2 };
			matchEngines = new MatchEngine[] { MatchEngine.OpenBabel, MatchEngine.CDK };
			caching = new boolean[] { false, false, true };
		}
		else
			throw new IllegalStateException();
	}

	static List<Feature> features = new ArrayList<Feature>();

	static
	{
		//		for (PropertySetShortcuts c : new PropertySetShortcuts[] { PropertySetShortcuts.ob,
		//				PropertySetShortcuts.cdk })
		for (PropertySetShortcut c : featureTypes)
		{
			if (c == PropertySetShortcut.fminer)
				continue;

			List<String> props = new ArrayList<String>();
			CompoundPropertySet sets[] = null;
			if (c != PropertySetShortcut.integrated)
			{
				sets = PropertySetProvider.INSTANCE.getDescriptorSets(null, c);
				for (CompoundPropertySet set : sets)
				{
					if ((c == PropertySetShortcut.cdk || c == PropertySetShortcut.ob) && set.getType() != Type.NUMERIC)
						continue;
					if (!set.isSizeDynamic())
						for (int i = 0; i < set.getSize(null); i++)
							props.add(CompoundPropertyUtil.propToExportString(set.get(null, i)));
				}
			}
			if (sets == null || !(sets[0] instanceof FragmentPropertySet))
			{
				features.add(new Feature(c.toString(), ListUtil.toArray(String.class, props)));
			}
			else
			{
				for (int minF : minFreq)
				{
					for (MatchEngine matchE : matchEngines)
					{
						if (sets[0] instanceof CDKFingerprintSet && matchE == MatchEngine.OpenBabel)
							continue;
						if (sets[0] instanceof FminerPropertySet && matchE == MatchEngine.CDK)
							continue;
						if (sets[0] instanceof OBFingerprintSet && matchE == MatchEngine.CDK)
							continue;

						Feature f = new Feature(c.toString(), ListUtil.toArray(String.class, props), minF, matchE);
						features.add(f);

						if (c == PropertySetShortcut.benigniBossa)
						{
							if (minF == 0)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
									f.numFragments.put(D_SMI, 2);
									f.numFragments.put(D_CSV, 6);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_CSV, 3);
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
								}
							}
							else if (minF == 1)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
									f.numFragments.put(D_CSV, 6);
									f.numFragments.put(D_SMI, 2);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
									f.numFragments.put(D_CSV, 3);
								}
							}
							else if (minF == 2)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
								}
							}
						}
						else if (c == PropertySetShortcut.obFP2)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_CSV, 825);
								f.numFragments.put(D_SMI, 432);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_CSV, 825);
								f.numFragments.put(D_SMI, 432);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.obFP3)
						{
							if (minF == 0)
							{
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_INCHI, 8);
							}
							else if (minF == 2)
							{
								f.numFragments.put(D_INCHI, 8);
							}
						}
						else if (c == PropertySetShortcut.obFP4)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_INCHI, 19);
								f.numFragments.put(D_SMI, 48);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_INCHI, 19);
								f.numFragments.put(D_SMI, 48);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.obMACCS)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_SMI, 111);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_SMI, 111);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.cdkFunct)
						{
							if (minF == 0)
							{
								f.matchEngine = MatchEngine.CDK;
								f.numFragments.put(D_INCHI, 6);
								f.numFragments.put(D_CSV, 40);
							}
							else if (minF == 1)
							{
								f.matchEngine = MatchEngine.CDK;
								f.numFragments.put(D_SDF, 14);
								f.numFragments.put(D_INCHI, 6);
								f.numFragments.put(D_CSV, 38);
							}
							else if (minF == 2)
							{
							}
						}
					}
				}
			}
		}
	}

	public static Set<String> tmpfiles = new HashSet<String>();

	@Test
	public void test()
	{
		try
		{
			Random r = new Random();
			ArrayUtil.scramble(datasets, r);
			ListUtil.scramble(features, r);
			ArrayUtil.scramble(clusterers, r);
			ArrayUtil.scramble(embedders, r);

			HashMap<String, String> featureNames = new HashMap<String, String>();
			HashMap<String, String> positions = new HashMap<String, String>();
			HashMap<String, String> outfiles = new HashMap<String, String>();

			int max = caching.length * datasets.length * features.size() * clusterers.length * embedders.length;
			int count = 0;

			//boolean cache = false;
			int cacheIdx = 0;
			for (boolean cache : caching)
			{
				Settings.CACHING_ENABLED = cache;

				int dIdx = 0;
				for (Dataset data : datasets)
				{
					DatasetFile dataset = null;
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
								count++;
								String msg = count + "/" + max + ":\n ";
								msg += "cache (" + (cacheIdx + 1) + "/" + caching.length + "): " + cache + "\n ";
								msg += "data  (" + (dIdx + 1) + "/" + datasets.length + "): " + data.name + "\n ";
								msg += "feat  (" + (fIdx + 1) + "/" + features.size() + "): " + feat + "\n ";
								msg += "clust (" + (cIdx + 1) + "/" + clusterers.length + "): " + clust.getName()
										+ "\n ";
								msg += "emb   (" + (eIdx + 1) + "/" + embedders.length + "): " + emb.getName();
								System.err.println("\n================================================\n" + msg
										+ "\n------------------------------------------------");

								if ((clusterers.length == 1 && embedders.length == 1)
										|| (clust == NoClusterer.INSTANCE && emb != Random3DEmbedder.INSTANCE)
										|| (clust != NoClusterer.INSTANCE && emb == Random3DEmbedder.INSTANCE))
								{
									Assert.assertNull(Settings.TOP_LEVEL_FRAME);

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
									if (algorithmProps != null)
										for (Algorithm alg : algorithmProps.keySet1())
											for (Algorithm alg2 : new Algorithm[] { clust, emb })
												if (alg == alg2)
												{
													for (Property p : alg.getProperties())
													{
														if (algorithmProps.containsKeyPair(alg, p.getName()))
														{
															p.setValue(algorithmProps.get(alg, p.getName()));
															System.err.println("setting " + p.getName() + " of "
																	+ alg.getName() + " to " + p.getValue());
															break;
														}
													}
													break;
												}

									CheSMapping mapping;
									if (cacheIdx == 1)
									{
										// use the standard export/workflow way that stores to global props to have this settings available in the wizard
										Properties props = MappingWorkflow.createMappingWorkflow("data/" + data.name,
												feats, clust, emb);
										mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
										dataset = mapping.getDatasetFile();
									}
									else
									{
										// use direct way without props, both should yield equal results
										// (doing this instead of comparing mapping directly because algorithms are singletons)
										dataset = new DatasetLoader(false).load("data/" + data.name);
										mapping = new CheSMapping(dataset, ListUtil.toArray(CompoundPropertySet.class,
												feats.getFeatures(dataset)), clust, UseOrigStructures.INSTANCE, emb,
												NoAligner.INSTANCE);
									}
									System.err.println(dataset + " " + dataset.hashCode());

									ClusteringData clusteringData = mapping.doMapping();
									ClusteringImpl clustering = new ClusteringImpl();
									clustering.newClustering(clusteringData);

									// check features
									Assert.assertEquals(feats.getFeatures(dataset).size(), mapping.getNumFeatureSets());
									System.err.println(data.name + " " + feat.shortName + " " + feat.minFreq + " "
											+ feat.matchEngine + " num-features:" + clustering.getFeatures().size());
									if (feat.numFragments.containsKey(data))
										Assert.assertTrue(clustering.getFeatures().size() == feat.numFragments
												.get(data));
									else if (feat.shortName.equals(PropertySetShortcut.integrated.toString())
											&& ObjectUtil.equals(data.integratedFeature, ""))
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
													Assert.assertTrue(numFeatures.get(key, minFreq[i]) > numFeatures
															.get(key, minFreq[j]));
												else
													Assert.assertTrue(numFeatures.get(key, minFreq[i]) < numFeatures
															.get(key, minFreq[j]));
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
											|| (emb instanceof AbstractRTo3DEmbedder
													&& mapping.getEmbedException() != null
													&& clustering.getFeatures().size() <= 10 && mapping
													.getEmbedException().getMessage()
													.contains("Too few unique data points")))
										Assert.assertEquals(Random3DEmbedder.INSTANCE, usedEmb);
									else
										Assert.assertEquals("Embedding should have been performed with " + emb
												+ ", instead used: " + usedClust, emb, usedEmb);
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
									{
										featureNames.put(idxStr, ListUtil.toString(clustering.getFeatures()));
										positions.put(idxStr, ListUtil.toString(usedEmb.getPositions()));
									}
									else
									{
										System.err.println("checking that features and positions are equal " + idxStr);
										Assert.assertEquals(featureNames.get(idxStr),
												ListUtil.toString(clustering.getFeatures()));
										Assert.assertEquals(positions.get(idxStr),
												ListUtil.toString(usedEmb.getPositions()));
									}

									// check exporting result
									String nonMissingValueProps[] = new String[0];
									for (String output : new String[] { "csv", "sdf" })
									{
										String outfile = "/tmp/" + data.name + "." + cacheIdx + "." + idxStr + output;
										ExportData.scriptExport(clustering, outfile, true, 1.0);
										tmpfiles.add(outfile);
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
													ArrayUtil.concat(String.class, data.integratedNonMissing,
															clusterProp), ArrayUtil.concat(String.class,
															feat.featureNames, data.integratedMissing));
										else
											verifyExportResultSDF(outfile, data.size, nonMissingValueProps);
									}
								}
								else
								{
									System.err.println("skipping cluster - embedding combination");
								}
								eIdx++;
							}
							cIdx++;
						}
						fIdx++;
					}
					dIdx++;
					dataset.clear();
				}
				cacheIdx++;
			}
			System.err.println("\n" + count + "/" + max + " tests done");
		}
		finally
		{
			int count = 0;
			int delCount = 0;
			for (String f : tmpfiles)
			{
				try
				{
					if (new File(f).delete())
						delCount++;
					count++;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			System.err.println("deleted " + delCount + "/" + count + " tmp-files");
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
