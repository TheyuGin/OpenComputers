package li.cil.oc.integration.forestry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IAlleleBeeEffect;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.arboriculture.EnumTreeChromosome;
import forestry.api.arboriculture.IAlleleFruit;
import forestry.api.arboriculture.IAlleleGrowth;
import forestry.api.arboriculture.IAlleleLeafEffect;
import forestry.api.arboriculture.IAlleleTreeSpecies;
import forestry.api.arboriculture.ITree;
import forestry.api.arboriculture.ITreeGenome;
import forestry.api.genetics.*;
import forestry.api.lepidopterology.EnumButterflyChromosome;
import forestry.api.lepidopterology.IAlleleButterflyEffect;
import forestry.api.lepidopterology.IAlleleButterflySpecies;
import forestry.api.lepidopterology.IButterfly;
import forestry.api.lepidopterology.IButterflyGenome;
import li.cil.oc.api.driver.Converter;

import java.util.HashMap;
import java.util.Map;

/*
 * Partially copied from:
 * https://github.com/OpenMods/OpenPeripheral
 */
public class ConverterIIndividual implements Converter {
    private abstract static class GenomeAccess {
        public IAllele getAllele(IGenome genome, int chromosome) {
            IChromosome[] genotype = genome.getChromosomes();
            IChromosome ch = genotype[chromosome];
            if (ch == null) return null;
            return getAllele(ch);
        }

        protected abstract IAllele getAllele(IChromosome chromosome);
    }

    private static final GenomeAccess ACTIVE = new GenomeAccess() {
        @Override
        protected IAllele getAllele(IChromosome chromosome) {
            return chromosome.getActiveAllele();
        }
    };

    private static final GenomeAccess INACTIVE = new GenomeAccess() {
        @Override
        protected IAllele getAllele(IChromosome chromosome) {
            return chromosome.getInactiveAllele();
        }
    };

    private interface IAlleleConverter<A extends IAllele> {
        Object convert(A allele);
    }

    private static final Map<Class<? extends IAllele>, IAlleleConverter<?>> converters =
            ImmutableMap.<Class<? extends IAllele>, IAlleleConverter<?>>builder()
                    .put(IAlleleFloat.class, (IAlleleConverter<IAlleleFloat>) IAlleleFloat::getValue)
                    .put(IAlleleInteger.class, (IAlleleConverter<IAlleleInteger>) IAlleleInteger::getValue)
                    .put(IAlleleBoolean.class, (IAlleleConverter<IAlleleBoolean>) IAlleleBoolean::getValue)
                    .put(IAlleleArea.class, (IAlleleConverter<IAlleleArea>) IAlleleArea::getValue)
                    .put(IAllelePlantType.class, (IAlleleConverter<IAllelePlantType>) IAllelePlantType::getPlantTypes)
                    .put(IAlleleGrowth.class, (IAlleleConverter<IAlleleGrowth>) allele -> allele.getProvider().getInfo())
                    .put(IAlleleBeeSpecies.class, (IAlleleConverter<IAlleleBeeSpecies>) allele -> {
                        Map<Object, Object> output = new HashMap<>();
                        ConverterIAlleles.convertAlleleSpecies(allele, output);
                        return output;
                    })
                    .build();

    private abstract static class GenomeReader<G extends IGenome, E extends Enum<E> & IChromosomeType> {
        private final G genome;

        public GenomeReader(G genome) {
            this.genome = genome;
        }

        @SuppressWarnings("unchecked")
        protected <A extends IAllele> A getAllele(GenomeAccess access, Class<A> cls, E chromosome) {
            Preconditions.checkArgument(chromosome.getAlleleClass() == cls);
            IAllele allele = access.getAllele(genome, chromosome.ordinal());
            return (A) allele;
        }

        protected <A extends IAllele> Object convertAllele(GenomeAccess access, Class<A> cls, E chromosome) {
            A allele = getAllele(access, cls, chromosome);
            if (allele == null) return "missing";
            @SuppressWarnings("unchecked")
            IAlleleConverter<IAllele> converter = (IAlleleConverter<IAllele>) converters.get(cls);
            return converter != null ? converter.convert(allele) : allele.getName();
        }

