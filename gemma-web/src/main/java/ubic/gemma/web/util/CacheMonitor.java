/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.web.util;

/**
 * @author paul
 */
public interface CacheMonitor {

    /**
     * Remove all items from all caches.
     */
    void clearAllCaches();

    /**
     * Remove all items from the cache given
     *
     * @param cacheName cache
     */
    void clearCache( String cacheName );

    void disableStatistics();

    void enableStatistics();

    String getStats();

}