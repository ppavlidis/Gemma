/**
 * The javascript search interface.
 * 
 * @authors kelsey, paul
 * @version: $Id$
 */
Ext.namespace("Gemma.Search");
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Gemma.Search.GeneralSearch = Ext.extend(Ext.Panel,{
	layout:'vbox',
	layoutConfig:{
		align:'stretch'
	},
	padding:10,
	title:'General Search',
	initComponent : function() {
		Gemma.Search.GeneralSearch.superclass.initComponent.call(this);
			this.form = new Gemma.SearchForm({
				flex:0
			});
			
			var autoLoadParams = false;
			if (this.form.restoreState()) {
				var settings = this.getSearchSettings();
				autoLoadParams = {
					params : settings.params
				};
			}
			
			this.resultGrid = new Gemma.SearchGrid({
						form : this.form,
						flex:1,
						region:'center',
						autoLoadStore: autoLoadParams
					});
					
			this.messagePanel = new Ext.Panel({
				xtype:'panel',
				tpl:'<tpl if="msg != \'\'"><img src="/Gemma/images/icons/warning.png"/>{msg}</tpl>',
				border:false,
				flex:1
			});
			this.bookmarkPanel = new Ext.Panel({
				xtype:'panel',
				tpl:'<a href="/Gemma/searcher.html?query={escapedQuery}{scopes}">Bookmarkable link</a>',
				border:false,
				flex:0,
				height:25,
				colspan:2
			});

			northPan = new Ext.Panel({
				style:'width:100%',
				flex:0,
				border:false,
				layout: 'column',
				region:'north',
				//height:250,
				items: [this.form, this.messagePanel]
			});
			
			this.add(northPan);
			this.add(this.bookmarkPanel);
			this.add(this.resultGrid);
			this.form.on("search", this.search.createDelegate(this));
			
			
			this.resultGrid.on("loadError", function(message){
				this.messagePanel.update({msg:message});
			});
			
			// adjust height of north panel to fit search form properly
			this.form.on('formResized', function(height, width){
				northPan.setHeight(height);
				this.doLayout();
			}, this);
			
			
			// respond to the user logging in or out
			Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
				// show/hide the advanced options section
				this.form.adjustForIsAdmin(isAdmin);
				northPan.setHeight(this.form.getHeight());
				this.doLayout();
				
			},this);
			Gemma.Application.currentUser.on("logOut", function(){
				this.form.adjustForIsAdmin(false);
				northPan.setHeight(this.form.getHeight());
				this.doLayout();
			},this);
		},

		getSearchSettings : function() {
			if (!this.form.getForm().findField('query').isValid()) {
				return;
			}
			var query = Ext.getCmp('search-text-field').getValue();
			var searchProbes = Ext.getCmp('search-prbs-chkbx').getValue();
			var searchGenes = Ext.getCmp('search-genes-chkbx').getValue();
			var searchExperiments = Ext.getCmp('search-exps-chkbx').getValue();
			var searchArrays = Ext.getCmp('search-ars-chkbx').getValue();
			var searchSequences = Ext.getCmp('search-seqs-chkbx').getValue();
			var searchGeneSets = Ext.getCmp('search-genesets-chkbx').getValue();
			var searchForPhenotypes = Ext.getCmp('search-forPhenotypes-chkbx').getValue();
			var searchEESets = Ext.getCmp('search-eesets-chkbx').getValue();
			var searchPapers = Ext.getCmp('search-papers-chkbx').getValue();

			var searchDatabase = true;
			var searchIndices = true;
			var searchCharacteristics = true;
			var searchGO = false;
			var searchUsingPhenotypes = false;
			if (Ext.get('hasAdmin').getValue()) {
				searchDatabase = Ext.getCmp('search-database-chkbx').getValue();
				searchIndices = Ext.getCmp('search-indices-chkbx').getValue();
				searchCharacteristics = Ext.getCmp('search-characteristics-chkbx').getValue();
				searchGO = Ext.getCmp('search-go-chkbx').getValue();
				searchUsingPhenotypes = Ext.getCmp('search-usingPhenotypes-chkbx').getValue();
			}

			var scopes = "&scope=";
			if (searchProbes) {
				scopes = scopes + "P";
			}
			if (searchGenes) {
				scopes = scopes + "G";
			}
			if (searchExperiments) {
				scopes = scopes + "E";
			}
			if (searchArrays) {
				scopes = scopes + "A";
			}
			// removed until sequences have a page
			// see bug 2233
			/*if (searchSequences) {
				scopes = scopes + "S";
			}*/
			if (searchGeneSets) {
				scopes = scopes + "M";
			}
			if (searchForPhenotypes) {
				scopes = scopes + "H";
			}
			if (searchEESets) {
				scopes = scopes + "N";
			}
			if (searchPapers) {
				scopes = scopes + "B";
			}

			var params = [{
				query : query,
				searchProbes : searchProbes,
				searchBioSequences : searchSequences,
				searchArrays : searchArrays,
				searchExperiments : searchExperiments,
				searchGenes : searchGenes,
				searchGeneSets : searchGeneSets,
				searchExperimentSets : searchEESets,
				useDatabase : searchDatabase,
				useIndices : searchIndices,
				useCharacteristics : searchCharacteristics,
				searchGenesByGO : searchGO,
				searchBibrefs: searchPapers,
				searchUsingPhenotypes : searchUsingPhenotypes,
				searchForPhenotypes: searchForPhenotypes
			}];
			return {
				params: params,
				scopes: scopes,
				query: query
			};
		},
		/**
		 * 
		 */
		search : search = function(t, event) {
			var settings = this.getSearchSettings();
			var params = settings.params;
			var query = settings.query;
			var scopes = settings.scopes;
			
			this.resultGrid.getStore().load({
						params : params
					});

			if (typeof pageTracker !== 'undefined') {
				pageTracker._trackPageview("/Gemma/searcher.search?query=" + escape(query) + scopes);
			}			
			this.messagePanel.update({msg:""});
			this.form.findById('submit-button').setDisabled(true);
			this.bookmarkPanel.update({
				escapedQuery:escape(query),
				scopes:scopes
			});
		}
});

