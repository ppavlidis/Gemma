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
//
// Attention: Generated code! Do not modify by hand!
// Generated by: SpringDao.vsl in andromda-spring-cartridge.
//
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
public interface ExpressionExperimentDao extends ubic.gemma.model.expression.experiment.BioAssaySetDao {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes entities must be transformed into objects of type
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject}.
     */
    public final static int TRANSFORM_EXPRESSIONEXPERIMENTVALUEOBJECT = 1;

    /**
     * Copies the fields of the specified entity to the target value object. This method is similar to
     * toExpressionExperimentValueObject(), but it does not handle any attributes in the target value object that are
     * "read-only" (as those do not have setter methods exposed).
     */
    public void toExpressionExperimentValueObject(
            ubic.gemma.model.expression.experiment.ExpressionExperiment sourceEntity,
            ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject targetVO );

    /**
     * Converts this DAO's entity to an object of type
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject}.
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject toExpressionExperimentValueObject(
            ubic.gemma.model.expression.experiment.ExpressionExperiment entity );

    /**
     * Converts this DAO's entity to a Collection of instances of type
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject}.
     */
    public void toExpressionExperimentValueObjectCollection( java.util.Collection entities );

    /**
     * Copies the fields of {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject} to the
     * specified entity.
     * 
     * @param copyIfNull If FALSE, the value object's field will not be copied to the entity if the value is NULL. If
     *        TRUE, it will be copied regardless of its value.
     */
    public void expressionExperimentValueObjectToEntity(
            ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject sourceVO,
            ubic.gemma.model.expression.experiment.ExpressionExperiment targetEntity, boolean copyIfNull );

    /**
     * Converts an instance of type {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject} to
     * this DAO's entity.
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperimentValueObjectToEntity(
            ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject expressionExperimentValueObject );

    /**
     * Converts a Collection of instances of type
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject} to this DAO's entity.
     */
    public void expressionExperimentValueObjectToEntityCollection( java.util.Collection instances );

    /**
     * Loads an instance of ubic.gemma.model.expression.experiment.ExpressionExperiment from the persistent store.
     */
    public ubic.gemma.model.common.Securable load( java.lang.Long id );

    /**
     * <p>
     * Does the same thing as {@link #load(java.lang.Long)} with an additional flag called <code>transform</code>. If
     * this flag is set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be
     * transformed. If this flag is any of the other constants defined in this class then the result <strong>WILL
     * BE</strong> passed through an operation which can optionally transform the entity (into a value object for
     * example). By default, transformation does not occur.
     * </p>
     * 
     * @param id the identifier of the entity to load.
     * @return either the entity or the object transformed from the entity.
     */
    public Object load( int transform, java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection<ExpressionExperiment> loadAll();

    /**
     * <p>
     * Does the same thing as {@link #loadAll()} with an additional flag called <code>transform</code>. If this flag is
     * set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be transformed. If this
     * flag is any of the other constants defined here then the result <strong>WILL BE</strong> passed through an
     * operation which can optionally transform the entity (into a value object for example). By default, transformation
     * does not occur.
     * </p>
     * 
     * @param transform the flag indicating what transformation to use.
     * @return the loaded entities.
     */
    public java.util.Collection<ExpressionExperiment> loadAll( final int transform );

    /**
     * Creates an instance of ubic.gemma.model.expression.experiment.ExpressionExperiment and adds it to the persistent
     * store.
     */
    public ubic.gemma.model.common.Securable create(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the
     * returned entity will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here
     * then the result <strong>WILL BE</strong> passed through an operation which can optionally transform the entity
     * (into a value object for example). By default, transformation does not occur.
     * </p>
     */
    public Object create( int transform,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * Creates a new instance of ubic.gemma.model.expression.experiment.ExpressionExperiment and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.expression.experiment.ExpressionExperiment instances to
     *        create.
     * @return the created instances.
     */
    public java.util.Collection create( java.util.Collection entities );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the
     * returned entity will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here
     * then the result <strong>WILL BE</strong> passed through an operation which can optionally transform the entities
     * (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection create( int transform, java.util.Collection entities );

    /**
     * Updates the <code>expressionExperiment</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.expression.experiment.ExpressionExperiment from the persistent store.
     */
    public void remove( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * Removes the instance of ubic.gemma.model.expression.experiment.ExpressionExperiment having the given
     * <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findOrCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in
     * {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)}.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findOrCreate( String queryString,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as
     * {@link #findOrCreate(boolean, ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findOrCreate(int, ubic.gemma.model.expression.experiment.ExpressionExperiment
     * expressionExperiment)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByName( String queryString,
            java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(int, java.lang.String name)}.
     * </p>
     */
    public Object findByName( int transform, String queryString, java.lang.String name );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment find(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(ubic.gemma.model.expression.experiment.ExpressionExperiment)}.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment find( String queryString,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.expression.experiment.ExpressionExperiment)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.expression.experiment.ExpressionExperiment)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(int,
     * ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)}.
     * </p>
     */
    public Object find( int transform, String queryString,
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByAccession( String queryString,
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByAccession( int transform, ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(boolean, ubic.gemma.model.common.description.DatabaseEntry)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByAccession(int,
     * ubic.gemma.model.common.description.DatabaseEntry accession)}.
     * </p>
     */
    public Object findByAccession( int transform, String queryString,
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByShortName( java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByShortName(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByShortName( String queryString,
            java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByShortName( int transform, java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByShortName(int, java.lang.String shortName)}.
     * </p>
     */
    public Object findByShortName( int transform, String queryString, java.lang.String shortName );

    /**
     * <p>
     * Function to get a count of an expressionExperiment's designelementdatavectors, grouped by quantitation type.
     * </p>
     */
    public java.util.Map getQuantitationTypeCountById( java.lang.Long Id );

    /**
     * 
     */
    public long getDesignElementDataVectorCountById( long id );

    /**
     * 
     */
    public long getBioAssayCountById( long id );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Gets the taxon for the given expressionExperiment
     * </p>
     */
    public ubic.gemma.model.genome.Taxon getTaxon( java.lang.Long ExpressionExperimentID );

    /**
     * <p>
     * Thaws the BioAssays associated with the given ExpressionExperiment and their associations, but not the
     * DesignElementDataVectors.
     * </p>
     */
    public void thawBioAssays( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public java.util.Collection<DesignElementDataVector> getSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, java.lang.Integer limit );

    /**
     * 
     */
    public java.util.Collection getQuantitationTypes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public java.util.Collection getDesignElementDataVectors( java.util.Collection designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * <p>
     * Function to get a count of expression experiments, grouped by Taxon
     * </p>
     */
    public java.util.Map<Taxon, Long> getPerTaxonCount();

    /**
     * 
     */
    public java.util.Collection<ExpressionExperimentValueObject> loadAllValueObjects();

    /**
     * 
     */
    public java.util.Collection<ExpressionExperimentValueObject> loadValueObjects( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> findByTaxon( ubic.gemma.model.genome.Taxon taxon );

    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType );

    /**
     * <p>
     * Get the quantitation types for the expression experiment, for the array design specified. This is really only
     * useful for expression experiments that use more than one array design.
     * </p>
     */
    public java.util.Collection<QuantitationType> getQuantitationTypes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public long getProcessedExpressionVectorCount(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public long getBioMaterialCount( ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> load( java.util.Collection ids );

    /**
     * 
     */
    public java.util.Collection getDesignElementDataVectors( java.util.Collection quantitationTypes );

    /**
     * <p>
     * Gets the AuditEvents of the specified expression experiment ids. This returns a map of id -> AuditEvent. If the
     * events do not exist, the map entry will point to null.
     * </p>
     */
    public java.util.Map getAuditEvents( java.util.Collection<Long> ids );

    /**
     * <p>
     * returns a collection of expression experiments that have an AD that assays for the given gene
     * </p>
     */
    public java.util.Collection<ExpressionExperiment> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Returns a collection of expression experiments that detected the given gene at a level greater than the given
     * rank (percentile)
     * </p>
     */
    public java.util.Collection<ExpressionExperiment> findByExpressedGene( ubic.gemma.model.genome.Gene gene,
            java.lang.Double rank );

    /**
     * <p>
     * Finds all the EE's that reference the given bibliographicReference id
     * </p>
     */
    public java.util.Collection<ExpressionExperiment> findByBibliographicReference( java.lang.Long bibRefID );

    /**
     * <p>
     * Gets the last audit event for the AD's associated with the given EE.
     * </p>
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastArrayDesignUpdate(
            ubic.gemma.model.expression.experiment.ExpressionExperiment ee, java.lang.Class eventType );

    /**
     * 
     */
    public java.util.Map getArrayDesignAuditEvents( java.util.Collection<Long> ids );

    /**
     * <p>
     * Retrieve a collection of the genes assayed in the experiment.
     * </p>
     */
    public java.util.Collection<Gene> getAssayedGenes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            java.lang.Double rankThreshold );

    /**
     * <p>
     * See getAssayedGenes
     * </p>
     */
    public java.util.Collection<CompositeSequence> getAssayedProbes(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            java.lang.Double rankThreshold );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByBioMaterial(
            ubic.gemma.model.expression.biomaterial.BioMaterial bm );

    /**
     * 
     */
    public java.util.Map getLastArrayDesignUpdate( java.util.Collection expressionExperiments, java.lang.Class type );

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.ExpressionExperiment findByFactorValue(
            ubic.gemma.model.expression.experiment.FactorValue factorValue );

    /**
     * <p>
     * Get the map of ids to number of terms associated with each expression experiment.
     * </p>
     */
    public java.util.Map getAnnotationCounts( java.util.Collection ids );

    /**
     * <p>
     * Get map of ids to how many factor values the experiment has, counting only factor values which are associated
     * with biomaterials.
     * </p>
     */
    public java.util.Map getPopulatedFactorCounts( java.util.Collection ids );

    /**
     * 
     */
    public java.util.Map getSampleRemovalEvents( java.util.Collection expressionExperiments );

    /**
     * <p>
     * Returns the missing-value-masked preferred quantitation type for the experiment, if it exists, or null otherwise.
     * </p>
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType getMaskedPreferredQuantitationType(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * <p>
     * Return any ExpressionExperimentSubSets the given Experiment might have.
     * </p>
     */
    public java.util.Collection getSubSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> findByFactorValues( java.util.Collection factorValues );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperiment> findByBioMaterials( java.util.Collection bioMaterials );

    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment ee );

}
