/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.auditAndSecurity.Securable;

/**
 * Carries extensive security information about an entity.
 * 
 * @author paul
 * @version $Id$
 */
public class SecurityInfoValueObject {

    private static final long serialVersionUID = 2166768356457316142L;

    /**
     * Groups the user has control over.
     */
    private Collection<String> availableGroups = new HashSet<String>();

    /**
     * Current focus. Can be null
     */
    private String currentGroup = null;

    private Boolean currentGroupCanRead = false;

    private Boolean currentGroupCanWrite = false;
    
    private Boolean currentUserCanwrite = false;

    private Boolean currentUserOwns = false;

    private String entityClazz;

    private String entityDescription;

    private Long entityId;
   
    private String entityName;

    private String entityShortName;
   
    private Collection<String> groupsThatCanRead = new HashSet<String>();
   
    private Collection<String> groupsThatCanWrite = new HashSet<String>();
  
    private boolean isPubliclyReadable;
  
    private boolean isShared;
  
    /**
     * Principal who owns the data. Can be null.
     */
    private SidValueObject owner;
  
    public SecurityInfoValueObject() {
    }

    /**
     * @param s to initialize. Security information will not be filled in.
     */
    public SecurityInfoValueObject( Securable s ) {
        this.entityClazz = s.getClass().getName();
        this.entityId = s.getId();
    }

    public Collection<String> getAvailableGroups() {
        return availableGroups;
    }

    public String getCurrentGroup() {
        return currentGroup;
    }

    /**
     * @return the currentUserCanwrite
     */
    public Boolean getCurrentUserCanwrite() {
        return currentUserCanwrite;
    }

    /**
     * @return the entityClazz
     */
    public String getEntityClazz() {
        return entityClazz;
    }

    /**
     * @return the entityDescription
     */
    public String getEntityDescription() {
        return entityDescription;
    }

    /**
     * @return the entityId
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * @return the entityName
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return the entityShortName
     */
    public String getEntityShortName() {
        return entityShortName;
    }

    /**
     * @return the groupsThatCanRead
     */
    public Collection<String> getGroupsThatCanRead() {
        return groupsThatCanRead;
    }

    /**
     * @return the groupsThatCanWrite
     */
    public Collection<String> getGroupsThatCanWrite() {
        return groupsThatCanWrite;
    }

    public SidValueObject getOwner() {
        return owner;
    }

    public boolean isCurrentGroupCanRead() {
        return currentGroupCanRead;
    }

    public boolean isCurrentGroupCanWrite() {
        return currentGroupCanWrite;
    }

    public boolean isPubliclyReadable() {
        return isPubliclyReadable;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setAvailableGroups( Collection<String> availableGroups ) {
        this.availableGroups = availableGroups;
    }

    public void setCurrentGroup( String currentGroup ) {
        this.currentGroup = currentGroup;
    }

    public void setCurrentGroupCanRead( boolean currentGroupCanRead ) {
        this.currentGroupCanRead = currentGroupCanRead;
    }

    public void setCurrentGroupCanWrite( boolean currentGroupCanWrite ) {
        this.currentGroupCanWrite = currentGroupCanWrite;
    }

    /**
     * @param currentUserCanwrite the currentUserCanwrite to set
     */
    public void setCurrentUserCanwrite( Boolean currentUserCanwrite ) {
        this.currentUserCanwrite = currentUserCanwrite;
    }

    /**
     * @param entityClazz the entityClazz to set
     */
    public void setEntityClazz( String entityClazz ) {
        this.entityClazz = entityClazz;
    }

    /**
     * @param entityDescription the entityDescription to set
     */
    public void setEntityDescription( String entityDescription ) {
        this.entityDescription = entityDescription;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId( Long entityId ) {
        this.entityId = entityId;
    }

    /**
     * @param entityName the entityName to set
     */
    public void setEntityName( String entityName ) {
        this.entityName = entityName;
    }

    /**
     * @param entityShortName the entityShortName to set
     */
    public void setEntityShortName( String entityShortName ) {
        this.entityShortName = entityShortName;
    }

    /**
     * @param groupsThatCanRead the groupsThatCanRead to set
     */
    public void setGroupsThatCanRead( Collection<String> groupsThatCanRead ) {
        this.groupsThatCanRead = groupsThatCanRead;
    }

    /**
     * @param groupsThatCanWrite the groupsThatCanWrite to set
     */
    public void setGroupsThatCanWrite( Collection<String> groupsThatCanWrite ) {
        this.groupsThatCanWrite = groupsThatCanWrite;
    }

    public void setOwner( SidValueObject owner ) {
        this.owner = owner;
    }

    public void setPubliclyReadable( boolean isPubliclyReadable ) {
        this.isPubliclyReadable = isPubliclyReadable;
    }

    public void setShared( boolean isShared ) {
        this.isShared = isShared;
    }

    public void setCurrentUserOwns( Boolean currentUserOwns ) {
        this.currentUserOwns = currentUserOwns;
    }

    public Boolean getCurrentUserOwns() {
        return currentUserOwns;
    }

}