        protected abstract void addAlleleInfo(GenomeAccess access, Map<String, Object> result);

        public Map<String, Object> getActiveInfo() {
            Map<String, Object> result = Maps.newHashMap();
            addAlleleInfo(ACTIVE, result);
            return result;
        }

        public Map<String, Object> getInactiveInfo() {
            Map<String, Object> result = Maps.newHashMap();
            addAlleleInfo(INACTIVE, result);
            return result;
        }
    }

    private static class BeeGenomeReader extends GenomeReader<IBeeGenome, EnumBeeChromosome> {

        public BeeGenomeReader(IBeeGenome genome) {
            super(genome);
        }

        @Override
        protected void addAlleleInfo(GenomeAccess access, Map<String, Object> result) {
            result.put("species", convertAllele(access, IAlleleBeeSpecies.class, EnumBeeChromosome.SPECIES));
            result.put("speed", convertAllele(access, IAlleleFloat.class, EnumBeeChromosome.SPEED));
            result.put("lifespan", convertAllele(access, IAlleleInteger.class, EnumBeeChromosome.LIFESPAN));
            result.put("fertility", convertAllele(access, IAlleleInteger.class, EnumBeeChromosome.FERTILITY));
            result.put("temperatureTolerance", convertAllele(access, IAlleleTolerance.class, EnumBeeChromosome.TEMPERATURE_TOLERANCE));
            result.put("nocturnal", convertAllele(access, IAlleleBoolean.class, EnumBeeChromosome.NOCTURNAL));
            result.put("humidityTolerance", convertAllele(access, IAlleleTolerance.class, EnumBeeChromosome.HUMIDITY_TOLERANCE));
            result.put("tolerantFlyer", convertAllele(access, IAlleleBoolean.class, EnumBeeChromosome.TOLERANT_FLYER));
            result.put("caveDwelling", convertAllele(access, IAlleleBoolean.class, EnumBeeChromosome.CAVE_DWELLING));
            result.put("flowerProvider", convertAllele(access, IAlleleFlowers.class, EnumBeeChromosome.FLOWER_PROVIDER));
            result.put("flowering", convertAllele(access, IAlleleInteger.class, EnumBeeChromosome.FLOWERING));
            result.put("effect", convertAllele(access, IAlleleBeeEffect.class, EnumBeeChromosome.EFFECT));
            result.put("territory", convertAllele(access, IAlleleArea.class, EnumBeeChromosome.TERRITORY));
        }
    }

    private static class ButterflyGenomeReader extends GenomeReader<IButterflyGenome, EnumButterflyChromosome> {

        public ButterflyGenomeReader(IButterflyGenome genome) {
            super(genome);
        }

        @Override
        protected void addAlleleInfo(GenomeAccess access, Map<String, Object> result) {
            result.put("species", convertAllele(access, IAlleleButterflySpecies.class, EnumButterflyChromosome.SPECIES));
            result.put("size", convertAllele(access, IAlleleFloat.class, EnumButterflyChromosome.SIZE));
            result.put("speed", convertAllele(access, IAlleleFloat.class, EnumButterflyChromosome.SPEED));
            result.put("lifespan", convertAllele(access, IAlleleInteger.class, EnumButterflyChromosome.LIFESPAN));
            result.put("metabolism", convertAllele(access, IAlleleInteger.class, EnumButterflyChromosome.METABOLISM));
            result.put("fertility", convertAllele(access, IAlleleInteger.class, EnumButterflyChromosome.FERTILITY));
            result.put("temperatureTolerance", convertAllele(access, IAlleleTolerance.class, EnumButterflyChromosome.TEMPERATURE_TOLERANCE));
            result.put("humidityTolerance", convertAllele(access, IAlleleTolerance.class, EnumButterflyChromosome.HUMIDITY_TOLERANCE));
            result.put("nocturnal", convertAllele(access, IAlleleBoolean.class, EnumButterflyChromosome.NOCTURNAL));
            result.put("tolerantFlyer", convertAllele(access, IAlleleBoolean.class, EnumButterflyChromosome.TOLERANT_FLYER));
            result.put("fireResist", convertAllele(access, IAlleleBoolean.class, EnumButterflyChromosome.FIRE_RESIST));
            result.put("flowerProvider", convertAllele(access, IAlleleFlowers.class, EnumButterflyChromosome.FLOWER_PROVIDER));
            result.put("effect", convertAllele(access, IAlleleButterflyEffect.class, EnumButterflyChromosome.EFFECT));
            result.put("territory", convertAllele(access, IAlleleArea.class, EnumButterflyChromosome.TERRITORY));
        }
    }

