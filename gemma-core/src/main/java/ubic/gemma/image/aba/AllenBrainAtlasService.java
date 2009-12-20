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
package ubic.gemma.image.aba;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.gemma.loader.entrez.pubmed.XMLUtils;
import ubic.gemma.util.ConfigUtils;

/**
 * Acts as a convient front end to the Allen Brain Atlas REST (web) services Used the ABAapi.java as the original
 * template for this Service (found in ABA demo code).
 * 
 * @author kelsey
 * @version $Id$
 * @spring.bean id="allenBrainAtlasService"
 */
@Service
public class AllenBrainAtlasService {

    private static final String ABA_CACHE = "/abaCache/";

    private static Log log = LogFactory.getLog( AllenBrainAtlasService.class.getName() );

    /**
     * http://brain-map.org
     */
    public static final String API_BASE_URL = "http://www.brain-map.org";

    /**
     * /aba/api/gene/[geneSymbol].xml";
     */
    public static final String GET_GENE_URL = "/aba/api/gene/@.xml";

    /**
     * /aba/api/imageseries/[imageSeriesId].xml
     */
    public static final String GET_IMAGESERIES_URL = "/aba/api/imageseries/@.xml";

    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid].xml
     */
    public static final String GET_NEUROBLAST_URL = "/aba/api/neuroblast/@/@.xml";

    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid]/[Sagittal | Coronal].xml
     */
    public static final String GET_NEUROBLAST_PLANE_URL = "/aba/api/neuroblast/@/@/@.xml";

    /**
     * /aba/api/expression/[imageSeriesId].sva
     */
    public static final String GET_EXPRESSION_VOLUME_URL = "/aba/api/expression/@.sva";

    /**
     * /aba/api/expression/imageseries/[imageSeriesId].xml
     */
    public static final String GET_EXPRESSION_INFO_URL = "/aba/api/expression/imageseries/@.xml";

    /**
     * /aba/api/ara/[Sagittal | Coronal].xml
     */
    public static final String GET_ATLAS_INFO_URL = "/aba/api/ara/@.xml";

    /**
     * /aba/api/atlas/map/[imageseriesid].map
     */
    public static final String GET_ATLAS_IMAGE_MAP_URL = "/aba/api/atlas/map/@.map";

    /**
     * /aba/api/image/info?path=[the actual path to the image, as recovered from the imageSeries.xml]
     */
    public static final String GET_IMAGE_INFO_BYPATH_URL = "/aba/api/image/info?path=@";

    /**
     * /aba/api/image/info/[imageId].xml
     */
    public static final String GET_IMAGE_INFO_BYID_URL = "/aba/api/image/info/@.xml";

    /**
     * /aba/api/image?zoom=[image tier; usually 0-6, or -1 for highest tier]&path=[actual path, as above]
     */
    public static final String GET_IMAGE_URL = "/aba/api/image?mime=@&zoom=@&path=@";

    /**
     * /aba/api/image?zoom=[tier]&top=[unscaled pixel top]&left=[unscaled pixel left]&width=[actual pixel
     * width]&height=[actual pixel height]&path=[as above]
     */
    public static final String GET_IMAGE_ROI_URL = "/aba/api/image?mime=@&zoom=@&top=@&left=@&width=@&height=@&path=@";

    /**
     * /aba/api/gene/search?term=[some text, which will be used in a contains query for symbol, name & aliases]
     */
    public static final String SEARCH_GENE_URL = "/aba/api/gene/search?term=@";

    /**
     * For showing details about gene information on the allen brain atlas web site
     */
    public static final String HTML_GENE_DETAILS_URL = "http://mouse.brain-map.org/brain/@.html?ispopup=1";

    /**
     * requesting an ROI with MIME_IMAGE from a browser will let the image be shown within the browser; using
     * MIME_APPLICATION will cause the user to prompt to download.
     */
    public static final Integer MIME_IMAGE = 2;
    public static final Integer MIME_APPLICATION = 1;

    protected PrintStream infoOut;
    protected PrintStream errOut;
    protected boolean verbose;
    protected boolean useFileCache;
    protected String cacheDir;

    public AllenBrainAtlasService() {
        initDefaults();
    }

    public String buildUrlString( String urlPattern, String args[] ) {

        for ( int i = 0; i < args.length; i++ )
            urlPattern = urlPattern.replaceFirst( "@", args[i] );

        return ( API_BASE_URL + urlPattern );
    }

    public Document getAtlasImageMap( Integer imageseriesId ) {
        File outputFile = getFile( "atlasImageMap" + imageseriesId.toString() );
        Document atlasImageMapDoc = null;

        try {
            FileOutputStream out = new FileOutputStream( outputFile );
            this.getAtlasImageMap( imageseriesId, out );

            atlasImageMapDoc = XMLUtils.openAndParse( new FileInputStream( outputFile ) );
        } catch ( ParserConfigurationException pce ) {
            log.error( pce );
            return null;
        } catch ( SAXException se ) {
            log.error( se );
        } catch ( FileNotFoundException fnfe ) {
            log.error( fnfe );
        } catch ( IOException io ) {

        }

        return atlasImageMapDoc;

    }

    public boolean getAtlasImageMap( Integer imageseriesid, OutputStream out ) throws MalformedURLException,
            IOException {

        String args[] = { imageseriesid.toString() };
        String getImageMapUrl = buildUrlString( GET_ATLAS_IMAGE_MAP_URL, args );

        return ( doPageDownload( getImageMapUrl, out ) );
    }

    public boolean getAtlasInfo( String plane, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { plane };
        String getAtlasInfoUrl = buildUrlString( GET_ATLAS_INFO_URL, args );

        return ( doPageDownload( getAtlasInfoUrl, out ) );
    }

    public String getCacheDir() {
        return ( this.cacheDir );
    }

    public boolean getCaching() {
        return ( this.useFileCache );
    }

    public PrintStream getErrOut() {
        return ( this.errOut );
    }

    public boolean getExpressionInfo( Integer imageseriesId, OutputStream out ) throws MalformedURLException,
            IOException {

        String args[] = { imageseriesId.toString() };
        String getExpressionInfoUrl = buildUrlString( GET_EXPRESSION_INFO_URL, args );

        return ( doPageDownload( getExpressionInfoUrl, out ) );
    }

    public boolean getExpressionVolume( Integer imageseriesId, OutputStream out ) throws MalformedURLException,
            IOException {

        String args[] = { imageseriesId.toString() };
        String getVolumeUrl = buildUrlString( GET_EXPRESSION_VOLUME_URL, args );

        return ( doPageDownload( getVolumeUrl, out ) );
    }

    /**
     * @param givenGene symbol of gene that will be used to search ABA.
     * @return
     */
    public AbaGene getGene( String givenGene ) throws IOException {

        String gene = correctCase( givenGene );

        File outputFile = getFile( gene );
        Document geneDoc = null;

        try {
            FileOutputStream out = new FileOutputStream( outputFile );
            this.getGene( gene, out );

            geneDoc = XMLUtils.openAndParse( new FileInputStream( outputFile ) );
        } catch ( ParserConfigurationException pce ) {
            log.warn( pce );
            return null;
        } catch ( SAXException se ) {
            log.warn( se );
        } catch ( FileNotFoundException fnfe ) {
            if ( log.isDebugEnabled() )
                log.debug( gene + " gene not found in aba . Error thrown because cachefile not created: " + fnfe );

            log.info( gene + " not found in aba" );

            return null;

        }

        Collection<String> xmlData = XMLUtils.extractTagData( geneDoc, "geneid" );
        Integer geneId = xmlData.isEmpty() ? null : Integer.parseInt( xmlData.iterator().next() );

        xmlData = XMLUtils.extractTagData( geneDoc, "genename" );
        String geneName = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "genesymbol" );
        String geneSymbol = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "entrezgeneid" );
        Integer entrezGeneId = xmlData.isEmpty() ? null : Integer.parseInt( xmlData.iterator().next() );

        xmlData = XMLUtils.extractTagData( geneDoc, "ncbiaccessionnumber" );
        String ncbiAccessionNumber = xmlData.isEmpty() ? null : xmlData.iterator().next();

        String geneUrl = ( geneSymbol == null ) ? null : this.getGeneUrl( geneSymbol );

        if ( geneId == null && geneSymbol == null ) return null;

        AbaGene geneData = new AbaGene( geneId, geneSymbol, geneName, entrezGeneId, ncbiAccessionNumber, geneUrl, null );

        NodeList idList = geneDoc.getChildNodes().item( 0 ).getChildNodes();

        // log.debug( "Got " + idList.getLength() );

        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );

            if ( !item.getNodeName().equals( "image-series" ) ) continue;

            NodeList imageSeriesList = item.getChildNodes();

            for ( int j = 0; j < imageSeriesList.getLength(); j++ ) {

                Node imageSeries = imageSeriesList.item( j );

                NodeList childNodes = imageSeries.getChildNodes();
                Integer imageSeriesId = null;
                String plane = null;

                for ( int m = 0; m < childNodes.getLength(); m++ ) {

                    Node c = childNodes.item( m );

                    // log.info( c.getNodeName() );
                    String n = c.getNodeName();
                    try {
                        if ( n.equals( "imageseriesid" ) ) {
                            imageSeriesId = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                        } else if ( n.equals( "plane" ) ) {
                            plane = XMLUtils.getTextValue( ( Element ) c );
                        } else {
                            // Just skip and check the next one.
                        }
                    } catch ( IOException ioe ) {
                        log.warn( ioe );
                    }

                }

                if ( imageSeriesId != null && plane != null ) {
                    ImageSeries is = new ImageSeries( imageSeriesId, plane );
                    geneData.addImageSeries( is );
                    log.debug( "added image series to gene data" );
                } else {
                    log.debug( "Skipping adding imageSeries to gene cause data missing" );
                }

            }
        }

        return geneData;

    }

    /**
     * Given a valid official symbol for a gene (case sensitive) returns an allen brain atals gene details URL
     * 
     * @param gene
     * @return
     */
    public String getGeneUrl( String gene ) {
        return HTML_GENE_DETAILS_URL.replaceFirst( "@", this.correctCase( gene ) );
    }

    public boolean getImageROI( String imagePath, Integer zoom, Integer top, Integer left, Integer width,
            Integer height, Integer mimeType, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { mimeType.toString(), zoom.toString(), top.toString(), left.toString(), width.toString(),
                height.toString(), imagePath };
        String getImageUrl = buildUrlString( GET_IMAGE_ROI_URL, args );

        return ( doPageDownload( getImageUrl, out ) );
    }

    public Collection<Image> getImageseries( Integer imageseriesId ) {

        File outputFile = getFile( "ImageseriesId_" + imageseriesId.toString() );
        Document imageSeriesDoc = null;

        try {
            FileOutputStream out = new FileOutputStream( outputFile );
            this.getImageseries( imageseriesId, out );

            imageSeriesDoc = XMLUtils.openAndParse( new FileInputStream( outputFile ) );
        } catch ( ParserConfigurationException pce ) {
            log.error( pce );
            return null;
        } catch ( SAXException se ) {
            log.error( se );
        } catch ( FileNotFoundException fnfe ) {
            log.error( fnfe );
        } catch ( IOException io ) {

        }

        NodeList idList = imageSeriesDoc.getChildNodes().item( 0 ).getChildNodes();
        Collection<Image> results = new HashSet<Image>();

        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );

            if ( !item.getNodeName().equals( "images" ) ) continue;

            NodeList imageList = item.getChildNodes();

            for ( int j = 0; j < imageList.getLength(); j++ ) {

                Node image = imageList.item( j );

                if ( !image.getNodeName().equals( "image" ) ) continue;

                NodeList childNodes = image.getChildNodes();

                Integer imageId = null;
                String displayName = null;
                Integer position = null;
                Integer referenceAtlasIndex = null;
                String thumbnailUrl = null;
                String zoomifiedNisslUrl = null;
                String expressionThumbnailUrl = null;
                String downloadImagePath = null;
                String downloadExpressionPath = null;

                for ( int m = 0; m < childNodes.getLength(); m++ ) {

                    Node c = childNodes.item( m );

                    // log.info( c.getNodeName() );
                    String n = c.getNodeName();
                    try {
                        if ( n.equals( "#text" ) ) {
                            continue; // added to make faster as half of comparisions are empty nodes of this type!
                        } else if ( n.equals( "imageid" ) ) {
                            imageId = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                        } else if ( n.equals( "imagedisplayname" ) ) {
                            displayName = XMLUtils.getTextValue( ( Element ) c );
                        } else if ( n.equals( "position" ) ) {
                            position = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                        } else if ( n.equals( "referenceatlasindex" ) ) {
                            referenceAtlasIndex = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                        } else if ( n.equals( "thumbnailurl" ) ) {
                            thumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                        } else if ( n.equals( "zoomifiednisslurl" ) ) {
                            zoomifiedNisslUrl = XMLUtils.getTextValue( ( Element ) c );
                        } else if ( n.equals( "expressthumbnailurl" ) ) {
                            expressionThumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                        } else if ( n.equals( "downloadImagePath" ) ) {
                            downloadImagePath = XMLUtils.getTextValue( ( Element ) c );
                        } else if ( n.equals( "downloadExpressionPath" ) ) {
                            downloadExpressionPath = XMLUtils.getTextValue( ( Element ) c );
                        } else {
                            continue;
                        }
                    } catch ( IOException ioe ) {
                        log.warn( ioe );
                    }
                }// for loop

                if ( imageId != null && downloadImagePath != null ) {
                    Image img = new Image( displayName, imageId, position, referenceAtlasIndex, thumbnailUrl,
                            zoomifiedNisslUrl, expressionThumbnailUrl, downloadImagePath, downloadExpressionPath, 0, 0 );
                    results.add( img );
                } else {
                    log
                            .info( "Skipping adding image to collection cause necessary data missing after parsing image xml" );
                }

            }
        }

        return results;

    }

    public Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries ) {

        Collection<Image> representativeImages = new HashSet<Image>();

        if ( imageSeries != null ) {
            for ( ImageSeries is : imageSeries ) {
                if ( is.getImages() == null ) continue;

                for ( Image img : is.getImages() ) {
                    // Convert the urls into fully qualified ones for ez displaying
                    String args[] = { "2", "2", img.getDownloadExpressionPath() };
                    img.setDownloadExpressionPath( this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_URL, args ) );
                    img.setExpressionThumbnailUrl( AllenBrainAtlasService.API_BASE_URL
                            + img.getExpressionThumbnailUrl() );
                    representativeImages.add( img );
                }
            }
        }

        return representativeImages;

    }

    public PrintStream getInfoOut() {
        return ( this.infoOut );
    }

    /**
     * @param gene
     * @return
     * @throws IOException
     */
    public Collection<ImageSeries> getRepresentativeSaggitalImages( String gene ) throws IOException {

        AbaGene grin1 = this.getGene( gene );
        if ( grin1 == null ) return null;

        Collection<ImageSeries> representativeSaggitalImages = new HashSet<ImageSeries>();

        for ( ImageSeries is : grin1.getImageSeries() ) {
            if ( is.getPlane().equalsIgnoreCase( "sagittal" ) ) {

                Collection<Image> images = this.getImageseries( is.getImageSeriesId() );
                Collection<Image> representativeImages = new HashSet<Image>();

                for ( Image img : images ) {
                    if ( ( 2600 > img.getPosition() ) && ( img.getPosition() > 2200 ) ) {
                        representativeImages.add( img );
                    }
                }

                if ( representativeImages.isEmpty() ) continue;

                // Only add if there is something to add
                is.setImages( representativeImages );
                representativeSaggitalImages.add( is );
            }
        }
        // grin1.setImageSeries( representativeSaggitalImages );

        return representativeSaggitalImages;

    }

    public boolean getVerbose() {
        return ( this.verbose );
    }

    public boolean searchGenes( String searchTerm, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { searchTerm };
        String searchGenesUrl = buildUrlString( SEARCH_GENE_URL, args );

        return ( doPageDownload( searchGenesUrl, out ) );
    }

    public void setCacheDir( String s ) {
        this.cacheDir = s;
    }

    /*
     * Convieniece method for striping out the images from the image series. Also fully qaulifies URLs for link to allen
     * brain atlas web site @param imageSeries @return
     */

    public void setCaching( boolean v ) {
        this.useFileCache = v;
    }

    public void setErrOut( PrintStream out ) {
        this.errOut = out;
    }

    public void setInfoOut( PrintStream out ) {
        this.infoOut = out;
    }

    public void setVerbose( boolean v ) {
        this.verbose = v;
    }

    protected boolean getGene( String gene, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { gene };
        String getGeneUrl = buildUrlString( GET_GENE_URL, args );

        return ( doPageDownload( getGeneUrl, out ) );
    }

    protected boolean getImage( String imagePath, Integer zoom, Integer mimeType, OutputStream out )
            throws MalformedURLException, IOException {
        String args[] = { mimeType.toString(), zoom.toString(), imagePath };
        String getImageUrl = buildUrlString( GET_IMAGE_URL, args );

        return ( doPageDownload( getImageUrl, out ) );
    }

    protected boolean getImageInfo( Integer imageId, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { imageId.toString() };
        String getImageInfoUrl = buildUrlString( GET_IMAGE_INFO_BYID_URL, args );

        return ( doPageDownload( getImageInfoUrl, out ) );
    }

    protected boolean getImageInfo( String imagePath, OutputStream out ) throws MalformedURLException, IOException {

        String args[] = { imagePath };
        String getImageInfoUrl = buildUrlString( GET_IMAGE_INFO_BYPATH_URL, args );

        return ( doPageDownload( getImageInfoUrl, out ) );
    }

    protected boolean getImageseries( Integer imageseriesId, OutputStream out ) throws MalformedURLException,
            IOException {

        String args[] = { imageseriesId.toString() };
        String getImageseriesUrl = buildUrlString( GET_IMAGESERIES_URL, args );

        return ( doPageDownload( getImageseriesUrl, out ) );
    }

    protected boolean getNeuroblast( Integer imageseriesId, String structure, String plane, OutputStream out )
            throws MalformedURLException, IOException {

        String getNeuroblastUrl;

        if ( plane == null ) {
            String args[] = { structure, imageseriesId.toString() };
            getNeuroblastUrl = buildUrlString( GET_NEUROBLAST_URL, args );
        } else {
            String args[] = { structure, imageseriesId.toString(), plane };
            getNeuroblastUrl = buildUrlString( GET_NEUROBLAST_PLANE_URL, args );
        }

        return ( doPageDownload( getNeuroblastUrl, out ) );
    }

    /**
     * The allen brain atlas website 1st letter of gene symbol is capatilized, rest are not (webservice is case
     * sensitive)
     * 
     * @param geneName
     * @return
     */
    private String correctCase( String geneName ) {
        return StringUtils.capitalize( StringUtils.lowerCase( geneName ) );
    }

    private boolean doPageDownload( String urlString, OutputStream out ) throws MalformedURLException, IOException {

        URL url = new URL( urlString );
        DataInputStream in = null;

        in = getInput( url );
        if ( in == null ) return ( false );

        transferData( in, out );

        return ( true );
    }

    private DataInputStream getCachedFile( String cachedName ) throws FileNotFoundException {
        DataInputStream fs = new DataInputStream( new FileInputStream( cachedName ) );
        return ( fs );
    }

    private File getFile( String fileName ) {

        File outputFile = new File( this.cacheDir + "aba_" + fileName + ".xml" );

        if ( outputFile.exists() ) {
            outputFile.delete();

            // wait for file to be deleted before proceeding
            int i = 5;
            while ( ( i > 0 ) && ( outputFile.exists() ) ) {
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException ie ) {
                    log.error( ie );
                }
                i--;
            }

        }
        return outputFile;

    }

    private DataInputStream getInput( URL url ) throws IOException {

        if ( this.useFileCache ) {
            String cachedName = this.cacheDir + "/" + url.toString().replace( "/", "_" );
            File f = new File( cachedName );
            if ( f.exists() ) {
                if ( this.verbose ) this.infoOut.println( "Using cached file '" + cachedName + "'" );

                return ( getCachedFile( cachedName ) );
            }
        }

        HttpURLConnection conn = ( HttpURLConnection ) url.openConnection();
        conn.connect();
        DataInputStream in = new DataInputStream( conn.getInputStream() );

        if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK ) {
            this.errOut.println( conn.getResponseMessage() );
            return ( null );
        }

        if ( this.verbose ) showHeader( conn );

        if ( this.useFileCache ) {
            String cachedName = this.cacheDir + "/" + url.toString().replace( "/", "_" );
            FileOutputStream out = new FileOutputStream( new File( cachedName ) );
            transferData( in, out );
            return ( getCachedFile( cachedName ) );
        }

        return ( in );
    }

    private void initDefaults() {
        this.verbose = false;
        this.useFileCache = false;
        this.cacheDir = ConfigUtils.getString( "gemma.appdata.home" ) + ABA_CACHE;
        File abaCacheDir = new File( this.cacheDir );
        if ( !( abaCacheDir.exists() && abaCacheDir.canRead() ) ) {
            log.warn( "Attempting to create aba cache directory in '" + this.cacheDir + "'" );
            abaCacheDir.mkdirs();
        }

        this.infoOut = System.out;
        this.errOut = System.err;
    }

    private void showHeader( URLConnection url ) {

        this.infoOut.println( "" );
        this.infoOut.println( "URL              : " + url.getURL().toString() );
        this.infoOut.println( "Content-Type     : " + url.getContentType() );
        this.infoOut.println( "Content-Length   : " + url.getContentLength() );
        if ( url.getContentEncoding() != null )
            this.infoOut.println( "Content-Encoding : " + url.getContentEncoding() );
    }

    private void transferData( DataInputStream in, OutputStream out ) throws IOException {
        // This is whacked. There must be a better way than throwing an exception.
        boolean EOF = false;
        while ( !EOF ) {
            try {
                out.write( in.readUnsignedByte() );
            } catch ( EOFException eof ) {
                EOF = true;
            }
        }
    }

}
