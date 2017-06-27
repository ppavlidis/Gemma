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
package ubic.gemma.persistence.service.common.description;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseVoEnabledDao;
import ubic.gemma.persistence.service.BrowsingDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see BibliographicReference
 */
public interface BibliographicReferenceDao extends BrowsingDao<BibliographicReference>,
        BaseVoEnabledDao<BibliographicReference, BibliographicReferenceValueObject> {

    BibliographicReference findByExternalId( String id, String databaseName );

    /**
     * <p>
     * Find by the external database id, such as for PubMed
     * </p>
     */
    BibliographicReference findByExternalId( DatabaseEntry externalId );

    Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences();

    Collection<ExpressionExperiment> getRelatedExperiments( BibliographicReference bibliographicReference );

    BibliographicReference thaw( BibliographicReference bibliographicReference );

    Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences );

    Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records );

    Collection<Long> listAll();

}
