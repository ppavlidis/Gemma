Ext.namespace('Gemma');

//
//  Column of heatmap cells for one gene group
//  
//
Gemma.MetaHeatmapColumn = Ext.extend(Ext.BoxComponent, {		
	initComponent: function() {
		Ext.apply(this, {			
			autoEl: { tag: 'canvas',
					  width: Gemma.MetaVisualizationConfig.cellWidth,
					  height: Gemma.MetaVisualizationConfig.cellHeight * this.visualizationSubColumnData.length,					  
					},
			margins: {top:0, right:0, bottom: Gemma.MetaVisualizationConfig.groupSeparatorHeight, left:0},

			applicationRoot: this.applicationRoot,

			cellHeight: Gemma.MetaVisualizationConfig.cellHeight,   // alias with a shorter name
			cellWidth:  Gemma.MetaVisualizationConfig.cellWidth,	// alias with a shorter name		
									
			geneGroupIndex: this.rowGroup,							// gene group index
			columnIndex: this.columnIndex,							// index within analysis panel
			analysisColumnGroupIndex: this.columnGroupIndex,		// index of analysis panel
			datasetColumnGroupIndex: this.datasetColumnGroupIndex,  // index of dataset column group panel
			datasetGroupIndex: this.datasetGroupIndex,				// dataset group index
			
			_visualizationValues: this.visualizationSubColumnData,
			pValues: this.pValuesSubColumnData,
			
			_contrastsVisualizationValues: this.visualizationContrastsSubColumnData,
			contrastsFoldChanges: this.contrastsFoldChanges,
			_contrastsFactorValues: this.factorValues,
            
			_discreteColorRange: Gemma.MetaVisualizationConfig.basicColourRange,
            _discreteColorRangeContrasts: Gemma.MetaVisualizationConfig.contrastsColourRange,
            _isExpanded: false,
            
            
            overallDifferentialExpressionScore: null,
		});		 
		Gemma.MetaHeatmapColumn.superclass.initComponent.apply(this, arguments);	
	},
		
	_drawHeatmapColumn : function ( doResize, highlightRow ) {
    	var expandableColumnPanel 		= this.ownerCt;

		var ctx = this.el.dom.getContext("2d");		
		var oldWidth = ctx.canvas.width;
		var newWidth = this.cellWidth;				
		ctx.canvas.width = newWidth;		
		ctx.clearRect(0, 0, this.el.dom.width, this.el.dom.height);		
		
		for (var i = 0; i < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; i++) {
			var color = this._discreteColorRange.getCellColorString(this._visualizationValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][i]]);
			this._drawHeatmapCell ( ctx, color, i, 0 );
			if (highlightRow == i) this._drawHeatmapCellBox ( ctx, highlightRow, 0 );			
		}

		if ( doResize ) {
			var widthChange = newWidth - oldWidth;		
			expandableColumnPanel.setWidth( newWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth );
			expandableColumnPanel.updateAllParentContainersWidthBy( widthChange );			
		}
		
		this.setWidth ( newWidth );
	},
    _drawContrasts: function ( doResize, highlightRow, highlightColumn ) {
		var ctx = this.el.dom.getContext("2d");    	

    	var expandableColumnPanel 		= this.ownerCt;

    	var oldWidth = ctx.canvas.width;
		var newWidth = this.cellWidth * this._contrastsFactorValues.length;				

		// Resize and clear canvas
		ctx.canvas.width = newWidth;
		ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

		// Draw cells
		for (var factorValueIndex = 0; factorValueIndex < this._contrastsFactorValues.length; factorValueIndex++) {			
			for (var geneIndex = 0; geneIndex < this.applicationRoot.geneOrdering[this.geneGroupIndex].length; geneIndex++) {				
				var color = this._discreteColorRangeContrasts.getCellColorString(this._contrastsVisualizationValues[this.applicationRoot.geneOrdering[this.geneGroupIndex][geneIndex]][factorValueIndex]);
				this._drawHeatmapCell ( ctx, color, geneIndex, factorValueIndex );
				if (highlightRow == geneIndex && highlightColumn == factorValueIndex) this._drawHeatmapCellBox ( ctx, highlightRow, highlightColumn );
			}			
		}

		if (doResize) {
			var widthChange = newWidth - oldWidth;
			expandableColumnPanel.setWidth( expandableColumnPanel.getWidth() + widthChange );
			expandableColumnPanel.updateAllParentContainersWidthBy( widthChange );			
		}

		this.setWidth(newWidth);
    },
    _drawHeatmapCellBox: function(ctx, rowIndex, columnIndex) {
		ctx.save();
		ctx.strokeStyle = Gemma.MetaVisualizationConfig.cellHighlightColor;
        ctx.strokeRect( this.cellWidth * columnIndex, this.cellHeight * rowIndex, this.cellWidth, this.cellHeight );
        ctx.restore();
    },
	_drawHeatmapCell: function(ctx, color, rowIndex, columnIndex) {
    	ctx.fillStyle = color;
		ctx.fillRect( columnIndex * this.cellWidth, rowIndex * this.cellHeight, this.cellWidth, this.cellHeight);
    },
    __calculateIndexFromXY: function(x,y) {
    	var row = Math.floor(y/this.cellHeight);
    	var column = Math.floor(x/this.cellWidth);
    	return {'row': row, 'column': column };
    },
    
	onRender:function() {
		Gemma.MetaHeatmapColumn.superclass.onRender.apply(this, arguments);		
		this._drawHeatmapColumn();
		
		this.el.on('click', function(e,t) { 						
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			
			/*	What to show?	
			 *  COLLAPSED
			 *  - Dataset name / link
			 *  - Analysis type / link to design
			 *  - Factor
			 *  - Gene / link to gene page
			 *  - Number of probes matching this gene?
			 *  - Specificity? (in overall dataset sense)
			 *  - Expression profile
			 *  
			 *  EXPANDED
			 *  - Dataset name / link
			 *  - Analysis type / link to design
			 *  - Factor
			 *  - Baseline
			 *  - Factor Value
			 *  - Gene / link to gene page
			 *  - Fold change
			 *    
			 */						
			var vizWindow = new Gemma.VisualizationWithThumbsWindow({
				title : 'Gene expression',
				thumbnails : false
			});
			var eeId = this.ownerCt._dataColumn.datasetId;
			var _datasetGroupPanel = this.ownerCt.ownerCt.ownerCt;
			vizWindow.show({
				params : [[eeId], [ this.applicationRoot._heatmapArea._geneIds[index.row] ] ]
			});		
		}, this);
		
		this.el.on('mousemove', function(e,t) { 						
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			if ( this._isExpanded ) {
				this._drawContrasts ( false, index.row, index.column );
				//this.applicationRoot._rotatedLabelsBox._drawTopLabels ( this._datasetGroupIndex, this._columnGroupIndex, this._columnIndex, index.column );				
				this.applicationRoot.MiniWindowTool.setTitle("Gene: "+" Experiment: "+" Factor: "+" Factor value: ");
				this.applicationRoot.MiniWindowTool.specificity.setText("Specificity: " +  100*this.ownerCt.miniPieValue/360);
				this.applicationRoot.MiniWindowTool.pValue.setText("pValue: " +  this.pValues[index.row]);
				this.applicationRoot.MiniWindowTool.foldChange.setText("Fold change: " +  this.contrastsFoldChanges[index.row][index.column]);			      	         							
			} else {
				this._drawHeatmapColumn ( false, index.row );
				//this.applicationRoot._rotatedLabelsBox._drawTopLabels ( this._datasetGroupIndex, this._columnGroupIndex, this._columnIndex );
				this.applicationRoot.MiniWindowTool.setTitle("Gene: "+" Experiment: "+" Factor: ");
				this.applicationRoot.MiniWindowTool.specificity.setText("Specificity: " +  100*this.ownerCt.miniPieValue/360);
				this.applicationRoot.MiniWindowTool.pValue.setText("pValue: " +  this.pValues[index.row]);
				//this.applicationRoot.MiniWindowTool.foldChange.setText("Fold change: " +  this._contrastsVisualizationValues[index.row][index.column]);			      	         				
			}
			this.applicationRoot._geneLabels.highlightGene( this.rowGroup, index.row );			
		}, this );

		this.el.on('mouseover', function(e,t) { 						
			var index = this.__calculateIndexFromXY(e.getPageX() - Ext.get(t).getX(), e.getPageY() - Ext.get(t).getY());
			if ( this._isExpanded ) {
				//this.applicationRoot._rotatedLabelsBox._drawTopLabels ( this._datasetGroupIndex, this._columnGroupIndex, this._columnIndex, index.column );				
			} else {
				//this.applicationRoot._rotatedLabelsBox._drawTopLabels ( this._datasetGroupIndex, this._columnGroupIndex, this._columnIndex );				
			}			
		}, this );		

		this.el.on('mouseout', function(e,t) { 						
			if (this._isExpanded) {
				this._drawContrasts( false );
			} else {
				this._drawHeatmapColumn( false );
			}
			//this.applicationRoot._geneLabels.highlightGene ( this.rowGroup, -1 );
			//this.applicationRoot._rotatedLabelsBox._drawTopLabels ( this._datasetGroupIndex );
		}, this );
	}					
});

