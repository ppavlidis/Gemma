/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.sequence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeService;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.ProbeAlignedRegionService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;
import ubic.gemma.util.SequenceBinUtils;

/**
 * Provides methods for mapping sequences to genes and gene products.
 * 
 * @spring.bean name="probeMapper"
 * @spring.property name="probeAlignedRegionService" ref="probeAlignedRegionService"
 * @spring.property name="chromosomeService" ref="chromosomeService"
 * @spring.property name="taxonService" ref="taxonService"
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapper {

    /**
     * Sequences which hybridize to this many or more sites in the genome are candidates to be considered non-specific.
     * This is used in combination with the REPEAT_FRACTION_MAXIMUM. Note that many sequences which contain repeats
     * nonetheless only align to very few sites in the genome.
     */
    private static final int NON_SPECIFIC_SITE_THRESHOLD = 3;

    /**
     * Sequences which have more than this fraction accounted for by repeats (via repeatmasker) will not be examined if
     * they produce multiple alignments to the genome, regardless of the alignment quality.
     */
    public static final double REPEAT_FRACTION_MAXIMUM = 0.3;

    /**
     * Sequence identity below which we throw hits away.
     */
    public static final double DEFAULT_IDENTITY_THRESHOLD = 0.80;

    /**
     * Blat score threshold below which we do not consider hits. This reflects the fraction of aligned bases.
     * 
     * @see Blat for the use of a similar parameter, used to determine the retention of raw Blat results.
     * @see BlatResult for how the score is computed.
     */
    public static final double DEFAULT_SCORE_THRESHOLD = 0.75;

    private Log log = LogFactory.getLog( ProbeMapper.class.getName() );

    private double identityThreshold = DEFAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = DEFAULT_SCORE_THRESHOLD;
    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;
    private ThreePrimeDistanceMethod threeprimeMethod = ThreePrimeDistanceMethod.RIGHT;

    private ProbeAlignedRegionService probeAlignedRegionService;
    private ChromosomeService chromosomeService;
    private TaxonService taxonService;

    /**
     * @return the blatScoreThreshold
     */
    public double getBlatScoreThreshold() {
        return this.blatScoreThreshold;
    }

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associatd with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product.
     * <p>
     * This is a major entrypoint for this API.
     * 
     * @param goldenPathDb
     * @param blatResults
     * @return A map of sequence names to collections of blat associations for each sequence.
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults ) {

        if ( log.isDebugEnabled() ) {
            log.debug( blatResults.size() + " Blat results to map " );
        }

        assert goldenPathDb != null;
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;
        int skippedDueToRepeat = 0;

        // group results together by BioSequence
        Map<BioSequence, Collection<BlatResult>> biosequenceToBlatResults = new HashMap<BioSequence, Collection<BlatResult>>();

        for ( BlatResult blatResult : blatResults ) {
            if ( !biosequenceToBlatResults.containsKey( blatResult.getQuerySequence() ) ) {
                biosequenceToBlatResults.put( blatResult.getQuerySequence(), new HashSet<BlatResult>() );
            }
            biosequenceToBlatResults.get( blatResult.getQuerySequence() ).add( blatResult );
        }

        // Do them one sequence at a time.
        for ( BioSequence sequence : biosequenceToBlatResults.keySet() ) {
            Collection<BlatResult> blatResultsForSequence = biosequenceToBlatResults.get( sequence );
            if ( log.isDebugEnabled() ) {
                log.debug( blatResultsForSequence.size() + " Blat results for " + sequence );
            }

            Double fractionRepeats = sequence.getFractionRepeats();
            if ( fractionRepeats != null && fractionRepeats > REPEAT_FRACTION_MAXIMUM
                    && blatResultsForSequence.size() >= NON_SPECIFIC_SITE_THRESHOLD ) {
                skippedDueToRepeat++;
                skipped++;
                continue;
            }

            Collection<BlatAssociation> blatAssociationsForSequence = new HashSet<BlatAssociation>();

            for ( BlatResult blatResult : blatResultsForSequence ) {
                assert blatResult.score() >= 0 : "Score was " + blatResult.score();
                assert blatResult.identity() >= 0 : "Identity was " + blatResult.identity();
                if ( blatResult.score() < scoreThreshold || blatResult.identity() < identityThreshold ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Result for " + sequence + " skipped with score=" + blatResult.score()
                                + " identity=" + blatResult.identity() );
                    skipped++;
                    continue;
                }

                // here's the key line!
                Collection<BlatAssociation> resultsForOneBlatResult = processBlatResult( goldenPathDb, blatResult );

                if ( resultsForOneBlatResult != null && resultsForOneBlatResult.size() > 0 ) {
                    blatAssociationsForSequence.addAll( resultsForOneBlatResult );
                } else {
                    // here we have to provide a 'provisional' mapping to a ProbeAlignedRegion.
                    ProbeAlignedRegion par = makePar( blatResult );
                    BlatAssociation parAssociation = makeBlatAssociationWithPar( blatResult, par );
                    blatAssociationsForSequence.add( parAssociation );
                    if ( log.isDebugEnabled() )
                        log.debug( "Adding PAR for " + sequence + " with alignment " + blatResult );
                }

                // there are rarely this many, but it does happen.
                if ( ++count % 100 == 0 && log.isDebugEnabled() )
                    log.debug( "Annotations computed for " + count + " blat results for " + sequence );

            } // end of iteration over results for this sequence.

            if ( log.isDebugEnabled() ) {
                log.debug( blatAssociationsForSequence.size() + " associations for " + sequence );
            }

            if ( blatAssociationsForSequence.size() == 0 ) continue;

            // Another important step: fill in the specificity, remove duplicates
            BlatAssociationScorer.scoreResults( blatAssociationsForSequence );

            if ( log.isDebugEnabled() ) {
                log.debug( blatAssociationsForSequence.size() + " associations for " + sequence
                        + " after redundancy reduction" );
            }

            String queryName = sequence.getName();
            assert StringUtils.isNotBlank( queryName );
            if ( !allRes.containsKey( queryName ) ) {
                allRes.put( queryName, new HashSet<BlatAssociation>() );
            }

            allRes.get( queryName ).addAll( blatAssociationsForSequence );

        } // end of iteration over sequence

        if ( log.isInfoEnabled() && skipped > 0 ) {
            log.info( "Skipped " + skipped + "/" + blatResults.size()
                    + " individual blat results that didn't meet criteria; " + skippedDueToRepeat
                    + " were skipped due to repeat or low complexity content." );
        }

        return allRes;
    }

    /**
     * @param blatResult
     * @param par
     * @return
     */
    private BlatAssociation makeBlatAssociationWithPar( BlatResult blatResult, ProbeAlignedRegion par ) {
        BlatAssociation parAssociation = BlatAssociation.Factory.newInstance();
        parAssociation.setGeneProduct( par.getProducts().iterator().next() );
        parAssociation.setBlatResult( blatResult );
        parAssociation.setBioSequence( blatResult.getQuerySequence() );
        parAssociation.setOverlap( blatResult.getQuerySequence().getLength().intValue() ); // by definition, because
        // this is for a new PAR
        assert parAssociation.getBioSequence() != null;
        return parAssociation;
    }

    /**
     * @param blatResult
     * @return
     */
    private ProbeAlignedRegion makePar( BlatResult blatResult ) {
        ProbeAlignedRegion par = ProbeAlignedRegion.Factory.newInstance();
        PhysicalLocation pl = blatResultToPhysicalLocation( blatResult );
        par.setPhysicalLocation( pl );

        /*
         * We form a hopefully unique name of the PAR using the query sequence and the coordinates of the blat result
         */
        par.setName( blatResult.getQuerySequence().getName() + ".par." + blatResult.getTargetChromosome().getName()
                + "." + blatResult.getTargetStart() + "." + blatResult.getTargetEnd() );

        /*
         * The official name isn't really what we have, as we made it up...but it will do
         */
        par.setOfficialSymbol( par.getName() );

        par.setDescription( "Based only on BLAT alignment to genome, symbol is assigned by system" );
        par.setTaxon( blatResult.getQuerySequence().getTaxon() );

        GeneProduct pargp = GeneProduct.Factory.newInstance();
        pargp.setName( par.getName() );
        pargp.setDescription( "Hypothetical RNA product based on alignment of probe to genome sequence" );
        pargp.setType( GeneProductType.RNA );
        pargp.setGene( par );
        pargp.setPhysicalLocation( pl );

        par.getProducts().add( pargp );
        return par;
    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankId
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb,
            String genbankId ) {

        log.debug( "Entering processGbId with " + genbankId );

        Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

        if ( blatResults == null || blatResults.size() == 0 ) {
            log.warn( "No results obtained for " + genbankId );
        }

        return processBlatResults( goldenPathDb, blatResults );

    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds ) {
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;
        for ( String[] genbankIdAr : genbankIds ) {

            if ( genbankIdAr == null || genbankIdAr.length == 0 ) {
                continue;
            }

            if ( genbankIdAr.length > 1 ) {
                throw new IllegalArgumentException( "Input file must have just one genbank identifier per line" );
            }

            String genbankId = genbankIdAr[0];

            Map<String, Collection<BlatAssociation>> res = processGbId( goldenPathDb, genbankId );
            allRes.putAll( res );

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Annotations computed for " + count + " genbank identifiers" );
        if ( log.isInfoEnabled() && skipped > 0 )
            log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    /**
     * Get BlatAssociation results for a single sequence. If you have multiple sequences to run it is always better to
     * use processSequences();
     * 
     * @param goldenPath
     * @param sequence
     * @return
     * @see processSequences
     */
    public Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath, BioSequence sequence ) {

        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );
        Collection<BlatResult> results;
        try {
            results = b.blatQuery( sequence, goldenPath.getTaxon() );
        } catch ( IOException e ) {
            throw new RuntimeException( "Error running blat", e );
        }
        Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenPath, results );
        assert allRes.keySet().size() == 1;
        return allRes.values().iterator().next();
    }

    /**
     * Given a collection of sequences, blat them against the selected genome.
     * 
     * @param output
     * @param goldenpath for the genome to be used.
     * @param sequences
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences ) {
        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );

        try {
            Map<BioSequence, Collection<BlatResult>> results = b.blatQuery( sequences, goldenpath.getTaxon() );
            Collection<BlatResult> blatres = new HashSet<BlatResult>();
            for ( Collection<BlatResult> coll : results.values() ) {
                blatres.addAll( coll );
            }
            Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenpath, blatres );
            return allRes;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

    /**
     * @param identityThreshold
     */
    public void setIdentityThreshold( double identityThreshold ) {
        this.identityThreshold = identityThreshold;

    }

    /**
     * @param scoreThreshold
     */
    public void setScoreThreshold( double scoreThreshold ) {
        this.scoreThreshold = scoreThreshold;

    }

    /**
     * Process a single BlatResult, identifying gene products it maps to.
     * 
     * @param goldenPathDb
     * @param blatResult
     * @return BlatAssociations between the queried biosequence and one or more gene products.
     */
    @SuppressWarnings("unchecked")
    private Collection<BlatAssociation> processBlatResult( GoldenPathSequenceAnalysis goldenPathDb,
            BlatResult blatResult ) {
        assert blatResult.getTargetChromosome() != null : "Chromosome not filled in for blat result";

        boolean ignoreStrand = determineStrandTreatment( blatResult );

        String strand = ignoreStrand == true ? null : blatResult.getStrand();

        Collection<BlatAssociation> blatAssociations = goldenPathDb.findAssociations( blatResult.getTargetChromosome()
                .getName(), blatResult.getTargetStart(), blatResult.getTargetEnd(), blatResult.getTargetStarts(),
                blatResult.getBlockSizes(), strand, threeprimeMethod );

        if ( blatAssociations != null && blatAssociations.size() > 0 ) {
            for ( BlatAssociation association : blatAssociations ) {
                association.setBlatResult( blatResult );
                association.setBioSequence( blatResult.getQuerySequence() );
            }
            return blatAssociations;
        }

        // no genes, have to look for pre-existing probealignedregions that overlap.
        if ( blatResult.getQuerySequence().getTaxon() == null )
            blatResult.getQuerySequence().setTaxon( goldenPathDb.getTaxon() );
        return findProbeAlignedRegionAssociations( blatResult, ignoreStrand );

    }

    /**
     * It is assume that strand should only be used if the sequence type is AFFY_{PROBE,COLLAPSED,TARGET} or OLIGO. In
     * all other cases (ESTs etc) the strand is ignored.
     * 
     * @param blatResult
     * @return boolean indicating, essentially, if the sequence on the array is double-stranded.
     */
    private boolean determineStrandTreatment( BlatResult blatResult ) {
        boolean ignoreStrand = true;

        SequenceType type = blatResult.getQuerySequence().getType();
        if ( type == null ) {
            return true;
        }
        if ( type.equals( SequenceType.OLIGO ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_COLLAPSED ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_PROBE ) ) {
            ignoreStrand = false;
        } else if ( type.equals( SequenceType.AFFY_TARGET ) ) {
            ignoreStrand = false;
        }
        return ignoreStrand;
    }

    /**
     * Identify ProbeAlignedRegions that overlap with the given blat result. A ProbeAlignedRegion is a region in which
     * there are no known or predicted genes, but a designElement aligned to.
     * 
     * @param blatResult
     * @param ignoreStrand
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<BlatAssociation> findProbeAlignedRegionAssociations( BlatResult blatResult, boolean ignoreStrand ) {

        PhysicalLocation pl = makePhysicalLocation( blatResult, ignoreStrand );

        Collection<ProbeAlignedRegion> pars = probeAlignedRegionService.findAssociations( pl );
        if ( log.isDebugEnabled() && pars.size() > 0 ) log.debug( "Found " + pars.size() + " PARS for " + blatResult );
        Collection<BlatAssociation> results = new HashSet<BlatAssociation>();
        for ( ProbeAlignedRegion region : pars ) {
            BlatAssociation ba = BlatAssociation.Factory.newInstance();
            GeneProduct product = region.getProducts().iterator().next();
            assert product.getId() != null;
            ba.setGeneProduct( product );
            ba.setBlatResult( blatResult );
            ba.setBioSequence( blatResult.getQuerySequence() );
            ba.setOverlap( SequenceManipulation.getGeneProductExonOverlap( blatResult.getTargetStarts(), blatResult
                    .getBlockSizes(), pl.getStrand(), product ) );
            results.add( ba );

        }
        return results;
    }

    /**
     * Turn the blat result into a physical location
     * 
     * @param blatResult
     * @param ignoreStrand
     * @return
     */
    private PhysicalLocation makePhysicalLocation( BlatResult blatResult, boolean ignoreStrand ) {
        PhysicalLocation pl = blatResultToPhysicalLocation( blatResult );
        if ( ignoreStrand ) {
            pl.setStrand( null );
        }
        return pl;
    }

    /**
     * @param blatResult
     * @return
     */
    private PhysicalLocation blatResultToPhysicalLocation( BlatResult blatResult ) {
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        Chromosome chrom = blatResult.getTargetChromosome();
        Taxon taxon = blatResult.getQuerySequence().getTaxon();
        if ( taxon.getId() == null ) {
            taxon = taxonService.findOrCreate( taxon ); // usually only during tests.
        }

        assert taxon.getId() != null;

        chrom.setTaxon( taxon );
        if ( chrom.getId() == null ) {
            Chromosome foundChrom = chromosomeService.find( chrom );
            if ( foundChrom == null )
                chrom = chromosomeService.findOrCreate( chrom ); // typically only during tests.
            else
                chrom = foundChrom;
        }

        assert chrom.getId() != null;

        pl.setChromosome( chrom );
        pl.setNucleotide( blatResult.getTargetStart() );
        pl.setNucleotideLength( ( new Long( blatResult.getTargetEnd() - blatResult.getTargetStart() ) ).intValue() );
        pl.setStrand( blatResult.getStrand() );
        pl.setBin( SequenceBinUtils.binFromRange( pl.getNucleotide().intValue(), pl.getNucleotide().intValue()
                + pl.getNucleotideLength().intValue() ) );

        return pl;
    }

    public void setProbeAlignedRegionService( ProbeAlignedRegionService probeAlignedRegionService ) {
        this.probeAlignedRegionService = probeAlignedRegionService;
    }

    public void setChromosomeService( ChromosomeService chromosomeService ) {
        this.chromosomeService = chromosomeService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }
}
