/*
* Grid to display ExpressionExperiments.
* Author: Paul (based on Luke's CoexpressionDatasetGrid)
* $Id$
*/
Ext.namespace('Ext.Gemma.ExpressionExperimentGrid');


/* Ext.Gemma.ExpressionExperimentGrid constructor 
 */
Ext.Gemma.ExpressionExperimentGrid = function ( div, config ) {

	this.records = config.records; delete config.records;
 	this.readMethod = config.readMethod; delete config.readMethod;
	this.readParams = config.readParams; delete config.readParams;
	this.editable = config.editable; delete config.editable;
	this.pageSize = config.pageSize; delete config.pageSize;
	this.ddGroup = config.ddGroup; delete config.ddGroup;
	
	/* keep a reference to ourselves to avoid convoluted scope issues below...
	 */
	var thisGrid = this;
	 
	
	/* establish default config options.
	 */
	var superConfig = {
		renderTo : div,
		collapsible : false,
		header : true,
		collapsed : false,
		forceFit : true,
		width : 'auto',
		layout : 'fit',
		hidden : false
	};
	
	var fields = [
		{ id: 'shortName', header: "Dataset", dataIndex: "shortName", renderer: Ext.Gemma.ExpressionExperimentGrid.getEEStyler(), width : 80 },
		{ id: 'name', header: "Name", dataIndex: "name", width : 120 },
		{ id: 'arrays', header: "Arrays", dataIndex: "arrayDesignCount", width : 50 },
		{ id: 'assays', header: "Assays", dataIndex: "bioAssayCount", renderer: Ext.Gemma.ExpressionExperimentGrid.getAssayCountStyler() , width : 50 }
	];
	
	if ( this.pageSize ) {
		if ( !this.records ) {
			superConfig.ds = new Ext.Gemma.PagingDataStore( {
				proxy : new Ext.data.DWRProxy( this.readMethod ),
				reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExpressionExperimentGrid.getRecord() ),
				pageSize : this.pageSize
			} );
		} else {
			superConfig.ds = new Ext.data.Store({
				proxy : new Ext.ux.data.PagingMemoryProxy( this.records ),
				reader : new Ext.data.ListRangeReader( {}, Ext.Gemma.ExpressionExperimentGrid.getRecord() ),
				pageSize : this.pageSize
			});
		}
		superConfig.bbar = new Ext.Gemma.PagingToolbar( {
			pageSize : this.pageSize,
			store : superConfig.ds
		} );
	} else {
		if ( !this.records) {		
			superConfig.ds = new Ext.data.Store( {
				proxy : new Ext.data.DWRProxy( this.readMethod ),
				reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExpressionExperimentGrid.getRecord() )
			} );
		} else {
			superConfig.ds = new Ext.data.Store({
				proxy : new Ext.data.MemoryProxy( this.records ),
				reader : new Ext.data.ListRangeReader( {}, Ext.Gemma.ExpressionExperimentGrid.getRecord() ) 
			});
		}
	}
	
	superConfig.cm = new Ext.grid.ColumnModel( fields );
	superConfig.cm.defaultSortable = true;
	
	superConfig.autoExpandColumn = 'name';

	
	for ( property in config ) {
		if (true) {
			superConfig[property] = config[property];
		}
	}
	
	Ext.Gemma.ExpressionExperimentGrid.superclass.constructor.call( this, superConfig );
	
	this.getStore().on( "load", function () {
		this.autoSizeColumns();
		this.doLayout();
	}, this );
	
};


/* static methods
 */
 Ext.Gemma.ExpressionExperimentGrid.getAssayCountStyler = function() {
	if ( Ext.Gemma.ExpressionExperimentGrid.assayCountStyler === undefined ) {
		Ext.Gemma.ExpressionExperimentGrid.assayCountStyler = function ( value, metadata, record, row, col, ds ) {
			return String.format(
				"{0}&nbsp;<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'><img src='/Gemma/images/magnifier.png' height='10' width='10'/></a>", record.data.bioAssayCount, record.data.id );
		};
	}
	return Ext.Gemma.ExpressionExperimentGrid.assayCountStyler;
};

Ext.Gemma.ExpressionExperimentGrid.updateDatasetInfo = function( datasets, eeMap ) {
	for ( var i=0; i<datasets.length; ++i ) {
		var ee = eeMap[ datasets[i].id ];
		if ( ee ) {
			datasets[i].shortName = ee.shortName;
			datasets[i].name = ee.name;
		}
	}
};


Ext.Gemma.ExpressionExperimentGrid.getEEStyler = function() {
	if ( Ext.Gemma.ExpressionExperimentGrid.eeStyler === undefined ) {
		Ext.Gemma.ExpressionExperimentGrid.eeTemplate = new Ext.Template(
			"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"
		);
		Ext.Gemma.ExpressionExperimentGrid.eeStyler = function ( value, metadata, record, row, col, ds ) {
			return Ext.Gemma.ExpressionExperimentGrid.eeTemplate.apply( record.data );
		};
	}
	return Ext.Gemma.ExpressionExperimentGrid.eeStyler;
};


Ext.Gemma.ExpressionExperimentGrid.getRecord = function() {
	if ( Ext.Gemma.ExpressionExperimentGrid.record === undefined ) {
		Ext.Gemma.ExpressionExperimentGrid.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"shortName", type:"string" },
			{ name:"name", type:"string" },
			{ name:"arrayDesignCount", type:"int" },
			{ name:"bioAssayCount", type:"int" },
			{ name:"externalUri", type:"string" }
		] );
	}
	return Ext.Gemma.ExpressionExperimentGrid.record;
};

/*
* Type definition
*/
Ext.extend( Ext.Gemma.ExpressionExperimentGrid, Ext.Gemma.GemmaGridPanel, {
	getReadParams : function() {
		return ( typeof this.readParams == "function" ) ? this.readParams() : this.readParams;
	},
	
	/**
	 * Return all the ids of the experiments shown in this grid.
	 */
	getEEIds : function() {
		var result = [];
		this.store.each(function (rec) { result.push(rec.get("id")); });
		return result;
	}
    
});

Ext.Gemma.DatasetSearchToolBar = function ( grid, config ) {
	var bar = this;
	var thisGrid = grid;
	var taxonSearch = true;
	if (config.taxonSearch) {
		this.taxonSearch = config.taxonSearch;
	}
	
	var eeSearchField = new Ext.Gemma.DatasetSearchField( {
		fieldLabel : "Experiment keywords"
	} );
	
	this.eeSearchField = eeSearchField;
	eeSearchField.on( 'aftersearch', function ( field, results ) {
		this.getStore().load( { params : [results] });
	}, grid );
	
	var taxonCombo = new Ext.Gemma.TaxonCombo( {
			emptyText : 'select a taxon',
			width : 150
		} );
		
	taxonCombo.on( "taxonchanged", function( combo, taxon ) {
		this.eeSearchField.taxonChanged(taxon, true);	 	
	}, this );

	Ext.Gemma.DatasetSearchToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : thisGrid.tbar
	} );		
	
	if ( this.taxonSearch ) {
		this.addField( taxonCombo );
		this.addSpacer();
	}
	this.addField( eeSearchField );

};

Ext.extend(Ext.Gemma.DatasetSearchToolBar, Ext.Toolbar, {
});

 