Gemma.Search.MAX_AUTO_EXPAND_SIZE = 15;

Gemma.SearchForm = Ext.extend(Ext.form.FormPanel, {
			frame : true,
			autoHeight : true,
			
			//width : 500,

			/**
			 * Restore state ... fixme.
			 */
			restoreState : function() {

				var url = document.URL;
				if (url.indexOf("?") > -1) {
					var sq = url.substr(url.indexOf("?") + 1);
					var params = Ext.urlDecode(sq);

					if ((params.termUri) && (params.termUri.length !== 0)) {
						this.form.findField('query').setValue(params.termUri);
					}
					else 
						if (params.query) {
							this.form.findField('query').setValue(params.query);
						}
						else {
							// NO Query object (just a random ? in string uri)
							return false;
						}

					if (params.scope) {
						if (params.scope.indexOf('E') > -1) {
							Ext.getCmp('search-exps-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-exps-chkbx').setValue(false);
						}
						if (params.scope.indexOf('A') > -1) {
							Ext.getCmp('search-ars-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-ars-chkbx').setValue(false);
						}
						if (params.scope.indexOf('P') > -1) {
							Ext.getCmp('search-prbs-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-prbs-chkbx').setValue(false);
						}
						if (params.scope.indexOf('G') > -1) {
							Ext.getCmp('search-genes-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-genes-chkbx').setValue(false);
						}
						// removed until sequences have a page
						// see bug 2233
						/*if (params.scope.indexOf('S') > -1) {
							Ext.getCmp('search-seqs-chkbx').setValue(true);
						} else {*/
						Ext.getCmp('search-seqs-chkbx').setValue(false);
						//}
						if (params.scope.indexOf('M') > -1) {
							Ext.getCmp('search-genesets-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-genesets-chkbx').setValue(false);
						}
						if (params.scope.indexOf('N') > -1) {
							Ext.getCmp('search-eesets-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-eesets-chkbx').setValue(false);
						}
						if (params.scope.indexOf('B') > -1) {
							Ext.getCmp('search-papers-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-papers-chkbx').setValue(false);
						}
						if (params.scope.indexOf('H') > -1) {
							Ext.getCmp('search-forPhenotypes-chkbx').setValue(true);
						} else {
							Ext.getCmp('search-forPhenotypes-chkbx').setValue(false);
						}
					}
				} else {
					return false;
				}

				return true;

			},

			initComponent : function() {

				var showAdvancedOptions = Ext.get("hasAdmin").getValue();
				Ext.apply(this, {
							items : [{
										xtype : 'panel',
										layout : 'column',
										width: 450,
										items : [new Ext.form.TextField({
															id : 'search-text-field',
															fieldLabel : 'Search term(s)',
															name : 'query',
															name : 'query',
															columnWidth : 0.75,
															allowBlank : false,
															regex : new RegExp("[-\\w\\s]{3,}\\*?"),
															regexText : "Query contains invalid characters",
															minLengthText : "Query must be at least 3 characters long",
															msgTarget : "validation-messages",
															validateOnBlur : false,
															value : this.query,
															minLength : 3,
															listeners : {
																'specialkey' : {
																	fn : function(r, e) {
																		if (e.getKey() === e.ENTER) {
																			this.fireEvent("search");
																		}
																	}.createDelegate(this),
																	scope : this
																}
															}
														}),

												new Ext.Button({
															id : 'submit-button',
															text : 'Submit',
															name : 'Submit',
															columnWidth : 0.25,
															setSize : function() {
															},
															handler : function() {
																this.fireEvent("search");
															}.createDelegate(this)
														})]
									}, {
										xtype : 'fieldset',
										layout: 'table',
										ref: 'searchForSelects',
										layoutConfig:{
											columns:4
										},
										collapsible : true,
										collapsed : false,
										autoHeight : true,
										defaultType : 'checkbox',
										title : 'Items to search for',
										width : 450,
										items : [{
													id : 'search-genes-chkbx',
													name : "searchGenes",
													boxLabel : "Genes",
													stateful : true,
													stateEvents : ['check'],
													hideLabel : true,
													checked: true,
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													}
												}, {
													id : 'search-exps-chkbx',
													name : "searchExperiments",
													stateful : true,
													checked: true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													boxLabel : "Experiments",
													hideLabel : true
												}, {
													id : 'search-ars-chkbx',
													name : "searchArrays",
													boxLabel : "Arrays",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												}, {
													id : 'search-forPhenotypes-chkbx',
													name : "searchForPhenotypes",
													boxLabel : "Phenotypes",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												}, {
													id : 'search-genesets-chkbx',
													name : "searchGeneSets",
													boxLabel : "Gene groups",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												}, {
													id : 'search-eesets-chkbx',
													name : "searchEESets",
													boxLabel : "Experiment groups",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												},{
													id : 'search-prbs-chkbx',
													name : "searchProbes",
													boxLabel : "Probes",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												}, {
													id : 'search-seqs-chkbx',
													name : "searchSequences",
													boxLabel : "Sequences",
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													// sequence results don't link anywhere
													// see bug 2233
													hideLabel : true,
													checked:false,
													hidden:true,
													disabled:true
												}, {
													id : 'search-papers-chkbx',
													name : "searchPapers",
													boxLabel : "Annotated Papers",
													hidden: true,
													disabled: true,
													stateful : true,
													stateEvents : ['check'],
													getState : function() {
														return {
															value : this.getValue()
														};
													},
													applyState : function(state) {
														this.setValue(state.value);
													},
													hideLabel : true
												}]
									},{
										hidden: !showAdvancedOptions,
										width: 400,
										xtype: 'fieldset',
										ref: 'advancedSelects',
										layout: 'table',
										layoutConfig: {
											columns: 3
										},
										defaultType: 'checkbox',
										collapsible: true,
										collapsed: false,
										autoHeight: true,
										title: 'Advanced options',
										items: [{
											id: 'search-database-chkbx',
											name: "searchDatabase",
											boxLabel: "Search database",
											hideLabel: true,
											checked: true
										}, {
											id: 'search-indices-chkbx',
											name: "searchIndices",
											boxLabel: "Seach indices",
											hideLabel: true,
											checked: true
										}, {
											id: 'search-characteristics-chkbx',
											name: "searchCharacteristics",
											boxLabel: "Search characteristics",
											hideLabel: true,
											checked: true
										}, {
											id: 'search-go-chkbx',
											name: "searchGO",
											boxLabel: "Search GO groups (genes)",
											hideLabel: true,
											checked: false
										}, {
											id: 'search-usingPhenotypes-chkbx',
											name: "searchUsingPhenotypes",
											boxLabel: "Search phenotype Associations (genes)",
											hideLabel: true,
											checked: false,
											disabled: false
										}]
									}]
						});			

				Gemma.SearchForm.superclass.initComponent.call(this);
				this.addEvents("search");

				this.restoreState();
				
				this.addEvents('formResized');
				this.searchForSelects.on('collapse', function(){
					this.fireEvent('formResized', this.getHeight(), this.getWidth());
				}, this);
				this.searchForSelects.on('expand', function(){
					this.fireEvent('formResized', this.getHeight(), this.getWidth());
				}, this);
				this.advancedSelects.on('collapse', function(){
					this.fireEvent('formResized', this.getHeight(), this.getWidth());
				}, this);
				this.advancedSelects.on('expand', function(){
					this.fireEvent('formResized', this.getHeight(), this.getWidth());
				}, this);
				
				
			}, // end of initComponent
			adjustForIsAdmin : function(isAdmin){
				this.advancedSelects.setVisible(isAdmin);
			}
		});

