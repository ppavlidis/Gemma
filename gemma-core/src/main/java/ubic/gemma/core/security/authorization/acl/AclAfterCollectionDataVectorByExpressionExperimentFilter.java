/*
 * The Gemma project
 *
 * Copyright (c) 2008-2010 University of British Columbia
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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.ByAssociationFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.expression.bioAssayData.DataVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.List;

/**
 * Filter collections of DesignElementDataVectors or DataVectorValueObjects based on the permissions of the associated
 * ExpressionExperiment(s).
 *
 * @author pavlidis (based in part on code from Acegi)
 */
public class AclAfterCollectionDataVectorByExpressionExperimentFilter
        extends ByAssociationFilteringProvider<ExpressionExperiment, Object> {

    private static final String CONFIG_ATTRIBUTE = "AFTER_ACL_DATAVECTOR_COLLECTION_READ";

    public AclAfterCollectionDataVectorByExpressionExperimentFilter( AclService aclService,
            List<Permission> requirePermission ) {
        super( aclService, AclAfterCollectionDataVectorByExpressionExperimentFilter.CONFIG_ATTRIBUTE,
                requirePermission );
    }

    @Override
    public String getProcessConfigAttribute() {
        return AclAfterCollectionDataVectorByExpressionExperimentFilter.CONFIG_ATTRIBUTE;
    }

    @Override
    protected ExpressionExperiment getAssociatedSecurable( Object targetDomainObject ) {
        ExpressionExperiment domainObject = null;
        if ( targetDomainObject instanceof DesignElementDataVector ) {
            domainObject = ( ( DesignElementDataVector ) targetDomainObject ).getExpressionExperiment();
        } else if ( targetDomainObject instanceof DataVectorValueObject ) {
            ExpressionExperimentValueObject expressionExperiment = ( ( DataVectorValueObject ) targetDomainObject )
                    .getExpressionExperiment();
            domainObject = ExpressionExperiment.Factory.newInstance();
            domainObject.setId( expressionExperiment.getId() );
        }
        return domainObject;
    }

}
