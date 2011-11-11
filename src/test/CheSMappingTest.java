package test;

import gui.AlignWizardPanel;
import gui.Build3DWizardPanel;
import gui.ClusterWizardPanel;
import gui.DatasetWizardPanel;
import gui.EmbedWizardPanel;
import gui.TaskPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import main.CheSMapping;
import main.TaskProvider;

import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.isomorphism.MCSComputer;

import util.ArrayUtil;
import util.ThreadUtil;
import alg.Algorithm;
import alg.AlgorithmException.ClusterException;
import alg.AlgorithmException.EmbedException;
import alg.FeatureComputer;
import alg.align3d.MaxFragAligner;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.build3d.UseOrigStructures;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import data.ClusteringData;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import data.IntegratedProperty;
import data.cdk.CDKPropertySet;
import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;
import data.fragments.StructuralFragments;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertyUtil;

public class CheSMappingTest
{
	static Random random = new Random();

	static
	{
		long seed = random.nextLong();
		System.err.println("seed: " + seed);
		random = new Random(seed);
	}

	static String FILES[] = new String[] { "chang.sdf", "heyl.sdf", "caco2.sdf", "NCTRER_v4b_232_15Feb2008.sdf",
			"bbp2.csv", "1compound.sdf", "3compounds.sdf" };
	static ThreeDBuilder BUILDERS[] = Build3DWizardPanel.BUILDERS;
	static MoleculePropertySetCreator FEATURE_SETS[] = { new InternalPropertiesCreator(), new CDKPropertiesCreator(),
			new OBStructuralPropertiesCreator(), new CDKStructuralPropertiesCreator(),
			new RandomStructuralPropertiesCreator() };
	static MoleculePropertySetFilter FEATURE_FILTER[] = { new NoPropertySetFilter(), new CoinFlipPropertySetFilter() };
	static DatasetClusterer CLUSTERERS[] = ClusterWizardPanel.CLUSTERERS;
	static ThreeDEmbedder EMBEDDERS[] = EmbedWizardPanel.EMBEDDERS;
	static ThreeDAligner ALIGNERS[] = AlignWizardPanel.ALIGNER;