    private static class TreeGenomeReader extends GenomeReader<ITreeGenome, EnumTreeChromosome> {

        public TreeGenomeReader(ITreeGenome genome) {
            super(genome);
        }

        @Override
        protected void addAlleleInfo(GenomeAccess access, Map<String, Object> result) {
            result.put("species", convertAllele(access, IAlleleTreeSpecies.class, EnumTreeChromosome.SPECIES));
            result.put("growth", convertAllele(access, IAlleleGrowth.class, EnumTreeChromosome.GROWTH));
            result.put("height", convertAllele(access, IAlleleFloat.class, EnumTreeChromosome.HEIGHT));
            result.put("fertility", convertAllele(access, IAlleleFloat.class, EnumTreeChromosome.FERTILITY));
            result.put("fruits", convertAllele(access, IAlleleFruit.class, EnumTreeChromosome.FRUITS));
            result.put("yield", convertAllele(access, IAlleleFloat.class, EnumTreeChromosome.YIELD));
            result.put("plant", convertAllele(access, IAllelePlantType.class, EnumTreeChromosome.PLANT));
            result.put("sappiness", convertAllele(access, IAlleleFloat.class, EnumTreeChromosome.SAPPINESS));
            result.put("territory", convertAllele(access, IAlleleArea.class, EnumTreeChromosome.TERRITORY));
            result.put("effect", convertAllele(access, IAlleleLeafEffect.class, EnumTreeChromosome.EFFECT));
            result.put("maturation", convertAllele(access, IAlleleInteger.class, EnumTreeChromosome.MATURATION));
            result.put("girth", convertAllele(access, IAlleleInteger.class, EnumTreeChromosome.GIRTH));
        }
    }

    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof IIndividual) {
            IIndividual individual = (IIndividual) value;
            output.put("displayName", individual.getDisplayName());
            output.put("ident", individual.getIdent());

            final boolean isAnalyzed = individual.isAnalyzed();
            output.put("isAnalyzed", isAnalyzed);
            output.put("isSecret", individual.isSecret());
            GenomeReader<?, ?> genomeReader = null;

            if (individual instanceof IIndividualLiving) {
                IIndividualLiving living = (IIndividualLiving) individual;
                output.put("health", living.getHealth());
                output.put("maxHealth", living.getMaxHealth());
            }

            if (individual instanceof IBee) {
                IBee bee = (IBee) individual;
                output.put("type", "bee");
                output.put("canSpawn", bee.canSpawn());
                output.put("generation", bee.getGeneration());
                output.put("hasEffect", bee.hasEffect());
                output.put("isAlive", bee.isAlive());
                output.put("isNatural", bee.isNatural());

                if (isAnalyzed) genomeReader = new BeeGenomeReader(bee.getGenome());
            } else if (individual instanceof IButterfly) {
                IButterfly butterfly = (IButterfly) individual;
                output.put("type", "butterfly");
                output.put("size", butterfly.getSize());
                if (isAnalyzed) genomeReader = new ButterflyGenomeReader(butterfly.getGenome());
            } else if (individual instanceof ITree) {
                ITree tree = (ITree) individual;
                output.put("type", "tree");
                output.put("plantType", tree.getPlantTypes().toString());
                if (isAnalyzed) genomeReader = new TreeGenomeReader(tree.getGenome());
            }

            if (genomeReader != null) {
                output.put("active", genomeReader.getActiveInfo());
                output.put("inactive", genomeReader.getInactiveInfo());
            }
        }
    }
}
