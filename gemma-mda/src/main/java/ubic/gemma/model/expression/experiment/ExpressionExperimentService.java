/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.monitor.Monitored;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ExpressionExperimentService {

    /**
     * Count how many ExpressionExperiments are in the database
     */
    public java.lang.Integer countAll();

    @Secured( { "GROUP_USER" })
    public ExpressionExperiment create( ExpressionExperiment expressionExperiment );

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically, though this behavior could be changed)
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void delete( ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment find( ExpressionExperiment expressionExperiment );

    /**
     * @param accession
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByAccession( ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * @param accession
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByAccession( String accession );

    /**
     * given a bibliographicReference returns a collection of EE that have that reference that BibliographicReference
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByBibliographicReference(
            ubic.gemma.model.common.description.BibliographicReference bibRef );

    /**
     * Given a bioMaterial returns an expressionExperiment
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bm );

    /**
     * @param bioMaterials
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByBioMaterials( Collection<BioMaterial> bioMaterials );

    /**
     * Returns a collection of expression experiment ids that express the given gene above the given expression level
     * 
     * @param gene
     * @param rank
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByExpressedGene( ubic.gemma.model.genome.Gene gene, double rank );

    /**
     * @param factorValue
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByFactorValue( FactorValue factorValue );

    /**
     * @param factorValues
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> factorValues );

    /**
     * Returns a collection of expression experiments that have an AD that detects the given Gene (ie a probe on the AD
     * hybidizes to the given Gene)
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * @param investigator
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByInvestigator(
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    /**
     * @param name
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByName( java.lang.String name );

    /**
     * gets all EE that match the given parent Taxon
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByParentTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * @param type
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByQuantitationType( QuantitationType type );

    /**
     * @param shortName
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment findByShortName( java.lang.String shortName );

    /**
     * gets all EE that match the given Taxon
     */
    /**
     * @param taxon
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> findByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment );

    /**
     * Get the map of ids to number of terms associated with each expression experiment.
     */
    public Map<Long, Integer> getAnnotationCounts( Collection<Long> ids );

    /**
     * Returns a collection of ArrayDesigns referenced by any of the BioAssays that make up the given
     * ExpressionExperiment.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment expressionExperiment );

    /**
     * Retrieve the BioAssayDimensions for the study.
     * 
     * @param expressionExperiment
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment );

    /**
     * Counts the number of biomaterials associated with this expression experiment.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer getBioMaterialCount( ExpressionExperiment expressionExperiment );

    /**
     * @param id
     * @return
     */
    public Integer getDesignElementDataVectorCountById( long id );

    /**
     * Find vectors constrained to the given quantitation type and design elements. Returns vectors for all experiments
     * the user has access to.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Get all the vectors for the given quantitation types.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes );

    /**
     * @param expressionExperiments
     * @param type
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<ExpressionExperiment, AuditEvent> getLastArrayDesignUpdate(
            Collection<ExpressionExperiment> expressionExperiments, Class<? extends AuditEventType> type );

    /**
     * Get the date of the last time any of the array designs associated with this experiment were updated.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public AuditEvent getLastArrayDesignUpdate( ExpressionExperiment expressionExperiment,
            java.lang.Class<? extends AuditEventType> eventType );

    /**
     * Gets the AuditEvents of the latest link analyses for the specified expression experiment ids. This returns a map
     * of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastLinkAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest missing value analysis for the specified expression experiment ids. This
     * returns a map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( Collection<Long> ids );

    /**
     * Gets the AuditEvents of the latest rank computation for the specified expression experiment ids. This returns a
     * map of id -> AuditEvent. If the events do not exist, the map entry will point to null.
     */
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( Collection<Long> ids );

    /**
     * @param ids
     * @return
     */
    public Map<Long, AuditEvent> getLastTroubleEvent( Collection<Long> ids );

    /**
     * @param ids
     * @return
     */
    public Map<Long, AuditEvent> getLastValidationEvent( Collection<Long> ids );

    /**
     * Function to get a count of expression experiments, grouped by Taxon
     */
    public Map<Taxon, Long> getPerTaxonCount();

    /**
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials.
     */
    public Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids );

    /**
     * Iterates over the quantiation types for a given expression experiment and returns the preferred quantitation
     * types.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getPreferredQuantitationType( ExpressionExperiment EE );

    /**
     * @param ee
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee );

    /**
     * Counts the number of ProcessedExpressionDataVectors.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer getProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment );

    /**
     * Function to get a count of an expressionExperiment's designelementdatavectors, grouped by quantitation type
     */
    public Map<QuantitationType, Long> getQuantitationTypeCountById( java.lang.Long Id );

    /**
     * Return all the quantitation types used by the given expression experiment
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment );

    /**
     * Get the quantitation types for the expression experiment, for the array design specified. This is really only
     * useful for expression experiments that use more than one array design.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * @param expressionExperiments
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Retrieve some of the vectors for the given expressionExperiment and quantitation type. Used for peeking at the
     * data without retrieving the whole data set.
     * 
     * @param quantitationType
     * @param limit
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<DesignElementDataVector> getSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit );

    /**
     * Return any ExpressionExperimentSubSets this Experiment might have.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment );

    /**
     * Returns the taxon of the given expressionExperiment.
     */
    public ubic.gemma.model.genome.Taxon getTaxon( java.lang.Long ExpressionExperimentID );

    @Monitored
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment load( java.lang.Long id );

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadAll();

    /**
     * Return data sets that don't have the given event type.
     * 
     * @param eventType
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadLackingEvent( Class<? extends AuditEventType> eventType );

    /**
     * Return data sets that do have the given event type.
     * 
     * @param eventType
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadWithEvent( Class<? extends AuditEventType> eventType );

    /**
     * TODO SECURE: How to secure value objects, should take a secured EE or a collection of secured EE's....?
     * 
     * @return
     */
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects();

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadMultiple( Collection<Long> ids );

    /**
     * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific write permissions on (as opposed to data sets which are public). Important: This method
     * will return all experiments if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<ExpressionExperiment> loadMyExpressionExperiments();

    /**
     * * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific READ permissions on (as opposed to data sets which are public). Important: This method
     * will return all experiments if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyPrivateData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<ExpressionExperiment> loadMySharedExpressionExperiments();

    /**
     * TODO SECURE: How to secure value objects, should take a secured EE or a collection of secured EE's....?
     * 
     * @param ids
     * @return
     */
    public Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * @param expressionExperiment
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    /**
     * Partially thaw the expression experiment given - do not thaw the raw data.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public ExpressionExperiment thawLite( ExpressionExperiment expressionExperiment );

    /**
     * @param expressionExperiment
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionExperiment expressionExperiment );

    /**
     * Return up to Math.abs(limit) experiments that were most recently updated (limit >0) or least recently updated
     * (limit < 0).
     * 
     * @param idsOfFetched
     * @param limit
     * @return map of EE to last update event date.
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_MAP_READ" })
    public Map<ExpressionExperiment, Date> findByUpdatedLimit( Collection<Long> idsOfFetched, Integer limit );

    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadTroubled();

    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadLackingFactors();

    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> loadLackingTags();

}