	static
	{
		MCSComputer.DEBUG = true;

		//		//		// modifications for smaller tests
		//		FILES = new String[] { "caco2.sdf" };
		FILES = new String[] { "heyl.sdf" };
		//FILES = new String[] { "chang.sdf", "heyl.sdf" };
		BUILDERS = new ThreeDBuilder[] { new UseOrigStructures() };
		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new InternalPropertiesCreator() };
		//		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new InternalPropertiesCreator(), new CDKPropertiesCreator(),
		//		//		//				new OBStructuralPropertiesCreator() };
		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new CDKPropertiesCreator() };
		//		//		//		//		FEATURE_SETS = new MoleculePropertySetCreator[] { new StructuralPropertiesCreator() };
		//		//		FEATURE_FILTER = new MoleculePropertySetFilter[] { new CoinFlipPropertySetFilter() };
		//		FEATURE_FILTER = new MoleculePropertySetFilter[] { new NoPropertySetFilter() };
		//		//		//		//CLUSTERERS = new DatasetClusterer[] { ClusterWizardPanel.CLUSTERERS[2] }; // cascadeK
		//		CLUSTERERS = new DatasetClusterer[] { new NoClusterer() };
		CLUSTERERS = new DatasetClusterer[] { ClusterWizardPanel.getDefaultClusterer() };
		//		//		CLUSTERERS = new DatasetClusterer[] { WekaClusterer.WEKA_CLUSTERER[0] };
		EMBEDDERS = new ThreeDEmbedder[] { EmbedWizardPanel.getDefaultEmbedder() };
		//		EMBEDDERS = new ThreeDEmbedder[] { new Random3DEmbedder() };
		//		//		//		//EMBEDDERS = new ThreeDEmbedder[] { new AbstractRTo3DEmbedder.PCAFeature3DEmbedder() };
		//		//		EMBEDDERS = new ThreeDEmbedder[] { EMBEDDERS[1] }; // weka pca
		ALIGNERS = new ThreeDAligner[] { new NoAligner() };
		//		//		//		ALIGNERS = new ThreeDAligner[] { new MaxFragAligner() };
		//		ALIGNERS = new ThreeDAligner[] { new MCSAligner() };
	}

	interface MoleculePropertySetFilter
	{
		public MoleculePropertySet[] filterSet(MoleculePropertySet[] set);
	}

	static class NoPropertySetFilter implements MoleculePropertySetFilter
	{
		@Override
		public MoleculePropertySet[] filterSet(MoleculePropertySet[] set)
		{
			return set;
		}
	}

	static class CoinFlipPropertySetFilter implements MoleculePropertySetFilter
	{
		@Override
		public MoleculePropertySet[] filterSet(MoleculePropertySet[] set)
		{
			if (random.nextBoolean()) // return just one prop
				return new MoleculePropertySet[] { set[random.nextInt(set.length)] };

			List<MoleculePropertySet> l = new ArrayList<MoleculePropertySet>();
			for (MoleculePropertySet p : set)
				if (random.nextBoolean())
					l.add(p);

			if (l.size() == 0)
				return new MoleculePropertySet[] { set[random.nextInt(set.length)] };

			MoleculePropertySet[] a = new MoleculePropertySet[l.size()];
			return l.toArray(a);
		}
	}

	interface MoleculePropertySetCreator
	{
		public MoleculePropertySet[] getSet(DatasetFile dataset);
	}

	static class InternalPropertiesCreator implements MoleculePropertySetCreator
	{
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			List<MoleculePropertySet> l = new ArrayList<MoleculePropertySet>();
			for (IntegratedProperty p : dataset.getIntegratedProperties(false))
				if (p.getType() == Type.NOMINAL || p.getType() == Type.NUMERIC)
					l.add(p);
			MoleculePropertySet[] a = new MoleculePropertySet[l.size()];
			return l.toArray(a);
		}
	}

	static class CDKPropertiesCreator implements MoleculePropertySetCreator
	{
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			return CDKPropertySet.NUMERIC_DESCRIPTORS;
		}
	}

	static class StructuralPropertiesCreator implements MoleculePropertySetCreator
	{
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			return StructuralFragments.instance.getSets();
		}
	}

	static class OBStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		@Override
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.OpenBabel);
			return super.getSet(dataset);
		}
	}

	static class CDKStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		@Override
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.CDK);
			return super.getSet(dataset);
		}
	}

	static class RandomStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		@Override
		public MoleculePropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.setMatchEngine(random.nextBoolean() ? MatchEngine.CDK : MatchEngine.OpenBabel);
			StructuralFragmentProperties.setMinFrequency(1 + random.nextInt(50));
			StructuralFragmentProperties.setSkipOmniFragments(random.nextBoolean());
			return super.getSet(dataset);
		}
	}

	@Test
	public void testDoMapping()
	{
		TaskProvider.registerThread("Ches-Mapper-Task");
		TaskProvider.task().getPanel();
		TaskPanel.PRINT_VERBOSE_MESSAGES = true;

		int n = FILES.length * BUILDERS.length * FEATURE_SETS.length * FEATURE_FILTER.length * CLUSTERERS.length
				* EMBEDDERS.length * ALIGNERS.length;

		int indices[] = new int[n];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;
		ArrayUtil.scramble(indices, random);

		for (int k = 0; k < n; k++)
		{
			int i = indices[k];

			int j = i;
			int max = n / FILES.length;
			int fileIndex = j / max;

			j = i % max;
			max = max / BUILDERS.length;
			int builderIndex = j / max;

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
			max = max / ALIGNERS.length;
			int alignerIndex = j / max;

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
				System.err.println("t> skipping: no structural features for max-frag alignment");
				continue;
			}

			DatasetWizardPanel datasetProvider = new DatasetWizardPanel(null);
			datasetProvider.load(CheSMappingTest.class.getResource("data/" + file).getFile());
			ThreadUtil.sleep(100);
			while (datasetProvider.getDatasetFile() == null || datasetProvider.isLoading())
				ThreadUtil.sleep(100);

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

			MoleculePropertySet[] featureSet = featureFilter.filterSet(featureCreator.getSet(datasetProvider
					.getDatasetFile()));

			System.err.println("t> do mapping");
			System.err.println("t> features: " + ArrayUtil.toString(featureSet));

			FeatureComputer featureComputer = new DefaultFeatureComputer(featureSet);

			CheSMapping ch = new CheSMapping(datasetProvider, featureComputer, clusterer, builder, embedder, aligner);
			ClusteringData clustering = ch.doMapping();

			if (clustering == null)
			{
				// this should never happen
				Assert.assertNotNull(ch.getMappingError());
				Assert.fail();
			}
			else
			{
				List<MoleculeProperty> featuresWithInfo = new ArrayList<MoleculeProperty>();
				for (MoleculeProperty p : clustering.getFeatures())
					if (!MoleculePropertyUtil.hasUniqueValue(p, datasetProvider.getDatasetFile()))
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

				Assert.assertTrue(clustering.getSize() >= 1);
				Assert.assertTrue(clustering.getSize() == clustering.getClusters().size());
			}
		}
	}
}
