/*
 * Panel for choosing datasets based on search criteria, or on established
 * "ExpressionExperimentSets".
 * 
 * At the top, the available ExpressionExperimentSets are shown. At the bottom
 * left, searching for experiments; at the bottom right, the experiments that
 * are in the set. If the user is an admin, they can save new
 * ExpressionExperimentSets to the database; otherwise they are saved for the
 * user in a cookie.
 * 
 * @author Paul
 * 
 * @version $Id$
 */
Ext.namespace('Ext.Gemma');
Ext.namespace('Ext.Gemma.ExpressionExperimentSetStore');

/**
 * 
 * @class Ext.Gemma.ExpressionExperimentSetPanel
 * @extends Ext.Panel
 */
Ext.Gemma.ExpressionExperimentSetPanel = Ext.extend(Ext.Panel, {

	layout : 'table',
	layoutConfig : {
		columns : 2
	},
	border : false,
	width : 220,

	setState : function(state) {
		if (this.ready) {
			this.selectById(state);
		} else {
			this.storedState = state;
		}
	},

	restoreState : function() {
		if (this.storedState) {
			this.selectById(this.storedState);
			delete this.storedState;
		}
		this.ready = true;
	},

	getSelected : function() {
		return this.store.getSelected();
	},

	selectById : function(id) {
		this.combo.setValue(id);
		this.store.selected = this.store.getById(id);
	//	Ext.log(this.store.selected.get("name"));
	},

	selectByName : function(name) {
		var index = this.store.findBy(function(record, i) {
			return record.get("name") == name;
		});
		var rec = this.store.getAt(index);
		this.combo.setValue(rec.get("id"));
		this.store.select(rec);
	},

	filterByTaxon : function(taxon) {
		this.combo.filterByTaxon(taxon); // side effect: grid is filered too.
	},

	initComponent : function() {

		this.store = new Ext.Gemma.ExpressionExperimentSetStore();

		this.dcp = new Ext.Gemma.DatasetChooserPanel({
			modal : true,
			eeSetStore : this.store
		});

		this.combo = new Ext.Gemma.ExpressionExperimentSetCombo({
			width : 175,
			store : this.store
		});

		Ext.Gemma.ExpressionExperimentSetPanel.superclass.initComponent
				.call(this);

		this.addEvents('set-chosen');

		this.combo.on("select", function(combo, sel) {
			this.fireEvent('set-chosen', sel);
		}.createDelegate(this));

		this.dcp.on("datasets-selected", function(sel) {
			this.combo.setValue(sel.get("name"));
			this.fireEvent('set-chosen', sel);
		}.createDelegate(this));

		this.store.on("load", this.restoreState.createDelegate(this));

	},

	onRender : function(ct, position) {
		Ext.Gemma.ExpressionExperimentSetPanel.superclass.onRender.call(this,
				ct, position);

		this.add(this.combo);

		this.add(new Ext.Button({
			text : "Edit",
			anchor : '',
			tooltip : "View dataset chooser interface to modify or create sets",
			handler : function() {
				this.dcp.show({
					selected : this.getSelected()
				});
			},
			scope : this
		}));

	}

});

/**
 * ComboBox to show ExpressionExperimentSets. Configure this with a
 * ExpressionExperimentSetStore.
 * 
 * @class Ext.Gemma.ExpressionExperimentSetCombo
 * @extends Ext.form.ComboBox
 */
Ext.Gemma.ExpressionExperimentSetCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'name',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	listWidth : 250,
	forceSelection : true,
	mode : 'local',
	triggerAction : 'all',
	emptyText : 'Select a search scope',

	filterByTaxon : function(taxon) {
		if (!taxon) {
			return;
		}

		this.store.clearFilter();
		this.store.filterBy(function(record, id) {
			if (!record.get("taxon")) {
				return true; // in case there is none.
			} else if (taxon.id == record.get("taxon").id) {
				return true;
			} else {
				return false;
			}
		});
		this.onLoad();

		if (this.store.getSelected()
				&& this.store.getSelected().get("taxon").id != taxon.id) {
			this.setValue("");
		}
	},

	initComponent : function() {

		Ext.Gemma.ExpressionExperimentSetCombo.superclass.initComponent
				.call(this);

		this.tpl = new Ext.XTemplate('<tpl for="."><div ext:qtip="{description} ({numExperiments} members)" class="x-combo-list-item">{name}{[ values.taxon ? " (" + values.taxon.scientificName + ")" : "" ]}</div></tpl>');
		this.tpl.compile();

		this.on("select", function(cb, rec, index) {
			this.store.setSelected(rec);
		});

	}

});

