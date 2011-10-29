
/**
 * Dialog to ask user for information about a new set (or potentially modifications to an existing one)
 * 
 * @extends Ext.Window
 */
Gemma.CreateSetDetailsWindow = Ext.extend(Ext.Window, {
			width : 500,
			height : 300,
			closeAction : 'hide',
			title : "Provide or edit group details",
			shadow : true,
			modal : true,
			/**
			 * set the taxon combo to the corresponding id param and optionally disable the combo
			 * @param taxonId
			 * @param disable whether to disable the combo, true by default
			 */
			lockInTaxonId: function(taxId, diable){
				if(taxId && taxId !== null){
					this.taxCombo.getStore().on('load', function(){
						this.taxCombo.setTaxonById(taxId);
						this.taxCombo.disable();
					}, this);
					// must run after listener in Gemma.StatefulRemoteCombo
					this.taxCombo.on('ready', function(){
						this.taxCombo.disable();
					}, this);
				}
			},
			initComponent : function() {

				this.addEvents("commit");
				this.formId = Ext.id();
				
				// allow taxon to be set in configs on instantiation
				this.taxCombo = new Gemma.TaxonCombo({
								id: 'new-eesetTaxon',
								name: 'newEesetTaxon',
								fieldLabel: 'Taxon'
							});
				
				Ext.apply(this, {
					items: new Ext.FormPanel({
						id: this.id + 'FormPanel',
						ref: 'formPanel',
						frame: false,
						labelAlign: 'left',
						height: 250,
						items: new Ext.form.FieldSet({
							id: this.id + 'FieldSet',
							ref: 'fieldSet',
							height: 240,
							items: [this.taxCombo, new Ext.form.TextField({
								ref: 'nameField',
								id: this.id + "Name",
								fieldLabel: 'Name',
								name: 'newSetName',
								minLength: 3,
								allowBlank: false,
								invalidText: "You must provide a name",
								msgTarget: 'side',
								width: 300
							}), new Ext.form.TextArea({
								ref: 'descField',
								id: this.id + 'Desc',
								fieldLabel: 'Description',
								name: 'newSetDescription',
								value: this.description,
								width: 300
							//value: this.suggestedDescription
							}), new Ext.form.Checkbox({
								fieldLabel: 'Public group',
								name: 'publik',
								checked: this.publik,
								value: this.publik,
								width: 300
							})]
						})
					}),
					buttons: [{
						text: "Cancel",
						handler: this.hide.createDelegate(this, [])
					}, {
						text: "OK",
						scope:this,
						handler: function(){
							if (!this.formPanel.fieldSet.nameField.validate()) {
								return;
							}
							var values = this.formPanel.getForm().getValues();
							var taxon = this.formPanel.getForm().findField('newEesetTaxon').getTaxon();
							this.fireEvent("commit", {
								name: values.newSetName,
								description: values.newSetDescription,
								publik: (typeof values.publik !== "undefined" && values.publik === "on"),
								taxon: taxon
							});
							this.hide();
							return;
						}
					}]
				
				});

				Gemma.CreateSetDetailsWindow.superclass.initComponent.call(this);
				this.addEvents("commit");
			}
		});