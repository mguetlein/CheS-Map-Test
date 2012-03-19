package runtime;

import gui.DatasetWizardPanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import main.BinHandler;
import main.PropHandler;
import main.Settings;
import main.TaskProvider;

import org.junit.Assert;
import org.openscience.cdk.isomorphism.MCSComputer;

import util.ListUtil;
import util.RuntimeUtil;
import util.RuntimeUtil.AlgorithmWrapper;
import util.RuntimeUtil.WrappedFeatureComputer;
import util.TestUtil.CDKPropertiesCreator;
import util.TestUtil.CDKPropertiesCreator.Subset;
import util.TestUtil.OBFingerprintCreator;
import util.ThreadUtil;
import alg.FeatureComputer;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.r.AbstractRClusterer;
import alg.embed3d.EmbedUtil;
import alg.embed3d.ThreeDEmbedder;
import data.ClusteringData;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertyUtil;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class RuntimeEval
{
	public static String DATA_DIR = "data/";
	public static String STATS_DIR = "stats/";

	static Random random;

	//	static String FILES[] = new String[] { "PBDE_LogVP.ob3d.sdf", "chang.sdf", "heyl.sdf", "caco2.sdf",
	//			"NCTRER_v4b_232_15Feb2008.sdf", "bbp2.csv", "1compound.sdf", "3compounds.sdf", "10compounds.sdf",
	//			"http://apps.ideaconsult.net:8080/ambit2/dataset/272?max=50" };
	//static String FILES[] = new String[] { "10compounds.sdf", "PBDE_LogVP.flat.sdf" };
	static String FILES[] = new String[] { "PBDE_LogVP.flat.sdf", "caco2.flat.sdf", "cox2.flat.sdf",
			"CPDBAS_v5d_1547_20Nov2008.ob.sdf" };
	//static String FILES[] = new String[] { "cox2.flat.sdf" };//, "cox2.flat.sdf" };
	//static String FILES[] = new String[] { "PBDE_LogVP.flat.sdf", "10compounds.sdf" };

	static
	{
		PropHandler.init(false);
		BinHandler.init();

		long seed = new Random().nextLong();
		System.err.println("seed: " + seed);
		random = new Random(seed);
	}

	static enum Mode
	{
		threeD, computeFeatures, cluster, cluster_ob, embed, embed_ob, align;
	}

	static Mode mode = Mode.computeFeatures;
	static boolean read = false;
	static boolean runSlowAlgorithms = false;

	static
	{
		//		TaskPanel.PRINT_VERBOSE_MESSAGES = true;
		MCSComputer.DEBUG = false;
		Settings.CACHING_ENABLED = false;
	}

	static ResultSet resultSet = new ResultSet();

	static int result = -1;

	public static void runBuilder(AlgorithmWrapper alg, DatasetFile dataset)
	{
		long start = System.currentTimeMillis();
		//				if (builder instanceof CDK3DBuilder)
		//				{
		//					for (String ff : CDK3DBuilder.FORCEFIELDS)
		//					{
		//						builder.getProperties()[0].setValue(ff);
		//						builder.build3D(dataset);
		//						resultSet.setResultValue(result, builder.getName() + " " + ff, System.currentTimeMillis()
		//								- start);
		//					}
		//				}
		//				else
		//				{
		((ThreeDBuilder) alg.get()).build3D(dataset);
		resultSet.setResultValue(result, alg.getName(), (dataset != null ? System.currentTimeMillis() - start : null));
		//				}
	}

	public static void computeFeatures(AlgorithmWrapper alg, DatasetFile dataset)
	{
		long start = System.currentTimeMillis();
		MoleculePropertySet[] featureSet = ((WrappedFeatureComputer) alg).getSet(dataset);
		FeatureComputer featureComputer = new DefaultFeatureComputer(featureSet);
		featureComputer.computeFeatures(dataset);
		resultSet.setResultValue(result, alg.getName(), System.currentTimeMillis() - start);
	}

	private static List<MoleculeProperty> featuresFirst(DatasetFile dataset, boolean cdkFeatures,
			ClusteringData clustering)
	{
		Settings.CACHING_ENABLED = true;
		FeatureComputer featureComputer;
		int minFrequency = -1;
		if (cdkFeatures)
		{
			featureComputer = new DefaultFeatureComputer(
					new CDKPropertiesCreator(Subset.WithoutIonizationPotential).getSet(dataset));
		}
		else
		{
			minFrequency = Math.max(1, (int) (dataset.numCompounds() * 0.05));
			featureComputer = new DefaultFeatureComputer(new OBFingerprintCreator(true, minFrequency).getSet(dataset));
		}
		featureComputer.computeFeatures(dataset);
		if (featureComputer.getFeatures().size() == 0)
			throw new Error("WTF");

		resultSet.setResultValue(result, "Features", featureComputer.getFeatures().size()
				+ (cdkFeatures ? " CDK" : (" LinFrag (f=" + minFrequency) + ")"));

		for (MoleculeProperty f : featureComputer.getFeatures())
			clustering.addFeature(f);
		for (MoleculeProperty p : featureComputer.getProperties())
			clustering.addProperty(p);
		for (CompoundData c : featureComputer.getCompounds())
			clustering.addCompound(c);
		List<MoleculeProperty> featuresWithInfo = new ArrayList<MoleculeProperty>();
		for (MoleculeProperty p : clustering.getFeatures())
			if (!MoleculePropertyUtil.hasUniqueValue(p, dataset))
				featuresWithInfo.add(p);
		Settings.CACHING_ENABLED = false;
		return featuresWithInfo;
	}

	public static void clusterDataset(AlgorithmWrapper alg, DatasetFile dataset, boolean cdkFeatures) throws Exception
	{
		String name = alg.getName();
		ClusteringData clustering = new ClusteringData(dataset.getName(), dataset.getFullName(),
				dataset.getSDFPath(true));
		List<MoleculeProperty> features = featuresFirst(dataset, cdkFeatures, clustering);
		long start = System.currentTimeMillis();
		((DatasetClusterer) alg.get()).clusterDataset(dataset, clustering.getCompounds(), features);
		resultSet.setResultValue(result, name, System.currentTimeMillis() - start);
	}

	public static void embedDataset(AlgorithmWrapper alg, DatasetFile dataset, boolean cdkFeatures) throws Exception
	{
		String name = alg.getName();
		ClusteringData clustering = new ClusteringData(dataset.getName(), dataset.getFullName(),
				dataset.getSDFPath(true));
		List<MoleculeProperty> features = featuresFirst(dataset, cdkFeatures, clustering);
		long start = System.currentTimeMillis();

		ThreeDEmbedder emb = (ThreeDEmbedder) alg.get();

		emb.embedDataset(dataset, ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds()), features);
		resultSet.setResultValue(result, name, System.currentTimeMillis() - start);

		double rSquare = EmbedUtil.computeRSquare(
				ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds()), features, emb.getPositions(),
				dataset);
		resultSet.setResultValue(result, name + " r²", rSquare);
	}

	private static DatasetClusterer alignClusterer = AbstractRClusterer.R_CLUSTERER[3];

	//private static DatasetClusterer alignClusterer = ClusterWizardPanel.getDefaultClusterer();
	//	static
	//	{
	//		PropertyUtil.getProperty(alignClusterer.getProperties(), "minNumClusters").setValue(new Integer(4));
	//		PropertyUtil.getProperty(alignClusterer.getProperties(), "maxNumClusters").setValue(new Integer(10));
	//	}

	public static void alignDataset(AlgorithmWrapper alg, DatasetFile dataset) throws Exception
	{
		ClusteringData clustering = new ClusteringData(dataset.getName(), dataset.getFullName(),
				dataset.getSDFPath(true));
		List<MoleculeProperty> features = featuresFirst(dataset, false, clustering);

		Settings.CACHING_ENABLED = true;
		alignClusterer.clusterDataset(dataset, clustering.getCompounds(), features);
		for (ClusterData c : alignClusterer.getClusters())
			clustering.addCluster(c);
		clustering.setClusterAlgorithm(alignClusterer.getName());
		Settings.CACHING_ENABLED = false;

		resultSet.removePropery("Features");
		resultSet.setResultValue(result, "Cluster*", clustering.getClusters().size());

		//Settings.BABEL_BINARY.setLocation("/home/martin/opentox-ruby/openbabel-2.2.3/bin/babel");

		long start = System.currentTimeMillis();
		((ThreeDAligner) alg.get()).algin(dataset, clustering.getClusters(), clustering.getFeatures());
		resultSet.setResultValue(result, alg.getName(), System.currentTimeMillis() - start);
	}

	public static void print()
	{
		resultSet.setNicePropery("Compounds", "Size");

		resultSet.setNicePropery("CDK 3D Structure Generation", "CDK");
		resultSet.setNicePropery("OpenBabel 3D Structure Generation", "OpenBabel");

		resultSet.setNicePropery("CDKPropertiesCreator Constitutional", "CDK constitutional");
		resultSet.setNicePropery("CDKPropertiesCreator WithoutIonizationPotential", "CDK all");
		resultSet.setNicePropery("OBFingerprintCreator", "All OpenBabel Fingerprints");
		resultSet.setNicePropery("OBCarcMutRulesCreator", "52 Smarts OpenBabel");
		resultSet.setNicePropery("CDKCarcMutRulesCreator", "52 Smarts CDK OLD");
		resultSet.setNicePropery("CDKCarcMutRulesCreator2", "52 Smarts CDK");
		resultSet.setNicePropery("OBDescriptorCreator", "OpenBabel Descriptors");
		resultSet.setNicePropery("CDKFingerprintCreator", "CDK Bio-Activity Fragments");

		resultSet.movePropertyBack("OBFingerprintCreator");
		resultSet.movePropertyBack("CDKFingerprintCreator");
		resultSet.movePropertyBack("OBCarcMutRulesCreator");
		resultSet.movePropertyBack("CDKCarcMutRulesCreator");
		resultSet.movePropertyBack("CDKCarcMutRulesCreator2");

		resultSet.removePropery("CDKCarcMutRulesCreator");

		resultSet.setNicePropery("Hierarchical (WEKA)", "Hierarch (WEKA)");
		resultSet.setNicePropery("Hierarchical (R)", "Hierarch (R)");
		resultSet.setNicePropery("Hierarchical - Dynamic Tree Cut (R)", "Hierarch - Dynamic Tree Cut (R)");
		resultSet.setNicePropery("FarthestFirst (WEKA)", "Farthest First (WEKA)");
		resultSet.setNicePropery("SimpleKMeans (WEKA)", "Simple k-Means (WEKA)");
		resultSet.setNicePropery("k-Means - Cascade (WEKA)", "k-Means - Cascade (WEKA) *");
		resultSet.setNicePropery("k-Means - Cascade (WEKA) 3-5 r3", "k-Means - Cascade (WEKA) **");
		resultSet.setNicePropery("Expectation Maximization (WEKA)", "EM (WEKA) ***");
		resultSet.setNicePropery("Expectation Maximization (WEKA) 5", "EM (WEKA) ****");

		//		resultSet.setNicePropery("TSNE 3D Embedder (R) 1000", "TSNE 3D Embedder (R)*");
		//		resultSet.setNicePropery("TSNE 3D Embedder (R) 200", "TSNE 3D Embedder (R)**");
		resultSet.setNicePropery("SMACOF 3D Embedder (R) 150", "SMACOF 3D Embedder (R)*");
		resultSet.setNicePropery("SMACOF 3D Embedder (R) 30", "SMACOF 3D Embedder (R)**");

		resultSet.setNicePropery("PCA 3D Embedder (WEKA) r²", "r²");
		resultSet.setNicePropery("PCA 3D Embedder (R) r²", "r²");
		resultSet.setNicePropery("Sammon 3D Embedder (R) r²", "r²");
		resultSet.setNicePropery("SMACOF 3D Embedder (R) 150 r²", "r²");
		resultSet.setNicePropery("SMACOF 3D Embedder (R) 30 r²", "r²");
		resultSet.setNicePropery("SMACOF 3D Embedder (R) 10 r²", "r²");

		resultSet.removePropery("SMACOF 3D Embedder (R) 10 r²");
		resultSet.removePropery("SMACOF 3D Embedder (R) 10");

		resultSet.movePropertyBack("SMACOF 3D Embedder (R) 30");
		resultSet.movePropertyBack("SMACOF 3D Embedder (R) 30 r²");

		for (String p : resultSet.getProperties())
		{
			if (p.equals("Compounds") || p.equals("Cluster*"))
				resultSet.toInt(p);
			else if (!p.equals("Dataset") && !p.equals("Features") && !p.equals("Cluster*") && !p.contains("r²"))
				resultSet.toLong(p);
		}

		for (int i = 0; i < resultSet.getNumResults(); i++)
		{
			String s = resultSet.getResultValue(i, "Dataset").toString();
			if (s.indexOf(".") != -1)
				resultSet.setResultValue(i, "Dataset", s.substring(0, s.indexOf(".")));

			s = resultSet.getResultValue(i, "Dataset").toString();
			if (s.indexOf("_") != -1)
				resultSet.setResultValue(i, "Dataset", s.substring(0, s.indexOf("_")));
		}

		System.out.println();
		System.out.println(resultSet.toNiceString(0, true, true));
		System.out.println();
		System.out.println(resultSet.toMediaWikiString(true, true, true));
	}

	private static File resultSetFile = new File(STATS_DIR + mode + ".txt");

	private static void save()
	{
		ResultSetIO.printToFile(resultSetFile, resultSet, true);
	}

	public static void main(String args[])
	{
		Locale.setDefault(Locale.US);

		if (resultSetFile.exists())
			resultSet = ResultSetIO.parseFromFile(resultSetFile);
		else
			resultSet = new ResultSet();

		if (!read)
		{
			TaskProvider.registerThread("Ches-Mapper-Task");
			TaskProvider.task().getPanel();
			//TaskPanel.PRINT_VERBOSE_MESSAGES = true;

			for (String file : FILES)
			{

				System.err.println("> dataset " + file);

				DatasetWizardPanel datasetProvider = new DatasetWizardPanel(null);
				//datasetProvider.load(CheSMappingTest.class.getResource("data/" + file).getFile());
				datasetProvider.load(DATA_DIR + file);
				ThreadUtil.sleep(100);
				while (datasetProvider.getDatasetFile() == null || datasetProvider.isLoading())
					ThreadUtil.sleep(100);
				Assert.assertEquals(datasetProvider.getDatasetFile().getFullName(), file);
				DatasetFile dataset = datasetProvider.getDatasetFile();
				if (mode != Mode.threeD)
					dataset.setSDFPath(dataset.getSDFPath(false), true);

				result = -1;
				for (int r = 0; r < resultSet.getNumResults(); r++)
				{
					if (resultSet.getResultValue(r, "Dataset").equals(dataset.getName()))
					{
						result = r;
						if (((int) Double.parseDouble(resultSet.getResultValue(r, "Compounds").toString())) != dataset
								.numCompounds())
							throw new Error(resultSet.getResultValue(r, "Compounds") + " != " + dataset.numCompounds());
						break;
					}
				}
				if (result == -1)
				{
					result = resultSet.addResult();
					resultSet.setResultValue(result, "Dataset", dataset.getName());
					resultSet.setResultValue(result, "Compounds", dataset.numCompounds());
				}

				AlgorithmWrapper[] algs = null;
				if (mode == Mode.threeD)
					algs = RuntimeUtil.BUILDERS;
				else if (mode == Mode.computeFeatures)
					algs = RuntimeUtil.FEATURE_SETS;
				else if (mode == Mode.cluster || mode == Mode.cluster_ob)
					algs = RuntimeUtil.CLUSTERERS;
				else if (mode == Mode.embed || mode == Mode.embed_ob)
					algs = RuntimeUtil.EMBEDDERS;
				else if (mode == Mode.align)
					algs = RuntimeUtil.ALIGNERS;

				for (AlgorithmWrapper alg : algs)
				{
					String name = alg.getName();
					if (resultSet.getResultValue(result, name) != null
							&& !resultSet.getResultValue(result, name).equals("null"))
					{
						System.err.println("> already computed " + alg.getName());
					}
					else if (!alg.isFeasible(dataset.numCompounds()))
					{
						System.err.println("> not feasible " + alg.getName());
					}
					else if (alg.isSlow(dataset.numCompounds()) && !runSlowAlgorithms)
					{
						System.err.println("> too slow " + alg.getName());
					}
					else
					{
						System.err.println("> run " + alg.getName());
						try
						{
							switch (mode)
							{
								case threeD:
									runBuilder(alg, dataset);
									break;
								case computeFeatures:
									computeFeatures(alg, dataset);
									break;
								case cluster:
									clusterDataset(alg, dataset, true);
									break;
								case cluster_ob:
									clusterDataset(alg, dataset, false);
									break;
								case embed:
									embedDataset(alg, dataset, true);
									break;
								case embed_ob:
									embedDataset(alg, dataset, false);
									break;
								case align:
									alignDataset(alg, dataset);
									break;
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					save();
				}
			}
		}

		System.err.flush();
		print();
	}
}