/**
 * @class Ext.Gemma.ExpressionExperimentSetStore
 * @extends Ext.data.Store
 */
Ext.Gemma.ExpressionExperimentSetStore = function(config) {

	this.record = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "name"
	}, {
		name : "description"
	}, {
		name : "numExperiments",
		type : "int"
	}, {
		name : "modifiable",
		type : "bool"
	}, {
		name : "expressionExperimentIds"
	}, {
		name : "taxon"
	}]);

	this.addEvents('ready');

	this.readMethod = ExpressionExperimentSetController.getAvailableExpressionExperimentSets;

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
		id : "id"
	}, this.record);

	Ext.Gemma.ExpressionExperimentSetStore.superclass.constructor.call(this,
			config);

	this.on("load", this.addFromCookie, this);

	this.load();

};

Ext.extend(Ext.Gemma.ExpressionExperimentSetStore, Ext.data.Store, {

	getSelected : function() {
		return this.selected;
	},

	setSelected : function(rec) {
		this.selected = rec;
	},

	addFromCookie : function() {
		var recs = this.cookieRetrieveEESets();
		if (recs && recs.length > 0) {
			// Ext.log("Add " + recs.length + " from cookie");
			this.add(recs);
		}
		this.fireEvent("ready");
	},

	cookieSaveOrUpdateEESet : function(rec) {

		var eeSets = this.cookieRetrieveEESets();
		var toBeUpdated = this.searchCookie(eeSets, rec);

		if (toBeUpdated) {
			// Ext.log("Modifying record");
			toBeUpdated.set("name", rec.get("name"));
			toBeUpdated.set("description", rec.get("description"));
			toBeUpdated.set("expressionExperimentIds", rec
					.get("expressionExperimentIds"));
			toBeUpdated.set("taxon", rec.get("taxon"));
			toBeUpdated.commit();
		} else {
			// Ext.log("Adding record");
			eeSets.push(rec);
		}

		this.cookieSaveEESets(eeSets);

		Ext.Msg.show({
			title : "OK",
			msg : "Saved to cookie",
			icon : Ext.MessageBox.INFO,
			buttons : Ext.Msg.OK
		});
	},

	/**
	 * See if the cookie already has an item to match the given one.
	 */
	searchCookie : function(storedSets, rec) {

		var recName = rec.get("name");

		for (var i = 0, len = storedSets.length; i < len; i++) {
			var s = storedSets[i];

			// Ext.log("Comparing " + s.get("name") + " to " + recName);
			if (s.get("name") == recName) {
				// Ext.log("Found existing set in cookie");
				return s;
			}
		}
		return null;
	},

	removeFromCookie : function(rec) {
		var eeSets = this.cookieRetrieveEESets();
		var updatedSets = [];
		for (var i = 0, len = eeSets.length; i < len; i++) {
			var s = eeSets[i];
			if (s.get("name") != rec.get("name")) {
				updatedSets.push(s);
			} else {
				// Ext.log("Remove " + s.get("name") + " from cookie");
			}
		}

		this.cookieSaveEESets(updatedSets);
	},

	/**
	 * 
	 * @param {}
	 *            eeSets [Records]
	 */
	cookieSaveEESets : function(eeSets) {
		var eeSetData = [];
		for (var i = 0, len = eeSets.length; i < len; i++) {
			eeSetData.push(eeSets[i].data);
		}
		Ext.state.Manager.set(
				Ext.Gemma.ExpressionExperimentSetStore.COOKIE_KEY, eeSetData);
		this.fireEvent("saveOrUpdate");
	},

	/**
	 * Retrieve EESets from the user's cookie.
	 * 
	 * @return {}
	 */
	cookieRetrieveEESets : function() {
		var storedSets = Ext.state.Manager
				.get(Ext.Gemma.ExpressionExperimentSetStore.COOKIE_KEY);
		var eeSets = [];
		if (storedSets && storedSets.length > 0) {
			for (var i = 0, len = storedSets.length; i < len; i++) {
				if (storedSets[i] && storedSets[i].name) {
					var rec = new this.record(storedSets[i]);
					if (rec && rec.data) { // make sure data aren't
						// corrupt.
						eeSets.push(rec);
					}
				}
			}
		}
		return eeSets;
	}

});

