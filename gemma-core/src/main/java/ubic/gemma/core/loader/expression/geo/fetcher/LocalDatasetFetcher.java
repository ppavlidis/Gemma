/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo.fetcher;

import ubic.gemma.model.common.description.LocalFile;

import java.io.File;
import java.util.Collection;

/**
 * Used for testing, but might have other uses, to fetch GEO data from local files instead of the GEO website.
 *
 * @author pavlidis
 */
public class LocalDatasetFetcher extends DatasetFetcher {

    private final String localPath;

    public LocalDatasetFetcher( String localPath ) {
        super();
        this.localPath = localPath;
    }

    @Override
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking GSE file for " + accession );

        assert localPath != null;

        String seekFileName = localPath + "/" + accession + SOFT_GZ;
        File seekFile = new File( seekFileName );

        if ( seekFile.canRead() ) {
            return getFile( accession, seekFileName );
        }

        throw new RuntimeException( "Failed to find " + seekFileName );
    }

}
