/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

Gemma.CytoscapeJSDisplay = Ext.extend( Ext.BoxComponent, {

    initComponent: function () {
        this.ready = false;
       
        this.emphasized = true;
       
        var display = this;

        Gemma.CytoscapeJSDisplay.superclass.initComponent.apply(this, arguments);
        this.addEvents('selection_available', 'selection_unavailable', 'layout_complete');

        display.coexDisplaySettings.on('stringency_change', function() {
            display.filter();
        });

        display.coexDisplaySettings.on('query_genes_only_change', function() {
            display.filter();
        });

        display.coexDisplaySettings.on('gene_overlay', function() {
            display.applyGeneListOverlay();
        });

        display.coexDisplaySettings.on('search_text_change', function( text ) {
            display.selectNodesMatchingText(text);
        });
    },

    hideAll: function (){
    	this.cy.elements().hide();    	
    },    
    
    initializeGraph: function (graphData) {
    	Gemma.CytoscapeJSCoexGraphInitializer(jQuery('#cy'), graphData, this.onGraphReady, this);
    },
    
    //ownerRef refers to this
    onGraphReady : function(ownerRef){    	
	    
    	ownerRef.cy.on('done', function(e){
    		
    		console.log('cytoscape event: done');
    		ownerRef.cy.panningEnabled(true);
    		ownerRef.fireEvent('selection_available');
    		ownerRef.ready = true;
	    });
    	
    	ownerRef.cy.on('layoutstop', function(e){
    		
    		 //this is a hack because in firefox the cytoscape code fires off two 'layoutstop' events for arbor when you use it for initialization
    		//workaround is use grid layout in initializer, then once it finishes use arbor
    		if(e.cy._private.layout.options.name=='grid'){
    			console.log('cytoscape event: layoutstop, layout name:grid');
    			ownerRef.refreshLayout();
    		}
    		else{//should be arbor
    			console.log('cytoscape event: layoutstop, layout name:');
    			ownerRef.cytoscapePanel.loadMask.hide();
    			ownerRef.nodeDegreeEmphasize(true); 
    			ownerRef.zoomToFit();
    			
    		}
    		
	    });
    },
    
    applyDefaultGraphStyle: function(isNodeDegreeEmphasis){
    	
    	this.cy.elements().toggleClass( 'emphasis' , isNodeDegreeEmphasis);    	
    	this.cy.elements().toggleClass( 'basic' , !isNodeDegreeEmphasis);    	
    	this.filter();
    },
    
    nodeDegreeEmphasize: function (isNodeDegreeEmphasis) {
         if (!this.ready) {return;}
         
         this.emphasized = isNodeDegreeEmphasis;
         
         this.applyGeneListOverlay();
     },
     
     filter: function () {
    	 if (!this.ready) {return;}
	        
         var stringency = this.coexDisplaySettings.getStringency();
         var queryGenesOnly = this.coexDisplaySettings.getQueryGenesOnly();

         var trimmed={};
         if ( queryGenesOnly ) {
         	trimmed.trimmedNodeIds = this.coexpressionSearchData.getQueryGeneIds();
         } else {           
              trimmed = Gemma.CoexVOUtil.trimKnownGeneResultsWithQueryGenes(
                 this.coexpressionSearchData.getDisplayedResults(),
                 this.coexpressionSearchData.getQueryGeneIds(),
                 stringency );
              
         }
         
         var nodeIdsToHide = this.getNodeIdsToHide(trimmed.trimmedNodeIds, this.coexpressionSearchData.allGeneIdsSet);
         
         var nodeHideFunction = function(i, element){        	      	
         	
         	return element.isNode() && nodeIdsToHide.indexOf(element.data("geneid")) !== -1;
         };
         
         var nodeShowFunction = function(i, element){ 
         	
         	return element.isNode() && trimmed.trimmedNodeIds.indexOf(element.data("geneid")) !== -1;
         };
         
         
         var nodesToHide = this.cy.filter(nodeHideFunction);
         var nodesToShow = this.cy.filter(nodeShowFunction);
         
         nodesToHide.hide();         
         nodesToHide.unselectify();
         
         nodesToShow.selectify();
         nodesToShow.show();
         
         var edgeShowFunction = function(i, element){        	      	
         	
         	return element.isEdge() && element.data("support") >= stringency;
         };
         
         var edgeHideFunction = function(i, element){ 
         	
         	return element.isEdge() && element.data("support") < stringency;
         };
         
         var edgesToHide = this.cy.filter(edgeHideFunction);
         var edgesToShow = this.cy.filter(edgeShowFunction);
         
         edgesToHide.hide();
         edgesToShow.show();
         
     },


    getSelectedGeneIds: function () {
        if (!this.ready) {return;}
        
        var nodes = this.cy.elements("node:selected:visible");
        
        var geneIds = [];
        for (var i = 0; i < nodes.length; i++) {
            geneIds.push(nodes[i].data("geneid"));
        }
        return geneIds;
    },

    refreshLayout: function () {
        if (!this.ready) {return;}
        this.hideAll();
        this.cy.layout(Gemma.CytoscapejsSettings.arborLayout);
        
    },
    
    zoomToFit: function () {
        if (!this.ready) {return;}        
        this.cy.fit();      
        
    },
    
    /**
     * @public
     * @param {boolean} visible
     */
    toggleNodeLabels: function (visible) {
        if (!this.ready) {return;}
        console.log("toggleNodeLabels");
        var content="";
        if (visible){
        	content='data(name)';        	
        }
        this.cy.style().selector('node').css({'content':content}).update();
        
    },   

    /**
     * @private
     * @param nodeIds
     */
    selectNodes: function (nodeIds) {
         
         var nodeSelectFunction = function(i, element){         	
         	return element.isNode() && nodeIds.indexOf(element.data("geneid")) !== -1;
         };
         
         var nodesToSelect = this.cy.filter(nodeSelectFunction);
         
         nodesToSelect.select();
         
    },

    /**
     * @private
     */
    deselectNodesAndEdges: function () {    	
    	this.cy.nodes().unselect();
    	this.cy.edges().unselect();        
    },    

    /**
     * @public
     */
    applyGeneListOverlay: function () {
        if (!this.ready) {return;}
        
        this.applyDefaultGraphStyle(this.emphasized);

        var overlayIds = this.coexDisplaySettings.getOverlayGeneIds();
        
        var nodeOverlayFunction = function(i, element){        	      	
         	
         	return element.isNode() && overlayIds.indexOf(element.data("geneid")) !== -1;
        };
        
        var nodesToOverlay = this.cy.filter(nodeOverlayFunction);
        
        this.cy.nodes().toggleClass( 'overlay' , false);
        
        if (nodesToOverlay.length > 0){
        	nodesToOverlay.toggleClass('overlay', true);
        }
    	
    	
        
    },

    /**
     * @public
     * @param nodeIds
     * @returns {{total: number, hidden: number}}
     */
    getNodesMatching: function (nodeIds) {
        var matchingCounts = {
            total: 0,
            hidden: 0
        };        
        
        var nodesMatchingFunction = function(i, element){        	      	
         	
         	return element.isNode() && nodeIds.indexOf(element.data("geneid")) !== -1;
        };
        
        var nodesMatched = this.cy.filter(nodesMatchingFunction);
        for (var i = 0; i < nodesMatched.length; i++) {
            
            if (nodesMatched[i] !== null) {
                matchingCounts.total += 1;
                if (!nodesMatched[i].visible()) {
                    matchingCounts.hidden += 1;
                }
            }
        }
        return matchingCounts;
    },

    /**
     * @private
     * @param text
     */
    selectNodesMatchingText: function (text) {
        if (!this.ready) {return;}

        this.deselectNodesAndEdges();
        if (text.length < 2) {
            return;
        }
        var nodeIdsToSelect = this.coexpressionSearchData.getCytoscapeGeneSymbolsMatchingQuery( text );
        this.selectNodes(nodeIdsToSelect);
    },
   
    /**
     * @private
     */
    applySelection: function () {
        if (!this.ready) {return;}

        this.selectNodesMatchingText( this.coexDisplaySettings.getSearchTextValue() );
    },
    
    getNodeIdsToHide: function (nodesToShow, allNodes) {
    	
    	var nodeIdsToHide=[];
    	
    	var length = allNodes.length;
    	
    	for (var i = 0;i<length;i++){
    		
    		if (nodesToShow.indexOf(allNodes[i]) ==-1){
    			nodeIdsToHide.push(allNodes[i]);
    		}
    		
    	}
    	
    	return nodeIdsToHide;
    	
    },

    exportPNG: function () {
    	
        var htmlString = '<img src="'+this.cy.png()+'"/>';

        var win = new Ext.Window({
            title: Gemma.HelpText.WidgetDefaults.CytoscapePanel.exportPNGWindowTitle,
            plain: true,
            html: htmlString,
            height: 700,
            width: 900,
            autoScroll: true
        });
        win.show();
    }
    
    
});