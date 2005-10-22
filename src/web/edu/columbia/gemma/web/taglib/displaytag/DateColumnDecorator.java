/*
 * The Gemma project
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
package edu.columbia.gemma.web.taglib.displaytag;

import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;
import org.displaytag.decorator.ColumnDecorator;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DateColumnDecorator implements ColumnDecorator {

    private FastDateFormat dateFormat = FastDateFormat.getInstance( "yyyy" );

    /*
     * (non-Javadoc)
     * 
     * @see org.displaytag.decorator.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate( Object columnValue ) {
        assert columnValue instanceof Date;
        Date date = ( Date ) columnValue;
        return this.dateFormat.format( date );
    }

}