Ext.Gemma.ExpressionExperimentSetStore.COOKIE_KEY = "eeSets";

/**
 * User interface for viewing, creating and editing ExpressionExperimentSets.
 * 
 * @class Ext.Gemma.DatasetChooserPanel
 * @extends Ext.Window
 */
Ext.Gemma.DatasetChooserPanel = Ext.extend(Ext.Window, {
	id : 'dataset-chooser',
	layout : 'border',
	width : 800,
	height : 500,
	closeAction : 'hide',
	constrainHeader : true,

	onCommit : function() {
		var rec = this.eeSetGrid.getSelectionModel().getSelected();
		if (rec) {
			this.eeSetStore.setSelected(rec);
			this.fireEvent("datasets-selected", rec);
		}
		this.hide();
	},

	initComponent : function() {

		Ext.apply(this, {
			buttons : [{
				id : 'done-selecting-button',
				text : "Done",
				handler : this.onCommit.createDelegate(this),
				scope : this
			}]
		});

		this.addEvents({
			"datasets-selected" : true
		});

		Ext.Gemma.DatasetChooserPanel.superclass.initComponent.call(this);

	},

	show : function(config) {
		if (config && config.selected) {
			// Avoid adding handler multiple times.
			this.on("show", function() {
				this.eeSetGrid.getSelectionModel()
						.selectRecords([config.selected]);
				this.eeSetGrid.getView().focusRow(this.eeSetGrid.getStore()
						.indexOf(config.selected));
			}, this, {
				single : true
			}, config);
		}

		Ext.Gemma.DatasetChooserPanel.superclass.show.call(this);
	},

	onRender : function(ct, position) {
		Ext.Gemma.DatasetChooserPanel.superclass.onRender.call(this, ct,
				position);

		var admin = dwr.util.getValue("hasAdmin");

		/**
		 * Plain grid for displaying datasets in the current set. Editable.
		 */
		this.eeSetMembersGrid = new Ext.Gemma.ExpressionExperimentGrid({
			editable : admin,
			region : 'center',
			title : "Datasets in current set",
			pageSize : 15,
			loadMask : {
				msg : 'Loading datasets ...'
			},
			split : true,
			height : 200,
			width : 400,
			rowExpander : true
		});

		/**
		 * Datasets that can be added to the current set.
		 */
		this.sourceDatasetsGrid = new Ext.Gemma.ExpressionExperimentGrid({
			editable : false,
			admin : admin,
			title : "Dataset locator",
			region : 'west',
			split : true,
			pageSize : 15,
			height : 200,
			loadMask : {
				msg : 'Searching ...'
			},
			width : 400,
			rowExpander : true,
			tbar : new Ext.Gemma.DataSetSearchAndGrabToolbar({
				taxonSearch : true,
				targetGrid : this.eeSetMembersGrid
			})
		});

		/**
		 * Top grid for showing the EEsets
		 */
		this.eeSetGrid = new Ext.Gemma.ExpressionExperimentSetGrid({
			store : this.eeSetStore,
			editable : admin,
			region : 'north',
			layout : 'fit',
			split : true,
			collapsible : true,
			collapseMode : 'mini',
			loadMask : {
				msg : 'Loading'
			},
			height : 200,
			title : "Available expression experiment sets",
			displayGrid : this.eeSetMembersGrid,
			searchGrid : this.sourceDatasetsGrid,
			tbar : new Ext.Gemma.EditExpressionExperimentSetToolbar({
				admin : admin
			})

		});

		this.add(this.eeSetGrid);
		this.add(this.eeSetMembersGrid);
		this.add(this.sourceDatasetsGrid);

	}

});

/**
 * 
 * @class Ext.Gemma.ExpressionExperimentSetGrid
 * @extends Ext.grid.GridPanel
 */
