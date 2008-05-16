Ext.namespace('Ext.Gemma');

/**
 * Combobox to display available taxa.
 * 
 * @class Ext.Gemma.TaxonCombo
 * @extends Ext.form.ComboBox
 */
Ext.Gemma.TaxonCombo = Ext.extend(Ext.form.ComboBox, {

	displayField : 'commonName',
	valueField : 'id',
	editable : false,
	loadingText : "Loading ...",
	triggerAction : 'all', // so selecting doesn't hide the others
	mode : 'local', // because we load only at startup.
	listWidth : 150,
	width : 120,
	stateId : "Ext.Gemma.TaxonCombo",

	emptyText : 'Select a taxon',

	setState : function(v) {
		if (this.state) {
			this.setValue(this.state);
		} else {
			this.state = v;
		}
	},

	restoreState : function() {
		this.setValue(this.state);
		delete this.state;
		this.fireEvent('ready');
	},

	record : Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "commonName",
		type : "string"
	}, {
		name : "scientificName",
		type : "string"
	}]),

	initComponent : function() {

		this.addEvents('taxonchanged', 'ready');

		var tmpl = new Ext.XTemplate('<tpl for="."><div class="x-combo-list-item">{commonName} ({scientificName})</div></tpl>');

		Ext.apply(this, {
			store : new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(GenePickerController.getTaxa),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, this.record),
				remoteSort : true
			}),
			tpl : tmpl
		});

		this.store.load({
			params : [],
			callback : this.restoreState.createDelegate(this),
			scope : this,
			add : false
		});

		Ext.Gemma.TaxonCombo.superclass.initComponent.call(this);
	},

	setValue : function(v) {

		var changed = false;
		if (this.getValue() != v) {
			changed = true;
		}

		Ext.Gemma.TaxonCombo.superclass.setValue.call(this, v);

		if (changed) {
			this.fireEvent('taxonchanged', this.getTaxon());
		}
	},

	getTaxon : function() {
		return this.store.getById(this.getValue());
	},

	/**
	 * To allow setting programmatically.
	 * 
	 * @param {}
	 *            taxon
	 */
	setTaxon : function(taxon) {
		if (taxon.id) {
			this.setValue(taxon.id);
		} else {
			this.setValue(taxon);
		}
	},

	getTaxonByScientificName : function(scientificName) {
		var all = this.store.getRange();
		for (var i = 0; i < all.length; ++i) {
			if (all[i].data.scientificName === scientificName) {
				return all[i].data;
			}
		}
	}

});
