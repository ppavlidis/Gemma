/*
 * The Gemma project
 *
 * Copyright (c) 2020 University of British Columbia
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
package ubic.gemma.web.tomcat;

import org.apache.catalina.LifecycleException;

import java.io.File;
import java.io.IOException;

/**
 * Embedded Tomcat server for development purposes.
 */
public class Tomcat {

    public static void main( String[] args ) throws LifecycleException, IOException {
        org.apache.catalina.startup.Tomcat server = new org.apache.catalina.startup.Tomcat();
        server.setPort( 8080 );
        server.addWebapp( "/Gemma", new File( "gemma-web/target/Gemma" ).getAbsolutePath() );
        server.start();
        server.getServer().await();
    }
}