Ext.Gemma.ExpressionExperimentSetGrid = Ext.extend(Ext.grid.EditorGridPanel, {

	autoExpandColumn : 'description',
	selModel : new Ext.grid.RowSelectionModel({
		singleSelect : true
	}),
	stripeRows : true,
	viewConfig : {
		forceFit : true
	},
	autoExpandMax : 400,
	autoExpandColumn : "description",

	initComponent : function() {

		Ext.Gemma.ExpressionExperimentSetGrid.superclass.initComponent
				.call(this);

		if (!this.store) {
			Ext.apply(this, {
				store : new Ext.Gemma.ExpressionExperimentSetStore()
			});
		}

		this.addEvents({
			'loadExpressionExperimentSet' : true,
			'dirty' : true
		});

		this.record = this.getStore().record;

		this.on("dirty", this.getTopToolbar().editing, this.getTopToolbar());

	},

	afterRender : function() {
		Ext.Gemma.ExpressionExperimentSetGrid.superclass.afterRender.call(this);

		this.getTopToolbar().grid = this;

		this.getSelectionModel().on("selectionchange", function() {
			if (this.getCurrentSet && this.getCurrentSet()
					&& this.getCurrentSet().dirty) {
				this.cloneBut.enable();
				this.resetBut.enable();
				this.commitBut.enable();
			}
		}, this.getTopToolbar());

		this.getStore().on("datachanged", function() {
			if (this.getCurrentSet && this.getCurrentSet()
					&& this.getCurrentSet().dirty) {
				this.cloneBut.enable();
				this.resetBut.enable();
				this.commitBut.enable();
			}
		});

		this.getSelectionModel().on("rowselect", function(selmol, index, rec) {
			if (this.displayGrid) {
				this.displayGrid.setTitle(rec.get("name"));
				this.displayGrid.getStore().load({
					params : [rec.get("expressionExperimentIds")]
				});
			}

			if (this.searchGrid && rec.get("taxon")) {
				this.searchGrid.getTopToolbar().filterTaxon(rec.get("taxon"));
			}
		}, this);

		/*
		 * Suppress updates while loading
		 */
		this.displayGrid.on("beforeload", function() {
			this.displayGrid.un("add", this.updateMembers);
			this.displayGrid.un("remove", this.updateMembers);
		});

		/*
		 * Update record. We add these listeners after the initial load so they
		 * aren't fired right away.
		 */
		this.displayGrid.getStore().on("load", function() {
			this.displayGrid.getStore().on("add",
					this.updateMembers.createDelegate(this),
					[this.displayGrid.getStore()]);
			this.displayGrid.getStore().on("remove",
					this.updateMembers.createDelegate(this),
					[this.displayGrid.getStore()]);

		}, this);
	},

	updateMembers : function(store) {
		var rec = this.getSelectionModel().getSelected();

		var ids = [];
		store.each(function(rec) {
			ids.push(rec.get("id"));
		});
		rec.set("expressionExperimentIds", ids);
		rec.set("numExperiments", ids.length);

		this.fireEvent("dirty", rec);
	},

	display : function() {
		// Show the selected eeset members in the lower right-hand grid (or
		// empty)

		var rec = this.getSelectionModel().getSelected();
		if (rec) {
			this.displayGrid.getStore().removeAll();
			this.displayGrid.getStore().load({
				params : [rec.get("expressionExperimentIds")]
			});
			this.displayGrid.setTitle(rec.get("name"));
		}

	},

	clearDisplay : function() {
		// Show the selected eeset members in the lower right-hand grid (or
		// empty)
		var rec = this.getSelectionModel().getSelected();
		if (rec) {
			this.displayGrid.getStore().removeAll();
			this.displayGrid.setTitle(rec.get("name"));
		}
	},

	columns : [{
		id : 'name',
		header : "Name",
		dataIndex : "name",
		sortable : true,
		editor : new Ext.form.TextField({
			allowBlank : false
		})
	}, {
		id : 'description',
		header : "Description",
		dataIndex : "description",
		sortable : true,
		editor : new Ext.form.TextField({
			allowBlank : false
		})
	}, {
		id : 'datasets',
		header : "Num datasets",
		dataIndex : "numExperiments",
		sortable : true
	}, {
		id : 'taxon',
		header : "Taxon",
		dataIndex : "taxon",
		sortable : true,
		renderer : function(v) {
			if (v) {
				return v.commonName;
			} else {
				return "";
			}
		}
	}]

});

/**
 * Toolbar for creating/updating expressionExperimentSet. Attach to the
 * virtualAnalysisGrid. Either save to the database or to a cookie.
 */
