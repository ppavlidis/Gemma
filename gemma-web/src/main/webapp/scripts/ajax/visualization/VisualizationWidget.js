Ext.namespace('Gemma');

/**
 * @extends Ext.data.Store
 */

Gemma.VisualizationStore = function(config) {

	this.record = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "ee"
			}, {
				name : "profiles"
			}]);

	this.readMethod = DEDVController.getDEDVForVisualization;

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
				id : "id"
			}, this.record);

	Gemma.VisualizationStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.VisualizationStore
 * @extends Ext.data.Store
 */

Ext.extend(Gemma.VisualizationStore, Ext.data.Store, {

			loadVisData : function(data) {
				var newDivName = "vis" + ee.shortName;
				var f = Flotr.draw(newDivName, flotrData);
			}

		});

Gemma.PLOT_SIZE = 150;

Gemma.ProfileTemplate = Ext.extend(Ext.XTemplate, {

			graphConfig : {
				lines : {
					lineWidth : 1
				},
				xaxis : {
					noTicks : 0
				},
				yaxis : {
					noTicks : 0
				},
				grid : {
					color : "white"
				},
				shadowSize : 0,
				legend : {
					position : 'n'
				}
			},

			overwrite : function(el, values, ret) {
				Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);
				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.ee.shortName;
					var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
							});

					// Must use prototype extraction here -- putting in newDiv fails.
					Flotr.draw($(shortName + "_vis"), record.profiles, this.graphConfig);
				}
			}
		});

Gemma.HeatmapTemplate = Ext.extend(Ext.XTemplate, {

			overwrite : function(el, values, ret) {
				Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);
				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.ee.shortName;
					var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
							});

					Heatmap.draw($(shortName + "_vis"), record.profiles);
				}
			}
		});

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
			id : 'VisualizationWindow',
			width : 800,
			height : 500,
			closeAction : 'destroy',
			bodyStyle : "background:white",
			layout : 'fit',
			constrainHeader : true,
			title : "Visualization",

			initComponent : function() {
				// If there are any compile errors with the template the error will not make its way to the console.
				// Tried every combination i could think of to get the profile to display...
				// Can't seem to access the data even though its there...

				this.dv = new Ext.DataView({
							autoHeight : true,
							emptyText : 'No images to display',
							store : new Gemma.VisualizationStore(),

							tpl : new Gemma.HeatmapTemplate('<tpl for="."><tpl for="ee">',
									'<div id ="{shortName}_vizwrap" > {shortName} </div>', '</tpl></tpl>'),

							prepareData : function(data) {

								// Need to transform the cordinate data from an object to an array for flotr
								// probe, genes
								var flotrData = [];
								var coordinateProfile = data.profiles;

								for (var i = 0; i < coordinateProfile.size(); i++) {
									var coordinateObject = coordinateProfile[i].points;

									var probe = coordinateProfile[i].probe.name;
									var genes = coordinateProfile[i].genes;
									var color = coordinateProfile[i].color;

									var oneProfile = [];

									for (var j = 0; j < coordinateObject.size(); j++) {
										var point = [coordinateObject[j].x, coordinateObject[j].y];
										oneProfile.push(point);
									}
									var plotConfig = {
										data : oneProfile,
										color : color
									};

									flotrData.push(plotConfig);
								}

								var data

								data.profiles = flotrData;
								return data;
							}
						});

				Ext.apply(this, {
							items : [this.dv]
						});

				Gemma.VisualizationWindow.superclass.initComponent.call(this);

			},

			displayWindow : function(eeIds, geneIds) {

				var params = [];
				params.push(eeIds);
				params.push(geneIds);
				this.show();
				this.dv.store.load({
							params : params
						});

			},

			clearWindow : function() {
				// this.hide();
				// TODO: Clear the window of old graphs
			}

		});
