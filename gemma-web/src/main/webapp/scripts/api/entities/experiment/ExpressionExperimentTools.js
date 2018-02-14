Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = ctxBasePath + '/images/default/s.gif';

/**
 *
 * Used as one tab of the EE page - the "Admin" tab.
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Gemma.CurationTools
 *
 */
Gemma.ExpressionExperimentTools = Ext.extend(Gemma.CurationTools, {

    experimentDetails: null,
    tbar: new Ext.Toolbar(),

    /**
     * @memberOf Gemma.ExpressionExperimentTools
     */
    initComponent: function () {
        this.curatable = this.experimentDetails;
        this.auditable = {
            id: this.experimentDetails.id,
            classDelegatingFor: "ubic.gemma.model.expression.experiment.ExpressionExperiment"
        };
        Gemma.ExpressionExperimentTools.superclass.initComponent.call(this);
        var manager = new Gemma.EEManager({
            editable: this.editable
        });
        manager.on('reportUpdated', function () {
            this.fireEvent('reloadNeeded');
        }, this);


        var refreshButton = new Ext.Button({
            text: 'Refresh',
            icon: ctxBasePath + '/images/icons/arrow_refresh_small.png',
            tooltip: 'Refresh statistics (not including the differential expression ones)',
            handler: function () {
                manager.updateEEReport(this.experimentDetails.id);
            },
            scope: this

        });
        this.getTopToolbar().addButton(refreshButton);

        var eeRow = new Ext.Panel({
            cls: 'ee-tool-row',
            defaults: {
                width: '100%',
                border: false,
                padding: 2
            }
        });

        eeRow.add({
            html: '<hr class="normal"/>'
        });

        var leftPanel = new Ext.Panel({
            cls: 'ee-tool-left',
            defaults: {
                border: false,
                padding: 2
            }
        });

        leftPanel.add({
            html: '<h4>Preprocessing:</h4>'
        });
        leftPanel.add(this.missingValueAnalysisPanelRenderer(this.experimentDetails, manager));
        leftPanel.add(this.processedVectorCreatePanelRenderer(this.experimentDetails, manager));
        // PCA analysis
        leftPanel.add(this.pcaPanelRenderer(this.experimentDetails, manager));
        // Batch information
        leftPanel.add(this.batchPanelRenderer(this.experimentDetails, manager));

        var batchInfoMissingPanel = this.batchInfoMissingRenderer(this.experimentDetails, manager);
        var batchConfoundPanel = this.batchConfoundRenderer(this.experimentDetails, manager);
        var batchEffectPanel = this.batchEffectRenderer(this.experimentDetails, manager);
        if (batchConfoundPanel !== null || batchEffectPanel !== null || batchInfoMissingPanel !== null) {
            leftPanel.add({html: "<br/><h4>Batch info quality:</h4>"});
            if (batchInfoMissingPanel !== null) leftPanel.add(batchInfoMissingPanel);
            if (batchConfoundPanel !== null) leftPanel.add(batchConfoundPanel);
            if (batchEffectPanel !== null) leftPanel.add(batchEffectPanel);
        }

        leftPanel.add({html: "<br/><h4>Analyses:</h4>"});
        leftPanel.add(this.differentialAnalysisPanelRenderer(this.experimentDetails, manager));
        leftPanel.add(this.linkAnalysisPanelRenderer(this.experimentDetails, manager));

        eeRow.add(leftPanel);

        var rightPanel = new Ext.Panel({
            cls: 'ee-tool-right',
            defaults: {
                border: false,
                padding: 2
            }
        });

        rightPanel.add({
            html: '<h3>Quality</h3>'
        });

        rightPanel.add({
            html: '<h3>Suitability</h3>'
        });

        eeRow.add(rightPanel);

        this.add(eeRow);
    },

    batchInfoMissingRenderer: function (ee, mgr) {

        var panelBC = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: []
        });

        var be = (ee.hasBatchInformation === false)
            ? {
                html: '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                + Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchInfo
            }
            : {
                html: '<i class="green fa fa-check-square-o fa-lg" ></i>&nbsp;Experiment does have batch information'
            };

        panelBC.add(be);

        return panelBC;
    },

    batchEffectRenderer: function (ee, mgr) {

        var panelBC = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: []
        });

        var be = (ee.batchEffect !== null && ee.batchEffect !== "")
            ? {
                html: '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                + ee.batchEffect
            }
            : {
                html: '<i class="' + ((ee.hasBatchInformation === false) ? 'dark-gray' : 'green') +
                ' fa fa-check-square-o fa-lg" ></i>&nbsp;Batch effect not detected'
            };

        panelBC.add(be);

        var recalculateBCBtn = new Ext.Button({
            tooltip: 'Recalculate batch effect',
            handler: function (b, e) {
                ExpressionExperimentController.recalculateBatchEffect(ee.id, {
                    callback: function () {
                        window.location.reload();
                    }
                });
                b.setIconClass("btn-loading");
            },
            scope: this,
            cls: 'transparent-btn'
        });

        recalculateBCBtn.setIconClass('btn-not-loading');

        panelBC.add(recalculateBCBtn);
        return panelBC;
    },

    batchConfoundRenderer: function (ee, mgr) {

        var panelBC = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: []
        });

        var be = (ee.batchConfound !== null && ee.batchConfound !== "")
            ? {
                html: '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                + ee.batchConfound + " (batch confound)"
            }
            : {
                html: '<i class="' + ((ee.hasBatchInformation === false) ? 'dark-gray' : 'green') + ' fa fa-check-square-o fa-lg" ></i>&nbsp;Batch confound not detected'
            };

        panelBC.add(be);

        var recalculateBCBtn = new Ext.Button({
            tooltip: 'Recalculate batch confound',
            handler: function (b, e) {
                ExpressionExperimentController.recalculateBatchConfound(ee.id, {
                    callback: function () {
                        window.location.reload();
                    }
                });
                b.setIconClass("btn-loading");
            },
            scope: this,
            cls: 'transparent-btn'
        });

        recalculateBCBtn.setIconClass('btn-not-loading');

        panelBC.add(recalculateBCBtn);
        return panelBC;
    },

    linkAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Link Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'missing value computation',
            handler: manager.doLinks.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.dateLinkAnalysis) {
            var type = ee.linkAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="Analysis was OK"';
            if (type == 'FailedLinkAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Analysis failed"';
            } else if (type == 'TooSmallDatasetLinkAnalysisEvent') {
                color = '#CCC';
                qtip = 'ext:qtip="Analysis was too small"';
                suggestRun = false;
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateLinkAnalysis)
            });
            if (suggestRun) {
                panel.add(runBtn);
            }
            return panel;
        } else {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
            return panel;
        }

    },

    missingValueAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Missing values: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'missing value computation',
            handler: manager.doMissingValues.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        /*
         * Offer missing value analysis if it's possible (this might need tweaking).
         */
        if (ee.technologyType != 'ONECOLOR' && ee.technologyType != 'NONE' && ee.hasEitherIntensity) {

            if (ee.dateMissingValueAnalysis) {
                var type = ee.missingValueAnalysisEventType;
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEvent') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }

                panel.add({
                    html: '<span style="color:' + color + ';" ' + qtip + '>'
                    + Gemma.Renderers.dateRenderer(ee.dateMissingValueAnalysis) + '&nbsp;'
                });
                if (suggestRun) {
                    panel.add(runBtn);
                }
                return panel;
            } else {
                panel.add({
                    html: '<span style="color:#3A3;">Needed</span>&nbsp;'
                });
                panel.add(runBtn);
                return panel;
            }

        } else {

            panel
                .add({
                    html: '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>'
                });
            return panel;
        }
    },

    processedVectorCreatePanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Processed Vector Computation: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'processed vector computation',
            handler: manager.doProcessedVectors.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.dateProcessedDataVectorComputation) {
            var type = ee.processedDataVectorComputationEventType;
            var color = "#000";

            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEvent') { // note:
                // no
                // such
                // thing.
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateProcessedDataVectorComputation) + '&nbsp;'
            });
            if (suggestRun) {
                panel.add(runBtn);
            }
            return panel;
        } else {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
            return panel;
        }
    },

    differentialAnalysisPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Differential Expression Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'differential expression analysis',
            handler: manager.doDifferential.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });
        if (ee.numPopulatedFactors > 0) {
            if (ee.dateDifferentialAnalysis) {
                var type = ee.differentialAnalysisEventType;

                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedDifferentialExpressionAnalysisEvent') { // note:
                    // no
                    // such
                    // thing.
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                panel.add({
                    html: '<span style="color:' + color + ';" ' + qtip + '>'
                    + Gemma.Renderers.dateRenderer(ee.dateDifferentialAnalysis) + '&nbsp;'
                });
                if (suggestRun) {
                    panel.add(runBtn);
                }
                return panel;
            } else {

                panel.add({
                    html: '<span style="color:#3A3;">Needed</span>&nbsp;'
                });
                panel.add(runBtn);
                return panel;
            }
        } else {

            panel.add({
                html: '<span style="color:#CCF;">NA</span>'
            });
            return panel;
        }
    },

    renderProcessedExpressionVectorCount: function (e) {
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    },

    /*
     * Get the last date PCA was run, add a button to run PCA
     */
    pcaPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Principal Component Analysis: '
            }]
        });
        var id = ee.id;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'principal component analysis',
            // See EEManger.js doPca(id, hasPca)
            handler: manager.doPca.createDelegate(this, [id, true]),
            scope: this,
            cls: 'transparent-btn'
        });

        // Get date and info
        if (ee.datePcaAnalysis) {
            var type = ee.pcaAnalysisEventType;

            var color = "#000";
            var qtip = 'ext:qtip="OK"';
            var suggestRun = true;

            if (type == 'FailedPCAAnalysisEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.datePcaAnalysis) + '&nbsp;'
            });
        } else
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });

        panel.add(runBtn);
        return panel;

    },

    /*
     * Get the last date batch info was downloaded, add a button to download
     */
    batchPanelRenderer: function (ee, manager) {
        var panel = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: [{
                html: 'Batch Information: '
            }]
        });
        var id = ee.id;
        var hasBatchInformation = ee.hasBatchInformation;
        var technologyType = ee.technologyType;
        var runBtn = new Ext.Button({
            icon: ctxBasePath + '/images/icons/control_play_blue.png',
            tooltip: 'batch information',
            // See EEManager.js doBatchInfoFetch(id)
            handler: manager.doBatchInfoFetch.createDelegate(this, [id]),
            scope: this,
            cls: 'transparent-btn'
        });

        // Batch info fetching not allowed for RNA seq and other non-microarray data
        if (technologyType == 'NONE') {
            panel.add({
                html: '<span style="color:#CCF; "ext:qtip="Not microarray data">' + 'NA' + '</span>&nbsp;'
            });
            return panel;
        }

        // If present, display the date and info. If batch information exists without date, display 'Provided'.
        // If no batch information, display 'Needed' with button for GEO and ArrayExpress data. Otherwise, NA.
        if (ee.dateBatchFetch) {
            var type = ee.batchFetchEventType;

            var color = "#000";
            var qtip = 'ext:qtip="OK"';

            if (type == 'FailedBatchInformationFetchingEvent') {
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            } else if (type == 'FailedBatchInformationMissingEvent') {
                color = '#CCC';
                qtip = 'ext:qtip="Raw data files not available from source"';
            }

            panel.add({
                html: '<span style="color:' + color + ';" ' + qtip + '>'
                + Gemma.Renderers.dateRenderer(ee.dateBatchFetch) + '&nbsp;'
            });
            panel.add(runBtn);
        } else if (hasBatchInformation) {
            panel.add({
                html: '<span style="color:#000;">Provided</span>'
            });
        } else if (ee.externalDatabase == "GEO" || ee.externalDatabase == "ArrayExpress") {
            panel.add({
                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
            });
            panel.add(runBtn);
        } else
            panel.add({
                html: '<span style="color:#CCF; "'
                + 'ext:qtip="Add batch information by creating a \'batch\' experiment factor">' + 'NA'
                + '</span>&nbsp;'
            });

        return panel;
    }
});
