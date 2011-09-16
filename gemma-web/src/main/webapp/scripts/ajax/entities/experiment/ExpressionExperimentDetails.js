Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
/**
 *
 * Panel containing the most interesting info about an experiment.
 * Used as one tab of the EE page
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 *
 */
Gemma.ExpressionExperimentDetails = Ext.extend(Ext.Panel, {
	
	dirtyForm : false,
	listeners:{
		leavingTab: function(){
			if(this.editModeOn && this.dirtyForm){
				var leave = confirm("You are still in edit mode. Your unsaved changes will be discarded when you switch tabs. Do you want to continue?");
				if(leave){
					return true;
				}
				return false;
			}
			return true;
		},
		tabChanged: function(){
			this.fireEvent('toggleEditMode', false);
		}
	},

    renderArrayDesigns: function(arrayDesigns){
        var result = '';
        for (var i = 0; i < arrayDesigns.length; i++) {
            var ad = arrayDesigns[i];
            result = result + '<a href="/Gemma/arrays/showArrayDesign.html?id=' + ad.id + '">' + ad.shortName +
            '</a> - ' +
            ad.name;
            if (i < arrayDesigns.length - 1) {
                result = result + "<br/>";
            }
        }
        return result;
    },
    renderCoExpressionLinkCount: function(ee){
    
        if (ee.coexpressionLinkCount === null) {
            return "Unavailable"; // analysis not run.
        }
        
        var downloadCoExpressionDataLink = String.format(
			"<span style='cursor:pointer'  ext:qtip='Download all coexpression  data in a tab delimited format'  "+
				"onClick='fetchCoExpressionData({0})' > &nbsp; <img src='/Gemma/images/download.gif'/> &nbsp; </span>", ee.id);
        var count;
        
        return ee.coexpressionLinkCount + "&nbsp;" + downloadCoExpressionDataLink;
        
    },
    
    /**
     *
    visualizeDiffExpressionHandler: function(eeid, diffResultId, factorDetails){
    
        var params = {};
        this.visDiffWindow = new Gemma.VisualizationWithThumbsWindow({
            thumbnails: false,
            readMethod: DEDVController.getDEDVForDiffExVisualizationByThreshold,
            title: "Top diff. ex. probes for " + factorDetails,
            showLegend: false,
            downloadLink: String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&rs={1}&thresh={2}&diffex=1", eeid, diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD)
        });
        this.visDiffWindow.show({
            params: [eeid, diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD]
        });
        
    },
     */
    
    /**
     *
     */
    visualizePcaHandler: function(eeid, component, count){
    
        var params = {};
        this.vispcaWindow = new Gemma.VisualizationWithThumbsWindow({
            thumbnails: false,
            readMethod: DEDVController.getDEDVForPcaVisualization,
            title: "Top loaded probes for PC" + component,
            showLegend: false,
            downloadLink: String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&component={1}&thresh={2}&pca=1", eeid, component, count)
        });
        this.vispcaWindow.show({
            params: [eeid, component, count]
        });
        
    },
    
    /**
     *
     */
    renderSourceDatabaseEntry: function(ee){
        var result = '';
        
        var logo = '';
        if (ee.externalDatabase == 'GEO') {
            var acc = ee.accession;
            acc = acc.replace(/\.[1-9]$/, ''); // in case of multi-species.
            logo = '/Gemma/images/logo/geoTiny.png';
            result = '<a target="_blank" href="http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=' + acc +
            '"><img src="' +
            logo +
            '"/></a>';
            
        }
        else 
            if (ee.externalDatabase == 'ArrayExpress') {
                logo = '/Gemma/images/logo/arrayExpressTiny.png';
                result = '<a target="_blank" href="http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession=' +
                ee.accession +
                '"><img src="' +
                logo +
                '"/></a>';
            }
            else {
                result = "Direct upload";
            }
        
        return result;
        
    },
    
    /**
     * Link for samples details page.
     *
     * @param {}
     *            ee
     * @return {}
     */
    renderSamples: function(ee){
        var result = ee.bioAssayCount;
        if (this.editable) {
            result = result +
            '&nbsp;&nbsp<a href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=' +
            ee.id +
            '"><img ext:qtip="View the details of the samples" src="/Gemma/images/icons/magnifier.png"/></a>';
        }
        return '' + result; // hack for possible problem with extjs 3.1 - bare
        // number not displayed, coerce to string.
    },
    
    renderStatus: function(ee){
        var result = '';
        if (ee.validatedFlag) {
            result = result + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" title="validated"/>';
        }
        
        if (ee.troubleFlag) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" title="trouble"/>';
        }
        
        if (ee.hasMultiplePreferredQuantitationTypes) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' +
            'title="This experiment has multiple \'preferred\' quantitation types. ' +
            'This isn\'t necessarily a problem but is suspicious."/>';
        }
        
        if (ee.hasMultipleTechnologyTypes) {
            result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' +
            'title="This experiment seems to mix array designs with different technology types."/>';
        }
        
        if (this.editable) {
            result = result +
            Gemma.SecurityManager.getSecurityLink('ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', 
				ee.id, ee.isPublic, ee.isShared, this.editable);
        }
        
        return result || "No flags";
        
    },
    
    linkAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''+panelId+'eemanager\').doLinks(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
        if (ee.dateLinkAnalysis) {
            var type = ee.linkAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedLinkAnalysisEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            else 
                if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
                    color = '#CCC';
                    qtip = 'ext:qtip="Too small"';
                    suggestRun = false;
                }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateLinkAnalysis, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
        
    },
    
    missingValueAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''+panelId+'eemanager\').doMissingValues(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';
        
        /*
         * Offer missing value analysis if it's possible (this might need
         * tweaking).
         */
        if (ee.technologyType != 'ONECOLOR' && ee.hasEitherIntensity) {
        
            if (ee.dateMissingValueAnalysis) {
                var type = ee.missingValueAnalysisEventType;
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEventImpl') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
            
        }
        else {
            return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
        }
    },
    
    processedVectorCreateRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''+panelId+'eemanager\').doProcessedVectors(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></span>';
        
        if (ee.dateProcessedDataVectorComputation) {
            var type = ee.processedDataVectorComputationEventType;
            var color = "#000";
            
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEventImpl') { // note:
                // no
                // such
                // thing.
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateProcessedDataVectorComputation, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },
    
    differentialAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''+panelId+'eemanager\').doDifferential(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></span>';
        
        if (ee.numPopulatedFactors > 0) {
            if (ee.dateDifferentialAnalysis) {
                var type = ee.differentialAnalysisEventType;
                
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedDifferentialExpressionAnalysisEventImpl') { // note:
                    // no
                    // such
                    // thing.
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateDifferentialAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
        }
        else {
            return '<span style="color:#CCF;">NA</span>';
        }
    },
    renderProcessedExpressionVectorCount: function(e){
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    },
    

    initComponent: function(){
    
    	this.panelId = this.getId();
        Gemma.ExpressionExperimentDetails.superclass.initComponent.call(this);
        
        // if no permissions hasWritePermission is not set.
        if ((Ext.get("hasWritePermission")) && Ext.get("hasWritePermission").getValue() == 'true') {
            this.editable = true;
        }
		//  this.editable && this.admin may also have been set in component configs 
		
		var panelId = this.getId();
        var e = this.experimentDetails;
        var currentDescription = e.description;
        var currentName = e.name;
        var currentShortName = e.shortName;
		var currentPubMedId = e.pubmedId;
		var currentPrimaryCitation = e.primaryCitation;
        var manager = new Gemma.EEManager({
            editable: this.editable,
            id: panelId+"eemanager"
        });
        this.manager = manager;
        
		/*PUB MED REGION*/
		
		var pubMedDisplay = new Ext.Panel({
			xtype: 'panel',
			fieldLabel:'Publication',
			baseCls: 'x-plain-panel',
			style:'padding-top:5px',
			tpl: new Ext.XTemplate(
					'<tpl if="pubAvailable==\'true\'">' +
						'{primaryCitation}' +
						'&nbsp; <a target="_blank" ext:qtip="Go to PubMed (in new window)"' +
						' href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids=' +
						'{pubMedId}' +
						'&query_hl=2&itool=pubmed_docsum"><img src="/Gemma/images/pubmed.gif" ealt="PubMed" /></a>&nbsp;&nbsp' +
					'</tpl>'+
					'<tpl if="pubAvailable==\'false\'">' +
						'Not Available'+
					'</tpl>'),
			data:{
				pubAvailable: (currentPubMedId)? 'true':'false',
				primaryCitation:currentPrimaryCitation,
				pubMedId:currentPubMedId
			},
			/*html: (currentPubMedId)?currentPrimaryCitation +
			'&nbsp; <a target="_blank" ext:qtip="Go to PubMed (in new window)"' +
			' href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids=' +
			currentPubMedId +
			'&query_hl=2&itool=pubmed_docsum"><img src="/Gemma/images/pubmed.gif" ealt="PubMed" /></a>&nbsp;&nbsp': 'Not Available' ,
			*/listeners: {
				'toggleEditMode': function(editOn){
					this.setVisible(!editOn);
				}
			}
		});
		var pubMedIdField = new Ext.form.NumberField({
			xtype: 'numberfield',
			allowDecimals: false,
			minLength: 7,
			maxLength: 9,
			allowNegative: false,
			emptyText: (this.isAdmin || this.editable) ? 'Enter pubmed id' : 'Not Available',
			width: 100,
			value:currentPubMedId,
			enableKeyEvents: true,
            bubbleEvents: ['changeMade'],
			listeners: {
				'keyup': function(field, event){
					if (field.isDirty()) {
						field.fireEvent('changeMade', field.isValid());
					}
				},
				scope: this
			
			}
		});
		var pubMedDelete = {
			xtype: 'button',
			text:'Clear',
			icon:'/Gemma/images/icons/cross.png',
			tooltip: 'Remove this experiment\'s association with this publication',
            bubbleEvents: ['changeMade'],
			handler: function(){
				pubMedIdField.setValue('');
				field.fireEvent('changeMade', true);
			},
			scope:this
		};
		var pubMedForm = new Ext.Panel({
			fieldLabel:'Publication',
			xtype : 'panel',
			layout:'hbox',
			hidden: true,// hide until edit mode is activated
			padding:3,
			items : [pubMedIdField,pubMedDelete],
			listeners:{
                'toggleEditMode': function(editOn){
					this.setVisible(editOn);
					this.doLayout();
                }
			}
		});
        
        /*
         * This is needed to make the annotator initialize properly.
         */
        new Gemma.MGEDCombo({});
        
        var taggerurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''+panelId+'eemanager\').tagger(' + e.id + ',' +
        e.taxonId +
        ',' +
        this.editable +
        ',' +
        (e.validatedAnnotations !== null) +
        ')"><img src="/Gemma/images/icons/pencil.png" alt="view tags" title="view tags"/></span>';
        
        tagView = new Gemma.AnnotationDataView({
            readParams: [{
                id: e.id,
                classDelegatingFor: "ExpressionExperimentImpl"
            }]
        });
        
        
        manager.on('tagsUpdated', function(){
            tagView.store.reload();
        });
        
        manager.on('done', function(){
            /*
             * After a process that requires refreshing the page.
             */
            window.location.reload();
        });
        
        manager.on('reportUpdated', function(data){
            ob = data[0];
            var k = Ext.get('coexpressionLinkCount-region');
            Ext.DomHelper.overwrite(k, {
                html: ob.coexpressionLinkCount
            });
            k.highlight();
            k = Ext.get('processedExpressionVectorCount-region');
            Ext.DomHelper.overwrite(k, {
                html: ob.processedExpressionVectorCount
            });
            k.highlight();
        });
        
        manager.on('differential', function(){
            window.location.reload(true);
        });
        
        
        save = function(){
			if (!this.saveMask) {
				this.saveMask = new Ext.LoadMask(this.getEl(), {
					msg: "Saving ..."
				});
			}
			this.saveMask.show();
            var shortName = shortNameField.getValue();
            var description = descriptionArea.getValue();
            var name = nameArea.getValue();
			var newPubMedId = pubMedIdField.getValue();
            
            var entity = {
                entityId: e.id
            };
            
            if (shortName != currentShortName) {
                entity.shortName = shortName;
            }
            
            if (description != currentDescription) {
                entity.description = description;
            }
            
            if (name != currentName) {
                entity.name = name;
            }
			
			if (!newPubMedId) {
				entity.pubMedId = currentPubMedId;
				entity.removePrimaryPublication = true;
			}else if(newPubMedId !== currentPubMedId){
				entity.pubMedId = newPubMedId;
				entity.removePrimaryPublication = false;
			}else {
				entity.removePrimaryPublication = false;
			}
			// returns ee details object
            ExpressionExperimentController.updateBasics(entity, function(data){
            
                shortNameField.setValue(data.shortName);
                nameArea.setValue(data.name);
                descriptionArea.setValue(data.description);
				pubMedIdField.setValue(data.pubmedId);
				pubMedDisplay.update({
					pubAvailable: (data.pubmedId) ? 'true' : 'false',
					primaryCitation: data.primaryCitation,
					pubMedId: data.pubmedId
				});
                
                currentShortName = data.shortName;
                currentName = data.name;
                currentDescription = data.description;
				currentPubMedId = data.pubmedId;
				currentPrimaryCitation = data.primaryCitation;
				
			this.dirtyForm = false;
			this.saveMask.hide();
                
            }.createDelegate(this));
			
			
        }.createDelegate(this);
        
        var descriptionArea = new Ext.form.TextArea({
            allowBlank: true,
            resizable: true,
            readOnly: true,
            disabled: true,
			growMin:1,
			growMax:150,
			growAppend:'',
			grow : true,
            disabledClass: 'disabled-plain',
            fieldClass: '',
            emptyText: 'No description provided',
            enableKeyEvents: true,
            bubbleEvents: ['changeMade'],
            listeners: {
                'keyup': function(field, e){
                    if (field.isDirty()) {
                        field.fireEvent('changeMade', field.isValid());
                    }
                },
                'toggleEditMode': function(editOn){
                    this.setReadOnly(!editOn);
                    this.setDisabled(!editOn);
                    if (editOn) {
                        this.removeClass('x-bare-field');
                    }
                    else {
                        this.addClass('x-bare-field');
                    }
                }
            },
            style: 'width: 100%; background-color: #fcfcfc; border: 1px solid #cccccc;',
            value: currentDescription
        });
        
        var shortNameField = new Ext.form.TextField({
            enableKeyEvents: true,
            allowBlank: false,
            disabledClass: 'disabled-plain',
            readOnly: true,
            disabled: true,
            style: 'font-weight: bold; font-size:1.4em; height:1.5em; color:black',
            bubbleEvents: ['changeMade'],
            listeners: {
                'keyup': function(field, e){
                    if (field.isDirty()) {
                        field.fireEvent('changeMade', field.isValid());
                    }
                },
                'toggleEditMode': function(editOn){
                    this.setReadOnly(!editOn);
                    this.setDisabled(!editOn);
                    if (editOn) {
                        this.removeClass('x-bare-field');
                    }
                    else {
                        this.addClass('x-bare-field');
                    }
                }
            },
            value: currentShortName
        });
        
        var nameArea = new Ext.form.TextArea({
            allowBlank: false,
            grow: true,
            //growMin: 22,
            growAppend: '',
            readOnly: true,//!this.editable,
            disabled: true,
            disabledClass: 'disabled-plain',
            emptyText: 'No description provided',
            enableKeyEvents: true,
            bubbleEvents: ['changeMade'],
            listeners: {
                'keyup': function(field, e){
                    if (field.isDirty()) {
                        field.fireEvent('changeMade', field.isValid());
                    }
                },
                'toggleEditMode': function(editOn){
                    this.setReadOnly(!editOn);
                    this.setDisabled(!editOn);
                    if (editOn) {
                        this.removeClass('x-bare-field');
                    }
                    else {
                        this.addClass('x-bare-field');
                    }
                }
            },
            style: 'font-weight: bold; font-size:1.3em; width:100%',
            value: currentName
        });
        
        resetEditableFields = function(){
            shortNameField.setValue(currentShortName);
            nameArea.setValue(currentName);
            descriptionArea.setValue(currentDescription);
			pubMedIdField.setValue(currentPubMedId);
            saveBtn.disable();
            cancelBtn.disable();
        };
        
        var editBtn = new Ext.Button({
            // would like to use on/off slider or swtich type control here
            text: 'Start editing',
            editOn: false,
            disabled: !this.editable,
            handler: function(button, event){
                this.fireEvent('toggleEditMode', true);
            },
            scope: this
        });
        var cancelBtn = new Ext.Button({
            text: 'Cancel',
            disabled: true,
            toolTip: 'Reset all fields to saved values',
            handler: function(){
                this.fireEvent('toggleEditMode', false);
            },
            scope: this
        });
        
        var saveBtn = new Ext.Button({
            text: 'Save',
            disabled: true,
            handler: function(){
                save();
                this.fireEvent('toggleEditMode', false);
            }
			,scope: this
        });
        var editEEButton = new Ext.Button({
            text: 'More edit options',
            icon: '/Gemma/images/icons/wrench.png',
            toolTip: 'Go to editor page for this experiment',
            disabled: !this.editable,
            handler: function(){
                window.open('/Gemma/expressionExperiment/editExpressionExperiment.html?id=' +
                this.experimentDetails.id);
            },
            scope: this
        });
        var deleteEEButton = new Ext.Button({
            text: 'Delete Experiment',
            icon: '/Gemma/images/icons/cross.png',
            toolTip: 'Delete the experiment from the system',
            disabled: !this.editable,
            handler: function(){
                manager.deleteExperiment(this.experimentDetails.id, true);
            },
            scope: this
        });
        
        this.on('toggleEditMode', function(editOn){
            // is there a way to make this even propagate to all children automatically?
			this.editModeOn = editOn; // needed to warn user before tab change
            editBtn.setText((editOn) ? 'Editing mode on' : 'Start editing');
            editBtn.setDisabled(editOn);
            nameArea.fireEvent('toggleEditMode', editOn);
            descriptionArea.fireEvent('toggleEditMode', editOn);
            shortNameField.fireEvent('toggleEditMode', editOn);
			pubMedForm.fireEvent('toggleEditMode', editOn);
			pubMedDisplay.fireEvent('toggleEditMode', editOn);
            resetEditableFields();
            saveBtn.setDisabled(!editOn);
            cancelBtn.setDisabled(!editOn);
			if(!editOn){
                resetEditableFields();
				this.dirtyForm = false;
			}
        });
        
        this.on('changeMade', function(wasValid){
            // enable save button
            saveBtn.setDisabled(!wasValid);
            cancelBtn.setDisabled(!wasValid);
			this.dirtyForm = true;
            
        });
        var basics = new Ext.Panel({
            ref: 'fieldPanel',
            collapsible: false,
            bodyBorder: false,
            frame: false,
            baseCls: 'x-plain-panel',
            bodyStyle: 'padding:10px',
            defaults: {
                bodyStyle: 'vertical-align:top;font-size:12px;color:black',
                baseCls: 'x-plain-panel',
                fieldClass: 'x-bare-field'
            },
            tbar: new Ext.Toolbar({
                hidden: !this.editable,
                items: [editBtn, ' ', saveBtn, ' ', cancelBtn, '-', editEEButton, '-', deleteEEButton]
            }),
            items: [shortNameField, nameArea, {
                layout: 'form',
                defaults: {
                    border: false
                },
                items: [{
                    fieldLabel: "Taxon",
                    html: e.taxon
                }, {
                    fieldLabel: 'Tags&nbsp;' + taggerurl,
                    items: [tagView]
                }, {
                    fieldLabel: 'Samples',
                    html: this.renderSamples(e),
                    width: 60
                }, {
                    fieldLabel: 'Profiles',
                    //id: 'processedExpressionVectorCount-region',
                    html: '<div id="downloads"> ' +
                    this.renderProcessedExpressionVectorCount(e) +
                    '&nbsp;&nbsp;' +
                    '<i>Downloads:</i> &nbsp;&nbsp; <span class="link"  ext:qtip="Download the tab delimited data" onClick="fetchData(true,' +
                    e.id +
                    ', \'text\', null, null)">Filtered</span> &nbsp;&nbsp;' +
                    '<span class="link" ext:qtip="Download the tab delimited data" onClick="fetchData(false,' +
                    e.id +
                    ', \'text\', null, null)">Unfiltered</span> &nbsp;&nbsp;' +
                    '<a class="helpLink" href="?" onclick="showHelpTip(event, \'Tab-delimited data file for this experiment. ' +
                    'The filtered version corresponds to what is used in most Gemma analyses, removing some probes. Unfiltered includes all probes\');' +
                    ' return false"> <img src="/Gemma/images/help.png" /> </a>' +
                    '</div>',
                    width: 400
                }, {
                    fieldLabel: 'Array designs',
                    html: this.renderArrayDesigns(e.arrayDesigns),
                    width: 480
                }, {
                    fieldLabel: 'Coexpr. Links',
                    html: this.renderCoExpressionLinkCount(e),
                    width: 80
                }, {
                    fieldLabel: 'Differential Expr. Analyses',
                    items: new Gemma.DifferentialExpressionAnalysesSummaryTree(e)
                }, {
                    fieldLabel: 'Status',
                    html: this.renderStatus(e)
                }]
            }, descriptionArea, {
                layout: 'form',
                defaults: {
                    border: false
                },
                items: [pubMedDisplay,pubMedForm, {
                    fieldLabel: 'Created',
                    html: Ext.util.Format.date(e.dateCreated) + ' from ' + this.renderSourceDatabaseEntry(e)
                }, {
                    html: 'The last time an array design associated with this experiment was updated: ' + e.lastArrayDesignUpdateDate,
                    hidden: !e.lastArrayDesignUpdateDate
                }]
            }]
        });
        
        this.add(basics);
        
				
		// adjust when user logs in or out
		Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
			var appScope = this;
			ExpressionExperimentController.canCurrentUserEditExperiment(this.experimentDetails.id, {
				callback: function(editable){
					//console.log(this);
					appScope.adjustForIsEditable(editable);
				},
				scope: appScope
			});
			
		},this);
		Gemma.Application.currentUser.on("logOut", function(){
			this.adjustForIsEditable(false);
			// TODO reset widget if experiment is private!
		},this);
		
        this.doLayout();
        this.fireEvent("ready");
        
    }, // end of initComponent
    adjustForIsEditable: function(editable){
		this.fieldPanel.getTopToolbar().setVisible(editable);
	}
});

