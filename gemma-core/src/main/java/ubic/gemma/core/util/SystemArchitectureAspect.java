/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * General-purpose pointcuts to recognize CRUD operations etc.
 * For help with expressions see http://static.springsource.org/spring/docs/2.5.x/reference/aop.html#6.2.3.4
 *
 * @author paul
 */
@SuppressWarnings({ "EmptyMethod", "unused" }) // Expected
@Aspect
public class SystemArchitectureAspect {

    /**
     * Methods that create new objects in the persistent store
     */
    @Pointcut("ubic.gemma.core.util.SystemArchitectureAspect.daoMethod() && ( execution(* save(..)) || execution(* create*(..)) || execution(* findOrCreate(..)) || execution(* persist*(..)) || execution(* add*(..))   )")
    public void creator() {//
    }

    @Pointcut("ubic.gemma.core.util.SystemArchitectureAspect.deleter() ||ubic.gemma.core.util.SystemArchitectureAspect.loader() || ubic.gemma.core.util.SystemArchitectureAspect.creator() || ubic.gemma.core.util.SystemArchitectureAspect.updater()")
    public void crud() {//
    }

    /**
     * This pointcut is used to apply audit and acl advice at DAO boundary.
     */
    @Pointcut("@target(org.springframework.stereotype.Repository) && execution(public * ubic.gemma..*.*(..))")
    public void daoMethod() {//
    }

    /**
     * Methods that remove items in the persistent store
     */
    @Pointcut("ubic.gemma.core.util.SystemArchitectureAspect.daoMethod() && (execution(* remove(..)) || execution(* delete*(..)))")
    public void deleter() {//
    }

    /**
     * Encompasses the 'model' packages
     */
    @Pointcut("within(ubic.gemma.model..*)")
    public void inModelLayer() {
    }

    /**
     * Encompasses the 'web' packages
     */
    @SuppressWarnings("Annotator") // Because it is in a different project
    @Pointcut("within(ubic.gemma.web..*)")
    public void inWebLayer() {
    }

    /**
     * Methods that load (read) from the persistent store
     */
    @Pointcut("ubic.gemma.core.util.SystemArchitectureAspect.daoMethod() && (execution(* load(..)) || execution(* loadAll(..)) || execution(* read(..)))")
    public void loader() {//
    }

    /**
     * Create, remove or update methods - with the exception of @Services flagged as @Infrastructure
     * that, probably.
     */
    @Pointcut(" ubic.gemma.core.util.SystemArchitectureAspect.creator() || ubic.gemma.core.util.SystemArchitectureAspect.updater() || ubic.gemma.core.util.SystemArchitectureAspect.deleter( )")
    public void modifier() {
    }

    /**
     * A entity service method: a public method in a \@Service.
     */
    @Pointcut("@target(org.springframework.stereotype.Service) && execution(public * ubic.gemma..*.*(..))")
    public void serviceMethod() {
        /*
         * Important document:
         * http://forum.springsource.org/showthread.php?28525-Difference-between-target-and-within-in-Spring-AOP
         *
         * Using @target makes a proxy out of everything, which causes problems if services aren't implementing
         * interfaces -- seems for InitializingBeans in particular. @within doesn't work, at least for the ACLs.
         */
    }

    @Pointcut("@target(org.springframework.stereotype.Service) && (execution(public * ubic.gemma..*.*(*)) || execution(public * ubic.gemma..*.*(*,..)))")
    public void serviceMethodWithArg() {//
    }

    /**
     * Methods which are marked as @Transactional
     */
    @Pointcut("@target(org.springframework.transaction.annotation.Transactional) && execution(public * ubic.gemma..*.*(..))")
    public void transactional() {
    }

    /**
     * Methods that update items in the persistent store
     */
    @Pointcut("ubic.gemma.core.util.SystemArchitectureAspect.daoMethod() && execution(* update(..))")
    public void updater() {
    }

}