Ext.reg('metaVizColumn', Gemma.MetaHeatmapColumn);

// COLUMN ( visualization for each row group

Gemma.MetaHeatmapExpandableColumn = Ext.extend ( Ext.Panel, {
	initComponent: function() {
		Ext.apply(this, {
			applicationRoot: this.applicationRoot, // reference to the root panel of the application
			
			border: false,
			bodyBorder: false,
			
			width: Gemma.MetaVisualizationConfig.cellWidth  + Gemma.MetaVisualizationConfig.columnSeparatorWidth,
			
			_dataColumn: this.dataColumn,							
			_numberOfRowGroups: this.dataColumn.visualizationValues.length,			
			_columnIndex: this.columnIndex,
			_columnGroupIndex: this.columnGroupIndex,
			_datasetGroupIndex: this.datasetGroupIndex,
			
			miniPieValue: 360.0 * this.dataColumn.numberOfProbesDiffExpressed / this.dataColumn.numberOfProbesTotal,
			sumOfPvalues: 0.0,
			
			_factorName: this.dataColumn.factorName,
			_factorValueNames: this.dataColumn.contrastsFactorValues,			
			_baselineFactorValue: this.dataColumn.baselineFactorValue,
			contrastsFoldChanges: this.dataColumn.constrastsFoldChangeValues,
						
			updateAllParentContainersWidthBy: function (delta) {
				this.ownerCt.changePanelWidthBy(delta);			
			},
			
			_visualizationColumns :[],
			
			layout: 'vbox',
			items: [{ xtype: 'button',
					  ref: '_expandButton',
					  enableToggle: true,

					  template: new Ext.Template('<div id="{1}"><canvas {0}></canvas></div>'),
					  buttonSelector: 'canvas:first-child',
					  getTemplateArgs : function(){
						return [this.cls, this.id];
		    		  },					  
					  cls: " width='10px' height='10px' ",
					  listeners: { 
				           toggle : function ( target, checked ) { 			
								if (checked) {
									var ctx = this._expandButton.btnEl.dom.getContext("2d");
									ctx.clearRect(0,0,10,10);
									MiniPieLib.drawFilledRectangle(ctx, 3, 2, 6, 6, 'rgba(10,100,10, 0.5)');		                     
									var doResize = true;
									for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {																			
										this._visualizationColumns[geneGroupSubColumnIndex]._drawContrasts( doResize );
										this._visualizationColumns[geneGroupSubColumnIndex]._isExpanded = true;
										doResize = false;
									}
								} else {
									var ctx = this._expandButton.btnEl.dom.getContext("2d");
									ctx.clearRect(0,0,10,10);
									MiniPieLib.drawFilledRectangle(ctx, 5, 4, 2, 2, 'rgba(10,100,10, 0.5)');		                     

									var doResize = true;
									for (var geneGroupSubColumnIndex = 0; geneGroupSubColumnIndex < this._visualizationColumns.length; geneGroupSubColumnIndex++) {
										this._visualizationColumns[geneGroupSubColumnIndex]._drawHeatmapColumn( doResize );
										this._visualizationColumns[geneGroupSubColumnIndex]._isExpanded = false;
										doResize = false;
									}
								}
								this.applicationRoot.topLabelsPanel._drawTopLabels();
								this.applicationRoot._heatmapArea.doLayout();							
							}, scope: this
					  }
				    },]
		});
				
		Gemma.MetaHeatmapExpandableColumn.superclass.initComponent.apply ( this, arguments );
}, 
	
	onRender: function() {
		Gemma.MetaHeatmapExpandableColumn.superclass.onRender.apply ( this, arguments );
		
		for (var geneGroupIndex = 0; geneGroupIndex < this._numberOfRowGroups; geneGroupIndex++) {
			var subColumn = new Gemma.MetaHeatmapColumn({applicationRoot: this.applicationRoot,
												 visualizationSubColumnData: this._dataColumn.visualizationValues [ geneGroupIndex ],
												 pValuesSubColumnData: this._dataColumn.pValues[geneGroupIndex],
												 visualizationContrastsSubColumnData: this._dataColumn.contrastsVisualizationValues [ geneGroupIndex ],
												 factorValues: this._factorValueNames,
												 contrastsFoldChanges: this.contrastsFoldChanges[ geneGroupIndex ],
												 rowGroup: geneGroupIndex,
												 columnIndex: this._columnIndex,
												 columnGroupIndex: this._columnGroupIndex,
												 datasetGroupIndex: this._datasetGroupIndex});			
			this._visualizationColumns.push(subColumn);
			this.add(subColumn);
		}
		
		this.overallDifferentialExpressionScore = 0;
		for ( var i = 0; i < this._dataColumn.pValues.length; i++ ) {
			for ( var j = 0; j < this._dataColumn.pValues[i].length; j++ ) {
				this.overallDifferentialExpressionScore += this._dataColumn.pValues[i][j];
			}
		}

		this.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;
		this.ownerCt.ownerCt.overallDifferentialExpressionScore += this.overallDifferentialExpressionScore;
		
		this.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.specificityScore);
		this.ownerCt.ownerCt.specificityScore = Math.max(this.miniPieValue, this.ownerCt.ownerCt.specificityScore);		
	}
});



Gemma.MetaHeatmapExpandButton = Ext.extend ( Ext.Button, {	
	initComponent: function() {
		Ext.apply(this, {
		});
		Gemma.MetaHeatmapExpandButton.superclass.initComponent.apply(this, arguments);			
	},
	onRender: function() {
		Gemma.MetaHeatmapExpandButton.superclass.onRender.apply(this, arguments);
		
	}
});
