/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.model.common.search.SearchSettings;

/**
 * 
 */
public abstract class UserQuery implements java.io.Serializable, gemma.gsec.model.SecuredNotChild {

    /**
     * Constructs new instances of {@link UserQuery}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link UserQuery}.
         */
        public static UserQuery newInstance() {
            return new UserQueryImpl();
        }

        /**
         * Constructs a new instance of {@link UserQuery}, taking all required and/or read-only properties as arguments.
         */
        public static UserQuery newInstance( String url, java.util.Date lastUsed ) {
            final UserQuery entity = new UserQueryImpl();
            entity.setUrl( url );
            entity.setLastUsed( lastUsed );
            return entity;
        }

        /**
         * Constructs a new instance of {@link UserQuery}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static UserQuery newInstance( String url, String name, java.util.Date lastUsed,
                Integer autoRunFrequencyHours, SearchSettings searchSettings ) {
            final UserQuery entity = new UserQueryImpl();
            entity.setUrl( url );
            entity.setName( name );
            entity.setLastUsed( lastUsed );
            entity.setAutoRunFrequencyHours( autoRunFrequencyHours );
            entity.setSearchSettings( searchSettings );
            return entity;
        }
    }

    private String url;

    private String name;

    private java.util.Date lastUsed;

    private Integer autoRunFrequencyHours;

    private Long id;

    private SearchSettings searchSettings;

    /**
     * Returns <code>true</code> if the argument is an UserQuery instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof UserQuery ) ) {
            return false;
        }
        final UserQuery that = ( UserQuery ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * How often to auto-run this query, given in hours.
     * </p>
     */
    public Integer getAutoRunFrequencyHours() {
        return this.autoRunFrequencyHours;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public java.util.Date getLastUsed() {
        return this.lastUsed;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     */
    public SearchSettings getSearchSettings() {
        return this.searchSettings;
    }

    /**
     * 
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setAutoRunFrequencyHours( Integer autoRunFrequencyHours ) {
        this.autoRunFrequencyHours = autoRunFrequencyHours;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLastUsed( java.util.Date lastUsed ) {
        this.lastUsed = lastUsed;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setSearchSettings( SearchSettings searchSettings ) {
        this.searchSettings = searchSettings;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

}