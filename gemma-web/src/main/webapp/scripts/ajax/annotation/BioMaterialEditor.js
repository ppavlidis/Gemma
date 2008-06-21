Ext.namespace('Gemma');

Gemma.BioMaterialEditor = function(config) {
	return {
		originalConfig : config,
		expressionExperiment : {
			id : config.eeId,
			classDelegatingFor : "ExpressionExperiment"
		},

		dwrCallback : function(data) {
			config = {
				backingArray : data
			};
			Ext.apply(config, this.originalConfig);
			this.grid = new Gemma.BioMaterialGrid(config);
			this.grid.refresh = this.init.bind(this);
			this.grid.render();
		},

		/**
		 * Gets called on startup but also when a refresh is needed.
		 */
		init : function() {
			if (this.grid) {
				// grid.destroy() seems to be broken...
				try {
					this.grid.destroy();
				} catch (e) {
				}
			}
			ExperimentalDesignController.getBioMaterials(
					this.expressionExperiment, this.dwrCallback.bind(this));
		}
	};
};

Gemma.BioMaterialGrid = Ext.extend(Gemma.GemmaGridPanel, {
	viewConfig : {
		forceFit : true
	},

	/**
	 * See ExperimentalDesignController.getBioMaterials BioMaterialValueObject
	 * to see layout of the object that is passed.
	 * 
	 * @param biomaterial
	 *            A template so we know how many columns we need.
	 */
	createColumns : function(biomaterial) {
		var columns = [this.rowExpander, {
			id : "bm",
			header : "BioMaterial",
			dataIndex : "bmName",
			sortable : true
		}, {
			id : "ba",
			header : "BioAssay",
			dataIndex : "baName",
			sortable : true
		}];
		this.fvIdToDescription = biomaterial.factorValues;

		/*
		 * Generate a function to render the factor value column.
		 */
		this.columnRenderer = this
				.createValueRenderer(biomaterial.factorValues)
				.createDelegate(this);

		/*
		 * Each row has a combo that holds the possible factor values.
		 */
		this.factorValueCombo = [];
		for (factorId in biomaterial.factors) {
			// f is the id.
			if (factorId.indexOf("factor") >= 0) {
				// strip "factor" from the id
				var efId = factorId.substring(6);
				this.factorValueCombo[factorId] = new Gemma.FactorValueCombo({
					efId : efId,
					lazyInit : false,
					lazyRender : true,
					record : this.fvRecord
				});
				var editor;
				if (this.editable) {
					editor = this.factorValueCombo[factorId];
				}

				/*
				 * Define the column for this particular factor.
				 */
				columns.push({
					id : factorId,
					header : biomaterial.factors[factorId],
					dataIndex : factorId,
					renderer : this.columnRenderer,
					editor : editor,
					sortable : true
				});
			}
		}
		return columns;
	},

	/**
	 * See ExperimentalDesignController.getBioMaterials BioMaterialValueObject
	 * to see layout of the object that is passed. *
	 * 
	 * @param biomaterial
	 *            A template so we know how the records will be laid out.
	 */
	createRecord : function(biomaterial) {

		var fields = [{
			name : "id",
			type : "int"
		}, {
			name : "bmName",
			type : "string"
		}, {
			name : "bmDesc",
			type : "string"
		}, {
			name : "bmChars",
			type : "string"
		}, {
			name : "baName",
			type : "string"
		}, {
			name : "baDesc",
			type : "string"
		}];

		// Add one slot per factor. The name of the fields will be like
		// 'factor428'.
		if (biomaterial.factors) {
			for (factorId in biomaterial.factors) {
				if (factorId.indexOf("factor") >= 0) {
					var o = {
						name : biomaterial.factors[factorId],
						type : "string"
					};
					fields.push(o);
				}
			}
		}
		var record = Ext.data.Record.create(fields);
		return record;
	},

	initComponent : function() {

		var data = this.transformData(this.backingArray);

		this.record = this.createRecord(this.backingArray[0]);

		Ext.apply(this, {
			factorValueCombo : {},
			plugins : this.rowExpander,
			columns : this.createColumns(this.backingArray[0]),
			store : new Ext.data.Store({
				proxy : new Ext.data.MemoryProxy(data),
				reader : new Ext.data.ArrayReader({}, this.record)
			})
		});

		Gemma.BioMaterialGrid.superclass.initComponent.call(this);

		if (this.editable) {
			this.on("afteredit", function(e) {
				var factorId = this.getColumnModel().getColumnId(e.column);
				var combo = this.factorValueCombo[factorId];
				var fvvo = combo.getFactorValue.call(combo);
				e.record.set(factorId, fvvo.factorValueId);
				this.getView().refresh();
			});
		}

		var tbar = new Gemma.BioMaterialToolbar({
			grid : this,
			renderTo : this.tbar
		});

		this.getStore().load();
	},

	/**
	 * Turn the incoming biomaterial valueobjects into an array structure that
	 * can be loaded into an ArrayReader.
	 */
	transformData : function(incoming) {
		var data = [];
		for (var i = 0; i < incoming.length; ++i) {
			var bmvo = incoming[i];
			var factors = incoming[i].factors;
			data[i] = [bmvo.id, bmvo.name, bmvo.description,
					bmvo.characteristics, bmvo.assayName, bmvo.assayDescription];

			for (factorId in factors) {
				if (factorId.indexOf("factor") >= 0) {
					data[i].push(incoming[i].factorIdToFactorValueId[factorId]);
				}
			}
		}
		return data;
	},

	fvRecord : Ext.data.Record.create([{
		name : "charId",
		type : "int"
	}, {
		name : "factorValueId",
		type : "string",
		convert : function(v) {
			return "fv" + v;
		}
	}, {
		name : "category",
		type : "string"
	}, {
		name : "categoryUri",
		type : "string"
	}, {
		name : "value",
		type : "string"
	}, {
		name : "valueUri",
		type : "string"
	}, {
		name : "factorValueString",
		type : "string"
	}]),

	reloadFactorValues : function() {
		for (var i in this.factorValueCombo) {
			var factorId = this.factorValueCombo[i];
			if (factorId.substring(0, 6) == "factor") {
				var combo = this.factorValueCombo[factorId];
				var column = this.getColumnModel().getColumnById(factorId);
				combo.setExperimentalFactor(combo.experimentalFactor.id,
						function(r, options, success) {
							var fvs = {};
							for (var i = 0; i < r.length; ++i) {
								fvs["fv" + r[i].data.factorValueId] = r[i].data.factorValueString;
							}
							var renderer = this.createValueRenderer(fvs);
							column.renderer = renderer;
							this.getView().refresh();
						});
			}
		}
	},

	createValueRenderer : function(factorValues) {
		return function(value, metadata, record, row, col, ds) {
			return factorValues[value] ? factorValues[value] : value;
		};
	},

	rowExpander : new Ext.grid.RowExpander({
		tpl : new Ext.Template(
				"<dl style='margin-left: 1em; margin-bottom: 2px;'><dt>BioMaterial {bmName}</dt><dd>{bmDesc}<br>{bmChars}</dd>",
				"<dt>BioAssay {baName}</dt><dd>{baDesc}</dd></dl>")
	})

});

