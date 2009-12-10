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
package ubic.gemma.model.expression.biomaterial;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface BioMaterialService {

    /**
     * Copies a bioMaterial.
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public BioMaterial copy( BioMaterial bioMaterial );

    /**
     * 
     */
    public Integer countAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public BioMaterial create( BioMaterial bioMaterial );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public BioMaterial findOrCreate( BioMaterial bioMaterial );

    /**
     * @param bioMaterial
     * @return true if a matching biomaterial exists in the system.
     */
    @Secured("GROUP_USER")
    public boolean exists( BioMaterial bioMaterial );

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment getExpressionExperiment( Long id );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public BioMaterial load( Long id );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<BioMaterial> loadAll();

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<BioMaterial> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( BioMaterial bioMaterial );

    /**
     * <p>
     * Updates the given biomaterial to the database.
     * </p>
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( BioMaterial bioMaterial );

}