Ext.Gemma.EditExpressionExperimentSetToolbar = Ext.extend(Ext.Toolbar, {

	userCanWriteToDB : false, // FIXME configure this properly.

	display : function() {
		this.grid.display();
	},

	getCurrentSetEEIds : function() {
		return this.getCurrentSet().get("expressionExperimentIds");
	},

	getCurrentSet : function() {
		var sm = this.grid.getSelectionModel();
		return sm.getSelected();
	},

	getCurrentSetId : function() {
		return this.getCurrentSet().get("id");
	},

	getNewDetails : function() {

		if (!this.detailsWin) {
			this.detailsWin = new Ext.Gemma.DetailsWindow({
				store : this.grid.getStore()
			});
		}

		this.detailsWin.purgeListeners();
		this.detailsWin.on("commit", function(args) {
			// Ext.log("Add new record");
			var constr = this.grid.getStore().record;
			var newRec = new constr({
				name : args.name,
				description : args.description,
				id : -1,
				expressionExperimentIds : [],
				numExperiments : 0,
				taxon : args.taxon
			}); // Ext creates the id.

			this.grid.getStore().add(newRec);
			this.grid.getSelectionModel().selectRecords([newRec]);
			this.grid.getView().focusRow(this.grid.getStore().indexOf(newRec));
			this.grid.clearDisplay();
			this.fireEvent("taxonset", args.taxon);
			this.commitBut.enable();

		}, this);

		this.detailsWin.name = '';
		this.detailsWin.description = '';
		this.detailsWin.show();
	},

	afterRender : function() {
		Ext.Gemma.EditExpressionExperimentSetToolbar.superclass.afterRender
				.call(this);

		// this.addButton(this.displayBut);
		// this.addSeparator();
		this.addFill();
		this.addButton(this.newBut);
		this.addButton(this.commitBut);
		this.addButton(this.cloneBut);
		this.addButton(this.resetBut);
		this.addButton(this.deleteBut);
		this.addButton(this.clearFilterBut);

		this.on("disable", function() {
			// Ext.log("Someone disabled me!");
			this.enable();
		});

	},

	initComponent : function() {

		Ext.Gemma.EditExpressionExperimentSetToolbar.superclass.initComponent
				.call(this);

		this.newBut = new Ext.Button({
			id : 'newnew',
			text : "New",
			handler : this.initNew,
			scope : this,
			disabled : false,
			tooltip : "Start a new set (click update to save when you are done)"
		});

		this.commitBut = new Ext.Button({
			id : 'update',
			text : "Save",
			handler : this.update,
			disabled : false,
			scope : this,
			tooltip : "Save or update the set"
		});

		this.cloneBut = new Ext.Button({
			id : 'newsave',
			text : "Clone",
			handler : this.copy,
			scope : this,
			disabled : false,
			tooltip : "Create as new set (click update to save)"
		});

		this.resetBut = new Ext.Button({
			id : 'reset',
			text : "Reset",
			handler : this.reset,
			scope : this,
			disabled : false,
			tooltip : "Reset to stored version"
		});

		this.deleteBut = new Ext.Button({
			id : 'delete',
			text : "Delete",
			handler : this.remove,
			scope : this,
			disabled : false,
			tooltip : "Delete selected set"
		});

		this.clearFilterBut = new Ext.Button({
			id : 'clearFilt',
			text : "Show all",
			handler : this.clearFilter,
			scope : this,
			disabled : false,
			tooltip : "Clear filters"
		});

		this.addEvents('saveOrUpdate', 'taxonset');
		this.on("saveOrUpdate", function() {
			this.commitBut.disable();
		});

		this.resetBut.on("enable", function() {
			// Ext.log("Attempt to enable resetBut");
		});
	},

	initNew : function() {
		// Ext.log("init");
		this.resetBut.disable();
		this.commitBut.disable();
		this.getNewDetails();
	},

	remove : function() {
		var rec = this.getCurrentSet();
		if (rec) {
			Ext.Msg
					.confirm(
							"Delete?",
							"Are you sure you want to delete this set? This cannot be undone.",
							function(but) {

								if (but == 'no') {
									return;
								}

								if (rec.get("id") < 0) {
									this.grid.getStore().remove(rec);
									this.grid.getStore().removeFromCookie(rec);
								} else {
									if (this.userCanWriteToDB) {
										// Ext.log("Deleting from DB");
										this.grid.getStore().remove(rec);
										/* Delete from db */
									} else {
										Ext.Msg
												.alert("Permission denied",
														"Sorry, you can't delete this set.");
									}
								}
								this.deleteBut.enable();
							}, this);
		}
	},

	clearFilter : function() {
		this.grid.getStore().clearFilter();
	},

	/**
	 * Save or update a record. If possible save it to the database; otherwise
	 * use a cookie store.
	 */
	update : function() {
		// Ext.log("update");
		this.resetBut.disable();
		this.commitBut.disable();

		// if the current set has no members, forget it.(save button should be
		// disabled)
		var rec = this.getCurrentSet();

		if (!rec.get("expressionExperimentIds")
				|| rec.get("expressionExperimentIds").length === 0) {
			// Ext.log("no members");
			return;
		}

		if (!rec.dirty) {
			// Ext.log("Not dirty");
			return;
		}

		if (rec.get("id") < 0) {
			if (this.userCanWriteToDB) {
				// Ext.log("Writing new to db");
				/* write new one to the db */
			} else {
				// Ext.log("Writing to cookie");
				this.grid.getStore().cookieSaveOrUpdateEESet(rec);
			}
		} else {
			if (this.userCanWriteToDB) {
				// Ext.log("Updating to db");
				/* write updated one to the db */
			} else {
				Ext.Msg
						.alert("Sorry, you can't edit this set. Try saving a clone instead.");
			}
		}

		rec.commit(); // to make it non-dirty.
		this.cloneBut.enable();
	},

	copy : function() {
		// Ext.log("save as copy");
		// Create a copy, change the name and give dummy id.
		var rec = this.getCurrentSet();
		var constr = this.grid.getStore().record;
		var newRec = new constr({
			name : rec.get("name") + "*", // indicate they should edit it.
			description : rec.get("description"),
			id : -1,
			expressionExperimentIds : rec.get("expressionExperimentIds"),
			numExperiments : rec.get("numExperiments"),
			taxon : rec.get("taxon")
		}); // note that id is assigned by Ext.

		// ensure the new record is dirty.
		newRec.set("description", "");
		newRec.set("description", rec.get("description"));

		this.grid.getStore().add(newRec);
		this.grid.getSelectionModel().selectRecords([newRec]);
		this.grid.getView().focusRow(this.grid.getStore().indexOf(newRec));
		this.commitBut.enable();
		this.cloneBut.disable(); // until we change it.

	},

	reset : function() {
		// Ext.log("reset");
		if (this.getCurrentSet()) {
			this.getCurrentSet().reject();
			this.resetBut.disable();
			this.cloneBut.enable();
			this.display();
		}
	},

	editing : function() {
		// Ext.log("editing");
		this.cloneBut.enable();
		this.resetBut.enable();
		this.commitBut.enable();
		this.newBut.enable();
	}

});

