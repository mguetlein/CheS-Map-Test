package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.DatasetFile;
import data.IntegratedProperty;
import data.cdk.CDKPropertySet;
import data.cdkfingerprints.CDKFingerprintSet;
import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;
import data.fragments.StructuralFragments;
import data.obdesc.OBDescriptorProperty;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

public class TestUtil
{
	public interface MoleculePropertySetFilter
	{
		public CompoundPropertySet[] filterSet(CompoundPropertySet[] set);
	}

	public static class NoPropertySetFilter implements MoleculePropertySetFilter
	{
		@Override
		public CompoundPropertySet[] filterSet(CompoundPropertySet[] set)
		{
			return set;
		}
	}

	public static class CoinFlipPropertySetFilter implements MoleculePropertySetFilter
	{
		Random random;

		public CoinFlipPropertySetFilter(Random random)
		{
			this.random = random;
		}

		@Override
		public CompoundPropertySet[] filterSet(CompoundPropertySet[] set)
		{
			if (set.length == 0)
				return new CompoundPropertySet[0];

			if (random.nextBoolean()) // return just one prop
				return new CompoundPropertySet[] { set[random.nextInt(set.length)] };

			List<CompoundPropertySet> l = new ArrayList<CompoundPropertySet>();
			for (CompoundPropertySet p : set)
				if (random.nextBoolean())
					l.add(p);

			if (l.size() == 0)
				return new CompoundPropertySet[] { set[random.nextInt(set.length)] };

			CompoundPropertySet[] a = new CompoundPropertySet[l.size()];
			return l.toArray(a);
		}
	}

	public interface MoleculePropertySetCreator
	{
		public CompoundPropertySet[] getSet(DatasetFile dataset);

		public String getName();
	}

	public static class InternalPropertiesCreator implements MoleculePropertySetCreator
	{
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			List<CompoundPropertySet> l = new ArrayList<CompoundPropertySet>();
			for (IntegratedProperty p : dataset.getIntegratedProperties())
				if (!p.isSmiles() && (p.getType() == Type.NOMINAL || p.getType() == Type.NUMERIC))
					l.add(p);
			CompoundPropertySet[] a = new CompoundPropertySet[l.size()];
			return l.toArray(a);
		}

		@Override
		public String getName()
		{
			return this.getClass().getSimpleName();
		}
	}

	public static class OBDescriptorCreator implements MoleculePropertySetCreator
	{
		public String getName()
		{
			return "OBDescriptorCreator";
		}

		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			List<CompoundPropertySet> l = new ArrayList<CompoundPropertySet>();
			for (OBDescriptorProperty p : OBDescriptorProperty.getDescriptors(false))
				if (p.getType() == Type.NOMINAL || p.getType() == Type.NUMERIC)
					l.add(p);
			return ListUtil.toArray(l);
		}
	}

	public static class CDKPropertiesCreator implements MoleculePropertySetCreator
	{
		public static enum Subset
		{
			All, WithoutIonizationPotential, Constitutional;
		}

		public CDKPropertiesCreator()
		{
			this(Subset.All);
		}

		Subset subset;
		CDKPropertySet[] set = CDKPropertySet.NUMERIC_DESCRIPTORS;

		public String getName()
		{
			return "CDKPropertiesCreator " + subset;
		}

		public CDKPropertiesCreator(Subset subset)
		{
			this.subset = subset;
			if (subset != Subset.All)
			{
				List<CDKPropertySet> newSet = new ArrayList<CDKPropertySet>();
				for (CDKPropertySet p : set)
					if (!p.getNameIncludingParams().equals("Ionization Potential"))
					{
						if (subset != Subset.Constitutional
								|| ArrayUtil.indexOf(p.getDictionaryClass(), "constitutional") != -1)
							newSet.add(p);
					}
				if (subset == Subset.WithoutIonizationPotential && newSet.size() != set.length - 1)
					throw new Error("WTF");
				set = ListUtil.toArray(newSet);
			}
		}

		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			return set;
		}
	}

	public static class StructuralPropertiesCreator implements MoleculePropertySetCreator
	{
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			return StructuralFragments.instance.getSets();
		}

		@Override
		public String getName()
		{
			return this.getClass().getSimpleName();
		}
	}

	public static class OBFingerprintCreator extends StructuralPropertiesCreator
	{
		boolean onlyFP2;
		Integer minFrequency;

		public OBFingerprintCreator(boolean onlyFP2, Integer minFrequency)
		{
			this.onlyFP2 = onlyFP2;
			this.minFrequency = minFrequency;
		}

		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.OpenBabel);
			if (minFrequency == null)
				StructuralFragmentProperties.setMinFrequency(10);
			else
				StructuralFragmentProperties.setMinFrequency(minFrequency);

			if (onlyFP2)
				return new CompoundPropertySet[] { OBFingerprintSet.FINGERPRINTS[0] };
			else
				return OBFingerprintSet.FINGERPRINTS;
		}
	}

	public static class CDKFingerprintCreator extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			return new CompoundPropertySet[] { CDKFingerprintSet.FINGERPRINTS[1] };
		}
	}

	public static class OBCarcMutRulesCreator extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.OpenBabel);
			return new CompoundPropertySet[] { StructuralFragments.instance
					.findFromString("Smarts file: ToxTree_BB_CarcMutRules") };
		}
	}

	public static class CDKCarcMutRulesCreator extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.CDK);
			return new CompoundPropertySet[] { StructuralFragments.instance
					.findFromString("Smarts file: ToxTree_BB_CarcMutRules") };
		}
	}

	//same as CDKCarcMutRulesCreator, just to compare new runtime
	public static class CDKCarcMutRulesCreator2 extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.CDK);
			return new CompoundPropertySet[] { StructuralFragments.instance
					.findFromString("Smarts file: ToxTree_BB_CarcMutRules") };
		}
	}

	public static class OBStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.OpenBabel);
			return super.getSet(dataset);
		}
	}

	public static class CDKStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.resetDefaults();
			StructuralFragmentProperties.setMatchEngine(MatchEngine.CDK);
			return super.getSet(dataset);
		}
	}

	public static class RandomStructuralPropertiesCreator extends StructuralPropertiesCreator
	{
		Random random;

		public RandomStructuralPropertiesCreator(Random random)
		{
			this.random = random;
		}

		@Override
		public CompoundPropertySet[] getSet(DatasetFile dataset)
		{
			StructuralFragmentProperties.setMatchEngine(random.nextBoolean() ? MatchEngine.CDK : MatchEngine.OpenBabel);
			StructuralFragmentProperties.setMinFrequency(1 + random.nextInt(50));
			StructuralFragmentProperties.setSkipOmniFragments(random.nextBoolean());
			return super.getSet(dataset);
		}
	}
}