Gemma.SearchGrid = Ext.extend(Ext.grid.GridPanel, {

	//width : 800,
	//height : 500,
	loadMask : true,
	stripeRows : true,
	collapsible : false,
	stateful : false,
	title : "Search results",
	selModel : new Ext.grid.RowSelectionModel({
				singleSelect : true
			}),
	record : Ext.data.Record.create([{
				name : "score",
				type : "float"
			}, {
				name : "resultClass",
				type : "string"
			}, {
				name : "id",
				type : "int"
			}, {
				name : "resultObject",
				sortType : this.sortInfo
			}, {
				name : "highlightedText",
				type : "string"
			}, {
				name : "indexSearchResult",
				type : "boolean"
			}]),

	toggleDetails : function(btn, pressed) {
		var view = this.getView();
		view.showPreview = pressed;
		view.refresh();
	},

	getSearchFun : function(text) {
		var value = new RegExp(Ext.escapeRe(text), 'i');
		return function(r, id) {
			var highlightedText = r.get("highlightedText");

			if (value.test(highlightedText)) {
				return true;
			}

			var clazz = r.get("resultClass");
			var obj = r.data.resultObject;
			if (clazz === "ExpressionExperimentValueObject") {
				return value.test(obj.shortName) || value.test(obj.name);
			} else if (clazz === "CompositeSequence") {
				return value.test(obj.name) || value.test(obj.description) || value.test(obj.arrayDesign.shortName);
			} else if (clazz === "ArrayDesignValueObject") {
				return value.test(obj.name) || value.test(obj.description);
			} else if (/^BioSequence.*/.exec(clazz)) { // because we get
				// proxies.
				return value.test(obj.name) || value.test(obj.description) || value.test(obj.taxon.commonName);
			} else if (clazz === "Gene" || clazz === "PredictedGene" || clazz === "ProbeAlignedRegion") {
				return value.test(obj.officialSymbol) || value.test(obj.officialName)
						|| value.test(obj.taxon.commonName);
			} else {
				return false;
			}
		};
	},

	searchForText : function(button, keyev) {
		var text = Ext.getCmp('search-in-grid').getValue();
		if (text.length < 2) {
			this.getStore().clearFilter();
			return;
		}
		this.getStore().filterBy(this.getSearchFun(text), this, 0);
	},

	initComponent : function() {
		var proxy = new Ext.data.DWRProxy(SearchService.search);
		var autoLoadStore = (this.autoLoadStore != undefined && this.autoLoadStore != null )? this.autoLoadStore: false;

		proxy.on("loadexception", this.handleLoadError.createDelegate(this));

		Ext.apply(this, {
					tbar : new Ext.Toolbar({
								items : [{
											pressed : true,
											enableToggle : true,
											text : 'Toggle details',
											tooltip : "Click to show/hide details for results",
											cls : 'x-btn-text-icon details',
											toggleHandler : this.toggleDetails.createDelegate(this)
										}, ' ', ' ', {
											xtype : 'textfield',
											id : 'search-in-grid',
											tabIndex : 1,
											enableKeyEvents : true,
											emptyText : 'Find in results',
											listeners : {
												"keyup" : {
													fn : this.searchForText.createDelegate(this),
													scope : this,
													options : {
														delay : 100
													}
												}
											}
										}]
							}),
					view : new Ext.grid.GroupingView({
								enableRowBody : true,
								showPreview : true,
								getRowClass : function(record, index, p, store) {
									if (this.showPreview) {
										p.body = "<p class='search-result-body' >" + record.get("highlightedText")
												+ "</p>"; // typo.css
									}
									return '';
								},
								startCollapsed : true,
								forceFit : true,
								groupTextTpl : '{text}s ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
							}),
					columns : [{
								header : "Category",
								width : 150,
								dataIndex : "resultClass",
								renderer : this.renderEntityClass,
								tooltip : "Type of search result",
								hidden : true,
								sortable : true
							}, {
								header : "Item",
								width : 480,
								dataIndex : "resultObject",
								renderer : this.renderEntity,
								tooltip : "a link to search result",
								sortable : true
							}, {
								header : "Score",
								width : 60,
								dataIndex : "score",
								tooltip : "How good of a match",
								hidden : true,
								sortable : true
							}, {
								header : "Matched via:",
								width : 180,
								hidden : true,
								dataIndex : "highlightedText",
								tooltip : "The text or part of the result that matched the search",
								sortable : true
							}],
					store : new Ext.data.GroupingStore({
								proxy : proxy,
								autoLoad: autoLoadStore,
								reader : new Ext.data.JsonReader({
											id : "id",
											root : "records",
											totalProperty : "totalRecords"
										}, this.record),
								remoteSort : false,
								groupField : 'resultClass',
								sortInfo : {
									field : "score",
									direction : "DESC"
								}
							})
				});
		Gemma.SearchGrid.superclass.initComponent.call(this);
		this.getStore().on("load", this.handleLoadSuccess.createDelegate(this));

	},

	handleLoadSuccess : function(scope, b, arg) {
		this.setTitle("Search results  --  "+scope.getCount() + " found");
		this.form.findById('submit-button').setDisabled(false);

		// If possible to expand all and not scroll then expand
		if (this.getStore().getCount() < Gemma.Search.MAX_AUTO_EXPAND_SIZE) {
			this.getView().expandAllGroups();
			return; // no point in checking below
		}

		// If there is only 1 returned group then expand it regardless of its
		// size.
		var lastResultClass = this.getStore().getAt(0).data.resultClass;
		var expand = true;
		var i = 1;
		for (i; i < this.getStore().getCount(); i++) {
			var record = this.getStore().getAt(i).data;
			if (record.resultClass !== lastResultClass) {
				expand = false;
			}
		}

		if (expand) {
			this.getView().expandAllGroups();
		}
	},

	handleLoadError : function(scope, b, message, exception) {
		this.fireEvent("loadError",message);
		Ext.DomHelper.overwrite('messages', {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
		Ext.DomHelper.append('messages', {
					tag : 'span',
					html : '&nbsp;&nbsp;' + message
				});
		this.form.findById('submit-button').setDisabled(false);
	},

	/*
	 * Renderers
	 */
	renderEntityClass : function(data, metadata, record, row, column, store) {
		var clazz = record.get("resultClass");
		if (clazz === "ExpressionExperimentValueObject") {
			return "Expression dataset";
		} else if (clazz === "CompositeSequence"  || clazz === "CompositeSequenceValueObject") {
			return "Probe";
		} else if (clazz === "ArrayDesignValueObject") {
			return "Array";
		} else if (/^BioSequence.*/.exec(clazz)) { // because we get proxies.
			return "Sequence";
		} else if (clazz === "GeneValueObject") {
			return "Gene";
		} else if (clazz === "GeneSetValueObject" || clazz === "DatabaseBackedGeneSetValueObject" ) {
			return "Gene group";
		} else if (clazz === "ExpressionExperimentSetValueObject" || clazz === "DatabaseBackedExpressionExperimentSetValueObject" ) {
			return "Experiment group";
		} else if (clazz === "BibliographicReferenceValueObject" ) {
			return "Annotated Paper";
		} else if (clazz === "CharacteristicValueObject" ) {
			return "Phenotype";
		} else {
			return clazz;
		}
	},

	sortInfo : function(record) {
		var clazz = record.resultsClass;
		if (clazz === "ExpressionExperimentValueObject") {
			return record.shortName;
		} else if (clazz === "CompositeSequence"  || clazz === "CompositeSequenceValueObject") {
			return record.name;
		} else if (clazz === "ArrayDesignValueObject") {
			return record.shortName;
		} else if (clazz === "BibliographicReferenceValueObject") {
			return record.citation;
		} else if (/^BioSequence.*/.exec(clazz)) { // because we get proxies.
			return record.name;
		} else if (clazz === "GeneValueObject" || clazz === 'GeneSetValueObject' || clazz === "DatabaseBackedGeneSetValueObject" 
				|| clazz === 'ExpressionExperimentSetValueObject' || clazz === "DatabaseBackedExpressionExperimentSetValueObject") {
			return record.name;
		} else {
			return clazz;
		}
	},

	renderEntity : function(data, metadata, record, row, column, store) {
		var dh = Ext.DomHelper;
		var clazz = record.get("resultClass");
		if (clazz === "ExpressionExperimentValueObject") {
			return "<a href=\"" + Gemma.LinkRoots.expressionExperimentPage +
			(data.sourceExperiment ? data.sourceExperiment : data.id) +
			"\">" +
			data.shortName +
			"</a> - " +
			data.name;
		} else if (clazz === "CompositeSequence" || clazz === "CompositeSequenceValueObject") {
			return "<a href=\"/Gemma/compositeSequence/show.html?id=" + data.id + "\">" + data.name + "</a> - " +
			(data.description ? data.description : "") +
			(data.arrayDesign ? "; Array: " + data.arrayDesign.shortName : '');
		} else if (clazz === "ArrayDesignValueObject") {
			return "<a href=\"/Gemma/arrays/showArrayDesign.html?id=" + data.id + "\">" + data.shortName + "</a>  " +
			data.name;
		} else if (/^BioSequence.*/.exec(clazz)) {
			return "<a href=\"/Gemma/genome/bioSequence/showBioSequence.html?id=" + data.id + "\">" + data.name +
			"</a> - " +
			data.taxon.commonName +
			" " +
			(data.description ? data.description : "");
		} else if (clazz === "GeneValueObject" || clazz === "PredictedGene" || clazz === "ProbeAlignedRegion") {
			return "<a href=\""+ Gemma.LinkRoots.genePage + data.id + "\">" + data.officialSymbol + 
			"</a><span style='color:grey'> " +
			data.taxonCommonName +
			"</span> " +
			((data.officialName && data.officialName !== null)?data.officialName:'');
		} else if ( clazz === "BibliographicReferenceValueObject" ) {
			return data.citation.citation +
					(new Ext.Template( Gemma.Common.tpl.pubmedLink.simple )).apply({
				    	pubmedURL: data.pubAccession
				    });
		} else if (clazz === "ExpressionExperimentSetValueObject" || clazz === "DatabaseBackedExpressionExperimentSetValueObject") {
			return "<a href=\""+ Gemma.LinkRoots.expressionExperimentSetPage + data.id + "\">" + data.name + "</a><span style='color:grey'> " +
			data.taxonName +
			"</span> (" +
			data.numExperiments +
			")";
		} else if (clazz === "GeneSetValueObject" || clazz === "DatabaseBackedGeneSetValueObject") {
			return "<a href=\"" + Gemma.LinkRoots.geneSetPage + data.id + "\">" + data.name + "</a><span style='color:grey'> " +
			data.taxonName +
			"</span> (" +
			data.size +
			")";
		}  else if ( clazz === "CharacteristicValueObject" ) {
			return "<a href=\"" + Gemma.LinkRoots.phenotypePage + data.urlId + "\">" + data.value + "</a><span style='color:grey'> " +
			data.valueUri +'</span>';
		} else {
			return data[0];
		}
	}

});
