package test;

import gui.DatasetWizardPanel;
import gui.LaunchCheSMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import main.CheSMapping;
import main.ScreenSetup;
import main.Settings;

import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.geometry.alignment.MultiKabschAlignement;
import org.openscience.cdk.isomorphism.MCSComputer;

import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.TestUtil.CDKPropertiesCreator;
import util.TestUtil.CDKStructuralPropertiesCreator;
import util.TestUtil.CoinFlipPropertySetFilter;
import util.TestUtil.InternalPropertiesCreator;
import util.TestUtil.MoleculePropertySetCreator;
import util.TestUtil.MoleculePropertySetFilter;
import util.TestUtil.NoPropertySetFilter;
import util.TestUtil.OBDescriptorCreator;
import util.TestUtil.OBStructuralPropertiesCreator;
import util.TestUtil.StructuralPropertiesCreator;
import util.TimeFormatUtil;
import alg.Algorithm;
import alg.AlgorithmException.ClusterException;
import alg.AlgorithmException.EmbedException;
import alg.align3d.MaxFragAligner;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.build3d.UseOrigStructures;
import alg.cluster.DatasetClusterer;
import alg.cluster.ManualClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.r.TSNEFeature3DEmbedder;
import data.ClusteringData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertyUtil;

public class MappingTest
{
	public static String DATA_DIR = "data/";
	static Random random;

	static
	{
		long seed = new Random().nextLong();
		System.err.println("seed: " + seed);
		random = new Random(seed);

		LaunchCheSMapper.init(Locale.US, ScreenSetup.DEFAULT, false);

		Settings.CACHING_ENABLED = true;
		MCSComputer.DEBUG = false;
		MultiKabschAlignement.DEBUG = false;
	}

	static String FILES[] = new String[] { "PBDE_LogVP.ob3d.sdf", "chang.sdf", "heyl.sdf", "caco2.sdf",
			"NCTRER_v4b_232_15Feb2008.sdf", "bbp2.csv", "1compound.sdf", "3compounds.sdf",
			"http://apps.ideaconsult.net:8080/ambit2/dataset/272?max=50", "2compounds_inchi.csv",
			"manual_cluster_distinct.csv", "manual_cluster_nondistinct.csv" };
	static ThreeDBuilder BUILDERS[] = ThreeDBuilder.BUILDERS;
	static MoleculePropertySetCreator FEATURE_SETS[] = new MoleculePropertySetCreator[] {
			new InternalPropertiesCreator(), new CDKPropertiesCreator(), new OBDescriptorCreator(),
			new OBStructuralPropertiesCreator(), new CDKStructuralPropertiesCreator() };
	static MoleculePropertySetFilter FEATURE_FILTER[] = new MoleculePropertySetFilter[] { new NoPropertySetFilter(),
			new CoinFlipPropertySetFilter(random) };
	static DatasetClusterer CLUSTERERS[] = DatasetClusterer.CLUSTERERS;
	static ThreeDEmbedder EMBEDDERS[] = ThreeDEmbedder.EMBEDDERS;
	static ThreeDAligner ALIGNERS[] = ThreeDAligner.ALIGNER;
	static boolean RANDOM_BUILD_ALIGN = false;
	static long MAX_RUNTIME = 2 * 60 * 60 * 1000; // 2 hours is max-runtime
	//static long MAX_RUNTIME = 1 * 60 * 1000; // 1 minute

	static DoubleKeyHashMap<String, DatasetClusterer, int[]> clusterResult = new DoubleKeyHashMap<String, DatasetClusterer, int[]>();
	static
	{
		clusterResult.put("manual_cluster_distinct.csv", ManualClusterer.INSTANCE, new int[] { 2, 7 });
		clusterResult.put("manual_cluster_nondistinct.csv", ManualClusterer.INSTANCE, new int[] { 6, 3, 3, 1 });
	}

	static
	{
		//BASIC
		//		//		FILES = new String[] { "basicTestSet.sdf" };
		BUILDERS = new ThreeDBuilder[] { UseOrigStructures.INSTANCE };
		EMBEDDERS = ArrayUtil.remove(ThreeDEmbedder.class, EMBEDDERS,
				new ThreeDEmbedder[] { TSNEFeature3DEmbedder.INSTANCE });
		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new OBDescriptorCreator() };
		//		FEATURE_FILTER = new MoleculePropertySetFilter[] { new CoinFlipPropertySetFilter(random) };
		//		//CLUSTERERS = new DatasetClusterer[] { WekaClusterer.WEKA_CLUSTERER[0] };
		//		CLUSTERERS = new DatasetClusterer[] { ManualClusterer.INSTANCE };
		//		EMBEDDERS = new ThreeDEmbedder[] { WekaPCA3DEmbedder.INSTANCE };
		//		ALIGNERS = new ThreeDAligner[] { NoAligner.INSTANCE };
		//		RANDOM_BUILD_ALIGN = false;

