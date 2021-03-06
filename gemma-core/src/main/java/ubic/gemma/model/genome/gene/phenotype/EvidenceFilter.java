/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype;

import java.util.Collection;

/**
 * Used to filter values received depending on taxon and privacy chosen
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class EvidenceFilter {

    private Long taxonId = 0L;
    private boolean showOnlyEditable = false;
    private Collection<Long> externalDatabaseIds = null;

    public EvidenceFilter() {
        super();
    }

    public EvidenceFilter( Long taxonId, boolean showOnlyEditable ) {
        super();
        this.taxonId = taxonId;
        this.showOnlyEditable = showOnlyEditable;
    }

    public EvidenceFilter( Long taxonId, boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {
        super();
        this.taxonId = taxonId;
        this.showOnlyEditable = showOnlyEditable;
        this.externalDatabaseIds = externalDatabaseIds;
    }

    public Collection<Long> getExternalDatabaseIds() {
        return externalDatabaseIds;
    }

    public void setExternalDatabaseIds( Collection<Long> externalDatabaseIds ) {
        this.externalDatabaseIds = externalDatabaseIds;
    }

    public Long getTaxonId() {
        return this.taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public boolean isShowOnlyEditable() {
        return this.showOnlyEditable;
    }

    public void setShowOnlyEditable( boolean showOnlyEditable ) {
        this.showOnlyEditable = showOnlyEditable;
    }

}
