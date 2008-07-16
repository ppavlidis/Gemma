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
package ubic.gemma.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.FSDirectory;
import org.compass.core.spi.InternalCompass;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

/**
 * Utility methods to manipulate compass (and lucene).
 * 
 * @author keshav
 * @version $Id$
 */
public class CompassUtils {

	private static Log log = LogFactory.getLog(CompassUtils.class);

	/**
	 * Deletes compass lock file(s).
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void deleteCompassLocks() {
		/*
		 * FIXME lock directory is now the same as the indexes, by default.
		 */
		log.debug("Lucene index lock dir: " + FSDirectory.LOCK_DIR);

		Collection<File> lockFiles = FileUtils.listFiles(new File(
				FSDirectory.LOCK_DIR),
				FileFilterUtils.suffixFileFilter("lock"), null);

		if (lockFiles.size() == 0) {
			log.debug("Lucene lock files do not exist.");
			return;
		}

		for (File file : lockFiles) {
			log.debug("Removing Lucene lock file " + file);
			file.delete();
		}
	}

	/**
	 * disables the index mirroring operation.
	 * 
	 * @param device
	 */
	public static void disableIndexMirroring(CompassGpsInterfaceDevice device) {
		device.stop();
	}

	/**
	 * enables the index mirroring operation.
	 * 
	 * @param device
	 */
	public static void enableIndexMirroring(CompassGpsInterfaceDevice device) {
		device.start();
	}

	/**
	 * Deletes and re-creates the index.
	 *   See the IndexService
	 * @param gps
	 * @throws IOException
	 */
	public static synchronized void rebuildCompassIndex(
			CompassGpsInterfaceDevice gps) {
		boolean wasRunningBefore = gps.isRunning();

		log.info("CompassGps was running? " + wasRunningBefore);

		/*
		 * Check state of device. If not running and you try to index, you will
		 * get a device exception.
		 */
		if (!wasRunningBefore) {
			enableIndexMirroring(gps);
		}

		/*
		 * We don't need to check if index already exists. If it doesn't, it
		 * won't be deleted.
		 */
		gps.getIndexCompass().getSearchEngineIndexManager().deleteIndex();
		log.info("Deleting old index");
		gps.getIndexCompass().getSearchEngineIndexManager().createIndex();
		log.info("indexing now ... ");
		gps.index();
		log.info( "Indexing done. Now Optimizing index" );
		gps.getIndexCompass().getSearchEngineOptimizer().optimize();
		log.info( "Optimizing complete" );
		/* Return state of device */
		if (!wasRunningBefore) {
			disableIndexMirroring(gps);
		}

	}

	/**
	 * @param compass eg:    InternalCompass expressionBean = (InternalCompass) this.getBean("compassExpression");  Need this for replacing the indexes and it contains the path to the indexes to replace
	 * @param pathToIndex  An absolute path to the directory where the new indexes are located. Path should end at index sub dir. 
	 * @throws IOException
	 */
	public static synchronized void swapCompassIndex(
			InternalCompass compass, String pathToIndex)
			throws IOException {
		
		
		final File srcDir = new File(pathToIndex);
		final File targetDir = new File(compass.getSettings().getSetting("compass.engine.connection").replaceFirst("file:", "")+"/index/");

		//Validate that the new indexes exist and can read from them
		if (!srcDir.canRead()){
			log.error("Unable to read from specified directory: " + srcDir.getAbsolutePath());
			return;
		}
		
		
		//Validate that we can write where we are copying the file to. 		
		if (!targetDir.canWrite()){
			log.error("Unable to read from specified directory: " +targetDir.getAbsolutePath());
			return;
		}

		compass.getSearchEngineIndexManager().stop();
		
		log.info("Deleting old index....");
		compass.getSearchEngineIndexManager().deleteIndex();

		log.info("Clearing Cache.... ");
		compass.getSearchEngineIndexManager().clearCache();
		
		//Oddly this creates some empty segmants that are just cruft.
		//log.info("Creating index...");
		//compass.getSearchEngineIndexManager().createIndex();
		
		log.info("swapping index.....");		
		FileUtils.copyDirectory(srcDir, targetDir);

		compass.getSearchEngineIndexManager().start();
		
		
//		gps.getIndexCompass().getSearchEngineIndexManager().replaceIndex(
//				gps.getIndexCompass().getSearchEngineIndexManager(),
//				new SearchEngineIndexManager.ReplaceIndexCallback() {
//					public void buildIndexIfNeeded()
//							throws SearchEngineException {
//						try{
//							//Copy must be put in the call back.  If not put here then the files just get removed. when replace index is called. 
//							FileUtils.copyDirectory(srcDir, targetDir);
//						}
//						catch(IOException ioe){
//							log.error("Unable to copy" + srcDir.getAbsolutePath() + "  to " + targetDir.getAbsolutePath());
//						}
//					}
//				});

	}

	/**
	 * "Turning on" means adding the compass context to our spring context, as
	 * well as creating the compass index directory. This does not turn on index
	 * mirroring to automatically update the index while persisting data (to a
	 * database). To do this, call enableIndexMirroring after running this.
	 * 
	 * @param testEnv
	 * @param paths
	 */
	public static void turnOnCompass(boolean testEnv, List<String> paths) {
		deleteCompassLocks();
		if (testEnv) {
			addCompassTestContext(paths);
		} else {
			addCompassContext(paths);
		}

	}

	/**
	 * Add the compass contexts to the other spring contexts
	 * 
	 * @param paths
	 */
	private static void addCompassContext(List<String> paths) {
		paths.add("classpath*:ubic/gemma/applicationContext-search.xml");
	}

	/**
	 * Add the compass test contexts to the other spring contexts.
	 * 
	 * @param paths
	 */
	private static void addCompassTestContext(List<String> paths) {
		paths.add("classpath*:ubic/gemma/applicationContext-search.xml");
	}
}