		//CUSTOM
		//		//				FILES = new String[] { "caco2.sdf" };
		//		FILES = new String[] { "10compounds.sdf" };//, "1compound.sdf", "3compounds.sdf", "heyl.sdf", "chang.sdf", "caco2.sdf" };
		//		//		FILES = new String[] {  "heyl.sdf" };
		//		//FILES = new String[] { "chang.sdf", "heyl.sdf" };
		//		BUILDERS = new ThreeDBuilder[] { UseOrigStructures.INSTANCE };
		//		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new InternalPropertiesCreator() };
		//		//		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new InternalPropertiesCreator(), new CDKPropertiesCreator(),
		//		//		//		//				new OBStructuralPropertiesCreator() };
		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new CDKPropertiesCreator(Subset.WithoutIonizationPotential) };
		//		//		//		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new StructuralPropertiesCreator() };
		//		//		//		FEATURE_FILTER = new MoleculePropertySetFilter[] { new CoinFlipPropertySetFilter() };
		//		FEATURE_FILTER = new MoleculePropertySetFilter[] { new NoPropertySetFilter() };
		//		//		//		//		//CLUSTERERS = new DatasetClusterer[] { ClusterWizardPanel.CLUSTERERS[2] }; // cascadeK
		//		//		CLUSTERERS = new DatasetClusterer[] { new NoClusterer() };
		//		//CLUSTERERS = new DatasetClusterer[] { ClusterWizardPanel.getDefaultClusterer() };
		//		//		CLUSTERERS = new DatasetClusterer[] { ClusterWizardPanel.getDefaultClusterer() };
		//		CLUSTERERS = new DatasetClusterer[] { DatasetClusterer.CLUSTERERS[8] };
		//		//		//		CLUSTERERS = new DatasetClusterer[] { WekaClusterer.WEKA_CLUSTERER[0] };
		//		//EMBEDDERS = new ThreeDEmbedder[] { EmbedWizardPanel.getDefaultEmbedder() };
		//		//		EMBEDDERS = new ThreeDEmbedder[] { new Random3DEmbedder() };
		//		EMBEDDERS = new ThreeDEmbedder[] { Sammon3DEmbedder.INSTANCE };
		//		//		//		EMBEDDERS = new ThreeDEmbedder[] { EMBEDDERS[1] }; // weka pca
		//		ALIGNERS = new ThreeDAligner[] { NoAligner.INSTANCE };
		//		//		//		//		ALIGNERS = new ThreeDAligner[] { new MaxFragAligner() };
		//		//		ALIGNERS = new ThreeDAligner[] { new MCSAligner() };
	}

	@Test
	public void testDoMapping()
	{
		//TaskPanel.PRINT_VERBOSE_MESSAGES = true;

		long start = System.currentTimeMillis();

		int n = FILES.length * FEATURE_SETS.length * FEATURE_FILTER.length * CLUSTERERS.length * EMBEDDERS.length;
		if (!RANDOM_BUILD_ALIGN)
			n *= BUILDERS.length * ALIGNERS.length;

		int indices[] = new int[n];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;
		ArrayUtil.scramble(indices, random);

		for (int k = 0; k < n; k++)
		{
			System.err.println("t> runtime " + TimeFormatUtil.format(System.currentTimeMillis() - start));
			if (System.currentTimeMillis() - start > MAX_RUNTIME)
			{
				System.out.println("max-runtime exceeded");
				break;
			}

			int i = indices[k];

			int j = i;
			int max = n / FILES.length;
			int fileIndex = j / max;

			j = i % max;
			max = max / (RANDOM_BUILD_ALIGN ? 1 : BUILDERS.length);
			int builderIndex = RANDOM_BUILD_ALIGN ? random.nextInt(BUILDERS.length) : (j / max);

			j = i % max;
			max = max / FEATURE_SETS.length;
			int featureIndex = j / max;

			j = i % max;
			max = max / FEATURE_FILTER.length;
			int filterIndex = j / max;

			j = i % max;
			max = max / CLUSTERERS.length;
			int clusterIndex = j / max;

			j = i % max;
			max = max / EMBEDDERS.length;
			int embedIndex = j / max;

			j = i % max;
			max = max / (RANDOM_BUILD_ALIGN ? 1 : ALIGNERS.length);
			int alignerIndex = RANDOM_BUILD_ALIGN ? random.nextInt(ALIGNERS.length) : (j / max);

			System.err.println("\n\n\nt> " + (k + 1) + "/" + n);
			System.err.println("t> " + fileIndex + " " + builderIndex + " " + featureIndex + " " + filterIndex + " "
					+ clusterIndex + " " + embedIndex + " " + alignerIndex);

			final String file = FILES[fileIndex];
			ThreeDBuilder builder = BUILDERS[builderIndex];
			MoleculePropertySetCreator featureCreator = FEATURE_SETS[featureIndex];
			MoleculePropertySetFilter featureFilter = FEATURE_FILTER[filterIndex];
			DatasetClusterer clusterer = CLUSTERERS[clusterIndex];
			ThreeDEmbedder embedder = EMBEDDERS[embedIndex];
			ThreeDAligner aligner = ALIGNERS[alignerIndex];

			System.err.println("t> " + file);
			System.err.println("t> " + ((Algorithm) builder).getName());
			System.err.println("t> " + featureCreator.getClass().getSimpleName());
			System.err.println("t> " + featureFilter.getClass().getSimpleName());
			System.err.println("t> " + ((Algorithm) clusterer).getName());
			System.err.println("t> " + ((Algorithm) embedder).getName());
			System.err.println("t> " + ((Algorithm) aligner).getName());

			if (aligner instanceof MaxFragAligner && !(featureCreator instanceof StructuralPropertiesCreator))
			{
				if (RANDOM_BUILD_ALIGN)
				{
					System.err.println("t> replace with no-aligner: no structural features for max-frag alignment");
					aligner = NoAligner.INSTANCE;
				}
				else
				{
					System.err.println("t> skipping: no structural features for max-frag alignment");
					continue;
				}
			}

			DatasetWizardPanel datasetProvider = new DatasetWizardPanel(false);
			//datasetProvider.load(CheSMappingTest.class.getResource("data/" + file).getFile());
			if (file.startsWith("http"))
				datasetProvider.load(file, true);
			else
				datasetProvider.load(DATA_DIR + file, true);
			Assert.assertEquals(datasetProvider.getDatasetFile().getFullName(), file);

			if (clusterer instanceof ManualClusterer
					&& datasetProvider.getDatasetFile().getIntegratedClusterProperty() == null)
			{
				System.err.println("t> skipping: no cluster feature for manual clustering");
				continue;
			}
			//			DatasetProvider datasetProvider = new DatasetProvider()
			//			{
			//				@Override
			//				public DatasetFile getDatasetFile()
			//				{
			//					return DatasetFile.localFile(CheSMappingTest.class.getResource(file).getFile());
			//				}
			//			};
			//			try
			//			{
			//				datasetProvider.getDatasetFile().loadDataset();
			//			}
			//			catch (Exception e)
			//			{
			//				e.printStackTrace();
			//				Assert.fail("could not load dataset" + e);
			//			}

			CompoundPropertySet[] featureSet = featureFilter.filterSet(featureCreator.getSet(datasetProvider
					.getDatasetFile()));

			System.err.println("t> do mapping");
			System.err.println("t> features: " + ArrayUtil.toString(featureSet));

			for (boolean cachingEnabled : new boolean[] { false, true })
			{
				System.err.println("t> caching enabled: " + cachingEnabled);
				Settings.CACHING_ENABLED = cachingEnabled;

				CheSMapping ch = new CheSMapping(datasetProvider.getDatasetFile(), featureSet, clusterer, builder,
						embedder, aligner);
				ClusteringData clustering = ch.doMapping();

				if (clustering == null)
				{
					// this should never happen
					Assert.assertNotNull(ch.getMappingError());
					Assert.fail();
				}
				else
				{
					List<CompoundProperty> featuresWithInfo = new ArrayList<CompoundProperty>();
					for (CompoundProperty p : clustering.getFeatures())
						if (!CompoundPropertyUtil.hasUniqueValue(p, datasetProvider.getDatasetFile()))
							featuresWithInfo.add(p);

					if (ch.getClusterException() != null)
					{
						Assert.assertEquals(clustering.getClusterAlgorithm(), NoClusterer.getNameStatic());
						Assert.assertTrue(ch.getClusterException() instanceof ClusterException);
					}
					else if (!clusterer.getName().equals(clustering.getClusterAlgorithm()))
					{
						Assert.assertEquals(clustering.getClusterAlgorithm(), NoClusterer.getNameStatic());
						Assert.assertTrue(clustering.getCompounds().size() == 1
								|| (clusterer.requiresFeatures() && featuresWithInfo.size() == 0));
					}
					if (clusterResult.get(file, clusterer) != null)
					{
						int res[] = clusterResult.get(file, clusterer);
						Assert.assertEquals(clustering.getNumClusters(), res.length);
						for (int l = 0; l < res.length; l++)
							Assert.assertEquals(clustering.getCluster(l).getSize(), res[l]);
					}

					if (ch.getEmbedException() != null)
					{
						Assert.assertEquals(clustering.getEmbedAlgorithm(), Random3DEmbedder.getNameStatic());
						Assert.assertTrue(ch.getEmbedException() instanceof EmbedException);
					}
					else if (!embedder.getName().equals(clustering.getEmbedAlgorithm()))
					{
						Assert.assertEquals(clustering.getEmbedAlgorithm(), Random3DEmbedder.getNameStatic());
						Assert.assertTrue(clustering.getCompounds().size() == 1
								|| (embedder.requiresFeatures() && featuresWithInfo.size() == 0));
					}

					if (ch.getAlignException() != null)
					{
						Assert.fail();
					}

					Assert.assertTrue(clustering.getNumClusters() >= 1);
					Assert.assertTrue(clustering.getNumClusters() == clustering.getClusters().size());
				}
			}
		}
	}
}