Ext.Gemma.DetailsWindow = Ext.extend(Ext.Window, {
	width : 500,
	height : 300,
	closeAction : 'hide',
	id : 'eeset-dialog',
	title : "Provide or edit expression experiment set details",
	shadow : true,
	modal : true,

	onCommit : function() {

		var values = Ext.getCmp('eeset-form').getForm().getValues();

		var name = values.eesetname;
		if (!this.nameField.validate() || name === null) {
			Ext.Msg.alert("Sorry", "You must provide a name for the set");
			return;
		}

		var indexOfExisting = this.store.findBy(function(record, id) {
			return record.get("name") == name;
		}, this);

		this.hide();

		if (indexOfExisting >= 0) {
			Ext.Msg.alert("Sorry",
					"Please provide a previously unused name for the set");
			return;
		}

		return this.fireEvent("commit", {
			name : values.eesetname,
			description : values.eesetdescription,
			taxon : values.eesetTaxon
		});
	},

	initComponent : function() {

		this.nameField = new Ext.form.TextField({
			fieldLabel : 'Name',
			value : this.name,
			id : 'eesetname',
			minLength : 3,
			invalidText : "You must provide a name",
			width : 300
		});

		Ext.apply(this, {
			items : new Ext.FormPanel({
				frame : true,
				labelAlign : 'left',
				id : 'eeset-form',
				height : 250,
				items : new Ext.form.FieldSet({
					height : 200,
					items : [new Ext.Gemma.TaxonCombo({
						id : 'eesetTaxon',
						fieldLabel : 'Taxon'
					}), this.nameField, new Ext.form.TextArea({
						fieldLabel : 'Description',
						value : this.description,
						id : 'eesetdescription',
						width : 300
					})]
				}),
				buttons : [{
					text : "Cancel",
					handler : this.hide.createDelegate(this, [])
				}, {
					text : "OK",
					handler : this.onCommit.createDelegate(this),
					scope : this,
					tooltip : "OK"
				}]

			})
		});

		Ext.Gemma.DetailsWindow.superclass.initComponent.call(this);
	}
});
