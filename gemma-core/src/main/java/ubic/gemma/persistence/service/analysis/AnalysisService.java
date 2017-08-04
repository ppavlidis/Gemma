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
package ubic.gemma.persistence.service.analysis;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Map;

/**
 * Provides basic services for dealing with analyses
 *
 * @author Gemma
 */
public interface AnalysisService<T extends Analysis> {

    /**
     * deletes the given analysis from the system
     */
    @Secured({ "GROUP_USER", "ACL_ANALYSIS_EDIT" })
    void delete( T toDelete );

    /**
     * find all the analyses that involved the given investigation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByInvestigation( Investigation investigation );

    /**
     *      * Given a collection of investigations returns a Map of Analysis --> collection of Investigations
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ", "AFTER_ACL_MAP_READ" })
    Map<Investigation, Collection<T>> findByInvestigations( Collection<? extends Investigation> investigations );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByParentTaxon( Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByTaxon( Taxon taxon );

    /**
     * An analysis is uniquely determined by its set of investigations. Only returns an analysis if the collection of
     * investigations given exacly matches other wise returns null
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    T findByUniqueInvestigations( Collection<? extends Investigation> investigations );

    /**
     * Not secured: for internal use only
     *
     * @param idsToFilter starting list of bioassayset ids.
     * @return the ones which have a coexpression analysis.
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * Not secured: for internal use only
     *
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    /**
     * Returns the analysis with the specified ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    T load( Long id );

    /**
     * Returns all of the analysis objects
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> loadAll();

    /**
     * Returns the {@link Analysis}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific write permissions on (as opposed to analyses which are public) and which are "Enabled". Important: This
     * method will return all analyses if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute.
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<T> loadMyAnalyses();

    /**
     * Returns the {@link Analysis}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to analyses which are public) and which are "Enabled". Important: This
     * method will return all analyses if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyPrivateData for
     * processConfigAttribute.
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_SHARED_DATA" })
    Collection<T> loadMySharedAnalyses();
}