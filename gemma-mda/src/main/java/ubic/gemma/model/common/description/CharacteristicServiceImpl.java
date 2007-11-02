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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @see ubic.gemma.model.common.description.CharacteristicService
 */
public class CharacteristicServiceImpl extends ubic.gemma.model.common.description.CharacteristicServiceBase {

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    protected java.util.Collection handleFindByValue( java.lang.String search ) throws java.lang.Exception {
        return this.getCharacteristicDao().findByValue( search + '%');
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleFindByParentClass(java.lang.Object)
     */
    @Override
    protected Map handleFindByParentClass( java.lang.Class parentClass ) {
        return this.getCharacteristicDao().findByParentClass( parentClass );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParent(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    protected Object handleGetParent( Characteristic characteristic ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParents(java.util.Collection)
     */
    @Override
    protected Map handleGetParents( Collection characteristics ) throws Exception {
        Map charToParent = new HashMap<Characteristic, Object>();
        
        return charToParent;
    }

	@Override
	protected Collection handleFindByUri(String searchString) throws Exception {
		return this.getCharacteristicDao().findByUri(searchString);
	}

}