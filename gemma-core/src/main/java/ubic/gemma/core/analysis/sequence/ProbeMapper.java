package ubic.gemma.core.analysis.sequence;

import ubic.gemma.core.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("unused") // Possible external use
public interface ProbeMapper {

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associatd with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product. Default
     * settings (ProbeMapperConfig) are used.
     * This is a major entry point for this API.
     *
     * @return A map of sequence names to collections of blat associations for each sequence.
     * @see ProbeMapperConfig
     */
    Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults );

    /**
     * Given some blat results (possibly for multiple sequences) determine which if any gene products they should be
     * associated with; if there are multiple results for a single sequence, these are further analyzed for specificity
     * and redundancy, so that there is a single BlatAssociation between any sequence andy andy gene product.
     * This is a major entry point for this API.
     *
     * @return A map of sequence names to collections of blat associations for each sequence.
     */
    Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults, ProbeMapperConfig config );

    /**
     * Given a genbank accession (for a mRNA or EST), find alignment data from GoldenPath.
     */
    Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb, String genbankId );

    Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds );

    /**
     * Get BlatAssociation results for a single sequence. If you have multiple sequences to run it is always better to
     * use processSequences();
     */
    Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath, BioSequence sequence );

    /**
     * Given a collection of sequences, blat them against the selected genome.
     *
     * @param goldenpath for the genome to be used.
     */
    Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences, ProbeMapperConfig config );

}