/*
 * Gemma.BioMaterialToolbar constructor... config is a hash with the following
 * options: grid is the grid that contains the factor values.
 */
Gemma.BioMaterialToolbar = function(config) {

	this.grid = config.grid;
	delete config.grid;
	this.editable = this.grid.editable;

	/*
	 * keep a reference to ourselves so we don't have to worry about scope in
	 * the button handlers below...
	 */
	var thisToolbar = this;

	/*
	 * establish default config options...
	 */
	var superConfig = {};

	/*
	 * add our items in front of anything specified in the config above...
	 */
	var saveButton = new Ext.Toolbar.Button({
		text : "save",
		tooltip : "Save changed biomaterials",
		disabled : true,
		handler : function() {
			var edited = thisToolbar.grid.getEditedRecords();
			var bmvos = [];
			for (var i = 0; i < edited.length; ++i) {
				var row = edited[i];
				var bmvo = {
					id : row.id,
					factorIdToFactorValueId : {}
				};
				for (var j in row) {
					if (row[j].substring(0, 6) == "factor") {
						bmvo.factorIdToFactorValueId[j] = row[j];
					}
				}
				bmvos.push(bmvo);
			}
			var callback = thisToolbar.grid.refresh.bind(thisToolbar.grid);
			ExperimentalDesignController.updateBioMaterials(bmvos, callback);
			saveButton.disable();
		}
	});
	this.grid.on("afteredit", function(model) {
		saveButton.enable();
	});

	var revertButton = new Ext.Toolbar.Button({
		text : "revert",
		tooltip : "Undo changes to selected biomaterials",
		disabled : true,
		handler : function() {
			thisToolbar.grid.revertSelected();
		}
	});
	this.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		revertButton.disable();
		for (var i = 0; i < selected.length; ++i) {
			if (selected[i].dirty) {
				revertButton.enable();
				break;
			}
		}
	});

	var refreshButton = new Ext.Toolbar.Button({
		text : "Expand/collapse all",
		tooltip : "Show/hide all biomaterial details",
		handler : function() {
			var expander = this.grid.rowExpander;
			expander.toggleAll();
		}
	});

	var items = [];
	if (this.editable) {
		items.push(new Ext.Toolbar.TextItem("Make changes in the grid below:"),
				new Ext.Toolbar.Spacer(), saveButton,
				new Ext.Toolbar.Separator(), revertButton);
	}
	items.push(new Ext.Toolbar.Fill(), refreshButton);
	config.items = config.items ? items.concat(config.items) : items;

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.BioMaterialToolbar.superclass.constructor.call(this, superConfig);

	if (this.editable) {
		this.factorCombo = new Gemma.ExperimentalFactorCombo({
			emptyText : "select a factor",
			edId : this.grid.edId
		});
		var factorCombo = this.factorCombo;
		factorCombo.on("select", function(combo, record, index) {
			factorValueCombo.setExperimentalFactor(record.id);
			factorValueCombo.enable(); // TODO do this in the callback
		});

		this.factorValueCombo = new Gemma.FactorValueCombo({
			emptyText : "select a factor value",
			disabled : true
		});
		var factorValueCombo = this.factorValueCombo;
		factorValueCombo.on("select", function(combo, record, index) {
			thisToolbar.grid.getSelectionModel().on("selectionchange",
					enableApplyOnSelect);
			enableApplyOnSelect(thisToolbar.grid.getSelectionModel());
		});

		var applyButton = new Ext.Toolbar.Button({
			text : "apply",
			tooltip : "Apply this value to selected biomaterials",
			disabled : true,
			handler : function() {
				var selected = thisToolbar.grid.getSelectionModel()
						.getSelections();
				var factor = "factor" + factorCombo.getValue();
				var factorValue = "fv" + factorValueCombo.getValue();
				for (var i = 0; i < selected.length; ++i) {
					selected[i].set(factor, factorValue);
				}
				saveButton.enable();
				thisToolbar.grid.getView().refresh();
			}
		});
		var enableApplyOnSelect = function(model) {
			var selected = model.getSelections();
			if (selected.length > 0) {
				applyButton.enable();
			} else {
				applyButton.disable();
			}
		};

		var secondToolbar = new Ext.Toolbar(this.getEl().createChild());
		secondToolbar.addText("Bulk changes:");
		secondToolbar.addSpacer();
		secondToolbar.addField(factorCombo);
		secondToolbar.addSpacer();
		secondToolbar.addField(factorValueCombo);
		secondToolbar.addSpacer();
		secondToolbar.addField(applyButton);
	}

};

/*
 * instance methods...
 */
Ext.extend(Gemma.BioMaterialToolbar, Ext.Toolbar, {

});