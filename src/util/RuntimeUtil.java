package util;

import gui.property.Property;
import gui.property.PropertyUtil;

import java.util.ArrayList;
import java.util.List;

import util.ListUtil;
import util.TestUtil.CDKCarcMutRulesCreator;
import util.TestUtil.CDKCarcMutRulesCreator2;
import util.TestUtil.CDKFingerprintCreator;
import util.TestUtil.CDKPropertiesCreator;
import util.TestUtil.MoleculePropertySetCreator;
import util.TestUtil.OBCarcMutRulesCreator;
import util.TestUtil.OBDescriptorCreator;
import util.TestUtil.OBFingerprintCreator;
import util.TestUtil.CDKPropertiesCreator.Subset;
import weka.CascadeSimpleKMeans;
import weka.clusterers.EM;
import alg.Algorithm;
import alg.align3d.MCSAligner;
import alg.align3d.MaxFragAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.CDK3DBuilder;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.WekaClusterer;
import alg.cluster.r.AbstractRClusterer;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import alg.embed3d.r.PCAFeature3DEmbedder;
import alg.embed3d.r.SMACOF3DEmbedder;
import alg.embed3d.r.Sammon3DEmbedder;
import alg.embed3d.r.TSNEFeature3DEmbedder;
import data.DatasetFile;
import dataInterface.CompoundPropertySet;

public class RuntimeUtil
{
	public static interface AlgorithmWrapper
	{
		public Algorithm get();

		public String getName();

		public boolean isFeasible(int numCompounds);

		public boolean isSlow(int numCompounds);
	}

	static class DefaultWrapper implements AlgorithmWrapper
	{
		Algorithm e;

		public DefaultWrapper(Algorithm e)
		{
			this.e = e;
		}

		public Algorithm get()
		{
			return e;
		}

		public String getName()
		{
			return e.getName();
		}

		@Override
		public boolean isFeasible(int numCompounds)
		{
			return true;
		}

		@Override
		public boolean isSlow(int numCompounds)
		{
			return false;
		}
	}

	static class WrappedBuilder extends DefaultWrapper
	{
		public WrappedBuilder(ThreeDBuilder e)
		{
			super(e);
		}

		@Override
		public boolean isFeasible(int numCompounds)
		{
			return !(e instanceof OpenBabel3DBuilder) || numCompounds <= 500;
		}

		public boolean isSlow(int numCompounds)
		{
			//return e instanceof OpenBabel3DBuilder && numCompounds > 34;
			return numCompounds > 34;
		}
	}

	static class WrappedAligner extends DefaultWrapper
	{
		public WrappedAligner(ThreeDAligner e)
		{
			super(e);
		}

		public boolean isSlow(int numCompounds)
		{
			return numCompounds > 34 && e instanceof MCSAligner;
		}
	}

	public static class WrappedFeatureComputer implements AlgorithmWrapper
	{
		MoleculePropertySetCreator creator;

		public WrappedFeatureComputer(MoleculePropertySetCreator creator)
		{
			this.creator = creator;
		}

		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			return creator.getSet(dataset);
		}

