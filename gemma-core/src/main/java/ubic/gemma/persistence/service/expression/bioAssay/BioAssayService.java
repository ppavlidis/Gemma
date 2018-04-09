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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;

/**
 * @author kelsey
 */
@Service
public interface BioAssayService extends BaseVoEnabledService<BioAssay, BioAssayValueObject> {

    /**
     * Associates a bioMaterial with a specified bioAssay.
     */
    @PreAuthorize("hasPermission(#bioAssay, 'write') or hasPermission(#bioAssay, 'administration')")
    void addBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    @Override
    @Secured({ "GROUP_USER" })
    BioAssay create( BioAssay bioAssay );

    /**
     * Locate all BioAssayDimensions in which the selected BioAssay occurs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    /**
     * @param accession eg GSM12345.
     * @return BioAssays that match based on the plain accession (unconstrained by ExternalDatabase).
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> findByAccession( String accession );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    BioAssay findOrCreate( BioAssay bioAssay );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    BioAssay load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> loadAll();

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> load( Collection<Long> ids );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( BioAssay bioAssay );

    /**
     * <p>
     * Removes the association between a specific bioMaterial and a bioAssay.
     * </p>
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    void thaw( BioAssay bioAssay );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> thaw( Collection<BioAssay> bioAssays );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( BioAssay bioAssay );

    Collection<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, boolean basic );
}