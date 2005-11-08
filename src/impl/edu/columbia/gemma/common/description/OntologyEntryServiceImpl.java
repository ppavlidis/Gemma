/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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

package edu.columbia.gemma.common.description;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.description.OntologyEntryService
 */
public class OntologyEntryServiceImpl extends edu.columbia.gemma.common.description.OntologyEntryServiceBase {

    /**
     * @see edu.columbia.gemma.common.description.OntologyEntryService#findOrCreate(edu.columbia.gemma.common.description.OntologyEntry)
     */
    protected edu.columbia.gemma.common.description.OntologyEntry handleFindOrCreate(
            edu.columbia.gemma.common.description.OntologyEntry ontologyEntry ) throws java.lang.Exception {
        return this.getOntologyEntryDao().findOrCreate( ontologyEntry );
    }

    /**
     * @see edu.columbia.gemma.common.description.OntologyEntryService#remove(edu.columbia.gemma.common.description.OntologyEntry)
     */
    protected void handleRemove( edu.columbia.gemma.common.description.OntologyEntry ontologyEntry )
            throws java.lang.Exception {
        this.getOntologyEntryDao().remove( ontologyEntry );
    }

}