		@Override
		public Algorithm get()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return creator.getName();
		}

		@Override
		public boolean isFeasible(int numCompounds)
		{
			if (creator instanceof CDKCarcMutRulesCreator)
				return numCompounds <= 500;
			else if (creator instanceof CDKFingerprintCreator)
				return numCompounds <= 500;
			else
				return true;
		}

		@Override
		public boolean isSlow(int numCompounds)
		{
			if (creator instanceof CDKPropertiesCreator)
				return ((CDKPropertiesCreator) creator).subset != Subset.Constitutional && numCompounds > 34;
			else if (creator instanceof CDKCarcMutRulesCreator)
				return numCompounds > 34;
			else if (creator instanceof CDKCarcMutRulesCreator2)
				return numCompounds > 100;
			else if (creator instanceof CDKFingerprintCreator)
				return numCompounds > 34;
			else
				return false;
		}

	}

	public static AlgorithmWrapper BUILDERS[] = { new WrappedBuilder(CDK3DBuilder.INSTANCE),
			new WrappedBuilder(OpenBabel3DBuilder.INSTANCE) };

	public static AlgorithmWrapper FEATURE_SETS[] = {
			new WrappedFeatureComputer(new CDKPropertiesCreator(Subset.Constitutional)),
			new WrappedFeatureComputer(new CDKPropertiesCreator(Subset.WithoutIonizationPotential)),
			new WrappedFeatureComputer(new OBFingerprintCreator(false, null)),
			new WrappedFeatureComputer(new OBCarcMutRulesCreator()),
			new WrappedFeatureComputer(new CDKCarcMutRulesCreator()),
			new WrappedFeatureComputer(new CDKCarcMutRulesCreator2()),
			new WrappedFeatureComputer(new OBDescriptorCreator()),
			new WrappedFeatureComputer(new CDKFingerprintCreator()), };

	static class WrappedClusterer extends DefaultWrapper
	{
		public WrappedClusterer(DatasetClusterer e)
		{
			super(e);
		}

		public DatasetClusterer get()
		{
			return (DatasetClusterer) super.get();
		}
	}

	public static AlgorithmWrapper CLUSTERERS[];

	static
	{
		List<AlgorithmWrapper> clusterer = new ArrayList<RuntimeUtil.AlgorithmWrapper>();
		for (WekaClusterer c : WekaClusterer.WEKA_CLUSTERER)
		{

			if (c.getWekaClusterer() instanceof CascadeSimpleKMeans)
			{
				clusterer.add(new WrappedClusterer(c)
				{
					@Override
					public boolean isSlow(int numCompounds)
					{
						return numCompounds >= 500;
					}

					@Override
					public DatasetClusterer get()
					{
						Property min = PropertyUtil.getProperty(e.getProperties(), "minNumClusters");
						Property max = PropertyUtil.getProperty(e.getProperties(), "maxNumClusters");
						Property restarts = PropertyUtil.getProperty(e.getProperties(), "restarts");
						min.setValue(min.getDefaultValue());
						max.setValue(max.getDefaultValue());
						restarts.setValue(restarts.getDefaultValue());
						return (DatasetClusterer) e;
					}
				});
				clusterer.add(new WrappedClusterer(c)
				{
					@Override
					public String getName()
					{
						return super.getName() + " 3-5 r3";
					}

					@Override
					public DatasetClusterer get()
					{
						PropertyUtil.getProperty(e.getProperties(), "minNumClusters").setValue(new Integer(3));
						PropertyUtil.getProperty(e.getProperties(), "maxNumClusters").setValue(new Integer(5));
						PropertyUtil.getProperty(e.getProperties(), "restarts").setValue(new Integer(3));
						return (DatasetClusterer) e;
					}
				});
			}
			else if (c.getWekaClusterer() instanceof EM)
			{
				clusterer.add(new WrappedClusterer(c)
				{
					@Override
					public DatasetClusterer get()
					{
						Property num = PropertyUtil.getProperty(e.getProperties(), "numClusters");
						num.setValue(num.getDefaultValue());
						return (DatasetClusterer) e;
					}

					@Override
					public boolean isSlow(int numCompounds)
					{
						return numCompounds >= 100;
					}

					@Override
					public boolean isFeasible(int numCompounds)
					{
						return numCompounds <= 500;
					}
				});
				clusterer.add(new WrappedClusterer(c)
				{
					@Override
					public String getName()
					{
						return super.getName() + " 5";
					}

					@Override
					public DatasetClusterer get()
					{
						PropertyUtil.getProperty(e.getProperties(), "numClusters").setValue(new Integer(5));
						return (DatasetClusterer) e;
					}
				});
			}
			else
				clusterer.add(new WrappedClusterer(c));
		}

		for (DatasetClusterer c : AbstractRClusterer.R_CLUSTERER)
			clusterer.add(new WrappedClusterer(c));
		CLUSTERERS = ListUtil.toArray(clusterer);

	}

	static class WrappedEmbedder extends DefaultWrapper
	{
		public WrappedEmbedder(ThreeDEmbedder e)
		{
			super(e);
		}

		public ThreeDEmbedder get()
		{
			return (ThreeDEmbedder) super.get();
		}
	}

	static class WrappedSMACOF implements AlgorithmWrapper
	{
		int n;

		public WrappedSMACOF(int n)
		{
			this.n = n;
		}

		@Override
		public ThreeDEmbedder get()
		{
			SMACOF3DEmbedder.INSTANCE.getProperties()[0].setValue(new Integer(n));
			return SMACOF3DEmbedder.INSTANCE;
		}

		@Override
		public String getName()
		{
			return SMACOF3DEmbedder.INSTANCE.getName() + " " + n;
		}

		@Override
		public boolean isFeasible(int numCompounds)
		{
			return numCompounds <= 100 || (numCompounds <= 100 && n < 30);
		}

		@Override
		public boolean isSlow(int numCompounds)
		{
			return numCompounds >= 50;
		}

	}

	static class WrappedTSNE implements AlgorithmWrapper
	{
		int n;

		public WrappedTSNE(int n)
		{
			this.n = n;
		}

		@Override
		public ThreeDEmbedder get()
		{
			TSNEFeature3DEmbedder.INSTANCE.getProperties()[0].setValue(new Integer(n));
			return TSNEFeature3DEmbedder.INSTANCE;
		}

		@Override
		public String getName()
		{
			return TSNEFeature3DEmbedder.INSTANCE.getName() + " " + n;
		}

		@Override
		public boolean isFeasible(int numCompounds)
		{
			return true;
		}

		@Override
		public boolean isSlow(int numCompounds)
		{
			return false;
		}
	}

	public static AlgorithmWrapper EMBEDDERS[] = { new WrappedEmbedder(WekaPCA3DEmbedder.INSTANCE),
			new WrappedEmbedder(PCAFeature3DEmbedder.INSTANCE),
			//new WrappedTSNE(1000),new WrappedTSNE(200), 
			new WrappedEmbedder(Sammon3DEmbedder.INSTANCE), new WrappedSMACOF(30), new WrappedSMACOF(150), };

	public static WrappedAligner ALIGNERS[] = { new WrappedAligner(MCSAligner.INSTANCE),
			new WrappedAligner(MaxFragAligner.INSTANCE) };
}
