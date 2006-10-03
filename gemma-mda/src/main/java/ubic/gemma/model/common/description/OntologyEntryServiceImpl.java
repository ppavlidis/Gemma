/*
 * The Gemma project.
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

package ubic.gemma.model.common.description;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.description.OntologyEntryService
 */
public class OntologyEntryServiceImpl extends ubic.gemma.model.common.description.OntologyEntryServiceBase {

    /**
     * @see ubic.gemma.model.common.description.OntologyEntryService#findOrCreate(ubic.gemma.model.common.description.OntologyEntry)
     */
    protected ubic.gemma.model.common.description.OntologyEntry handleFindOrCreate(
            ubic.gemma.model.common.description.OntologyEntry ontologyEntry ) throws java.lang.Exception {
        return this.getOntologyEntryDao().findOrCreate( ontologyEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.OntologyEntryService#remove(ubic.gemma.model.common.description.OntologyEntry)
     */
    protected void handleRemove( ubic.gemma.model.common.description.OntologyEntry ontologyEntry )
            throws java.lang.Exception {
        this.getOntologyEntryDao().remove( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleCreate(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected OntologyEntry handleCreate( OntologyEntry ontologyEntry ) throws Exception {
        return ( OntologyEntry ) this.getOntologyEntryDao().create( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleUpdate(ubic.gemma.model.common.description.OntologyEntry)
     */
    @Override
    protected void handleUpdate( OntologyEntry ontologyEntry ) throws Exception {
        this.getOntologyEntryDao().update( ontologyEntry );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.OntologyEntryServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected OntologyEntry handleLoad( Long id ) throws Exception {
        return ( OntologyEntry ) this.getOntologyEntryDao().load( id );
    }

}